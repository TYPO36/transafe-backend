package com.benmake.transafe.translate.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 翻译结果生产者
 *
 * @author JTP
 * @date 2026-04-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateResultProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送成功结果
     *
     * @param taskId 任务ID
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param charCount 字符数
     */
    public void sendSuccessResult(String taskId, String fileId, Long userId, int charCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("fileId", fileId);
        result.put("userId", userId);
        result.put("status", "completed");
        result.put("charCount", charCount);
        result.put("completedAt", LocalDateTime.now().toString());

        rabbitTemplate.convertAndSend(
                com.benmake.transafe.translate.config.TranslateRabbitMQConfig.TRANSLATE_EXCHANGE,
                com.benmake.transafe.translate.config.TranslateRabbitMQConfig.ROUTING_RESULT,
                result
        );

        log.info("发送翻译成功结果: taskId={}, charCount={}", taskId, charCount);
    }

    /**
     * 发送失败结果
     *
     * @param taskId 任务ID
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param errorMessage 错误信息
     * @param retryCount 重试次数
     */
    public void sendFailureResult(String taskId, String fileId, Long userId,
                                   String errorMessage, int retryCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("fileId", fileId);
        result.put("userId", userId);
        result.put("status", "failed");
        result.put("errorMessage", errorMessage);
        result.put("retryCount", retryCount);
        result.put("completedAt", LocalDateTime.now().toString());

        rabbitTemplate.convertAndSend(
                com.benmake.transafe.translate.config.TranslateRabbitMQConfig.TRANSLATE_EXCHANGE,
                com.benmake.transafe.translate.config.TranslateRabbitMQConfig.ROUTING_RESULT,
                result
        );

        log.info("发送翻译失败结果: taskId={}, error={}", taskId, errorMessage);
    }
}