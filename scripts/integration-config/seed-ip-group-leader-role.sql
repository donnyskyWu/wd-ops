-- 手动/集成：在 wd 与（如需要）shenyu-system 各执行一次，保证 Football 角色可见。
-- 与 V150__seed_ip_group_leader_role.sql 对齐：code=ip_group_leader，name=IP组长，type=1 内置

SET NAMES utf8mb4;

INSERT INTO system_role (
    id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    160, 'IP组长', 'ip_group_leader', 20, 5, '', 0, 1,
    'OPS 内置：IP 组组长候选人',
    'integration', NOW(), 'integration', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_role r
    WHERE r.code = 'ip_group_leader' AND r.tenant_id = 1 AND r.deleted = b'0'
);
