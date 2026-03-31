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
import com.benmake.transafe.document.repository.DocumentRepository;
import com.benmake.transafe.document.service.DocumentIndexService;
import com.benmake.transafe.document.service.ParseService;
import com.benmake.transafe.file.service.LocalFileStorageService;
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
 * @author TYPO
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseServiceImpl implements ParseService {

    private final DocumentRepository documentRepository;
    private final ParserFactory parserFactory;
    private final DocumentParseProducer producer;
    private final DocumentIndexService documentIndexService;
    private final LocalFileStorageService fileStorageService;

    @Override
    @Transactional
    public void processParse(ParseMessageDTO message) {
        String fileId = message.getFileId();
        log.info("开始解析文档: fileId={}, fileName={}, fileType={}",
                fileId, message.getFileName(), message.getFileType());

        DocumentEntity doc = documentRepository.findByFileId(fileId)
                .orElseThrow(() -> {
                    log.error("文档不存在: fileId={}", fileId);
                    return new RuntimeException("文档不存在: " + fileId);
                });

        // 更新状态为解析中
        doc.setParseStatus(ParseStatus.PARSING);
        doc.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(doc);

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
            InputStream inputStream = getFileInputStream(doc.getFileStoragePath());
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
                        String fileId = UUID.randomUUID().toString().replace("-", "");
                        String fileName = entry.getName();
                        String fileType = getFileType(fileName);
                        long fileSize = entry.getSize();

                        // 读取文件内容到内存
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = zipStream.read(buffer)) != -1) {
                            baos.write(buffer, 0, read);
                        }
                        byte[] content = baos.toByteArray();

                        // 上传到存储服务
                        String storagePath = saveToStorage(fileName, new ByteArrayInputStream(content), message.getUserId());

                        DocumentEntity extractedDoc = new DocumentEntity();
                        extractedDoc.setFileId(fileId);
                        extractedDoc.setUserId(doc.getUserId());
                        extractedDoc.setParentId(doc.getFileId());
                        extractedDoc.setRootId(rootId);
                        extractedDoc.setFileName(fileName);
                        extractedDoc.setFileSize(fileSize);
                        extractedDoc.setFileStoragePath(storagePath);
                        extractedDoc.setFileType(fileType);
                        extractedDoc.setIsAttachment(true);
                        extractedDoc.setPriority(doc.getPriority());
                        extractedDoc.setParseStatus(ParseStatus.PENDING);
                        extractedDoc.setRetryCount(0);

                        documentRepository.save(extractedDoc);
                        extractedFiles.add(extractedDoc);

                        // 发送解析消息
                        sendParseMessage(extractedDoc, message.getPassword());
                    }
                }
            }

            // 标记ZIP文件为已解析
            doc.setParseStatus(ParseStatus.PARSED);
            doc.setParseErrorCode(0);
            doc.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(doc);

            // 索引ZIP文件（不含内容，只有附件列表）
            DocumentIndex index = toDocumentIndex(doc, null, null);
            index.setAttachments(extractedFiles.stream()
                    .map(f -> DocumentIndex.AttachmentInfo.builder()
                            .name(f.getFileName())
                            .size(f.getFileSize())
                            .fileId(f.getFileId())
                            .build())
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
        doc.setParseStatus(ParseStatus.PARSED);
        doc.setParseErrorCode(0);
        doc.setParseErrorMessage(null);
        doc.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(doc);

        // 索引到ES
        DocumentIndex index = toDocumentIndex(doc, result.content(), result.metadata());
        documentIndexService.save(index);

        // 处理附件（EML文件）
        if (DocumentType.EML.name().equalsIgnoreCase(doc.getFileType())) {
            processEmailAttachments(doc, result.metadata());
        }

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
        documentRepository.save(doc);

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
        documentRepository.save(doc);

        // 可重试错误，发送重试消息
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

        for (Map<String, Object> attachment : attachments) {
            String fileId = UUID.randomUUID().toString().replace("-", "");
            String fileName = (String) attachment.get("name");
            Integer size = (Integer) attachment.get("size");
            String fileType = getFileType(fileName);

            DocumentEntity attachDoc = new DocumentEntity();
            attachDoc.setFileId(fileId);
            attachDoc.setUserId(doc.getUserId());
            attachDoc.setParentId(doc.getFileId());
            attachDoc.setRootId(rootId);
            attachDoc.setFileName(fileName);
            attachDoc.setFileSize(size != null ? size.longValue() : 0L);
            attachDoc.setFileStoragePath((String) attachment.get("storagePath"));
            attachDoc.setFileType(fileType);
            attachDoc.setIsAttachment(true);
            attachDoc.setPriority(doc.getPriority());
            attachDoc.setParseStatus(ParseStatus.PENDING);
            attachDoc.setRetryCount(0);

            documentRepository.save(attachDoc);

            // 发送解析消息
            ParseMessageDTO attachMessage = ParseMessageDTO.builder()
                    .fileId(fileId)
                    .parentId(doc.getFileId())
                    .rootId(rootId)
                    .fileStoragePath(attachDoc.getFileStoragePath())
                    .fileType(fileType)
                    .fileName(fileName)
                    .isAttachment(true)
                    .priority(doc.getPriority())
                    .retryCount(0)
                    .timestamp(LocalDateTime.now())
                    .userId(doc.getUserId())
                    .build();

            producer.send(attachMessage);
        }

        log.info("邮件附件处理完成: parentId={}, attachmentCount={}",
                doc.getFileId(), attachments.size());
    }

    /**
     * 发送解析消息
     */
    private void sendParseMessage(DocumentEntity doc, String password) {
        ParseMessageDTO message = ParseMessageDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileStoragePath(doc.getFileStoragePath())
                .fileType(doc.getFileType())
                .fileName(doc.getFileName())
                .isAttachment(doc.getIsAttachment())
                .password(password)
                .priority(doc.getPriority())
                .retryCount(doc.getRetryCount())
                .timestamp(LocalDateTime.now())
                .userId(doc.getUserId())
                .build();

        producer.send(message);
    }

    /**
     * 转换为ParseMessageDTO
     */
    private ParseMessageDTO toMessage(DocumentEntity doc) {
        return ParseMessageDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileStoragePath(doc.getFileStoragePath())
                .fileType(doc.getFileType())
                .fileName(doc.getFileName())
                .isAttachment(doc.getIsAttachment())
                .password(doc.getPasswordProvided())
                .priority(doc.getPriority())
                .retryCount(doc.getRetryCount())
                .timestamp(LocalDateTime.now())
                .userId(doc.getUserId())
                .build();
    }

    /**
     * 转换为DocumentIndex
     */
    private DocumentIndex toDocumentIndex(DocumentEntity doc, String content, Map<String, Object> metadata) {
        DocumentIndex.DocumentIndexBuilder builder = DocumentIndex.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileName(doc.getFileName())
                .fileNameKeyword(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileStoragePath(doc.getFileStoragePath())
                .fileType(doc.getFileType())
                .parseStatus(doc.getParseStatus())
                .parseErrorCode(doc.getParseErrorCode())
                .parseErrorMessage(doc.getParseErrorMessage())
                .hasPassword(doc.getParseErrorCode() != null
                        && doc.getParseErrorCode() == ParseErrorCode.PASSWORD_PROTECTED.getCode())
                .isAttachment(doc.getIsAttachment())
                .priority(doc.getPriority())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt());

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
     * 保存文件到存储服务
     */
    private String saveToStorage(String fileName, InputStream content, Long userId) {
        return fileStorageService.saveFile(fileName, content, userId);
    }
}
