# Ops Greenfield 部署 — 操作手册

**版本:** 2026-08-25 · **SSOT:** `docs/deploy/ops-greenfield-production/`  
**角色:** Part A = DBA · Part B = DevOps · 增量升级见文末

---

## 0. 前提

| 项 | 要求 |
|----|------|
| MySQL | 可建 `shenyu-ops`；可写 `shenyu-system` |
| 依赖 | Nacos prod、Redis、member/match/system-server 已注册 |
| 制品 | `football-module-ops-server.jar` + 含 Ops 路由的 Admin UI |
| 占位符 | `{{OPS_DB_*}}` `{{SYSTEM_DB_*}}` `{{WORK_TASK_DEFAULT_TEMPLATE_ID}}` `{{WORK_TASK_DEFAULT_NODE_ID}}` |

---

# Part A — DBA（4 步）

## Step 1 — 建库

```sql
CREATE DATABASE IF NOT EXISTS `{{OPS_DB_NAME}}`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '{{OPS_DB_USER}}'@'%' IDENTIFIED BY '{{OPS_DB_PASSWORD}}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
  ON `{{OPS_DB_NAME}}`.* TO '{{OPS_DB_USER}}'@'%';
FLUSH PRIVILEGES;
```

## Step 2 — Ops schema（Flyway V1→V191，186 SQL + history）

> **空库要求：** `01` 必须在**空** `shenyu-ops` 上执行（仅 `CREATE DATABASE` 后无表）。若曾部分跑过旧版 `01` 残留表/种子，可能报 `1062 Duplicate entry` → **DROP DATABASE + CREATE** 后重跑。

```bash
mysql -h HOST -u USER -p shenyu-ops \
  < docs/deploy/ops-greenfield-production/sql/01-shenyu-ops-schema.sql
```

**验收:**（**前置：** 仅 Step 2 `01-shenyu-ops-schema.sql` 即可；**不必**等 Step 4 `03-shenyu-ops-seeds.sql`）

```bash
mysql -h HOST -u USER -p shenyu-ops \
  < docs/deploy/ops-greenfield-production/sql/verify-schema.sql
```

Windows PowerShell（`<` 重定向不可用）:

```powershell
$env:MYSQL_PWD='***'
Get-Content docs/deploy/ops-greenfield-production/sql/verify-schema.sql -Raw |
  mysql -h 127.0.0.1 -u root -D shenyu-ops
```

