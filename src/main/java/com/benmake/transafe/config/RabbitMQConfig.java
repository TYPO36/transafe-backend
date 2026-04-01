package com.benmake.transafe.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置
 *
 * <p>配置消息队列、交换机、死信队列和重试机制。</p>
 *
 * <h3>队列架构</h3>
 * <pre>
 * 任务队列 (TASK_QUEUE)
 *   └── 失败 → 死信交换机 (DLQ_EXCHANGE)
 *                 └── 死信队列 (TASK_DLQ)
 *
 * 重试队列 (TASK_RETRY_QUEUE)
 *   └── 延迟1分钟后 → 任务队列 (TASK_QUEUE)
 * </pre>
 *
 * @author JTP
 * @since 2026-03-30
 */
@Configuration
public class RabbitMQConfig {

    // ============================================================
    // 常量定义
    // ============================================================

    /** 任务队列名称 */
    public static final String TASK_QUEUE = "file.parse.task";

    /** 结果队列名称 */
    public static final String RESULT_QUEUE = "file.parse.result";

    /** 主交换机名称 */
    public static final String EXCHANGE = "file.parse.exchange";

    /** 死信交换机名称 */
    public static final String DLQ_EXCHANGE = "file.parse.dlx";

    /** 重试交换机名称 */
    public static final String RETRY_EXCHANGE = "file.parse.retry";

    /** 死信队列名称 */
    public static final String TASK_DLQ = "file.parse.task.dlq";

    /** 重试队列名称 */
    public static final String TASK_RETRY_QUEUE = "file.parse.task.retry";

    /** 任务路由键 */
    public static final String TASK_ROUTING_KEY = "task";

    /** 结果路由键 */
    public static final String RESULT_ROUTING_KEY = "result";

    /** 死信路由键 */
    public static final String DLQ_ROUTING_KEY = "task.dlq";

    /** 最大重试次数 */
    public static final int MAX_RETRY_COUNT = 3;

    /** 重试延迟（毫秒） */
    public static final long RETRY_DELAY_MS = 60000;

    // ============================================================
    // 消息转换器和模板
    // ============================================================

    /**
     * 消息转换器（JSON格式）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * 监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        // 消费失败不重新入队，由死信队列处理
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // ============================================================
    // 交换机
    // ============================================================

    /**
     * 主交换机
     */
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    /**
     * 重试交换机
     */
    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(RETRY_EXCHANGE, true, false);
    }

    // ============================================================
    // 队列
    // ============================================================

    /**
     * 任务队列（带死信配置）
     *
     * <p>消息过期或被拒绝后，自动转发到死信交换机。</p>
     */
    @Bean
    public Queue taskQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000); // 24小时
        args.put("x-max-priority", 10);
        args.put("x-dead-letter-exchange", DLQ_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
        return new Queue(TASK_QUEUE, true, false, false, args);
    }

    /**
     * 结果队列
     */
    @Bean
    public Queue resultQueue() {
        return new Queue(RESULT_QUEUE, true);
    }

    /**
     * 死信队列
     *
     * <p>存储处理失败的消息，供人工审查或后续处理。</p>
     */
    @Bean
    public Queue taskDeadLetterQueue() {
        return new Queue(TASK_DLQ, true);
    }

    /**
     * 重试队列（延迟队列）
     *
     * <p>消息在此队列停留1分钟后，转发回任务队列重新处理。</p>
     */
    @Bean
    public Queue taskRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", RETRY_DELAY_MS);
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", TASK_ROUTING_KEY);
        return new Queue(TASK_RETRY_QUEUE, true, false, false, args);
    }

    // ============================================================
    // 绑定
    // ============================================================

    /**
     * 任务队列绑定
     */
    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue()).to(exchange()).with(TASK_ROUTING_KEY);
    }

    /**
     * 结果队列绑定
     */
    @Bean
    public Binding resultBinding() {
        return BindingBuilder.bind(resultQueue()).to(exchange()).with(RESULT_ROUTING_KEY);
    }

    /**
     * 死信队列绑定
     */
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(taskDeadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    /**
     * 重试队列绑定
     */
    @Bean
    public Binding retryBinding() {
        return BindingBuilder.bind(taskRetryQueue()).to(retryExchange()).with(TASK_ROUTING_KEY);
    }
}