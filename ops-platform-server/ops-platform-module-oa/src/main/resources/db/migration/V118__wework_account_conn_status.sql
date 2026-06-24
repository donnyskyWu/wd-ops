-- M10-WECOM-S-02: 企微应用探活状态（ADR-048 · dict_conn_status）

ALTER TABLE oa_wework_account ADD COLUMN conn_status VARCHAR(32) NULL COMMENT 'dict_conn_status' AFTER status;
ALTER TABLE oa_wework_account ADD COLUMN last_health_check_at TIMESTAMP NULL AFTER conn_status;
