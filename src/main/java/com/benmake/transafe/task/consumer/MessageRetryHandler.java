package com.benmake.transafe.task.consumer;

import com.benmake.transafe.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息重试处理器
 *
 * <p>处理消息消费失败后的重试逻辑。</p>
 *
 * <h3>重试策略</h3>
 * <ul>
 *   <li>最大重试次数：3 次</li>
 *   <li>重试延迟：1 分钟</li>
 *   <li>超过最大次数：发送到死信队列</li>
 * </ul>
 *
 * <h3>重试计数存储</h3>
 * <p>重试计数存储在消息头的 x-retry-count 字段中。</p>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetryHandler {

    private final RabbitTemplate rabbitTemplate;

    /** 重试计数消息头键 */
    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    /** 原始交换机消息头键 */
    private static final String ORIGINAL_EXCHANGE_HEADER = "x-original-exchange";

    /** 原始路由键消息头键 */
    private static final String ORIGINAL_ROUTING_KEY_HEADER = "x-original-routing-key";

    /**
     * 处理消息消费失败
     *
     * <p>根据重试次数决定：重新投递或发送到死信队列。</p>
     *
     * @param message    原始消息
     * @param exchange   目标交换机
     * @param routingKey 路由键
     */
    public void handleFailure(Message message, String exchange, String routingKey) {
        Integer retryCount = (Integer) message.getMessageProperties()
                .getHeaders().get(RETRY_COUNT_HEADER);

        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount++;

        if (retryCount <= RabbitMQConfig.MAX_RETRY_COUNT) {
            // 更新重试计数
            message.getMessageProperties().setHeader(RETRY_COUNT_HEADER, retryCount);

            log.warn("消息重试: retryCount={}/{}, exchange={}, routingKey={}",
                    retryCount, RabbitMQConfig.MAX_RETRY_COUNT, exchange, routingKey);

            // 发送到重试队列（延迟后自动转发回任务队列）
            rabbitTemplate.send(RabbitMQConfig.RETRY_EXCHANGE, routingKey, message);
        } else {
            log.error("消息达到最大重试次数，发送到死信队列: retryCount={}, exchange={}, routingKey={}",
                    retryCount, exchange, routingKey);

            // 发送到死信队列
            rabbitTemplate.send(RabbitMQConfig.DLQ_EXCHANGE, RabbitMQConfig.DLQ_ROUTING_KEY, message);
        }
    }

    /**
     * 处理任务结果消费失败
     *
     * @param message 原始消息
     */
    public void handleTaskResultFailure(Message message) {
        handleFailure(message, RabbitMQConfig.EXCHANGE, RabbitMQConfig.RESULT_ROUTING_KEY);
    }

    /**
     * 获取当前重试次数
     *
     * @param message 消息
     * @return 重试次数
     */
    public int getRetryCount(Message message) {
        Integer retryCount = (Integer) message.getMessageProperties()
                .getHeaders().get(RETRY_COUNT_HEADER);
        return retryCount != null ? retryCount : 0;
    }

    /**
     * 检查是否可以重试
     *
     * @param message 消息
     * @return true-可重试，false-已达最大次数
     */
    public boolean canRetry(Message message) {
        return getRetryCount(message) < RabbitMQConfig.MAX_RETRY_COUNT;
    }
}