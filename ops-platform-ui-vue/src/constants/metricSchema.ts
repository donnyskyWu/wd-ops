/** PRD/API-M6 预定义数据源表字段与关联（来自 migration 结构） */

export interface MetricFieldMeta {
  name: string
  label: string
  type: 'number' | 'string' | 'date' | 'datetime'
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

export const METRIC_TABLE_SCHEMAS: Record<string, MetricTableSchema> = {
  oa_content: {
    table: 'oa_content',
    label: '内容表',
    alias: 't',
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'account_id', label: '账号ID', type: 'number' },
      { name: 'title', label: '标题', type: 'string' },
      { name: 'platform_type', label: '平台', type: 'string' },
      { name: 'content_type', label: '内容类型', type: 'string' },
      { name: 'publish_time', label: '发布时间', type: 'datetime' },
      { name: 'read_count', label: '阅读数', type: 'number' },
      { name: 'like_count', label: '点赞数', type: 'number' },
      { name: 'comment_count', label: '评论数', type: 'number' },
      { name: 'forward_count', label: '转发数', type: 'number' },
      { name: 'is_hit', label: '是否爆款', type: 'number' },
      { name: 'status', label: '状态', type: 'string' },
    ],
    joins: [
      { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
    ],
  },
  oa_content_daily: {
    table: 'oa_content_daily',
    label: '内容日统计',
    alias: 't',
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'content_id', label: '内容ID', type: 'number' },
      { name: 'stat_date', label: '统计日期', type: 'date' },
      { name: 'read_count', label: '阅读数', type: 'number' },
      { name: 'play_count', label: '播放数', type: 'number' },
    ],
    joins: [
      { table: 'oa_content', label: '内容表', localKey: 'content_id', foreignKey: 'id' },
    ],
  },
  oa_account: {
    table: 'oa_account',
    label: '账号表',
    alias: 't',
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'platform_type', label: '平台', type: 'string' },
      { name: 'account_name', label: '账号名称', type: 'string' },
      { name: 'external_account_id', label: '外部账号ID', type: 'string' },
      { name: 'status', label: '状态', type: 'string' },
    ],
    joins: [],
  },
  oa_follower_daily: {
    table: 'oa_follower_daily',
    label: '粉丝日统计',
    alias: 't',
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'account_id', label: '账号ID', type: 'number' },
      { name: 'stat_date', label: '统计日期', type: 'date' },
      { name: 'follower_count', label: '粉丝数', type: 'number' },
      { name: 'new_follower', label: '新增粉丝', type: 'number' },
      { name: 'unfollow_count', label: '取关数', type: 'number' },
      { name: 'net_growth', label: '净增', type: 'number' },
      { name: 'growth_rate', label: '增长率', type: 'number' },
    ],
    joins: [
      { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
    ],
  },
  oa_order: {
    table: 'oa_order',
    label: '订单表',
    alias: 't',
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'order_no', label: '订单号', type: 'string' },
      { name: 'order_amount', label: '订单金额', type: 'number' },
      { name: 'order_time', label: '下单时间', type: 'datetime' },
      { name: 'account_id', label: '账号ID', type: 'number' },
      { name: 'ip_group_id', label: 'IP组ID', type: 'number' },
    ],
    joins: [
      { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
    ],
  },
  oa_order_attribution: {
    table: 'oa_order_attribution',
    label: '订单归因',
    alias: 't',
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'order_id', label: '订单ID', type: 'number' },
      { name: 'account_id', label: '账号ID', type: 'number' },
      { name: 'ip_group_id', label: 'IP组ID', type: 'number' },
      { name: 'author_id', label: '作者ID', type: 'number' },
      { name: 'revenue', label: '收入', type: 'number' },
      { name: 'cost', label: '成本', type: 'number' },
      { name: 'roi', label: 'ROI', type: 'number' },
      { name: 'stat_date', label: '统计日期', type: 'date' },
    ],
    joins: [
      { table: 'oa_order', label: '订单表', localKey: 'order_id', foreignKey: 'id' },
      { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
    ],
  },
  oa_account_cost: {
    table: 'oa_account_cost',
    label: '账号成本',
    alias: 't',
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'account_id', label: '账号ID', type: 'number' },
      { name: 'cost_type', label: '成本类型', type: 'string' },
      { name: 'amount', label: '金额', type: 'number' },
      { name: 'pay_method', label: '支付方式', type: 'string' },
      { name: 'pay_date', label: '支付日期', type: 'date' },
      { name: 'period', label: '周期', type: 'string' },
    ],
    joins: [
      { table: 'oa_account', label: '账号表', localKey: 'account_id', foreignKey: 'id' },
    ],
  },
}

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
): string {
  const fieldMeta = fields.find((f) => f.name === cond.field)
  const col = `${alias}.${cond.field}`
  if (cond.operator === 'IS NULL' || cond.operator === 'IS NOT NULL') {
    return `${col} ${cond.operator}`
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

function buildFromWhereClause(
  schema: MetricTableSchema,
  mainAlias: string,
  joinTables: string[],
  conditions: MetricFilterCondition[],
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
  conditions
    .filter((c) => c.field && c.operator)
    .forEach((c) => {
      if ((c.operator === 'IS NULL' || c.operator === 'IS NOT NULL') || c.value || c.value === '0') {
        whereParts.push(buildConditionSql(mainAlias, c, schema.fields))
      }
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
  const schema = METRIC_TABLE_SCHEMAS[config.dataSource]
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

export function buildMetricSqlFromConfig(config: MetricBuilderConfig): string {
  const schema = METRIC_TABLE_SCHEMAS[config.dataSource]
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
  )

  let sql = `SELECT ${selectCols.join(', ')} FROM ${fromClause} WHERE ${whereClause}`
  if (config.groupByFields.length > 0) {
    sql += ` GROUP BY ${config.groupByFields.map((f) => `${mainAlias}.${f}`).join(', ')}`
  }
  return sql
}

export function getAvailableFields(dataSource: string, joinTables: string[]): MetricFieldMeta[] {
  const schema = METRIC_TABLE_SCHEMAS[dataSource]
  if (!schema) return []
  const fields = [...schema.fields]
  joinTables.forEach((jt) => {
    const joinSchema = METRIC_TABLE_SCHEMAS[jt]
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
  const schema = METRIC_TABLE_SCHEMAS[dataSource]
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
