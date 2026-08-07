# ADR-070：内容计划创建支持多 IP 组

| 字段 | 值 |
|------|---|
| 编号 | ADR-070 |
| 标题 | 内容计划创建支持多 IP 组 |
| 状态 | **Accepted** |
| 日期 | **2026-08-07** |
| 关联 | [ADR-012](./ADR-012-计划管理任务联动.md) · [ADR-064](./ADR-064-OPS六角色RBAC矩阵.md) · [ADR-066](./ADR-066-IP组长视为IP组成员.md) · PRD-M2 FR-M2-009 · [API-M2-计划管理](../engineering/API-M2-计划管理.md) |

---

## 1. 背景

Phase 1 计划管理（ADR-012）仅支持创建时选择**单个** `ipGroupId`，任务按 `(节点 × 赛事 × 执行人)` 生成。运营需在**一次计划**内覆盖多个 IP 组，各组按同一 SOP 模板与赛事池并行产出任务，并在计划详情 / 任务执行页按组展示。

PRD/API v1.5 未定义多 IP 组语义；本 ADR 锁定实现（Flyway V179 + `ContentPlanServiceImpl` / `TaskServiceImpl`）。

---

## 2. 决策

| # | 决策 |
|---|------|
| D1 | 新增 junction 表 `oa_content_plan_ip_group`（`tenant_id`, `plan_id`, `ip_group_id`），表达计划 ↔ IP 组 **多对多** |
| D2 | 创建请求 **`ipGroupIds: Long[]`（≥1）** 为 SSOT；保留 **`ipGroupId`** 单值兼容；`ipGroupIds` 优先 |
| D3 | 主表 `oa_content_plan.ip_group_id` **保留**，= `ipGroupIds[0]`，供列表筛选与旧客户端兼容 |
| D4 | **步骤模式**（无显式 `tasks`）：对每个 `(ipGroupId × assigneeId × competitionId)` 生成一条 `oa_task`，各 task 写入对应 `ip_group_id` |
| D5 | **任务模式**（显式 `tasks`）：多 IP 组时每条 task **必填** `ipGroupId` 且须属于计划 IP 组集合 |
| D6 | **编辑锁定**：`PUT .../update` **不可**变更 `templateId` 与 IP 组集合；草稿更新仅替换赛事/步骤/任务，junction 行不变 |
| D7 | **执行人校验**：步骤模式 — 执行人须落在所选 IP 组**并集**的成员 ∪ 组长（ADR-066）；任务模式 — 每条 task 的执行人须属于**该 task 的 IP 组** |
| D8 | **列表/详情数据范围**：非管理员可见计划 ⇔ junction 中**任一组** ∈ 当前用户可访问 IP 组（成员 ∪ 组长，ADR-064）；详情读权限同理（任一组可访问即可） |
| D9 | **任务执行 Tab**：`GET .../task/{id}/execute` 对同 `plan_id + node_id + competition_id` 的 sibling 任务返回 `ipGroupTabs[]`（>1 条时），供前端按 IP 组切换 |
| D10 | **预览**：`POST .../plan/preview-tasks` 接受 `ipGroupIds`，按组 × 节点 × 赛事返回 `ContentPlanTaskPreviewVO`（含 `ipGroupId` / `ipGroupName`） |
| D11 | **存量迁移**：V179 将已有 `oa_content_plan.ip_group_id` 回填至 junction 表 |

---

## 3. Schema（V179）

```text
oa_content_plan_ip_group (
  id, tenant_id, plan_id, ip_group_id,
  creator, create_time, updater, update_time, deleted
)
-- 索引：idx_oa_plan_ip_group_plan, idx_oa_plan_ip_group_group
```

`oa_task.ip_group_id` 已有，无需改表。

---

## 4. 后果

| 影响 | 说明 |
|------|------|
| 任务量 | 步骤模式下任务数 ≈ `|ipGroupIds| × |competitions| × |nodes|`（× 执行人，当前 UI 单执行人） |
| 兼容 | 仅传 `ipGroupId` 的行为与 v1.5 一致；响应同时返回 `ipGroupIds` / `ipGroups` |
| 前端 | 创建向导多选 IP 组；详情/执行页 Tab 消费 `ipGroupTabs` |
| 权限 | 单组用户仅见/改其可访问组下的计划与任务；多组计划对「部分可见」用户仍整计划只读（任一组命中即可见） |

---

## 5. 非目标

- 不支持草稿编辑时**增删** IP 组（须删草稿重建）
- 不改变 ADR-012 计划状态机与 `PLAN_DRAFT` / `visible_in_list` 语义
- 不在本 ADR 扩展任务列表 API 的 IP 组多选筛选（仍单 `ipGroupId` 参数）

---

## 6. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-08-07 | Accepted；V179 + API-M2 v1.6 |
