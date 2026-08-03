# TASK-EXEC-EDIT-CONTENT-20260802

## Symptom

任务执行抽屉 → **编辑内容** 弹出 `ContentEditDialog` 时报：
- `请求参数格式错误` (HTTP 400 / code 1400)
- `无权限访问` (HTTP 403)

## Root cause

P5 迁入 `ProductionContentController` 时 **未挂** 任务执行依赖的字面量 GET 路由：

| FE API | Path | 迁入后实际命中 |
|--------|------|----------------|
| `getContentByTask(taskId)` | `GET /ops/content/by-task?taskId=` | `GET /ops/content/{id}`，`id=by-task` → Long 解析失败 → **1400** |
| `getScriptRef(...)` | `GET /ops/content/script-ref?...` | 同上，`id=script-ref` → **1400** |

`ContentEditPanel` 任务模式（`initTaskMode`）并行调用 `getTaskExecute` + `getContentByTask`。后者 400 被 `.catch(() => null)` 吞掉，但全局 axios 拦截器仍会 toast；关联内容无法加载。

若回退走 `getContent(contentId)`（内容管理 6117 **creator** 读范围），任务执行人非 `creator_user_id` 时会 **403 FORBIDDEN**——与任务执行「按 taskId 加载、不校验 creator」的 S-12 语义不一致。

## Fix

1. `ProductionContentController`：在 `/{id}` **之前** 增加 `GET /by-task`、`GET /script-ref`；顺带补 `POST /{id}/generate`；`review-config` 前移。
2. `ProductionContentServiceImpl.getByTaskId`：增加 `assertTaskAssignee`（对齐 `TaskServiceImpl.requireAssignee`），任务执行人可读关联内容且 **不经过** `ContentDataScopeSupport.assertReadable`。

## Files changed

- `football-backend-saas/.../controller/content/ProductionContentController.java`
- `football-backend-saas/.../service/content/ProductionContentServiceImpl.java`

## Verification (manual)

1. 重启 ops-server（`mvn compile` 后 `./scripts/start-ops-dev.ps1` 或 `-FirstRun`）。
2. 登录 `:5777`，打开 **任务管理** → **执行**（内容生成节点，已有草稿）。
3. 点击 **编辑内容** → 应打开内容抽屉，无 400/403 toast；标题/正文与列表一致。
4. DevTools Network：`GET .../ops/content/by-task?taskId=<id>` 返回 `code:0` 或 `data:null`（尚无内容时）。

## FE call chain (unchanged)

`TaskExecutePanel.openContentDialog` → `ContentEditDialog` → `ContentEditPanel.initTaskMode` → `getContentByTask(taskId)`。
