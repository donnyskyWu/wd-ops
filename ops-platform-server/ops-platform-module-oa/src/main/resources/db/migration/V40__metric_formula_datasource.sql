-- M6 指标管理：补齐 PRD/API 要求的 formula + data_source 字段（兼容 H2/MySQL）
ALTER TABLE oa_metric ADD COLUMN metric_formula TEXT NULL;
ALTER TABLE oa_metric ADD COLUMN data_source VARCHAR(64) NULL;
