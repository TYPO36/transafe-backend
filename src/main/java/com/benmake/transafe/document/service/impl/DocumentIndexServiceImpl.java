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
 * @author TYPO
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexServiceImpl implements DocumentIndexService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void save(DocumentIndex index) {
        try {
            elasticsearchOperations.save(index);
            log.info("ES索引保存成功: fileId={}", index.getFileId());
        } catch (Exception e) {
            log.error("ES索引保存失败: fileId={}", index.getFileId(), e);
            throw new RuntimeException("ES索引保存失败: " + index.getFileId(), e);
        }
    }

    @Override
    public DocumentIndex findById(String fileId) {
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
            return null;
        }
    }

    @Override
    public void deleteById(String fileId) {
        try {
            elasticsearchOperations.delete(DocumentIndex.builder().fileId(fileId).build());
            log.info("ES索引删除成功: fileId={}", fileId);
        } catch (Exception e) {
            log.error("ES索引删除失败: fileId={}", fileId, e);
        }
    }
}
