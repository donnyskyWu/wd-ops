# OPS 审计日志迁移方案（OPS-only）

> **范围**：仅改 `ops-platform-server` / `ops-platform-ui-vue` / OPS 集成层（菜单 seed、football-front `views/ops/**` 挂载副本）。**禁止**修改 `football-backend-saas/**`、`football-front` 原生业务页面逻辑。  
> **状态**：规划稿 · 待产品/架构确认后分派并行 worker。  
> **日期**：2026-07-18

---

## 0. 背景与现状摘要

| 维度 | 当前 OPS 实现 | Football 标准 |
|------|----------------|---------------|
| **登录** | OPS 无独立登录；仍暴露「登录日志」菜单 + `GET /admin-api/oa/system/log/login` | Football `system-server` 写/读 `system_login_log`；页面 `#/log/login-log` |
| **操作日志读** | S3 已完成：`OperateLogAdapter` 跨库读 `shenyu-system.system_operate_log` | Admin API：`GET /admin-api/system/operate-log/page` |
| **操作日志写** | `@AuditLog` AOP → `OperationLogRecorder` → **本地 `sys_operation_log`（wd）** | `@LogRecord`（mzt-log）→ `LogRecordServiceImpl` → Feign `OperateLogCommonApi` → `system_operate_log` |

**关键缺口**：读路径已接 Football 库，**写路径仍落本地 `sys_operation_log`**，导致 OPS 操作在 Football 操作日志页不可见（或仅能看到 Football 自身 `@LogRecord` 记录）。

---

## 1. 决策

### 1.1 登录日志 — **整页移除（OPS 侧）**

**理由**：OPS 不承载登录；统一经 Football SSO。OPS 菜单下的「登录日志」与 Football「审计日志 → 登录日志」重复，且易误导（OPS API 读 Football 库但无 OPS 登录事件）。

**删除/停用清单**

| 层 | 路径 / 对象 | 动作 |
|----|-------------|------|
| **UI 源码** | `ops-platform-ui-vue/src/views/system/LoginLog.vue` | 删除 |
| | `ops-platform-ui-vue/src/router/index.ts`（`/system-log/login`） | 删除路由 |
| | `ops-platform-ui-vue/src/views/Layout.vue`（侧栏「登录日志」） | 删除菜单项 |
| | `ops-platform-ui-vue/src/api/system-log.ts`（`fetchLoginLogs` / `LoginLogVO`） | 删除 login 段 |
| | `ops-platform-ui-vue/tests/ux-routes.ts` | 删除 M9 login 行 |
| **Football 挂载副本** | `football-front/apps/web-ele/src/views/ops/system/LoginLog.vue` | 删除 |
| | `football-front/apps/web-ele/src/api/ops/system-log.ts`（login 段） | 删除 |
| **后端 API** | `LogController.login()`、`GET .../log/login` | 删除 endpoint |
| | `LogService.listLogin()` / `LogServiceImpl` login 分支 | 删除 |
| | `LoginLogAdapter` | 删除 |
| | `FootballSystemLoginLogDO` / `FootballSystemLoginLogMapper` | 删除 |
| | `SysLoginLogDO` / `SysLoginLogMapper`（若无其他引用） | 删除或标记 deprecated |
| | `LoginLogVO` | 删除 |
| **菜单/权限** | `system_menu` id **6138**（`oa:log:login`） | DELETE / visible=0 |
| | `scripts/integration-config/seed-oa-system-menu.sql` 6138 行 | 删除 |
| | `scripts/integration-config/patch-smoke-api-permissions.sql` login 行 | 删除 |
| | `docs/delivery/oa-menu-permission-map.csv` 登录日志行 | 删除 |
| | `sys_permission` id 37 `oa:log:login`（V52 seed） | 停用（新 migration） |
| **测试/脚本** | `MdbS3LoginLogAdapterIT.java` | 删除 |
| | UAT/e2e 中 `#/ops/system-log/login` 探测 | 移除 |

**保留（Football 侧，只读引用）**：Football 原生 `#/log/login-log`（menu 501，`system/loginlog/index`）供平台管理员在 Football「审计日志」下查看；**不在 OPS 菜单重复挂载**。

---

### 1.2 操作日志 — **写走 Football `@LogRecord` 链路，读走 Football Admin API（或 UI 跳转）**

#### 写路径（推荐架构）

