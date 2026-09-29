# TESTCASES-M2-内容生产 — AI 排版增量

> **FR**：FR-M2-012 | **Slice**：S-21a（已批准）  
> **版本**：v1.2 | 2026-09-17 | **状态**：Ready  
> **优先级**：P0 阻断 Slice/Gate；P1 不阻断 MVP  
> **SSOT**：[ADR-028](../adr/ADR-028-M2-AI排版语义分段.md) · [PRD 增量](../product/PRD-M2-AI排版增量.md) · [UX 增量](../product/UX-M2-AI排版增量.md) · [API 增量](../engineering/API-M2-AI排版增量.md) · [ADR-020](../adr/ADR-020-M2-公推模板版式套用语义.md)

---

## 0. 范围与判定口径

- MVP 覆盖 `TEMPLATE_GUIDED`、`AUTO`、preview/apply、语义分段、正文保真、默认映射降级、两款内置模板 seed、权限/数据范围/租户隔离、ADR-027 legacy 规则链回归，以及 **FOOTBALL_AI 一键排版**（四套预设、body-first、free-only apply、实体解码、无 LLM）。
- AUTO 按 **Accepted ADR-028/API 增量**执行：服务端 `FootballTemplateDecider` 在 `decision-scan` / `analysis-report` 间决策。`docs/football-layout/SKILL.md` 中“用户必须选择模板”的交互规则不适用于本 API；其 semantic-parser、模板组件和默认参数仍作为参照。
- `styleHints` 是 Phase 1.5 / MVP+，仅列 P1，不计入 MVP DoD。
- API 未定义 idempotency key、版本号或并发 apply 冲突策略；MVP 仅验收已定义的 preview 无写入、`overwrite` 覆盖门和并发 preview 隔离，不推断“并发 apply 谁胜出”。
- 正文保真同时检查：数据库 `body` **原始字符串不变**；归一化后的分段拼接值与输入正文一致；`layoutHtml` 提取纯文本符合 ADR-020。

## 1. P0 — API 与服务

### TC-M2-012-P0-01 TEMPLATE_GUIDED preview 成功

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-1；AC-M2-012-1；API-M2-012-01 |
| **前置** | 同租户 ARTICLE 正文非空；存在 ENABLED 且 `document_type` 匹配的公推模板 |
| **步骤** | POST `/admin-api/oa/content/{id}/typeset/ai-semantic/preview`，传 `mode=TEMPLATE_GUIDED`、模板 id（字符串） |
| **期望** | code=0；返回所选 `selectedTemplateId`、`segments`、`layoutJson`、`layoutHtml`；`fidelityCheck.passed=true`；数据库内容记录无变化 |

### TC-M2-012-P0-02 TEMPLATE_GUIDED apply 成功

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-1；AC-M2-012-2 |
| **前置** | TC-M2-012-P0-01 的内容与模板；记录 apply 前完整 `body` 字符串 |
| **步骤** | POST apply，传相同模式和模板，`overwrite=false`；随后 GET 内容详情 |
| **期望** | code=0；`body_format=LAYOUT`；`layout_template_id` 等于请求模板 id；`layout_json/layout_html` 已写入；`body` 与 apply 前逐字符一致 |

### TC-M2-012-P0-03 AUTO 路由 decision-scan seed

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-2；AC-M2-012-5；API-M2-012-02 |
| **前置** | 两款内置 seed 均 ENABLED；服务测试中固定 `FootballTemplateDecider` 输出 `decision-scan` |
| **步骤** | AUTO preview，不传 `templateId` |
| **期望** | code=0；`selectedFootballTemplate=decision-scan`；`selectedTemplateId/Name` 对应决策扫读版 seed；使用该 seed 的 `layout_schema` 完成 merge |

### TC-M2-012-P0-04 AUTO 路由 analysis-report seed

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-2；AC-M2-012-5；API-M2-012-02 |
| **前置** | 两款内置 seed 均 ENABLED；服务测试中固定 `FootballTemplateDecider` 输出 `analysis-report` |
| **步骤** | AUTO preview，不传 `templateId` |
| **期望** | code=0；`selectedFootballTemplate=analysis-report`；`selectedTemplateId/Name` 对应情报分析版 seed；使用该 seed 的 `layout_schema` 完成 merge |

### TC-M2-012-P0-05 AUTO apply 持久化选中 seed

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-2；AC-M2-012-7 |
| **前置** | AUTO preview 成功并返回任一内置模板 |
| **步骤** | 用相同内容发起 AUTO apply；GET 内容详情 |
| **期望** | `layout_template_id` 等于本次 AUTO 选中的内置 seed id；`body_format=LAYOUT`；`body` 原始字符串不变 |

### TC-M2-012-P0-06 preview 与 apply 写入边界

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-4；AC-M2-012-10 |
| **前置** | 记录内容行的 `body_format/layout_json/layout_html/layout_template_id` |
| **步骤** | 连续调用 preview 两次，再查询数据库；随后调用 apply 并再次查询 |
| **期望** | 两次 preview 均不改变四个持久化字段；仅 apply 写入版式字段 |

