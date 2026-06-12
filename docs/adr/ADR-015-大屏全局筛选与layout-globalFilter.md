# ADR-015：大屏全局筛选与 layout `globalFilter`

| 字段 | 值 |
|------|---|
| 编号 | ADR-015 |
| 标题 | 大屏全局筛选：业务字段映射 `globalFilter` 取代占位符 `filterBind` |
| 状态 | ✅ Accepted |
| 日期 | 2026-06-12 |
| 触发 | 本会话大屏全局筛选实现 · FR-M6-006/007 补全 |
| 决策人 | 实现会话 |

---

## 1. 背景

大屏全屏页与配置预览需支持 **日期 + IP 组 + 平台** 全局筛选（Q4=B）。

初版实现用 layout `filterBind`：将 SQL 占位符名映射到全局来源（如 `:publish_start` → `startDate`）。问题：

- 配置者需理解 SQL 占位符命名
- 与 MetricBuilder / `metricSchema.ts` 字段模型脱节
- 同一 widget 易出现占位符与全局条件重复注入

## 2. 决策

### 2.1 新版：`globalFilter`（业务字段级）

METRIC / QUERY widget 在 layout 中写入：

```json
{
  "globalFilter": {
    "dateField": "publish_time",
    "dateColumn": "t.publish_time",
    "dateFieldType": "datetime",
    "ipGroupField": "ip_group_id",
    "ipGroupColumn": "t.ip_group_id"
  }
}
```

- 配置页下拉选项来自 `metricSchema.ts`（`getFilterableDateFields` / `getFilterableIpGroupFields`）
- 后端 `DashboardSqlParamBinder.applyGlobalFilter` 在 SQL 上注入 `WHERE`/`AND` 条件
- 配置 `globalFilter.dateField` 后，不再绑定 `:startDate` 等标准日期占位符（避免双重过滤）

### 2.2 旧版：`filterBind`（兼容只读）

- 历史 layout 若含 `filterBind`，后端仍解析
- 新配置 UI **不再写入** `filterBind`；保存时删除

### 2.3 `:tenantId` 自动绑定

- 所有 METRIC/QUERY SQL 中的 `:tenantId` 由 `DashboardSqlParamBinder.bind` **始终**替换为 `TenantContextHolder.getTenantId()`
- **无需**在配置页映射；文档与配置说明中明确即可

### 2.4 STAT 与全局日期

- widget `type=STAT`（内置或自定义）在 `DashboardDataServiceImpl` 中 **忽略** 请求日期范围，固定 `effectiveStart=effectiveEnd=today`
- IP 组、平台筛选仍生效

### 2.5 平台筛选范围（已知限制）

| 数据源 | 平台筛选 |
|--------|----------|
| BUILTIN | 后端服务层直接过滤 |
| METRIC/QUERY + 无 `globalFilter` | legacy 可绑定 `:platformType` |
| METRIC/QUERY + 有 `globalFilter` | **当前未提供 `platformField` 映射**；顶栏平台对自定义 SQL **可能不生效** |

**未决**：是否在 `globalFilter` 增加 `platformField` / `platformColumn` — 需产品确认后再开 Slice；本期 Spec 仅记录限制，不扩展 scope。

## 3. 影响

| 层 | 变更 |
|----|------|
| 后端 | `DashboardSqlParamBinder.prepareSql`；`AnalyticsMetricServiceImpl.preview(..., widgetDef)`；`CustomQueryServiceImpl.execute(..., widgetDef)` |
| 前端 | `dataScreen.ts` schema；`ScreenConfig.vue` 映射 UI；`DataScreenFullscreen.vue` 顶栏筛选 |
| 数据 | V61 修复 98601/98602 layout 中文编码（非 schema 变更） |
| 文档 | PRD-M6 §FR-M6-006/007；API-M6 §5；UX-M6 §7/8 |

## 4. 参考

- [`PRD-M6-数据分析.md`](../product/PRD-M6-数据分析.md)
- [`API-M6-数据分析.md`](../engineering/API-M6-数据分析.md)
- `ops-platform-server/.../DashboardSqlParamBinder.java`
- `ops-platform-ui-vue/src/types/dataScreen.ts`
