package com.benmake.transafe.document.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.benmake.transafe.document.common.constant.ParseStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体
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
     * 文件唯一标识(UUID)
     */
    @TableField("file_id")
    private String fileId;

    /**
     * 父文档file_id，顶层为null
     */
    @TableField("parent_id")
    private String parentId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 根文档file_id，所有关联文档指向顶层
     */
    @TableField("root_id")
    private String rootId;

    /**
     * 原始文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小(字节)
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件存储路径
     */
    @TableField("file_storage_path")
    private String fileStoragePath;

    /**
     * 文件类型: pdf,doc,docx,ppt,pptx,xls,xlsx,txt,eml
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 状态: pending/parsing/parsed/failed
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
     * 是否为附件
     */
    @TableField("is_attachment")
    private Boolean isAttachment = false;

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
}