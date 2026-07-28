# OPS × Football 合并 + 清理执行工作计划

| 字段 | 值 |
|------|---|
| 文档性质 | **团队执行工作计划**（按阶段落地；非 Slice 实现规格） |
| 版本 | **v1.2** |
| 日期 | **2026-07-28** |
| 决策 SSOT | [OPS-FOOTBALL-RPC-MUST-HAVE.md](./OPS-FOOTBALL-RPC-MUST-HAVE.md) · [OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md](./OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md) · 完整分析 [OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md) v1.8 |
| 关联 ADR | ADR-047 · ADR-049 · ADR-050 · [ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) · ADR-056 |
| 状态 | **部分可执行**（Phase A/B 可推进；Phase C 整包 **NO-GO** 直至 G-* cutover 验收 + 删 `@DS`；§3.1 **已有限 Supersede** 2026-07-28） |
| 执行优先级（已拍板） | **Phase A 前端 → Phase B 数据库 → Phase C 后端** |

> **读法**：本文回答「按什么顺序做、谁先做、做到什么算完」。缺口 API 与接口说明书见 MUST-HAVE §7；清理对象明细见 CLEANUP Inventory。  
> **诚实边界**：前端源合并**现在就能开**；DB 可先停写/标废弃；**禁止**在 Football Feign 未就绪时物理删除 `@DS` Mapper / 多库数据源。

---

## 1. 目标与原则

### 1.1 目标态

| # | 目标 | 依据 |
|---|------|------|
| T1 | OPS 前端并入 **football-front 单源**；standalone `:3000` 非目标态 | CLEANUP §4 · ADR-049 |
| T2 | OPS **只访问 `wd`**；跨库一律 **RPC/Feign**，禁止 `@DS("system\|member\|mp\|pay")` | MUST-HAVE §1 |
| T3 | 不重复建设 Football 已有管理能力（用户/部门/菜单/字典管理、日志读、作者 CUD、钉钉通讯录） | **D-DEDUP-01** |
| T4 | OPS 自建域（IP 组、SOP/任务、绩效、`oa_*_ext`、`sys_param` 等）继续落 `wd` | MUST-HAVE §6 |
| T5 | 用户身份 SSOT = shenyu-system `system_users.id`（ADR-056） | ADR-056 |

### 1.2 已拍板决议（执行时不得回退）

| ID | 决议 | 对工作计划的含义 |
|----|------|------------------|
| **D-DEDUP-01** | 不平行建设 Football 管理能力 | Phase A 删/藏平行管理页；Phase C 删平行 Controller |
| **D-INF-01** | 文件统一 `FileApi` / `/infra/file` | A 可先改前端调用；C 淘汰本地盘（API **已有**，可较早切） |
| **D-AUTHOR-01** | 作者 OPS **只读不写** | A/C 立即停写主数据；管理归 Football Admin |
| **D-DING-02** | 钉钉通讯录同步 **不做** | A/C 立即隐藏/410；**不做** Feign |
| **D-SYS-03** | Token **不新建** introspect；Gateway / `check` | C 鉴权切轨，不新建接口 |

完整拍板与缺口人日 → [MUST-HAVE §2 / §5](./OPS-FOOTBALL-RPC-MUST-HAVE.md)。

### 1.3 执行铁律

1. **先可逆、后不可逆**：标记废弃 / hide / 停写 → 切轨验证绿 → 再物理删除。
2. **前端 Admin 直调 ≠ 后端 Feign**：UserSelect/DictSelect 可走 `/admin-api`；`@InDict`、IP 组候选、文章写等服务端路径必须等 G-* Feign（MUST-HAVE §7）。
3. **不跨阶段硬删**：Phase C 的 `@DS` 删除依赖对应 G-*；Phase B 的表物理删依赖 harness/读路径结案。
4. **一片一会话仍适用业务 Slice**；本计划是跨模块工程轨，按工作包拆会话，勿一次清空多库。

---

## 2. 阶段总览

```text
Phase A 前端合并与清理  ──►  Phase B 数据库结构清理  ──►  Phase C 后端合并与清理
     │                              │                         │
     │ 可立即启动                   │ 停写/规范可立即；         │ 切轨依赖 Football
     │                              │ 物理删/所有权移交分批     │ G-* 就绪后才能删 @DS
     ▼                              ▼                         ▼
 football-front 单源            wd 停写 + Flyway 政策      Feign 替换 + 单库 wd
 平行页删除 / Admin 直调        禁止跨库写 shenyu-*        multidb 配置删除
```

| 阶段 | 名称 | 可启动条件 | 粗估（人周，规划用） |
|------|------|------------|----------------------|
| **A** | 前端合并与清理 | **现在**（不依赖新 Feign） | **2–4** |
| **B** | 数据库数据与结构清理 | A 启动后即可并行启动「停写」；物理删/移交在读路径结案后 | **1–2**（不含大规模数据迁移） |
| **C** | 后端合并与清理 | Football §7 提案评审通过并按 G-* 交付；与 A/B 可部分重叠但**删 @DS 不可抢跑** | **4–8**（OPS 侧；Football 另计 16–38 人日） |

> Football 缺口合计约 **16–38 人日**（MUST-HAVE §5）**不含**在上表 OPS 人周内。G-DING-01 主导上限，可排期靠后。

**并行诚实说明**

| 事项 | 现在能否做 | 说明 |
|------|------------|------|
| 前端源合并进 football-front | ✅ 能 | 不依赖 Feign |
| DictSelect / UserSelect 走 Admin | ✅ 能 | 浏览器直调已有 |
| 文件前端改 `/infra/file` | ✅ 基本能 | API 已有（D-INF-01）；联调需 Gateway/租户对齐 |
| DB 标记停写 / deprecated | ✅ 能 | 见 Phase B-WP1 |
| 删除 `@DS` Mapper / 多库 yml | ❌ **不能** | 等对应 G-* + cutover 验收 |
| Supersede ADR-050 §3.1 | ✅ 已拍板 | [ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) 选项 C（2026-07-28）；G-* 白名单 |

---

## 3. Phase A — 前端合并与清理（优先）

### 总目标

OPS UI 以 **football-front 为唯一源**；去掉平行管理页与废弃路由；选择器/字典/文件调用对齐 Football Admin；standalone `:3000` 降为非 Gate。

**CLEANUP 映射**：§4 前端 · §5 菜单/权限 · §6 脚本/E2E · P0-2/P0-3/P0-7。

---

### A-WP1　壳内单源与 mount 退役路径

