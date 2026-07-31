# OPS × Football 合并 — 决策清单（待拍板）

| 字段 | 值 |
|------|-----|
| 版本 | v1.2 |
| 日期 | 2026-07-28 |
| 状态 | 部分已拍板（D-ADR-050 ✅ 2026-07-28） |
| 关联 | WORK-PLAN v1.2 §8 · §8.7 实码审计 |

> **用法**：逐项阅读背景与选项，在「决策栏」勾选或填写；无需跳转其他文档即可完成拍板。  
> 可选延伸阅读（纯路径，非链接）：`docs/delivery/OPS-FOOTBALL-MERGE-WORK-PLAN.md` · `docs/delivery/OPS-FOOTBALL-RPC-MUST-HAVE.md` · `docs/adr/ADR-050-Ops与Football多库复用总纲.md` · `docs/adr/ADR-056-Football用户身份SSOT.md`

---

## D-ADR-050　Supersede §3.1（允许 Football 为 OPS 增 Feign）

- **背景**  
  ADR-050 §3.1（2026-07-05）规定：**禁止**修改 `football-backend-saas` 业务代码，OPS 只能用 `@DS` 只读跨库或调用**既有** API，不得要求 Football 新增/改接口。  
  合并目标态（MUST-HAVE §1）要求 OPS **只访问 `wd`**，跨库一律 Feign；G-SYS-02（`assert-enabled` / roleCode 列用户）、G-DING-01 等仍为 Football **侧新增或扩展 RPC**；G-MEM-03 / G-MP-01 / G-PAY-01 **Football `ops` 分支已有实码**（2026-07-28 审计），阻塞在 OPS Feign 集成。  
  在 §3.1 未 Supersede 前，Phase C **整包 NO-GO**，Football 团队也有正当理由拒改 API。

- **现状**  
  - ADR-050 §3.1 仍为 **Accepted** 硬约束。  
  - Football `ops` 分支已交付部分 API（如 `getSimpleUserList`、`getUserListByRoleId`、`authorLevel`），属事实上的例外，但无正式 ADR 背书。  
  - WORK-PLAN §8.6 **B-ADR-050** 阻塞 Phase C 整包与 multidb 物理删除。

- **选项 A：正式 Supersede §3.1**  
  - **优点**：为 Football 补 G-* API 提供架构依据；Phase C 切轨、删 `@DS` 有法可依；与 MUST-HAVE / WORK-PLAN 目标一致。  
  - **缺点**：推翻 2026-07-05 用户原则，需架构/产品书面确认；Football 侧产生 16–38 人日增量工作。

- **选项 B：维持 §3.1，OPS 永久保留 `@DS` 只读**  
  - **优点**：Football 零改动；符合原 ADR 字面。  
  - **缺点**：与 D-DEDUP-01、单库 `wd` 目标冲突；运维双轨（multidb + Feign）；长期技术债。

- **选项 C：有限 Supersede（白名单 G-*）**  
  - **优点**：仅允许 MUST-HAVE §7 列出的 G-SYS/DICT/MEM/MP/PAY/DING 扩展；其他 Football 业务仍禁止改。  
  - **缺点**：需维护白名单；边界争议时仍可能扯皮。

- **推荐**  
  若合并目标仍是「OPS 只连 `wd` + RPC」，**选项 A 或 C** 在 Phase C 大规模切轨前必须完成；**选项 B** 与当前 WORK-PLAN 不兼容。

- **若选 A/C，后续动作**  
  1. 起草 ADR-050 修订或新 ADR（引用 MUST-HAVE §7 为允许范围）。  
  2. Football 按 G-* 排期交付；每交付一项再开 OPS C-WP 切轨会话。  
  3. WORK-PLAN §8.6 移除 **B-ADR-050**。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [x] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：产品/架构 · 2026-07-28（**选项 C**）

> **已拍板（2026-07-28）**：**选项 C — 有限 Supersede** — ADR-050 §3.1 对白名单 G-* RPC（MUST-HAVE §7）Supersede；OPS Phase C Feign 切轨合法；其余 Football 业务代码仍不在 OPS 仓修改。正式 ADR：[ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md)。**不**等同 Phase C 整包 GO — cutover 仍须 Integration 验收 + 删 `@DS`。

#### FAQ：Feign / RPC / `@DS` 怎么区分？

| 术语 | 含义 | OPS 目标态 |
|------|------|------------|
| **RPC** | Football 微服务暴露的 **`/rpc-api/*` HTTP 接口**（如 `ArticleApi`、`AdminUserApi`） | 跨库读写的**唯一**合法服务端路径 |
| **Feign** | OPS 侧 **Spring Cloud OpenFeign 客户端**，声明式调用上述 RPC | Phase C 切轨手段：vendored `*-api` + `@FeignClient` |
| **`@DS` 直连** | OPS 进程用 MyBatis **多数据源直读/写** football 库表 | **过渡态**；Feign 双跑验收绿后删除 |

浏览器直调 **`/admin-api`**（DictSelect、UserSelect）≠ 后端 Feign：仅前端可用；`@InDict`、写入校验、member/mp/pay 写路径**必须**走 RPC/Feign。

