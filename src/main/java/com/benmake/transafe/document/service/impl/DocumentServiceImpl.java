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
import com.benmake.transafe.file.entity.FileEntity;
import com.benmake.transafe.file.service.FileProxyService;
import com.benmake.transafe.infra.mapper.FileMapper;
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
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public DocumentDTO createDocumentByFileId(String fileId, Long userId, boolean isVip) {
        return doCreateDocument(fileId, null, userId, isVip);
    }

    @Override
    @Transactional
    public DocumentDTO createDocumentByFileId(String fileId, Long userId, boolean isVip, String rootId) {
        return doCreateDocument(fileId, rootId, userId, isVip);
    }

    /**
     * 内部方法：创建文档记录
     *
     * @param fileId 文件ID（关联 file 表）
     * @param rootId 根文档ID
     * @param userId 用户ID
     * @param isVip 是否为VIP
     * @return 文档DTO
     */
    private DocumentDTO doCreateDocument(String fileId, String rootId, Long userId, boolean isVip) {
        LocalDateTime now = LocalDateTime.now();
        DocumentEntity doc = new DocumentEntity();
        doc.setFileId(fileId);
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

        // 获取文件信息用于发送解析消息
        FileEntity fileEntity = fileMapper.findByFileId(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + fileId));

        // 发送解析消息
        ParseMessageDTO message = ParseMessageDTO.builder()
                .fileId(fileId)
                .parentId(null)
                .rootId(doc.getRootId())
                .fileStoragePath(fileEntity.getStoragePath())
                .fileType(fileEntity.getFileType())
                .fileName(fileEntity.getFileName())
                .isAttachment(false)
                .priority(isVip ? 1 : 0)
                .retryCount(0)
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .build();

        producer.send(message);

        log.info("文档创建成功: fileId={}, rootId={}, userId={}, isVip={}", fileId, doc.getRootId(), userId, isVip);

        return toDTO(fileEntity, doc);
    }

    @Override
    @Transactional
    public DocumentDTO uploadAndCreateDocument(MultipartFile file, Long userId, boolean isVip) {
        // 1. 先上传文件到存储服务
        FileUploadResponse fileResponse = fileProxyService.uploadFile(file, userId);

        // 2. 创建文档记录
        return createDocumentByFileId(fileResponse.getFileId(), userId, isVip);
    }

    @Override
    @Transactional
    public BatchUploadResponse batchUploadAndCreateDocument(MultipartFile[] files, Long userId, boolean isVip) {
        List<BatchUploadResponse.UploadItem> items = new ArrayList<>();
        int success = 0;
        int failed = 0;

        // 为批量上传创建根文档，用于跟踪整个批次的解析进度
        // 注意：批量根文档没有对应的 file 记录，只是一个虚拟的根节点
        String batchRootId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        DocumentEntity batchRoot = new DocumentEntity();
        batchRoot.setFileId(batchRootId);
        batchRoot.setParseStatus(ParseStatus.PENDING);
        batchRoot.setIsAttachment(false);
        batchRoot.setPriority(isVip ? 1 : 0);
        batchRoot.setRetryCount(0);
        batchRoot.setParseErrorCode(0);
        batchRoot.setCreatedAt(now);
        batchRoot.setUpdatedAt(now);
        // 批量根文档的 rootId 就是自己
        batchRoot.setRootId(batchRootId);
        documentMapper.insert(batchRoot);

        for (MultipartFile file : files) {
            try {
                // 上传文件
                FileUploadResponse fileResponse = fileProxyService.uploadFile(file, userId);
                // 创建文档记录，设置 rootId 指向批量根文档
                DocumentDTO doc = createDocumentByFileId(
                        fileResponse.getFileId(),
                        userId,
                        isVip,
                        batchRootId
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
        return documentMapper.findByFileIdWithFile(fileId)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + fileId));
    }

    @Override
    public DocumentDTO getDocumentStatus(String fileId) {
        return getDocument(fileId);
    }

    @Override
    public DocumentTreeDTO getDocumentTree(String fileId) {
        return documentMapper.findByFileIdWithFile(fileId)
                .map(this::buildTree)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + fileId));
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
    private DocumentTreeDTO buildTree(com.benmake.transafe.document.vo.DocumentVO vo) {
        DocumentTreeDTO.DocumentTreeDTOBuilder builder = DocumentTreeDTO.builder()
                .fileId(vo.getFileId())
                .parentId(vo.getParentId())
                .rootId(vo.getRootId())
                .fileName(vo.getFileName())
                .fileSize(vo.getFileSize())
                .fileType(vo.getFileType())
                .parseStatus(vo.getParseStatus())
                .parseErrorCode(vo.getParseErrorCode())
                .parseErrorMessage(vo.getParseErrorMessage())
                .hasPassword(vo.getParseErrorCode() != null
                        && vo.getParseErrorCode() == ParseErrorCode.PASSWORD_PROTECTED.getCode())
                .isAttachment(vo.getIsAttachment())
                .priority(vo.getPriority());

        // 递归获取子文档
        List<DocumentEntity> children = documentMapper.findByParentId(vo.getFileId());
        if (!children.isEmpty()) {
            // 子文档需要获取文件信息
            List<DocumentTreeDTO> childTrees = children.stream()
                    .map(childDoc -> {
                        // 每个子文档单独查询文件信息
                        return documentMapper.findByFileIdWithFile(childDoc.getFileId())
                                .map(this::buildTree)
                                .orElse(null);
                    })
                    .filter(tree -> tree != null)
                    .collect(Collectors.toList());
            builder.children(childTrees);
        }

        return builder.build();
    }

    /**
     * 转换为DTO
     */
    private DocumentDTO toDTO(FileEntity fileEntity, DocumentEntity doc) {
        return DocumentDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileName(fileEntity.getFileName())
                .fileSize(fileEntity.getFileSize())
                .fileType(fileEntity.getFileType())
                .fileStoragePath(fileEntity.getStoragePath())
                .parseStatus(doc.getParseStatus())
                .parseErrorCode(doc.getParseErrorCode())
                .parseErrorMessage(doc.getParseErrorMessage())
                .hasPassword(doc.getParseErrorCode() != null
                        && doc.getParseErrorCode() == ParseErrorCode.PASSWORD_PROTECTED.getCode())
                .isAttachment(doc.getIsAttachment())
                .priority(doc.getPriority())
                .content(doc.getContent())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    /**
     * 转换为DTO（从 DocumentVO）
     */
    private DocumentDTO toDTO(com.benmake.transafe.document.vo.DocumentVO vo) {
        return DocumentDTO.builder()
                .fileId(vo.getFileId())
                .parentId(vo.getParentId())
                .rootId(vo.getRootId())
                .fileName(vo.getFileName())
                .fileSize(vo.getFileSize())
                .fileType(vo.getFileType())
                .fileStoragePath(vo.getStoragePath())
                .parseStatus(vo.getParseStatus())
                .parseErrorCode(vo.getParseErrorCode())
                .parseErrorMessage(vo.getParseErrorMessage())
                .hasPassword(vo.getParseErrorCode() != null
                        && vo.getParseErrorCode() == ParseErrorCode.PASSWORD_PROTECTED.getCode())
                .isAttachment(vo.getIsAttachment())
                .priority(vo.getPriority())
                .content(vo.getContent())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }

    /**
     * 转换为ParseMessageDTO
     */
    private ParseMessageDTO toMessage(DocumentEntity doc) {
        FileEntity fileEntity = fileMapper.findByFileId(doc.getFileId())
                .orElseThrow(() -> new RuntimeException("文件不存在: " + doc.getFileId()));

        return ParseMessageDTO.builder()
                .fileId(doc.getFileId())
                .parentId(doc.getParentId())
                .rootId(doc.getRootId())
                .fileStoragePath(fileEntity.getStoragePath())
                .fileType(fileEntity.getFileType())
                .fileName(fileEntity.getFileName())
                .isAttachment(doc.getIsAttachment())
                .password(doc.getPasswordProvided())
                .priority(doc.getPriority())
                .retryCount(doc.getRetryCount())
                .timestamp(LocalDateTime.now())
                .userId(fileEntity.getUserId())
                .build();
    }
}
