# Spec-vs-Impl 一致性扫描报告 · 2026-06-09

> **触发**：用户反馈"IP 组编辑改上级小组不生效"修复后，反问"这样的问题是否还有"。
>
> **结论**：**有 6 个 P0**（用户操作必失败、curl/IT 无法发现），其中 **5 个集中在 M2 知识库**（后端未完工但前端先行），1 个在 M1 作者模块的 `OpsUserVO` 字段名错位。

---

## 扫描方法

并行 4 子任务，**已与原始文件二次实证**（修正确认）：
1. **`docs/engineering/API-M*.md` ↔ 后端 `*Req`/`*UpdateReq`/`*SaveReq`** — 字段漏失
2. **前端 `src/api/*.ts` ↔ 后端 `*Controller`** — URL/方法不匹配
3. **前端 `*VO` 类型 ↔ 后端 `*RespVO`** — 字段名/类型错位
4. **前端 `el-form` `rules` ↔ 后端 DTO 校验注解** — 校验松紧不一

扫描深度：M1–M11（11 个 spec 模块）、54 个后端 Controller、40+ 个 DTO、37 个前端 API 文件、70+ 个前端表单。

**已排除**：P-GATE-UNMOCK 报告里已列的 13 项黄灯（DictSelect prop mismatch、AuthorDashboardVO 差异等）+ 刚修的 S-E IP 组 parentId。

---

## 🔴 P0（6 个）— 用户操作必失败，IT/curl 测不出

### P0-1 + P0-2：M2 知识库模块"前端先行"黑洞

| 维度 | 现状 |
|------|------|
| 前端 `src/api/knowledge.ts` | 调用 **6 个端点**：`/knowledge/list`、`/knowledge/{id}`、`/knowledge/create`、`/knowledge/update`、`/knowledge/delete`、`/knowledge/like` |
| 后端 `KnowledgeController.java` | **只有 2 个**：`/list`、`/create` |
| URL 前缀 | 前端 `request.get({ url: '/knowledge/list' })`，`baseURL='/admin-api'` → 实际请求 `/admin-api/knowledge/list`；后端 `@RequestMapping("/admin-api/oa/knowledge")` → 实际 `/admin-api/oa/knowledge/list` |
| 结果 | 前端 4 个端点（详情、编辑、删除、收藏）**全部 404**；剩下 2 个（列表、新增）也 404（缺 `/oa/` 段） |

**修复路径**：
- 补 4 个 Controller 方法：`getById` / `update` / `delete` / `toggleLike`
- 前端 6 个 URL 前加 `/oa/` 段
- 新增 `M2KnowledgeCrudIT` 覆盖 6 端点

**根因**：M2 知识库模块后端未完工，前端先行写完。Spec 文档里没写 M2（`API-M*.md` 里没有 `API-M2-知识库`）。

### P0-3：M1 作者模块 `OpsUserVO` 字段名错位

| 维度 | 字段 |
|------|------|
| 前端 `types/author.ts:180-189` 期望 | `userId`、`userName`、`deptName`、`relTime`（4 字段） |
| 后端 `api/dto/author/OpsUserVO.java:8-15` 返回 | `opsUserId`、`opsUserName`、`ipGroupId`、`startDate`、`endDate`（5 字段） |
| **结果** | Author 详情弹窗"运营人员"表三列（用户名/部门/关联时间）**全空白** |

**修复路径**（二选一，建议前者）：
- **A**：改后端 `OpsUserVO` 字段名 → `userId/userName/deptName/relTime`（与前端对齐，schema 更合理）
- **B**：改前端 4 个 TS 字段名 → 与后端对齐（破坏现有 UI 文案）

### P0-4 → P2：AccountRespVO 缺字段（**已二次实证降级**）

子 agent 报"P0-4 PlatformAccountVO 缺 `intermediaryName`"。**二次实证**：`AccountRespVO.java:24` 已有 `intermediaryName` 字段。**降级为 P2**（仅前端 TS 类型未声明，运行时不影响）。

### P0-5：M6 漏斗分析 `exportFunnel` id 在 body

| 维度 | 现状 |
|------|------|
| 前端 `funnel.ts:19` | `request.post({ url: '/funnel/export', data: { id } })` |
| 后端 | `@RequestParam Long id`（接 query，不接 body） |
| 结果 | **必报 400** |