| 项 | 内容 |
|----|------|
| **目标** | `ops-platform-ui-vue` 源码进入 football-front；`mount-ops-all.py` 过渡后退役 |
| **前置依赖** | 无 Feign 依赖；需 football-front 仓库写权限与合并约定（目录 `/ops` 或既有挂载结构） |
| **涉及路径 / CLEANUP** | `ops-platform-ui-vue/**` · `scripts/mount-ops-all.py`（CLEANUP §4）· `src/utils/ops-route.ts` |
| **验收标准** | Gate/日常开发以 Football `:5777` + Gateway 打开 OPS 路由；mount 仅 CI 兼容或已删；文档写明非单源路径为过渡 |
| **风险 / 回滚** | 合并冲突、路径前缀 `/ops` 断裂 → 保留 mount 脚本一版 + git 分支回滚；勿同时删 standalone 直到冒烟绿 |

**任务清单**

- [x] 约定 football-front 内 OPS 源目录与构建入口（过渡：`ops-platform-ui-vue` 为源 → mount 到 football-front `ops`；见 FOOTBALL-OPS-BRANCH.md）
- [x] 完成首轮源合并（或确立「football-front 为主、OPS 仓同步」的单向流程）（过渡单向：改 ui-vue → mount；终态单源待退役 mount）
- [x] 梳理 `ops-route.ts` standalone 分支；壳内前缀行为文档化（平行管理 URL → Football Admin）
- [x] 冒烟：登录 → OPS 业务菜单 → 至少 2 个 P0 页可打开（Gate 走 `:5777` + Gateway；M6 E2E 2026-07-27 已绿主路径）
- [ ] 单源稳定后：计划删除/归档 `mount-ops-all.py`（CLEANUP P2-2）

---

### A-WP2　平行管理页与废弃路由清理

| 项 | 内容 |
|----|------|
| **目标** | 落实 D-DEDUP-01：用户/角色/租户/作者管理不下沉；删除或永久隐藏废弃页 |
| **前置依赖** | 无；菜单侧 6137–6139/6155 已摘（CLEANUP §5） |
| **涉及路径 / CLEANUP** | `views/system/UserManage.vue` · `RoleManage.vue` · `TenantManage.vue` · `views/operations/AuthorRedirect.vue` · `api/system-user.ts` · 路由 `deprecated`/`hideInMenu`（CLEANUP §4、P0-2） |
| **验收标准** | 上述路由不可从菜单进入；外链抽检无 404 业务依赖；`system-user` CRUD API 客户端标 deprecated 或已删 |
| **风险 / 回滚** | 书签/旧链 → 保留 Redirect 到 Football Admin 一版；代码可用 git 恢复 |

**任务清单**

- [x] 确认 User/Role/Tenant 路由保持 `hideInMenu` + deprecated（改为 `FootballAdminRedirect` → `#/system/user|role|tenant`）
- [x] 确认 AuthorRedirect → Football `#/author/info`（书签兼容保留 Redirect 页）
- [x] 删除或停用 `api/system-user.ts` CRUD 调用方（文件标 deprecated；路由不再挂 CRUD 页）
- [x] 残留组件引用 grep（操作日志/登录日志/字典管理页）→ 无引用则结案（2026-07-28 grep：平行 CRUD 页无业务引用）
- [ ] 环境抽检：菜单无 6137–6139/6155；角色未绑 `oa:user:*` / `oa:dept:*` / `oa:dict:update` 等平行权限（CLEANUP §5、P0-3）

---

### A-WP3　DictSelect / 字典前端切 Admin（可立即）

| 项 | 内容 |
|----|------|
| **目标** | 前端字典读直调 Football `/admin-api/system/dict-*`；不再依赖 `/oa/dict` 薄封装（管理不下沉） |
| **前置依赖** | **无新 Feign**；Gateway 已暴露 system dict Admin。**注意**：后端 `@InDict` 仍属 Phase C + G-DICT-01 |
| **涉及路径 / CLEANUP** | `api/dict.ts` · `DictSelect.vue`（CLEANUP §4 · P1-3 前端半段） |
| **验收标准** | 业务表单 DictSelect 选项正常；网络面板走 `/admin-api/system/dict-*`；`/oa/dict` 仅残留可标 deprecated |
| **风险 / 回滚** | 字段/排序差异 → 对照 DTO 后兼容映射；可双读开关回退 OPS 封装 |

**任务清单**

- [x] DictSelect 改为 Football Admin API（`GET /system/dict-data/type`）
- [x] 清理 `api/dict.ts` 无用路径或标注 deprecated（`/oa/dict` 标 deprecated 回退）
- [ ] 抽样平台类型等业务字典展示与排序

---

### A-WP4　文件上传/预览改 infra（可较早，API 已有）

| 项 | 内容 |
|----|------|
| **目标** | 前端上传/预览对齐 D-INF-01：`/admin-api/infra/file` |
| **前置依赖** | Football `FileApi` / Admin **已有**；需租户与 Gateway 路由对齐（G-INF-01 契约 0–2 人日，可能已够用）。**后端** `LocalFileStorageService` 淘汰在 Phase C |
| **涉及路径 / CLEANUP** | `api/file.ts` · `ImageUploadField` 等（CLEANUP §4 · P1-4 前端半段） |
| **验收标准** | 上传返回 infra url/path；预览可用；新业务不再写本地 `/oa/file` 作为唯一路径 |
| **风险 / 回滚** | 历史本地 key 预览失败 → 过渡期保留 `/oa/file/view` 代理或双读 |

**任务清单**

- [x] `api/file.ts` 切 `/admin-api/infra/file/upload`（及预签名若需要）
- [ ] 业务上传组件联调（任务附件、内容配图等抽样）
- [x] 文档注明：旧 `/oa/file` 仅为过渡（`file.ts` / ImageUploadField 注释）

---

### A-WP5　Harness / E2E / 文档（前端侧）

| 项 | 内容 |
|----|------|
| **目标** | Standalone `:3000/:8080` 标注非 Gate；E2E 迁 Football 壳 |
| **前置依赖** | A-WP1 至少有可跑的壳内路径 |
| **涉及路径 / CLEANUP** | `tests/uat-browser-gap.spec.ts` · `run-uat-browser-e2e.ps1` · `start-stop-*` · `OPS-DEV-DEPLOY-GUIDE.md`（CLEANUP §6、P0-1、P0-7） |
| **验收标准** | Gate 文档与脚本默认 Integration；standalone 套件归档或标非 Gate |
| **风险 / 回滚** | 低；文档/脚本级可逆 |

**任务清单**

- [ ] 标注 `start-ops-standalone.ps1` / UAT standalone 非 Gate
- [ ] E2E 默认基址改为 `:5777` + Gateway（或双 profile）
- [ ] 部署指南页首：五库 `@DS` = 过渡；目标 = 仅 wd + RPC；链 MUST-HAVE + CLEANUP + **本文**

