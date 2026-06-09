# CHECKLIST-M3-绩效核算

> **M3 自检清单** | 版本 v1.0 | 2026-06-07
> **关联**：[`SLICES-M3-绩效核算.md`](./SLICES-M3-绩效核算.md)、[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---

## 1. 范围与 FR

- [ ] 6 个 Slice 全部完成
- [ ] 4 个 FR（FR-M3-001~004）全部实现
- [ ] 17 个 API 实现完整

## 2. 功能

- [ ] 模板 CRUD（AC-M3-001-1, 5）
- [ ] 区间编辑器（AC-M3-001-2, 3, 4）
- [ ] 考核执行（AC-M3-002-1, 2, 5, 6）
- [ ] 人工调整（AC-M3-002-3, 4）
- [ ] 绩效结果（AC-M3-003-1, 2, 3）
- [ ] 订单归因 + ROI（AC-M3-004-1, 2, 3）

## 3. 全局规范（🔴 必查）

- [ ] `position` 用 `<DictSelect dict-type="dict_position" />`
- [ ] `metricId` 用 `<MetricSelect />`
- [ ] `calcRule` 用 `<DictSelect dict-type="dict_perf_calc_method" />`
- [ ] `targetUserId` 用 `<UserSelect />`
- [ ] `periodType` 用 `<DictSelect dict-type="dict_perf_period" />`
- [ ] `status` 用 `<DictSelect dict-type="dict_perf_status" />`
- [ ] `ipGroupId` 用 `<IpGroupTreeSelect />`
- [ ] `accountId` 用 `<AccountSelect />`
- [ ] 所有 `*_id` 强制传 ID
- [ ] 跨租户隔离
- [ ] 错误码符合 § 5

## 4. 状态机

- [ ] 考核记录 7 状态完整实现
- [ ] 模板 3 状态完整实现
- [ ] 驳回/重提/确认路径正确

## 5. UX 一致性

- [ ] 区间编辑器与 UX-M3-002 一致
- [ ] 考核详情/调整弹窗与 UX-M3-004 一致
- [ ] 趋势图与 UX-M3-006 一致
- [ ] ROI 卡片与 UX-M3-008 一致

## 6. 数据 & 安全

- [ ] 考核人/被考核人权限隔离
- [ ] 调整幅度强制 ±20%
- [ ] 确认后不可修改
- [ ] 跨租户隔离

## 7. 性能

- [ ] 自动算分（100 指标）≤ 5s
- [ ] 趋势分析 ≤ 2s
- [ ] ROI 聚合 ≤ 1s

## 8. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过（19 条）
- [ ] 区间边界（gap/重叠）测试
- [ ] 权限隔离测试

## 9. 文档

- [ ] PRD 同步
- [ ] Swagger 文档生成
- [ ] 字典项已登记

## 10. Sign-off

| 角色 | 姓名 | 日期 | 签名 |
|------|------|------|------|
| 开发 |  |  |  |
| 测试 |  |  |  |
| 产品 |  |  |  |
| 架构 |  |  |  |
