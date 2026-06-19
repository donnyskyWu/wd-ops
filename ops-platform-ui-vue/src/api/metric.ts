/**
 * M6 指标管理 - API接口封装
 */
import { request } from '@/utils/request'

/** @deprecated 数据源列表改由 M8 元数据 API 动态加载（useMetricSchemas） */
export const METRIC_DATA_SOURCES = [
  { label: '内容表 (oa_content)', value: 'oa_content' },
  { label: '内容日统计 (oa_content_daily)', value: 'oa_content_daily' },
  { label: '账号表 (oa_account)', value: 'oa_account' },
  { label: '粉丝日统计 (oa_follower_daily)', value: 'oa_follower_daily' },
  { label: '订单表 (oa_order)', value: 'oa_order' },
  { label: '订单归因 (oa_order_attribution)', value: 'oa_order_attribution' },
  { label: '账号成本 (oa_account_cost)', value: 'oa_account_cost' },
] as const

export function buildMetricFormulaTemplate(dataSource: string, metricType: string) {
  if (!dataSource) return ''
  if (metricType === 'COMPOSITE') {
    return 'metric_a / metric_b * 100'
  }
  return `SELECT COUNT(*) AS metric_value FROM ${dataSource} t WHERE t.tenant_id = :tenantId AND t.deleted = 0`
}

export function previewMetric(data: { metricFormula: string; bindParams?: Record<string, string> }) {
  return request.post({ url: '/oa/metric/preview', data })
}

export function getMetricList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/metric/list', params })
}

export function createMetric(data: Record<string, unknown>) {
  return request.post({ url: '/oa/metric/create', data })
}

export function updateMetric(data: Record<string, unknown>) {
  return request.put({ url: '/oa/metric/update', data })
}

export function deleteMetric(id: number) {
  return request.delete({ url: `/oa/metric/${id}` })
}

export function getMetricOptions() {
  return request.get({ url: '/oa/metric/list', params: { pageSize: 200, pageNum: 1 } }).then((res: any) => res.list || [])
}
