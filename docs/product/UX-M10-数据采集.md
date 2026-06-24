# UX-M10-数据采集

> **版本**：v1.1 | 2026-06-24
> **关联 PRD**：[`PRD-M10-数据采集.md`](./PRD-M10-数据采集.md)
> **全局规范**：[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 | 路由 | FR |
|------|------|-----|
| 采集任务列表 | `/collect/task` | FR-M10-001 |
| 采集任务编辑 | `/collect/task/:id` | FR-M10-001 |
| 采集日志 | `/collect/log` | FR-M10-001 |
| 数据质量检查 | `/collect/quality` | FR-M10-002 |

---

## 2. P-M10-001 采集任务列表

| 控件 | 类型 | 字典 |
|------|------|------|
| F-NAME | `<Input />` | - |
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` | 字典 |
| F-METHOD | `<DictSelect dict-type="dict_collect_method" />` | 字典 |
| F-FREQUENCY | `<DictSelect dict-type="dict_collect_frequency" />` | 字典 |
| F-STATUS | `<DictSelect dict-type="dict_collect_status" />` | 字典 |
| TBL-TASK | 表格 | `oa_collect_task` |
| BTN-ADD | "新增任务" | - |
| BTN-RUN | "立即执行" | - |
| BTN-LOG | "查看日志" | - |

---

## 3. P-M10-002 采集任务编辑

### 3.1 基本信息

| 控件 | 类型 | 字典 | 必填 |
|------|------|------|------|
| F-NAME | `<Input />` | - | ✅ |
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` | 字典 | ✅ |
| **F-ACCOUNT** | **`<AccountSelect />`** 或企微 **`<WeworkAccountSelect />`** | `oa_account` / `oa_wework_account` | ✅ |
| F-COLLECT-SCOPE | 只读 `<el-tag>` 列表（采集范围） | 平台默认 dataTypes | - |
| ~~F-METHOD~~ | ~~隐藏~~ — 后端默认 `INTERNAL` | — | — |
| ~~F-SOURCE~~ | ~~隐藏~~ — 后端按平台默认 | — | — |
| ~~F-DATA-TYPE~~ | ~~隐藏~~ — 存库 null = 全量 | — | — |
| F-FREQUENCY | `<DictSelect dict-type="dict_collect_frequency" />` | 字典 | ✅ |
| F-CRON | `<Input />`（cron 表达式） | - | ✅ |
| F-API-CONFIG | `<TextArea />`（API 配置 JSON） | - | - |
| F-STATUS | `<DictSelect dict-type="dict_collect_status" />` | 字典 | ✅ |

### 3.2 监控信息

| 控件 | 类型 |
|------|------|
| LAST-RUN-AT | `<DateTimePicker />`（只读） |
| NEXT-RUN-AT | `<DateTimePicker />`（只读） |
| RUN-COUNT | `<InputNumber />`（只读） |
| FAIL-COUNT | `<InputNumber />`（只读） |

---

## 4. P-M10-003 采集日志

| 控件 | 类型 |
|------|------|
| F-TASK | `<Select />`（所有任务） |
| F-STATUS | `<DictSelect dict-type="dict_collect_status" />`（含 `PARTIAL` 部分成功） |
| F-DATE-RANGE | `<DateRangePicker />` |
| TBL-LOG | 表格（含耗时、错误信息、状态） |
| BTN-DETAIL | 打开详情抽屉 |
| DETAIL-TYPE-RESULTS | 多类型时折叠面板展示 `typeResults`（每类型 recordCount / 错误 / 样本） |

**日志详情 API**：`GET /admin-api/oa/collect/log/{id}` → `result.typeResults[]`（ADR-049）。

---

## 5. P-M10-004 数据质量检查

| 控件 | 类型 | 字典 |
|------|------|------|
| F-NAME | `<Input />` | - |
| F-CHECK-TYPE | `<DictSelect dict-type="dict_quality_check_type" />` | 字典 |
| F-LEVEL | `<DictSelect dict-type="dict_quality_level" />` | 字典 |
| TBL-CHECK | 表格 | `oa_data_quality_check` |
| TBL-LOG | 表格 | `oa_data_quality_log` |

---

## 6. 跨页通用

- **强关联**：`accountId` 强制选择器
- **字典**：5 个 `dict_collect_*` + `dict_quality_*` 全部用 `<DictSelect />`
- **凭证**：`apiConfig` 输入框 + 加密存储
- **空/错/加载**：三态完整

---

*下一步：API / STATE / SLICES / CHECKLIST / TESTCASES。*
