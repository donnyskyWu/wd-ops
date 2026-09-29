# -*- coding: utf-8 -*-
"""Assemble self-contained OPS product handover 部署运维 Markdown from SSOT sources."""
from __future__ import annotations

from datetime import date
from pathlib import Path

HANDOVER = Path(__file__).resolve().parent
REPO = HANDOVER.parents[2]

DEPLOY_OUT = HANDOVER / "OPS产品交付-部署运维手册.md"
DELIVERY_DATE = "2026-09-20"

SOURCES = [
    "docs/delivery/product-handover/OPS产品交付-部署运维手册.md（上一版摘要）",
    "docs/delivery/product-handover/README.md（部署与 Zip 章节）",
    "docs/deploy/ops-greenfield-production/OPERATIONS-GUIDE.md",
    "docs/deploy/ops-greenfield-production/config/env-variables.md",
    "docs/deploy/ops-greenfield-production/config/gateway-ops-routes.md",
    "docs/deploy/ops-greenfield-production/rollback.md",
    "docs/delivery/OPS-DEV-DEPLOY-GUIDE.md",
    "BOOT-INF/classes/application.yaml",
    "BOOT-INF/classes/application-local.yaml",
    "BOOT-INF/classes/application-prod.yaml",
    "scripts/start-ops-dev.ps1",
    "scripts/integration-config/ops-test-remote.env.example",
    "scripts/integration-config/nacos-ops-server-prod.yaml.example",
    "docs/delivery/PLAN-ops-list-pagination.md（运维 FAQ 摘要）",
]


