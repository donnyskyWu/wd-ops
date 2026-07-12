# Ops × Football 多库复用 — 主执行计划

> **性质**：多库集成程序 SSOT（Single Source of Truth）  
> **版本**：v1.0 | 2026-07-05  
> **依据**：[AI驱动产品开发方法论-产品经理指南.md](../../AI驱动产品开发方法论-产品经理指南.md) · [PHASE-DEV-METHOD.md](../engineering/PHASE-DEV-METHOD.md) · [ADR-050](../adr/ADR-050-Ops与Football多库复用总纲.md) · [ADR-051](../adr/ADR-051-Ops与Football多库复用-作者域.md) · [OPS-FOOTBALL-MULTI-DB-REUSE-ANALYSIS](./OPS-FOOTBALL-MULTI-DB-REUSE-ANALYSIS.md)  
> **用户确认**：2026-07-05 — localhost TRUNCATE · IP 组 skeleton · ADR-050 Accepted · ADR-051 修订

---

## 0. 执行方法论

### 0.1 质量保障组合（来自方法论 §6 + §8）

```
规格驱动分片（Slice）
  + PDCA 每阶段 Gate
  + 选择性 TDD（双写/Saga/权限）
  + 集成修复模式库（§0.3）
```

**推荐默认**：**B（分片）+ C（核心逻辑 TDD）+ PDCA（每 Gate 验收）** — 禁止裸用「一次性直给」。

### 0.2 三轮循环（每阶段 Gate）

| 轮次 | 动作 | 产出 |
|------|------|------|
| **① 规划** | 读 Spec/ADR → 原子任务清单 → 前置 Gate checklist | 任务勾选表 |
| **② 执行** | 实现 + 脚本/SQL → 跑自动化 | 代码/配置/SQL |
| **③ 验收** | **Football :5777 登录 + Ops 菜单操作** · 58 路由 E2E · 阶段人工场景 ·（辅助）curl/SQL | Gate 报告 |

**不通过 → 修复迭代循环**（Act）：

```
FAIL → 记入阻塞表 → 根因分析 → 限定范围 fix → 重跑本 Gate 全量（禁止带伤进下一阶段）
```

### 0.3 集成修复模式库（历史沉淀）

| 类别 | 现象 | 修复模式 | 证据 |
|------|------|----------|------|
| 挂载 | Vue 组件 404 / 空白 | `mount-ops-all.py` 重跑；检查 `@/` import 路径 | INTEGRATION-PROGRESS §11 |
| CSS | EP 组件样式缺失 | `link-ops-deps.ps1` + Vite deps 重建 | verify-ops-vite-modules |
| Gateway | MalformedInputException | UTF-8 无 BOM yaml；Nacos 重发 | INTEGRATION-PROGRESS §9 |
| Gateway | 504 / 超时 | 增 `spring.cloud.gateway.httpclient.response-timeout` | gateway-integration-local.yaml |
| 权限 | 403 dict/list | `FootballAuthProvider.mergeOaPermissions` + seed role_menu | INTEGRATION-PROGRESS §19 |
| 登录 | 500 member Feign | Redis 密码统一 + patch-system-menu-user-type.sql | INTEGRATION-PROGRESS §9 |
| E2E | 58 路由 FAIL | per-menu `verify-ops-pages-per-menu.py` 定位 → 单页 fix | UAT 20260704 |
| 数据源 | 单库假设 | `@DS("member"|"mp"|"pay"|"system")` + dev-local-multidb profile | ADR-050 §4 |
| Flyway | V130→V131 PK 变更 | TRUNCATE ext → ALTER PK | V131 migration |

### 0.4 测试工具矩阵

