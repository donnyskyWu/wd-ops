# API-M2-内容生产

> **版本**：v1.4 | 2026-06-14
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
  "finalVideoUrl": null,
  "bodyFormat": "PLAIN",
  "layoutJson": null,
  "layoutHtml": null,
  "layoutTemplateId": null
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
- `bodyFormat` `@InDict(type="dict_content_body_format")`；默认 `PLAIN`
- `layoutJson` / `layoutHtml`：当 `bodyFormat=LAYOUT` 时 `layoutJson` 必填；服务端同步生成/校验 `layoutHtml`
- `layoutTemplateId` 可选；若传则校验模板存在且类型匹配（**2011**）

### 3.2.6 POST `/admin-api/oa/content/{id}/apply-layout-template`（草案 · S-14）

**请求体** `ContentApplyLayoutTemplateReq`：

```json
{
  "layoutTemplateId": 501,
  "overwrite": false
}
```

**业务**：
- 校验 `contentType=ARTICLE`
- 校验模板 `status=ENABLED` 且 document_type 匹配（ADR-019 §2.3）
- 若内容已有 `bodyFormat=LAYOUT` 且 `overwrite=false` → **2012**
- 复制模板 `layoutJson`/`layoutHtml` → 内容；设置 `layoutTemplateId`；`bodyFormat=LAYOUT`

**响应**：更新后的 `ContentRespVO`

---

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
- 按 `review-config` 决定目标状态：`PENDING_FIRST_REVIEW` / `PENDING_SECOND_REVIEW` / `PENDING_PUBLISH`
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
- 驳回 → `REJECTED`；通过 → 按配置进入下一级或 `PENDING_PUBLISH`

---

### 3.5.1 GET `/admin-api/oa/content/{id}/publish-options`（✅ ADR-022）

**权限**：`oa:content:publish`

**业务**：内容状态须为 `PENDING_PUBLISH`；返回按平台分组的、已配置 `publish_enabled=1` 的 NORMAL 账号。

---

### 3.5.2 POST `/admin-api/oa/content/{id}/publish`（✅ ADR-022）

**权限**：`oa:content:publish`

**请求体** `ContentPublishReq`：

```json
{
  "platformType": "DOUYIN",
  "accountIds": [9006, 9007]
}
```

**业务**：
- 校验账号 tenant / 平台 / 发布权限
- 逐账号调用 `PlatformPublishAdapter`（Phase 2：`DevStubPlatformPublishAdapter` mock）
- 全部成功 → `PUBLISHED`，写入 `oa_content_publish_record`

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

## 6. 公推模板库 API（FR-M2-005 · 草案 · S-14）

> **路径前缀**：`/admin-api/oa/layout-template/*`  
> **存储格式**：见 [`ADR-019`](../adr/ADR-019-M2-公推模板库存储与导入.md)

### 6.1 GET `/admin-api/oa/layout-template/list`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateName | String | ❌ | 模糊 |
| documentType | String | ❌ | `dict_document_type`；传 `__GENERAL__` 筛「通用（空）」 |
| status | String | ❌ | `dict_layout_template_status` |
| sourceType | String | ❌ | `dict_layout_template_source` |
| pageNum / pageSize | Integer | ❌ | 默认 20 |

**响应**：`PageResult<LayoutTemplateVO>`

```json
{
  "id": 501,
  "templateName": "赛后复盘标准版式",
  "contentType": "ARTICLE",
  "documentType": "POST_MATCH_REVIEW",
  "description": "...",
  "sourceType": "MANUAL",
  "sourceUrl": null,
  "status": "ENABLED",
  "thumbnailUrl": "https://...",
  "creatorUserId": 1003,
  "creatorName": "张三",
  "updatedAt": "2026-06-14T10:00:00+08:00"
}
```

**说明**：列表 **不** 返回完整 `layoutJson`（过大）；详情接口返回。

---

### 6.2 GET `/admin-api/oa/layout-template/{id}`

