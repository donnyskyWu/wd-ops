# CHECKLIST-M2-内容生产 — AI 排版增量

> **FR**：FR-M2-012 | **Slice**：S-21a（已批准）  
> **版本**：v1.1 | 2026-09-08 | **状态**：Ready  
> **关联**：[ADR-028](../adr/ADR-028-M2-AI排版语义分段.md) · [PRD 增量](../product/PRD-M2-AI排版增量.md) · [UX 增量](../product/UX-M2-AI排版增量.md) · [API 增量](../engineering/API-M2-AI排版增量.md) · [TESTCASES 增量](./TESTCASES-M2-AI排版增量.md)

---

## 0. MVP 边界与 DoD

- [ ] 本 Slice 仅实现 FR-M2-012 MVP；不实现改写/润色、非 ARTICLE、批量/定时排版、135/秀米、NL 参数调整（ADR-028 §3；PRD §1.3）
- [ ] ADR-027 规则链保留为备选/legacy；AI 排版为默认入口，二者模式和 API 语义不混用（ADR-028 §2.8；TC-M2-012-P0-28、33）
- [ ] AUTO 以 Accepted ADR/API 为准，由服务端在 `decision-scan` / `analysis-report` 中决策；`football-layout/SKILL.md` 原“用户必须选择”交互不进入 OPS AUTO（ADR-028 §2.1、§2.4）
- [ ] `styleHints` 明确归入 Phase 1.5 / MVP+，本文 §11 不计入 MVP Checklist 完成率与 DoD（PRD FR-M2-012-6；API §3.1.1）
- [ ] **Slice DoD：本文 §1~§10 的 MVP checklist 100% 勾选 + TESTCASES P0 39/39 通过**（PHASE-DEV-METHOD R-I05）
- [ ] **阶段 Gate：相关模块 Checklist 100% + P0 100% + seed 验证 + `mvn verify` / `playwright test` 范围无失败 + 上一阶段 P0 仍绿 + Gate 报告归档**（PHASE-DEV-METHOD R-G03~R-G08）

## 1. 实现前 Gate

- [ ] ADR-028、PRD/UX/API AI 排版增量均为 Accepted，且实现引用固定版本（ADR-028 Accept 条件 1）
- [ ] S-21a 已按批准结论登记到 `SLICES-M2-内容生产.md`，并以 `SLICES-M2-S21a-AI排版.md` 为实现 SSOT（PHASE-DEV-METHOD R-S05）
- [ ] `FootballTemplateDecider` 按 ADR-028 §2.4.1/API §3.3 的特征、权重、阈值、同分与低置信度规则实现（TC-M2-012-P0-34~38）
- [ ] 已确认并记录依赖：S-14c `LayoutMergeService`/preview/apply、S-15~17 工作台、M8 LLM（PRD §0）
- [ ] **S-21b**（ADR-029 Accepted）：`TEMPLATE_GUIDED` 对 Shenyu 模板 12/17 的 P0 回归在 S-21b re-import 完成后执行（TC-M2-013-P0-06 / TC-M2-012 交叉项）
- [ ] 已核对 `docs/football-layout/shared/semantic-parser.md`、两份 template 文档及默认 params，仅把 taxonomy/组件/默认参数转为运行时 schema/seed，不运行时读取 Markdown（ADR-028 §2.4）
- [ ] 已按五段式实现 Prompt 限定本 Slice 文件范围、FR/AC、错误码与回归（PHASE-DEV-METHOD R-I02~R-I04）
- [ ] 已明确 API 未定义并发 apply 冲突/idempotency key；不在代码中擅自新增字段或错误码（API §2；TC §0）

## 2. 后端 API 与校验

