package com.benmake.transafe.file.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.file.config.FileStorageConfig;
import com.benmake.transafe.file.dto.FileInfoResponse;
import com.benmake.transafe.file.dto.FileUploadResponse;
import com.benmake.transafe.file.entity.FileEntity;
import com.benmake.transafe.infra.mapper.FileMapper;
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
 * <h3>依赖配置</h3>
 * <p>需要在 application.yaml 中配置：</p>
 * <pre>
 * file-storage:
 *   type: local
 *   local-path: ./files
 * </pre>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    // ============================================================
    // 依赖注入
    // ============================================================

    /**
     * 文件数据访问层
     * 用于操作 file 表，存储文件元数据信息
     */
    private final FileMapper fileMapper;

    /**
     * 文件存储配置
     * 从 application.yaml 读取 file-storage.local-path 配置
     */
    private final FileStorageConfig fileStorageConfig;

    // ============================================================
    // 常量定义
    // ============================================================

    /**
     * 日期路径格式化器
     * 用于生成按日期分类的存储路径，格式：yyyy/MM/dd
     * 示例：2024/01/15
     */
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // ============================================================
    // 公共方法 - 文件操作
    // ============================================================

    /**
     * 上传文件到本地存储
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>生成唯一文件 ID（32位 UUID，无横杠）</li>
     *   <li>计算存储路径：user_{userId}/yyyy/MM/dd/{fileId}</li>
     *   <li>创建目标目录（如不存在）</li>
     *   <li>保存文件到磁盘</li>
     *   <li>保存文件元数据到数据库</li>
     * </ol>
     *
     * <h4>异常处理</h4>
     * <ul>
     *   <li>IO异常：记录日志并抛出 SYSTEM_ERROR</li>
     *   <li>文件为空：由 MultipartFile 自身校验</li>
     * </ul>
     *
     * @param file   上传的文件对象，由 Spring MVC 自动解析
     * @param userId 用户 ID，用于隔离用户文件
     * @return 文件上传响应，包含文件 ID、名称、大小、类型
     * @throws BusinessException 文件保存失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResponse uploadFile(MultipartFile file, Long userId) {
        // 1. 生成唯一文件ID和提取文件信息
        // 使用 UUID 确保文件名唯一性，避免文件名冲突
        String fileId = generateFileId();
        // 获取原始文件名，可能为 null
        String originalFilename = file.getOriginalFilename();
        // 提取文件扩展名，用于后续处理（如文件类型校验、解析器选择）
        String fileType = getFileExtension(originalFilename);

        // 2. 计算存储路径
        // 路径格式：user_{userId}/2024/01/15/{fileId}
        // 按日期分目录可以避免单目录文件过多，提高文件系统性能
        String datePath = LocalDateTime.now().format(DATE_PATH_FORMATTER);
        String relativePath = "user_" + userId + "/" + datePath + "/" + fileId;

        try {
            // 3. 创建目标目录
            // Files.createDirectories 会递归创建所有不存在的父目录
            Path targetDir = Paths.get(fileStorageConfig.getLocalPath(), "user_" + userId, datePath);
            Files.createDirectories(targetDir);

            // 4. 保存文件到磁盘
            // 使用文件 ID 作为存储文件名，避免特殊字符和中文文件名问题
            Path targetPath = targetDir.resolve(fileId);
            try (InputStream inputStream = file.getInputStream()) {
                // StandardCopyOption.REPLACE_EXISTING: 如果文件已存在则覆盖
                // 理论上 UUID 不会重复，这里只是为了保险
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 5. 保存文件元数据到数据库
            // 文件实体记录：文件ID、用户ID、原始文件名、大小、类型、存储路径、状态等
            LocalDateTime now = LocalDateTime.now();
            FileEntity fileEntity = FileEntity.builder()
                    .fileId(fileId)              // 唯一标识，用于后续查询和下载
                    .userId(userId)              // 所属用户，用于权限校验
                    .fileName(originalFilename)  // 原始文件名，用于显示和下载时的文件名
                    .fileSize(file.getSize())    // 文件大小（字节），用于配额统计
                    .fileType(fileType)          // 文件扩展名，用于选择解析器
                    .storagePath(relativePath)   // 相对存储路径，用于定位文件
                    .status("UPLOADED")          // 状态：UPLOADED-已上传，DELETED-已删除
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            fileMapper.insert(fileEntity);

            log.info("文件上传成功: fileId={}, fileName={}, size={}, userId={}",
                    fileId, originalFilename, file.getSize(), userId);

            // 6. 返回上传结果
            return FileUploadResponse.builder()
                    .fileId(fileId)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .build();

        } catch (IOException e) {
            // IO异常可能是：磁盘空间不足、权限不足、路径非法等
            log.error("文件保存失败: fileId={}, error={}", fileId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
    }

    /**
     * 下载文件
     *
     * <p>根据文件 ID 获取文件资源，用于下载或在线预览。</p>
     *
     * <h4>返回值说明</h4>
     * <p>返回 Spring 的 Resource 接口，支持：</p>
     * <ul>
     *   <li>直接返回给前端下载</li>
     *   <li>获取输入流进行处理</li>
     *   <li>获取文件元信息（大小、类型等）</li>
     * </ul>
     *
     * @param fileId 文件唯一标识
     * @param userId 用户 ID，用于权限校验
     * @return Resource 文件资源对象
     * @throws BusinessException 文件不存在或无权限时抛出
     */
    public Resource downloadFile(String fileId, Long userId) {
        // 1. 查询文件元数据，同时校验用户权限
        FileEntity fileEntity = getFileEntity(fileId, userId);

        // 2. 构建文件完整路径
        // 路径 = 配置的根路径 + 数据库存储的相对路径
        Path filePath = Paths.get(fileStorageConfig.getLocalPath(), fileEntity.getStoragePath());

        // 3. 检查文件是否存在
        // 文件可能被外部删除，需要检查
        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        // 4. 创建资源对象
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            // 路径格式错误，理论上不会发生
            log.error("文件路径解析失败: path={}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败");
        }
    }

    /**
     * 获取文件信息
     *
     * <p>返回文件的元数据信息，不包含文件内容。</p>
     *
     * @param fileId 文件唯一标识
     * @param userId 用户 ID，用于权限校验
     * @return FileInfoResponse 文件信息响应
     * @throws BusinessException 文件不存在或无权限时抛出
     */
    public FileInfoResponse getFileInfo(String fileId, Long userId) {
        FileEntity fileEntity = getFileEntity(fileId, userId);

        // 将实体转换为响应 DTO
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
     * 获取用户的文件列表（分页）
     *
     * <p>查询指定用户的所有已上传文件，支持分页。</p>
     *
     * <h4>返回数据结构</h4>
     * <pre>
     * {
     *   "content": [...],          // 文件列表
     *   "totalElements": 100,      // 总记录数
     *   "totalPages": 10,          // 总页数
     *   "size": 10,                // 每页大小
     *   "number": 0,               // 当前页码（从0开始）
     *   "first": true,             // 是否第一页
     *   "last": false,             // 是否最后一页
     *   "empty": false             // 是否为空
     * }
     * </pre>
     *
     * @param userId  用户 ID
     * @param pageNum 页码（从1开始）
     * @param size    每页大小
     * @return Map&lt;String, Object&gt; 分页结果
     */
    public Map<String, Object> listFiles(Long userId, int pageNum, int size) {
        // MyBatis Plus 分页查询
        // Page 构造函数：页码从1开始，size 为每页大小
        Page<FileEntity> page = new Page<>(pageNum, size);

        // 查询用户已上传的文件（排除已删除的）
        IPage<FileEntity> fileEntities = fileMapper.findByUserIdAndStatus(page, userId, "UPLOADED");

        // 构建返回结果
        // 使用 Map 是为了与前端分页组件兼容
        Map<String, Object> result = new HashMap<>();
        result.put("content", fileEntities.getRecords().stream()
                .map(entity -> FileInfoResponse.builder()
                        .fileId(entity.getFileId())
                        .fileName(entity.getFileName())
                        .fileSize(entity.getFileSize())
                        .fileType(entity.getFileType())
                        .status(entity.getStatus())
                        .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                        .build())
                .toList());
        result.put("totalElements", fileEntities.getTotal());   // 总记录数
        result.put("totalPages", fileEntities.getPages());      // 总页数
        result.put("size", fileEntities.getSize());             // 每页大小
        result.put("number", fileEntities.getCurrent() - 1);    // 当前页码（转为从0开始）
        result.put("first", fileEntities.getCurrent() == 1);    // 是否第一页
        result.put("last", fileEntities.getCurrent() == fileEntities.getPages()); // 是否最后一页
        result.put("empty", fileEntities.getRecords().isEmpty()); // 是否为空

        return result;
    }

    /**
     * 删除文件（软删除）
     *
     * <p>将文件状态标记为 DELETED，并删除物理文件。</p>
     *
     * <h4>软删除说明</h4>
     * <ul>
     *   <li>数据库记录保留，状态改为 DELETED</li>
     *   <li>物理文件删除失败不影响数据库状态更新</li>
     *   <li>可扩展为定时任务清理 DELETED 状态的记录</li>
     * </ul>
     *
     * @param fileId 文件唯一标识
     * @param userId 用户 ID，用于权限校验
     * @throws BusinessException 文件不存在或无权限时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileId, Long userId) {
        // 1. 查询文件元数据
        FileEntity fileEntity = getFileEntity(fileId, userId);

        // 2. 删除物理文件
        Path filePath = Paths.get(fileStorageConfig.getLocalPath(), fileEntity.getStoragePath());
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("物理文件删除成功: path={}", filePath);
            }
        } catch (IOException e) {
            // 物理文件删除失败只记录日志，不影响数据库状态更新
            // 因为文件可能已被外部删除，重要的是更新数据库状态
            log.error("物理文件删除失败: path={}, error={}", filePath, e.getMessage(), e);
        }

        // 3. 更新数据库状态（软删除）
        fileEntity.setStatus("DELETED");
        fileEntity.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(fileEntity);

        log.info("文件删除成功: fileId={}, userId={}", fileId, userId);
    }

    /**
     * 更新文件信息
     *
     * <p>目前支持修改文件名。</p>
     *
     * @param fileId      文件唯一标识
     * @param userId      用户 ID，用于权限校验
     * @param newFileName 新文件名
     * @return FileInfoResponse 更新后的文件信息
     * @throws BusinessException 文件不存在或无权限时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public FileInfoResponse updateFileInfo(String fileId, Long userId, String newFileName) {
        FileEntity fileEntity = getFileEntity(fileId, userId);

        String oldName = fileEntity.getFileName();
        fileEntity.setFileName(newFileName);
        fileEntity.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(fileEntity);

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

    // ============================================================
    // 内部方法 - 工具方法
    // ============================================================

    /**
     * 获取文件实体（内部使用）
     *
     * <p>根据文件 ID 和用户 ID 查询文件，并校验状态和权限。</p>
     *
     * @param fileId 文件唯一标识
     * @param userId 用户 ID
     * @return FileEntity 文件实体
     * @throws BusinessException 文件不存在或无权限时抛出
     */
    public FileEntity getFileEntity(String fileId, Long userId) {
        return fileMapper.findByFileIdAndUserId(fileId, userId)
                .filter(f -> "UPLOADED".equals(f.getStatus()))  // 只返回已上传状态的文件
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    /**
     * 生成唯一文件 ID
     *
     * <p>使用 UUID 生成 32 位唯一标识符（移除横杠）。</p>
     *
     * <h4>为什么使用 UUID</h4>
     * <ul>
     *   <li>全局唯一，避免文件名冲突</li>
     *   <li>不可预测，避免恶意遍历</li>
     *   <li>与原始文件名解耦，避免特殊字符问题</li>
     * </ul>
     *
     * @return 32 位唯一文件 ID
     */
    private String generateFileId() {
        // UUID 格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        // 移除横杠后：32 位十六进制字符串
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取文件扩展名
     *
     * <p>从文件名中提取扩展名（最后一个点之后的部分）。</p>
     *
     * @param fileName 文件名
     * @return 小写的文件扩展名，无扩展名返回 "unknown"
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        // 取最后一个点之后的部分，并转小写
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    // ============================================================
    // 供解析器使用的方法
    // ============================================================

    /**
     * 根据存储路径获取文件输入流
     *
     * <p>供文档解析器使用，根据存储路径读取文件内容。</p>
     *
     * <h4>使用场景</h4>
     * <ul>
     *   <li>文档解析：PDF、Word、Excel 等文件解析时读取内容</li>
     *   <li>文件预览：生成预览图或提取文本</li>
     * </ul>
     *
     * @param storagePath 存储路径（相对路径，如：user_1/2024/01/15/abc123）
     * @return InputStream 文件输入流
     * @throws BusinessException 存储路径为空或文件不存在时抛出
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
     *
     * <p>供文档解析器使用，通过文件 ID 读取文件内容。</p>
     *
     * @param fileId 文件唯一标识
     * @param userId 用户 ID，用于权限校验
     * @return InputStream 文件输入流
     * @throws BusinessException 文件不存在或无权限时抛出
     */
    public InputStream getFileInputStreamById(String fileId, Long userId) {
        FileEntity fileEntity = fileMapper.findByFileIdAndUserId(fileId, userId)
                .filter(f -> "UPLOADED".equals(f.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        return getFileInputStream(fileEntity.getStoragePath());
    }

    /**
     * 保存文件内容并返回存储路径
     *
     * <p>供文档解析器使用，用于保存解析过程中产生的临时文件。</p>
     *
     * <h4>使用场景</h4>
     * <ul>
     *   <li>ZIP 解压：保存解压后的文件</li>
     *   <li>邮件附件：保存邮件中的附件</li>
     *   <li>临时文件：保存处理过程中的中间文件</li>
     * </ul>
     *
     * <h4>注意</h4>
     * <p>此方法只保存物理文件，不创建数据库记录。
     * 如需持久化，应调用 uploadFile 方法或手动创建记录。</p>
     *
     * @param fileName 文件名（仅用于日志记录）
     * @param content  文件内容输入流
     * @param userId   用户 ID
     * @return String 相对存储路径
     * @throws BusinessException 文件保存失败时抛出
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