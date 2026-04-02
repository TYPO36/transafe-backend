package com.benmake.transafe.document.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.benmake.transafe.document.common.constant.ParseStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体（合并后的实体）
 *
 * <p>职责：统一管理文件/文档/邮件的元数据，ES 存储内容，MySQL 只存元数据</p>
 *
 * <p>设计说明：
 * <ul>
 *   <li>文件元数据：file_id, user_id, file_name, file_size, file_type, storage_path, status</li>
 *   <li>树结构关系：parent_id, root_id, is_attachment（支持批量上传、邮件附件等场景）</li>
 *   <li>解析状态：parse_status, parse_error_code, parse_error_message, password_provided, priority, retry_count</li>
 * </ul>
 * </p>
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@TableName("document")
public class DocumentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件唯一标识（UUID）
     */
    @TableField("file_id")
    private String fileId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    // ==================== 文件元数据 ====================

    /**
     * 原始文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件类型/扩展名
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件存储路径（相对路径）
     */
    @TableField("storage_path")
    private String storagePath;

    /**
     * 文件状态：UPLOADED-已上传，DELETED-已删除
     */
    @TableField("status")
    private String status = "UPLOADED";

    // ==================== 树结构关系 ====================

    /**
     * 父文档file_id，顶层为null
     */
    @TableField("parent_id")
    private String parentId;

    /**
     * 根文档file_id，所有关联文档指向顶层
     */
    @TableField("root_id")
    private String rootId;

    /**
     * 是否为附件
     */
    @TableField("is_attachment")
    private Boolean isAttachment = false;

    // ==================== 解析状态 ====================

    /**
     * 解析状态: pending/parsing/parsed/failed
     */
    @TableField("parse_status")
    private String parseStatus = ParseStatus.PENDING;

    /**
     * 错误码: 0成功, 3001密码保护, 3002不支持格式, 3003文件损坏, 3004解析超时
     */
    @TableField("parse_error_code")
    private Integer parseErrorCode = 0;

    /**
     * 错误信息
     */
    @TableField("parse_error_message")
    private String parseErrorMessage;

    /**
     * 用户提供的密码(仅密码保护文件)
     */
    @TableField("password_provided")
    private String passwordProvided;

    /**
     * 优先级: 0普通, 1优先
     */
    @TableField("priority")
    private Integer priority = 0;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount = 0;

    // ==================== 翻译状态 ====================

    /**
     * 是否需要翻译
     */
    @TableField("need_translate")
    private Boolean needTranslate = false;

    /**
     * 翻译目标语言（如 "zh", "en"）
     */
    @TableField("target_lang")
    private String targetLang;

    /**
     * 翻译源语言（默认 "auto" 自动检测）
     */
    @TableField("source_lang")
    private String sourceLang;

    /**
     * 翻译状态: null/pending/translating/translated/failed
     */
    @TableField("translate_status")
    private String translateStatus;

    // ==================== 审计字段 ====================

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted = 0;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version = 0;
}
