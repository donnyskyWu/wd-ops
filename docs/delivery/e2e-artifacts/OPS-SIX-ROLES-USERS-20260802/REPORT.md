# OPS Six Roles — Test Users (2026-08-02)

## Summary

ADR-064 六业务角色可测用户已写入 Beta `shenyu-system`；登录冒烟（system-server `:48081`）六账号均 `code=0` + accessToken。

## SQL

| Item | Path |
|------|------|
| Role seed（已有） | `scripts/integration-config/seed-ops-six-roles-rbac.sql` |
| User seed（新建） | `scripts/integration-config/seed-ops-six-roles-test-users.sql` |
| Apply helper | `scripts/integration-config/apply-seed-oa-menu.py`（utf8mb4 stdin） |
| Docs | `docs/delivery/OPS-TEST-DB.md` §ADR-064 六角色测试账号 |

## Account table（tenant_id=1）

| username | password | role code | role name | user_id | role_id | note |
|----------|----------|-----------|-----------|---------|---------|------|
| opsleader | admin123 | ip_group_leader | IP组长 | 9160 | 160 | Beta 原先无 `ip_group_leader` 绑定 → 按例外新建 |
| opsmanager | admin123 | ops_manager | 运营主管 | 9161 | 161 | seed |
| opsfinance | admin123 | finance | 财务人员 | 9162 | 162 | seed |
| opseditor | admin123 | content_editor | 内容编辑 | 9163 | 163 | seed |
| opsoperator | admin123 | ops_operator | 运营 | 9164 | 164 | seed |
| opsanalyst | admin123 | data_analyst | 数据分析 | 9165 | 165 | seed |
| admin | admin123 | super_admin | 系统管理员 | 1749825673829120001 | 1 | 已有，未改 |

### IP组长说明

- 查询 `system_user_role` × `system_role.code=ip_group_leader`：**0 行**（seed 前）
- 业务表 `oa_ip_group.leader_user_id` 常见存量：`zhangwu`(1749825673829120202)、`admin`、`test`/`goudan` 等，但**未绑** `ip_group_leader` 角色，且密码非统一测试口令
- 故新建 `opsleader` / `admin123`，专供六角色联调

### Username 约束

Football `AuthLoginReqVO`：`^[A-Za-z0-9]+$` — **不可**使用 `ops_manager` 等形式（下划线会 400）。

## Beta verification

Host `110.42.49.224` / DB `shenyu-system`：

- 6 users id 9160–9165，`status=0`，nickname HEX 中文 OK
- 6 `system_user_role` id 9160–9165 → roles 160–165
- Login `POST /admin-api/system/auth/login`（tenant-id:1）：六账号均成功（见 `RESULTS.json`）
- Seed 重跑幂等：用户/绑定仍为 6/6

## Apply

```powershell
# load ops-test-remote.env first
python scripts/integration-config/apply-seed-oa-menu.py `
  --host $env:OPS_TEST_DB_HOST --user shenyu-system --password <env> --database shenyu-system `
  --seed scripts/integration-config/seed-ops-six-roles-test-users.sql
```

Local（可选，需先有 roles 160–165）：

```powershell
python scripts/integration-config/apply-seed-oa-menu.py `
  --host localhost --user root --password root --database shenyu-system `
  --seed scripts/integration-config/seed-ops-six-roles-test-users.sql
```
