/**
 * M4 S-08 个人账号 API（个微 / 企微）
 */
import { request } from '@/utils/request'

export interface PersonalWechatVO {
  id: number
  accountName: string
  wechatId: string
  contactPhone?: string
  phoneId?: number
  phoneNumberMasked?: string
  status: string
  apiUrl?: string
  appId?: string
  appSecret?: string
  token?: string
  createTime?: string
}

export interface WeworkEmployeeVO {
  id: number
  weworkAccountId: number
  nickname: string
  weworkUserId: string
  phone?: string
  department?: string
  position?: string
  status: string
  createTime?: string
}

export interface WeworkVO {
  id: number
  accountName: string
  corpId: string
  agentId: string
  secret?: string
  status: string
  createTime?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export function getPersonalWechatPage(params: {
  accountName?: string
  wechatId?: string
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<PageResult<PersonalWechatVO>> {
  return request.get({ url: '/oa/internal/personal-account/list', params })
}

export function getPersonalWechat(id: number): Promise<PersonalWechatVO> {
  return request.get({ url: `/oa/internal/personal-account/${id}` })
}

export function createPersonalWechat(data: {
  accountName: string
  wechatId: string
  contactPhone?: string
  phoneId?: number
  status?: string
}): Promise<number> {
  return request.post({ url: '/oa/internal/personal-account/create', data })
}

export function updatePersonalWechat(data: {
  id: number
  accountName?: string
  wechatId?: string
  contactPhone?: string
  phoneId?: number
  status?: string
}): Promise<boolean> {
  return request.put({ url: '/oa/internal/personal-account/update', data })
}

export function deletePersonalWechat(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/internal/personal-account/delete', params: { id } })
}

export function savePersonalWechatApiConfig(data: {
  id: number
  apiUrl?: string
  appId?: string
  appSecret?: string
  token?: string
}): Promise<boolean> {
  return request.post({ url: '/oa/internal/personal-account/api-config', data })
}

export function getPersonalWechatApiConfig(id: number): Promise<PersonalWechatVO> {
  return request.get({ url: `/oa/internal/personal-account/api-config/${id}` })
}

export function getWeworkPage(params: {
  accountName?: string
  corpId?: string
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<PageResult<WeworkVO>> {
  return request.get({ url: '/oa/internal/wework/list', params })
}

export function createWework(data: {
  accountName: string
  corpId: string
  agentId: string
  secret: string
  status?: string
}): Promise<number> {
  return request.post({ url: '/oa/internal/wework/create', data })
}

export function updateWework(data: {
  id: number
  accountName?: string
  corpId?: string
  agentId?: string
  secret?: string
  status?: string
}): Promise<boolean> {
  return request.put({ url: '/oa/internal/wework/update', data })
}

export function deleteWework(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/internal/wework/delete', params: { id } })
}

export function getWeworkEmployeePage(params: {
  weworkAccountId?: number
  nickname?: string
  weworkUserId?: string
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<PageResult<WeworkEmployeeVO>> {
  return request.get({ url: '/oa/internal/wework/employee/list', params })
}

export function createWeworkEmployee(data: {
  weworkAccountId: number
  nickname: string
  weworkUserId: string
  phone?: string
  department?: string
  position?: string
  status?: string
}): Promise<number> {
  return request.post({ url: '/oa/internal/wework/employee/create', data })
}

export function updateWeworkEmployee(data: {
  id: number
  nickname: string
  weworkUserId: string
  phone?: string
  department?: string
  position?: string
  status?: string
}): Promise<boolean> {
  return request.put({ url: '/oa/internal/wework/employee/update', data })
}

export function deleteWeworkEmployee(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/internal/wework/employee/delete', params: { id } })
}
