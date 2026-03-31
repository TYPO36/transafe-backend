package com.benmake.transafe.document.service.impl;

import com.benmake.transafe.document.dto.SearchResultDTO;
import com.benmake.transafe.document.es.DocumentIndex;
import com.benmake.transafe.document.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务实现
 *
 * @author TYPO
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public SearchResultDTO search(String keyword, int page, int size) {
        // 构建高亮查询
        HighlightParameters highlightParams = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .withFragmentSize(150)
                .withNumberOfFragments(3)
                .build();

        List<HighlightField> highlightFields = List.of(
                new HighlightField("content"),
                new HighlightField("fileName")
        );

        Highlight highlight = new Highlight(highlightParams, highlightFields);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m
                        .query(keyword)
                        .fields("content", "fileName", "metadata.emailSubject")))
                .withPageable(PageRequest.of(page - 1, size))
                .withHighlightQuery(new HighlightQuery(highlight, DocumentIndex.class))
                .build();

        SearchHits<DocumentIndex> hits = elasticsearchOperations.search(query, DocumentIndex.class);

        List<SearchResultDTO.SearchItem> items = hits.getSearchHits().stream()
                .map(this::toSearchItem)
                .collect(Collectors.toList());

        return SearchResultDTO.builder()
                .total(hits.getTotalHits())
                .page(page)
                .size(size)
                .items(items)
                .build();
    }

    /**
     * 转换搜索项
     */
    private SearchResultDTO.SearchItem toSearchItem(SearchHit<DocumentIndex> hit) {
        DocumentIndex doc = hit.getContent();

        // 获取高亮内容
        String highlight = "";
        List<String> contentHighlights = hit.getHighlightField("content");
        if (contentHighlights != null && !contentHighlights.isEmpty()) {
            highlight = String.join("...", contentHighlights);
        } else {
            List<String> fileNameHighlights = hit.getHighlightField("fileName");
            if (fileNameHighlights != null && !fileNameHighlights.isEmpty()) {
                highlight = String.join("...", fileNameHighlights);
            }
        }

        return SearchResultDTO.SearchItem.builder()
                .fileId(doc.getFileId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .highlight(highlight)
                .score(hit.getScore())
                .isAttachment(doc.getIsAttachment())
                .build();
    }
}
