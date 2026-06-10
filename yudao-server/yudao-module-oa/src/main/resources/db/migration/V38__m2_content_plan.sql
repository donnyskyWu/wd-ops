-- ADR-011: 内容生产计划管理 + 任务联动

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_plan_status', '计划状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_plan_status', '草稿', 'DRAFT', 1, 'ENABLED'),
('dict_plan_status', '进行中', 'IN_PROGRESS', 2, 'ENABLED'),
('dict_plan_status', '终止审批中', 'TERMINATE_PENDING', 3, 'ENABLED'),
('dict_plan_status', '已终止', 'TERMINATED', 4, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

CREATE TABLE IF NOT EXISTS oa_content_plan (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    plan_name       VARCHAR(100) NOT NULL,
    template_id     BIGINT       NOT NULL,
    ip_group_id     BIGINT       NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    description     VARCHAR(500) NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_content_plan_tenant (tenant_id),
    KEY idx_oa_content_plan_status (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS oa_content_plan_competition (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    plan_id         BIGINT       NOT NULL,
    competition_id  VARCHAR(64)  NOT NULL,
    competition_name VARCHAR(200) NOT NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_plan_comp_plan (tenant_id, plan_id)
);

CREATE TABLE IF NOT EXISTS oa_content_plan_step (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    plan_id             BIGINT       NOT NULL,
    node_id             BIGINT       NOT NULL,
    assignee_ids_json   VARCHAR(500) NOT NULL COMMENT '执行人 ID JSON 数组',
    scheduled_start     TIMESTAMP    NULL,
    scheduled_end       TIMESTAMP    NULL,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_plan_step_plan (tenant_id, plan_id)
);

ALTER TABLE oa_task ADD COLUMN plan_id BIGINT NULL COMMENT '关联计划 ID';
ALTER TABLE oa_task ADD COLUMN visible_in_list TINYINT NOT NULL DEFAULT 1 COMMENT '0=计划草稿期隐藏';
ALTER TABLE oa_task ADD COLUMN scheduled_start TIMESTAMP NULL;
ALTER TABLE oa_task ADD COLUMN scheduled_end TIMESTAMP NULL;

ALTER TABLE oa_task ADD KEY idx_oa_task_plan (tenant_id, plan_id);
