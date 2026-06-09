# M0 首页 · 模块交付报告

> **模块**：M0  
> **Gate**：GATE-S7 ✅  
> **日期**：2026-06-09  
> **Flyway**：V26

---

## 1. Slice 完成度

| Slice | 目标 | 状态 | IT |
|-------|------|------|-----|
| S-01 | 顶栏 + 4 指标卡 | ✅ | M0HomeS01IT |
| S-02 | 趋势 + 平台分布 | ✅ | M0HomeS01IT |
| S-03 | 待办 + 快捷入口 | ✅ | M0HomeS01IT |

---

## 2. 数据库

| 迁移 | 内容 |
|------|------|
| V26 | `oa_home_alert`（3 条 seed 告警）、tenant=2 IP 组 8001（跨租户测试）、快捷入口权限 |

---

## 3. API 清单

| 端点 | 说明 |
|------|------|
| `GET /dashboard/home/kpi` | 前端 KPI 卡片（账号/粉丝/内容/待审/预警） |
| `GET /dashboard/home/account-overview` | 平台账号分布 |
| `GET /dashboard/home/content-overview` | 近 7 天内容趋势 |
| `GET /dashboard/home/alert-list` | 告警列表 |
| `GET /dashboard/home/todo-list` | 待办汇总 |
| `GET /dashboard/home/metrics` | API-M0 四指标 |
| `GET /dashboard/home/trend` | 内容/粉丝趋势 |
| `GET /dashboard/home/platform-dist` | 平台分布 |
| `GET /dashboard/home/todos` | 待办明细 |
| `GET /dashboard/home/quick-actions` | 权限过滤快捷入口 |
| `POST /dashboard/home/refresh` | 清缓存 |

**数据来源**：真实 seed 表聚合（非 Mock）  
**缓存**：ConcurrentHashMap，TTL 5 分钟  
**测试**：`mvn test` → **151/151** 全绿（含 GateS7E2EIT×9）

---

## 4. 前端

- `dashboard.ts`：保留原有 5 个接口 + 新增 API-M0 函数
- `Dashboard.vue`：已对接 API（失败时降级 Mock）

---

## 5. 关联文档

- [API-M0-首页](../engineering/API-M0-首页.md)
- [GATE-S7 报告](./gates/GATE-S7-报告-20260609.md)
