# ADR-053：M2 AI 内容对话生成（S-15）

| 字段 | 值 |
|------|---|
| 编号 | ADR-053 |
| 标题 | M2 内容编辑 — AI 多轮对话生成替代单次 ai-generate |
| 状态 | **Accepted** |
| 日期 | 2026-07-12 |
| 关联 | `docs/内容生成/PRD.md` · `docs/内容生成/API_Contract.md` · S-15 · M8 |

---

## 1. 背景

内容编辑页原有 `POST /oa/content/ai-generate` 为**单次生成**：前端弹窗选择 M8 模型 ID + 提示词 ID，一次调用写入正文，不支持多轮修改。

产品新需求（PRD v1.1）要求：右侧 720px 抽屉、多模型（qwen/deepseek/glm/kimi）、多轮对话、Markdown 渲染、采纳回填。

## 2. 决策

### 2.1 API 路径与服务归属

| 项 | 决策 |
|----|------|
| Base | `/admin-api/oa/ai-content/*`（oa-module，与现有 `/oa/content` 并列） |
| 端点 | `POST /generate`、`GET /models`、`GET+PUT /preference-summary`、`POST /adopt` |
| 旧接口 | `POST /oa/content/ai-generate` **保留但标记 @Deprecated**，任务流/回归用例不破坏 |

### 2.2 `scheme_type` 与 `document_type` 分离

| 字段 | 字典 | 说明 |
|------|------|------|
| `document_type` | `dict_document_type` | 内容形态（文章/脚本等），不变 |
| `scheme_type` | `dict_scheme_type` | 赛事方案类型（胜平负/让球/大小球等），**新增**于 `oa_production_content` 与编辑表单 |

二者独立校验；AI 对话 `context.scheme_type` 传字典 value（如 `WIN_DRAW_LOSE`）。

### 2.3 主播风格

使用 `dict_anchor_style`（aggressive / conservative / data / emotional / comprehensive），与 PRD/API Contract 对齐。

### 2.4 LLM 调用

- 抽取 `AiLlmInvokeSupport`，与旧 `ProductionContentServiceImpl.aiGenerate` 共用 HTTP Chat Completions 逻辑
- 新流程使用 M8 `scene=AI_CONTENT_CHAT` 提示词模板，多轮 messages 组装
- **提示词按文档类型选用**见 [ADR-063](./ADR-063-AI内容提示词按文档类型.md)（`document_type` 精确匹配，缺省回退通用 `AI_CONTENT_CHAT`）
- 模型映射：API 字符串 `qwen|deepseek|glm|kimi` → 租户 `oa_ai_model_config`（model_id / model_type 匹配）

### 2.5 会话与偏好（P0 范围）

| 能力 | P0 | 说明 |
|------|-----|------|
| 会话 | DB 表 `oa_ai_content_session` | 每轮 upsert，前端仍传完整 `conversation_history` |
| 采纳 | DB 表 `oa_ai_content_adopt` | 记录采纳行为 |
| 偏好总结 | 表 + stub | GET 无记录返回 `null`；PUT 可手动保存；**不**做 AI 自动提炼 |

### 2.6 前端

- 新组件 `AiContentDrawer.vue`（720px 右侧抽屉），替代原 `el-dialog` AI 弹窗
- `ContentEditPanel`：开启 AI 开关后显示「方案类型」；「生成」前校验 IP 组 + 赛事 + 标题 + 方案类型
- 采纳 → Markdown 转富文本写入正文（`plainTextToHtml` / `syncRichToForm`）

## 3. 数据变更（V139）

- `dict_scheme_type`、`dict_anchor_style`
- `oa_production_content.scheme_type`
- `oa_ai_content_session`、`oa_ai_content_adopt`、`oa_ai_content_preference`
- Seed：`AI_CONTENT_CHAT` 提示词；DeepSeek 模型别名

## 4. 弃用说明

| 弃用项 | 替代 |
|--------|------|
| 内容管理「AI 辅助生成」弹窗 | `AiContentDrawer` + `/oa/ai-content/*` |
| 单次 `ai-generate` 作为主路径 | 多轮 `/oa/ai-content/generate` |

旧弹窗代码已从 `ContentEditPanel` 移除；后端 `ai-generate` 仍可用。

## 5. Out of Scope（P1+）

- 偏好总结 AI 自动提炼与 5 秒动画面板完整交互（P0 仅展示已有偏好）
- 重新生成二次确认、模型切换确认弹窗
- 流式输出
- 限流 3 次/分钟（P0 未实现）
- 独立 AI 内容生成全屏路由（P0 使用抽屉）
