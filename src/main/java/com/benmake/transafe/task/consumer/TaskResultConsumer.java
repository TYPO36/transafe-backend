package com.benmake.transafe.task.consumer;

import com.benmake.transafe.infra.mapper.TaskMapper;
import com.benmake.transafe.quota.service.QuotaService;
import com.benmake.transafe.task.entity.TaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务结果消费者
 *
 * @author JTP
 * @date 2026-04-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResultConsumer {

    private final TaskMapper taskMapper;
    private final QuotaService quotaService;
    private final MessageRetryHandler retryHandler;

    /**
     * 接收解析结果
     *
     * <p>处理失败的消息会自动重试，超过最大重试次数后发送到死信队列。</p>
     */
    @RabbitListener(queues = "${rabbitmq.queue.result:file.parse.result}")
    public void handleResult(Message message, Map<String, Object> result) {
        String taskId = (String) result.get("taskId");

        try {
            processResult(result);
            log.info("任务处理成功: taskId={}", taskId);
        } catch (Exception e) {
            log.error("任务处理失败: taskId={}, error={}", taskId, e.getMessage(), e);
            // 处理失败，尝试重试
            retryHandler.handleTaskResultFailure(message);
        }
    }

    /**
     * 处理解析结果
     */
    @Transactional
    protected void processResult(Map<String, Object> result) {
        String taskId = (String) result.get("taskId");
        String status = (String) result.get("status");

        log.info("处理解析结果: taskId={}, status={}", taskId, status);

        TaskEntity task = taskMapper.findByTaskId(taskId).orElse(null);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }

        task.setStatus(status);

        if ("SUCCESS".equals(status)) {
            // 获取字符数
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            if (metadata != null && metadata.get("charCount") != null) {
                Integer charCount = ((Number) metadata.get("charCount")).intValue();
                // 将结果存入 result 字段（JSON格式）
                task.setResult("{\"charCount\":" + charCount + "}");

                // 更新配额消耗
                quotaService.consumeTranslationQuota(task.getUserId(), charCount);
            }
        } else if ("FAILED".equals(status)) {
            task.setErrorMessage((String) result.get("errorMessage"));
        }

        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("任务状态更新完成: taskId={}, status={}", taskId, status);
    }
}