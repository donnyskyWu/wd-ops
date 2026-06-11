/**
 * M9 字典管理 API（admin CRUD）
 */
import { request } from '@/utils/request'

export interface DictAdminRow {
  id: number
  typeId?: number
  dictName: string
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  status: string
  colorType?: string
  remark?: string
}

export interface DictPageResult {
  list: DictAdminRow[]
  total: number
}

export function fetchDictAdminList(params: {
  dictName?: string
  dictType?: string
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<DictPageResult>({ url: '/oa/system/dict/list', params })
}

export function createDictType(data: {
  dictType: string
  dictName: string
  items: Array<{
    dictLabel: string
    dictValue: string
    sort?: number
    status?: string
    colorType?: string
    remark?: string
  }>
}) {
  return request.post<number>({ url: '/oa/system/dict/create', data })
}

export function updateDictType(data: {
  id: number
  dictName: string
  status?: string
  items?: Array<{
    id?: number
    dictLabel: string
    dictValue: string
    sort?: number
    status?: string
    colorType?: string
    remark?: string
  }>
}) {
  return request.put<boolean>({ url: '/oa/system/dict/update', data })
}

export function deleteDictData(id: number) {
  return request.delete<boolean>({ url: `/oa/system/dict/${id}` })
}