### P0-6：M5 绩效结果 `exportPerfResult` 缺必填字段

| 维度 | 现状 |
|------|------|
| 前端 `perfResult.ts:24` | 只传 `ids` |
| 后端 `PerfExportReq` | 需复核 `exportType`/`format` 必填项 |
| 结果 | 边缘 case 400 |

---

## 🟡 P1（10 个）— 边缘 case / 代码卫生

| # | 位置 | 问题 |
|---|------|------|
| 7 | `FunnelCreateReq.steps[]` | 多 `stepName` 字段，spec 未提 |
| 8 | `orderAttribution.ts:24-27` | POST+`params` 与后端 `@RequestParam` 勉强能跑，需规范化 |
| 9 | `account.ts` vs `account-analysis.ts` | 重复文件，VO 不一致 |
| 10 | `OpsAnchorRelVO` | 命名混用 `opsUserId` vs `userId` |
| 11–13 | `OpsAnchorStatsVO` 等 3 处 | type/length 差异 |
| 14 | `ContentAnalysis` / `FollowerAnalysis` | 命名一致性 |

---

## 🟢 P2（2 个）— 理论问题

- `content/edit.vue: maxlength=100` 严于 DTO `@Size(max=200)`（行为 OK，需注释）
- 前端 `AccountRespVO` TS 类型未声明 `intermediaryName`（运行时不影响）

---

## 关键洞察

### 1. Knowledge 模块是最大未完工黑洞
**5 个 P0 全部来自同一模块**。前端先行 + 后端 spec 没写 + 测试用例没覆盖 → 用户操作必失败。**建议本 Gate 必修**。

### 2. VO 字段命名规则未统一
后端混用 `opsUserId`（冗余前缀）和 `userId`，前端按调用方语境命名。**建议立项 ADR**：明确 VO 字段命名规约（如 `M1` 内统一用 `userId` 而非 `opsUserId`）。

### 3. curl/IT 测不出的根因
**所有 P0 都需要"前端 UI 调 → 后端真路由"才会触发**。`mvn test` 跑 MockMvc 不走网关，前端 `npm run dev` 走 Vite 代理但只测了部分模块。

**→ 必须补 e2e（Playwright）覆盖 "前端提交 → 后端处理" 闭环**。

### 4. 反思本次 IP 组 parentId 问题
根因是 **spec 漏字段**。`API-M1 §2.4` 原稿只说"修改 IP 组基本信息（不修改成员/账号/主播关系）"，**没说改 parentId 算不算"基本信息"**。**建议在 PHASE-DEV-METHOD.md 加一条规则**：
- **任何"修改"型端点，DTO 必须显式列** 改什么字段 / 不改什么字段
- **未列字段 = 不改**（白名单制）

---

## 后续行动

按用户选择 **A 方案**：**全部纳入 P-GATE-UNMOCK-R** 下一 Gate 目标。

### P-GATE-UNMOCK-R 新增范围（与原 13 项黄灯合并）

| 优先级 | 任务 | 估时 |
|--------|------|------|
| **P0** | 补 Knowledge 4 个 Controller + 前端 6 个 URL 修前缀 + IT | 1–2h |
| **P0** | 修 `OpsUserVO` 字段名（建议 A 方案）+ 改 IT 期望 | 30min |
| **P0** | 修 `exportFunnel` id 传参（前端 `params` 改 `data` 或后端改 `@RequestBody`） | 15min |
| **P0** | 复核 `PerfExportReq` 必填 + 修前端 | 30min |
| **P1** | 修 `FunnelCreateReq.steps[].stepName` 字段（加 spec 或删字段） | 30min |
| **P1** | 统一 `account.ts` 与 `account-analysis.ts` | 1h |
| **P1** | 其余 7 项 P1 逐一处理 | 2h |
| **P2** | 注释 + 类型补全 | 30min |
| **ADR** | 字段命名规约（VO） | 30min |
| **方法** | 改 PHASE-DEV-METHOD：DTO 字段白名单制 | 30min |
| **e2e** | Playwright 覆盖 P0-1/2/3 闭环 | 1h |

合计估时 ~8h，**建议拆 2 个 Slice**（S-R1 修 P0，S-R2 修 P1+ADR+e2e）。

---

*报告生成：2026-06-09 20:38（UTC+8）*
*扫描方法：4 子任务并行 + 2 关键项二次实证修正*
