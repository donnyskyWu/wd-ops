# SLICES-M2-S21a — AI 排版（语义分段 + 确定性模板决策）

> **Slice**：S-21a  
> **版本**：v1.0 | 2026-09-08  
> **状态**：**Implemented / Wrap-up**（核心代码与单测已落地；2026-09-14 增 FOOTBALL_AI 一键排版 + V204；Slice DoD 未 100%；阶段 Gate 未通过）  
> **优先级**：P0  
> **预估工时**：6~8 人日  
> **关联 FR**：FR-M2-012-1~6  
> **Gate 边界**：M2 独立 Slice；**一片一会话**；不得据此宣称阶段 Gate 已通过

---

## 1. 目标与规格引用

对已有 ARTICLE 正文提供两种 AI 排版模式，并保证正文文字不变：

- `TEMPLATE_GUIDED`：用户选择公推模板，AI 按 `layout_schema` 生成结构化语义分段，代码映射槽位并渲染。
- `AUTO`：用户不选模板，按 football-layout taxonomy 分段；`FootballTemplateDecider` 用固定特征、权重与阈值在 `decision-scan` / `analysis-report` 中二选一。
- 映射失败默认降级到最近 paragraph / `repeat:true` 槽；返回降级摘要。
- ADR-027 规则链保留为备选/legacy。

实现会话必须用 `@` 引用以下 SSOT，不粘贴或自行改写规则：

- `@docs/adr/ADR-028-M2-AI排版语义分段.md`
- `@docs/product/PRD-M2-AI排版增量.md`
- `@docs/product/UX-M2-AI排版增量.md`
- `@docs/engineering/API-M2-AI排版增量.md`
- `@docs/delivery/SLICES-M2-S21a-AI排版.md`
- `@docs/delivery/TESTCASES-M2-AI排版增量.md`
- `@docs/delivery/CHECKLIST-M2-AI排版增量.md`
- `@docs/football-layout/SKILL.md`
- `@docs/football-layout/shared/semantic-parser.md`
- `@docs/football-layout/template-decision-scan.md`
- `@docs/football-layout/template-analysis-report.md`
- `@docs/adr/ADR-020-M2-公推模板版式套用语义.md`
- `@docs/adr/ADR-021-M2-富文本与版式双向同步.md`
- `@docs/adr/ADR-027-M2-版式资源工作台.md`

---

## 2. Scope / Out of Scope

### 2.1 In Scope

1. 两个 preview/apply API、请求响应 DTO、权限/数据范围/租户校验。
2. `SemanticSegment[]` 结构化输出、taxonomy 校验、Fidelity Gate。
3. `FootballTemplateDecider` 确定性评分及可审计 `templateDecision`。
4. 两款内置 PRESET 模板 schema/默认参数与 M8 `AI_TYPESET_SEMANTIC` seed。
5. `SegmentSlotMapper` 语义映射、paragraph/repeat 降级、`LayoutMergeService` 增量入口。
6. 工作台 AI/RULE 切换、可清空模板选择、对比预览、低置信度/降级提示、确认应用。
7. **一键排版**（FOOTBALL_AI）：四套预设 UI + V204 seed + free-only apply + `LayoutMergeFidelityGate`（见 PRD FR-M2-012-7 · ADR-027 §4.3）。
8. TESTCASES P0 39 条对应自动化与 legacy 回归。

### 2.2 Out of Scope

- `styleHints` 段内高亮（Phase 1.5+，不计 MVP DoD）。
- AI 改写、润色、补全、重排正文字符。
- NL 参数调整、批量/定时排版、非 ARTICLE、135/秀米集成。
- 修改模板 CRUD 业务、下线 ADR-027 规则链、扫描 DB catalog 自动选模板。
- LLM 最终选模板或 LLM tie-break。
- 并发 apply 的版本锁、idempotency key 或新错误码。
- M10、外部 SSO、登录页及其他 Slice/Gate 工作。

---

## 3. 五段式实现 Prompt

### 第 1 段：上下文（Context）

你是负责 Football OPS M2 内容生产的资深工程师。技术栈为 Java 17、Spring Boot 3、MyBatis Plus、MySQL 8、Vue 3/TypeScript、JUnit 5 与 Playwright。当前只实现 **S-21a AI 排版**。先读取 §1 的全部 `@` Spec，并确认上一阶段 Gate 状态；若前置 Gate 未满足，只报告阻塞，不跨 Gate 实现。

