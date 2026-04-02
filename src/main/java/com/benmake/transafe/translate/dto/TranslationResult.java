package com.benmake.transafe.translate.dto;

import java.io.Serializable;

/**
 * 翻译结果
 *
 * @author JTP
 * @date 2026-04-02
 */
public record TranslationResult(
        /**
         * 是否成功
         */
        boolean success,

        /**
         * 翻译后的文本
         */
        String translatedText,

        /**
         * 错误信息
         */
        String errorMessage,

        /**
         * 字符数
         */
        int charCount,

        /**
         * 服务提供商
         */
        String serviceProvider
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建成功结果
     */
    public static TranslationResult success(String text, int charCount, String provider) {
        return new TranslationResult(true, text, null, charCount, provider);
    }

    /**
     * 创建失败结果
     */
    public static TranslationResult failure(String errorMessage) {
        return new TranslationResult(false, null, errorMessage, 0, null);
    }
}