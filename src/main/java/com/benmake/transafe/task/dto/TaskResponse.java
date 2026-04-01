package com.benmake.transafe.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务响应
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private String taskId;

    /**
     * 文档ID（对应 document 表主键）
     */
    private Long documentId;

    private String fileName;
    private String fileType;
    private String status;
    private Integer charCount;
    private String errorMessage;
    private String createdAt;
    private String completedAt;
}
