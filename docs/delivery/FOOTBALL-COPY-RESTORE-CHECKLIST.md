# Football 仓库拷贝恢复 Checklist（Ops 集成）

> **用途**：从别处拷贝最新 `football-backend-saas`、`football-front` 到本工作区后，恢复 Ops × Football 本地集成能力。  
> **SSOT**：[ADR-047](../adr/ADR-047-Football-Ops平台集成决策.md) · [INTEGRATION-S0](./INTEGRATION-S0-Football-Ops.md) · [OPS-STARTUP-MATRIX](./OPS-STARTUP-MATRIX.md) · [FOOTBALL-PROJECT-CHANGES](./FOOTBALL-PROJECT-CHANGES.md)  
> **版本**：v1.0 | 2026-07-18

---

## 0. 能否直接拷贝 fresh？

| 问题 | 答案 |
|------|------|
| **能否拷贝最新 upstream Football 仓库覆盖本地？** | **可以**，但有前提 |
| **拷贝后能否零配置启动 Gate 路径？** | **不能** — 集成配置在 **wd 主仓**（`scripts/`、`ops-platform-*`），不在 Football 子仓 |
| **能否丢弃 Football 子仓全部 dirty 文件？** | **大部分可以** — 见 §8；集成层应通过 wd 脚本 + 最小补丁恢复 |

### 核心原则（ADR-047）

1. **Ops 业务 SSOT** 在 `ops-platform-server/`、`ops-platform-ui-vue/` — **禁止被 Football 拷贝覆盖**。
2. **Football 集成** = 配置 / 数据 / 挂载脚本；**禁止**改 Football Java/Vue **业务逻辑**（Gateway 路由、`.env`、Ops 挂载目录除外）。
3. **Gate 路径**（`:5777` + Gateway `:48080`）使用 **localhost 五库** + **Redis 123456** + **overlay YAML**（运行时注入，不必全部写进 Football 仓库）。

### 拷贝策略（推荐）

```
1. 备份（可选）当前 wd/football-backend-saas、wd/football-front
2. 删除或移走旧目录
3. git clone / 解压最新 upstream 到 wd/football-backend-saas、wd/football-front
4. 按本文 §3–§7 恢复集成层（不碰 ops-platform-*）
5. 验证：.\scripts\start-ops-dev.ps1
```

---

## 1. 端口与服务矩阵

| 组件 | 端口 | 说明 |
|------|------|------|
| **Nacos** | **8848** | Docker 本地；`scripts/start-nacos-local.ps1` |
| **Redis** | **6379** | 密码 **`123456`**（集成硬要求） |
| **Gateway** | **48080** | 统一 API；探针 `/admin-api/system/tenant/simple-list` |
| **system-server** | **48081** | Football 登录 SSOT |
| **member mock** | **48087** | Python 桩（默认）；非完整 member-server |
| **mp-server** | **48086** | 微信等 |
| **oa-server** | **48094** | Ops 过渡微服务 |
| **football-front** | **5777** | `pnpm dev:ele` → `apps/web-ele` |
| **Ops standalone UI** | **3000** | `ops-platform-ui-vue`（非 Gate） |
| **Ops standalone API** | **8080** | oa-server profile **`dev` only** |
| **unify-collector-api** | **8000** | `restart-all.ps1` 路径 |

**登录（Gate）**：http://localhost:5777 · `admin` / `admin123` · 租户 **1**

---

## 2. 禁止覆盖的 wd 主仓内容

拷贝 Football 子仓时，**切勿覆盖或删除**以下路径（Ops SSOT）：

| 路径 | 原因 |
|------|------|
| `ops-platform-server/ops-platform-module-oa/` | oa-server 业务、Flyway、Nacos/multidb profile |
| `ops-platform-ui-vue/` | Standalone harness `:3000`、mount 源文件 |
| `scripts/` | 一键启动、integration-config、mount-ops-all.py |
| `docs/delivery/oa-menu-permission-map.csv` | 菜单映射 SSOT |
| `docs/delivery/integration-config/`（若在 docs 下） | — |
| `unify-collector-api/` | M10 采集（`restart-all.ps1`） |

