/**
 * 任务管理 - TypeScript类型定义
 * 
 * 基于PRD v9.0和页面规格文档
 */

// ==================== 枚举类型 ====================

/**
 * 任务状态枚举
 */
export enum TaskStatus {
  /** 待执行 */
  PENDING = 'PENDING',
  /** 执行中 */
  IN_PROGRESS = 'IN_PROGRESS',
  /** 待审核 */
  PENDING_REVIEW = 'PENDING_REVIEW',
  /** 审核通过 */
  REVIEW_APPROVED = 'REVIEW_APPROVED',
  /** 审核驳回 */
  REVIEW_REJECTED = 'REVIEW_REJECTED',
  /** 已完成 */
  COMPLETED = 'COMPLETED',
}

// ==================== 任务核心类型 ====================

/**
 * 任务列表项
 */
export interface TaskVO {
  /** 任务ID */
  id: number
  /** 任务名称 */
  planName: string
  /** 节点名称 */
  nodeName: string
  /** 执行人 */
  assigneeName: string
  /** 执行岗位 */
  executorRole: string
  /** 任务状态 */
  status: TaskStatus
  /** 状态文本 */
  statusText: string
  /** SLA截止时间 */
  slaDeadline: string
  /** 是否超时 */
  isOverdue: boolean
}

/**
 * 任务查询参数
 */
export interface TaskQuery {
  /** 模板ID */
  templateId?: number
  /** 任务状态 */
  status?: TaskStatus
  /** 执行人ID */
  assigneeId?: number
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
