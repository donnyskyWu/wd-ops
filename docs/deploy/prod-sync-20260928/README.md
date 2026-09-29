# 生产库同步包 · 2026-09-28

本目录打包 **V205 / V206 架构变更** 与 **0928 Excel 账号导入** 相关 SQL，按数据库拆分。**请勿在本机对生产直连执行**；由 DBA 在变更窗口按顺序手工执行。

Flyway SSOT：`football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/`

---

## 涉及数据库（与 prod 配置对照）

| 库名 | 用途 | 本批次 |
|------|------|--------|
| **shenyu-ops** | OPS 业务表（`OPS_DB_NAME`，Nacos 模板默认 `shenyu-ops`） | **有** 架构 + 可选数据 |
| **shenyu-system** | Football 用户 / 字典 | **无** SQL（见 `03-shenyu-system-NO-CHANGES.md`） |
| **shenyu-member** | 会员域 | **无变更** |

配置参考：

- JAR：`BOOT-INF/classes/application-prod.yaml` — `jdbc:mysql://…/${OPS_DB_NAME}`，`FLYWAY_ENABLED` 默认 **true**
- Nacos 生产模板：`docs/deploy/ops-greenfield-production/config/nacos-ops-server-prod.yaml` — 库名 **shenyu-ops**，`spring.flyway.enabled: **false**`（手工 SQL + 可选写 history）

---

## 变更摘要

| 版本 | 内容 |
|------|------|
| **V205** | `oa_account.holder_user_id`；`oa_work_task_assignment.publish_account_id` + 索引 |
| **V206** | `oa_account.short_video_status` / `live_status` / `operator_user_id` + 索引（ADR-078） |
| 字典 | 双状态为 VARCHAR 原文，**无** dict seed |

代码能力：M2 任务发布账号（V205）、M4 抖音/快手导入与列表展示（V206）。**member 库无迁移。**

---

## 单文件一次执行（架构 + 抖快预清理 + 0928 UPSERT）

**推荐文件（一条命令跑通）**

| 项 | 内容 |
|----|------|
| **路径** | `docs/deploy/prod-sync-20260928/shenyu-ops-FULL-schema-and-data-20260928.sql` |
| **目标库** | `shenyu-ops` |
| **重新生成** | `python docs/deploy/prod-sync-20260928/_merge_prod_sync_sql.py`（改 01/03/04 等分步文件后） |

**内含分段（按顺序）**

| 段 | 内容 | 本文件状态 |
|----|------|------------|
| **A** | V205/V206 幂等 DDL | ✅ 执行 |
| **B** | `flyway_schema_history` 写入 | ⏸ 注释（Nacos `flyway.enabled=false` 时手工取消注释） |
| **C** | tenant=1 仅 DOUYIN/KUAISHOU 预清理（含子表，**不**删公司/实名人/设备/SIM） | ✅ **已启用**（非注释） |
| **D** | 0928 Excel 批次 UPSERT | ✅ **已启用**（非注释） |
| **E** | holder/operator/双状态 PATCH | ⏸ 注释 |
| **F** | 只读校验（`verify-schema-V205-V206.sql`） | ✅ 执行 |

**执行命令**

```bash
cd docs/deploy/prod-sync-20260928
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < shenyu-ops-FULL-schema-and-data-20260928.sql
```

**执行前必读**

1. **备份** shenyu-ops（全库或 `oa_*` 相关表）。
2. **tenant_id = 1** 为脚本硬编码；生产默认租户不是 1 时**禁止**直接执行。
3. **Section C** 会删除 tenant=1 下**全部**抖音/快手账号及子表；其它平台与支撑表不动。
4. **Section D** 对同 id / 同唯一键 UPSERT，**不改 id**；生产已占用同 id 但业务含义不同时须人工核对。
5. `holder_user_id` / `operator_user_id` 须对应 shenyu-system 已存在用户（ADR-056）。
6. 脚本含 AES 加密密码字段，按敏感数据规范管控。
7. 若生产 **Flyway 关闭**且已手工跑完 A，可同时取消 Section B 注释，避免日后误开 Flyway 重复 DDL。

