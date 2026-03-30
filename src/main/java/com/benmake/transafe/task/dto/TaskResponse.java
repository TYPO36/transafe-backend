package com.benmake.transafe.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务响应
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private String taskId;
    private String fileId;
    private String fileName;
    private String fileType;
    private String status;
    private Integer charCount;
    private String errorMessage;
    private String createdAt;
    private String completedAt;
}