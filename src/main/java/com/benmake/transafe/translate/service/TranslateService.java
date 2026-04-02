package com.benmake.transafe.translate.service;

/**
 * 翻译服务接口
 *
 * @author JTP
 * @date 2026-04-02
 */
public interface TranslateService {

    /**
     * 对已解析的文档进行翻译
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param targetLang 目标语言
     * @param sourceLang 源语言（可选，默认auto）
     * @return 任务ID
     */
    String translateDocument(String fileId, Long userId, String targetLang, String sourceLang);
}