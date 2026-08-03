-- ADR-064: OPS six-role test users + system_user_role (idempotent)
-- Prerequisite: seed-ops-six-roles-rbac.sql (roles 160–165) already applied
-- Target: Football shenyu-system.system_users / system_user_role
-- Password: admin123 (BCrypt same as Dev admin)
-- Username rule: AuthLoginReqVO ^[A-Za-z0-9]+$ (no underscore)
-- Apply via utf8mb4 stdin (apply-seed-oa-menu.py), not PowerShell pipe.
SET NAMES utf8mb4;

BEGIN;

SET @pwd_admin123 := '$2a$04$KljJDa/LK7QfDm0lF5OhuePhlPfjRH3tB2Wu351Uidz.oQGJXevPi';

-- Resolve role ids by code (prefer seed ids 160–165; tolerate remapped ids)
SET @role_ip_group_leader := (
    SELECT id FROM system_role WHERE code = 'ip_group_leader' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @role_ops_manager := (
    SELECT id FROM system_role WHERE code = 'ops_manager' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @role_finance := (
    SELECT id FROM system_role WHERE code = 'finance' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @role_content_editor := (
    SELECT id FROM system_role WHERE code = 'content_editor' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @role_ops_operator := (
    SELECT id FROM system_role WHERE code = 'ops_operator' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @role_data_analyst := (
    SELECT id FROM system_role WHERE code = 'data_analyst' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

-- Soft-delete early underscore usernames from first seed draft (login rejects _)
UPDATE system_users
SET deleted = b'1',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE tenant_id = 1
  AND username IN ('ops_leader', 'ops_manager', 'ops_finance', 'ops_editor', 'ops_operator', 'ops_analyst')
  AND deleted = b'0';

UPDATE system_user_role ur
JOIN system_users u ON u.id = ur.user_id
SET ur.deleted = b'1',
    ur.updater = 'adr-064-seed',
    ur.update_time = NOW()
WHERE ur.tenant_id = 1
  AND ur.deleted = b'0'
  AND u.username IN ('ops_leader', 'ops_manager', 'ops_finance', 'ops_editor', 'ops_operator', 'ops_analyst');

-- ===== Users (fixed ids 9160–9165; username-keyed UPSERT) =====

-- 9160 opsleader → ip_group_leader
-- Create when username absent AND (no existing ip_group_leader bind OR seed id already present)
INSERT INTO system_users (
    id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar,
    status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9160, 'opsleader', @pwd_admin123, 'IP组长测试', 'ADR-064 seed · ip_group_leader',
    103, NULL, '', '13900009160', 0, '',
    0, '', NULL, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE @role_ip_group_leader IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_users x WHERE x.id = 9160)
  AND NOT EXISTS (
      SELECT 1 FROM system_users x
      WHERE x.username = 'opsleader' AND x.tenant_id = 1 AND x.deleted = b'0'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM system_user_role ur
      JOIN system_users xu ON xu.id = ur.user_id AND xu.deleted = b'0'
      WHERE ur.role_id = @role_ip_group_leader
        AND ur.tenant_id = 1
        AND ur.deleted = b'0'
        AND xu.username <> 'opsleader'
  );

-- If soft-deleted seed row exists at 9160, revive as opsleader
UPDATE system_users
SET username = 'opsleader',
    password = @pwd_admin123,
    nickname = 'IP组长测试',
    remark = 'ADR-064 seed · ip_group_leader',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9160 AND tenant_id = 1;

UPDATE system_users
SET password = @pwd_admin123,
    nickname = 'IP组长测试',
    remark = 'ADR-064 seed · ip_group_leader',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE username = 'opsleader' AND tenant_id = 1 AND deleted = b'0';

-- 9161 opsmanager → ops_manager
INSERT INTO system_users (
    id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar,
    status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9161, 'opsmanager', @pwd_admin123, '运营主管测试', 'ADR-064 seed · ops_manager',
    103, NULL, '', '13900009161', 0, '',
    0, '', NULL, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_users x
    WHERE x.username = 'opsmanager' AND x.tenant_id = 1 AND x.deleted = b'0'
)
AND NOT EXISTS (SELECT 1 FROM system_users x WHERE x.id = 9161);

UPDATE system_users
SET username = 'opsmanager',
    password = @pwd_admin123,
    nickname = '运营主管测试',
    remark = 'ADR-064 seed · ops_manager',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9161 AND tenant_id = 1;

UPDATE system_users
SET password = @pwd_admin123,
    nickname = '运营主管测试',
    remark = 'ADR-064 seed · ops_manager',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE username = 'opsmanager' AND tenant_id = 1 AND deleted = b'0';

-- 9162 opsfinance → finance
INSERT INTO system_users (
    id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar,
    status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9162, 'opsfinance', @pwd_admin123, '财务人员测试', 'ADR-064 seed · finance',
    103, NULL, '', '13900009162', 0, '',
    0, '', NULL, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_users x
    WHERE x.username = 'opsfinance' AND x.tenant_id = 1 AND x.deleted = b'0'
)
AND NOT EXISTS (SELECT 1 FROM system_users x WHERE x.id = 9162);

UPDATE system_users
SET username = 'opsfinance',
    password = @pwd_admin123,
    nickname = '财务人员测试',
    remark = 'ADR-064 seed · finance',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9162 AND tenant_id = 1;

UPDATE system_users
SET password = @pwd_admin123,
    nickname = '财务人员测试',
    remark = 'ADR-064 seed · finance',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE username = 'opsfinance' AND tenant_id = 1 AND deleted = b'0';

-- 9163 opseditor → content_editor
INSERT INTO system_users (
    id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar,
    status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9163, 'opseditor', @pwd_admin123, '内容编辑测试', 'ADR-064 seed · content_editor',
    103, NULL, '', '13900009163', 0, '',
    0, '', NULL, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_users x
    WHERE x.username = 'opseditor' AND x.tenant_id = 1 AND x.deleted = b'0'
)
AND NOT EXISTS (SELECT 1 FROM system_users x WHERE x.id = 9163);

UPDATE system_users
SET username = 'opseditor',
    password = @pwd_admin123,
    nickname = '内容编辑测试',
    remark = 'ADR-064 seed · content_editor',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9163 AND tenant_id = 1;

UPDATE system_users
SET password = @pwd_admin123,
    nickname = '内容编辑测试',
    remark = 'ADR-064 seed · content_editor',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE username = 'opseditor' AND tenant_id = 1 AND deleted = b'0';

-- 9164 opsoperator → ops_operator
INSERT INTO system_users (
    id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar,
    status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9164, 'opsoperator', @pwd_admin123, '运营测试', 'ADR-064 seed · ops_operator',
    103, NULL, '', '13900009164', 0, '',
    0, '', NULL, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_users x
    WHERE x.username = 'opsoperator' AND x.tenant_id = 1 AND x.deleted = b'0'
)
AND NOT EXISTS (SELECT 1 FROM system_users x WHERE x.id = 9164);

UPDATE system_users
SET username = 'opsoperator',
    password = @pwd_admin123,
    nickname = '运营测试',
    remark = 'ADR-064 seed · ops_operator',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9164 AND tenant_id = 1;

UPDATE system_users
SET password = @pwd_admin123,
    nickname = '运营测试',
    remark = 'ADR-064 seed · ops_operator',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE username = 'opsoperator' AND tenant_id = 1 AND deleted = b'0';

-- 9165 opsanalyst → data_analyst
INSERT INTO system_users (
    id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar,
    status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9165, 'opsanalyst', @pwd_admin123, '数据分析测试', 'ADR-064 seed · data_analyst',
    103, NULL, '', '13900009165', 0, '',
    0, '', NULL, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_users x
    WHERE x.username = 'opsanalyst' AND x.tenant_id = 1 AND x.deleted = b'0'
)
AND NOT EXISTS (SELECT 1 FROM system_users x WHERE x.id = 9165);

UPDATE system_users
SET username = 'opsanalyst',
    password = @pwd_admin123,
    nickname = '数据分析测试',
    remark = 'ADR-064 seed · data_analyst',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9165 AND tenant_id = 1;

UPDATE system_users
SET password = @pwd_admin123,
    nickname = '数据分析测试',
    remark = 'ADR-064 seed · data_analyst',
    status = 0,
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE username = 'opsanalyst' AND tenant_id = 1 AND deleted = b'0';

-- Resolve user ids by username
SET @uid_opsleader := (
    SELECT id FROM system_users WHERE username = 'opsleader' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @uid_opsmanager := (
    SELECT id FROM system_users WHERE username = 'opsmanager' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @uid_opsfinance := (
    SELECT id FROM system_users WHERE username = 'opsfinance' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @uid_opseditor := (
    SELECT id FROM system_users WHERE username = 'opseditor' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @uid_opsoperator := (
    SELECT id FROM system_users WHERE username = 'opsoperator' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);
SET @uid_opsanalyst := (
    SELECT id FROM system_users WHERE username = 'opsanalyst' AND tenant_id = 1 AND deleted = b'0' LIMIT 1
);

-- ===== user_role binds (fixed ids 9160–9165) =====

INSERT INTO system_user_role (
    id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9160, @uid_opsleader, @role_ip_group_leader, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE @uid_opsleader IS NOT NULL
  AND @role_ip_group_leader IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = @uid_opsleader
        AND ur.role_id = @role_ip_group_leader
        AND ur.tenant_id = 1
        AND ur.deleted = b'0'
  )
  AND NOT EXISTS (SELECT 1 FROM system_user_role ur WHERE ur.id = 9160);

INSERT INTO system_user_role (
    id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9161, @uid_opsmanager, @role_ops_manager, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE @uid_opsmanager IS NOT NULL
  AND @role_ops_manager IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = @uid_opsmanager
        AND ur.role_id = @role_ops_manager
        AND ur.tenant_id = 1
        AND ur.deleted = b'0'
  )
  AND NOT EXISTS (SELECT 1 FROM system_user_role ur WHERE ur.id = 9161);

INSERT INTO system_user_role (
    id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9162, @uid_opsfinance, @role_finance, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE @uid_opsfinance IS NOT NULL
  AND @role_finance IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = @uid_opsfinance
        AND ur.role_id = @role_finance
        AND ur.tenant_id = 1
        AND ur.deleted = b'0'
  )
  AND NOT EXISTS (SELECT 1 FROM system_user_role ur WHERE ur.id = 9162);

INSERT INTO system_user_role (
    id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9163, @uid_opseditor, @role_content_editor, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE @uid_opseditor IS NOT NULL
  AND @role_content_editor IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = @uid_opseditor
        AND ur.role_id = @role_content_editor
        AND ur.tenant_id = 1
        AND ur.deleted = b'0'
  )
  AND NOT EXISTS (SELECT 1 FROM system_user_role ur WHERE ur.id = 9163);

INSERT INTO system_user_role (
    id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9164, @uid_opsoperator, @role_ops_operator, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE @uid_opsoperator IS NOT NULL
  AND @role_ops_operator IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = @uid_opsoperator
        AND ur.role_id = @role_ops_operator
        AND ur.tenant_id = 1
        AND ur.deleted = b'0'
  )
  AND NOT EXISTS (SELECT 1 FROM system_user_role ur WHERE ur.id = 9164);

INSERT INTO system_user_role (
    id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    9165, @uid_opsanalyst, @role_data_analyst, 'adr-064-seed', NOW(), 'adr-064-seed', NOW(), b'0', 1
FROM DUAL
WHERE @uid_opsanalyst IS NOT NULL
  AND @role_data_analyst IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = @uid_opsanalyst
        AND ur.role_id = @role_data_analyst
        AND ur.tenant_id = 1
        AND ur.deleted = b'0'
  )
  AND NOT EXISTS (SELECT 1 FROM system_user_role ur WHERE ur.id = 9165);

-- Revive fixed-id binds if soft-deleted; re-point user_id/role_id
UPDATE system_user_role
SET user_id = COALESCE(@uid_opsleader, user_id),
    role_id = COALESCE(@role_ip_group_leader, role_id),
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9160 AND @uid_opsleader IS NOT NULL AND @role_ip_group_leader IS NOT NULL;

UPDATE system_user_role
SET user_id = COALESCE(@uid_opsmanager, user_id),
    role_id = COALESCE(@role_ops_manager, role_id),
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9161 AND @uid_opsmanager IS NOT NULL AND @role_ops_manager IS NOT NULL;

UPDATE system_user_role
SET user_id = COALESCE(@uid_opsfinance, user_id),
    role_id = COALESCE(@role_finance, role_id),
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9162 AND @uid_opsfinance IS NOT NULL AND @role_finance IS NOT NULL;

UPDATE system_user_role
SET user_id = COALESCE(@uid_opseditor, user_id),
    role_id = COALESCE(@role_content_editor, role_id),
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9163 AND @uid_opseditor IS NOT NULL AND @role_content_editor IS NOT NULL;

UPDATE system_user_role
SET user_id = COALESCE(@uid_opsoperator, user_id),
    role_id = COALESCE(@role_ops_operator, role_id),
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9164 AND @uid_opsoperator IS NOT NULL AND @role_ops_operator IS NOT NULL;

UPDATE system_user_role
SET user_id = COALESCE(@uid_opsanalyst, user_id),
    role_id = COALESCE(@role_data_analyst, role_id),
    deleted = b'0',
    updater = 'adr-064-seed',
    update_time = NOW()
WHERE id = 9165 AND @uid_opsanalyst IS NOT NULL AND @role_data_analyst IS NOT NULL;

COMMIT;

-- Verify
SELECT u.id, u.username, u.nickname, u.status, HEX(u.nickname) AS nickname_hex, r.id AS role_id, r.code AS role_code
FROM system_users u
JOIN system_user_role ur ON ur.user_id = u.id AND ur.deleted = b'0' AND ur.tenant_id = 1
JOIN system_role r ON r.id = ur.role_id AND r.deleted = b'0'
WHERE u.username IN ('opsleader', 'opsmanager', 'opsfinance', 'opseditor', 'opsoperator', 'opsanalyst')
  AND u.tenant_id = 1
  AND u.deleted = b'0'
ORDER BY r.id, u.id;
