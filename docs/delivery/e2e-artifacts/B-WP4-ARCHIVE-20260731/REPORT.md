# B-WP4-ARCHIVE 执行报告（2026-07-31）

| 项 | 值 |
|----|-----|
| **签收表** | [B-WP4-ARCHIVE-签收表-20260731.md](../../gates/B-WP4-ARCHIVE-签收表-20260731.md) |
| **环境** | `localhost:3306/wd`（integration；root/root）— **未**触碰远程/生产 |
| **顺序** | 备份 → 归档 SQL → 只读探测 |
| **Q3 回滚窗口** | **0 天**（用户决定：备份后执行，不要回滚窗口） |
| **Q4** | **未删** `FootballOAuth2MasterTokenMapper`（ADR-056 全量切轨后） |

---

## 1. 备份

| 产物 | 路径 | 大小 |
|------|------|------|
| 候选表 dump | `backup/wd-q1-candidates-20260731.sql` | ~209 KB |
| 归档前行数 | `backup/pre-archive-rowcounts.txt` | — |

备份表：`sys_user` / `sys_user_token` / `sys_user_role` / `sys_role` / `sys_role_permission` / `sys_permission` / `sys_operation_log` / `sys_dict_type` / `sys_dict_data` / `system_users`。

---

## 2. 按 Q1 建议执行结果（采纳建议）

| # | 对象 | 处置 | 已执行 |
|---|------|------|--------|
| 1 | `wd.sys_user` | 停写只读 | ✅ `trg_bwp4_sys_user_{bi,bu,bd}` |
| 2 | `wd.sys_user_token` | 停写只读 | ✅ triggers；**未删** IT seed（Q2 Yes → 物理删前须先改写 IT） |
| 3 | legacy `sys_role` / `sys_user_role` / `sys_role_permission` / `sys_permission` | 停写只读 | ✅ triggers |
| 4 | `wd.sys_operation_log` | RENAME archive | ✅ → `archive_wd.sys_operation_log`（616 行；`wd` 侧表已不存在） |
| 5 | `wd.sys_dict_type` / `sys_dict_data` | 停写只读 | ✅ triggers |
| 6 | §3.4 桥接列/桥 | 暂不纳入 | ✅ 未改列、未 DROP |
| 7 | `wd.system_users` overlay | 停写只读 | ✅ triggers；Mapper 保留（Q4） |

SQL 脚本：

- `sql/02-stop-write-readonly.sql`
- `sql/03-rename-sys-operation-log.sql`
- `sql/04-rollback.sql`（可选；无强制 hold 窗口）

写阻断抽检：`INSERT INTO wd.sys_user ...` → `ERROR 1644 … stop-write (read-only)`；`SELECT` 仍可读。

---

## 3. 只读探测（归档后）

| 检查 | 结果 |
|------|------|
| Gateway login `admin` / `admin123` tenant=1 | ✅ `code=0`（`00-login.json`） |
| `GET /admin-api/ops/ip-group/list` | ✅ `code=0` total=20（`probe-ip-group-list.json`） |
| `GET /admin-api/ops/content/list` | ✅ `code=0` total=38（`probe-content-list.json`） |
| `GET /admin-api/ops/account/list` | ✅ `code=0` total=183（`probe-account-list.json`） |
| ops-server `:48094/actuator/health` | ✅ `UP` |
| `FootballOAuth2MasterTokenMapper.java` 仍存在 | ✅ |

汇总：`RESULTS.json`

> 注：本地 Gateway 已按 ADR-058 仅公开 `/admin-api/ops/**`（Rewrite → `/admin-api/oa/**`）；探测须带 `tenant-id` + `X-Tenant-Id`。

---

## 4. Phase C 整包

归档 + 探测均成功 → 可复评 **Phase C 整包 GO**（见 MASTER / WORK-PLAN / FEIGN-CHECKLIST 同步更新）。

**未做**：远程/生产归档；§3.4 列 DROP；`FootballOAuth2MasterTokenMapper` 删除；`sys_user*` / dict 物理 RENAME/DROP；IT seed 删除。
