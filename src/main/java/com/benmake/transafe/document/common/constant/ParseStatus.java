package com.benmake.transafe.document.common.constant;

/**
 * 解析状态常量
 *
 * @author TYPO
 * @date 2026-03-31
 */
public final class ParseStatus {

    /**
     * 待解析
     */
    public static final String PENDING = "pending";

    /**
     * 解析中
     */
    public static final String PARSING = "parsing";

    /**
     * 已解析
     */
    public static final String PARSED = "parsed";

    /**
     * 解析失败
     */
    public static final String FAILED = "failed";

    private ParseStatus() {
        // 私有构造函数，防止实例化
    }
}
