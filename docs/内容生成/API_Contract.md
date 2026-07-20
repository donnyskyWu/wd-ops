# API 接口契约 - AI 内容生成服务

| 字段 | 值 |
|------|------|
| **服务名称** | AI 内容生成服务（shenyu-ai-content） |
| **版本** | v1.0 |
| **协议** | HTTPS + JSON |
| **认证方式** | Bearer Token（Header: `Authorization: Bearer <token>`） |
| **Base URL** | `https://{gateway-host}/api/v1` |

---

## 接口总览

| 编号 | 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|------|
| API-01 | AI 内容生成 | POST | `/ai-content/generate` | 调用 AI Agent 生成赛事方案内容（支持多轮对话） |
| API-02 | 获取历史偏好总结 | GET | `/ai-content/preference-summary` | 获取用户最近一次对话的偏好总结 |
| API-03 | 更新偏好总结 | PUT | `/ai-content/preference-summary` | 用户手动修改偏好总结后保存 |
| API-04 | 获取可用模型列表 | GET | `/ai-content/models` | 获取当前可用的 AI 模型列表 |
| API-05 | 采纳方案 | POST | `/ai-content/adopt` | 记录方案采纳行为，用于数据统计 |

---

## 通用约定

### 认证

所有接口请求 Header 需携带：

```
Authorization: Bearer <user_token>
Content-Type: application/json
```

### 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "trace_id": "req_abc123def456"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务状态码，`0` 表示成功，非 `0` 为错误码 |
| `message` | string | 提示信息 |
| `data` | object/null | 业务数据，失败时为 `null` |
| `trace_id` | string | 请求追踪 ID，用于问题排查 |

### 错误码

| code | 含义 | 处理建议 |
|------|------|----------|
| 0 | 成功 | — |
| 40001 | 参数校验失败 | 检查请求体字段 |
| 40002 | Token 无效或过期 | 引导用户重新登录 |
| 40003 | 对话轮次已达上限 | 提示用户采纳或重新生成 |
| 40004 | 会话 ID 不存在或已过期 | 重新开始新对话 |
| 40005 | 模型不可用 | 切换其他模型 |
| 42001 | 请求频率超限（限流） | 稍后重试，单用户 ≤3 次/分钟 |
| 50001 | AI 服务调用失败 | 提示用户重试或切换模型 |
| 50002 | 生成超时 | 提示用户重试 |
| 50003 | 内容审核未通过 | 提示用户修改输入后重试 |

---

## API-01：AI 内容生成

调用 AI Agent 生成赛事方案内容，支持初始生成和多轮对话修改。

**注意：本接口为同步调用（非流式输出），每次请求需完整携带上下文参数、历史对话记录和用户偏好总结。**

### 基本信息

| 项 | 值 |
|---|---|
| **路径** | `POST /api/v1/ai-content/generate` |
| **鉴权** | 需要 |
| **超时** | 初始生成 30s，修改回复 15s |
| **限流** | 单用户 3 次/分钟 |
| **输出方式** | **同步返回（非流式）**，完整内容一次性返回 |

### 请求体

