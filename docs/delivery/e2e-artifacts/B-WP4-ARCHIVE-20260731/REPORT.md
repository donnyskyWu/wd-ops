# B-WP4-ARCHIVE 执行报告（2026-07-31）

| 项 | 值 |
|----|-----|
| **签收表** | [B-WP4-ARCHIVE-签收表-20260731.md](../../gates/B-WP4-ARCHIVE-签收表-20260731.md) |
| **环境（localhost）** | `localhost:3306/wd`（integration；root/root）✅ 已归档 |
| **环境（远程测试）** | `110.42.49.224:3306/shenyu-ops`（MySQL 5.7.44；`ops-test-remote.env`）— **备份 ✅；同库 `archive_*` RENAME ✅（partial：`system_users` 保留）** |
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

### 5.2 归档执行结果（按表）· 同库 `archive_*`（用户澄清 2026-07-31）

> 澄清：不建 `archive_wd`；trigger 因无 SUPER 失败 → 同库 `RENAME TABLE … TO archive_<name>`；可删数据/表。  
> 备份复验：`backup-remote-110/shenyu-ops-q1-candidates-20260731.sql` = **205611 bytes**（非空）后执行。

| # | 对象 | 处置（远程适配） | 远程结果 |
|---|------|------------------|----------|
| 1 | `sys_user` / `sys_user_token` / `sys_user_role` | 同库 RENAME `archive_*` | ✅ `archive_sys_user`(6) / `_token`(2) / `_role`(8) |
| 3 | `sys_role` / `sys_role_permission` / `sys_permission` | 同库 RENAME | ✅ `archive_sys_role`(7) / `_permission`(36) / `archive_sys_permission`(16) |
| 4 | `sys_operation_log` | 同库 RENAME | ✅ → `archive_sys_operation_log`（**613** 行；原名 GONE） |
| 5 | `sys_dict_type` / `sys_dict_data` | 同库 RENAME | ✅ `archive_sys_dict_type`(98) / `_data`(392) |
| 6 | §3.4 桥接列 | 暂不纳入 | ✅ 未改 |
| 7 | `system_users` overlay | **SKIP RENAME** | ⚠ 保留（19 行）— `FootballOAuth2MasterTokenMapper` @DS master 仍读；SSOT=`shenyu-system.system_users`；**未删** Mapper 代码（Q4） |

DROP：无（全部 RENAME 成功，未走 DROP fallback）。  
日志：`backup-remote-110/04-rename-same-schema-RESULTS.txt`  
SQL：`sql/remote-110/03-rename-sys-operation-log.sql` · `04-rename-q1-legacy-same-schema.sql` · `05-rollback-same-schema.sql`（`02-stop-write-readonly.sql` remote **DEPRECATED**）

### 5.3 SQL 校验（归档后）

| 检查 | 结果 |
|------|------|
| `sys_operation_log` | **GONE** |
| `archive_sys_operation_log` | **EXISTS** · `COUNT(*)=613` |
| live legacy (`sys_user*` / role / dict / operation_log) | **NULL**（均已改名） |
| `archive_sys_%` | 9 张：user×3 · role/perm×3 · dict×2 · operation_log |
| `system_users` | **仍存在**（19；主动保留） |
| `archive_wd` DB | **未创建**（按澄清） |
| `trg_bwp4%` | 0（未用 trigger） |

### 5.4 状态

**远程 = 备份 ✅ + 同库归档 ✅（partial：`system_users` 与 `system_*` overlay 集群因应用依赖保留）**。  
localhost Phase C GO 证据不受影响。`FootballOAuth2MasterTokenMapper` 未删。
