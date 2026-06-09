/**
 * 内容管理 - TypeScript类型定义
 * 
 * 基于PRD v9.0和页面规格文档
 */

// ==================== 枚举类型 ====================

/**
 * 内容状态枚举
 */
export enum ContentStatus {
  /** 草稿 */
  DRAFT = 'draft',
  /** 待初审 */
  PENDING_REVIEW = 'pending_review',
  /** 审核中 */
  REVIEWING = 'reviewing',
  /** 审核通过 */
  APPROVED = 'approved',
  /** 已驳回 */
  REJECTED = 'rejected',
  /** 发布中 */
  PUBLISHING = 'publishing',
  /** 已发布 */
  PUBLISHED = 'published',
  /** 发布失败 */
  PUBLISH_FAILED = 'publish_failed',
}

/**
 * 审核阶段枚举
 */
export enum ReviewStage {
  /** 初审 */
  INITIAL = 'initial',
  /** 复审 */
  SECOND = 'second',
  /** 终审 */
  FINAL = 'final',
}

/**
 * 内容类型枚举
 */
export type ContentType = 'ARTICLE' | 'VIDEO' | 'LIVE' | 'OTHER'

// ==================== 内容核心类型 ====================

/**
 * 内容列表项
 */
export interface ContentListItem {
  /** 内容ID */
  id: number
  /** 内容标题 */
  title: string
  /** 内容类型 */
  contentType: ContentType
  /** 平台类型 */
  platformType: string
  /** 发布账号名称 */
  accountName: string
  /** 创作者姓名 */
  creatorName: string
  /** 是否AI生成 */
  aiGenerated: boolean
  /** 内容状态 */
  status: ContentStatus
  /** 当前审核阶段 */
  reviewStage: ReviewStage | null
  /** 创建时间 */
  createdAt: string
  /** 发布时间 */
  publishedAt?: string
}

/**
 * 内容查询参数
 */
export interface ContentQuery {
  /** 标题关键字 */
  title?: string
  /** 内容类型 */
  contentType?: ContentType
  /** 平台类型 */
  platformType?: string
  /** 内容状态 */
  status?: ContentStatus
  /** 创建开始时间 */
  createStart?: string
  /** 创建结束时间 */
  createEnd?: string
  /** 创作者ID */
  creatorUserId?: number
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

// ==================== 内容详情类型 ====================

/**
 * 内容详情
 */
export interface ContentDetailVO {
  /** 内容ID */
  id: number
  /** 内容标题 */
  title: string
  /** 内容正文 */
  content: string
  /** 内容类型 */
  contentType: ContentType
  /** 平台类型 */
  platformType: string
  /** 发布账号ID */
  accountId: number
  /** 发布账号名称 */
  accountName: string
  /** 创作者ID */
  creatorId: number
  /** 创作者姓名 */
  creatorName: string
  /** 是否AI生成 */
  aiGenerated: boolean
  /** 内容状态 */
  status: ContentStatus
  /** 当前审核阶段 */
  reviewStage: ReviewStage | null
  /** 封面图URL */
  coverUrl?: string
  /** 创建时间 */
  createdAt: string
  /** 更新时间 */
  updatedAt: string
  /** 发布时间 */
  publishedAt?: string
}

/**
 * 创建/编辑内容请求
 */
export interface CreateContentReq {
  /** 内容标题 */
  title: string
  /** 内容正文 */
  content: string
  /** 内容类型 */
  contentType: ContentType
  /** 平台类型 */
  platformType: string
  /** 发布账号ID */
  accountId: number
  /** 封面图URL */
  coverUrl?: string
  /** 是否AI生成 */
  aiGenerated?: boolean
}

// ==================== 审核相关类型 ====================

/**
 * 审核记录
 */
export interface ReviewRecordVO {
  /** 审核记录ID */
  id: number
  /** 内容ID */
  contentId: number
  /** 审核阶段 */
  reviewStage: ReviewStage
  /** 审核人姓名 */
  reviewerName: string
  /** 审核结果 */
  reviewResult: 'approved' | 'rejected'
  /** 审核意见 */
  reviewComment: string
  /** 审核时间 */
  reviewedAt: string
}

/**
 * 提交审核请求
 */
export interface SubmitReviewReq {
  /** 内容ID */
  contentId: number
}

/**
 * 审核操作请求
 */
export interface ReviewActionReq {
  /** 内容ID */
  contentId: number
  /** 审核阶段 */
  reviewStage: ReviewStage
  /** 审核结果 */
  reviewResult: 'approved' | 'rejected'
  /** 审核意见 */
  reviewComment: string
}
