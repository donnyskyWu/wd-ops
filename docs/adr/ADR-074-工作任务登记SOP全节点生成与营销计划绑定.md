# ADR-074：工作任务登记 — 营销计划绑定 SOP + 多赛事 + 全节点 Task 生成

| 字段 | 值 |
|------|---|
| 编号 | ADR-074 |
| 标题 | 工作任务登记确认后按营销计划解析 SOP，全节点生成 `oa_task`（多赛事/行） |
| 状态 | **Accepted** |
| 日期 | 2026-08-31 |
| 批准日期 | **2026-08-31**（产品 Owner 会话批准） |
| 决策人 | 产品 Owner |
| 关联 | [PRD-M2-工作任务管理](../product/PRD-M2-工作任务管理.md) · [PRD-M2-内容生产](../product/PRD-M2-内容生产.md) FR-M2-001 · [ADR-012](./ADR-012-计划管理任务联动.md) · [ADR-016](./ADR-016-M2-节点类型与任务内容关联.md) · [ADR-056](./ADR-056-Football用户身份SSOT.md) · [ADR-066](./ADR-066-IP组长视为IP组成员.md) · [ADR-071](./ADR-071-工作任务登记轻量Task生成.md)（**Superseded §2 核心路径**） · [ADR-072](./ADR-072-工作任务红黑判定与AI提示词.md) · [ADR-077](./ADR-077-SOP内容生成节点文档类型与登记自动草稿AI.md)（confirm 后自动 DRAFT + jingcai / withdraw 删草稿） |

---

## 1. 背景

FR-M2-010「工作任务管理」原按 [ADR-071](./ADR-071-工作任务登记轻量Task生成.md)：**每登记行 → 1 条 CONTENT_GENERATION 任务**，SOP 模板由系统参数 `work_task.default_template_id` / `default_node_id` 写死。

产品 Owner 2026-08-31 决议变更：

1. **SOP 模板**增加 **营销计划**字段；**启用态**下每类营销计划 **仅 1 个 SOP**（1:1）。
2. **营销计划枚举**调整为：快手付费课程 / 公司平台付费 / 免费公推（测试阶段 **不迁移** 历史 assignment 数据）。
3. **任务登记行**支持 **多赛事**（同一行、同一营销计划）；**不**引入独立「合并组」头表。
4. **确认登记**时：按行 `marketing_plan` 解析启用 SOP → 对该模板 **每个节点** 生成 **1 条** `oa_task`（每条 task 携带 **全部** `competition_ids`）。
5. **节点执行人**：与计划管理一致 — `PlanTaskGeneratorService.resolveAssignee(node.executor_role, ipGroupId)`；IP 组内无匹配岗位 → **IP 组长** `leader_user_id`（ADR-066）。

仍 **不创建** `oa_content_plan`（与 FR-M2-009 边界保持 ADR-071 D1 精神）。

---

## 2. 决策（Accepted）

