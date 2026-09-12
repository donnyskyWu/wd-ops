# Ops 改动梳理 — 2026-08-26

> 会话弧汇总：Greenfield 部署包 · Legacy sys_* 退役 · 直播时长 S-tier · Collector 远程配置  
> SSOT 同步：PRD-M6 · PRD-M10 · API-M6 · UX-M6 · CHECKLIST-M6 · 部署/联调指南

---

## 1. 分类总览

| 类别 | 项 | 是否 PRD 变更 |
|------|-----|---------------|
| **需求变更** | 直播时长：stub → live-server RPC；作者维度；小时单位；v1 无 peak_viewers | ✅ PRD-M6 §2.4 · AC-M6-002-4/5 |
| **需求澄清** | M10：Ops 必须支持 `COLLECTOR_BASE_URL` 指向 unify-collector-api | ✅ PRD-M10 §4.3（环境配置，非新 FR） |
| **架构/终态** | shenyu-ops 仅保留 sys_param/message/metadata_*/notification_event；身份/字典 → Feign | ADR-056；Greenfield omit V190/V191 DDL |
| **工程修复** | SQL 生成器：strip legacy sys_*、块注释 split、omit 注释 SQL、history 对齐 JAR | 部署文档 only |
| **工程修复** | verify-schema.sql：1146 安全、flyway 检查 | 部署文档 only |
| **工程修复** | Flyway 本地 repair 脚本；gen-ops-flyway-history description 空格 | 联调/本地 only |
| **工具/流程** | Greenfield DBA 三包；OPERATIONS-GUIDE 排障；prod 顺序 DB→JAR 同 tag | 交付文档 |
| **非需求** | Git merge master→ops；JAR rebuild 说明 | 不写入 PRD |

---

## 2. 需求变更详述

### 2.1 直播时长 S-tier（M6 / FR-M6-002）

| 项 | 之前 | 之后 |
|----|------|------|
| 数据源 | 账号 stub / 假数据 | `LiveRoomApi.getLiveRoomCount` Feign |
| 维度 | 账号 | **作者**（`oa_ip_group_anchor_rel`） |
| 时长单位 | 不一致 | **小时**（RPC 分钟 ÷60，1 位小数） |
| peak_viewers | 可能有列 | v1 **不展示**（后端 `"-"` 占位） |
| 前端 | 旧 KPI/占比 | `ReportLiveDuration.vue`：趋势双轴 + 作者明细 + Excel 导出 |

**后端**：`ReportServiceImpl.liveDuration*` · `LiveRoomReadService` · `LiveRoomApi`（ops Feign 客户端）  
**前端**：`football-front/.../ReportLiveDuration.vue`  
**E2E**：[LIVE-DURATION-S-TIER-20260826/REPORT.md](./e2e-artifacts/LIVE-DURATION-S-TIER-20260826/REPORT.md)

### 2.2 Collector 远程配置（M10 集成）

| 项 | 说明 |
|----|------|
| `COLLECTOR_BASE_URL` | `oa.unified-collector.base-url` |
| 本地联调 | `Import-OpsCollectorRemoteEnv` in `start-integration-oa.ps1` |
| 错误信息 | `CollectorErrorMessages` 展示已配置 URL |
| 文档 | OPS-TEST-DB § Unified Collector · PRD-M10 §4.3 |

### 2.3 Legacy sys_* harness（V190/V191）

Greenfield `01` **不创建** `sys_tenant/user/role/dict/operation_log`；终态仅业务表 + 上表四类 sys_*。  
增量库：Flyway V190/V191 或 `drop-ops-legacy-sys-tables.sql`。  
E2E：[LEGACY-SYS-HARNESS-RETIRE-20260825/REPORT.md](./e2e-artifacts/LEGACY-SYS-HARNESS-RETIRE-20260825/REPORT.md)

---

## 3. 工程 / 部署（非 PRD）

### 3.1 Greenfield SQL 生成器（2026-08-25+）

- `scripts/integration-config/gen-ops-greenfield-sql.py` — omit V190/V191 legacy sys_*；块注释感知 `split_sql_statements`；不输出 `--` 注释 SQL
- `scripts/integration-config/gen-ops-flyway-history.py` — description 空格非 underscore
- 产物：`docs/deploy/ops-greenfield-production/sql/01|02|03*.sql`、`verify-schema.sql`

