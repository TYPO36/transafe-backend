package com.benmake.transafe.document.service.impl;

import com.benmake.transafe.document.es.DocumentIndex;
import com.benmake.transafe.document.service.DocumentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

/**
 * ES文档索引服务实现
 *
 * <p>ES索引服务采用优雅降级策略：</p>
 * <ul>
 *   <li>ES服务可用时：正常索引文档内容</li>
 *   <li>ES服务不可用时：记录错误日志，不影响主流程</li>
 * </ul>
 *
 * @author TYPO
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexServiceImpl implements DocumentIndexService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * ES服务是否可用的标志
     */
    private volatile boolean esAvailable = true;

    @Override
    public void save(DocumentIndex index) {
        if (!esAvailable) {
            log.warn("ES服务不可用，跳过索引保存: fileId={}", index.getFileId());
            return;
        }

        try {
            elasticsearchOperations.save(index);
            log.info("ES索引保存成功: fileId={}", index.getFileId());
        } catch (Exception e) {
            log.error("ES索引保存失败: fileId={}, error={}", index.getFileId(), e.getMessage());
            // 标记ES不可用，后续请求跳过ES操作
            esAvailable = false;
            // 不抛出异常，让文档解析流程继续
            log.warn("ES服务已标记为不可用，后续将跳过索引操作");
        }
    }

    @Override
    public DocumentIndex findById(String fileId) {
        if (!esAvailable) {
            log.warn("ES服务不可用，跳过索引查询: fileId={}", fileId);
            return null;
        }

        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.term(t -> t.field("fileId").value(fileId)))
                    .build();
            SearchHits<DocumentIndex> hits = elasticsearchOperations.search(query, DocumentIndex.class);
            if (hits.hasSearchHits()) {
                return hits.getSearchHit(0).getContent();
            }
            return null;
        } catch (Exception e) {
            log.error("ES索引查询失败: fileId={}", fileId, e);
            esAvailable = false;
            return null;
        }
    }

    @Override
    public void deleteById(String fileId) {
        if (!esAvailable) {
            log.warn("ES服务不可用，跳过索引删除: fileId={}", fileId);
            return;
        }

        try {
            elasticsearchOperations.delete(DocumentIndex.builder().fileId(fileId).build());
            log.info("ES索引删除成功: fileId={}", fileId);
        } catch (Exception e) {
            log.error("ES索引删除失败: fileId={}", fileId, e);
            esAvailable = false;
        }
    }

    /**
     * 重置ES可用状态（供外部调用，如健康检查恢复后）
     */
    public void resetEsAvailable() {
        this.esAvailable = true;
        log.info("ES服务已恢复，重新启用索引操作");
    }

    @Override
    public void updateTranslatedContent(String fileId, String translatedContent) {
        if (!esAvailable) {
            log.warn("ES服务不可用，跳过翻译内容更新: fileId={}", fileId);
            return;
        }

        try {
            DocumentIndex index = findById(fileId);
            if (index == null) {
                log.warn("文档索引不存在，无法更新翻译内容: fileId={}", fileId);
                return;
            }

            index.setTranslatedContent(translatedContent);
            elasticsearchOperations.save(index);
            log.info("翻译内容更新成功: fileId={}, contentLength={}", fileId, translatedContent.length());
        } catch (Exception e) {
            log.error("翻译内容更新失败: fileId={}", fileId, e);
            esAvailable = false;
        }
    }
}