---

## D-G-SYS-02　按 roleCode 列用户 + assert-enabled（Football RPC vs getUserListByRoleId + 映射 vs 保留 @DS）

- **背景**  
  ADR-056 要求角色/用户校验走 shenyu-system；OPS 多处需「按角色 code 列用户」（如 IP 组数据权限、`listPresentableUserIdsByRoleCode`）及写入前 **assert-enabled**。  
  Football `ops` 分支（2026-07-28 实码审计）已交付：`getUser`、`getUserListByRoleId(roleId)`、**`PermissionCommonApi.hasAnyRoles`**。  
  **仍缺**：`GET .../user/assert-enabled`（§7.2.1）、按 **roleCode** 列用户（§7.2.3 `user-ids-by-role-code`，或 OPS roleId 映射 + 现有 RPC）。  
  C-WP2 第二切片：**`hasAnyRoles` 可立即接 Feign**；`assert-enabled` / roleCode 列用户仍阻塞删 system `@DS` 部分路径。

- **现状**  
  - 已有：`GET /rpc-api/system/user/getUserListByRoleId?roleId=`（2026-07-28）。  
  - 已有：**`PermissionCommonApi.hasAnyRoles`**（`userId` + role codes → Boolean）。  
  - **缺失**：`GET /rpc-api/system/user/assert-enabled?id=`（MUST-HAVE §7.2.1）。  
  - **缺失**：`GET /rpc-api/system/permission/user-ids-by-role-code?roleCode=`（MUST-HAVE §7.2.3 建议）。  
  - OPS 临时方案：维护 `roleCode → roleId` 映射，再调 `getUserListByRoleId`；`hasAnyRoles` 直接 Feign。

- **选项 A：Football 新增 assert-enabled +「按 roleCode 列用户」RPC**  
  - **优点**：OPS 无映射维护；与 MUST-HAVE §7.2 一致；可彻底删 `@DS` 角色/校验读。  
  - **缺点**：Football 1–3 人日；依赖 D-ADR-050 Supersede（assert-enabled 为新增）。

- **选项 B：OPS 侧 roleCode→roleId 映射 + 现有 `getUserListByRoleId`；`hasAnyRoles` 先 Feign**  
  - **优点**：**hasAnyRoles 零 Football 新增**；roleCode 列用户可较快切 Feign。  
  - **缺点**：映射需与 Football 角色 seed 同步；**assert-enabled 仍缺**直至 Football 补或继续 @DS 单点校验。

- **选项 C：过渡期保留 `@DS` union，simple-list / hasAnyRoles 走 Feign**  
  - **优点**：G-SYS-01 双跑 + hasAnyRoles Feign 已可落地。  
  - **缺点**：无法删 system DS；assert-enabled / roleCode 列用户需明确 sunset。

- **推荐**  
  **立即**：OPS 接 **`hasAnyRoles` Feign**（Football 已有）。  
  中长期 **选项 A**（assert-enabled + roleCode 列用户）；若 Football 排期紧，**B 过渡 roleCode 列用户 + 书面 sunset**；assert-enabled 未补前写入校验可暂留 @DS 单点。

- **若选 X，后续动作**  
  - **A**：Football 实现 §7.2.1 + §7.2.3 → OPS 改 Validator → 删 LookupMapper 校验/角色路径。  
  - **B**：OPS 新增 `RoleCodeResolver` + **hasAnyRoles Feign** → 接 `getUserListByRoleId`；assert-enabled 单列 backlog。  
  - **C**：WORK-PLAN C-WP2 标注「G-SYS-02 部分完成」；阻塞 C-WP7 删 multidb system 源。

