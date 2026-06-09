-- M2 内容生产：SOP 模板 / 任务 / 生产内容 / 知识库

CREATE TABLE IF NOT EXISTS oa_sop_template (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    template_name   VARCHAR(100) NOT NULL,
    content_type    VARCHAR(32)  NOT NULL,
    platform_type   VARCHAR(32)  NOT NULL,
    description     VARCHAR(500) NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_sop_template_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS oa_sop_node (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_id         BIGINT       NOT NULL,
    node_name           VARCHAR(50)  NOT NULL,
    node_order          INT          NOT NULL DEFAULT 0,
    executor_role       VARCHAR(32)  NOT NULL,
    need_review         TINYINT      NOT NULL DEFAULT 0,
    reviewer_role       VARCHAR(32)  NULL,
    predecessors_json   VARCHAR(500) NULL,
    parallel_group      VARCHAR(32)  NULL,
    sla_hours           INT          NOT NULL DEFAULT 24,
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_sop_node_template (template_id)
);

CREATE TABLE IF NOT EXISTS oa_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    template_id     BIGINT       NOT NULL,
    node_id         BIGINT       NOT NULL,
    plan_name       VARCHAR(100) NULL,
    assignee_id     BIGINT       NOT NULL,
    ip_group_id     BIGINT       NULL,
    author_id       BIGINT       NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    need_review     TINYINT      NOT NULL DEFAULT 0,
    sla_deadline    TIMESTAMP    NULL,
    deliverables    VARCHAR(500) NULL,
    start_time      TIMESTAMP    NULL,
    complete_time   TIMESTAMP    NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_task_tenant (tenant_id),
    KEY idx_oa_task_assignee (tenant_id, assignee_id),
    KEY idx_oa_task_template (tenant_id, template_id)
);

CREATE TABLE IF NOT EXISTS oa_sop_review (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    task_id         BIGINT       NOT NULL,
    reviewer_id     BIGINT       NULL,
    reviewer_role   VARCHAR(32)  NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    comment         VARCHAR(500) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_sop_review_task (task_id),
    KEY idx_oa_sop_review_reviewer (tenant_id, reviewer_id)
);

CREATE TABLE IF NOT EXISTS oa_production_content (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              TEXT         NOT NULL,
    cover_image       VARCHAR(500) NULL,
    creator_user_id   BIGINT       NOT NULL,
    account_id        BIGINT       NOT NULL,
    platform_type     VARCHAR(32)  NOT NULL,
    content_type      VARCHAR(32)  NOT NULL,
    status            VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    ai_generated      TINYINT      NOT NULL DEFAULT 0,
    creator           VARCHAR(64)  DEFAULT 'system',
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           VARCHAR(64)  DEFAULT 'system',
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_prod_content_tenant (tenant_id),
    KEY idx_oa_prod_content_account (tenant_id, account_id),
    KEY idx_oa_prod_content_status (tenant_id, status)
);

CREATE TABLE IF NOT EXISTS oa_review_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    content_id      BIGINT       NOT NULL,
    stage           VARCHAR(32)  NOT NULL,
    action          VARCHAR(32)  NOT NULL,
    reviewer_id     BIGINT       NOT NULL,
    comment         VARCHAR(500) NULL,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_review_record_content (content_id)
);

CREATE TABLE IF NOT EXISTS oa_knowledge_base (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    title           VARCHAR(100) NOT NULL,
    content         TEXT         NULL,
    category        VARCHAR(32)  NULL,
    tags            VARCHAR(200) NULL,
    is_public       TINYINT      NOT NULL DEFAULT 1,
    status          TINYINT      NOT NULL DEFAULT 1,
    creator         VARCHAR(64)  DEFAULT 'system',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT 'system',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_oa_knowledge_tenant (tenant_id)
);

-- M2 字典
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_content_type', '内容类型', 'ENABLED'),
('dict_sop_node_status', 'SOP任务状态', 'ENABLED'),
('dict_content_status', '生产内容状态', 'ENABLED'),
('dict_review_stage', '审核阶段', 'ENABLED'),
('dict_content_review_result', '内容审核结果', 'ENABLED'),
('dict_knowledge_category', '知识库分类', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_content_type', '全部', 'ALL', 0, 'ENABLED'),
('dict_content_type', '短视频', 'SHORT_VIDEO', 1, 'ENABLED'),
('dict_content_type', '图文', 'ARTICLE', 2, 'ENABLED'),
('dict_content_type', '视频', 'VIDEO', 3, 'ENABLED'),
('dict_sop_node_status', '待执行', 'PENDING', 1, 'ENABLED'),
('dict_sop_node_status', '执行中', 'IN_PROGRESS', 2, 'ENABLED'),
('dict_sop_node_status', '已完成', 'COMPLETED', 3, 'ENABLED'),
('dict_sop_node_status', '待审核', 'PENDING_REVIEW', 4, 'ENABLED'),
('dict_sop_node_status', '审核通过', 'APPROVED', 5, 'ENABLED'),
('dict_sop_node_status', '审核驳回', 'REJECTED', 6, 'ENABLED'),
('dict_sop_node_status', '节点完成', 'DONE', 7, 'ENABLED'),
('dict_content_status', '草稿', 'DRAFT', 1, 'ENABLED'),
('dict_content_status', '待初审', 'PENDING_FIRST_REVIEW', 2, 'ENABLED'),
('dict_content_status', '待复审', 'PENDING_SECOND_REVIEW', 3, 'ENABLED'),
('dict_content_status', '待终审', 'PENDING_FINAL_REVIEW', 4, 'ENABLED'),
('dict_content_status', '已驳回', 'REJECTED', 5, 'ENABLED'),
('dict_content_status', '已发布', 'PUBLISHED', 6, 'ENABLED'),
('dict_content_status', '已下架', 'UNPUBLISHED', 7, 'ENABLED'),
('dict_review_stage', '初审', 'FIRST_REVIEW', 1, 'ENABLED'),
('dict_review_stage', '复审', 'SECOND_REVIEW', 2, 'ENABLED'),
('dict_review_stage', '终审', 'FINAL_REVIEW', 3, 'ENABLED'),
('dict_content_review_result', '通过', 'APPROVE', 1, 'ENABLED'),
('dict_content_review_result', '驳回', 'REJECT', 2, 'ENABLED'),
('dict_knowledge_category', '模板库', 'TEMPLATE_LIB', 1, 'ENABLED'),
('dict_knowledge_category', '运营技巧', 'OPS_TIPS', 2, 'ENABLED'),
('dict_position', '运营官方号', 'OPS_OFFICIAL', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);
