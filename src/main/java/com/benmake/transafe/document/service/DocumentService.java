package com.benmake.transafe.document.service;

import com.benmake.transafe.document.common.constant.ParseStatus;
import com.benmake.transafe.document.common.enums.ParseErrorCode;
import com.benmake.transafe.document.dto.DocumentDTO;
import com.benmake.transafe.document.dto.DocumentTreeDTO;
import com.benmake.transafe.document.dto.ParseMessageDTO;
import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.document.mq.DocumentParseProducer;
import com.benmake.transafe.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档服务
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentParseProducer producer;

    /**
     * 创建文档记录
     *
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param storagePath 存储路径
     * @param fileType 文件类型
     * @param userId 用户ID
     * @param isVip 是否为VIP
     * @return 文档DTO
     */
    @Transactional
    public DocumentDTO createDocument(String fileName, Long fileSize, String storagePath,
                                     String fileType, Long userId, boolean isVip) {
        String fileId = UUID.randomUUID().toString().replace("-", "");

        DocumentEntity doc = new DocumentEntity();
        doc.setFileId(fileId);
        doc.setFileName(fileName);
        doc.setFileSize(fileSize);
        doc.setFileStoragePath(storagePath);
        doc.setFileType(fileType);
        doc.setParseStatus(ParseStatus.PENDING);
        doc.setIsAttachment(false);
        doc.setPriority(isVip ? 1 : 0);
        doc.setRetryCount(0);
        doc.setParseErrorCode(0);

        doc = documentRepository.save(doc);

        // 发送解析消息
        ParseMessageDTO message = ParseMessageDTO.builder()
                .fileId(fileId)
                .parentId(null)
                .rootId(null)
                .fileStoragePath(storagePath)
                .fileType(fileType)
                .fileName(fileName)
                .isAttachment(false)
                .priority(isVip ? 1 : 0)
                .retryCount(0)
                .timestamp(LocalDateTime.now())
                .build();

        producer.send(message);

        log.info("文档创建成功: fileId={}, fileName={}, isVip={}", fileId, fileName, isVip);

        return toDTO(doc);
    }

    /**
     * 获取文档详情
     *
     * @param fileId 文件唯一标识
     * @return 文档DTO
     */
    public DocumentDTO getDocument(String fileId) {
        DocumentEntity doc = documentRepository.findByFileId(fileId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + fileId));
        return toDTO(doc);
    }

    /**
     * 获取文档状态
     *
     * @param fileId 文件唯一标识
     * @return 文档DTO
     */
    public DocumentDTO getDocumentStatus(String fileId) {
        return getDocument(fileId);
    }

    /**
     * 获取文档树
     *
     * @param fileId 文件唯一标识
     * @return 文档树DTO
     */
    public DocumentTreeDTO getDocumentTree(String fileId) {
        DocumentEntity doc = documentRepository.findByFileId(fileId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + fileId));
        return buildTree(doc);
    }

    /**
     * 密码重试解析
     *
     * @param fileId 文件唯一标识
     * @param password 密码
     */
    @Transactional
    public void retryWithPassword(String fileId, String password) {
        DocumentEntity doc = documentRepository.findByFileId(fileId)
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
        documentRepository.save(doc);

        // 发送重试消息（不走优先队列）
        producer.sendForRetry(toMessage(doc));

        log.info("密码重试已提交: fileId={}, retryCount={}", fileId, doc.getRetryCount());
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
        List<DocumentEntity> children = documentRepository.findByParentId(doc.getFileId());
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
                .build();
    }
}