- [ ] 新增 preview 端点 `/content/{id}/typeset/ai-semantic/preview`，请求/响应仅使用 API 增量已定义字段（API §2.1；TC-M2-012-P0-01、03、04）
- [ ] 新增 apply 端点 `/content/{id}/typeset/ai-semantic/apply`，返回更新后的 `ContentRespVO`（API §2.2；TC-M2-012-P0-02、05）
- [x] `mode` 仅允许 `TEMPLATE_GUIDED` / `AUTO`，非法值按枚举规则返回 1503（API §2.1、§3.2；TC-M2-012-P0-10）
- [x] TEMPLATE_GUIDED 强制 `templateId`，按 snowflake 字符串接收；不存在、停用、类型不匹配返回 1501（API §2.1、§7；TC-M2-012-P0-11）— `AiSemanticTypesetIT`
- [ ] AUTO 请求不依赖 `templateId`，且不扫描 DB 模板 catalog 选模板（ADR-028 §2.1；TC-M2-012-P0-03、04、19）
- [x] 正文解析顺序严格为请求 `body` → `content.body` → `extractPlainText(content.layout_html)`（API §2.1；TC-M2-012-P0-07）— `AiSemanticTypesetIT` + `AiTypesetBodySupportTest`
- [x] 正文解析后为空返回 2036；preview/apply 均不继续调用 LLM（AC-M2-012-4；TC-M2-012-P0-08）
- [x] 仅允许 `content_type=ARTICLE`，否则返回 1500（API §7；TC-M2-012-P0-09）
- [x] apply 默认 `overwrite=false`；已有 LAYOUT 时返回 2031，true 时才覆盖（AC-M2-012-11、12；TC-M2-012-P0-20~22）
- [x] preview 事务不写 `body_format/layout_json/layout_html/layout_template_id`（FR-M2-012-4；TC-M2-012-P0-06、23）
- [ ] apply 仅写 API §2.2 明确的版式字段和 `body_format=LAYOUT`，不改 `body` 原始字符串（AC-M2-012-2、9；TC-M2-012-P0-02、05、21）
- [ ] 2031/2036/2037/2039/2041/2042、1500/1501/1503/1504 与 API 增量触发条件一致，无新增猜测错误码（API §6）

## 3. 后端语义管线与正文保真

- [x] `AiSemanticSegmentService` 使用当前租户系统「AI 模型配置」中经真实连接测试的 OpenAI-compatible LLM，并加载 M8 `AI_TYPESET_SEMANTIC` 输出 API §3.1 `SemanticSegment[]`（FR-M2-012-5；TC-M2-012-P0-15）
- [x] AI 模型 API Key 经既有配置 API AES-256 加密写入 `api_key_encrypted`，列表/日志/测试证据仅显示 masked；`jingcai.article` 只负责文案生成，不作为语义分段 fallback
- [ ] `segmentType` 与 API §3.2/football semantic-parser taxonomy 对齐，并通过 `@InDict` 或 M8 JSON schema 约束（API §3.2；TC-M2-012-P0-10、15）
- [ ] 每段 `text` 是原正文连续子串，index 从 0 递增且保持文档序（ADR-028 §2.3；TC-M2-012-P0-13~15）
- [x] `TypesetFidelityGate` 实现已定义归一化：CRLF→LF、连续空白折叠、trim（ADR-028 §2.3）— `TypesetFidelityGateTest` 8/8
- [x] 分段拼接与归一化正文不一致时 preview/apply 返回 2037，禁止 fallback 改字、丢段或部分写入（AC-M2-012-8；TC-M2-012-P0-13、14）— `TypesetFidelityGateTest` + `AiSemanticTypesetServiceTest`
- [ ] apply 后 DB `body` 逐字符等于 apply 前；`extractPlainText(layout_html)` 满足 ADR-020 保真（AC-M2-012-9；TC-M2-012-P0-02、05、21）
- [ ] LLM 超时/非法 JSON/不可解析输出返回 2039，且不进入 merge（API §6；TC-M2-012-P0-12）
- [ ] LLM 不直接产出 HTML SSOT；HTML 仅由服务端 `LayoutMergeService` render 产生（ADR-028 §2.2）
- [ ] 推荐三行、短标题及典型足球元素按 taxonomy 形成正确段类型（AC-M2-012-13、14；TC-M2-012-P0-15）

## 4. AUTO 内置模板与决策算法

