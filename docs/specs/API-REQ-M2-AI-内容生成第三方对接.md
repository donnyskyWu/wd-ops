# API-REQ-M2-AI-内容生成第三方对接

> **模块**：M2 内容生产 · AI 辅助创作  
> **版本**：v2.0（简版） · 2026-06-26  
> **状态**：Draft

## 文档说明

运营平台根据 **赛事**、**内容类型**、**文档类型** 调用 AI 生成运营文稿。前端经本平台 `POST /admin-api/oa/content/ai-generate` 发起；后端按 M8 提示词模板填充 `{eventinfo}` / `{competitionName}` 后，转发至第三方 **OpenAI 兼容** Chat Completions（`/v1/chat/completions`）。生成结果写入 `oa_production_content.body`，须人工审核（`ai_generated=1`）后发布。流式（SSE）为后续增强，Phase 1 以同步 JSON 为准。

---

## 接口

| 项 | 值 |
|----|-----|
| 本平台路径 | `POST /admin-api/oa/content/ai-generate` |
| 第三方底层 | `POST {apiEndpoint}/v1/chat/completions`（OpenAI 兼容） |
| 鉴权 | Bearer Token（密钥存 M8 `oa_ai_model_config`，AES-256） |
| Content-Type | `application/json` |

---

## 输入参数

| 字段 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| `modelId` | Long | ✅ | M8 已启用 AI 模型 ID | `1` |
| `promptId` | Long | ✅ | M8 提示词 ID（须与下方类型匹配） | `2` |
| `contentType` | String | ✅ | `dict_content_type` | `ARTICLE` |
| `documentType` | String | 条件 | `dict_document_type`；`contentType=ARTICLE` 时必填 | `POST_MATCH_REVIEW` |
| `competitionId` | String | 推荐 | 外部赛事 scheduleId（MatchSelectDialog） | `"20260626001"` |
| `competitionName` | String | 推荐 | 赛事展示名，填充提示词占位符 | `"2026 中超联赛 第 12 轮 上海申花 vs 山东泰山"` |
| `taskId` | Long | ❌ | 任务驱动创作时传入；无 `competitionId` 时从任务解析赛事 | `8801` |
| `platformType` | String | ❌ | 目标平台语气参考，`dict_platform_type` | `WECHAT_MP` |
| `tone` | String | ❌ | 文风：`FORMAL` / `CASUAL` / `HYPE`（供应商自定义枚举） | `CASUAL` |

**枚举**

| 字典 | 取值 |
|------|------|
| `contentType` | `ARTICLE` · `SHORT_VIDEO` · `VIDEO` · `LIVE` |
| `documentType`（仅 ARTICLE） | `SHORT_VIDEO_SCRIPT` · `NEW_ACCOUNT_TRAFFIC` · `POST_MATCH_REVIEW` · `OFFICIAL_PLAN` · `PREHEAT_PREVIEW` |

**赛事解析优先级**：`competitionId` + `competitionName` → 仅 `competitionId`（查任务/计划快照）→ `taskId` 关联任务 → 空字符串。

---

## 输出参数

| 字段 | 类型 | 说明 | 映射 |
|------|------|------|------|
| `content` | String | 生成正文（纯文本或 Markdown） | → `oa_production_content.body` |
| `title` | String | 标题（可候选，取首条） | → `title` |
| `digest` | String | 摘要/导语，≤120 字（公众号 digest） | 前端暂存，发布时写入 |
| `tags` | String[] | 话题/标签（可选） | 前端选用 |
| `eventInfo` | String | 实际注入提示词的赛事文案 | 回显 |
| `mock` | Boolean | `true` 表示模型未连通、返回占位 | — |
| `message` | String | 人类可读状态说明 | — |

第三方若仅返回 Chat Completions 纯文本，本平台将全文写入 `content`，`title` 由前端或二次调用补充。

---

## 错误码

| 码 | 含义 |
|----|------|
| `1400` | 参数不合法（如提示词与 `contentType`/`documentType` 不匹配） |
| `1500` | 模型或提示词不存在 |
| `1501` | 模型或提示词已停用 |
| `1503` | 字典值非法（`contentType` / `documentType`） |
| `1504` | 跨租户访问 |
| `2010` | 内容状态不允许操作（旧 `/generate` 接口） |
| `502` / 超时 | 第三方模型不可用或超时（M8 `timeout` 默认 120s） |

---

## 示例

**Request**

```json
{
  "modelId": 1,
  "promptId": 2,
  "contentType": "ARTICLE",
  "documentType": "POST_MATCH_REVIEW",
  "competitionId": "20260626001",
  "competitionName": "2026 中超联赛 第 12 轮 上海申花 2:1 山东泰山",
  "platformType": "WECHAT_MP",
  "tone": "CASUAL"
}
```

**Response**

```json
{
  "code": 0,
  "data": {
    "content": "【赛后复盘】申花主场逆转泰山……（正文 1500–3000 字）",
    "title": "逆转！申花主场 2:1 击败泰山，保级形势明朗",
    "digest": "申花下半场连进两球逆转泰山，积分榜升至第 6。",
    "tags": ["中超", "申花", "赛后复盘"],
    "eventInfo": "2026 中超联赛 第 12 轮 上海申花 2:1 山东泰山",
    "mock": false,
    "message": "AI 生成完成（模型 qwen-plus）"
  }
}
```

---

## 范围外

- 短视频 AI **成片**（`generated_video_url`，BLK-M2-010 / Phase 2）
- 自动发布、版式 merge、知识库转存（本平台 M2 其他接口）
- 内容审核 SLA、多 endpoint 编排、供应商配额管理细则
