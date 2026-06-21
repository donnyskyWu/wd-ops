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
  linkedWeworkEmployeeId?: number
  linkedWeworkEmployeeName?: string
  linkedWeworkUserId?: string
  createTime?: string
  aochuangWechatAccountId?: string
  aochuangAccountRefId?: number
  aochuangAccountName?: string
  aochuangBindStatus?: string
  aochuangNickname?: string
  aochuangAvatar?: string
  aochuangIsAlive?: boolean
  lastDeviceSyncAt?: string
  lastFriendSyncAt?: string
  lastMessageSyncAt?: string
  collectStatus?: string
}

export interface AochuangPendingDeviceVO {
  aochuangWechatAccountId: string
  wechatId?: string
  alias?: string
  nickname?: string
  avatar?: string
  isAlive?: boolean
  aochuangAccountRefId?: number
  aochuangAccountName?: string
  suggestedPersonalWechatId?: number
  fuzzyScore?: number
}

export interface PersonalWechatSyncDevicesResult {
  autoBoundCount: number
  updatedSnapshotCount: number
  pendingCount: number
  pendingDevices: AochuangPendingDeviceVO[]
}

export interface AochuangFriendVO {
  id: number
  personalWechatId: number
  aochuangWechatAccountId: string
  aochuangFriendId: string
  wechatId?: string
  alias?: string
  nickname?: string
  avatar?: string
  remark?: string
  syncedAt?: string
}

export interface PersonalWechatSyncFriendsResult {
  syncedCount: number
  createdCount: number
  updatedCount: number
  completed: boolean
}

export interface AochuangMessageVO {
  id: number
  personalWechatId: number
  aochuangWechatAccountId: string
  aochuangMessageId: string
  aochuangFriendId?: string
  msgType?: string
  direction?: string
  content?: string
  messageTime?: string
  syncedAt?: string
}

export interface PersonalWechatDailyStatsVO {
  personalWechatId: number
  statDate: string
  totalFriends?: number
  newFriends?: number
  deletedFriends?: number
  messagesSent?: number
  messagesReceived?: number
  groupCount?: number
}

export interface PersonalWechatSyncMessagesResult {
  syncedCount: number
  createdCount: number
  updatedCount: number
  dailyStatsDays: number
  completed: boolean
}

export interface WeworkEmployeeVO {
  id: number
  weworkAccountId: number
  nickname: string
  weworkUserId: string
  phone?: string
  department?: string
  position?: string
  linkedPersonalWechatId?: number
  linkedPersonalWechatName?: string
  linkedWechatId?: string
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
  linkedWeworkEmployeeId?: number
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
  linkedWeworkEmployeeId?: number
  clearLinkedWeworkEmployee?: boolean
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

export function syncPersonalWechatDevices(data?: { aoCreateAccountId?: number }): Promise<PersonalWechatSyncDevicesResult> {
  return request.post({ url: '/oa/internal/personal-account/sync-devices', data: data || {} })
}

export function bindPersonalWechatDevice(
  id: number,
  data: {
    aochuangWechatAccountId: string
    aochuangAccountRefId: number
    bindStatus?: string
    aochuangNickname?: string
    aochuangAvatar?: string
    aochuangIsAlive?: boolean
  }
): Promise<boolean> {
  return request.post({ url: `/oa/internal/personal-account/${id}/bind-device`, data })
}

export function createAndBindPersonalWechat(data: {
  accountName: string
  wechatId: string
  contactPhone?: string
  aochuangWechatAccountId: string
  aochuangAccountRefId: number
  aochuangNickname?: string
  aochuangAvatar?: string
  aochuangIsAlive?: boolean
}): Promise<number> {
  return request.post({ url: '/oa/internal/personal-account/create-and-bind', data })
}

export function syncPersonalWechatFriends(
  id: number,
  data?: { fullSync?: boolean }
): Promise<PersonalWechatSyncFriendsResult> {
  return request.post({ url: `/oa/internal/personal-account/${id}/sync-friends`, data: data || {} })
}

export function getPersonalWechatFriends(
  id: number,
  params?: { nickname?: string; pageNo?: number; pageSize?: number }
): Promise<PageResult<AochuangFriendVO>> {
  return request.get({ url: `/oa/internal/personal-account/${id}/friends`, params })
}

export function syncPersonalWechatMessages(
  id: number,
  data?: { fullSync?: boolean }
): Promise<PersonalWechatSyncMessagesResult> {
  return request.post({ url: `/oa/internal/personal-account/${id}/sync-messages`, data: data || {} })
}

export function getPersonalWechatMessages(
  id: number,
  params?: { aochuangFriendId?: string; pageNo?: number; pageSize?: number }
): Promise<PageResult<AochuangMessageVO>> {
  return request.get({ url: `/oa/internal/personal-account/${id}/messages`, params })
}

export function getPersonalWechatDailyStats(
  id: number,
  params?: { startDate?: string; endDate?: string }
): Promise<PersonalWechatDailyStatsVO[]> {
  return request.get({ url: `/oa/internal/personal-account/${id}/daily-stats`, params })
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
  linkedPersonalWechatId?: number
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
  linkedPersonalWechatId?: number
  clearLinkedPersonalWechat?: boolean
  status?: string
}): Promise<boolean> {
  return request.put({ url: '/oa/internal/wework/employee/update', data })
}

export function deleteWeworkEmployee(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/internal/wework/employee/delete', params: { id } })
}
