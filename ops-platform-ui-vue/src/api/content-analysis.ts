/**
 * 作品分析 - API（GATE-S3 真实 API）
 */
import { request } from '@/utils/request'
import type {
  ContentAnalysisVO,
  ContentAnalysisQuery,
  PageResult,
  ContentTrendDetailVO,
  ContentStatsVO,
  ContentExportReqVO,
} from '@/types/content-analysis'

export function getContentAnalysisList(params: ContentAnalysisQuery): Promise<PageResult<ContentAnalysisVO>> {
  return request.get({ url: '/oa/content-analysis/list', params })
}

export function getContentTrend(contentId: number): Promise<ContentTrendDetailVO> {
  return request.get({ url: '/oa/content-analysis/trend', params: { contentId } })
}

export function getContentStats(params: Omit<ContentAnalysisQuery, 'pageNo' | 'pageSize'>): Promise<ContentStatsVO> {
  return request.get({ url: '/oa/content-analysis/stats', params })
}

export function exportContentAnalysis(data: ContentExportReqVO): Promise<string> {
  return request.post({ url: '/oa/content-analysis/export', data })
}
