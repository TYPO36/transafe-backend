package com.benmake.transafe.document.common.enums;

import lombok.Getter;

/**
 * 解析错误码枚举
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Getter
public enum ParseErrorCode {

    /**
     * 解析成功
     */
    SUCCESS(0, "解析成功", false),

    /**
     * 文件被密码保护
     */
    PASSWORD_PROTECTED(3001, "文件被密码保护", true),

    /**
     * 文件格式不支持
     */
    UNSUPPORTED_FORMAT(3002, "文件格式不支持", false),

    /**
     * 文件已损坏
     */
    FILE_CORRUPTED(3003, "文件已损坏", false),

    /**
     * 解析超时(>5分钟)
     */
    PARSE_TIMEOUT(3004, "解析超时(>5分钟)", true),

    /**
     * 存储服务异常
     */
    STORAGE_ERROR(3005, "存储服务异常", true),

    /**
     * 未知错误
     */
    UNKNOWN_ERROR(3099, "未知错误", true);

    private final int code;
    private final String message;
    private final boolean retryable;

    ParseErrorCode(int code, String message, boolean retryable) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }

    /**
     * 根据错误码获取枚举
     *
     * @param code 错误码
     * @return 对应的 ParseErrorCode 或 UNKNOWN_ERROR
     */
    public static ParseErrorCode fromCode(int code) {
        for (ParseErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * 判断是否可重试
     *
     * @return 是否可重试
     */
    public boolean isRetryable() {
        return retryable;
    }
}
