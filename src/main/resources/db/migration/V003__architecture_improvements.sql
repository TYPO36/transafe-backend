-- ============================================================
-- Transafe 数据库架构改进迁移脚本
-- 版本：V003
-- 日期：2026-04-01
-- 说明：添加逻辑删除字段、乐观锁版本字段、关键索引
-- ============================================================

-- ============================================================
-- 1. 添加逻辑删除字段
-- ============================================================

-- 用户表添加 deleted 字段
ALTER TABLE `user`
ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除'
AFTER `updated_at`;

-- 文件表添加 deleted 和 version 字段
ALTER TABLE `file`
ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除'
AFTER `updated_at`,
ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号'
AFTER `deleted`;

-- 文档表添加 deleted 字段
ALTER TABLE `document`
ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除'
AFTER `updated_at`;

-- 任务表添加 deleted 字段
ALTER TABLE `task`
ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除'
AFTER `completed_at`;

-- 配额表添加 deleted 和 version 字段
ALTER TABLE `quota`
ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除'
AFTER `updated_at`,
ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号'
AFTER `deleted`;

-- ============================================================
-- 2. 添加逻辑删除索引
-- ============================================================

CREATE INDEX `idx_user_deleted` ON `user`(`deleted`);
CREATE INDEX `idx_file_deleted` ON `file`(`deleted`);
CREATE INDEX `idx_document_deleted` ON `document`(`deleted`);
CREATE INDEX `idx_task_deleted` ON `task`(`deleted`);
CREATE INDEX `idx_quota_deleted` ON `quota`(`deleted`);

-- ============================================================
-- 3. 添加业务索引
-- ============================================================

-- 用户表索引
CREATE UNIQUE INDEX `uk_user_username` ON `user`(`username`);
CREATE UNIQUE INDEX `uk_user_email` ON `user`(`email`);
CREATE UNIQUE INDEX `uk_user_phone` ON `user`(`phone`);
CREATE INDEX `idx_user_status` ON `user`(`status`);
CREATE INDEX `idx_user_created_at` ON `user`(`created_at`);

-- 文件表索引
CREATE UNIQUE INDEX `uk_file_file_id` ON `file`(`file_id`);
CREATE INDEX `idx_file_user_id` ON `file`(`user_id`);
CREATE INDEX `idx_file_user_status` ON `file`(`user_id`, `status`);
CREATE INDEX `idx_file_created_at` ON `file`(`created_at`);

-- 文档表索引
CREATE UNIQUE INDEX `uk_document_file_id` ON `document`(`file_id`);
CREATE INDEX `idx_document_user_id` ON `document`(`user_id`);
CREATE INDEX `idx_document_user_status` ON `document`(`user_id`, `parse_status`);
CREATE INDEX `idx_document_root_id` ON `document`(`root_id`);
CREATE INDEX `idx_document_parent_id` ON `document`(`parent_id`);
CREATE INDEX `idx_document_parse_status` ON `document`(`parse_status`);
CREATE INDEX `idx_document_created_at` ON `document`(`created_at`);

-- 任务表索引
CREATE UNIQUE INDEX `uk_task_task_id` ON `task`(`task_id`);
CREATE INDEX `idx_task_user_id` ON `task`(`user_id`);
CREATE INDEX `idx_task_user_status` ON `task`(`user_id`, `status`);
CREATE INDEX `idx_task_status` ON `task`(`status`);
CREATE INDEX `idx_task_created_at` ON `task`(`created_at`);

-- 配额表索引
CREATE UNIQUE INDEX `uk_quota_user_id` ON `quota`(`user_id`);
CREATE INDEX `idx_quota_last_reset_date` ON `quota`(`last_reset_date`);

-- ============================================================
-- 完成
-- ============================================================