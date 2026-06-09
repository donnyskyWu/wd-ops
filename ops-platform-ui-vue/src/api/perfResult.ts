/**
 * 绩效结果 - API
 */
import { request } from '@/utils/request'

export interface PerfResultQuery {
  userId?: number
  periodType?: string
  grade?: string
  startDate?: string
  pageNum?: number
  pageSize?: number
}

export function getPerfResultList(params: PerfResultQuery) {
  return request.get({ url: '/oa/perf/result/list', params })
}

export function getPerfUserTrend(userId: number, month?: number) {
  return request.get({ url: `/oa/perf/result/${userId}/trend`, params: { month } })
}

export function exportPerfResult(ids: number[]) {
  return request.post({ url: '/oa/perf/result/export', data: { ids } })
}