---

## 快速一条龙（单文件 · 其它场景）

不想按 01→02→03→04→05 分步执行时，可用本目录合并脚本（**未对任何库执行**）：

| 场景 | 文件 | 说明 |
|------|------|------|
| 生产 · 仅架构（+ 可选段需手工取消注释） | `shenyu-ops-FULL-prod-20260928.sql` | Section A + F 直接执行；B/C/D/E 为 `--` 注释块 |
| 生产 · 架构 + 抖快预清理 + 0928 UPSERT | `shenyu-ops-FULL-schema-and-data-20260928.sql` | 见上一节「单文件一次执行」 |
| 测试 / Beta · 全量替换抖音/快手 + 支撑表批次 | `scripts/import/import-accounts-shenyu-ops-20260928.sql` | **测试专用**（PRE-IMPORT 含支撑表批次 DELETE） |

```bash
# 生产：只跑架构 + 末尾只读校验（预清理/数据/PATCH 在文件内取消对应 UNCOMMENT 块）
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < shenyu-ops-FULL-prod-20260928.sql

# 生产：架构 + 抖快预清理 + 0928 UPSERT 一次跑通（重新导入抖快场景）
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < shenyu-ops-FULL-schema-and-data-20260928.sql

# 生产分步（推荐）：先删抖快，再 UPSERT（公司/实名人/设备等不批量删）
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 04-shenyu-ops-data-import-0928-OPTIONAL.sql

# 测试：schema（若未 V206）+ PRE-IMPORT 全段 + 数据 + patch 一体
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < scripts/import/import-accounts-shenyu-ops-20260928.sql
```

**取消注释提示（FULL-prod）：**

- Section B：搜索 `UNCOMMENT IF flyway disabled`，去掉该块每行前的 `-- `（Nacos `spring.flyway.enabled=false` 时）
- Section C：搜索 `UNCOMMENT TO PRECLEAN DOUYIN/KUAISHOU`（重导抖快前）
- Section D：搜索 `UNCOMMENT TO IMPORT 0928 DATA`
- Section E：搜索 `UNCOMMENT IF PATCH holder/operator`

分步文件与合并文件内容同源；改 01/02/03/04/05 后执行 `python docs/deploy/prod-sync-20260928/_merge_prod_sync_sql.py` 可重新生成 FULL 文件。

---

## 推荐执行顺序（生产）

| 步骤 | 文件 | 目标库 | 必须？ |
|------|------|--------|--------|
| 0 | 备份 shenyu-ops（全库或相关表） | shenyu-ops | **是** |
| 1 | `01-shenyu-ops-schema-V205-V206.sql` | shenyu-ops | **是**（发版前/与应用同窗口） |
| 2 | `verify-schema-V205-V206.sql` | shenyu-ops | 建议（只读校验） |
| 3 | `02-shenyu-ops-flyway-history-insert.sql` | shenyu-ops | **可选** — 仅当 prod **关闭 Flyway** 且已手工跑完 01 |
| 4 | `03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql` | shenyu-ops | **可选** — **重新导入抖快**时执行；删 tenant=1 全部 DOUYIN/KUAISHOU（含子表），**不**删公司/实名人/设备/SIM |
| 5 | `04-shenyu-ops-data-import-0928-OPTIONAL.sql` | shenyu-ops | **可选** — 见下文警告；支撑表 UPSERT |
| 6 | `05-shenyu-ops-patch-user-fields-OPTIONAL.sql` | shenyu-ops | **可选** — 导入后补 holder/operator/双状态 |
| — | `03-shenyu-system-NO-CHANGES.md` | — | shenyu-system 无 SQL，无需执行 |

**生产导入策略（2026-09-28 批次）**

- **抖音/快手**：允许「先 03 预清理 → 再 04 导入」整批替换。
- **公司 / 实名人 / 手机 / SIM**：**禁止**生产批量 DELETE；04 仅 UPSERT；误导入批次用 `04a` 按 id 段/creator 回滚，**不会**删全库支撑数据。