### TC-M2-012-P0-07 正文取值优先级与 fallback

| 项 | 内容 |
|----|------|
| **关联** | API §2.1 步骤 2 |
| **前置** | 准备请求 `body`、内容 `body`、可提取纯文本的 `layout_html`，三者文本可区分 |
| **步骤** | 分别执行：请求 body 非空；请求 body 空且 content.body 非空；前两者空且 layout_html 非空 |
| **期望** | 分段输入依次取请求 body、content.body、`extractPlainText(layout_html)`；每次 `fidelityCheck.plainTextBefore` 与实际来源一致 |

### TC-M2-012-P0-08 空正文拒绝

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-4；API-M2-012-03 |
| **前置** | 请求 body、content.body 及 layout_html 可提取文本均为空 |
| **步骤** | 分别调用 preview 与 apply |
| **期望** | 均返回 2036；apply 不写库 |

### TC-M2-012-P0-09 非 ARTICLE 拒绝

| 项 | 内容 |
|----|------|
| **关联** | API §7 内容类型 |
| **前置** | 同租户非 ARTICLE 内容正文非空 |
| **步骤** | 调用 preview 与 apply |
| **期望** | 均返回 1500；无 LLM 调用；无写库 |

### TC-M2-012-P0-10 mode 非法值拒绝

| 项 | 内容 |
|----|------|
| **关联** | API §2.1 请求、§3.2 校验 |
| **前置** | ARTICLE 正文非空 |
| **步骤** | 传 `mode=INVALID` 调 preview/apply |
| **期望** | 返回 1503；不进入分段/merge；不写库 |

### TC-M2-012-P0-11 TEMPLATE_GUIDED 模板校验

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-3；API §2.1 步骤 4、§7 |
| **前置** | 分别准备不存在、DISABLED、`document_type` 不匹配的模板；另准备缺少 `templateId` 的请求 |
| **步骤** | 逐一发起 TEMPLATE_GUIDED preview |
| **期望** | 三类无效模板均返回 1501；缺少必填 `templateId` 被参数校验拒绝；均不调用 LLM、不写库 |

### TC-M2-012-P0-12 AI 分段失败

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-6；API §6（2039） |
| **前置** | 分别 mock M8 超时、返回不可解析 JSON |
| **步骤** | 调用任一模式 preview/apply |
| **期望** | 返回 2039；不执行 merge；apply 不写库 |

### TC-M2-012-P0-13 preview 保真门拒绝

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-3；AC-M2-012-8；API-M2-012-04 |
| **前置** | mock 分段结果发生增字、删字或换序，使归一化拼接文本不等于正文 |
| **步骤** | 调用 preview |
| **期望** | 返回 2037；不返回可应用的版式结果；数据库无变化 |

### TC-M2-012-P0-14 apply 保真门拒绝且无部分写入

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-3；AC-M2-012-8、AC-M2-012-9 |
| **前置** | 已有内容版式；记录所有版式字段；mock 保真失败 |
| **步骤** | 调用 apply，`overwrite=true` |
| **期望** | 返回 2037；原 `body` 及全部版式字段保持不变，不出现部分覆盖 |

### TC-M2-012-P0-15 football taxonomy 典型分段

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-5；AC-M2-012-13、AC-M2-012-14 |
| **前置** | 正文首行为短标题，正文含比赛头、对阵、比赛时间、分析段及连续三行竞彩/比分/指数推荐 |
| **步骤** | 调用 preview，检查 `segments` 与 `segmentationReport` |
| **期望** | 首行识别为 `ARTICLE_TITLE`；三行推荐组合为 `RECOMMENDATION_LIST`；枚举均属于 API §3.2；文档序与原文一致 |

### TC-M2-012-P0-16 映射失败默认降级

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-6；AC-M2-012-15、AC-M2-012-17；API-M2-012-07 |
| **前置** | 模板 schema 无某段类型专用槽，但存在 paragraph 或 `repeat:true` 槽 |
| **步骤** | preview 传入会产生该段类型的正文 |
| **期望** | code=0；该段落入最近可用 fallback 槽；`mappingDegraded=true`；`degradedSegments` 含 index/type/slot/reason；`mappingWarnings` 非空；不返回 2041 |

### TC-M2-012-P0-17 段多于槽位

| 项 | 内容 |
|----|------|
| **关联** | ADR-028 §2.5 |
| **前置** | 正文产生的分析/普通段数量大于模板非重复槽数量；模板有默认 repeat paragraph 槽 |
| **步骤** | preview 并检查 layoutJson、layoutHtml 与保真结果 |
| **期望** | 多余 `ANALYSIS_PARAGRAPH/PLAIN_PARAGRAPH` 按文档序追加到 repeat 槽；无段丢失；保真通过 |

### TC-M2-012-P0-18 段少于槽位

| 项 | 内容 |
|----|------|
| **关联** | ADR-028 §2.5 |
| **前置** | 模板可选槽数量多于正文语义段 |
| **步骤** | preview 并检查 layoutJson/layoutHtml |
| **期望** | 空槽隐藏；不插入占位文案；提取纯文本不增加字符 |

