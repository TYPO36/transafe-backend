package com.benmake.transafe.document.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 *
 * <p>合并后的文档视图，包含 document 表所有字段（不含 content，content 存 ES）</p>
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {

    // ==================== Document 主键 ====================

    /**
     * 文档主键ID
     */
    private Long id;

    /**
     * 文件唯一标识(UUID)
     */
    private String fileId;

    // ==================== 用户与文件元数据 ====================

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件存储路径
     */
    private String storagePath;

    /**
     * 文件状态：UPLOADED-已上传，DELETED-已删除
     */
    private String status;

    // ==================== 树结构关系 ====================

    /**
     * 父文档file_id，顶层为null
     */
    private String parentId;

    /**
     * 根文档file_id
     */
    private String rootId;

    /**
     * 是否为附件
     */
    private Boolean isAttachment;

    // ==================== 解析状态 ====================

    /**
     * 解析状态: pending/parsing/parsed/failed
     */
    private String parseStatus;

    /**
     * 错误码
     */
    private Integer parseErrorCode;

    /**
     * 错误信息
     */
    private String parseErrorMessage;

    /**
     * 用户提供的密码
     */
    private String passwordProvided;

    /**
     * 优先级: 0普通, 1优先
     */
    private Integer priority;

    /**
     * 重试次数
     */
    private Integer retryCount;

    // ==================== 审计字段 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
