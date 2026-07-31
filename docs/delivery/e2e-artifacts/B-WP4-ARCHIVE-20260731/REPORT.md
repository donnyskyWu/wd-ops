# B-WP4-ARCHIVE 执行报告（2026-07-31）

| 项 | 值 |
|----|-----|
| **签收表** | [B-WP4-ARCHIVE-签收表-20260731.md](../../gates/B-WP4-ARCHIVE-签收表-20260731.md) |
| **环境（localhost）** | `localhost:3306/wd`（integration；root/root）✅ 已归档 |
| **环境（远程测试）** | `110.42.49.224:3306/shenyu-ops`（MySQL 5.7.44；`ops-test-remote.env`）— **备份 ✅；归档 SQL ⛔ 权限阻塞** |
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

归档 + 探测均成功（localhost）→ 可复评 **Phase C 整包 GO**（见 MASTER / WORK-PLAN / FEIGN-CHECKLIST 同步更新）。

**未做（localhost 范围外）**：§3.4 列 DROP；`FootballOAuth2MasterTokenMapper` 删除；`sys_user*` / dict 物理 RENAME/DROP；IT seed 删除。

---

## 5. 远程测试环境（110.42.49.224）· 2026-07-31

| 项 | 值 |
|----|-----|
| **Host / DB** | `110.42.49.224:3306` / **`shenyu-ops`**（无 `wd` 库；OPS master = 原 wd） |
| **凭据** | `scripts/integration-config/ops-test-remote.env` → user `shenyu-ops` |
| **Server** | MySQL **5.7.44-log** |
| **适配 SQL** | `sql/remote-110/`（`USE shenyu-ops`；collation `utf8mb4_general_ci`） |
| **应用探测** | 未跑 Gateway/ops 远程探测（当前 integration 默认仍指向 localhost）；以 SQL 校验为准 |

### 5.1 备份（已完成）

| 产物 | 路径 | 大小 |
|------|------|------|
| 候选表 dump | `backup-remote-110/shenyu-ops-q1-candidates-20260731.sql` | **205611 bytes (~200.8 KB)** |
| 归档前行数 | `backup-remote-110/pre-archive-rowcounts.txt` | — |

预归档行数：

| 表 | rows |
|----|------|
| sys_user | 6 |
| sys_user_token | 2 |
| sys_user_role | 8 |
| sys_role | 7 |
| sys_role_permission | 36 |
| sys_permission | 16 |
| sys_operation_log | 613 |
| sys_dict_type | 98 |
| sys_dict_data | 392 |
| system_users | 19 |

### 5.2 归档执行结果（按表）

| # | 对象 | 处置（与 localhost 同） | 远程结果 |
|---|------|-------------------------|----------|
| 1 | `shenyu-ops.sys_user` | 停写 trigger | ⛔ **未落地** — `ERROR 1419`（binlog 开启且无 SUPER / 未开 `log_bin_trust_function_creators`） |
| 2 | `sys_user_token` | 停写 trigger | ⛔ 同上 |
| 3 | legacy role 表 | 停写 trigger | ⛔ 同上 |
| 4 | `sys_operation_log` | RENAME → `archive_wd` | ⛔ **未执行** — `CREATE DATABASE archive_wd` → `ERROR 1044` Access denied；**未**做同库另名 RENAME（保持与 localhost `archive_wd` 对等，避免擅自改处置） |
| 5 | `sys_dict_*` | 停写 trigger | ⛔ 同 #1 |
| 6 | §3.4 桥接列 | 暂不纳入 | ✅ 未改 |
| 7 | `system_users` | 停写 trigger | ⛔ 同 #1 |

日志：`backup-remote-110/02-stop-write.log` · `03-create-archive-wd.log` · `03-rename-SKIPPED.txt`

### 5.3 SQL 校验（归档后现状）

| 检查 | 结果 |
|------|------|
| `SHOW DATABASES LIKE 'archive_wd'` | 不存在 |
| `shenyu-ops.sys_operation_log` | **仍存在**（613 行可读） |
| `information_schema.TRIGGERS` `trg_bwp4%` | **0 条** |
| 写阻断抽检 | 未生效（trigger 未建）；试 INSERT 因缺 `tenant_id` 被表约束拒绝，**非** B-WP4 stop-write |
| `SELECT` 候选表 | ✅ 仍可读 |

### 5.4 阻塞（需 DBA / 提升权限后重跑）

`shenyu-ops@%` 仅有 `USAGE` + `ALL PRIVILEGES ON shenyu-ops.*`。项目 env **无** root/elevated 凭据，故停止继续破坏性尝试。

解阻任一路径即可重跑 `sql/remote-110/`：

1. DBA：`SET GLOBAL log_bin_trust_function_creators = 1;`（或 SUPER）→ 再执行 `02-stop-write-readonly.sql`
2. DBA：`CREATE DATABASE archive_wd …;` + `GRANT ALL ON archive_wd.* TO 'shenyu-ops'@'%';`（或 elevated 用户直接跑 `03-rename-sys-operation-log.sql`）
3. 或提供 elevated MySQL 账号写入本地 `ops-test-remote.env`（勿入库）后由 OPS 重跑

**状态**：远程 = **备份完成 + 归档挂起（权限）**；localhost Phase C GO 证据不受影响。
