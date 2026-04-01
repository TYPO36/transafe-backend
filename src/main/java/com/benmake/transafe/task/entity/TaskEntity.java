package com.benmake.transafe.task.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体（通用任务表）
 *
 * <p>通过 task_type 区分任务类型，支持解析任务、翻译任务等</p>
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@TableName("task")
public class TaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一标识
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联文档ID（指向 document 表的主键）
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 任务类型：PARSE-解析任务，TRANSLATE-翻译任务
     */
    @TableField("task_type")
    private String taskType;

    /**
     * 任务状态：pending-待处理，processing-处理中，completed-已完成，failed-失败
     */
    @TableField("status")
    private String status = "PENDING";

    /**
     * 任务结果（JSON）
     */
    @TableField("result")
    private String result;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

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
