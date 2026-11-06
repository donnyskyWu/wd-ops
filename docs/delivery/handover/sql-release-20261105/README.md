# M-Live 投产比 SQL Release · 2026-11-05

Handover：**一库一文件**，从 SSOT 合并可重复执行的 ROI/OA 结构变更。勿直连生产；与 [ADR-084](../../../adr/ADR-084-ROI-unify-ops-server.md) **ops-server（:48094）** 同窗口发版。

## M-Live 投产比 · 运维最小发布（默认）

仅表结构/种子/条件 ALTER，**不含** `roi_operation_log` / 库内审计触发器（ADR-085）及 Section 15 钉钉可选段。

**2026-11-06**：`shenyu-ops-roi-minimal.sql` 已含 Section **01A**（`oa_ip_group*` 空表）与 **01B**（`oa_account` / `oa_account_cost` 基表）、**01C**（M4 `oa_company` / `oa_realname*` / `oa_phone` / `oa_sim_card` + `oa_account` 与 `AccountDO` 对齐的增量列；避免仅导 01B 时「平台账号管理」并行请求选择器表 404），以及 **14B**（`roi_attachment` / `roi_wecom_media` 的 `infra_file_*` 列与 `storage_name` 长度，SSOT `20261106_roi_infra_file_storage.sql`）。若环境已导入旧版 minimal，可另跑 `../sql-release-20261106/shenyu-ops-oa_account_cost-bootstrap.sql`（幂等；与 01B+Section 03 重叠可跳过）。

| 顺序 | 文件 | 库 |
|---|---|---|
| 1 | **`shenyu-ops-roi-minimal.sql`** | shenyu-ops |
| 2 | **`shenyu-system.sql`** | shenyu-system（原生只读边界 + 字典；与 ops 边界相关时必跑） |

无需 `log_bin_trust_function_creators`。**操作日志 Tab / `GET …/audit-logs` 不在 M-Live 范围（ADR-085）。**

平台全量（含 Section 03 等例程）：`shenyu-ops-schema.sql` + `shenyu-ops-routines.sql`（或 DBA 已开 trust 时 `shenyu-ops-full.sql`）。详见 `RUNME.md`。

## MySQL 5.7 / MariaDB 10.x 兼容性

SSOT 可面向 MySQL 8 编写；`_build_sql_release_20261105.py` 写出前对合并结果做下列变换（生成物 collation 统一为 `utf8mb4_unicode_ci` / `utf8mb4_bin`，非 MySQL 8 默认 `utf8mb4_0900_*`）：

| SSOT（MySQL 8+） | 生成物（5.7 / MariaDB 10.x） |
|---|---|
| `utf8mb4_0900_ai_ci` 等 | `utf8mb4_unicode_ci` 或 `utf8mb4_bin` |
| `DEFAULT (UTC_TIMESTAMP(n))` | `DEFAULT CURRENT_TIMESTAMP(n)` |
| `CREATE FUNCTION` / `PROCEDURE IF NOT EXISTS` | 同 DELIMITER 块内 `DROP … IF EXISTS` + `CREATE …` |
| `CREATE TRIGGER IF NOT EXISTS` | 同 DELIMITER 块内 `DROP TRIGGER IF EXISTS` + `CREATE TRIGGER` |

**语义注意**

- `DEFAULT CURRENT_TIMESTAMP(n)` 在 MySQL 5.7 按**会话时区**写入，非 UTC；触发器/DML 中的 `UTC_TIMESTAMP(3)` 仍为 UTC。生产若要求默认 UTC，导入前 `SET time_zone = '+00:00'` 或统一服务器时区。
- 函数/过程/触发器采用 **DROP + CREATE** 以实现可重复导入；重复执行会短暂移除已有例程，请在低峰或停写窗口执行 `shenyu-ops-routines.sql`。

**未自动变换（DBA 知晓）**

- `CHECK (...)` 约束：MySQL 5.7 接受语法但**不强制**（8.0.16+ 才强制）；MariaDB 10.2+ InnoDB 会强制。本 release 保留 SSOT 的 CHECK 定义。
- 脚本内 `JSON_*` 函数：需 MySQL 5.7.8+ / MariaDB 10.2+（本 release 目标版本已满足）。

重新生成后脚本会自检上述「坏模式」计数为 0（见 `_build_sql_release_20261105.py` 内 `MYSQL57_VERIFY_PATTERNS`）。

### Error 1419（binlog + CREATE FUNCTION）