---

### Phase A 验收门禁（整阶段）

- [ ] football-front 单源（或已冻结 mount 退役日期）
- [ ] 平行管理页不可达；菜单/权限抽检通过
- [ ] DictSelect 走 Admin；文件走 infra（或明确过渡开关 + 期限）
- [ ] Gate 脚本不再默认 standalone
- [ ] CLEANUP §4 可立即项勾选完成

### Phase A 建议人周

| 工作包 | 人周 |
|--------|------|
| A-WP1 单源 | 1–2 |
| A-WP2 删页/权限 | 0.5 |
| A-WP3 字典 | 0.3–0.5 |
| A-WP4 文件 | 0.5–1 |
| A-WP5 文档/E2E | 0.3–0.5 |
| **合计** | **约 2–4** |

---

## 4. Phase B — 数据库数据与结构清理

### 总目标

生产路径对 Football SSOT 表 **停写**；澄清 Flyway 所有权；在读依赖结案后再归档/删表。  
**不做**：在 Feign 未就绪时要求 OPS 停止读 football 库（读仍由 Phase C `@DS` 承担，直至切轨）。

**CLEANUP 映射**：§3 数据结构 · P0-4 · P2-3。

---

### B-WP1　立即停写 / 规范（可与 Phase A 并行）

| 项 | 内容 |
|----|------|
| **目标** | 防止继续污染非 SSOT 表 |
| **前置依赖** | **无 RPC 依赖**；需研发规范 + code review 门禁 |
| **涉及路径 / CLEANUP** | CLEANUP §3.1：`wd.system_users` overlay · `wd.oa_author`（非 ext）· `wd.sys_dict_*` 作为 SSOT 新写 · `wd.sys_operation_log` 本地写 |
| **验收标准** | 规范文档 + PR checklist；抽检无新写入；作者扩展只写 `oa_author_ext` |
| **风险 / 回滚** | 误伤合法写 → 白名单 `oa_*_ext` / OPS 自建表（CLEANUP §3.2） |
| **规范 SSOT** | [OPS-FOOTBALL-STOP-WRITE-POLICY.md](./OPS-FOOTBALL-STOP-WRITE-POLICY.md) |

**任务清单**

- [x] 发布停写规范文档 + PR checklist（2026-07-28 → STOP-WRITE-POLICY.md）
- [x] 禁止新写 `wd.system_users`（ADR-056）；SSOT = shenyu-system（规范 §2.1）
- [x] 禁止新写 `wd.oa_author`（非 ext）；扩展走 `oa_author_ext`（规范 §2.2 · D-AUTHOR-01）
- [x] 业务字典变更：**勿只写** `wd.sys_dict_*`；走 shenyu-system（规范 §2.3；所有权见 B-WP3）
- [ ] 审计 `sys_operation_log`：确认无读依赖后计划停本地写（与 CLEANUP P0-6 / C 侧删除双写联动）
- [x] `wd.sys_user` / token 等：生产停用说明；仅 H2 IT fallback（规范 §2.1；物理删等 harness 退役）
- [ ] PR 门禁：至少一次抽检无违规新写（规范 §5）

---

### B-WP2　可先做 vs 须等 RPC 的边界（诚实表）

| 动作 | 可否先做 | 原因 |
|------|----------|------|
| 文档/注释标记 deprecated、停写规范 | ✅ 先做 | 无运行时依赖 |
| 角色解绑平行权限码 | ✅ 先做 | 与前端 A-WP2 同步 |
| 停止 OPS Flyway **新**跨库写 shenyu-* | ✅ 先做（政策） | CLEANUP §3.3；历史 V152/V158 保留 |
| 物理删除 `wd.sys_dict_*` / `sys_operation_log` / legacy `sys_user` | ❌ 后做 | 需确认无读、IT/harness 退役 |
| 要求 OPS 去掉对 `author_user` / `mp_account` 等的 **直连读** | ❌ 属 Phase C | 等 G-MEM-* / G-MP-* / G-PAY-* |
| 删除业务表上 userId 桥接列 | ❌ 后做 | ADR-056 全量 Football id 存储并验证后；先停写桥逻辑 |

---

### B-WP3　Flyway / seed 所有权移交

| 项 | 内容 |
|----|------|
| **目标** | 字典与系统主数据变更改由 Football/运维维护；OPS Flyway 禁止再 `INSERT INTO \`shenyu-system\`.*` |
| **前置依赖** | 与 Football 运维约定接手人；**不阻塞** A；建议在 C 大规模切轨前完成政策 |
| **涉及路径 / CLEANUP** | `V152` · `V158` · `V137` · `seed-oa-system-menu.sql`（CLEANUP §3.3、P2-3） |
| **验收标准** | 书面政策生效；新 migration review 检查清单含「禁止跨库写 shenyu-*」；6100+ OPS 菜单 seed **可保留** |
| **风险 / 回滚** | 字典漏同步 → 临时运维脚本（非 OPS Flyway 仿 V152） |

**任务清单**

- [ ] 发布 Flyway 政策（禁止跨库写）
- [ ] 字典变更流程移交 Football/运维
- [ ] 菜单：保留 OPS 6100–6999；勿再同步 Football 原生菜单树
- [ ] 角色：SSOT = Football；OPS 仅文档化对齐，不长期双维护

---

### B-WP4　RPC cutover 后的归档删除（排在 Phase C 验收后）

| 项 | 内容 |
|----|------|
| **目标** | 归档无用表/桥接；不提前删 |
| **前置依赖** | **Phase C cutover 绿** + harness 退役 |
| **涉及路径 / CLEANUP** | §3.4 桥接逻辑；legacy `sys_user*`；确认后的 `sys_operation_log` |
| **验收标准** | 有归档 SQL/说明；回滚脚本或备份 |
| **风险 / 回滚** | 高 — 必须备份后执行 |

**任务清单**

- [ ] 列出待归档表与依赖查询
- [ ] 备份 → 归档 → 应用只读探测
- [ ] 更新 `docs/sql` / schema 导出（若团队在用）

---

### Phase B 验收门禁

- [x] 停写规范落地（B-WP1 文档 + checklist；抽检待办）
- [ ] Flyway 跨库写政策生效（B-WP3）
- [ ] 物理删除仅在 C cutover 后按清单执行（B-WP4）
- [ ] CLEANUP §3「可立即」项完成；「依赖/cutover」项未提前勾删

### Phase B 建议人周

约 **1–2**（规范 + 所有权 + 审计；不含大表迁移）。

---

## 5. Phase C — 后端合并与清理（最后主战场）

### 总目标