### 第 2 段：任务定义（Task）

- Slice ID：`S-21a`
- FR：FR-M2-012-1~6
- AC：AC-M2-012-1~15、17、19~20；AC-16/18 的 `styleHints` 不在 MVP
- API：API-M2-012-01~07、09~11（08 为 Phase 1.5+）
- 测试：TC-M2-012-P0-01~39
- 完成 TEMPLATE_GUIDED/AUTO、正文保真、确定性模板决策、默认映射降级、预览/应用与 legacy 回归。

### 第 3 段：约束清单（Constraints）

1. 正文 `body` 在 apply 前后逐字符一致；LLM 不输出 HTML SSOT。
2. LLM 只提取 `SemanticSegment[]`；模板分数与选择只能由 ADR-028 §2.4.1 的代码规则产生。
3. 同分/分差 1 固定 `decision-scan`；不得增加随机性、模型 tie-break、DB catalog 或 legacy fallback。
4. `styleHints` 不进入 MVP 实现、测试阻断项或 DoD。
5. 强关联/字典/tenant_id/错误码遵守项目五大铁律；`@PreAuthorize` 与 DB 权限生效。
6. AUTO 按当前租户查询内置 seed；不得跨租户读取。初始 SQL seed 延续仓库 PRESET 既有口径，仅幂等写入 `tenant_id=1`；其他租户缺 seed 返回 2042。
7. 不改 `pom.xml`、公共组件、应用配置或本 Slice 清单外文件；确需新增依赖或字段时停止并补 ADR/Spec。
8. 发现迁移版本已占用时，实施前选择当时下一个可用版本并同步本 Slice/Checklist，不覆盖已有迁移。

### 第 4 段：输出要求（Deliverables）

按 §4 文件白名单交付可编译后端、可 typecheck 前端、幂等 seed、单元/IT/Playwright 测试。preview 不写库；apply 只写既有版式字段与 `body_format=LAYOUT`。响应严格符合 API v1.1，包含可复算的 `templateDecision`，降级时包含 `degradedSegments` 与 `mappingWarnings`。

### 第 5 段：自检清单（Self-Check）

- [ ] 字段、枚举、错误码与 API v1.1 完全一致
- [ ] decider 精确分值、边界、去重、同分/分差 1 单测通过
- [ ] Fidelity Gate 增删字/换序/CRLF/空白测试通过，DB body 原串不变
- [ ] 权限、数据范围、tenant_id 与跨租户测试通过
- [ ] 两款 seed 与 M8 prompt 通过 SeedVerificationIT + 人工抽检
- [ ] TEMPLATE_GUIDED/AUTO preview→apply 与 RULE legacy Playwright 通过
- [ ] CHECKLIST §1~10 100%，P0 39/39，相关 `mvn verify` / lint / typecheck / unit / Playwright 无失败
- [ ] 未实现 §2.2 项，未宣称阶段 Gate 通过

---

## 4. 文件范围白名单

以下为实施期允许修改/新增的计划范围；精确包名遵循现有 `football.module.ops` 结构。标注“计划新增”的文件尚不存在，不得据此扩改相邻模块。

### 4.1 后端

| 类型 | 允许文件 |
|------|----------|
| Controller | 修改 `football-backend-saas/football-module-ops/football-module-ops-server/src/main/java/football/module/ops/controller/content/ProductionContentController.java` |
| 现有服务 | 修改 `.../service/content/ProductionContentService.java`、`ProductionContentServiceImpl.java`、`LayoutMergeService.java`、`AiLlmInvokeSupport.java` |
| 计划新增服务 | `.../service/content/AiSemanticSegmentService.java`、`TypesetFidelityGate.java`、`FootballLayoutPipeline.java`、`FootballTemplateDecider.java`、`SegmentSlotMapper.java` |
| DTO（计划新增） | `.../api/dto/content/ContentAiSemanticTypesetPreviewReq.java`、`ContentAiSemanticTypesetApplyReq.java`、`ContentAiSemanticTypesetPreviewResp.java`、`SemanticSegment.java`、`FootballTemplateDecision.java`、`DegradedSegment.java` |
| 模板读取 | 仅在必要时修改 `.../service/content/WechatLayoutTemplateService.java`、`WechatLayoutTemplateServiceImpl.java`、`.../dal/mysql/content/WechatLayoutTemplateMapper.java`；不改模板 CRUD 语义 |
| 错误码 | 仅修改 OPS 内容模块既有错误码常量文件，加入 API 已批准的 2037/2039/2041/2042（若尚不存在） |

