/**
 * M4 S-06 平台账号管理 API
 */
import { request } from '@/utils/request'

export interface PlatformAccountVO {
  id: number
  platformType: string
  accountType?: string
  accountName: string
  externalAccountId?: string
  companyId?: number
  companyName?: string
  realnameId?: number
  realName?: string
  phoneId?: number
  phoneNumberMasked?: string
  simCardId?: number
  simCardMasked?: string
  intermediaryId?: number
  ipGroupId?: number
  status: string
  hasCookie?: boolean
  createTime?: string
}

export interface PlatformAccountPageResult {
  list: PlatformAccountVO[]
  total: number
}

export interface PlatformAccountCreateReq {
  platformType: string
  accountName: string
  externalAccountId: string
  accountType?: string
  companyId: number
  realnameId: number
  phoneId?: number
  simCardId?: number
  intermediaryId?: number
  ipGroupId?: number
  cookie?: string
  status?: string
  forceReplace?: boolean
  reason?: string
}

export interface PlatformAccountUpdateReq extends Partial<PlatformAccountCreateReq> {
  id: number
}

export interface PlatformAccountReplaceReq {
  realnameId?: number
  phoneId?: number
  simCardId?: number
  reason: string
}

export function getPlatformAccountPage(params: {
  platformType?: string
  accountName?: string
  companyId?: number
  realnameId?: number
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<PlatformAccountPageResult> {
  return request.get({ url: '/oa/account/list', params })
}

export function getPlatformAccount(id: number): Promise<PlatformAccountVO> {
  return request.get({ url: '/oa/account/get', params: { id } })
}

export function createPlatformAccount(data: PlatformAccountCreateReq): Promise<number> {
  return request.post({ url: '/oa/account/create', data })
}

export function updatePlatformAccount(data: PlatformAccountUpdateReq): Promise<boolean> {
  return request.put({ url: '/oa/account/update', data })
}

export function deletePlatformAccount(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/account/delete', params: { id } })
}

export function replacePlatformAccount(id: number, data: PlatformAccountReplaceReq): Promise<boolean> {
  return request.post({ url: `/oa/account/${id}/replace`, data })
}
