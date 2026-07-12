# Ops 启动矩阵 — Standalone vs Football Integration

> **SSOT**：本文件 · [INTEGRATION-PROGRESS §21](./INTEGRATION-PROGRESS.md#21-待办清单--按用户场景2026-07-04-修订) · [INTEGRATION-PROGRESS §23 #5/#2b](./INTEGRATION-PROGRESS.md#23-本地-football-集成路线图post-mdb--2026-07-05)  
> **最后更新**：2026-07-10

两条本地开发路径 **互斥用途**：Standalone 为 dev/QA harness；Integration 为 **Gate 唯一签收路径**（MDB S0–S4 · INT-S0/S1 · post-MDB smoke）。

**日常 Gate 开发默认**：`.\scripts\start-ops-dev.ps1`（预检 Redis/MySQL 后重启集成栈，见 [OPS-DEV-DEPLOY-GUIDE §2.6](./OPS-DEV-DEPLOY-GUIDE.md#26-redis-密码自动修复集成路径)）。

---

## 1. 总览矩阵

| 维度 | **Path 1 — Ops Standalone** | **Path 2 — Football Integration（Gate 路径）** |
|------|----------------------------|-----------------------------------------------|
| **脚本** | `.\scripts\start-ops-standalone.ps1` | **`.\scripts\start-ops-dev.ps1`**（推荐；底层 `start-integration-all.ps1`） |
| **停止** | 手动关窗口 / 杀 :3000/:8080 | `.\scripts\stop-integration-all.ps1`（**默认保留 Redis :6379**；`-StopRedis` 显式释放） |
| **UI** | `ops-platform-ui-vue` → **http://localhost:3000** | `football-front` → **http://localhost:5777** |
| **API 入口** | 直连 oa-server → **http://localhost:8080** | Gateway → **http://localhost:48080**；oa-server **:48094** |
| **鉴权** | Dev Token `dev-token-oa-admin` + **`X-Tenant-Id: 1`** | Football 登录 **`admin` / `admin123`**，租户 **1** |
| **Spring profiles（oa-server）** | **`dev` only** | `dev,dev-nacos,dev-nacos-local,dev-local-multidb` |
| **数据库** | **`application-dev.yml`** → **101.37.161.136:3306/wd**（单库；**非** localhost 五库） | **`application-dev-local-multidb.yml`** → **localhost:3306 五库**（wd · shenyu-member · shenyu-mp · shenyu-pay · shenyu-system） |
| **Nacos / Redis** | 不启动 | Nacos **:8848** · Redis **:6379** |
| **Gateway** | ❌ | ✅ **:48080** |
| **system-server** | ❌ | ✅ **:48081** |
| **member mock（Hybrid C）** | ❌ 不相关 | ✅ Python **`mock-member-author-server.py` :48087**（登录 Feign 桩；Ops 作者读 `@DS("member")` 直连 shenyu-member） |
| **Football 前端壳** | ❌ | ✅ |
| **Gate / 签收** | **非 Gate** — dev/QA 参考 only | **Gate SSOT**：E2E **58/58** · `post-mdb-local-smoke.py` **4/4** · [POST-MDB-LOCAL-SIGNOFF](./gates/POST-MDB-LOCAL-SIGNOFF-20260705.md) |
| **典型验收脚本** | `scripts/run-uat-browser-e2e.ps1`（15/15 standalone 页） | `scripts/run-uat-football-e2e.ps1` · `scripts/verify-ops-pages-per-menu.py --api` |

### 1.1 何时用哪条路径

| 目标 | 推荐路径 |
|------|----------|
| 日常改 Ops 页面 / API，无需 Football 菜单壳 | **Standalone** |
| Gate 签收、MDB 多库、Gateway 鉴权、5777 全菜单 E2E | **Integration** — **`start-ops-dev.ps1`** |
| 验证 `dev-token-oa-admin` 与遗留 `sys_user` RBAC | **Standalone**（见 §3） |
| 验证 Football 登录链 + Redis token + `oa:*` 菜单权限 | **Integration** |

---

## 2. Path 1 — Ops Standalone（dev/QA harness）

### 2.1 启动

```powershell
# 仓库根目录
.\scripts\start-ops-standalone.ps1
# 仅后端：.\scripts\start-ops-standalone.ps1 -NoFrontend
```

| 组件 | 端口 | 说明 |
|------|------|------|
| oa-server | **8080** | `mvn spring-boot:run '-Dspring-boot.run.profiles=dev'` |
| ops-platform-ui-vue | **3000** | `npm run dev`；proxy `/admin-api` → :8080 |

**日志**：`scripts/logs/backend-dev-run.log` · `frontend-dev-run.log`

### 2.2 Profile 与数据库（重要）

Standalone **仅** 激活 profile **`dev`**，**不** 加载 `dev-local-multidb`。

| 配置源 | 内容 |
|--------|------|
| `application-dev.yml` | 单数据源 **101.37.161.136:3306/wd**；Nacos discovery **关闭**；`oa.auth.mode: dev-fixed` |
| `application-dev-local-multidb.yml` | **未启用** — 该文件仅供 Integration / Gate 路径 |

> **勿混淆**：`application-dev.yml` 头部注释已标明远程库仅作 export/backup 参考；**日常 Gate 与 localhost 五库开发必须用 Integration 路径**。Standalone 默认仍指向远程 `wd`，与 post-MDB localhost 五库 **不是同一套数据**。

### 2.3 鉴权

| 请求头 | 值 |
|--------|-----|
| `Authorization` | `Bearer dev-token-oa-admin` |
| `X-Tenant-Id` | `1` |

前端：`ops-platform-ui-vue/.env.development` → `VITE_API_TOKEN=dev-token-oa-admin`

**Provider 链**：`CompositeAuthProvider` → **`DevAuthProvider` 优先** → 查 `wd.sys_user_token` + `sys_user` / `sys_role` / `sys_permission`（ADR-003）。

### 2.4 不启动的组件

Gateway · Nacos · Redis（oa 集成 Redis 读 token 未开）· system-server · member mock **:48087** · football-front **:5777**

### 2.5 Gate 地位

**不属于** GATE-MDB / GATE-INT / post-MDB 签收路径。UAT standalone 浏览器 E2E（15/15）为 harness 回归参考，见 [UAT-BROWSER-E2E-20260704](./UAT-BROWSER-E2E-20260704.md)。

---

## 3. Path 2 — Football Integration（Gate 路径）

### 3.1 启动

```powershell
# 推荐（日常）：预检 Redis 123456 + MySQL + 重启全栈
.\scripts\start-ops-dev.ps1

# 首次 / 后端大改
.\scripts\start-ops-dev.ps1 -FirstRun

# 底层脚本（无预检包装）
.\scripts\start-integration-all.ps1 -SkipBuild
# 重启：.\scripts\start-integration-all.ps1 -Restart -SkipBuild

# 停止（默认不杀 :6379，避免 Windows redis-server 重启后丢密码）
.\scripts\stop-integration-all.ps1
.\scripts\stop-integration-all.ps1 -StopRedis   # 仅当确需释放 Redis 端口时
```

| 组件 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | Docker 本地 |
| Redis | 6379 | 密码 **123456**；`Ensure-IntegrationRedis` 自动检测/设置（见 OPS-DEV-DEPLOY-GUIDE §2.6） |
| Gateway | **48080** | oa 路由 `Path=/admin-api/oa/**` |
| system-server | 48081 | Football 登录 |
| **member mock（Hybrid C）** | **48087** | `mock-member-author-server.py` — `AuthorApi` Feign 桩 |
| oa-server | **48094** | profiles 含 **`dev-local-multidb`** |
| football-front | **5777** | hash 路由 `#/ops/...` |

**登录**：http://localhost:5777 → `admin` / `admin123`，租户 **1**

**member 说明（Hybrid C）**：登录 Feign 走 :48087 mock；Ops 作者/账号 CRUD 经 oa-server `@DS("member")` **直连 shenyu-member**，不经 member-server HTTP。详见 [INTEGRATION-PROGRESS §20](./INTEGRATION-PROGRESS.md#20-faq--member-mock-vs-真服2026-07-04--23-4-评估-2026-07-05)。

### 3.2 Profile 与数据库

oa-server：`dev,dev-nacos,dev-nacos-local,dev-local-multidb`

| DS 名 | 库 |
|-------|-----|
| master | localhost:3306/**wd** |
| member | localhost:3306/**shenyu-member** |
| mp | localhost:3306/**shenyu-mp** |
| pay | localhost:3306/**shenyu-pay** |
| system | localhost:3306/**shenyu-system** |

凭证默认：`root` / `root`（见 `application-dev-local-multidb.yml`）

Football 服务：`local,local-nacos` + `scripts/integration-config/football-integration-overlay.yml`

### 3.3 鉴权

| 步骤 | 说明 |
|------|------|
| 1 | 浏览器或 `POST /admin-api/system/auth/login` → accessToken（Redis + wd master） |
| 2 | Gateway / oa-server：`FootballAuthProvider` — Redis `oauth2_access_token:{token}` · wd master user/RBAC · shenyu-system username 映射 |
| 3 | `oa:*` 权限来自 **wd.system_menu** + **wd.system_role_menu**（post-MDB §23 #2 已 patch） |

**Gate 路径不使用 `dev-token-oa-admin`。**

### 3.4 Gate 签收探针

| 探针 | 脚本 / 期望 |
|------|-------------|
| API smoke 4/4 | `python scripts/post-mdb-local-smoke.py` |
| 全菜单 E2E 58/58 | `python scripts/verify-ops-pages-per-menu.py --api` |
| Football 浏览器 E2E | `scripts/run-uat-football-e2e.ps1` |
| 签收报告 | [POST-MDB-LOCAL-SIGNOFF-20260705](./gates/POST-MDB-LOCAL-SIGNOFF-20260705.md) |

---

## 4. §23 #2b — dev-token / sys_user 映射（post-S0 TRUNCATE）

> **状态**：文档化完成（2026-07-05）。**无** 独立 re-seed 脚本；需手工 SQL 时参考 Flyway 源文件。

### 4.1 背景

MDB **S0** 执行 `scripts/integration-config/s0-wd-truncate-testdata.sql` 于 **localhost wd**，除配置表外 TRUNCATE 业务数据，并 **清空废弃身份表**：

- `sys_user` · `sys_user_token` · `sys_role` · `sys_user_role` · `sys_permission` · …

其中 **`dev-token-oa-admin`** 原由 Flyway **`V2__seed_base.sql`** 写入 `sys_user_token`（user_id **1001**，用户 **oa-admin**）。TRUNCATE 后 **localhost wd 上 dev-token 行数为 0**（[POST-MDB-LOCAL-SIGNOFF §3](./gates/POST-MDB-LOCAL-SIGNOFF-20260705.md) 已记录）。

### 4.2 两条路径下的鉴权分工

| 路径 | Token | Provider | 依赖表 |
|------|-------|----------|--------|
| **Standalone :8080** | `dev-token-oa-admin` | `DevAuthProvider` | **wd.sys_user_token** → sys_user → sys_role / sys_permission |
| **Integration Gate :48080/:48094** | Football login accessToken | `FootballAuthProvider` | Redis + wd master OAuth · **wd.system_menu / system_role_menu** · shenyu-system 用户映射 |

**结论（产品/工程约定）**：

1. **Gate 签收仅认 Football 登录** — 不要求、不依赖 dev-token。
2. **dev-token 仅 Standalone harness** — 通过 :8080 直连 oa-server 时使用。
3. **`FootballAuthProvider.mergeOaPermissions`** 为 **过渡桥接**：当 Football 用户名在 wd 仍存在匹配 **sys_user** 时，合并遗留 **sys_permission** 码；S0 TRUNCATE 后 localhost **sys_user 为空**，该合并 **无操作**，Gate 权限完全由 **wd Football RBAC seed** 承担（§23 #2 已 patch）。

### 4.3 Standalone 在 post-S0 环境下的注意点

| 场景 | dev-token 是否可用 | 建议 |
|------|-------------------|------|
| Standalone 默认（**101.37.161.136/wd**） | 取决于远程库是否执行过 S0 TRUNCATE | 远程未 TRUNCATE 则通常仍可用 |
| Standalone 若手动改为 **localhost wd**（叠加 `dev-local-multidb`） | ❌ TRUNCATE 后缺失 | 改用 **Integration Gate 路径**，或手工 re-seed（下表） |
| Integration Gate | N/A（不用 dev-token） | **`start-ops-dev.ps1`** + Football 登录 |

### 4.4 手工恢复 dev-token（可选，非 Gate 必需）

无专用 `seed-dev-token.ps1`。可从 Flyway 源 **`V2__seed_base.sql`** 提取最小 INSERT（在目标 **wd** 库执行，**仅 dev harness**）：

```sql
-- 最小 dev-token 恢复（与 V2__seed_base.sql 一致）
INSERT INTO sys_tenant (id, name, status) VALUES (1, 'default', 'ENABLED')
  ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO sys_user (id, tenant_id, username, nickname, status) VALUES
  (1001, 1, 'oa-admin', '系统管理员', 'ENABLED')
  ON DUPLICATE KEY UPDATE username=VALUES(username);
INSERT INTO sys_role (id, tenant_id, code, name) VALUES (1, 1, 'OA_ADMIN', '系统管理员')
  ON DUPLICATE KEY UPDATE code=VALUES(code);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1001, 1)
  ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);
INSERT INTO sys_user_token (user_id, token, status) VALUES (1001, 'dev-token-oa-admin', 'ENABLED')
  ON DUPLICATE KEY UPDATE status=VALUES(status);
-- sys_role_permission 等权限 seed 需与历史 Flyway 一致；完整权限见 V2 及后续 migration
```

执行后验证：

```powershell
curl -H "Authorization: Bearer dev-token-oa-admin" -H "X-Tenant-Id: 1" http://localhost:8080/admin-api/oa/hello
```

---

## 5. 常见误用

| 误用 | 后果 | 正确做法 |
|------|------|----------|
| Standalone 脚本 + 期望 Gate 58/58 | 不同 UI、鉴权、DB | 用 **`start-ops-dev.ps1`** |
| Integration 路径仍用 dev-token 调 Gateway | 401/403（Redis Football token 路径） | Football 登录拿 Bearer |
| 日常 dev 仅用 `application-dev.yml` 远程库 | 与 localhost MDB 数据漂移 | Gate / 多库开发走 Integration |
| 认为 member mock 影响 Standalone | 无关 | Standalone 不启 :48087 |
| UI「内部服务错误」/ Gateway `AUTH, but no password is set` | 本机 Redis 无密码（旧 stop 杀过 :6379 后 Windows 服务重启） | **`.\scripts\start-ops-dev.ps1`** 自动设密码；勿日常手改 `redis-cli` |
| 重启后手动 `redis-cli CONFIG SET` | 非必要；易与脚本预检重复 | 只跑 **`start-ops-dev.ps1`** |
| User/dict API 500（Unknown column `user_type` on `system_role_menu`） | RoleMenuDO 需 `user_type` 列 | `start-integration-all.ps1` 自动 apply `patch-system-role-menu-user-type.sql`；或 `python scripts/integration-config/apply-system-role-menu-user-type.py` |

---

## 6. 相关文档

| 文档 | 用途 |
|------|------|
| [OPS-DEV-DEPLOY-GUIDE](./OPS-DEV-DEPLOY-GUIDE.md) | **开发调试与部署操作指南**（环境准备 · 脚本参数 · 生产构建范围） |
| [INTEGRATION-PROGRESS](./INTEGRATION-PROGRESS.md) | 集成进度 · §21 场景待办 · §23 路线图 |
| [POST-MDB-LOCAL-SIGNOFF](./gates/POST-MDB-LOCAL-SIGNOFF-20260705.md) | Gate 本地签收 |
| [ADR-049](../adr/ADR-049-Ops与Football数据归属与松耦合集成.md) D6 | Standalone harness 决策 |
| [ADR-050](../adr/ADR-050-Ops与Football多库复用总纲.md) | localhost 五库 |
| [application-dev.yml](../../ops-platform-server/ops-platform-module-oa/src/main/resources/application-dev.yml) | Standalone 单库配置 |
| [application-dev-local-multidb.yml](../../ops-platform-server/ops-platform-module-oa/src/main/resources/application-dev-local-multidb.yml) | Integration 五库配置 |
