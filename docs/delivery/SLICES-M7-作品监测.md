# SLICES-M7-作品监测

> **切片计划**：M7 作品监测
> **版本**：v1.0 | 2026-06-07
> **总切片数**：4 片 | 预估工时：约 10 人日

---

| Slice | 目标 | FR | 工时 | 优先级 |
|-------|------|----|------|--------|
| S-01 | 外部账号分析 | FR-M7-001 | 3.0 | P0 |
| S-02 | 爆款/低分作品 | FR-M7-001 | 3.0 | P0 |
| S-03 | 高/低粉账号 | FR-M7-002 | 2.0 | P0 |
| S-04 | IP 主题 + 行业分析 | FR-M7-002 | 2.0 | P0 |

---

## 全局规范

- `platformType` 用 `<DictSelect dict-type="dict_platform_type" />`
- `ipGroupId` 用 `<IpGroupTreeSelect />`
- `accountId` 用 `<AccountSelect />`

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
| S-M7-01 | AC-M7-01 | 作品录入（URL 校验） | 0.5d |
| S-M7-02 | AC-M7-02 | 监测任务配置（频率字典） | 1d |
| S-M7-03 | AC-M7-03 | 告警生成（级别字典） | 1d |

### 估算单位
- `d` = 人天（1 人 = 8 小时）
- 总估时 = sum of all slices

### 与测试用例的映射
每个 AC 对应 [`TESTCASES-*.md`](../delivery/) 中的 TC-F-* 用例。
