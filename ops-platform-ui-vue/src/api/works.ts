/**
 * 作品分析 - API接口封装
 */

import { request } from '@/utils/request'
import type {
  ContentAnalysisVO,
  ContentStats,
  ContentTrendPoint,
  ContentAnalysisQuery,
  PageResult,
} from '@/types/works'

export function getContentAnalysisList(params: ContentAnalysisQuery): Promise<PageResult<ContentAnalysisVO>> {
  return request.get({ url: '/oa/content-analysis/list', params })
}

export function getContentStats(
  params: Pick<ContentAnalysisQuery, 'ipGroupId' | 'platformType' | 'accountId' | 'startDate' | 'endDate'>,
): Promise<ContentStats> {
  return request.get({ url: '/oa/content-analysis/stats', params })
}

export function getContentTrend(contentId: number): Promise<ContentTrendPoint[]> {
  return request.get({ url: `/oa/content-analysis/${contentId}/trend` })
}
