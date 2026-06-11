/**
 * 作品分析 - TypeScript类型定义
 * P-GATE-UNMOCK-R S-R2-D：字段名与后端 VO 对齐
 * 后端 ContentAnalysisVO 实测字段：id, accountId, accountName, title, platformType, contentType, publishTime,
 *   readCount, likeCount, commentCount, forwardCount, isHit, dataSource
 * 后端 ContentStatsVO 实测字段：totalCount, hitCount, totalRead, avgRead
 * 后端 trend 端点返回 List<Map>：{date, count, readCount, likeCount, commentCount, forwardCount}
 */

export interface ContentAnalysisVO {
  id: number
  accountId: number
  accountName: string
  title: string
  platformType: string
  contentType: string
  ipGroupName?: string
  publishTime: string
  readCount: number
  likeCount: number
  commentCount: number
  forwardCount: number
  isHit: boolean
  dataSource?: string
  // 兼容旧字段（前端展示层兜底）
  viewCount?: number
  shareCount?: number
  isViral?: boolean
}

export interface ContentStats {
  totalCount: number
  hitCount: number
  totalRead: number
  avgRead: number
  // S-R8 B1: 5 KPI 卡全量聚合字段（来自后端 ContentStatsVO 扩展）
  totalPublished: number
  totalViews: number
  totalLikes: number
  totalComments: number
  totalShares: number
}

export interface ContentTrendPoint {
  date: string
  // S-R8 B3: 字段名与后端实返一致（readCount/likeCount/commentCount/forwardCount/count）
  count: number
  readCount: number
  likeCount: number
  commentCount: number
  forwardCount: number
}

export interface ContentAnalysisQuery {
  ipGroupId?: number
  platformType?: string
  accountId?: number
  contentType?: string
  startDate?: string
  endDate?: string
  keyword?: string
  // S-R8 B6: 与后端 controller 契约对齐
  page: number
  size: number
}

export interface PageResult<T> {
  total: number
  list: T[]
}