- **决策栏**：`[ ] 选 A  [x] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：产品/架构 · 2026-07-28（**选项 B**）

> **已拍板（2026-07-28）**：**选项 B** — OPS 侧 `roleCode→roleId` 映射 + 现有 `getUserListByRoleId`；`hasAnyRoles` Feign；`assert-enabled` 用 **`getUser` + `validateUserList` 组合**（无专用 RPC）。

---

## D-G-SYS-01　IP 组数据权限开关（simple-list 是否忽略数据权限）

- **背景**
  G-SYS-01 要求后端 `GET /rpc-api/system/user/simple-list`（IP 组候选用户、写入校验等）。MUST-HAVE §7.1 标注**开放问题**：IP 组场景是否需「**忽略数据权限、仅按租户返回全量启用用户**」。  
  Football Admin 的 simple-list 默认带数据权限；OPS IP 组配置页可能需要「租户内全部可指派用户」，而非当前登录人可见子集。

- **现状**  
  - G-SYS-01 **已交付**（`AdminUserApi.getSimpleUserList`）；OPS C-WP2 首切片 Feign 双跑已落地。  
  - 用户反馈（2026-07-28）：`deptName` **非阻塞**——DTO 可先不含 deptName，IP 组以 id/nickname 为主。  
  - **未决**：数据权限开关；若 Football 不扩参，OPS IP 组候选可能少于预期。

- **选项 A：Football 增加 `ignoreDataPermission=true`（或等价）查询参数**  
  - **优点**：语义清晰；OPS IP 组/管理员指派场景一次 RPC 拿全量启用用户。  
  - **缺点**：需 Football 评审安全边界（仅 RPC、租户隔离仍生效）。

- **选项 B：沿用 Admin 默认数据权限行为**  
  - **优点**：零 Football 改动；与 Football Admin UserSelect 一致。  
  - **缺点**：IP 组候选可能缺人；需产品确认是否可接受。

- **选项 C：OPS 继续 `@DS` 或 Admin 代理读「全量」直至有 RPC 参数**  
  - **优点**：行为与现网 multidb 一致。  
  - **缺点**：无法完成 G-SYS-01 切轨闭环；与单库目标冲突。

- **推荐**  
  请产品/架构确认 IP 组「候选用户」业务定义：若必须全量启用用户 → **A**；若与 Football 数据权限一致即可 → **B**。

- **若选 X，后续动作**  
  - **A**：Football 扩 Api + OPS Feign 传参 → IP 组 IT 回归。  
  - **B**：关闭 MUST-HAVE §7.1 开放问题；OPS 文档注明与 Admin 同权限。  
  - **C**：G-SYS-01 标记「前端/双跑 only」，阻塞删 `@DS`。

- **决策栏**：`[ ] 选 A  [x] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：产品/架构 · 2026-07-28（**选项 B**）

> **已拍板（2026-07-28）**：**选项 B** — 沿用 Admin 默认数据权限行为；simple-list **不**忽略数据权限；关闭 MUST-HAVE §7.1 开放问题。

---

## D-G-PAY-01　订单列表（OPS 何时切 `getOrderPage` Feign）

- **背景**  
  OPS 订单只读归因链路当前 `@DS("pay")` 读 `pay_all_order`（ADR-050 D7）。目标态走 `PayOrderApi` Feign。  
  **2026-07-28 实码审计**：Football `ops` 分支 **`PayOrderApi.getOrderPage` 已有**；MUST-HAVE §7.8 提案 path 可能不同，优先**对表复用现有方法**。  
  **B-G-PAY-01** 类型已改为 **OPS 集成**（非 Football API 缺口）。

- **现状**  
  - Football：`PayOrderApi.getOrderPage` **已存在**（ops 分支实码）。  
  - OPS：`FootballPayAllOrderReadMapper` 等仍 @DS。  
  - 待办：OPS 列 vs 响应字段 **对表**；Feign 双跑 + Integration 验收。

- **选项 A：对表通过后立即切 Feign（复用 `getOrderPage`）**  
  - **优点**：Football 零新增；删 pay `@DS`。  
  - **缺点**：需 OPS 适配分页入参/筛选差异。

- **选项 B：对表后延至 C-WP5 统一 member/mp/pay 切轨**  
  - **优点**：一次 cutover、减少双轨窗口。  
  - **缺点**：pay `@DS` 保留更久。

- **选项 C：字段未 100% 覆盖时 OPS Adapter 补列或 Football DTO 扩展**  
  - **优点**：列表列稳定。  
  - **缺点**：可能小改 Football 出参（非新 path）。

- **选项 D：维持 `@DS("pay")` 只读过渡**  
  - **优点**：零 OPS Feign 工作短期。  
  - **缺点**：阻塞 C-WP7；与单库目标冲突。

- **推荐**  
  **半天字段对表** → 覆盖 ≥100% 则 **A 或 B**；缺列则 **C** 小扩 DTO。**D** 仅短期过渡。

- **若选 X，后续动作**  
  - **A/B/C**：产出对表签字 → OPS `FootballOrderReadController` Feign + 双跑 → 删 pay DS。  
  - **D**：WORK-PLAN 保留 B-G-PAY-01 至 cutover 日期。

- **决策栏**：`[x] 选 A  [ ] 选 B  [ ] 选 C  [ ] 选 D  [ ] 其他：_______`

- **决策人 / 日期**：产品/架构 · 2026-07-28（**选项 A**）

> **已拍板（2026-07-28）**：**选项 A** — 字段对表通过后立即切 Feign（复用 `getOrderPage`）；**下一切片** C-WP5 执行。

### D-G-PAY-01 REV1（2026-07-30 · **假设 B** · Supersede 选项 A）

| 字段 | 值 |
|------|---|
| 触发 | Integration 手验：Admin `getOrderPage` 绑定 permitted-ids + `finance_channel*` 富化，local schema 漂移 → 裸 RPC 500；见 [G-PAY-01-FIX](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/G-PAY-01-FIX.md) |
| 拍板 | 用户明确 **按假设 B** |
| 决策 | **废止**「复用 `getOrderPage`」；改走 [MUST-HAVE §7.8](./OPS-FOOTBALL-RPC-MUST-HAVE.md) `POST /rpc-api/pay/order/page-for-ops` |
| ADR | [ADR-057](../adr/ADR-057-G-PAY-01-page-for-ops.md) **Accepted** |
| 入参 | `startTime`/`endTime`（半开 `[start,end)`）+ 可选 `authorId`/`status` + 分页；Header `tenant-id` |
| 出参 | OPS 10 列（§8.8 对表仍有效）；**无** Admin 作者权限 / 渠道富化 |
| OPS | `FootballOrderReadService` → `PayOrderApi.pageForOps` |
| 手验 | **Pass**（2026-07-30）— [G-STAR-HANDVERIFY](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md)；~~B-G-PAY-01~~ 已解除 |

