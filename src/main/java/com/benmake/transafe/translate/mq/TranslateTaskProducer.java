package com.benmake.transafe.translate.mq;

import com.benmake.transafe.translate.config.TranslateRabbitMQConfig;
import com.benmake.transafe.translate.dto.TranslateMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 翻译任务生产者
 *
 * @author JTP
 * @date 2026-04-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送翻译任务到普通队列
     *
     * @param message 翻译消息
     */
    public void send(TranslateMessageDTO message) {
        if (message.getPriority() == null) {
            message.setPriority(0);
        }
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }

        rabbitTemplate.convertAndSend(
                TranslateRabbitMQConfig.TRANSLATE_EXCHANGE,
                TranslateRabbitMQConfig.ROUTING_TRANSLATE,
                message
        );

        log.info("发送翻译任务: fileId={}, taskId={}, targetLang={}",
                message.getFileId(), message.getTaskId(), message.getTargetLang());
    }

    /**
     * 发送翻译任务到优先队列
     *
     * @param message 翻译消息
     */
    public void sendPriority(TranslateMessageDTO message) {
        message.setPriority(1);
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }

        rabbitTemplate.convertAndSend(
                TranslateRabbitMQConfig.TRANSLATE_EXCHANGE,
                TranslateRabbitMQConfig.ROUTING_TRANSLATE_PRIORITY,
                message
        );

        log.info("发送优先翻译任务: fileId={}, taskId={}", message.getFileId(), message.getTaskId());
    }

    /**
     * 根据优先级自动选择队列发送
     *
     * @param message 翻译消息
     */
    public void sendByPriority(TranslateMessageDTO message, boolean isVip) {
        if (isVip || (message.getPriority() != null && message.getPriority() == 1)) {
            sendPriority(message);
        } else {
            send(message);
        }
    }

    /**
     * 发送重试任务
     *
     * @param message 翻译消息
     */
    public void sendForRetry(TranslateMessageDTO message) {
        int retryCount = message.getRetryCount() != null ? message.getRetryCount() : 0;
        retryCount++;

        if (retryCount > 3) {
            log.warn("翻译任务重试次数超限: taskId={}, fileId={}", message.getTaskId(), message.getFileId());
            return;
        }

        message.setRetryCount(retryCount);
        send(message);

        log.info("发送翻译重试任务: fileId={}, taskId={}, retryCount={}",
                message.getFileId(), message.getTaskId(), retryCount);
    }
}