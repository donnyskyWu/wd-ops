# OPS × Football 合并后清理清单（Cleanup Inventory）

| 字段 | 值 |
|------|---|
| 文档性质 | **合并后去冗余清理清单**（可执行；非 Slice 实现规格） |
| 版本 | v1.1 |
| 日期 | 2026-07-30 |
| 决策 SSOT | [OPS-FOOTBALL-RPC-MUST-HAVE.md](./OPS-FOOTBALL-RPC-MUST-HAVE.md) · [OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md) v1.8 |
| **执行工作计划** | **[OPS-FOOTBALL-MERGE-WORK-PLAN.md](./OPS-FOOTBALL-MERGE-WORK-PLAN.md)**（v1.3 · 2026-07-30）— 清理项按 **A 前端 → B 数据库 → C 后端** 落地；§7 分阶段与工作包对照见该文 §9 |
| 关联 ADR | ADR-047 · ADR-050 · [ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) · ADR-056 · [ADR-057](../adr/ADR-057-G-PAY-01-page-for-ops.md) |
| 状态 | Active — G-* 手验 Pass；**B-DS-RESIDUE ✅**；**C-WP7-PHYS 代码物理删 ✅**（2026-07-30）；**Phase C 整包仍 NO-GO** 直至 **B-WP4 表归档** 产品签收 |

> **读法**：本清单回答「合并后哪些代码 / 配置 / 表 / 页面会变成冗余」。接口缺口与人日见 MUST-HAVE；本文只列 **清理对象**。

---

## 原则回顾

| # | 原则 | 清理含义 |
|---|------|----------|
| 1 | OPS **只访问 `wd`**；跨域一律 **RPC/Feign** | 删除 `@DS("system\|member\|mp\|pay")` 直连与相关 Nacos/yml 数据源 |
| 2 | **D-DEDUP-01**：不重复 Football 已有能力 | 删除/隐藏平行管理 UI、平行读 API、作者 CUD、钉钉通讯录同步 |
| 3 | 前端合并进 **football-front**；OPS 为微服务（`:48094`） | 废弃 standalone `:3000/:8080` 作为目标态；`mount-ops-all.py` 过渡后改为单源 |
| 4 | OPS 自建保留域继续落 `wd` | **保留** `oa_*` / `oa_*_ext`、IP 组、SOP、绩效、`sys_param` 等 |

**时机标签**

| 标签 | 含义 |
|------|------|
| **可立即标记废弃/隐藏** | 菜单已摘、路由 hide、文档标注 deprecated；代码可 `@Deprecated`，但物理删除可稍后 |
| **依赖 Football 就绪后删除** | 对应 G-* 缺口或 Gateway/`check`/FileApi 切轨完成前，**禁止**物理删除（否则功能断） |
| **RPC cutover 后删除** | 切轨验收绿后再删配置/Mapper/测试 |

---

## 清理清单（按类别）

### 1. 后端代码（oa-server）

#### 1.1 `@DS("system"|"member"|"mp"|"pay")` 直连 → 待替换为 Feign 后删除

> grep：`ops-platform-module-oa/**/*.java` 中 `@DS("…")`（不含 `@DS("master")` 保留项）。

