-- M10-COL-S-04: 私域转化预置漏斗（桥接表 → M6 漏斗分析闭环）

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_funnel_type', '私域转化', 'PRIVATE_DOMAIN', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

INSERT INTO oa_funnel (id, tenant_id, funnel_name, funnel_type, status, remark, creator, updater) VALUES
(99401, 1, 'SEED-私域转化漏斗', 'PRIVATE_DOMAIN', 1, 'M10-COL-S-04: 奥创好友→桥接→手机/公众号粉丝', 'seed-m10', 'seed-m10')
ON DUPLICATE KEY UPDATE funnel_name = VALUES(funnel_name), funnel_type = VALUES(funnel_type), status = VALUES(status);

INSERT INTO oa_funnel_step (id, funnel_id, step_order, event_code, step_name, creator, updater) VALUES
(99411, 99401, 1, 'AOCHUANG_FRIEND', '奥创好友', 'seed-m10', 'seed-m10'),
(99412, 99401, 2, 'PD_BRIDGE_APPROVED', '已通过桥接', 'seed-m10', 'seed-m10'),
(99413, 99401, 3, 'PD_BRIDGE_PHONE', '手机身份', 'seed-m10', 'seed-m10'),
(99414, 99401, 4, 'PD_BRIDGE_MP_FOLLOWER', '公众号粉丝', 'seed-m10', 'seed-m10')
ON DUPLICATE KEY UPDATE step_name = VALUES(step_name), event_code = VALUES(event_code);
