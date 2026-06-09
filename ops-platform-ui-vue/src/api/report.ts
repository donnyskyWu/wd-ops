/**
 * M6 数据分析报表 - API接口封装
 */
import { request } from '@/utils/request'

export function getUnifiedAccountList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/unified-account/list', params })
}

export function getUnifiedAccountStats(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/unified-account/stats', params })
}

export function exportUnifiedAccount(params: Record<string, unknown>) {
  return request.post({ url: '/oa/report/unified-account/export', params })
}

export function getAccountStatusTrend(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/account-status/trend', params })
}

export function getAccountStatusSummary(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/account-status/summary', params })
}

export function getAccountStatusLog(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/account-status/log', params })
}

export function getVideoOutputList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/video-output/list', params })
}

export function getVideoOutputTrend(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/video-output/trend', params })
}

export function getVideoOutputRanking(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/video-output/ranking', params })
}

export function getLiveDurationList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/live-duration/list', params })
}

export function getLiveDurationTrend(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/live-duration/trend', params })
}

export function getCostAllocationList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/cost-allocation/list', params })
}

export function getReportRoiList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/roi/list', params })
}

export function getTeamConfigList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/team-config/list', params })
}

export function getAccountAlertList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/report/account-alert/list', params })
}
