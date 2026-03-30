package com.benmake.transafe.task.consumer;

import com.benmake.transafe.quota.service.QuotaService;
import com.benmake.transafe.task.entity.TaskEntity;
import com.benmake.transafe.task.repository.TaskRepository;
import com.benmake.transafe.task.service.TaskProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 任务结果消费者
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResultConsumer {

    private final TaskRepository taskRepository;
    private final QuotaService quotaService;

    /**
     * 接收解析结果
     */
    @RabbitListener(queues = "${rabbitmq.queue.result:file.parse.result}")
    @Transactional
    public void handleResult(Map<String, Object> result) {
        String taskId = (String) result.get("taskId");
        String status = (String) result.get("status");

        log.info("接收解析结果: taskId={}, status={}", taskId, status);

        TaskEntity task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }

        task.setStatus(status);

        if ("SUCCESS".equals(status)) {
            // 获取字符数
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            if (metadata != null && metadata.get("charCount") != null) {
                Integer charCount = ((Number) metadata.get("charCount")).intValue();
                task.setCharCount(charCount);

                // 更新配额消耗
                quotaService.consumeTranslationQuota(task.getUserId(), charCount);
            }
        } else if ("FAILED".equals(status)) {
            task.setErrorMessage((String) result.get("errorMessage"));
        }

        task.setCompletedAt(java.time.LocalDateTime.now());
        taskRepository.save(task);

        log.info("任务状态更新完成: taskId={}, status={}", taskId, status);
    }
}