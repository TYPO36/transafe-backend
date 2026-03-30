package com.benmake.transafe.task.service;

import com.benmake.transafe.infra.mq.TaskMessage;
import com.benmake.transafe.task.entity.TaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 任务生产者
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.task:file.parse.task}")
    private String taskQueue;

    /**
     * 发送解析任务
     */
    public void sendParseTask(TaskEntity task) {
        TaskMessage message = TaskMessage.builder()
                .taskId(task.getTaskId())
                .fileId(task.getFileId())
                .userId(task.getUserId())
                .fileName(task.getFileName())
                .fileType(task.getFileType())
                .createdAt(LocalDateTime.now().toString())
                .build();

        rabbitTemplate.convertAndSend(taskQueue, message);
        log.info("发送解析任务: taskId={}, fileId={}", task.getTaskId(), task.getFileId());
    }

    /**
     * 生成任务ID
     */
    public String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}