按 G-* 将 `@DS` 直连替换为 Feign；鉴权改 Gateway/`check`；cutover 后删除多库配置与废弃代码。  
**明确禁止**：Football API 未就绪时删除 `@DS` Mapper / `application-*-multidb` 非 master 数据源。

**CLEANUP 映射**：§1 后端 · §2 数据源 · P1-* · P2-*。

**Football 并行轨（非本阶段「可删」前提可抢，但是「可切」前提）**

| 缺口 | 阻塞的 C 工作包 | Football 粗估 |
|------|-----------------|---------------|
| G-SYS-01/02 | C-WP2 | 1–2 + 2–4 人日 |
| D-SYS-03（Gateway/`check`，不新建） | C-WP1 | OPS+Gateway 联调 |
| G-DICT-01 | C-WP3 | 1–3 |
| G-INF-01 | C-WP4 | 0–2（API 已有） |
| G-MEM-01/02/03 · G-MP-01 · G-PAY-01 | C-WP5 | 见 MUST-HAVE §5 |
| G-DING-01 | C-WP6 | 8–15（可后置） |

~~建议：**C 启动前**正式 **Supersede ADR-050 §3.1**，否则 Football 补 API 无架构依据。~~ **已拍板**（2026-07-28）：[ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) 选项 C — G-* 白名单有限 Supersede。

---

### C-WP0　可立即标记废弃（不删 @DS）

| 项 | 内容 |
|----|------|
| **目标** | 与 A/B 对齐：平行管理 API、钉钉通讯录、作者写、操作日志双写 → 410/隐藏/`@Deprecated` |
| **前置依赖** | **无 Feign**；可与 A 并行 |
| **涉及路径 / CLEANUP** | §1.5–1.9 · P0-5 · P0-6：`UserController`/`RoleController`/`DeptController`（含钉钉 sync）· 作者写 · `OperationLogRecorder` · `DingTalkDevController` |
| **验收标准** | 生产路径不可达或 410；IT 若依赖则改测或标 harness-only |
| **风险 / 回滚** | 低（标记级） |

**任务清单**

- [x] 平行 system 管理 Controller 标 `@Deprecated`（User/Role/Dept；钉钉 sync 方法已标废弃，C-WP0 2026-07-28）
- [ ] 平行 system 管理 Controller：410 或隐藏（保留 `ParamController`）
- [ ] 钉钉通讯录 sync：**删除/410**（D-DING-02）
- [ ] 作者主数据 CUD：禁止新写；保留 `AuthorExtController`
- [ ] 审计并去掉 `sys_operation_log` 双写计划落地
- [ ] DevAuth 生产路径标注废弃（IT 可暂留）

---

### C-WP1　鉴权切轨（D-SYS-03）

| 项 | 内容 |
|----|------|
| **目标** | `FootballAuthProvider` 消费 Gateway login-user 或 `checkAccessToken`；停止 DB/Redis 直读 token |
| **前置依赖** | Gateway 透传头稳定 **或** `OAuth2TokenCommonApi.checkAccessToken` 可用 — **不新建** introspect |
| **涉及路径 / CLEANUP** | §1.2 · P1-1：`FootballAuthProvider` · `FootballOAuth2TokenMapper` · Redis reader · 配置 `oa.auth.football-redis` |
| **验收标准** | Integration 登录链路绿；无 `@DS("system")` 读 token；失败可回退开关一版 |
| **风险 / 回滚** | 高 — 保留双路径 feature flag；验证绿再删 Mapper |

**任务清单**

- [ ] 实现 Gateway 头消费（推荐）或 `check` 调用
- [ ] 双跑对比（旧直读 vs 新路径）抽样
- [ ] 停用 Token Mapper / Redis 直读
- [ ] **勿**在验证前删除 multidb `system` 数据源

---

### C-WP2　用户 lookup / 校验 Feign（G-SYS-01/02 · ADR-056）

| 项 | 内容 |
|----|------|
| **目标** | 后端 simple-list + assert-enabled / 角色校验走 Feign；Validator 去掉 `@DS` 唯一依赖 |
| **前置依赖** | **G-SYS-01、G-SYS-02 已实现**（前端 Admin **不能**替代） |
| **涉及路径 / CLEANUP** | §1.1 · §1.6 · P1-2：`FootballSystemUserLookupMapper` · `FootballSystemUserValidator` · `SysUserMapper` 引用收敛 |
| **验收标准** | IP 组候选/写入校验 IT 绿；生产无 system DS 用户读 |
| **风险 / 回滚** | 数据权限「全量启用用户」开关（MUST-HAVE §7.1 开放问题）未决则阻塞 — 需 Football 评审拍板 |

**任务清单**

- [x] 接入 Feign simple-list（G-SYS-01 首切片：`AdminUserApi` vendored + `listEnabledUsersInTenant` 双跑，2026-07-28）
- [x] 接入 assert / hasAnyRoles（G-SYS-02 第二切片：`PermissionCommonApi` + `getUser`/`validateUserList`/`getUserListByRoleId` 双跑，2026-07-28）
- [ ] Validator 终态仅 Football id + Feign；legacy union 仅过渡
- [ ] 删除 `FootballSystemUserLookupMapper`（验证后）
- [ ] 昵称展示类改 UserApi（禁止 SysUserMapper 作唯一写入校验）

---

### C-WP3　字典后端 Feign（G-DICT-01）

| 项 | 内容 |
|----|------|
| **目标** | `@InDict` / `SystemDictAdapter` 走 `DictDataApi`；删除 Football Dict `@DS` Mapper |
| **前置依赖** | **G-DICT-01**；前端 A-WP3 建议已完成（降低联调噪音） |
| **涉及路径 / CLEANUP** | §1.4 · P1-3 |
| **验收标准** | 字典校验 IT/业务抽样绿；可删 system dict Mapper |
| **风险 / 回滚** | `sort` 等 DTO 字段缺失 → 阻塞删除，先扩 DTO |

**任务清单**

- [x] 接入 Feign `DictDataApi` list + valid 双跑（G-DICT-01：`SystemDictAdapter` `@InDict` 读路径，2026-07-28）
- [ ] 删除 Football Dict `@DS` Mapper（验证后）

---

### C-WP4　文件后端切 FileApi（G-INF-01 / D-INF-01）

| 项 | 内容 |
|----|------|
| **目标** | 淘汰 `LocalFileStorageService`；`/oa/file` 代理或下线 |
| **前置依赖** | FileApi **已有**；A-WP4 前端已切则更顺；契约/租户对齐 |
| **涉及路径 / CLEANUP** | §1.3 · P1-4 |
| **验收标准** | 上传下载走 infra；本地盘配置可删；`ImageKeyHelper` 前缀更新 |
| **风险 / 回滚** | 历史本地文件 → 迁移脚本或长期只读代理 |

