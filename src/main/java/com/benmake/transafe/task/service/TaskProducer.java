package com.benmake.transafe.task.service;

import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.infra.mapper.DocumentMapper;
import com.benmake.transafe.infra.mq.TaskMessage;
import com.benmake.transafe.task.entity.TaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 任务生产者
 *
 * @author JTP
 * @date 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProducer {

    private final RabbitTemplate rabbitTemplate;
    private final DocumentMapper documentMapper;

    @Value("${rabbitmq.queue.task:file.parse.task}")
    private String taskQueue;

    /**
     * 发送解析任务
     */
    public void sendParseTask(TaskEntity task) {
        // 通过 documentId 查询 document 获取 fileId, fileName, fileType
        String fileId = null;
        String fileName = null;
        String fileType = null;

        if (task.getDocumentId() != null) {
            DocumentEntity doc = documentMapper.findByDocumentId(task.getDocumentId()).orElse(null);
            if (doc != null) {
                fileId = doc.getFileId();
                fileName = doc.getFileName();
                fileType = doc.getFileType();
            }
        }

        TaskMessage message = TaskMessage.builder()
                .taskId(task.getTaskId())
                .fileId(fileId)
                .userId(task.getUserId())
                .fileName(fileName)
                .fileType(fileType)
                .createdAt(LocalDateTime.now().toString())
                .build();

        rabbitTemplate.convertAndSend(taskQueue, message);
        log.info("发送解析任务: taskId={}, documentId={}, fileId={}", task.getTaskId(), task.getDocumentId(), fileId);
    }

    /**
     * 生成任务ID
     */
    public String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
