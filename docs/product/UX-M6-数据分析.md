# UX-M6-数据分析

> **版本**：v1.3 | 2026-06-12
> **关联 PRD**：[`PRD-M6-数据分析.md`](./PRD-M6-数据分析.md)
> **全局规范**：[`GLOBAL-CONVENTIONS.md`](../engineering/GLOBAL-CONVENTIONS.md)

---


> **视觉规范参考**：[`开发规范/UI设计与开发规范.md`](../../开发规范/UI设计与开发规范.md)（仅原型/设计阶段）
> **实现技术栈**：[`TECH-CONSTRAINTS.md § 1.2`](../engineering/TECH-CONSTRAINTS.md)（Vue 3 + Element Plus）
> **决策记录**：[`ADR-002`](../../adr/ADR-002-前端规范源选择.md)

## 1. 页面清单

| 页面 ID | 名称 | 路由 | FR |
|---------|------|------|-----|
| P-M6-001 | 指标管理 | `/analysis/metric` | FR-M6-001 |
| P-M6-002 | 全平台账号视图 | `/analysis/report/unified-account` | FR-M6-002 |
| P-M6-003 | 账号状态监控 | `/analysis/report/account-status` | FR-M6-002 |
| P-M6-004 | 短视频产出统计 | `/analysis/report/video-output` | FR-M6-002 |
| P-M6-005 | 直播时长统计 | `/analysis/report/live-duration` | FR-M6-002 |
| P-M6-006 | 账号成本分摊 | `/analysis/report/cost-allocation` | FR-M6-002 |
| P-M6-007 | ROI 分析报表 | `/analysis/report/roi` | FR-M6-002 |
| P-M6-008 | IP 团队人员配置 | `/analysis/report/team-config` | FR-M6-002 |
| P-M6-009 | 账号异常预警 | `/analysis/report/account-alert` | FR-M6-002 |
| P-M6-010 | 财务概览 | `/analysis/finance-overview` | FR-M6-003 |
| P-M6-011 | 漏斗分析（预置） | `/analysis/funnel` | FR-M6-004 |
| P-M6-012 | 漏斗分析（自定义） | `/analysis/funnel/custom` | FR-M6-004 |
| P-M6-013 | 自定义查询 | `/analysis/query` | FR-M6-005 |
| P-M6-014 | 数据大屏 | `/analysis/dashboard/:id` | FR-M6-006 |
| P-M6-015 | 大屏配置 | `/analysis/dashboard-config` | FR-M6-007 |

---

## 2. 通用筛选条

所有报表页：

| 控件 | 类型 | 字典 |
|------|------|------|
| F-IP | `<IpGroupTreeSelect />` | `oa_ip_group` |
| F-PLATFORM | `<DictSelect dict-type="dict_platform_type" />` | 字典 |
| F-DATE-RANGE | `<DateRangePicker />` | - |
| F-TIME-DIM | `<Select />` | DAY/WEEK/MONTH |
| F-REPORT-TYPE | `<DictSelect dict-type="dict_report_type" />` | 字典 |

---

## 3. P-M6-001 指标管理

| 控件 | 类型 |
|------|------|
| F-NAME | `<Input />` |
| F-CODE | `<Input />` |
| F-METRIC-TYPE | `<DictSelect dict-type="dict_perf_metric_type" />` |
| F-BUILDER | `<MetricBuilder />`（非 COMPOSITE） | 数据源/计算/汇总/joins/过滤 → SQL |
| F-FORMULA | `<CodeEditor />` 或 Builder 输出（COMPOSITE 仅手输） |
| F-DESCRIPTION | `<TextArea />` | 仅前端，未持久化 |
| BTN-PREVIEW | 按钮 | 调用 `POST /metric/preview` |
| TBL-METRIC | 表格 |

---

## 4. P-M6-002~009 八张报表

布局统一：

```
+----------------------------------------------------------+
| 筛选条：[IP组] [平台] [日期] [时间维度] [报表类型] [查询]|
+----------------------------------------------------------+
| 汇总卡：核心指标                                          |
+----------------------------------------------------------+
| 图表：折线 / 柱状 / 饼图                                  |
+----------------------------------------------------------+
| 明细表                                                    |
+----------------------------------------------------------+
| [导出 Excel] [导出 PDF]                                   |
+----------------------------------------------------------+
```

详细字段见各报表详细设计（5.26.A~H）。

**实现**：表格列绑定 snake_case 字段；枚举列用 `<DictLabel />`。

---

## 5. P-M6-011 / P-M6-012 漏斗分析

### 5.1 预置漏斗

| 漏斗 | 步骤 |
|------|------|
| 关注转化 | 曝光 → 关注 → 二次访问 |
| 阅读转化 | 推送 → 阅读 → 完读 |
| 互动转化 | 阅读 → 点赞 → 评论 → 转发 |
| 订单转化 | 加购 → 提交订单 → 支付 |

### 5.2 自定义漏斗（P-M6-012）

- 步骤配置：每步 `<Select />` 选 `oa_metric`（`/metric/list`）
- 无预设 eventCode 下拉

### 5.3 漏斗图

```
[曝光 100%] ████████████
[关注 30%]  ████
[二次 8%]   █
```

