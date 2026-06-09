# CHECKLIST-M6-数据分析

> **M6 自检清单** | 版本 v1.0 | 2026-06-07
> **关联**：[`SLICES-M6-数据分析.md`](./SLICES-M6-数据分析.md)、[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---

## 1. 范围与 FR

- [ ] 9 个 Slice 全部完成
- [ ] 7 个 FR 全部实现
- [ ] 35+ 个 API 实现完整

## 2. 全局规范（🔴 必查）

- [ ] `ipGroupId` 用 `<IpGroupTreeSelect />`
- [ ] `accountId` 用 `<AccountSelect />`
- [ ] `platformType` / `contentType` / `reportType` / `reportPeriod` 用 `<DictSelect />`
- [ ] `funnelType` / `queryStatus` / `dashboardType` 用 `<DictSelect />`
- [ ] `metricType` / `alertType` / `alertLevel` / `alertStatus` 用 `<DictSelect />`
- [ ] 所有 `*_id` 强制 ID
- [ ] 跨租户隔离

## 3. 功能

- [ ] 指标管理（AC-M6-001-1~3）
- [ ] 8 张报表（FR-M6-002）
- [ ] 财务概览（FR-M6-003）
- [ ] 漏斗分析（AC-M6-004-1~3）
- [ ] 自定义查询（AC-M6-005-1~3）
- [ ] 数据大屏（AC-M6-006-1, 2）

## 4. 状态机

- [ ] 自定义查询 3 状态完整
- [ ] 漏斗 / 指标 / 大屏 3 状态

## 5. 性能

- [ ] 报表查询 ≤ 2s
- [ ] 大屏加载 ≤ 3s
- [ ] 漏斗计算 ≤ 1s

## 6. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过
- [ ] SQL 注入测试
- [ ] 字典值校验测试

## 7. 文档

- [ ] PRD 同步
- [ ] Swagger 文档
- [ ] 字典项已登记

## 8. Sign-off

| 角色 | 姓名 | 日期 | 签名 |
|------|------|------|------|
| 开发 |  |  |  |
| 测试 |  |  |  |
| 产品 |  |  |  |
| 数据 |  |  |  |