| 脚本 | 用途 | S0 期望 | S1+ 期望 |
|------|------|---------|----------|
| `scripts/verify-ops-pages-per-menu.py` | Football :5777 逐菜单探针 | — | **58/58 PASS** |
| `scripts/verify-ops-pages.py` | Vue/API 静态检查 | 103/103 vue · 90/90 vite | 同左 |
| `scripts/run-uat-football-e2e.ps1` | Playwright 登录链 + P0 | stack UP 时 `@uat-football` PASS | 全绿 |
| `scripts/uat-football-e2e-report.py` | E2E 报告 JSON | probe 写入 docs/delivery | 同左 |
| API curl | `@DS` smoke / 业务 API | **辅助**（不可单独 PASS） | **辅助**（不可单独 PASS） |
| `mvn verify` | 后端 IT | Flyway V131 绿 | 模块 IT 100% |
| Row count SQL | TRUNCATE/seed 验证 | 见 §S0 | ext 覆盖率 |

### 0.5 硬约束

| 约束 | 说明 |
|------|------|
| **仅 localhost TRUNCATE** | `localhost:3306/wd` · root/root；**禁止** touch 101.37.161.136 |
| **不改 Football 业务代码与逻辑** | ADR-050 §3.1：改造仅限 `oa-server` + `wd` + `football-front` 挂载层；**禁止**改 member/mp/pay/system-server 业务逻辑；gateway 集成基建（路由/超时）**已冻结**；读 Football 用 `@DS` 只读或 Feign 既有 API |
| **Spec 未写明 → 停止** | 输出阻塞清单，禁止推断 |
| **Gate 顺序** | GATE-MDB-S0 → S1 → S2 → S3 → S4，禁止跳阶段 |
| **Football UI 验收** | 见 **§0.6**；`:3000` standalone **非 Gate** |

### 0.6 验收总则（强制 Gate）

> **原则**：GATE-MDB-S0～S4 **必须**经 **Football 集成全栈 UI**（`:5777`）登录后进入 Ops 菜单实际操作；符合预期方可签 Gate。  
> **禁止**：仅凭 curl/API probe、row count SQL、或 standalone `ops-platform-ui-vue :3000` 判定 PASS。

#### 标准验收流程

| 步骤 | 动作 |
|------|------|
| 1 | `.\scripts\start-integration-all.ps1`（或 `-SkipBuild` 复跑） |
| 2 | 浏览器打开 `http://localhost:5777` · 登录 **admin / admin123** · 租户 **1** |
| 3 | 经 Football 侧栏进入 Ops · hash 路由 `#/ops/...` |
| 4 | 执行本阶段 **人工场景清单**（下表） |
| 5 | 回归基线：`.\scripts\run-uat-football-e2e.ps1` → Playwright `uat-football-ops-login.spec.ts`（`@uat-football`）**58/58 PASS** |
| 6 | 可选辅助：`verify-ops-pages-per-menu.py` · API curl · row count SQL（**不得单独作为 Gate PASS 依据**） |

#### 工具定位

| 工具 | 角色 |
|------|------|
| `start-integration-all.ps1` | 验收环境 **唯一** 启动入口 |
| `run-uat-football-e2e.ps1` + `uat-football-ops-login.spec.ts` | **58 路由**回归基线（`oa-menu-permission-map.csv`） |
| `verify-ops-pages-per-menu.py` | 58 路由探针（与 Playwright 互补） |
| API curl / `@DS` smoke / row count SQL | **辅助诊断** |
| `start-ops-standalone.ps1` · `:3000` | **开发参考**，**非** Gate 验收路径 |

#### 各阶段 Football UI 人工场景（Gate 必做 subset）

| 阶段 | 必做人工场景（`:5777` 登录后） | 58 路由基线 |
|------|-------------------------------|-------------|
| **S0** | 登录成功 · 首页 `#/ops/dashboard` 可渲染 · 作者/内容/账号页不因 TRUNCATE 白屏 | **58/58** |
| **S1** | `#/ops/author` 列表 ≥35 · **新建作者**（member+ext）· `#/ops/internal-account` 微信列表 ≥187 · **公号 sync** 双写可见 | **58/58** |
| **S2** | `#/ops/ip-group` tree（skeleton+新建）· 非微信账号 CRUD · `#/ops/system-dict` 平台字典可读 · M4 选择器 5/5 | **58/58** |
| **S3** | `#/ops/system-log/login` ≥3000 · `#/ops/system-log/operation` ≥600 · `#/ops/collect/task` bind 字段 · 订单只读页 | **58/58** + spotcheck |
| **S4** | S0–S3 场景复验 · cutover 后作者页仍读 member · 全菜单无 500/空白 | **58/58** + `mvn verify` |

