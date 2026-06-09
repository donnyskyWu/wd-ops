/**
 * 人效盘点 - TypeScript类型定义
 * 
 * 基于PRD v9.0和页面规格文档
 */

// ==================== 枚举类型 ====================

/**
 * 时间维度枚举
 */
export type TimeDimension = 'WEEK' | 'MONTH'

// ==================== 人效核心类型 ====================

/**
 * 人效盘点列表项
 */
export interface ProductivityVO {
  /** 用户ID */
  userId: number
  /** 人员姓名 */
  userName: string
  /** IP组名称 */
  ipGroupName: string
  /** 岗位 */
  position: string
  /** 完成任务数 */
  completedCount: number
  /** 进行中任务数 */
  inProgressCount: number
  /** 超时任务数 */
  overdueCount: number
  /** 完成率 */
  completionRate: string
  /** 总成本 */
  totalCost: number
  /** 总收入 */
  totalRevenue: number
  /** ROI */
  roi: string
  /** 发布内容数 */
  contentPublished: number
  /** 平均播放量 */
  avgPlayCount: number
  /** 爆款数量 */
  hitContentCount: number
}

/**
 * 人效盘点查询参数
 */
export interface ProductivityQuery {
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** IP组ID */
  ipGroupId?: number
  /** 用户ID */
  userId?: number
  /** 时间维度 */
  timeDimension?: TimeDimension
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

// ==================== 人效详情类型 ====================

/**
 * 任务明细项
 */
export interface TaskDetailVO {
  /** 任务名称 */
  taskName: string
  /** 状态 */
  status: string
  /** 完成时间 */
  completeTime: string
}

/**
 * 任务维度
 */
export interface TaskDimensionVO {
  /** 完成任务数 */
  completedCount: number
  /** 进行中任务数 */
  inProgressCount: number
  /** 超时任务数 */
  overdueCount: number
  /** 完成率 */
  completionRate: string
  /** 任务明细列表 */
  tasks: TaskDetailVO[]
}

/**
 * 账号成本明细
 */
export interface AccountCostBreakdown {
  /** 账号名称 */
  accountName: string
  /** 成本 */
  cost: number
  /** 收入 */
  revenue: number
}

/**
 * 财务维度
 */
export interface FinanceDimensionVO {
  /** 总成本 */
  totalCost: number
  /** 总收入 */
  totalRevenue: number
  /** ROI */
  roi: string
  /** 账号成本明细 */
  accountBreakdown: AccountCostBreakdown[]
}

/**
 * 内容趋势数据点
 */
export interface ContentTrendPoint {
  /** 时间周期 */
  period: string
  /** 发布数量 */
  publishCount: number
  /** 平均播放 */
  avgPlay: number
}

/**
 * 内容维度
 */
export interface ContentDimensionVO {
  /** 发布内容数 */
  contentPublished: number
  /** 平均播放量 */
  avgPlayCount: number
  /** 爆款数量 */
  hitContentCount: number
  /** 内容趋势 */
  trend: ContentTrendPoint[]
}

/**
 * 趋势维度数据点
 */
export interface TrendDimensionPoint {
  /** 时间周期 */
  period: string
  /** 完成率 */
  completionRate: number
  /** ROI */
  roi: number
  /** 内容数量 */
  contentCount: number
}

/**
 * 趋势维度
 */
export interface TrendDimensionVO {
  /** 趋势数据系列 */
  series: TrendDimensionPoint[]
}

/**
 * 人效详情
 */
export interface ProductivityDetailVO {
  /** 用户ID */
  userId: number
  /** 用户姓名 */
  userName: string
  /** 任务维度 */
  taskDimension: TaskDimensionVO
  /** 财务维度 */
  financeDimension: FinanceDimensionVO
  /** 内容维度 */
  contentDimension: ContentDimensionVO
  /** 趋势维度 */
  trendDimension: TrendDimensionVO
}
