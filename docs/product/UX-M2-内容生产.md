# UX-M2-内容生产

> **版本**：v1.0 | 2026-06-07
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
| P-M2-003 | 任务列表 | `/prod/task` | FR-M2-002 |
| P-M2-004 | 任务详情 | `/prod/task/:id` | FR-M2-002 |
| P-M2-005 | 我的任务 | `/prod/task/mine` | FR-M2-002 |
| P-M2-006 | 内容列表 | `/prod/content` | FR-M2-003 |
| P-M2-007 | 内容创作/编辑 | `/prod/content/edit` | FR-M2-003 |
| P-M2-008 | 内容审核 | `/prod/content/review` | FR-M2-003 |
| P-M2-009 | 知识库列表 | `/prod/knowledge` | FR-M2-004 |
| P-M2-010 | 知识详情 | `/prod/knowledge/:id` | FR-M2-004 |

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

### 3.1 布局

```
+----------------------------------------------------------+
| [返回] 模板名称 [__]  [保存]  [启用]                      |
+----------------------------------------------------------+
| 左侧：节点列表（拖入）  |  中间：DAG 画布                |
| - 添加节点             |  [节点1]                        |
| - 编辑节点             |     ↓                           |
| - 删除节点             |  [节点2] [节点3] [节点4]  ←并行|
|                        |     ↓ ↓ ↓                       |
|                        |  [节点5]                        |
+----------------------------------------------------------+
```

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

### 3.3 状态

| 状态 | 表现 |
|------|------|
| 保存失败（DAG 环） | 红色 Banner："节点 X 与 Y 形成环" |
| 保存成功 | Toast "保存成功" + 画布刷新 |
| 节点无 `executor_role` | "请选择执行岗位" |

---

## 4. P-M2-003/004 任务列表/详情

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-IP | `<IpGroupTreeSelect />` | `oa_ip_group` |
| F-STATUS | `<DictSelect dict-type="dict_sop_node_status" />` | 字典 |
| F-EXECUTOR | `<UserSelect />` | `sys_user` |
| TBL-TASK | 表格 | `oa_task` |
| BTN-MY-TASKS | 按钮 | "我的任务"（快速过滤） |

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

## 5. P-M2-006/007 内容列表/编辑

### 5.1 内容列表

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-TITLE | `<Input />` | - |
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` | 字典 |
| F-CONTENT-TYPE | `<DictSelect dict-type="dict_content_type" />` | 字典 |
| F-ACCOUNT | `<AccountSelect />`（联动 platform） | `oa_account` |
| F-STATUS | `<DictSelect dict-type="dict_content_status" />` | 字典 |
| F-AI | `<DictSelect dict-type="dict_yes_no" />` | 字典（是否 AI 生成） |
| TBL-CONTENT | 表格 | `oa_content` |

### 5.2 内容编辑

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-TITLE | `<Input />` | - |
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` | 字典（决定 F-ACCOUNT 候选） |
| F-CONTENT-TYPE | `<DictSelect dict-type="dict_content_type" />` | 字典 |
| F-ACCOUNT | `<AccountSelect :platformType="F-PLATFORM" />` | `oa_account` |
| F-COVER | `<ImageUploader />` | - |
| F-BODY | `<RichTextEditor />` | - |
| F-AI | `<Switch />` + 弹窗"AI 辅助创作" | - |
| BTN-SUBMIT | 按钮 | "提交审核" |

### 5.3 AI 辅助创作弹窗

- 输入框（提示词）
- 选择模型（`<DictSelect dict-type="dict_ai_model" />`，v1.0 固定 GPT-4）
- 按钮"生成" → 流式输出 → 用户采纳/拒绝

---

## 6. P-M2-008 内容审核

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-STAGE | `<DictSelect dict-type="dict_review_stage" />` | 字典（初/复/终审） |
| TBL-REVIEW | 表格 | `oa_content` + `oa_review_record` |
| BTN-APPROVE | 按钮 | "通过" |
| BTN-REJECT | 按钮 | "驳回" |
| BTN-COMMENT | 弹窗 | "评论" |

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

## 8. 跨页通用约定

- **关联属性强制选择**：所有 `*_id` 字段必须用选择器
- **字典字段**：状态/类型/平台一律 `<DictSelect />`
- **岗位字段**：`<DictSelect dict-type="dict_position" />`
- **IP 组筛选**：列表页支持 IP 组树形选择器
- **空/错/加载**：三态完整覆盖

---

*下一步：API Spec / STATE / SLICES / CHECKLIST / TESTCASES。*
