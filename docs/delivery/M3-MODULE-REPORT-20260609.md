# M3 绩效核算 · 模块交付报告

> **模块**：M3  
> **Gate**：GATE-S5（后端 P0 已交付）  
> **日期**：2026-06-09  
> **Flyway**：V21 → V22

---

## 1. Slice 完成度

| Slice | 目标 | 状态 | IT |
|-------|------|------|-----|
| S-01 | 考核模板 CRUD | ✅ | M3PerfTemplateS01IT |
| S-02 | 区间编辑器 + 权重校验 | ✅ | M3PerfTemplateS01IT |
| S-03 | 考核执行（自动算分） | ✅ | M3PerfRecordS03IT |
| S-04 | 人工调整 ±20% | ✅ | M3PerfRecordS03IT |
| S-05 | 绩效结果 + 趋势 | ✅ | M3PerfRecordS03IT |
| S-06 | 订单归因 + ROI | ✅ | M3OrderAttributionS06IT |

---

## 2. 数据库

| 迁移 | 内容 |
|------|------|
| V21 | `oa_metric` · `oa_perf_template` · `oa_perf_template_item` · `oa_perf_record` · `oa_perf_item_record` · `oa_order` · `oa_order_attribution` + 字典 |
| V22 | **seed-perf**：模板×2（OPS_LEADER/OPERATOR）、考核记录×5、订单×12 + 归因链 |

---

## 3. API 清单

| 域 | 路径 | 说明 |
|----|------|------|
| 模板 | `/oa/perf/template/*` | list · create · update · activate · `{id}/items` |
| 记录 | `/oa/perf/record/*` | list · create · calculate · adjust · detail · confirm |
| 结果 | `/oa/perf/result/*` | list · `{userId}/trend` · export（stub） |
| 归因 | `/oa/order-attribution/*` | list · roi · export（stub） |

**错误码**：2008 重复周期 · 2009 调整超限 · 3001 权重≠100% · 2011/2012 区间 gap/重叠

**测试**：`mvn test` → **123/123 全绿**（含 GATE-S0~S4 回归）

---

## 4. 前端

- `perfTemplate.ts` / `perfRecord.ts` / `perfResult.ts` / `orderAttribution.ts` 已切换 `@/utils/request`

---

## 5. 待补（P1 / Gate 外）

- S-02 区间可视化编辑器（前端 UI）
- 绩效/归因导出异步任务完整实现
- 独立指标管理 CRUD 页面

---

## 6. 关联文档

- [API-M3-绩效核算](../engineering/API-M3-绩效核算.md)
- [SLICES-M3-绩效核算](./SLICES-M3-绩效核算.md)
- [GATE-S5 报告](./gates/GATE-S5-报告-20260609.md)