def build_deploy_doc() -> str:
    return f"""# OPS 产品交付 — 部署运维手册

**交付包版本**：{DELIVERY_DATE}  
**产品版本**：PRD v9.2  
**文档性质**：客户交付用独立部署运维说明（正文自包含，不依赖仓库内其他 Markdown 即可执行安装与日常运维）  
**目标读者**：DBA、DevOps、集成工程师、客户侧运维  

**线上环境参考**：Football 管理端 [`https://saas.shenyu.com`](https://saas.shenyu.com)（Hash 路由 `#/ops/...`）；本地 Gate 联调 `http://localhost:5777`。  
**生产签收路径**：浏览器 → **football-front** → **football-gateway** → Nacos 发现 → **ops-server**（非 Standalone `:3000/:8080`）。

**编制依据（仓库 SSOT，已内联至本文）**：Greenfield `OPERATIONS-GUIDE`、环境变量表、`OPS-DEV-DEPLOY-GUIDE`、集成启动脚本与 profile 配置摘要。

---

## 目录

1. [环境要求](#1-环境要求)
2. [架构与组件](#2-架构与组件)
3. [部署形态一览](#3-部署形态一览)
4. [配置项总表](#4-配置项总表)
5. [Spring Profile 与 Nacos](#5-spring-profile-与-nacos)
6. [数据库初始化（Greenfield）](#6-数据库初始化greenfield)
7. [启动顺序与脚本](#7-启动顺序与脚本)
8. [前后端部署（生产）](#8-前后端部署生产)
9. [Collector 与 AI 联调](#9-collector-与-ai-联调)
10. [监控、日志与健康检查](#10-监控日志与健康检查)
11. [备份与回滚](#11-备份与回滚)
12. [常见问题（运维 FAQ）](#12-常见问题运维-faq)
13. [安全检查清单](#13-安全检查清单)
14. [本期范围外（Phase 2）](#14-本期范围外phase-2)
15. [修订记录](#15-修订记录)

---

## 1. 环境要求

### 1.1 软件与版本

| 组件 | 版本建议 | 用途 |
|------|----------|------|
| JDK | 17+ | ops-server、Football 微服务 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | football-front |
| pnpm | 最新稳定 | football-front monorepo |
| MySQL | 8.x | `shenyu-ops`、`shenyu-system` 及 Football 业务库 |
| Redis | 6.x/7.x | OAuth2 Token、会话与缓存 |
| Nacos | 2.3.x（示例 v2.3.2） | 服务注册与配置中心 |
| Docker（推荐） | 可用 CLI | 本地 Nacos/Redis 容器 |
| Python | 3.11+（可选） | SQL 生成、菜单 seed、Collector API |

### 1.2 交付制品

| 制品 | 说明 |
|------|------|
| `football-module-ops-server.jar` | Ops 业务服务；Nacos 注册名 **`ops-server`**，默认端口 **48094** |
| football-front 构建产物 | 含 `/ops/**` 路由的 Admin UI（与 ops-server **同 release tag**） |
| Greenfield SQL 三包 | `01-shenyu-ops-schema.sql` → `02-shenyu-system-menus.sql` → `03-shenyu-ops-seeds.sql`（与 JAR 同版本打包） |

### 1.3 网络与依赖前提（生产 Greenfield）

| 项 | 要求 |
|----|------|
| MySQL | 可建库 `shenyu-ops`；对 `shenyu-system`（或环境等价库）有菜单/字典写入权限 |
| 基础设施 | Nacos、Redis 已就绪；**system-server**、**member-server**（私域报表）、**match-server**（赛事，可选）已在同一 Nacos namespace 注册 |
| Gateway | 已配置 `/admin-api/ops/**` → `grayLb://ops-server` |
| 密钥 | 运维侧准备 `OA_AES_KEY`（与 Football 生产一致）、采集与 AI 相关密钥（见 §4） |

---

## 2. 架构与组件

### 2.1 生产目标拓扑

```text
用户浏览器
  → CDN / Nginx（football-front 静态资源）
  → football-gateway (:48080, 前缀 /admin-api)
  → Nacos（grayLb 服务发现）
  → system-server (:48081) / ops-server (:48094) / member-server / mp-server / …
  → MySQL（shenyu-ops + Football 多库）
  → Redis（Token + 缓存）
  → unify-collector-api（可选，M10/M8 采集执行）
  → 外部 jingcai.article（文案 AI）、M8 配置的 LLM（排版语义，可选）
```

### 2.2 逻辑组件职责

| 组件 | 职责 |
|------|------|
| **football-front** | Football 管理端壳；Ops 页面位于 `views/ops`，路由 `/ops/**` |
| **football-gateway** | 统一 API 入口；OAuth2 Bearer；Ops 路径 **`/admin-api/ops/**`** |
| **ops-server** | Ops 业务 API；多数据源（master→shenyu-ops 等）；Flyway（profile 控制） |
| **shenyu-system** | 用户/角色/菜单/字典 SSOT（ADR-056）；Ops 菜单 id **6100–6999**，权限前缀 **`ops:*`** |
| **unify-collector** | 统一采集 HTTP 服务；ops-server 通过 `COLLECTOR_BASE_URL` 调用 |
| **XXL-JOB**（按 profile） | 工作任务赛后预测等定时任务；executor 名 **`football-ops-executor`** |

### 2.3 本地集成栈（Gate 路径，开发与验收）

| 服务 | 端口 | 说明 |
|------|------|------|
| football-front | **5777** | 日常 UI 入口 |
| football-gateway | **48080** | `/admin-api` |
| Nacos | **8848** | namespace **`local`** |
| Redis | **6379** | 本地集成密码 **123456** |
| MySQL | **3306** | 五库：shenyu-ops / shenyu-member / shenyu-mp / shenyu-pay / shenyu-system |
| ops-server | **48094** | profile **`local`** 或 **`dev-test-beta`** |
| system-server | 48081 | |
| mp-server | 48086 | |
| member-server | 48087 | 默认真实 JAR（方案列表）；可用 mock 仅做登录冒烟 |

### 2.4 鉴权与 Football SSO

| 项 | 说明 |
|----|------|
| 登录 | **Football OAuth2**；生产用户访问 `https://saas.shenyu.com` 管理端登录后进入 `#/ops/...` |
| Token | 存 Redis；Gateway 校验 Bearer；**Gate 路径禁止使用 dev-token 调 Gateway** |
| 用户 ID SSOT | `shenyu-system.system_users.id`（写入 Ops 外键须走 Football 用户校验） |
| 租户 | 请求头/上下文带 tenantId；业务表 `tenant_id` 隔离 |
| Phase 2 | **外部 SSO / 独立 OPS 登录页** 不在本期签收范围 |

---

## 3. 部署形态一览

| 形态 | 适用场景 | 入口 / Profile |
|------|----------|----------------|
| **生产 Greenfield** | 新建 `shenyu-ops` + system 菜单 | `--spring.profiles.active=prod` + Nacos `prod` namespace |
| **本地 Gate** | 开发、Gate 冒烟、客户 POC | `.\\scripts\\start-ops-dev.ps1`；profile **`local`** |
| **Beta 远程 DB** | 连测试机 MySQL/Redis，Nacos 仍本地 | `start-ops-dev.ps1 -Beta` + `ops-test-remote.env`；profile **`dev-test-beta`** |
| **Standalone** | 快速改 Ops（**非生产签收**） | `start-ops-standalone.ps1`；`:8080` / `:3000` |
| **Standalone + Collector** | M10 真实采集联调 | `restart-all.ps1`；Collector 默认 `:8000` |

**本地登录（Gate）**：`http://localhost:5777` · 账号 `admin` / `admin123` · 租户 **1**（仅联调环境；生产勿使用默认口令）。

---

## 4. 配置项总表

### 4.1 ops-server 必填（生产 profile `prod`）

| 环境变量 | 占位示例 | 说明 |
|----------|----------|------|
| `NACOS_SERVER_ADDR` | `nacos.prod.internal:8848` | Nacos 地址 |
| `NACOS_PASSWORD` | `***` | Nacos 认证 |
| `NACOS_USERNAME` | `nacos` | 可选，默认 nacos |
| `NACOS_NAMESPACE` | `prod` | 发现与配置 namespace |
| `OPS_DB_HOST` | `mysql.prod.internal` | shenyu-ops 主机 |
| `OPS_DB_PORT` | `3306` | |
| `OPS_DB_NAME` | `shenyu-ops` | |
| `OPS_DB_USER` | `shenyu-ops` | |
| `OPS_DB_PASSWORD` | `***` | |
| `REDIS_HOST` | `redis.prod.internal` | 与 Football 共用 |
| `REDIS_PASSWORD` | `***` | |
| `REDIS_PORT` | `6379` | 可选 |
| `REDIS_DATABASE` | `0` | Beta 联调常用 `1`，生产以运维为准 |
| `OA_AES_KEY` | Base64 32 字节 | 敏感字段 AES-256；**勿随意轮换** |
| `COLLECTOR_BASE_URL` | `https://collector.example.com/` | 统一采集基址 |
| `COLLECTOR_API_TOKEN` | `***` | 与 unify-collector 的 `API_TOKEN` 一致 |
| `FOOTBALL_AI_SCHEME_GENERATE_API_KEY` | `***` | jingcai 文案；别名 `AUTHOR_AI_SCHEME_API_KEY` |
| `ADMIN_UI_URL` | `https://saas.shenyu.com` 或客户 Admin 域名 | 通知跳转基址 |

### 4.2 常用可选

| 环境变量 | 默认 / 说明 |
|----------|-------------|
| `FLYWAY_ENABLED` | `true`；DBA 全手工灌库且 history 已对齐时可 `false` |
| `FOOTBALL_AI_SCHEME_GENERATE_URL` | `http://ai.author.shenyu.com/api/v1/tasks` | jingcai POST |
| `FOOTBALL_AI_SCHEME_GET_URL` | 同上前缀 | jingcai GET 轮询 |
| `FOOTBALL_AI_MODEL` | `deepseek-v4-flash` | |
| `OA_MATCH_INTERNAL_BASE_URL` | 赛事 proxy 基址 | 计划/任务选赛 |
| `XXL_JOB_ENABLED` | `true` | 本地 profile 常 `false` |
| `XXL_JOB_ADMIN_ADDRESSES` | 须覆盖生产调度中心地址 | ADR-070 |
| `XXL_JOB_ACCESS_TOKEN` | 与 xxl-job-admin 一致 | |

### 4.3 DBA 脚本占位符（非 JVM 环境变量）

| 占位符 | 说明 |
|--------|------|
| `{{OPS_DB_NAME}}` / `{{OPS_DB_USER}}` / `{{OPS_DB_PASSWORD}}` | 建库与授权 |
| `{{SYSTEM_DB_HOST}}` / `{{SYSTEM_DB_NAME}}` | 执行 `02-shenyu-system-menus.sql` 时替换 |
| `{{WORK_TASK_DEFAULT_TEMPLATE_ID}}` | 对照本库 `oa_sop_template.id`（示例 **9402**） |
| `{{WORK_TASK_DEFAULT_NODE_ID}}` | `oa_sop_node` 且 `node_type=CONTENT_GENERATION`（示例 **9404**） |

### 4.4 生产 systemd / Docker 环境块示例

```bash
export SPRING_PROFILES_ACTIVE=prod
export NACOS_SERVER_ADDR=nacos.prod.internal:8848
export NACOS_PASSWORD=***
export OPS_DB_HOST=mysql.prod.internal
export OPS_DB_NAME=shenyu-ops
export OPS_DB_USER=shenyu-ops
export OPS_DB_PASSWORD=***
export REDIS_HOST=redis.prod.internal
export REDIS_PASSWORD=***
export OA_AES_KEY=***
export COLLECTOR_BASE_URL=https://collector.example.com/
export COLLECTOR_API_TOKEN=***
export FOOTBALL_AI_SCHEME_GENERATE_API_KEY=***
export ADMIN_UI_URL=https://saas.shenyu.com
export XXL_JOB_ENABLED=true
export XXL_JOB_ADMIN_ADDRESSES=http://xxl-job.prod.internal:9090/xxl-job-admin
export XXL_JOB_ACCESS_TOKEN=***
```

---

## 5. Spring Profile 与 Nacos

### 5.1 配置分层（ops-server）

| 层级 | 文件 / 来源 | 作用 |
|------|-------------|------|
| 公共 | `application.yaml` | 服务名 `ops-server`、端口 48094、Flyway 默认、`oa.*` / `football.ai.*` 结构 |
| Profile Bootstrap | `application-{{profile}}.yaml`（JAR 内） | Nacos 连接、数据源、Redis、Flyway 开关 |
| 环境 SSOT | Nacos DataId **`ops-server-{{profile}}.yaml`** | Beta/Prod 动态配置（DB/Redis/鉴权覆盖） |

**JAR 内 profile 文件命名**：`local`、`dev-test-beta`、`prod`（与 `--spring.profiles.active` 一致）。

### 5.2 Profile 摘要（无密钥）

| Profile | Nacos config | 数据源 | 典型场景 |
|---------|--------------|--------|----------|
| **`local`** | **关闭**（`spring.cloud.nacos.config.enabled=false`） | localhost `shenyu-ops` root/root | `start-ops-dev.ps1` 默认 |
| **`dev-test-beta`** | 关闭或 overlay | `ops-test-remote.env` 远程五库 | `-Beta` 远程 DB |
| **`prod`** | **开启**；namespace `${{NACOS_NAMESPACE:prod}}` | 全部由 `OPS_DB_*` / `REDIS_*` 注入 | 生产 |
| 默认 active | JAR 内 `spring.profiles.active` 可能为 prod | 启动时务必显式指定 profile | |

**生产硬开关**：`oa.auth.dev-token.enabled=false`；Gateway 登录用户桩 `gateway-login-user.enabled=true`；Redis OAuth2 `football-redis.enabled=true`。

### 5.3 Nacos 注册要求

| 服务 | namespace | 备注 |
|------|-----------|------|
| `ops-server` | 与 Gateway 相同（如 `prod`） | 本手册主服务 |
| `system-server` | 同 namespace | 字典/权限 Feign |
| `member-server` | 同 namespace | 私域报表 MVP |
| `match-server` | 同 namespace | 赛事（可配 `OA_MATCH_INTERNAL_BASE_URL`） |

### 5.4 Gateway 路由（生产终态）

| 属性 | 值 |
|------|-----|
| 路径 | `/admin-api/ops/**` |
| 目标 | `grayLb://ops-server` |
| 端口 | 48094 |
| 超时 | 建议 **300s**（AI 长请求） |

> 历史前缀 `/admin-api/oa/**` Rewrite 已废除；前端与 OpenAPI 以 **`/admin-api/ops/**`** 为准。

---

## 6. 数据库初始化（Greenfield）

**原则**：同一 release tag 下，先 DBA 三包 SQL，再部署同 tag 的 `football-module-ops-server.jar`。JAR 首次启动 Flyway **仅补 Java migration V113**（若 history 已灌满则跳过其余）。

**SQL 文件命名**（随交付包或仓库 `docs/deploy/ops-greenfield-production/sql/`）：

| 顺序 | 文件名 | 目标库 | 内容 |
|------|--------|--------|------|
| 0 | 建库语句（见下） | MySQL | `CREATE DATABASE shenyu-ops` |
| 1 | `01-shenyu-ops-schema.sql` | **shenyu-ops** | Flyway V1→V191 溯源 + history（186 条 SQL migration） |
| 2 | `02-shenyu-system-menus.sql` | **shenyu-system** | 菜单 6100+、字典 baseline、六业务角色 RBAC |
| 2b（可选） | `02a` / `02b` 分文件 | system | 按交付包说明拆分字典/角色时 |
| 3 | `03-shenyu-ops-seeds.sql` | **shenyu-ops** | AI 模型目录、提示词、sys_param；**须先替换工作任务 SOP 占位符** |
| 验收 | `verify-schema.sql` | shenyu-ops | 表与 Flyway 负检 |

### 6.1 Step 1 — 建库

```sql
CREATE DATABASE IF NOT EXISTS `shenyu-ops`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'shenyu-ops'@'%' IDENTIFIED BY '{{OPS_DB_PASSWORD}}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
  ON `shenyu-ops`.* TO 'shenyu-ops'@'%';
FLUSH PRIVILEGES;
```

### 6.2 Step 2 — Ops schema

```bash
mysql -h {{OPS_DB_HOST}} -u {{OPS_DB_USER}} -p shenyu-ops < 01-shenyu-ops-schema.sql
mysql -h {{OPS_DB_HOST}} -u {{OPS_DB_USER}} -p shenyu-ops < verify-schema.sql
```

**注意**：

- `01` 必须在**空库**执行（仅建库后无表）。若部分失败导致脏库 → `DROP DATABASE` 后重跑。
- `shenyu-ops` **不含** Football `system_*` 表；用户/字典运行时读 **shenyu-system**（Feign）。
- **V113** 为 Java migration，DBA **不手工执行**；由 JAR 启动补跑。

**验收 SQL**：

```sql
SELECT COUNT(*) FROM flyway_schema_history WHERE type='SQL' AND success=1;
-- 期望 186（JAR 补 V113 后 187）
```

### 6.3 Step 3 — System 菜单 / 字典

执行前确认 `system_menu` 含 **`user_type`** 列（Football schema SSOT）。

```bash
mysql -h {{SYSTEM_DB_HOST}} -u {{SYSTEM_DB_USER}} -p shenyu-system < 02-shenyu-system-menus.sql
```

**验收**：

```sql
SELECT id, name FROM system_menu WHERE id = 6100 AND deleted = b'0';
SELECT permission FROM system_menu WHERE permission LIKE 'ops:work-task:%' AND deleted = b'0';
SELECT code FROM system_role WHERE code IN ('ip_group_leader','ops_manager','finance',
  'content_editor','data_analyst','collect_operator') AND deleted = b'0';
```

**本地开发**：若使用 overlay 库 `shenyu-system` 缺 `user_type`，改对 **`shenyu-sys`** 或测试 SSOT 库执行 `02`；或仅补工作任务菜单脚本 `fix_local_work_task_menu.sql`。

**本地菜单 seed（幂等）**：

```powershell
python scripts/integration-config/apply-seed-oa-menu.py `
  --host localhost --port 3306 --user root --password root `
  --database shenyu-system
```

（禁止 PowerShell 管道导入 SQL，避免中文乱码。）

### 6.4 Step 4 — Ops 种子

1. 查询 SOP ID：

```sql
SELECT id, name FROM oa_sop_template WHERE deleted=0 AND tenant_id=1;
SELECT n.id FROM oa_sop_node n
WHERE n.node_type='CONTENT_GENERATION' AND n.deleted=0 AND n.tenant_id=1;
```

2. 编辑 `03-shenyu-ops-seeds.sql` 替换 `{{WORK_TASK_DEFAULT_TEMPLATE_ID}}` / `{{WORK_TASK_DEFAULT_NODE_ID}}`。

3. 执行：

```bash
mysql -h {{OPS_DB_HOST}} -u {{OPS_DB_USER}} -p shenyu-ops < 03-shenyu-ops-seeds.sql
```

**部署后在 Admin 补全（禁止写入 SQL 的密钥）**：M8 AI 模型 API Key 与连通状态；钉钉 AppKey/Secret；`notification.platform-base-url`；外部公众号 Cookie（若启用采集）。

### 6.5 增量升级（已有 Ops 库）

| 场景 | 动作 |
|------|------|
| 新版本 JAR | `FLYWAY_ENABLED=true` 启动，Flyway 补 V190/V191 等 |
| 仅缺字典/种子 | 幂等重跑 `02` + `03`，**勿**对已有 v191 库重跑 `01` |
| 手工增量 SQL | `prod-incremental-shenyu-ops-V192-V196.sql` 等（按交付包 `sql/README-prod-incremental-*.md` 说明） |
| Legacy sys_* 清理 | `scripts/integration-config/drop-ops-legacy-sys-tables.sql` |

### 6.6 再生成 SQL（需 Flyway 源码目录）

```bash
python scripts/integration-config/gen-ops-greenfield-sql.py
python scripts/integration-config/gen-ops-flyway-history.py
```

### 6.7 本地 Flyway 修复（非生产）

| 脚本 | 用途 |
|------|------|
| `repair-flyway-local-validate.sql` | description 空格与 history 不一致 |
| `repair-flyway-checksums-local.sql` | checksum 与 JAR 不一致 |

---

## 7. 启动顺序与脚本

### 7.1 生产推荐顺序

1. MySQL / Redis / Nacos 就绪  
2. DBA 完成 §6（或确认增量 Flyway 策略）  
3. 推送 Nacos `ops-server-prod.yaml`（与环境变量一致）  
4. 启动 **system-server**、**member-server** 等 Football 基座  
5. 启动 **ops-server**（prod profile + 环境变量）  
6. 确认 Nacos 中 `ops-server` 健康  
7. 启动/ reload **football-gateway**（路由 `/admin-api/ops/**`）  
8. 部署 **football-front** 静态资源（同 tag）  
9. 注册 **XXL-JOB** 任务（若启用）  
10. §10 健康检查与 P0 冒烟  

### 7.2 本地一键（Gate）

```powershell
# 仓库根目录
.\\scripts\\start-ops-dev.ps1              # 日常：重启栈，跳过 Maven
.\\scripts\\start-ops-dev.ps1 -FirstRun     # 首次或大改：含 Maven 构建
.\\scripts\\start-ops-dev.ps1 -Beta         # 远程测试 DB + 本地 Nacos
.\\scripts\\start-ops-dev.ps1 -UseMemberMock  # 仅登录冒烟（无方案列表）
```

停止：`.\\scripts\\stop-integration-all.ps1`（默认**不**停止 Redis :6379，避免 Windows 服务丢密码）。

### 7.3 仅 ops-server（集成）

```powershell
.\\scripts\\start-integration-oa.ps1
```

### 7.4 Standalone（非签收）

```powershell
.\\scripts\\start-ops-standalone.ps1
```

### 7.5 生产 JAR 启动示例

```bash
java -Xms512m -Xmx2048m -jar football-module-ops-server.jar \\
  --spring.profiles.active=prod
```

（环境变量见 §4；可选 `--spring.config.additional-location=file:/opt/ops/config/nacos-ops-server-prod.yaml`。）

### 7.6 Beta 远程环境文件

复制 `scripts/integration-config/ops-test-remote.env.example` → `ops-test-remote.env`（**勿提交真实密码**），填写：

| 变量族 | 说明 |
|--------|------|
| `OPS_TEST_DB_HOST` / `OPS_TEST_*_DB` | 远程 MySQL 五库 + match/bpm |
| `OPS_TEST_REDIS_*` | 远程 Redis |
| `OPS_TEST_NACOS_*` | 测试 Nacos（Beta 脚本仍可能用本地 :8848，以脚本输出为准） |
| `COLLECTOR_BASE_URL` / `COLLECTOR_API_TOKEN` | 远程 unify-collector |

---

## 8. 前后端部署（生产）

### 8.1 构建 ops-server

```powershell
cd football-backend-saas
mvn -pl football-module-ops/football-module-ops-server -am package -DskipTests
# 产物：football-module-ops/football-module-ops-server/target/football-module-ops-server.jar
```

### 8.2 构建 football-front

按 Football 项目标准流程构建 Admin UI，确保包含 `views/ops` 路由；静态资源部署至 Nginx/CDN，**与 ops-server 同版本 tag**。

### 8.3 P0 路由冒烟（示例）

| 检查 | 路径 |
|------|------|
| 工作任务 | `ops/production/work-task/index` |
| 数据报表 | `ops/analysis/report/*` |
| IP 组 | `ops/operations/ip-group` |

### 8.4 API 冒烟（需 Bearer Token）

```bash
curl -H "Authorization: Bearer {{TOKEN}}" \\
  "https://{{GATEWAY_HOST}}/admin-api/ops/ip-group/list"
curl -H "Authorization: Bearer {{TOKEN}}" \\
  "https://{{GATEWAY_HOST}}/admin-api/ops/work-task/sheet/get-or-create?ipGroupId={{IP_GROUP_ID}}&workDate=2026-08-25"
```

私域报表（member 就绪时）：

```bash
curl -H "Authorization: Bearer {{TOKEN}}" \\
  "https://{{GATEWAY_HOST}}/admin-api/ops/private-domain-report/authors"
```

### 8.5 XXL-JOB（工作任务预测）

1. Executor **`football-ops-executor`** 在线  
2. Job **`workTaskWinPredictionJobHandler`**，Cron `0 0 * * * ?`  
3. 手动触发一次 SUCCESS  

---

## 9. Collector 与 AI 联调

### 9.1 统一采集（Collector）

| 项 | 说明 |
|----|------|
| 配置 | `COLLECTOR_BASE_URL`、`COLLECTOR_API_TOKEN` 与 unify-collector **`API_TOKEN`** 一致 |
| 本地 | 默认 stub 或不可达时列表为空；远程联调在 `ops-test-remote.env` 配置 URL |
| 绑定 | M4 **平台账号 · 采集 Tab** 维护凭证；`POST .../collector-bind/test-connection` 应到达 collector（业务 DISCONNECTED 与 HTTP 401 区分） |
| 抖音等平台 | 采集失败时查账号绑定、collector 日志、平台会话是否过期；业务错误码以 collector JSON **`code`/`message`** 为准 |

### 9.2 jingcai 文案 AI

| 项 | 说明 |
|----|------|
| 变量 | `FOOTBALL_AI_SCHEME_GENERATE_API_KEY`、`FOOTBALL_AI_SCHEME_GENERATE_URL`、`FOOTBALL_AI_MODEL` |
| 现象 | HTTP **401** / 任务失败 → 检查 API Key、URL 可达、模型 slug |
| 职责 | 内容首写/润色；**不**承担排版 |

### 9.3 M8 LLM 排版

| 项 | 说明 |
|----|------|
| 配置 | 系统参数 + M8「AI 模型」连通；与 jingcai **分离** |
| 401 / 未连通 | Admin 补 API Key，`conn_status=CONNECTED` |
| 降级 | **FOOTBALL_AI 一键排版**（四套预设）不调用 LLM，可用于无 LLM 环境验收 |

### 9.4 双路径铁律

文案走 jingcai；排版走 M8 LLM 或 FOOTBALL_AI 规则预设；**互不 fallback**。

---

## 10. 监控、日志与健康检查

### 10.1 Actuator 健康 URL

| 服务 | URL（示例） |
|------|-------------|
| **ops-server** | `GET http://127.0.0.1:48094/actuator/health` → UP |
| system-server | `GET http://127.0.0.1:48081/actuator/health` |
| mp-server | `GET http://127.0.0.1:48086/actuator/health` |
| member-server | `GET http://127.0.0.1:48087/actuator/health` |
| Standalone 后端 | `GET http://localhost:8080/actuator/health` |
| Gateway | `GET http://localhost:48080/admin-api/system/tenant/simple-list`（401 亦表示网关存活） |

### 10.2 本地集成日志路径

| 文件 | 说明 |
|------|------|
| `scripts/logs/gateway-integration.log` | Gateway |
| `scripts/logs/ops-server-nacos-run.log` | ops-server |
| `scripts/logs/football-front-dev.log` | 前端 |
| Standalone | `scripts/logs/backend-dev-run.log`、`frontend-dev-run.log` |

生产日志：以 `logback-spring.xml` 与运维平台（ELK 等）为准。

### 10.3 业务冒烟（UI）

1. 登录 Admin，侧栏可见 **「运营数据」**（menu 6100）  
2. 打开 IP 组、工作任务、内容列表无 500  
3. 带 Token 调用 `/admin-api/ops/...` 返回 `code=0`  

---

## 11. 备份与回滚

### 11.1 备份建议

| 对象 | 建议 |
|------|------|
| `shenyu-ops` | 投产前全量；投产后定期 mysqldump + binlog |
| `shenyu-system` | 菜单/角色变更前备份；与 Ops 菜单 id 6100+ 相关 |
| 配置 | Nacos 导出 `ops-server-prod.yaml`；环境变量清单离线保管 |
| 制品 | JAR + SQL 三包 + front 构建号归档 |

### 11.2 Greenfield 回滚（投产前无业务数据）

```sql
DROP DATABASE IF EXISTS `shenyu-ops`;
-- system 侧软删菜单 6100–6199 或工作任务权限（见下）
```

### 11.3 投产后优先 forward-fix

勿轻易 DROP 业务表。应用仅回滚 JAR 时，若 Flyway 已执行 V181+，须同步 `flyway_schema_history`，避免版本漂移。

**种子回滚示例**：

```sql
UPDATE oa_ai_prompt_config SET deleted=1 WHERE scene='WORK_TASK_WIN_PREDICTION';
UPDATE sys_param SET deleted=1 WHERE param_key LIKE 'work_task.%';
```

**system 菜单软删**：

```sql
UPDATE system_menu SET deleted=b'1', updater='rollback-v183'
WHERE permission LIKE 'ops:work-task:%' AND deleted=b'0';
UPDATE system_menu SET deleted=b'1' WHERE id BETWEEN 6100 AND 6199;
```

**XXL-JOB**：admin UI 停止 `workTaskWinPredictionJobHandler`；或配置 `oa.work-task.win-prediction.enabled: false`。

---

## 12. 常见问题（运维 FAQ）

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| UI「内部服务错误」/ 登录失败 | Gateway **48080 DOWN** 或 Redis 无密码 | 运行 `start-ops-dev.ps1`；Redis 须 **123456**；查 `gateway-integration.log` |
| 登录后无「运营数据」 | `shenyu-system` 未灌 Ops 菜单 | `apply-seed-oa-menu.py`；重登 |
| API **401** | Token 无效、Redis 无 OAuth2 键、误用 dev-token 调 Gateway | Football 登录拿 Bearer；检查 Redis 与 system-server |
| FlywayValidateException | checksum/description 与 JAR 不一致 | §6.7 repair 脚本或 `flyway repair` |
| ops-server 500 | 五库缺失、profile 错误 | 建库；确认 `dev-local-multidb` / prod 数据源 |
| **AI 401** / 文案失败 | jingcai Key/URL 错误 | §9.2 环境变量；系统参数勿覆盖为空 |
| **AI 排版**无响应 | M8 模型未连通 | Admin 配置 API Key；或改用 FOOTBALL_AI 一键排版 |
| Collector **401** | `COLLECTOR_API_TOKEN` 不一致 | 对齐 unify-collector 与 ops-server |
| Collector 抖音/平台失败 | 账号未绑定、会话过期、collector 业务拒单 | M4 采集 Tab 更新凭证；看 collector 响应 **code**（如平台侧拒采） |
| 列表翻页无效 / 只见前 20 条 | 前端 Pagination 未绑 `@change` 或后端未分页 | 2026-09 已治理多数 Ops 列表；仍异常时查浏览器 Network 的 `pageNo`/`pageSize` |
| 私域报表空 | member-server 未注册 | Nacos 检查 `member-server`；§8.4 API |
| Maven PowerShell 拆参 | `-D` 未加引号 | `"-Dspring-boot.run.profiles=dev"` |
| 菜单中文 `????` | SQL 编码破坏 | 仅用 `apply-seed-oa-menu.py` utf8mb4 |

---

## 13. 安全检查清单

| # | 项 | 生产要求 |
|---|-----|----------|
| 1 | `oa.auth.dev-token.enabled` | **false** |
| 2 | `OA_AES_KEY` | 与 Football 一致；限制运维可见范围 |
| 3 | DB/Redis/Nacos/Collector/AI 密钥 | 仅环境变量或密钥管理；**勿入库、勿进 Git** |
| 4 | Gateway HTTPS | 对外 TLS；Admin UI 域名与 `ADMIN_UI_URL` 一致 |
| 5 | `@PreAuthorize` / 数据权限 | 验收抽查高危 API |
| 6 | 多租户 | 请求带正确 tenantId；抽查 1504 |
| 7 | XXL_JOB_ACCESS_TOKEN | 与 admin 一致；内网访问 |
| 8 | 默认口令 | 禁用 admin/admin123（仅本地联调） |
| 9 | CORS / 静态资源 | 按 Football 安全基线 |
| 10 | 审计 | 关键操作留痕（Ops + system 日志） |

---

## 14. 本期范围外（Phase 2）

| 能力 | 说明 |
|------|------|
| 外部 SSO / 钉钉 OAuth 独立登录 | 本期 Football 壳登录 |
| 独立 OPS Standalone 登录页 | 非生产签收主链路 |
| **M10 全量采集调度运维** | 菜单与配置边界存在；**不**承诺全量采集运维体系、外部采集规模化手册 |
| 全栈 docker-compose / K8s 一键清单 | 本仓库未提供；由客户运维按 §7–§8 落地 |

---

## 15. 修订记录

| 日期 | 说明 |
|------|------|
| {DELIVERY_DATE} | 重写为自包含部署运维手册：环境/架构/配置/Greenfield SQL/启动/联调/FAQ/安全清单 |
"""


def main() -> None:
    text = build_deploy_doc()
    DEPLOY_OUT.write_text(text, encoding="utf-8", newline="\n")
    print(f"Wrote {DEPLOY_OUT} ({len(text.splitlines())} lines)")
    print("Sources merged:")
    for s in SOURCES:
        print(f"  - {s}")


if __name__ == "__main__":
    main()
