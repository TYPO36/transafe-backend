package com.benmake.transafe.translate.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 翻译任务RabbitMQ配置
 *
 * <p>配置翻译队列、交换机、死信队列等</p>
 *
 * <h3>队列架构</h3>
 * <pre>
 * 普通翻译队列 (TRANSLATE_QUEUE)
 *   └── 失败 → 死信交换机 (TRANSLATE_DLX)
 *                 └── 死信队列 (TRANSLATE_DLQ)
 *
 * 优先翻译队列 (TRANSLATE_QUEUE_PRIORITY)
 *   └── VIP用户优先处理
 * </pre>
 *
 * @author JTP
 * @date 2026-04-02
 */
@Configuration
public class TranslateRabbitMQConfig {

    // ============================================================
    // 常量定义
    // ============================================================

    /** 普通翻译队列 */
    public static final String TRANSLATE_QUEUE = "translate_queue";

    /** 优先翻译队列 */
    public static final String TRANSLATE_QUEUE_PRIORITY = "translate_queue_priority";

    /** 翻译结果队列 */
    public static final String TRANSLATE_RESULT_QUEUE = "translate_result_queue";

    /** 翻译交换机 */
    public static final String TRANSLATE_EXCHANGE = "translate_exchange";

    /** 死信交换机 */
    public static final String TRANSLATE_DLX = "translate.dlx";

    /** 死信队列 */
    public static final String TRANSLATE_DLQ = "translate.dlq";

    /** 普通翻译路由键 */
    public static final String ROUTING_TRANSLATE = "translate.normal";

    /** 优先翻译路由键 */
    public static final String ROUTING_TRANSLATE_PRIORITY = "translate.priority";

    /** 翻译结果路由键 */
    public static final String ROUTING_RESULT = "translate.result";

    /** 死信路由键 */
    public static final String ROUTING_DLQ = "translate.dlq";

    // ============================================================
    // 消息转换器和监听器工厂
    // ============================================================

    /**
     * JSON消息转换器
     */
    @Bean
    public MessageConverter translateMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 翻译队列监听器容器工厂
     *
     * <p>配置并发消费者，用于处理翻译任务</p>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory translateListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(translateMessageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(2);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    /**
     * 翻译结果队列监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory translateResultListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(translateMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // ============================================================
    // 交换机
    // ============================================================

    /**
     * 翻译交换机
     */
    @Bean
    public DirectExchange translateExchange() {
        return new DirectExchange(TRANSLATE_EXCHANGE, true, false);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange translateDeadLetterExchange() {
        return new DirectExchange(TRANSLATE_DLX, true, false);
    }

    // ============================================================
    // 队列
    // ============================================================

    /**
     * 普通翻译队列（带死信配置）
     */
    @Bean
    public Queue translateQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000); // 24小时
        args.put("x-max-priority", 10);
        args.put("x-dead-letter-exchange", TRANSLATE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_DLQ);
        return new Queue(TRANSLATE_QUEUE, true, false, false, args);
    }

    /**
     * 优先翻译队列
     */
    @Bean
    public Queue translatePriorityQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000);
        args.put("x-max-priority", 10);
        args.put("x-dead-letter-exchange", TRANSLATE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_DLQ);
        return new Queue(TRANSLATE_QUEUE_PRIORITY, true, false, false, args);
    }

    /**
     * 翻译结果队列
     */
    @Bean
    public Queue translateResultQueue() {
        return new Queue(TRANSLATE_RESULT_QUEUE, true);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue translateDeadLetterQueue() {
        return new Queue(TRANSLATE_DLQ, true);
    }

    // ============================================================
    // 绑定
    // ============================================================

    /**
     * 普通翻译队列绑定
     */
    @Bean
    public Binding translateBinding() {
        return BindingBuilder.bind(translateQueue())
                .to(translateExchange())
                .with(ROUTING_TRANSLATE);
    }

    /**
     * 优先翻译队列绑定
     */
    @Bean
    public Binding translatePriorityBinding() {
        return BindingBuilder.bind(translatePriorityQueue())
                .to(translateExchange())
                .with(ROUTING_TRANSLATE_PRIORITY);
    }

    /**
     * 翻译结果队列绑定
     */
    @Bean
    public Binding translateResultBinding() {
        return BindingBuilder.bind(translateResultQueue())
                .to(translateExchange())
                .with(ROUTING_RESULT);
    }

    /**
     * 死信队列绑定
     */
    @Bean
    public Binding translateDlqBinding() {
        return BindingBuilder.bind(translateDeadLetterQueue())
                .to(translateDeadLetterExchange())
                .with(ROUTING_DLQ);
    }
}