**任务清单**

- [x] Vendor `FileApi` + `FileCreateReqDTO`（G-INF-01：`framework/common/biz/infra/file`，2026-07-28）
- [x] `OaOperateLogConfiguration` 注册 Feign + `application-dev-nacos-local.yml` infra-server URL
- [x] `LocalFileStorageService` 上传/预签名读 Feign 双跑（内容图、任务附件；本地盘回退）
- [x] `ImageKeyHelper` 兼容 infra 完整 URL；`/oa/file/view|download` 远程 key 走预签名重定向
- [ ] 删除 `LocalFileStorageService` 本地盘与 `/oa/file` 代理（验证后）
- [ ] 存量本地 key 迁移或长期只读代理策略落地

---

### C-WP5　member / mp / pay 域切轨（G-MEM-* · G-MP-01 · G-PAY-01）

| 项 | 内容 |
|----|------|
| **目标** | 作者只读、文章写、公众号 page/写、订单列表全部 Feign；删除对应 `@DS` |
| **前置依赖** | **对应 G-* 全部就绪**；作者 **无 CUD Feign**（D-AUTHOR-01） |
| **涉及路径 / CLEANUP** | §1.1 · P1-5：`MemberAuthorReadService` · `MemberArticleWriteService` · `MpAccountDataService` · `FootballPayAllOrderReadMapper` 等 |
| **验收标准** | 内容生产同步、公众号编排+ext、订单只读归因链路绿；member/mp/pay DS 可删 |
| **风险 / 回滚** | 字段对齐失败 → 按 MUST-HAVE §7 对表；双写窗口尽量短 |

**任务清单（按依赖排序建议）**

- [ ] G-MEM-01/02 作者只读 + authorLevel → 删 member 读直连
- [ ] G-MEM-03 文章写 → 删 `AuthorArticleMapper` 写路径
- [ ] G-MP-01 公众号 → 删 mp Mapper；ext 仍落 wd
- [ ] G-PAY-01 订单列表 → 删 pay 读 Mapper

---

### C-WP6　钉钉通用推送（G-DING-01，可后置）

| 项 | 内容 |
|----|------|
| **目标** | 本地 DingTalk Client 收敛到统一 Feign |
| **前置依赖** | **G-DING-01**（新建，周期长）；通讯录 **不做** |
| **涉及路径 / CLEANUP** | §1.9 · P1-6 |
| **验收标准** | 任务/审核通知走统一 API；本地 Client 删除 |
| **风险 / 回滚** | 可延期；未就绪前保留本地 Client，**不影响**「仅 wd + 其他域 RPC」主目标的大部分 |

---

### C-WP7　Cutover 后物理删除（P2）

| 项 | 内容 |
|----|------|
| **目标** | 单库 `wd` + 删除废弃代码/配置 |
| **前置依赖** | C-WP1–5（及可选 6）验收绿 |
| **涉及路径 / CLEANUP** | §2 · P2-1–P2-5：`application-dev-local-multidb.yml` 非 master · `oa-server-remote-multidb.yaml` · smoke Mapper · deprecated Controller · Supersede ADR-050 §3.1 |
| **验收标准** | OPS 进程仅 master→wd；integration 绿；文档目标态一致 |
| **风险 / 回滚** | 保留上一版 multidb 配置包只读归档一周 |

**任务清单**

- [ ] 删除非 master 数据源与推送脚本目标态
- [ ] 删除 smoke Mapper、无用 Controller
- [ ] Standalone/dev-token 生产路径下线（IT 另 profile）
- [x] 正式 ADR：Supersede ADR-050 §3.1（[ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) 2026-07-28）
- [ ] 联动 Phase B-WP4 表归档

---

### Phase C 验收门禁

- [ ] 无生产路径 `@DS("system|member|mp|pay")`
- [ ] 鉴权无 token 表/Redis 快照直读
- [ ] MUST-HAVE 必须域（除可选延期的 G-DING-01）切轨绿
- [ ] 配置仅 wd；CLEANUP P1/P2 勾选
- [x] ADR-050 §3.1 已有限 Supersede（ADR-050-REV1 G-* 白名单）

### Phase C 建议人周（OPS）

| 工作包 | 人周 |
|--------|------|
| C-WP0 标记废弃 | 0.5 |
| C-WP1 鉴权 | 0.5–1 |
| C-WP2 用户 | 1–1.5 |
| C-WP3 字典 | 0.5 |
| C-WP4 文件 | 0.5–1 |
| C-WP5 业务域 | 1.5–3 |
| C-WP6 钉钉 | 0.5–1（等 API） |
| C-WP7 物理删 | 0.5 |
| **合计** | **约 4–8** |

另：Football **16–38 人日**需单独排期，且 **阻塞** C-WP2/3/5/6。

---

## 6. 依赖关系一览（防抢跑）

```text
                    ┌─────────────────────┐
                    │ Football §7 评审     │
                    │ + 按 G-* 交付        │
                    └─────────┬───────────┘
                              │
Phase A (前端) ───────────────┼──────────────► 单源 / Admin 直调 / 文件前端
                              │
Phase B 停写/政策 ────────────┤──────────────► 可与 A 并行
                              │
Phase C 标记废弃(C-WP0) ──────┤──────────────► 可与 A 并行
                              │
         G-SYS / DICT / MEM… ─┴──► C-WP1..5 切轨 ──► C-WP7 删 @DS/multidb
                                                          │
                                                          ▼
                                                    B-WP4 表物理归档
```

| 错误做法 | 正确做法 |
|----------|----------|
| 先删 `@DS` 再等 Feign | Feign 验收 → 双跑 → 再删 |
| 前端未单源就删 mount 且无回滚 | 单源冒烟绿再退役 mount |
| 前端 DictSelect 已切就宣称 `@InDict` 完成 | 后端仍走 G-DICT-01 |
| DB 先 drop `sys_dict_*` | 先停写 + 读结案 + 再归档 |

---

## 7. 近期 1–2 周行动（Phase A 立刻做）

> 目标：两周内拿下「可立即」前端清理 + 单源启动，并为 B/C 铺路。

### 7.1 RPC 状态快照（Football `ops` 分支 · 2026-07-28 代码检）

