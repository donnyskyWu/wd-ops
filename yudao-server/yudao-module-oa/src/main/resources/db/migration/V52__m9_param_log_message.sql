-- M9 S-04~S-06: 系统参数 / 日志 / 消息 + 字典扩展 + 权限点

ALTER TABLE sys_dict_data ADD COLUMN color_type VARCHAR(32) NULL DEFAULT 'default';
ALTER TABLE sys_dict_data ADD COLUMN remark VARCHAR(512) NULL;

CREATE TABLE IF NOT EXISTS sys_param (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    param_name   VARCHAR(128) NOT NULL,
    param_key    VARCHAR(128) NOT NULL,
    param_value  TEXT         NOT NULL,
    param_type   VARCHAR(32)  NOT NULL DEFAULT 'STRING',
    category     VARCHAR(32)  NOT NULL DEFAULT 'BASIC',
    remark       VARCHAR(512) NULL,
    creator      VARCHAR(64)  DEFAULT 'system',
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater      VARCHAR(64)  DEFAULT 'system',
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_param_tenant_key (tenant_id, param_key),
    KEY idx_sys_param_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    user_id        BIGINT       NULL,
    username       VARCHAR(64)  NULL,
    module         VARCHAR(64)  NOT NULL,
    action         VARCHAR(64)  NOT NULL,
    level          VARCHAR(32)  NOT NULL DEFAULT 'INFO',
    content        TEXT         NULL,
    method         VARCHAR(256) NULL,
    request_params TEXT         NULL,
    response_body  TEXT         NULL,
    ip             VARCHAR(64)  NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'SUCCESS',
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sys_operation_log_tenant (tenant_id),
    KEY idx_sys_operation_log_time (create_time)
);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    user_id     BIGINT       NULL,
    username    VARCHAR(64)  NULL,
    ip          VARCHAR(64)  NULL,
    user_agent  VARCHAR(512) NULL,
    status      VARCHAR(32)  NOT NULL,
    message     VARCHAR(512) NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sys_login_log_tenant (tenant_id),
    KEY idx_sys_login_log_time (create_time)
);

CREATE TABLE IF NOT EXISTS sys_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    title       VARCHAR(256) NOT NULL,
    category    VARCHAR(32)  NOT NULL,
    channel     VARCHAR(128) NULL,
    receiver    VARCHAR(512) NOT NULL,
    content     TEXT         NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    fail_reason VARCHAR(512) NULL,
    send_time   TIMESTAMP    NULL,
    creator     VARCHAR(64)  DEFAULT 'system',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT 'system',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    KEY idx_sys_message_tenant (tenant_id)
);

