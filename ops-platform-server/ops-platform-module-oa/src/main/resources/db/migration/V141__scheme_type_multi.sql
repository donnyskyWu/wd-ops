-- 方案分析类型支持多选：逗号分隔存储，扩展列宽
ALTER TABLE oa_production_content
  MODIFY COLUMN scheme_type VARCHAR(256) NULL COMMENT '赛事方案类型 dict_scheme_type（逗号分隔多值）';

ALTER TABLE oa_ai_content_adopt
  MODIFY COLUMN scheme_type VARCHAR(256) NULL COMMENT '方案类型（逗号分隔多值）';
