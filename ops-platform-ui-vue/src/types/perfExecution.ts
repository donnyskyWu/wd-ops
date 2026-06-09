/**
 * 考核执行 - TypeScript类型定义
 */

// ==================== 考核执行核心类型 ====================

/**
 * 考核状态枚举
 */
export enum PerfRecordStatus {
  /** 草稿 */
  DRAFT = 'draft',
  /** 计算中 */
  CALCULATING = 'calculating',
  /** 待确认 */
  CALCULATED = 'calculated',
  /** 已确认 */
  CONFIRMED = 'confirmed',
}

/**
 * 周期类型枚举
 */
export enum CycleType {
  /** 月度 */
  MONTH = 'month',
  /** 周度 */
  WEEK = 'week',
  /** 自定义 */
  CUSTOM = 'custom',
}

/**
 * 考核记录列表项
 */
export interface PerfRecordListItem {
  /** 记录ID */
  id: number
  /** 被考核人姓名 */
  evaluateeName: string
  /** 岗位 */
  position: string
  /** 考核周期显示 */
  cycleDisplay: string
  /** 总分 */
  totalScore?: number
  /** 等级 */
  grade?: string
  /** 状态 */
  status: PerfRecordStatus
  /** 考核人姓名 */
  evaluatorName: string
  /** 创建时间 */
  createdAt: string
}

/**
 * 指标得分记录
 */
export interface PerfItemRecord {
  /** 指标项ID */
  itemId: number
  /** 指标名称 */
  metricName: string
  /** 权重 */
  weight: number
  /** 实际值 */
  actualValue: number
  /** 得分 */
  score: number
  /** 加权得分 */
  weightedScore: number
  /** 等级 */
  grade: string
  /** 是否人工调整 */
  isManualAdjust: boolean
  /** 调整备注 */
  adjustRemark?: string
}

/**
 * 考核记录详情
 */
export interface PerfRecordDetail {
  /** 记录ID */
  id: number
  /** 被考核人ID */
  evaluateeId: number
  /** 被考核人姓名 */
  evaluateeName: string
  /** 岗位 */
  position: string
  /** 模板ID */
  templateId: number
  /** 模板名称 */
  templateName: string
  /** 周期类型 */
  cycleType: CycleType
  /** 考核周期显示 */
  cycleDisplay: string
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** 总分 */
  totalScore?: number
  /** 等级 */
  grade?: string
  /** 状态 */
  status: PerfRecordStatus
  /** 考核人姓名 */
  evaluatorName: string
  /** 指标明细 */
  items: PerfItemRecord[]
  /** 创建时间 */
  createdAt: string
}

/**
 * 创建考核请求
 */
export interface CreatePerfRecordRequest {
  /** 被考核人ID */
  evaluateeId: number
  /** 模板ID */
  templateId: number
  /** 周期类型 */
  cycleType: CycleType
  /** 考核月份（月度时） */
  month?: string
  /** 开始日期（周度/自定义时） */
  startDate?: string
  /** 结束日期（周度/自定义时） */
  endDate?: string
}

/**
 * 考核查询参数
 */
export interface PerfRecordQuery {
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
  /** 被考核人ID */
  evaluateeId?: number
  /** 周期类型 */
  cycleType?: CycleType
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 状态 */
  status?: PerfRecordStatus
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  list: T[]
  total: number
}