> 报告模板：[UAT-FOOTBALL-E2E-20260704.md](./UAT-FOOTBALL-E2E-20260704.md) · Gate 归档 `docs/delivery/gates/GATE-MDB-S{n}-报告-{YYYYMMDD}.md`

---

## S0 — 基建 + 数据清理

### 目标

localhost 五库就绪；wd 测试数据 TRUNCATE；V131 迁移；ADR 签字；多数据源 profile 骨架。

### 范围

- TRUNCATE wd B/C 组（§N.8）
- IP 组 skeleton（1 大组 + 2 小组）
- Flyway V131
- ADR-050 Accepted + ADR-051 修订
- `dev-local-multidb` profile 草案

### 不做什么

- 不改远程 DB
- 不改 AuthorService / PlatformAccountSyncService 业务逻辑（S1）
- 不 DROP `oa_author` 表结构（S4）
- 不改 Football 四库 seed
- **不改 Football 业务代码**（member/mp/pay/system-server）；gateway 仅沿用已冻结集成配置

### 前置 Gate（启动前 checklist）

- [x] localhost:3306 可连（root/root）
- [x] 五库存在：wd + shenyu-member/mp/pay/system
- [x] 四库 seed 就绪（35 作者 / 187 公号 / pay 17.8 万 / system 字典）
- [x] Flyway V130 已应用（local）
- [x] 用户书面确认 TRUNCATE（2026-07-05）
- [ ] **mysqldump 备份 wd**（见回滚）

### 任务清单（有序）

| # | 任务 | 产出 |
|---|------|------|
| S0-01 | mysqldump 备份 wd | `backup/wd-pre-mdb-s0-{date}.sql` |
| S0-02 | 执行 `s0-wd-truncate-testdata.sql` | wd B/C 组空表 |
| S0-03 | 执行 `s0-wd-ip-group-skeleton.sql` | oa_ip_group = 3 行 |
| S0-04 | 合入并跑 Flyway V131 | ext PK + oa_account_ext |
| S0-05 | ADR-050 Accepted + ADR-051 修订 | docs/adr/ |
| S0-06 | 创建 `application-dev-local-multidb.yml` 草案 | oa-server 五 DS |
| S0-07 | `@DS` smoke IT（member/mp/pay/system 各 SELECT 1） | 4/4 PASS |
| S0-08 | 更新 ANALYSIS + MASTER §19 | 文档链接 |
| S0-09 | 归档 GATE-MDB-S0 报告 | gates/ |

### 验收标准（可度量）

> **Gate 路径**：§0.6 Football UI；下表 SQL/curl 为 **辅助**，不可替代 :5777 签收。

| ID | 检查项 | 命令 / 期望 |
|----|--------|-------------|
| AC-S0-01 | TRUNCATE 后 oa_content | `SELECT COUNT(*) FROM wd.oa_content` → **0** |
| AC-S0-02 | TRUNCATE 后 oa_author | `SELECT COUNT(*) FROM wd.oa_author` → **0** |
| AC-S0-03 | TRUNCATE 后 oa_account | `SELECT COUNT(*) FROM wd.oa_account` → **0** |
| AC-S0-04 | IP 组 skeleton | `SELECT COUNT(*) FROM wd.oa_ip_group` → **3**（1 大 + 2 小） |
| AC-S0-05 | KEEP 表未动 | `sys_dict_type` ≥ 90 · `sys_param` ≥ 10 · `flyway_schema_history` 含 V131 |
| AC-S0-06 | Football 四库未动 | member.author_user=**35** · mp.mp_account=**187** |
| AC-S0-07 | V131 结构 | `oa_author_ext` PK = `author_user_id`；`oa_account_ext` 存在 |
| AC-S0-08 | author_id COMMENT | `SHOW FULL COLUMNS FROM oa_content LIKE 'author_id'` 含 ADR-050/051 |
| AC-S0-09 | **无 Football 业务代码 diff** | `football-backend-saas/**/{member-server,mp-server,pay-server,system-server}/**` 无本阶段提交 |
| **AC-S0-10** | **Football 全栈 UP** | `start-integration-all.ps1` · :5777 / :48080 可访问 |
| **AC-S0-11** | **Football 登录链** | :5777 · admin/admin123 · tenant **1** · 进入 Ops 侧栏 |
| **AC-S0-12** | **58 路由回归基线** | `run-uat-football-e2e.ps1` 或 `verify-ops-pages-per-menu.py` → **58/58 PASS** |
| **AC-S0-13** | **S0 人工场景** | `#/ops/dashboard` · `#/ops/author` · `#/ops/internal-content` 可打开无白屏/500 |