---

## D-G-MEM-02　作者只读（前端 Admin vs RPC simple-list）

- **背景**  
  D-AUTHOR-01 已拍板：OPS **只读不写**作者主数据；CUD 归 Football Admin。G-MEM-02 提案 member 侧 `get/list/simple-list` Feign。  
  用户反馈（2026-07-28）：前端可走 Football **`AuthorUserController`/Admin VO**；服务端 RPC `simple-list` **可选**——Phase A 用 Admin 已够用；**删 member `@DS`** 仍须决策服务端读路径。

- **现状**  
  - G-MEM-01 **`authorLevel`** 已在 DTO 交付（2026-07-28）。  
  - 浏览器：可直调 Admin 作者 API。  
  - **oa-server 后端**（内容同步、校验、归因）：仍 `@DS("member")` 读作者；无强制 Feign 则无法删 DS。

- **选项 A：Football 交付 G-MEM-02 RPC（AuthorApi simple-list 等）**  
  - **优点**：后端与前端路径一致；符合 MUST-HAVE「后端必须 Feign」原则。  
  - **缺点**：Football 1–2 人日。

- **选项 B：OPS 后端通过 HTTP 调 Football Admin（非 Feign RPC）**  
  - **优点**：复用已有 Admin；可能零 Football RPC。  
  - **缺点**：服务间走 Admin 非标准；鉴权/租户头复杂；与 MUST-HAVE 惯例 `/rpc-api` 不一致。

- **选项 C：前端 Admin 只读 + 后端暂保留 `@DS("member")` 至 G-MEM-03 一并切**  
  - **优点**：前端可先完成；authorLevel 已可用。  
  - **缺点**：member DS 删除推迟；与「后端 Feign」终态不符。

- **推荐**  
  Phase A 前端 **Admin 只读** 已满足；若目标删 `@DS`，**选项 A** 仍 must-have；**C** 仅作短期过渡并需截止日期。

