-- M6 指标构建器配置（含参数化查询条件）
ALTER TABLE oa_metric ADD COLUMN params_json TEXT NULL COMMENT '指标构建器 JSON 配置';
