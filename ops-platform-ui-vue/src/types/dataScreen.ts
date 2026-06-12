/** M6 数据大屏 layout schema */

export type DataScreenScope = 'INTERNAL' | 'EXTERNAL'
export type WidgetType = 'KPI' | 'STAT' | 'CHART' | 'LIST'
export type WidgetSourceType = 'BUILTIN' | 'METRIC' | 'QUERY'
export type ChartType = 'line' | 'bar' | 'pie'
export type SortOrder = 'ASC' | 'DESC'
export type YAgg = 'SUM' | 'AVG' | 'COUNT' | 'MAX' | 'MIN'

export interface ListColumnDef {
  key: string
  label: string
}

export interface BuiltinChartMeta {
  xKey: string
  yKey: string
  groupKey?: string
  chartType: ChartType
  readonly?: boolean
}

/** 全局筛选来源（页面顶部日期 / IP组 / 平台） */
export type GlobalFilterSourceKey =
  | 'startDate'
  | 'endDate'
  | 'startDateTime'
  | 'endDateTime'
  | 'ipGroupId'
  | 'platformType'

/** SQL 占位符名 → 全局筛选来源；未出现的占位符不注入全局条件（旧版，兼容保留） */
export type WidgetFilterBind = Partial<Record<string, GlobalFilterSourceKey>>

/**
 * 全局筛选 → 业务字段映射（新版）。
 * dateColumn / ipGroupColumn 由前端按 metricSchema 解析后写入 layout，供后端注入 WHERE。
 */
export interface WidgetGlobalFilter {
  dateField?: string
  dateColumn?: string
  dateFieldType?: 'date' | 'datetime'
  ipGroupField?: string
  ipGroupColumn?: string
}

export interface DataScreenWidgetDef {
  id: string
  type: WidgetType
  title: string
  sourceType: WidgetSourceType
  builtinKey?: string
  metricId?: number
  valueKey?: string
  queryId?: number
  xKey?: string
  yKey?: string
  groupKey?: string
  yAgg?: YAgg
  chartType?: ChartType
  columns?: ListColumnDef[]
  limit?: number
  sortKey?: string
  sortOrder?: SortOrder
  /** METRIC / QUERY 专用：全局筛选映射到业务字段（优先于 filterBind） */
  globalFilter?: WidgetGlobalFilter
  /** @deprecated 旧版 SQL 占位符映射，仅兼容历史配置 */
  filterBind?: WidgetFilterBind
}

export function emptyGlobalFilter(): WidgetGlobalFilter {
  return {}
}

export function normalizeGlobalFilter(gf: unknown): WidgetGlobalFilter {
  if (!gf || typeof gf !== 'object') return {}
  const raw = gf as Record<string, unknown>
  const result: WidgetGlobalFilter = {}
  if (raw.dateField) {
    result.dateField = String(raw.dateField)
    result.dateColumn = raw.dateColumn ? String(raw.dateColumn) : undefined
    result.dateFieldType = raw.dateFieldType === 'datetime' ? 'datetime' : 'date'
  }
  if (raw.ipGroupField) {
    result.ipGroupField = String(raw.ipGroupField)
    result.ipGroupColumn = raw.ipGroupColumn ? String(raw.ipGroupColumn) : undefined
  }
  return result
}

export interface DataScreenLayout {
  version: number
  scope: DataScreenScope
  refreshSeconds: number
  widgets: DataScreenWidgetDef[]
}

export interface DashboardVO {
  id: number
  dashboardName: string
  dashboardType: string
  layout: string
  status: number
}

export interface WidgetPayload {
  value?: number | string
  unit?: string
  chartType?: ChartType
  chart?: {
    xAxis?: string[]
    series?: Array<{ name: string; type: string; data: unknown[] }>
    legend?: string[]
  }
  columns?: ListColumnDef[]
  rows?: Record<string, unknown>[]
  error?: string
}

