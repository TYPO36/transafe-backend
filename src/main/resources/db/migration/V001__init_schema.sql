-- ============================================
-- Transafe 完整建表脚本
-- 说明：删除所有表后重建，包含最新结构
-- 日期：2026-04-01
-- ============================================

-- 删除所有表（按依赖顺序）
DROP TABLE IF EXISTS `document`;
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `quota`;
DROP TABLE IF EXISTS `file`;
DROP TABLE IF EXISTS `user`;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常, LOCKED-锁定, DISABLED-禁用',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户, VIP-VIP用户, ADMIN-管理员',
    `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `lock_until` DATETIME COMMENT '账户锁定截止时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_email` (`email`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    KEY `idx_user_status` (`status`),
    KEY `idx_user_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 文件表（存储文件元信息）
-- ============================================
CREATE TABLE `file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `file_id` VARCHAR(32) NOT NULL COMMENT '文件唯一标识（UUID）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(20) COMMENT '文件类型/扩展名',
    `storage_path` VARCHAR(500) NOT NULL COMMENT '存储路径（相对路径）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：UPLOADED-已上传, DELETED-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_file_id` (`file_id`),
    KEY `idx_file_user_id` (`user_id`),
    KEY `idx_file_user_status` (`user_id`, `status`),
    KEY `idx_file_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件存储表';

-- ============================================
-- 文档表（解析任务和内容）
-- ============================================
CREATE TABLE `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `file_id` VARCHAR(32) NOT NULL COMMENT '文件唯一标识，关联 file 表',
    `parent_id` VARCHAR(32) COMMENT '父文档file_id，顶层为null',
    `root_id` VARCHAR(32) COMMENT '根文档file_id',
    `parse_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '解析状态：pending-待解析, parsing-解析中, parsed-已解析, failed-失败',
    `parse_error_code` INT NOT NULL DEFAULT 0 COMMENT '错误码：0成功',
    `parse_error_message` VARCHAR(500) COMMENT '错误信息',
    `password_provided` VARCHAR(100) COMMENT '用户提供的密码（仅密码保护文件）',
    `is_attachment` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为附件：0-否, 1-是',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级：0-普通, 1-优先',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `content` TEXT COMMENT '解析后的文本内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_file_id` (`file_id`),
    KEY `idx_document_root_id` (`root_id`),
    KEY `idx_document_parent_id` (`parent_id`),
    KEY `idx_document_parse_status` (`parse_status`),
    KEY `idx_document_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档解析表';

-- ============================================
-- 任务表
-- ============================================
CREATE TABLE `task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(32) NOT NULL COMMENT '任务唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待处理, processing-处理中, completed-已完成, failed-失败',
    `result` TEXT COMMENT '任务结果（JSON）',
    `error_message` VARCHAR(500) COMMENT '错误信息',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_task_id` (`task_id`),
    KEY `idx_task_user_id` (`user_id`),
    KEY `idx_task_status` (`status`),
    KEY `idx_task_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ============================================
-- 配额表
-- ============================================
CREATE TABLE `quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `storage_used` BIGINT NOT NULL DEFAULT 0 COMMENT '已使用存储空间（字节）',
    `storage_limit` BIGINT NOT NULL DEFAULT 1073741824 COMMENT '存储空间上限（默认1GB）',
    `daily_upload_used` INT NOT NULL DEFAULT 0 COMMENT '今日已上传次数',
    `daily_upload_limit` INT NOT NULL DEFAULT 100 COMMENT '每日上传上限',
    `last_reset_date` DATE COMMENT '上次重置日期',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quota_user_id` (`user_id`),
    KEY `idx_quota_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配额表';

-- ============================================
-- 初始化管理员账户
-- ============================================
INSERT INTO `user` (`username`, `password`, `email`, `nickname`, `status`, `role`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@transafe.com', '管理员', 'ACTIVE', 'ADMIN');

-- ============================================
-- 完成
-- ============================================