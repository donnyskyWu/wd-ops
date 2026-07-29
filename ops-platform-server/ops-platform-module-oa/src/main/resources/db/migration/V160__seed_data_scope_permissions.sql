-- 数据权限相关菜单功能权限 seed（Dev Token + @PreAuthorize 对齐 oa-menu-permission-map.csv）

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(1601, 'oa:plan:list', '计划查询', 'M2', 'v160', 'v160'),
(1602, 'oa:custom-query:list', '自定义查询', 'M6', 'v160', 'v160'),
(1603, 'oa:metric:list', '指标管理', 'M6', 'v160', 'v160'),
(1604, 'oa:metric-analysis:list', '指标分析', 'M6', 'v160', 'v160'),
(1605, 'oa:cost:list', '账号成本查询', 'M5', 'v160', 'v160'),
(1606, 'oa:platform-account:list', '平台账号查询', 'M4', 'v160', 'v160'),
(1607, 'oa:account-analysis:list', '账号分析', 'M1', 'v160', 'v160')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- OA_ADMIN 全量
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1601), (1, 1602), (1, 1603), (1, 1604), (1, 1605), (1, 1606), (1, 1607)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- OPS_LEADER / OPS_OPERATOR / FINANCE：业务读权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 1601), (3, 1602), (3, 1603), (3, 1604), (3, 1605), (3, 1606), (3, 1607),
(4, 1601), (4, 1602), (4, 1603), (4, 1604), (4, 1605), (4, 1606), (4, 1607),
(5, 1605), (5, 1606), (5, 1607)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