```
Service 方法
  @LogRecord(type, subType, bizNo, success=...)
  LogRecordContext.putVariable(...)
        ↓ mzt-log AOP
  ILogRecordService (LogRecordServiceImpl 同款)
        ↓ Feign
  OperateLogCommonApi.createOperateLogAsync
  POST /rpc-api/system/operate-log/create  →  system-server  →  system_operate_log
```

**Football 参考实现（只读，不改 Football 代码）**

- 注解示例：`football-module-member/.../ArticleServiceImpl.java`（`@LogRecord` + `LogRecordContext.putVariable`）
- 常量：`football-module-member-api/.../LogRecordConstants.java`
- 框架：`football-spring-boot-starter-security` → `FootballOperateLogConfiguration`（`@EnableLogRecord`）+ `LogRecordServiceImpl`
- RPC 接口：`football-common/.../OperateLogCommonApi.java`（Feign → `system-server`）
- RPC 实现：`football-module-system-server/.../OperateLogApiImpl.java`

**OPS 侧落地方案（二选一，推荐 A）**

| 方案 | 说明 | 优点 | 风险 |
|------|------|------|------|
| **A. 引入 `@LogRecord`（对齐 Football）** | oa-server 增加 mzt-log + 复制/依赖 `LogRecordServiceImpl` + `@EnableFeignClients(OperateLogCommonApi)`；新增 `OaLogRecordConstants`；逐模块将 `@AuditLog` 换为 `@LogRecord` | 与 Football 一致；日志字段完整（type/subType/bizId/action/extra） | 需引入 `football-module-system-api` 或等价 DTO；Boot 3.2.5 与 Football BOM 版本需 pin 兼容 |
| **B. 保留 `@AuditLog`，Recorder 改 Feign** | `OperationLogRecorder` 改为组装 `OperateLogCreateReqDTO` 调 `OperateLogCommonApi` | 改动面小；可快速统一存储 | 非 Football 标准注解；success 模板、DIFF 等能力弱 |

**推荐**：**A 为目标态**；若工期紧，可先 **B 作 Batch-0 桥接**（1–2 天），再 **Batch-1 按模块并行换 `@LogRecord`**。

**需移除/废弃（写路径切换完成后）**

- `AuditLogAspect.java`、`AuditLog.java`
- `OperationLogRecorder.java`
- `SysOperationLogDO` / `SysOperationLogMapper`
- 本地表 `sys_operation_log`：停写；历史数据可保留只读或一次性归档后 truncate（单独 migration，非本期必须）

#### 读路径（推荐架构）

**选项 1（推荐）：OPS 菜单移除，跳转 Football 原生页**

| 项 | 值 |
|----|-----|
| Hash 路由 | `#/log/operate-log` |
| 菜单 | `system_menu` id **500**，parent **108**「审计日志」`path=log` |
| 组件 | `system/operatelog/index`（`SystemOperateLog`） |
| Admin API | `GET /admin-api/system/operate-log/page` |
| 权限 | `system:operate-log:query` / `system:operate-log:export` |
| 前端 API 封装 | `football-front/apps/web-ele/src/api/system/operate-log/index.ts` |

**选项 2：保留 OPS `LogManage.vue`，前端直调 Football API**

- 删除 `GET /admin-api/oa/system/log/operation`
- `LogManage.vue` 改为调用 `/system/operate-log/page`（与 Football 字段对齐，UI 需适配 `type/subType/action` 等）

**选项 3（现状，不推荐为终态）**：保留 `OperateLogAdapter` 跨库 `@DS("system")` — 已 S3 验收，但**不符合「走 Football API」**表述。

**推荐**：**选项 1**（最少 OPS 维护面）+ 给 OPS 角色补 `system:operate-log:query`；删除 menu **6139** 与 `oa:log:operation`。

---

## 2. Football API 端点速查（只读引用）

| 用途 | HTTP | 服务 | 权限 |
|------|------|------|------|
| 分页查询 | `GET /admin-api/system/operate-log/page` | system-server | `system:operate-log:query` |
| 导出 | `GET /admin-api/system/operate-log/export-excel` | system-server | `system:operate-log:export` |
| RPC 创建 | `POST /rpc-api/system/operate-log/create` | system-server（Feign） | 内部 RPC，无 UI 权限 |
| RPC 分页 | `GET /rpc-api/system/operate-log/page` | system-server（Feign `OperateLogApi`） | 内部 RPC |
| 登录日志（Football 原生，非 OPS） | `GET /admin-api/system/login-log/page` | system-server | `system:login-log:query` |

