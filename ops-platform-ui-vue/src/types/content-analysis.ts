/**
 * 作品分析 - TypeScript类型定义
 * 
 * 基于PRD v9.0和API接口契约文档
 */

// ==================== 枚举类型 ====================

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

// ==================== 作品分析核心类型 ====================

/**
 * 作品分析列表项
 */
export interface ContentAnalysisVO {
  /** 作品ID */
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
 * 作品分析查询参数
 */
export interface ContentAnalysisQuery {
  /** 发布开始日期 */
  startDate?: string
  /** 发布结束日期 */
  endDate?: string
  /** IP组ID */
  ipGroupId?: number
  /** 平台类型 */
  platformType?: string
  /** 账号ID */
  accountId?: number
  /** 内容类型 */
  contentType?: ContentType
  /** 标题关键词（模糊） */
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
  stats?: ContentStatsVO
}

// ==================== 作品统计类型 ====================

/**
 * 作品统计数据
 */
export interface ContentStatsVO {
  /** 发布总数 */
  totalContent: number
  /** 阅读/播放总数 */
  totalRead: number
  /** 点赞总数 */
  totalLike: number
  /** 评论总数 */
  totalComment: number
  /** 转发总数 */
  totalForward: number
  /** 爆款数量 */
  hitCount: number
  /** 平均阅读量 */
  avgReadCount: number
}

// ==================== 作品趋势类型 ====================

/**
 * 作品趋势数据点
 */
export interface ContentTrendVO {
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
 * 作品趋势详情
 */
export interface ContentTrendDetailVO {
  /** 作品ID */
  contentId: number
  /** 标题 */
  title: string
  /** 趋势数据系列 */
  series: ContentTrendVO[]
}

// ==================== 导出类型 ====================

/**
 * 导出请求参数
 */
export interface ContentExportReqVO {
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** IP组ID */
  ipGroupId?: number
  /** 平台类型 */
  platformType?: string
  /** 内容类型 */
  contentType?: ContentType
}
