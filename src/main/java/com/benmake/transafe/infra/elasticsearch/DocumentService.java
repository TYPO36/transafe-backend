package com.benmake.transafe.infra.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ES 文档服务
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final ParagraphRepository paragraphRepository;

    /**
     * 保存段落
     */
    public ParagraphDocument saveParagraph(String taskId, Long userId, String fileId,
                                           Integer paragraphIndex, String content) {
        ParagraphDocument doc = ParagraphDocument.builder()
                .id(UUID.randomUUID().toString())
                .taskId(taskId)
                .userId(userId)
                .fileId(fileId)
                .paragraphId(UUID.randomUUID().toString())
                .paragraphIndex(paragraphIndex)
                .originalContent(content)
                .createdAt(LocalDateTime.now())
                .build();

        return paragraphRepository.save(doc);
    }

    /**
     * 添加翻译
     */
    public ParagraphDocument addTranslation(String paragraphId, String selectedText,
                                            String translatedText, Integer startIndex, Integer endIndex) {
        ParagraphDocument doc = paragraphRepository.findById(paragraphId).orElse(null);
        if (doc == null) {
            log.warn("段落不存在: paragraphId={}", paragraphId);
            return null;
        }

        ParagraphDocument.Translation translation = ParagraphDocument.Translation.builder()
                .id(UUID.randomUUID().toString())
                .selectedText(selectedText)
                .translatedText(translatedText)
                .startIndex(startIndex)
                .endIndex(endIndex)
                .translatedAt(LocalDateTime.now())
                .build();

        doc.getTranslations().add(translation);
        return paragraphRepository.save(doc);
    }

    /**
     * 获取任务的所有段落
     */
    public List<ParagraphDocument> getTaskParagraphs(String taskId) {
        return paragraphRepository.findByTaskIdOrderByParagraphIndex(taskId);
    }

    /**
     * 删除任务的所有段落
     */
    public void deleteByTaskId(String taskId) {
        List<ParagraphDocument> paragraphs = paragraphRepository.findByTaskId(taskId);
        paragraphRepository.deleteAll(paragraphs);
        log.info("删除任务段落: taskId={}, count={}", taskId, paragraphs.size());
    }
}