export interface DashboardWidgetResult {
  id: string
  type: WidgetType
  title: string
  payload: WidgetPayload
}

export interface DashboardDataResponse {
  dashboard: DashboardVO
  widgets: DashboardWidgetResult[]
}

export interface DashboardDataQuery {
  ipGroupId?: number
  startDate?: string
  endDate?: string
  platformType?: string
}

export type DashboardDateRangeKey = '1d' | '7d' | '30d'

/** 大屏全局日期范围（与全屏/配置预览共用） */
export function resolveDashboardDateRange(key: DashboardDateRangeKey): { startDate: string; endDate: string } {
  const end = new Date()
  const start = new Date()
  if (key === '1d') {
    // today only
  } else if (key === '30d') {
    start.setDate(end.getDate() - 29)
  } else {
    start.setDate(end.getDate() - 6)
  }
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  return { startDate: fmt(start), endDate: fmt(end) }
}

/** 组装大屏数据 API 全局查询参数 */
export function buildDashboardDataQuery(opts: {
  dateRangeKey: DashboardDateRangeKey
  ipGroupId?: number
  platformType?: string
}): DashboardDataQuery {
  const { startDate, endDate } = resolveDashboardDateRange(opts.dateRangeKey)
  const query: DashboardDataQuery = { startDate, endDate }
  if (opts.ipGroupId != null) query.ipGroupId = opts.ipGroupId
  if (opts.platformType) query.platformType = opts.platformType
  return query
}

export const BUILTIN_KPI_KEYS = [
  { key: 'work_count', label: '作品数', scope: 'INTERNAL' },
  { key: 'like_count', label: '点赞数', scope: 'INTERNAL' },
  { key: 'follower_growth', label: '涨粉数', scope: 'INTERNAL' },
  { key: 'read_count', label: '阅读数', scope: 'INTERNAL' },
  { key: 'play_count', label: '播放数', scope: 'INTERNAL' },
  { key: 'today_content', label: '今日新增作品', scope: 'INTERNAL' },
  { key: 'today_follower', label: '今日新增粉丝', scope: 'INTERNAL' },
  { key: 'today_orders', label: '今日新增订单', scope: 'INTERNAL' },
  { key: 'today_order_amount', label: '今日订单金额', scope: 'INTERNAL' },
  { key: 'pending_review', label: '待处理审核', scope: 'INTERNAL' },
  { key: 'monitor_accounts', label: '监控账号总数', scope: 'EXTERNAL' },
  { key: 'external_works', label: '外部作品总数', scope: 'EXTERNAL' },
  { key: 'total_plays', label: '总阅读量/播放量', scope: 'EXTERNAL' },
  { key: 'total_likes', label: '总点赞数', scope: 'EXTERNAL' },
  { key: 'hit_24h', label: '24h爆款数', scope: 'EXTERNAL' },
  { key: 'low_score_24h', label: '24h低分作品数', scope: 'EXTERNAL' },
] as const

export const BUILTIN_CHART_KEYS = [
  { key: 'follower_trend', label: '多平台粉丝趋势', scope: 'INTERNAL', chartType: 'line' as ChartType },
  { key: 'content_type_pie', label: '内容类型占比', scope: 'INTERNAL', chartType: 'pie' as ChartType },
  { key: 'read_trend', label: '多平台阅读量趋势', scope: 'INTERNAL', chartType: 'bar' as ChartType },
  { key: 'ip_theme_trend', label: '竞品IP主题作品趋势', scope: 'EXTERNAL', chartType: 'bar' as ChartType },
] as const

export const BUILTIN_LIST_KEYS = [
  { key: 'hit_works_24h', label: '爆款预警（最近24h）', scope: 'INTERNAL' },
  { key: 'high_follower_top10', label: '竞品账号粉丝排行 Top10', scope: 'EXTERNAL' },
  { key: 'external_hit_works', label: '竞品爆款作品（最近24h）', scope: 'EXTERNAL' },
] as const

