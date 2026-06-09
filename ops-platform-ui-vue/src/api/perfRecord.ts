/**
 * 考核执行 - API
 */
import { request } from '@/utils/request'

export interface PerfRecordQuery {
  ipGroupId?: number
  targetUserId?: number
  periodType?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

export function getPerfRecordList(params: PerfRecordQuery) {
  return request.get({ url: '/oa/perf/record/list', params })
}

export function createPerfRecord(data: {
  targetUserId: number
  periodType: string
  periodStart: string
  periodEnd: string
}) {
  return request.post({ url: '/oa/perf/record/create', data })
}

export function calculatePerfRecord(recordId: number) {
  return request.post({ url: '/oa/perf/record/calculate', data: { recordId } })
}

export function adjustPerfRecord(data: {
  itemRecordId: number
  manualAdjustment: number
  remark?: string
}) {
  return request.put({ url: '/oa/perf/record/adjust', data })
}

export function getPerfRecordDetail(id: number) {
  return request.get({ url: '/oa/perf/record/detail', params: { id } })
}

export function confirmPerfRecord(id: number) {
  return request.post({ url: '/oa/perf/record/confirm', data: { id } })
}