| G-* | 状态 | 说明 |
|-----|------|------|
| **G-SYS-01** | ✅ | `AdminUserApi.getSimpleUserList` 已在 Football `ops` 交付；OPS 侧 C-WP2 首切片已接 Feign 双跑 |
| **G-SYS-02** | ⚠️ | `getUser` · `getUserListByRoleId(roleId)` · **`hasAnyRoles` 已有**；**真缺**：`assert-enabled` · 按 **roleCode** 列用户（`user-ids-by-role-code` 或 OPS roleId 映射） |
| **G-DICT-01** | ⚠️ | `DictDataApi` 可读；OPS C-WP3 **`list`/`valid` 双跑 ✅**（`@InDict` 读路径）；`typeExists`/管理写仍 @DS |
| **G-INF-01** | ✅ OPS 双跑 | `FileApi` / Admin `/infra/file` 已有；OPS `LocalFileStorageService` Feign 双跑 ✅（2026-07-28）；本地盘待 cutover 删除 |
| **G-MEM-01** | ✅ | `AuthorSimpleRespDTO.authorLevel` 已在 Football `ops` 交付 |
| **G-MEM-02** | ⚠️ | 前端可走 Football `AuthorUserController`/VO；服务端 RPC `simple-list` **可选**（只读够用 Admin 时可后置） |
| **G-MEM-03** | ✅ Football已有 / OPS未接 | `ArticleApi` create/update/status-change 已在 Football `ops` 实码；OPS 仍 `@DS` 写 |
| **G-MP-01** | ✅ Football已有 / OPS未接 | `MpAccountInfoApi` page/create/update/get 已在 Football `ops` 实码；OPS 仍 mp `@DS` |
| **G-PAY-01** | ✅ Football已有 / OPS未接 | `PayOrderApi.getOrderPage` 已在 Football `ops` 实码；OPS 未接 Feign，字段对表后切轨 |
| **G-DING-01** | ⚠️ | OPS 本地 DingTalk Client **不阻塞**整包；`ding_user_id` 桥接待做 |
| **D-SYS-03** | ⚠️ | Gateway/`check` 路径未切轨；token 仍 @DS 直读 |

> **Phase C 整包 NO-GO**：OPS 侧 G-* Feign 未 cutover（删 `@DS`/multidb 未完成）+ §8.7 仍缺 Integration 验收；**单片** C-WP0 / C-WP2（G-SYS-01 双跑）可执行。**B-ADR-050 已解除**（2026-07-28 ADR-050-REV1）。

### Week 1

| # | 动作 | 工作包 | Owner 建议 |
|---|------|--------|------------|
| 1 | 冻结单源方案（目录、构建、谁为 SSOT 仓） | A-WP1 | 前端 + 架构 |
| 2 | 平行页/路由/API deprecated 核查并删无用引用 | A-WP2 | 前端 |
| 3 | 环境菜单/角色平行权限抽检 | A-WP2 / CLEANUP P0-3 | 前端 + 运维 |
| 4 | 部署指南/启动矩阵加「过渡 vs 目标」banner，链本文 | A-WP5 | 任意 |
| 5 | 拉齐 Football：§7 提案评审排期（为 C 铺路，**不阻塞 A**） | — | 架构 + Football |

### Week 2

| # | 动作 | 工作包 | Owner 建议 |
|---|------|--------|------------|
| 1 | 推进 football-front 源合并首版可跑通 | A-WP1 | 前端 |
| 2 | DictSelect 切 Admin | A-WP3 | 前端 |
| 3 | 文件上传切 infra（联调 Gateway） | A-WP4 | 前端 |
| 4 | Standalone E2E/脚本标非 Gate | A-WP5 | 前端 |
| 5 | 启动 B-WP1 停写规范（与前端并行，半日级） | B-WP1 | 后端 |
| 6 | 启动 C-WP0 标记废弃（410/隐藏，不删 @DS） | C-WP0 | 后端 |

**两周结束时应具备**

- [ ] OPS 页可在 Football 壳稳定打开（至少主路径）
- [ ] 平行管理 UI 对用户不可见
- [ ] Dict / 文件前端方向已切或有明确过渡开关
- [x] Football API 评审有日期；ADR-050 §3.1 Supersede 草稿已提（→ ADR-050-REV1 Accepted 2026-07-28）

- [x] OPS 页可在 Football 壳稳定打开（至少主路径）（2026-07-27 Gate E2E）
- [x] 平行管理 UI 对用户不可见（A-WP2 Redirect + hideInMenu）
- [x] Dict / 文件前端方向已切或有明确过渡开关（A-WP3/A-WP4 前端半段 ✅）
- [x] Football API 评审有日期；ADR-050 §3.1 Supersede 草稿已提（→ ADR-050-REV1 Accepted 2026-07-28）

---

## 8. 执行状态与 §7 反馈修订（2026-07-28）

### 8.1 Readiness

| 维度 | 结论 |
|------|------|
| **Phase A** | **部分可执行** — 单源/mount 过渡、平行页、DictSelect、文件前端已完成；E2E/standalone 标非 Gate 待办 |
| **Phase B** | **可启动停写/政策**（B-WP1/B-WP3）；物理删等 C cutover |
| **Phase C 整包** | **NO-GO** — OPS 未 cutover G-MEM-03/MP/PAY Feign + 删 `@DS`/multidb；Integration 验收未完成；**禁止**整包抢跑删 multidb |
| **Phase C 单片** | **GO** — C-WP0 标记废弃；C-WP2 G-SYS-01 Feign 双跑（本日已落地首切片） |

### 8.2 用户反馈纳入（2026-07-28）

| ID | 反馈 | 对工作计划的影响 |
|----|------|------------------|
| **G-SYS-01** | `deptName` **非阻塞** | Feign DTO 可先不含 deptName；IP 组候选以 id/nickname 为主 |
| **G-DICT-01** | `sort` **非阻塞** | 后端 Adapter→Feign 可先不切；排序差异不挡 Phase A |
| **G-SYS-02** | `hasAnyRoles` + `getUserListByRoleId` **已有**；**真缺** `assert-enabled` + 按 roleCode 列用户 | C-WP2：`hasAnyRoles` 可接 Feign；`assert-enabled` / roleCode 列用户待 Football 或 OPS 映射 |
| **G-MEM-01** | `AuthorSimpleRespDTO.authorLevel` **已有** | Football 已交付；OPS C-WP5 读路径可排期 |
| **G-MEM-02** | 前端 `AuthorUserController`/VO；服务端 RPC simple-list **可选** | Phase A 够用 Admin；member `@DS` 删除仍等 Feign 或 Admin 代理决策 |
| **G-DING-01** | OPS 本地钉钉 **不阻塞**；`ding_user_id` 桥接 **待做** | G-DING-01 可后置；通讯录 sync 仍 D-DING-02 不做 |
| **G-PAY-01** | `PayOrderApi.getOrderPage` **Football 已有** | C-WP5 阻塞在 OPS Feign 接入 + 字段对表，非 Football API 缺口 |

