# WORK-TASK-WITHDRAW-E2E 报告：任务登记撤回

**日期** 2026-08-19 · **模块** M2 工作任务管理 · **环境** Gate `:48080` / Ops `:48094` / FE `:5777` · admin/admin123 tenant=1 · Beta DB 110.42.49.224

## 结论（一句话）

**撤回根因已修复并 API E2E 全绿**：MyBatis-Plus `updateById(null)` 未将 `generated_task_id` 写回 NULL；改用 `LambdaUpdateWrapper.set(..., null)` 后，撤回清链、二次撤回拒绝、GET 回读均符合预期。

## 根因与证据

| 项 | 修复前 | 修复后 |
|----|--------|--------|
| POST withdraw code | 0 | 0 |
| 响应/GET `generatedTaskId` | **仍为 160/161** | **null** |
| 二次 withdraw | 误成功 (code=0) | **1400**「第 N 行未确认，无法撤回」 |
| 前端状态标签 | 仍显示「已确认」 | loadSheet 后应为「草稿」 |

### 技术根因

`WorkTaskServiceImpl.clearAssignmentGeneratedTask` 使用 `updateById(row)` 且 `row.setGeneratedTaskId(null)`。MyBatis-Plus 默认 **NOT_NULL** 策略会跳过 null 字段，即便 DO 上加了 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`，运行中仍观察到 **DB 未更新**（见修复前 API 实证）。

**修复**：显式 `LambdaUpdateWrapper` SET `generated_task_id = NULL`。

```java
workTaskAssignmentMapper.update(null, new LambdaUpdateWrapper<WorkTaskAssignmentDO>()
    .eq(WorkTaskAssignmentDO::getId, row.getId())
    .set(WorkTaskAssignmentDO::getGeneratedTaskId, null)
    ...
);
```

### 排除项

| 假设 | 结论 |
|------|------|
| 前端传错 id（rowNo vs assignment id） | ❌ 前端 `assignmentIds = selectedRows.map(r => r.id)` 正确 |
| 前端状态字段名不一致 | ❌ 前后端均为 `generatedTaskId` |
| loadSheet 未刷新 | ❌ GET 直读 DB 仍带旧值，非前端缓存 |
| 任务未取消 | 任务 small int id；撤回后 my-tasks 不可见（`visibleInList=0`） |

## 修复文件

| 文件 | 变更 |
|------|------|
| `football-module-ops-server/.../WorkTaskServiceImpl.java` | `clearAssignmentGeneratedTask` → `LambdaUpdateWrapper` |
| `scripts/integration-config/smoke_work_task_api.py` | confirm 传 `assignmentIds` + withdraw 断言 |
| `scripts/integration-config/smoke_work_task_withdraw_e2e.py` | **新增** confirm→withdraw 专用 E2E |

## E2E 结果

### Pre-flight

| 服务 | 状态 |
|------|------|
| Gateway :48080 | ✅ 200 |
| Ops :48094 | ✅ UP（修复后 `-Rebuild` 重启） |
| Frontend :5777 | ✅ 200 |

### API 流程（修复后）

```text
GET  sheet/get-or-create              → rows[1] generatedTaskId=161
POST sheet/withdraw assignmentIds=[2] → code=0, response generatedTaskId=null
GET  sheet/get-or-create              → row2 generatedTaskId=null ✅
POST sheet/withdraw (again)           → code=1400 第 2 行未确认 ✅

confirm row1 → generatedTaskId=171
withdraw row1 → generatedTaskId=null ✅
second withdraw → 1400 ✅
```

脚本：

- `python scripts/integration-config/smoke_work_task_api.py` → **ALL OK**
- `python scripts/integration-config/smoke_work_task_withdraw_e2e.py` → **WITHDRAW E2E PASS**

### UI 浏览器

| 步骤 | 结果 |
|------|------|
| agent-browser 登录 → work-task | ⚠️ SKIP（登录页未自动跳转，需人工复测） |
| 预期：勾选已确认行 → 撤回 → 标签「已确认」→「草稿」 | 待 UI 复测（API 已证明 DB/后端正确） |

## 部署说明

修改后需 **重启 ops-server**（JAR 文件锁）：

```powershell
.\scripts\start-integration-oa.ps1 -Rebuild -Profiles "dev,dev-nacos,dev-nacos-local,dev-test-beta"
```

## 关联

- 前序修复：`WorkTaskAssignmentDO @TableField(updateStrategy=ALWAYS)` — **不足**，需 UpdateWrapper
- ADR-072 工作任务登记 · FR-M2-010 撤回登记
