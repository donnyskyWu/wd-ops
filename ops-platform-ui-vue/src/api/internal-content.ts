/**
 * 内部内容分析 + 补录 - API（GATE-S3 真实 API）
 */
import { request } from '@/utils/request'
import type {
  InternalContentVO,
  InternalContentQuery,
  PageResult,
  ContentTrendDetailVO,
} from '@/types/internal-content'

export function getInternalContentList(params: InternalContentQuery): Promise<PageResult<InternalContentVO>> {
  return request.get({ url: '/oa/internal-content/list', params })
}

export function getInternalContentTrend(contentId: number): Promise<ContentTrendDetailVO> {
  return request.get({ url: `/oa/internal-content/${contentId}/trend` })
}

export function submitContentImport(data: Record<string, unknown>): Promise<number> {
  return request.post({ url: '/oa/internal-content/import', data })
}

export function getContentImportList(params: Record<string, unknown>): Promise<PageResult<unknown>> {
  return request.get({ url: '/oa/internal-content/import/list', params })
}

export function reviewContentImport(id: number, data: Record<string, unknown>): Promise<boolean> {
  return request.put({ url: `/oa/internal-content/import/${id}/review`, data })
}
