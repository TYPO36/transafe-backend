package com.benmake.transafe.document.service;

import com.benmake.transafe.document.es.DocumentIndex;

/**
 * ES文档索引服务接口
 *
 * @author TYPO
 * @since 2026-03-31
 */
public interface DocumentIndexService {

    /**
     * 保存文档索引
     *
     * @param index 文档索引
     */
    void save(DocumentIndex index);

    /**
     * 根据fileId获取文档索引
     *
     * @param fileId 文件唯一标识
     * @return 文档索引
     */
    DocumentIndex findById(String fileId);

    /**
     * 删除文档索引
     *
     * @param fileId 文件唯一标识
     */
    void deleteById(String fileId);

    /**
     * 更新翻译内容
     *
     * @param fileId 文件唯一标识
     * @param translatedContent 翻译后的内容
     */
    void updateTranslatedContent(String fileId, String translatedContent);
}