```bash
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 01-shenyu-ops-schema-V205-V206.sql
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < verify-schema-V205-V206.sql
# 若 Nacos flyway.enabled=false：
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 02-shenyu-ops-flyway-history-insert.sql
# 重新导入抖快（可选）：
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql
mysql -h<host> -u<user> -p --default-character-set=utf8mb4 shenyu-ops < 04-shenyu-ops-data-import-0928-OPTIONAL.sql
```

---

## Flyway vs 手工

| 场景 | 做法 |
|------|------|
| 生产 **Flyway 开启**（`FLYWAY_ENABLED=true`） | 发版新 JAR 后由应用自动跑 V205/V206；**仍可**先跑 01（幂等）做「先发 SQL、后发版」；**不要**重复写 history 除非与 DBA 对齐 checksum |
| 生产 **Flyway 关闭**（Nacos 模板） | **必须**手工执行 01；建议再执行 02，避免日后误开 Flyway 重复 DDL |

---

## 数据脚本：生产 vs 测试

| 脚本 | 环境 | 说明 |
|------|------|------|
| `scripts/import/import-accounts-shenyu-ops-20260928.sql` | **测试 / Beta 破坏性全量** | PRE-IMPORT **全段**：抖快 + 批次支撑表 DELETE |
| `03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql` | **生产（可选）** | 仅删 tenant=1 抖快及子表；**不**动支撑表 |
| `04-shenyu-ops-data-import-0928-OPTIONAL.sql` | 生产（业务确认后） | **UPSERT**；抖快账号在 03 清空后可 INSERT |
| `04a-shenyu-ops-data-import-0928-ROLLBACK-BATCH.sql` | 生产/测试 | 仅删 Excel **批次**（id 段 / creator）；**不**删 tenant 全量抖快，**不**删非批次公司/实名人等 |
| `05-shenyu-ops-patch-user-fields-OPTIONAL.sql` | 生产/测试 | 与 `scripts/import/patch-accounts-user-fields-20260928.sql` 同源 |

### 数据导入前检查（tenant_id 警告）

- 生成脚本固定 **`tenant_id = 1`**。生产若默认租户不是 1，**禁止**直接执行；需重新生成或改 WHERE。
- 固定 ID 段：公司 ≥100001、实名人 ≥110001、设备 ≥120001、账号 ≥140001 等。04/FULL Section C 对**同主键或同唯一键**行做 UPSERT，**不会改 id**；若生产已占用同 id 但业务含义不同，仍须人工核对后再跑。
- `holder_user_id` / `operator_user_id` 为 shenyu-system 用户 id；导入前确认用户存在（ADR-056）。
- 脚本含 **AES 加密密码字段**（非明文 env）；仍属敏感数据，按账号安全规范管控。

---

## 回滚说明（架构）

手工回滚 **仅建议在变更窗口内、无新数据依赖新列时** 由 DBA 评估：

```sql
-- 示例：删除 V206 列（先确认无业务写入）
-- ALTER TABLE oa_account DROP COLUMN operator_user_id, DROP COLUMN live_status, DROP COLUMN short_video_status;
-- DROP INDEX idx_oa_account_operator_user ON oa_account;

-- V205
-- ALTER TABLE oa_work_task_assignment DROP COLUMN publish_account_id;
-- ALTER TABLE oa_account DROP COLUMN holder_user_id;
```

若已执行 02，需同步 `DELETE FROM flyway_schema_history WHERE version IN ('205','206');`（参见 `docs/deploy/ops-greenfield-production/rollback.md`）。

数据回滚：优先 `04a-…-ROLLBACK-BATCH.sql`（批次级）；抖快整批重导用 `03` + `04`，**不要**在生产跑测试脚本 PRE-IMPORT 的支撑表 DELETE 段。

---

## 幂等重跑（Section C / 04 / FULL schema-and-data）

