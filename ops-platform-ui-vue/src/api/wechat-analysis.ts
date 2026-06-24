/**
 * M1 微信数据分析 — 企微 / 个微 Tab（M10-WECOM-S-04 · M10-AO-S-06 · ADR-048 / ADR-045）
 */
import { request } from '@/utils/request'

export interface WeworkAnalysisListItem {
  accountId: number
  accountName: string
  totalFriends: number
  todayFriendInteractions: number
  todayMessagesSent: number
  statDate?: string
}

export interface WeworkDailyStatsItem {
  statDate: string
  totalFriends: number
  todayFriendInteractions: number
  todayMessagesSent: number
  syncedAt?: string
}

export interface WeworkAnalysisDetail {
  accountId: number
  accountName: string
  corpId?: string
  connStatus?: string
  dailyStats: WeworkDailyStatsItem[]
}

export interface PersonalAnalysisListItem {
  accountId: number
  accountName: string
  totalFriends: number
  newFriends: number
  messagesSent: number
  statDate?: string
}

export interface PersonalDailyStatsItem {
  statDate: string
  totalFriends: number
  newFriends: number
  deletedFriends: number
  messagesSent: number
  messagesReceived: number
  groupCount: number
}

export interface PersonalAnalysisDetail {
  accountId: number
  accountName: string
  wechatId?: string
  collectStatus?: string
  aochuangBindStatus?: string
  dailyStats: PersonalDailyStatsItem[]
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export interface WechatAnalysisListQuery {
  accountId?: number
  accountName?: string
  statDate?: string
  page?: number
  size?: number
}

export function getWeworkAnalysisList(params: WechatAnalysisListQuery): Promise<PageResult<WeworkAnalysisListItem>> {
  return request.get({ url: '/oa/wechat-analysis/wework/list', params })
}

export function getWeworkAnalysisDetail(params: {
  accountId: number
  startDate?: string
  endDate?: string
}): Promise<WeworkAnalysisDetail> {
  return request.get({ url: '/oa/wechat-analysis/wework/detail', params })
}

export function getPersonalAnalysisList(params: WechatAnalysisListQuery): Promise<PageResult<PersonalAnalysisListItem>> {
  return request.get({ url: '/oa/wechat-analysis/personal/list', params })
}

export function getPersonalAnalysisDetail(params: {
  accountId: number
  startDate?: string
  endDate?: string
}): Promise<PersonalAnalysisDetail> {
  return request.get({ url: '/oa/wechat-analysis/personal/detail', params })
}
