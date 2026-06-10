/**
 * 内容管理 - API（GATE-S4 真实 API，oa_production_content）
 */
import { request } from '@/utils/request'
import type {
  ContentListItem,
  ContentQuery,
  PageResult,
  CreateContentReq,
  ReviewActionReq,
} from '@/types/content'

export function getContentList(params: ContentQuery): Promise<PageResult<ContentListItem>> {
  return request.get({ url: '/oa/content/list', params })
}

export function createContent(data: CreateContentReq): Promise<number> {
  return request.post({ url: '/oa/content/create', data })
}

export function updateContent(data: CreateContentReq & { id: number }): Promise<boolean> {
  return request.put({ url: '/oa/content/update', data })
}

export function submitContentReview(id: number): Promise<boolean> {
  return request.post({ url: `/oa/content/${id}/submit-review` })
}

export function reviewContent(id: number, data: ReviewActionReq): Promise<boolean> {
  return request.post({ url: `/oa/content/${id}/review`, data })
}

export function deleteContent(id: number): Promise<boolean> {
  return request.delete({ url: `/oa/content/${id}` })
}

/** S-07 stub：AI 生成占位 */
export function aiGenerateContent(prompt: string): Promise<{ content: string; title: string }> {
  return request.post({ url: '/oa/content/ai-generate', data: { prompt } })
}

export default {
  getContentList,
  createContent,
  updateContent,
  submitContentReview,
  reviewContent,
  deleteContent,
  aiGenerateContent,
}