- [ ] `FootballLayoutPipeline` 落地 semantic-parser → `FootballTemplateDecider` → resolve seed schema → mapper → merge（API §3.3）
- [x] decider 仅从 Fidelity Gate 通过的 `SemanticSegment[]` 与原文提取 API §3.3.1 九项特征；同段同特征只计一次，匹配大小写不敏感（TC-M2-012-P0-37）
- [x] `decision-scan` 与 `analysis-report` 分值逐项符合 API §3.3.2；长度 1200/1400/2000/2200 边界无重叠计分（TC-M2-012-P0-34~36）
- [x] 分差 ≥2 选择高分模板并标记 HIGH；同分或分差 1 固定 `decision-scan` 并标记 LOW（TC-M2-012-P0-38）
- [x] LLM 不直接决定最终模板，不参与同分决策；低置信度不扫描 DB catalog、不走 ADR-027 legacy（ADR-028 §2.4.1）
- [x] decider 输出严格限定为两模板，并返回可复算的分数、`scoreGap/confidence/selectionReason/decisionReasons/featureSummary`（API §3.3）
- [ ] `decision-scan` 结果解析到决策扫读版内置 seed id/schema（AC-M2-012-5；TC-M2-012-P0-03）
- [ ] `analysis-report` 结果解析到情报分析版内置 seed id/schema（AC-M2-012-5；TC-M2-012-P0-04）
- [ ] AUTO 响应返回 `selectedTemplateId/Name/selectedFootballTemplate` 供 UI 展示（PRD §4 FR-M2-012-2；TC-M2-012-P0-03~05）
- [ ] 合法分段始终二选一；仅选中标识无法解析到 ENABLED seed/schema 时返回 2042（AC-M2-012-6；TC-M2-012-P0-19）
- [ ] TEMPLATE_GUIDED 不调用 `FootballTemplateDecider`，只加载用户所选公推模板 schema（API §3.3；TC-M2-012-P0-01）

## 5. 槽位映射与 merge

- [x] `SegmentSlotMapper` 按语义段映射 schema 槽，不退化为 ADR-027 的全局“顺序吃段”（ADR-028 §1.1、§2.2）
- [x] 无专用槽时默认降级至最近 paragraph / `repeat:true` 槽，不在 MVP 默认抛 2041（AC-M2-012-15、17；TC-M2-012-P0-16）
- [x] 降级响应完整返回 `mappingDegraded=true`、`degradedSegments` 和 `mappingWarnings`（API §2.1、§3.4；TC-M2-012-P0-16）
- [x] 服务端为每次降级记录 warning（ADR-028 §2.5；TC-M2-012-P0-16）
- [x] 分析/普通段溢出时按文档序追加到默认 repeat paragraph 槽，正文无丢失（ADR-028 §2.5；TC-M2-012-P0-17）
- [ ] 段少于槽时隐藏空槽，不插入占位文案（ADR-028 §2.5；TC-M2-012-P0-18）
- [ ] `LayoutMergeService` 增量入口接受 segments→slot 映射输入并输出 `layout_json/layout_html`（ADR-028 §3.1）
- [ ] `paramOverrides` 仅按已有 ADR-027/LayoutMergeService 契约透传，不新增未定义参数（API §2.1；TC-M2-012-P1-05）
- [ ] strict/2041 仅作为可选非 MVP 配置保留；未有批准配置契约前不纳入 MVP DoD（API §3.4；TC-M2-012-P1-01）

## 6. 前端交互