### 4.2 前端

| 类型 | 允许文件 |
|------|----------|
| 工作台 | `football-front/apps/web-ele/src/components/ops/layout/LayoutResourceSidebar.vue`、`TypesettingPanel.vue`、`TypesetCompareDialog.vue` |
| 模板选择 | 仅必要时修改 `.../components/ops/layout/LayoutTemplateSelectDialog.vue` 以支持可清空 |
| API/types | `football-front/apps/web-ele/src/api/ops/typesetting.ts`；可在同目录计划新增 `aiSemanticTypesetting.ts` |
| 编辑器集成 | 仅必要时修改 `football-front/apps/web-ele/src/views/ops/production/content/ContentEditPanel.vue` |

### 4.3 SQL / seed

| 交付 | 允许文件 |
|------|----------|
| 两款 PRESET + M8 prompt | 计划新增 `football-backend-saas/football-module-ops/football-module-ops-server/src/main/resources/db/migration/V198__m2_ai_semantic_typeset_seed.sql`；实施时先确认 V198 未占用 |
| 生产增量同步 | 若项目交付规范要求，同步新增 `docs/deploy/ops-greenfield-production/sql/` 下同名增量脚本；不得改历史 Flyway |
| Schema | **不新增业务表或列**；复用 `oa_wechat_layout_template`、既有内容版式字段和 M8 prompt 配置结构 |

### 4.4 测试

| 层 | 允许/计划文件 |
|----|---------------|
| 后端单测 | 计划新增 `.../src/test/java/football/module/ops/service/content/FootballTemplateDeciderTest.java`、`TypesetFidelityGateTest.java`、`SegmentSlotMapperTest.java`、`AiSemanticTypesetServiceTest.java` |
| 后端 IT | 计划新增 `.../src/test/java/football/module/ops/controller/content/AiSemanticTypesetIT.java`；扩展现有 `SeedVerificationIT`（若实际类名不同，先定位后只改该 seed 验证类） |
| 前端组件测试 | 在 layout 组件相邻目录新增/修改 `*.test.ts`，覆盖模式、低置信度、降级与错误提示 |
| E2E | 计划新增 `football-front/apps/web-ele/tests/content-ai-semantic-typeset.spec.ts` |

### 4.5 文档

实施后只允许回填本 Slice、ADR-028、AI 排版 PRD/UX/API、AI 排版 TESTCASES/CHECKLIST及 Gate 要求的证据/报告。不得借此改其他模块 Spec。

---

## 5. 实现顺序与分步交付

| 步骤 | 内容 | 分步 DoD |
|-----:|------|----------|
| 1 | 迁移版本检查；两款 PRESET 与 M8 prompt 幂等 seed | SeedVerificationIT + schema JSON 可解析 |
| 2 | DTO、taxonomy 校验、Fidelity Gate | 2039/2037 与保真单测绿 |
| 3 | `FootballTemplateDecider` 特征/评分/审计输出 | TC-P0-34~38 精确断言全绿 |
| 4 | TEMPLATE_GUIDED/AUTO pipeline、seed 解析、mapper 降级、merge | TC-P0-01~19 服务测试绿 |
| 5 | preview/apply、权限、数据范围、租户、overwrite | TC-P0-20~27 IT 绿 |
| 6 | 工作台 AI/RULE、模板可清空、对比、低置信度/降级提示 | TC-P0-28~32、39 组件/E2E 绿 |
| 7 | legacy 回归与整片验证 | TC-P0-33；Checklist §1~10 100%；相关命令全绿 |

每一步可独立提交评审，但不得在步骤 7 完成前标记 Slice 完成。

---

## 6. 追溯映射

| 交付 | FR / AC | API | TC | Checklist |
|------|---------|-----|----|-----------|
| TEMPLATE_GUIDED | FR-1；AC-1~4 | 01、03 | P0-01、02、08、11 | §2、§3 |
| AUTO + seed 路由 | FR-2；AC-5~7 | 02、11 | P0-03~05、19 | §4、§7 |
| 确定性 decider | FR-2；AC-19~20 | 09、10 | P0-34~39 | §4、§6、§9 |
| Fidelity Gate | FR-3；AC-8~9 | 04 | P0-13~15 | §3 |
| preview/apply/overwrite | FR-4；AC-10~12 | 01、05 | P0-06、20~23 | §2、§6 |
| taxonomy + 映射降级 | FR-5/6；AC-13~15、17 | 07 | P0-15~18 | §3、§5 |
| 权限/租户 | 横切 | 06 | P0-24~27 | §8 |
| legacy 回归 | ADR-027 | 既有 API | P0-28、33 | §6、§9 |
| styleHints | AC-16/18 | 08 | P1-02/03 | §11，**不计 MVP** |

