# ADR-077：SOP 内容生成节点文档类型 + 登记确认自动草稿 + jingcai

| 字段 | 值 |
|------|---|
| 编号 | ADR-077 |
| 标题 | SOP `CONTENT_GENERATION` 配置 `documentType`；工作任务 confirm 后自动建 DRAFT 并异步 jingcai |
| 状态 | **Accepted** |
| 日期 | 2026-09-02 |
| 批准日期 | **2026-09-02**（产品 Owner 书面确认「按这些默认写 ADR-077」） |
| 决策人 | 产品 Owner |
| 关联 | [ADR-016](./ADR-016-M2-节点类型与任务内容关联.md) · [ADR-054](./ADR-054-OPS内容生产Football方案主表合并.md) · [ADR-056](./ADR-056-Football用户身份SSOT.md) · [ADR-074](./ADR-074-工作任务登记SOP全节点生成与营销计划绑定.md) · [ADR-075](./ADR-075-工作任务合并执行组.md) · [ADR-076](./ADR-076-OPS内容生产amphipoda玩法与matchScheme对齐.md) · [PRD-M2-工作任务管理](../product/PRD-M2-工作任务管理.md) · [PRD-M2-内容生产](../product/PRD-M2-内容生产.md) |

---

## 1. 背景

工作任务登记 confirm（ADR-074 / ADR-075）已按 SOP **全节点**生成 `oa_task`，但：

1. SOP **内容生成**节点未配置 `dict_document_type`，生成的 task / 内容无法继承文档类型。
2. ADR-016 §2.3 规定内容为 **懒创建**（执行页首次进入才 `POST /content/create`）。
3. 产品要求：confirm **成功后**对全部 `CONTENT_GENERATION` 任务 **自动建 DRAFT** 并 **必须调用 jingcai**；玩法可选；任务可并行；**不**把任务标完成。
4. 付费墙 **禁止**新布尔列；正式方案写入 `paid_body`，其余写入 `free_body`。此自动首写与 ADR-054 **D8**（禁止仅凭 `documentType` 映射列）冲突，须 **单独 ADR** 开例外，**不得**塞进 ADR-074 / ADR-076。

---

## 2. 决策（Accepted）

| # | 决策 |
|---|------|
| **D1** | SOP 管理：`node_type=CONTENT_GENERATION` 的节点 **必填** `documentType`（`@InDict dict_document_type`，与内容记录同一字典）。`CONTENT_PUBLISH` / `NORMAL` **不**配置、不校验该字段。 |
| **D2** | confirm 生成 task 时，把节点 `documentType` **拷贝**到 `oa_task.document_type`（反规范化，便于列表/Job）。 |
| **D3** | **例外 ADR-016 §2.3**：工作任务 sheet **confirm 成功路径** 可为每个 `CONTENT_GENERATION` task **自动创建** 1 条关联内容（仍 **1 task : 0..1 内容**，`task_id` 唯一，1502）。计划管理启动（FR-M2-009）**不**在本 ADR 自动建内容。 |
| **D4** | 自动草稿 **映射** task 已有字段：`task_id`、`document_type`、`author_id`、`ip_group_id`、`competition_ids_json`（及首场摘要）、`title = plan_name`。`content_type=ARTICLE`。`status=DRAFT`。`creatorUserId` = task `assignee_id`（已有必填，**ADR-056** 写入校验）。**允许**付费/免费正文为空（或实现层占位）；AI 稍后回填。 |
| **D5** | `oa_task.plan_name`（及自动草稿标题）= **工作日 + 作者 + 赛事摘要 + 节点名**（保证同登记行多节点标题可区分）。 |
| **D6** | 自动草稿 **不**把任务标 `DONE` / `COMPLETED`；**不等** DAG 前驱完成。同一 sheet 下多条 `CONTENT_GENERATION` **可并行**入队 jingcai。完成门禁见 **[ADR-079](./ADR-079-任务完成工作说明与内容审核通过门禁.md)**（内容 **审核通过** 后方可完成；本条 **不**改 jingcai）。 |
| **D7** | **触发**：sheet confirm **事务提交成功之后**（`afterCommit`）异步 Job 调 jingcai。confirm API **立即**返回成功（仍为 ADR-074 的 `generatedTaskCount` 等，**不**新增响应字段）。**禁止**在 confirm 事务内同步调用 jingcai。 |
| **D8** | **全部 5 个** `dict_document_type` 值均自动调 jingcai（无白名单例外）。 |
| **D9** | jingcai **请求上下文**仅用现网 generate 已有字段：`authorId`、`matchScheme`、`matchName`、`documentType`、`isPaywall`（E2E 已传）。**不**新增 jingcai / generate payload 字段。`isPaywall` **只存在于请求上下文，不落库新列**。 |
| **D10** | `matchScheme` **必填**（≥1 场，来自 task `competition_ids_json`）。`matchPlays` **可空**（玩法可选）。`matchType` 缺省沿用 ADR-076：**1（竞足）**。此条 **更新** ADR-076「AI 前须有玩法」：自动 Job **以及**已改过的手工 AI 均可「仅有比赛、无玩法」调用 jingcai。 |
| **D11** | **例外 ADR-054 D8（仅本自动首写）**：`documentType=OFFICIAL_PLAN` → `isPaywall=true` → 成功结果写入 **`paid_body`**；其余四类 → `isPaywall=false` → 写入 **`free_body`**。**之后**用户编辑、手工 AI 采纳 **仍遵守 ADR-054 D8**（用户选列，禁止再按文档类型自动映射）。 |
| **D12** | jingcai **失败**：confirm **仍成功**；内容保持 `DRAFT`；task / 内容展示失败原因；**允许重试**（复用现有 `POST /ops/ai-content/generate` 或同一 Job 再入队，**不**新造 payload）。 |
| **D13** | **withdraw**（扩展 ADR-074 D9 / ADR-071 D7）：取消未完成的自动生成 Job；**删除**已自动生成且仍为 `DRAFT` 的关联内容（产品接受该默认）。既有「关联 task 已 `IN_PROGRESS`/`COMPLETED` → 1502 禁止撤回」**不变**。 |
| **D14** | **不**新建付费墙布尔列；**不**新建大型 Job 子系统。异步状态落在内容表字段（见 §4）。执行人 / 作者身份 **ADR-056**。 |