Gateway 集成下 Admin API 前缀：`http://localhost:48080/admin-api/...`（经 `gateway-integration-local.yaml` → `system-server`）。

---

## 3. 集成可行性结论

| 问题 | 结论 |
|------|------|
| oa-server 能否用 `@LogRecord`？ | **能**。需添加 mzt-log starter + `@EnableLogRecord` + `ILogRecordService` 实现（复用 Football `LogRecordServiceImpl` 逻辑）+ **OpenFeign** 调 `OperateLogCommonApi`。当前 oa-server **无** Feign / mzt-log 依赖。 |
| 能否 Feign 创建日志而不改 Football？ | **能**。`OperateLogCommonApi` 已在 `football-common` 定义；oa-server 仅需依赖 API 模块 + Nacos 发现 `system-server`（集成 profile 已有）。 |
| 能否 Feign/Gateway 查列表替代 `OperateLogAdapter`？ | **能**。Admin API 或 `OperateLogApi.getOperateLogPage`；需处理 OPS 旧筛选字段（`username/module/level`）→ Football VO（`userId/type/subType/action/createTime[]`）映射。 |
| 是否保留 `sys_operation_log`？ | **写：否**（迁移后停写）。**读：否**（终态走 Football）。表可 deprecated，不必物理删表（Flyway 另开 ADR）。 |
| oa-server 现有 Feign 模式？ | **无** OpenFeign client；与 Football 集成现仅 **@DS 跨库 Mapper** + **Redis OAuth2** + Gateway HTTP。操作日志写需 **新增 Feign 基建**（Batch-0  blocker）。 |

**版本注意**：oa-server 使用 Spring Boot **3.2.5** + Cloud **2023.0.4**；Football 使用 Boot **3.5.x**。引入 Football API jar 时只引 **DTO + Feign 接口**，勿整包引入 starter，避免 BOM 冲突；mzt-log 版本与 Football `football-dependencies` 对齐（实施时从 Football BOM 抄版本号）。

---

## 4. 并行子任务表

| id | 范围 | 主要文件/模块 | 依赖 | 可并行 |
|----|------|---------------|------|--------|
| **AL-01** | 移除登录日志 UI（ops-ui-vue） | `LoginLog.vue`、router、Layout、`system-log.ts`、ux-routes | — | **Y** |
| **AL-02** | 移除登录日志 UI 挂载副本 | `football-front/.../views/ops/system/LoginLog.vue`、`api/ops/system-log.ts` login 段 | — | **Y** |
| **AL-03** | 移除登录日志后端 | `LogController`、`LoginLogAdapter`、Football/Sys LoginLog DO+Mapper、`LoginLogVO`、`MdbS3LoginLogAdapterIT` | — | **Y** |
| **AL-04** | 菜单/权限：删 6138 login | `seed-oa-system-menu.sql`、新 Flyway patch、`oa-menu-permission-map.csv`、`patch-smoke-api-permissions.sql` | — | **Y** |
| **AL-05** | **操作日志写：Feign + LogRecord 基建** | `ops-platform-module-oa/pom.xml`（openfeign、system-api/mzt-log）、`OaOperateLogConfiguration`、`OaLogRecordServiceImpl`（或 vendored `LogRecordServiceImpl`）、`@EnableFeignClients` | Nacos 集成环境可用 | **N**（Batch-1 前置） |
| **AL-06** | **OaLogRecordConstants + 试点模块** | 新建 `OaLogRecordConstants`；选 1 模块（建议 M9 `ParamServiceImpl` 3 处）换 `@LogRecord` | AL-05 | **Y**（试点完成后按模块拆分） |
| **AL-07** | **批量 `@AuditLog` → `@LogRecord`** | ~60 个 Service 文件、~200 处 `@AuditLog`（按 M1/M2/M4… 分 worker） | AL-06 试点通过 | **Y**（模块间互不依赖） |
| **AL-08** | 移除 `@AuditLog` 栈 | `AuditLogAspect`、`AuditLog`、`OperationLogRecorder`、`SysOperationLog*` | AL-07 全部完成 | **N** |
| **AL-09** | 操作日志读：UI 切 Football | 删/隐藏 `LogManage.vue`、router、Layout；menu 6139 → 外链或删除；文档说明 `#/log/operate-log` | AL-04 可部分重叠 | **Y** |
| **AL-10** | 操作日志读：后端清理 | 删 `LogController.operation`、`OperateLogAdapter`、Football operate log Mapper；或改 thin proxy（若选选项 2） | AL-09 方案确认 | **Y**（与 AL-09 同批） |
| **AL-11** | 角色权限映射 | OPS 角色补 `system:operate-log:query`；移除 `oa:log:operation` / `oa:log:login` role_menu | AL-04、AL-09 | **N** |
| **AL-12** | 测试与验收脚本 | 更新 `TESTCASES-M9`、`e2e-dataflow-verify.ps1`、UAT spotcheck；新增「OPS 写操作 → Football operate-log 可见」用例 | AL-05–AL-11 | **N** |