| # | 决策 |
|---|------|
| **D1** | **不创建** `oa_content_plan` / `oa_content_plan_step`；确认登记时 `oa_task.plan_id = NULL`。 |
| **D2** | `oa_sop_template` 新增 **`marketing_plan`** VARCHAR(32)，`@InDict dict_marketing_plan_type`；**启用**（`status=1`）时同租户 **`marketing_plan` 唯一**（重复 → 1502）。草稿/停用 **不占** 唯一坑位。 |
| **D3** | `dict_marketing_plan_type` **替换**为：`KUAISHOU_PAID_COURSE` 快手付费课程 · `COMPANY_PAID` 公司平台付费 · `FREE_PUBLIC` 免费公推。测试环境 **仅改字典**，**不**回填历史 `oa_work_task_assignment.marketing_plan`。 |
| **D4** | `oa_work_task_assignment`：**一行 = 一个作者 + 一个营销计划 + 多赛事**。新增 **`competition_ids_json`**（JSON 数组，元素含 `competitionId/competitionName/leagueName/matchTime` 快照）；废弃单行单赛为主模型（Flyway 迁移见 §4）。 |
| **D5** | **任务粒度**：每个 SOP 节点 → **1 条** `oa_task`；`oa_task.competition_ids_json` 复制登记行全部赛事（方案 A）。**不**展开为「节点 × 每场赛事」多条 task。 |
| **D6** | **SOP 解析**：confirm 时 `template = sopTemplateMapper.selectEnabledByMarketingPlan(tenantId, assignment.marketing_plan)`；不存在 → 1502「该营销计划未配置启用的 SOP」。**废弃** confirm 路径对 `work_task.default_template_id` / `default_node_id` 的依赖（参数可保留只读兼容，不再参与生成）。 |
| **D7** | **assignee_id**：按 **节点** `executor_role` 调用 `PlanTaskGeneratorService.resolveAssignee(role, ipGroupId, tenantId)`；无岗位匹配 → `oa_ip_group.leader_user_id`。登记行 **`assignee_id` 不参与** task 生成（保留字段仅审计/可选 UI，见 PRD §3.2.1 修订）。 |
| **D8** | 创建 task：`status=PENDING`，`visible_in_list=1`（同 ADR-071 D3）；写入 `template_id`、`node_id`、`author_id`、`ip_group_id`、`work_task_assignment_id`、`competition_ids_json`。 |
| **D9** | **登记行 ↔ 多 task**：新增 **`oa_work_task_assignment_task`**（`assignment_id`, `task_id`）或 assignment.`generated_task_ids_json`；withdraw 时 **批量** CANCEL 该 assignment 关联的全部 task（ADR-071 D7 扩展）。原 `generated_task_id` 单字段 **废弃**。 |
| **D10** | **唯一性**：同一 `(tenant_id, work_date, author_id)` 下，**同一 `competition_id` 不可出现在两个登记行**的 `competition_ids_json` 中（1502）。替代原 `uk_work_task_assignment_unique (work_date, competition_id, author_id)`。 |
| **D11** | **撤回/红黑/权限**：ADR-071 D7/D8、ADR-072 赛后 Job **继续有效**；Job 扫描范围改为 assignment 关联的 **CONTENT_GENERATION** 节点 task（或首个内容节点，Slice 实现时与 ADR-072 对齐）。 |

---

## 3. 确认登记伪代码

```
sheet = loadSheet(ipGroupId, workDate)
assignments = sheet.rows where required fields filled

for each assignment in assignments:
  validate author_id in ip_group anchors (1501)
  validate marketing_plan @InDict (1503)
  validate each competition in competition_ids_json:
    validate not duplicate (work_date, author_id, competition_id) across sheet (1502)

  template = sopTemplateService.getEnabledByMarketingPlan(marketing_plan)  // 1502 if missing
  nodes = sopNodeMapper.listByTemplateIdOrderByDag(template.id)

  for each node in nodes:
    assigneeId = planTaskGenerator.resolveAssignee(node.executorRole, sheet.ipGroupId, tenantId)
    // resolveAssignee: match ip_group_member.position == executorRole; else leader_user_id

    task = TaskService.create(
      plan_id = null,
      work_task_assignment_id = assignment.id,
      template_id = template.id,
      node_id = node.id,
      author_id = assignment.author_id,
      assignee_id = assigneeId,
      ip_group_id = sheet.ip_group_id,
      competition_ids_json = assignment.competition_ids_json,
      competition_id = first(assignment.competition_ids_json).id,  // 兼容列表/索引展示
      status = PENDING,
      visible_in_list = 1,
      plan_name = buildPlanName(workDate, authorName, marketingPlanLabel, competitionSummary)
    )
    assignmentTaskRel.insert(assignment.id, task.id)

sheet.status = CONFIRMED
```

**withdraw 扩展：**

```
for each assignment in sheet.assignments:
  for each taskId in assignmentTaskRel.listByAssignment(assignment.id):
    taskService.cancelAndHide(taskId)  // CANCELLED + visible_in_list=0
  assignmentTaskRel.deleteByAssignment(assignment.id)
sheet.status = DRAFT
```

---

## 4. Schema / Flyway（Slice 实现 SSOT）

