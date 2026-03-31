package com.benmake.transafe.document.mq;

import com.benmake.transafe.document.config.DocumentRabbitMQConfig;
import com.benmake.transafe.document.dto.ParseMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 文档解析MQ消息生产者
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到普通队列
     *
     * @param message 解析消息
     */
    public void sendToNormalQueue(ParseMessageDTO message) {
        message.setPriority(0);
        rabbitTemplate.convertAndSend(
                DocumentRabbitMQConfig.EXCHANGE,
                DocumentRabbitMQConfig.ROUTING_NORMAL,
                message
        );
        log.info("发送普通解析队列: fileId={}, fileName={}",
                message.getFileId(), message.getFileName());
    }

    /**
     * 发送消息到优先队列
     *
     * @param message 解析消息
     */
    public void sendToPriorityQueue(ParseMessageDTO message) {
        message.setPriority(1);
        rabbitTemplate.convertAndSend(
                DocumentRabbitMQConfig.EXCHANGE,
                DocumentRabbitMQConfig.ROUTING_PRIORITY,
                message
        );
        log.info("发送优先解析队列: fileId={}, fileName={}",
                message.getFileId(), message.getFileName());
    }

    /**
     * 发送消息用于重试
     *
     * @param message 解析消息
     */
    public void sendForRetry(ParseMessageDTO message) {
        message.setPriority(0);
        message.setRetryCount(message.getRetryCount() + 1);
        rabbitTemplate.convertAndSend(
                DocumentRabbitMQConfig.EXCHANGE,
                DocumentRabbitMQConfig.ROUTING_NORMAL,
                message
        );
        log.info("发送重试解析队列: fileId={}, retryCount={}",
                message.getFileId(), message.getRetryCount());
    }

    /**
     * 根据优先级选择队列发送
     *
     * @param message 解析消息
     */
    public void send(ParseMessageDTO message) {
        if (message.getPriority() != null && message.getPriority() == 1) {
            sendToPriorityQueue(message);
        } else {
            sendToNormalQueue(message);
        }
    }
}