### TC-M2-012-P0-19 AUTO 内置 seed 不可用

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-6；API §6 |
| **前置** | decider 已按评分选出稳定标识，但对应内置 seed 不存在、DISABLED 或 schema 不可解析 |
| **步骤** | 调用 AUTO preview/apply |
| **期望** | 返回 2042；不 fallback 到 DB catalog/LLM/legacy 规则链；apply 不写库 |

### TC-M2-012-P0-20 已有版式覆盖门

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-11；API-M2-012-05 |
| **前置** | 内容已有 `body_format=LAYOUT`；保存原版式字段 |
| **步骤** | apply 传 `overwrite=false` |
| **期望** | 返回 2031；原版式字段和 body 均不变 |

### TC-M2-012-P0-21 确认覆盖已有版式

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-12 |
| **前置** | 内容已有 LAYOUT；保存原 body |
| **步骤** | apply 传 `overwrite=true`；GET 内容详情 |
| **期望** | 新 `layout_json/layout_html/layout_template_id` 覆盖旧值；body 原始字符串不变 |

### TC-M2-012-P0-22 重复 apply 的已定义幂等边界

| 项 | 内容 |
|----|------|
| **关联** | API §2.2 `overwrite` |
| **前置** | 首次 apply 已成功 |
| **步骤** | 对同一内容再次发送相同请求且 `overwrite=false` |
| **期望** | 返回 2031；不产生第二次覆盖；不要求 LLM 输出或 HTML 跨请求字节级一致 |

### TC-M2-012-P0-23 并发 preview 隔离

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-4 Preview 不写库 |
| **前置** | 同一 ARTICLE 记录版式字段快照 |
| **步骤** | 并发发起 2 个 preview（可分别为 AUTO 与 TEMPLATE_GUIDED），等待均结束后查询数据库 |
| **期望** | 两请求响应互不串用 mode/template 结果；无论成功或业务失败，数据库快照字段均未变化 |

## 2. P0 — 权限、数据范围与租户

### TC-M2-012-P0-24 无权限拒绝

| 项 | 内容 |
|----|------|
| **关联** | API-M2-012-06；UX §6 |
| **前置** | 登录用户无 `oa:content:typeset` |
| **步骤** | 直接调用 preview/apply，并打开内容编辑页 |
| **期望** | API 返回 403；不调用 LLM、不写库；前端隐藏入口或工作台只读 |

### TC-M2-012-P0-25 内容数据范围拒绝

| 项 | 内容 |
|----|------|
| **关联** | ADR-028 §2.6；API §7 |
| **前置** | 用户有权限码，但不是内容创建者且不在该内容 IP 组数据范围 |
| **步骤** | 调用 preview/apply |
| **期望** | `ContentDataScopeSupport` 拒绝；不泄露正文、segments 或 layoutHtml；不写库 |

### TC-M2-012-P0-26 跨租户内容隔离

| 项 | 内容 |
|----|------|
| **关联** | API §6、§7 |
| **前置** | 租户 A 用户持有权限码；内容属于租户 B |
| **步骤** | 以租户 A 调用该内容 preview/apply |
| **期望** | 返回 1504；无 LLM 调用；无写库 |

### TC-M2-012-P0-27 跨租户模板隔离

| 项 | 内容 |
|----|------|
| **关联** | API §6、§7 |
| **前置** | 内容属于租户 A；TEMPLATE_GUIDED 的模板 id 属于租户 B |
| **步骤** | 租户 A 调用 preview/apply |
| **期望** | 返回 1504；不读取/返回租户 B 的 schema、名称或预览；无写库 |

## 3. P0 — 前端交互与 legacy 回归

### TC-M2-012-P0-28 AI 默认入口与模式切换

| 项 | 内容 |
|----|------|
| **关联** | UX §2、§5 |
| **前置** | 有权限的用户打开可编辑 ARTICLE 的版式资源工作台 |
| **步骤** | 进入“一键排版”Tab；切换 AI/RULE；关闭后重新进入 |
| **期望** | 默认 landing 为 AI；RULE 显示 ADR-027 原规则排版；两模式控件和请求语义不混用 |

### TC-M2-012-P0-29 空正文、内容类型与只读态

| 项 | 内容 |
|----|------|
| **关联** | UX U1、U2、§6 |
| **前置** | 分别打开空正文 ARTICLE、非 ARTICLE、只读 ARTICLE |
| **步骤** | 检查一键排版入口和预览按钮 |
| **期望** | 空正文预览按钮 disabled 且 tooltip“请先输入正文”；非 ARTICLE/只读态不显示可操作的一键排版入口 |

### TC-M2-012-P0-30 TEMPLATE_GUIDED 前端闭环

| 项 | 内容 |
|----|------|
| **关联** | UX §4.1；AC-M2-012-10 |
| **前置** | ARTICLE 正文非空；选择器有可用公推模板 |
| **步骤** | 选择模板→AI 排版预览→查看排版前/后和分段摘要→应用 |
| **期望** | preview 请求为 TEMPLATE_GUIDED 且带字符串 templateId；成功后才显示对比区/apply；apply 成功刷新编辑器并 Toast“AI 排版已应用” |

