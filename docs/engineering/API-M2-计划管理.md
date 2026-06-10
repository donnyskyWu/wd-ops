# API-M2-计划管理

> **版本**：v1.1 | 2026-06-11  
> **关联 PRD**：[`PRD-M2-内容生产.md`](../product/PRD-M2-内容生产.md) § FR-M2-009  
> **实现**：`ContentPlanController` · **ADR**：[`ADR-012`](../adr/ADR-012-计划管理任务联动.md)

---

## 0. 通用说明

- 基路径：`/admin-api/oa/plan`
- Phase 1 计划写接口**未加**方法级 `@PreAuthorize`（与 M2 其他写接口一致，Gate S2 策略）；租户隔离 + 终止审批岗位校验在 Service 层。
- 终止审批：`approveTerminate` / `rejectTerminate` 要求当前用户 `position=OPS_LEADER`，否则 **403**（`OaErrorCodes.FORBIDDEN`）。

---

## 1. GET `/admin-api/oa/plan/list`

| 参数 | 类型 | 说明 |
|------|------|------|
| planName | String | 模糊 |
| status | String | `dict_plan_status` |
| pageNo | Integer | 默认 1 |
| pageSize | Integer | 默认 20 |

## 2. GET `/admin-api/oa/plan/get?id=`

返回计划详情 + competitions + steps。

## 3. POST `/admin-api/oa/plan/create`

```json
{
  "planName": "6月内容计划",
  "templateId": 9401,
  "ipGroupId": 9001,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "description": "...",
  "competitions": [{ "competitionId": "cmp-001", "competitionName": "春季赛" }],
  "steps": [
    {
      "nodeId": 9401,
      "assigneeIds": [1003],
      "scheduledStart": "2026-06-01 00:00:00",
      "scheduledEnd": "2026-06-30 23:59:59"
    }
  ]
}
```

**规则**：
- 须覆盖模板**全部**节点（缺一报错 1500）
- 每节点 `assigneeIds` 非空；执行人须本租户有效用户（1501）
- `templateId` / `ipGroupId` 须同租户（1501 / 1504）
- `endDate` 不得早于 `startDate`
- `scheduledStart` / `scheduledEnd` 可选；缺省为计划起止日 00:00:00 / 23:59:59

**响应**：`CommonResult<Long>`（计划 id）

## 4. POST `/admin-api/oa/plan/{id}/start`

草稿 → 进行中；计划任务 `PLAN_DRAFT` → `PENDING`，`visible_in_list=1`。

## 5. POST `/admin-api/oa/plan/{id}/terminate`

进行中 → 终止审批中（`TERMINATE_PENDING`）。

**请求体**（可选）`ContentPlanTerminateReq`：

```json
{ "reason": "赛事取消" }
```

非进行中状态 → **2023**。

## 6. POST `/admin-api/oa/plan/{id}/terminate/approve`

仅 `position=OPS_LEADER`；计划与关联任务 → `TERMINATED`。

## 7. POST `/admin-api/oa/plan/{id}/terminate/reject`

终止审批中 → 进行中（`IN_PROGRESS`）。仅 `OPS_LEADER`。

## 8. DELETE `/admin-api/oa/plan/delete?id=`

仅草稿可删；非草稿 → **2023**。级联删除计划赛事/步骤及关联 `oa_task`。

---

## 9. 错误码

| 码 | 场景 |
|----|------|
| 1501 | 模板/IP 组/执行人不存在 |
| 1504 | 跨租户 |
| 403 | 非运营组长审批终止 |
| 2023 | 计划状态不允许（启动/终止/删除） |

---

**赛事 Mock**：前端 `src/mock/competition.ts`，Phase 2 替换真实 API。