**响应** `LayoutTemplateDetailVO`：含 `layoutJson`、`layoutHtml`（只读冗余）。

---

### 6.3 GET `/admin-api/oa/layout-template/select-list`

**用途**：内容创作页模板选择器（轻量列表）。

| 参数 | 类型 | 必填 |
|------|------|------|
| documentType | String | ❌ | 当前内容 documentType；服务端按 ADR-019 匹配规则过滤 |
| contentType | String | ✅ | 固定 `ARTICLE` |

**响应**：`List<LayoutTemplateSelectVO>`（id、templateName、documentType、thumbnailUrl）

---

### 6.4 POST `/admin-api/oa/layout-template/create`

**请求体** `LayoutTemplateCreateReq`：

```json
{
  "templateName": "标准引流版式",
  "description": "...",
  "documentType": null,
  "layoutJson": { "version": 1, "blocks": [] },
  "status": "ENABLED"
}
```

**校验**：
- `templateName` `@NotBlank @Size(max=100)`
- `contentType` 服务端固定 `ARTICLE`
- `documentType` 可空；非空则 `@InDict(dict_document_type)`
- `layoutJson` `@NotNull`；schema 校验（**2013** 非法块结构）
- `status` `@InDict(dict_layout_template_status)`

**业务**：服务端 `layoutHtml = renderAndSanitize(layoutJson)`

---

### 6.5 PUT `/admin-api/oa/layout-template/update`

同 create + `id`；已停用模板可编辑。

---

### 6.6 DELETE `/admin-api/oa/layout-template/{id}`

- 逻辑删除；若被 `oa_production_content.layout_template_id` 引用 → **仍允许删除**（内容保留快照）
- 无引用要求

---

### 6.7 POST `/admin-api/oa/layout-template/import-url`（BLK-M2-012）

**请求体**：

```json
{
  "sourceUrl": "https://mp.weixin.qq.com/s/...",
  "templateName": "导入-竞品排版",
  "documentType": null
}
```

**响应**：

```json
{
  "jobId": 9001,
  "status": "PENDING"
}
```

**异步 Job 完成**：`GET /layout-template/import-job/{jobId}` → `SUCCESS` 时含 `layoutJson` 预览 + 建议 `templateName`

**失败**：`FAILED` + `errorCode`（**2014** URL 不可抓取）

---

### 6.8 POST `/admin-api/oa/layout-template/import-docx`（BLK-M2-013/014）

**请求**：`multipart/form-data`

| 字段 | 类型 | 必填 |
|------|------|------|
| file | File | ✅ `.docx` |
| templateName | String | ❌ |
| documentType | String | ❌ |

**响应**：同 import-url（异步 Job）

---

### 6.9 POST `/admin-api/oa/layout-template/import-paste`（Fallback）

**请求体**：

```json
{
  "templateName": "粘贴导入",
  "documentType": null,
  "html": "<section>...</section>"
}
```

**业务**：HTML → parse → `layoutJson` + `layoutHtml`；同步返回详情（无异步）

---

### 6.10 GET `/admin-api/oa/layout-template/import-job/{jobId}`

**响应** `LayoutImportJobVO`：

```json
{
  "id": 9001,
  "status": "SUCCESS",
  "sourceType": "URL",
  "sourceUrl": "https://...",
  "previewLayoutJson": { "version": 1, "blocks": [] },
  "errorMessage": null
}
```

**字典** `status`：`PENDING` / `RUNNING` / `SUCCESS` / `FAILED`

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
| 2010 | 内容状态不允许删除 |
| **2011** | 版式模板不存在或类型不匹配（FR-M2-005） |
| **2012** | 内容已有版式且未确认覆盖 |
| **2013** | `layoutJson` schema 校验失败 |
| **2014** | 公众号 URL 抓取/解析失败（BLK-M2-012） |
| **2015** | Word 导入解析失败 |
| **2016** | 导入 Job 不存在或已过期 |

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
| `layoutTemplateId` | `<LayoutTemplateSelect />` | 公推版式模板 | 1501 / 2011 |