### TC-M2-012-P0-31 AUTO 前端闭环

| 项 | 内容 |
|----|------|
| **关联** | UX U3、U4、§4.2 |
| **前置** | ARTICLE 正文非空；模板选择器清空 |
| **步骤** | 点击预览；检查请求和结果；点击应用 |
| **期望** | preview 请求为 AUTO 且不传 templateId；展示后端返回的“决策扫读版”或“情报分析版”及分段摘要；apply 成功刷新编辑器 |

### TC-M2-012-P0-32 降级、错误与覆盖交互

| 项 | 内容 |
|----|------|
| **关联** | UX U5、U7~U11 |
| **前置** | 依次 mock mappingDegraded、2037、2039、2042；另准备已有 LAYOUT 内容 |
| **步骤** | 发起 preview/apply，观察提示与按钮；在已有版式内容点击应用 |
| **期望** | 降级显示 warning 且仍可 apply；2037/2039/2042 分别显示 UX 指定文案；已有版式先确认“正文文字不会改动”，确认后传 `overwrite=true` |

### TC-M2-012-P0-33 ADR-027 规则链回归

| 项 | 内容 |
|----|------|
| **关联** | ADR-028 §1.2、§2.8；UX §5 |
| **前置** | 原 ADR-027 规则、样式和模板数据可用 |
| **步骤** | 切换 RULE；执行原 `TEMPLATE` 和规则链 `AUTO` 的 preview/apply 冒烟 |
| **期望** | 原入口和请求路径仍可用；规则链 AUTO 不调用 AI semantic API；AI 模式不改变 legacy 配置和结果 |

## 4. P0 — `FootballTemplateDecider` 确定性规则

### TC-M2-012-P0-34 短文、单场、明确推荐选择决策扫读版

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-19；ADR-028 §2.4.1；API §3.3 |
| **前置** | 构造 `bodyCharCount=800`、`matchCount=1`、`analysisParagraphCount=3`、`recommendationCount=1`、`conclusionSegmentCount=1`，其余深度特征为 0 |
| **步骤** | 调用 decider 两次，并复算每个 reason code |
| **期望** | decision=12、analysis=0、gap=12；选 `decision-scan`；`confidence=HIGH`、`selectionReason=SCORE_THRESHOLD`；两次输出完全一致且不调用 LLM |

### TC-M2-012-P0-35 长文、多场、多维数据选择情报分析版

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-19；ADR-028 §2.4.1；API §3.3 |
| **前置** | 构造 2600 字、2 场、8 个分析段、5 个小标题、4 个不同深度维度、3 个数字数据段且含赔率；不含推荐/结论 |
| **步骤** | 调用 decider 并复算命中特征 |
| **期望** | decision=0、analysis=20、gap=20；选 `analysis-report`；`confidence=HIGH`；reason 顺序与 API 权重表一致 |

### TC-M2-012-P0-36 长度阈值边界

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-19；API §3.3.2 |
| **前置** | 其余特征保持同一固定 baseline，分别构造 1200、1201、1399、1400、2000、2001、2199、2200 个非空白 code point |
| **步骤** | 对每个边界调用评分函数，单独检查长度 reason code 与其分值 |
| **期望** | decision 长度分依次为 3、1、1、1、1、0、0、0；analysis 长度分依次为 0、0、0、1、1、1、1、3；档位不重叠、不重复计分 |

### TC-M2-012-P0-37 特征去重与可观测文本匹配

| 项 | 内容 |
|----|------|
| **关联** | ADR-028 §2.4.1；API §3.3.1 |
| **前置** | 一个段内重复出现“赔率/盘口/欧指”和多个百分比；多个段命中同一深度维度；另有大小写混合 `xG/H2H` |
| **步骤** | 提取 featureSummary |
| **期望** | 同一段对 `oddsSegmentCount`、`numericDataSegmentCount` 各最多 +1；`deepDimensionCount` 按不同维度去重；xG/H2H 大小写不敏感；不读取 metadata 未定义字段 |

### TC-M2-012-P0-38 同分与分差 1 固定低置信度回退

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-20；API-M2-012-10 |
| **前置** | 分别构造两模板同分与分差为 1 的特征集，另 mock LLM 调用计数器 |
| **步骤** | 各调用 decider 多次 |
| **期望** | 均固定选 `decision-scan`；`confidence=LOW`、`selectionReason=LOW_CONFIDENCE_FALLBACK`；`scoreGap` 分别为 0/1；LLM 调用次数为 0 |

### TC-M2-012-P0-39 低置信度前端提示

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-20；UX U12 |
| **前置** | AUTO preview 返回 `templateDecision.confidence=LOW` |
| **步骤** | 查看预览区，并改选一个公推模板重新 preview |
| **期望** | 展示“内容特征接近，已使用决策扫读版”提示；仍可 apply；改选模板后请求切为 TEMPLATE_GUIDED |

