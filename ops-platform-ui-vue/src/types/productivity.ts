/**
 * 人效盘点 - TypeScript类型定义（S-R9 字段对齐后端 VO）
 *
 * 后端 ProductivityReviewVO 实返字段（实测）：
 *   userId, userName, position, ipGroupId, ipGroupName,
 *   taskTotal, taskCompleted, taskInProgress, taskOverdue, completionRate,
 *   costAmount, revenue, roi, orderCount,
 *   contentOutput, avgRead, avgPlay, hitCount
 */

export type TimeDimension = 'WEEK' | 'MONTH'

export interface ProductivityReviewVO {
  userId: number
  userName: string
  position?: string
  ipGroupId?: number
  ipGroupName?: string

  // 任务 KPI
  taskTotal: number
  taskCompleted: number
  taskInProgress: number
  taskOverdue: number
  completionRate: number

  // 财务 KPI
  costAmount: number
  revenue: number
  roi: number
  orderCount: number

  // 内容 KPI（ContentDO 无 user 关联 → 全 0，详见 ADR-008）
  contentOutput: number
  avgRead: number
  avgPlay: number
  hitCount: number
}

export interface ProductivityReviewQuery {
  startDate?: string
  endDate?: string
  timeDimension?: TimeDimension
  ipGroupId?: number
  userId?: number
  position?: string
  keyword?: string
  page: number
  size: number
}

export interface PageResult<T> {
  total: number
  list: T[]
}

export interface ProductivityReviewDetailVO {
  summary?: ProductivityReviewVO
  taskList?: Array<{
    id: number
    planName: string
    status: string
    startTime?: string
    completeTime?: string
    slaDeadline?: string
  }>
  financeByGroup?: Array<{
    ipGroupId: number
    ipGroupName?: string
    revenue: number
    cost: number
    roi: number
  }>
  contentMetrics?: Record<string, number>
  trend?: Array<{
    date: string
    revenue: number
    orderCount: number
  }>
}
