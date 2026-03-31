package com.benmake.transafe.file.service.impl;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.file.dto.FileInfoResponse;
import com.benmake.transafe.file.dto.FileUploadResponse;
import com.benmake.transafe.file.service.FileProxyService;
import com.benmake.transafe.file.service.LocalFileStorageService;
import com.benmake.transafe.quota.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件代理服务实现
 *
 * @author TYPO
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileProxyServiceImpl implements FileProxyService {

    private final LocalFileStorageService localFileStorageService;
    private final QuotaService quotaService;

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, Long userId) {
        // 检查存储空间
        if (!quotaService.checkStorageSpace(userId, file.getSize())) {
            throw new BusinessException(ErrorCode.STORAGE_EXCEEDED);
        }

        FileUploadResponse result = localFileStorageService.uploadFile(file, userId);

        // 更新存储使用量
        quotaService.updateStorageUsed(userId, file.getSize());

        log.info("用户 {} 上传文件成功: fileId={}, fileName={}", userId, result.getFileId(), result.getFileName());
        return result;
    }

    @Override
    public Resource downloadFile(String fileId, Long userId) {
        return localFileStorageService.downloadFile(fileId, userId);
    }

    @Override
    public FileInfoResponse getFileInfo(String fileId, Long userId) {
        return localFileStorageService.getFileInfo(fileId, userId);
    }

    @Override
    public Map<String, Object> listFiles(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FileInfoResponse> filePage = localFileStorageService.listFiles(userId, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("content", filePage.getContent());
        result.put("totalElements", filePage.getTotalElements());
        result.put("totalPages", filePage.getTotalPages());
        result.put("size", filePage.getSize());
        result.put("number", filePage.getNumber());
        result.put("first", filePage.isFirst());
        result.put("last", filePage.isLast());
        result.put("empty", filePage.isEmpty());

        return result;
    }

    @Override
    public void deleteFile(String fileId, Long userId) {
        // 获取文件信息以获取文件大小
        FileInfoResponse fileInfo = localFileStorageService.getFileInfo(fileId, userId);

        // 删除文件
        localFileStorageService.deleteFile(fileId, userId);

        // 更新存储使用量
        quotaService.updateStorageUsed(userId, -fileInfo.getFileSize());

        log.info("用户 {} 删除文件成功: fileId={}", userId, fileId);
    }

    @Override
    public FileInfoResponse updateFileInfo(String fileId, Long userId, String newFileName) {
        return localFileStorageService.updateFileInfo(fileId, userId, newFileName);
    }

    @Override
    public String getFileName(String fileId, Long userId) {
        try {
            FileInfoResponse info = localFileStorageService.getFileInfo(fileId, userId);
            return info.getFileName();
        } catch (Exception e) {
            return fileId;
        }
    }
}