/** 内置列表可选字段（按 builtinKey） */
export const BUILTIN_LIST_FIELDS: Record<string, ListColumnDef[]> = {
  hit_works_24h: [
    { key: 'rank', label: '排名' },
    { key: 'title', label: '作品标题' },
    { key: 'read_count', label: '阅读量' },
    { key: 'like_count', label: '点赞数' },
    { key: 'trend_pct', label: '趋势' },
  ],
  high_follower_top10: [
    { key: 'rank', label: '排名' },
    { key: 'account_name', label: '账号名称' },
    { key: 'platform_type', label: '平台' },
    { key: 'follower_count', label: '粉丝数' },
  ],
  external_hit_works: [
    { key: 'rank', label: '排名' },
    { key: 'account_name', label: '账号' },
    { key: 'title', label: '作品标题' },
    { key: 'play_count', label: '阅读量' },
    { key: 'like_count', label: '点赞' },
  ],
}

/** 内置图表轴字段元信息（只读展示 + 默认 chartType） */
export const BUILTIN_CHART_FIELDS: Record<string, BuiltinChartMeta> = {
  follower_trend: { xKey: 'stat_date', yKey: 'follower_count', groupKey: 'platform_type', chartType: 'line', readonly: true },
  content_type_pie: { xKey: 'content_type', yKey: 'count', chartType: 'pie', readonly: true },
  read_trend: { xKey: 'publish_date', yKey: 'read_count', groupKey: 'platform_type', chartType: 'bar', readonly: true },
  ip_theme_trend: { xKey: 'metric', yKey: 'value', chartType: 'bar', readonly: true },
}

export function getBuiltinListFields(builtinKey?: string): ListColumnDef[] {
  if (!builtinKey) return []
  return BUILTIN_LIST_FIELDS[builtinKey] ?? []
}

export function getBuiltinChartMeta(builtinKey?: string): BuiltinChartMeta | undefined {
  if (!builtinKey) return undefined
  return BUILTIN_CHART_FIELDS[builtinKey]
}

/** METRIC / QUERY 列表默认可选字段 */
export const DEFAULT_CUSTOM_LIST_FIELDS: ListColumnDef[] = [
  { key: 'name', label: '名称' },
  { key: 'value', label: '数值' },
]

/** METRIC / QUERY 图表轴默认可选字段 */
export const DEFAULT_CUSTOM_CHART_FIELDS: ListColumnDef[] = [
  { key: 'stat_date', label: '日期 (stat_date)' },
  { key: 'publish_date', label: '发布日期 (publish_date)' },
  { key: 'metric_value', label: '指标值 (metric_value)' },
  { key: 'value', label: '数值 (value)' },
  { key: 'count', label: '计数 (count)' },
  { key: 'platform_type', label: '平台 (platform_type)' },
  { key: 'name', label: '名称 (name)' },
]

export const GLOBAL_FILTER_SOURCE_OPTIONS: Array<{ key: GlobalFilterSourceKey | ''; label: string; hint: string }> = [
  { key: '', label: '不绑定', hint: '该占位符不注入全局筛选' },
  { key: 'startDate', label: '全局开始日期', hint: '页面日期范围起始日' },
  { key: 'endDate', label: '全局结束日期', hint: '页面日期范围结束日' },
  { key: 'startDateTime', label: '全局开始时刻', hint: '起始日 00:00:00' },
  { key: 'endDateTime', label: '全局结束时刻', hint: '结束日 23:59:59' },
  { key: 'ipGroupId', label: '全局 IP 组', hint: '未选 IP 组时为 0' },
  { key: 'platformType', label: '全局平台', hint: '未选平台时为空串' },
]

const LEGACY_FILTER_BIND_KEYS = new Set([
  'startDate', 'endDate', 'startDateTime', 'endDateTime', 'ipGroupId', 'platformType',
])

