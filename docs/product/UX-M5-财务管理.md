# UX-M5-财务管理

> **版本**：v1.0 | 2026-06-07
> **关联 PRD**：[`PRD-M5-财务管理.md`](./PRD-M5-财务管理.md)
> **全局规范**：[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 ID | 名称 | 路由 | 关联 FR |
|---------|------|------|---------|
| P-M5-001 | 账号成本列表 | `/finance/cost` | FR-M5-001 |
| P-M5-002 | 账号成本录入 | `/finance/cost/edit` | FR-M5-001 |
| P-M5-003 | ROI 分析 | `/finance/roi` | FR-M5-002 |
| P-M5-004 | ROI 趋势 | `/finance/roi/trend` | FR-M5-002 |

---

## 2. P-M5-001 账号成本列表

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-ACCOUNT | `<AccountSelect />` | `oa_account` |
| F-COST-TYPE | `<DictSelect dict-type="dict_cost_type" />` | 字典 |
| F-PAY-METHOD | `<DictSelect dict-type="dict_cost_pay_method" />` | 字典 |
| F-PERIOD | `<DictSelect dict-type="dict_cost_period" />` | 字典 |
| F-DATE-RANGE | `<DateRangePicker />` | - |
| TBL-COST | 表格 | `oa_author_cost` + `oa_cost_process_record` |
| BTN-ADD | "录入成本" | - |

---

## 3. P-M5-002 账号成本录入

| 控件 | 类型 | 字典/实体 | 校验 |
|------|------|----------|------|
| F-ACCOUNT | `<AccountSelect />` | `oa_account` | 必填 |
| F-COST-TYPE | `<DictSelect dict-type="dict_cost_type" />` | 字典 | 必填 |
| F-AMOUNT | `<InputNumber :precision="2" :min="0.01" />` | - | 必填 |
| F-PAY-METHOD | `<DictSelect dict-type="dict_cost_pay_method" />` | 字典 | 必填 |
| F-PAY-DATE | `<DatePicker />` | - | 必填 |
| F-PERIOD | `<DictSelect dict-type="dict_cost_period" />` | 字典 | 必填 |
| F-REMARK | `<TextArea />` | - | - |
| F-ATTACHMENT | `<FileUploader />` | - | - |

---

## 4. P-M5-003 ROI 分析

| 控件 | 类型 | 字典/实体 |
|------|------|----------|
| F-IP | `<IpGroupTreeSelect />` | `oa_ip_group` |
| F-ACCOUNT | `<AccountSelect />` | `oa_account` |
| F-DATE-RANGE | `<DateRangePicker />` | - |
| F-DIMENSION | `<Select />` | 固定值（ip_group/account/person） |
| TBL-ROI | 表格 | 聚合 |

### 4.1 ROI 卡片

```
+---------------------+
|  ROI = 4.20         |
|  营收 100w          |
|  成本 23.8w         |
+---------------------+
```

### 4.2 趋势图

折线图：X 轴时间，Y 轴 ROI。

---

## 5. 跨页通用

- 账号/IP 组选择器强制
- 金额字段 `<InputNumber :precision="2" />`
- Skeleton / Error / Empty 三态

---

*下一步：API / STATE / SLICES / CHECKLIST / TESTCASES。*