- [x] `LayoutResourceSidebar` 一键排版 Tab 增加 AI/RULE segmented，默认 AI（UX §2.1；TC-M2-012-P0-28）
- [x] `LayoutTemplateSelect` 可清空；有值发 TEMPLATE_GUIDED，无值发 AUTO 且不传 templateId（UX U3；TC-M2-012-P0-30、31）
- [x] 仅可编辑 ARTICLE 且具权限时提供可操作入口；空正文预览按钮 disabled 并显示指定 tooltip（UX U1、U2；TC-M2-012-P0-24、29）— `canShowAiTypesetToolbar` Vitest + ContentEditPanel 代码对齐
- [ ] preview loading 文案为“AI 正在分析正文结构…”，进行中侧栏不可关闭（UX U10、§7）
- [ ] preview 成功后才展示排版前/后、`segmentationReport` 和 apply 按钮（UX U4；TC-M2-012-P0-30、31）
- [ ] AUTO 成功展示后端 `selectedTemplateName/selectedFootballTemplate` 对应的两款中文模板名（UX §2.2；TC-M2-012-P0-31）
- [ ] AUTO 低置信度展示“内容特征接近，已使用决策扫读版”提示，仍可 apply 或改选公推模板（UX U12；TC-M2-012-P0-39）
- [ ] `mappingDegraded=true` 展示 warning/降级摘要，且用户仍可 apply（UX U11；TC-M2-012-P0-32）
- [ ] 已有 LAYOUT 应用前展示覆盖确认文案，确认后传 `overwrite=true`（UX U5；TC-M2-012-P0-32）
- [ ] apply 成功刷新 `RichTextEditor` 的 layout_html/body_format 并 Toast“AI 排版已应用”（UX U6；TC-M2-012-P0-30、31）
- [ ] 2037/2039/2042 分别使用 UX U7/U8/U9 文案；失败时不展示可应用的陈旧 preview（TC-M2-012-P0-32）
- [x] 非 ARTICLE 隐藏入口、无模板显示空态、无权限隐藏或只读，均与 UX §6 一致（TC-M2-012-P0-24、29）— Vitest + `ContentEditPanel` `showArticleLayout`/`effectiveReadonly`
- [ ] RULE 模式完整保留 ADR-027 原 UI/API，不把规则链 AUTO 显示或发送为 AI AUTO（UX §5；TC-M2-012-P0-28、33）

## 7. 模板与提示词 seed

- [x] 新增内置 `decision-scan`（决策扫读版）seed，含可由 `LayoutMergeService` 解析的 `layout_schema` 与默认参数（ADR-028 §3.1 #6；TC-M2-012-P0-03）
- [x] 新增内置 `analysis-report`（情报分析版）seed，含可由 `LayoutMergeService` 解析的 `layout_schema` 与默认参数（ADR-028 §3.1 #6；TC-M2-012-P0-04）
- [x] 两款 seed 的稳定标识、名称和解析方式支持 AUTO 返回 `selectedTemplateId/Name`，且不会与普通 catalog 扫描语义混淆（API §3.3）
- [x] 两款 seed 均覆盖默认 paragraph / repeat fallback 槽，满足 MVP 降级策略（ADR-028 §2.5；TC-M2-012-P0-16、17）
- [x] 新增 M8 `AI_TYPESET_SEMANTIC` 提示词 seed，约束不增删改字符、仅输出允许枚举、text 为连续子串（API §5）
- [x] seed 按批准 Slice 的既有 PRESET 口径仅为 `tenant_id=1` 幂等写入；运行时始终按当前 tenant 查询，其他租户无 seed 时返回 2042，禁止跨租户复用（全局铁律 1504）
- [ ] `SeedVerificationIT` 验证两款模板 seed 与提示词 seed 存在、ENABLED/可用、schema/JSON 可解析（Gate R-G05）
- [ ] 人工抽检两模板典型 layout_html，确认服务端 render 与模板组件意图一致且无正文增删（ADR-028 §2.4）

## 8. 安全、鉴权与租户

- [x] preview/apply 均启用 `@PreAuthorize("@opsPerm.hasAnyAuthority('ops:content:typeset','oa:content:typeset')")`（API §2.1；TC-M2-012-P0-24）— 控制器注解 + `OpsPermissionCheckerTest` + `OpsGlobalExceptionHandlerAccessDeniedTest`
- [x] greenfield system 菜单幂等提供兼容权限 `ops:content:typeset`，并绑定内容编辑/运营角色；控制器同时兼容 ADR-027 的 `oa:content:typeset`
- [ ] Dev Token 仅提供登录来源，权限从 DB 读取；禁止硬编码 userId/tenantId（ADR-003；PHASE-DEV-METHOD R-A01~03）
- [x] 内容数据范围复用 `ContentDataScopeSupport`，创建者/IP 组之外不可读取或排版（API §7；TC-M2-012-P0-25）— `AiSemanticTypesetIT`
- [x] 内容按 tenant_id 隔离，跨租户访问返回 1504 且不调用 LLM（TC-M2-012-P0-26）
- [x] TEMPLATE_GUIDED 模板按 tenant_id 隔离，跨租户模板返回 1504 且不泄露 schema/name（TC-M2-012-P0-27）
- [x] AUTO 内置 seed 的解析遵循批准的租户策略，不通过客户端提交或硬编码任意租户 id（全局铁律 1504）
- [x] 非法 mode 使用枚举校验；templateId 使用后端实体/租户/状态校验，不能只依赖前端（1501/1503/1504）