Football 子仓仅为 **wd 工作区内的 sibling 目录**，与主仓 git 独立。

---

## 3. 分步恢复流程

### Phase A — 前置环境

- [ ] **JDK 17+**、**Maven 3.8+**、**Node 18+**、**pnpm**、**Docker Desktop**（Nacos/Redis 容器）
- [ ] **MySQL 8** @ `localhost:3306`，凭证默认 **`root` / `root`**
- [ ] 创建五库（Gate 路径必需）：

```sql
CREATE DATABASE IF NOT EXISTS wd DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `shenyu-member` DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `shenyu-mp` DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `shenyu-pay` DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `shenyu-system` DEFAULT CHARSET utf8mb4;
```

- [ ] 导入 Football 四库基线：参考 `docs/sql/shenyu-*.sql`（如 `shenyu-system0708.sql`）
- [ ] **`wd` 库**：首次启动 oa-server 时 **Flyway 自动迁移**；或按 MDB 文档执行 seed

### Phase B — football-backend-saas（最小补丁）

- [ ] **Gateway oa 路由**（生产/Nacos 路径需要；本地脚本 overlay 有直连 fallback，仍建议保留）：

文件：`football-backend-saas/football-gateway/src/main/resources/application.yaml`

在 `spring.cloud.gateway.server.webflux.routes` 追加：

```yaml
            ## oa-server 服务（Ops 运营数据平台）
            - id: oa-admin-api
              uri: grayLb://oa-server
              predicates:
                - Path=/admin-api/oa/**
              metadata:
                response-timeout: 300000
                connect-timeout: 10000
              filters:
                - RewritePath=/admin-api/oa/v3/api-docs, /v3/api-docs
```

并在 `knife4j.gateway.routes` 增加 `oa-server` 文档聚合项（见 [FOOTBALL-PROJECT-CHANGES §S0/S1](./FOOTBALL-PROJECT-CHANGES.md)）。

- [ ] **不要**依赖修改各微服务 `application-local.yaml` 里的 MySQL 为 `shenyudb` — Gate 启动脚本会通过 **overlay** 覆盖为 `localhost:3306/wd`（见 §4.1）。
- [ ] **首次 / 大改**构建 JAR：

```powershell
cd football-backend-saas
mvn -pl football-gateway,football-module-mp/football-module-mp-server,football-module-system/football-module-system-server -am package -DskipTests
```

### Phase C — football-front（Ops 挂载层）

- [ ] 安装依赖：

```powershell
cd football-front
pnpm install
cd ..
.\scripts\link-ops-deps.ps1   # echarts / xlsx / @tiptap 等 junction
```

- [ ] **批量挂载 Ops 页面**（从 `ops-platform-ui-vue` 复制到 `views/ops/*`）：

```powershell
python scripts/mount-ops-all.py
```

- [ ] **手工补丁**（mount 不会自动改的文件 — 对照 [FOOTBALL-PROJECT-CHANGES](./FOOTBALL-PROJECT-CHANGES.md)）：

| 文件 | 必需改动 |
|------|----------|
| `apps/web-ele/.env.development` | 见 §5.2 |
| `apps/web-ele/vite.config.mts` | proxy `target: http://localhost:48080`，timeout 300s |
| `apps/web-ele/src/bootstrap.ts` | `app.use(opsElementPlusPlugin)` |
| `apps/web-ele/src/plugins/ops-element-plus.ts` | 新增（全量 Element Plus 注册） |
| `apps/web-ele/src/styles/ops-theme.scss` | 新增（暗色主题；mount 会部分处理） |
| `apps/web-ele/src/router/routes/modules/dashboard.ts` | `Workspace` 改为**顶层**路由 `/workspace`（S4-fix1） |
| `apps/web-ele/src/router/routes/modules/ops.ts` | 由 `mount-ops-all.py` 生成 |
| `apps/web-ele/src/components/ops/Pagination.vue` | 确保 `import { computed } from 'vue'`（remount 易丢） |

- [ ] remount 后可选修复：

```powershell
python scripts/fix-ops-theme.py      # 硬编码色 → var(--el-*)
python scripts/fix-ops-imports.py    # @/ → #/ 遗漏
python scripts/fix-ops-templates.py  # 根 <template> 回归
```

