# OPS 过程文档包 — 目录说明

**适用**：研发/测试/运维接手、二次开发与验收追溯  
**仓库 SSOT**：`wd`  monorepo（本 Zip **不包含** `football-backend` / 前端子模块内独立文档；集成细节见 ADR-049/050/056 与部署三件套）  
**打包日**：2026-09-22  
**开发版 PRD**：`完整PRD-v9.2-开发版.md`（仓库根目录；若仅有 v9.1 则 Zip 内文件名仍为实际版本）

---

## 1. Zip 顶层结构

| 目录 | 受众 | 内容摘要 |
|------|------|----------|
| `product-handover/` | 客户 / 项目经理 | **三件套**（需求 + 技术 + 部署运维）自包含 `.md` / `.docx`；本目录 `README.md` |
| `product-manuals/` | 业务用户 | 角色操作手册、流程手册（`docs/product/delivery/`） |
| `delivery-screenshots/` | 业务 / 培训 | 交付截图 PNG |
| **`process-docs/`** | **研发 / QA** | **过程文档包**（PRD、API、库表、ADR/UX、切片与验收） |

客户三件套与过程文档 **并列存放**，不互相替换；过程文档为仓库 Markdown/SQL 精选副本，便于离线阅读。

---

## 2. `process-docs/` 子目录

### 2.1 `process-docs/prd/` — 产品需求（开发版）

| 来源（仓库路径） | 说明 |
|------------------|------|
| `/完整PRD-v9.2-开发版.md` | 全模块开发版 PRD 总册 |
| `docs/product/PRD-*.md` | 分模块 PRD（M0–M10、增量如 AI 排版 / amphipoda 对齐） |
| `docs/product/PRD-业务版-v9.2.md` | 业务版（与三件套需求文档同源，便于对照 FR/AC） |

**未纳入**：`完整PRD-v9.1-开发版.md`（仅当 v9.2 缺失时作为根 PRD 打入）。

### 2.2 `process-docs/api/` — 接口设计

| 来源 | 说明 |
|------|------|
| `docs/engineering/API-M*.md` | M0–M11 模块 REST 契约（路径、DTO、错误码、鉴权） |

### 2.3 `process-docs/database/` — 数据库与部署 SQL

| 路径 | 说明 |
|------|------|
| `database/greenfield/*.md` | Greenfield 部署说明（`docs/deploy/ops-greenfield-production/`） |
| `database/greenfield/sql/*.sql` | 零基础建库脚本（`01`/`02`/`03`、增量包、`verify-schema.sql` 等） |
| `database/greenfield/config/` | 环境变量、Gateway、XXL-JOB 等配置说明（若有） |
| `database/reference/wd-schema.sql` | 历史 wd 库结构参考 |
| `database/SQL-MANIFEST.txt` | Greenfield SQL 文件名与大小清单（打包时生成） |

**未纳入 Zip**：

- `docs/delivery/e2e-artifacts/**` 下备份/探针 SQL（体积大、含环境快照）
- 运行时 Flyway 全量目录（以代码仓 `db/migration` 为准；Greenfield `01` 已 Consolidated 至约定版本）
- 含真实密钥的 `application-local*.yaml`（本地联调配置）

**DDL 补充**：表级决策与增量 DDL 叙述见 `process-docs/design/adr/`（含 schema-drift、Football 多库等 ADR）。

### 2.4 `process-docs/design/` — 详细设计

| 子目录 | 来源 | 说明 |
|--------|------|------|
| `design/adr/` | `docs/adr/ADR-*.md` | 架构/领域决策（约 70+ 篇，OPS 相关全量） |
| `design/ux/` | `docs/product/UX-M*.md` | 模块交互与页面规格 |
| `design/method/` | `docs/engineering/PHASE-DEV-METHOD.md` 等 | 阶段开发方法、AI 实现指南、全局约定 |
| `design/state/` | `docs/engineering/STATE-M*.md` | 模块实现状态快照 |

### 2.5 `process-docs/delivery/` — 切片与联调

| 类型 | 文件名模式 | 用途 |
|------|------------|------|
| 切片 | `SLICES-M*.md` | 实现切片与 DoD |
| 联调 | `OPS-DEV-DEPLOY-GUIDE.md` | 本地/Dev 启动（非客户部署三件套重复） |
| 计划 | `PLAN-ops-list-pagination.md` 等 | 专项实现/验收计划（若有） |

