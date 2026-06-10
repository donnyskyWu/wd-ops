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
/** 与 dict_platform_type 字典 value 对齐（API-M2 §1.1） */
export type PlatformType =
  | 'ALL'
  | 'WECHAT_OFFICIAL'
  | 'WECHAT_VIDEO'
  | 'DOUYIN'
  | 'KUAISHOU'
  | 'XIAOHONGSHU'
  | 'WEWORK'
  | 'WECHAT_PERSONAL'

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
  /** 创建时间 */
  createTime: string
}

/**
 * SOP模板查询参数
 * S-R11 B1: 后端 spec API-M2 §1.1 显式定义 pageNum（不是 pageNo）
 */
export interface SopTemplateQuery {
  /** 模板名称 */
  templateName?: string
  /** 内容类型 */
  contentType?: ContentType
  /** 状态 */
  status?: SopStatus
  /** 页码（后端契约名） */
  pageNum: number
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
 * 审核任务（S-R11 B8 修复：字段名对齐后端 SopReviewVO）
 * 后端字段：id / taskId / planName / nodeName / reviewerId / reviewerRole / status / comment / createTime
 */
export interface SopReviewVO {
  /** 审核ID */
  id: number
  /** 任务ID */
  taskId: number
  /** 计划名称 */
  planName: string
  /** 节点名称 */
  nodeName: string
  /** 审核人ID */
  reviewerId: number
  /** 审核人岗位 */
  reviewerRole: string
  /** 审核状态（后端字段名 status） */
  status: ReviewStatus
  /** 审核意见 */
  comment: string
  /** 创建时间（后端字段名 createTime） */
  createTime: string
}

/**
 * 审核任务查询参数
 * S-R11 B7 修复：后端 /pending 不分页；这里类型保留供后续扩展
 */
export interface SopReviewQuery {
  /** 模板ID */
  templateId?: number
  /** 审核状态 */
  reviewStatus?: ReviewStatus
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
