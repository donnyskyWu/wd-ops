# INTEGRATION-S0：Football × Ops 集成基建 Checklist



> **阶段**：Integration S0（集成基建，独立于原 GATE-S0）  

> **SSOT**：[ADR-047-Football-Ops平台集成决策](../adr/ADR-047-Football-Ops平台集成决策.md)  

> **版本**：v0.2 | 2026-07-02  

> **进度看板**：[INTEGRATION-PROGRESS](./INTEGRATION-PROGRESS.md)  

> **Gate 条件**：本 Checklist 100% + 环境矩阵 smoke 通过



---



## 0. 范围



| In Scope | Out of Scope |

|----------|--------------|

| 环境矩阵、Nacos/Gateway/Redis/MySQL 联通 | Ops 业务 Slice 功能开发 |

| Gateway `oa-server` 路由（配置-only） | 修改 `football-front` / `football-backend-saas` Java/Vue 逻辑 |

| 菜单 / 权限映射提取计划 | M9 页面 UI 迁移实现 |

| `football-module-oa` 工程决策与 S1 引导 | Phase 2 M10 / 外部 SSO |



---



## 1. 环境矩阵



| 组件 | 地址 / 端口 | 用途 | 验证命令 / 期望 |

|------|-------------|------|-----------------|

| **Gateway** | `http://{host}:48080` | 统一 API 入口 | `curl -s -o /dev/null -w "%{http_code}" http://localhost:48080/admin-api/system/tenant/simple-list` → 401/200 |

| **Nacos** | `{host}:8848`（dev 示例 `192.168.10.47:8848`） | 注册 + 配置中心 | 控制台可见 `gateway-server`、`system-server` |

| **Redis** | 与 Football dev 配置一致 | Token / 缓存 | `redis-cli ping` → PONG |

| **MySQL** | **`101.37.161.136:3306/wd`** | 单库 SSOT | Ops 已配置；Football 微服务 Nacos datasource 需对齐 |

| **oa-server** | 建议 `48094`（与 wecom `48093` 错开） | OA 微服务 | Nacos 注册名 **`oa-server`** |

| **system-server** | Football 默认 | M9 SSOT | Gateway `/admin-api/system/**` |

| **Ops 前端（过渡）** | `5173` | 开发联调 | 经 Gateway 代理 `/admin-api/oa/**` |

| **football-front（目标壳）** | `5777`（`pnpm dev:ele` → `apps/web-ele`） | 生产壳 | hash 路由 `#/...` |



### 1.1 Ops 当前 DB 配置（已对齐）



文件：`ops-platform-server/ops-platform-module-oa/src/main/resources/application-dev.yml`



```yaml

spring.datasource.url: jdbc:mysql://101.37.161.136:3306/wd?...

```



Football 侧：**仅改 Nacos** `system-server-dev.yaml` / `infra-server-dev.yaml` 中 `spring.datasource.*` 指向同一库（不改 Java）。



### 1.2 Nacos 命名空间



| Profile | namespace | 说明 |

|---------|-----------|------|

| dev | `dev` | 与 `football-gateway/application-dev.yaml` 一致 |

| local | 各服务 `application-local.yaml` | 本地可指向 embedded / 远程 |



---



## 2. Football 仓库：仅允许的配置变更



> **硬规则**：下列文件可改；**禁止**改其他 `.java` / `.vue` / `.ts` 逻辑文件。



### 2.1 Gateway 路由



| 文件 | 变更 |

|------|------|

| `football-backend-saas/football-gateway/src/main/resources/application.yaml` | 新增 `oa-admin-api` 路由 + knife4j `oa-server` |

| `football-backend-saas/football-gateway/src/main/resources/application-dev.yaml` | Nacos 地址（如需） |

| `football-backend-saas/football-gateway/src/main/resources/application-local.yaml` | 本地 Nacos |

| Nacos **`gateway-server-dev.yaml`** | 生产级路由（优先） |



### 2.2 各微服务数据源（Nacos 或 yaml）



| 文件 / 配置 DataId | 变更 |

|--------------------|------|

| Nacos `system-server-dev.yaml` | `spring.datasource.url` → `101.37.161.136/wd` |

| Nacos `infra-server-dev.yaml` | 同上 |

| Nacos `oa-server-dev.yaml` | **新建**（S1） |

| `football-module-system/.../application-dev.yaml` | 仅 `nacos.server-addr` 等（已有） |

| `football-module-infra/.../application-dev.yaml` | 同上 |