### 测试方法

| 层级 | 脚本 | 期望 |
|------|------|------|
| **Football E2E（Gate）** | `run-uat-football-e2e.ps1` · `verify-ops-pages-per-menu.py` | **58/58** + §0.6 S0 人工场景 |
| SQL | 上表 row count | 8/8 PASS（辅助） |
| Flyway | `mvn -pl ops-platform-module-oa flyway:migrate`（或启动 oa-server） | V131 SUCCESS（辅助） |
| Smoke | `@DS` 连通 curl/IT | 4 DS PASS（辅助） |
| ~~standalone~~ | ~~`:3000` verify-ops-pages.py~~ | **非 Gate** |

### 修复迭代循环

| FAIL 场景 | 根因方向 | 修复 |
|-----------|----------|------|
| TRUNCATE FK 错误 | 漏禁 FK_CHECKS | 脚本已 SET FOREIGN_KEY_CHECKS=0 |
| IP 组 INSERT 冲突 | TRUNCATE 未跑 | 先 S0-02 再 S0-03 |
| V131 ALTER 失败 | V130 有 id PK | TRUNCATE ext 后 DROP id |
| oa-server 启动失败 | DS 配置错误 | 回退 profile 至单库 |

### 回滚策略

```powershell
# 执行 TRUNCATE 前（仅 localhost）
mysqldump -h 127.0.0.1 -P 3306 -u root -proot wd > backup/wd-pre-mdb-s0-20260705.sql

# 回滚
mysql -h 127.0.0.1 -P 3306 -u root -proot wd < backup/wd-pre-mdb-s0-20260705.sql
```

### 文档 / ADR 更新点

- ADR-050 Accepted
- ADR-051 修订（PK / 无 PENDING_MAP）
- ANALYSIS §I Q10/Q11 → ✅
- MASTER §19 GATE-MDB-S0

---

## S1 — 作者 + 微信公号

### 目标

AuthorService 读 member + ext join；微信公号读 mp + oa_account_ext；新建作者无 oa_author。

### 范围

- `AuthorServiceImpl` 多库改造
- `PlatformAccountSyncService` 微信双写
- `OaAuthorExtDO` PK 字段对齐 V131
- `oa_content.author_id` 解析层切换

### 不做什么

- 非微信 oa_account CRUD（S2）
- 日志/消息切 Football（S3）
- DROP oa_author（S4）
- **不改 Football 业务代码**；作者写 member 库由 `oa-server` `@DS` / sync 完成，**不**改 member-server

### 前置 Gate

- [x] **GATE-MDB-S0** 🟡（localhost 已执行；正式签收待补）
- [x] V131 已应用（Flyway @ v131）
- [x] `dev-local-multidb` profile 四 DS smoke 绿（`MultidbDsSmokeIT`）
- [x] IP 组 skeleton 存在（ext.ip_group_id 有目标）

### 任务清单

