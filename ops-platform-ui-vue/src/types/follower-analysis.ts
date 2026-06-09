/**
 * 粉丝分析 - TypeScript类型定义
 * 
 * 基于PRD v9.0和API接口契约文档
 */

// ==================== 枚举类型 ====================

/**
 * 平台类型枚举
 */
export type PlatformType = 
  | 'WECHAT_MP'           // 公众号
  | 'VIDEO_ACCOUNT'       // 视频号
  | 'DOUYIN'              // 抖音
  | 'KUAISHOU'            // 快手
  | 'XIAOHONGSHU'         // 小红书
  | 'SERVICE_ACCOUNT'     // 服务号
  | 'WEWORK'              // 企业微信
  | 'ALL'                 // 全部

/**
 * 时间维度枚举
 */
export type TimeDimension = 'DAY' | 'WEEK' | 'MONTH'

// ==================== 粉丝分析核心类型 ====================

/**
 * 粉丝分析列表项
 */
export interface FollowerAnalysisVO {
  /** 时间周期 */
  timePeriod: string
  /** 账号名称 */
  accountName: string
  /** IP组名称 */
  ipGroupName: string
  /** 粉丝总数 */
  followerCount: number
  /** 新增粉丝 */
  newFollower: number
  /** 取消关注 */
  unfollowCount: number
  /** 净增粉丝 */
  netGrowth: number
  /** 增长率 */
  growthRate: string
}

/**
 * 粉丝分析查询参数
 */
export interface FollowerAnalysisQuery {
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** IP组ID */
  ipGroupId?: number
  /** 平台类型 */
  platformType?: PlatformType
  /** 账号ID */
  accountId?: number
  /** 时间维度 */
  timeDimension?: TimeDimension
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
  stats?: FollowerStatsVO
}

// ==================== 粉丝统计类型 ====================

/**
 * 粉丝统计数据
 */
export interface FollowerStatsVO {
  /** 粉丝总数 */
  totalFollower: number
  /** 新增粉丝 */
  newFollower: number
  /** 取消关注 */
  unfollowCount: number
  /** 净增粉丝 */
  netGrowth: number
}

// ==================== 粉丝趋势类型 ====================

/**
 * 粉丝趋势数据点
 */
export interface FollowerTrendPoint {
  /** 日期 */
  date: string
  /** 粉丝总数 */
  followerCount: number
  /** 新增粉丝 */
  newFollower?: number
  /** 净增粉丝 */
  netGrowth: number
}

/**
 * 粉丝趋势（单账号）
 */
export interface FollowerTrendVO {
  /** 账号ID */
  accountId: number
  /** 账号名称 */
  accountName: string
  /** 趋势数据系列 */
  series: FollowerTrendPoint[]
}

/**
 * 粉丝趋势查询参数
 */
export interface FollowerTrendQuery {
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** 账号ID列表（逗号分隔，最多10个） */
  accountIds?: string
  /** 时间维度 */
  timeDimension?: TimeDimension
}

// ==================== 导出类型 ====================

/**
 * 导出请求参数
 */
export interface FollowerExportReqVO {
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** IP组ID */
  ipGroupId?: number
  /** 平台类型 */
  platformType?: PlatformType
  /** 时间维度 */
  timeDimension?: TimeDimension
}