| 对象 | DS | 现状用途 | 替换目标（MUST-HAVE） | 时机 |
|------|-----|----------|----------------------|------|
| ~~`dal/mysql/auth/FootballOAuth2TokenMapper`~~ | system | Token / 角色读（legacy 回滚） | Gateway/`check`（D-SYS-03） | **已删**（**C-WP7-PHYS** 2026-07-30） |
| ~~`dal/mysql/system/FootballSystemUserLookupMapper`~~ · ~~`FootballSystemRoleLookupMapper`~~ · ~~`FootballSystemUserSystemReader`~~ | system | 用户 lookup / nickname / roleCode | G-SYS-01/02 Feign | **已删**（**B-DS-RESIDUE** 2026-07-30）；nickname=`getByIds`；roleCode→roleId=wd master |
| ~~`dal/mysql/dict/FootballSystemDictTypeMapper`~~ · ~~`FootballSystemDictDataMapper`~~ | system | 字典读/管理 | G-DICT-01 `DictDataApi` | **已删**（B-DS-RESIDUE）；`@InDict`/data Feign-only；admin/types → **410** |
| `service/system/SystemDictAdapter` | — | `@InDict` / DictService | 同上 | **Feign-only**；管理方法 410（无 Mapper） |
| `service/author/MemberAuthorReadService` · ~~`AuthorUserMapper`~~ | — | 作者读 | G-MEM-02 Feign | **Feign-only**（早切）；AuthorUserMapper 已不存在 |
| `service/football/MemberArticleWriteService` · ~~`AuthorArticleMapper`~~ | member | 文章 insert/update | G-MEM-03 写 Feign | **Mapper/`getById`/member DS 已删**（C-WP7 2026-07-30）；写 Feign-only ✅ |
| `service/account/MpAccountDataService` · `MpUserDataService` · ~~`MpAccountMapper`~~ · ~~`MpUserMapper`~~ | mp | 公众号读写 | G-MP-01 Feign | **服务 Feign-only + 手验 Pass**；死 `MpAccountMapper` **已删**（C-WP7-PHYS） |
| ~~`dal/mysql/football/FootballPayAllOrderReadMapper`~~ | pay | 订单只读 | G-PAY-01 / ADR-057 | **已删**（2026-07-29）；Feign `pageForOps` 手验 Pass |
| ~~`dal/mysql/smoke/SystemDsSmokeMapper`~~ · ~~`PayDsSmokeMapper`~~ | system/pay | 多库 smoke | cutover 后整包删除 | **均已删**（C-WP7-PHYS / 先前） |

**保留（非清理）**：`@DS("master")` — `OaAuthorExtMapper`、`OaAccountExtMapper`、`OaAccountExtDataService`（OPS 业务表）。~~`FootballOAuth2MasterTokenMapper`~~ 见 1.2 **已删**（P-E）。

#### 1.2 鉴权：FootballAuthProvider DB/Redis 直读 token

| 对象 | 包/文件 | 动作 | 时机 |
|------|---------|------|------|
| ~~`FootballAuthProvider`~~ → `GatewayAuthProvider` | `service/auth/` | Gateway login-user / `checkAccessToken` | **C-WP1 ✅**；旧直读类 **已删**（C-WP7-PHYS） |
| ~~`FootballOAuth2TokenRedisReader`~~ · ~~`FootballOAuth2RedisProperties`~~ | `service/auth/` · `config/` | Redis 直读 token 快照 | **已删**（C-WP7-PHYS）；`FootballOAuth2TokenSnapshot` 常量保留给操作日志 |
| ~~`FootballOAuth2TokenMapper`~~（@DS system） | 见上 | 停用 | **已删**（C-WP7-PHYS） |
| ~~`FootballOAuth2MasterTokenMapper`~~（@DS master · `system_users` overlay） | `dal/mysql/auth/` | 停用「wd 内 system_users 桥」 | **已删**（**P-E** 2026-07-31）；RBAC→Feign `hasAnyPermissions` / `hasAnyRoles` + Gateway login-user；证据 [P-E-RBAC-FEIGN-20260731](./e2e-artifacts/P-E-RBAC-FEIGN-20260731/REPORT.md) |
| `DevAuthProvider`（条件装配）· `DevAuthFilter`（历史名）· `CompositeAuthProvider` | `framework/auth/` · `service/auth/` | 生产：`oa.auth.dev-token.enabled=false`；IT：`application-test.yml` 开启 | **C-WP7-PHYS ✅** 生产路径下线 |
| ~~`oa.auth.legacy-ds-token`~~ / ~~`football-redis`~~ | 配置 | 已移除 | **C-WP7-PHYS** 去紧急回滚开关 |

#### 1.3 文件：LocalFileStorageService / `/oa/file` → FileApi

