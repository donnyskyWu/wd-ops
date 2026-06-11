# CHECKLIST-M6-数据分析

> **M6 自检清单** | 版本 v1.2 | 2026-06-11
> **关联**：[`SLICES-M6-数据分析.md`](./SLICES-M6-数据分析.md)、[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---

## 1. 范围与 FR

- [x] 9 个 Slice 代码落地（大屏/部分 AC 待 Gate 验收）
- [x] 7 个 FR 实现
- [x] 35+ 个 API 实现完整

## 2. 全局规范（🔴 必查）

- [x] `ipGroupId` 用 `<IpGroupTreeSelect />`
- [x] `accountId` 用 `<AccountSelect />`
- [x] `platformType` / `contentType` / `reportType` / `reportPeriod` 用 `<DictSelect />` / `<DictLabel />`
- [x] `funnelType` / `queryStatus` / `dashboardType` 用 `<DictSelect />`
- [x] `metricType` / `alertType` / `alertLevel` / `alertStatus` 用 `<DictSelect />`
- [x] 所有 `*_id` 强制 ID
- [x] 跨租户隔离

## 3. 功能

- [x] 指标管理（AC-M6-001-1~3）
- [x] V40 `metric_formula` / `data_source` 列已迁移
- [x] MetricBuilder 可视化生成 SQL（BASIC/CALCULATED/DERIVED）
- [x] COMPOSITE 类型手动 formula（无 Builder）
- [x] `POST /metric/preview` 预览可用
- [x] 8 张报表（FR-M6-002）；snake_case 响应 + DictLabel
- [x] `dict_roi_dimension`（V42）
- [ ] 财务概览（FR-M6-003）— 待 Gate 全量验收
- [x] 漏斗分析：自定义步骤绑定 `oa_metric`（V45 种子）
- [x] 自定义查询：双 Tab + QueryBuilder + QueryResultPanel + `/query/preview`
- [ ] 数据大屏（AC-M6-006-1, 2）— 待 Gate 验收

## 4. 状态机

- [x] 自定义查询 DRAFT/PUBLISHED 流转
- [x] 漏斗 / 指标 / 大屏基础状态

## 5. 性能

- [ ] 报表查询 ≤ 2s（待压测）
- [ ] 大屏加载 ≤ 3s
- [ ] 漏斗计算 ≤ 1s

## 6. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过
- [x] SQL 注入测试（preview/execute 经 SqlSafetySupport）
- [x] 字典值校验测试

## 7. 文档

- [x] PRD/UX/API v1.2 同步
- [ ] Swagger 文档
- [x] 字典项已登记（含 dict_roi_dimension）

## 8. Sign-off

| 角色 | 姓名 | 日期 | 签名 |
|------|------|------|------|
| 开发 |  |  |  |
| 测试 |  |  |  |
| 产品 |  |  |  |
| 数据 |  |  |  |
