/**
 * M4 S-02 实名人管理 API
 */
import { request } from '@/utils/request'

export interface RealnameVO {
  id: number
  companyId?: number
  companyName?: string
  realName: string
  idType: string
  idCardMasked?: string
  phoneMasked?: string
  wechat?: string
  gender?: string
  status: string
  idCardFrontKey?: string
  idCardFrontUrl?: string
  idCardBackKey?: string
  idCardBackUrl?: string
  createTime?: string
}

export interface RealnamePageResult {
  list: RealnameVO[]
  total: number
}

export interface RealnameCreateReq {
  companyId?: number
  realName: string
  idType: string
  idCard: string
  phone: string
  wechat?: string
  gender?: string
  status?: string
  idCardFrontKey?: string
  idCardBackKey?: string
}

export interface RealnameUpdateReq extends Partial<RealnameCreateReq> {
  id: number
}

export function getRealnamePage(params: {
  realName?: string
  companyId?: number
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<RealnamePageResult> {
  return request.get({ url: '/oa/realname/list', params })
}

export function getRealname(id: number): Promise<RealnameVO> {
  return request.get({ url: `/oa/realname/${id}` })
}

export function createRealname(data: RealnameCreateReq): Promise<number> {
  return request.post({ url: '/oa/realname/create', data })
}

export function updateRealname(data: RealnameUpdateReq): Promise<boolean> {
  return request.put({ url: '/oa/realname/update', data })
}

export function deleteRealname(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/realname/delete', params: { id } })
}
