# UX-M2-内容生产

> **版本**：v1.4 | 2026-06-14
> **关联 PRD**：[`PRD-M2-内容生产.md`](./PRD-M2-内容生产.md)
> **全局规范**：[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 ID | 名称 | 路由 | 关联 FR |
|---------|------|------|---------|
| P-M2-001 | SOP 模板列表 | `/prod/sop/template` | FR-M2-001 |
| P-M2-002 | SOP 模板编辑（DAG 画布） | `/prod/sop/template/:id` | FR-M2-001 |
| P-M2-003 | 任务列表（含「我的任务」默认 Tab） | `/prod/task` | FR-M2-002 |
| P-M2-004 | 任务详情 | `/prod/task/:id` | FR-M2-002 |
| P-M2-005 | ~~我的任务~~（已合并至 P-M2-003 Tab） | — | FR-M2-002 |
| P-M2-006 | 内容列表（弹窗创作/查看） | `/content` | FR-M2-003 |
| P-M2-007 | 内容创作/编辑（**路由保留**，菜单隐藏；主路径为弹窗） | `/content/edit` | FR-M2-003 |
| P-M2-008 | 内容审核 | `/prod/content/review` | FR-M2-003 |
| P-M2-009 | 知识库列表 | `/prod/knowledge` | FR-M2-004 |
| P-M2-010 | 知识详情 | `/prod/knowledge/:id` | FR-M2-004 |
| P-M2-011 | 计划管理 | `/plan` | FR-M2-009 |
| P-M2-012 | 任务执行 | `/prod/task/:id/execute` | FR-M2-002 |
| P-M2-013 | 公推模板库列表 | `/prod/layout-template` | FR-M2-005 |
| P-M2-014 | 公推模板导入向导 | `/prod/layout-template/import`（或弹窗） | FR-M2-005 |
| P-M2-015 | 公推模板编辑/预览 | `/prod/layout-template/:id/edit` | FR-M2-005 |

---

## 2. P-M2-001 SOP 模板列表

| 控件 | 类型 | 文案/默认值 | 字典/实体 |
|------|------|------------|----------|
| F-NAME | `<Input />` | "模板名称" | - |
| F-CONTENT-TYPE | `<DictSelect dict-type="dict_content_type" />` | "内容类型" | 字典 |
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` | "平台类型" | 字典 |
| BTN-ADD | 按钮 | "新增模板" | - |
| BTN-EDIT | 链接 | "编辑" | - |
| BTN-DELETE | 链接 | "删除" | - |
| BTN-ACTIVATE | 链接 | "启用" | - |
| TBL-TPL | 表格 | - | `oa_sop_template` |

---

## 3. P-M2-002 SOP 模板编辑（DAG 画布）

> **版本**：v1.1 | 2026-06-11

### 3.1 布局（实现：栅格 4+14+6）

```
+------------------------------------------------------------------+
| 面包屑 | [保存] [返回] | 自动布局 | 适配视图 | 校验DAG | 撤销/重做 |
+------------------------------------------------------------------+
| 节点库 (lg:4)     |  LogicFlow 画布 (lg:14)  | 属性面板 (lg:6) |
| 拖拽节点模板      |  DAG 可视化 + 连线        | 节点名/岗位/审核  |
+------------------------------------------------------------------+
```

| 列 | 宽度 | 内容 |
|----|------|------|
| 左 | `el-col :lg="4"` | 节点模板拖拽列表 |
| 中 | `el-col :lg="14"` | LogicFlow 画布 |
| 右 | `el-col :lg="6"` | 选中节点属性（`dict_position`） |

**保存**：`persistNodes` 调用 `createSopNode` / `updateSopNode`；新节点 ID remap 后刷新边。

**缺口**：删除节点仅前端移除；无 `DELETE /sop/node` API（刷新恢复）。

### 3.2 节点编辑弹窗

| 控件 | 类型 | 字典/实体 | 必填 |
|------|------|----------|------|
| F-NODE-NAME | `<Input />` | - | ✅ |
| F-EXEC-ROLE | `<DictSelect dict-type="dict_position" />` | `dict_position` | ✅ |
| F-NEED-REVIEW | `<Switch />` | - | ✅（默认 false） |
| F-REVIEW-ROLE | `<DictSelect dict-type="dict_position" />` | `dict_position` | 条件（need_review=1 时必填） |
| F-PREDECESSORS | `<SelectMultiple />`（多选同模板其他节点） | `oa_sop_node` | ❌ |
| F-PARALLEL-GROUP | `<Input />` | - | ❌ |
| F-SLA-HOURS | `<InputNumber />` | - | ❌ |
| F-NODE-TYPE | `<DictSelect dict-type="dict_sop_node_type" />` | `dict_sop_node_type` | ✅（ADR-016） |

**`dict_sop_node_type` 选项**：内容生成 / 内容发布 / 普通节点。

### 3.3 状态

| 状态 | 表现 |
|------|------|
| 保存失败（DAG 环） | 红色 Banner："节点 X 与 Y 形成环" |
| 保存成功 | Toast "保存成功" + 画布刷新 |
| 节点无 `executor_role` | "请选择执行岗位" |

---

## 4. P-M2-003/004 任务列表/详情

**默认 Tab**：「我的任务」（`activeTab=my`），次 Tab「全部任务」。

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| TAB-MY | Tab | 「我的任务」（默认） |
| TAB-ALL | Tab | 「全部任务」 |
| F-IP | `<IpGroupTreeSelect />` | `oa_ip_group` |
| F-STATUS | `<DictSelect dict-type="dict_sop_node_status" />` | 字典 |
| F-EXECUTOR | `<UserSelect />` | `sys_user`（全部任务 Tab） |
| TBL-TASK | 表格 | `oa_task` |
| COL-SCHEDULE | 列 | `scheduled_start` / `scheduled_end`（计划起止） |
| BTN-EXECUTE | 链接 | 「执行」（仅「我的任务」Tab + 可执行态） |
| BTN-SUBMIT-REVIEW | 链接 | 「提交审核」（任务级，非内容确认） |

### 4.1 任务详情（弹窗/抽屉）

| 区域 | 内容 |
|------|------|
| 基本信息 | 任务名称、关联计划、IP 组、SOP 模板 |
| 节点进度 | 时间轴（已完成/进行中/待执行/已驳回） |
| 审核记录 | 审核人、时间、结果、评论 |
| 操作 | 开始/完成/提交审核（按当前节点状态） |

### 4.2 SLA 超时展示

- 任务超时 → 节点标红 + 显示超时时长
- 顶部 Banner："您有 X 个超时任务"

---

## 4.3 P-M2-012 任务执行页（需求 4–5）

**路由**：`/prod/task/:id/execute` · **入口**：我的任务 → 状态=待执行 →「执行」

```
+----------------------------------------------------------+
| 任务基本信息（名称、节点、计划、IP组、赛事、SLA）          |
+----------------------------------------------------------+
| 执行说明（只读，`oa_sop_node.instruction_text`）                        |
| 附件列表只读（`attachment_urls` JSON；上传见 BLK-M2-007）                            |
+----------------------------------------------------------+
| [node_type=内容生成]                                     |
|   关联内容摘要 / 「进入内容创作」→ /prod/content/edit?taskId= |
| [node_type=内容发布] 占位（BLK-M2-009）                  |
| [node_type=普通节点] 交付说明输入区                      |
+----------------------------------------------------------+
| [保存]  [完成]                                           |
+----------------------------------------------------------+
```

| 控件 | 类型 | 说明 |
|------|------|------|
| BTN-CONTENT-EDIT | 按钮 | 打开 `ContentEditDialog`（`taskId`）；非路由菜单 |
| BTN-SAVE | 按钮 | 保存执行页草稿字段 |
| BTN-COMPLETE | 按钮 | 完成；内容生成节点校验内容 `COMPLETED`（AC-M2-002-6） |

| 状态 | 表现 |
|------|------|
| 无关联内容 | 提示「请先进入内容创作」 |
| 内容未完成 | 完成按钮禁用 + Tooltip |
| 完成成功 | Toast + 返回我的任务列表 |

---

## 5. P-M2-006/007 内容列表/编辑

### 5.1 内容列表

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| BTN-ADD | 按钮 | 「新增内容」→ 打开 `ContentEditDialog` |
| BTN-EXPORT | 按钮 | 导出 CSV（ADR-018） |
| BTN-VIEW | 链接 | 只读弹窗 + 审核流程 steps |
| BTN-SUBMIT | 链接 | 「提交审核」（DRAFT） |

> **无**独立侧栏菜单「内容创作」；`/content/edit` 路由保留供深链。

### 5.2 内容编辑（`ContentEditPanel` / `ContentEditDialog`）

**模式 A — 独立创作**（弹窗）：提交审核；无封面字段。

**模式 B — 任务驱动**（执行页弹窗或带 `taskId`）：

| 控件 | 类型 | 字典/实体 | 必填 |
|------|------|----------|------|
| F-TASK-ID | 隐藏 | `oa_task` | ✅（模式 B） |
| F-IP-GROUP | 只读 | 任务 IP 组 | ✅ |
| F-COMPETITION | `<MatchSelectDialog />` | 外部赛事 | 可选 |
| F-TITLE | `<Input />` | - | ✅ |
| F-CONTENT-TYPE | `<DictSelect dict-type="dict_content_type" />` | 字典 | ✅ |
| F-DOCUMENT-TYPE | `<DictSelect dict-type="dict_document_type" />` | 字典 | 条件（`ARTICLE`） |
| F-SCRIPT-REF | 只读区 | 同赛事短视频文案 | 条件（`SHORT_VIDEO`） |
| F-BODY | **`<RichTextEditor />`**（ARTICLE）或 `<Textarea />`（其他） | layout_html + body 摘要 | 条件 |
| F-LAYOUT-STRUCT | **`<LayoutEditor />`**（右侧，LAYOUT 时） | layout_json | 条件（ARTICLE+已套用模板） |
| F-LAYOUT-TPL | `<LayoutTemplateSelect />` | `oa_wechat_layout_template` | 条件（`ARTICLE`） |
| BTN-APPLY-TPL | 按钮 | 「选择版式模板」→ 选择器 + 二次确认 | 条件（`ARTICLE`） |
| F-GENERATED-VIDEO | 预览 | AI 生成视频 URL | 条件（短视频） |
| F-FINAL-VIDEO | `<Input />` | 最终视频 URL | ❌ |
| BTN-GENERATE | 按钮 | AI 弹窗（选 M8 模型+提示词，真实 LLM） | - |
| BTN-SAVE | 按钮 | 「保存」→ `DRAFT` | - |
| BTN-SUBMIT-REVIEW | 按钮 | 「提交审核」（非「确认」） | - |

**模式 A 额外字段**：

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-IP-GROUP | `<Select />` 用户所属 IP 组 | **必填** |
| F-PLATFORM | `<DictSelect multiple />` | 可选 |
| F-ACCOUNT | `<AccountSelect multiple />` | 可选，联动 platform + ip_group |
| F-AI | `<Switch />` + AI 弹窗 | - |

**只读/待审核**（`PENDING_*`）：表单 disabled；展示审核流程 `el-steps`（角色+用户）。

**联动**：
- `content_type=ARTICLE` → 显示 `document_type` + **版式模板区**
- `content_type=SHORT_VIDEO` → 文案引用区
- 切换 IP 组 → 刷新作者信息
- 应用模板后 → 富文本编辑器即时显示 `layout_html`；`body_format=LAYOUT`；`body` 为纯文本摘要（ADR-021）

> **FR-135（2026-06-14）**：ARTICLE 正文主区为 TipTap 富文本；左右分栏，右侧为版式结构编辑（套用模板后可见）。

**变更 2026-06-15（FR-143 / FR-147 · ADR-021）**：

| 控件/行为 | 说明 |
|-----------|------|
| BTN-MAXIMIZE | 正文区「全屏/还原」；全屏时隐藏版式侧栏 |
| BTN-LAYOUT-COLLAPSE | 「展开/收起版式结构」；**默认收起** |
| F-LAYOUT-STRUCT | 收起或全屏时主区 24 栅格；展开时 14+10 分栏 |
| RTE-TOOLBAR | 对齐公众号编辑器：字号/颜色/高亮/对齐/列表/引用/表格/图片上传与宽度 |
| WECHAT-PASTE | 粘贴走 `normalizeWechatPasteHtml`；保存走 `sanitizeWechatExportHtml` |
| IMG-WIDTH | 编辑态 `ResizableImage`；查看态 `LayoutViewer` + `ensureImageWidthStyles` 一致 |

**只读/待审核**（`PENDING_*`）：`LayoutEditor` **readOnly**；无 `layout_json` 时 fallback 渲染 `body` 纯文本

### 5.2.1 版式模板选择器（`LayoutTemplateSelectDialog`）

| 控件 | 说明 |
|------|------|
| F-DOC-TYPE-FILTER | 按当前内容 `document_type` 过滤（+ 通用模板 document_type 为空） |
| TBL-TPL | 卡片/表格：缩略图、名称、document_type 标签、来源（手动/链接/Word） |
| BTN-PREVIEW | 抽屉预览 `layout_html` |
| BTN-APPLY | 应用（若已有版式 → `MessageBox.confirm`） |

### 5.3 AI 辅助创作弹窗

- 选择 **M8 已启用 AI 模型**（`GET /config/ai-model/list` enabled）
- 选择 **匹配提示词**（按 `contentType` / `documentType`）
- 「生成」→ 调 `POST /content/ai-generate` → 写入 `body`

---

## 6. P-M2-008 内容审核（ADR-017）

**Tab**：一级审核 / 二级审核（随 `review-config` 动态显示；均未开启时提示直接发布）。

| 控件 | 类型 | 说明 |
|------|------|------|
| TAB-L1 | Tab | 一级审核队列 |
| TAB-L2 | Tab | 二级审核队列 |
| TBL-REVIEW | 表格 | 待审内容 |
| BTN-VIEW | 链接 | 只读抽屉 + 正文（**`LayoutViewer`** 渲染 layout_html）+ 审核流程 steps |
| BTN-APPROVE / REJECT | 按钮 | 仅待审态、有权限时 |

审核流程 steps 描述格式：`{角色名}：{用户1、用户2}`。

---

## 7. P-M2-009/010 知识库

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-TITLE | `<Input />` | - |
| F-CATEGORY | `<Select />` | 固定值（案例库/模板库/行业资料/运营经验） |
| F-TAGS | `<TagInput />` | - |
| F-PUBLIC | `<DictSelect dict-type="dict_yes_no" />` | 字典 |
| F-SEARCH | `<Input />` | - |
| TBL-KNOWLEDGE | 表格 | `oa_knowledge_base` |

---

## 8. P-M2-013~015 公推模板库（FR-M2-005 · 草案）

> **菜单**：内容生产 → **公推模板库**（与「知识库」并列，**非**知识库「模板库」分类）

### 8.1 P-M2-013 模板列表

**路由**：`/prod/layout-template`

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-NAME | `<Input />` | 模板名称 |
| F-DOC-TYPE | `<DictSelect dict-type="dict_document_type" />` | 可筛「通用」（空） |
| F-STATUS | `<DictSelect dict-type="dict_layout_template_status" />` | 启用/停用 |
| F-SOURCE | `<DictSelect dict-type="dict_layout_template_source" />` | 手动/链接/Word |
| BTN-ADD | 按钮 | 「新建模板」→ P-M2-015 |
| BTN-IMPORT | 按钮 | 「导入」→ P-M2-014 |
| BTN-EDIT | 链接 | 编辑 |
| BTN-PREVIEW | 链接 | 只读预览抽屉 |
| BTN-DISABLE | 链接 | 停用（二次确认） |
| TBL-TPL | 表格 | 名称、document_type、来源、更新人、更新时间、状态 |

**权限**：无 CRUD 权限者 **不展示** BTN-ADD/EDIT/DISABLE（仍可只读列表，**待产品确认**）

### 8.2 P-M2-014 导入向导（960px 弹窗或独立页）

```
+----------------------------------------------------------+
| Step 1: 选择导入方式                                      |
|   ( ) 公众号文章链接    ( ) Word 文档    ( ) 粘贴 HTML    |
+----------------------------------------------------------+
| Step 2: 输入                                               |
| [链接] 或 [Upload docx] 或 [富文本粘贴区]                  |
| 模板名称 [____]  文档子类型 [DictSelect 可空]              |
+----------------------------------------------------------+
| Step 3: 预览（异步 Job 完成后）                            |
|   LayoutViewer 预览  |  解析警告（BLK-M2-014 样式丢失提示）  |
+----------------------------------------------------------+
| [取消]  [上一步]  [保存为模板]                             |
+----------------------------------------------------------+
```

| 状态 | 表现 |
|------|------|
| URL Job 进行中 | `el-progress` + 「正在解析，可关闭后在列表查看」 |
| URL 失败 | 红色 Alert + 引导切换「粘贴 HTML」Tab |
| docx 超限 | 「文件不能超过 10MB」 |

### 8.3 P-M2-015 模板编辑/预览

> **FR-134（2026-06-14）**：编辑页提供 Tab「富文本编辑」/「结构编辑」，双向同步 `layout_schema` ↔ 预览 HTML（ADR-021）。  
> **FR-147（2026-06-15）**：富文本 Tab 使用与内容创作相同的 `RichTextEditor` + `EditorToolbar`（非独立简化版）。

**布局**：

```
+----------------------------------------------------------+
| 模板名称 [__]  文档子类型 [DictSelect 可空]  [保存] [预览] |
+----------------------------------------------------------+
| [富文本编辑] | [结构编辑]  — TipTap WYSIWYG ↔ LayoutSchemaEditor |
+----------------------------------------------------------+
```

- **预览**：全屏抽屉，`LayoutViewer` 模拟公众号阅读宽度（约 677px 居中）
- 新建空模板：默认含 1 个 `heading` + 1 个 `paragraph` 块

### 8.4 内容查看/审核 — 版式渲染（跨 P-M2-006/008）

| 场景 | 组件 | 数据 |
|------|------|------|
| 有 `layout_html` | `<LayoutViewer readOnly />` | `layout_html` |
| 仅 `body` 纯文本 | `<pre>` 或 `<Textarea disabled />` | `body` |
| 列表摘要 |  strip HTML 前 80 字 | `layout_html` 优先 |

---

## 9. P-M2-011 计划管理

**路由**：`/plan` · **实现**：`views/production/plan/index.vue`

### 9.1 列表页

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-PLAN-NAME | `<Input />` | - |
| F-STATUS | `<DictSelect dict-type="dict_plan_status" />` | 字典 |
| BTN-ADD | 按钮 | "新增计划" |
| TBL-PLAN | 表格 | `oa_content_plan` |
| COL-PROGRESS | `<Progress />` | 按任务完成率 |
| BTN-START | 链接 | 草稿 → "启动" |
| BTN-TERMINATE | 链接 | 进行中 → "申请终止" |
| BTN-APPROVE-TERM | 链接 | 终止审批中 → "批准终止"（组长） |
| BTN-REJECT-TERM | 链接 | 终止审批中 → "驳回终止"（组长） |
| BTN-DELETE | 链接 | 草稿 → "删除" |

### 9.2 新增计划弹窗（960px）

```
+----------------------------------------------------------+
| 基本信息                                                  |
| 计划名称 [__]     日期范围 [daterange]                    |
| SOP 模板 [select]  IP 组 [IpGroupTreeSelect]             |
| 关联赛事 [MatchSelectDialog]（外部 API）               |
| 计划描述 [textarea]                                       |
+----------------------------------------------------------+
| SOP 步骤分配（选模板后自动加载节点）                       |
| # | 步骤名称 | 节点类型 | 赛事（多选） | 执行岗位 | 执行人（单选） | 开始 | 结束 |
+----------------------------------------------------------+
| [取消]  [保存草稿]                                        |
+----------------------------------------------------------+
```

| 控件 | 类型 | 字典/实体 | 必填 |
|------|------|----------|------|
| F-PLAN-NAME | `<Input />` max 100 | - | ✅ |
| F-DATE-RANGE | `<DatePicker type="daterange" />` | - | ✅ |
| F-TEMPLATE | `<Select />`（启用模板） | `oa_sop_template` | ✅ |
| F-IP-GROUP | `<IpGroupTreeSelect />` | `oa_ip_group` | ✅ |
| F-COMPETITIONS | `<MatchSelectDialog multiple />` | 外部赛事 `scheduleId` | ✅（≥1） |
| F-DESCRIPTION | `<TextArea />` max 500 | - | ❌ |
| F-STEP-COMPETITION | `<Select multiple />` | 计划赛事池 | ✅（每节点 ≥1） |
| F-STEP-ASSIGNEE | `<UserSelect />`（单选） | `sys_user` | ✅（每节点 1 人） |
| F-STEP-START/END | `<DateTimePicker />` | 默认计划日期 | ❌ |

**联动**：
- 选 SOP 模板 → 拉取节点列表填充步骤表
- 改日期范围 → 未填写的步骤起止时间默认同步

### 9.3 详情抽屉

只读展示：计划名称、模板、IP 组、日期、状态、赛事列表、描述、**生成的任务记录表**（含 scheduled_start/end、executor_role）。

### 9.4 状态与提示

| 状态 | 表现 |
|------|------|
| 保存成功 | Toast "计划已保存为草稿" |
| 启动确认 | 二次确认："启动后关联任务将出现在任务列表" |
| 申请终止 | `prompt` 输入终止原因 |
| 批准终止 | 二次确认（组长） |
| 驳回终止 | 二次确认（组长）；计划回到进行中 |
| 步骤未分配执行人 | 警告 "请为每个 SOP 步骤分配执行人" |

---

## 10. 跨页通用约定

- **关联属性强制选择**：所有 `*_id` 字段必须用选择器
- **字典字段**：状态/类型/平台一律 `<DictSelect />`
- **岗位字段**：`<DictSelect dict-type="dict_position" />`
- **IP 组筛选**：列表页支持 IP 组树形选择器
- **面包屑**：仅 `Layout.vue` 顶栏一处；页面内不再重复 `el-breadcrumb`（2026-06-13）

---

*下一步：API Spec / STATE / SLICES / CHECKLIST / TESTCASES。*