### Phase D — 数据库 seed（localhost wd）

按顺序（**仅 localhost**；远程 101.37.161.136 已 Deferred）：

| 步骤 | 命令 / 脚本 | 说明 |
|------|-------------|------|
| 1 | oa-server 首次启动 | Flyway 建表 |
| 2 | `python scripts/integration-config/apply-seed-oa-menu.py` | `system_menu` + `system_role_menu`（UTF-8） |
| 3 | `python scripts/integration-config/apply-smoke-api-permissions.py` | admin 角色 oa:* 授权 |
| 4 | 按需 | `import-football-pay-tables.sql`、`patch-system-menu-user-type.sql` 等 |

> **注意**：PowerShell 管道导入中文 menu 会乱码 → **必须**用 Python/`mysql --default-character-set=utf8mb4`。

### Phase E — 启动与验证

```powershell
# 推荐一键（预检 Redis + MySQL + 重启全栈）
.\scripts\start-ops-dev.ps1

# 首次需 Maven 构建
.\scripts\start-ops-dev.ps1 -FirstRun

# 验收
python scripts/post-mdb-local-smoke.py
python scripts/verify-ops-pages-per-menu.py --api
```

**启动顺序**（`start-integration-all.ps1` 内部）：

1. Nacos `:8848`（Docker）
2. Redis `:6379`（密码 123456）
3. Push Nacos local configs
4. Gateway `:48080` + mp `:48086` + member mock `:48087` + system `:48081`
5. oa-server `:48094`
6. football-front `:5777`

---

## 4. 配置文件路径与精确设置

### 4.1 wd 侧运行时 overlay（**不写入 Football 仓库也可 Gate 启动**）

| 文件 | 注入对象 | 关键设置 |
|------|----------|----------|
| `scripts/integration-config/football-integration-overlay.yml` | system/mp/member JAR | `nacos.server-addr: 127.0.0.1:8848` · `discovery.namespace: local` · **master DS → `jdbc:mysql://localhost:3306/wd`** · **Redis password `123456`** · Feign → `:48081/:48087/:48082/:48086` · RocketMQ autoconfig exclude |
| `scripts/integration-config/gateway-integration-local.yaml` | gateway JAR | Nacos off + **直连路由** system/oa/member · Redis **123456** · `response-timeout: 300s` |
| `scripts/integration-config/*-server-local.yaml` | 推送到 Nacos namespace **`local`** | `push-integration-config-to-nacos.ps1` |

**启动参数示例**（脚本已封装）：

```text
java -jar football-gateway.jar \
  --spring.profiles.active=dev \
  --spring.config.additional-location=optional:file:scripts/integration-config/gateway-integration-local.yaml

java -jar football-module-system-server.jar \
  --spring.profiles.active=local,local-nacos \
  --spring.config.additional-location=optional:file:scripts/integration-config/football-integration-overlay.yml
```

### 4.2 oa-server（wd 主仓 — 勿被 Football 拷贝影响）

| 文件 | Profile | 关键设置 |
|------|---------|----------|
| `ops-platform-module-oa/.../application-dev-nacos.yml` | dev-nacos | `spring.application.name: oa-server` · **`server.port: 48094`** · 远程 Nacos `192.168.10.47:8848` namespace **`dev`** |
| `ops-platform-module-oa/.../application-dev-nacos-local.yml` | dev-nacos-local | **`server-addr: 127.0.0.1:8848`** · **`discovery.namespace: local`** · 无 nacos 凭证 |
| `ops-platform-module-oa/.../application-dev-local-multidb.yml` | dev-local-multidb | **五库** localhost · `oa.auth.football-redis.enabled: true` · Redis **123456** |

**集成 oa-server profiles**（`start-integration-all.ps1` 默认）：

```text
dev,dev-nacos,dev-nacos-local,dev-local-multidb
```

**Standalone**（`restart-all.ps1` / `start-ops-standalone.ps1`）仅用 **`dev`** → 端口 **8080**，单库远程 `101.37.161.136/wd`，**无 Nacos**。

### 4.3 football-backend-saas 内 yaml（upstream 默认 vs 集成）

