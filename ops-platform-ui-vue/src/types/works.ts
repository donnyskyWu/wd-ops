/**
 * 作品分析 - TypeScript类型定义
 */

// ==================== 作品核心类型 ====================

/**
 * 作品列表项
 */
export interface ContentAnalysisVO {
  /** 作品ID */
  id: number
  /** 作品标题 */
  title: string
  /** 内容类型 */
  contentType: string
  /** 账号名称 */
  accountName: string
  /** 所属IP组 */
  ipGroupName: string
  /** 发布时间 */
  publishTime: string
  /** 阅读/播放量 */
  viewCount: number
  /** 点赞数 */
  likeCount: number
  /** 评论数 */
  commentCount: number
  /** 转发数 */
  shareCount: number
  /** 是否爆款 */
  isViral: boolean
}

/**
 * 作品统计数据
 */
export interface ContentStats {
  /** 发布总数 */
  totalPublished: number
  /** 阅读/播放总量 */
  totalViews: number
  /** 点赞总数 */
  totalLikes: number
  /** 评论总数 */
  totalComments: number
  /** 转发总数 */
  totalShares: number
}

/**
 * 作品趋势数据点
 */
export interface ContentTrendPoint {
  /** 日期 */
  date: string
  /** 阅读量 */
  viewCount: number
  /** 互动数（点赞+评论+转发） */
  interactionCount: number
}

/**
 * 作品查询参数
 */
export interface ContentAnalysisQuery {
  /** IP组ID */
  ipGroupId?: number
  /** 平台类型 */
  platformType?: string
  /** 账号ID */
  accountId?: number
  /** 内容类型 */
  contentType?: string
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 关键词 */
  keyword?: string
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
