/**
 * M5 财务管理 - API接口封装
 */
import { request } from '@/utils/request'

export function getCostList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/finance/cost/list', params })
}

export function createCost(data: Record<string, unknown>) {
  return request.post({ url: '/oa/finance/cost/create', data })
}

export function updateCost(data: Record<string, unknown>) {
  return request.put({ url: '/oa/finance/cost/update', data })
}

export function deleteCost(id: number) {
  return request.delete({ url: `/oa/finance/cost/${id}` })
}

export function getRoiAnalysis(params: Record<string, unknown>) {
  return request.get({ url: '/oa/finance/roi/analysis', params })
}

export function getRoiTrend(params: Record<string, unknown>) {
  return request.get({ url: '/oa/finance/roi/trend', params })
}

export function getRoiBreakdown(params: Record<string, unknown>) {
  return request.get({ url: '/oa/finance/roi/breakdown', params })
}

export function exportRoi(params: Record<string, unknown>) {
  return request.post({ url: '/oa/finance/roi/export', params })
}
