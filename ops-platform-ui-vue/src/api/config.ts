/**
 * M8 配置管理 API — 契约见 docs/engineering/API-M8-配置管理.md
 */
import { request } from '@/utils/request'

export interface CollectConfigVO {
  id: number
  configName: string
  subType?: string
  platformType?: string
  accountId?: number
  accountName?: string
  accountIdentifier?: string
  appId?: string
  appSecretMasked?: string
  cookie?: string
  authTokenMasked?: string
  fieldMapping?: string
  isLive?: boolean
  dbHost?: string
  dbPort?: number
  dbName?: string
  dbUsername?: string
  dbPasswordMasked?: string
  tableName?: string
  syncMode?: string
  connStatus?: string
  collectFrequency?: string
  collectMethod?: string
  apiUrl?: string
  apiKeyMasked?: string
  status?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ThresholdConfigVO {
  id: number
  thresholdCategory?: string
  thresholdType?: string
  metricName: string
  metricType?: string
  platformType?: string
  contentType?: string
  judgeMode?: string
  lowFans?: number
  highFans?: number
  dailyLow?: number
  dailyHigh?: number
  hotValue?: number
  lowValue?: number
  overrideAccountId?: number
  overrideValue?: number
  compareOperator?: string
  thresholdValue?: number
  alertLevel?: string
  notifyMethods?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface AiModelConfigVO {
  id: number
  modelName: string
  modelId?: string
  modelType?: string
  apiEndpoint?: string
  apiKeyMasked?: string
  maxTokens?: number
  timeout?: number
  isDefault?: boolean
  connStatus?: string
  temperature?: number
  topP?: number
  status?: string
  remark?: string
  createTime?: string
}

export interface AiModelStatsVO {
  total: number
  enabled: number
  connected: number
  defaultCount: number
}

export interface AiPromptConfigVO {
  id: number
  templateName: string
  version?: string
  scene?: string
  contentType?: string
  documentType?: string
  promptContent: string
  variableDesc?: string
  temperature?: number
  status?: string
  remark?: string
  createTime?: string
}

export interface KeywordConfigVO {
  id: number
  platform: string
  keyword: string
  matchType: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface AoCreateApiVO {
  id?: number
  apiUrl: string
  appId: string
  appSecretMasked?: string
  tokenMasked?: string
  status?: string
  dailyQuota?: number
  currentUsage?: number
}

export interface AoCreateAccountVO {
  id: number
  aocreateApiId?: number
  accountName: string
  aochuangAccountId: string
  status?: string
  connStatus?: string
  lastDeviceSyncAt?: string
  createTime?: string
  updateTime?: string
}

export interface AoCreateAccountTestResultVO {
  success: boolean
  deviceCount: number
  connStatus?: string
  message?: string
}

export interface ImportResultVO {
  successCount: number
  failCount: number
  failReasons: string[]
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
    toggleStatus(id: number, status: string) {
      return request.put<boolean>({ url: `${base}/toggle-status`, data: { id, status } })
    },
    testConnection(id: number) {
      return request.post<boolean>({ url: `${base}/test-connection`, data: { id } })
    },
  }
}

export const internalCollectApi = {
  ...collectApi('internal-collect'),
  getAoCreate() {
    return request.get<AoCreateApiVO | null>({ url: '/oa/config/internal-collect/aocreate' })
  },
  saveAoCreate(data: Record<string, unknown>) {
    return request.post<boolean>({ url: '/oa/config/internal-collect/aocreate', data })
  },
  listAoCreateAccounts(params?: Record<string, unknown>) {
    return request.get<ConfigPageResult<AoCreateAccountVO>>({
      url: '/oa/config/internal-collect/aocreate/accounts/list',
      params,
    })
  },
  createAoCreateAccount(data: Record<string, unknown>) {
    return request.post<number>({ url: '/oa/config/internal-collect/aocreate/accounts/create', data })
  },
  updateAoCreateAccount(data: Record<string, unknown>) {
    return request.put<boolean>({ url: '/oa/config/internal-collect/aocreate/accounts/update', data })
  },
  deleteAoCreateAccount(id: number) {
    return request.delete<boolean>({
      url: '/oa/config/internal-collect/aocreate/accounts/delete',
      params: { id },
    })
  },
  testAoCreateAccountConnection(id: number) {
    return request.post<AoCreateAccountTestResultVO>({
      url: `/oa/config/internal-collect/aocreate/accounts/${id}/test-connection`,
    })
  },
}

export const externalCollectApi = {
  ...collectApi('external-collect'),
  importCsv(content: string) {
    return request.post<ImportResultVO>({ url: '/oa/config/external-collect/import', data: { content } })
  },
  keywordList(params?: Record<string, unknown>) {
    return request.get<ConfigPageResult<KeywordConfigVO>>({ url: '/oa/config/external-collect/keyword/list', params })
  },
  keywordCreate(data: Record<string, unknown>) {
    return request.post<number>({ url: '/oa/config/external-collect/keyword/create', data })
  },
  keywordUpdate(data: Record<string, unknown>) {
    return request.put<boolean>({ url: '/oa/config/external-collect/keyword/update', data })
  },
  keywordDelete(id: number) {
    return request.delete<boolean>({ url: '/oa/config/external-collect/keyword/delete', params: { id } })
  },
}

export const externalSourceApi = collectApi('external-source')
export const orderCollectApi = collectApi('order-collect')

export function fetchThresholdList(params?: Record<string, unknown>) {
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

export function fetchAiModelList(params?: Record<string, unknown>) {
  return request.get<ConfigPageResult<AiModelConfigVO>>({ url: '/oa/config/ai-model/list', params })
}

export function fetchAiModelStats() {
  return request.get<AiModelStatsVO>({ url: '/oa/config/ai-model/stats' })
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

export function testAiModelConnection(id: number) {
  return request.post<boolean>({ url: '/oa/config/ai-model/test-connection', data: { id } })
}

export function setDefaultAiModel(id: number) {
  return request.put<boolean>({ url: '/oa/config/ai-model/set-default', data: { id } })
}

export function fetchAiPromptList(params?: Record<string, unknown>) {
  return request.get<ConfigPageResult<AiPromptConfigVO>>({ url: '/oa/config/ai-prompt/list', params })
}

export function getAiPrompt(id: number) {
  return request.get<AiPromptConfigVO>({ url: '/oa/config/ai-prompt/get', params: { id } })
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
