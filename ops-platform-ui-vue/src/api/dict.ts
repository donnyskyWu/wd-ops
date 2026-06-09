/**
 * 字典查询 - API（ADR-006 / API-M11）
 */
import { request } from '@/utils/request'

export interface DictItemVO {
  dictType: string
  label: string
  value: string
  sort: number
  status: string
}

export interface DictTypeVO {
  type: string
  name: string
  status: string
}

export function fetchDictData(type: string) {
  return request.get<{ list: DictItemVO[]; total: number }>({
    url: '/oa/dict/data',
    params: { type },
  })
}

export function fetchDictTypes() {
  return request.get<{ list: DictTypeVO[]; total: number }>({
    url: '/oa/dict/types',
  })
}
