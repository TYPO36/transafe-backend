-- ============================================
-- V006: 合并 document 和 file 表
-- 说明：统一管理文件/文档/邮件，ES 存储内容，MySQL 只存元数据
-- 作者：JTP
-- 日期：2026-04-01
-- ============================================

-- 1. 删除旧表（按依赖顺序）
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `document`;
DROP TABLE IF EXISTS `file`;
DROP TABLE IF EXISTS `quota`;
DROP TABLE IF EXISTS `user`;

-- 2. 创建用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `membership_level` INT NOT NULL DEFAULT 0 COMMENT '会员等级：0-普通，1-VIP',
    `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常，LOCKED-锁定，DISABLED-禁用',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员',
    `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `lock_until` DATETIME COMMENT '账户锁定截止时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_email` (`email`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    KEY `idx_user_status` (`status`),
    KEY `idx_user_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3. 创建文档表（合并原 file 和 document 表）
-- 统一管理：上传的文件、邮件、以及 ES 内容的快速索引
CREATE TABLE `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` VARCHAR(32) NOT NULL COMMENT '文件唯一标识（UUID）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',

    -- 文件元数据
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(20) COMMENT '文件类型/扩展名',
    `storage_path` VARCHAR(500) NOT NULL COMMENT '存储路径（相对路径）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：UPLOADED-已上传，DELETED-已删除',

    -- 树结构关系（支持批量上传、邮件附件等场景）
    `parent_id` VARCHAR(32) COMMENT '父文档file_id，顶层为null',
    `root_id` VARCHAR(32) COMMENT '根文档file_id，所有关联文档指向顶层',
    `is_attachment` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为附件：0-否，1-是',

    -- 解析状态（MySQL 保留用于业务逻辑，ES 也存一份用于快速检索）
    `parse_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '解析状态：pending-待解析，parsing-解析中，parsed-已解析，failed-失败',
    `parse_error_code` INT NOT NULL DEFAULT 0 COMMENT '错误码：0成功，3001密码保护，3002不支持格式，3003文件损坏，3004解析超时',
    `parse_error_message` VARCHAR(500) COMMENT '错误信息',
    `password_provided` VARCHAR(100) COMMENT '用户提供的密码（仅密码保护文件）',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级：0-普通，1-优先',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',

    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_file_id` (`file_id`),
    KEY `idx_document_user_id` (`user_id`),
    KEY `idx_document_user_status` (`user_id`, `parse_status`),
    KEY `idx_document_root_id` (`root_id`),
    KEY `idx_document_parent_id` (`parent_id`),
    KEY `idx_document_parse_status` (`parse_status`),
    KEY `idx_document_created_at` (`created_at`),
    KEY `idx_document_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表：统一管理文件/文档/邮件，ES存储内容';

-- 4. 创建任务表（通用任务表，支持解析、翻译等不同任务类型）
CREATE TABLE `task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` VARCHAR(32) NOT NULL COMMENT '任务唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `document_id` BIGINT COMMENT '关联文档ID（指向 document 表的主键）',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型：PARSE-解析任务，TRANSLATE-翻译任务',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待处理，processing-处理中，completed-已完成，failed-失败',
    `result` TEXT COMMENT '任务结果（JSON）',
    `error_message` VARCHAR(500) COMMENT '错误信息',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_task_id` (`task_id`),
    KEY `idx_task_user_id` (`user_id`),
    KEY `idx_task_user_status` (`user_id`, `status`),
    KEY `idx_task_document_id` (`document_id`),
    KEY `idx_task_status` (`status`),
    KEY `idx_task_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表：通用任务表，通过 task_type 区分任务类型';

-- 5. 创建配额表
CREATE TABLE `quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `daily_translation_total` INT NOT NULL DEFAULT 5000 COMMENT '每日翻译额度',
    `daily_translation_used` INT NOT NULL DEFAULT 0 COMMENT '每日翻译已用',
    `storage_total` BIGINT NOT NULL DEFAULT 5368709120 COMMENT '存储空间上限（默认5GB）',
    `storage_used` BIGINT NOT NULL DEFAULT 0 COMMENT '已使用存储空间（字节）',
    `last_reset_date` DATE COMMENT '上次重置日期',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quota_user_id` (`user_id`),
    KEY `idx_quota_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配额表：用户资源配额管理';

-- 6. 初始化管理员账户
-- 密码: admin123 (BCrypt加密)
INSERT INTO `user` (`username`, `password`, `email`, `nickname`, `status`, `role`, `created_at`, `updated_at`, `deleted`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@transafe.com', '管理员', 'ACTIVE', 'ADMIN', NOW(), NOW(), 0);

-- 7. ES 索引说明
-- document 表的 ES 索引需要同步更新，主要字段：
-- {
--   "document_id": Long,        -- MySQL document.id（主键）
--   "file_id": String,          -- MySQL document.file_id（唯一标识）
--   "user_id": Long,
--   "content": Text,            -- 解析后的文本内容
--   "parse_status": Keyword,    -- pending/parsing/parsed/failed
--   "file_name": Text,
--   "file_type": Keyword,
--   "created_at": Date
-- }

-- ============================================
-- 完成
-- ============================================
