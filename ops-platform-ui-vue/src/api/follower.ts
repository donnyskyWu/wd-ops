/**
 * 粉丝分析 - API (P-GATE-UNMOCK-R S-R2-C, S-R6 重构)
 * 路径对齐后端：/admin-api/oa/follower-analysis/* (spec §4.4)
 */
import { request } from '@/utils/request'
import type { FollowerQuery, FollowerStats } from '@/types/follower'

export interface FollowerRowVO {
  statDate: string
  accountId: number
  accountName: string
  ipGroupName: string
  followerCount: number
  newFollower: number
  unfollowCount: number
  netGrowth: number
  growthRate: number
}

export interface FollowerPageResult {
  total: number
  list: FollowerRowVO[]
}

export function getFollowerList(params: FollowerQuery): Promise<FollowerPageResult> {
  return request.get({ url: '/oa/follower-analysis/list', params })
}

export function getFollowerTrend(params: FollowerQuery): Promise<FollowerTrendPointVO[]> {
  return request.get({ url: '/oa/follower-analysis/trend', params })
}

export interface FollowerTrendPointVO {
  timePeriod: string
  accountName?: string
  ipGroupName?: string
  followerCount?: number
  newFollower?: number
  unfollowCount?: number
  netGrowth?: number
  growthRate?: number
}

/**
 * S-R6-B1+B4：聚合统计（替代前端 list.reduce）
 */
export function getFollowerStats(params: FollowerQuery): Promise<FollowerStats> {
  return request.get({ url: '/oa/follower-analysis/stats', params })
}

/**
 * S-R6-B3：导出。直接返回 blob 让浏览器下载（后端响应 Content-Disposition: attachment）。
 * 文件名 follower_analysis_{ts}.csv。
 */
export function exportFollowerAnalysis(params: FollowerQuery): Promise<Blob> {
  return request.get({
    url: '/oa/follower-analysis/export',
    params,
    responseType: 'blob',
  })
}
