-- 本地 shenyu-ops：M4 平台账号页选择器依赖表 + oa_account 与代码一致的列（幂等）
-- 2026-11-06：从 Flyway V3–V8、V82、V85、V86、V94、V110、V119、V167、V173 摘录；不含 sys_dict_*（本库无该表）
-- 用法：mysql -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 < 本文件

USE `shenyu-ops`;

-- ---------------------------------------------------------------------------
-- oa_company / 扩容（V3 + V94 business_license_keys）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_company (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '公司ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    company_name            VARCHAR(100) NOT NULL COMMENT '公司名称',
    credit_code             VARCHAR(18)  NOT NULL COMMENT '统一社会信用代码',
    industry                VARCHAR(40)  NULL COMMENT '所属行业',
    address                 VARCHAR(200) NULL COMMENT '公司地址',
    legal_name              VARCHAR(64)  NULL COMMENT '法人姓名',
    legal_id_card_encrypted VARCHAR(128) NULL COMMENT '法人身份证号（加密）',
    mp_capacity_standard    INT          NOT NULL DEFAULT 0 COMMENT '公众号容量标准',
    mp_registered_count     INT          NOT NULL DEFAULT 0 COMMENT '已注册公众号数量',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '公司状态',
    business_license_keys   TEXT         NULL COMMENT '营业执照图片Key列表',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_oa_company_credit (tenant_id, credit_code),
    KEY idx_oa_company_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司信息表';

CREATE TABLE IF NOT EXISTS oa_company_expansion (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '扩容记录ID',
    tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
    company_id      BIGINT       NOT NULL COMMENT '公司ID',
    from_capacity   INT          NOT NULL COMMENT '扩容前容量',
    to_capacity     INT          NOT NULL COMMENT '扩容后容量',
    reason          VARCHAR(200) NOT NULL COMMENT '扩容原因',
    operator_name   VARCHAR(64)  NULL COMMENT '操作人姓名',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_oa_company_expansion_company (tenant_id, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司扩容记录表';

-- ---------------------------------------------------------------------------
-- oa_realname / intermediary（V4 + V5 + V94 身份证图）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_realname (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '实名人ID',
    tenant_id           BIGINT       NOT NULL COMMENT '租户ID',
    company_id          BIGINT       NULL COMMENT '所属公司ID',
    real_name           VARCHAR(64)  NOT NULL COMMENT '真实姓名',
    id_type             VARCHAR(32)  NOT NULL DEFAULT 'ID_CARD' COMMENT '证件类型',
    id_card_encrypted   VARCHAR(128) NOT NULL COMMENT '身份证号（加密）',
    phone_encrypted     VARCHAR(128) NOT NULL COMMENT '手机号（加密）',
    wechat              VARCHAR(64)  NULL COMMENT '微信号',
    gender              VARCHAR(16)  NULL COMMENT '性别',
    status              VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    account_bound_count INT          NOT NULL DEFAULT 0 COMMENT '已绑定账号数量',
    id_card_front_key   VARCHAR(512) NULL COMMENT '身份证正面图片Key',
    id_card_back_key    VARCHAR(512) NULL COMMENT '身份证背面图片Key',
    creator             VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater             VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_oa_realname_tenant (tenant_id),
    KEY idx_oa_realname_company (tenant_id, company_id),
    KEY idx_oa_realname_name (tenant_id, real_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实名人信息表';

CREATE TABLE IF NOT EXISTS oa_realname_intermediary (
    id                          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT         NOT NULL,
    realname_id                 BIGINT         NOT NULL,
    intermediary_name           VARCHAR(64)    NOT NULL,
    intermediary_phone_encrypted VARCHAR(128)  NULL,
    intermediary_wechat         VARCHAR(64)    NULL,
    relation_type               VARCHAR(32)    NOT NULL,
    commission_rate             DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    remark                      VARCHAR(200)   NULL,
    creator                     VARCHAR(64)    DEFAULT 'system',
    create_time                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                     VARCHAR(64)    DEFAULT 'system',
    update_time                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                     SMALLINT       NOT NULL DEFAULT 0,
    KEY idx_oa_realname_intermediary_realname (tenant_id, realname_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实名中间人表';

-- ---------------------------------------------------------------------------
-- oa_phone（V6 + V85 扩展字段）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_phone (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '手机ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    realname_id             BIGINT       NULL COMMENT '实名人ID',
    phone_number_encrypted  VARCHAR(128) NOT NULL COMMENT '手机号（加密）',
    phone_number_hash       VARCHAR(64)  NOT NULL COMMENT '手机号哈希值',
    phone_code              VARCHAR(32)  NULL COMMENT '手机验证码',
    phone_model             VARCHAR(100) NULL COMMENT '手机型号',
    settings_screenshot_key VARCHAR(512) NULL,
    front_image_key         VARCHAR(512) NULL,
    back_image_key          VARCHAR(512) NULL,
    purchase_batch          VARCHAR(64)  NULL,
    purchase_date           DATE         NULL,
    purchase_time           TIME         NULL,
    handler_name            VARCHAR(64)  NULL,
    device_number           VARCHAR(64)  NULL,
    is_aochuang             VARCHAR(8)   NULL,
    phone_type              VARCHAR(32)  NULL,
    keeper_id               BIGINT       NULL COMMENT '保管人ID',
    wechat_bound            VARCHAR(64)  NULL COMMENT '绑定的微信号',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    account_bound_count     INT          NOT NULL DEFAULT 0 COMMENT '已绑定账号数量',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_oa_phone_number (tenant_id, phone_number_hash),
    KEY idx_oa_phone_tenant (tenant_id),
    KEY idx_oa_phone_realname (tenant_id, realname_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手机信息表';

-- ---------------------------------------------------------------------------
-- oa_sim_card（V7，不含重复创建 oa_account）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_sim_card (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '手机卡ID',
    tenant_id               BIGINT       NOT NULL COMMENT '租户ID',
    phone_id                BIGINT       NULL COMMENT '所属手机ID',
    phone_number_encrypted  VARCHAR(128) NOT NULL COMMENT '手机号（加密）',
    phone_number_hash       VARCHAR(64)  NOT NULL COMMENT '手机号哈希值',
    is_primary              VARCHAR(8)   NOT NULL DEFAULT 'YES' COMMENT '是否主卡',
    operator                VARCHAR(32)  NOT NULL COMMENT '运营商',
    assigned_user_id        BIGINT       NOT NULL COMMENT '分配用户ID',
    iccid_encrypted         VARCHAR(128) NULL COMMENT 'ICCID（加密）',
    iccid_hash              VARCHAR(64)  NULL COMMENT 'ICCID哈希值',
    package_name            VARCHAR(100) NULL COMMENT '套餐名称',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    account_bound_count     INT          NOT NULL DEFAULT 0 COMMENT '已绑定账号数量',
    creator                 VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    create_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater                 VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    update_time             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 SMALLINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_oa_sim_phone (tenant_id, phone_number_hash),
    KEY idx_oa_sim_tenant (tenant_id),
    KEY idx_oa_sim_phone_id (tenant_id, phone_id),
    KEY idx_oa_sim_operator (tenant_id, operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手机卡信息表';

-- ---------------------------------------------------------------------------
-- oa_account：补齐 AccountDO 字段（按列名检测，忽略 flyway_schema_history）
-- ---------------------------------------------------------------------------

-- V8 cookie
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'cookie_encrypted');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN cookie_encrypted TEXT NULL COMMENT ''Cookie AES-256''', 'SELECT ''skip cookie_encrypted''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- V110 credentials（V119 为 TEXT）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'mp_token_encrypted');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN mp_token_encrypted TEXT NULL COMMENT ''公众号后台 Token AES-256''', 'SELECT ''skip mp_token_encrypted''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'auth_token_encrypted');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN auth_token_encrypted TEXT NULL COMMENT ''平台专用 Token AES-256''', 'SELECT ''skip auth_token_encrypted''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'field_mapping');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN field_mapping TEXT NULL COMMENT ''账号级字段映射 JSON''', 'SELECT ''skip field_mapping''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'app_secret_encrypted');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN app_secret_encrypted TEXT NULL COMMENT ''AppSecret AES-256''', 'SELECT ''skip app_secret_encrypted''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- V82 publish
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'publish_enabled');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN publish_enabled TINYINT NOT NULL DEFAULT 0 COMMENT ''是否配置发布权限''', 'SELECT ''skip publish_enabled''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- V167 collect
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'collect_enabled');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN collect_enabled TINYINT NOT NULL DEFAULT 0 COMMENT ''是否采集''', 'SELECT ''skip collect_enabled''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- V173 live collect flag
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'collect_live_enabled');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN collect_live_enabled TINYINT NOT NULL DEFAULT 0 COMMENT ''是否采集直播数据''', 'SELECT ''skip collect_live_enabled''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- V86 公众号扩展（部分列可能已存在）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'trademark_name');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN trademark_name VARCHAR(128) NULL COMMENT ''商标名称''', 'SELECT ''skip trademark_name''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'email');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN email VARCHAR(128) NULL COMMENT ''邮箱''', 'SELECT ''skip email''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'usage_status');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN usage_status VARCHAR(32) NULL COMMENT ''使用状态''', 'SELECT ''skip usage_status''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'original_account_name');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN original_account_name VARCHAR(128) NULL COMMENT ''原公众号名称''', 'SELECT ''skip original_account_name''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'cert_count');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN cert_count INT NOT NULL DEFAULT 0 COMMENT ''公众号认证次数''', 'SELECT ''skip cert_count''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'linked_video_account_id');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN linked_video_account_id BIGINT NULL COMMENT ''关联视频号 oa_account.id''', 'SELECT ''skip linked_video_account_id''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'video_account_registered_at');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN video_account_registered_at TIMESTAMP NULL COMMENT ''视频号注册时间''', 'SELECT ''skip video_account_registered_at''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'admin_name');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN admin_name VARCHAR(64) NULL COMMENT ''管理员姓名''', 'SELECT ''skip admin_name''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND COLUMN_NAME = 'admin_id_card_encrypted');
SET @s := IF(@c = 0, 'ALTER TABLE oa_account ADD COLUMN admin_id_card_encrypted VARCHAR(256) NULL COMMENT ''管理员身份证 AES-256''', 'SELECT ''skip admin_id_card_encrypted''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- V7 索引：若表由 minimal 创建可能缺失
SET @i := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND INDEX_NAME = 'idx_oa_account_phone');
SET @s := IF(@i = 0, 'CREATE INDEX idx_oa_account_phone ON oa_account (tenant_id, phone_id)', 'SELECT ''skip idx_oa_account_phone''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @i := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND INDEX_NAME = 'idx_oa_account_phone_hash');
SET @s := IF(@i = 0, 'CREATE INDEX idx_oa_account_phone_hash ON oa_account (tenant_id, phone_number_hash)', 'SELECT ''skip idx_oa_account_phone_hash''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @i := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_account' AND INDEX_NAME = 'idx_oa_account_sim');
SET @s := IF(@i = 0, 'CREATE INDEX idx_oa_account_sim ON oa_account (tenant_id, sim_card_id)', 'SELECT ''skip idx_oa_account_sim''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- oa_company / oa_realname / oa_phone：若表已存在但缺 V85/V94 列，补齐
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_company' AND COLUMN_NAME = 'business_license_keys');
SET @s := IF(@c = 0, 'ALTER TABLE oa_company ADD COLUMN business_license_keys TEXT NULL COMMENT ''营业执照图片Key列表''', 'SELECT ''skip oa_company.business_license_keys''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_realname' AND COLUMN_NAME = 'id_card_front_key');
SET @s := IF(@c = 0, 'ALTER TABLE oa_realname ADD COLUMN id_card_front_key VARCHAR(512) NULL', 'SELECT ''skip oa_realname.id_card_front_key''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_realname' AND COLUMN_NAME = 'id_card_back_key');
SET @s := IF(@c = 0, 'ALTER TABLE oa_realname ADD COLUMN id_card_back_key VARCHAR(512) NULL', 'SELECT ''skip oa_realname.id_card_back_key''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'settings_screenshot_key');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN settings_screenshot_key VARCHAR(512) NULL', 'SELECT ''skip oa_phone.settings_screenshot_key''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'front_image_key');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN front_image_key VARCHAR(512) NULL', 'SELECT ''skip oa_phone.front_image_key''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'back_image_key');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN back_image_key VARCHAR(512) NULL', 'SELECT ''skip oa_phone.back_image_key''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'purchase_batch');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN purchase_batch VARCHAR(64) NULL', 'SELECT ''skip oa_phone.purchase_batch''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'purchase_date');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN purchase_date DATE NULL', 'SELECT ''skip oa_phone.purchase_date''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'purchase_time');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN purchase_time TIME NULL', 'SELECT ''skip oa_phone.purchase_time''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'handler_name');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN handler_name VARCHAR(64) NULL', 'SELECT ''skip oa_phone.handler_name''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'device_number');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN device_number VARCHAR(64) NULL', 'SELECT ''skip oa_phone.device_number''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'is_aochuang');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN is_aochuang VARCHAR(8) NULL', 'SELECT ''skip oa_phone.is_aochuang''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_phone' AND COLUMN_NAME = 'phone_type');
SET @s := IF(@c = 0, 'ALTER TABLE oa_phone ADD COLUMN phone_type VARCHAR(32) NULL', 'SELECT ''skip oa_phone.phone_type''');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
