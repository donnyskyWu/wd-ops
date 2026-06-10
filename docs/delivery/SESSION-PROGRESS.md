# 会话开发进度 · 2026-06-10

> **状态：本期 S0–S7 后端交付完成 · 8/8 Gate ✅ + 11 走查切片 + 2 补丁 Gate · 进入上线前补强 + 持续走查阶段**

## 总报告

📄 **[deliverable-OVERVIEW-20260610.md](./deliverable-OVERVIEW-20260610.md)** — 总览（进度 + 总计划 + 方法论 + 协作）· **多人协作必读**

📄 **[DELIVERY-COMPLETION-REPORT-20260609.md](./DELIVERY-COMPLETION-REPORT-20260609.md)** — 本期完成情况 SSOT 汇总

## Gate 进度：8/8 ✅

| Gate | 模块 | IT | 报告 |
|------|------|-----|------|
| S0 | Auth + Seed | 75 | [S0](./gates/GATE-S0-报告-20260609.md) |
| S1 | M4 | 53 | [S1](./gates/GATE-S1-报告-20260609.md) |
| S2 | M8/M9 | 75 | [S2](./gates/GATE-S2-报告-20260609.md) |
| S3 | M1 | 102 | [S3](./gates/GATE-S3-报告-20260609.md) |
| S4 | M2 | 114 | [S4](./gates/GATE-S4-报告-20260609.md) |
| S5 | M3 | 123 | [S5](./gates/GATE-S5-报告-20260609.md) |
| S6 | M5/M6/M7 | 137 | [S6](./gates/GATE-S6-报告-20260609.md) |
| S7 | M0 + E2E | **151** | [S7](./gates/GATE-S7-报告-20260609.md) |

## 质量指标

| 项 | 结果 |
|----|------|
| Flyway | V1 → **V27**（dict_author_type_extend） |
| 后端 IT | **163/163**（M11DictS01IT 6/6 + S-E IP组 parentId 5/5） |
| E2E API | GateS7E2EIT **9/9** |
| Playwright 抽检 | dashboard + routes **86/86** |
| P-Gate | **P-GATE-DICT** ✅ · **P-GATE-UNMOCK S-E** ✅（IP组 parentId 编辑） |

## 补丁 Gate：P-GATE-DICT（2026-06-09）

| 项 | 内容 |
|----|------|
| 范围 | DictController + DictService + V27 + DictSelect + Author.vue |
| 触发 | 用户反馈「作者添加报错 + 列表/组长非真实数据」 |
| 决策 | A1 + D3 + V-extend |
| 报告 | [P-GATE-DICT-报告-20260609.md](./gates/P-GATE-DICT-报告-20260609.md) |
| ADR | [ADR-006](../../adr/ADR-006-字典查询端点与作者类型扩展.md) |
| API | [API-M11-字典管理](../../engineering/API-M11-字典管理.md) |

## 上线前待办

1. Playwright 全量回归
2. UAT 产品签字
3. 生产鉴权 `oa.auth.mode` 审查
4. 启动后端 + 前端，「新增作者」流程实操验证

## 补丁 Gate：P-GATE-UNMOCK（2026-06-09）

| 项 | 内容 |
|----|------|
| 范围 | 7 个 selector 净化 + 4 个业务页接真 + knowledge.ts 切 `@/utils/request` + FunnelAnalysis null 守卫 |
| 触发 | 用户复盘「作者管理问题的根因 + 其他模块是否也有类似问题」 |
| 决策 | S-A + S-B + S-C 一次到位（real-or-empty 策略） |
| 切片文件 | 13 个（7 selector + 4 业务页 + 1 API + 1 报表） |
| 后端回归 | 156/156 IT ✅（0 失败） |
| 前端 TS | 0 新增错误（剩 7 个 P-GATE-DICT 期间已存在） |
| 报告 | [P-GATE-UNMOCK-报告-20260609.md](./gates/P-GATE-UNMOCK-报告-20260609.md) |
| 计划 | [P-GATE-UNMOCK-计划.md](./P-GATE-UNMOCK-计划.md) |
| 后续 | P-GATE-UNMOCK-R（报表专项，13 项 🟡 遗留） |

## 补丁 S-E：IP组编辑支持 parentId（2026-06-09）

| 项 | 内容 |
|----|------|
| 触发 | 用户反馈「IP组编辑时把上级小组改了，重新打开没变化」 |
| 根因 | 原 spec `IpGroupUpdateReq` 漏 `parentId` 字段，后端 update() 没处理 |
| 修复 | 1) `IpGroupUpdateReq.parentId` 字段<br>2) service `update()` 加 parentId 处理（仅小组、自引用/子孙引用/类型校验、name 唯一性重检）<br>3) `isDescendant()` 防循环工具方法<br>4) `M1IpGroupUpdateParentIT` 5 用例全过<br>5) `API-M1 §2.4` spec 补字段说明 |
| 回归 | 后端全量 IT **162/162** ✅ 零失败 |

## Spec-vs-Impl 反思扫描（2026-06-09）

