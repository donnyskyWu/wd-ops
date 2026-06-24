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
  dataType?: string
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
/** 全量采集（如抖音 4 类型串行）可能超过默认 15s，单独放宽超时 */
const RUN_TASK_TIMEOUT_MS = 180_000

export function runCollectTask(id: number): Promise<boolean> {
  return request.post({ url: `/oa/collect/task/${id}/run`, timeout: RUN_TASK_TIMEOUT_MS })
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
  retryCount?: number
}

export interface CollectLogTypeResultVO {
  dataType?: string
  success?: boolean
  errorMessage?: string
  summary?: string
  targetTable?: string
  targetHint?: string
  recordCount?: number
  metrics?: Record<string, unknown>
  samples?: Record<string, unknown>[]
}

export interface CollectLogResultVO {
  summary?: string
  dataType?: string
  targetTable?: string
  targetHint?: string
  recordCount?: number
  metrics?: Record<string, unknown>
  samples?: Record<string, unknown>[]
  typeResults?: CollectLogTypeResultVO[]
}

export interface CollectLogDetailVO extends CollectLogVO {
  platformType?: string
  accountId?: number
  accountName?: string
  source?: string
  dataType?: string
  endAt?: string
  result?: CollectLogResultVO
}

export function getCollectLogPage(params: any): Promise<{ list: CollectLogVO[]; total: number }> {
  return request.get({ url: '/oa/collect/log/page', params })
}

export function getCollectLogDetail(id: number): Promise<CollectLogDetailVO> {
  return request.get({ url: `/oa/collect/log/${id}` })
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

// ==================== 私域桥接 (M10-AO-S-07) ====================
export interface PrivateDomainBridgeVO {
  id: number
  sourceType: string
  sourceId: number
  sourceLabel?: string
  targetType: string
  targetId: number
  targetLabel?: string
  matchMethod: string
  confidence?: number
  matchEvidenceJson?: string
  reviewStatus: string
  linkedBy?: string
  linkedAt?: string
  createTime?: string
}

export function getPrivateDomainBridgePage(params: any): Promise<{ list: PrivateDomainBridgeVO[]; total: number }> {
  return request.get({ url: '/oa/collect/private-domain-bridge/page', params })
}
export function confirmPrivateDomainBridge(id: number): Promise<boolean> {
  return request.post({ url: `/oa/collect/private-domain-bridge/${id}/confirm` })
}
export function rejectPrivateDomainBridge(id: number, data?: { reason?: string }): Promise<boolean> {
  return request.post({ url: `/oa/collect/private-domain-bridge/${id}/reject`, data })
}