### 8.3 Football `ops` 分支已交付 API（2026-07-27 ~ 07-28）

| API / 字段 | 路径 / 说明 | 日期 |
|------------|-------------|------|
| `AdminUserApi.getSimpleUserList` | `GET /rpc-api/system/user/simple-list` | 07-27 |
| `AdminUserApi.getUser` | `GET /rpc-api/system/user/get` | 已有 |
| `AdminUserApi.getUserListByRoleId` | `GET /rpc-api/system/user/getUserListByRoleId` | 07-28 |
| `AuthorSimpleRespDTO.authorLevel` | member 作者 simple-list DTO | 07-28 |
| `OperateLogCommonApi.create` | OPS **已接入**（AL-05） | 已有 |

### 8.4 Gap 表：Phase A / B / C 完成 vs 剩余

| 阶段 | 已完成（2026-07-28） | 剩余 |
|------|----------------------|------|
| **A** | 平行页 Redirect/hideInMenu；DictSelect→Admin；`api/file.ts`→infra；mount 过渡文档化；P0 冒烟主路径 | standalone/E2E 标非 Gate；mount 脚本退役；菜单权限抽检 |
| **B** | — | B-WP1 停写规范 ✅（2026-07-28）；B-WP3 Flyway 跨库写政策；物理删等 C |
| **C** | C-WP0：`User/Role/Dept` `@Deprecated`；C-WP2：**G-SYS-01** + **G-SYS-02** Feign 双跑 ✅；C-WP3：**G-DICT-01** `DictDataApi` 双跑 ✅（`@InDict` 读路径）；C-WP4：**G-INF-01** `FileApi` 双跑 ✅（上传/预签名读；本地盘回退）；C-WP5 首切片：**G-PAY-01** + **G-MEM-03** + **G-MP-01** Feign 双跑 ✅（@DS 保留） | C-WP1 鉴权；C-WP4 物理删本地盘；C-WP7；删 `@DS` |

### 8.5 推荐执行顺序

```text
C-WP0（标记废弃，可与 A 并行）
    → C-WP2 切片（G-SYS-01 双跑 ✅ → G-SYS-02 角色校验 Feign）
    → B-WP1（停写规范，与前端并行）
    → A-WP5 / B-WP3（文档与 Flyway 政策）
    → Phase C 其余 G-*（Football 交付 / `ops` 分支已有 + OPS cutover 验收）
```

### 8.6 阻塞表（Blockers）

> **2026-07-28 决策更新**：D-ADR-050 **选项 C**（ADR-050-REV1 有限 Supersede §3.1 ✅）；D-G-SYS-01 **选项 B**（沿用 Admin 数据权限）；D-G-SYS-02 **选项 B**（Feign 双跑已落地）；D-G-PAY-01 **选项 A**（下一切片 C-WP5）；D-G-MEM-02 前端 Admin 够用；D-G-DING **延后**。

| ID | 阻塞项 | 类型 | 影响 | 解除条件 / 状态 |
|----|--------|------|------|-----------------|
| ~~**B-ADR-050**~~ | ~~ADR-050 §3.1 未 Supersede~~ | ~~流程~~ | ~~Phase C 整包 NO-GO；Football 拒扩 API 风险~~ | **已解除**（2026-07-28）：[ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) 选项 C — G-* 白名单 Supersede |
| ~~**B-G-SYS-02a**~~ | ~~`assert-enabled` RPC~~ | ~~Football API~~ | ~~写入前启用/租户校验仍 @DS~~ | **部分解除**：D-G-SYS-02 选项 B → `getUser`+`validateUserList` Feign 双跑 ✅；删 `@DS` 待 cutover |
| ~~**B-G-SYS-02b**~~ | ~~按 roleCode 列用户 RPC~~ | ~~Football API / OPS 映射~~ | ~~`listPresentableUserIdsByRoleCode` 仍 @DS~~ | **部分解除**：D-G-SYS-02 选项 B → roleCode→roleId + `getUserListByRoleId` Feign + legacy union ✅ |
| **B-G-MEM-03** | 文章写 Feign 未接入 | **OPS 集成** | 内容生产同步仍 `@DS` 写 | **部分解除**：C-WP5 `ArticleApi` create/update/status-change 双跑 ✅；删 `@DS` 待 cutover |
| **B-G-MP-01** | 公众号 Feign 未接入 | **OPS 集成** | mp `@DS` 不可删 | **部分解除**：C-WP5 `MpAccountInfoApi` get/create/update/appId 双跑 ✅；page 仍 @DS |
| **B-G-PAY-01** | 订单 Feign 未接入 | **OPS 集成** | pay 读 Mapper 不可删 | **部分解除**：D-G-PAY-01 字段对表 ✅ + `getOrderPage` 双跑 ✅；删 `@DS` 待 cutover |
| **B-GW-DEV** | 本地 `system-server` Feign URL | 环境 | Feign 双跑仅 dev-nacos-local 直连 `:48081`；无 Nacos 时走 @DS 回退 | Integration 起 system-server 或 Nacos 发现 |
| **B-FEIGN-IT** | H2 IT 无 system-server | 测试 | IT 自动回退 @DS（设计如此）；Feign 路径需 integration 手验 | `dev-nacos-local` + system-server 联调记录 |
| ~~**G-SYS-01 开放**~~ | ~~simple-list 忽略数据权限~~ | ~~产品~~ | ~~IP 组候选可能少于预期~~ | **已关闭**：D-G-SYS-01 **选项 B** — 与 Admin 同权限 |

### 8.7 Football RPC 实码审计（2026-07-28）

> 依据 `football-backend-saas` **`ops` 分支实码**（非 MUST-HAVE §7 提案状态）。§7 实现状态以此为准；MUST-HAVE 仍作契约说明书。

**Football 已有、OPS 未接（阻塞在 OPS 集成，见 §8.6 B-G-MEM-03 / B-G-MP-01 / B-G-PAY-01）**

| G-* | Football 实码 | OPS 现状 |
|-----|---------------|----------|
| G-MEM-03 | `ArticleApi` create / update / status-change | `@DS("member")` 写 `AuthorArticleMapper` |
| G-MP-01 | `MpAccountInfoApi` page / create / update / get | `@DS("mp")` |
| G-PAY-01 | `PayOrderApi.getOrderPage` | `@DS("pay")` 读 Mapper |
| G-SYS-02（部分） | `PermissionCommonApi.hasAnyRoles` | `hasRoleCode` Feign 双跑 ✅（2026-07-28）；legacy union 仍过渡 |

**Football 仍缺（真 API 缺口）**

