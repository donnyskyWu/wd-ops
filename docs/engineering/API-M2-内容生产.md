# API-M2-内容生产

> **版本**：v1.0 | 2026-06-07
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
  "slaHours": 24
}
```

**字典**：`executorRole` / `reviewerRole` 使用 `dict_position` value。

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
  "slaHours": 24
}
```

**校验**：
- `nodeName` `@NotBlank @Size(max=50)`
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
  "accountId": 123,        // 强关联，ID 而非 name
  "creatorUserId": 456,    // 强关联
  "body": "...",
  "coverImage": "https://...",
  "aiGenerated": 0
}
```

**校验**：
- `title` `@NotBlank @Size(max=200)`
- `contentType` `@InDict(type="dict_content_type")`
- `platformType` `@InDict(type="dict_platform_type")`
- `accountId` `@NotNull`：必须从 `<AccountSelect />` 选，传值时校验 `account.platformType == platformType`
- `creatorUserId` `@NotNull`
- `body` `@NotBlank`

---

### 3.3 PUT `/admin-api/oa/content/update`

同 create + `id`。

---

### 3.4 POST `/admin-api/oa/content/{id}/submit-review`

**业务**：
- 状态：`DRAFT` → `PENDING_FIRST_REVIEW`
- 记录到 `oa_review_record`

---

### 3.5 POST `/admin-api/oa/content/{id}/review`

**请求体** `ContentReviewReq`：

```json
{
  "action": "APPROVE",  // APPROVE / REJECT
  "stage": "FIRST_REVIEW",  // FIRST / SECOND / FINAL
  "comment": "..."
}
```

**校验**：
- `action` `@InDict(type="dict_content_review_result")`
- `stage` `@InDict(type="dict_review_stage")`
- 当前用户 = 该阶段审核人

**业务**：
- 驳回 → `status = REJECTED`（流程结束）
- 通过 → 状态机转移（详见 `STATE-M2-内容生产.md` § 2）
- 终审通过 → 触发 `@Async` 发布

---

### 3.6 DELETE `/admin-api/oa/content/{id}`

**业务**（S-R22-Mike）：
- 仅 `DRAFT` / `REJECTED` 可删除
- 其他状态 → `2010` CONTENT_STATUS_INVALID
- 逻辑删除（`deleted=1`）

---

### 3.7 POST `/admin-api/oa/content/ai-generate`

**请求体**：

```json
{
  "prompt": "写一篇关于夏日防晒的公众号推文",
  "model": "gpt-4"
}
```

**响应**（SSE）：

```
data: {"chunk": "夏日"}
data: {"chunk": "的阳光..."}
...
data: {"done": true}
```

**校验**：
- `model` `@InDict(type="dict_ai_model")`

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
