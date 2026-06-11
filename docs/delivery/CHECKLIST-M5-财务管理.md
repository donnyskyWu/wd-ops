# CHECKLIST-M5-财务管理

> **M5 自检清单** | 版本 v1.1 | 2026-06-11
> **关联**：[`SLICES-M5-财务管理.md`](./SLICES-M5-财务管理.md)、[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---

## 1. 范围与 FR

- [ ] 4 个 Slice 全部完成
- [ ] 2 个 FR 全部实现
- [ ] 9 个 API 实现完整

## 2. 功能

- [ ] 账号成本 CRUD（AC-M5-001-1, 2, 3）
- [ ] ROI 实时（AC-M5-002-1, 2）
- [ ] ROI 趋势 + 导出（AC-M5-002-3）

## 3. 全局规范（🔴 必查）

- [ ] `accountId` 用 `<AccountSelect />`
- [ ] `costType` 用 `<DictSelect dict-type="dict_cost_type" />`
- [ ] `payMethod` 用 `<DictSelect dict-type="dict_cost_pay_method" />`
- [ ] `period` 用 `<DictSelect dict-type="dict_cost_period" />`
- [ ] 成本详情抽屉 platform/costType/period 用 `<DictLabel />` 展示
- [ ] `ipGroupId` 用 `<IpGroupTreeSelect />`
- [ ] 所有 `*_id` 强制 ID
- [ ] 跨租户隔离
- [ ] 错误码符合 § 3

## 4. 状态机

- 不适用（成本 + ROI 无状态机）

## 5. UX 一致性

- [ ] 列表 / 录入弹窗 / ROI 卡片 / 趋势图
- [ ] Skeleton / Error / Empty 三态

## 6. 数据 & 安全

- [ ] 金额精度（DECIMAL(10,2)）
- [ ] 凭证本地存储
- [ ] 跨租户隔离
- [ ] 财务月结 0 误差

## 7. 性能

- [ ] ROI 实时聚合 ≤ 2s
- [ ] 趋势分析 ≤ 3s

## 8. 测试

- [ ] 单元测试覆盖 80%+
- [ ] AC 全部通过（6 条）
- [ ] 财务月结回归

## 9. 文档

- [ ] PRD 同步
- [ ] Swagger 文档
- [ ] 字典项已登记

## 10. Sign-off

| 角色 | 姓名 | 日期 | 签名 |
|------|------|------|------|
| 开发 |  |  |  |
| 测试 |  |  |  |
| 产品 |  |  |  |
| 财务 |  |  |  |
