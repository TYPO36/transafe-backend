package com.benmake.transafe.document.service;

import com.benmake.transafe.document.dto.FileInfoResponse;
import com.benmake.transafe.document.dto.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文档存储服务接口
 *
 * <p>提供文件的存储、下载、删除等核心功能</p>
 *
 * @author JTP
 * @date 2026-04-01
 */
public interface DocumentStorageService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 上传响应
     */
    FileUploadResponse uploadFile(MultipartFile file, Long userId);

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件资源
     */
    Resource downloadFile(String fileId, Long userId);

    /**
     * 获取文件信息
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件信息
     */
    FileInfoResponse getFileInfo(String fileId, Long userId);

    /**
     * 获取文件列表
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 分页文件列表
     */
    Map<String, Object> listFiles(Long userId, int page, int size);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    void deleteFile(String fileId, Long userId);

    /**
     * 更新文件信息
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param newFileName 新文件名
     * @return 文件信息
     */
    FileInfoResponse updateFileInfo(String fileId, Long userId, String newFileName);

    /**
     * 获取文件名（用于下载）
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件名
     */
    String getFileName(String fileId, Long userId);
}
