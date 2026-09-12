# 直播时长 S-tier — E2E 签收报告 (2026-08-26)

## 范围

| 项 | 值 |
|----|-----|
| FR | FR-M6-002 · 5.26.4 直播时长 |
| 优先级 | P0 · S-tier（作者汇总够用，用户已确认） |
| 估算 | ~2–2.5 人日（已交付） |

## 实现摘要

| 层 | 变更 |
|----|------|
| 后端 | `ReportServiceImpl` → `LiveRoomReadService` → Feign `LiveRoomApi.getLiveRoomCount` |
| 维度 | `oa_ip_group_anchor_rel.anchor_user_id` + member 昵称 |
| 单位 | 分钟 → **小时**（1 位小数） |
| v1 不含 | `peak_viewers` 前端列 |
| 前端 | `ReportLiveDuration.vue`：IP 组筛选、趋势双轴、作者明细、Excel 导出 |

## API 冒烟

| 接口 | 期望 |
|------|------|
| `GET /admin-api/ops/report/live-duration/list` | code=0；行含 `author_name`、`session_count`、`total_duration`（小时） |
| `GET /admin-api/ops/report/live-duration/trend` | code=0；按日 `session_count` + `total_duration` |

**前置**：Integration 栈 UP；IP 组存在 anchor 绑定；live-server 可 RPC。

## UI 走查

| 步骤 | 结果 |
|------|------|
| 打开 `/ops/analysis/report/live-duration` | 趋势图 + 明细表加载 |
| 选择 IP 组 + 日期 → 查询 | 作者行、场次、时长(小时) 有值或空态 |
| 导出 Excel | 含作者/场次/时长列 |
| 表头无「峰值在线」 | ✅ v1 Out of Scope |

## Spec 对齐

- PRD-M6 §2.4 · AC-M6-002-4/5
- API-M6 §2.4
- UX-M6 P-M6-005

## 与 LEGACY-SYS 报告关系

[LEGACY-SYS-HARNESS-RETIRE-20260825](../LEGACY-SYS-HARNESS-RETIRE-20260825/REPORT.md) 中 `live-duration/list` 为 **stub (total=30)**；本报告 supersede 该条为 **S-tier 真数据**。

## 关键文件

```
football-module-ops-server/.../ReportServiceImpl.java
football-module-ops-server/.../LiveRoomReadService.java
football-module-ops-server/.../framework/common/biz/live/room/LiveRoomApi.java
football-front/apps/web-ele/src/views/ops/analysis/ReportLiveDuration.vue
```

## 签收

| 角色 | 状态 | 日期 |
|------|------|------|
| 实现 | ✅ | 2026-08-26 |
| Spec sync | ✅ | 2026-08-26 |
| Gate 全量 UAT | 待 GATE-S6+ 回归 |
