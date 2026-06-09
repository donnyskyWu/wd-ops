/**
 * M6 指标管理 - API接口封装
 */
import { request } from '@/utils/request'

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
