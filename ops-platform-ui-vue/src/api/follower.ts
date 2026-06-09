/**
 * 粉丝分析 - API接口封装
 */

import { request } from '@/utils/request'
import type {
  FollowerStats,
  FollowerTrendPoint,
  FollowerDetailVO,
  FollowerQuery,
  PageResult,
} from '@/types/follower'

// ==================== 粉丝统计 ====================

/**
 * 获取粉丝统计数据
 */
export function getFollowerStats(params: Pick<FollowerQuery, 'ipGroupId' | 'platformType' | 'accountId' | 'startDate' | 'endDate'>): Promise<FollowerStats> {
  return request.get<FollowerStats>({ url: '/oa/follower/stats', params })
}

/**
 * 获取粉丝趋势数据
 */
export function getFollowerTrend(params: Pick<FollowerQuery, 'ipGroupId' | 'platformType' | 'accountId' | 'startDate' | 'endDate' | 'dimension'>): Promise<FollowerTrendPoint[]> {
  return request.get<FollowerTrendPoint[]>({ url: '/oa/follower/trend', params })
}

/**
 * 获取粉丝明细列表（分页）
 */
export function getFollowerList(params: FollowerQuery): Promise<PageResult<FollowerDetailVO>> {
  return request.get<PageResult<FollowerDetailVO>>({ url: '/oa/follower/list', params })
}
