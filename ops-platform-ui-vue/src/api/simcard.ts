/**
 * M4 S-05 手机卡管理 API
 */
import { request } from '@/utils/request'

export interface SimCardVO {
  id: number
  phoneId?: number
  phoneNumberMasked?: string
  isPrimary?: string
  operator?: string
  assignedUserId?: number
  assignedUserName?: string
  iccidMasked?: string
  packageName?: string
  status: string
  totalLinkedAccounts?: number
  wechatMpCount?: number
  douyinCount?: number
  weworkCount?: number
  createTime?: string
}

export interface LinkedAccountVO {
  platformType: string
  platformLabel: string
  accountName: string
  accountId?: string
  status: string
  linkedAt?: string
}

export interface LinkedAccountsResult {
  phoneNumberMasked?: string
  operator?: string
  totalCount: number
  accounts: LinkedAccountVO[]
}

export interface SimCardPageResult {
  list: SimCardVO[]
  total: number
}

export interface SimCardCreateReq {
  phoneId?: number
  phoneNumber?: string
  isPrimary?: string
  operator: string
  assignedUserId: number
  iccid?: string
  packageName?: string
  status?: string
}

export interface SimCardUpdateReq extends Partial<SimCardCreateReq> {
  id: number
}

export function getSimCardPage(params: {
  iccid?: string
  phoneId?: number
  operator?: string
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<SimCardPageResult> {
  return request.get({ url: '/oa/sim-card/list', params })
}

export function createSimCard(data: SimCardCreateReq): Promise<number> {
  return request.post({ url: '/oa/sim-card/create', data })
}

export function updateSimCard(data: SimCardUpdateReq): Promise<boolean> {
  return request.put({ url: '/oa/sim-card/update', data })
}

export function deleteSimCard(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/sim-card/delete', params: { id } })
}

export function getLinkedAccounts(
  id: number,
  params?: { platformType?: string; operator?: string },
): Promise<LinkedAccountsResult> {
  return request.get({ url: `/oa/sim-card/${id}/linked-accounts`, params })
}
