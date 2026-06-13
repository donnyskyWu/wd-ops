# API-M2-内容生产

> **版本**：v1.3 | 2026-06-13
> **关联 PRD**：[`PRD-M2-内容生产.md`](../product/PRD-M2-内容生产.md)
> **关联 UX**：[`UX-M2-内容生产.md`](../product/UX-M2-内容生产.md)
> **关联全局规范**：[`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md)

---

## 1. SOP 模板 API

### 1.1 GET `/admin-api/oa/sop/template/list`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateName | String | ❌ | 模糊匹配 |
| contentType | String | ❌ | `dict_content_type` |
| platformType | String | ❌ | `dict_platform_type` |
| status | Integer | ❌ | 0/1 |
| pageNum | Integer | ❌ | 默认 1 |
| pageSize | Integer | ❌ | 默认 20 |

**响应**：`CommonResult<PageResult<SopTemplateVO>>`

**字段**：

```json
{
  "id": 1,
  "templateName": "标准内容生产运营流程",
  "contentType": "ALL",
  "platformType": "ALL",
  "description": "...",
  "status": 1,
  "nodeCount": 14,
  "createdAt": "2026-06-01T10:00:00+08:00"
}
```

**字典**：`contentType` / `platformType` 使用字典 value。

---

### 1.2 POST `/admin-api/oa/sop/template/create`

**请求体** `SopTemplateCreateReq`：

```json
{
  "templateName": "...",
  "contentType": "SHORT_VIDEO",
  "platformType": "DOUYIN",
  "description": "...",
  "status": 1
}
```

**校验**：
- `templateName` `@NotBlank @Size(max=100)`
- `contentType` `@InDict(type="dict_content_type")`
- `platformType` `@InDict(type="dict_platform_type")`
- `description` `@Size(max=500)`

---

### 1.3 PUT `/admin-api/oa/sop/template/update`

**请求体** `SopTemplateUpdateReq`：包含 `id` + 同 create 字段。

---

### 1.4 DELETE `/admin-api/oa/sop/template/{id}`

**响应**：`true`

**业务规则**：
- 若模板被任务引用 → 拒绝删除（错误码 1502）
- 软删除（`status=0`），保留 90 天后物理删除

---

### 1.5 GET `/admin-api/oa/sop/node/list?templateId=xxx`

**响应**：`List<SopNodeVO>`

**字段**：

```json
{
  "id": 1,
  "templateId": 1,
  "nodeName": "写推文",
  "nodeOrder": 2,
  "executorRole": "OPS_OFFICIAL",
  "needReview": 1,
  "reviewerRole": "OPS_LEADER",
  "predecessors": [1],
  "parallelGroup": "GROUP_A",
  "slaHours": 24,
  "nodeType": "CONTENT_GENERATION"
}
```

**字典**：`executorRole` / `reviewerRole` 使用 `dict_position` value；`nodeType` 使用 `dict_sop_node_type`（ADR-016：CONTENT_GENERATION / CONTENT_PUBLISH / NORMAL）。

---

### 1.6 POST `/admin-api/oa/sop/node/create`

**请求体** `SopNodeCreateReq`：

```json
{
  "templateId": 1,
  "nodeName": "写推文",
  "nodeOrder": 2,
  "executorRole": "OPS_OFFICIAL",
  "needReview": 1,
  "reviewerRole": "OPS_LEADER",
  "predecessors": [1],
  "parallelGroup": "GROUP_A",
  "slaHours": 24,
  "nodeType": "NORMAL"
}
```

**校验**：
- `nodeName` `@NotBlank @Size(max=50)`
- `nodeType` `@NotBlank @InDict(type="dict_sop_node_type")`
- `executorRole` `@InDict(type="dict_position")`
- `needReview=1` → `reviewerRole` 必填
- `predecessors` → 同模板节点 ID 列表
- `slaHours` ≥ 0
- DAG 合法性（调用 `validate-dag`）

---

### 1.7 PUT `/admin-api/oa/sop/node/update`

同 create 字段 + `id`。

---

### 1.8 POST `/admin-api/oa/sop/node/validate-dag`

**请求体**：

```json
{
  "templateId": 1,
  "nodes": [
    {"id": 1, "predecessors": []},
    {"id": 2, "predecessors": [1]},
    ...
  ]
}
```

**响应**：

```json
{
  "valid": true
}
```

或

```json
{
  "valid": false,
  "cyclePath": [1, 2, 3, 1]
}
```

---

### 1.9 GET `/admin-api/oa/sop/review/pending`

**请求参数**：

| 参数 | 类型 | 必填 |
|------|------|------|
| reviewerId | Long | ❌（默认当前用户） |

**响应**：`List<SopReviewVO>`

---

### 1.10 POST `/admin-api/oa/sop/review/approve`

**请求体**：

```json
{
  "reviewId": 123,
  "comment": "审核通过"
}
```

**业务**：
- 校验当前用户 `position` = `reviewer_role`
- 状态：`PENDING` → `APPROVED`
- 触发后续节点（重新 DAG 计算）

---

### 1.11 POST `/admin-api/oa/sop/review/reject`

**请求体**：

```json
{
  "reviewId": 123,
  "comment": "内容需修改"
}
```

**业务**：
- 状态：`PENDING` → `REJECTED`
- 任务回到 `IN_PROGRESS`

---

## 2. 任务管理 API

### 2.1 GET `/admin-api/oa/task/list`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ipGroupId | Long | ❌ | IP 组筛选 |
| status | String | ❌ | `dict_sop_node_status` |
| executorId | Long | ❌ | 执行人 |
| startDate | Date | ❌ | 创建时间 |
| endDate | Date | ❌ | - |
| pageNum | Integer | ❌ | - |
| pageSize | Integer | ❌ | - |

**响应**：`PageResult<TaskVO>`

**字段**：

```json
{
  "id": 1,
  "planName": "6月娱乐八卦计划",
  "nodeName": "写推文",
  "assigneeName": "张三",
  "executorRole": "OPS_OFFICIAL",
  "status": "IN_PROGRESS",
  "slaDeadline": "2026-06-08T18:00:00+08:00"
}
```

**字典**：`executorRole` / `status` 使用字典 value。

---

### 2.2 POST `/admin-api/oa/task/{id}/start`

**业务**：
- 校验当前用户 = `assignee_id`
- 状态：`PENDING` → `IN_PROGRESS`

---

### 2.3 POST `/admin-api/oa/task/{id}/complete`

**请求体**：

```json
{
  "deliverables": "推文草稿 URL"
}
```

**业务**：
- 校验当前用户 = `assignee_id`
- 状态：`IN_PROGRESS` → `COMPLETED`
- 若 `need_review=1` → 提交审核

---

### 2.4 POST `/admin-api/oa/task/{id}/submit-review`

**业务**：
- 状态：`COMPLETED` → `PENDING_REVIEW`
- 创建 `oa_sop_review` 记录

---

### 2.5 GET `/admin-api/oa/task/my-tasks`

**业务**：
- 默认查询当前用户被分配的任务
- 包含超时任务（标红）

---

### 2.6 GET `/admin-api/oa/task/{id}/execute`（需求 4–5，✅ S-12）

**响应** `TaskExecuteVO`：

```json
{
  "id": 1,
  "nodeName": "撰写短视频文案",
  "nodeType": "CONTENT_GENERATION",
  "planName": "6月内容计划",
  "competitionId": "cmp-001",
  "competitionName": "2026 春季城市赛",
  "executionInstruction": "...",
  "attachments": [],
  "linkedContent": { "id": 100, "title": "...", "status": "DRAFT" }
}
```

- `executionInstruction` 来源 `oa_sop_node.instruction_text`（空则回退 `nodeName`）；`attachments` 来源同表 `attachment_urls` JSON 只读，**无上传 API**（BLK-M2-007 上传仍阻塞）。
- `linkedContent`：内容生成节点关联的 `oa_content`（0..1）。

### 2.7 POST `/admin-api/oa/task/{id}/execute/save`（✅ S-12）

保存执行页草稿（交付说明等，字段待 BLK 定稿）。

### 2.8 POST `/admin-api/oa/task/{id}/execute/complete`（需求 5，✅ S-12）

**业务**：
- 校验当前用户 = `assignee_id`
- `nodeType=CONTENT_GENERATION` → 须 `linkedContent.status=COMPLETED`，否则 **2008**
- 状态：`IN_PROGRESS` → `DONE`（`need_review=0`）或 `PENDING_REVIEW`（`need_review=1`）

---

## 3. 内容管理 API

### 3.1 GET `/admin-api/oa/content/list`

**请求参数**：

| 参数 | 类型 | 字典 |
|------|------|------|
| title | String | - |
| platformType | String | `dict_platform_type` |
| contentType | String | `dict_content_type` |
| accountId | Long | `oa_account` |
| status | String | `dict_content_status` |
| aiGenerated | Integer | `dict_yes_no` |
| pageNum / pageSize | Integer | - |

---

### 3.2 POST `/admin-api/oa/content/create`

**请求体** `ContentCreateReq`：

```json
{
  "title": "...",
  "contentType": "SHORT_VIDEO",
  "platformType": "DOUYIN",
  "platformTypes": ["DOUYIN", "KUAISHOU"],
  "accountId": 123,
  "accountIds": [123, 456],
  "creatorUserId": 456,
  "body": "...",
  "aiGenerated": 0,
  "taskId": null,
  "competitionId": "123456789",
  "competitionName": "英超-曼联 VS 切尔西",
  "documentType": null,
  "ipGroupId": 9001,
  "finalVideoUrl": null
}
```

**校验**：
- `title` `@NotBlank @Size(max=200)`
- `contentType` `@InDict(type="dict_content_type")`
- `platformType` / `platformTypes` 可选；若传 `accountId(s)` 则校验平台匹配（2006）
- `accountId` / `accountIds` **可选**（任务场景可不填）
- `creatorUserId` `@NotNull`
- `ipGroupId` `@NotNull`：独立创作须为用户所属 IP 组；任务场景继承任务 IP 组
- `body` LONGTEXT；`contentType=SHORT_VIDEO` 时可为空（视频 URL 为准）
- `coverImage` **UI 已移除**；库字段保留，可不传
- `taskId` 非空时：同一 `taskId` 仅允许 1 条内容（1502）
- `documentType`：`contentType=ARTICLE` 时 `@NotBlank @InDict(type="dict_document_type")`

### 3.2.1 GET `/admin-api/oa/content/by-task?taskId=`（✅ S-12）

返回任务关联内容（0..1），供执行页与编辑页预加载。

### 3.2.2 GET `/admin-api/oa/content/script-ref?competitionId=`（✅ S-13）

返回同赛事 `documentType=SHORT_VIDEO_SCRIPT` 且 `status=COMPLETED` 的文档正文（供短视频引用）。

### 3.2.3 POST `/admin-api/oa/content/{id}/confirm`（遗留）

**业务**：`DRAFT` → `COMPLETED`（**不推荐**；新流程用 `submit-review`，ADR-017）。

---

### 3.2.4 GET `/admin-api/oa/content/review-config`（✅ ADR-017）

**响应** `ContentReviewConfigVO`：

```json
{
  "level1Enabled": true,
  "level2Enabled": true,
  "level1Role": "OPS_LEADER",
  "level2Role": "DEPT_HEAD"
}
```

---

### 3.2.5 GET `/admin-api/oa/content/ai-prompt-options`（✅ 2026-06-13）

按 `contentType` / `documentType` 返回可选 M8 提示词列表（AI 弹窗）。

---

### 3.3 PUT `/admin-api/oa/content/update`

同 create + `id`。

---

### 3.4 POST `/admin-api/oa/content/{id}/submit-review`

**业务**（ADR-017）：
- 校验 `ipGroupId`、内容完整性
- 按 `review-config` 决定目标状态：`PENDING_FIRST_REVIEW` / `PENDING_SECOND_REVIEW` / `PUBLISHED`
- 记录 `oa_review_record`；返回审核流程 steps（含角色+可审用户）

---

### 3.5 POST `/admin-api/oa/content/{id}/review`

**请求体** `ContentReviewReq`：

```json
{
  "action": "APPROVE",
  "stage": "FIRST_REVIEW",
  "comment": "..."
}
```

**stage**：`FIRST_REVIEW` | `SECOND_REVIEW`（默认配置）；`FINAL_REVIEW` 仅遗留数据。

**校验**：
- 当前用户满足 ADR-017 权限（含 IP 组长范围）
- 驳回 → `REJECTED`；通过 → 按配置进入下一级或 `PUBLISHED`

---

### 3.6 DELETE `/admin-api/oa/content/{id}`

**业务**（S-R22-Mike）：
- 仅 `DRAFT` / `REJECTED` 可删除
- 其他状态 → `2010` CONTENT_STATUS_INVALID
- 逻辑删除（`deleted=1`）

---

### 3.7 POST `/admin-api/oa/content/ai-generate`（✅ 真实 LLM）

**请求体** `ContentAiGenerateReq`：

```json
{
  "contentId": 100,
  "modelId": 1,
  "promptId": 2,
  "competitionId": "123456789",
  "contentType": "ARTICLE",
  "documentType": "SHORT_VIDEO_SCRIPT"
}
```

- `modelId` → M8 `oa_ai_model_config` 已启用记录
- `promptId` → M8 提示词（含 `content_type` / `document_type` 字段，V69）
- 后端 HTTP 调 LLM，写入 `body` 或视频 URL（短视频 BLK-M2-010 部分占位）

**响应**：`ContentAiGenerateResultVO`（生成文本/URL）

---

## 4. 知识库 API

### 4.1 GET `/admin-api/oa/knowledge/list`

**请求参数**：

| 参数 | 类型 | 必填 | 字典 |
|------|------|------|------|
| title | String | ❌ | - |
| category | String | ❌ | 固定枚举 |
| tag | String | ❌ | - |
| isPublic | Integer | ❌ | `dict_yes_no` |
| pageNum | Integer | ❌ | - |

---

### 4.2 POST `/admin-api/oa/knowledge/create`

**请求体**：

```json
{
  "title": "...",
  "category": "TEMPLATE_LIB",
  "content": "...",
  "tags": ["运营", "SOP"],
  "isPublic": 1
}
```

**校验**：
- `title` `@NotBlank @Size(max=100)`
- `category` `@InDict(type="dict_knowledge_category")`（**v1.0 用固定值，v2.0 改字典**）
- `isPublic` `@InDict(type="dict_yes_no")`

---

### 4.3 PUT `/admin-api/oa/knowledge/update`

---

### 4.4 GET `/admin-api/oa/knowledge/search?keyword=xxx`

**业务**：
- ES/全文搜索（v1.0 简化为 MySQL LIKE）

---

## 5. 错误码

| 错误码 | 含义 |
|--------|------|
| 1500 | 关联实体不存在 |
| 1501 | 关联实体已停用/注销 |
| 1502 | 关联实体已被引用 |
| 1503 | 字典值不合法 |
| 1504 | 跨租户访问禁止 |
| 2001 | DAG 存在环 |
| 2002 | 节点 `executor_role` 缺失 |
| 2003 | `need_review=1` 但 `reviewer_role` 缺失 |
| 2004 | 前置节点未完成 |
| 2005 | 模板无节点，无法启用 |
| 2006 | 账号平台类型与内容平台类型不匹配 |
| 2007 | 审核人岗位不匹配 |
| 2008 | 内容生成节点完成门禁：无关联内容或内容未 COMPLETED |

---

*下一步：STATE / SLICES / CHECKLIST / TESTCASES。*

## 强关联字段 → 选择器映射（🔴 API 必含）

本模块涉及以下强关联字段，**前端必须使用对应选择器组件**：

| 字段 | 选择器组件 | 关联实体 | 错误码 |
|------|----------|---------|--------|
| `authorId` | `<RealNameSelect />` | 作者 | 1501 / 1504 |
| `accountId` | `<AccountSelect />` | 发布平台账号 | 1501 / 1504 |
| `platform` | `<DictSelect dict-type="dict_platform_type" />` | 平台字典 | 1503 |
| `reviewStage` | `<DictSelect dict-type="dict_review_stage" />` | 审核阶段 | 1503 |
