/**
 * M10 Channel-A Collector 账号绑定 API（ADR-047）
 */
import { request } from '@/utils/request'

export interface CollectorAccountBindVO {
  id?: number
  oaAccountId: number
  collectorAccountId?: string
  platformType?: string
  bindStatus?: string
  connStatus?: string
  lastBindAt?: string
  lastHealthCheckAt?: string
  createTime?: string
  updateTime?: string
}

export interface CollectorAccountBindTestResult {
  success: boolean
  connStatus?: string
  collectorAccountId?: string
  collectorStatus?: string
  message?: string
}

export interface CollectorBatchBindImportItem {
  oaAccountId?: number
  platformType?: string
  result?: string
  message?: string
}

export interface CollectorBatchBindImportResult {
  scanned: number
  imported: number
  skipped: number
  failed: number
  items?: CollectorBatchBindImportItem[]
}

export function getCollectorBind(oaAccountId: number): Promise<CollectorAccountBindVO | null> {
  return request.get({ url: `/oa/account/${oaAccountId}/collector-bind` })
}

export function bindCollectorAccount(oaAccountId: number): Promise<CollectorAccountBindVO> {
  return request.post({ url: `/oa/account/${oaAccountId}/collector-bind` })
}

export function syncCollectorCredentials(oaAccountId: number): Promise<CollectorAccountBindVO> {
  return request.post({ url: `/oa/account/${oaAccountId}/collector-bind/sync` })
}

export function testCollectorConnection(oaAccountId: number): Promise<CollectorAccountBindTestResult> {
  return request.post({ url: `/oa/account/${oaAccountId}/collector-bind/test-connection` })
}

/** 租户内批量绑定凭证齐全且尚未 bind 的 Channel-A 账号 */
export function batchImportCollectorAccounts(): Promise<CollectorBatchBindImportResult> {
  return request.post({ url: '/oa/collector-bind/batch-import' })
}
