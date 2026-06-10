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
  // S-R13 B2 修复：URL 模板已含 accountId，params 不再重复传
  const { accountId, ...query } = params
  return request.get({ url: `/oa/account-analysis/${accountId}/followers`, params: query })
}

export function getAccountContentDetail(params: ContentQueryVO): Promise<PageResult<ContentVO>> {
  // S-R13 B2 修复：URL 模板已含 accountId，params 不再重复传
  const { accountId, ...query } = params
  return request.get({ url: `/oa/account-analysis/${accountId}/contents`, params: query })
}

export function exportAccountAnalysis(data: Record<string, unknown>): Promise<string> {
  return request.post({ url: '/oa/account-analysis/export', data })
}