## 5. P0 — FOOTBALL_AI 一键排版

> **FR-M2-012-7** · 端点 `POST /typeset` · `mode=FOOTBALL_AI` · 无 LLM（ADR-027 §4.3 · API §9）

### TC-M2-012-P0-40 竞彩营销版 preset

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；AC-M2-012-21；API §9 |
| **类型** | 自动化（IT/集成） |
| **前置** | tenant 1 存在 V204 `football-ai:marketing` PRESET 且 ENABLED；ARTICLE 正文非空 |
| **步骤** | POST `/admin-api/oa/content/typeset`，传 `mode=FOOTBALL_AI`、`footballTemplate=marketing`、`body` 为典型竞彩短文 |
| **期望** | code=0；返回 `layoutHtml`/`layoutJson`；`selectedFootballTemplate=marketing`；`selectedTemplateName` 含「竞彩营销」；HTML 含 marketing 样式特征（如圆角分析卡、`#e94560` 强调）；**不调用** M8 `AI_TYPESET_SEMANTIC` |

### TC-M2-012-P0-41 简洁通读版 preset

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；API §9；UX §2.3 |
| **类型** | 自动化（IT/集成） |
| **前置** | V204 `football-ai:clean-read` PRESET ENABLED；正文为长段通读型短文 |
| **步骤** | POST typeset，`mode=FOOTBALL_AI`、`footballTemplate=clean-read`、传 `body` |
| **期望** | code=0；`selectedFootballTemplate=clean-read`；layout 无头图/卡片装饰，导语 + 长文通读结构；无 LLM 调用 |

### TC-M2-012-P0-42 赛事分析版 AUTO 决策

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；FR-M2-012-2；API §9；UX Q3 |
| **类型** | 自动化（IT）+ 人工（UI 模板名展示） |
| **前置** | V198 `decision-scan` / `analysis-report` seed 均 ENABLED；分别准备偏决策扫读与偏情报分析的两份正文 |
| **步骤** | POST typeset，`mode=FOOTBALL_AI`，**不传** `footballTemplate`；前端「赛事分析版」卡片触发同等请求 |
| **期望** | code=0；`selectedFootballTemplate` 为 `decision-scan` 或 `analysis-report` 之一；AUTO 时返回 `templateDecision`（confidence/reasons）；分段走启发式 `SemanticSegmentHeuristicUpgrader`，**不调用 LLM**；UI 预览区展示已选模板中文名 |

### TC-M2-012-P0-43 付费引流版 paidBoundary

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；API §9 `paramOverrides.paidBoundary`；UX Q4 |
| **类型** | 自动化（IT）+ 人工（双栏编辑器） |
| **前置** | 内容同时存在免费区与付费区正文；marketing PRESET 可用 |
| **步骤** | 对**免费区** POST typeset：`footballTemplate=marketing`、`paramOverrides.paidBoundary=true`；对付费区传相同 preset 但 **不传** `paidBoundary` |
| **期望** | 免费区 layout 末尾含付费分界线 markup；付费区 layout **无** paidBoundary 插入；`selectedFootballTemplate=marketing`；两区分别 typeset、互不覆盖 |

### TC-M2-012-P0-44 仅免费区有正文 free-only apply

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；AC-M2-012-22；ADR-027 §4.3 |
| **类型** | 人工走查 / Playwright |
| **前置** | ARTICLE 仅 `free_body` 非空，付费区 `body`/`layout_html` 为空或初始态；记录付费区四个版式字段快照 |
| **步骤** | 打开 `WechatQuickTypesetDialog` → 任选预设 → 预览 →「使用此排版」 |
| **期望** | 仅免费编辑器刷新 `free_body`/`layout_json`/`layout_html`；付费编辑器与 DB 付费区版式字段**不变**；正文纯文本 SSOT 未改写 |

### TC-M2-012-P0-45 html 与 body 均为空拒绝

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；AC-M2-012-23；API §9 |
| **类型** | 自动化（IT） |
| **前置** | 有效权限 Token |
| **步骤** | POST typeset，`mode=FOOTBALL_AI`，`html=""` 且 `body=""`（或均省略） |
| **期望** | HTTP 400 / Bean Validation 失败；不进入分段/merge；不写库 |

### TC-M2-012-P0-46 body-first 优先于 styled HTML

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；AC-M2-012-24；API §9；ADR-020 |
| **类型** | 自动化（单测/IT） |
| **前置** | 构造可区分的 `body` 纯文本与 `html` styled 版本（二者 extractPlainText 结果不同）；mock 或 spy 分段输入来源 |
| **步骤** | POST typeset，同时传 `body` 与 `html`；再仅传 `html`（body 空）作对照 |
| **期望** | 同时存在时分段输入取 **请求 `body`**，不从 styled div 重新拆段；`plainTextBefore` 与 `body` 一致；仅 html 时 fallback 到 html 提取文本 |