| 文件 | upstream 典型值 | 集成注意 |
|------|-----------------|----------|
| `football-module-system/.../application-local.yaml` | `shenyudb` @ localhost | **Gate 被 overlay 覆盖为 `wd`**；单独 `java -jar` 无 overlay 仍会连错库 |
| `football-module-system/.../application-local-nacos.yml` | 可能指向远程 `101.37.161.136/wd` | 可选保留；**overlay 优先** |
| `football-gateway/.../application-local.yaml` | Redis 无密码注释 | 集成需 **123456**（由 gateway-integration-local.yaml 覆盖） |

**MySQL 对照**

| 场景 | system-server master | oa-server |
|------|---------------------|-----------|
| Gate localhost | `localhost:3306/wd`（overlay） | 五库 multidb profile |
| Standalone Ops | — | `101.37.161.136:3306/wd`（`application-dev.yml`） |
| upstream Football 单机 | `localhost:3306/shenyudb` | — |

**shenyu-system vs wd**：Football 身份/OAuth 表在 **`shenyu-system`**；Ops 菜单 RBAC seed 在 **`wd.system_menu`**；oa-server 鉴权桥接两者（`FootballAuthProvider`）。

### 4.4 football-front

**`apps/web-ele/.env.development`**（精确值）：

```env
VITE_PORT=5777
VITE_BASE=/
VITE_BASE_URL=http://localhost:48080
VITE_GLOB_API_URL=/admin-api
VITE_ROUTER_HISTORY=hash
VITE_APP_DEFAULT_USERNAME=admin
VITE_APP_DEFAULT_PASSWORD=admin123
```

**`apps/web-ele/vite.config.mts`** proxy：

```typescript
proxy: {
  '/admin-api': {
    target: 'http://localhost:48080',
    changeOrigin: true,
    ws: true,
    timeout: 300_000,
    proxyTimeout: 300_000,
  },
},
```

**Ops 挂载目录**（`mount-ops-all.py` 产出）：

| 目录 | 说明 |
|------|------|
| `apps/web-ele/src/views/ops/**` | ~103 个 Vue 页 |
| `apps/web-ele/src/api/ops/**` | Ops API 客户端 |
| `apps/web-ele/src/components/ops/**` | 共用组件 |
| `apps/web-ele/src/router/routes/modules/ops.ts` | hide-in-menu 路由 |

Hash 路由示例：`http://localhost:5777/#/ops/operations/ip-group`

### 4.5 Nacos 本地

```powershell
.\scripts\start-nacos-local.ps1
# Console: http://127.0.0.1:8848/nacos  (nacos/nacos, auth disabled)
.\scripts\push-integration-config-to-nacos.ps1
```

namespace：**`local`**（与 `application-dev-nacos-local.yml` · overlay 一致）

### 4.6 Redis 本地

- 密码：**`123456`**（`football-integration-overlay.yml`、`gateway-integration-local.yaml`、`application-dev-local-multidb.yml` 一致）
- `start-ops-dev.ps1` → `Ensure-IntegrationRedis` 自动 `CONFIG SET requirepass` 或起 Docker `redis-integration-local`
- **勿**在 `stop-integration-all.ps1` 默认流程中杀 `:6379`（Windows redis-server 重启会丢密码 → Gateway 登录失败）

---

## 5. 启动脚本索引

| 脚本 | 用途 |
|------|------|
| **`scripts/start-ops-dev.ps1`** | **日常 Gate 一键**（Redis/MySQL 预检 → integration-all） |
| `scripts/start-integration-all.ps1` | 全栈 Nacos+Gateway+Football+oa+:5777 |
| `scripts/start-nacos-local.ps1` | 仅 Nacos Docker |
| `scripts/start-integration-oa.ps1` | 仅 oa-server :48094 |
| `scripts/start-football-system.ps1` | infra+mp+system（不含 Gateway 一键流程） |
| `scripts/start-integration-system.ps1` | mp+member+system Gateway 联调 |
| `scripts/restart-all.ps1` | **Standalone + collector**：:8000 + :8080 + :3000 |
| `scripts/start-ops-standalone.ps1` | Standalone Ops（无 Football） |
| `scripts/mount-ops-all.py` | Ops 前端批量挂载 |
| `scripts/link-ops-deps.ps1` | football-front 运行时依赖 junction |
| `scripts/stop-integration-all.ps1` | 停止集成栈 |