---

## 7. DoD、回滚与 Gate 声明

### Slice DoD

- `CHECKLIST-M2-AI排版增量.md` **§1~§10 100%**；§11 Phase 1.5+ 不计入。
- `TESTCASES-M2-AI排版增量.md` **P0 39/39（100%）**，失败数 0。
- 后端相关范围 `mvn verify` 无失败；前端 lint/typecheck/unit 与目标 Playwright 无失败。
- 两款模板与 M8 prompt 通过 SeedVerificationIT 和人工抽检；ADR-020/027 相关 P0 回归仍绿。
- 归档必要证据；仅当所属阶段全部 Gate 条件满足并有正式 Gate 报告时，才可更新阶段状态。

### 回滚

- 前端回滚后保留 ADR-027 RULE 入口。
- 后端回滚不得破坏既有 `layout_json/layout_html/body` 读取；新增 API 可随版本撤回。
- seed 回滚只处理本 Slice 两款 PRESET 与 prompt；若模板已被内容 `layout_template_id` 引用，不删除记录，改为停用并保留历史可读性。
- 任意回滚前后 `body` 原始字符串必须不变。

### 当前声明

S-21a **核心实现已完成**（API、语义管线、decider、renderer、工作台 AI/RULE、seed V198+），但 **Slice DoD 与阶段 Gate 均未通过**（见 §8）。

---

## 8. 收尾状态（2026-09-11）

### 8.1 已完成

| 域 | 交付 |
|----|------|
| 后端 | `ai-semantic/preview|apply`、Fidelity Gate、FootballTemplateDecider、SegmentSlotMapper、FootballLayoutSemanticRenderer（decision-scan + analysis-report 全组件）、free+paid `combineTypesetBodyText`、2037 对齐修复 |
| 前端 | AI/RULE 切换、模板可清空→AUTO、对比预览/apply、低置信/降级/错误文案、Vitest **7/7** |
| Seed | V198 两款 PRESET + `AI_TYPESET_SEMANTIC`（tenant 1 幂等） |
| 单测 | S-21a 聚焦 **83/83** 绿（IT 8、service 5、renderer 34、emphasis 6、decider 5、fidelity 8、mapper 4、body 7、prod 3、权限 3） |

**analysis-report 组件（subagent 0e8d60cb）**：highlight-box、analysis-card 合并、QUOTE/ORDERED_LIST/IMAGE/DIVIDER/DECORATIVE_SLOGAN、styleHints 渲染路径、多场 TEAM_VS 分组 — **均已实现并有单测**。

### 8.2 未完成 / 阻塞

| 项 | 说明 |
|----|------|
| P0 | **37/39**（自动 33 + 人工 4）；P0-30 AUTO-only N/A；**P0-32 Playwright E2E 待补** |
| Checklist §1~§10 | **45/111 ≈ 41%**（Fidelity/decider/单测/部分冒烟项已勾选） |
| `SeedVerificationIT` | **未新增/未跑**（仓库无该类） |
| `AiSemanticTypesetIT` | **8/8 绿**（P0-07/11/25） |
| Playwright | `content-ai-semantic-typeset.spec.ts` **不存在** |
| `mvn verify`（ops-server） | 202 条中 **8 失败 + 1 错误**（WorkTask / WechatArticleHtmlFetcher，**非 S-21a**；本轮未重跑） |
| 联调冒烟 | **2026-09-11 session 2**：栈 UP；9448/9449 真实 LLM AUTO preview code=0 |
| S-21b-2 styleRef 克隆 | **Phase 2 / 独立 Slice**，不计 S-21a MVP DoD |

### 8.3 Gate 结论

**不可宣称 Slice DoD 或阶段 Gate 通过。** 下一优先：新增并跑通 Playwright P0-32、补 `SeedVerificationIT`、Checklist §1~§10 100%、ops-server 全量 verify 无失败（或明确豁免范围）、正式 Gate 报告 Sign-off。

---

*Implemented / Wrap-up · 2026-09-11*
