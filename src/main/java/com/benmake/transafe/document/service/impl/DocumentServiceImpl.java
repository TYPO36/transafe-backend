package com.benmake.transafe.document.service.impl;

import com.benmake.transafe.document.common.constant.ParseStatus;
import com.benmake.transafe.document.common.enums.ParseErrorCode;
import com.benmake.transafe.document.dto.BatchUploadResponse;
import com.benmake.transafe.document.dto.DocumentDTO;
import com.benmake.transafe.document.dto.DocumentTreeDTO;
import com.benmake.transafe.document.dto.ParseMessageDTO;
import com.benmake.transafe.document.dto.ParseProgressDTO;
import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.document.mq.DocumentParseProducer;
import com.benmake.transafe.infra.mapper.DocumentMapper;
import com.benmake.transafe.document.service.DocumentService;
import com.benmake.transafe.file.dto.FileUploadResponse;
import com.benmake.transafe.file.service.FileProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档服务实现
 *
 * @author JTP
 * @date 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentParseProducer producer;
    private final FileProxyService fileProxyService;

    @Override
    @Transactional
    public DocumentDTO createDocument(String fileName, Long fileSize, String storagePath,
                                     String fileType, Long userId, boolean isVip) {
        // 单文件上传时，rootId 设置为自身的 fileId
        String fileId = UUID.randomUUID().toString().replace("-", "");
        return doCreateDocument(fileId, null, fileName, fileSize, storagePath, fileType, userId, isVip);
    }

    @Override
    @Transactional
    public DocumentDTO createDocument(String fileName, Long fileSize, String storagePath,
                                     String fileType, Long userId, boolean isVip, String rootId) {
        String fileId = UUID.randomUUID().toString().replace("-", "");
        return doCreateDocument(fileId, rootId, fileName, fileSize, storagePath, fileType, userId, isVip);
    }

    /**
     * 内部方法：创建文档记录
     */
    private DocumentDTO doCreateDocument(String fileId, String rootId, String fileName, Long fileSize,
                                       String storagePath, String fileType, Long userId, boolean isVip) {
        LocalDateTime now = LocalDateTime.now();
        DocumentEntity doc = new DocumentEntity();
        doc.setFileId(fileId);
        doc.setUserId(userId);
        doc.setFileName(fileName);
        doc.setFileSize(fileSize);
        doc.setFileStoragePath(storagePath);
        doc.setFileType(fileType);
        doc.setParseStatus(ParseStatus.PENDING);
        doc.setIsAttachment(false);
        doc.setPriority(isVip ? 1 : 0);
        doc.setRetryCount(0);
        doc.setParseErrorCode(0);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        // rootId：如果指定了就用指定的，否则就是自己的 fileId（单文件场景）
        doc.setRootId(rootId != null ? rootId : fileId);

        documentMapper.insert(doc);

        // 发送解析消息
        ParseMessageDTO message = ParseMessageDTO.builder()
                .fileId(fileId)
                .parentId(null)
                .rootId(doc.getRootId())
                .fileStoragePath(storagePath)
                .fileType(fileType)
                .fileName(fileName)
                .isAttachment(false)
                .priority(isVip ? 1 : 0)
                .retryCount(0)
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .build();

        producer.send(message);

        log.info("文档创建成功: fileId={}, rootId={}, fileName={}, isVip={}", fileId, doc.getRootId(), fileName, isVip);

        return toDTO(doc);
    }

    @Override
    @Transactional
    public DocumentDTO uploadAndCreateDocument(MultipartFile file, Long userId, boolean isVip) {
        // 1. 先上传文件到存储服务
        FileUploadResponse fileResponse = fileProxyService.uploadFile(file, userId);

        // 2. 创建文档记录
        return createDocument(
                fileResponse.getFileName(),
                fileResponse.getFileSize(),
                fileResponse.getFileId(),
                fileResponse.getFileType(),
                userId,
                isVip
        );
    }

    @Override
    @Transactional
    public BatchUploadResponse batchUploadAndCreateDocument(MultipartFile[] files, Long userId, boolean isVip) {
        List<BatchUploadResponse.UploadItem> items = new ArrayList<>();
        int success = 0;
        int failed = 0;

        // 为批量上传创建根文档，用于跟踪整个批次的解析进度
        String batchRootId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        DocumentEntity batchRoot = new DocumentEntity();
        batchRoot.setFileId(batchRootId);
        batchRoot.setUserId(userId);
        batchRoot.setFileName("批量上传_" + System.currentTimeMillis());
        batchRoot.setFileSize(0L);
        batchRoot.setFileStoragePath(null);
        batchRoot.setFileType("batch");
        batchRoot.setParseStatus(ParseStatus.PENDING);
        batchRoot.setIsAttachment(false);
        batchRoot.setPriority(isVip ? 1 : 0);
        batchRoot.setRetryCount(0);
        batchRoot.setParseErrorCode(0);
        batchRoot.setCreatedAt(now);
        batchRoot.setUpdatedAt(now);
        documentMapper.insert(batchRoot);

        for (MultipartFile file : files) {
            try {
                // 上传文件并创建文档记录
                FileUploadResponse fileResponse = fileProxyService.uploadFile(file, userId);
                DocumentDTO doc = createDocument(
                        fileResponse.getFileName(),
                        fileResponse.getFileSize(),
                        fileResponse.getFileId(),
                        fileResponse.getFileType(),
                        userId,
                        isVip,
                        batchRootId // 设置 rootId 指向批量根文档
                );

                items.add(BatchUploadResponse.UploadItem.builder()
                        .fileId(doc.getFileId())
                        .fileName(doc.getFileName())
                        .status(doc.getParseStatus())
                        .build());
                success++;
            } catch (Exception e) {
                log.error("批量上传失败: fileName={}", file.getOriginalFilename(), e);
                failed++;
            }
        }

        return BatchUploadResponse.builder()
                .rootId(batchRootId)
                .total(files.length)
                .success(success)
                .failed(failed)
                .items(items)
                .build();
    }

    @Override
    public DocumentDTO getDocument(String fileId) {
        DocumentEntity doc = documentMapper.findByFileId(fileId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + fileId));
        return toDTO(doc);
    }

    @Override
    public DocumentDTO getDocumentStatus(String fileId) {
        return getDocument(fileId);
    }

    @Override
    public DocumentTreeDTO getDocumentTree(String fileId) {
        DocumentEntity doc = documentMapper.findByFileId(fileId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + fileId));
        return buildTree(doc);
    }

    @Override
    @Transactional
    public void retryWithPassword(String fileId, String password) {
        DocumentEntity doc = documentMapper.findByFileId(fileId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + fileId));

        if (doc.getParseErrorCode() == null
                || doc.getParseErrorCode() != ParseErrorCode.PASSWORD_PROTECTED.getCode()) {
            throw new RuntimeException("该文档不是密码保护文件，无法重试");
        }

        doc.setPasswordProvided(password);
        doc.setRetryCount(doc.getRetryCount() + 1);
        doc.setParseStatus(ParseStatus.PENDING);
        doc.setParseErrorCode(0);
        doc.setParseErrorMessage(null);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        // 发送重试消息（不走优先队列）
        producer.sendForRetry(toMessage(doc));

        log.info("密码重试已提交: fileId={}, retryCount={}", fileId, doc.getRetryCount());
    }

    @Override
    public ParseProgressDTO getParseProgress(String rootId) {
        // 统计根文档下的所有文档
        long total = documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.PENDING)
                + documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.PARSING)
                + documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.PARSED)
                + documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.FAILED);

        long pending = documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.PENDING);
        long parsing = documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.PARSING);
        long success = documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.PARSED);
        long failed = documentMapper.countByRootIdAndParseStatus(rootId, ParseStatus.FAILED);

        int progress = total > 0 ? (int) ((success + failed) * 100 / total) : 0;
        boolean completed = total > 0 && (pending + parsing) == 0;

        return ParseProgressDTO.builder()
                .rootId(rootId)
                .total(total)
                .pending(pending)
                .parsing(parsing)
                .success(success)
                .failed(failed)
                .progress(progress)
                .completed(completed)
                .build();
    }

    /**
     * 构建文档树
     */
    private DocumentTreeDTO buildTree(DocumentEntity doc) {
        DocumentTreeDTO.DocumentTreeDTOBuilder builder = DocumentTreeDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .parseStatus(doc.getParseStatus())
                .parseErrorCode(doc.getParseErrorCode())
                .parseErrorMessage(doc.getParseErrorMessage())
                .hasPassword(doc.getParseErrorCode() != null
                        && doc.getParseErrorCode() == ParseErrorCode.PASSWORD_PROTECTED.getCode())
                .isAttachment(doc.getIsAttachment())
                .priority(doc.getPriority());

        // 递归获取子文档
        List<DocumentEntity> children = documentMapper.findByParentId(doc.getFileId());
        if (!children.isEmpty()) {
            builder.children(children.stream()
                    .map(this::buildTree)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * 转换为DTO
     */
    private DocumentDTO toDTO(DocumentEntity doc) {
        return DocumentDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .fileStoragePath(doc.getFileStoragePath())
                .parseStatus(doc.getParseStatus())
                .parseErrorCode(doc.getParseErrorCode())
                .parseErrorMessage(doc.getParseErrorMessage())
                .hasPassword(doc.getParseErrorCode() != null
                        && doc.getParseErrorCode() == ParseErrorCode.PASSWORD_PROTECTED.getCode())
                .isAttachment(doc.getIsAttachment())
                .priority(doc.getPriority())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
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
}