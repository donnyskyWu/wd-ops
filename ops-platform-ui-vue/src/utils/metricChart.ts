import type { EChartsOption } from 'echarts'
import type { QueryChartConfig } from '@/constants/metricSchema'

const DATE_NAME_PATTERN = /date|time|day|month|year|dt|created|updated/i

export function isNumericValue(val: unknown): boolean {
  if (typeof val === 'number' && !Number.isNaN(val)) return true
  if (val == null || val === '') return false
  return !Number.isNaN(Number(val))
}

export function isNumericColumn(rows: Record<string, unknown>[], prop: string): boolean {
  if (!rows.length) return false
  const numericCount = rows.filter((r) => isNumericValue(r[prop])).length
  return numericCount > 0
}

export function detectNumericColumnProps(rows: Record<string, unknown>[], props: string[]): string[] {
  return props.filter((prop) => isNumericColumn(rows, prop))
}

export function detectDimensionColumnProp(rows: Record<string, unknown>[], props: string[]): string {
  const byName = props.find((p) => DATE_NAME_PATTERN.test(p))
  if (byName) return byName

  const nonNumeric = props.filter((p) => !isNumericColumn(rows, p))
  if (nonNumeric.length) return nonNumeric[0]

  return props[0] ?? ''
}

export function defaultMetricChartConfig(
  rows: Record<string, unknown>[],
  columnProps: string[],
): QueryChartConfig {
  const props = columnProps.length ? columnProps : (rows[0] ? Object.keys(rows[0]) : [])
  const numericProps = detectNumericColumnProps(rows, props)
  const xField = detectDimensionColumnProp(rows, props.filter((p) => !numericProps.includes(p) || props.length === 1))
    || detectDimensionColumnProp(rows, props)
  const yCandidates = numericProps.filter((p) => p !== xField)
  const yField = yCandidates.find((p) => p === 'metric_value')
    ?? yCandidates[0]
    ?? numericProps[0]
    ?? props.find((p) => p !== xField)
    ?? props[1]
    ?? props[0]
    ?? ''

  return { chartType: 'line', xField, yField, seriesField: '' }
}

export function buildMetricChartOption(
  rows: Record<string, unknown>[],
  config: QueryChartConfig,
  options: { seriesLabel?: string } = {},
): EChartsOption | null {
  if (!rows.length || !config.xField || !config.yField) return null

  const { chartType, xField, yField, seriesField } = config
  const seriesLabel = options.seriesLabel ?? yField

  if (chartType === 'pie') {
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'pie',
        radius: '55%',
        data: rows.map((r) => ({
          name: String(r[xField] ?? ''),
          value: Number(r[yField] ?? 0),
        })),
      }],
    }
  }

  if (seriesField) {
    const categories = [...new Set(rows.map((r) => String(r[xField] ?? '')))]
    const seriesNames = [...new Set(rows.map((r) => String(r[seriesField] ?? '默认')))]
    const series = seriesNames.map((name) => ({
      name,
      type: chartType,
      smooth: chartType === 'line',
      data: categories.map((cat) => {
        const match = rows.find(
          (r) => String(r[xField] ?? '') === cat && String(r[seriesField] ?? '默认') === name,
        )
        return match ? Number(match[yField] ?? 0) : 0
      }),
    }))
    return {
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, type: 'scroll' },
      grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
      xAxis: { type: 'category', data: categories, boundaryGap: chartType === 'bar' },
      yAxis: { type: 'value' },
      series,
    }
  }

  const xData = rows.map((r) => String(r[xField] ?? ''))
  return {
    tooltip: { trigger: 'axis' },
    legend: seriesLabel ? { data: [seriesLabel], bottom: 0 } : undefined,
    grid: { left: '3%', right: '4%', bottom: seriesLabel ? '12%' : '3%', containLabel: true },
    xAxis: { type: 'category', data: xData, boundaryGap: chartType === 'bar' },
    yAxis: { type: 'value' },
    series: [{
      name: seriesLabel,
      type: chartType,
      smooth: chartType === 'line',
      data: rows.map((r) => Number(r[yField] ?? 0)),
    }],
  }
}
