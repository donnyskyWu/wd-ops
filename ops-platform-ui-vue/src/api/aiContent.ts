/**
 * AI 内容对话生成 API（ADR-053 · /oa/ai-content/*）
 */
import { request } from '@/utils/request'

const AI_CONTENT_TIMEOUT_MS = 180_000

export interface AiContentContext {
  matchName: string
  authorName: string
  schemeTypes: string[]
  historyRecord?: string
  anchorStyle?: string
  productDescription?: string
}

export interface AiContentMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AiPreferenceDimension {
  value: string
  confidence?: number
  sourceRound?: number
}

export interface AiPreferenceSummary {
  summaryText: string
  dimensions?: Record<string, AiPreferenceDimension>
  sourceSessionId?: string
  generatedAt?: string
  isUpdatedByUser?: boolean
}

export interface AiContentModel {
  id: number
  name: string
  icon?: string
  status: string
  description?: string
  isDefault?: boolean
}

export function generateAiContent(data: {
  sessionId: string
  modelId: number
  message: string
  context: AiContentContext
  preferenceSummary?: string
  conversationHistory?: AiContentMessage[]
  roundCount?: number
}) {
  return request.post<{
    sessionId: string
    content: string
    modelId: number
    roundCount: number
    tokensUsed?: number
    generatedAt?: string
    mock?: boolean
    message?: string
  }>({ url: '/oa/ai-content/generate', data, timeout: AI_CONTENT_TIMEOUT_MS })
}

export function fetchAiContentModels() {
  return request.get<{ models: AiContentModel[] }>({ url: '/oa/ai-content/models' })
}

export function fetchAiPreferenceSummary(authorId?: number) {
  return request.get<AiPreferenceSummary | null>({
    url: '/oa/ai-content/preference-summary',
    params: authorId ? { authorId } : undefined,
  })
}

export function updateAiPreferenceSummary(data: {
  summaryText: string
  dimensions?: Record<string, AiPreferenceDimension>
}) {
  return request.put<{ updatedAt: string }>({
    url: '/oa/ai-content/preference-summary',
    data,
  })
}

export function generateAiPreferenceSummary(data: {
  sessionId: string
  conversationHistory: AiContentMessage[]
  context: AiContentContext
  authorId?: number
  contentId?: number
  preferenceSummary?: string
}) {
  return request.post<AiPreferenceSummary>({
    url: '/oa/ai-content/preference-summary/generate',
    data,
    timeout: 30_000,
  })
}

export function adoptAiContent(data: {
  sessionId: string
  contentId?: number
  content: string
  modelId: number
  schemeTypes?: string[]
  roundCount?: number
}) {
  return request.post<{ sessionId: string; contentId?: number; adoptedAt: string }>({
    url: '/oa/ai-content/adopt',
    data,
  })
}

export interface AiConversationHistory {
  conversationHistory: AiContentMessage[]
  roundCount?: number
  sourceSessionId?: string
  savedAt?: string
}

export function fetchAiConversationHistory(params?: { contentId?: number; authorId?: number }) {
  return request.get<AiConversationHistory | null>({
    url: '/oa/ai-content/conversation',
    params,
  })
}

export function saveAiConversationHistory(data: {
  sessionId: string
  conversationHistory: AiContentMessage[]
  authorId?: number
  contentId?: number
}) {
  return request.put<AiConversationHistory>({
    url: '/oa/ai-content/conversation',
    data,
  })
}
