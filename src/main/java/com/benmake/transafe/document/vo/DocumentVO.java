package com.benmake.transafe.document.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 *
 * <p>用于接收 DocumentMapper join file 表的查询结果</p>
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {

    // ==================== Document 表字段 ====================

    /**
     * 文档主键ID
     */
    private Long id;

    /**
     * 文件唯一标识(UUID)
     */
    private String fileId;

    /**
     * 父文档file_id，顶层为null
     */
    private String parentId;

    /**
     * 根文档file_id
     */
    private String rootId;

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
     * 是否为附件
     */
    private Boolean isAttachment;

    /**
     * 优先级: 0普通, 1优先
     */
    private Integer priority;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 解析后的文本内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ==================== File 表字段 (join 获取) ====================

    /**
     * 用户ID (from file)
     */
    private Long userId;

    /**
     * 原始文件名 (from file)
     */
    private String fileName;

    /**
     * 文件大小(字节) (from file)
     */
    private Long fileSize;

    /**
     * 文件类型 (from file)
     */
    private String fileType;

    /**
     * 文件存储路径 (from file)
     */
    private String storagePath;
}
