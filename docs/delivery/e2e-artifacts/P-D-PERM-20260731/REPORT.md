# P-D 权限码 `oa:*` → `ops:*` — 报告

| 字段 | 值 |
|------|---|
| 日期 | 2026-07-31 |
| Slice | P-D / ADR-058 P6 |
| 状态 | **✅ 完成** |

## 范围落地

| 层 | 动作 |
|----|------|
| Java `@PreAuthorize` | 生产 Controllers 已为 `ops:*`（`hasAuthority('oa:` = 0） |
| Flyway | `V166__rename_permission_oa_to_ops.sql`：`system_menu.permission` `oa:`→`ops:`；**不**改 `sys_permission`（B-WP4 stop-write trigger） |
| football-ops DB | `system_menu`：oa=0 / ops=60；Flyway `166` success=1 |
| shenyu-system DB | `system_menu`：oa=0 / ops=60（本地已对齐） |
| Seeds / scripts | `seed-oa-system-menu.sql` 等权限字面量改为 `ops:`；新增 `rename-permission-oa-to-ops-shenyu-system.sql`（Beta 幂等） |
| FE | ops views 无硬编码 `oa:` / `ops:` 权限串（按钮权限来自菜单） |
| 路径 | **未**改动；仍 `/admin-api/ops/**`（P-C 不回归） |

## 冒烟（RESULTS.json）

| Check | 结果 |
|-------|------|
| GW `/admin-api/ops/account/list` | code=0 |
| GW `/admin-api/ops/content/list` | code=0 |
| 直连 `:48094` account/content list | code=0 |
| GW `/admin-api/ops/ip-group/tree` | code=0 |
| **合计** | **5/5** |

Admin 登录：`admin` / `admin123`，tenant=1。ops-server 重建后 Flyway 应用 V166，health UP。

## 备注

- 历史 Flyway（V12/V15/…）仍含 `oa:` 插入字面量；**靠 V166 在 migrate 末尾改写**，不改写历史迁移。
- `sys_permission` 遗留 `oa:*` 为 B-WP4 只读归档；鉴权 SSOT = `system_menu` + super_admin expand。
- Beta：对 `shenyu-system` 跑 `scripts/integration-config/rename-permission-oa-to-ops-shenyu-system.sql`；ops master 靠部署后 Flyway V166。
