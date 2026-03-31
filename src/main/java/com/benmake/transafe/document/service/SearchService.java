package com.benmake.transafe.document.service;

import com.benmake.transafe.document.dto.SearchResultDTO;

/**
 * 搜索服务接口
 *
 * @author TYPO
 * @since 2026-03-31
 */
public interface SearchService {

    /**
     * 全文搜索
     *
     * @param keyword 关键词
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 搜索结果
     */
    SearchResultDTO search(String keyword, int page, int size);
}
