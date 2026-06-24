-- M10: 采集日志结果摘要（详情展示用，不含凭证）
ALTER TABLE oa_collect_log ADD COLUMN result_json TEXT NULL;
