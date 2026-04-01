package com.benmake.transafe.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * @author JTP
 * @since 2026-03-31
 */
@Getter
public enum ErrorCode {

    // 成功
    SUCCESS(0, "操作成功"),

    // 通用错误 1xxx
    PARAM_ERROR(1000, "参数错误"),
    SYSTEM_ERROR(1001, "系统异常，请稍后重试"),

    // 认证错误 2xxx
    AUTH_FAILED(2000, "认证失败"),
    TOKEN_INVALID(2001, "Token无效或已过期"),
    TOKEN_REVOKED(2002, "Token已失效，请重新登录"),
    UNAUTHORIZED(2003, "未认证"),
    ACCESS_DENIED(2004, "无权限访问"),
    LOGIN_FAILED(2005, "用户不存在或密码错误"),
    ACCOUNT_DISABLED(2006, "账户已被禁用"),
    ACCOUNT_LOCKED(2007, "账号已被锁定，请稍后重试"),

    // 用户错误 3xxx
    USER_NOT_FOUND(3000, "用户不存在"),
    EMAIL_EXISTS(3001, "邮箱已被注册"),
    PHONE_EXISTS(3002, "手机号已被注册"),
    USERNAME_EXISTS(3003, "用户名已被注册"),
    USERNAME_NOT_FOUND(3004, "用户名不存在"),

    // 业务错误 4xxx
    TASK_NOT_FOUND(4000, "任务不存在"),
    STORAGE_EXCEEDED(4001, "存储空间不足"),
    QUOTA_EXCEEDED(4002, "翻译配额不足"),
    FILE_NOT_FOUND(4003, "文件不存在"),
    FILE_TYPE_NOT_SUPPORTED(4004, "不支持的文件类型"),
    FILE_TOO_LARGE(4005, "文件大小超出限制");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}