package com.benmake.transafe.file.service;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.file.config.FileStorageConfig;
import com.benmake.transafe.file.dto.FileInfoResponse;
import com.benmake.transafe.file.dto.FileUploadResponse;
import com.benmake.transafe.file.entity.FileEntity;
import com.benmake.transafe.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.UUID;

/**
 * 本地文件存储服务
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final FileRepository fileRepository;
    private final FileStorageConfig fileStorageConfig;

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 上传文件
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, Long userId) {
        // 生成唯一文件ID
        String fileId = generateFileId();
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);

        // 计算存储路径：user_{userId}/yyyy/MM/dd/fileId
        String datePath = LocalDateTime.now().format(DATE_PATH_FORMATTER);
        String relativePath = "user_" + userId + "/" + datePath + "/" + fileId;

        try {
            // 确保目录存在
            Path targetDir = Paths.get(fileStorageConfig.getLocalPath(), "user_" + userId, datePath);
            Files.createDirectories(targetDir);

            // 保存文件
            Path targetPath = targetDir.resolve(fileId);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 保存文件元数据
            FileEntity fileEntity = FileEntity.builder()
                    .fileId(fileId)
                    .userId(userId)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .storagePath(relativePath)
                    .status("UPLOADED")
                    .build();
            fileEntity.prePersist();
            fileRepository.save(fileEntity);

            log.info("文件上传成功: fileId={}, fileName={}, size={}, userId={}",
                    fileId, originalFilename, file.getSize(), userId);

            return FileUploadResponse.builder()
                    .fileId(fileId)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .fileType(fileType)
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
        FileEntity fileEntity = getFileEntity(fileId, userId);

        Path filePath = Paths.get(fileStorageConfig.getLocalPath(), fileEntity.getStoragePath());
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
        FileEntity fileEntity = getFileEntity(fileId, userId);

        return FileInfoResponse.builder()
                .fileId(fileEntity.getFileId())
                .fileName(fileEntity.getFileName())
                .fileSize(fileEntity.getFileSize())
                .fileType(fileEntity.getFileType())
                .status(fileEntity.getStatus())
                .createdAt(fileEntity.getCreatedAt() != null ? fileEntity.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * 获取文件列表（分页）
     */
    public Page<FileInfoResponse> listFiles(Long userId, Pageable pageable) {
        Page<FileEntity> fileEntities = fileRepository.findByUserIdAndStatus(userId, "UPLOADED", pageable);

        return fileEntities.map(entity -> FileInfoResponse.builder()
                .fileId(entity.getFileId())
                .fileName(entity.getFileName())
                .fileSize(entity.getFileSize())
                .fileType(entity.getFileType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .build());
    }

    /**
     * 删除文件
     */
    @Transactional
    public void deleteFile(String fileId, Long userId) {
        FileEntity fileEntity = getFileEntity(fileId, userId);

        // 删除物理文件
        Path filePath = Paths.get(fileStorageConfig.getLocalPath(), fileEntity.getStoragePath());
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("物理文件删除成功: path={}", filePath);
            }
        } catch (IOException e) {
            log.error("物理文件删除失败: path={}, error={}", filePath, e.getMessage(), e);
            // 继续执行，更新数据库状态
        }

        // 软删除：更新状态
        fileEntity.setStatus("DELETED");
        fileEntity.preUpdate();
        fileRepository.save(fileEntity);

        log.info("文件删除成功: fileId={}, userId={}", fileId, userId);
    }

    /**
     * 更新文件信息
     */
    @Transactional
    public FileInfoResponse updateFileInfo(String fileId, Long userId, String newFileName) {
        FileEntity fileEntity = getFileEntity(fileId, userId);

        String oldName = fileEntity.getFileName();
        fileEntity.setFileName(newFileName);
        fileEntity.preUpdate();
        fileRepository.save(fileEntity);

        log.info("文件名更新: fileId={}, oldName={}, newName={}", fileId, oldName, newFileName);

        return FileInfoResponse.builder()
                .fileId(fileEntity.getFileId())
                .fileName(fileEntity.getFileName())
                .fileSize(fileEntity.getFileSize())
                .fileType(fileEntity.getFileType())
                .status(fileEntity.getStatus())
                .createdAt(fileEntity.getCreatedAt() != null ? fileEntity.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * 获取文件元数据（内部使用）
     */
    public FileEntity getFileEntity(String fileId, Long userId) {
        return fileRepository.findByFileIdAndUserId(fileId, userId)
                .filter(f -> "UPLOADED".equals(f.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    /**
     * 生成唯一文件ID
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

    // ==================== 供解析器使用的方法 ====================

    /**
     * 根据存储路径获取文件输入流（供解析器使用）
     *
     * @param storagePath 存储路径（相对路径）
     * @return 文件输入流
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
     * 根据文件ID获取文件输入流（供解析器使用）
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件输入流
     */
    public InputStream getFileInputStreamById(String fileId, Long userId) {
        FileEntity fileEntity = fileRepository.findByFileIdAndUserId(fileId, userId)
                .filter(f -> "UPLOADED".equals(f.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        return getFileInputStream(fileEntity.getStoragePath());
    }

    /**
     * 保存文件内容并返回存储路径（供解析器使用，如ZIP解压、邮件附件保存）
     *
     * @param fileName 文件名
     * @param content 文件内容流
     * @param userId 用户ID
     * @return 存储路径
     */
    public String saveFile(String fileName, InputStream content, Long userId) {
        String fileId = generateFileId();
        String datePath = LocalDateTime.now().format(DATE_PATH_FORMATTER);
        String relativePath = "user_" + userId + "/" + datePath + "/" + fileId;

        try {
            Path targetDir = Paths.get(fileStorageConfig.getLocalPath(), "user_" + userId, datePath);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(fileId);
            Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件保存成功: fileId={}, fileName={}, path={}", fileId, fileName, relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("文件保存失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
    }
}
