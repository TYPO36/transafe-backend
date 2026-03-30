package com.benmake.transafe.file.service;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.file.client.FileUploadClient;
import com.benmake.transafe.file.dto.FileUploadResponse;
import com.benmake.transafe.quota.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件代理服务
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileProxyService {

    private final FileUploadClient fileUploadClient;
    private final QuotaService quotaService;

    /**
     * 上传文件
     */
    public FileUploadResponse uploadFile(MultipartFile file, Long userId) {
        // 检查存储空间
        if (!quotaService.checkStorageSpace(userId, file.getSize())) {
            throw new BusinessException("STORAGE_EXCEEDED", "存储空间不足");
        }

        FileUploadResponse result = fileUploadClient.uploadFile(file, userId);

        // 更新存储使用量
        quotaService.updateStorageUsed(userId, file.getSize());

        log.info("用户 {} 上传文件成功: fileId={}, fileName={}", userId, result.getFileId(), result.getFileName());
        return result;
    }

    /**
     * 下载文件
     */
    public ResponseEntity<Resource> downloadFile(String fileId, Long userId) {
        return fileUploadClient.downloadFile(fileId);
    }

    /**
     * 获取文件信息
     */
    public Map<String, Object> getFileInfo(String fileId) {
        return fileUploadClient.getFileInfo(fileId);
    }

    /**
     * 获取文件列表
     */
    public Map<String, Object> listFiles(Long userId, int page, int size) {
        return fileUploadClient.listFiles(userId, page, size);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileId, Long userId) {
        // 获取文件信息
        Map<String, Object> fileInfo = fileUploadClient.getFileInfo(fileId);
        Object fileSizeObj = fileInfo.get("fileSize");
        Long fileSize = fileSizeObj instanceof Number ? ((Number) fileSizeObj).longValue() : 0L;

        // 删除文件
        fileUploadClient.deleteFile(fileId);

        // 更新存储使用量
        quotaService.updateStorageUsed(userId, -fileSize);

        log.info("用户 {} 删除文件成功: fileId={}", userId, fileId);
    }
}