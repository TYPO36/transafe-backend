-- V004: 添加用户角色字段和超级管理员
-- 作者: JTP
-- 日期: 2026-04-01

-- 1. 添加 role 字段
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) DEFAULT 'USER' COMMENT '用户角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员' AFTER `status`;

-- 2. 创建超级管理员用户
-- 密码: admin123 (BCrypt加密)
-- 注意: 生产环境请立即修改此密码
INSERT INTO `user` (
    `username`,
    `password`,
    `nickname`,
    `membership_level`,
    `status`,
    `role`,
    `created_at`,
    `updated_at`,
    `deleted`
) VALUES (
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',  -- BCrypt('admin123')
    '超级管理员',
    1,
    'ACTIVE',
    'SUPER_ADMIN',
    NOW(),
    NOW(),
    0
) ON DUPLICATE KEY UPDATE `role` = 'SUPER_ADMIN', `updated_at` = NOW();

-- 3. 更新表结构注释
ALTER TABLE `user` MODIFY COLUMN `membership_level` INT DEFAULT 0 COMMENT '会员等级：0-普通，1-VIP';