-- M9 日志/参数字典
INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_log_module', '日志模块', 'ENABLED'),
('dict_log_level', '日志级别', 'ENABLED'),
('dict_log_type', '日志类型', 'ENABLED'),
('dict_param_type', '参数类型', 'ENABLED'),
('dict_param_category', '参数分类', 'ENABLED'),
('dict_message_category', '消息分类', 'ENABLED'),
('dict_message_status', '消息状态', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_log_module', '系统', 'SYSTEM', 1, 'ENABLED'),
('dict_log_module', '用户', 'USER', 2, 'ENABLED'),
('dict_log_module', '账号', 'ACCOUNT', 3, 'ENABLED'),
('dict_log_module', '内容', 'CONTENT', 4, 'ENABLED'),
('dict_log_module', '财务', 'FINANCE', 5, 'ENABLED'),
('dict_log_module', '分析', 'ANALYTICS', 6, 'ENABLED'),
('dict_log_module', '报表', 'REPORT', 7, 'ENABLED'),
('dict_log_module', '配置', 'CONFIG', 8, 'ENABLED'),
('dict_log_module', '采集', 'COLLECT', 9, 'ENABLED'),
('dict_log_level', 'DEBUG', 'DEBUG', 1, 'ENABLED'),
('dict_log_level', 'INFO', 'INFO', 2, 'ENABLED'),
('dict_log_level', 'WARN', 'WARN', 3, 'ENABLED'),
('dict_log_level', 'ERROR', 'ERROR', 4, 'ENABLED'),
('dict_log_type', '登录', 'LOGIN', 1, 'ENABLED'),
('dict_log_type', '操作', 'OPERATION', 2, 'ENABLED'),
('dict_log_type', '异常', 'EXCEPTION', 3, 'ENABLED'),
('dict_log_type', '审计', 'AUDIT', 4, 'ENABLED'),
('dict_param_type', '字符串', 'STRING', 1, 'ENABLED'),
('dict_param_type', '数字', 'NUMBER', 2, 'ENABLED'),
('dict_param_type', '布尔', 'BOOLEAN', 3, 'ENABLED'),
('dict_param_type', 'JSON', 'JSON', 4, 'ENABLED'),
('dict_param_category', '基础配置', 'BASIC', 1, 'ENABLED'),
('dict_param_category', '采集配置', 'COLLECT', 2, 'ENABLED'),
('dict_param_category', 'AI配置', 'AI', 3, 'ENABLED'),
('dict_param_category', '通知配置', 'NOTIFICATION', 4, 'ENABLED'),
('dict_message_category', '预警通知', 'ALERT', 1, 'ENABLED'),
('dict_message_category', '系统通知', 'SYSTEM', 2, 'ENABLED'),
('dict_message_category', '业务通知', 'BUSINESS', 3, 'ENABLED'),
('dict_message_status', '待发送', 'PENDING', 1, 'ENABLED'),
('dict_message_status', '已发送', 'SENT', 2, 'ENABLED'),
('dict_message_status', '发送失败', 'FAILED', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- 默认系统参数 seed（tenant 1）
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater) VALUES
(1, '数据采集间隔（秒）', 'collect.interval.seconds', '3600', 'NUMBER', 'COLLECT', '定时采集任务的时间间隔，单位：秒', 'm9-seed', 'm9-seed'),
(1, '最大并发采集数', 'collect.max.concurrency', '10', 'NUMBER', 'COLLECT', '同时进行的采集任务最大数量', 'm9-seed', 'm9-seed'),
(1, 'AI生成内容审核开关', 'ai.content.review.enabled', 'true', 'BOOLEAN', 'AI', '是否启用AI生成内容的自动审核流程', 'm9-seed', 'm9-seed'),
(1, '默认AI模型', 'ai.default.model', 'QWEN', 'STRING', 'AI', '系统默认使用的AI模型类型', 'm9-seed', 'm9-seed'),
(1, '数据保留天数', 'data.retention.days', '365', 'NUMBER', 'BASIC', '历史数据保留的天数，超过自动清理', 'm9-seed', 'm9-seed'),
(1, 'API请求超时时间（毫秒）', 'api.timeout.milliseconds', '30000', 'NUMBER', 'BASIC', '外部API请求的超时时间', 'm9-seed', 'm9-seed')
ON DUPLICATE KEY UPDATE param_name = VALUES(param_name);

-- 示例日志 seed
INSERT INTO sys_operation_log (tenant_id, user_id, username, module, action, level, content, method, ip, status, create_time) VALUES
(1, 1001, 'oa_admin', 'SYSTEM', 'LOGIN', 'INFO', '管理员登录', 'POST /system/auth/login', '192.168.1.100', 'SUCCESS', TIMESTAMP '2026-06-08 09:00:00'),
(1, 1001, 'oa_admin', 'ACCOUNT', 'CREATE', 'INFO', '新增平台账号 抖音-AI技术前沿', 'POST /oa/account/create', '192.168.1.100', 'SUCCESS', TIMESTAMP '2026-06-08 09:15:00'),
(1, 1002, 'operator1', 'ACCOUNT', 'UPDATE', 'INFO', '修改 抖音-AI技术前沿 账号', 'PUT /oa/account/123', '192.168.1.101', 'SUCCESS', TIMESTAMP '2026-06-08 09:20:00');

INSERT INTO sys_login_log (tenant_id, user_id, username, ip, user_agent, status, message, create_time) VALUES
(1, 1001, 'oa_admin', '192.168.1.100', 'Mozilla/5.0 Chrome/120', 'SUCCESS', '登录成功', TIMESTAMP '2026-06-08 09:00:00'),
(1, 1002, 'operator1', '192.168.1.101', 'Mozilla/5.0 Chrome/120', 'SUCCESS', '登录成功', TIMESTAMP '2026-06-08 09:10:00'),
(1, NULL, 'unknown_user', '192.168.1.200', 'Mozilla/5.0 Chrome/120', 'FAIL', '用户名或密码错误', TIMESTAMP '2026-06-07 18:30:00');

INSERT INTO sys_message (tenant_id, title, category, channel, receiver, content, status, send_time, creator, updater) VALUES
(1, '【严重】播放量异常下跌预警', 'ALERT', 'EMAIL,WECHAT', 'zhangsan@company.com', '检测到抖音账号近3天播放量下降超过50%', 'SENT', TIMESTAMP '2026-05-28 10:30:00', 'm9-seed', 'm9-seed'),
(1, '系统维护通知', 'SYSTEM', 'EMAIL', 'admin@tenant1.local', '系统将于本周六凌晨2点进行维护', 'SENT', TIMESTAMP '2026-05-27 15:00:00', 'm9-seed', 'm9-seed');

-- 权限点
INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(28, 'oa:param:list', '系统参数查询', 'M9', 'm9-ext', 'm9-ext'),
(29, 'oa:param:create', '系统参数创建', 'M9', 'm9-ext', 'm9-ext'),
(30, 'oa:param:update', '系统参数更新', 'M9', 'm9-ext', 'm9-ext'),
(31, 'oa:param:delete', '系统参数删除', 'M9', 'm9-ext', 'm9-ext'),
(32, 'oa:dict:admin-list', '字典管理查询', 'M9', 'm9-ext', 'm9-ext'),
(33, 'oa:dict:create', '字典创建', 'M9', 'm9-ext', 'm9-ext'),
(34, 'oa:dict:update', '字典更新', 'M9', 'm9-ext', 'm9-ext'),
(35, 'oa:dict:delete', '字典删除', 'M9', 'm9-ext', 'm9-ext'),
(36, 'oa:log:operation', '操作日志查询', 'M9', 'm9-ext', 'm9-ext'),
(37, 'oa:log:login', '登录日志查询', 'M9', 'm9-ext', 'm9-ext'),
(38, 'oa:message:list', '消息查询', 'M9', 'm9-ext', 'm9-ext'),
(39, 'oa:message:send', '消息发送', 'M9', 'm9-ext', 'm9-ext'),
(40, 'oa:message:delete', '消息删除', 'M9', 'm9-ext', 'm9-ext')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 28), (1, 29), (1, 30), (1, 31),
(1, 32), (1, 33), (1, 34), (1, 35),
(1, 36), (1, 37),
(1, 38), (1, 39), (1, 40);
