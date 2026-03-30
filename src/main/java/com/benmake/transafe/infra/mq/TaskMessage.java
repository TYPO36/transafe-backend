package com.benmake.transafe.infra.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务消息
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage implements Serializable {

    private String taskId;
    private String fileId;
    private Long userId;
    private String fileName;
    private String fileType;
    private Map<String, Object> parseConfig;
    private String createdAt;
}