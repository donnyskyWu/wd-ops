# API-M2-计划管理

> **版本**：v1.5 | 2026-06-13  
> **关联 PRD**：[`PRD-M2-内容生产.md`](../product/PRD-M2-内容生产.md) § FR-M2-009  
> **实现**：`ContentPlanController` · `MatchController` · **ADR**：[`ADR-012`](../adr/ADR-012-计划管理任务联动.md) · [`ADR-016`](../adr/ADR-016-M2-节点类型与任务内容关联.md)

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

返回计划详情 + `competitions` + `steps` + **`tasks`**（任务记录，含 `scheduledStart`/`scheduledEnd`、`executorRole`）。

## 3. POST `/admin-api/oa/plan/create`

```json
{
  "planName": "6月内容计划",
  "templateId": 9401,
  "ipGroupId": 9001,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "description": "...",
  "competitions": [{ "competitionId": "123456789", "competitionName": "英超-曼联 VS 切尔西-2026-06-12 20:00" }],
  "steps": [
    {
      "nodeId": 9401,
      "competitionId": "123456789",
      "competitionIds": ["123456789", "987654321"],
      "assigneeIds": [1003],
      "scheduledStart": "2026-06-01 00:00:00",
      "scheduledEnd": "2026-06-30 23:59:59"
    }
  ]
}
```

**规则**：
- 须覆盖模板**全部**节点（缺一报错 1500）
- 每 step 须 `competitionIds`（≥1）或兼容字段 `competitionId`；均须属于 `competitions` 列表
- 每节点 `assigneeIds` **长度=1**（UI 单选执行人）；须本租户有效用户且在计划 IP 组成员内（1501）
- **多赛事**：对每个 `(assigneeId × competitionId)` 生成一条 `oa_task`
- `templateId` / `ipGroupId` 须同租户（1501 / 1504）
- `endDate` 不得早于 `startDate`
- `scheduledStart` / `scheduledEnd` 可选；缺省为计划起止日 00:00:00 / 23:59:59

**响应**：`CommonResult<Long>`（计划 id）

## 4. PUT `/admin-api/oa/plan/update`

**仅草稿（`DRAFT`）可编辑**；非草稿 → **2023**。

**请求体** `ContentPlanUpdateReq`（不可改 `templateId` / `ipGroupId`，沿用创建时值）：

```json
{
  "id": 1001,
  "planName": "6月内容计划（修订）",
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "description": "...",
  "competitions": [{ "competitionId": "123456789", "competitionName": "英超-曼联 VS 切尔西-2026-06-12 20:00" }],
  "steps": [
    {
      "nodeId": 9401,
      "competitionId": "123456789",
      "competitionIds": ["123456789", "987654321"],
      "assigneeIds": [1003],
      "scheduledStart": "2026-06-01 00:00:00",
      "scheduledEnd": "2026-06-30 23:59:59"
    }
  ]
}
```

**规则**（与 create 步骤/赛事部分一致）：
- 须覆盖模板**全部**节点（缺一报错 1500）
- 每 step `competitionIds` 必填（≥1），且均须属于 `competitions` 列表（1500）
- 每节点 `assigneeIds` 长度=1；执行人须本租户有效用户（1501）
- `endDate` 不得早于 `startDate`（1503）
- 更新时级联替换计划赛事、步骤及关联 `PLAN_DRAFT` 任务

**响应**：`CommonResult<Boolean>`

## 5. POST `/admin-api/oa/plan/{id}/start`

草稿 → 进行中；计划任务 `PLAN_DRAFT` → `PENDING`，`visible_in_list=1`。

## 6. POST `/admin-api/oa/plan/{id}/terminate`

进行中 → 终止审批中（`TERMINATE_PENDING`）。

**请求体**（可选）`ContentPlanTerminateReq`：

```json
{ "reason": "赛事取消" }
```

非进行中状态 → **2023**。

## 7. POST `/admin-api/oa/plan/{id}/terminate/approve`

仅 `position=OPS_LEADER`；计划与关联任务 → `TERMINATED`。

## 8. POST `/admin-api/oa/plan/{id}/terminate/reject`

终止审批中 → 进行中（`IN_PROGRESS`）。仅 `OPS_LEADER`。

## 9. DELETE `/admin-api/oa/plan/delete?id=`

仅草稿可删；非草稿 → **2023**。级联删除计划赛事/步骤及关联 `oa_task`。

---

## 10. 错误码

| 码 | 场景 |
|----|------|
| 1501 | 模板/IP 组/执行人不存在 |
| 1504 | 跨租户 |
| 403 | 非运营组长审批终止 |
| 2023 | 计划状态不允许（启动/终止/删除/编辑） |

---

## 11. 外部赛事代理（BLK-M2-004 已决）

> 前端**禁止**直连外部域名（CORS）；统一经本模块后端转发。外部 API **无 tenant**，代理层不做租户过滤。

### 11.1 GET `/admin-api/oa/match/list`

| 参数 | 类型 | 说明 |
|------|------|------|
| date | String | 比赛日期 `yyyy-MM-dd`，默认当天 |
| pageNo | Integer | 默认 1 |
| pageSize | Integer | 默认 20，最大 200 |
| leagueId | String | 联赛 `sclassId` |
| teamKeyword | String | 主队/客队模糊（服务端过滤） |
| lotteryType | String | 竞彩类型，如 `jc` |

**响应** `PageResult<MatchVO>`：

| 字段 | 来源（外部 `/app-api/match/list`） | 说明 |
|------|-----------------------------------|------|
| scheduleId | scheduleId | 计划/任务 `competitionId` |
| displayName | 拼接 | `{联赛}-{主队} VS {客队}-{matchTime}` |
| sClassId | sclassId | 联赛 ID |
| sClassName | `/filter/competitions/flat` 或 sClassName | 联赛中文名 |
| homeTeamName | homeTeamName | 主队 |
| guestTeamName | guestTeamName | 客队 |
| matchTime | matchTime(ms) | 格式化 `yyyy-MM-dd HH:mm` |
| matchTimeRaw | matchTime | 原始毫秒时间戳 |
| lotteryType | lotteryType / 默认 `jc` | 竞彩类型 |

### 11.2 GET `/admin-api/oa/match/leagues`

返回联赛下拉列表（转发 `/filter/competitions/flat`）。

| 字段 | 外部字段 |
|------|---------|
| id | sclassId |
| name | nameZh |
| nameEn | nameEn |
| shortName | shortName |

**配置**（`application-*.yml`）：

```yaml
oa:
  match:
    api-base-url: http://110.42.49.224:48088/app-api/match
    headers: {}   # 可选，外部 API 鉴权头
```

**前端**：`src/api/match.ts` · `MatchSelectDialog.vue` · 计划页 `plan/index.vue`。