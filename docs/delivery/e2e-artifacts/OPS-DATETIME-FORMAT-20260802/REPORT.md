# OPS 时间字段统一格式化 — 20260802

## Pattern

- Util: `formatDateTime` from `#/utils/ops/index` → `YYYY-MM-DD HH:mm:ss`（空值显示 `-`）
- Tables: `#default` slot on `el-table-column`（与 IpGroup `joinTime` / `boundAt` 一致）
- Descriptions / meta: `{{ formatDateTime(x) }}`
- Dynamic columns（采集日志样本、DataReport）: `isDateTime*Prop` + `formatDateTime`
- SLA 截止：原 `formatSlaDeadline`（截到分钟）改为 `formatDateTime`

## Files touched (datetime)

约 **39** 个 `views/ops/**/*.vue`（含列表/详情/配置/绩效/采集等）。  
同目录工作区另有 `AiContentDrawer` / `ContentEditPanel` 等非本任务改动，未计入。

## Smoke (static)

- Content list: `createTime` slot + export 已格式化
- Collect log / log-detail: `startAt` / `endAt` + sample `*At` 列
- Account list: `InternalAccountManage` / `PersonalAccountManage` `createTime`

Re-inventory of raw `*Time` / `*At` table props & template binds under `views/ops`: **0 residual**

## Intentional residuals

| Field | Reason |
|-------|--------|
| `statDate` / `startDate` / `endDate` / `planDate` / `payDate` / `updateDate` | Date-only（Spec/列名） |
| `FansAnalysis` / `AccountAnalysisDetail` 上已有的 `formatDateTime(statDate)` | 既有行为，未引入 date-only util |
| Form `v-model` date-pickers | 输入控件，非展示 |

## No commit