| 变更 | 说明 | 建议版本 |
|------|------|----------|
| `oa_sop_template.marketing_plan` | VARCHAR(32) NULL；启用校验必填 | V192+ |
| 启用态唯一 | 应用层 + 可选 UK `(tenant_id, marketing_plan, deleted)` 配合 status 校验 | V192+ |
| `oa_work_task_assignment.competition_ids_json` | JSON NOT NULL（confirm 前至少 1 项） | V192+ |
| `oa_work_task_assignment.competition_id` 等 | 保留只读冗余或迁移后 DROP（Slice 定） | V192+ |
| `oa_task.competition_ids_json` | JSON NULL | V192+ |
| `oa_work_task_assignment_task` | `assignment_id`, `task_id`, 标准审计字段 | V192+ |
| DROP `uk_work_task_assignment_unique` | 替换为 D10 应用层校验 | V192+ |
| `dict_marketing_plan_type` | system 库三条新值（V193+）；移除旧三条 | V193+ |

**Out of Scope 本 ADR：** 历史 assignment / task 数据迁移（测试阶段无正式数据）。

---

## 5. 与 ADR-071 / 计划管理边界

| 维度 | ADR-071（旧） | ADR-074（新） |
|------|--------------|--------------|
| SOP 来源 | sys_param 写死单节点 | **marketing_plan → 启用 SOP 1:1** |
| 任务数/行 | 1 | **SOP 节点数 N** |
| 赛事 | 单行 1 赛 | **行内多赛** → 每 task 带 `competition_ids_json` |
| assignee | 登记行 assignee | **按节点 executor_role 解析**（同计划管理） |
| plan_id | NULL | NULL（不变） |
| content_plan | 不创建 | 不创建（不变） |

[ADR-071](./ADR-071-工作任务登记轻量Task生成.md) **D2/D5/D6 及 §3 伪代码** 由本 ADR **废止**；D1/D3/D4/D7/D8 及 withdraw 精神 **保留/扩展**。

---

## 6. 前端 / API 影响（Spec 待 Slice 100%）

| 区域 | 变更 |
|------|------|
| SOP 管理 | 表单增加营销计划 `DictSelect`；启用时校验 1:1 |
| 任务登记 Tab1 | 赛事 **多选**；营销计划三新值；执行人列 **可选/审计**（不驱动 assignee） |
| confirm API | 返回 `generatedTaskCount` = Σ 节点数 |
| 矩阵 Tab2 | 多赛事行展示摘要（如「3 场」或名称拼接） |
| PRD | [PRD-M2-工作任务管理](../product/PRD-M2-工作任务管理.md) v0.4 |
| API | [API-M2-工作任务管理.md](../engineering/API-M2-工作任务管理.md)（新建） |

---

## 7. 后果

- 实现 Slice **S-20**（建议编号）：Schema + SOP 1:1 + confirm 重写 + withdraw 批量 + 前端登记/SOP 页。
- `PlanTaskGeneratorService.resolveAssignee` **须** 被 WorkTask confirm 复用（ADR-071 曾明确 bypass）。
- 系统参数 `work_task.default_template_id` / `default_node_id`：**标记 deprecated**，Seed 可保留不删。
- TESTCASES：更新 AC-M2-010-3、010-1/010-2（assignee 改为按节点岗位）。

---

## 8. Out of Scope

- `oa_content_plan` 创建与计划管理 UI 改造。
- 节点 × 每场赛事展开为多 task（方案 B）。
- 历史营销计划枚举数据迁移。
- SOP 版本管理。

---

## 9. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-08-31 | Accepted：产品 Owner 确认营销计划 1:1、三新枚举、多赛事行、全节点 task、resolveAssignee |
| 2026-09-02 | 指针：confirm/withdraw 侧效应见 ADR-077（不改本 ADR D1–D11 核心） |
| 2026-09-03 | 指针：confirm afterCommit `TASK_PENDING` 见 [ADR-078](./ADR-078-工作任务确认登记钉钉提醒执行人.md)（不改 D1–D11） |
| 2026-09-03 | 指针：节点名称 / 执行页登记备注见 [ADR-080](./ADR-080-任务节点名称与执行页登记备注.md)（不改 D1–D11） |
