# DB Sync → Beta Test · 2026-08-01

> 将今日 DB 相关改动同步到测试环境（`110.42.49.224`）。凭据来自本地 `scripts/integration-config/ops-test-remote.env`（未写入本报告）。

## 1. 今日 DB 影响面（识别）

| 变更 | 类型 | 是否需同步 Beta |
|------|------|----------------|
| `V166__rename_permission_oa_to_ops.sql` | Flyway · `shenyu-ops.system_menu` `oa:*`→`ops:*` | ✅ 已同步 |
| `V167__tenant_unified_collect_task.sql` | Flyway · `collect_enabled` / `is_unified` / `oa_collect_task_account` / `sys_param` | ✅ 已同步 |
| `rename-permission-oa-to-ops-shenyu-system.sql` | 手工 · `shenyu-system.system_menu` | ✅ 已同步 |
| ADR-062 实名人 1:N | **无 DDL**（仅 BE/FE 独占校验） | ⏭ 跳过 |
| MP realname / create RPC | **无 DDL**（`oa_account_ext` 已有） | ⏭ `shenyu-mp` 未动 |
| 本地库名 `football-ops`→`shenyu-ops` | 仅本机 | ⏭ Beta 本已是 `shenyu-ops` |

同步前远程 `flyway_schema_history` 最高版本：**165**（缺 166/167）。

## 2. 目标库

| 库 | Host | Port | User | 操作 |
|----|------|------|------|------|
| `shenyu-ops` | `110.42.49.224` | 3306 | `shenyu-ops` | V166 + V167 + history |
| `shenyu-system` | `110.42.49.224` | 3306 | `shenyu-system` | permission rename |
| `shenyu-mp` / member / pay / bpm | — | — | — | **未改** |

## 3. 已应用清单

| # | 脚本 / 迁移 | 结果 |
|---|-------------|------|
| 1 | Flyway **V166** `rename_permission_oa_to_ops` | applied · history rank=167 · checksum=`1528112299`（与本地一致） |
| 2 | Flyway **V167** `tenant_unified_collect_task` | applied · history rank=168 · checksum=`-1067562857` |
| 3 | `rename-permission-oa-to-ops-shenyu-system.sql` | applied · `oa:` 60→0，`ops:` 0→60 |

执行工具：`docs/delivery/e2e-artifacts/DB-SYNC-TEST-20260801/_apply_missing.py`（读 env，幂等守卫）。

## 4. 验证结果

| 检查项 | 结果 |
|--------|------|
| Flyway max version | **167** |
| V166 / V167 `success=1` | ✅ |
| failed flyway rows | **0** |
| `oa_account.collect_enabled` | ✅ |
| `oa_account_ext.collect_enabled` | ✅ |
| `oa_collect_task.is_unified` + UK | ✅ |
| 表 `oa_collect_task_account` | ✅ |
| `sys_param` `collect.schedule.cron` = `0 0 23 * * ?`（tenant=1） | ✅ |
| master `system_menu` leftover `oa:` | **0** |
| system `system_menu` `oa:` / `ops:` | **0 / 60** |

## 5. 残留 / 风险

1. **本地 V167 history checksum 曾为 NULL**；Beta 已写入正确 CRC32。下次本地 Flyway validate 若报 V167 mismatch，可用同算法 repair（不必回写 Beta）。
2. **master `system_menu` overlay** 与 **shenyu-system SSOT** 均已 rename；若 Nacos/缓存仍持旧 permission，需重新登录或清 Redis 权限缓存后再验 UI。
3. **未跑** `seed-ops-test-remote.ps1` 全量菜单 seed（今日无强制菜单 DDL）；若侧栏缺项另开 seed。
4. **mp / member / pay** 无今日 DDL，未触碰。
5. 未 `git commit`（按任务约束）。

## 6. 复跑（幂等）

```powershell
python docs/delivery/e2e-artifacts/DB-SYNC-TEST-20260801/_apply_missing.py
python docs/delivery/e2e-artifacts/DB-SYNC-TEST-20260801/_probe_remote.py
```