```json
{
  "session_id": "sess_20250628_abc123",
  "model": "qwen",
  "message": "帮我生成一份胜平负分析方案，侧重近期状态",
  "context": {
    "match_name": "澳首超-坎培拉奥林匹克 VS 堪培拉白头鹰",
    "author_name": "张三",
    "scheme_type": "胜平负分析",
    "history_record": "近10场7胜2平1负，主场胜率75%",
    "anchor_style": "comprehensive",
    "product_description": "面向新手彩民的赛事推荐产品，核心卖点是数据驱动、通俗易懂"
  },
  "preference_summary": "上次用户偏好简洁直接的语言风格，侧重胜平负分析方向，期望篇幅中等（500-800字），修改时倾向于增加数据对比和风险提示。",
  "conversation_history": [
    {
      "role": "user",
      "content": "帮我生成一份胜平负分析方案"
    },
    {
      "role": "assistant",
      "content": "## 澳首超 赛事方案\n..."
    }
  ],
  "round_count": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `session_id` | string | 是 | 会话 ID，首次调用由前端生成（UUID），后续对话复用同一 ID |
| `model` | string | 是 | AI 模型标识，可选值：`qwen`、`deepseek`、`glm`、`kimi` |
| `message` | string | 是 | 用户当前输入的文本内容 |
| `context` | object | **是，每次必传** | 上下文参数（见下表），**每次调用都必须完整携带** |
| `preference_summary` | string | **是，每次必传** | 历史用户偏好总结。首次使用无历史偏好时可传空字符串 `""`，后续每次调用都必须携带 |
| `conversation_history` | array | **是，每次必传** | 上次及之前的对话内容，用于多轮对话上下文。首次生成时传空数组 `[]`，后续每次调用都必须携带完整历史 |
| `round_count` | int | 否 | 当前对话轮次计数，默认 1。超过 10 轮后端将拒绝请求 |

#### context 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `match_name` | string | **是，每次必传** | 赛事名称（如："澳首超-坎培拉奥林匹克 VS 堪培拉白头鹰"） |
| `author_name` | string | **是，每次必传** | 作者/主播名称 |
| `scheme_type` | string | 是 | 方案类型，如"胜平负分析"、"让球分析"等 |
| `history_record` | string | 否 | 历史战绩描述 |
| `anchor_style` | string | 否 | 主播风格标识：`aggressive`（激进型）、`conservative`（稳健型）、`data`（数据型）、`emotional`（情感型）、`comprehensive`（综合分析型） |
| `product_description` | string | 否 | 产品定义说明，描述产品定位、目标用户、核心卖点等 |

### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "session_id": "sess_20250628_abc123",
    "content": "## 澳首超 赛事方案\n\n### 核心推荐：胜\n\n| 选项 | 赔率 | 信心指数 |\n|------|------|----------|\n| 胜   | 2.10 | ★★★★☆  |\n| 平   | 3.40 | ★★☆☆☆  |\n| 负   | 3.20 | ★★☆☆☆  |\n\n**分析要点：**\n坎培拉奥林匹克近期状态出色，主场胜率高达 75%...",
    "model": "qwen",
    "round_count": 1,
    "tokens_used": 1280,
    "generated_at": "2026-07-11T14:30:00+08:00"
  },
  "trace_id": "req_ai_001"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `session_id` | string | 会话 ID，与请求一致 |
| `content` | string | AI 生成的完整内容，Markdown 格式（**同步一次性返回，非流式**） |
| `model` | string | 实际使用的模型标识 |
| `round_count` | int | 更新后的轮次计数 |
| `tokens_used` | int | 本次调用消耗的 Token 数量（用于计费统计） |
| `generated_at` | string | 生成时间，ISO 8601 格式 |

### 错误响应示例

```json
{
  "code": 40003,
  "message": "对话轮次已达上限（10/10），请采纳当前方案或重新开始",
  "data": null,
  "trace_id": "req_ai_err_001"
}
```

---

## API-02：获取历史偏好总结

获取当前用户最近一次成功完成的对话会话的偏好总结。

### 基本信息

| 项 | 值 |
|---|---|
| **路径** | `GET /api/v1/ai-content/preference-summary` |
| **鉴权** | 需要 |
| **超时** | 5s |

### 查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `author_id` | string | 否 | 指定作者 ID，可按作者维度查询偏好。不传则返回当前用户的全局偏好 |

### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "summary_text": "上次对话中，您偏好简洁直接的语言风格，侧重胜平负分析方向，期望篇幅中等（500-800字），修改时倾向于增加数据对比和风险提示。",
    "dimensions": {
      "language_style": {
        "value": "简洁直接",
        "confidence": 0.85,
        "source_round": 3
      },
      "prediction_direction": {
        "value": "偏好胜平负分析",
        "confidence": 0.92,
        "source_round": 2
      },
      "length_preference": {
        "value": "中等篇幅（500-800字）",
        "confidence": 0.78,
        "source_round": 4
      },
      "modification_tendency": {
        "value": "喜欢增加数据对比，要求更激进的角度，强调风险提示",
        "confidence": 0.88,
        "source_round": 5
      }
    },
    "source_session_id": "sess_20250625_xyz789",
    "generated_at": "2026-07-10T18:20:00+08:00",
    "is_updated_by_user": false
  },
  "trace_id": "req_pref_001"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `summary_text` | string | 完整的偏好总结文本（面向用户展示） |
| `dimensions` | object | 结构化偏好维度详情（见下表） |
| `source_session_id` | string | 偏好总结来源的会话 ID |
| `generated_at` | string | 偏好总结生成时间 |
| `is_updated_by_user` | bool | 该偏好是否曾被用户手动修改过 |

#### dimensions 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `language_style` | object | 语言风格偏好 |
| `prediction_direction` | object | 赛事预测方向偏好 |
| `length_preference` | object | 字数/篇幅偏好 |
| `modification_tendency` | object | 内容修改倾向 |

每个维度对象包含：
- `value`（string）：偏好值
- `confidence`（float, 0~1）：AI 提取的置信度
- `source_round`（int）：该偏好首次被识别的对话轮次

### 无历史记录时的响应

```json
{
  "code": 0,
  "message": "success",
  "data": null,
  "trace_id": "req_pref_002"
}
```

前端收到 `data: null` 时，不展示偏好总结面板。

---

## API-03：更新偏好总结

用户手动修改偏好总结后，保存到后端。

### 基本信息

| 项 | 值 |
|---|---|
| **路径** | `PUT /api/v1/ai-content/preference-summary` |
| **鉴权** | 需要 |
| **超时** | 3s |

### 请求体

```json
{
  "summary_text": "我偏好简洁直接的分析风格，侧重胜平负和让球分析，篇幅控制在500字以内，喜欢用数据对比的方式展示。",
  "dimensions": {
    "language_style": { "value": "简洁直接" },
    "prediction_direction": { "value": "胜平负分析和让球分析" },
    "length_preference": { "value": "简短精炼（500字以内）" },
    "modification_tendency": { "value": "喜欢数据对比展示" }
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `summary_text` | string | 是 | 用户修改后的完整偏好总结文本 |
| `dimensions` | object | 否 | 结构化维度信息，不传则后端仅保存 summary_text |

### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "updated_at": "2026-07-11T14:35:00+08:00"
  },
  "trace_id": "req_pref_upd_001"
}
```

---

## API-04：获取可用模型列表

获取当前可用的 AI 模型列表，用于前端模型选择器的动态渲染。

### 基本信息

| 项 | 值 |
|---|---|
| **路径** | `GET /api/v1/ai-content/models` |
| **鉴权** | 需要 |
| **超时** | 3s |

### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "models": [
      {
        "id": "qwen",
        "name": "Qwen",
        "icon": "🔵",
        "status": "available",
        "description": "通义千问，适合中文内容生成",
        "is_default": true
      },
      {
        "id": "deepseek",
        "name": "DeepSeek",
        "icon": "🟢",
        "status": "available",
        "description": "深度求索，擅长数据分析"
      },
      {
        "id": "glm",
        "name": "GLM",
        "icon": "🟣",
        "status": "available",
        "description": "智谱 GLM，逻辑推理能力强"
      },
      {
        "id": "kimi",
        "name": "Kimi",
        "icon": "🟠",
        "status": "available",
        "description": "月之暗面 Kimi，长文本处理优秀"
      }
    ]
  },
  "trace_id": "req_models_001"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 模型唯一标识（用于 API-01 的 `model` 参数） |
| `name` | string | 模型展示名称 |
| `icon` | string | 模型图标 emoji |
| `status` | string | 模型状态：`available`（可用）、`unavailable`（不可用）、`maintenance`（维护中） |
| `description` | string | 模型描述，用于 hover 提示 |
| `is_default` | bool | 是否为默认模型（仅一个为 true） |

---

## API-05：采纳方案

记录用户采纳方案的行为，用于数据统计和效果分析。

### 基本信息

| 项 | 值 |
|---|---|
| **路径** | `POST /api/v1/ai-content/adopt` |
| **鉴权** | 需要 |
| **超时** | 3s |

### 请求体

```json
{
  "session_id": "sess_20250628_abc123",
  "content_id": "content_50001",
  "model": "qwen",
  "round_count": 3,
  "final_content": "## 澳首超 赛事方案\n..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `session_id` | string | 是 | 会话 ID |
| `content_id` | string | 是 | 被回填的内容 ID（编辑页正文的唯一标识） |
| `model` | string | 是 | 最终采纳方案使用的模型 |
| `round_count` | int | 是 | 生成该方案所经历的对话轮次 |
| `final_content` | string | 是 | 最终采纳的方案内容（Markdown） |

### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "adopt_id": "adopt_20250628_001",
    "adopted_at": "2026-07-11T14:40:00+08:00"
  },
  "trace_id": "req_adopt_001"
}
```

---

## 附录

### A. 主播风格枚举值

| 值 | 含义 |
|---|---|
| `aggressive` | 激进型 |
| `conservative` | 稳健型 |
| `data` | 数据型 |
| `emotional` | 情感型 |
| `comprehensive` | 综合分析型 |

### B. 方案类型枚举值（建议）

| 值 | 含义 |
|---|---|
| `win_draw_loss` | 胜平负分析 |
| `handicap` | 让球分析 |
| `over_under` | 大小球分析 |
| `half_full` | 半全场分析 |
| `correct_score` | 比分预测 |
| `combo` | 组合分析 |

### C. Prompt 组装规则（后端内部约定）

后端收到 API-01 请求后，按以下模板组装 Prompt（**非流式调用，一次性返回完整内容**）：

```
你是一个专业的体育赛事分析助手。请根据以下信息生成赛事方案内容：

## 赛事信息
- 赛事：{context.match_name}
- 方案类型：{context.scheme_type}

## 作者信息
- 主播：{context.author_name}
- 历史战绩：{context.history_record}
- 主播风格：{context.anchor_style}

## 产品定义
{context.product_description}

## 用户偏好
{preference_summary}

## 历史对话
{conversation_history}

## 当前请求
{message}

请根据以上要求生成完整的赛事分析方案，注意：
1. 使用 Markdown 格式
2. 包含数据表格
3. 体现主播的 {anchor_style} 风格特点
4. 参考用户历史偏好生成
```
