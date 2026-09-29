-- FR-M4 平台账号持有人（Football 用户 SSOT · ADR-056）
ALTER TABLE oa_account
    ADD COLUMN holder_user_id BIGINT NULL COMMENT '持有人 system_users.id' AFTER admin_user_id;

CREATE INDEX idx_oa_account_holder ON oa_account (tenant_id, holder_user_id);

-- FR-M2-010 任务登记发布账号
ALTER TABLE oa_work_task_assignment
    ADD COLUMN publish_account_id BIGINT NULL COMMENT '发布平台账号 oa_account.id' AFTER assignee_id;

CREATE INDEX idx_wt_assignment_publish_account ON oa_work_task_assignment (tenant_id, publish_account_id);