### TC-M2-012-P0-47 LayoutMergeFidelityGate 拒绝丢字

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；AC-M2-012-25；API §9 |
| **类型** | 自动化（单测/IT） |
| **前置** | mock merge 或分段结果导致归一化纯文本与输入 body 不一致 |
| **步骤** | POST typeset preview 路径（或带 id 的 typeset）；随后尝试写回编辑器/apply |
| **期望** | 返回 `LAYOUT_SCHEMA_INVALID` / 保真相关错误；不返回可应用的 layout；**不写库**、不覆盖编辑器 |

### TC-M2-012-P0-48 HTML 实体解码保真

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；PRD §FR-M2-012-7 ¶6；ADR-020 |
| **类型** | 自动化（单测） |
| **前置** | 正文含 `&ldquo;`/`&rdquo;`/`&nbsp;` 等实体及中文标点（如「」、——） |
| **步骤** | 经 `OpsHtmlTextHelper.decodeEntities`（最多 3 pass）后进入 FOOTBALL_AI 分段；检查 merge 前后可见字符 |
| **期望** | 解码后中文标点与引号可见字符保留；`escapeHtml` 不破坏中文标点；保真 Gate 通过 |

### TC-M2-012-P0-49 FOOTBALL_AI 不调用 LLM

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；ADR-027 §4.3；API §9 |
| **类型** | 自动化（IT + mock 计数） |
| **前置** | mock/spy `AiLlmInvokeSupport` 或 M8 调用计数器；四套 preset 各准备一份正文 |
| **步骤** | 分别对 marketing、clean-read、decision-scan（显式）、AUTO（空 footballTemplate）发起 typeset |
| **期望** | 四次请求 LLM 调用次数均为 **0**；分段均来自 `SemanticSegmentHeuristicUpgrader`；不与 `/typeset/ai-semantic/*` 混用 |

### TC-M2-012-P0-50 一键排版弹窗 preview→apply 闭环

| 项 | 内容 |
|----|------|
| **关联** | FR-M2-012-7；UX §2.3 Q1~Q5 |
| **类型** | 人工走查 / Playwright |
| **前置** | 可编辑 ARTICLE、标题或任一侧正文非空；有 `oa:content:typeset` 权限 |
| **步骤** | 工具栏「一键排版」→ 切换四套风格卡片（每次自动刷新预览 iframe）→「使用此排版」；验证 `layoutSync` 写回 |
| **期望** | 预览调用 `POST /typeset` · `mode=FOOTBALL_AI`；预览失败仅提示重试、**不 fallback** 到 AI 排版；应用后 `layout_json`/`layout_html` 同步、正文纯文本不变；Toast/刷新行为符合 UX |

## 6. P1 — MVP 后增强/非阻断验证

### TC-M2-012-P1-01 strict 映射失败

| 项 | 内容 |
|----|------|
| **关联** | API §3.4、§6 |
| **前置** | 后续版本明确并启用 strict 配置；构造无可映射且无 fallback 槽的段 |
| **步骤** | 调用 preview/apply |
| **期望** | 返回 2041；apply 不写库 |

### TC-M2-012-P1-02 styleHints 保真

| 项 | 内容 |
|----|------|
| **关联** | AC-M2-012-16、AC-M2-012-18；API-M2-012-08 |
| **前置** | Phase 1.5+ 启用 styleHints；分段含合法字符偏移 |
| **步骤** | preview/apply，检查 inline span 与纯文本 |
| **期望** | 高亮 markup 生效；segment.text、body 和提取纯文本均未改变 |

### TC-M2-012-P1-03 styleHints 非法偏移

| 项 | 内容 |
|----|------|
| **关联** | API §3.1.1 |
| **前置** | Phase 1.5+；mock 越界或重叠的 styleHints |
| **步骤** | 调用 preview |
| **期望** | 按届时补充的 styleHints 校验契约处理，且任何情况下不得改变可见正文；契约明确前不得转为 P0 |

### TC-M2-012-P1-04 paramOverrides 透传

| 项 | 内容 |
|----|------|
| **关联** | API §2.1 请求字段 |
| **前置** | 模板声明可覆盖参数 |
| **步骤** | preview/apply 传模板已声明的 `paramOverrides` |
| **期望** | 参数传入 `LayoutMergeService` 并反映在渲染结果；不改变 body 纯文本 |

### TC-M2-012-P1-05 长耗时与移动端展示

| 项 | 内容 |
|----|------|
| **关联** | UX §7 |
| **前置** | mock preview 超过 60s；另以移动端 viewport 打开侧栏 |
| **步骤** | 发起 preview 并尝试关闭侧栏；检查超时提示和移动端对比区 |
| **期望** | loading 时侧栏不可关闭；超过 60s 显示指定提示；移动端侧栏全屏、排版后区域可滚动 |

## 7. P0 Gate 统计与验收

| 类别 | P0 | P1 |
|------|---:|---:|
| API 与服务 | 23 | 4 |
| 权限/数据范围/租户 | 4 | 0 |
| 前端与 legacy 回归 | 6 | 1 |
| FootballTemplateDecider | 6 | 0 |
| FOOTBALL_AI 一键排版 | 11 | 0 |
| **合计** | **50** | **5** |

