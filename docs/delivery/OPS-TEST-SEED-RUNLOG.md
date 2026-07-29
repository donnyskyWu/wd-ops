# OPS Test Seed Run Log

| Item | Value |
|------|-------|
| Time | 2026-07-25 21:30:00 |
| Host | 110.42.49.224:3306 |
| shenyu-system | shenyu-system |
| shenyu-ops | shenyu-ops |

## Issue: menu charset (2026-07-25)

| 项 | 结论 |
|----|------|
| 现象 | Beta 侧栏 OPS 菜单显示 `????` 乱码 |
| 根因 | **seed 写入错误**：首次 `seed-ops-test-remote.ps1` 经 PowerShell 管道灌 SQL，中文未以 utf8mb4 落库，`HEX(name)=3F3F3F3F`（字面量 `?`） |
| 非根因 | Football 读库 / JDBC `characterEncoding=UTF-8` 正常；DB 内已是乱码 |
| 修复 | `apply-seed-oa-menu.py` 重灌 + `seed-ops-test-remote.ps1` 改为调用 Python；Flyway V164 条件 UPDATE |

## Scripts applied

- scripts/integration-config/apply-seed-oa-menu.py -> shenyu-system (seed-oa-system-menu.sql, utf8mb4 stdin)
- scripts/integration-config/seed-ops-test-remote-shenyu-system-menus.sql -> shenyu-system (prior run)
- scripts/integration-config/seed-ops-test-remote-dict.py -> shenyu-system (prior run)

## Cleanup (V163 on shenyu-ops, 2026-07-25)

- Applied: ops-platform-server/.../V163__drop_shenyu_ops_redundant_tables.sql
- backup_tables remaining: 0
- football_demo remaining: 0
- system_dict on master remaining: 0
- kept system_* overlay: system_menu, system_oauth2_access_token, system_role, system_role_menu, system_users, system_user_role

## Verification (after charset repair)

```
ops_menu_count	71
ops_role_menu_count	71
ops_dict_type_count	97
ops_dict_data_count	387
sample_menu	6100	运营数据
sample_menu	6117	内容管理
sample_menu	6159	IP组管理
corrupted_menu_rows	0
ip_group_leader_role	1
```

HEX 抽检：`6100` → `E8BF90E890A5E695B0E68DAE`（运营数据）

## Notes

- OPS menus live in shenyu-system.system_menu (6100-6999); @PreAuthorize reads @DS(system).
- Menu seed MUST use apply-seed-oa-menu.py (utf8mb4 stdin); PowerShell pipe corrupts Chinese.
- Flyway V164 repairs corrupted menu names in shenyu-system (name = literal '?').
- Business dicts merged from shenyu-ops.sys_dict_* to shenyu-system (V152/V158/V161 aligned).
- Flyway V161–V164 on shenyu-ops apply on next oa-server start.
- Start beta stack: .\scripts\start-ops-dev.ps1 -Beta
- **Re-login :5777** after menu repair to refresh sidebar cache.
