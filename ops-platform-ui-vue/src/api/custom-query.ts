/**
 * M6 自定义查询 - API 接口封装
 */
import { request } from '@/utils/request'

export function getCustomQueryList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/query/list', params })
}

export function createCustomQuery(data: Record<string, unknown>) {
  return request.post({ url: '/oa/query/create', data })
}

export function executeCustomQuery(id: number) {
  return request.post({ url: `/oa/query/${id}/execute` })
}

/** 不保存，直接执行 SQL（自定义查询 Tab 内联结果） */
export function previewCustomQuery(data: { sqlText: string }) {
  return request.post({ url: '/oa/query/preview', data })
}

export function updateCustomQuery(data: Record<string, unknown>) {
  return request.put({ url: '/oa/query/update', data })
}

export function publishCustomQuery(id: number) {
  return request.post({ url: `/oa/query/${id}/publish` })
}
