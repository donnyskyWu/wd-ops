# OPS / Football Beta 测试环境连接

> **范围**：仅测试环境（`110.42.49.224`）。密码与密钥只写在本地 `scripts/integration-config/ops-test-remote.env`（gitignore），仓库只保留 `.env.example` 占位符。
>
> Football 产品仓库工作分支见 [FOOTBALL-OPS-BRANCH.md](./FOOTBALL-OPS-BRANCH.md)（**`ops`**，勿用 master）。

## 默认 vs Beta（必读）

| 模式 | 何时用 | 怎么启 |
|------|--------|--------|
| **默认 = 本地** | 日常开发 / Gate | `.\scripts\start-ops-dev.ps1` → ops-server localhost（master→**`shenyu-ops`** + Football 四库 root/root）+ `football-integration-overlay.yml`（127.0.0.1）+ Redis 本机 123456 |
| **Beta = 显式 opt-in** | 连测试机 `110.42.49.224` | `.\scripts\start-ops-dev.ps1 -Beta`（或 `-TestRemote`）；OPS master 仍为 **`shenyu-ops`**（远程；本任务不改） |

**不要**改默认启动脚本或把 beta overlay 设成 `start-ops-dev` / `start-integration-all` 的唯一路径。Beta 配置文件保留作可选，日常连回本地只需跑默认脚本（无需删 beta 文件）。

### 一键 Beta（推荐）

```powershell
# 需已填写 scripts/integration-config/ops-test-remote.env
.\scripts\start-ops-dev.ps1 -Beta
# 等价: .\scripts\start-ops-dev.ps1 -TestRemote
# 或:   .\scripts\start-integration-all.ps1 -Restart -SkipBuild -Beta
```

脚本会：加载 `ops-test-remote.env` → Football 用 `*-overlay-beta.yml` → Gateway 用 `gateway-integration-beta.yaml`（本地直连路由 + 远程 Redis）→ oa 追加 profile `dev-test-beta`。进程仍监听本机端口；vite `:5777` → Gateway `:48080` 不变。

## MySQL（3306）

> **库名 SSOT**：本地与 Beta OPS master 物理名均为 **`shenyu-ops`**（2026-08-01 本地自 `football-ops` 复制；`football-ops` / 历史 `wd` 可留备份）。Beta 主机仍为 `110.42.49.224`（下表）。

| 用途 | Database | User | Password |
|------|----------|------|----------|
| OPS master（原 wd；Beta） | `shenyu-ops` | `shenyu-ops` | 见 env |
| system（用户 SSOT） | `shenyu-system` | `shenyu-system` | 见 env |
| member | `shenyu-member` | `shenyu-member` | 见 env |
| mp | `shenyu-mp` | `shenyu-mp` | 见 env |
| pay | `shenyu-pay` | `shenyu-pay` | 见 env |
| bpm（Football 服务；OPS oa-server 当前无 bpm DS） | `shenyu-bpm` | `shenyu-bpm` | 见 env |

- Host：`110.42.49.224`
- Port：`3306`
- Server：MySQL **5.7.x**（从本机 MySQL 8 导入时注意 collation：`utf8mb4_0900_*` → `utf8mb4_general_ci`）

## Nacos

| 项 | 值 |
|----|-----|
| server-addr | `110.42.49.224:8848` |
| username | `nacos` |
| password | 见 env |
| discovery.namespace / config.namespace | `beta` |
| discovery.group / config.group | `DEFAULT_GROUP` |

## Redis

| 项 | 值 |
|----|-----|
| host | `110.42.49.224` |
| port | `6379` |
| database | `1` |
| password | 见 env |

## 配置文件

| 文件 | 用途 | 是否提交 |
|------|------|----------|
| `scripts/integration-config/ops-test-remote.env.example` | 占位模板 | 是 |
| `scripts/integration-config/ops-test-remote.env` | 真实凭据 | **否**（gitignore） |
| `scripts/integration-config/ops-test-beta-multidb.yml` | OPS multidb + Nacos + Redis overlay | 是（无密码） |
| `ops-platform-server/.../application-dev-test-beta.yml` | 可选 Spring profile `dev-test-beta` | 是（密码走 env） |
| `scripts/integration-config/football-integration-overlay-beta.yml` | Football 共用 beta overlay | 是 |
| `scripts/integration-config/{member,mp,pay,bpm}-integration-overlay-beta.yml` | 各服务 master DS | 是 |
| `scripts/integration-config/wd-test-remote.env(.example)` | **兼容旧名**（仅 master）；新工作请用 `ops-test-remote.env` | example 可提交 |

**默认本地**仍用 `application-dev-local-multidb.yml` + `football-integration-overlay.yml`（localhost）。**不要**把 beta 配成 `start-ops-dev` 默认 profile；beta 仅通过 `ops-test-remote.env` + `dev-test-beta` / `*-beta.yml` 显式启用。

