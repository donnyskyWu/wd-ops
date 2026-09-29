# -*- coding: utf-8 -*-
"""Assemble self-contained OPS product handover 需求/技术 Markdown from SSOT sources."""
from __future__ import annotations

import re
from datetime import date
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
HANDOVER = Path(__file__).resolve().parent
BUSINESS_PRD = REPO / "docs" / "product" / "PRD-业务版-v9.2.md"
DEV_PRD = REPO / "完整PRD-v9.2-开发版.md"

REQ_OUT = HANDOVER / "OPS产品交付-需求文档.md"
TECH_OUT = HANDOVER / "OPS产品交付-技术文档.md"

DELIVERY_DATE = "2026-09-20"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _strip_business_for_delivery(text: str) -> str:
    lines = text.splitlines()
    out: list[str] = []
    skip_block = False
    for i, line in enumerate(lines):
        if i == 0:
            continue  # replace title
        if line.startswith("> 本文档从业务视角"):
            skip_block = True
            continue
        if skip_block and line.strip() == "---":
            skip_block = False
            continue
        if skip_block:
            continue
        if line.startswith("*本文档由 `完整PRD"):
            break
        if line.startswith("**文档关系**："):
            break
        out.append(line)
    body = "\n".join(out).strip()
    body = re.sub(
        r"\*\*本期不覆盖\*\*（Phase 2）：\n- 外部 SSO 单点登录\n- 独立登录页改造\n- 部分 P1 优先级的增强功能（详见各模块优先级标注）",
        """**本期不覆盖**（Phase 2 / 非签收路径）：

| 能力 | 说明 |
|------|------|
| 外部 SSO / 钉钉 OAuth 独立登录 | Phase 2；本期通过 Football 管理端壳登录 |
| 独立 OPS 登录页（Standalone 非 Gate 默认路径） | 非生产签收路径，不作为客户验收主链路 |
| M10 全量采集调度运维与外部采集规模化 | Phase 2；菜单与配置边界存在，交付不承诺全量采集运维手册级能力 |
| 部分 P1 增强 | 以各模块功能表「优先级」列为准（如人效盘点增强、外部作品监测深度等） |

**本期交付验收侧重**：日常运营可脱离 Excel 主链路；内容 SOP→审核→发布可走通；ROI/成本可录入与查询；监测与报表可读已落库数据。""",
        body,
        count=1,
    )
    return body


