package com.benmake.transafe.document.mq;

import com.benmake.transafe.document.config.DocumentRabbitMQConfig;
import com.benmake.transafe.document.dto.ParseMessageDTO;
import com.benmake.transafe.document.service.ParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 普通队列消费者
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NormalQueueConsumer {

    private final ParseService parseService;

    /**
     * 处理普通队列消息
     *
     * @param message 解析消息
     */
    @RabbitListener(
            queues = DocumentRabbitMQConfig.QUEUE_NORMAL,
            containerFactory = "normalListenerContainerFactory"
    )
    public void handleMessage(ParseMessageDTO message) {
        log.info("普通队列接收消息: fileId={}, fileName={}, retryCount={}",
                message.getFileId(), message.getFileName(), message.getRetryCount());
        try {
            parseService.processParse(message);
        } catch (Exception e) {
            log.error("普通队列解析失败: fileId={}", message.getFileId(), e);
        }
    }
}
