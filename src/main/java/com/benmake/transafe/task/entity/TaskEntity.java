package com.benmake.transafe.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@TableName("task")
public class TaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private String taskId;

    @TableField("user_id")
    private Long userId;

    @TableField("file_id")
    private String fileId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("status")
    private String status = "PENDING";

    @TableField("char_count")
    private Integer charCount = 0;

    @TableField(value = "error_message", jdbcType = org.apache.ibatis.type.JdbcType.VARCHAR)
    private String errorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;
}