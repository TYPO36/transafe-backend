package com.benmake.transafe.translate.mq;

import com.benmake.transafe.document.es.DocumentIndex;
import com.benmake.transafe.document.service.DocumentIndexService;
import com.benmake.transafe.translate.config.TranslateRabbitMQConfig;
import com.benmake.transafe.translate.dto.TranslateMessageDTO;
import com.benmake.transafe.translate.dto.TranslationResult;
import com.benmake.transafe.translate.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 翻译队列消费者
 *
 * @author JTP
 * @date 2026-04-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateQueueConsumer {

    private final TranslationService translationService;
    private final DocumentIndexService documentIndexService;
    private final TranslateResultProducer resultProducer;
    private final TranslateTaskProducer taskProducer;

    /**
     * 消费普通翻译队列
     */
    @RabbitListener(
            queues = TranslateRabbitMQConfig.TRANSLATE_QUEUE,
            containerFactory = "translateListenerContainerFactory"
    )
    public void handleNormalMessage(TranslateMessageDTO message) {
        processTranslation(message);
    }

    /**
     * 消费优先翻译队列
     */
    @RabbitListener(
            queues = TranslateRabbitMQConfig.TRANSLATE_QUEUE_PRIORITY,
            containerFactory = "translateListenerContainerFactory"
    )
    public void handlePriorityMessage(TranslateMessageDTO message) {
        processTranslation(message);
    }

    /**
     * 处理翻译任务
     */
    private void processTranslation(TranslateMessageDTO message) {
        log.info("开始翻译任务: fileId={}, taskId={}, targetLang={}",
                message.getFileId(), message.getTaskId(), message.getTargetLang());

        try {
            // 1. 从ES获取原文内容
            DocumentIndex docIndex = documentIndexService.findById(message.getFileId());
            if (docIndex == null) {
                log.warn("文档索引不存在: fileId={}", message.getFileId());
                sendFailureResult(message, "文档索引不存在");
                return;
            }

            String originalContent = docIndex.getContent();
            if (originalContent == null || originalContent.isBlank()) {
                log.warn("文档内容为空: fileId={}", message.getFileId());
                sendFailureResult(message, "文档内容为空");
                return;
            }

            // 2. 调用翻译服务
            String sourceLang = message.getSourceLang() != null ? message.getSourceLang() : "auto";
            TranslationResult result = translationService.translate(
                    originalContent,
                    sourceLang,
                    message.getTargetLang()
            );

            if (!result.success()) {
                log.error("翻译失败: fileId={}, error={}", message.getFileId(), result.errorMessage());
                // 重试逻辑
                if (message.getRetryCount() != null && message.getRetryCount() < 3) {
                    taskProducer.sendForRetry(message);
                } else {
                    sendFailureResult(message, result.errorMessage());
                }
                return;
            }

            // 3. 更新ES的translatedContent
            docIndex.setTranslatedContent(result.translatedText());
            documentIndexService.save(docIndex);

            // 4. 发送成功结果
            resultProducer.sendSuccessResult(
                    message.getTaskId(),
                    message.getFileId(),
                    message.getUserId(),
                    result.charCount()
            );

            log.info("翻译完成: fileId={}, chars={}, provider={}",
                    message.getFileId(), result.charCount(), result.serviceProvider());

        } catch (Exception e) {
            log.error("翻译任务异常: fileId={}", message.getFileId(), e);
            sendFailureResult(message, e.getMessage());
        }
    }

    /**
     * 发送失败结果
     */
    private void sendFailureResult(TranslateMessageDTO message, String errorMessage) {
        resultProducer.sendFailureResult(
                message.getTaskId(),
                message.getFileId(),
                message.getUserId(),
                errorMessage,
                message.getRetryCount() != null ? message.getRetryCount() : 0
        );
    }
}