- **若选 X，后续动作**  
  - **A**：Football AuthorApi 扩展 → OPS `MemberAuthorReadService` Feign → C-WP5 删 member 读 Mapper。  
  - **B**：OPS 新建 Admin 内部客户端 + ADR。  
  - **C**：C-WP5 标注 member 读 defer。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [x] 其他：前端 Admin + AuthorApi getAuthor/getAuthors 够用；暂不需要 Football RPC simple-list`

- **决策人 / 日期**：产品/架构 · 2026-07-28

> **已拍板（2026-07-28）**：`AuthorSimpleRespDTO.authorLevel` 已交付；**前端 Admin + AuthorApi getAuthor/getAuthors 够用**；member 读 `@DS` 删除推迟至 C-WP5（与 G-MEM-03 一并评估）。

---

## D-G-DING　钉钉（OPS 本地 vs 等 G-DING-01；ding_user_id 桥接）

- **背景**  
  D-DING-02 已拍板：**不做**钉钉通讯录同步 Feign。G-DING-01 为**通用工作通知**（任务/审核等），MUST-HAVE §7.9 提案新建 `DingTalkMessageApi`。  
  OPS 现用本地 `DingTalkWorkNotifyClient` / `DingTalkRobotClient`；推送需 `ding_user_id`。Football 钉钉迭代 PRD 计划在 `system_users` 增 `ding_user_id`，Ops 当前读 legacy `sys_user.ding_user_id`。  
  用户反馈：**OPS 本地钉钉不阻塞整包**；**`ding_user_id` 桥接待做**。

- **现状**  
  - 通讯录 sync：已 `@Deprecated`，应 410（C-WP0）。  
  - 业务推送：仍 OPS 本地 Client；G-DING-01 **未交付**（8–15 人日，可后置）。  
  - `ding_user_id`：Football system 侧列未就绪时，OPS 无法统一 Feign 推送。

- **选项 A：等 G-DING-01 + Football `system_users.ding_user_id`，OPS 删本地 Client**  
  - **优点**：SSOT 在 Football；与 PRD-Football-钉钉同步与消息推送 一致。  
  - **缺点**：周期长；推送功能依赖 Football 排期。

- **选项 B：过渡期保留 OPS 本地 Client + 读 wd/`sys_user` 或 `@DS system` 取 ding_user_id**  
  - **优点**：不阻塞 Phase A/B/C 主路径；用户已确认不阻塞整包。  
  - **缺点**：双轨；Football id 与 ding_user_id 桥接需 OPS 维护。

- **选项 C：OPS 只读 Football Admin/RPC 查 ding_user_id，推送仍本地 Client**  
  - **优点**：身份 SSOT 向 Football 靠拢；推送 API 仍可后置。  
  - **缺点**：需只读契约（列或 getUser 扩展）；半切状态。

- **推荐**  
  **B 或 C 过渡 + G-DING-01 后置**；通讯录 sync 永不恢复（D-DING-02）。整包 cutover 可将 G-DING-01 列为可选延期项（WORK-PLAN C-WP6）。

- **若选 X，后续动作**  
  - **A**：Football G-DING-01 + ding_user_id 列 → OPS `NotificationServiceImpl` 改 Feign → 删 Client。  
  - **B**：文档化桥接规则（ADR-056 用户 id + ding_user_id 映射）→ C-WP6 排期。  
  - **C**：Football getUser 带 dingUserId → OPS 只改解析，推送仍本地。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：_______

> **已拍板（2026-07-28）**：**延后** — G-DING-01 / D-G-DING 不阻塞 Phase A/B/C 主路径；C-WP6 可选排期。

---

## D-GW-DEV　Integration 环境（Feign 联调前置）

- **背景**  
  OPS Feign 切轨需发现 `system-server` 等微服务。本地 **dev-nacos-local** 可直连 `:48081`；无 Nacos / 未起 system-server 时 OPS 设计为 **回退 @DS**（双跑）。  
  WORK-PLAN §8.6 **B-GW-DEV**：Integration 起 system-server 或 Nacos 发现未稳定时，Feign 路径仅 dev 手验，IT 仍走 H2 @DS。

- **现状**  
  - G-SYS-01 双跑已在 dev-nacos-local + 直连 URL 验证。  
  - Gate 路径：`start-integration-all.ps1` / `start-ops-dev.ps1` → `:5777` + Gateway `:48080`。  
  - **B-FEIGN-IT**（见 D-FEIGN-IT）：H2 IT  intentionally 无 system-server。

- **选项 A：Feign 验收以 Integration 环境为准（Nacos + 全栈 Football 微服务）**  
  - **优点**：与生产拓扑一致；Gate 可覆盖 Feign 路径。  
  - **缺点**：本地资源重；需维护 `scripts/integration-config/` 与 `push-integration-config-to-nacos.ps1`。

- **选项 B：dev-nacos-local 直连 URL（无 Nacos）+ 文档化 hand-run 清单**  
  - **优点**：轻量；当前 G-SYS-01 双跑已用。  
  - **缺点**：与 Integration 发现机制不一致；易漂移。

- **选项 C：维持双跑至 cutover；Feign 仅 staging/Beta 手验**  
  - **优点**：不阻塞 OPS 单片开发。  
  - **缺点**：Feign 回归 gaps；删 @DS 前需补 Integration 证据。

- **推荐**  
  **A** 为 Feign cutover 正式验收标准；开发单片可用 **B**；删 multidb 前必须有一次 **A** 全绿记录。

- **若选 X，后续动作**  
  - **A**：更新 OPS-DEV-DEPLOY-GUIDE 启动矩阵；每 G-* cutover 附 Integration 手验记录。  
  - **B**：`application-dev-nacos-local.yml` 固定 Feign URL 表。  
  - **C**：WORK-PLAN 保留 B-GW-DEV 直至 staging 验过。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：_______

---

## D-PHASE-C　执行节奏（整包 vs 切片；是否先 B-WP1 停写）

- **背景**  
  WORK-PLAN 已定 **Phase A 前端 → B 数据库 → C 后端**。§8.1：Phase C **整包 NO-GO**，**单片 GO**（C-WP0、G-SYS-01 双跑已落地）。  
  B-WP1「停写规范」可与 A 并行，防止继续写 `wd.system_users`、`wd.oa_author` 等非 SSOT 表（ADR-056 / D-AUTHOR-01）。

- **现状（2026-07-30 修订）**  
  - Phase A：大部分完成；E2E/mount 退役待办。  
  - Phase B：**B-WP1 停写规范 ✅**（2026-07-28）；B-WP3 Flyway 政策待办。  
  - Phase C：C-WP0/1/2/3/4/5 cutover ✅；G-* 业务手验 Pass 7/0（Skip G-DING）；**C-WP7-PHYS 代码 ✅**；**整包仍 NO-GO**（仅余 B-WP4-ARCHIVE，见 WORK-PLAN §8.1 / §8.6）。  
  - 下一步：B-WP4 产品签收 → 再评估整包 GO。

- **选项 A：严格单片推进（当前 §8.5）+ 立即启动 B-WP1 停写**  
  - **优点**：风险可控；不抢跑删 @DS；停写减污染。  
  - **缺点**：整包完成时间拉长；需频繁 WORK-PLAN 更新。

- **选项 B：等 ADR-050 + 主要 G-* 就绪后 Phase C 整包切换**  
  - **优点**：一次 cutover，减少双跑窗口。  
  - **缺点**：等待 Football 16–38 人日；期间 @DS 与 Feign 双轨时间长。

- **选项 C：优先 B-WP1 + Phase A 收尾，Phase C 仅 C-WP0/C-WP2 直至 Football 批量交付**  
  - **优点**：Football 压力后置；OPS 先清前端与写规范。  
  - **缺点**：后端 multidb 长期存在。

- **推荐**  
  **选项 A/C**（实质相同）：**B-WP1 应立即启动**；Phase C 坚持「Feign 验收 → 双跑 → 删 @DS」，禁止整包抢跑。

- **若选 X，后续动作**  
  - **A/C**：本周 B-WP1 checklist + PR review 门禁；Football G-* 排期会。  
  - **B**：冻结 C-WP2 以后直至 D-ADR-050 + OPS C-WP5 Feign 切轨完成。

- **决策栏**：`[x] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：产品/架构 · 2026-07-28（**选项 A**）