---

## 3. 与既有 ADR 的关系

| ADR | 关系 |
|-----|------|
| **ADR-016** | `dict_document_type` / `task_id` 1:1 / 完成门禁 **保留**。§2.3「执行页首次进入才 create」对 **工作任务 confirm 自动首建** 开例外；仍禁止同一 `task_id` 第二条内容。 |
| **ADR-054 D8** | 手工 AI 采纳选列 **不变**。本 ADR **仅**自动首写按 D11 映射 `paid_body` / `free_body`。 |
| **ADR-074** | confirm 全节点 task、`plan_id=NULL`、不建 `oa_content_plan` **不变**。本 ADR 在 confirm **之后**增加：拷贝 `documentType`、同步建 DRAFT、afterCommit jingcai；withdraw 增加取消 Job + 删草稿。 |
| **ADR-075** | 合并执行组仍只生成 **一套** SOP task；自动草稿 / jingcai 按这批 `CONTENT_GENERATION` task 执行（每 task 1 内容）。 |
| **ADR-076** | jingcai 组包 / Football 双写路径 **不变**。自动 Job 允许 `matchPlays` 空；`matchScheme` ≥1 场仍必填。手工 AI 玩法可选与产品已改 UI 对齐。 |
| **ADR-056** | `assignee_id` / `creatorUserId` / 作者展示：写入 `resolveStorableUserId`，回显 `resolvePresentableUserId`。 |

---

## 4. Schema / 状态字段

全表继续 `tenant_id` 隔离（1504）。枚举 `@InDict`（1503）。

| 变更 | 说明 | 建议版本 |
|------|------|----------|
| `oa_sop_node.document_type` | VARCHAR(32) NULL；`CONTENT_GENERATION` 保存/启用时必填 | Slice A 定（建议 V194+，以当时最新 V 为准） |
| `oa_task.document_type` | VARCHAR(32) NULL；confirm 自节点拷贝 | 同上 |
| `oa_production_content.ai_generate_status` | VARCHAR(32) NULL；`@InDict dict_ai_generate_status` | 同上 |
| `oa_production_content.ai_generate_error` | VARCHAR(500) NULL；失败原因（可展示） | 同上 |

**`dict_ai_generate_status`**（新增，非付费墙列）：