**规模参考**：`@AuditLog` 约 **200+** 处，分布在 **~60** 个 Java 文件（见 `ops-platform-module-oa` grep）。

---

## 5. 推荐执行顺序（并行批次）

```text
Batch-0（可全开 4 路并行）
  AL-01 + AL-02 + AL-03 + AL-04     ← 登录日志彻底移除

Batch-1（串行 gate）
  AL-05                               ← Feign + @EnableLogRecord 基建（阻塞写路径）

Batch-2（可并行）
  AL-06                               ← 单模块试点 + IT
  AL-09 + AL-10 + AL-11（与 AL-06 并行，读路径/UI/权限）

Batch-3（按模块并行，可多 worker）
  AL-07-M1 | AL-07-M2 | AL-07-M4 | …  ← 各模块 @LogRecord 迁移

Batch-4（收敛）
  AL-08                               ← 删除 AuditLog 栈
  AL-12                               ← 全量回归
```

---

## 6. 验收标准（DoD 草案）

1. OPS 菜单/路由/**无**「登录日志」；`GET /oa/system/log/login` 返回 404 或路由不存在。
2. OPS 侧栏**无**独立「操作日志」或点击后进入 Football `#/log/operate-log`（选项 1）。
3. 任意带 `@LogRecord` 的 OPS 写操作（如创建系统参数）在 Football 操作日志页可查到，`type` 为 OPS 模块常量。
4. `sys_operation_log` **无新写入**（SQL 或 IT 断言）。
5. `mvn -pl ops-platform-module-oa test` 绿；集成 smoke 更新通过。
6. **零** `football-backend-saas/**`、`football-front` 原生 `views/system/**` Java/Vue 逻辑 diff。

---

## 7. 阻塞 / 待确认项

| # | 问题 | 建议默认 |
|---|------|----------|
| Q1 | 操作日志读：选项 1（跳转 Football）还是选项 2（保留 OPS 页调 Football API）？ | **选项 1** |
| Q2 | 写路径：直接 `@LogRecord`（A）还是先 Feign 桥接（B）？ | **先 B 再 A**，或资源足则直接 A |
| Q3 | OPS 角色是否默认授予 `system:operate-log:query`？ | **是**（替换 `oa:log:operation`） |
| Q4 | 历史 `sys_operation_log` 数据是否迁移到 `system_operate_log`？ | **否**（Out of Scope；仅停写） |
| Q5 | `@LogRecord` 的 `bizNo` 必须 Long：无业务 id 的操作（如 export）如何处理？ | 使用 `0L` 或雪花 id placeholder，需在 `OaLogRecordConstants` 规范 |

---

## 8. 附录：OPS 文件索引

### 登录日志（待删）

- `ops-platform-ui-vue/src/views/system/LoginLog.vue`
- `football-front/apps/web-ele/src/views/ops/system/LoginLog.vue`
- `ops-platform-server/.../controller/system/LogController.java`（login 方法）
- `ops-platform-server/.../service/system/LoginLogAdapter.java`

### 操作日志（待改/删）

- 写：`AuditLogAspect.java`、`OperationLogRecorder.java`、`SysOperationLogMapper.java`
- 读：`OperateLogAdapter.java`、`LogManage.vue`、`football-front/.../ops/system/LogManage.vue`
- API：`ops-platform-ui-vue/src/api/system-log.ts`、`football-front/.../api/ops/system-log.ts`

### Football 原生操作日志（跳转目标）

- 路由：`#/log/operate-log`（menu 500 @ parent 108）
- 页面：`football-front/apps/web-ele/src/views/system/operatelog/index.vue`
- API：`football-front/apps/web-ele/src/api/system/operate-log/index.ts`

---

*本文档为规划产出；实施前需产品确认 §7 待确认项。*