> **已拍板（2026-07-28）**：**选项 A** — 严格单片推进 + **B-WP1 已启动**；停写规范 → [OPS-FOOTBALL-STOP-WRITE-POLICY.md](./OPS-FOOTBALL-STOP-WRITE-POLICY.md)。

---

## D-FOOTBALL-PUSH　football-backend-saas `ops` 推远端

- **背景**  
  Football 与 OPS 联调约定：**Gitee `ops` 分支**为合入轨（详见 `docs/delivery/FOOTBALL-OPS-BRANCH.md`）。本地 `football-backend-saas` / `football-front` 已在 `ops` 上开发并交付部分 G-* API；若未 push `origin/ops`，其他环境/队友无法拉取 Feign 契约与 Integration 验证。

- **现状**  
  - 本地 `ops` 分支含：`getSimpleUserList`、`getUserListByRoleId`、`AuthorSimpleRespDTO.authorLevel` 等。  
  - 远程 `origin/ops` 是否同步：**待确认**（阻塞 Beta / 他人联调）。  
  - ADR-050 §3.1 历史上禁止 Football 业务改动；push 前宜与 **D-ADR-050** 对齐，避免 master 误 merge。

- **选项 A：立即 push `football-backend-saas`（及 front）到 `origin/ops`，仅 ops 分支**  
  - **优点**：Integration/Beta 可拉同一契约；OPS Feign 双跑可复现。  
  - **缺点**：需 Football 仓库写权限；须 code review 不污染 master。

- **选项 B：打包 patch / MR 由 Football 主仓 review 后合入 ops**  
  - **优点**：流程正式；符合跨团队治理。  
  - **缺点**：慢；阻塞 OPS 联调直至合并。

- **选项 C：暂存本地 ops，OPS 继续 vendored Api + @DS 回退**  
  - **优点**：无 remote 依赖。  
  - **缺点**：契约漂移；Integration 无法验 Feign；与 G-* 交付目标冲突。

- **推荐**  
  在 **D-ADR-050** 方向明确后 **A 或 B**；至少保证 `origin/ops` 与本地 Feign 接口一致。禁止 push 到 `master`。

- **若选 X，后续动作**  
  - **A**：`git push origin ops`（backend + front）→ 更新 WORK-PLAN §7.3 快照 → Beta 拉取验证。  
  - **B**：提 Gitee PR → 指定 reviewer → 合并后 OPS 改依赖版本。  
  - **C**：文档注明仅本地 dev-nacos-local 可 Feign。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：_______

---

## D-G-MEM-03　文章写 Feign（§8.6 · Football 已有 → OPS 何时切）

- **背景**  
  内容生产同步需 create/update/上下架 Football 文章（MUST-HAVE §7.6）。当前 OPS `@DS("member")` 写 `AuthorArticleMapper`。D-AUTHOR-01 仅禁作者 **CUD**，不禁文章写。  
  **2026-07-28 实码审计**：Football `ops` 分支 **`ArticleApi` create/update/status-change 已有**；**B-G-MEM-03** 为 **OPS 集成**阻塞。

- **现状**  
  - Football：`ArticleApi` 写接口 **已交付**（ops 分支实码）。  
  - OPS：仍 `@DS` 写；未 vendored Feign / 无双跑。  
  - 解除：**OPS C-WP5** 接 Feign + 内容同步 IT。

- **选项 A：C-WP5 排期立即切 `ArticleApi` Feign**  
  - **优点**：删 member 写 `@DS`；与目标态一致。  
  - **缺点**：需双跑 + 字段对齐（`schedulePublishStatus` 等默认值）。

- **选项 B：维持 @DS 写直至 C-WP5 与 mp/pay 一并 cutover**  
  - **优点**：一次切换、减少回归次数。  
  - **缺点**：member DS 保留更久。

- **选项 C：内容同步降级为 Football Admin 人工操作**  
  - **优点**：OPS 可不写 member。  
  - **缺点**：产品流程变更；通常不可接受。

- **推荐**  
  **A 或 B**（Football 侧无需新 API）；**C** 除非产品改 scope。

- **若选 X，后续动作**  
  - **A/B**：OPS `MemberArticleWriteService` / 桥接改 Feign → 双跑 → 删 `AuthorArticleMapper` 写路径。  
  - **C**：更新 scope，关闭 OPS 写路径需求。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：_______

---

## D-G-MP-01　公众号 page/写 Feign（§8.6 · Football 已有 → OPS 何时切）

