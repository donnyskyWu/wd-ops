-- 知识库正文支持长文（与 oa_production_content.body LONGTEXT 对齐）
ALTER TABLE oa_knowledge_base
    MODIFY COLUMN content LONGTEXT NULL COMMENT '正文';