| 对象 | 动作 | 时机 |
|------|------|------|
| `service/file/LocalFileStorageService` | 上传 Feign-only（G-INF-01 cutover）；legacy key 本地读保留 | 存量迁移 / 长期只读代理待办；手验 Pass 2026-07-30 |
| `controller/file/FileController`（`/admin-api/oa/file/*`） | 过渡期可代理→FileApi；终态下线 | 过渡可代理；终态 RPC cutover 后删除 |
| `util/ImageKeyHelper.FILE_VIEW_PREFIX`（`/admin-api/oa/file/view?key=`） | 改 infra URL | 随切轨 |
| `TaskServiceImpl` 等对 `LocalFileStorageService` 的注入 | 改调用 | 同上 |
| `DevAuthFilter` 对 `/oa/file/view|download` 的豁免 | 随本地文件下线 | 同上 |

#### 1.4 字典：SystemDictAdapter @DS → Dict Feign

| 对象 | 动作 | 时机 |
|------|------|------|
| `SystemDictAdapter` · `SystemDictServiceImpl` · `DictService` | 后端读 Feign-only；**管理写/type-list/admin-list 均 410**（B-DS-RESIDUE） | ✅ |
| `controller/dict/DictController`（`/admin-api/oa/dict`） | 前端 DictSelect 改 Football Admin 后可删薄封装，或保留代理一层 | 可立即标记废弃（管理）；读代理依赖前端切轨 |
| `controller/system/SystemDictController`（`/admin-api/oa/system/dict` 别名） | 同上 | 同上 |
| `dal/mysql/dict/SysDictDataMapper`（wd `sys_dict_*`） | 业务字典若已全部 merge 到 shenyu-system，停写 wd；读走 Feign | 见 §3；依赖字典 merge 完成 |

#### 1.5 作者写路径（OPS 只读 · D-AUTHOR-01）

| 对象 | 动作 | 时机 |
|------|------|------|
| `AuthorServiceImpl` 中对 member `author_user` 的 **create/update/status**（若仍存在） | **禁止新写**；管理归 Football Admin | 可立即标记废弃（写 API）；读改 Feign 后删 `@DS` |
| `controller/author/AuthorController` 写接口 | **业务码 410** ✅ C-WP0；保留 `AuthorExtController` 写 `oa_author_ext` | 物理删属 P2 |
| `MemberAuthorReadService` | 只读 → Feign 后删除直连 | 依赖 G-MEM-02 |
| 菜单 6155「作者管理」 | **已移除**（seed 注释 · V145） | 已完成 |

#### 1.6 SysUserMapper / legacy `sys_user` 作为写入校验

> ADR-056：写入校验须 `FootballSystemUserValidator`；禁止仅用 `SysUserMapper`。

| 对象（grep `SysUserMapper` 引用） | 建议 |
|----------------------------------|------|
| `FootballSystemUserValidator` | **保留**过渡 union；终态去掉 legacy/master 分支，仅 Feign |
| `UserServiceImpl` · `DeptServiceImpl` | M9 standalone 管理能力 — **废弃删除**（D-DEDUP-01） |
| `AuthorServiceImpl` · `ProductionContentServiceImpl` · `ContentPlanServiceImpl` · `SopReviewServiceImpl` · `InternalContentServiceImpl` · `NotificationServiceImpl` · `PerfResultServiceImpl` · `OrderAttributionServiceImpl` · `WechatLayoutTemplateServiceImpl` · `ProductivityReviewServiceImpl` · `DingTalkDevController` | 昵称/展示/通知解析：改 Football UserApi；**禁止**作唯一写入校验 | 依赖 G-SYS-02；可立即加注释/审计 |
| `dal/mysql/auth/SysUserMapper` · `SysUserTokenMapper` · `SysRoleMapper` | standalone/dev-token 退役后删除 | RPC cutover + harness 退役后 |

#### 1.7 Standalone-only / 平行管理 Controllers（M9 等）

