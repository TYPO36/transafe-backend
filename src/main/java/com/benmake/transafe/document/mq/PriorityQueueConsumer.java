package com.benmake.transafe.document.mq;

import com.benmake.transafe.document.config.DocumentRabbitMQConfig;
import com.benmake.transafe.document.dto.ParseMessageDTO;
import com.benmake.transafe.document.service.ParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优先队列消费者
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriorityQueueConsumer {

    private final ParseService parseService;

    /**
     * 处理优先队列消息
     *
     * @param message 解析消息
     */
    @RabbitListener(
            queues = DocumentRabbitMQConfig.QUEUE_PRIORITY,
            containerFactory = "priorityListenerContainerFactory"
    )
    public void handleMessage(ParseMessageDTO message) {
        log.info("优先队列接收消息: fileId={}, fileName={}, retryCount={}",
                message.getFileId(), message.getFileName(), message.getRetryCount());
        try {
            parseService.processParse(message);
        } catch (Exception e) {
            log.error("优先队列解析失败: fileId={}", message.getFileId(), e);
        }
    }
}
