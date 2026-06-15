/**
 * M4 S-04 手机管理 API
 */
import { request } from '@/utils/request'

export interface PhoneVO {
  id: number
  realnameId?: number
  realName?: string
  phoneNumberMasked?: string
  phoneCode?: string
  phoneModel?: string
  settingsScreenshotKey?: string
  settingsScreenshotUrl?: string
  frontImageKey?: string
  frontImageUrl?: string
  backImageKey?: string
  backImageUrl?: string
  purchaseBatch?: string
  purchaseDate?: string
  purchaseTime?: string
  handlerName?: string
  deviceNumber?: string
  isAochuang?: string
  phoneType?: string
  keeperId?: number
  keeperName?: string
  wechatBound?: string
  status: string
  accountBoundCount?: number
  createTime?: string
}

export interface PhonePageResult {
  list: PhoneVO[]
  total: number
}

export interface PhoneCreateReq {
  realnameId?: number
  phoneNumber: string
  phoneCode?: string
  phoneModel?: string
  settingsScreenshotKey?: string
  frontImageKey?: string
  backImageKey?: string
  purchaseBatch?: string
  purchaseDate?: string
  purchaseTime?: string
  handlerName?: string
  deviceNumber?: string
  isAochuang?: string
  phoneType?: string
  keeperId: number
  wechatBound?: string
  status?: string
}

export interface PhoneUpdateReq extends Partial<PhoneCreateReq> {
  id: number
}

export function getPhonePage(params: {
  phoneNumber?: string
  realnameId?: number
  phoneType?: string
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<PhonePageResult> {
  return request.get({ url: '/oa/phone/list', params })
}

export function createPhone(data: PhoneCreateReq): Promise<number> {
  return request.post({ url: '/oa/phone/create', data })
}

export function updatePhone(data: PhoneUpdateReq): Promise<boolean> {
  return request.put({ url: '/oa/phone/update', data })
}

export function deletePhone(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/phone/delete', params: { id } })
}