### 3.2 Flyway 本地修复

| 脚本 | 用途 |
|------|------|
| `repair-flyway-local-validate.sql` | description `seed_base` → `seed base` |
| `repair-flyway-checksums-local.sql` | checksum 对齐当前 JAR |
| `repair-flyway-local-v161-v162.sql` | 历史 V161/V162 专项 |

### 3.3 生产部署顺序

1. 同一 **release tag** 执行 DBA 三包 SQL（01 → 02 → 03）
2. `verify-schema.sql` 全 OK
3. 部署 **同 tag** `football-module-ops-server.jar`（Flyway 仅补 V113 Java）
4. Nacos prod：`COLLECTOR_BASE_URL` 等见 `config/env-variables.md`

---

## 4. 主要文件清单

### 4.1 代码（football-module-ops / front）

| 路径 | 变更 |
|------|------|
| `football-module-ops-server/.../ReportServiceImpl.java` | liveDurationList/Trend/Row |
| `football-module-ops-server/.../LiveRoomReadService.java` | Feign 封装 |
| `football-module-ops-server/.../LiveRoomApi.java` + DTO | ops 侧 Feign 契约 |
| `football-module-ops-server/.../UnifiedCollectorApiClient.java` | 错误 URL 展示 |
| `football-module-ops-server/.../CollectorErrorMessages.java` | COLLECTOR_BASE_URL 提示 |
| `football-module-ops-server/src/main/resources/application-local.yaml` | `${COLLECTOR_BASE_URL:...}` |
| `football-front/.../ReportLiveDuration.vue` | 作者列、小时、趋势 |

### 4.2 脚本 / 部署包

| 路径 | 变更 |
|------|------|
| `scripts/integration-config/gen-ops-greenfield-sql.py` | Greenfield 生成 |
| `scripts/integration-config/gen-ops-flyway-history.py` | history 描述 |
| `scripts/integration-config/repair-flyway-*.sql` | 本地 Flyway 修复 |
| `scripts/lib/integration-preflight.ps1` | `Import-OpsCollectorRemoteEnv` |
| `scripts/start-integration-oa.ps1` | 调用 collector env 导入 |
| `docs/deploy/ops-greenfield-production/**` | 三包 SQL + OPERATIONS-GUIDE |

### 4.3 文档（本次 sync）

| 路径 | 变更 |
|------|------|
| `docs/product/PRD-M6-数据分析.md` | §2.4 直播时长 + AC |
| `docs/product/PRD-M10-数据采集.md` | §4.3 COLLECTOR_BASE_URL |
| `docs/engineering/API-M6-数据分析.md` | §2.4 API 字段 |
| `docs/product/UX-M6-数据分析.md` | P-M6-005 布局 |
| `docs/delivery/CHECKLIST-M6-数据分析.md` | S-tier 勾选 |
| `docs/delivery/OPS-TEST-DB.md` | Unified Collector 节 |
| `docs/delivery/OPS-DEV-DEPLOY-GUIDE.md` | §4.1.1 Greenfield + collector FAQ |
| `docs/deploy/ops-greenfield-production/README.md` | 再生成 + repair |
| `docs/deploy/ops-greenfield-production/OPERATIONS-GUIDE.md` | 相关链接 |
| `docs/delivery/MASTER-EXECUTION-TRACKER.md` | DOC-SYNC-20260826 行 |
| `docs/delivery/CHANGELOG-OPS-20260826.md` | 本文件 |
| `docs/delivery/e2e-artifacts/LIVE-DURATION-S-TIER-20260826/REPORT.md` | E2E 签收 |

---

## 5. 生产注意事项

- **DB 与 JAR 同版本 tag**；勿用旧版 `01`（会残留 sys_dict SET 或 Flyway checksum 不一致）
- Greenfield 后 **勿**在 shenyu-ops 期望 `system_*` / legacy sys_user 表
- `COLLECTOR_BASE_URL` 生产必设；健康检查 `/livez`
- 直播时长依赖 **live-server** 注册可达；无数据时先查 Feign + `oa_ip_group_anchor_rel`
- Beta  profile Flyway off 时 V190/V191 需手工 `drop-ops-legacy-sys-tables.sql`

---

*关联 ADR：ADR-056（Football 用户 SSOT）· ADR-070（XXL-JOB）· Greenfield legacy omit（OPERATIONS-GUIDE §Step 2）*
