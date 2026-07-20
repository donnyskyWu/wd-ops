-- V154: Repair sys_role after V150 reused id=6 (conflicts with V74 DEPT_HEAD at id=6)
-- Ensures DEPT_HEAD remains available in OPS sys_role while ip_group_leader stays seeded.

SET NAMES utf8mb4;

INSERT INTO sys_role (id, tenant_id, code, name, status, data_scope, remark, creator, updater)
SELECT 7, 1, 'DEPT_HEAD', '部门负责人', 'ENABLED', 'ALL',
       'V154 repair: restore DEPT_HEAD after V150 id=6 conflict', 'flyway', 'flyway'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.code = 'DEPT_HEAD' AND r.tenant_id = 1 AND r.deleted = 0
);

INSERT INTO sys_role (tenant_id, code, name, status, data_scope, remark, creator, updater)
SELECT 1, 'ip_group_leader', 'IP组长', 'ENABLED', 'SELF',
       'V154 repair: sys_role ip_group_leader when V150 insert skipped', 'flyway', 'flyway'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = 0
);

INSERT INTO sys_user_role (user_id, role_id)
SELECT 1001, r.id FROM sys_role r
WHERE r.code = 'DEPT_HEAD' AND r.tenant_id = 1 AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = 1001 AND ur.role_id = r.id
  );
