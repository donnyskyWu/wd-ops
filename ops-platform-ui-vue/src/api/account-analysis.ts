/**
 * 账号分析 - API（GATE-S3 真实 API）
 */
import { request } from '@/utils/request'
import type {
  AccountAnalysisVO,
  AccountAnalysisQueryVO,
  PageResult,
  FollowerDetailVO,
  FollowerDetailQueryVO,
  ContentVO,
  ContentQueryVO,
} from '@/types/account-analysis'

export function getAccountAnalysisList(params: AccountAnalysisQueryVO): Promise<PageResult<AccountAnalysisVO>> {
  return request.get({ url: '/oa/account-analysis/list', params })
}

export function getAccountFollowerDetail(params: FollowerDetailQueryVO): Promise<FollowerDetailVO> {
  return request.get({ url: `/oa/account-analysis/${params.accountId}/followers`, params })
}

export function getAccountContentDetail(params: ContentQueryVO): Promise<PageResult<ContentVO>> {
  return request.get({ url: `/oa/account-analysis/${params.accountId}/contents`, params })
}

export function exportAccountAnalysis(data: Record<string, unknown>): Promise<string> {
  return request.post({ url: '/oa/account-analysis/export', data })
}
