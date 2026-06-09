# SLICES-M8-配置管理

> **切片计划**：M8 配置管理
> **版本**：v1.0 | 2026-06-07

| Slice | 目标 | FR | 工时 |
|-------|------|----|------|
| S-01 | 内部采集配置 | FR-M8-001 | 2.0 |
| S-02 | 外部采集配置 | FR-M8-002 | 1.5 |
| S-03 | 外部资源/通用采集 | FR-M8-003, 004 | 1.0 |
| S-04 | 阈值配置 | FR-M8-005 | 2.0 |
| S-05 | AI 模型 + 提示词 | FR-M8-006, 007 | 2.0 |

---

## 全局规范

- `platformType` 用 `<DictSelect dict-type="dict_platform_type" />`
- `accountId` 用 `<AccountSelect />`
- `ipGroupId` 用 `<IpGroupTreeSelect />`
- `collectFrequency` / `collectMethod` / `collectStatus` 用 `<DictSelect />`

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
| S-M8-01 | AC-M8-01 | 字典 CRUD（57 个 dict_type） | 1.5d |
| S-M8-02 | AC-M8-02 | 字典禁用前引用检查 | 0.5d |
| S-M8-03 | AC-M8-03 | 系统配置管理（配置中心） | 1d |

### 估算单位
- `d` = 人天（1 人 = 8 小时）
- 总估时 = sum of all slices

### 与测试用例的映射
每个 AC 对应 [`TESTCASES-*.md`](../delivery/) 中的 TC-F-* 用例。
