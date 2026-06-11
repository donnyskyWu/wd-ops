/**
 * M6 漏斗分析 - API接口封装
 */
import { request } from '@/utils/request'

export function getFunnelList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/funnel/list', params })
}

export function createFunnel(data: Record<string, unknown>) {
  return request.post({ url: '/oa/funnel/create', data })
}

export function getFunnelData(id: number, params?: Record<string, unknown>) {
  return request.get({ url: `/oa/funnel/${id}/data`, params })
}

export function exportFunnel(params: Record<string, unknown>) {
  return request.post({ url: '/oa/funnel/export', params })
}