export function emptyFilterBind(): WidgetFilterBind {
  return {}
}

export function extractSqlPlaceholders(sql: string | undefined | null): string[] {
  if (!sql) return []
  const matches = [...sql.matchAll(/:([a-zA-Z_][a-zA-Z0-9_]*)/g)]
  return [...new Set(matches.map((m) => m[1]))].filter((k) => k !== 'tenantId')
}

/** 兼容旧版「全局字段 → SQL 占位符」结构，统一转为「SQL 占位符 → 全局来源」 */
export function normalizeFilterBind(bind: unknown): WidgetFilterBind {
  if (!bind || typeof bind !== 'object') return {}
  const map = bind as Record<string, string>
  const keys = Object.keys(map)
  if (!keys.length) return {}
  const isLegacy = keys.some((k) => LEGACY_FILTER_BIND_KEYS.has(k))
  if (!isLegacy) {
    const result: WidgetFilterBind = {}
    for (const [ph, src] of Object.entries(map)) {
      if (ph && src && LEGACY_FILTER_BIND_KEYS.has(src)) {
        result[ph] = src as GlobalFilterSourceKey
      }
    }
    return result
  }
  const inverted: WidgetFilterBind = {}
  for (const [src, ph] of Object.entries(map)) {
    if (ph && LEGACY_FILTER_BIND_KEYS.has(src)) {
      inverted[ph] = src as GlobalFilterSourceKey
    }
  }
  return inverted
}

export interface PlaceholderBindRow {
  placeholder: string
  source: GlobalFilterSourceKey | ''
}

export function buildPlaceholderBindRows(
  sql: string | undefined | null,
  bind?: WidgetFilterBind,
): PlaceholderBindRow[] {
  return extractSqlPlaceholders(sql).map((placeholder) => ({
    placeholder,
    source: bind?.[placeholder] ?? '',
  }))
}

export function filterBindFromRows(rows: PlaceholderBindRow[]): WidgetFilterBind {
  const result: WidgetFilterBind = {}
  for (const row of rows) {
    if (row.source) result[row.placeholder] = row.source
  }
  return result
}

export function extractColumnKeysFromRows(rows: Record<string, unknown>[]): string[] {
  const keys = new Set<string>()
  for (const row of rows.slice(0, 5)) {
    for (const k of Object.keys(row)) keys.add(k)
  }
  return [...keys]
}

export function getAvailableChartFields(
  sourceType: WidgetSourceType,
  builtinKey?: string,
  dynamicKeys?: string[],
): ListColumnDef[] {
  if (sourceType === 'BUILTIN') {
    const meta = getBuiltinChartMeta(builtinKey)
    if (!meta) return []
    const fields: ListColumnDef[] = [
      { key: meta.xKey, label: meta.xKey },
      { key: meta.yKey, label: meta.yKey },
    ]
    if (meta.groupKey) fields.push({ key: meta.groupKey, label: meta.groupKey })
    return fields
  }
  const base = DEFAULT_CUSTOM_CHART_FIELDS.map((f) => ({ ...f }))
  const seen = new Set(base.map((f) => f.key))
  for (const k of dynamicKeys ?? []) {
    if (!seen.has(k)) {
      base.push({ key: k, label: k })
      seen.add(k)
    }
  }
  return base
}

export function getAvailableListFields(
  sourceType: WidgetSourceType,
  builtinKey?: string,
  existingColumns?: ListColumnDef[],
): ListColumnDef[] {
  if (sourceType === 'BUILTIN') {
    return getBuiltinListFields(builtinKey).map((f) => ({ ...f }))
  }
  const base = DEFAULT_CUSTOM_LIST_FIELDS.map((f) => ({ ...f }))
  if (!existingColumns?.length) return base
  const seen = new Set(base.map((f) => f.key))
  for (const col of existingColumns) {
    if (!seen.has(col.key)) {
      base.push({ ...col })
      seen.add(col.key)
    }
  }
  return base
}

