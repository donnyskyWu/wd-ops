/**
 * M4 S-03 实名人中介人 API
 */
import { request } from '@/utils/request'

export interface IntermediaryVO {
  id: number
  realnameId: number
  intermediaryName: string
  intermediaryPhoneMasked?: string
  intermediaryWechat?: string
  relationType: string
  commissionRate?: number
  commissionRateDisplay?: string
  remark?: string
  createTime?: string
}

export interface IntermediaryCreateReq {
  intermediaryName: string
  intermediaryPhone?: string
  intermediaryWechat?: string
  relationType: string
  commissionRate: number
  remark?: string
}

export interface IntermediaryUpdateReq extends Partial<IntermediaryCreateReq> {
  id: number
}

export function listIntermediaries(realnameId: number): Promise<IntermediaryVO[]> {
  return request.get({ url: `/oa/realname/${realnameId}/intermediaries` })
}

export function createIntermediary(realnameId: number, data: IntermediaryCreateReq): Promise<number> {
  return request.post({ url: `/oa/realname/${realnameId}/intermediary`, data })
}

export function updateIntermediary(data: IntermediaryUpdateReq): Promise<boolean> {
  return request.put({ url: '/oa/realname/intermediary/update', data })
}

export function deleteIntermediary(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/realname/intermediary/delete', params: { id } })
}