| `football-module-wecom/.../application-dev.yaml` | 参考模板，不改逻辑 |



### 2.3 football-front（已解压，`apps/web-ele/`）



| 文件 | 变更 |

|------|------|

| `.env.development` / `.env` | `VITE_GLOB_API_URL=http://localhost:48080` |

| `vite.config.*` | 仅 proxy target 指向 Gateway（若存在） |



### 2.4 明确禁止



- `football-backend-saas/pom.xml` 的 `<modules>` 新增条目（S0 不动；S1 亦避免）

- 任意 `football-module-*/src/main/java/**` 业务逻辑

- `football-front/src/**` 路由 / 权限 / 布局逻辑



---



## 3. 菜单与权限映射提取计划



### 3.1 源文件（Ops SSOT）



| 源 | 路径 | 提取内容 |

|----|------|----------|

| 侧边栏 | `ops-platform-ui-vue/src/views/Layout.vue` | 菜单树：一级分组 + `index` 路径 |

| 路由 | `ops-platform-ui-vue/src/router/index.ts` | `path` · `meta.title` · `component` 路径 |

| 权限 | `ops-platform-server/.../db/migration/V*.sql` | `oa:*` 权限码 seed |

| API | `docs/engineering/API-M*.md` | 权限与模块对照 |



### 3.2 提取步骤（S0 文档化，S1 执行 SQL）



1. **脚本化扫描**（S1）：解析 `Layout.vue` 的 `el-menu-item index="/xxx"` 与 `router/index.ts` 的 `path` + `meta.title`，输出 CSV：`route_path, menu_title, parent_group, component_file`。

2. **权限对齐**：从 Flyway 中 `INSERT INTO sys_permission`（或等价表）提取 `oa:*`，映射到 Football `system_menu.permission`。

3. **M9 菜单排除**：`Layout.vue` §10 系统管理中 **用户 / 角色 / 租户** 三项不导入 OA 菜单，改指向 Football 已有 `/system/user` 等（hash 路由）。

4. **产出物**：`docs/delivery/integration/oa-menu-permission-map.csv`（S1 创建）+ Flyway seed `V200__integration_football_menu.sql`（S1）。

5. **前缀规则**：业务菜单 permission 保持 **`oa:{module}:{action}`**；M9 身份类用 Football 原生 **`system:*`**。



### 3.3 Layout.vue 菜单分组快照（提取起点）



| 分组 | 示例子路径 | 模块 |

|------|------------|------|

| 首页 | `/dashboard` | M0 |

| 运营管理 | `/ip-group` … `/efficiency` | M1 |

| 内容生产 | `/sop` … `/layout-template` | M2 |

| 绩效核算 | `/perf-template` … | M3 |

| 账号管理 | `/company` … `/personal-account` | M4 |

| 财务管理 | `/account-cost` … | M5 |

| 数据采集 | `/collect/*` | M10 |

| 数据分析 | `/metric` … `/screen-config` | M6 |

| 作品监测 | `/external-account` … | M7 |

| 配置管理 | `/config-*` | M8 |

| 系统管理 | `/system-user` … | M9（**用户/角色/租户废弃**） |



---



## 4. S1 微服务 Bootstrap 方案



### 4.1 推荐路径（两阶段）



| 阶段 | 方案 | 说明 |

|------|------|------|

| **S1-A（快速联通）** | 保留 `ops-platform-module-oa`，加 Nacos Discovery | 最小改动：改 `spring.application.name=oa-server`，引入 `spring-cloud-starter-alibaba-nacos-discovery`，Gateway 配置路由 |

| **S1-B（结构对齐）** | 新建 `wd/football-module-oa/` sibling 工程 | 从 Ops 迁移包名 `football.module.oa`，依赖 Football BOM；**不**加入 `football-backend-saas/pom.xml` modules |



**S0 决策**：✅ 采用 **S1-A → S1-B** 渐进；S0 仅完成决策文档与环境矩阵。



### 4.2 `football-module-oa` 目标目录（S1-B）



```

wd/

├── football-backend-saas/     # 不改 modules 列表

├── football-module-oa/        # 新建 sibling

│   ├── pom.xml                # parent 引用 football-dependencies BOM

│   ├── football-module-oa-api/

│   └── football-module-oa-server/

│       ├── pom.xml

│       └── src/main/

│           ├── java/football/module/oa/

│           └── resources/

│               ├── application.yaml      # spring.application.name: oa-server

│               └── db/migration/         # 自 Ops 迁移

├── ops-platform-server/       # S1-A 过渡；S2 废弃

└── ops-platform-ui-vue/       # 过渡前端

```



