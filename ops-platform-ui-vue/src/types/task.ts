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
  REVIEW_APPROVED = 'APPROVED',
  /** 审核驳回 */
  REVIEW_REJECTED = 'REJECTED',
  /** 已完成（无需审核） */
  DONE = 'DONE',
  /** 已完成（旧/需审核中间态） */
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
  /** 执行岗位（编码） */
  executorRole: string
  /** 执行岗位名称 */
  executorRoleText?: string
  /** 赛事 ID */
  competitionId?: string
  /** 赛事名称 */
  competitionName?: string
  /** 任务状态 */
  status: TaskStatus
  /** 状态文本（可选，前端可本地映射） */
  statusText?: string
  /** SLA截止时间（ISO 字符串） */
  slaDeadline?: string
  /** 计划开始时间（来自计划创建步骤） */
  scheduledStart?: string
  /** 计划结束时间（来自计划创建步骤） */
  scheduledEnd?: string
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

export interface TaskLinkedContentVO {
  id: number
  title: string
  status: string
}

export interface TaskAttachmentVO {
  name: string
  url: string
}

export interface TaskExecuteVO {
  id: number
  planName: string
  nodeName: string
  nodeType: string
  ipGroupId?: number
  ipGroupName?: string
  competitionId?: string
  competitionName?: string
  slaDeadline?: string
  status: string
  needReview?: number
  executionInstruction?: string
  attachments?: TaskAttachmentVO[]
  deliverableAttachments?: TaskAttachmentVO[]
  deliverables?: string
  linkedContent?: TaskLinkedContentVO | null
}
