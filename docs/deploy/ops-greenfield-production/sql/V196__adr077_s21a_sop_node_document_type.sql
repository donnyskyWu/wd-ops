-- =============================================================================
-- ADR-077 Slice A / S-21-A 独立回放 SQL
-- 适用：已有库（local / beta / prod / 其他环境）在 Flyway 未自动跑到 V196 时手工执行
--
-- 在 OPS 库执行（表 ALTER）；字典写入同实例的 `shenyu-system`（与 V193 相同跨库约定）。
-- 本机若字典表在 `shenyu-sys`（无 `shenyu-system.system_dict_type`）：把下面 `shenyu-system` 换成 `shenyu-sys` 后再 source，或只跑字典段。
-- 字符集：utf8mb4。禁止 PowerShell 管道喂 mysql（中文会变成 ???）。
--
-- 推荐命令（cmd，按环境改 host/user/db）：
--   mysql -h127.0.0.1 -uroot -proot --default-character-set=utf8mb4 shenyu-ops < V196__adr077_s21a_sop_node_document_type.sql
--
-- 若本文件已由 Flyway V196 执行过：不要再跑 ALTER（会 Duplicate column）。
-- 字典 INSERT 为幂等。
--
-- Rollback（逻辑，不自动执行）：
--   ALTER TABLE oa_sop_node DROP COLUMN document_type;
--   ALTER TABLE oa_task DROP COLUMN document_type;
--   ALTER TABLE oa_production_content DROP COLUMN ai_generate_error;
--   ALTER TABLE oa_production_content DROP COLUMN ai_generate_status;
--   UPDATE `shenyu-system`.system_dict_data SET deleted = b'1', updater = 'rollback-v196', update_time = NOW()
--     WHERE dict_type = 'dict_ai_generate_status' AND deleted = b'0';
--   UPDATE `shenyu-system`.system_dict_type SET deleted = b'1', updater = 'rollback-v196', update_time = NOW()
--     WHERE type = 'dict_ai_generate_status' AND deleted = b'0';
-- =============================================================================
SET NAMES utf8mb4;

ALTER TABLE oa_sop_node
    ADD COLUMN document_type VARCHAR(32) NULL COMMENT 'dict_document_type；仅 CONTENT_GENERATION 必填（ADR-077）' AFTER node_type;

ALTER TABLE oa_task
    ADD COLUMN document_type VARCHAR(32) NULL COMMENT '自 SOP 节点拷贝的 dict_document_type（ADR-077 D2，Slice B 写入）' AFTER node_id;

ALTER TABLE oa_production_content
    ADD COLUMN ai_generate_status VARCHAR(32) NULL COMMENT 'dict_ai_generate_status（ADR-077）' AFTER document_type,
    ADD COLUMN ai_generate_error VARCHAR(500) NULL COMMENT 'jingcai 失败原因，可展示（ADR-077）' AFTER ai_generate_status;

INSERT INTO `shenyu-system`.system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'AI生成状态', 'dict_ai_generate_status', 0, 'ADR-077 S-21-A', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_type st
    WHERE st.type = 'dict_ai_generate_status' AND st.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 1, '排队中', 'QUEUED', 'dict_ai_generate_status', 0, 'info', '', 'confirm 后已入队', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'QUEUED' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 2, '生成中', 'GENERATING', 'dict_ai_generate_status', 0, 'warning', '', '正在调用 jingcai', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'GENERATING' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 3, '成功', 'SUCCESS', 'dict_ai_generate_status', 0, 'success', '', '已写入对应正文列', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'SUCCESS' AND sd.deleted = b'0'
);

INSERT INTO `shenyu-system`.system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 4, '失败', 'FAILED', 'dict_ai_generate_status', 0, 'danger', '', '可重试；ai_generate_error 非空', 'flyway-v196', NOW(), 'flyway-v196', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `shenyu-system`.system_dict_data sd
    WHERE sd.dict_type = 'dict_ai_generate_status' AND sd.value = 'FAILED' AND sd.deleted = b'0'
);
