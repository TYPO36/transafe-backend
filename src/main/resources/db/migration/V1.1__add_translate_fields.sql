-- ============================================================
-- 翻译功能数据库迁移脚本
-- 版本: V1.1
-- 日期: 2026-04-02
-- 描述: 为 document 表添加翻译相关字段
-- ============================================================

-- 为 document 表添加翻译相关字段
ALTER TABLE document ADD COLUMN need_translate TINYINT(1) DEFAULT 0 COMMENT '是否需要翻译';
ALTER TABLE document ADD COLUMN target_lang VARCHAR(10) DEFAULT NULL COMMENT '翻译目标语言（如 zh, en）';
ALTER TABLE document ADD COLUMN source_lang VARCHAR(10) DEFAULT NULL COMMENT '翻译源语言（默认 auto 自动检测）';
ALTER TABLE document ADD COLUMN translate_status VARCHAR(20) DEFAULT NULL COMMENT '翻译状态: null/pending/translating/translated/failed';

-- 添加索引优化查询
CREATE INDEX idx_document_translate_status ON document(translate_status);
CREATE INDEX idx_document_need_translate ON document(need_translate);

-- 验证 task 表是否支持 TRANSLATE 类型
-- task_type 字段应为 VARCHAR 类型，支持 PARSE 和 TRANSLATE 两种值
-- 如果需要确保 task_type 支持 TRANSLATE，可以执行以下检查：
-- SELECT DISTINCT task_type FROM task;