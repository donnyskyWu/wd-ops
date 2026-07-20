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

export interface CollectorQrLoginStartResult {
  sessionId: string
  qrcodeBase64?: string
  qrcodeUrl?: string
  status?: string
  message?: string
  expiresInSeconds?: number
  /** 兼容 snake_case 透传 */
  session_id?: string
  qrcode_base64?: string
  qrcode_url?: string
  expires_in_seconds?: number
}

export interface CollectorQrLoginPollResult {
  status: string
  message?: string
  collectorAccountId?: string
  credentialsSaved?: boolean
  bindStatus?: string
  connStatus?: string
}

/** 启动扫码登录（OA 代理 collector 统一 QR API；Playwright 拉起可能需 2 分钟） */
export function startCollectorQrLogin(oaAccountId: number): Promise<CollectorQrLoginStartResult> {
  return request.post({
    url: `/oa/account/${oaAccountId}/collector-bind/qr-login/start`,
    timeout: 130_000,
  })
}

/** 轮询扫码状态 */
export function pollCollectorQrLogin(
  oaAccountId: number,
  sessionId: string,
): Promise<CollectorQrLoginPollResult> {
  return request.get({
    url: `/oa/account/${oaAccountId}/collector-bind/qr-login/poll`,
    params: { sessionId },
  })
}

/** 取消扫码会话 */
export function cancelCollectorQrLogin(oaAccountId: number, sessionId: string): Promise<boolean> {
  return request.delete({
    url: `/oa/account/${oaAccountId}/collector-bind/qr-login/cancel`,
    params: { sessionId },
  })
}

/** Channel-A 支持扫码登录的平台（与 ADR-050 一致） */
export const QR_LOGIN_PLATFORMS = new Set([
  'WECHAT_OFFICIAL',
  'WECHAT_VIDEO',
  'DOUYIN',
  'KUAISHOU',
  'XIAOHONGSHU',
])
