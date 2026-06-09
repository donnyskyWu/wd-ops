/**
 * 粉丝分析 - TypeScript类型定义
 */

// ==================== 枚举类型 ====================

/**
 * 时间维度枚举
 */
export enum TimeDimension {
  DAY = 'day',
  WEEK = 'week',
  MONTH = 'month',
}

// ==================== 粉丝核心类型 ====================

/**
 * 粉丝统计卡片
 */
export interface FollowerStats {
  /** 粉丝总数 */
  totalFollowers: number
  /** 新增粉丝 */
  newFollowers: number
  /** 取消关注 */
  unfollowers: number
  /** 净增粉丝 */
  netFollowers: number
  /** 增长率 */
  growthRate: number
}

/**
 * 粉丝趋势数据点
 */
export interface FollowerTrendPoint {
  /** 日期 */
  date: string
  /** 粉丝总数 */
  totalFollowers: number
  /** 新增粉丝 */
  newFollowers: number
  /** 取消关注 */
  unfollowers: number
  /** 净增粉丝 */
  netFollowers: number
}

/**
 * 粉丝明细数据
 */
export interface FollowerDetailVO {
  /** ID */
  id: number
  /** 时间 */
  date: string
  /** 账号名称 */
  accountName: string
  /** 所属IP组 */
  ipGroupName: string
  /** 粉丝总数 */
  totalFollowers: number
  /** 新增粉丝 */
  newFollowers: number
  /** 取消关注 */
  unfollowers: number
  /** 净增粉丝 */
  netFollowers: number
  /** 增长率 */
  growthRate: number
}

/**
 * 粉丝查询参数
 */
export interface FollowerQuery {
  /** IP组ID */
  ipGroupId?: number
  /** 平台类型 */
  platformType?: string
  /** 账号ID */
  accountId?: number
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** 时间维度 */
  dimension: TimeDimension
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
}

/**
 * 分页响应
 */
export interface PageResult<T> {
  total: number
  list: T[]
}
