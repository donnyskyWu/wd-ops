/**
 * M8 配置管理 API
 * 契约: /admin-api/oa/config/*
 */
import { request } from '@/utils/request'

export interface CollectConfigVO {
  id: number
  configName: string
  subType?: string
  platformType?: string
  accountId?: number
  collectFrequency?: string
  collectMethod?: string
  apiUrl?: string
  apiKeyMasked?: string
  requestMethod?: string
  requestParams?: string
  responseMapping?: string
  collectFields?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface ThresholdConfigVO {
  id: number
  metricName: string
  metricType: string
  platformType?: string
  ipGroupId?: number
  compareOperator?: string
  thresholdValue: number
  alertLevel?: string
  notifyMethods?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface AiModelConfigVO {
  id: number
  modelName: string
  modelType?: string
  apiEndpoint?: string
  apiKeyMasked?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  status?: string
  remark?: string
  createTime?: string
}

export interface AiPromptConfigVO {
  id: number
  templateName: string
  scene?: string
  promptContent: string
  variableDesc?: string
  temperature?: number
  status?: string
  remark?: string
  createTime?: string
}

export interface ConfigPageResult<T> {
  list: T[]
  total: number
}

type CollectScope = 'internal-collect' | 'external-collect' | 'external-source' | 'order-collect'

function collectApi(scope: CollectScope) {
  const base = `/oa/config/${scope}`
  return {
    list(params?: Record<string, unknown>) {
      return request.get<ConfigPageResult<CollectConfigVO>>({ url: `${base}/list`, params })
    },
    create(data: Record<string, unknown>) {
      return request.post<number>({ url: `${base}/create`, data })
    },
    update(data: Record<string, unknown>) {
      return request.put<boolean>({ url: `${base}/update`, data })
    },
    delete(id: number) {
      return request.delete<boolean>({ url: `${base}/delete`, params: { id } })
    },
  }
}

export const internalCollectApi = collectApi('internal-collect')
export const externalCollectApi = collectApi('external-collect')
export const externalSourceApi = collectApi('external-source')
export const orderCollectApi = collectApi('order-collect')

export function fetchThresholdList(params?: {
  metricName?: string
  metricType?: string
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<ConfigPageResult<ThresholdConfigVO>>({ url: '/oa/config/threshold/list', params })
}

export function createThreshold(data: Partial<ThresholdConfigVO>) {
  return request.post<number>({ url: '/oa/config/threshold/create', data })
}

export function updateThreshold(data: Partial<ThresholdConfigVO> & { id: number }) {
  return request.put<boolean>({ url: '/oa/config/threshold/update', data })
}

export function deleteThreshold(id: number) {
  return request.delete<boolean>({ url: '/oa/config/threshold/delete', params: { id } })
}

export function fetchAiModelList(params?: {
  modelName?: string
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<ConfigPageResult<AiModelConfigVO>>({ url: '/oa/config/ai-model/list', params })
}

export function createAiModel(data: Record<string, unknown>) {
  return request.post<number>({ url: '/oa/config/ai-model/create', data })
}

export function updateAiModel(data: Record<string, unknown>) {
  return request.put<boolean>({ url: '/oa/config/ai-model/update', data })
}

export function deleteAiModel(id: number) {
  return request.delete<boolean>({ url: '/oa/config/ai-model/delete', params: { id } })
}

export function fetchAiPromptList(params?: {
  templateName?: string
  scene?: string
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<ConfigPageResult<AiPromptConfigVO>>({ url: '/oa/config/ai-prompt/list', params })
}

export function createAiPrompt(data: Record<string, unknown>) {
  return request.post<number>({ url: '/oa/config/ai-prompt/create', data })
}

export function updateAiPrompt(data: Record<string, unknown>) {
  return request.put<boolean>({ url: '/oa/config/ai-prompt/update', data })
}

export function deleteAiPrompt(id: number) {
  return request.delete<boolean>({ url: '/oa/config/ai-prompt/delete', params: { id } })
}