| G-* | 缺什么 | 备注 |
|-----|--------|------|
| G-SYS-02 | ~~`GET .../user/assert-enabled`~~ · ~~按 roleCode 列用户 RPC~~ | **OPS 方案已拍板**：`getUser`+`validateUserList` · roleCode→roleId+`getUserListByRoleId`；专用 RPC 非 must-have |
| G-DING-01 | `DingTalkMessageApi` 通用推送 | **延后**（D-G-DING）；通讯录 sync 仍不做 |
| ~~G-SYS-01（可选）~~ | ~~simple-list `ignoreDataPermission`~~ | **已关闭**（D-G-SYS-01 选项 B） |

**非 Football 新 API、但阻塞整包 cutover**：D-SYS-03 Gateway/`check` 切轨 · OPS 各域 Feign 双跑与 Integration 验收 · 物理删 `@DS`/multidb。

---

## 9. 建议总工期（粗估）

| 轨道 | 工期 |
|------|------|
| Phase A | 2–4 人周 |
| Phase B（规范+政策；物理删后置） | 1–2 人周 |
| Phase C（OPS） | 4–8 人周 |
| Football G-*（并行） | 16–38 **人日** ≈ 3–8 人周 |
| **端到端（含并行）** | 约 **6–12 自然周**（视 G-DING-01 与 Football 排期） |

---

## 10. 关联文档与指针

| 文档 | 关系 |
|------|------|
| [OPS-FOOTBALL-MERGE-DECISIONS.md](./OPS-FOOTBALL-MERGE-DECISIONS.md) | **待拍板决策清单**（§8 阻点/选项；拍板前必读） |
| [OPS-FOOTBALL-STOP-WRITE-POLICY.md](./OPS-FOOTBALL-STOP-WRITE-POLICY.md) | **B-WP1 停写规范 SSOT**（PR checklist · grep 模式） |
| [OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md](./OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md) | **G-* Integration 手验清单**（Feign vs @DS · cutover 签字） |
| [OPS-FOOTBALL-RPC-MUST-HAVE.md](./OPS-FOOTBALL-RPC-MUST-HAVE.md) | 原则、拍板、缺口、接口说明书；**执行顺序以本文为准** |
| [OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md](./OPS-FOOTBALL-MERGE-CLEANUP-INVENTORY.md) | 清理对象与时机标签；工作包任务勾选时回写 Inventory |
| [OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md](./OPS-FOOTBALL-FULL-MERGE-RPC-ANALYSIS.md) | 完整分析归档；日常不以此排期 |
| ADR-056 / ADR-050 / ADR-050-REV1 | 用户 SSOT；多库总纲；§3.1 G-* 有限 Supersede |

### 工作包 ↔ CLEANUP / MUST-HAVE 速查

| 工作包 | CLEANUP | MUST-HAVE |
|--------|---------|-----------|
| A-WP1–5 | §4 §5 §6 · P0-1/2/3/7 | D-DEDUP-01 · D-INF-01（前端） |
| B-WP1–4 | §3 · P0-4 · P2-3 | §6 保留域 · ADR-056 |
| C-WP0 | §1.5–1.9 · P0-5/6 | D-AUTHOR-01 · D-DING-02 |
| C-WP1 | §1.2 · P1-1 | D-SYS-03 · §7.0 |
| C-WP2 | §1.1 §1.6 · P1-2 | G-SYS-01/02 · §7.1–7.2 |
| C-WP3 | §1.4 · P1-3 | G-DICT-01 · §7.3 |
| C-WP4 | §1.3 · P1-4 | G-INF-01 · §7.4 |
| C-WP5 | §1.1 · P1-5 | G-MEM/MP/PAY · §7.5–7.8 |
| C-WP6 | §1.9 · P1-6 | G-DING-01 · §7.9 |
| C-WP7 | §2 · P2-* | §1 原则结论 |

---

## 11. 维护约定

1. 每完成一个工作包：更新本文勾选 + CLEANUP 对应行日期。
2. Football 每交付一个 G-*：在 MUST-HAVE / 本文 C 依赖表打勾，再开对应 C-WP 切轨会话。
3. 发现 Spec 未写明的 API/字段：**停止实现**，记入阻塞表，勿用惯例补全（见工程规则）。
4. 版本演进：破坏性调整升 v1.x；仅勾选进度可只改日期。

---

**版本** v1.2 · **日期** 2026-07-28  
**下一步**：Integration 手验 G-PAY-01（须先起 pay-server :48085）；G-MEM/MP OPS 业务双跑；B-WP3 Flyway 政策 / C-WP1 鉴权切轨。手验清单 → [OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md](./OPS-FOOTBALL-INTEGRATION-FEIGN-CHECKLIST.md)。

### 8.8 G-PAY-01 字段对表（D-G-PAY-01 选项 A · 2026-07-28 ✅）

> Football 实码：`PayOrderApi.getOrderPage` · `POST /rpc-api/pay/order/page` · 入参 `OrderPageReqDTO` · 出参 `PageResult<AllOrderRespDTO>`。

| OPS 侧（`FootballPayAllOrderReadDO` / `FootballOrderListVO`） | Football `AllOrderRespDTO` | 对表 |
|--------------------------------------------------------------|---------------------------|------|
| `id` | `id` | ✅ 1:1 |
| `orderNo` | `orderNo` | ✅ |
| `userId` | `userId` | ✅ |
| `authorId` | `authorId` | ✅ |
| `amount` | `amount` | ✅ |
| `payAmount` | `payAmount` | ✅ |
| `status` | `status` | ✅ 0待支付/1成功/2失败 |
| `orderType` | `orderType` | ✅ 0方案/1订阅/2专栏 |
| `payTime` | `payTime` | ✅ |
| `createTime` | `createTime` | ✅ |
| `sourceTable`（VO 固定 `"pay_all_order"`） | — | ✅ OPS 本地填充，非 RPC 字段 |
| `tenantId`（Mapper WHERE） | Header `tenant-id` | ✅ Feign 租户拦截器 |

**入参映射**（OPS `listPayAllOrders` → `OrderPageReqDTO`）

| OPS | Football | 备注 |
|-----|----------|------|
| `pageNum` | `pageNo` | ✅ |
| `pageSize` | `pageSize` | ✅ max 100 |
| `authorId` | `authorId` | ✅ |
| `status` | `status` | ✅ |
| `startDate`/`endDate` | `createTime[0]`/`createTime[1]` | ✅ OPS 右开区间 → Feign 末秒 `endExclusive-1ns` |
| `tenantId` | Header | ✅ |

**结论**：OPS 列表所需 10 列 **100% 覆盖**；Feign 响应额外字段（`articleId`、`privilegeId`、`payType` 等）OPS 不使用。**D-G-PAY-01 字段对表签字 ✅**。
