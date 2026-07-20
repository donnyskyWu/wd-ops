-- V150: Built-in Football role「IP组长」(code=ip_group_leader)
-- Target DB: Flyway primary = wd（含 system_role 集成 overlay；与 V137/V149 一致）
-- SSOT 说明：生产若角色仅在 shenyu-system，请同步执行 scripts/integration-config/seed-ip-group-leader-role.sql
-- 语义：具备该角色的用户才可被选为 oa_ip_group.leader_user_id（后端校验）；数据范围仍由 ledIpGroupIds 决定，非本角色 data_scope

SET NAMES utf8mb4;

-- ① Football system_role（内置 type=1；data_scope=5 仅本人，避免误授 ALL）
INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    161, 'IP组长', 'ip_group_leader', 20, 5, '', 0, 1,
    'OPS 内置：IP 组组长候选人；指派组长前须授予本角色',
    'flyway', NOW(), 'flyway', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role r
    WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
)
AND NOT EXISTS (
    SELECT 1 FROM system_role r WHERE r.id = 161
);

-- ② 遗留 sys_role（DevAuth / H2 IT 角色查询路径）
INSERT INTO sys_role (id, tenant_id, code, name, status, data_scope, remark, creator, updater)
SELECT 6, 1, 'ip_group_leader', 'IP组长', 'ENABLED', 'SELF',
       'seed · 与 Football system_role.ip_group_leader 对齐', 'flyway', 'flyway'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = 0
);

-- ③ seed 用户 1002（运营组长 / 既有 IP 组组长）授予角色；1003 供 IT 创建小组时选用
INSERT INTO sys_user_role (user_id, role_id)
SELECT 1002, r.id FROM sys_role r
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = 1002 AND ur.role_id = r.id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT 1003, r.id FROM sys_role r
WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = 1003 AND ur.role_id = r.id
  );