> **注意：** `shenyu-ops` **没有** Football `system_*` 表（V163/V172 已 DROP overlay 副本；SSOT = `shenyu-system`）。
> `01-shenyu-ops-schema.sql` 已对 V137/V148/V150/V153/V183 等跨库 `system_*` / `wd.*` 迁移做 **SKIP**，仅写 ops 业务表。
> **Legacy sys_* harness（V190/V191 目标表）：** 生成器 `gen-ops-greenfield-sql.py` 将 V1–V189 中针对 `sys_tenant`/`sys_user*`/`sys_role*`/`sys_dict_*`/`sys_operation_log` 的 CREATE/seed/ALTER/INDEX/**SET 变量子查询**（如 `SET @next_type_id = (SELECT … FROM sys_dict_type)`）**整段省略**（不保留 `--` 注释 SQL，避免误执行）；每段迁移仅留一行 `[greenfield skip]` 摘要。V190/V191 整段 **no-op**。终态不会创建这些表。
> 若报 `1146 Table '...flyway_schema_history' doesn't exist`（约 line 104）→ 未跑 `01` 或库为空；先执行 Step 2 `01`，或使用 2026-08-25 版 `verify-schema.sql`（Flyway 段已用 `information_schema` + `PREPARE`，空库也会完整输出 MISSING 而非中断）。
> 若报 `1146 Table 'shenyu-ops.system_menu' doesn't exist`（或 `system_dict_*` / `system_role`）→ 检查是否误在 ops 库跑菜单/字典 SQL，或使用了旧版 `01`；请跑 Step 3 的 `02-shenyu-system-menus.sql`。
> 若报 `1146 … sys_dict_type` / `sys_dict_data` → 旧版 `01` 未省略 `SET @next_* = (SELECT … FROM sys_dict_*)` 等 dict seed 残留；**重新生成并执行** `gen-ops-greenfield-sql.py` 输出的 `01`（2026-08-25 起已修复）。
> 若报 `1064` 且错误片段含 `dict INSERTs removed` / 行首 `...`，多为生成器在 `/* */` 块注释内误按 `;` 拆语句（V181 §2）；2026-08-25 起 `split_sql_statements` 已块注释感知并含 `validate_greenfield_sql_landmines` 自检。本地验收：空库执行 `01` 后 `SOURCE sql/verify-schema.sql` 全 OK。
> **本地跑过旧版 `01` 后 ops 起不来（FlywayValidateException）：**
> - **checksum mismatch**（如 V181 `-912526136` vs JAR `1729697317`）：`python scripts/integration-config/gen-ops-flyway-history.py` 后对照 `ops-flyway-record-history.sql` 更新 checksum，或 Flyway `repair`。
> - **description mismatch**（如 `seed_base` vs `seed base`）：旧版 history 用文件名 underscore；2026-08-25 起 `gen-ops-flyway-history.py` 已改为空格。已有库执行 `scripts/integration-config/repair-flyway-local-validate.sql`。
> - 然后重启 `football-module-ops`（`.\scripts\start-integration-oa.ps1`）。

### shenyu-ops 表清单（Greenfield 终态）

| 类别 | 表 | 说明 |
|------|-----|------|
| **保留 · 业务** | `oa_*`（content、ip_group、work_task、report、collect…） | Ops 域 SSOT；含 `oa_xiaohongshu_note`（M10 采集落库） |
| **保留 · 配置** | `sys_param` · `sys_message` | 系统参数 / 站内消息（用户指定保留） |
| **保留 · M6 元数据** | `sys_metadata_entity` · `sys_metadata_field` | `MetadataServiceImpl` 报表/自定义查询引擎 |
| **保留 · 通知去重** | `sys_notification_event` | `NotificationServiceImpl` 幂等 ledger |
| **V191 DROP · Feign SSOT** | `sys_tenant` · `sys_user*` · `sys_role*` · `sys_permission` | 租户/用户/角色 → shenyu-system Feign（ADR-056） |
| **V190 DROP · Feign SSOT** | `sys_dict_type` · `sys_dict_data` | 运行时 `DictService` → Feign `shenyu-system.system_dict_*` |
| **V190 DROP · 死代码** | `sys_operation_log` | 无 Java Mapper/Service |
| **DROP · Football overlay** | `system_menu` · `system_role` · `system_users` · … | V163/V172 |
| **DROP · legacy** | `oa_author` · `archive_sys_*` · `sys_audit_log` · `sys_dept` · `sys_login_log` | V172 |

**迁移路径（Greenfield）：** `01` 是 Flyway V1→V191 **溯源拼接**，但 Greenfield 生成器会 **omit** V190/V191 目标 legacy `sys_*` 的全部 DDL/DML（不嵌入注释 SQL）；V190/V191 段本身为 no-op。生产鉴权/RBAC/字典/租户读 **SSOT = Step 3 `shenyu-system`**（ADR-056 / G-DICT-01）。已有库增量清理：`scripts/integration-config/drop-ops-legacy-sys-tables.sql`（§1 = V190；§2 = V191 身份表）。重新生成：`python scripts/integration-config/gen-ops-greenfield-sql.py`。

期望：全部 `check_status = OK`；`flyway_v190 = OK`；`flyway_v191 = OK`。补充：

```sql
SELECT COUNT(*) FROM flyway_schema_history WHERE type='SQL' AND success=1;
-- 期望 186（JAR 首次启动补 V113 Java 后为 187）
```

### V113 Java migration

V113 为 Java migration（INTERNAL collect → M4 账号迁移），**无 `.sql` 文件**，DBA **不手工跑**。`01-shenyu-ops-schema.sql` 的 history **故意不含 V113**；DevOps 首次启动 JAR（`FLYWAY_ENABLED=true`）时 Flyway 自动补跑。Greenfield 空库通常为 **no-op**（零行更新）。

## Step 3 — System 菜单 / 字典 / RBAC

> **Schema SSOT（测试库）：** `110.42.49.224` / `shenyu-system`（见 [OPS-TEST-DB.md](../../delivery/OPS-TEST-DB.md)）。2026-08-25 `DESCRIBE` 要点：
> - `system_menu` → **`user_type`**（无 `tenant_id` 列）
> - `system_dict_data` → **`value`**（非 `dict_value`）
> - `system_role_menu` → **`tenant_id` + `user_type`**
> - `system_role` → **`tenant_id`**
>
> 本地 dev overlay 连 **`shenyu-sys`**（与 Beta 测试 schema 一致）；本地 **`shenyu-system`**（`tenant_id` overlay）勿跑 `02`。本地验证对 **`shenyu-sys`** 执行 `02`；仅缺工作任务时可跑 `scripts/integration-config/fix_local_work_task_menu.sql`（已与测试 schema 对齐）。

**执行前检查（可选）：**

```sql
-- Connect: mysql -h HOST -u USER -p shenyu-system
SELECT COUNT(*) AS has_user_type FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'system_menu' AND column_name = 'user_type';
-- 期望 1；为 0 则停止，换正确 system 库或先对齐 Football schema
```

```bash
mysql -h HOST -u USER -p shenyu-system \
  < docs/deploy/ops-greenfield-production/sql/02-shenyu-system-menus.sql