def build_requirements_doc() -> str:
    business = _strip_business_for_delivery(_read(BUSINESS_PRD))

    header = f"""# OPS 产品交付 — 需求规格说明书

**交付包版本**：{DELIVERY_DATE}  
**产品版本**：PRD v9.2（2026-09-17）  
**适用产品**：运营数据分析平台（OPS），Football 管理端集成路径  
**文档性质**：客户交付用独立需求说明（正文自包含，不依赖仓库内其他 Markdown 即可阅读业务需求）  
**目标读者**：产品负责人、业务验收、项目经理、运营管理者  

**内容来源（编制依据）**：`PRD-业务版-v9.2.md`、`完整PRD-v9.2-开发版.md` 功能点与 M2 增量、`docs/product/PRD-M2-*` 增量规格。

---

"""

    appendix = """
---

## 12. 功能点索引（57 项 + 8 张报表）

下表与开发版 PRD §3A 一致，便于验收时按编号追溯。

### 12.1 编号体系

| 编号前缀 | 业务域 |
|----------|--------|
| `HOME-` | 首页 |
| `OPS-` | 运营管理 |
| `PROD-` | 内容生产 |
| `PERF-` | 绩效核算 |
| `INTERNAL-` | 账号管理 |
| `FINANCE-` | 财务管理 |
| `ANALYSIS-` | 数据分析 |
| `MONITOR-` | 作品监测 |
| `CONFIG-` | 配置管理 |
| `SYSTEM-` | 系统管理 |
| `COLLECT-` | 数据采集 |

### 12.2 功能点明细

#### 首页（1）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| HOME-001 | 首页仪表盘（含 IP 组筛选） | P0 | `/admin-api/oa/dashboard/home/*` |

#### 运营管理（7）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| OPS-001 | IP 组管理（大组/小组） | P0 | `/admin-api/oa/ip-group/*` |
| OPS-002 | 作者管理 | P0 | `/admin-api/oa/author/*` |
| OPS-003 | 账号分析 | P0 | `/admin-api/oa/account-analysis/*` |
| OPS-004 | 粉丝分析 | P0 | `/admin-api/oa/follower-analysis/*` |
| OPS-005 | 作品分析 | P0 | `/admin-api/oa/content-analysis/*` |
| OPS-006 | 内部内容分析（含补录） | P0 | `/admin-api/oa/internal-content/*` |
| OPS-007 | 人效盘点 | P1 | `/admin-api/oa/productivity-review/*` |

#### 内容生产（9）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| PROD-001 | SOP 管理 | P0 | `/admin-api/oa/sop/*` |
| PROD-002 | 计划管理（FR-M2-009） | P0 | `/admin-api/oa/plan/*` |
| PROD-003 | 任务管理 | P0 | `/admin-api/oa/task/*` |
| PROD-006 | 工作任务管理（FR-M2-010） | P0 | `/admin-api/oa/work-task/*` |
| PROD-004 | 内容管理（玩法 + AI + 审核 + 发布） | P0 | `/admin-api/oa/content/*`、`/admin-api/oa/ai-content/*` |
| PROD-007 | 公推模板库 | P0 | `/admin-api/oa/layout-template/*` |
| PROD-008 | AI 排版 / 一键排版（FR-M2-012） | P0 | `/admin-api/oa/content/typeset/*` |
| PROD-005 | 内容知识库 | P1 | `/admin-api/oa/knowledge/*` |

#### 绩效核算（4）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| PERF-001 | 考核模板 | P0 | `/admin-api/oa/perf-template/*` |
| PERF-002 | 考核执行 | P0 | `/admin-api/oa/perf-execute/*` |
| PERF-003 | 绩效结果 | P0 | `/admin-api/oa/perf-result/*` |
| PERF-004 | 订单归因分析 | P1 | `/admin-api/oa/order-attribution/*` |

#### 账号管理（7）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| INTERNAL-001 | 公司管理 | P0 | `/admin-api/oa/company/*` |
| INTERNAL-002 | 实名人管理 | P0 | `/admin-api/oa/real-person/*` |
| INTERNAL-003 | 手机管理 | P0 | `/admin-api/oa/phone/*` |
| INTERNAL-004 | 手机卡管理 | P0 | `/admin-api/oa/phone-card/*` |
| INTERNAL-005 | 平台账号管理 | P0 | `/admin-api/oa/platform-account/*` |
| INTERNAL-006 | 个人账号管理 | P0 | `/admin-api/oa/personal-account/*` |
| INTERNAL-007 | 三方关联统计 | P1 | `/admin-api/oa/tripartite-link/*` |

#### 财务管理（2）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| FINANCE-001 | 账号成本管理 | P0 | `/admin-api/oa/account-cost/*` |
| FINANCE-002 | ROI 分析 | P0 | `/admin-api/oa/roi/*` |

#### 数据分析（7 + 8 张报表）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| ANALYSIS-001 | 指标管理 / 指标分析 | P0 | `/admin-api/oa/metric/*` |
| ANALYSIS-002 | 数据报表（8 张标准报表） | P0 | `/admin-api/oa/report/*` |
| ANALYSIS-003 | 总体财务分析 | P1 | `/admin-api/oa/finance-overview/*` |
| ANALYSIS-004 | 漏斗分析 | P1 | `/admin-api/oa/funnel/*` |
| ANALYSIS-005 | 自定义查询 | P0 | `/admin-api/oa/custom-query/*` |
| ANALYSIS-006 | 数据大屏 / 大屏配置 | P0 | `/admin-api/oa/dashboard/*` |
| ANALYSIS-007 | 微信数据分析 | P1 | `/admin-api/oa/wx-data/*` |

标准报表：RPT-001 全平台账号视图 · RPT-002 账号状态监控 · RPT-003 短视频产出 · RPT-004 直播时长 · RPT-005 成本分摊 · RPT-006 ROI · RPT-007 IP 团队人员 · RPT-008 账号异常预警。

#### 作品监测（5）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| MONITOR-001 | 内部作品监控 | P1 | `/admin-api/oa/monitor/internal/*` |
| MONITOR-002 | 外部作品监控 | P1 | `/admin-api/oa/monitor/external/*` |
| MONITOR-003 | 爆款分析 | P0 | `/admin-api/oa/monitor/hit/*` |
| MONITOR-004 | 低分分析 | P0 | `/admin-api/oa/monitor/low-score/*` |
| MONITOR-005 | 高粉/低粉 / IP 主题 / 行业 | P1 | `/admin-api/oa/monitor/*` |

#### 配置管理（7）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| CONFIG-001 ~ 007 | 内外采集、外部数据源、订单采集、阈值、AI 模型/提示词 | P0/P1 | `/admin-api/oa/config/*` |

#### 系统管理（6）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| SYSTEM-001 ~ 006 | 用户、角色、租户、系统参数、字典、日志/消息 | P0/P1 | `/admin-api/oa/system/*`、`/admin-api/oa/dict/*` |

#### 数据采集（2）

| 编号 | 功能名称 | 优先级 | API 前缀 |
|------|----------|--------|----------|
| COLLECT-001 | 采集任务管理 | P0 | `/admin-api/oa/collect/task/*` |
| COLLECT-002 | 数据质量 | P0 | `/admin-api/oa/collect/quality/*` |

---

## 13. M2 v9.2 增量需求与验收标准（摘要）

### 13.1 计划管理（FR-M2-009 / PROD-002）

**描述**：业务计划关联 SOP、一个或多个 IP 组与外部赛事；草稿期任务不可见；启动后进入「我的任务」；多 IP 组按组并行生成任务。

| AC 编号 | 验收要点 |
|---------|----------|
| AC-M2-009-1 | 保存草稿后任务列表不可见该计划任务 |
| AC-M2-009-2 | 启动后任务可见且状态为待执行（PENDING） |
| AC-M2-009-3 | 终止须组长审批通过后计划与任务均为已终止 |
| AC-M2-009-4 | 步骤分配赛事后，任务关联赛事属于步骤赛事集合 |
| AC-M2-009-5 | 仅草稿计划可编辑名称/日期/赛事池/步骤 |
| AC-M2-009-6 | 一步骤 2 赛事 + 1 执行人 → 生成 2 条任务 |
| AC-M2-009-7 | 计划详情展示任务列表及计划起止时间 |
| AC-M2-009-8 | 多 IP 组启动后各组均生成对应任务 |

**与工作任务边界**：计划管理由运营管理者驱动完整 SOP DAG；工作任务由 IP 组长按日登记「作者 × 营销计划 × 多赛事」，确认即发布。

### 13.2 工作任务管理（FR-M2-010 / PROD-006）

| 子需求 | 要点 |
|--------|------|
| FR-M2-010-1 | 任务登记 Tab：默认 10 行；多赛事/作者/营销计划；DRAFT 保存不生成 task |
| FR-M2-010-2 | 矩阵 Tab：列头含作者、IP 组、组长 |
| FR-M2-010-3 | 撤回：sheet 回 DRAFT；关联 task 取消且列表不可见 |
| FR-M2-010-4 | 赛后红黑判定：定时任务 + AI 预测对比赛果 |
| FR-M2-010-5 | 确认后自动内容草稿 + 异步 jingcai 首写 |
| FR-M2-010-6 | 确认后提醒执行人（站内 + 钉钉） |

**任务生成规则**：不创建内容计划；按营销计划解析启用 SOP，每个 SOP 节点 1 条 task；执行人按节点岗位在 IP 组内解析；同一工作日内同一作者下赛事 ID 不可跨行重复。

**完成门禁**：非内容生成节点须填写工作说明；内容生成节点须关联内容审核通过后方可完成节点。

### 13.3 amphipoda 玩法对齐（FR-M2-011）

| 子需求 | 要点 |
|--------|------|
| FR-M2-011-1 | 玩法 UI：竞足/传足/北单/足球 Tab；N 场 + 玩法 |
| FR-M2-011-2 | 存储 `match_scheme_json` + `match_type` |
| FR-M2-011-3 | 保存时 Football 发布方案双写（失败不阻断 OPS 保存） |
| FR-M2-011-4 | AI 首生成/润色走 jingcai |
| FR-M2-011-5 | 任务预填 N 场赛程，不预填玩法 |

### 13.4 AI 排版与一键排版（FR-M2-012）

| 子需求 | 要点 |
|--------|------|
| FR-M2-012-1 ~ 2 | 模板引导 / 自动模式（LLM 语义分段） |
| FR-M2-012-3 | 正文保真：排版前后纯文本一致 |
| FR-M2-012-4 | 预览后确认应用 |
| FR-M2-012-7 | 一键排版四套 FOOTBALL_AI 预设（无 LLM） |

**双路径铁律**：文案 jingcai；排版 M8 LLM 或规则预设；二者不互相 fallback。

---

## 14. 核心业务规则索引（BR-001 ~ BR-024）

| 编号 | 名称 | 摘要 |
|------|------|------|
| BR-001 | ROI 计算 | ROI = 总营收 ÷ 总成本 |
| BR-002 | 粉丝 LTV | 平均（取消关注时间 − 关注时间） |
| BR-003 | 爆款判定 | 阅读/点赞/评论/转发同时 ≥ 阈值（AND） |
| BR-004 | 低分判定 | 四维同时 ≤ 阈值（AND） |
| BR-005 | 三级审核 | 初审→复审→终审；任一驳回须修改重提 |
| BR-006 | 数据权限 | 角色 + 部门 + IP 组 + 人员四级 |
| BR-007 | 大屏数据 | 进入加载，不自动轮询，可手动刷新 |
| BR-008 | SOP 并行 | 同并行组可同时推进；汇聚节点等待全部前置 |
| BR-009 | 审核状态机 | 驳回回到执行中重新提交 |
| BR-010 | 绩效自动算分 | 指标引擎 + 模板权重；支持人工微调 |
| BR-011 | 财务审计 | 成本修改记录操作人与时间 |
| BR-012 ~ BR-018 | 报表与预警 | 八张标准报表字段与账号异常预警规则 |
| BR-019 | IP 组层级 | 大组管小组；大组不直接管账号/主播 |
| BR-020 | 运营→主播 | 一对多关联，按 IP 组归类 |
| BR-021 | 成本明细 | 购买成本一次性；过程成本多条 |
| BR-022 | 公众号容量 | 剩余可注册低于阈值预警 |
| BR-023 | 三方关联 | 微信 + 视频号 + 企微绑定统计 |
| BR-024 | 自定义查询发布 | 管理员发布查询供他人直接使用 |

---

## 15. 非功能性需求

### 15.1 性能

| 指标 | 要求 |
|------|------|
| 页面加载 | 首屏 < 3s，后续交互 < 1s |
| API 响应 | P95 < 500ms |
| 大数据聚合 | 100 万条级聚合 < 5s |
| 导出 | 10 万行 Excel < 30s |

### 15.2 安全与合规

| 要求 | 说明 |
|------|------|
| 认证 | JWT Token（Football OAuth2） |
| 授权 | RBAC + 数据权限拦截 |
| 敏感数据 | 手机号等 AES-256（环境密钥 `OA_AES_KEY`） |
| 多租户 | 业务表 `tenant_id` 隔离 |
| 审计 | 关键操作日志 |

### 15.3 可用性

| 要求 | 说明 |
|------|------|
| 目标可用性 | ≥ 99.5%（KPI） |
| 缓存 | Redis 热点数据（集成栈） |
| 异步 | 耗时任务异步（AI 首写、导出等） |

---

## 16. 验收与测试分级

| 级别 | 含义 | 交付门禁 |
|------|------|----------|
| **P0 用例** | 阻塞上线的核心流程 | 必须 100% 通过 |
| **P1 用例** | 重要增强 | 按合同约定；默认可分期 |
| **模块 Checklist** | 开发自检项 | 模块交付前勾选完成 |
| **Gate 冒烟** | 跨模块主链路 | 首页、IP 组、内容、成本、报表可读 |

**集成验收环境（示例）**：Football 前端 `http://localhost:5777`；租户 `1`；本地启动脚本 `start-ops-dev.ps1`（详见同包《部署运维手册》）。

---

## 17. 关联交付物（同 ZIP 包内）

| 文档 | 说明 |
|------|------|
| OPS产品交付-技术文档 | 架构、API 摘要、集成与安全 |
| OPS产品交付-部署运维手册 | 安装、SQL 顺序、环境变量、健康检查 |
| product-manuals | 分角色操作手册 |
| delivery-screenshots | 界面截图 |

---

## 修订记录

| 日期 | 说明 |
|------|------|
| {DELIVERY_DATE} | 重写为自包含需求规格：合并业务版 v9.2 + 功能点索引 + M2 FR/AC + 非功能与验收 |
""".replace("{DELIVERY_DATE}", DELIVERY_DATE)

    # Renumber: business doc has sections 1-11; keep as-is, appendix is 12+
    return header + business + appendix


