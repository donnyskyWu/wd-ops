# API-M6-数据分析

> **版本**：v1.0 | 2026-06-07
> **关联 PRD**：[`PRD-M6-数据分析.md`](../product/PRD-M6-数据分析.md)
> **关联全局规范**：[`GLOBAL-CONVENTIONS.md`](./GLOBAL-CONVENTIONS.md)

---

## 1. 指标 API

### 1.1 GET `/admin-api/oa/metric/list`

| 参数 | 字典 |
|------|------|
| metricType | `dict_perf_metric_type` |

### 1.2 POST `/admin-api/oa/metric/create`

```json
{
  "metricName": "推文发布数",
  "metricCode": "ARTICLE_PUBLISH_COUNT",
  "metricFormula": "SELECT COUNT(*) FROM oa_content WHERE content_type='ARTICLE'",
  "dataSource": "oa_content",
  "metricType": "BASIC",
  "unit": "篇",
  "description": "..."
}
```

**校验**：
- `metricName` `@NotBlank`
- `metricCode` 英文+下划线+唯一
- `metricType` `@InDict(type="dict_perf_metric_type")`

### 1.3 PUT `/admin-api/oa/metric/update`

### 1.4 DELETE `/admin-api/oa/metric/{id}`

**业务**：被引用 → 错误码 1502

---

## 2. 报表 API（8 张）

### 2.1 全平台账号视图

- `GET /admin-api/oa/report/unified-account/list`
- `GET /admin-api/oa/report/unified-account/stats`
- `POST /admin-api/oa/report/unified-account/export`

### 2.2 账号状态监控

- `GET /admin-api/oa/report/account-status/trend`
- `GET /admin-api/oa/report/account-status/summary`
- `GET /admin-api/oa/report/account-status/log`
- `POST /admin-api/oa/report/account-status/export`

### 2.3 短视频产出

- `GET /admin-api/oa/report/video-output/list`
- `GET /admin-api/oa/report/video-output/trend`
- `GET /admin-api/oa/report/video-output/ranking`
- `POST /admin-api/oa/report/video-output/export`

### 2.4 直播时长

- `GET /admin-api/oa/report/live-duration/list`
- `GET /admin-api/oa/report/live-duration/trend`
- `POST /admin-api/oa/report/live-duration/export`

### 2.5 账号成本分摊

- `GET /admin-api/oa/report/cost-allocation/list`
- `POST /admin-api/oa/report/cost-allocation/export`

### 2.6 ROI 分析报表

- `GET /admin-api/oa/report/roi/list`
- `POST /admin-api/oa/report/roi/export`

### 2.7 IP 团队人员配置

- `GET /admin-api/oa/report/team-config/list`

### 2.8 账号异常预警

- `GET /admin-api/oa/report/account-alert/list`
- `POST /admin-api/oa/report/account-alert/export`

**通用请求参数**：

| 参数 | 类型 | 字典 |
|------|------|------|
| ipGroupId | Long | - |
| accountId | Long | - |
| platformType | String | `dict_platform_type` |
| startDate / endDate | Date | - |
| timeDimension | String | 固定 DAY/WEEK/MONTH |

---

## 3. 漏斗 API

### 3.1 GET `/admin-api/oa/funnel/list`

### 3.2 POST `/admin-api/oa/funnel/create`

```json
{
  "funnelName": "我的漏斗",
  "funnelType": "CUSTOM",
  "steps": [
    {"stepOrder": 1, "eventCode": "VIEW"},
    {"stepOrder": 2, "eventCode": "FOLLOW"}
  ]
}
```

**校验**：
- `funnelType` `@InDict(type="dict_funnel_type")`

### 3.3 GET `/admin-api/oa/funnel/{id}/data`

**响应**：

```json
{
  "funnelId": 1,
  "steps": [
    {"stepOrder": 1, "name": "曝光", "count": 10000, "conversionRate": 100.0},
    {"stepOrder": 2, "name": "关注", "count": 3000, "conversionRate": 30.0},
    {"stepOrder": 3, "name": "二次访问", "count": 800, "conversionRate": 8.0}
  ]
}
```

