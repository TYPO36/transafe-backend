package com.benmake.transafe.document.service.impl;

import com.benmake.transafe.document.common.constant.DocumentType;
import com.benmake.transafe.document.common.constant.ParseStatus;
import com.benmake.transafe.document.common.enums.ParseErrorCode;
import com.benmake.transafe.document.dto.ParseMessageDTO;
import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.document.es.DocumentIndex;
import com.benmake.transafe.document.mq.DocumentParseProducer;
import com.benmake.transafe.document.parser.DocumentParser;
import com.benmake.transafe.document.parser.ParserFactory;
import com.benmake.transafe.document.service.DocumentIndexService;
import com.benmake.transafe.document.service.ParseService;
import com.benmake.transafe.document.service.impl.LocalFileStorageService;
import com.benmake.transafe.infra.mapper.DocumentMapper;
import com.benmake.transafe.infra.mapper.TaskMapper;
import com.benmake.transafe.task.entity.TaskEntity;
import com.benmake.transafe.translate.dto.TranslateMessageDTO;
import com.benmake.transafe.translate.mq.TranslateTaskProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档解析服务实现
 *
 * @author JTP
 * @date 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseServiceImpl implements ParseService {

    private final DocumentMapper documentMapper;
    private final ParserFactory parserFactory;
    private final DocumentParseProducer producer;
    private final DocumentIndexService documentIndexService;
    private final LocalFileStorageService fileStorageService;
    private final TaskMapper taskMapper;
    private final TranslateTaskProducer translateTaskProducer;

    @Override
    @Transactional
    public void processParse(ParseMessageDTO message) {
        String fileId = message.getFileId();
        log.info("开始解析文档: fileId={}, fileName={}, fileType={}",
                fileId, message.getFileName(), message.getFileType());

        DocumentEntity doc = documentMapper.findByFileId(fileId)
                .orElseThrow(() -> {
                    log.error("文档不存在: fileId={}", fileId);
                    return new RuntimeException("文档不存在: " + fileId);
                });

        // 更新状态为解析中
        doc.setParseStatus(ParseStatus.PARSING);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        try {
            // 处理ZIP压缩包
            if (DocumentType.ZIP.name().equalsIgnoreCase(message.getFileType())) {
                processZipFile(doc, message);
                return;
            }

            // 获取解析器
            DocumentParser parser = parserFactory.getParser(message.getFileType());
            if (parser == null) {
                handleParseError(doc, ParseErrorCode.UNSUPPORTED_FORMAT, "不支持的文件格式");
                return;
            }

            // 获取文件输入流
            InputStream inputStream = getFileInputStream(message.getFileStoragePath());
            if (inputStream == null) {
                handleParseError(doc, ParseErrorCode.STORAGE_ERROR, "无法读取文件");
                return;
            }

            // 执行解析
            DocumentParser.ParseResult result = parser.parse(
                    inputStream, message.getFileName(), message.getPassword()
            );

            // 处理密码保护
            if (result.isPasswordProtected()) {
                handlePasswordProtected(doc, message);
                return;
            }

            // 处理解析错误
            if (!result.isSuccess()) {
                handleParseError(doc, ParseErrorCode.fromCode(result.errorCode()), result.errorMessage());
                return;
            }

            // 解析成功
            handleParseSuccess(doc, result);

        } catch (Exception e) {
            log.error("解析异常: fileId={}", fileId, e);
            handleParseError(doc, ParseErrorCode.UNKNOWN_ERROR, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processZipFile(DocumentEntity doc, ParseMessageDTO message) {
        try {
            InputStream inputStream = getFileInputStream(message.getFileStoragePath());
            if (inputStream == null) {
                handleParseError(doc, ParseErrorCode.STORAGE_ERROR, "无法读取ZIP文件");
                return;
            }

            List<DocumentEntity> extractedFiles = new ArrayList<>();
            String rootId = doc.getRootId() != null ? doc.getRootId() : doc.getFileId();

            try (ZipArchiveInputStream zipStream = new ZipArchiveInputStream(inputStream)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zipStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        String fileName = entry.getName();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = zipStream.read(buffer)) != -1) {
                            baos.write(buffer, 0, read);
                        }
                        byte[] content = baos.toByteArray();

                        // 保存到存储服务（创建 document 记录）
                        DocumentEntity savedDoc = fileStorageService.saveFileAndGetDocumentEntity(
                                fileName, new ByteArrayInputStream(content), message.getUserId());

                        // 创建文档记录
                        LocalDateTime now = LocalDateTime.now();
                        DocumentEntity extractedDoc = new DocumentEntity();
                        extractedDoc.setFileId(savedDoc.getFileId());
                        extractedDoc.setUserId(message.getUserId());
                        extractedDoc.setParentId(doc.getFileId());
                        extractedDoc.setRootId(rootId);
                        extractedDoc.setIsAttachment(true);
                        extractedDoc.setPriority(doc.getPriority());
                        extractedDoc.setParseStatus(ParseStatus.PENDING);
                        extractedDoc.setRetryCount(0);
                        extractedDoc.setParseErrorCode(0);
                        extractedDoc.setCreatedAt(now);
                        extractedDoc.setUpdatedAt(now);

                        documentMapper.insert(extractedDoc);
                        extractedFiles.add(extractedDoc);

                        // 发送解析消息
                        sendParseMessage(extractedDoc, savedDoc, message.getPassword());
                    }
                }
            }

            // 标记ZIP文件为已解析
            doc.setParseStatus(ParseStatus.PARSED);
            doc.setParseErrorCode(0);
            doc.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(doc);

            // 获取 ZIP 文件信息用于索引
            DocumentEntity zipDoc = documentMapper.findByFileId(doc.getFileId()).orElse(null);

            // 索引ZIP文件（不含内容，只有附件列表）
            DocumentIndex index = toDocumentIndex(doc, zipDoc, null, null);
            index.setAttachments(extractedFiles.stream()
                    .map(f -> {
                        DocumentEntity fe = documentMapper.findByFileId(f.getFileId()).orElse(null);
                        return DocumentIndex.AttachmentInfo.builder()
                                .name(fe != null ? fe.getFileName() : "")
                                .size(fe != null ? fe.getFileSize() : 0)
                                .fileId(f.getFileId())
                                .build();
                    })
                    .collect(Collectors.toList()));
            documentIndexService.save(index);

            log.info("ZIP文件解析完成: fileId={}, 包含{}个文件",
                    doc.getFileId(), extractedFiles.size());

        } catch (Exception e) {
            log.error("ZIP文件解析失败: fileId={}", doc.getFileId(), e);
            handleParseError(doc, ParseErrorCode.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * 处理解析成功
     */
    private void handleParseSuccess(DocumentEntity doc, DocumentParser.ParseResult result) {
        // 注意：content 不再存入 MySQL，只存 ES
        doc.setParseStatus(ParseStatus.PARSED);
        doc.setParseErrorCode(0);
        doc.setParseErrorMessage(null);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        // 获取文档信息用于索引
        DocumentEntity docEntity = documentMapper.findByFileId(doc.getFileId()).orElse(null);

        // 索引到ES
        DocumentIndex index = toDocumentIndex(doc, docEntity, result.content(), result.metadata());
        documentIndexService.save(index);

        // 处理附件（EML文件）
        if (docEntity != null && DocumentType.EML.name().equalsIgnoreCase(docEntity.getFileType())) {
            processEmailAttachments(doc, result.metadata());
        }

        // 检查是否需要翻译，触发翻译任务
        checkAndTriggerTranslation(doc);

        log.info("文档解析成功: fileId={}", doc.getFileId());
    }

    /**
     * 处理密码保护文件
     */
    private void handlePasswordProtected(DocumentEntity doc, ParseMessageDTO message) {
        doc.setParseStatus(ParseStatus.FAILED);
        doc.setParseErrorCode(ParseErrorCode.PASSWORD_PROTECTED.getCode());
        doc.setParseErrorMessage(ParseErrorCode.PASSWORD_PROTECTED.getMessage());
        doc.setPasswordProvided(message.getPassword());
        doc.setRetryCount(doc.getRetryCount() + 1);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        log.info("文档密码保护: fileId={}", doc.getFileId());
    }

    /**
     * 处理解析错误
     */
    private void handleParseError(DocumentEntity doc, ParseErrorCode errorCode, String errorMsg) {
        doc.setParseStatus(ParseStatus.FAILED);
        doc.setParseErrorCode(errorCode.getCode());
        doc.setParseErrorMessage(errorMsg);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        if (errorCode.isRetryable() && doc.getRetryCount() < 3) {
            producer.sendForRetry(toMessage(doc));
            log.info("文档解析失败但可重试: fileId={}, errorCode={}, retryCount={}",
                    doc.getFileId(), errorCode.getCode(), doc.getRetryCount());
        } else {
            log.warn("文档解析失败: fileId={}, errorCode={}", doc.getFileId(), errorCode.getCode());
        }
    }

    /**
     * 处理邮件附件
     */
    @SuppressWarnings("unchecked")
    private void processEmailAttachments(DocumentEntity doc, Map<String, Object> metadata) {
        if (metadata == null) return;

        Object attachmentsObj = metadata.get("attachments");
        if (!(attachmentsObj instanceof List)) return;

        List<Map<String, Object>> attachments = (List<Map<String, Object>>) attachmentsObj;
        String rootId = doc.getRootId() != null ? doc.getRootId() : doc.getFileId();

        // 获取父文档的 userId
        DocumentEntity parentDoc = documentMapper.findByFileId(doc.getFileId()).orElse(null);
        Long userId = parentDoc != null ? parentDoc.getUserId() : null;

        for (Map<String, Object> attachment : attachments) {
            String fileName = (String) attachment.get("name");
            String storagePath = (String) attachment.get("storagePath");
            String fileType = getFileType(fileName);

            // 从存储路径中获取 fileId
            String fileId = storagePath != null && storagePath.contains("/")
                    ? storagePath.substring(storagePath.lastIndexOf("/") + 1)
                    : null;

            if (fileId == null) {
                log.warn("无法从 storagePath 获取 fileId: {}", storagePath);
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            DocumentEntity attachDoc = new DocumentEntity();
            attachDoc.setFileId(fileId);
            attachDoc.setUserId(userId);
            attachDoc.setParentId(doc.getFileId());
            attachDoc.setRootId(rootId);
            attachDoc.setIsAttachment(true);
            attachDoc.setPriority(doc.getPriority());
            attachDoc.setParseStatus(ParseStatus.PENDING);
            attachDoc.setRetryCount(0);
            attachDoc.setParseErrorCode(0);
            attachDoc.setCreatedAt(now);
            attachDoc.setUpdatedAt(now);

            documentMapper.insert(attachDoc);

            ParseMessageDTO attachMessage = ParseMessageDTO.builder()
                    .fileId(fileId)
                    .parentId(doc.getFileId())
                    .rootId(rootId)
                    .fileStoragePath(storagePath)
                    .fileType(fileType)
                    .fileName(fileName)
                    .isAttachment(true)
                    .priority(doc.getPriority())
                    .retryCount(0)
                    .timestamp(LocalDateTime.now())
                    .userId(userId)
                    .build();

            producer.send(attachMessage);
        }

        log.info("邮件附件处理完成: parentId={}, attachmentCount={}",
                doc.getFileId(), attachments.size());
    }

    /**
     * 发送解析消息
     */
    private void sendParseMessage(DocumentEntity doc, DocumentEntity docEntity, String password) {
        ParseMessageDTO message = ParseMessageDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileStoragePath(docEntity.getStoragePath())
                .fileType(docEntity.getFileType())
                .fileName(docEntity.getFileName())
                .isAttachment(doc.getIsAttachment())
                .password(password)
                .priority(doc.getPriority())
                .retryCount(doc.getRetryCount())
                .timestamp(LocalDateTime.now())
                .userId(docEntity.getUserId())
                .build();

        producer.send(message);
    }

    /**
     * 转换为ParseMessageDTO
     */
    private ParseMessageDTO toMessage(DocumentEntity doc) {
        DocumentEntity docEntity = documentMapper.findByFileId(doc.getFileId())
                .orElseThrow(() -> new RuntimeException("文档不存在: " + doc.getFileId()));

        return ParseMessageDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileStoragePath(docEntity.getStoragePath())
                .fileType(docEntity.getFileType())
                .fileName(docEntity.getFileName())
                .isAttachment(doc.getIsAttachment())
                .password(doc.getPasswordProvided())
                .priority(doc.getPriority())
                .retryCount(doc.getRetryCount())
                .timestamp(LocalDateTime.now())
                .userId(docEntity.getUserId())
                .build();
    }

    /**
     * 转换为DocumentIndex
     */
    private DocumentIndex toDocumentIndex(DocumentEntity doc, DocumentEntity docEntity, String content, Map<String, Object> metadata) {
        // 使用 doc 的 id 作为 documentId
        Long documentId = doc.getId();
        Long userId = doc.getUserId();

        DocumentIndex.DocumentIndexBuilder builder = DocumentIndex.builder()
                .documentId(documentId)
                .fileId(doc.getFileId())
                .userId(userId)
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .parseStatus(doc.getParseStatus())
                .parseErrorCode(doc.getParseErrorCode())
                .parseErrorMessage(doc.getParseErrorMessage())
                .hasPassword(doc.getParseErrorCode() != null
                        && doc.getParseErrorCode() == ParseErrorCode.PASSWORD_PROTECTED.getCode())
                .isAttachment(doc.getIsAttachment())
                .priority(doc.getPriority())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt());

        // 从 docEntity 获取文件信息
        if (docEntity != null) {
            builder.fileName(docEntity.getFileName())
                    .fileNameKeyword(docEntity.getFileName())
                    .fileSize(docEntity.getFileSize())
                    .fileStoragePath(docEntity.getStoragePath())
                    .fileType(docEntity.getFileType());
        }

        if (content != null) {
            builder.content(content);
        }

        if (metadata != null) {
            builder.metadata(metadata);

            // 提取邮件元数据
            Object dateObj = metadata.get("emailDate");
            LocalDateTime emailDate = null;
            if (dateObj instanceof java.util.Date date) {
                emailDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            }
            DocumentIndex.EmailMetadata emailMetadata = DocumentIndex.EmailMetadata.builder()
                    .subject((String) metadata.get("emailSubject"))
                    .from((String) metadata.get("emailFrom"))
                    .to(listToString(metadata.get("emailTo")))
                    .cc(listToString(metadata.get("emailCc")))
                    .date(emailDate)
                    .build();
            builder.emailMetadata(emailMetadata);
        }

        return builder.build();
    }

    /**
     * List转字符串
     */
    private String listToString(Object obj) {
        if (obj instanceof List<?> list) {
            return String.join(",", list.stream().map(Object::toString).toList());
        }
        return obj != null ? obj.toString() : null;
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * 从存储服务获取文件输入流
     */
    private InputStream getFileInputStream(String storagePath) {
        return fileStorageService.getFileInputStream(storagePath);
    }

    /**
     * 检查并触发翻译任务
     *
     * <p>如果文档标记为需要翻译，则创建翻译任务并发送到翻译队列</p>
     */
    private void checkAndTriggerTranslation(DocumentEntity doc) {
        // 检查是否需要翻译
        if (!Boolean.TRUE.equals(doc.getNeedTranslate()) || doc.getTargetLang() == null) {
            return;
        }

        // 获取完整的文档信息
        DocumentEntity docEntity = documentMapper.findByFileId(doc.getFileId()).orElse(null);
        if (docEntity == null) {
            log.warn("无法找到文档，跳过翻译: fileId={}", doc.getFileId());
            return;
        }

        // 更新翻译状态
        doc.setTranslateStatus("pending");
        documentMapper.updateById(doc);

        // 创建翻译任务
        TaskEntity translateTask = new TaskEntity();
        translateTask.setTaskId(generateTaskId());
        translateTask.setUserId(doc.getUserId());
        translateTask.setDocumentId(doc.getId());
        translateTask.setTaskType("TRANSLATE");
        translateTask.setStatus("pending");
        translateTask.setCreatedAt(LocalDateTime.now());

        taskMapper.insert(translateTask);

        // 发送翻译消息
        TranslateMessageDTO message = TranslateMessageDTO.builder()
                .fileId(doc.getFileId())
                .taskId(translateTask.getTaskId())
                .userId(doc.getUserId())
                .sourceLang(doc.getSourceLang() != null ? doc.getSourceLang() : "auto")
                .targetLang(doc.getTargetLang())
                .priority(doc.getPriority())
                .retryCount(0)
                .timestamp(LocalDateTime.now())
                .build();

        // 根据优先级选择队列
        boolean isVip = doc.getPriority() != null && doc.getPriority() == 1;
        translateTaskProducer.sendByPriority(message, isVip);

        log.info("翻译任务已创建: fileId={}, taskId={}, targetLang={}",
                doc.getFileId(), translateTask.getTaskId(), doc.getTargetLang());
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
