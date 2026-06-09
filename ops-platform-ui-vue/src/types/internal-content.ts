/**
 * 内部内容分析 - TypeScript类型定义
 * 
 * 基于PRD v9.0和页面规格文档
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

/**
 * 内容类型枚举
 */
export type ContentType = 'ARTICLE' | 'VIDEO' | 'LIVE'

/**
 * 排序字段枚举
 */
export type ContentOrderBy = 'publish_time' | 'read_count' | 'like_count'

/**
 * 排序方向枚举
 */
export type OrderDirection = 'asc' | 'desc'

// ==================== 内部内容核心类型 ====================

/**
 * 内部内容列表项
 */
export interface InternalContentVO {
  /** 内容ID */
  contentId: number
  /** 标题 */
  title: string
  /** 内容类型 */
  contentType: ContentType
  /** 账号名称 */
  accountName: string
  /** IP组名称 */
  ipGroupName: string
  /** 发布时间 */
  publishTime: string
  /** 阅读/播放量 */
  readCount: number
  /** 点赞数 */
  likeCount: number
  /** 评论数 */
  commentCount: number
  /** 转发数 */
  forwardCount: number
  /** 是否爆款 */
  isHit: boolean
}

/**
 * 内部内容查询参数
 */
export interface InternalContentQuery {
  /** 平台类型 */
  platformType: PlatformType
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** IP组ID */
  ipGroupId?: number
  /** 账号ID */
  accountId?: number
  /** 内容类型 */
  contentType?: ContentType
  /** 是否爆款 */
  isHit?: boolean
  /** 标题关键词 */
  keyword?: string
  /** 排序字段 */
  orderBy?: ContentOrderBy
  /** 排序方向 */
  orderDir?: OrderDirection
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

// ==================== 内容趋势类型 ====================

/**
 * 内容趋势数据点
 */
export interface ContentTrendPoint {
  /** 日期 */
  date: string
  /** 阅读/播放量 */
  readCount: number
  /** 点赞数 */
  likeCount: number
  /** 评论数 */
  commentCount: number
  /** 转发数 */
  forwardCount: number
}

/**
 * 内容趋势详情
 */
export interface ContentTrendDetailVO {
  /** 内容ID */
  contentId: number
  /** 标题 */
  title: string
  /** 趋势数据系列 */
  series: ContentTrendPoint[]
}
