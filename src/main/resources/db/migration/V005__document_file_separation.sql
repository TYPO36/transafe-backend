-- ============================================
-- V005: Document/File 表拆分重构
-- 说明：document 表只保留解析相关字段，文件元信息统一由 file 表管理
-- 作者：JTP
-- 日期：2026-04-01
-- ============================================

-- 1. 为 document 表新增 content 字段（存储解析后的文本内容）
ALTER TABLE document ADD COLUMN content TEXT COMMENT '解析后的文本内容';

-- 2. 删除 document 表的冗余字段（这些信息已在 file 表中）
-- 注意：执行前请确保已备份数据，并且 file 表中已有对应记录
ALTER TABLE document DROP COLUMN user_id;
ALTER TABLE document DROP COLUMN file_name;
ALTER TABLE document DROP COLUMN file_size;
ALTER TABLE document DROP COLUMN file_type;
ALTER TABLE document DROP COLUMN file_storage_path;

-- 3. 添加外键约束（可选，根据业务需求决定是否启用）
-- 注意：启用外键约束会影响删除操作的性能，请根据实际情况决定
-- ALTER TABLE document ADD CONSTRAINT fk_document_file
--     FOREIGN KEY (file_id) REFERENCES file(file_id) ON DELETE CASCADE;

-- ============================================
-- 数据迁移说明（如果已有数据需要迁移）
-- ============================================
-- 如果 document 表已有数据，需要先执行以下步骤：
--
-- 步骤1：确保 file 表中有对应记录
-- 如果 document 表有数据但 file 表没有，需要先迁移：
--
-- INSERT INTO file (file_id, user_id, file_name, file_size, file_type, storage_path, status, created_at, updated_at)
-- SELECT
--     file_id,
--     user_id,
--     file_name,
--     file_size,
--     file_type,
--     file_storage_path AS storage_path,
--     'UPLOADED' AS status,
--     created_at,
--     updated_at
-- FROM document
-- WHERE file_id NOT IN (SELECT file_id FROM file);
--
-- 步骤2：验证数据迁移完整性
-- SELECT COUNT(*) FROM document d WHERE NOT EXISTS (SELECT 1 FROM file f WHERE f.file_id = d.file_id);
-- 以上查询应返回 0
--
-- 步骤3：确认无误后再执行上面的 DROP COLUMN 操作
-- ============================================

-- 4. 更新表注释
ALTER TABLE document COMMENT '文档解析表：管理文档解析任务、解析状态和解析内容，通过 file_id 关联 file 表获取文件元信息';
ALTER TABLE file COMMENT '文件存储表：管理文件物理存储元信息，包括文件名、大小、类型、存储路径等';