### 2026-09-11 收尾执行快照（report 87478759 续）

- **自动通过（33）**：P0-01～P0-06、P0-07～P0-11、P0-12～P0-17、P0-19～P0-23、P0-25～P0-27、P0-34～P0-38。
  - 本轮新增：`AiSemanticTypesetIT`（P0-07 正文三源、`TEMPLATE_GUIDED` 1501×4、P0-25 数据范围 403）、`AiTypesetBodySupportTest` 三源 fallback、`OpsPermissionCheckerTest` + `OpsGlobalExceptionHandlerAccessDeniedTest`（P0-24 权限链）、`aiSemanticTypesetting.test.ts` `canShowAiTypesetToolbar`（P0-29）。
- **人工走查通过（4）**：P0-28、P0-31、P0-33、P0-39（沿用 2026-09-08 浏览器证据；本次未复跑）。
- **Vitest 辅助通过（部分 P0-32）**：2037/2039/2042 文案、overwrite 确认文案、低置信提示文案 — **7/7** 绿；映射降级 warning 展示与 mock 全链路 UI 仍待 E2E。
- **待验证 / 延期（2）**：
  - **P0-30**：工作台 `TypesettingPanel` 已固定 **AUTO-only**（无模板选择器→`TEMPLATE_GUIDED` 前端闭环）；后端/Gateway 仍支持 TEMPLATE_GUIDED（2026-09-08 证据有效）。按产品决策记 **N/A（MVP AUTO 入口）**，不计入 Gate 失败。
  - **P0-32**：错误文案单测已覆盖；降级 warning 展示 + mock 2037/2039/2042/overwrite 的 **Playwright 全链路** 未在本轮复跑。
- **失败（0）**：无。
- 联调（本轮）：`start-ops-dev.ps1 -NoRestart` 后 **Nacos push 失败**、`:48094` ops-server **185s 未就绪**、Gateway `:48080` 登录不可达 → **未能复跑真实 LLM AUTO preview**（2026-09-08 Gateway 证据仍有效）。
- S-21a 聚焦单测（本轮增量后）：**AiSemanticTypesetIT 8** + **OpsPermissionCheckerTest 2** + **OpsGlobalExceptionHandlerAccessDeniedTest 1** + **AiTypesetBodySupportTest 7** + 既有 decider/fidelity/mapper/service **50** ≈ **68** 绿（未全量重跑 ops-server 202 条）。
- 前端 Vitest：`aiSemanticTypesetting.test.ts` **7/7**。
- **P0 合计：37/39**（33 自动 + 4 人工；P0-30 N/A；P0-32 部分待 E2E）。
- **浏览器可用性**：本机 Gate 栈未 UP → **当前无法在浏览器完成真实 AI 排版**；修复 Docker/Nacos/ops-server 后可按 2026-09-08 路径复验 AUTO preview→apply。
- 模型环境：本机 `local` profile、`shenyu-ops` tenant 1；系统「AI 模型配置」中的 `qwen3.7-plus` 为 `ENABLED + CONNECTED + default`。密钥仅以 `sk-****` 表示，经配置 API 写入后 `api_key_encrypted` 为 64 字符密文且不以 `sk-` 开头。`AI_TYPESET_SEMANTIC` 使用 OpenAI-compatible Chat Completion；`jingcai.article` 继续仅用于文案生成。
- 真实 Gateway 证据：AUTO 决策扫读版 preview code=0；AUTO 情报分析版 preview code=0、25 segments，apply 后模板 id=19、`body` SHA-256 不变；低置信用例 score=7/8、gap=1、固定回退 `decision-scan`；TEMPLATE_GUIDED 模板 id=17 的 QUOTE 段真实降级，`mappingDegraded=true` 且保真通过。
- 并发证据：AUTO 与 TEMPLATE_GUIDED 两个真实 preview 均 code=0、响应模式/模板互不串用，完成后两条内容的四个版式字段组合 SHA-256 快照不变。
- UI 证据：浏览器真实完成 AI/RULE 切换、AUTO preview→覆盖确认→apply、模板选择与 TEMPLATE_GUIDED preview、低置信提示、映射降级 warning，以及 legacy RULE preview/apply；RULE 期间 AI semantic 请求数为 0。空正文的 AI preview 已确认 disabled 且 tooltip 为“请先输入正文”，但 P0-29 的非 ARTICLE/只读子场景仍待验证。
- 安全修复：模型连接测试改为真实最小 Chat Completion；连接字段变化会重置 `DISCONNECTED`。控制台访问日志与数据库访问日志均补充 `apiKey/secret` 脱敏；重启后运行日志潜在 `sk-` 长密钥匹配数为 0。
- 补充证据：focused 后端 17/17、前端 4/4 通过；Qwen 语义 JSON 请求关闭 thinking 后，短文真实 preview 约 4 秒、长文真实 apply 约 37 秒。
- seed 证据：tenant 1 的 `decision-scan`、`analysis-report` 均单行、ENABLED、schema_version=2 且 JSON 可读；重复执行 V198 后行数不变；`AI_TYPESET_SEMANTIC` 单行 ENABLED。
- 权限 seed 缺口：greenfield system 菜单原无 `ops:content:typeset`；已在 `02-shenyu-system-menus.sql` 以菜单 6290 幂等补齐，并在 `02b-shenyu-system-ops-roles.sql` 绑定内容编辑/运营角色。本地重复执行后菜单单行、四个角色各一条绑定。
- 范围 verify（2026-09-11）：ops-server 全量 **202** 条，**193 通过**，**7 失败 + 1 Mockito strictness 错误**；失败均在 `WorkTask*` / `WechatArticleHtmlFetcherTest`（非 S-21a 范围）。
- S-21a 聚焦单测：**50/50** 绿（见 SLICES-M2-S21a §8.1）。
- 前端 Vitest：`aiSemanticTypesetting.test.ts` **5/5**（本次修复 `vi.mock('./client')` 避免 window 依赖）。
- 联调：Gateway `:48080` / Nacos 未起（Docker 不可用）；`:48094` 可 `--spring.cloud.nacos.discovery.enabled=false` 起 health=UP，但无有效 Token，**未能复跑真实 LLM AUTO preview**（2026-09-08 证据仍有效）。
- production build / Playwright：未在本轮复跑；Gate 仍不通过。
- **P0 进度：37/39**（P0-30 AUTO-only N/A；P0-32 E2E 待补）。