| # | 任务 |
|---|------|
| S1-01 | `OaAuthorExtDO`：`authorUserId` 作 `@TableId` |
| S1-02 | `AuthorServiceImpl`：`@DS("member")` 列表 + ext join |
| S1-03 | 新建作者：member INSERT + ext INSERT（无 oa_author） |
| S1-04 | `OaAccountExtDO` + Mapper |
| S1-05 | `PlatformAccountSyncService`：mp + ext 双写 |
| S1-06 | Content/Task author_id 解析改为 author_user_id |
| S1-07 | IT：AuthorServiceTest · AccountExtTest |
| S1-08 | UAT：作者列表 35 · 微信公号 187 |

### 验收标准

> **Gate 路径**：§0.6 · :5777 登录 → Ops 菜单操作；curl 仅辅助。

| ID | 期望 |
|----|------|
| **AC-S1-UI-01** | :5777 登录 admin/admin123 tenant 1 → Ops 侧栏可见 |
| **AC-S1-UI-02** | `#/ops/author` 列表 total **≥ 35**（member SSOT） |
| **AC-S1-UI-03** | **UI 新建作者**：保存后列表可见 · ext 行 PK = author_user_id（可 SQL 辅助核对） |
| **AC-S1-UI-04** | `#/ops/internal-account` 微信列表 **≥ 187** |
| **AC-S1-UI-05** | **UI 微信公号 sync**：mp_account + oa_account_ext 双写 · sync_status=SYNCED |
| **AC-S1-UI-06** | `run-uat-football-e2e.ps1` · `uat-football-ops-login.spec.ts` → **58/58 PASS** |
| AC-S1-07 | **无 Football 业务代码 diff** | member/mp/system-server 无本阶段业务逻辑改动 |
| AC-S1-AUX-01 | （辅助）`GET /admin-api/oa/author/list` → total ≥ 35 |
| AC-S1-AUX-02 | （辅助）`GET /admin-api/oa/platform-account/list?platform=WECHAT_OFFICIAL` → 读 mp 187 |

### 测试方法

| 脚本 | 期望 |
|------|------|
| **Football E2E（Gate）** | `run-uat-football-e2e.ps1` · `verify-ops-pages-per-menu.py` → **58/58** + §0.6 S1 人工场景 |
| `mvn verify`（oa-module Author* IT） | P0 100%（辅助） |
| API probe 作者/公号 | 列表非空 + 字段对齐（辅助） |

### 修复迭代循环

参考 §0.3；新增：

| FAIL | 修复 |
|------|------|
| 作者列表空 | `@DS("member")` 未生效 → 查 dynamic-datasource 配置 |
| ext join 丢行 | LEFT JOIN 策略；无 ext 仍展示 Football 作者 |
| 微信列表 500 | mp DS 凭证 / 库名 |

### 回滚

- 代码：revert S1 commits
- 数据：member/mp **不 rollback**（SSOT）；wd ext 可 TRUNCATE

### 文档更新

- API-M1 作者节 · ADR-051 §4 实现状态
- GATE-MDB-S1 报告 → [GATE-MDB-S1-报告-20260705.md](./gates/GATE-MDB-S1-报告-20260705.md)（2026-07-05 · E2E 57/58 · author/internal-account ✅）

---

## S2 — 非微信 + IP 组 + 字典

### 目标

非微信 oa_account 空表 CRUD；IP 组 UI 可用；字典双轨 Adapter。

### 范围

- 抖音/企微等 oa_account
- M4 资产链与新账号 ID
- 平台字典读 system / 业务 dict 读 wd

### 不做什么

- 日志 UI 切源（S3）
- 采集 bind 改 mp_account_id（S3）
- **不改 Football 业务代码**；字典 Adapter 在 `oa-server`，**不**改 system-server 字典逻辑

### 前置 Gate

- [x] GATE-MDB-S1 ✅
- [x] S1 UAT 58/58 仍绿

### 任务清单

| # | 任务 |
|---|------|
| S2-01 | 非微信 PlatformAccount CRUD（空表） |
| S2-02 | IP 组 CRUD + 与 ext 关联验证 |
| S2-03 | DictAdapter：platform → system_dict |
| S2-04 | M4 资产选择器与新 account id 对齐 |
| S2-05 | IT + UAT 账号管理 4 模块页 |

