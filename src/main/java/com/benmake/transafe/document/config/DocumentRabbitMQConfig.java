package com.benmake.transafe.document.config;

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
 * 文档解析RabbitMQ配置
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Configuration
public class DocumentRabbitMQConfig {

    /**
     * 普通解析队列
     */
    public static final String QUEUE_NORMAL = "document_parse_queue";

    /**
     * 优先解析队列
     */
    public static final String QUEUE_PRIORITY = "document_parse_queue_priority";

    /**
     * 文档解析交换机
     */
    public static final String EXCHANGE = "document_parse_exchange";

    /**
     * 普通队列路由键
     */
    public static final String ROUTING_NORMAL = "document.parse.normal";

    /**
     * 优先队列路由键
     */
    public static final String ROUTING_PRIORITY = "document.parse.priority";

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter documentJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 普通队列监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory normalListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(documentJsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    /**
     * 优先队列监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory priorityListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(documentJsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    /**
     * 普通解析队列
     */
    @Bean
    public Queue documentParseQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000); // 24小时
        args.put("x-max-priority", 10); // 最大优先级
        return new Queue(QUEUE_NORMAL, true, false, false, args);
    }

    /**
     * 优先解析队列
     */
    @Bean
    public Queue documentParsePriorityQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000); // 24小时
        args.put("x-max-priority", 10); // 最大优先级
        return new Queue(QUEUE_PRIORITY, true, false, false, args);
    }

    /**
     * 文档解析交换机
     */
    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    /**
     * 普通队列绑定
     */
    @Bean
    public Binding normalBinding() {
        return BindingBuilder.bind(documentParseQueue())
                .to(documentExchange())
                .with(ROUTING_NORMAL);
    }

    /**
     * 优先队列绑定
     */
    @Bean
    public Binding priorityBinding() {
        return BindingBuilder.bind(documentParsePriorityQueue())
                .to(documentExchange())
                .with(ROUTING_PRIORITY);
    }
}