---

## 6. MonolithMockConfiguration 与本地 hack

| 项 | 说明 |
|----|------|
| `football-server/.../MonolithMockConfiguration.java` | **单体** `football-server` 启动用；**Gate 微服务路径不需要** |
| `football-server` 聚合启动 | ADR-047：**非集成目标**；集成走 Gateway + 分模块 JAR |
| `scripts/integration-config/mock-member-author-server.py` | **:48087** AuthorApi Feign 桩（Hybrid C，默认） |

拷贝 fresh 后：**丢弃** `MonolithMockConfiguration.java` 及 `football-server` 相关改动，使用 **member mock + overlay**。

---

## 7. 常见陷阱（来自现有文档）

| 现象 | 根因 | 修复 |
|------|------|------|
| UI「内部服务错误」/ 登录失败 | Gateway :48080 DOWN 或 Redis 无密码 | `.\scripts\start-ops-dev.ps1`；Redis 须 **123456** |
| Gateway `AUTH, but no password is set` | 本机 Redis 无密码占用 :6379 | `Ensure-IntegrationRedis` 或 Docker redis |
| Gateway 503 system-server | Nacos 未注册 / 服务未起 | 全栈脚本；查 `scripts/logs/*-integration.log` |
| Nacos namespace 不一致 | dev vs local 混用 | oa-server 用 **local**（`dev-nacos-local`）；Football 用 overlay **local** |
| 登录 code=500 | member :48087 不可用 / Redis 不一致 | 确认 mock 运行；`patch-system-menu-user-type.sql` |
| oa API 401 | Football token 未进 oa-server | `FootballAuthProvider` + Redis reader（oa-server 主仓，非 Football） |
| oa API 403 | `system_role_menu` 缺 admin 授权 | `apply-smoke-api-permissions.py` |
| 5777 Ops 页空白 / `computed is not defined` | remount 覆盖 Pagination | 补 Vue import；见 FOOTBALL-PROJECT-CHANGES S4-fix4 |
| 菜单中文 `????` | PowerShell 管道导入 SQL | `apply-seed-oa-menu.py` |
| Flyway checksum mismatch | 改了已执行 V*.sql | 测试库 repair；勿改历史 migration |
| `shenyudb` 连库错误 | 无 overlay 单独启 JAR | 必须用 `start-integration-all.ps1` 或手动加 overlay |
| Standalone vs Gate 数据不一致 | profile 混用 | Standalone=远程单库；Gate=localhost 五库 |
| remount 后主题/模板回归 | mount 覆盖手工 fix | `fix-ops-theme.py` / `fix-ops-templates.py` |

---

## 8. Git dirty 文件分类（当前工作区快照）

### 8.1 football-backend-saas — **建议保留 / 重新应用**

| 文件 | 类型 | 说明 |
|------|------|------|
| `football-gateway/.../application.yaml` | **集成必需** | `oa-admin-api` 路由 + knife4j |
| `football-gateway/.../application-local.yaml` | 可选 | Nacos local；可被 overlay 覆盖 |
| `football-module-*/.../application-local-nacos.yml` | 可选 | Nacos 注册模板；**overlay 优先** |
| `football-module-*/.../application-local.yaml` | 谨慎 | 若仅改 Redis/Nacos 可丢弃，靠 overlay |

### 8.2 football-backend-saas — **建议丢弃（拷贝 fresh 后不要带回）**

| 文件 | 原因 |
|------|------|
| `football-gateway/BOOT-INF/**` | 构建产物误提交 |
| `football-server/.../MonolithMockConfiguration.java` | 单体 hack；Gate 不用 |
| `football-module-system/.../*.java`（Auth/Permission/Member 等 **M 类**） | **违反 ADR-047** 业务逻辑改动；应用 mock+overlay 替代 |
| `football-module-*/ThreadPoolConfig.java` 等零散 Java | 非集成 SSOT；upstream 为准 |
| `local-changes.patch` / `local-changes-new.patch` / `startup-error.txt` | 本地调试垃圾 |

