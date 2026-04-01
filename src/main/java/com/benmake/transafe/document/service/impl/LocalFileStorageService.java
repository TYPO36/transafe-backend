package com.benmake.transafe.document.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.document.dto.FileInfoResponse;
import com.benmake.transafe.document.dto.FileUploadResponse;
import com.benmake.transafe.file.config.FileStorageConfig;
import com.benmake.transafe.infra.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 本地文件存储服务
 *
 * <p>提供文件的本地存储、下载、删除等核心功能。文件存储结构如下：</p>
 * <pre>
 * {local-path}/
 *   └── user_{userId}/
 *       └── {yyyy}/
 *           └── {MM}/
 *               └── {dd}/
 *                   └── {fileId}
 * </pre>
 *
 * <h3>文件存储路径设计说明</h3>
 * <ul>
 *   <li>按用户分目录：便于管理和清理用户文件</li>
 *   <li>按日期分子目录：避免单目录文件过多，提高检索效率</li>
 *   <li>使用 UUID 作为文件名：避免文件名冲突和安全风险</li>
 * </ul>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    /**
     * 文档数据访问层
     * 用于操作 document 表，存储文件元数据信息（合并后的表）
     */
    private final DocumentMapper documentMapper;

    /**
     * 文件存储配置
     * 从 application.yaml 读取 file-storage.local-path 配置
     */
    private final FileStorageConfig fileStorageConfig;

    /**
     * 日期路径格式化器
     * 用于生成按日期分类的存储路径，格式：yyyy/MM/dd
     */
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 上传文件到本地存储
     *
     * <p>处理流程：
     * <ol>
     *   <li>生成唯一文件 ID（32位 UUID，无横杠）</li>
     *   <li>计算存储路径：user_{userId}/yyyy/MM/dd/{fileId}</li>
     *   <li>创建目标目录（如不存在）</li>
     *   <li>保存文件到磁盘</li>
     *   <li>保存文件元数据到数据库（document 表）</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResponse uploadFile(MultipartFile file, Long userId) {
        String fileId = generateFileId();
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);

        String datePath = LocalDateTime.now().format(DATE_PATH_FORMATTER);
        String relativePath = "user_" + userId + "/" + datePath + "/" + fileId;

        try {
            Path targetDir = Paths.get(fileStorageConfig.getLocalPath(), "user_" + userId, datePath);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(fileId);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            LocalDateTime now = LocalDateTime.now();
            DocumentEntity doc = new DocumentEntity();
            doc.setFileId(fileId);
            doc.setUserId(userId);
            doc.setFileName(originalFilename);
            doc.setFileSize(file.getSize());
            doc.setFileType(fileType);
            doc.setStoragePath(relativePath);
            doc.setStatus("UPLOADED");
            doc.setParseStatus("pending");
            doc.setIsAttachment(false);
            doc.setPriority(0);
            doc.setRetryCount(0);
            doc.setParseErrorCode(0);
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);
            documentMapper.insert(doc);

            log.info("文件上传成功: fileId={}, fileName={}, size={}, userId={}",
                    fileId, originalFilename, file.getSize(), userId);

            return FileUploadResponse.builder()
                    .fileId(fileId)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .storagePath(relativePath)
                    .build();

        } catch (IOException e) {
            log.error("文件保存失败: fileId={}, error={}", fileId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
    }

    /**
     * 下载文件
     */
    public Resource downloadFile(String fileId, Long userId) {
        DocumentEntity doc = getDocumentEntity(fileId, userId);
        Path filePath = Paths.get(fileStorageConfig.getLocalPath(), doc.getStoragePath());

        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            log.error("文件路径解析失败: path={}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败");
        }
    }

    /**
     * 获取文件信息
     */
    public FileInfoResponse getFileInfo(String fileId, Long userId) {
        DocumentEntity doc = getDocumentEntity(fileId, userId);
        return FileInfoResponse.builder()
                .fileId(doc.getFileId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * 获取用户的文件列表（分页）
     */
    public Map<String, Object> listFiles(Long userId, int pageNum, int size) {
        Page<DocumentEntity> page = new Page<>(pageNum, size);
        IPage<DocumentEntity> docEntities = documentMapper.selectPage(page, null);

        Map<String, Object> result = new HashMap<>();
        result.put("content", docEntities.getRecords().stream()
                .filter(d -> d.getUserId().equals(userId) && "UPLOADED".equals(d.getStatus()))
                .map(doc -> FileInfoResponse.builder()
                        .fileId(doc.getFileId())
                        .fileName(doc.getFileName())
                        .fileSize(doc.getFileSize())
                        .fileType(doc.getFileType())
                        .status(doc.getStatus())
                        .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
                        .build())
                .toList());
        result.put("totalElements", docEntities.getTotal());
        result.put("totalPages", docEntities.getPages());
        result.put("size", docEntities.getSize());
        result.put("number", docEntities.getCurrent() - 1);
        result.put("first", docEntities.getCurrent() == 1);
        result.put("last", docEntities.getCurrent() == docEntities.getPages());
        result.put("empty", docEntities.getRecords().isEmpty());

        return result;
    }

    /**
     * 删除文件（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileId, Long userId) {
        DocumentEntity doc = getDocumentEntity(fileId, userId);

        Path filePath = Paths.get(fileStorageConfig.getLocalPath(), doc.getStoragePath());
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("物理文件删除成功: path={}", filePath);
            }
        } catch (IOException e) {
            log.error("物理文件删除失败: path={}, error={}", filePath, e.getMessage(), e);
        }

        doc.setStatus("DELETED");
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        log.info("文件删除成功: fileId={}, userId={}", fileId, userId);
    }

    /**
     * 更新文件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public FileInfoResponse updateFileInfo(String fileId, Long userId, String newFileName) {
        DocumentEntity doc = getDocumentEntity(fileId, userId);

        String oldName = doc.getFileName();
        doc.setFileName(newFileName);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        log.info("文件名更新: fileId={}, oldName={}, newName={}", fileId, oldName, newFileName);

        return FileInfoResponse.builder()
                .fileId(doc.getFileId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * 获取文档实体（内部使用）
     */
    public DocumentEntity getDocumentEntity(String fileId, Long userId) {
        return documentMapper.findByFileId(fileId)
                .filter(d -> d.getUserId().equals(userId) && "UPLOADED".equals(d.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    /**
     * 生成唯一文件 ID
     */
    private String generateFileId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 根据存储路径获取文件输入流
     */
    public InputStream getFileInputStream(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存储路径不能为空");
        }

        Path filePath = Paths.get(fileStorageConfig.getLocalPath(), storagePath);
        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "文件不存在: " + storagePath);
        }

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("读取文件失败: path={}, error={}", filePath, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败");
        }
    }

    /**
     * 根据文件 ID 获取文件输入流
     */
    public InputStream getFileInputStreamById(String fileId, Long userId) {
        DocumentEntity doc = documentMapper.findByFileId(fileId)
                .filter(d -> d.getUserId().equals(userId) && "UPLOADED".equals(d.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        return getFileInputStream(doc.getStoragePath());
    }

    /**
     * 保存文件内容并返回存储路径
     */
    public String saveFile(String fileName, InputStream content, Long userId) {
        return saveFileAndGetDocumentEntity(fileName, content, userId).getStoragePath();
    }

    /**
     * 保存文件内容并返回文档实体
     */
    public DocumentEntity saveFileAndGetDocumentEntity(String fileName, InputStream content, Long userId) {
        String fileId = generateFileId();
        String fileType = getFileExtension(fileName);
        String datePath = LocalDateTime.now().format(DATE_PATH_FORMATTER);
        String relativePath = "user_" + userId + "/" + datePath + "/" + fileId;

        try {
            byte[] contentBytes = content.readAllBytes();
            long fileSize = contentBytes.length;

            Path targetDir = Paths.get(fileStorageConfig.getLocalPath(), "user_" + userId, datePath);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(fileId);
            Files.write(targetPath, contentBytes);

            LocalDateTime now = LocalDateTime.now();
            DocumentEntity doc = new DocumentEntity();
            doc.setFileId(fileId);
            doc.setUserId(userId);
            doc.setFileName(fileName);
            doc.setFileSize(fileSize);
            doc.setFileType(fileType);
            doc.setStoragePath(relativePath);
            doc.setStatus("UPLOADED");
            doc.setParseStatus("pending");
            doc.setIsAttachment(false);
            doc.setPriority(0);
            doc.setRetryCount(0);
            doc.setParseErrorCode(0);
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);
            documentMapper.insert(doc);

            log.info("文件保存成功: fileId={}, fileName={}, size={}, path={}", fileId, fileName, fileSize, relativePath);
            return doc;
        } catch (IOException e) {
            log.error("文件保存失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
    }
}