| Controller | 路径模式 | 处置 |
|------------|----------|------|
| `UserController` | `/admin-api/oa/system/user` · `/admin-api/system/user` | **永久 410** ✅ C-WP0（`/profile` 仍可用）；物理删属 P2 |
| `RoleController` | `…/system/role` | **永久 410** ✅ C-WP0；物理删属 P2 |
| `DeptController`（含钉钉 sync） | `…/system/dept` | **永久 410** ✅ C-WP0（含 sync-*，D-DING-02）；物理删属 P2 |
| `PermissionController` | `…/system/permission` | 删除平行权限管理（**C-WP0 未强制 410**；残留读 list） |
| `TenantController` | `…/system/tenant` | 删除（**C-WP0 未强制 410**；GateS2 仍测权限） |
| `ParamController` | `/admin-api/oa/system/param` | **保留**（`sys_param` 属 OPS） |
| `MessageController` | `…/system/message` | 产品二选一：Football Notify 或 OPS 自建；勿平行管理台（分析 G-NTF-01） |
| `SystemDictController` / `DictController` | 见 1.4 | 管理不下沉；读切 Feign/Admin |

**时机**：菜单/路由已 deprecated → **可立即标记废弃**；物理删依赖前端与 IT 不再调用。

#### 1.8 操作日志：重复写 / 禁止平行读

| 对象 | 现状 | 处置 | 时机 |
|------|------|------|------|
| `OaLogRecordServiceImpl` → `OperateLogCommonApi` | **已接 Feign 写**（保留） | 保留 | — |
| `OperationLogRecorder` + `SysOperationLogMapper` → `wd.sys_operation_log` | **C-WP0 已停 insert**（recorder no-op `@Deprecated`） | Mapper/表物理删 | P2 确认无读依赖后删 |
| 操作日志 **读** Controller / UI | 菜单 6139 **已移除**（V147）；无平行读 Controller | 确认无残留后结案 | 已基本完成 |
| `OaLogRecordServiceImpl` 内 query 抛 `UnsupportedOperationException` | 正确（禁止平行读） | 保留断言 | — |

#### 1.9 钉钉（保留推送 · 删除通讯录）

| 对象 | 处置 | 时机 |
|------|------|------|
| `DeptService.syncDepartmentsFromDingTalk` / `syncUsersFromDingTalk` | **删除**（D-DING-02） | 可立即标记废弃/隐藏 |
| `DingTalkWorkNotifyClient` · `DingTalkRobotClient` · `NotificationServiceImpl.pushDingTalk` | 待 G-DING-01 统一 API 后收敛 | 依赖 Football 就绪后删除（本地 Client） |
| `DingTalkDevController` | 仅 dev；生产禁用 | 可立即限制 profile |

---

### 2. 数据源 / 配置

| 对象 | 动作 | 时机 |
|------|------|------|
| `application-dev-local-multidb.yml` 中 `member` / `mp` / `pay` / `system` 数据源块 | cutover 后 **整段删除**；仅留 `master`→`wd` | RPC cutover 后删除 |
| `scripts/integration-config/oa-server-remote-multidb.yaml` | 同上（远程五库指向） | RPC cutover 后删除 |
| Nacos overlays 中 OPS 指向 football DB 的 jdbc（若仍有） | 改为仅 `wd` + Feign 服务发现 | 同上 |
| `oa.auth.football-redis.*` | Auth 切 Gateway/`check` 后删除 | 同上 |
| Dev-token / standalone profiles（`dev` only · `DevAuthFilter`） | 生产禁用；文档标注非目标态 | 可立即标记废弃（目标态）；harness 可暂留 |
| `push-remote-multidb-config.ps1` | 停止作为目标态推送；改「仅 wd」配置包 | 可立即标记废弃 |
| **终态保留** | OPS 仅连 **`wd`（master）** | — |

---

### 3. 数据结构 / 表

#### 3.1 停止新写 / 逐步废弃（Football SSOT）

