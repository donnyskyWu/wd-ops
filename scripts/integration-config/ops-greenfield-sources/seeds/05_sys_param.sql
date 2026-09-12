-- =============================================================================
-- Ops DB (shenyu-ops) — sys_param catalog (tenant_id=1)
-- Sources: V52 / V74 / V167 / V169 / V170 / V175 / V177 (01-shenyu-ops-schema.sql)
-- Work-task SOP IDs stay in 03_sys_param_work_task.sql (placeholders).
-- Secrets stay empty — configure in M8「系统参数」after deploy.
-- Idempotent: INSERT … WHERE NOT EXISTS (does not overwrite existing values).
-- Version: 2026-08-27
-- =============================================================================
SET NAMES utf8mb4;

-- ----- V52 m9-seed -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '数据采集间隔（秒）', 'collect.interval.seconds', '3600', 'NUMBER', 'COLLECT',
       '定时采集任务的时间间隔，单位：秒', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.interval.seconds' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '最大并发采集数', 'collect.max.concurrency', '10', 'NUMBER', 'COLLECT',
       '同时进行的采集任务最大数量', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.max.concurrency' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, 'AI生成内容审核开关', 'ai.content.review.enabled', 'true', 'BOOLEAN', 'AI',
       '是否启用AI生成内容的自动审核流程', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'ai.content.review.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, 'AI内容对话走竞彩Author', 'ai.content.chat-via-jingcai', 'true', 'BOOLEAN', 'AI',
       'true=AI内容对话抽屉走 jingcai.article 异步任务；false=直连 M8 模型 Chat Completions（需 football.ai.scheme-generate-* 仍用于其他场景）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'ai.content.chat-via-jingcai' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '默认AI模型', 'ai.default.model', 'QWEN', 'STRING', 'AI',
       '系统默认使用的AI模型类型', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'ai.default.model' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '数据保留天数', 'data.retention.days', '365', 'NUMBER', 'BASIC',
       '历史数据保留的天数，超过自动清理', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'data.retention.days' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, 'API请求超时时间（毫秒）', 'api.timeout.milliseconds', '30000', 'NUMBER', 'BASIC',
       '外部API请求的超时时间', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'api.timeout.milliseconds' AND deleted = 0);

-- ----- V74 content review + V169 ADR-064 role codes -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '开启一级审核', 'content.review.level1.enabled', 'true', 'BOOLEAN', 'CONTENT_REVIEW',
       '关闭后提交审核将跳过一级审核', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level1.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '开启二级审核', 'content.review.level2.enabled', 'true', 'BOOLEAN', 'CONTENT_REVIEW',
       '关闭后一级通过后直接发布；两级均关闭则提交后直接发布', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level2.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '一级审核角色', 'content.review.level1.role', 'ip_group_leader', 'STRING', 'CONTENT_REVIEW',
       '一级审核角色；ip_group_leader=内容所属IP组组长范围（ADR-064）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level1.role' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '二级审核角色', 'content.review.level2.role', 'ops_manager', 'STRING', 'CONTENT_REVIEW',
       '二级审核角色；ops_manager（ADR-064）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'content.review.level2.role' AND deleted = 0);

-- ----- V167 -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '统一采集调度Cron', 'collect.schedule.cron', '0 0 23 * * ?', 'STRING', 'COLLECT',
       'ADR-061 租户统一采集任务默认每日 23:00', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.schedule.cron' AND deleted = 0);

-- ----- V170 DingTalk / notification (empty secrets) -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉集成启用', 'dingtalk.enabled', 'false', 'BOOLEAN', 'DINGTALK',
       'ADR-026 工作通知主通道；true 启用 asyncsend_v2', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 AppKey', 'dingtalk.client-id', '', 'STRING', 'DINGTALK',
       '企业内部应用 ClientId / AppKey（部署后在 M8 填写，勿写入 SQL）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.client-id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 AppSecret', 'dingtalk.client-secret', '', 'STRING', 'DINGTALK',
       '企业内部应用 ClientSecret（部署后在 M8 填写，勿写入 SQL）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.client-secret' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 CorpId', 'dingtalk.corp-id', '', 'STRING', 'DINGTALK',
       '企业 ID（可选归档）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.corp-id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉 AgentId', 'dingtalk.agent-id', '', 'STRING', 'DINGTALK',
       '工作通知微应用 AgentId', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.agent-id' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉机器人启用', 'dingtalk.robot.enabled', 'false', 'BOOLEAN', 'DINGTALK',
       '工作通知失败时的可选 Webhook 降级', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.robot.enabled' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉机器人 Webhook', 'dingtalk.robot.webhook-url', '', 'STRING', 'DINGTALK',
       '自定义机器人 Webhook URL（含 access_token；部署后填写）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.robot.webhook-url' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '钉钉机器人加签', 'dingtalk.robot.secret', '', 'STRING', 'DINGTALK',
       'Webhook 加签 SEC 密钥（可选；部署后填写）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'dingtalk.robot.secret' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '通知平台根 URL', 'notification.platform-base-url', '', 'STRING', 'NOTIFICATION',
       '钉钉消息跳转链接前缀（如 https://ops.example.com/ops）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'notification.platform-base-url' AND deleted = 0);

-- ----- V175 / V177 collector placeholders -----
INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '外部统一采集调度Cron', 'collect.external.unified.cron', '0 0 22 * * ?', 'STRING', 'COLLECT',
       'ADR-068 统一外部数据采集任务默认每日 22:00', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.external.unified.cron' AND deleted = 0);

INSERT INTO sys_param (tenant_id, param_name, param_key, param_value, param_type, category, remark, creator, updater)
SELECT 1, '外部公众号采集 Cookie', 'collect.external.wechat_official.cookie', '', 'STRING', 'COLLECT',
       'mp.weixin.qq.com 运营后台 Session Cookie（部署后在 M8 填写，勿写入 SQL）', 'deploy-seed', 'deploy-seed'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE tenant_id = 1 AND param_key = 'collect.external.wechat_official.cookie' AND deleted = 0);
