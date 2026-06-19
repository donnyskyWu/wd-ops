/** PRD/API-M6 预定义数据源表字段与关联（来自 migration 结构） */

import type { MetadataQueryConditionType } from '@/api/metadata'

export interface MetricFieldMeta {
  name: string
  label: string
  type: 'number' | 'string' | 'date' | 'datetime'
}

export interface MetricFilterFieldMeta {
  name: string
  label: string
  queryConditionType: MetadataQueryConditionType
  dictType?: string
  dataType?: string
}

export interface MetricJoinMeta {
  table: string
  label: string
  /** 主表字段 */
  localKey: string
  /** 关联表字段 */
  foreignKey: string
}

export interface MetricTableSchema {
  table: string
  label: string
  alias: string
  fields: MetricFieldMeta[]
  joins: MetricJoinMeta[]
}

/**
 * 表关联定义 — ADR-046 未建模 join 元数据，暂保留静态配置。
 * 字段/实体由 M8 元数据 API 驱动（见 composables/useMetricSchemas.ts）。
 */
export const METRIC_TABLE_JOINS: Record<string, MetricJoinMeta[]> = {
  oa_content: [
    { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
  ],
  oa_content_daily: [
    { table: 'oa_content', label: '内容表', localKey: 'content_id', foreignKey: 'id' },
  ],
  oa_account: [],
  oa_follower_daily: [
    { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
  ],
  oa_order: [
    { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
  ],
  oa_order_attribution: [
    { table: 'oa_order', label: '订单表', localKey: 'order_id', foreignKey: 'id' },
    { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
  ],
  oa_account_cost: [
    { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
  ],
}

/** 运行时 schema 缓存，由 ensureMetricSchemasLoaded() 填充 */
let _metricTableSchemas: Record<string, MetricTableSchema> = {}

export function getMetricTableSchemas(): Record<string, MetricTableSchema> {
  return _metricTableSchemas
}

export function setMetricTableSchemas(schemas: Record<string, MetricTableSchema>): void {
  _metricTableSchemas = schemas
}

/** @deprecated 请使用 getMetricTableSchemas()；保留别名便于渐进迁移 */
export const METRIC_TABLE_SCHEMAS: Record<string, MetricTableSchema> = new Proxy(
  {} as Record<string, MetricTableSchema>,
  {
    get(_target, prop: string) {
      return _metricTableSchemas[prop]
    },
    ownKeys() {
      return Reflect.ownKeys(_metricTableSchemas)
    },
    getOwnPropertyDescriptor(_target, prop) {
      if (prop in _metricTableSchemas) {
        return { configurable: true, enumerable: true, value: _metricTableSchemas[prop as string] }
      }
      return undefined
    },
  },
)

export const METRIC_CALC_METHODS = [
  { value: 'COUNT', label: '计数 (COUNT)', needsField: false },
  { value: 'SUM', label: '求和 (SUM)', needsField: true },
  { value: 'AVG', label: '平均 (AVG)', needsField: true },
  { value: 'MAX', label: '最大值 (MAX)', needsField: true },
  { value: 'MIN', label: '最小值 (MIN)', needsField: true },
  { value: 'DISTINCT_COUNT', label: '去重计数 (COUNT DISTINCT)', needsField: true },
] as const

export type MetricCalcMethod = (typeof METRIC_CALC_METHODS)[number]['value']

export const METRIC_FILTER_OPERATORS = [
  { value: '=', label: '等于' },
  { value: '!=', label: '不等于' },
  { value: '>', label: '大于' },
  { value: '>=', label: '大于等于' },
  { value: '<', label: '小于' },
  { value: '<=', label: '小于等于' },
  { value: 'LIKE', label: '包含' },
  { value: 'IN', label: '在列表中' },
  { value: 'IS NULL', label: '为空' },
  { value: 'IS NOT NULL', label: '不为空' },
] as const

export interface MetricFilterCondition {
  field: string
  operator: string
  value: string
  /** 分析时作为可输入参数 */
  asParameter?: boolean
  queryConditionType?: MetadataQueryConditionType
  dictType?: string
  /** SQL 占位符名，默认 p_{field} */
  paramKey?: string
}

export interface MetricBuilderConfig {
  dataSource: string
  calcMethod: MetricCalcMethod
  calcField: string
  groupByFields: string[]
  joinTables: string[]
  conditions: MetricFilterCondition[]
}

/** 自定义查询构建器配置（计算方式可选） */
export interface QueryBuilderConfig {
  dataSource: string
  displayFields: string[]
  calcMethod?: MetricCalcMethod | ''
  calcField?: string
  groupByFields: string[]
  joinTables: string[]
  conditions: MetricFilterCondition[]
}

export type QueryChartType = 'bar' | 'line' | 'pie'

export interface QueryChartConfig {
  chartType: QueryChartType
  xField: string
  yField: string
  seriesField: string
}

export const QUERY_CHART_TYPES = [
  { value: 'bar' as const, label: '柱状图' },
  { value: 'line' as const, label: '折线图' },
  { value: 'pie' as const, label: '饼图' },
]

export function defaultQueryChartConfig(columns: string[]): QueryChartConfig {
  const xField = columns[0] ?? ''
  const yField = columns.find((c) => c !== xField) ?? columns[1] ?? ''
  return { chartType: 'bar', xField, yField, seriesField: '' }
}

export function createEmptyMetricConfig(dataSource = ''): MetricBuilderConfig {
  return {
    dataSource,
    calcMethod: 'COUNT',
    calcField: '',
    groupByFields: [],
    joinTables: [],
    conditions: [],
  }
}

export function resolveParamKey(cond: MetricFilterCondition): string {
  if (cond.paramKey?.trim()) return cond.paramKey.trim()
  const base = cond.field.replace(/\./g, '_')
  return `p_${base}`
}

export function packMetricBuilderParams(config: MetricBuilderConfig): string {
  return JSON.stringify({ builder: config })
}

export function unpackMetricBuilderParams(paramsJson?: string | null): MetricBuilderConfig | null {
  if (!paramsJson) return null
  try {
    const parsed = JSON.parse(paramsJson) as { builder?: MetricBuilderConfig }
    if (parsed.builder?.dataSource) {
      return { ...createEmptyMetricConfig(), ...parsed.builder }
    }
  } catch {
    /* ignore */
  }
  return null
}

/** 从构建器配置提取参数化条件（去重按 paramKey） */
export function extractMetricParameters(config: MetricBuilderConfig | null): MetricFilterCondition[] {
  if (!config?.conditions?.length) return []
  const seen = new Set<string>()
  const result: MetricFilterCondition[] = []
  for (const cond of config.conditions) {
    if (!cond.asParameter || !cond.field || !cond.operator) continue
    const key = resolveParamKey(cond)
    if (seen.has(key)) continue
    seen.add(key)
    result.push(cond)
  }
  return result
}

/** 「全部」/ 未选择时不参与 WHERE 与参数绑定的哨兵值（与 dict_platform_type.ALL 等对齐） */
const METRIC_PARAM_NO_FILTER_VALUES = new Set(['', 'ALL', '__ALL__'])

/** 参数值是否应生效为筛选条件（空值或「全部」哨兵 → false） */
export function isMetricBindParamActive(raw: string | null | undefined): boolean {
  if (raw == null) return false
  const trimmed = String(raw).trim()
  if (!trimmed) return false
  return !METRIC_PARAM_NO_FILTER_VALUES.has(trimmed.toUpperCase())
}

/** 将参数值绑定到 SQL（前端预览用，与后端 bindCustomParams 对齐） */
export function bindMetricCustomParams(sql: string, bindParams: Record<string, string>): string {
  if (!sql || !bindParams || !Object.keys(bindParams).length) return sql
  let result = sql
  for (const [key, raw] of Object.entries(bindParams)) {
    if (!isMetricBindParamActive(raw)) continue
    const value = quoteLiteralForBind(raw)
    result = result.replaceAll(`:${key}`, value)
  }
  return result
}

function quoteLiteralForBind(raw: string | undefined): string {
  if (raw == null) return 'NULL'
  const trimmed = raw.trim()
  if (!trimmed) return "''"
  if (/^-?\d+(\.\d+)?$/.test(trimmed)) return trimmed
  return `'${trimmed.replace(/'/g, "''")}'`
}

export function createEmptyQueryConfig(dataSource = 'oa_content_daily'): QueryBuilderConfig {
  return {
    dataSource,
    displayFields: [],
    calcMethod: '',
    calcField: '',
    groupByFields: [],
    joinTables: [],
    conditions: [],
  }
}

export function getJoinAlias(table: string): string {
  const short = table.replace(/^oa_/, '').replace(/_/g, '').slice(0, 6)
  return `j_${short}`
}

function quoteValue(field: MetricFieldMeta | undefined, raw: string): string {
  if (!raw && raw !== '0') return "''"
  const trimmed = raw.trim()
  if (field?.type === 'number') return trimmed
  if (field?.type === 'date' || field?.type === 'datetime') return `'${trimmed}'`
  if (/^'.+'$/.test(trimmed)) return trimmed
  return `'${trimmed.replace(/'/g, "''")}'`
}

function buildConditionSql(
  alias: string,
  cond: MetricFilterCondition,
  fields: MetricFieldMeta[],
  bindParams?: Record<string, string>,
): string {
  const fieldMeta = fields.find((f) => f.name === cond.field)
  const col = cond.field.includes('.') ? cond.field : `${alias}.${cond.field}`
  if (cond.operator === 'IS NULL' || cond.operator === 'IS NOT NULL') {
    return `${col} ${cond.operator}`
  }
  if (cond.asParameter) {
    const pk = resolveParamKey(cond)
    if (cond.queryConditionType === 'DATE_RANGE') {
      if (bindParams) {
        const raw = bindParams[pk]
        let start = bindParams[`${pk}_start`]?.trim() ?? ''
        let end = bindParams[`${pk}_end`]?.trim() ?? ''
        if (raw?.trim()) {
          const parts = raw.split(',')
          if (!start) start = parts[0]?.trim() ?? ''
          if (!end) end = parts[1]?.trim() ?? ''
        }
        const rangeParts: string[] = []
        if (start) rangeParts.push(`${col} >= :${pk}_start`)
        if (end) rangeParts.push(`${col} <= :${pk}_end`)
        if (rangeParts.length) return rangeParts.join(' AND ')
      }
      return `${col} >= :${pk}_start AND ${col} <= :${pk}_end`
    }
    if (cond.operator === 'LIKE') {
      return `${col} LIKE CONCAT('%', :${pk}, '%')`
    }
    if (cond.operator === 'IN') {
      return `${col} IN (:${pk})`
    }
    return `${col} ${cond.operator} :${pk}`
  }
  if (cond.operator === 'IN') {
    const parts = cond.value.split(',').map((v) => quoteValue(fieldMeta, v.trim()))
    return `${col} IN (${parts.join(', ')})`
  }
  if (cond.operator === 'LIKE') {
    return `${col} LIKE ${quoteValue({ name: cond.field, label: '', type: 'string' }, `%${cond.value}%`)}`
  }
  return `${col} ${cond.operator} ${quoteValue(fieldMeta, cond.value)}`
}

function conditionIsActive(cond: MetricFilterCondition): boolean {
  if (!cond.field || !cond.operator) return false
  if (cond.operator === 'IS NULL' || cond.operator === 'IS NOT NULL') return true
  if (cond.asParameter) return true
  return !!(cond.value || cond.value === '0')
}

/** 运行时参数是否已填写（空值/全部则跳过该 WHERE 条件） */
export function paramHasBindValue(cond: MetricFilterCondition, bindParams: Record<string, string>): boolean {
  const pk = resolveParamKey(cond)
  if (cond.queryConditionType === 'DATE_RANGE') {
    const raw = bindParams[pk]
    if (raw?.trim()) {
      const [start, end] = raw.split(',')
      return isMetricBindParamActive(start) || isMetricBindParamActive(end)
    }
    return isMetricBindParamActive(bindParams[`${pk}_start`])
      || isMetricBindParamActive(bindParams[`${pk}_end`])
  }
  return isMetricBindParamActive(bindParams[pk])
}

/** 字段 code → 元数据中文名；未命中时回退 field code */
export function resolveFieldLabel(fieldName: string, dataSource: string, joinTables: string[] = []): string {
  if (!fieldName) return fieldName
  if (fieldName === 'metric_value') return '指标值'

  const schema = getMetricTableSchemas()[dataSource]
  const mainField = schema?.fields.find((f) => f.name === fieldName)
  if (mainField) return mainField.label

  const available = getAvailableFields(dataSource, joinTables)
  const exact = available.find((f) => f.name === fieldName)
  if (exact) return exact.label

  const suffix = fieldName.includes('.') ? fieldName.split('.').pop()! : fieldName
  const bySuffix = available.find((f) => f.name === suffix || f.name.endsWith(`.${suffix}`))
  if (bySuffix) return bySuffix.label

  return fieldName
}

/** save：持久化公式，不含参数化条件；runtime：分析/预览，含 :p_xxx 占位符 */
export type MetricSqlBuildMode = 'save' | 'runtime'

export interface MetricSqlBuildOptions {
  mode?: MetricSqlBuildMode
  /** runtime 模式下：仅保留已填写值的参数化条件 */
  bindParams?: Record<string, string>
}

function filterConditionsForMode(
  conditions: MetricFilterCondition[],
  mode: MetricSqlBuildMode,
  bindParams?: Record<string, string>,
): MetricFilterCondition[] {
  return conditions.filter((c) => {
    if (!conditionIsActive(c)) return false
    if (mode === 'save' && c.asParameter) return false
    if (mode === 'runtime' && c.asParameter && bindParams) {
      return paramHasBindValue(c, bindParams)
    }
    return true
  })
}

function buildFromWhereClause(
  schema: MetricTableSchema,
  mainAlias: string,
  joinTables: string[],
  conditions: MetricFilterCondition[],
  mode: MetricSqlBuildMode = 'save',
  bindParams?: Record<string, string>,
): { fromClause: string; whereClause: string } {
  const fromParts = [`${schema.table} ${mainAlias}`]
  const joined = new Set<string>()
  joinTables.forEach((joinTable) => {
    if (joined.has(joinTable)) return
    const joinMeta = schema.joins.find((j) => j.table === joinTable)
    if (!joinMeta) return
    joined.add(joinTable)
    const joinAlias = getJoinAlias(joinTable)
    fromParts.push(
      `LEFT JOIN ${joinTable} ${joinAlias} ON ${mainAlias}.${joinMeta.localKey} = ${joinAlias}.${joinMeta.foreignKey} AND ${joinAlias}.tenant_id = ${mainAlias}.tenant_id AND ${joinAlias}.deleted = 0`,
    )
  })

  const whereParts = [
    `${mainAlias}.tenant_id = :tenantId`,
    `${mainAlias}.deleted = 0`,
  ]
  filterConditionsForMode(conditions, mode, bindParams).forEach((c) => {
    whereParts.push(buildConditionSql(mainAlias, c, schema.fields, bindParams))
  })

  return {
    fromClause: fromParts.join(' '),
    whereClause: whereParts.join(' AND '),
  }
}

function buildAggExpr(calcMethod: MetricCalcMethod, mainAlias: string, calcField?: string): string {
  switch (calcMethod) {
    case 'COUNT':
      return 'COUNT(*)'
    case 'DISTINCT_COUNT':
      return calcField ? `COUNT(DISTINCT ${mainAlias}.${calcField})` : 'COUNT(*)'
    default:
      return calcField ? `${calcMethod}(${mainAlias}.${calcField})` : `${calcMethod}(*)`
  }
}

export function buildQuerySqlFromConfig(config: QueryBuilderConfig): string {
  const schema = getMetricTableSchemas()[config.dataSource]
  if (!schema) return ''

  const mainAlias = schema.alias
  const selectCols: string[] = []
  const hasCalc = !!config.calcMethod

  config.displayFields.forEach((f) => {
    const col = `${mainAlias}.${f}`
    if (!selectCols.includes(col)) selectCols.push(col)
  })

  if (hasCalc) {
    const agg = buildAggExpr(config.calcMethod!, mainAlias, config.calcField)
    selectCols.push(`${agg} AS metric_value`)
  }

  if (selectCols.length === 0) {
    selectCols.push(`${mainAlias}.*`)
  }

  const { fromClause, whereClause } = buildFromWhereClause(
    schema,
    mainAlias,
    config.joinTables,
    config.conditions,
  )

  let sql = `SELECT ${selectCols.join(', ')} FROM ${fromClause} WHERE ${whereClause}`

  const groupFields = config.groupByFields.length > 0
    ? config.groupByFields
    : (hasCalc ? config.displayFields : [])

  if (hasCalc && groupFields.length > 0) {
    sql += ` GROUP BY ${groupFields.map((f) => `${mainAlias}.${f}`).join(', ')}`
  }

  return sql
}

export function buildMetricSqlFromConfig(
  config: MetricBuilderConfig,
  options: MetricSqlBuildOptions = {},
): string {
  const mode = options.mode ?? 'save'
  const schema = getMetricTableSchemas()[config.dataSource]
  if (!schema) return ''

  const mainAlias = schema.alias
  let selectExpr: string
  switch (config.calcMethod) {
    case 'COUNT':
      selectExpr = 'COUNT(*)'
      break
    case 'DISTINCT_COUNT':
      selectExpr = config.calcField ? `COUNT(DISTINCT ${mainAlias}.${config.calcField})` : 'COUNT(*)'
      break
    default:
      selectExpr = config.calcField
        ? `${config.calcMethod}(${mainAlias}.${config.calcField})`
        : `${config.calcMethod}(*)`
  }

  const selectCols = [selectExpr + ' AS metric_value']
  if (config.groupByFields.length > 0) {
    config.groupByFields.forEach((f) => selectCols.unshift(`${mainAlias}.${f}`))
  }

  const { fromClause, whereClause } = buildFromWhereClause(
    schema,
    mainAlias,
    config.joinTables,
    config.conditions,
    mode,
    options.bindParams,
  )

  let sql = `SELECT ${selectCols.join(', ')} FROM ${fromClause} WHERE ${whereClause}`
  if (config.groupByFields.length > 0) {
    sql += ` GROUP BY ${config.groupByFields.map((f) => `${mainAlias}.${f}`).join(', ')}`
  }
  return sql
}

/** 分析/预览时生成含参数占位符的完整 SQL；优先从 params_json 重建，兼容旧数据直接带 :p_ 的公式 */
export function buildRuntimeMetricSql(
  savedFormula: string,
  paramsJson?: string | null,
  bindParams?: Record<string, string>,
): string {
  const builder = unpackMetricBuilderParams(paramsJson)
  if (builder?.dataSource) {
    const runtimeSql = buildMetricSqlFromConfig(builder, { mode: 'runtime', bindParams })
    if (runtimeSql) return runtimeSql
  }
  return savedFormula
}

/** 解析指标查询结果列的中文表头 */
export function resolveMetricResultColumnLabel(
  col: string,
  config: MetricBuilderConfig | null | undefined,
): string {
  if (col === 'metric_value') {
    if (config?.calcMethod) {
      return METRIC_CALC_METHODS.find((m) => m.value === config.calcMethod)?.label ?? '指标值'
    }
    return '指标值'
  }
  if (!config?.dataSource) return col
  return resolveFieldLabel(col, config.dataSource, config.joinTables)
}

export function metricFormulaHasCustomParams(formula: string): boolean {
  return /:p_[a-zA-Z0-9_]+/.test(formula)
}

export function getAvailableFields(dataSource: string, joinTables: string[]): MetricFieldMeta[] {
  const schemas = getMetricTableSchemas()
  const schema = schemas[dataSource]
  if (!schema) return []
  const fields = [...schema.fields]
  joinTables.forEach((jt) => {
    const joinSchema = schemas[jt]
    if (joinSchema) {
      joinSchema.fields.forEach((f) => {
        fields.push({ ...f, name: `${getJoinAlias(jt)}.${f.name}`, label: `[${joinSchema.label}] ${f.label}` })
      })
    }
  })
  return fields
}

/** 大屏全局日期筛选可选字段（date / datetime） */
export function getFilterableDateFields(dataSource: string, joinTables: string[] = []): MetricFieldMeta[] {
  return getAvailableFields(dataSource, joinTables).filter((f) => f.type === 'date' || f.type === 'datetime')
}

/** 大屏全局 IP 组筛选可选字段 */
export function getFilterableIpGroupFields(dataSource: string, joinTables: string[] = []): MetricFieldMeta[] {
  return getAvailableFields(dataSource, joinTables).filter(
    (f) => f.name === 'ip_group_id' || f.name.endsWith('.ip_group_id'),
  )
}

/** 业务字段名 → SQL 列表达式（含主表 alias） */
export function resolveFieldSqlColumn(fieldName: string, dataSource: string): string {
  if (!fieldName) return ''
  if (fieldName.includes('.')) return fieldName
  const schema = getMetricTableSchemas()[dataSource]
  if (!schema) return fieldName
  return `${schema.alias}.${fieldName}`
}

export function unpackQueryBuilderParams(paramsJson?: string | null): QueryBuilderConfig | null {
  if (!paramsJson) return null
  try {
    const parsed = JSON.parse(paramsJson) as { builder?: QueryBuilderConfig }
    if (parsed.builder?.dataSource) {
      return { ...createEmptyQueryConfig(), ...parsed.builder }
    }
  } catch {
    /* ignore */
  }
  return null
}