| 表 / 列 | 库 | 说明 | 时机 |
|---------|-----|------|------|
| `wd.system_users`（若仍作 overlay 写入） | wd | **禁止新写**；SSOT = `shenyu-system.system_users`（ADR-056） | 可立即停写 |
| `wd.sys_user` / `sys_user_token` / legacy 角色表 | wd | 仅 H2 IT / standalone fallback；生产停用 | harness 退役后删 |
| `wd.oa_author`（非 ext） | wd | ADR-050/051：**停写**；用 `author_user` + `oa_author_ext` | 可立即停写 |
| `wd.sys_operation_log` | wd | 写已切 Feign；本地表可归档后停写 | 确认无读后 |
| `wd.sys_dict_*` 业务字典 | wd | 已 merge → `shenyu-system.system_dict_*`（V152/V158）；**新字典勿只写 wd** | 可立即规范；旧表可只读过渡 |
| 对 `author_user` / `author_article` / `mp_account` / `pay_all_order` 的 **直连写** | football 库 | 改 Feign；OPS 不再持有写 Mapper | 依赖对应 G-* |

#### 3.2 保留（OPS 自建）

| 表/域 | 说明 |
|-------|------|
| `oa_*_ext`（如 `oa_author_ext`、`oa_account_ext`） | 运营扩展；PK/FK = Football id |
| IP 组、SOP/任务、绩效、计划、内容生产编排表、`sys_param`、大屏/AI 配置等 | MUST-HAVE §6 |
| 非微信平台账号等 OPS 自有资产表 | 继续落 wd |

#### 3.3 Flyway / seed 所有权（澄清）

| 脚本模式 | 现状 | 清理建议 |
|----------|------|----------|
| `V152__merge_ops_dict_to_shenyu_system.sql` · `V158__sync_v157_dict_to_shenyu_system.sql` | OPS Flyway **跨库写入** `shenyu-system` | **迁移所有权**：字典变更改由 Football/运维脚本维护；OPS Flyway **禁止**再 `INSERT INTO \`shenyu-system\`.*` |
| `V137__sync_shenyu_system_menus.sql` · `seed-oa-system-menu.sql`（6100+） | 菜单灌入 system | 菜单 seed **可保留**（OPS 权限挂 Football 菜单），但勿再同步 Football 原生菜单树 |
| `V150` 角色 seed 与 shenyu 对齐说明 | 双写风险 | 角色 SSOT = Football；OPS 仅文档化同步，不长期双维护 |
| `V148`（写错库历史） | 已由 V152 纠正 | 保留历史 migration，勿再仿写 |

#### 3.4 RPC cutover 后可废弃的列/桥（示例）

| 项 | 说明 |
|----|------|
| 业务表中仅服务「wd userId ↔ football userId」normalize 的过渡列/缓存 | ADR-056 全量以 Football id 存储后可删桥接逻辑（非必删列，先停写） |
| `sync_status` 等跨库 Saga 字段（若文章/公号改 Feign 后仍要） | **可能保留**（应用层对账）；勿盲目删 |

---

### 4. 前端

| 对象 | 现状 | 处置 | 时机 |
|------|------|------|------|
| `ops-platform-ui-vue` standalone **:3000** | ADR-049 D6 harness | 源码合并进 football-front 后 **deprecate 独立启动路径** | 可立即标记废弃（目标态）；物理下线在源合并后 |
| `scripts/mount-ops-all.py` | 中间态：拷贝/挂载到 `football-front` | 终态 = football-front **单源**；mount 脚本退役或仅 CI 兼容 | 源合并后删除 |
| `views/system/UserManage.vue` · `RoleManage.vue` · `TenantManage.vue` | 路由已 `deprecated` + `hideInMenu` | **删除页面与路由** | 可立即标记；确认无外链后删 |
| `views/operations/AuthorRedirect.vue` | 已 deprecated | 确认跳转 Football `#/member/author` 后删 | 可立即 |
| 操作日志 / 登录日志 / 字典管理页 | 菜单已摘（6137–6139） | 确认无残留组件引用 | 已基本完成 |
| `api/file.ts` → `/admin-api/oa/file/upload` · `ImageUploadField` | 本地文件 | 改 `/admin-api/infra/file`（D-INF-01） | 依赖切轨 |
| `api/dict.ts` → `/oa/dict/data` · `DictSelect.vue` | OPS 薄封装 | 前端可直调 Football `/admin-api/system/dict-*`（MUST-HAVE） | 可立即改调用；后端 `@InDict` 仍走 Feign |
| `api/system-user.ts`（`/oa/system/user/*` CRUD） | 平行用户管理 | **删除** | 可立即标记废弃 |
| `api/football-user.ts` + `UserSelect.vue` | 已走 `/system/user/simple-list` | **保留**（正确方向） | — |
| `src/utils/ops-route.ts` standalone 分支 | 壳内 `/ops` 前缀 | 单源后简化 | 源合并后 |

