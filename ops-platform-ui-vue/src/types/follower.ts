/**
 * 粉丝分析 - TypeScript类型定义
 * P-GATE-UNMOCK-R S-R2-C：字段名与后端 VO 对齐
 * 后端 VO 实测字段：statDate, accountId, accountName, ipGroupName, followerCount, newFollower, unfollowCount, netGrowth, growthRate
 * 后端端点：/admin-api/oa/follower-analysis/list  (spec §4.2)
 */

export enum TimeDimension {
  DAY = 'day',
  WEEK = 'week',
  MONTH = 'month',
}

export interface FollowerStats {
  totalFollowers: number
  newFollowers: number
  unfollowers: number
  netFollowers: number
  growthRate: number
}

export interface FollowerTrendPoint {
  date: string
  totalFollowers: number
  newFollowers: number
  unfollowers: number
  netFollowers: number
}

export interface FollowerDetailVO {
  id?: number
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

export interface FollowerQuery {
  ipGroupId?: number
  platformType?: string
  accountId?: number
  startDate: string
  endDate: string
  dimension?: TimeDimension | string
  page?: number
  size?: number
}

export interface PageResult<T> {
  total: number
  list: T[]
}