验收清单、测试用例与 Gate **不在此目录重复**，见 §2.6。

### 2.6 `process-docs/test/` — 测试用例与测试报告（完整项目交接）

| 子目录 | 来源（仓库路径） | 说明 |
|--------|------------------|------|
| **`test/testcases/`** | `docs/delivery/CHECKLIST-M*.md` | 模块交付 / 验收检查清单 |
| | `docs/delivery/TESTCASES-M*.md` | 模块测试用例（含 P0） |
| | `PLAYWRIGHT-OPS-SPECS-INDEX.md` | **生成索引**：ops/uat Playwright spec 仓库路径与 Zip 内打包清单 |
| **`test/automation/playwright/`** | `football-front/apps/web-ele/tests/`（及 `ops-platform-ui-vue/tests/` 若存在） | 匹配的 `*ops*.spec.ts` / `*uat*.spec.ts` 源码副本；`ops-list-pagination` 附带 `tests/helpers/` 最小依赖 |
| **`test/reports/`** | `docs/delivery/gates/*.md` | 阶段 Gate、S-R*、MDB、签收报告（Zip 内保留 `gates/` 子路径） |
| | `docs/delivery/MASTER-EXECUTION-TRACKER.md` | 总进度与 Gate 状态 SSOT |
| | `docs/delivery/*报告*.md`、`*REPORT*.md`、`UAT-*.md` 等 | 模块报告、UAT / 走查类 Markdown（**不含** `e2e-artifacts/` 归档） |

**典型数量（2026-09-22 打包校验）**：`test/testcases/` 约 27 项（13 CHECKLIST + 13 TESTCASES + Playwright 索引）；`test/reports/` 约 64 项（含 `gates/` 全量与根目录模块/UAT 报告；与 gates 同名的根目录副本会分别保留）；`test/automation/playwright/` 在子模块已检出时通常 4 个 `.ts`（2 spec + 2 helper）。

**Playwright 子模块**：若 `football-front/apps/web-ele/tests` 未检出，索引中保留文档引用的 SSOT 路径，且 Zip **不含** `.ts` 副本。

---

## 3. 刻意排除项（安全与体积）

| 类别 | 原因 |
|------|------|
| `SEED-AUTH-TOKENS.md`、未脱敏本地 YAML | 可能含 Token/密钥 |
| 仓库根 `_tmp_*`、`BOOT-INF/`、`.codegraph/`、日志 | 非交付物 |
| `docs/delivery/e2e-artifacts/` 归档 SQL/备份 | 体积大、环境特定 |
| `docs/virtual-asset-hub/` | 非 OPS 本期范围 |
| 过程文档包内工程 PNG（除 `delivery-screenshots/`） | 操作手册已带截图 |
| 子模块 `football-backend` / 前端仓内独立 README | 以 wd 文档 + ADR 集成为准 |

---

## 4. 与子模块文档的关系

| 范围 | 说明 |
|------|------|
| **wd SSOT** | PRD、API、ADR、UX、Greenfield SQL、CHECKLIST/SLICES/TESTCASES |
| **football-backend** | 运行时 Java/Flyway 源码；接口细节以 `API-M*.md` + 代码为准 |
| **前端** | 路由与页面以 `UX-M*.md` 及 `product-manuals` 中 ROUTE 索引为准 |

---

## 5. 再生成过程文档 Zip 内容

在仓库根目录：

```powershell
cd "d:\self\sy\运营数据平台\202606\wd"
python docs/delivery/product-handover/_pack_delivery_zip.py
```

仅校验过程文档收集逻辑（不写 Zip）：

```powershell
python docs/delivery/product-handover/_pack_process_docs.py
```

---

## 6. 修订记录

| 日期 | 说明 |
|------|------|
| 2026-09-22 | 首版：过程文档包纳入交付 Zip，与三件套并列 |
| 2026-09-22 | 测试用例/报告独立为 `process-docs/test/testcases` 与 `test/reports` |
| 2026-09-22 | Playwright ops/uat spec 源码纳入 `process-docs/test/automation/playwright/` |
