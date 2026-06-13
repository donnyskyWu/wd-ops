-- M2: support long AI-generated article body (TEXT 64KB -> LONGTEXT)
ALTER TABLE oa_production_content
    MODIFY COLUMN body LONGTEXT NOT NULL COMMENT '正文';
