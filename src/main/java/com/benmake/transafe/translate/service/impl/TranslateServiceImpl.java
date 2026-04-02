package com.benmake.transafe.translate.service.impl;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.infra.mapper.DocumentMapper;
import com.benmake.transafe.infra.mapper.TaskMapper;
import com.benmake.transafe.task.entity.TaskEntity;
import com.benmake.transafe.translate.dto.TranslateMessageDTO;
import com.benmake.transafe.translate.mq.TranslateTaskProducer;
import com.benmake.transafe.translate.service.TranslateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 翻译服务实现
 *
 * @author JTP
 * @date 2026-04-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateServiceImpl implements TranslateService {

    private final DocumentMapper documentMapper;
    private final TaskMapper taskMapper;
    private final TranslateTaskProducer translateTaskProducer;

    @Override
    @Transactional
    public String translateDocument(String fileId, Long userId, String targetLang, String sourceLang) {
        // 查询文档
        DocumentEntity doc = documentMapper.findByFileId(fileId)
                .filter(d -> d.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // 更新翻译参数
        doc.setNeedTranslate(true);
        doc.setTargetLang(targetLang);
        doc.setSourceLang(sourceLang != null ? sourceLang : "auto");
        doc.setTranslateStatus("pending");
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        // 发送翻译消息
        boolean isVip = doc.getPriority() != null && doc.getPriority() == 1;
        String taskId = sendTranslateMessage(doc, isVip);

        log.info("创建翻译任务: fileId={}, taskId={}, targetLang={}", fileId, taskId, targetLang);

        return taskId;
    }

    /**
     * 发送翻译消息
     */
    private String sendTranslateMessage(DocumentEntity doc, boolean isVip) {
        // 创建翻译任务
        TaskEntity translateTask = new TaskEntity();
        translateTask.setTaskId(generateTaskId());
        translateTask.setUserId(doc.getUserId());
        translateTask.setDocumentId(doc.getId());
        translateTask.setTaskType("TRANSLATE");
        translateTask.setStatus("pending");
        translateTask.setCreatedAt(LocalDateTime.now());

        taskMapper.insert(translateTask);

        // 发送翻译消息
        TranslateMessageDTO message = TranslateMessageDTO.builder()
                .fileId(doc.getFileId())
                .taskId(translateTask.getTaskId())
                .userId(doc.getUserId())
                .sourceLang(doc.getSourceLang() != null ? doc.getSourceLang() : "auto")
                .targetLang(doc.getTargetLang())
                .priority(doc.getPriority())
                .retryCount(0)
                .timestamp(LocalDateTime.now())
                .build();

        translateTaskProducer.sendByPriority(message, isVip);

        return translateTask.getTaskId();
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}