### 4.3 oa-server 注册对照（参考 wecom-server）



| 项 | wecom-server | oa-server（目标） |

|----|--------------|-------------------|

| `spring.application.name` | `wecom-server` | **`oa-server`** |

| 端口 | 48093 | **48094**（建议） |

| Gateway Path | `/admin-api/wecom/**` | **`/admin-api/oa/**`** |

| Nacos | ✅ | ✅ S1 启用 |



---



## 5. S0 Checklist



### 5.1 文档与决策



- [x] ADR-047-Football-Ops 集成决策文档

- [x] 本 INTEGRATION-S0 Checklist

- [x] MASTER-EXECUTION-TRACKER §18 集成索引

- [ ] 表归属清单（Football `system_*` vs OA `oa_*` / `sys_*`）— S1 产出



### 5.2 环境



- [ ] MySQL `101.37.161.136/wd` 可从开发机连通

- [ ] Nacos dev 命名空间可访问（`192.168.10.47:8848`，namespace `dev`）

- [ ] Redis 与 Football dev 配置一致

- [ ] Gateway 48080 本地 / 远程可启动

- [x] 确认 `football-front/` hash 路由与 `.env` 配置（`apps/web-ele/.env.development` → `VITE_BASE_URL=http://localhost:48080`）



### 5.3 配置-only 预检（不提交逻辑改动）



- [x] 确认 Gateway 路由扩展点（`application.yaml` § routes）— **S1-A 已追加 `oa-admin-api`**

- [x] 确认 Ops DB 已指向 101.37.161.136

- [x] 列出 Football Nacos DataId 清单（system/infra/gateway）— 见 §1.2、`gateway-server-dev.yaml`、`system-server-dev.yaml`、`infra-server-dev.yaml`；**S1-B 新建 `oa-server-dev.yaml`**

- [x] 确认 `oa-server` 服务名与 ADR-009 API 前缀一致（`/admin-api/oa/**`）



### 5.3.1 S1-A 已完成（2026-07-02）



| 项 | 路径 / 说明 |

|----|-------------|

| Nacos Discovery | `ops-platform-module-oa/pom.xml` → `spring-cloud-starter-alibaba-nacos-discovery` |

| 集成 Profile | `application-dev-nacos.yml`（`oa-server` · `:48094` · Nacos dev） |

| 独立 Dev | `application-dev.yml` 禁用 Nacos；`restart-all.ps1` 仍用 `dev` · `:8080` |

| 启动脚本 | `scripts/start-integration-oa.ps1` |

| Gateway 路由 | `football-gateway/.../application.yaml` → `oa-admin-api` + knife4j `oa-server` |

| 前端 Gateway | `football-front/apps/web-ele/.env.development` |



**启动集成 oa-server**：



```powershell

.\scripts\start-integration-oa.ps1

# 或

cd ops-platform-server/ops-platform-module-oa

mvn spring-boot:run "-Dspring-boot.run.profiles=dev,dev-nacos"

```



**已知阻塞**：开发机若无法访问 `192.168.10.47:8848`，Nacos 注册失败；配置已就绪，待网络可达后 smoke。





### 5.3.2 本地集成路径（dev-nacos-local）



| 步骤 | 命令 |

|------|------|

| 1. 本地 Nacos（Docker） | .\scripts\start-nacos-local.ps1 → http://127.0.0.1:8848/nacos |

| 2. oa-server 集成模式 | .\scripts\start-integration-oa.ps1（默认 profiles dev,dev-nacos,dev-nacos-local） |

| 一键 | .\scripts\start-integration-stack.ps1（Nacos 失败仍启 oa-server，48094 fail-soft） |

| 健康检查 | curl http://localhost:48094/actuator/health → UP |

| Nacos 注册 | 控制台 namespace **dev** 下服务名 **oa-server** |



Profile：`application-dev-nacos.yml`（远程 192.168.10.47 · fail-fast: false）+ `application-dev-nacos-local.yml`（127.0.0.1:8848）。



独立 Ops 开发（无 Nacos）：`.\scripts\restart-all.ps1` 仍用 profile **dev** · 端口 **8080**。



### 5.4 映射提取（计划就绪即可 S0 勾选）



- [x] 明确 Layout.vue + router 为菜单 SSOT

- [x] 明确 M9 用户/角色/租户排除规则

- [x] 明确 `oa:*` 权限前缀保留策略

- [ ] 运行提取脚本产出 CSV（S1）