### 3.4 POST `/admin-api/oa/funnel/export`

---

## 4. 自定义查询 API

### 4.1 GET `/admin-api/oa/query/list`

| 参数 | 字典 |
|------|------|
| status | `dict_query_status` |

### 4.2 POST `/admin-api/oa/query/create`

```json
{
  "queryName": "近 30 天抖音粉丝增长",
  "status": "DRAFT",
  "sqlText": "SELECT date, count FROM ...",
  "params": {}
}
```

**校验**：
- `status` `@InDict(type="dict_query_status")`
- `sqlText` SQL 注入检查

### 4.3 POST `/admin-api/oa/query/{id}/execute`

### 4.4 POST `/admin-api/oa/query/{id}/publish`

---

## 5. 大屏 API

### 5.1 GET `/admin-api/oa/dashboard/{id}`

### 5.2 POST `/admin-api/oa/dashboard/create`

```json
{
  "dashboardName": "运营总览大屏",
  "dashboardType": "BUSINESS",
  "layout": "[{...}, ...]"
}
```

**校验**：
- `dashboardType` `@InDict(type="dict_dashboard_type")`

### 5.3 GET `/admin-api/oa/dashboard-config/list`

### 5.4 POST `/admin-api/oa/dashboard-config/update`

---

## 6. 错误码

| 错误码 | 含义 |
|--------|------|
| 1500~1504 | 全局关联 |
| 1503 | 字典值不合法 |
| 2017 | SQL 注入风险 |
| 2018 | 公式语法错误 |

---

*下一步：STATE / SLICES / CHECKLIST / TESTCASES。*

---

## 字典与数据安全（🔴 API 必含）

### 字典使用（dict-type）

| 字段 | dict-type | 取值约束 |
|------|-----------|----------|
| report_dim | `dict_report_dim` | 字典合法值，错误码 1503 |
| metric_type | `dict_metric_type` | 字典合法值，错误码 1503 |
| export_format | `dict_export_format` | 字典合法值，错误码 1503 |
| time_range | `dict_time_range` | 字典合法值，错误码 1503 |

### 数据安全

- **脱敏**：`导出报表中的姓名、手机号` 在 API 响应中按权限脱敏（前端展示如 `138****1234`）
- **加密**：所有凭证字段在数据库中以 **AES-256** 加密存储（key 由配置中心注入）
- **日志**：所有写操作记录 `sys_audit_log`（含 operatorId / operation / before / after）
- **传输**：HTTPS + JWT；敏感字段二次加密（前端公钥 + 后端私钥）

详见 [`GLOBAL-CONVENTIONS.md § 5`](./GLOBAL-CONVENTIONS.md) (数据安全)

---

## 字段字典映射表（🔴 API 必含）

下表列出 数据分析 模块所有需要走数据字典的字段。前端使用 `<DictSelect dict-type="..." />` 组件，后端 `@InDict` 注解校验。

| 字段 | dict-type | 取值范围 | 错误码（不合法时） |
|------|-----------|----------|--------------------|
| `reportType` | `dict_report_type` | 日报/周报/月报/自定义 | 1503 |
| `dim` | `dict_report_dim` | 平台/IP组/作者/时间 | 1503 |
| `metric` | `dict_metric_type` | 浏览/点赞/评论/转化 | 1503 |
| `exportFormat` | `dict_export_format` | Excel/CSV/PDF | 1503 |
| `timeRange` | `dict_time_range` | 今日/本周/本月/自定义 | 1503 |

### 使用规范

1. **前端**：`<DictSelect v-model="form.reportType" dict-type="dict_report_type" />`
2. **后端**：`@InDict("dict_report_type") private String reportType;`
3. **字典维护**：在 M8 配置管理模块维护
4. **错误码**：字典值不在范围内 → 1503

详见 [`GLOBAL-CONVENTIONS.md § 2`](./GLOBAL-CONVENTIONS.md) (字典)
