package com.benmake.transafe.document.parser;

import java.io.InputStream;
import java.util.Map;

/**
 * 文档解析器接口
 *
 * @author TYPO
 * @date 2026-03-31
 */
public interface DocumentParser {

    /**
     * 解析文档
     *
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @param password 密码（可选）
     * @return 解析结果
     */
    ParseResult parse(InputStream inputStream, String fileName, String password);

    /**
     * 检测是否为密码保护
     *
     * @param inputStream 文件输入流
     * @return 是否密码保护
     */
    boolean isPasswordProtected(InputStream inputStream);

    /**
     * 解析结果
     */
    record ParseResult(
            String content,
            Map<String, Object> metadata,
            boolean isPasswordProtected,
            Integer errorCode,
            String errorMessage
    ) {
        /**
         * 创建成功结果
         */
        public static ParseResult success(String content, Map<String, Object> metadata) {
            return new ParseResult(content, metadata, false, 0, null);
        }

        /**
         * 创建密码保护结果
         */
        public static ParseResult passwordProtected() {
            return new ParseResult(null, null, true, 3001, "文件被密码保护");
        }

        /**
         * 创建错误结果
         */
        public static ParseResult error(int errorCode, String errorMessage) {
            return new ParseResult(null, null, false, errorCode, errorMessage);
        }

        /**
         * 判断是否成功
         */
        public boolean isSuccess() {
            return errorCode != null && errorCode == 0;
        }
    }
}
