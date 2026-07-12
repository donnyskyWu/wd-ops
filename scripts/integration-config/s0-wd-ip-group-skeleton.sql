-- S0: 最小 IP 组 skeleton — 1 大组 + 2 小组
-- 目标库：localhost:3306/wd ONLY
-- 前置：s0-wd-truncate-testdata.sql 已执行（oa_ip_group 已空）
-- 用户确认：2026-07-05

USE wd;

INSERT INTO oa_ip_group (id, tenant_id, group_name, group_type, parent_id, leader_user_id, sort_order, status, remark, creator, updater)
VALUES
    (9000, 1, '默认大组', 1, NULL, NULL, 1, 1, 'S0 reset skeleton — 大组', 's0-reset', 's0-reset'),
    (9001, 1, '默认小组A', 2, 9000, NULL, 1, 1, 'S0 reset skeleton — 小组', 's0-reset', 's0-reset'),
    (9002, 1, '默认小组B', 2, 9000, NULL, 2, 1, 'S0 reset skeleton — 小组', 's0-reset', 's0-reset');
