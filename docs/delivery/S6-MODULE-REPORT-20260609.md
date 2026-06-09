# S6 扩展模块 · M5/M6/M7 交付报告

> **Gate**：GATE-S6  
> **日期**：2026-06-09  
> **Flyway**：V23 → V25

---

## 1. 模块完成度

### M5 财务管理

| Slice | 目标 | 状态 | IT |
|-------|------|------|-----|
| S-01 | 账号成本 CRUD | ✅ | M5FinanceCostS01IT |
| S-02 | 成本定时归集 | ✅ stub | — |
| S-03 | ROI 实时分析 | ✅ | M5FinanceRoiS03IT |
| S-04 | ROI 趋势 + 导出 | ✅ | M5FinanceRoiS03IT |

### M6 数据分析

| Slice | 目标 | 状态 | IT |
|-------|------|------|-----|
| S-01 | 指标管理 | ✅ | M6MetricS01IT |
| S-02 | 报表通用框架 | ✅ | M6ReportS02IT |
| S-03~S-06 | 8 张报表 | ✅ P0 | M6ReportS02IT |
| S-07 | 漏斗分析 | ✅ | M6FunnelS07IT |
| S-08 | 自定义查询 | ✅ stub | — |
| S-09 | 数据大屏 | ✅ seed | — |

### M7 作品监测

| Slice | 目标 | 状态 | IT |
|-------|------|------|-----|
| S-01 | 外部账号分析 | ✅ | M7MonitorS01IT |
| S-02 | 爆款/低分作品 | ✅ | M7MonitorS01IT |
| S-03 | 高/低粉账号 | ✅ | M7MonitorS01IT |
| S-04 | IP 主题 + 行业 | ✅ | M7MonitorS01IT |

---

## 2. 数据库与 Seed

| 迁移 | 内容 |
|------|------|
| **V23** | seed-analytics（硬门槛）：90 天粉丝日表、内容日趋势、账号状态日志、22+ 内容样本 |
| **V24** | `oa_account_cost`、`oa_funnel/step`、`oa_custom_query`、`oa_dashboard`、`oa_external_work` + 字典 |
| **V25** | seed-finance（12 成本）、seed-monitor（17 外部作品含爆款/低分）、漏斗+大屏 |

**Seed 验证**：`SeedVerificationIT#seedAnalytics` · `#seedMonitor`

---

## 3. API 清单

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| M5 | `/oa/finance/cost/*` · `/oa/finance/roi/*` | 成本 CRUD + ROI 分析/趋势/结构 |
| M6 | `/oa/metric/*` · `/oa/report/*` · `/oa/funnel/*` · `/oa/query/*` · `/oa/dashboard/*` | 8 张报表 + 漏斗 + 大屏 |
| M7 | `/oa/monitor/*` | 外部/爆款/低分/粉丝/IP/行业 |

**测试**：`mvn test` → **137/137 全绿**

---

## 4. 前端

- 新增：`finance.ts`、`report.ts`、`funnel.ts`、`metric.ts`、`monitor.ts`
- 扩展：`dashboard.ts`；`works.ts` 改 `@/utils/request`

---

## 5. 待补（P1）

- M5 定时成本归集 `@Scheduled` 完整实现
- M6 自定义查询 publish 审批流、报表异步导出
- M7 监测定时抓取 + 告警写 `oa_alert`

---

## 6. 关联文档

- [GATE-S6 报告](./gates/GATE-S6-报告-20260609.md)
- [API-M5](../engineering/API-M5-财务管理.md) · [API-M6](../engineering/API-M6-数据分析.md) · [API-M7](../engineering/API-M7-作品监测.md)