def build_tech_doc() -> str:
    return f"""# OPS 产品交付 — 技术规格说明书

**交付包版本**：{DELIVERY_DATE}  
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
| 响应 | `{{"code": 0, "msg": "success", "data": ...}}` |
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
| POST | `/admin-api/oa/plan/{{id}}/start` | 启动 |
| POST | `/admin-api/oa/plan/{{id}}/terminate` | 申请终止 |
| GET | `/admin-api/oa/plan/{{id}}/tasks` | 计划任务明细 |

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
| POST | `/admin-api/oa/task/{{id}}/execute/complete` | 节点完成（含门禁） |
| GET | `/admin-api/oa/content/list` | 内容列表 |
| POST | `/admin-api/oa/content/create` | 创建内容 |
| POST | `/admin-api/oa/content/{{id}}/submit-review` | 提交审核 |
| POST | `/admin-api/oa/content/{{id}}/review` | 审核操作 |
| POST | `/admin-api/oa/content/{{id}}/publish` | 发布 |
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
| GET | `/admin-api/oa/report/{{code}}/data` | 标准报表数据 |
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
| {DELIVERY_DATE} | 重写为自包含技术规格：架构、模块映射、API 摘要表、数据/安全/集成/部署摘要 |
"""


def main() -> None:
    if not BUSINESS_PRD.is_file():
        raise FileNotFoundError(BUSINESS_PRD)
    req = build_requirements_doc()
    tech = build_tech_doc()
    REQ_OUT.write_text(req, encoding="utf-8", newline="\n")
    TECH_OUT.write_text(tech, encoding="utf-8", newline="\n")
    print(f"Wrote {REQ_OUT} ({len(req.splitlines())} lines)")
    print(f"Wrote {TECH_OUT} ({len(tech.splitlines())} lines)")

    deploy_script = HANDOVER / "_assemble_standalone_deploy.py"
    if deploy_script.is_file():
        import subprocess
        import sys

        subprocess.run([sys.executable, str(deploy_script)], check=True)


if __name__ == "__main__":
    main()
