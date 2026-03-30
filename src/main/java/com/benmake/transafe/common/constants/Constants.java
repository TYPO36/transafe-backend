package com.benmake.transafe.common.constants;

/**
 * 系统常量定义
 *
 * @author TYPO
 * @since 2026-03-30
 */
public final class Constants {

    private Constants() {
    }

    /**
     * 用户状态
     */
    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String USER_STATUS_INACTIVE = "INACTIVE";

    /**
     * 任务状态
     */
    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_PROCESSING = "PROCESSING";
    public static final String TASK_STATUS_SUCCESS = "SUCCESS";
    public static final String TASK_STATUS_FAILED = "FAILED";

    /**
     * 会员等级
     */
    public static final int MEMBERSHIP_LEVEL_FREE = 0;
    public static final int MEMBERSHIP_LEVEL_NORMAL = 1;
    public static final int MEMBERSHIP_LEVEL_ADVANCED = 2;
    public static final int MEMBERSHIP_LEVEL_PROFESSIONAL = 3;
    public static final int MEMBERSHIP_LEVEL_ENTERPRISE = 4;

    /**
     * 默认配额
     */
    public static final int DEFAULT_DAILY_TRANSLATION_QUOTA = 5000;
    public static final long DEFAULT_STORAGE_QUOTA = 5368709120L; // 5GB

    /**
     * JWT相关
     */
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";

    /**
     * RabbitMQ
     */
    public static final String QUEUE_TASK = "file.parse.task";
    public static final String QUEUE_RESULT = "file.parse.result";
    public static final String EXCHANGE = "file.parse.exchange";
}