- **背景**  
  M4 资产链微信账号 SSOT 在 `mp_account`（ADR-050 D2）；OPS 扩展 `oa_account_ext`。编排需 page/create/update Feign（MUST-HAVE §7.7）。  
  **2026-07-28 实码审计**：Football `ops` 分支 **`MpAccountInfoApi` page/create/update/get 已有**；**B-G-MP-01** 为 **OPS 集成**阻塞。

- **现状**  
  - Football：Mp 账号 RPC **已交付**（ops 分支实码）。  
  - OPS：仍 mp `@DS`；`MpAccountDataService` 未 Feign。  
  - 解除：**OPS C-WP5** 接 Feign；`oa_account_ext` 仍落 wd。

- **选项 A：C-WP5 排期切 `MpAccountInfoApi` Feign**  
  - **优点**：删 mp `@DS`；终态一致。  
  - **缺点**：OPS 适配 + 双跑。

- **选项 B：过渡期保留 mp @DS，与 member/pay 一并 cutover**  
  - **优点**：M4 功能稳定；单次切换。  
  - **缺点**：阻塞 C-WP7 更久。

- **选项 C：微信账号运维仅在 Football Admin，OPS 只读 ext 关联**  
  - **优点**：OPS 可不写 mp。  
  - **缺点**：若 OPS 仍有创建/更新公众号编排则不够。

- **推荐**  
  按 OPS 是否仍需「OPS 内创建公众号」定：**A** 若保留编排；**C** 若账号创建全部 Football Admin（需产品确认）。

- **若选 X，后续动作**  
  - **A**：OPS `MpAccountDataService` Feign + 双跑 → 删 mp Mapper。  
  - **B/C**：更新 scope 与 WORK-PLAN C-WP5。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：_______

---

## D-FEIGN-IT　H2 IT 与 Feign 路径验证策略（§8.6 B-FEIGN-IT）

- **背景**  
  OPS H2 集成测试无真实 `system-server`；Feign 失败时 **设计回退 @DS**（G-SYS-01 双跑已体现）。WORK-PLAN **B-FEIGN-IT**：Feign 路径需 Integration 手验，不能单靠 `mvn verify` 断言 Feign 真调通。

- **现状**  
  - IT 绿 ≠ Feign 绿。  
  - 删 @DS 若仅依赖 IT，可能漏 Feign 配置/契约错误。

- **选项 A：IT 保持 @DS 回退；Feign 仅 Integration/E2E 手验清单**  
  - **优点**：CI 快、稳定；与现设计一致。  
  - **缺点**：需人工/regression 记录 Feign cutover。

- **选项 B：新增 Testcontainers / WireMock 模拟 Football RPC**  
  - **优点**：CI 可断言 Feign 契约。  
  - **缺点**：维护成本高；与真实 Football 漂移风险。

- **选项 C：cutover 后 IT 强制 Feign（移除 @DS 回退）**  
  - **优点**：终态测试真实。  
  - **缺点**：CI 需起 Football 或 mock；实施晚。

- **推荐**  
  当前 **A**；每个 G-* cutover 在 `docs/delivery/e2e-artifacts/` 或 Gate 报告附 Integration 证据；终态趋 **C**。

- **若选 X，后续动作**  
  - **A**：MUST-HAVE/WORK-PLAN 注明 IT 边界；G-* cutover checklist 加 Integration 项。  
  - **B/C**：单独立项评估（非本期 must-have）。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：_______

---

## D-G-DICT-01　字典后端 Feign 与 sort 字段（补充，用户反馈非阻塞）

- **背景**  
  前端 DictSelect 已切 Admin（A-WP3 ✅）。后端 `@InDict` / `SystemDictAdapter` 仍 @DS，须 G-DICT-01。MUST-HAVE §7.3 建议 DTO 增 **`sort`**。  
  用户反馈（2026-07-28）：**`sort` 非阻塞**——排序差异不挡 Phase A；后端 Adapter→Feign 可后移。

- **现状**  
  - `DictDataApi` 可读；`sort` 可能缺失。  
  - 不阻塞 Phase A；**阻塞** C-WP3 删 system dict Mapper（若校验依赖 sort 则再议）。

- **选项 A：Football 补 `sort` 后 OPS 切 Feign**  
  - **优点**：与 Admin 排序一致。  
  - **缺点**：小改动仍要 Football。

- **选项 B：OPS Adapter 无 sort 先切 Feign，排序用 label/value 默认序**  
  - **优点**：快；用户已接受非阻塞。  
  - **缺点**：个别字典顺序可能与 Admin 略异。

- **选项 C：C-WP3 推迟至 sort 就绪**  
  - **优点**：零妥协。  
  - **缺点**：system @DS 保留更久。

- **推荐**  
  **B** 与 user feedback 一致；若某字典强依赖 sort，再局部 **A**。

- **若选 X，后续动作**  
  - **A/B**：C-WP3 排期；G-DICT-01 从 WORK-PLAN §7 快照 ⚠️ 改 ✅/进行中。  
  - **C**：保留 @DS dict 读。

