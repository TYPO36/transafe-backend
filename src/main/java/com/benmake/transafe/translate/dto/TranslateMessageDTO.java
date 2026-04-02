package com.benmake.transafe.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 翻译任务消息DTO
 *
 * @author JTP
 * @date 2026-04-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslateMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 任务ID（关联 task 表）
     */
    private String taskId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 源语言（可选，默认 auto 自动检测）
     */
    private String sourceLang;

    /**
     * 目标语言（必填）
     */
    private String targetLang;

    /**
     * 优先级: 0普通, 1优先
     */
    private Integer priority;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
}