| value | label | 含义 |
|-------|-------|------|
| `QUEUED` | 排队中 | confirm 后已入队、尚未调 jingcai |
| `GENERATING` | 生成中 | 正在调用 jingcai |
| `SUCCESS` | 成功 | 已写入对应正文列 |
| `FAILED` | 失败 | 可重试；`ai_generate_error` 非空 |

**不**新建 `oa_*_job` 表：仓库内异步生成无独立业务 Job 表惯例（XXL-JOB 仅用于采集/红黑等 **cron**，不适合 confirm 一次性扇出）。重试 / 取消以内容状态 + 逻辑删除为准：withdraw 将未完成 Job 视为取消（内容已删或 status 不再 `QUEUED`/`GENERATING` 则 Worker 跳过）。

`dict_document_type` **五值**（ADR-016，全部自动 jingcai）：

`SHORT_VIDEO_SCRIPT` · `NEW_ACCOUNT_TRAFFIC` · `POST_MATCH_REVIEW` · `OFFICIAL_PLAN` · `PREHEAT_PREVIEW`

---

## 5. confirm / 自动草稿 / jingcai 伪代码

```
// confirm TX（ADR-074/075 + 本 ADR D2/D3/D4）
sheet = loadDraftSheet(...)
validate rows + SOP + documentType on each CONTENT_GENERATION node  // 缺/非法 → 1503
for each assignment (or execution group):
  template = enabled SOP by marketing_plan
  for each node:
    task = create oa_task(..., document_type = node.document_type,
                           plan_name = workDate + author + matchSummary + nodeName)
    if node.node_type == CONTENT_GENERATION:
      content = create oa_production_content(          // 同 TX，保证 confirm 后必有 DRAFT
        task_id, document_type, author_id, ip_group_id,
        title = task.plan_name,
        content_type = ARTICLE, status = DRAFT,
        competition_ids / matchScheme from task competitions (matchPlays empty),
        match_type = 1,
        paid_body / free_body empty,
        ai_generate_status = QUEUED,
        creatorUserId = resolveStorableUserId(task.assignee_id)
      )
      // 沿用现网 create 双写 Football 草稿（ADR-054 D4）；失败不阻断策略与现网 create 一致
afterCommit:
  for each auto-created content:
    enqueue AsyncJingcaiJob(contentId)   // 并行；不等 DAG

// AsyncJingcaiJob（D7–D12）
if content deleted or status not QUEUED/FAILED-retry: return
content.ai_generate_status = GENERATING
isPaywall = (documentType == OFFICIAL_PLAN)
call existing generate context:
  authorId, matchScheme (≥1, plays 可空), matchName, documentType, isPaywall
on success:
  if isPaywall: paid_body = result   // 仅自动首写
  else:         free_body = result
  ai_generate_status = SUCCESS; ai_generate_error = null
  // 不改 task.status
on failure:
  ai_generate_status = FAILED; ai_generate_error = reason
  // confirm 已成功；task 仍 PENDING；内容仍 DRAFT

// withdraw（D13）
if any linked task IN_PROGRESS/COMPLETED: 1502
cancel unfinished Jobs (Worker sees deleted / not QUEUED|GENERATING → skip)
delete DRAFT contents linked to this sheet's tasks
cancelAndHide tasks; sheet = DRAFT   // ADR-074
```

---

## 6. API / UX 影响（Spec 增量，非实现）

| 区域 | 变更 |
|------|------|
| SOP 节点 | `documentType`；`CONTENT_GENERATION` 必填 1503 |
| Task | 响应带 `documentType`；执行页 `linkedContent` 带 `aiGenerateStatus` / `aiGenerateError` |
| confirm | 流程增加拷贝 + 建 DRAFT；**响应契约不新增字段**；成功后异步 jingcai |
| generate | `context.isPaywall` 正式写入契约（请求上下文）；`matchPlays` 可空 |
| 重试 | 复用 `POST /ops/ai-content/generate`（或再入队）；无新字段 |
| UX | SOP 属性面板 `DictSelect dict_document_type`；任务/内容「生成中 / 失败 / 重试」 |

---

## 7. 实现 Slice（一片一会话）