---

### 5. 菜单 / 权限 seed

| 项 | 状态 | 动作 |
|----|------|------|
| 6100–6999 OPS 业务菜单 | **保留**（运营独有） | 仅挂 OPS 模块权限 `oa:*` |
| 6105「系统管理(OA)」下 | 现仅 **消息 6140**、**参数 6141** | 勿再加用户/部门/字典/日志 |
| 6137 字典 / 6138 登录日志 / 6139 操作日志 / 6155 作者 | **已从 seed 移除**（注释 + V145–V149） | 结案；环境若残留则跑对应 V 脚本 |
| Football 原生菜单（用户/部门/菜单/字典/操作日志） | SSOT | OPS seed **不复制**；需要时角色授权原生 menu id |
| `oa:user:*` / `oa:dept:*` / `oa:dict:update` 等平行权限 | 若仍在库中 | **清理角色绑定**；权限只保留 OPS 自有模块 |

**时机**：菜单清理 **可立即**核对环境；权限码清扫与前端删页同步。

---

### 6. 脚本 / 文档 / 测试

| 类别 | 对象 | 处置 | 时机 |
|------|------|------|------|
| 脚本 | `start-ops-standalone.ps1` · `restart-all.ps1`（:3000/:8080） | 标注 **非 Gate / 非目标态**；终态以 `start-ops-dev.ps1` / integration 为准 | 可立即标记废弃 |
| 脚本 | `mount-ops-all.py` | 见 §4 | 源合并后删 |
| 脚本 | `push-remote-multidb-config.ps1` · `oa-server-remote-multidb.yaml` | 目标态改为单库 wd | cutover 后删 |
| E2E | `tests/uat-browser-gap.spec.ts`（:3000+:8080）· `run-uat-browser-e2e.ps1` | 迁移到 Football `:5777` + Gateway；standalone 套件归档 | 可立即标记非 Gate |
| E2E | `helpers/integration-api.ts` 默认 `:8080` | 改 Gateway `:48080` 或保留双 profile | 随 harness |
| IT | `MdbS2DictAdapterIT` 等依赖 `@DS("system")` | 改 Feign mock / Testcontainers 契约测 | 依赖切轨 |
| 文档 | `OPS-DEV-DEPLOY-GUIDE.md` 五库直连、作者 `@DS("member")` 直连叙述 | 改写为 **过渡现状** + **目标 RPC**；链到 MUST-HAVE / 本文 | **可立即修订** |
| 文档 | `OPS-STARTUP-MATRIX.md` · ADR-050 §3.1「不改 Football」 | ADR-050 §3.1 待正式 Supersede；矩阵突出 Integration 为唯一 Gate | 可立即标注 |
| 文档 | 仍教「multidb 直连 = 目标态」的 MULTI-DB EXECUTION 文 | 加页首 banner：目标态已切换为 RPC | 可立即 |

---

### 7. 分阶段清理建议

#### P0 — 可立即做（不依赖新 Feign）

| # | 动作 |
|---|------|
| P0-1 | 文档：部署指南 / 启动矩阵标明五库 `@DS` = **过渡**；目标 = 仅 wd + RPC（链 MUST-HAVE + 本文） |
| P0-2 | 前端：确认 User/Role/Tenant/Author 管理路由保持 hide；API `system-user` CRUD 标 deprecated |
| P0-3 | 菜单：环境抽检 6137–6139/6155 已不存在；角色勿绑平行权限 |
| P0-4 | 停写：`oa_author`（非 ext）、wd `system_users` 新写、wd `sys_dict_*` 作为 SSOT 新写 |
| P0-5 | ~~钉钉通讯录 sync API/按钮隐藏或 410（D-DING-02）~~ ✅ C-WP0 2026-07-30（业务码 410） |
| P0-6 | ~~操作日志：审计是否仍双写 `sys_operation_log`；计划去掉 `OperationLogRecorder` 本地写~~ ✅ C-WP0 2026-07-30（停 insert；Mapper 物理删属 P2） |
| P0-7 | Standalone 脚本/E2E 标注非 Gate |

