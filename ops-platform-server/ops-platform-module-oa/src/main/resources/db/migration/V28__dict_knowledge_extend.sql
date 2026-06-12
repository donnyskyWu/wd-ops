-- V28 · P-GATE-UNMOCK-R S-R1 P0-1 (2026-06-09)
-- 扩展 dict_knowledge_category：原 2 项 → 5 项
-- 原因：前端 KnowledgeCategory 枚举 4 值（case/template/industry/experience），
--       后端字典仅 TEMPLATE_LIB/OPS_TIPS 2 项，表单提交必报 1503。
-- 修复：补 CASE_LIB / INDUSTRY_LIB / EXPERIENCE_LIB 三项。
-- 后续（S-R2）：前端改用 DictSelect 而非硬编码 enum，自动从后端拉字典。

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_knowledge_category', '案例库', 'CASE_LIB', 3, 'ENABLED'),
('dict_knowledge_category', '行业资料', 'INDUSTRY_LIB', 4, 'ENABLED'),
('dict_knowledge_category', '运营经验', 'EXPERIENCE_LIB', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
