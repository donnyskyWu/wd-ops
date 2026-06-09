/**
 * SOP管理 - TypeScript类型定义
 * 
 * 基于PRD v9.0和页面规格文档
 */

// ==================== 枚举类型 ====================

/**
 * SOP模板状态
 */
export type SopStatus = 0 | 1 // 0=停用, 1=启用

/**
 * 审核状态
 */
export type ReviewStatus = 'PENDING' | 'REVIEWING' | 'APPROVED' | 'REJECTED'

/**
 * 内容类型
 */
export type ContentType = 'ALL' | 'ARTICLE' | 'VIDEO' | 'LIVE'

/**
 * 平台类型
 */
export type PlatformType = 'ALL' | 'WECHAT_MP' | 'VIDEO_ACCOUNT' | 'DOUYIN' | 'KUAISHOU' | 'XIAOHONGSHU' | 'SERVICE_ACCOUNT' | 'WEWORK'

// ==================== SOP模板类型 ====================

/**
 * SOP模板
 */
export interface SopTemplateVO {
  /** 模板ID */
  id: number
  /** 模板名称 */
  templateName: string
  /** 适用内容类型 */
  contentType: ContentType
  /** 适用平台 */
  platformType: PlatformType
  /** 节点数 */
  nodeCount: number
  /** 状态 */
  status: SopStatus
  /** 状态文本 */
  statusText: string
  /** 创建时间 */
  createTime: string
}

/**
 * SOP模板查询参数
 */
export interface SopTemplateQuery {
  /** 模板名称 */
  templateName?: string
  /** 内容类型 */
  contentType?: ContentType
  /** 状态 */
  status?: SopStatus
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
}

/**
 * 创建SOP模板请求
 */
export interface CreateSopTemplateReq {
  /** 模板名称 */
  templateName: string
  /** 内容类型 */
  contentType?: ContentType
  /** 适用平台 */
  platformType?: PlatformType
  /** 模板描述 */
  description?: string
}

/**
 * 分页响应
 */
export interface PageResult<T> {
  total: number
  list: T[]
}

// ==================== SOP节点类型 ====================

/**
 * SOP节点
 */
export interface SopNodeVO {
  /** 节点ID */
  id: number
  /** 模板ID */
  templateId: number
  /** 节点名称 */
  nodeName: string
  /** 节点顺序 */
  nodeOrder: number
  /** 节点描述 */
  nodeDescription: string
  /** 执行岗位 */
  executorRole: string
  /** 是否需要审核 */
  needReview: 0 | 1
  /** 审核人岗位 */
  reviewerRole?: string
  /** 前置节点ID列表 */
  predecessors: number[]
  /** 并行组 */
  parallelGroup?: string
  /** SLA时限（小时） */
  slaHours?: number
}

/**
 * DAG校验请求
 */
export interface DagValidateRequest {
  /** 模板ID */
  templateId: number
  /** 节点列表 */
  nodes: Array<{
    id: number
    predecessors: number[]
  }>
}

/**
 * DAG校验响应
 */
export interface DagValidateResponse {
  /** 是否合法 */
  valid: boolean
  /** 错误信息 */
  message?: string
}

// ==================== 审核任务类型 ====================

/**
 * 审核任务
 */
export interface SopReviewVO {
  /** 审核ID */
  reviewId: number
  /** 任务名称 */
  taskName: string
  /** 节点名称 */
  nodeName: string
  /** 执行人 */
  executorName: string
  /** 审核状态 */
  reviewStatus: ReviewStatus
  /** 提交时间 */
  submitTime: string
}

/**
 * 审核任务查询参数
 */
export interface SopReviewQuery {
  /** 模板ID */
  templateId?: number
  /** 审核状态 */
  reviewStatus?: ReviewStatus
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
}

/**
 * 审核操作请求
 */
export interface ReviewActionReq {
  /** 审核ID */
  reviewId: number
  /** 审核意见 */
  comment: string
}
