-- Football system-server permission assign (用户管理 → 分配权限)
-- Required by: list-user-authors, list-user-userIds, assign-user-role

CREATE TABLE IF NOT EXISTS system_user_author (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    author_id   BIGINT       NOT NULL COMMENT '作者ID',
    creator     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     BIT(1)       DEFAULT b'0' COMMENT '是否删除',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_author_id (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和作者关联表';

CREATE TABLE IF NOT EXISTS system_user_data (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    user_id_union BIGINT       NOT NULL COMMENT '绑定用户ID',
    creator       VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater       VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       BIT(1)       DEFAULT b'0' COMMENT '是否删除',
    tenant_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_id_union (user_id_union)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关联数据表';
