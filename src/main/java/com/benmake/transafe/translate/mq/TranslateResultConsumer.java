package com.benmake.transafe.translate.mq;

import com.benmake.transafe.infra.mapper.TaskMapper;
import com.benmake.transafe.quota.service.QuotaService;
import com.benmake.transafe.task.entity.TaskEntity;
import com.benmake.transafe.translate.config.TranslateRabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 翻译结果消费者
 *
 * <p>处理翻译完成后的结果，更新任务状态和配额</p>
 *
 * @author JTP
 * @date 2026-04-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateResultConsumer {

    private final TaskMapper taskMapper;
    private final QuotaService quotaService;

    /**
     * 消费翻译结果
     */
    @RabbitListener(
            queues = TranslateRabbitMQConfig.TRANSLATE_RESULT_QUEUE,
            containerFactory = "translateResultListenerContainerFactory"
    )
    public void handleResult(Map<String, Object> result) {
        String taskId = (String) result.get("taskId");
        String status = (String) result.get("status");

        log.info("处理翻译结果: taskId={}, status={}", taskId, status);

        try {
            processResult(result);
        } catch (Exception e) {
            log.error("翻译结果处理失败: taskId={}", taskId, e);
        }
    }

    /**
     * 处理翻译结果
     */
    @Transactional
    protected void processResult(Map<String, Object> result) {
        String taskId = (String) result.get("taskId");
        String status = (String) result.get("status");

        TaskEntity task = taskMapper.findByTaskId(taskId).orElse(null);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }

        // 更新任务状态
        task.setStatus(status);
        task.setCompletedAt(LocalDateTime.now());

        if ("completed".equals(status)) {
            // 成功：记录字符数，消耗配额
            Object charCountObj = result.get("charCount");
            if (charCountObj != null) {
                Integer charCount = ((Number) charCountObj).intValue();
                task.setResult("{\"charCount\":" + charCount + "}");

                // 更新配额消耗
                Long userId = task.getUserId();
                if (userId != null) {
                    quotaService.consumeTranslationQuota(userId, charCount);
                }
            }
        } else if ("failed".equals(status)) {
            // 失败：记录错误信息
            task.setErrorMessage((String) result.get("errorMessage"));
        }

        taskMapper.updateById(task);

        log.info("翻译任务状态更新完成: taskId={}, status={}", taskId, status);
    }
}