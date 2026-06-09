/**
 * 数据采集(M10) - API 接口封装
 * 依据: API-M10 / PRD-M10
 */
import { request } from '@/utils/request'

export interface CollectTaskVO {
  id: number
  name: string
  platformType: string
  accountId: number
  accountName?: string
  method: string
  source: string
  frequency: string
  cron?: string
  apiConfig?: string
  status: string
  lastRunAt?: string
  nextRunAt?: string
  runCount?: number
  failCount?: number
  createdAt: string
}

// ==================== 任务 ====================
export function getCollectTaskPage(params: any): Promise<{ list: CollectTaskVO[]; total: number }> {
  return request.get({ url: '/oa/collect/task/page', params })
}
export function getCollectTaskDetail(id: number): Promise<CollectTaskVO> {
  return request.get({ url: `/oa/collect/task/${id}` })
}
export function createCollectTask(data: Partial<CollectTaskVO>): Promise<number> {
  return request.post({ url: '/oa/collect/task/create', data })
}
export function updateCollectTask(data: Partial<CollectTaskVO>): Promise<boolean> {
  return request.put({ url: '/oa/collect/task/update', data })
}
export function deleteCollectTask(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/collect/task/delete', params: { id } })
}
export function runCollectTask(id: number): Promise<boolean> {
  return request.post({ url: `/oa/collect/task/${id}/run` })
}
export function toggleCollectTaskStatus(id: number, status: string): Promise<boolean> {
  return request.put({ url: `/oa/collect/task/${id}/status`, params: { status } })
}

// ==================== 日志 ====================
export interface CollectLogVO {
  id: number
  taskId: number
  taskName: string
  status: string
  startAt: string
  durationMs: number
  recordCount: number
  errorMessage?: string
}

export function getCollectLogPage(params: any): Promise<{ list: CollectLogVO[]; total: number }> {
  return request.get({ url: '/oa/collect/log/page', params })
}

// ==================== 质量 ====================
export interface QualityCheckVO {
  id: number
  name: string
  checkType: string
  level: string
  tableName: string
  rule: string
  enabled: boolean
  lastCheckAt?: string
  passRate?: number
}

export interface QualityLogVO {
  id: number
  checkId: number
  checkName: string
  checkTime: string
  totalCount: number
  passCount: number
  failCount: number
  level: string
}

export function getQualityCheckPage(params: any): Promise<{ list: QualityCheckVO[]; total: number }> {
  return request.get({ url: '/oa/collect/quality/check/page', params })
}
export function getQualityLogPage(params: any): Promise<{ list: QualityLogVO[]; total: number }> {
  return request.get({ url: '/oa/collect/quality/log/page', params })
}