## 如何启用（OPS oa-server）

**推荐**：`.\scripts\start-ops-dev.ps1 -Beta`（自动加载 env + overlays + `dev-test-beta`）。

手动（调试单服务）时：

1. 复制并填写凭据（若尚无本地 env）：

```powershell
Copy-Item scripts\integration-config\ops-test-remote.env.example scripts\integration-config\ops-test-remote.env
# 编辑 ops-test-remote.env 填入真实密码
```

2. 将 env 加载到当前 PowerShell 会话：

```powershell
Get-Content scripts\integration-config\ops-test-remote.env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $k, $v = $_.Split('=', 2)
  Set-Item -Path "env:$($k.Trim())" -Value $v.Trim()
}
```

3. 在现有本地 profiles **之后**追加 `dev-test-beta`（覆盖五库 + Redis/Nacos 指向 beta）：

```text
-Dspring-boot.run.profiles=dev,dev-nacos,dev-nacos-local,dev-local-multidb,dev-test-beta
```

或仅追加外部 overlay（不改 jar 内 profile）：

```text
--spring.config.additional-location=optional:file:./scripts/integration-config/ops-test-beta-multidb.yml
```

仅改 master 时仍可用旧 profile `dev-test-remote-wd`（兼容）。

## Football 服务（可选）

在 `ops` 分支上启动时，先加载 `ops-test-remote.env`，再叠加：

- 共用：`football-integration-overlay-beta.yml`（system + Nacos beta + Redis）
- 按服务：`member|mp|pay|bpm-integration-overlay-beta.yml`

本地日常仍用 `football-integration-overlay.yml`（127.0.0.1）。

## 测试库初始化（菜单 / 字典 / 角色）

测试机 `shenyu-system` 默认**不含** OPS 菜单块（6100–6999）。首次或菜单缺失时，在仓库根目录执行：

```powershell
# 需已填写 scripts/integration-config/ops-test-remote.env
.\scripts\integration-config\seed-ops-test-remote.ps1
# 仅验证计数：
.\scripts\integration-config\seed-ops-test-remote.ps1 -VerifyOnly
```

脚本会（幂等）：

| 步骤 | 脚本 | 目标库 | 内容 |
|------|------|--------|------|
| 1 | `apply-seed-oa-menu.py` → `seed-oa-system-menu.sql` | shenyu-system | OPS 菜单 6100–6999 + super_admin role_menu（**须 utf8mb4 stdin**，勿用 PowerShell 管道直灌） |
| 2 | `seed-ops-test-remote-shenyu-system-menus.sql` | shenyu-system | V159/V162 采集路径、6175 全部任务、ip_group_leader 角色、移除 6137–6139/6155 |
| 3 | `seed-ops-test-remote-dict.py` | shenyu-ops → shenyu-system | 从 `sys_dict_*` 同步 97+ 业务字典（对齐 V152/V158/V161） |
| 4 | `seed-ops-six-roles-rbac.sql` | shenyu-system | ADR-064 六业务角色 + role_menu（160–165） |
| 5 | `seed-ops-six-roles-test-users.sql` | shenyu-system | ADR-064 六角色可测用户 + user_role（幂等） |

**证据**：`docs/delivery/OPS-TEST-SEED-RUNLOG.md`（最近一次 seed 后的 COUNT）。

### ADR-064 六角色测试账号（tenant=1）

> 用户名须匹配 Football `AuthLoginReqVO`：`^[A-Za-z0-9]+$`（**禁止下划线**）。密码与 Dev 惯例一致：`admin123`（BCrypt 同 admin）。

| username | password | role code | role name | user id | 说明 |
|----------|----------|-----------|-----------|---------|------|
| `opsleader` | `admin123` | `ip_group_leader` | IP组长 | 9160 | Beta 原先无角色绑定；按例外新建 |
| `opsmanager` | `admin123` | `ops_manager` | 运营主管 | 9161 | seed 新建 |
| `opsfinance` | `admin123` | `finance` | 财务人员 | 9162 | seed 新建 |
| `opseditor` | `admin123` | `content_editor` | 内容编辑 | 9163 | seed 新建 |
| `opsoperator` | `admin123` | `ops_operator` | 运营 | 9164 | seed 新建 |
| `opsanalyst` | `admin123` | `data_analyst` | 数据分析 | 9165 | seed 新建 |
| `admin` | `admin123` | `super_admin` | 系统管理员 | （已有） | **不**由本 seed 创建 |

```powershell
# Beta
python scripts/integration-config/apply-seed-oa-menu.py `
  --host $env:OPS_TEST_DB_HOST --user shenyu-system --password <见 env> --database shenyu-system `
  --seed scripts/integration-config/seed-ops-six-roles-test-users.sql

# Local（若本机已有 roles 160–165）
python scripts/integration-config/apply-seed-oa-menu.py `
  --host localhost --user root --password root --database shenyu-system `
  --seed scripts/integration-config/seed-ops-six-roles-test-users.sql
```

