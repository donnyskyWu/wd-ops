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
  params: Pick<ContentAnalysisQuery, 'ipGroupId' | 'platformType' | 'contentType' | 'accountId' | 'startDate' | 'endDate'>,
): Promise<ContentStats> {
  return request.get({ url: '/oa/content-analysis/stats', params })
}

export function getContentTrend(params: { contentId: number; startDate?: string; endDate?: string }): Promise<ContentTrendPoint[]> {
  // S-R8 B3: 修正 trend 端点路径（去掉 ${contentId}，改为 query param）
  return request.get({ url: '/oa/content-analysis/trend', params })
}