- **决策栏**：`[ ] 选 A  [ ] 选 B  [ ] 选 C  [ ] 其他：_______`

- **决策人 / 日期**：_______

---

## 决策汇总表

| ID | 主题 | 你的选择 | 解除的阻塞 |
|----|------|----------|------------|
| D-ADR-050 | Supersede §3.1 | **C**：有限 Supersede（G-* 白名单） | **B-ADR-050 已解除**（2026-07-28）；Football 扩 API 依据 ✅ |
| D-G-SYS-02 | roleCode 列用户 + assert-enabled | **B**：`hasAnyRoles` Feign + roleCode→roleId + `getUserListByRoleId`；assert-enabled = `getUser`+`validateUserList` | B-G-SYS-02a/b **部分解除**（C-WP2 G-SYS-02 ✅；删 system @DS 仍待 cutover） |
| D-G-SYS-01 | IP 组数据权限开关 | **B**：沿用 Admin 默认数据权限 | G-SYS-01 切轨闭环 ✅；§7.1 开放问题关闭 |
| D-G-PAY-01 | OPS 切订单 Feign | **A→REV1 假设 B**：`page-for-ops`（ADR-057 Accepted） | **B-G-PAY-01 已解除**；手验 Pass 2026-07-30 |
| D-G-MEM-02 | 作者只读路径 | **前端 Admin + AuthorApi 够用**；暂不需要 RPC simple-list | MemberAuthorRead Feign-only ✅（B-DS-RESIDUE 核实） |
| D-G-DING | 钉钉推送与 ding_user_id | **延后** | C-WP6 可选；不阻塞整包 |
| D-GW-DEV | Integration Feign 环境 | | B-GW-DEV（手验已在直连栈完成） |
| D-PHASE-C | 执行节奏 / B-WP1 | **A**：立即 B-WP1 ✅ | 停写污染；整包抢跑风险 |
| D-FOOTBALL-PUSH | ops 推远端 | | 联调/Beta 契约一致 |
| D-G-MEM-03 | OPS 切 ArticleApi Feign | **已 cutover + 手验 Pass** | **B-G-MEM-03 已解除**（2026-07-30） |
| D-G-MP-01 | OPS 切 MpAccount Feign | **已 cutover + 手验 Pass** | **B-G-MP-01 已解除**（2026-07-30） |
| D-FEIGN-IT | IT vs Feign 验证 | **A**：IT 回退；手验清单 | FEIGN-CHECKLIST §3 ✅ |
| D-G-DICT-01 | 字典 sort / 后端 Feign | 读 Feign-only；admin/types 410 | C-WP3 + B-DS-RESIDUE ✅ |

---

## 拍板后 OPS 可立即执行的下一步

1. ~~**若 D-ADR-050 选 A/C**~~ **D-ADR-050 已选 C**（2026-07-28）：[ADR-050-REV1](../adr/ADR-050-REV1-Football-G-RPC-Supersede.md) 已 Accepted。  
2. ~~**若 D-PHASE-C 选 A/C**~~ **D-PHASE-C 已选 A**（2026-07-28）：**B-WP1** → [OPS-FOOTBALL-STOP-WRITE-POLICY.md](./OPS-FOOTBALL-STOP-WRITE-POLICY.md)。  
3. ~~继续 C-WP2 / C-WP5 Feign 切轨~~ **已完成**（2026-07-29/30 cutover + [G-STAR-HANDVERIFY](./e2e-artifacts/G-STAR-HANDVERIFY-20260730/REPORT.md)）。  
4. **Phase A 收尾**：A-WP5 standalone/E2E 标非 Gate；菜单平行权限抽检。  
5. **清整包阻塞**：~~B-DS-RESIDUE~~ + ~~B-C-WP7-PHYS~~ **已解除** → 剩余 **B-WP4-ARCHIVE**（表归档产品签收）→ 再评估 Phase C 整包 GO。  
6. **若 D-FOOTBALL-PUSH 选 A/B**：同步 `origin/ops`（含 ADR-057 `page-for-ops`）。

---

**维护**：拍板后请在本文件「决策栏」填写结果，并回写 `docs/delivery/OPS-FOOTBALL-MERGE-WORK-PLAN.md` §8.6–§8.7 阻塞表状态（勾选/关闭对应 B-*）。

---

## D-ADR-058　OPS 单仓 + ops 命名（产品 mandate · 2026-07-30）

- **已拍板**：见正式 ADR [ADR-058](../adr/ADR-058-OPS后端单仓与football-module-ops命名.md)。
- **要点**：废除 ADR-047 §4.1 sibling；monorepo `football-module-ops`；DB 仅 `wd`；终态 `ops-server` + `/admin-api/ops/**`；权限 `oa:*` 过渡保留（独立 Slice 再迁 `ops:*`）。
- **对 Phase C**：正交；不改写 C 整包门禁；命名/搬迁另开 Phase D。

**版本** v1.3 · **日期** 2026-07-30 · **状态** 部分已拍板（G-PAY REV1 / 手验已回写 · ADR-058 Accepted）
