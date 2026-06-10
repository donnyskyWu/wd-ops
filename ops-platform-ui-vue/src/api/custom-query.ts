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

export function publishCustomQuery(id: number) {
  return request.post({ url: `/oa/query/${id}/publish` })
}
