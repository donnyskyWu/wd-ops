-- M9: 部门表 + 钉钉同步字段 + 权限点

CREATE TABLE IF NOT EXISTS sys_dept (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    parent_id     BIGINT       NULL COMMENT '上级部门 ID，NULL 为根',
    name          VARCHAR(128) NOT NULL,
    ding_dept_id  BIGINT       NULL COMMENT '钉钉部门 ID',
    sort          INT          NOT NULL DEFAULT 0,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    creator       VARCHAR(64)  DEFAULT 'system',
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       VARCHAR(64)  DEFAULT 'system',
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_sys_dept_tenant (tenant_id),
    KEY idx_sys_dept_parent (tenant_id, parent_id),
    UNIQUE KEY uk_sys_dept_tenant_ding (tenant_id, ding_dept_id)
);

ALTER TABLE sys_user ADD COLUMN dept_id BIGINT NULL COMMENT '部门 ID';
ALTER TABLE sys_user ADD COLUMN ding_user_id VARCHAR(64) NULL COMMENT '钉钉用户 ID';

CREATE INDEX idx_sys_user_dept ON sys_user (tenant_id, dept_id);
CREATE UNIQUE INDEX uk_sys_user_tenant_ding ON sys_user (tenant_id, ding_user_id);

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(22, 'oa:dept:list', '部门查询', 'M9', 'm9-dept', 'm9-dept'),
(23, 'oa:dept:create', '部门创建', 'M9', 'm9-dept', 'm9-dept'),
(24, 'oa:dept:update', '部门更新', 'M9', 'm9-dept', 'm9-dept'),
(25, 'oa:dept:delete', '部门删除', 'M9', 'm9-dept', 'm9-dept'),
(26, 'oa:dept:sync-dingtalk', '钉钉部门同步', 'M9', 'm9-dept', 'm9-dept'),
(27, 'oa:user:sync-dingtalk', '钉钉人员同步', 'M9', 'm9-dept', 'm9-dept')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27);