export function defaultColumnsForList(builtinKey?: string): ListColumnDef[] {
  const fields = getBuiltinListFields(builtinKey)
  return fields.length ? fields.map((f) => ({ ...f })) : DEFAULT_CUSTOM_LIST_FIELDS.map((f) => ({ ...f }))
}

export function parseLayout(layoutJson: string | undefined | null): DataScreenLayout {
  if (!layoutJson) {
    return { version: 1, scope: 'INTERNAL', refreshSeconds: 60, widgets: [] }
  }
  try {
    const parsed = JSON.parse(layoutJson) as DataScreenLayout
    return {
      version: parsed.version ?? 1,
      scope: parsed.scope ?? 'INTERNAL',
      refreshSeconds: parsed.refreshSeconds ?? 60,
      widgets: parsed.widgets ?? [],
    }
  } catch {
    return { version: 1, scope: 'INTERNAL', refreshSeconds: 60, widgets: [] }
  }
}

export function emptyLayout(scope: DataScreenScope = 'INTERNAL'): DataScreenLayout {
  return { version: 1, scope, refreshSeconds: 60, widgets: [] }
}

/** Merge saved layout definitions with API widget payloads (layout drives order/titles/structure). */
export function mergeWidgetResultsWithLayout(
  layout: DataScreenLayout,
  apiWidgets: DashboardWidgetResult[],
): DashboardWidgetResult[] {
  const dataMap = new Map(apiWidgets.map((w) => [w.id, w]))
  return layout.widgets.map((def) => {
    const fromApi = dataMap.get(def.id)
    if (fromApi) {
      return { ...fromApi, type: def.type, title: def.title || fromApi.title }
    }
    return {
      id: def.id,
      type: def.type,
      title: def.title,
      payload: def.type === 'CHART'
        ? { chartType: def.chartType, chart: { xAxis: [], series: [] } }
        : def.type === 'LIST'
          ? { columns: def.columns || [], rows: [] }
          : { value: '--' },
    }
  })
}

export function formatNumber(val: unknown): string {
  if (val == null) return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  if (n >= 100000000) return `${(n / 100000000).toFixed(1)}亿`
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return n.toLocaleString('zh-CN')
}

export type LayoutSection =
  | { kind: 'charts'; charts: DashboardWidgetResult[] }
  | { kind: 'mixed'; chart: DashboardWidgetResult; list: DashboardWidgetResult }
  | { kind: 'list-full'; list: DashboardWidgetResult }

/** 大屏底部布局：图表两两成行，末行单图与列表并排，剩余列表通栏 */
export function buildLayoutSections(
  charts: DashboardWidgetResult[],
  lists: DashboardWidgetResult[],
): LayoutSection[] {
  const sections: LayoutSection[] = []
  let ci = 0
  let li = 0
  while (ci < charts.length) {
    const chartsRemaining = charts.length - ci
    const listsRemaining = lists.length - li
    if (chartsRemaining === 1 && listsRemaining > 0) {
      sections.push({ kind: 'mixed', chart: charts[ci], list: lists[li] })
      ci += 1
      li += 1
      continue
    }
    if (chartsRemaining >= 2) {
      sections.push({ kind: 'charts', charts: charts.slice(ci, ci + 2) })
      ci += 2
      continue
    }
    sections.push({ kind: 'charts', charts: [charts[ci]] })
    ci += 1
  }
  while (li < lists.length) {
    sections.push({ kind: 'list-full', list: lists[li] })
    li += 1
  }
  return sections
}

const NUMERIC_COLUMN_KEYS = new Set([
  'read_count', 'play_count', 'like_count', 'follower_count', 'rank',
])

export function isNumericListColumn(key: string): boolean {
  return NUMERIC_COLUMN_KEYS.has(key) || key.endsWith('_count') || key.endsWith('_amount')
}
