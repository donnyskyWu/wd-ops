# OPS 产品交付 — 技术规格说明书

**交付包版本**：2026-09-20  
**产品版本**：PRD v9.2  
**文档性质**：客户交付用独立技术说明（架构、模块、API 摘要、数据与安全自包含）  
**目标读者**：架构师、后端/前端负责人、集成与运维工程师  
**详细部署步骤**：见同包《OPS产品交付-部署运维手册》  

**编制依据**：Football 集成部署指南、开发版 PRD §2/§7/§8/§9、`docs/engineering/API-M*.md`、ADR-047/050/056 等。

---

## 目录

1. [系统架构](#1-系统架构)
2. [技术栈与制品](#2-技术栈与制品)
3. [后端模块映射（M0–M10）](#3-后端模块映射m0m10)
4. [API 规范与模块接口摘要](#4-api-规范与模块接口摘要)
5. [数据与多租户](#5-数据与多租户)
6. [安全、错误码与铁律](#6-安全错误码与铁律)
7. [外部集成](#7-外部集成)
8. [鉴权与 Football 用户 SSOT](#8-鉴权与-football-用户-ssot)
9. [前端与路由](#9-前端与路由)
10. [构建与源码位置](#10-构建与源码位置)
11. [部署架构摘要](#11-部署架构摘要)
12. [修订记录](#12-修订记录)

---

## 1. 系统架构

### 1.1 生产目标形态（Football 集成）

```text
浏览器 → football-front (:5777)
       → football-gateway (:48080, /admin-api)
       → Nacos 服务发现 (grayLb)
       → system-server / ops-server / member-server 等
```

| 组件 | 说明 |
|------|------|
| **前端** | `football-front`，Ops 页面位于 `apps/web-ele/src/views/ops/` |
| **Ops 后端** | `football-backend-saas/football-module-ops/`，注册名 **`ops-server`**，默认端口 **48094** |
| **网关** | 统一前缀 `/admin-api`；OAuth2 与租户由 Football 体系承载 |
| **配置中心** | Nacos（生产/联调 profile） |

### 1.2 逻辑分层

| 层 | 职责 |
|----|------|
| 用户层 | 运营、财务、审核、数据分析等角色 |
| 表现层 | Vue 3 + Element Plus + ECharts |
| 业务服务层 | ops-server 按域划分 Controller/Service |
| 引擎层 | SOP DAG、报表引擎、采集适配、AI 编排 |
| 数据层 | MySQL `shenyu-ops` + Redis；system 库用户/RBAC |
| 基础设施 | Nacos、XXL-JOB（按 profile）、统一采集服务 |

### 1.3 Standalone 说明

仓库内另有 `ops-platform-ui-vue` + 独立后端路径，用于部分开发/Gate；**客户生产签收以 Football 集成路径为准**。

---

## 2. 技术栈与制品

### 2.1 集成栈（签收路径）

| 维度 | 选型 |
|------|------|
| 后端语言 | Java 17+ |
| 框架 | Spring Boot 3.x、MyBatis Plus |
| 数据库 | MySQL 8.x |
| 缓存 | Redis |
| 注册/配置 | Nacos |
| 定时任务 | XXL-JOB（启用 profile 时） |
| 前端 | TypeScript、Vue 3 Composition API、Element Plus 2.x、Pinia、Axios |
| 测试 | JUnit 5、Mockito；前端 Vitest |

### 2.2 交付制品

| 制品 | 说明 |
|------|------|
| `football-module-ops-server.jar` | Ops 业务服务 |
| football-front 构建产物 | 含 `/ops/**` 路由的 Admin UI |
| Greenfield SQL 包 | `shenyu-ops` schema + system 菜单/字典 + seeds |

---

## 3. 后端模块映射（M0–M10）

| 产品模块 | ops-server 域（包/能力） | 典型表前缀 |
|----------|---------------------------|------------|
| M0 首页 | dashboard / home | 聚合查询 |
| M1 运营管理 | ip、author、account-analysis、follower、content-analysis、internal、productivity | `oa_ip_*`、`oa_author_*` |
| M2 内容生产 | sop、plan、task、work-task、content、layout-template、typeset、knowledge | `oa_content*`、`oa_task*`、`oa_sop*` |
| M3 绩效 | perf-template、perf-execute、perf-result、order-attribution | `oa_perf*` |
| M4 账号 | company、real-person、phone、phone-card、platform/personal account | `oa_company*`、`oa_account*` |
| M5 财务 | account-cost、roi | `oa_author_cost*` |
| M6 分析 | metric、report、custom-query、dashboard/screen、metadata | `oa_report*`、`sys_metadata_*` |
| M7 监测 | monitor（hit、low-score、external…） | 监测事实表 + 阈值配置 |
| M8 配置 | config（collect、threshold、ai-model…） | 配置表 |
| M9 系统 | sys_param、dict、message、dept sync | `sys_param`、`sys_message` |
| M10 采集 | collect task/quality、collector 客户端 | 采集任务/日志 |

**API 前缀约定**：主要为 `/admin-api/oa/...`；部分新接口为 `/admin-api/ops/...`（以网关路由为准）。历史文档中 `oa` 与 `ops` 模块名并存，Gateway 统一暴露。

---

## 4. API 规范与模块接口摘要

### 4.1 通用约定

| 项 | 约定 |
|----|------|
| 前缀 | `/admin-api/oa/`（及少量 `/admin-api/ops/`） |
| 风格 | REST：GET 查询、POST 创建、PUT 更新、DELETE 删除 |
| 分页 | `pageNo`/`pageSize` 或 `page`/`size`（以具体接口为准） |
| 响应 | `{"code": 0, "msg": "success", "data": ...}` |
| 业务错误 | OPS 域常用 **1500–1504**（见 §6） |
| 数据权限 | 拦截器按角色 + IP 组等附加过滤 |

### 4.2 模块 REST 前缀总表

| 模块 | 功能域 | 路径前缀 |
|------|--------|----------|
| M0 | 首页指标/趋势/待办 | `/admin-api/oa/dashboard/home/*` |
| M1 | IP 组 | `/admin-api/oa/ip-group/*` |
| M1 | 作者 | `/admin-api/oa/author/*` |
| M1 | 账号/粉丝/作品/内部内容/人效 | `/admin-api/oa/account-analysis/*` 等 |
| M2 | SOP | `/admin-api/oa/sop/*` |
| M2 | 计划 | `/admin-api/oa/plan/*` |
| M2 | 任务 | `/admin-api/oa/task/*` |
| M2 | 工作任务 | `/admin-api/oa/work-task/*` |
| M2 | 内容/审核/发布 | `/admin-api/oa/content/*` |
| M2 | jingcai 文案 | `/admin-api/oa/ai-content/generate` |
| M2 | AI 排版 | `/admin-api/oa/content/typeset/*` |
| M2 | 公推模板 | `/admin-api/oa/layout-template/*` |
| M3 | 绩效 | `/admin-api/oa/perf-*` |
| M4 | 账号资产 | `/admin-api/oa/company/*`、`/phone/*`、`/platform-account/*` 等 |
| M5 | 成本/ROI | `/admin-api/oa/account-cost/*`、`/roi/*` |
| M6 | 指标/报表/查询/大屏 | `/admin-api/oa/metric/*`、`/report/*`、`/custom-query/*` |
| M7 | 监测 | `/admin-api/oa/monitor/*` |
| M8 | 配置 | `/admin-api/oa/config/*` |
| M9 | 系统参数/字典 | `/admin-api/oa/system/param/*`、`/dict/*` |
| M10 | 采集 | `/admin-api/oa/collect/*` |

### 4.3 核心接口示例（M0 / M2）

#### 首页（M0）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin-api/oa/dashboard/home/metrics` | 核心指标（支持 IP 组筛选） |
| GET | `/admin-api/oa/dashboard/home/trend` | 内容发布趋势 |
| GET | `/admin-api/oa/dashboard/home/platform-dist` | 平台分布 |
| GET | `/admin-api/oa/dashboard/home/todos` | 待办提醒 |
| GET | `/admin-api/oa/dashboard/home/quick-actions` | 快捷入口 |

#### 计划管理（M2 · FR-M2-009）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin-api/oa/plan/list` | 计划列表 |
| POST | `/admin-api/oa/plan/create` | 创建草稿 |
| PUT | `/admin-api/oa/plan/update` | 编辑草稿 |
| POST | `/admin-api/oa/plan/{id}/start` | 启动 |
| POST | `/admin-api/oa/plan/{id}/terminate` | 申请终止 |
| GET | `/admin-api/oa/plan/{id}/tasks` | 计划任务明细 |

#### 工作任务（M2 · FR-M2-010）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin-api/oa/work-task/sheet/get-or-create` | 获取/创建当日登记单 |
| PUT | `/admin-api/oa/work-task/sheet/save` | 保存 DRAFT |
| POST | `/admin-api/oa/work-task/sheet/confirm` | 确认 → 生成 task + 草稿 |
| POST | `/admin-api/oa/work-task/sheet/withdraw` | 撤回 |
| GET | `/admin-api/oa/work-task/matrix` | 矩阵视图 |

#### 内容 / SOP / 排版（M2 摘要）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin-api/oa/sop/template/list` | SOP 模板列表 |
| POST | `/admin-api/oa/sop/node/validate-dag` | DAG 校验 |
| GET | `/admin-api/oa/task/my-tasks` | 我的任务 |
| POST | `/admin-api/oa/task/{id}/execute/complete` | 节点完成（含门禁） |
| GET | `/admin-api/oa/content/list` | 内容列表 |
| POST | `/admin-api/oa/content/create` | 创建内容 |
| POST | `/admin-api/oa/content/{id}/submit-review` | 提交审核 |
| POST | `/admin-api/oa/content/{id}/review` | 审核操作 |
| POST | `/admin-api/oa/content/{id}/publish` | 发布 |
| POST | `/admin-api/oa/ai-content/generate` | jingcai 文案 |
| POST | `/admin-api/oa/content/typeset/ai-semantic/preview` | AI 排版预览 |
| POST | `/admin-api/oa/content/typeset` | 一键排版 |
| GET | `/admin-api/oa/layout-template/list` | 公推模板列表 |

#### 运营管理 / 财务 / 采集（摘录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin-api/oa/ip-group/tree` | IP 组树 |
| GET | `/admin-api/oa/account-analysis/list` | 账号分析列表 |
| GET | `/admin-api/oa/account-cost/list` | 成本列表 |
| GET | `/admin-api/oa/roi/analysis` | ROI 分析 |
| GET | `/admin-api/oa/report/{code}/data` | 标准报表数据 |
| GET | `/admin-api/oa/monitor/hit/list` | 爆款列表 |
| GET | `/admin-api/oa/collect/task/list` | 采集任务 |
| GET | `/admin-api/oa/system/param/list` | 系统参数 |

> 完整字段级契约以各模块 OpenAPI/工程 API 文档为准；上表覆盖验收与集成联调的主路径。

---

## 5. 数据与多租户

### 5.1 库划分

| 库 | 用途 |
|----|------|
| **`shenyu-ops`** | Ops 业务 SSOT：`oa_*`、`sys_param`、`sys_message`、`sys_metadata_*`、`flyway_schema_history` |
| **`shenyu-system`**（或环境等价） | Football 用户、角色、菜单、字典（ADR-056） |
| 其他 Football 库 | member、match、mp 等，按 Feign 依赖 |

### 5.2 多租户

- 业务表含 **`tenant_id`**。
- 鉴权租户来自 Football 登录上下文；**禁止**硬编码 tenantId/userId。
- 跨租户访问返回 **1504**。

### 5.3 敏感字段

- 手机号、API Key 等：**AES-256**。
- 环境变量 **`OA_AES_KEY`**（Base64 32 字节），对应配置 `oa.crypto.aes-key`。

### 5.4 迁移要点（Greenfield）

1. 建库 `shenyu-ops`  
2. 执行 `01-shenyu-ops-schema.sql`（Flyway 溯源）  
3. 执行 `02-shenyu-system-menus.sql`（菜单 6100–6999、权限）  
4. 执行 `03-shenyu-ops-seeds.sql`（占位符需按环境替换）  
5. JAR 启动时 Flyway 补跑 Java migration（如 V113）

详见《部署运维手册》第 3 节。

---

## 6. 安全、错误码与铁律

### 6.1 OPS 业务错误码（1500–1504）

| 码 | 含义 |
|----|------|
| 1500 | 关联实体不存在 |
| 1501 | 关联实体已停用/注销 |
| 1502 | 关联实体已被引用或业务冲突 |
| 1503 | 字典/枚举值不合法（`@InDict`） |
| 1504 | 跨租户访问禁止 |

### 6.2 实现铁律（摘要）

1. 强关联 ID 必须选择器 + 后端校验（1500/1501/1502）。  
2. 枚举走字典校验（1503）。  
3. 全表租户隔离（1504）。  
4. 敏感字段 AES-256。  
5. 接口 `@PreAuthorize` 必须生效；Dev Token 仅替代登录来源，**权限从 DB 读取**。

### 6.3 排版保真

AI 排版/模板套用不得改变正文语义文字；保真失败返回业务码 **2037**（M2）。

---

## 7. 外部集成

| 集成方 | 用途 | 配置要点 |
|--------|------|----------|
| **Football / shenyu-system** | 登录、RBAC、字典 Feign | system-server；菜单 SQL |
| **统一采集 Collector** | M8/M10 采集执行 | `COLLECTOR_BASE_URL`、`COLLECTOR_API_TOKEN` |
| **jingcai.article** | 文案首生成/润色、工作任务 AI | `FOOTBALL_AI_SCHEME_*`、`FOOTBALL_AI_MODEL` |
| **M8 LLM** | AI 排版语义分段 | 配置管理 · AI 模型 + `sys_param` |
| **member-server** | 私域报表聚合（M6） | Nacos 发现 |
| **赛事 match** | 计划/任务赛事选择 | `oa.match.internal-base-url` |
| **钉钉** | 通知（非 SSO） | 系统参数 |

**AI 双路径**：文案默认 jingcai；排版走 M8 LLM 或 FOOTBALL_AI 一键排版；职责分离，不互相 fallback。

---

## 8. 鉴权与 Football 用户 SSOT

| 规则 | 说明 |
|------|------|
| 用户 ID SSOT | `shenyu-system.system_users.id`（ADR-056） |
| `LoginUser.userId` | 同上 |
| 写入 user 外键 | `FootballSystemUserValidator.resolveStorableUserId` |
| 回显 UserSelect | `resolvePresentableUserId`（兼容历史 ID） |
| Ops 菜单 ID 段 | 约 **6100–6999** |

---

## 9. 前端与路由

- **History 模式**，无 hash；生产路径形如 `/ops/production/plan`。
- Ops 根路径：`/ops`（menu_id 6100）。
- 主要分组：

| 分组 | Football 路由前缀 |
|------|-------------------|
| 首页 | `/ops/dashboard` |
| 运营管理 | `/ops/operations/*` |
| 内容生产 | `/ops/production/*` |
| 绩效 | `/ops/performance/*` |
| 财务 | `/ops/finance/*` |
| 账号 | `/ops/internal/*` |
| 数据分析 | `/ops/analysis/*` |
| 作品监测 | `/ops/monitor/*` |
| 数据采集 | `/ops/collect/*` |
| 配置 | `/ops/config/*` |
| 系统(OA) | `/ops/system-oa/*` |

代表页面：内容管理 `/ops/production/content`；工作任务 `/ops/production/work-task`；IP 组 `/ops/operations/ip-group`；ROI `/ops/finance/roi-analysis`。

---

## 10. 构建与源码位置

| 部件 | 路径 |
|------|------|
| Ops 后端 | `football-backend-saas/football-module-ops/` |
| Ops 前端（集成） | `football-front/apps/web-ele/src/views/ops/` |
| Standalone UI（非签收默认） | `ops-platform-ui-vue/` |
| Greenfield SQL 生成 | `scripts/integration-config/gen-ops-greenfield-sql.py` |
| 本地联调启动 | `scripts/start-ops-dev.ps1` |

---

## 11. 部署架构摘要

| 环境 | 说明 |
|------|------|
| 生产 Greenfield | Football 全栈 + 新建/迁移 `shenyu-ops` + system 菜单 |
| 本地 Gate | MySQL 多库 + Nacos `local` + `start-ops-dev.ps1` |
| 访问入口 | football-front（示例 `http://localhost:5777`） |

**必填环境变量（ops-server 生产摘要）**：

| 变量 | 说明 |
|------|------|
| `NACOS_SERVER_ADDR` / `NACOS_PASSWORD` | 注册与配置 |
| `OPS_DB_*` | `shenyu-ops` 数据源 |
| `REDIS_HOST` / `REDIS_PASSWORD` | 缓存 |
| `OA_AES_KEY` | 敏感字段加解密 |
| `COLLECTOR_BASE_URL` / `COLLECTOR_API_TOKEN` | 统一采集 |
| `FOOTBALL_AI_SCHEME_GENERATE_API_KEY` | jingcai |
| `ADMIN_UI_URL` | 通知跳转基址 |

**健康检查**：`GET http://127.0.0.1:48094/actuator/health`（ops-server）。

**Phase 2 不在本期技术签收范围**：外部 SSO 独立登录页、M10 全量采集运维体系扩展（菜单与边界能力除外）。

---

## 12. 修订记录

| 日期 | 说明 |
|------|------|
| 2026-09-20 | 重写为自包含技术规格：架构、模块映射、API 摘要表、数据/安全/集成/部署摘要 |
