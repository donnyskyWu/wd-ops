/**
 * M9 系统参数 API
 */
import { request } from '@/utils/request'

export interface ParamVO {
  id: number
  paramName: string
  paramKey: string
  paramValue: string
  paramType: string
  category: string
  remark?: string
  updateTime?: string
}

export interface ParamPageResult {
  list: ParamVO[]
  total: number
}

export function fetchParamList(params: {
  paramName?: string
  paramKey?: string
  category?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<ParamPageResult>({ url: '/oa/system/param/list', params })
}

export function createParam(data: Omit<ParamVO, 'id' | 'updateTime'>) {
  return request.post<number>({ url: '/oa/system/param/create', data })
}

export function updateParam(data: ParamVO) {
  return request.put<boolean>({ url: '/oa/system/param/update', data })
}

export function deleteParam(id: number) {
  return request.delete<boolean>({ url: '/oa/system/param/delete', params: { id } })
}

export const PARAM_CATEGORY_TAB: Record<string, string> = {
  basic: 'BASIC',
  collect: 'COLLECT',
  ai: 'AI',
  notification: 'NOTIFICATION',
  contentReview: 'CONTENT_REVIEW',
}

export const PARAM_TYPE_LABEL: Record<string, string> = {
  STRING: '字符串',
  NUMBER: '数字',
  BOOLEAN: '布尔',
  JSON: 'JSON',
}

export const PARAM_TYPE_TAG: Record<string, '' | 'success' | 'warning' | 'info'> = {
  STRING: '',
  NUMBER: 'success',
  BOOLEAN: 'warning',
  JSON: 'info',
}
