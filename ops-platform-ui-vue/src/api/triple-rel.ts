/**
 * M4 S-09 三方关联 API
 */
import { request } from '@/utils/request'

export interface TripleRelVO {
  id: number
  wechatAccountId?: number
  wechatName?: string
  videoAccountId?: number
  videoName?: string
  weworkAccountId?: number
  weworkName?: string
  relationType: string
  status: number
  bindTime?: string
}

export interface TripleRelStatistics {
  totalBound: number
  fullTriple: number
  wechatVideo: number
  wechatWework: number
  videoWework: number
  unbound: number
}

export interface TripleRelGraph {
  personalWechatId: number
  personalWechat?: { id: number; wechatId: string; accountName: string }
  videoAccounts?: { id: number; accountName: string }[]
  weworkAccount?: { id: number; accountName: string; corpId: string }
}

export function getTripleRelPage(params: {
  relationType?: string
  status?: number
  pageNo?: number
  pageSize?: number
}): Promise<{ list: TripleRelVO[]; total: number }> {
  return request.get({ url: '/oa/internal/triple-rel/list', params })
}

export function getTripleRelStatistics(): Promise<TripleRelStatistics> {
  return request.get({ url: '/oa/internal/triple-rel/statistics' })
}

export function getTripleRelGraph(personalWechatId: number): Promise<TripleRelGraph> {
  return request.get({ url: '/oa/internal/triple-rel/graph', params: { personalWechatId } })
}

export function createTripleRel(data: {
  personalWechatId?: number
  videoAccountIds?: number[]
  weworkAccountId?: number
  relationType: string
}): Promise<number> {
  return request.post({ url: '/oa/internal/triple-rel/create', data })
}

export function unbindTripleRel(id: number): Promise<boolean> {
  return request.put({ url: '/oa/internal/triple-rel/unbind', params: { id } })
}

export function rebindTripleRel(id: number): Promise<boolean> {
  return request.put({ url: '/oa/internal/triple-rel/rebind', params: { id } })
}