| 表 | 冲突键 | 重跑行为 |
|----|--------|----------|
| `oa_company` | 主键 `id`；或 `uk_oa_company_credit` (tenant_id, credit_code) | 刷新 `company_name` / `credit_code` / `status` / `updater`；**不修改 id** |
| `oa_realname` | 主键 `id` | 刷新姓名、证件/手机密文、状态、`updater` |
| `oa_phone` | 主键 `id`；或 `uk_oa_phone_number` (tenant_id, phone_number_hash) | 刷新实名人、密文/哈希、设备号、keeper、状态等 |
| `oa_sim_card` | 主键 `id`；或 `uk_oa_sim_phone` (tenant_id, phone_number_hash) | 刷新 phone_id、运营商、套餐、assigned_user_id 等 |
| `oa_account` | 主键 `id`；或 `uk_oa_account_platform_ext` (tenant_id, platform_type, external_account_id) | 刷新账号名、FK、密码、双状态、holder/operator 等；**不修改 id** |

**典型场景**

1. **首次导入中断后重跑 04**：同批次 id 槽（如 `120005`）已占但 hash 已变 → 先 UPDATE 该 id 槽再跳过 INSERT，避免 `1062 Duplicate entry '120005'`。
2. **Excel 修订后重跑 04**（不先 04a）：同 id 行字段更新为新生成值；`creator` 不在 UPDATE 列表中，保留首次导入值。
3. **生产已有同平台 external_account_id、不同 id**：触发 `uk_oa_account_platform_ext`，更新**已有行**（保留生产 id）；需业务确认 FK 指向是否仍正确。

**仍非幂等 / 需慎用**

- `03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql` 会删除 tenant 下全部抖音/快手（生产重导抖快时**按需**执行）。
- 测试脚本 **PRE-IMPORT 全段** 还会删批次支撑表（仅 Beta/本地 destructive test）。
- `04a-…-ROLLBACK-BATCH.sql` 为 DELETE，非 UPSERT。
- Section A 架构脚本：列/索引已存在则跳过（information_schema），可重复执行。

**验证重跑**（只读示例）

```sql
SELECT id, company_name, updater FROM oa_company WHERE id = 100001;
-- 重跑 04 后 updater 仍为 xlsx-import-20260928（或 patch 脚本中的 updater）
```

---

## 文件清单

| 文件 | 说明 |
|------|------|
| `shenyu-ops-FULL-prod-20260928.sql` | **一条龙** · 架构 + 注释可选 B/C/D + 校验 |
| `shenyu-ops-FULL-schema-and-data-20260928.sql` | **一条龙** · 架构 + 抖快预清理(C) + 0928 UPSERT(D) 默认启用 |
| `_merge_prod_sync_sql.py` | 从 01/02/03/04/05/verify 重新合并 FULL 文件 |
| `01-shenyu-ops-schema-V205-V206.sql` | 幂等 DDL（V205+V206） |
| `02-shenyu-ops-flyway-history-insert.sql` | 可选 history |
| `03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql` | 可选 · 仅抖快预清理（生产重导） |
| `03-shenyu-system-NO-CHANGES.md` | system 库无变更说明（与上列 03 SQL 无关） |
| `04-shenyu-ops-data-import-0928-OPTIONAL.sql` | 可选数据 UPSERT（配合 03 重导抖快） |
| `04a-shenyu-ops-data-import-0928-ROLLBACK-BATCH.sql` | 批次清理 |
| `05-shenyu-ops-patch-user-fields-OPTIONAL.sql` | 可选字段 PATCH |
| `verify-schema-V205-V206.sql` | 只读验证 |

重新生成 04/05：  
`python scripts/import/prod-release/_generate_prod_sync_data_sql.py`

重新生成 FULL 合并文件：  
`python docs/deploy/prod-sync-20260928/_merge_prod_sync_sql.py`

---

## 前置批次

若生产尚未到 V204，请先执行  
`docs/deploy/ops-greenfield-production/sql/README-prod-incremental-20260914.md` 中的 V192→V204 增量脚本。