---



## 6. 当前进度看板



> **一页速览**：[INTEGRATION-PROGRESS.md](./INTEGRATION-PROGRESS.md) · 更新：2026-07-03


### 6.1 ADR-047 已锁定决策



| # | 决策 | 说明 |

|---|------|------|

| D1 | **直连微服务** | Nacos + Gateway **48080**；不走 monolith-first |

| D2 | **单库** | MySQL **`101.37.161.136:3306/wd`** |

| D3 | **M9 SSOT** | Football `system-server`；废弃 Ops 用户/角色/租户 |

| D4 | **权限前缀** | 业务保留 **`oa:*`** |

| D5 | **数据权限** | OA 扩展 `biz-data-permission`；禁止改 Football 框架 Java |

| D6 | **前端路由** | Football hash 路由；禁止改 `football-front` 业务逻辑 |



**硬约束**：仅允许配置 / Nacos / Gateway / DB seed；禁止改 `football-front/**`、`football-backend-saas/**` Java/Vue **逻辑**（详见 ADR-047 §3）。



### 6.2 阶段进度



| 阶段 | 状态 | 完成项 | 阻塞 | 下一步 |

|------|------|--------|------|--------|

| **S0** 集成基建 | 🟢 基本完成 | ADR-047；本文档；MASTER §18；菜单映射计划；Gateway 扩展点 | 环境连通 smoke 未完成 | §5.2 环境勾选；GATE-INT-S0 报告 |

| **S1-A** 快速联通 | 🟢 完成 | Nacos Discovery；`dev-nacos*` Profile；Gateway `oa-admin-api`；集成脚本 | 远程 Nacos 网络 | 本地 stack health |

| **S1-B** 结构对齐 | 🔵 进行中 | **`system_*`→`wd` 导入完成**；5777 bootstrap 绿 | login 500（member :48087） | `start-integration-system.ps1` |

| **S2** M9 + 菜单 | ⬜ 待开始 | — | member/infra 微服务 | CSV + Flyway seed |

| **S3** 全栈联调 | ⬜ 待开始 | — | **Nacos namespace local vs dev** | 对齐 namespace；system smoke |

| **S4** 5777 壳 | 🟡 部分 | `.env` → Gateway | **登录未绿**（auth/login code=500） | member-server + 前端登录 smoke |

| **S5** 远程签收 | ⬜ 待开始 | — | 依赖 S2–S4 | GATE-INT-S1 |



**5777 状态（2026-07-03）**：壳层可开；**bootstrap 绿**（tenant API code=0）；**登录未通过**（需 member-server）。DDL/启动脚本见 [INTEGRATION-PROGRESS §9](./INTEGRATION-PROGRESS.md#9-s1-b-状态快照2026-07-03)。



### 6.3 服务矩阵



| 组件 | 端口 | 说明 |

|------|------|------|

| Nacos | **8848** | 本地 Docker 或 `192.168.10.47` |

| Gateway | **48080** | 统一 API 入口 |

| system-server | **48081** | Football 身份 SSOT |

| oa-server | **48094** | Ops 过渡微服务 |

| football-front | **5777** | `pnpm dev:ele` → `apps/web-ele` |



### 6.4 启动脚本索引



| 脚本 | 用途 |

|------|------|

| `scripts/start-nacos-local.ps1` | 本地 Nacos |

| `scripts/start-integration-stack.ps1` | Nacos + oa-server |

| `scripts/start-integration-oa.ps1` | 仅 oa-server |

| `scripts/start-integration-system.ps1` | mp + member + system（Gateway 联调） |

| `scripts/start-football-system.ps1` | infra + mp + system（5777 前置） |

| `scripts/restart-all.ps1` | 独立 Ops（8080，无 Nacos） |



---



## 7. S0 Gate 产出



| 产出 | 路径 |

|------|------|

| 集成 ADR | `docs/adr/ADR-047-Football-Ops平台集成决策.md` |

| S0 Checklist | 本文档 |

| **进度看板** | `docs/delivery/INTEGRATION-PROGRESS.md` |

| Gate 报告（S0 完成后） | `docs/delivery/gates/GATE-INT-S0-报告-{YYYYMMDD}.md` |



---



## 8. S1 首批任务预览



见 [MASTER-EXECUTION-TRACKER §18.3](./MASTER-EXECUTION-TRACKER.md#183-s1-首批任务) 与 [INTEGRATION-PROGRESS §阶段总览](./INTEGRATION-PROGRESS.md#阶段总览)。

