/**
 * 账号分析 - API接口封装
 */

import { request } from '@/utils/request'
import type {
  AccountVO,
  AccountQuery,
  PageResult,
  FollowerTrendPoint,
  ContentTrendPoint,
} from '@/types/account'

// ==================== 账号列表 ====================

/**
 * 获取账号列表（分页）
 */
export function getAccountList(params: AccountQuery): Promise<PageResult<AccountVO>> {
  return request.get({ url: '/oa/account/list', params }) as unknown as Promise<PageResult<AccountVO>>
}

/**
 * 平台账号列表（与 getAccountList 同源，独立命名便于 M4 详情页引用）
 */
export function getPlatformAccountList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/account/list', params })
}

/**
 * 平台账号详情
 */
export function getPlatformAccountDetail(id: number) {
  return request.get({ url: '/oa/account/get', params: { id } })
}

/**
 * 平台账号更新
 */
export function updatePlatformAccount(data: Record<string, unknown>) {
  return request.put({ url: '/oa/account/update', data })
}

// ==================== 趋势数据 ====================

/**
 * 获取粉丝趋势数据
 */
export function getFollowerTrend(accountId: number): Promise<FollowerTrendPoint[]> {
  return request.get({ url: `/oa/account/${accountId}/follower-trend` }) as unknown as Promise<FollowerTrendPoint[]>
}

/**
 * 获取内容趋势数据
 */
export function getContentTrend(accountId: number): Promise<ContentTrendPoint[]> {
  return request.get({ url: `/oa/account/${accountId}/content-trend` }) as unknown as Promise<ContentTrendPoint[]>
}