#### P1 — Football 缺口就绪后切轨（按 G-*）

| # | 依赖 | 删除/替换 |
|---|------|-----------|
| P1-1 | D-SYS-03 Gateway/`check` | ~~`FootballAuthProvider` / TokenMapper / Redis~~ **已删**（C-WP7-PHYS） |
| P1-2 | G-SYS-01/02 | `FootballSystemUserLookupMapper`；Validator 去 `@DS`；收敛 `SysUserMapper` |
| P1-3 | G-DICT-01 | `SystemDictAdapter` + Football Dict Mappers；前端 DictSelect 改 Admin |
| P1-4 | G-INF-01 | `LocalFileStorageService` / `/oa/file` |
| P1-5 | G-MEM-02/03 · G-MP-01 · G-PAY-01 | member/mp/pay `@DS` Mapper 与 WriteService |
| P1-6 | G-DING-01 | 本地 DingTalk Client 收敛到统一 API |

#### P2 — RPC cutover 验收绿后物理删除

| # | 动作 |
|---|------|
| P2-1 | 删除 `application-dev-local-multidb.yml` 中非 master 数据源；删除 remote-multidb 配置与推送脚本 |
| P2-2 | 删除 smoke Mapper、无用 Controller、deprecated Vue 页、mount 脚本（若单源完成） |
| P2-3 | Flyway 政策：禁止再跨库写 shenyu-*；字典所有权移交 |
| P2-4 | ~~Standalone `:3000/:8080` 与 dev-token 生产路径下线~~ **代码侧 ✅**（C-WP7-PHYS：`dev-token` 仅 test）；前端 standalone 脚本标非 Gate 仍属 A-WP5 |
| P2-5 | 正式 ADR：**Supersede ADR-050 §3.1**（允许 Football 补 Feign；OPS 禁直连） |

---

## 统计摘要（v1.0 盘点）

| 类别 | 条目数（约） | 可立即废弃/隐藏 | 依赖 Football 后删 | cutover 后删 |
|------|-------------|-----------------|-------------------|--------------|
| 1 后端代码 | **28** | 8 | 16 | 4 |
| 2 数据源/配置 | **7** | 2 | 0 | 5 |
| 3 数据结构/表/Flyway | **12** | 5 | 3 | 4 |
| 4 前端 | **11** | 6 | 2 | 3 |
| 5 菜单/权限 | **6** | 5 | 0 | 1 |
| 6 脚本/文档/测试 | **12** | 8 | 2 | 2 |
| **合计** | **~76** | **~34** | **~23** | **~19** |

> 条目为「清理动作行」，非文件行数；同一类多个 Mapper 已合并计数。

---

## 关联文档

| 文档 | 用途 |
|------|------|
| [OPS-FOOTBALL-MERGE-WORK-PLAN.md](./OPS-FOOTBALL-MERGE-WORK-PLAN.md) | **执行排期与工作包**（A→B→C）；勾选进度以该文为主，回写本清单行 |
| [OPS-FOOTBALL-RPC-MUST-HAVE.md](./OPS-FOOTBALL-RPC-MUST-HAVE.md) | 必须做的 RPC / 拍板 / 明确不做 |
| [OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md) | 完整分析与去重对照 |
| [ADR-056](../adr/ADR-056-Football用户身份SSOT.md) | 用户身份 SSOT |
| [ADR-050](../adr/ADR-050-Ops与Football多库复用总纲.md) | 多库现状（§3.1 待 Supersede） |

---

**维护**：切轨每完成一个 G-*，在对应行打勾并更新日期；勿在未替换调用方前物理删除 `@DS` Mapper。