### 8.3 football-front — **建议保留 / 重新应用（集成层）**

| 路径 | 说明 |
|------|------|
| `apps/web-ele/.env.development` | Gateway URL + hash 路由 |
| `apps/web-ele/vite.config.mts` | proxy → :48080 |
| `apps/web-ele/src/bootstrap.ts` | opsElementPlusPlugin |
| `apps/web-ele/src/plugins/ops-element-plus.ts` | **新增** |
| `apps/web-ele/src/styles/ops-theme.scss` | **新增** |
| `apps/web-ele/src/router/routes/modules/dashboard.ts` | Workspace 顶层路由 |
| `apps/web-ele/src/router/routes/modules/ops.ts` | hide 路由（可 regen） |
| `apps/web-ele/src/views/ops/**` | **mount-ops-all.py 可 regen** |
| `apps/web-ele/src/api/ops/**` | 同上 |
| `apps/web-ele/src/components/ops/**` | 同上 |
| `apps/web-ele/src/types/ops/**` · `utils/ops/**` · `constants/ops/**` | 同上 |

### 8.4 football-front — **建议丢弃或还原 upstream**

| 路径 | 原因 |
|------|------|
| `playground/**` 大量 **D** | 疑似误删；fresh copy 应恢复 upstream |
| `scripts/clean.mjs`、`scripts/vsh/**`、`scripts/turbo-run/**` 等 **D** | 误删 monorepo 工具；还原 |
| `apps/web-ele/src/views/system/user/data.ts` 等 **M** | 本地集成实验（作者下拉走 oa）；**非 Gate SSOT**；按需 cherry-pick |
| `pnpm-workspace.yaml` **M** | 核对 upstream；无 Ops 需求则还原 |

### 8.5 集成层恢复优先级

若拷贝 fresh 后时间有限，**最小可运行集**：

1. Gateway `application.yaml` oa 路由  
2. `python scripts/mount-ops-all.py` + `.env.development` + `vite.config.mts` + `bootstrap.ts` + `ops-element-plus.ts`  
3. `.\scripts\link-ops-deps.ps1`  
4. MySQL 五库 + seed menu  
5. `.\scripts\start-ops-dev.ps1 -FirstRun`

---

## 9. 相关文档

| 文档 | 用途 |
|------|------|
| [INTEGRATION-S0-Football-Ops](./INTEGRATION-S0-Football-Ops.md) | S0 环境矩阵 |
| [INTEGRATION-PROGRESS](./INTEGRATION-PROGRESS.md) | 进度与历史修复 |
| [OPS-STARTUP-MATRIX](./OPS-STARTUP-MATRIX.md) | Standalone vs Integration |
| [OPS-DEV-DEPLOY-GUIDE](./OPS-DEV-DEPLOY-GUIDE.md) | 部署与 FAQ |
| [FOOTBALL-PROJECT-CHANGES](./FOOTBALL-PROJECT-CHANGES.md) | Football 侧变更明细 |
| [OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN](./OPS-FOOTBALL-MULTI-DB-EXECUTION-PLAN.md) | 五库 MDB Gate |
| [ADR-047](../adr/ADR-047-Football-Ops平台集成决策.md) | 集成决策 |
| [ADR-050](../adr/ADR-050-Ops与Football多库复用总纲.md) | 多库总纲 |

---

## 10. 快速检查清单（拷贝后 10 分钟）

- [ ] `ops-platform-server`、`ops-platform-ui-vue` 未被覆盖  
- [ ] MySQL 五库存在；四库有 Football 基线数据  
- [ ] Gateway `application.yaml` 含 `oa-admin-api`  
- [ ] `mount-ops-all.py` 已跑；`.env.development` / `vite.config.mts` / `bootstrap.ts` 已补丁  
- [ ] `link-ops-deps.ps1` 已跑  
- [ ] `apply-seed-oa-menu.py` 已跑（localhost wd）  
- [ ] `start-ops-dev.ps1` 健康表全绿  
- [ ] http://localhost:5777 登录 admin/admin123  
- [ ] `#/ops/operations/ip-group` 可打开且 API code=0