无 SUPER、`log_bin=ON`、`log_bin_trust_function_creators=OFF` 时，**整包** `shenyu-ops.sql` / `shenyu-ops-full.sql` 会在 Section 03 起失败。**不能**靠改 SQL 内容绕过，须 DBA 开 trust 变量。

| 角色 | 推荐文件 |
|---|---|
| 普通运维（无 GLOBAL 权限） | 先 `shenyu-ops-schema.sql`（表/种子，**不触发 1419**）→ 请 DBA 按 `DBA-1419-一次性设置.md` 开 trust → 再 `shenyu-ops-routines.sql` |
| DBA 已开 trust | 可一次导入 `shenyu-ops-full.sql` 或 `shenyu-ops.sql`（与 full 内容相同） |

| 项 | 说明 |
|---|---|
| 运维 copy-paste | `DBA-1419-一次性设置.md` |
| 前置说明 | `00-mysql57-prerequisites.sql`（**不含** `SET GLOBAL`） |
| DBA 命令 | `SET GLOBAL log_bin_trust_function_creators = 1;` |
| 持久化 | 自建 `my.cnf`；阿里云 RDS / PolarDB / 腾讯云 CDB：**参数组 + 重启** |
| 校验 | `SHOW VARIABLES LIKE 'log_bin_trust_function_creators';` → `ON` / `1` |

`shenyu-system.sql` **无** CREATE FUNCTION/触发器，**不会** 1419。

**本 release 必执行**：`shenyu-ops-schema.sql` +（DBA 后）`shenyu-ops-routines.sql`，以及 `shenyu-system.sql`。`shenyu-pay` / `shenyu-member` / `shenyu-match` 无 DDL。

## 文件

| 文件 | 库 | 说明 |
|---|---|---|
| `00-mysql57-prerequisites.sql` | — | 1419 背景与校验（非必跑 DDL） |
| `DBA-1419-一次性设置.md` | — | **给 DBA/运维** 的参数组与 SET GLOBAL 步骤 |
| **`shenyu-ops-roi-minimal.sql`** | shenyu-ops | **M-Live 运维默认**：Section 01、01A–01C、02–03、05–07、09–14、14B；无例程/触发器 |
| `shenyu-ops-schema.sql` | shenyu-ops | 平台全量第一步：Section 01–03、05–07、09–14、14B 表/种子（无 DELIMITER） |
| `shenyu-ops-routines.sql` | shenyu-ops | 平台第二步（须 trust）：函数/触发器 + Section 03 存储过程版列扩展 |
| `shenyu-ops-full.sql` / `shenyu-ops.sql` | shenyu-ops | 完整单文件（schema + routines）；**仅** DBA 已开 trust 时使用 |
| `shenyu-system.sql` | shenyu-system | 原生只读边界表 + `dict_roi_dimension` |

## 执行顺序

1. **备份** `shenyu-ops`、`shenyu-system`。
2. `mysql … shenyu-ops < shenyu-ops-schema.sql`（无钉钉时跳过 schema 内 **Section 15 `[OPTIONAL]`**）。
3. **DBA** 按 `DBA-1419-一次性设置.md` 设置 `log_bin_trust_function_creators = 1` 并校验。
4. `mysql … shenyu-ops < shenyu-ops-routines.sql`（或步骤 3 已完成时直接 `shenyu-ops-full.sql` 代替 2+4）。
5. `mysql … shenyu-system < shenyu-system.sql`
6. 部署 **ops-server**；冒烟 `GET /admin-api/ops/roi/bootstrap`。

测试：`scripts/run-handover-sql-test.ps1`（默认只跑 schema + system；`MYSQL_TRUST_CREATORS=1` 时追加 routines）。速查 `RUNME.md`。

## Flyway 注意（ops-server · shenyu-ops）

| Flyway | 与本包对应 |
|---|---|
| V205 | `shenyu-ops.sql` Section 12 |
| V206 | Section 13 |
| V42 `dict_roi_dimension` | `shenyu-system.sql` Section B |

若生产 **Flyway 已开启**：V205/V206 与本包 12–13 等价，可先手工 SQL 再发版，或仅发版由 Flyway 执行（勿重复执行同一 DDL）。ROI 主体表无 V207+，SSOT 为 `sql/mysql/updates/` Section 01–11。

## 重新生成

```bash
python docs/delivery/handover/sql-release-20261105/_build_sql_release_20261105.py
```

SSOT：`football-backend-saas/sql/mysql/updates/`、`football-module-ops-server/.../db/migration/V205|V206*.sql`。