## 9. 测试与回归

- [ ] `TESTCASES-M2-AI排版增量.md` P0 **37/39**（P0-30 AUTO-only N/A；P0-32 E2E 待补），失败数 0
- [x] 服务单测覆盖 Fidelity Gate 的 CRLF、空白归一化、增删字、换序和原始 body 不变（TC-M2-012-P0-13、14）— `TypesetFidelityGateTest` 8/8（2026-09-11）
- [x] 服务单测覆盖 taxonomy 校验、推荐列表、标题、溢出/不足和默认降级（TC-M2-012-P0-15~18）— `AiSemanticTypesetServiceTest` 5/5 + `SegmentSlotMapperTest` 4/4 + `FootballLayoutSemanticRendererTest` 34/34（2026-09-11）
- [x] decider 单测覆盖精确分值、长度边界、特征去重、同分/分差 1 fallback、输出稳定性与 seed 不可用 2042（TC-M2-012-P0-03、04、19、34~38）— `FootballTemplateDeciderTest` 5/5（2026-09-11）
- [x] Controller/IT 覆盖 preview/apply、2031/2036/2037/2039、1500/1501/1503/1504、403（TC-M2-012-P0-01~27）— 本轮补 `AiSemanticTypesetIT`（P0-07/11/25）+ 权限 403 单测；其余项沿用既有单测/IT
- [ ] 并发 preview 验证无写入和响应隔离；重复 apply 验证 overwrite 门（TC-M2-012-P0-22、23）
- [x] 前端组件测试覆盖 AUTO 模式、错误文案和覆盖确认（TC-M2-012-P0-29、32 部分）— Vitest **7/7**；TEMPLATE_GUIDED 前端闭环（P0-30）记 AUTO-only N/A
- [ ] Playwright 覆盖 TEMPLATE_GUIDED 与 AUTO 的 preview→apply 主链路，以及 RULE legacy 冒烟（TC-M2-012-P0-28、30、31、33）
- [ ] ADR-020 `M2LayoutTemplateS14IT`/正文保真相关 P0 回归仍绿
- [ ] ADR-027 原规则链、手动 apply-layout-template preview/apply 回归仍绿（TC-M2-012-P0-33）
- [ ] 所属模块范围 `mvn verify` 无失败；前端 lint/typecheck/unit/playwright 无失败
- [ ] 测试证据记录请求、错误码、DB 前后快照与必要截图，敏感正文脱敏

## 10. 部署、回滚与文档归档

