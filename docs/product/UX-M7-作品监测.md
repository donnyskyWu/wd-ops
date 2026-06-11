# UX-M7-作品监测

> **版本**：v1.1 | 2026-06-11

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 | 路由 | FR |
|------|------|-----|
| 外部账号分析 | `/monitor/external` | FR-M7-001 |
| 爆款作品分析 | `/monitor/hit` | FR-M7-001 |
| 低分作品分析 | `/monitor/low-score` | FR-M7-001 |
| 高粉账号 | `/monitor/high-follower` | FR-M7-002 |
| 低粉账号 | `/monitor/low-follower` | FR-M7-002 |
| IP 主题分析 | `/monitor/ip-theme/:id` | FR-M7-002 |
| 行业分析 | `/monitor/industry/:id` | FR-M7-002 |

---

## 2. 通用筛选条

| 控件 | 类型 |
|------|------|
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` |
| F-IP | `<IpGroupTreeSelect />` |
| F-INDUSTRY | `<Select />`（固定值） |
| F-DATE-RANGE | `<DateRangePicker />` |

---

## 3. 列表布局

```
+----------------------------------------------------------+
| 筛选条                                                    |
+----------------------------------------------------------+
| 汇总卡：账号数/作品数/平均粉丝数/爆款数/低分数           |
+----------------------------------------------------------+
| 图表：趋势 + 排名                                        |
+----------------------------------------------------------+
| 表格（按字段排序、筛选、导出）                            |
| 操作列「详情」→ 右侧抽屉 480px                           |
+----------------------------------------------------------+
```

### 3.1 详情抽屉（实现 2026-06-11）

| 页面 | 抽屉标题 | 控件 |
|------|---------|------|
| 外部账号分析 | 外部账号详情 | `el-descriptions` 只读 |
| 爆款作品分析 | 爆款作品详情 | 同上 |
| 低分作品分析 | 低分作品详情 | 同上 |
| 高粉账号分析 | 高粉账号详情 | 同上 |
| 低粉账号分析 | 低粉账号详情 | 同上 |

---

*下一步：API / STATE / SLICES / CHECKLIST / TESTCASES。*


---

## 全局规范引用

> 本文档遵循 [`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md) 中定义的全局规范：
> - 强关联属性 → 强制使用 5 类选择器组件（RealNameSelect / PhoneSelect / SimCardSelect / CompanySelect / AccountSelect），禁用手动输入
> - 枚举属性（方式/状态/类型/平台/阶段）→ 统一从数据字典（`dict_*`）选择，页面只读下拉
> - 跨租户 + 状态校验 → 错误码 1500-1504 统一语义
> - 数据安全 → 敏感字段（身份证/手机/API 密钥）强制脱敏展示，凭证类字段 AES-256 加密存储
> - 详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)、[`§ 3`](./GLOBAL-CONVENTIONS.md) (选择器)、[`§ 4`](./GLOBAL-CONVENTIONS.md) (错误码)