证据目录：`docs/delivery/e2e-artifacts/OPS-SIX-ROLES-USERS-20260802/`。

**期望验证**（2026-07-25 已执行）：

- `system_menu` id 6100–6999：`71` 行
- `system_role_menu` menu_id 6100–6999：`71` 行
- `system_dict_type` type like `dict_%`：`97` 行
- `system_dict_data`：`387` 行
- `system_role.code=ip_group_leader`：`1` 行

> 测试库 MySQL 用户**无跨库 GRANT**；字典同步用 Python 双连接，不用 Flyway 式 ``INSERT INTO `shenyu-system`.… SELECT … FROM shenyu-ops``。

### 菜单 / 角色中文乱码（charset）

**根因**：seed 未以 `utf8mb4` 写入 `shenyu-system`（常见经 PowerShell 管道灌 SQL），中文被 MySQL 落成字面量 `?`（`HEX(name)=3F3F…`），与 V157 AI 提示词同类问题。属 **seed 数据错误**，非 Football 读库或 JDBC 问题。

**角色样例（2026-08-02）**：`system_role.code=ip_group_leader` 曾显示 `IP??`（`HEX=49503F3F`）。已在 Beta `shenyu-system` 直接 UPDATE 为「IP组长」（`HEX=4950E7BB84E995BF`）；`seed-ops-test-remote-shenyu-system-menus.sql` / `seed-ip-group-leader-role.sql` 增加幂等 repair；`seed-ops-test-remote.ps1` 改走 Python utf8mb4 stdin。Beta `shenyu-ops.system_role` 该行本就正确，无需改。

**修复**（任选其一，幂等）：

```powershell
# 推荐：完整重灌菜单（Python stdin，避免 PowerShell 管道乱码）
python scripts/integration-config/apply-seed-oa-menu.py `
  --host 110.42.49.224 --user shenyu-system --password <见 env> --database shenyu-system

# 或一键 seed（已改为内部调用 apply-seed-oa-menu.py）
.\scripts\integration-config\seed-ops-test-remote.ps1

# 仅验证样例菜单中文
.\scripts\integration-config\seed-ops-test-remote.ps1 -VerifyOnly
# 期望 sample_menu：6100 运营数据、6117 内容管理、6159 IP组管理
```

oa-server Flyway **V164** 会在 `shenyu-system` 上对仍为 `?` 的 OPS 菜单名做条件 UPDATE（与 V157 同模式）。修复后请 **重新登录** Beta（`:5777`）刷新侧栏缓存。

## shenyu-ops 冗余清理（V163）

清单：`docs/delivery/OPS-TEST-CLEANUP-INVENTORY.md`  
Flyway：`V163__drop_shenyu_ops_redundant_tables.sql`

已在测试库手工执行 V163（2026-07-25）。删除：备份表、football_demo*、master 内重复的 system_dict/mail/sms/social/notify/log/dept/tenant 等。  
**保留** master overlay：`system_users`、`system_role`、`system_menu`、`system_user_role`、`system_oauth2_access_token`。

oa-server 下次启动仍会跑 Flyway V161–V163（DROP IF EXISTS 幂等）。

## Flyway 与 MySQL 5.7（Beta 必读）

**现象**：`-Beta` 启动后 `oa-server :48094 DOWN`；日志 `oa-server-nacos-run.log` 含：

```text
FlywayEditionUpgradeRequiredException: MySQL 5.7 is no longer supported by Flyway Community Edition
```

**根因**：测试机 MySQL **5.7.x**；Spring Boot 3.2 默认 Flyway **9.x** Community 已移除 5.7 支持。oa-server 起不来 → Gateway 转发 `/admin-api/oa/*` 失败 → 内容管理等页「数据加载失败，请重试，系统错误」。

**修复**（仓库已 pin）：`ops-platform-module-oa/pom.xml` 中 `<flyway.version>10.22.0</flyway.version>`（Flyway 10+ Community 恢复 MySQL 5.7；9.x/8.x 会抛 `FlywayEditionUpgradeRequiredException`）。改 pom 后须 **重新编译** oa-server：

```powershell
cd ops-platform-server\ops-platform-module-oa
mvn -q -DskipTests package
# 若 Flyway 曾失败卡住，先清理失败记录：
python scripts/integration-config/repair-flyway-failed.py
# 再
.\scripts\start-ops-dev.ps1 -Beta
```

**为何反复出现**：切 `-Beta` 连远程 5.7 时若未 rebuild oa-server（或 Flyway 被升回 9.x），每次启动都会在 Flyway 阶段崩溃；UI 侧错误文案相同，易误判为字典/权限问题。

## 安全

- 测试口令只放本地 env；**不要**提交到 git，也不要把真实密码写进可提交的 markdown。
- 轮换口令后只更新本地 `ops-test-remote.env`。
