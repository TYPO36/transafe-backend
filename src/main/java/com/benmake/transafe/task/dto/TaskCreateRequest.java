package com.benmake.transafe.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 任务创建请求
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
public class TaskCreateRequest {

    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    private Map<String, Object> parseConfig;
}