### 验收标准

> **Gate 路径**：§0.6 · :5777 登录操作。

| ID | 期望 |
|----|------|
| **AC-S2-UI-01** | `#/ops/ip-group` tree 含 3 skeleton + **UI 新建**子组 |
| **AC-S2-UI-02** | 非微信账号页 **UI CRUD** 200 · 列表可见 |
| **AC-S2-UI-03** | `#/ops/system-dict` 平台字典可读（system）；业务 dict 仍 wd |
| **AC-S2-UI-04** | M4 资产选择器 **5/5**（CHECKLIST-M4 §10 · Football UI 点选） |
| **AC-S2-UI-05** | `run-uat-football-e2e.ps1` → **58/58 PASS** |
| AC-S2-06 | **无 Football 业务代码 diff** | system-server / mp-server 无本阶段业务逻辑改动 |
| AC-S2-AUX-01 | （辅助）非微信 CRUD API curl |
| AC-S2-AUX-02 | （辅助）IP 组 tree API · 字典 API curl |

### 测试方法

**Gate**：`run-uat-football-e2e.ps1` · `verify-ops-pages-per-menu.py` **58/58** + §0.6 S2 人工场景 · M4 IT（辅助）

### 修复迭代 / 回滚 / 文档

同 S1 模式；更新 CHECKLIST-M4 · [GATE-MDB-S2 报告](./gates/GATE-MDB-S2-报告-20260705.md)（2026-07-05 · E2E 58/58 · dict adapter ✅）

---

## S3 — 日志 + 消息 + 采集

### 目标

登录/操作日志读 Football；消息分场景；采集 bind 改 mp_account_id。

### 不做什么

- DROP oa_author（S4）
- 远程 cutover
- **不改 Football 业务代码**；日志/消息/订单读 `@DS("system"|"pay")` 或既有 API，**不**改 system/pay-server

### 前置 Gate

- [x] GATE-MDB-S2 ✅

### 任务清单

| # | 任务 |
|---|------|
| S3-01 | LoginLogAdapter · OperateLogAdapter |
| S3-02 | 消息：Ops 广播 vs Football 站内信分菜单 |
| S3-03 | 采集任务 bind → mp_account_id / oa_account.id |
| S3-04 | `@DS("pay")` 订单只读 Mapper 改造 |
| S3-05 | UAT 系统管理 + 采集页 |

### 验收标准

> **Gate 路径**：§0.6 · :5777 系统管理/采集页操作。

| ID | 期望 |
|----|------|
| **AC-S3-UI-01** | `#/ops/system-log/login` 列表 **≥ 3000**（system SSOT） |
| **AC-S3-UI-02** | `#/ops/system-log/operation` 列表 **≥ 600** |
| **AC-S3-UI-03** | `#/ops/collect/task` bind 字段展示 mp_account_id / oa_account.id |
| **AC-S3-UI-04** | 订单只读页（Football UI）跨库数据可见 |
| **AC-S3-UI-05** | `run-uat-football-e2e.ps1` → **58/58 PASS** · UAT spotcheck 22/22 |
| AC-S3-06 | sys_login_log 停写（SQL 辅助） |
| AC-S3-07 | **无 Football 业务代码 diff** | system/pay-server 无本阶段业务逻辑改动 |
| AC-S3-AUX-01 | （辅助）`GET /admin-api/oa/football-order/list` 跨库 PASS |

### 测试方法

**Gate**：`run-uat-football-e2e.ps1` · §0.6 S3 人工场景 · UAT-SPOTCHECK-EXPANDED 复跑 · API curl / pay DS smoke（辅助）

---

## S4 — Cutover

### 目标

DROP oa_author；废弃 wd Football 副本表；全环境分库配置；Gate 重签。

### 不做什么

- **不改 Football 业务代码**（全程序硬约束复核）
- 远程 101.37.161.136 cutover — **Out of Scope**（2026-07-05 用户取消：非部署环境，不继续远程 sync；见 GATE-MDB-REMOTE ⏸ Deferred）

