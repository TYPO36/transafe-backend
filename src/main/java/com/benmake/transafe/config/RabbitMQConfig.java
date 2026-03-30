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
 * @author TYPO
 * @since 2026-03-30
 */
@Configuration
public class RabbitMQConfig {

    public static final String TASK_QUEUE = "file.parse.task";
    public static final String RESULT_QUEUE = "file.parse.result";
    public static final String EXCHANGE = "file.parse.exchange";
    public static final String TASK_ROUTING_KEY = "task";
    public static final String RESULT_ROUTING_KEY = "result";

    /**
     * 消息转换器
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
        return factory;
    }

    /**
     * 任务队列
     */
    @Bean
    public Queue taskQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000); // 24小时
        args.put("x-max-priority", 10);
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
     * 交换机
     */
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

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
}