```

**基线字典（Greenfield 无 wd 库）:** 合并脚本已 **跳过** V148 `wd→system_dict_*` 与 V152 `wd→shenyu-system` 迁移。确认 Football 已有 `dict_*` 字典：

```sql
-- Connect: mysql -h HOST -u USER -p shenyu-system
SELECT type, name FROM system_dict_type
WHERE type LIKE 'dict\_%' AND deleted = b'0' ORDER BY type LIMIT 20;
```

若为空 → 从已验证 staging/test 的 `shenyu-system` 导出 `dict_*` 行，或使用 `scripts/integration-config/seed-ops-test-remote-dict.py`（需源库凭证）。

**验收:**

```sql
SELECT id, name FROM system_menu WHERE id = 6100 AND deleted = b'0';
SELECT permission FROM system_menu WHERE permission LIKE 'ops:work-task:%' AND deleted = b'0';
SELECT code FROM system_role WHERE code IN ('ip_group_leader','ops_manager','finance',
  'content_editor','data_analyst','collect_operator') AND deleted = b'0';
```

## Step 4 — Ops 种子

**先查 SOP ID**（完整 verify 见 `scripts/integration-config/ops-greenfield-sources/seeds/01_prerequisite_sop_verify.sql`；或执行）:

```sql
-- Connect: mysql -h HOST -u USER -p shenyu-ops
SELECT id, name FROM oa_sop_template WHERE deleted=0 AND tenant_id=1;
SELECT n.id, n.template_id FROM oa_sop_node n
WHERE n.node_type='CONTENT_GENERATION' AND n.deleted=0 AND n.tenant_id=1;
```

编辑 `sql/03-shenyu-ops-seeds.sql` 中 `{{WORK_TASK_DEFAULT_TEMPLATE_ID}}` / `{{WORK_TASK_DEFAULT_NODE_ID}}`（本地测试可参考 [config/env-variables.md](./config/env-variables.md) 示例 `9402` / `9404`），然后：

```bash
mysql -h HOST -u USER -p shenyu-ops \
  < docs/deploy/ops-greenfield-production/sql/03-shenyu-ops-seeds.sql
```

**可选业务数据:** IP 组 + anchor 绑定（私域报表 / 工作任务 P0）。通过 Admin UI「IP组管理」录入，或参考 `scripts/integration-config/s0-wd-ip-group-skeleton.sql` 改 tenant/author ID。

DBA 完成 Step 1–4 后通知 DevOps。

---

# Part B — DevOps

## B.1 配置

| 文件 | 用途 |
|------|------|
| [config/nacos-ops-server-prod.yaml](./config/nacos-ops-server-prod.yaml) | Spring Boot 模板 |
| [config/env-variables.md](./config/env-variables.md) | 环境变量 |
| [config/gateway-ops-routes.md](./config/gateway-ops-routes.md) | Gateway 路由 |
| [config/xxl-job-register.md](./config/xxl-job-register.md) | XXL-JOB |

必设：`OPS_DB_*` `REDIS_*` `NACOS_*` `OA_AES_KEY`（与 Football 一致）`FOOTBALL_AI_*` `OA_MATCH_INTERNAL_BASE_URL` `XXL_JOB_*` · `oa.auth.dev-token.enabled=false`

## B.2 ops-server

```bash
java -Xms512m -Xmx2048m -jar football-module-ops-server.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/opt/ops/config/nacos-ops-server-prod.yaml
```

首次启动：Flyway 补 V113 Java → 注册 Nacos `ops-server@48094` → XXL executor 在线。  
Health: `GET http://{{OPS_HOST}}:48094/actuator/health` → UP

## B.3 Gateway

```
/admin-api/ops/** → grayLb://ops-server
```

## B.4 Frontend

部署含 Ops 路由的 Football Admin UI（与 ops-server **同 tag**）。P0 路由：`ops/production/work-task/index`、`ops/analysis/report/*`。菜单索引：`docs/delivery/OPS-MENU-ROUTE-INDEX.md`

## B.5 XXL-JOB

1. Executor `football-ops-executor` 在线  
2. Job `workTaskWinPredictionJobHandler`，Cron `0 0 * * * ?`  
3. 手动触发一次 SUCCESS

## B.6 冒烟（P0）

```bash
curl -H "Authorization: Bearer {{TOKEN}}" \
  "https://{{GATEWAY_HOST}}/admin-api/ops/ip-group/list"
curl -H "Authorization: Bearer {{TOKEN}}" \
  "https://{{GATEWAY_HOST}}/admin-api/ops/work-task/sheet/get-or-create?ipGroupId={{IP_GROUP_ID}}&workDate=2026-08-25"
curl -H "Authorization: Bearer {{TOKEN}}" \
  "https://{{GATEWAY_HOST}}/admin-api/ops/private-domain-report/weekly-funnel?weekStart=2026-08-11&weekEnd=2026-08-17"
```

