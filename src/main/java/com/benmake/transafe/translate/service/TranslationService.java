package com.benmake.transafe.translate.service;

import com.benmake.transafe.translate.dto.TranslationResult;

/**
 * 翻译服务接口（策略模式）
 *
 * <p>支持多种翻译API的统一接口，方便后续扩展</p>
 *
 * @author JTP
 * @date 2026-04-02
 */
public interface TranslationService {

    /**
     * 翻译文本
     *
     * @param text 原文
     * @param sourceLang 源语言（如 "zh", "en", "auto"）
     * @param targetLang 目标语言（如 "zh", "en"）
     * @return 翻译结果
     */
    TranslationResult translate(String text, String sourceLang, String targetLang);

    /**
     * 获取服务名称
     *
     * @return 服务名称标识
     */
    String getServiceName();

    /**
     * 检查服务是否可用
     *
     * @return true 可用，false 不可用
     */
    boolean isAvailable();

    /**
     * 获取剩余配额（字符数）
     *
     * @return 剩余字符数，-1 表示无限制或无法获取
     */
    int getRemainingQuota();
}