### 2026-09-11 session 2 执行快照

- **栈状态**：ops :48094、Gateway :48080、front :5777 均 HTTP 200。
- **聚焦后端单测 83/83 绿**：
  - `AiSemanticTypesetIT` 8 · `AiSemanticTypesetServiceTest` 5 · `FootballLayoutSemanticRendererTest` 34 · `ParagraphEmphasisHelperTest` 6
  - `FootballTemplateDeciderTest` 5 · `TypesetFidelityGateTest` 8 · `SegmentSlotMapperTest` 4 · `AiTypesetBodySupportTest` 7 · `ProductionContentAiSemanticTypesetTest` 3 · 权限 3
- **前端 Vitest**：`aiSemanticTypesetting.test.ts` **7/7** 绿。
- **真实 LLM 联调（Gateway）**：
  - content **9448** AUTO preview code=0，7.5s，`decision-scan`，5 segments，`fidelityCheck.passed=true`，layoutHtml 2299B
  - content **9449**（marketing）AUTO preview code=0，`analysis-report`，layoutHtml 9421B，`text-indent:2em` ×11
- **Playwright**：`content-ai-semantic-typeset.spec.ts` **不存在**；P0-32 全链路 E2E 仍待补。
- **P0 合计仍为 37/39**（本轮复验 LLM 不新增 P0 计数；P0-30 N/A；P0-32 阻塞）。
- **失败（0）**。

### 2026-09-17 文档同步（FR-M2-012-7 TESTCASES 补全）

- **新增 P0-40~50**（11 条）：覆盖 FOOTBALL_AI 四套预设、body-first、free-only apply、空正文校验、实体解码、保真 Gate、无 LLM、弹窗 preview→apply。
- **P0 合计：37/50**（原 37/39 仍有效；**P0-40~50 全部待验证**；P0-30 AUTO-only N/A；P0-32 Playwright 仍阻塞）。
- **失败（0）**；未宣称新增用例已通过。
- FOOTBALL_AI 实现与 V204 seed 已于 2026-09-14 落地（CHECKLIST §0 已勾选）；本轮仅补测试规格与 Gate 归档，**未复跑自动化**。

### Gate 通过口径

- [ ] 本文 **50/50 条 P0 全部通过**（含 P0-30 N/A 口径按产品决策不计失败），失败数为 0；P1 记录结果但不阻断 MVP。
- [ ] 后端自动化覆盖两模式、两款内置 seed 路由、错误码、保真门、默认降级、权限/租户及 overwrite。
- [ ] 前端自动化覆盖 AI 默认入口、TEMPLATE_GUIDED/AUTO、预览后应用、降级 warning、覆盖确认和 legacy 切换。
- [ ] seed 验证确认 `decision-scan`、`analysis-report` 与 `AI_TYPESET_SEMANTIC` 均存在、可解析、租户策略符合实现 Spec。
- [ ] 相关模块 `CHECKLIST-M2-AI排版增量.md` 100%；上一阶段 P0 冒烟仍绿；按所属阶段重跑 `mvn verify` / `playwright test`。
- [ ] Gate 报告归档并更新 `MASTER-EXECUTION-TRACKER.md` 后，方可宣称通过。

## 8. 实现前阻塞

**无。** 原 BLK-M2-012-01（Slice 未批准）与 BLK-M2-012-02（评分算法未定）已于 2026-09-08 关闭。并发 apply 的锁/版本/冲突响应与 idempotency key 仍为未定义的非 MVP 边界，本 Slice 不实现、不验收。

---

*Ready · v1.2 · 2026-09-17*
