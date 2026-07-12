-- GATE-S0 baseline (MySQL 8 + H2 compatible)

CREATE TABLE IF NOT EXISTS sys_tenant (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '租户ID',
    name        VARCHAR(64)  NOT NULL COMMENT '租户名称',
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '租户状态: ENABLED-启用, DISABLED-停用',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除'
) COMMENT='系统租户表';

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    tenant_id   BIGINT       NOT NULL COMMENT '租户ID',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    nickname    VARCHAR(64)  NOT NULL COMMENT '用户昵称',
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态: ENABLED-启用, DISABLED-停用',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    KEY idx_sys_user_tenant (tenant_id)
) COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_user_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '令牌ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    token       VARCHAR(128) NOT NULL COMMENT '访问令牌',
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '令牌状态: ENABLED-有效, DISABLED-失效',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_sys_user_token (token),
    KEY idx_sys_user_token_user (user_id)
) COMMENT='用户令牌表';

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    tenant_id   BIGINT       NOT NULL COMMENT '租户ID',
    code        VARCHAR(64)  NOT NULL COMMENT '角色编码',
    name        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    KEY idx_sys_role_tenant (tenant_id)
) COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    role_id     BIGINT   NOT NULL COMMENT '角色ID',
    creator     VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_user_role (user_id, role_id)
) COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '字典类型ID',
    type        VARCHAR(64)  NOT NULL COMMENT '字典类型编码',
    name        VARCHAR(128) NOT NULL COMMENT '字典类型名称',
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-启用, DISABLED-停用',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_sys_dict_type (type)
) COMMENT='字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '字典数据ID',
    dict_type   VARCHAR(64)  NOT NULL COMMENT '字典类型编码',
    label       VARCHAR(128) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(128) NOT NULL COMMENT '字典值',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-启用, DISABLED-停用',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    UNIQUE KEY uk_sys_dict_data (dict_type, dict_value),
    KEY idx_sys_dict_data_type (dict_type)
) COMMENT='字典数据表';

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '审计日志ID',
    tenant_id   BIGINT       NOT NULL COMMENT '租户ID',
    user_id     BIGINT       NULL COMMENT '操作用户ID',
    username    VARCHAR(64)  NULL COMMENT '操作用户名',
    module      VARCHAR(64)  NOT NULL COMMENT '操作模块',
    action      VARCHAR(64)  NOT NULL COMMENT '操作动作',
    biz_id      VARCHAR(64)  NULL COMMENT '业务ID',
    content     TEXT         NULL COMMENT '操作内容',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_sys_audit_log_tenant (tenant_id)
) COMMENT='系统审计日志表';

CREATE TABLE IF NOT EXISTS oa_demo_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '演示项ID',
    tenant_id   BIGINT       NOT NULL COMMENT '租户ID',
    name        VARCHAR(128) NOT NULL COMMENT '演示项名称',
    creator     VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    KEY idx_oa_demo_item_tenant (tenant_id)
) COMMENT='OA演示项表';
