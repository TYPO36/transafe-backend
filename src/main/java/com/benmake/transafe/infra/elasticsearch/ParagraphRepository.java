package com.benmake.transafe.infra.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 段落文档 Repository
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Repository
public interface ParagraphRepository extends ElasticsearchRepository<ParagraphDocument, String> {

    List<ParagraphDocument> findByTaskId(String taskId);

    List<ParagraphDocument> findByTaskIdOrderByParagraphIndex(String taskId);

    List<ParagraphDocument> findByUserId(Long userId);
}