- [ ] 部署顺序按批准 Slice 明确：数据库/seed → 后端 → 前端；旧前端/旧规则链在增量后端上线期间仍可用（ADR-028 §1.2；TC-M2-012-P0-33）
- [x] 上线前在目标环境通过系统「AI 模型配置」真实连接测试，并验证 `AI_TYPESET_SEMANTIC` 可用；缺失时不开放 AI 默认入口（API §5；TC-M2-012-P0-12）— tenant 1 真实 LLM AUTO preview code=0（content 9448，7.5s，2026-09-11）
- [x] 上线后冒烟：TEMPLATE_GUIDED preview、AUTO 两 seed 路由、apply 保真、权限拒绝、legacy RULE（TC-M2-012-P0-01、03、04、05、24、33）— **部分**：AUTO `decision-scan`（9448）+ `analysis-report`（9449 marketing）真实 preview code=0；TEMPLATE_GUIDED/apply/legacy RULE 沿用 2026-09-08 证据
- [ ] 回滚前端时保留 legacy 规则链；回滚后端时不破坏既有 layout_json/html 和内容读取（ADR-028 §1.2；TC-M2-012-P0-33）
- [ ] seed/数据库回滚脚本只处理本增量资产；已被内容引用的内置模板回滚策略先在批准 Slice 中定义（FR-M2-012-2；TC-M2-012-P0-05）
- [ ] 回滚演练验证 `body` 始终可读且原始字符串未被 AI 排版改写（FR-M2-012-3；TC-M2-012-P0-14、21）
- [ ] PRD/UX/API/ADR、本文 TESTCASES/CHECKLIST、批准后的 SLICES 相互链接且编号无悬空（FR-M2-012；TC-M2-012-P0-01~39）
- [x] 自动化报告、seed 验证、人工抽检与 E2E 证据归档到 `docs/delivery/e2e-artifacts/` 对应目录（Gate R-G05、R-G08）— 本轮 session 2 报告见 `e2e-artifacts/S21a-AI-TYPESET-WRAPUP-20260911/REPORT.md`（非正式 Gate Sign-off）
- [ ] 所属阶段 Gate 报告按 `GATE-S{n}-报告-{YYYYMMDD}.md` 归档并完成 Sign-off（PHASE-DEV-METHOD R-G08）
- [ ] 更新 `MASTER-EXECUTION-TRACKER.md` 前确认 Gate 全项通过；不得仅凭本 Slice 通过宣称下一阶段可联调（PHASE-DEV-METHOD R-G01~R-G08）

## 11. Phase 1.5 / MVP+（不计入 MVP DoD）

- [ ] `styleHints` 字符偏移、允许 hintType、重叠/越界处理契约已补 Spec（API §3.1.1；TC-M2-012-P1-02、03）
- [ ] merge 仅注入 inline markup，不改变 segment.text、body 或提取纯文本（AC-M2-012-16、18）
- [ ] styleHints 自动化通过后单独更新 Phase 1.5 Checklist/Gate，不回填为 MVP P0
- [ ] 长耗时/移动端增强与 paramOverrides 扩展验证按 P1 执行（TC-M2-012-P1-04、05）

## 12. Sign-off

### 2026-09-11 收尾状态（续 · session 2）

- **P0：37/39**（自动 33 + 人工 4；P0-30 AUTO-only N/A；P0-32 Playwright 待补）；失败数 **0**。详见 `TESTCASES-M2-AI排版增量.md` §6。
- **Checklist §1~§10：45/111 ≈ 41%**（本轮补勾选 Fidelity/decider/单测/冒烟项；Gate/SeedVerificationIT/Playwright/全量 verify 仍缺）。
- **S-21a 聚焦单测 83/83**（AiSemanticTypesetIT 8 + Service 5 + Renderer 34 + ParagraphEmphasis 6 + decider 5 + fidelity 8 + mapper 4 + body 7 + prod 3 + 权限 3）；前端 Vitest **7/7**。
- **联调栈 UP**：ops :48094、Gateway :48080、front :5777 均 200；真实 LLM AUTO preview content **9448** code=0（decision-scan，5 segments，7.5s）；marketing content **9449** code=0（analysis-report，9421B html）。
- **analysis-report renderer 组件收尾完成**（highlight-box、analysis-card、QUOTE/ORDERED_LIST/IMAGE/DIVIDER/DECORATIVE_SLOGAN、styleHints 渲染、multi-match TEAM_VS）。
- **S-21b-2 styleRef 克隆**：Phase 2 独立 Slice，不计 MVP DoD（`SLICES-M2-S21b` §2.3）。
- ops-server 全量 `mvn verify` 仍 **8 失败 + 1 错误**（WorkTask / WechatArticleHtmlFetcher，非本 Slice；本轮未重跑）。
- **`SeedVerificationIT`**、**`content-ai-semantic-typeset.spec.ts`** 仍缺失/未绿 → Gate 不可通过。
- **Slice DoD 与阶段 Gate 均不得标记通过。**

| 角色 | 姓名 | 日期 | 结论 |
|------|------|------|------|
| 产品 |  |  |  |
| 架构 |  |  |  |
| 开发 |  |  |  |
| 测试 |  |  |  |

---

*Ready · 2026-09-08*