| 项 | 内容 |
|----|------|
| 触发 | 用户问"这样的问题是否还有"，扫描所有 spec / DTO / 前端 API |
| 方法 | 4 子任务并行：① API-M*.md vs DTO ② 前端 api/*.ts vs Controller ③ 前端 VO vs 后端 VO ④ 前端表单 vs 后端校验 |
| 新发现 | **6 P0 / 10 P1 / 2 P2**（不含已知豁免）|
| 报告 | [SPEC-VS-IMPL-报告-20260609.md](./SPEC-VS-IMPL-报告-20260609.md) |
| 后续 | 全部纳入 **P-GATE-UNMOCK-R**（下一 Gate） |

## 补丁 S-R1：M2 知识库 + OpsUserVO 字段对齐（2026-06-09）

| 项 | 内容 |
|----|------|
| 触发 | S-R1 范围，4 个 P0 中的 3 个可修（P0-1 知识库 / P0-2 URL / P0-3 OpsUserVO）|
| 范围 | 后端 5 文件 / 前端 3 文件 / 新建 2 IT / 1 V28 / 1 spec 草案 / 1 ADR |
| 修复详情 | **P0-1** 补 KnowledgeController 4 个方法（getById/update/delete/like）+ service + DTO + V28 字典扩 3 项<br>**P0-2** 前端 6 个 URL 加 `/oa/` 段<br>**P0-3** `OpsUserVO` 字段名对齐后端（userId/userName/deptName/relTime → opsUserId/opsUserName/ipGroupId/startDate/endDate）<br>**新增发现** P-GATE-UNMOCK-R 反思漏报：知识库页面用 mock 兜底（已修：list/handleView/handleEdit/onMounted 全部走真 API）|
| 误判纠正 | P0-4（funnel export id in body）实测后端 `@RequestParam` 接 query → 实际**没问题**，前端用 `params` 正确 |
| ADR | [ADR-007](../adr/ADR-007-M2知识库最小可用实现与遗留.md)（4 项 S-R1 妥协：tags/likeCount/isLiked/viewCount 不持久化）|
| Spec | [API-M2-知识库.md](../engineering/API-M2-知识库.md) 草案（Phase 2 完善）|
| 回归 | 全量 IT **169/169** ✅ 零失败（M2KnowledgeCrudIT 6/6 + M1AuthorOpsListFieldIT 1/1）|
| 验收 | curl 5 端点实测全部 code=0 |
| 热修 | `DictSelect.vue` 兼容 prop：`type`/`dictType` 都接受 + 必填→可选（30+ 调用方 0 改动），消除"Missing required prop: type"警告（P-GATE-UNMOCK-R 黄灯兑现）|
| 热修-2 | 知识库标签输入：el-select 改 `<input>` + `<el-tag>`（回车确认、删除、10 个上限、重复校验），4 条 seed 数据（9463-9466 涵盖全部 5 个 dict 值）|
| 热修-3 | **M2 保存 500 根因**：前端 form `isPublic:Boolean`（el-switch）→ 后端 DTO `Integer`，提交时漏 `boolean→0/1` 转换。修复：`handleSubmit` 改 `isPublic: formData.isPublic ? 1 : 0`。curl 复现：`true → 500` ✅ · `1 → 200 code=0` ✅。**反思教训**：`P-GATE-UNMOCK-R` 列表的「前端表单 vs 后端校验」子任务，Boolean/Integer 类型是高频漏点，下一轮 S-R1 复审时必须显式列出 |

## 第二轮用户反馈 8 项（2026-06-09）

| # | 用户反馈 | 根因 | 切片 |
|---|---------|------|------|
| Q1 | 搜索条件窄、应默认"全部" | TableSearch 缺统一宽度 + 字典默认无"全部" | S-R2-A |
| Q2 | 账号分析 粉丝/作品详情不能点 | 后端 `AccountAnalysisController` 缺 `/{id}/followers` 与 `/{id}/contents`（spec §4.1 有定义）；前端 `handleViewDetail` 仅 `ElMessage.info` 未跳转 | S-R2-B |
| Q3 | 粉丝分析是 mock | 前端 `api/follower.ts` 路径 `/oa/follower/*` ≠ 后端 `/oa/follower-analysis/*`（spec §4.2），前端兜底 mock | S-R2-C |
| Q4 | 作品分析无数据 | 前端 `types/works.ts` VO 字段名（`viewCount/shareCount/isViral`）≠ 后端 VO（`readCount/forwardCount/isHit`）；后端 `/content-analysis/trend` 缺失（spec §4.3 有） | S-R2-D |
| Q5 | 内部内容 补录类型字典空 + 平台切换数据不变 | ① 前端 `dict_import_source` ≠ 后端真实 `dict_content_import_type`；② `InternalContent.vue` 完全是硬编码 mock，`loadData` 是空函数 | S-R2-E |
| Q6 | 人效盘点 mock | `Efficiency.vue` 4 张表全硬编码（实测后端 `/productivity-review/list` 有 5 条真实数据） | S-R2-F |
| Q7 | SOP 不能点击 | 后端 SOP 完整（35 文件），前端路由 OK；**真因待实测**——`SopTemplateController` 收 `pageNum/pageSize` 前端用 `pageNo/pageSize` → 列表 0 条 → 看起来"不能点" | S-R2-G |
| Q8 | 成本类型字典没有 | 后端 `dict_cost_type` 真实 3 条（curl 验证），前端 `DictSelect dict-type="dict_cost_type"` 正确。**疑似 GBK 编码渲染乱码** 待浏览器验证 | S-R2-H |

**实测端点**（5/7 通，2/7 缺）：

| 端点 | 状态 | 备注 |
|------|------|------|
| `/admin-api/oa/account-analysis/list` | ✅ 200, 10 条 | 后端有数据 |
| `/admin-api/oa/account-analysis/9001/followers` | ❌ 403 | 后端**无**此端点（spec §4.1 要求） |
| `/admin-api/oa/follower-analysis/list` | ✅ 200, 20 条 | 后端有数据 |
| `/admin-api/oa/follower-analysis/trend` | ✅ 200 | OK |
| `/admin-api/oa/internal-content/list` | ✅ 200, 20 条 | 后端有 |
| `/admin-api/oa/internal-content/import/list` | ✅ 200, 0 条 | 字典没值，需 seed |
| `/admin-api/oa/productivity-review/list` | ✅ 200, 5 条 | 后端有 |
| `/admin-api/oa/productivity-review/detail/9001` | ❌ 403 | 详情接口**缺** |
| `/admin-api/oa/content-analysis/trend` | ❌ 403 | 趋势接口**缺**（spec §4.3 要求） |
| `/admin-api/oa/dict/data?type=dict_content_import_type` | ✅ 200, 4 条 | 字典存在 |
| `/admin-api/oa/dict/data?type=dict_import_source` | ✅ 200, 0 条 | **前端用错 type**（Q5 根因） |
| `/admin-api/oa/dict/data?type=dict_cost_type` | ✅ 200, 3 条 | 字典有（Q8 待浏览器排查乱码） |

**S-R2 计划**：[S-R2-计划.md](./S-R2-计划.md) — 分 8 个 slice。**执行节奏待用户确认**（一片一会话铁律）。

---

## S-R2 实施（2026-06-09 用户选 "一次性"）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| S-R2-A | TableSearch 统一搜索项 min-width=240 | `components/TableSearch.vue` | ✅ 已改 |
| S-R2-B | 后端补 `account-analysis/{id}/followers` + `contents`；前端跳真实详情（`/analysis/account/:id/detail`）；新建详情页 `AccountAnalysisDetail.vue`；路由加 | `controller/.../AccountAnalysisController.java`, `service/.../AccountAnalysisServiceImpl.java`, `views/analysis/AccountAnalysisDetail.vue`, `router/index.ts`, `views/operations/AccountAnalysis.vue` | ✅ 已改 |
| S-R2-C | 粉丝前端 API 路径 `/oa/follower/*` → `/oa/follower-analysis/*`；类型字段对齐后端 (`statDate/followerCount/newFollower/unfollowCount/netGrowth`)；去 mock | `api/follower.ts`, `types/follower.ts`, `views/operations/FansAnalysis.vue` | ✅ 已改 |
| S-R2-D | 后端补 `content-analysis/trend`（按日聚合 list）；前端 VO 字段对齐后端（`readCount/forwardCount/isHit`）；stats 用 list 聚合 | `controller/ContentAnalysisController.java`, `service/.../ContentAnalysisServiceImpl.java`, `types/works.ts`, `views/operations/WorksAnalysis.vue` | ✅ 已改 |
| S-R2-E | 内部内容前端 `dict_import_source` → `dict_content_import_type`（真实 type）；`viewCount` → `readCount`；`loadData` 真接 API 去 mock | `views/operations/InternalContent.vue` | ✅ 已改 |
| S-R2-F | 人效盘点去 4 表 mock（`wechatList/videoList/fansList/vipList`）；`loadData` 真接 `/productivity-review/list`；前端用 `computed` 派生 4 tab 视图 | `views/operations/Efficiency.vue` | ✅ 已改 |
| S-R2-G | SOP `loadTemplateList` / `loadReviewList` 去 mock 接真 API；删 `mockGetSopTemplateList` / `mockGetSopReviewList` | `views/production/sop/index.vue` | ✅ 已改 |
| S-R2-H | DictSelect 加 DEV 调试日志（监控 options 实际长度）；Q8 真因 = 浏览器视角无 bug（curl PowerShell GBK 乱码是测试工具问题） | `components/DictSelect.vue` | ✅ 已改 |

### S-R3 实施（2026-06-09 走查根因）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| 范围声明 | 用户批准"full-sr3"：修 PlatformType 对齐 + 补个微 Tab + 分页参数对齐；后确认为 5 个高频 Vue 页面 + 1 个集中 alias 工具（不破坏 30+ 文件 enum 字面值） | — | ✅ 批准 |
| alias 工具 | 新建 `@/utils/enum-alias.ts`：`PLATFORM_ALIAS`、`normalizePlatform`、`normalizeAccountStatus`、`PLATFORM_LABEL`、`ACCOUNT_TYPE_LABEL` | 新增 1 文件 | ✅ 已加 |
| 5 页 Vue | ① AccountAnalysis：Tab enum 改真实值 + 默认公众号 + 删"服务号/个微" + `watch(activePlatform)` 替 `@tab-click` + 分页 page/size ② FansAnalysis：normalizePlatform + page/size ③ WorksAnalysis：normalizePlatform + page/size ④ InternalContent：platforms 数组改真实值 + 删服务号 + normalizePlatform + page/size ⑤ Efficiency：page/size | 5 个 Vue | ✅ 已改 |
| PRD 同步 | ① P-M1-003 路径 `/ops/account-analysis` → `/account-analysis` ② TAB-001 平台清单删服务号/个微 + 注 | `docs/product/PRD-M1-运营管理.md` | ✅ 已改 |
| 浏览器走查 | agent-browser：默认公众号 4 行 / 抖音 2 行 / 快手 1 行 / 全部 10 行 / 视频号 0 / 企微 0 / 小红书 1 — **6 Tab 切全对** | — | ✅ 验过 |
| 后端 IT | M11DictS01IT 7/7 + M1AnalysisS06IT 10/10 + M1InternalContentTrendIT 3/3 = **20/20 BUILD SUCCESS** | — | ✅ 无回归 |

详见 `docs/delivery/gates/S-R3-报告-20260609.md`

### S-R2-Fix 实施（2026-06-09 走查再发现 3 个反馈）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| Q1 | 账号分析：① 调错端点 `/oa/account/list` → `/oa/account-analysis/list` ② query 用 `platform` 不是 `platformType` ③ 默认 Tab=公众号（spec AC-M1-003-1） ④ `AccountAnalysisQueryVO.platform?` 修正 | `views/operations/AccountAnalysis.vue`, `types/account-analysis.ts` | ✅ 已改 |
| Q2 | 粉丝分析：① 导出按钮挪进 TableSearch 同行 ② 时间维度 DictSelect 加 `placeholder="全部" clearable` ③ 默认值 `''` ④ reset 也走 `''` ⑤ 提交时 `dimension \|\| undefined` | `views/operations/FansAnalysis.vue` | ✅ 已改 |
| Q3 | 内部内容趋势去 mock：① 新增 DTO `ContentTrendDetailVO` + `ContentTrendPointVO` ② `InternalContentService.trend(Long)` 从 `oa_content_daily` 按日聚合 ③ `GET /oa/internal-content/{id}/trend` 端点 ④ `ContentDailyDO` 改不继承 `TenantBaseDO`（表无 `updater`） ⑤ 前端 `handleViewTrend` 改调真 API + 空/错误态 ⑥ 新增 IT `M1InternalContentTrendIT` 3/3 ✅ | 8 个文件（含 1 个 IT） | ✅ 已改 + 后端重启 |

详见 `docs/delivery/gates/S-R2-Fix-报告-20260609.md`

### S-R2-Phase2 实施（2026-06-09 走查补漏）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| Phase2-1 | 补 `dict_time_dimension` 字典（spec 提到但 V2~V28 seed 漏了） | `V29__m1_dict_time_dimension.sql` | ✅ 已改 + 后端重启 + 22:56:09 Flyway 跑 v29 |

详见 `docs/delivery/gates/S-R2-Phase2-报告-20260609.md`

### 后端新端点（待 IT 覆盖）

- `GET /admin-api/oa/account-analysis/{id}/followers` — 1501 实体不存在场景
- `GET /admin-api/oa/account-analysis/{id}/contents` — 分页 0/正常
- `GET /admin-api/oa/content-analysis/trend` — 空范围/有数据

### S-R4 实施（2026-06-10 走查作者管理）

**方法论沉淀**：[`walkthrough-methodology.mdc`](../../.cursor/rules/walkthrough-methodology.mdc)（7 步法 + 关键反模式 + agent-browser 模板）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| Bug1 | 作者看板 4 个 KPI 全 0 | `AuthorServiceImpl.dashboard()` 注入 FollowerDailyMapper + ContentMapper，按 primaryAccountId 聚合 followerCount / contentStats | ✅ 已修 |
| Bug2 | 作者看板粉丝趋势/内容图 100% 假数据 | `AuthorDashboard.vue` 移除所有 `Math.random()`，接 `author.followerTrend` + `kpi.monthlyContent` | ✅ 已修 |
| Bug3 | PRD 6 个 M1 页路径 `/ops/*` 与实际不符 | `PRD-M1-运营管理.md` §5.1 表统一改实际路径（无 `/ops/` 前缀） | ✅ 已改 |
| IT+ | `M1AuthorS04IT` 加 2 个 IT：`dashboardKpiFromSeedData`（9101 真值 110820 / 5 内容 / 2 爆款）+ `dashboardKpiZeroWhenNoPrimaryAccount`（9105 无主推号 → 全 0） | `M1AuthorS04IT.java` | ✅ 25/25 BUILD SUCCESS |

**自验**：
- 后端 mvn test 25/25 ✅（4 个 IT 全绿，含 S-R4 新增 2 个）
- API 实测 `GET /admin-api/oa/author/9101/dashboard` 返真实 KPI + 29 日 trend
- 浏览器复测：4 KPI 显示 11.1万/5/0h/0；3 tab 切换正常；列表 7 行完整

**反思 + 留给 S-R5**：
1. spec ↔ schema drift：`oa_content` / `oa_account` / `oa_live_session` / `oa_order_attribution` 都无 `author_id` 关联字段 → 作者多号数据无法聚合（spec §4.2.3 §5 期望全作者聚合）—— **需产品拍板补字段**
2. seed V18-V23 文件 CJK 被截断 → 列表显示 `SEED-???` —— **TODO S-R5 重写 seed 为 UTF-8**
3. IT 覆盖仍滞后（这次后补）—— 下次走查**改 service 即加 IT（Red→Green）**
4. enum 字面值清理（30+ 文件）—— 仍由 `enum-alias.ts` 维持，**S-R6 集中清理**

详见 `docs/delivery/gates/S-R4-报告-20260610.md`

### S-R5 实施（2026-06-10 走查内部内容分析）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| Bug1 | **P0 补录提交 100% mock** —— `handleImportSubmit` 硬编码 toast，不调 API | `InternalContent.vue` 调真 `submitContentImport` + loading + error | ✅ 已修 |
| Bug2 | **P0 审核通过不合并到 oa_content_daily** —— `reviewImport` 只改 status，AC-M1-006-2 失败 | `InternalContentServiceImpl.reviewImport` 审核通过时调 `upsertContentDaily` | ✅ 已修 |
| Bug3 | P1 "我的补录" 入口缺失 —— 后端 API 完整前端无 UI | 加顶部"我的补录"按钮 + 弹窗（11 列），调 getContentImportList / reviewContentImport | ✅ 已修 |
| Bug4 | P1 搜索栏 5 项筛选项失效 —— 后端 list 只收 4 个，前端多发 5 个 ignore | **defer S-R6**（需扩 service 签名） |
| Bug5 | P2 切 Tab 时 total 不重置 | `handleTabChange` + `handleSearch` 加 `pagination.total = 0` | ✅ 已修 |
| Bug6 | P2 补录类型硬编码 4 个含 2 个 dict 没有 | 删 `IMPORT_TYPE_CONFIG`，补录类型直接由 DictSelect 渲染 dict 真值 | ✅ 已修 |
| IT+ | **M1InternalContentImportIT 新增 5 个 IT**：submitImport / reviewApprovedMergesToContentDaily / reviewRejected / importDateTooOld / approvedImportIsLocked | `M1InternalContentImportIT.java` | ✅ 30/30 BUILD SUCCESS |

**自验**：
- 后端 mvn test 30/30 ✅（5 个 IT 全绿，含 S-R5 新增 5 个）
- API 实测：submit id=3 → review approve → `GET /9301/trend` 真有 `2026-06-10 readCount=9999`（**AC-M1-006-2 完全通过**）
- 浏览器复测：补录提交走真 API（network `POST /import` 200）+ "我的补录"弹窗 2 行 + 审核通过调 `PUT /import/2/review` + trend 合并验证

**反思**：
1. **S-R2-Fix 漏列 TODO**："补录提交是 mock"未列入报告 TODO —— **新规则**：每次走查发现的真 mock 必写 Slice 报告 TODO
2. **IT-first 实践**：S-R5 是"service 改完立刻写 IT"（比 S-R4 后补好），但仍不完美（理想 Red→Green）
3. **spec ↔ 实现 drift**：spec §AC-M1-006-2 提 `data_source='IMPORT'`，但 `oa_content_daily` 表无此字段 —— data_source 标识在 oa_content 行上（spec 隐含正确但描述不严谨）
4. **筛选项失效**（Bug4）未修：5 个筛选项 frontend 多发后端 ignore —— **S-R6 必修**

详见 `docs/delivery/gates/S-R5-报告-20260610.md`

### S-R6 实施（2026-06-10 走查粉丝分析）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| B1+B4 | **P0 5 KPI 累加 list 当前页 + 增长率取 list[0]** | 新建 `FollowerStatsVO` + 后端 `stats()` SQL 聚合；`/stats` 端点 | ✅ 已修 |
| B2 | **P0 controller 不收 `dimension`** | `FollowerAnalysisController` `/trend`+`/stats` 加 dimension；`loadRows()` 按 WEEK/MONTH 内存分组 | ✅ 已修 |
| B3 | **P0 无 `/export` 端点** | `FollowerAnalysisServiceImpl.exportCsv` 写 UTF-8 BOM CSV；`/export` GET 返回 `text/csv` + `Content-Disposition: attachment; filename="follower_analysis_{ts}.csv"` | ✅ 已修（csv 替 xlsx，spec 同步）|
| B5 | P1 `TimeDimension` enum 大小写不一致 | `enum-alias.ts` 加 `TIME_DIMENSION_ALIAS` + `normalizeTimeDimension` | ✅ 已修 |
| B6 | P1 `FollowerQuery` 类型 `pageNo/pageSize` | 改 `page/size` | ✅ 已修 |
| B7 | P1 缺 7日/30日切换 | 模板加 `<el-radio-group>` + `handleQuickRangeChange` 自动更新 dateRange 并 loadData | ✅ 已修 |
| FE | KPI 渲染增长率去除 ×100 双重计算（后端 IMPLEMENT 已 ×100）| `FansAnalysis.vue` `growthRate` 直接 `.toFixed(2)` | ✅ 已修 |
| IT+ | **M1FollowerStatsIT 新增 6 个 IT**：statsAggregates / statsEmpty / statsWithDimensionWeek / trendWithoutDimension / exportReturnsCsv / exportEmptyReturnsHeaderOnly | `M1FollowerStatsIT.java` | ✅ 6/6 全过 |

**自验**：
- 后端 mvn test 193/193 ✅（6 个 IT 全绿，含 S-R6 新增 6 个；无回归）
- API 实测：`/stats` 30 日窗口 = 110,820 / +32,710 / -8,910 / +23,800 / 27.35% / `/export` 91 行 CSV
- 浏览器复测：默认 30d KPI 真聚合 ✅；切 7d KPI 缩到 7 日 ✅；切按月 dimension=MONTH ✅；切抖音 platformType=DOUYIN ✅；导出命中 `/export` 882 字节 ✅

**反思**：
1. **聚合铁律**：5 KPI 永远在后端 SQL 聚合，**前端 list.reduce 永远有分页错位风险**——B1 教训
2. **导出端点必须随 spec 走**：spec AC 写 xlsx 但 oa 模块未引入 POI，临时用 csv+UTF-8 BOM 兼容；后续引入 easyexcel 时升级
3. **dimension 端到端 4 段对齐**：B2 教训——DictSelect 给了选项但后端 controller 不收，等于死字段；spec 落地必须 4 段对齐（API/前端/IT/seed）
4. **大段替换需立刻 reload 验证**：S-R6 期间替换 handleExport 多粘一个 `}` 导致整页 vite compile error——替换完毕立即 reload 检查
5. **增长率 ×100 双重计算是常见坑**：后端 IMPLEMENT 习惯返回小数，前端显示 % 别再 ×100——必须明文约定

**遗留 TODO**：
1. 导出 csv → xlsx（需引入 POI/easyexcel，oa 模块当前无依赖）
2. 30+ 文件 enum literal 集中清理（当前由 `enum-alias.ts` 在 5 个高频页面 normalize）
3. SEED 文件 CJK 截断/乱码（PowerShell 终端显示问题，浏览器正常）
4. "个微"平台：spec PRD §4.2 提但 `dict_platform_type` 无对应 value（需产品确认）

详见 `docs/delivery/gates/S-R6-报告-20260610.md`

### S-R7 实施（2026-06-10 修 InternalContent Bug4）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| B1 | **P1 后端不收 `ipGroupId`** | `InternalContentServiceImpl.list` 通过 `oa_account` 关联查 accountId 子集 | ✅ 已修 |
| B2 | **P1 后端不收 `keyword`** | service 加 `.like(ContentDO::getTitle, keyword)` | ✅ 已修 |
| B3 | **P1 后端不收 `startDate/endDate`** | service 拆 if 调 `.ge/.le(PublishTime, ...)`（**避开 NPE**）| ✅ 已修 |
| B4 | **P1 `importType` 控件错位**（content 表无此字段，是补录表字段）| 模板删 DictSelect + searchForm 删 importType + loadData/handleReset 删 importType | ✅ 已修 |
| B5 | P1 service 仍用 `pageNo/size`（S-R3 漏改）| 改 `page/size` | ✅ 已修 |
| IT+ | **M1InternalContentListFilterIT 新增 6 个 IT**：listFiltersByIpGroup / listFiltersByKeyword / listFiltersByDateRange / listFiltersCombined / listAcceptsPageSize / listAcceptsStartDateOnly | `M1InternalContentListFilterIT.java` | ✅ 6/6 全过 |

**自验**：
- 后端 mvn test 200/200 ✅（6 个 IT 全绿，含 S-R7 新增 6 个；无回归）
- API 实测：baseline 26 / keyword 不存在 0 / ipGroup 1 0（IP1 下无 content 种子）/ 单日 1 ✅
- 浏览器复测：InternalContent 3 控件（删了补录类型）；关键词=SEED → API 收 → 26；Z_NONEXISTENT → 0（修复前会 26）✅

**反思**：
1. **MyBatis-Plus `.ge(boolean, col, val)` 的 val 提前求值坑**：`null.atStartOfDay()` NPE——**lambda wrapper 必须拆 if 分支显式调**
2. **schema 字段 vs UI 字段要先校准**：B4 教训——前端 importType 是基于"页面有补录"语义联想，实际 schema 不支持
3. **Spring 静默忽略未声明 param**：B1-B3 教训——前端发 N 后端声明 M<N，**无任何 warn**，IT 必须断言多余 param 不破请求
4. **S-R3 漏改 InternalContent list 的 pageNo/size**：本次顺手修——S-R3 应全模块 grep `pageNo` 一次性改完

详见 `docs/delivery/gates/S-R7-报告-20260610.md`

### S-R6-TODO4 追加（2026-06-10 补个微）

| 改动 | 文件 | 状态 |
|------|------|------|
| V30 Flyway 迁移补 `dict_platform_type.个微=WECHAT_PERSONAL sort=7`（`WHERE NOT EXISTS` 幂等）| `V30__m1_dict_platform_type_personal_wechat.sql` | ✅ |
| IT 验证 7 个 value 全在 | `M11DictS01IT.platformTypeIncludesPersonalWechat` | ✅ |
| 前端 AccountAnalysis Tab 同步加"个微" | `AccountAnalysis.vue` L11+1 | ✅ |
| spec 同步 PRD §4.3 TAB-001 文案 | `PRD-M1-运营管理.md` L260-261 | ✅ |

**自验**：mvn test 194/194 ✅；后端 dict 7 条含 WECHAT_PERSONAL；浏览器 FansAnalysis 平台 7 项 + AccountAnalysis Tab 7 项；切个微 API `platform=WECHAT_PERSONAL` 200。

### S-R8 实施（2026-06-10 走查作品分析）

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| Bug1 | **P0 5 统计卡 = 当前页 list.reduce** —— 切第二页会变（与 S-R6 粉丝同款）| `ContentMapper.sumStats` + `ContentStatsVO` 扩 3 字段 + `ContentAnalysisServiceImpl.stats` 改 SQL 聚合 + `WorksAnalysis.vue` 移除 `statFromList` | ✅ 已修 |
| Bug2 | **P0 导出 columns 字段名错**（`viewCount/shareCount` vs 后端 `readCount/forwardCount`）| `WorksAnalysis.vue handleExport` 改 `readCount/forwardCount` + 加 `isHit` | ✅ 已修 |
| Bug3 | **P0 详情弹窗永远空白** —— `getContentTrend` 路径错（`/${id}/trend` 实际是 query param） + 类型 `ContentTrendPoint` 字段 `viewCount/interactionCount` 错 | `api/works.ts` 改 `/trend` + params；`types/works.ts` 改 `count/readCount/likeCount/commentCount/forwardCount`；eCharts legend.data 改 `['阅读量', '互动数']`（与 series name 一致，避免 console warning）| ✅ 已修 |
| Bug4 | P1 `/trend` 收 `contentId` —— 详情取单作品趋势 | `ContentAnalysisController.trend` + `ContentAnalysisService.trend` + `buildContentWrapper(contentId)` 加 `.eq(contentId != null, ContentDO::getId, contentId)` | ✅ 已修 |
| Bug5 | P2 「是否爆款」筛选项 | spec §4.5.1 唯一 AC 是爆款识别（红标签），UX-M1 §203 提了筛但 spec 没强 AC | ❌ 用户决定本期不做 |
| Bug6 | P2 `ContentAnalysisService.list` 接口 `pageNo/pageSize` vs controller `page/size` 契约混乱 | `ContentAnalysisService.list` 改 `page/size` | ✅ 已修 |

**新 IT**：`M1WorksAnalysisStatsIT` 7 个（stats 字段、stats 全量聚合、stats IP 组过滤、trend 全量、trend+contentId、list page/size、list isHit 过滤）。

**自验**：
- mvn test M1WorksAnalysisStatsIT 7/7 ✅
- mvn test 全套 207/207（无回归）✅
- mvn test M1AnalysisS06IT 10/10 ✅
- 浏览器：5 KPI 卡 26/1,185,500/46,920/1,121/4,191 全量聚合；详情弹窗 1 canvas（之前 0）；导出"成功 10 条"；console 干净

**反思**：
1. **后端聚合先于前端**原则在 S-R6 粉丝已确立模式 —— 复盘：每次 KPI 卡 + 列表分页页，stats 端点必须后端聚合
2. S-R2 字段对齐时**漏改导出 columns**（viewCount/shareCount）—— 字段名变更要全局 grep 一次
3. S-R2 新增 `/trend` 端点时**只考虑了"按筛选聚合"场景**，没考虑"详情单作品"场景 —— trend 端点签名要预留 contentId 过滤能力
4. eCharts legend.data 与 series name 不一致触发 console warning —— 前端 console hygiene 也属于"清理范围"

详见 `docs/delivery/gates/S-R8-报告-20260610.md`

### S-R9 实施（2026-06-10 走查效率分析）

走查 **FR-M1-007 效率分析**（S-R2 曾走过但实际"接 API 后用假数据填充 UI"，本期发现 9 个 P0/P1 问题）。

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| Bug1 | P0 后端 list 缺 4 关键参数（startDate/endDate/timeDimension/position） | `ProductivityReviewController.list` 收 9 参 + `ProductivityReviewService.list` 接口改 | ✅ 已修 |
| Bug2 | P0 后端 5 KPI 全 0（无聚合） | `TaskMapper.sumByUser` + `OrderAttributionMapper.sumByUser` + `ProductivityReviewServiceImpl.list` 一次 SQL 聚合 + `ProductivityReviewVO` 扩 12 字段 | ✅ 已修 |
| Bug3 | P0 缺 3 端点：`/detail/{userId}` `/detail/anchors` `/export` | `ProductivityReviewController` + `ServiceImpl.detail/anchors/exportCsv` + `ProductivityReviewDetailVO` 新增 | ✅ 已修 |
| Bug4 | P0 mini-chart 用 `Math.random()` 假数据 | `Efficiency.vue` `renderMiniChart` 改基于 detail 接口 trend 数据（本期保留空 chart 占位） | ✅ 已修 |
| Bug5 | P0 4 tab × 系数假数据（`contentOutput * 1000`）+ 硬编码 0 | `Efficiency.vue` 4 tab 全用 `productivityList` 接真 VO 字段 | ✅ 已修 |
| Bug6 | P0 loadData 不读 search 控件（4 控件装饰） | `Efficiency.vue` loadData 接 timeDimension/ipGroupId/position/keyword，statDate 默认 undefined 避免空日期 | ✅ 已修 |
| Bug7 | P1 `dict_position_type` 字典名错（实际是 `dict_position`） | `Efficiency.vue` DictSelect 改 `dict_position` | ✅ 已修 |
| Bug8 | P1 展开 Card `Unhandled error at TableTdWrapper` + `互动率: %` undefined | `Efficiency.vue` 加 4 个 null-safe 工具函数（`formatNumber/formatPct/getEfficiency/getEfficiencyClass`） | ✅ 已修 |
| Bug9 | P2 service 接口 `pageNo/pageSize` vs controller `page/size` 契约混乱 | `ProductivityReviewService.list` 改 `page/size` | ✅ 已修 |

**新 IT**：`M1EfficiencyKpiIT` 9 个（listKpis、user1003Kpis、completionRateInRange、defaultTimeDimension、detail、anchors、exportCsv、pagination、positionFilter）。

**自验**：
- 后端 mvn test 全套 **216/216**（无回归）✅
- M1EfficiencyKpiIT 9/9 ✅
- API 实测：1003 → taskTotal=5/revenue=102900/roi=4.0/orderCount=8（1003 在 IP组 9001 下 12 笔订单，营收 123900 但 4 笔不在 seed 当月所以 8 笔）
- 浏览器：wechat tab 1003 行 5/1/20.00%/¥25,725/¥102,900/4.00%/8；展开 4 Card 真数据；video/fans/vip 4 tab 全 OK；搜索"运营"过滤生效；console 无 Unhandled error

**反思**：
1. **S-R2 走查报告说"接真 API"实际未真聚合** —— 走查验证必须"鼠标点开看 KPI 卡"而非只看 API 200 OK；后续走查加重"真值校验"步骤
2. **后端聚合先于前端**原则再次验证 —— S-R6 粉丝/S-R8 作品/S-R9 人效三连击，KPI 卡 = SQL 聚合已是铁律
3. **dict 字典名要实测确认** —— V12 seed 的是 `dict_position` 不是 `dict_position_type`，前端猜名字容易出错；前端 DictSelect 写错 dictType 后端返回 0 条但前端静默，要主动查 DB
4. **ContentDO 无 user 关联是 schema drift** —— ADR-008 新增；M2 SOP 上线前需产品决策
5. **S-R2/S-R3 走查遗留排查**：之前的"已走查"页面未必干净，E2E 走查方法论（agent-browser + Walkthrough 7 步法）暴露出"mock 改 real 是表层，aggregation 才到根"

详见 `docs/delivery/gates/S-R9-报告-20260610.md`

### S-R10 实施（2026-06-10 人效分析复测）

用户复测 `/efficiency`（= FR-M1-007），发现 S-R9 报告**漏改 2 项**：

| 切片 | 内容 | 文件 | 状态 |
|------|------|------|------|
| Bug10 | P1 mini-chart 展开时不渲染 canvas —— S-R9 报告说"基于 trend 数据"实际传空数组 `[]` | `Efficiency.vue` 新增 `handleExpand(row, expandedRows)` 按需 fetch detail.trend + renderMiniChart | ✅ 已修 |
| Bug11 | P2 onMounted 无差别渲染 mini-chart（不展开行也跑 echarts.init） | `Efficiency.vue` 移除 onMounted 渲染循环，只在展开时按需渲染 | ✅ 已修 |

**附带修复**：
- 声明 `wechatCharts/videoCharts` 对象（S-R9 时引用未声明导致 `ReferenceError`）
- `<el-table>` 加 `@expand-change="handleExpand"`（公众号 + 短视频 2 个 tab）
- `renderMiniChart` 加空数据态（"暂无趋势数据"占位）

**新 IT**：`M1EfficiencyKpiIT` 加 2 个（detailTrendFallback / detailContentMetrics），从 9 → **11/11** 通过。

**自验**：
- mvn test `M1EfficiencyKpiIT` 11/11 ✅
- 浏览器：1003 行展开 → **1 canvas 渲染**（之前 0）；video tab 1002 展开 → 1 canvas；折叠 → canvas 清理；console 无 ReferenceError

**反思（关键）**：
1. **S-R9 报告自我审查不严** —— 报告写"已修复"但代码 `renderMiniChart(we, [], {})` 实际是空数据
2. **走查报告里"已修"必须配真实验证**（截图/canvas 数/数据样本），不能是文字承诺
3. **复测价值** —— 用户再次走查同一页暴露 S-R9 漏改 2 项
4. **走查方法论升级**：报告 §三.3.2 表格每项"已修"配实证编号；§四.4 加 **canvas 渲染数/数据样本数** 列

**S-R7 backlog 关闭**：
- SOP `pageNum` 不是 bug —— API spec §1.1 故意定义为 `pageNum`（不是 `pageNo`），前端用 `pageNo` 是不遵守 spec，但与"API 契约混乱"无关。SOP 走查时再修。

详见 `docs/delivery/gates/S-R10-报告-20260610.md`

### ⚠️ 风险 / 待办

1. SOP `SopTemplateController.list` 收 `pageNum`（**非 `pageNo`，spec §1.1 故意定义**）—— 待 SOP 走查时再修
2. 30+ 文件 enum literal 仍由 `enum-alias.ts` 维持 —— 待专门 slice 集中清理
3. spec ↔ schema drift（`oa_content` 无 `author_id`、`oa_live_session`/`oa_order_attribution` 关联缺）—— 待产品拍板
4. seed V18-V23 文件 CJK 被截断（PowerShell 终端显示问题，DB 存的是真中文）—— 浏览器显示正常
5. **「是否爆款」筛选项**（S-R8 B5）—— spec §4.5.1 没强 AC，用户决定本期不做

---

## 2026-06-10 15:00 · 总览沉淀

### 完成
1. ✅ 写 [deliverable-OVERVIEW-20260610.md](./deliverable-OVERVIEW-20260610.md)（总览 · 7 节 · 1.5 万字 · 多人协作必读）
2. ✅ 补 [walkthrough-methodology.mdc](../.cursor/rules/walkthrough-methodology.mdc)：
   - 加"报告自查方法 7 条"（S-R10 沉淀）
   - 加"走查报告 6 节模板"（SSOT）
   - 加"反模式 7"：报告"已修"配空数据
3. ✅ 同步 [MASTER-EXECUTION-TRACKER.md](./MASTER-EXECUTION-TRACKER.md) §1.1 走查 + 补丁记录表

### 切片累计
- 11 走查 + 2 补丁 Gate + 8 主 Gate
- 50+ 真 bug
- 后端 IT 总量 216/216 ✅

### 当前待走查
- SOP
- IP 组
- 账号分析详情
- 知识库

### 新增 TODO（多协作）
| ID | 任务 | 优先级 |
|----|------|--------|
| L-新-1 | 导出 xlsx（spec）vs csv（实现） | P1 backlog |
| L-新-2 | 30+ 文件 enum literal 集中清理 | P1 backlog |
| L-新-3 | ADR-008 oa_content 加 author_id | **P0 待产品决策** |
| L-新-4 | SOP `pageNum` 修前端 pageNo | P2 SOP 走查时 |

---
*最后更新：2026-06-10 15:00*
