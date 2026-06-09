# SLICES-M10-数据采集

> **切片计划**：M10 数据采集
> **版本**：v1.0 | 2026-06-07
> **总切片数**：5 片 | 预估工时：约 15 人日

---

| Slice | 目标 | FR | 依赖 | 工时 | 优先级 |
|-------|------|----|------|------|--------|
| S-01 | 采集任务 CRUD | FR-M10-001 (1/2) | - | 3.0 | P0 |
| S-02 | Spring 调度 + 重试 | FR-M10-001 (2/2) | S-01 | 4.0 | P0 |
| S-03 | 采集日志 + 监控 | FR-M10-001 | S-01 | 2.0 | P0 |
| S-04 | 数据质量检查 | FR-M10-002 (1/2) | - | 3.0 | P0 |
| S-05 | 质量规则 + 告警 | FR-M10-002 (2/2) | S-04 | 3.0 | P0 |

---

## 全局规范

- `accountId` 用 `<AccountSelect />`（**强关联** ⭐）
- `platformType` / `method` / `source` / `frequency` / `status` 用 `<DictSelect />`
- `checkType` / `level` 用 `<DictSelect />`
- `apiConfig` 加密存储

---

*下一步：CHECKLIST + TESTCASES。*

---

## 全局规范引用

> 本切片文档遵循 [`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md) 中定义的全局规范：
> - 强关联属性 → 5 类选择器组件（RealNameSelect / PhoneSelect / SimCardSelect / CompanySelect / AccountSelect）
> - 枚举属性 → 统一从数据字典（`dict_*`）选择
> - 跨租户 + 状态校验 → 错误码 1500-1504
> - 数据安全 → 敏感字段脱敏展示，凭证类字段 AES-256 加密存储
> - 详见 [`GLOBAL-CONVENTIONS.md § 1`](../engineering/GLOBAL-CONVENTIONS.md) (铁律)、[`§ 2`](../engineering/GLOBAL-CONVENTIONS.md) (字典)

---

## AC 映射表（验收条件）

每个 Slice 都对应 PRD 中的一个或多个 AC（Acceptance Criteria），保证可追溯。

| Slice ID | 关联 AC | 标题 | 估时 |
|----------|---------|------|------|
| S-M10-01 | AC-M10-01 | 采集任务 CRUD | 1.5d |
| S-M10-02 | AC-M10-02 | 凭据加密入库（AES-256） | 1d |
| S-M10-03 | AC-M10-03 | 代理池管理 | 1d |

### 估算单位
- `d` = 人天（1 人 = 8 小时）
- 总估时 = sum of all slices

### 与测试用例的映射
每个 AC 对应 [`TESTCASES-*.md`](../delivery/) 中的 TC-F-* 用例。
