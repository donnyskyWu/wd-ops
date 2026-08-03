# ADR-063：AI 内容对话提示词按文档类型选用

| 字段 | 值 |
|------|---|
| 编号 | ADR-063 |
| 标题 | AI 内容对话按 `documentType` 选用提示词 |
| 状态 | **Accepted** |
| 日期 | 2026-08-02 |
| 关联 | ADR-053 · ADR-016 §2.6 · `docs/内容生成/各类文档提示词.docx` |

---

## 1. 背景

ADR-053 主路径 `POST /ops/ai-content/generate` 固定使用 `scene=AI_CONTENT_CHAT` 且 `document_type IS NULL` 的通用提示词。  
产品提供《各类文档提示词》后，要求按 `dict_document_type` 分类型选用不同系统提示词。  
表 `oa_ai_prompt_config` 已具备 `content_type` / `document_type` 字段；旧 `listAiPromptOptions` 已按类型过滤。

## 2. 决策

1. **仍用** `scene=AI_CONTENT_CHAT`；为每个文档类型各种子一条 `document_type=<CODE>` 记录。
2. **解析顺序**（`AiContentServiceImpl.resolveChatPrompt`）：
   - 请求带 `documentType` → 优先匹配 `tenant + scene + ENABLED + document_type`（可选 `content_type` 兼容 null）
   - 无匹配 → 回退 `document_type IS NULL` 的通用 `AI_CONTENT_CHAT`（ADR-053 种子）
3. **前端**：`ContentEditPanel` → `AiContentDrawer` 将 `documentType` / `contentType` 传入 `/ops/ai-content/generate`；ARTICLE 未选文档类型时拦截打开抽屉。
4. **占位符**：保留 ADR-053 的 `{{match_name}}` 等；docx 中 `{anchor}` / `{event_info}` 等在种子中归一为 `{{author_name}}` / `{{match_name}}` 等，并由 `fillPromptPlaceholders` 双格式替换。
5. **响应**：增加 `promptId` / `promptTemplateName` / `documentType` 便于验收。

## 3. 文档类型映射（仅 docx 出现的类型）

| docx 章节 | `dict_document_type` |
|-----------|----------------------|
| 赛后复盘提示词 | `POST_MATCH_REVIEW` |
| 预热前瞻提示词 | `PREHEAT_PREVIEW` |
| 新号引流提示词 | `NEW_ACCOUNT_TRAFFIC` |
| 短视频文案提示词 | `SHORT_VIDEO_SCRIPT` |
| 正式方案提示词 | `OFFICIAL_PLAN` |

## 4. 数据

- Flyway：`V168__ai_content_chat_prompt_by_document_type.sql`（幂等 INSERT + UPDATE）
- 通用模板（`document_type` 空）保留，作 fallback

## 5. Out of Scope

- 不新增文档类型枚举
- 不改采集 / collector
- 不替换旧 `POST /ops/content/ai-generate`（仍按 promptId 显式指定）
