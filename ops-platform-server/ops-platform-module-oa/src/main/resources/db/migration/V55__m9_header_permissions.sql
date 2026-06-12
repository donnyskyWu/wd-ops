-- M9 header: personal center + message inbox permissions (self-service, not admin CRUD)

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(41, 'oa:user:profile', '个人中心查看', 'M9', 'm9-header', 'm9-header'),
(42, 'oa:message:inbox', '消息收件箱', 'M9', 'm9-header', 'm9-header')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Grant inbox/profile to all seeded roles (every dev token user)
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 41), (1, 42),
(2, 41), (2, 42),
(3, 41), (3, 42),
(4, 41), (4, 42),
(5, 41), (5, 42)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Idempotent: ensure OA_ADMIN still has M9 extended permissions from V52
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 28), (1, 29), (1, 30), (1, 31),
(1, 32), (1, 33), (1, 34), (1, 35),
(1, 36), (1, 37),
(1, 38), (1, 39), (1, 40)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Demo unread messages for header inbox (receiver matches oa-admin username / email)
INSERT INTO sys_message (tenant_id, title, category, channel, receiver, content, status, send_time, creator, updater) VALUES
(1, '欢迎使用运营数据平台', 'SYSTEM', 'STATION', 'oa-admin', '您可在头部消息中心查看系统通知与业务提醒。', 'SENT', TIMESTAMP '2026-06-11 09:00:00', 'v55-seed', 'v55-seed'),
(1, '个人中心已开通', 'SYSTEM', 'STATION', 'admin@tenant1.local', '点击右上角头像可查看个人资料与未读消息。', 'SENT', TIMESTAMP '2026-06-11 09:05:00', 'v55-seed', 'v55-seed');