### 前置 Gate

- [ ] GATE-MDB-S0～S3 全部 ✅
- [ ] 对账：ext 覆盖率报告

### 任务清单

| # | 任务 |
|---|------|
| S4-01 | Flyway DROP oa_author（若确认零引用） |
| S4-02 | 标记/ DROP wd 内 author_user/pay_*/system_* 副本 |
| S4-03 | Nacos 全环境分库 matrix |
| S4-04 | GATE-MDB-S4 全量回归：58/58 + E2E + mvn verify |
| S4-05 | 更新 ADR-047 D2 为「Superseded by ADR-050」 |

### 验收标准

> **Gate 路径**：§0.6 · cutover 后 **Football UI 全量复验**。

| ID | 期望 |
|----|------|
| **AC-S4-UI-01** | :5777 登录 · S1–S3 **人工场景全复验** |
| **AC-S4-UI-02** | cutover 后 `#/ops/author` 仍读 member（无 oa_author 依赖） |
| **AC-S4-UI-03** | `run-uat-football-e2e.ps1` → **58/58 PASS** |
| AC-S4-04 | 无 oa_author 表或零引用（SQL） |
| AC-S4-05 | 远程环境配置文档化（执行需另批） |
| AC-S4-06 | `mvn verify` 151/151 IT 绿（辅助） |
| AC-S4-07 | **全程序无 Football 业务代码 diff** | S0–S4 累计：member/mp/pay/system-server 业务模块零改动；gateway 仅已冻结集成配置 |

---

## 附录 A — S0 行数验收 SQL

```sql
SELECT 'oa_content' t, COUNT(*) c FROM wd.oa_content
UNION ALL SELECT 'oa_author', COUNT(*) FROM wd.oa_author
UNION ALL SELECT 'oa_account', COUNT(*) FROM wd.oa_account
UNION ALL SELECT 'oa_ip_group', COUNT(*) FROM wd.oa_ip_group
UNION ALL SELECT 'oa_author_ext', COUNT(*) FROM wd.oa_author_ext
UNION ALL SELECT 'sys_dict_type', COUNT(*) FROM wd.sys_dict_type
UNION ALL SELECT 'author_user(wd)', COUNT(*) FROM wd.author_user
UNION ALL SELECT 'author_user(member)', COUNT(*) FROM shenyu-member.author_user
UNION ALL SELECT 'mp_account', COUNT(*) FROM shenyu-mp.mp_account;
```

## 附录 B — 下一阶段入口（S1）

| 条件 | 状态 |
|------|------|
| GATE-MDB-S0 报告归档 | 待签 |
| V131 Flyway SUCCESS | 待跑 |
| `@DS` smoke 4/4 | 待做 |
| ADR-050/051 已 Accepted | ✅ |
| IP 组 3 行 | 待执行 |

---

## 变更记录

| 日期 | 说明 |
|------|------|
| 2026-07-05 | 初版；用户确认 TRUNCATE + skeleton + ADR |
| 2026-07-05 | §0.5 / 各阶段「不做什么」/ Gate AC：嵌入「不改 Football 业务代码与逻辑」硬约束 |
| 2026-07-05 | **§0.6 验收总则**：GATE-MDB-S0～S4 强制 Football :5777 UI 签收；curl/:3000 非 Gate |
| 2026-07-05 | **GATE-MDB-S2** 签收：DictAdapter · 非微信 CRUD · E2E 58/58 |
| 2026-07-05 | **GATE-MDB-S4** 签收：V132 cutover · oa_author DROP · E2E 58/58 · 远程 matrix 文档 |
| 2026-07-05 | **远程 cutover Out of Scope**：101.37.161.136 非部署环境；GATE-MDB-REMOTE ⏸ Deferred；日常 dev 仅 localhost 五库 |
| 2026-07-05 | **Post-MDB 本地签收**：POST-MDB-LOCAL-SIGNOFF-20260705 · E2E 58/58 · DB SSOT 探针；Gateway OA curl 401 记入 INTEGRATION-PROGRESS §23 #1 |
