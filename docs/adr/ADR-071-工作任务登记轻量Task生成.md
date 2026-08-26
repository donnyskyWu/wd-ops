# ADR-071：工作任务登记轻量 `oa_task` 生成（绕过 SOP 计划）

| 字段 | 值 |
|------|---|
| 编号 | ADR-071 |
| 标题 | 工作任务登记确认后直接生成 CONTENT_GENERATION 单节点 `oa_task` |
| 状态 | **Accepted** |
| 日期 | 2026-08-19 |
| 批准日期 | **2026-08-19**（产品 Owner 会话批准） |
| 决策人 | 产品 Owner |
| 关联 | [PRD-M2-工作任务管理](../product/PRD-M2-工作任务管理.md) · [ADR-012](./ADR-012-计划管理任务联动.md) · [ADR-056](./ADR-056-Football用户身份SSOT.md) · [ADR-066](./ADR-066-IP组长视为IP组成员.md) · [ADR-072](./ADR-072-工作任务红黑判定与AI提示词.md) |

---

## 1. 背景

FR-M2-010「工作任务管理」要求 IP 组长按日登记「赛事 × 作者 × 营销计划」，确认后为运营同学生成「我的任务」待办。

现有 FR-M2-009「计划管理」通过 `oa_content_plan` + 完整 SOP DAG 批量生成多节点 `oa_task`。工作任务登记粒度更轻、频率更高，产品 Owner 2026-08-19 决议 **绕过完整 SOP**，不创建 `oa_content_plan`。

---

## 2. 决策（Accepted）

| # | 决策 |
|---|------|
| **D1** | **不创建** `oa_content_plan` / `oa_content_plan_step`；确认登记时 `oa_task.plan_id = NULL`。 |
| **D2** | 每 `oa_work_task_assignment` 行 **仅生成 1 条** `oa_task`；SOP 节点固定为 **CONTENT_GENERATION**（系统参数 `work_task.default_template_id` / `work_task.default_node_id` 指定，节点类型校验必须为 CONTENT_GENERATION）。 |
| **D3** | 创建即 `status=PENDING`，`visible_in_list=1`（**不走** PLAN_DRAFT 隐藏路径，ADR-012 §2.2 不适用）。 |
| **D4** | 新增 `oa_task.work_task_assignment_id` BIGINT 可空，反向追溯日任务登记行（方案 A；见 PRD §4.2.3）。 |
| **D5** | **assignee_id**（Q1 已决）：来自登记行 `oa_work_task_assignment.assignee_id`，存 Football **`system_users.id`**（ADR-056）；**不是** `author_id`。**author_id** 来自登记行，存 **`author_user.id`**（内容品牌/IP 主播）。确认时一并写入 `competition_id`、`ip_group_id`。 |
| **D6** | Tab1 每行 **执行人 UserSelect**（`:ip-group-id` 过滤当前 sheet IP 组成员 + 组长，ADR-066）；**默认值** = 当前 sheet 所属 IP 组的 `leader_user_id`（非 plan 岗位解析 `resolveAssignee`）。写入前 `FootballSystemUserValidator.resolveStorableUserId` + IP 组成员存在性校验（1501/1504）。 |
| **D7** | **撤回（withdraw）**：仅 `CONFIRMED` sheet 可 `POST /sheet/withdraw` → sheet 回 `DRAFT`；关联 `oa_task` 置 **`status=CANCELLED`** 且 **`visible_in_list=0`**（Scheme 1 默认）；清空 assignment.`generated_task_id`。若 task 已 `IN_PROGRESS` / `COMPLETED`，禁止 withdraw 或须产品扩展（v1 建议禁止并返回 1502）。 |
| **D8** | **场次序号** `session_no`：sheet 内按 `row_no` 递增生成 **3 位**字符串（`001`…`010`），**不**读取 MatchVO 竞彩场次号（Q7）。 |
| **D9** | `plan_name` 建议：`{workDate} {authorName} {matchName}`；`scheduled_start` / `scheduled_end` 可 NULL 或取自 `match_time`（实现 Slice 细化）。 |

---

## 3. 任务生成伪代码

```
for each assignment in sheet.rows where required fields filled:
  validate uniqueness(tenant, work_date, competition_id, author_id)
  validate assignee_id in ip_group members (1501)
  validate author_id in ip_group anchors (1501)
  task = TaskService.create(
    plan_id = null,
    work_task_assignment_id = assignment.id,
    template_id = sys_param work_task.default_template_id,
    node_id = sys_param work_task.default_node_id,  // CONTENT_GENERATION
    competition_id = assignment.competition_id,
    author_id = assignment.author_id,
    assignee_id = assignment.assignee_id,
    ip_group_id = sheet.ip_group_id,
    status = PENDING,
    visible_in_list = 1
  )
  assignment.generated_task_id = task.id
sheet.status = CONFIRMED
```

---

## 4. 与计划管理的边界

| 维度 | FR-M2-009 计划管理 | FR-M2-010 工作任务登记（本 ADR） |
|------|-------------------|--------------------------------|
| 计划表 | `oa_content_plan` | **无** |
| 任务数/行 | 多 SOP 节点 × 多执行人 | **1 task / assignment** |
| assignee 默认 | `PlanTaskGeneratorService.resolveAssignee` | **IP 组长** `leader_user_id` |
| plan_id | 非空 | **NULL** |
| 撤回 | 终止审批流 | **withdraw → CANCELLED task** |

---

## 5. 后果

- `TaskService.create` 或等价封装须支持 `plan_id=NULL` + `work_task_assignment_id`。
- 系统参数 seed：`work_task.default_template_id`、`work_task.default_node_id`（Flyway / sys_param）。
- 「我的任务」查询不变：`assignee_id` 匹配当前用户 + `visible_in_list=1`。
- Slice S-17 实现 confirm + withdraw；S-16 负责 schema + 本 ADR 批准。

---

## 6. Out of Scope

- 不改造 `ContentPlanService` / `PlanTaskGeneratorService` 主路径。
- 不在 confirm 时触发红黑判定（见 ADR-072 赛后 Job）。
- 不生成 PLAN_DRAFT 态任务。

---

## 7. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-08-19 | Draft：产品 Owner 关闭 Q1–Q7；锁定 bypass SOP、assignee 行选、withdraw、场次序号 |
| 2026-08-19 | **Accepted**：产品 Owner 会话批准；S-16 Flyway V181 落地 schema + sys_param |