| Slice | 名称 | 范围 | 依赖 |
|-------|------|------|------|
| **A** | Schema + SOP `documentType` | Flyway 三列 + `dict_ai_generate_status` seed；SOP node API/UX `DictSelect`；`CONTENT_GENERATION` 必填 1503 | — |
| **B** | confirm 拷贝 + 自动 DRAFT | confirm TX：拷贝 `document_type`、建 1:1 DRAFT、组 `matchScheme`（空玩法）、`plan_name` 含节点名 | A |
| **C** | afterCommit jingcai Job | 并行入队；D8–D11 组包与首写列；失败不回滚 confirm | B |
| **D** | 失败 / 重试 UX | 任务执行页 + 内容列表/编辑展示 `GENERATING`/`FAILED` + 原因 + 重试 | C |
| **E** | withdraw 取消 Job + 删草稿 | 未完成 Job 取消；删除关联 DRAFT；1502 规则不变 | C |

**推荐顺序**：A → B → C → D → E。下一实现会话 **只做 Slice A**。

建议实现编号 **S-21**（S-20 已占用）；子片 A–E 不与其他 Slice 混会话。

---

## 8. 后果

- 工作任务 confirm 后，内容生成节点 **立刻**有可打开的 DRAFT（不再必须先点「进入内容创作」才建记录）。
- 自动正文可能在无玩法情况下由 jingcai 生成；用户仍可事后在编辑页补玩法并按 ADR-054 **手工**再生成/采纳。
- ADR-054 D8 对 **自动首写** 不再绝对禁止按 `documentType` 选列；文档与实现须把该例外写死为「仅此路径一次」。
- 红黑 Job（ADR-072）扫描 `CONTENT_GENERATION` 关联内容时，可能更早看到 DRAFT（含空正文或已填充）；**不**改变红黑抽取规则（本 ADR 不改 ADR-072）。

---

## 9. Out of Scope

- 自动或 confirm 路径把任务标完成。
- confirm 事务内同步 jingcai。
- 新增付费墙 / `is_paywall` **持久化列**。
- 新建独立 Job 表或 XXL-JOB cron 专用于本扇出。
- FR-M2-009 计划管理启动后的自动建草稿 / 自动 jingcai（**未锁定**，见 §10）。
- 改 member-server / jingcai 服务契约。
- `CONTENT_PUBLISH` 节点执行与完成条件（仍 BLK-M2-009）。

---

## 10. 明确不发明（遗留，实现不得臆补）

| # | 问题 | 处理 |
|---|------|------|
| 1 | 计划管理（FR-M2-009）启动是否同样自动 DRAFT + jingcai | **本 ADR 不做**；未获产品确认前禁止实现 |
| 2 | 空正文是否必须写固定占位文案 | **允许空**；不规定必须「待生成」等文案 |
| 3 | jingcai 并发限流 / 自动重试次数 | **不新造**；沿用现网 generate 超时；失败靠人工重试 |
| 4 | 传足套餐场次数是否约束自动 Job | 自动 Job **只**要求 ≥1 场赛程；不发明传足套餐推断 |
| 5 | 用户已改过但仍为 DRAFT 的自动稿，撤回是否删除 | 产品已接受 **一律删除 DRAFT**；不按「是否编辑过」分支 |

---

## 11. 验收（Spec 闸门，非本会话实现）

- CHECKLIST / TESTCASES 由 **Slice A–E** 各自补 P0；本文件只锁定决策。
- P0 方向：SOP 无 `documentType` 不能保存内容生成节点；confirm 后 1:1 DRAFT；confirm 立即成功；jingcai 失败仍 CONFIRMED；`OFFICIAL_PLAN` 进 `paid_body`、其他进 `free_body`；withdraw 删 DRAFT 并取消未完成 Job；手工 AI 采纳仍用户选列。

---

## 12. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-09-02 | Accepted：产品 Owner 确认默认（标题=plan_name、空正文、无付费墙列、五类均 jingcai、afterCommit、失败可重试、撤回删草稿） |
| 2026-09-03 | 指针：confirm 钉钉提醒执行人见 [ADR-078](./ADR-078-工作任务确认登记钉钉提醒执行人.md)；本 ADR jingcai Job **不**发钉钉 |
| 2026-09-03 | 指针：任务完成门禁见 [ADR-079](./ADR-079-任务完成工作说明与内容审核通过门禁.md)；本 ADR jingcai Job **不变** |