> `weekly-funnel` 必填 `weekStart` + `weekEnd`（ISO 日期）；`weekLabel` 仅用于反馈读写。`work-task` 须传真实 `ipGroupId`（勿硬编码 `1`）。

UI：超管 → 「运营数据」→「工作任务管理」；「数据分析」→「数据报表」月达成/周转化。

---

# Part C — 文件清单

```
docs/deploy/ops-greenfield-production/
├── README.md
├── OPERATIONS-GUIDE.md      ← 本文件
├── rollback.md
├── sql/
│   ├── 01-shenyu-ops-schema.sql    # Flyway V1–V191 + history
│   ├── 02-shenyu-system-menus.sql  # 菜单 / 字典 / RBAC
│   ├── 03-shenyu-ops-seeds.sql     # AI prompt + sys_param
│   └── verify-schema.sql
└── config/
    ├── nacos-ops-server-prod.yaml
    ├── env-variables.md
    ├── gateway-ops-routes.md
    └── xxl-job-register.md
```

| 功能 | Flyway | System SQL |
|------|--------|------------|
| Ops 全量表 + SOP seed | V1–V180 | — |
| 工作任务 | V181–V182 | 02（含 03/05/06/07 段） |
| 私域报表 MVP | V184 | — |
| Match pool（已废弃） | V185–V187 → V189 DROP | 勿单独部署 |
| LIVE_DRAIN 字典 | V188（ops no-op） | 02 内 06 段 |

## 幂等性

| 脚本 | 重复执行 |
|------|----------|
| `01-shenyu-ops-schema.sql` | `CREATE IF NOT EXISTS` / 条件 INSERT history — 空库执行一次；已有 v191 勿重跑 |
| `02-shenyu-system-menus.sql` | `INSERT ... WHERE NOT EXISTS` / 条件 UPDATE — 可安全重跑 |
| `03-shenyu-ops-seeds.sql` | `WHERE NOT EXISTS` — 可安全重跑 |
| JAR Flyway（DBA 已灌库） | 仅补 V113 Java，其余跳过 |

## 附录 A — 本地空库验证（2026-08-25）

| 脚本 | 库 | 结果 | 备注 |
|------|-----|------|------|
| `01-shenyu-ops-schema.sql` | shenyu-ops（DROP 后空库） | ✅ | ~27s；186 flyway history；~99 表（V190 −3、V191 −7 sys_*） |
| `verify-schema.sql` | shenyu-ops | ✅ | 全部 OK（含 V190/V191 负检 + `flyway_v190`/`flyway_v191`/`flyway_sql_migrations_186` + `sys_param`/`sys_message`；**前置 = 01 only**） |
| `03-shenyu-ops-seeds.sql` | shenyu-ops | ✅ | 占位符替换为 9402/9404 后 |
| `02-shenyu-system-menus.sql` | 本地 **shenyu-sys** | ✅ | 列与 Beta 测试库 SSOT 一致 |
| `02-shenyu-system-menus.sql` | 本地 shenyu-system（overlay） | ❌ | 无 `user_type` — 勿用；改连 shenyu-sys 或 Beta |

---

## 附录：已有 Ops 增量升级（V181–V191）

部署新版 JAR + `FLYWAY_ENABLED=true`（推荐）。

| 版本 | 内容 | 手工替代 |
|------|------|----------|
| V190 | DROP `sys_dict_*` + `sys_operation_log` | `drop-ops-legacy-sys-tables.sql` §1 |
| V191 | DROP `sys_tenant` / `sys_user*` / `sys_role*` / `sys_permission` | `drop-ops-legacy-sys-tables.sql` §2 |

若 DBA 已跑 `01` 至 V189，可 JAR Flyway 补 V190→V191，或单独执行 `scripts/integration-config/drop-ops-legacy-sys-tables.sql`。System 侧若缺工作任务菜单，补跑 `sql/02-shenyu-system-menus.sql` 中对应段。**勿**单独部署 V185–V187 match pool（V189 已 DROP）。

**Beta 测试库（2026-08-25）：** 当前 flyway SQL count=184（至 V189）；V190/V191 待 JAR 重启后 Flyway 补跑。

## 相关

- [rollback.md](./rollback.md) · ADR-064/070/071/072 · E2E: `docs/delivery/e2e-artifacts/WORK-TASK-E2E-20260819/`
- SQL 再生成：`scripts/integration-config/gen-ops-greenfield-sql.py`（需 Flyway 源目录）
