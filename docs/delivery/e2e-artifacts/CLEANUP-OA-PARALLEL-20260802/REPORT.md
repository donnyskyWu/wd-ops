# CLEANUP P0-3 — 平行菜单 / `oa:*` 权限残留清理

| 字段 | 值 |
|------|---|
| 日期 | 2026-08-02 |
| 工作包 | A-WP2 / CLEANUP Inventory §5 · P0-3 |
| SQL | [`scripts/integration-config/cleanup-oa-parallel-menu-perm.sql`](../../../scripts/integration-config/cleanup-oa-parallel-menu-perm.sql) |
| 状态 | **✅ PASS**（幂等；环境此前已干净） |

## 范围

| 做 | 不做 |
|----|------|
| `shenyu-system.system_menu` / `system_role_menu`：6137 / 6138 / 6139 / 6155 | DROP `archive_*` |
| 平行权限码菜单行：`oa|ops`:`user\|dept\|dict\|log\|role\|tenant\|permission\|author` | 动 `oa_*` 业务表 |
| 抽检 `ops:*` 业务菜单与 `super_admin` OPS 绑定 | 删 M8 `sys_metadata_*` / 字典 SSOT |
| | 绕过 B-WP4 改写 `sys_permission`（stop-write / 已 archive） |

## Before → After

### `shenyu-system`（鉴权 SSOT）

| 环境 | Host | menu 6137–39/6155 | role_menu 目标 | 平行 perm 菜单 | OPS 菜单 active | super_admin OPS 绑定 |
|------|------|-------------------:|---------------:|---------------:|----------------:|---------------------:|
| Local | 127.0.0.1 | 0 → 0 | 0 → 0 | 0 → 0 | 71 → 71 | 71 → 71 |
| Beta | 110.42.49.224 | 0 → 0 | 0 → 0 | 0 → 0 | 77 → 77 | 71 → 71 |

- `6105` 子菜单仅 **6140 消息**、**6141 参数**（符合 CLEANUP §5）。
- 二次执行本地脚本：BEFORE=AFTER，幂等。

### `shenyu-ops`（legacy permission；未 mutate）

| 环境 | 状态 | 平行 `oa:user\|role\|tenant\|permission:*` |
|------|------|---------------------------------------------|
| Local | 直播表 `sys_permission` + B-WP4 **stop-write triggers** | 7 行 / 8 条 `sys_role_permission`（只读残留） |
| Beta | 已 RENAME → `archive_sys_permission`（B-WP4） | archive 内 7 行；**未触碰** |

生产鉴权路径 = `system_menu`（+ super_admin expand）；见 P-D / V166。legacy 表按 B-WP4 只读/归档，本档不 DROP、不绕过 trigger 硬删。

## 已清理环境

1. **Local** `127.0.0.1` / `shenyu-system` — 脚本已 apply（no-op）
2. **Beta** `110.42.49.224` / `shenyu-system` — 脚本已 apply（no-op）

## 产物

| 文件 | 说明 |
|------|------|
| `00-before-*.txt` | 审计快照 |
| `01-apply-*.txt` | BEFORE/AFTER counts |
| `02-verify-*.txt` | 菜单/角色/六角色抽检 |
| `02-ops-*-status.txt` | ops 侧 permission / archive 状态 |
| `RESULTS.json` | 机器可读汇总 |

## 文档勾选

- WORK-PLAN A-WP2「环境抽检…P0-3」→ ✅
- CLEANUP Inventory §5 / P0-3 → 环境结案 ✅
