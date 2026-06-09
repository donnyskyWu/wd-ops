/**
 * 粉丝分析 - API（GATE-S3 真实 API）
 */
import { request } from '@/utils/request'
import type {
  FollowerAnalysisVO,
  FollowerAnalysisQuery,
  PageResult,
  FollowerTrendVO,
  FollowerTrendQuery,
  FollowerExportReqVO,
} from '@/types/follower-analysis'

export function getFollowerAnalysisList(params: FollowerAnalysisQuery): Promise<PageResult<FollowerAnalysisVO>> {
  return request.get({ url: '/oa/follower-analysis/list', params })
}

export function getFollowerTrend(params: FollowerTrendQuery): Promise<FollowerTrendVO[]> {
  return request.get({ url: '/oa/follower-analysis/trend', params })
}

export function exportFollowerAnalysis(data: FollowerExportReqVO): Promise<string> {
  return request.post({ url: '/oa/follower-analysis/export', data })
}