---

## 6. P-M6-013 自定义查询

### 6.1 页级 Tab

```
[自定义查询] [我的查询]
```

### 6.2 自定义查询 Tab 布局

```
[可折叠: 查询配置]
  QueryBuilder（数据源 / 字段 / 过滤 / 聚合 / 排序）
  [预览] [保存]
[QueryResultPanel]
  条件摘要条
  [结果列表 | 图表展示]
  - 结果列表：中文表头
  - 图表展示：基于当前结果（柱状/折线）
```

### 6.3 我的查询 Tab

已保存查询表格：名称、状态、更新时间；操作：执行 / 编辑 / 删除。

### 6.4 状态

| 控件 | 字典 |
|------|------|
| F-STATUS | `<DictSelect dict-type="dict_query_status" />` |

---

## 7. P-M6-014 数据大屏（全屏）

路由：`/analysis/dashboard/:id`（`DataScreenFullscreen.vue`）

### 7.1 顶栏全局筛选

| 控件 ID | 类型 | 说明 |
|---------|------|------|
| F-SCOPE | 按钮组 | 内部数据 / 外部数据（切换模板 scope） |
| F-DATE-RANGE | 原生 `<select>` 暗色 | 今日 / 近7天 / 近30天 |
| F-IP-GROUP | 原生 `<select>` 暗色 | 全部 IP 组 + 树扁平选项 |
| F-PLATFORM | 原生 `<select>` 暗色 | 全部平台 + `dict_platform_type` |
| F-REFRESH | 原生 `<select>` 暗色 | 30s / 1min / 5min / 不刷新 |

- 筛选项变更 → 调用 `GET /oa/dashboard/{id}/data` 并刷新图表
- 与配置页预览区筛选参数一致（`buildDashboardDataQuery`）

### 7.2 内容区布局

```
+----------------------------------------------------------+
| 顶栏：标题 | 模板 | [内部|外部] | 日期 | IP组 | 平台 | 刷新 |
+----------------------------------------------------------+
| KPI 网格（最多 6 列）                                     |
+----------------------------------------------------------+
| [今日工作概览] STAT 网格（仅 INTERNAL scope 显示标题）    |
+----------------------------------------------------------+
| 图表行 ×2（两列）                                         |
| 末行：单图 + 列表并排 / 或列表通栏                        |
+----------------------------------------------------------+
```

- 暗色主题；样式对齐 `dashboard.html` 原型
- ECharts 渲染 CHART；`ScreenListTable` 渲染 LIST

### 7.3 组件行为

| type | 全局日期 | IP 组 / 平台 |
|------|----------|--------------|
| KPI | 生效 | 生效 |
| STAT | **不生效**（固定当天） | 生效 |
| CHART / LIST | 生效 | 生效 |

### 7.4 大屏类型

| 控件 | 字典 |
|------|------|
| F-DASHBOARD-TYPE | `<DictSelect dict-type="dict_dashboard_type" />` |

---

## 8. P-M6-015 大屏配置

路由：`/analysis/dashboard-config`（`ScreenConfig.vue`）

### 8.1 布局

```
+----------------------------------------------------------+
| 工具栏：模板选择 | 保存 | 预览大屏                        |
+----------------------------------------------------------+
| 左栏（配置）          | 右栏（预览 + 说明）                |
| - 模板信息            | - 预览筛选条（同全屏）             |
| - 指标卡（≤6）        | - ScreenPreviewPanel              |
| - 今日统计            | - 配置说明                         |
| - 图表（≤4）          |                                    |
| - 列表                |                                    |
+----------------------------------------------------------+
```

### 8.2 组件编辑弹窗

| 数据源 | 特有配置 |
|--------|----------|
| BUILTIN | 内置键下拉；图表轴只读 |
| METRIC | 指标下拉；KPI/STAT 值字段 |
| QUERY | 已发布查询下拉 |

**CHART（METRIC/QUERY）**：图表类型 + X/Y 轴字段下拉（预览 SQL 列动态补全）

**LIST**：列勾选（checkbox）、排序字段、条数 limit

### 8.3 全局筛选映射（METRIC / QUERY）

弹窗内「全局筛选映射」分区：

| 控件 | 类型 | 说明 |
|------|------|------|
| F-GF-DATE | `<Select />` 可清空 | 日期字段（`metricSchema` 可过滤日期列） |
| F-GF-IP | `<Select />` 可清空 | IP 组字段（`metricSchema` 可过滤 IP 组列） |

- 选项随所选指标/查询的数据源变化
- 保存时写入 widget `globalFilter`（含 `dateColumn` / `ipGroupColumn`）
- 平台筛选：内置组件自动生效；自定义 SQL 平台过滤见 ADR-015

### 8.4 预览区筛选

与 P-M6-014 顶栏一致：`IpGroupTreeSelect` + `DictSelect dict_platform_type` + 日期快捷项。

---

## 9. 跨页通用

- 所有 IP/账号 强制选择器
- 所有 报表类型/平台/状态 用 `<DictSelect />`
- 大屏：暗色主题 + 自动刷新

---

*下一步：API / STATE / SLICES / CHECKLIST / TESTCASES。*
