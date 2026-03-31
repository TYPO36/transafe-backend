package com.benmake.transafe.document.service;

import com.benmake.transafe.document.dto.BatchUploadResponse;
import com.benmake.transafe.document.dto.DocumentDTO;
import com.benmake.transafe.document.dto.DocumentTreeDTO;
import com.benmake.transafe.document.dto.ParseProgressDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档服务接口
 *
 * @author TYPO
 * @since 2026-03-31
 */
public interface DocumentService {

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
    DocumentDTO createDocument(String fileName, Long fileSize, String storagePath,
                               String fileType, Long userId, boolean isVip);

    /**
     * 创建文档记录（指定rootId）
     *
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param storagePath 存储路径
     * @param fileType 文件类型
     * @param userId 用户ID
     * @param isVip 是否为VIP
     * @param rootId 根文档ID
     * @return 文档DTO
     */
    DocumentDTO createDocument(String fileName, Long fileSize, String storagePath,
                               String fileType, Long userId, boolean isVip, String rootId);

    /**
     * 上传文件并创建文档记录
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @param isVip 是否为VIP
     * @return 文档DTO
     */
    DocumentDTO uploadAndCreateDocument(MultipartFile file, Long userId, boolean isVip);

    /**
     * 批量上传文件并创建文档记录
     *
     * @param files 上传的文件数组
     * @param userId 用户ID
     * @param isVip 是否为VIP
     * @return 批量上传响应
     */
    BatchUploadResponse batchUploadAndCreateDocument(MultipartFile[] files, Long userId, boolean isVip);

    /**
     * 获取文档详情
     *
     * @param fileId 文件唯一标识
     * @return 文档DTO
     */
    DocumentDTO getDocument(String fileId);

    /**
     * 获取文档状态
     *
     * @param fileId 文件唯一标识
     * @return 文档DTO
     */
    DocumentDTO getDocumentStatus(String fileId);

    /**
     * 获取文档树
     *
     * @param fileId 文件唯一标识
     * @return 文档树DTO
     */
    DocumentTreeDTO getDocumentTree(String fileId);

    /**
     * 获取解析进度
     *
     * @param rootId 根文档ID
     * @return 解析进度DTO
     */
    ParseProgressDTO getParseProgress(String rootId);

    /**
     * 密码重试解析
     *
     * @param fileId 文件唯一标识
     * @param password 密码
     */
    void retryWithPassword(String fileId, String password);
}
