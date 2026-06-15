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
  return request.get({
    url: '/oa/content/list',
    params: {
      ...params,
      pageNum: params.pageNo,
    },
  })
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

/** LLM 调用可能耗时较长，单独放宽超时（全局默认 15s 不足） */
const AI_GENERATE_TIMEOUT_MS = 180_000

/** AI 辅助生成（模式 A） */
export function aiGenerateContent(data: {
  modelId: number
  promptId: number
  contentType?: string
  documentType?: string
  competitionId?: string
  competitionName?: string
  taskId?: number
}): Promise<{ content: string; title?: string; eventInfo?: string; mock?: boolean; message?: string }> {
  return request.post({ url: '/oa/content/ai-generate', data, timeout: AI_GENERATE_TIMEOUT_MS })
}

/** 按 ID 加载内容详情 */
export function getContent(id: number) {
  return request.get({ url: `/oa/content/${id}` })
}

/** 按内容/文档类型列出可用 AI 提示词 */
export function getAiPromptOptions(contentType: string, documentType?: string) {
  return request.get<Array<{ id: number; templateName: string; scene?: string }>>({
    url: '/oa/content/ai-prompt-options',
    params: { contentType, documentType },
  })
}

/** S-12：按任务加载关联内容 */
export function getContentByTask(taskId: number) {
  return request.get({ url: '/oa/content/by-task', params: { taskId } })
}

/** S-13：同赛事短视频文案引用 */
export function getScriptRef(competitionId: string, documentType = 'SHORT_VIDEO_SCRIPT') {
  return request.get({
    url: '/oa/content/script-ref',
    params: { competitionId, documentType },
  })
}

/** S-13：任务驱动确认 → COMPLETED */
export function confirmContent(id: number) {
  return request.post({ url: `/oa/content/${id}/confirm` })
}

/** S-13：AI 生成占位（BLK-M2-005/010） */
export function generateContent(id: number) {
  return request.post({
    url: `/oa/content/${id}/generate`,
  })
}

/** S-13：当前用户 IP 组 + 作者 */
export function getMyIpGroups() {
  return request.get({ url: '/oa/user/ip-groups' })
}

export function getContentReviewConfig(): Promise<{
  level1Enabled: boolean
  level2Enabled: boolean
  level1Role?: string
  level2Role?: string
}> {
  return request.get({ url: '/oa/content/review-config' })
}

export interface ContentPublishPlatformOption {
  platformType: string
  platformName: string
  accounts: Array<{
    id: number
    accountName: string
    externalAccountId?: string
    publishEnabled?: boolean
  }>
}

export function getContentPublishOptions(id: number): Promise<{ platforms: ContentPublishPlatformOption[] }> {
  return request.get({ url: `/oa/content/${id}/publish-options` })
}

export function publishContent(id: number, data: { platformType: string; accountIds: number[] }) {
  return request.post<{ contentId: number; status: string; mock?: boolean; records?: unknown[] }>({
    url: `/oa/content/${id}/publish`,
    data,
  })
}

export function transferContentToKnowledge(id: number) {
  return request.post<{ contentId: number; knowledgeId: number }>({
    url: `/oa/content/${id}/transfer-to-knowledge`,
  })
}

export default {
  getContentList,
  createContent,
  updateContent,
  submitContentReview,
  reviewContent,
  deleteContent,
  aiGenerateContent,
  getContent,
  getAiPromptOptions,
  getContentByTask,
  getScriptRef,
  confirmContent,
  generateContent,
  getMyIpGroups,
  getContentReviewConfig,
  getContentPublishOptions,
  publishContent,
  transferContentToKnowledge,
}
