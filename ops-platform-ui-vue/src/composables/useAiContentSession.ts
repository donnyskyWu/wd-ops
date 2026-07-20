import { ref } from 'vue'
import type { AiContentContext, AiContentMessage, AiPreferenceDimension } from '@/api/aiContent'

export const DEFAULT_AI_INPUT_MESSAGE = '生成内容'

/** 新建内容尚无 contentId 时，暂存于编辑面板内的对话快照 */
export interface PendingAiConversation {
  sessionId: string
  conversationHistory: AiContentMessage[]
  roundCount: number
  preferenceSummary?: string
  preferenceDimensions?: Record<string, AiPreferenceDimension>
  persistContext?: AiContentContext
}

export function buildPendingSnapshot(params: {
  sessionId: string
  conversationHistory: AiContentMessage[]
  roundCount: number
  preferenceSummary?: string
  preferenceDimensions?: Record<string, AiPreferenceDimension>
  persistContext?: AiContentContext
}): PendingAiConversation | null {
  if (!params.conversationHistory.length) return null
  return {
    sessionId: params.sessionId,
    conversationHistory: [...params.conversationHistory],
    roundCount: params.roundCount,
    preferenceSummary: params.preferenceSummary || undefined,
    preferenceDimensions: params.preferenceDimensions
      ? { ...params.preferenceDimensions }
      : undefined,
    persistContext: params.persistContext,
  }
}

export function createAiContentSessionId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `sess_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

export function useAiContentSession() {
  const sessionId = ref(createAiContentSessionId())
  const roundCount = ref(0)
  const conversationHistory = ref<AiContentMessage[]>([])
  const latestAssistantContent = ref('')
  const preferenceSummary = ref('')
  const preferenceDimensions = ref<Record<string, AiPreferenceDimension>>({})

  const resetSession = () => {
    sessionId.value = createAiContentSessionId()
    roundCount.value = 0
    conversationHistory.value = []
    latestAssistantContent.value = ''
  }

  const resetPreference = () => {
    preferenceSummary.value = ''
    preferenceDimensions.value = {}
  }

  const appendUserMessage = (userMessage: string) => {
    conversationHistory.value.push({ role: 'user', content: userMessage })
  }

  const appendAssistantMessage = (assistantContent: string) => {
    conversationHistory.value.push({ role: 'assistant', content: assistantContent })
    latestAssistantContent.value = assistantContent
    roundCount.value += 1
  }

  const appendTurn = (userMessage: string, assistantContent: string) => {
    appendUserMessage(userMessage)
    appendAssistantMessage(assistantContent)
  }

  const restoreConversation = (messages: AiContentMessage[], rounds?: number) => {
    conversationHistory.value = [...messages]
    const assistantCount = messages.filter((m) => m.role === 'assistant').length
    roundCount.value = rounds ?? assistantCount
    const lastAssistant = [...messages].reverse().find((m) => m.role === 'assistant')
    latestAssistantContent.value = lastAssistant?.content || ''
  }

  const buildContext = (params: {
    matchName: string
    authorName: string
    schemeTypes: string[]
    historyRecord?: string
    anchorStyle?: string
    productDescription?: string
  }): AiContentContext => ({
    matchName: params.matchName,
    authorName: params.authorName,
    schemeTypes: params.schemeTypes,
    historyRecord: params.historyRecord?.trim() || undefined,
    anchorStyle: params.anchorStyle || undefined,
    productDescription: params.productDescription?.trim() || undefined,
  })

  return {
    sessionId,
    roundCount,
    conversationHistory,
    latestAssistantContent,
    preferenceSummary,
    preferenceDimensions,
    resetSession,
    resetPreference,
    appendUserMessage,
    appendAssistantMessage,
    appendTurn,
    restoreConversation,
    buildContext,
  }
}
