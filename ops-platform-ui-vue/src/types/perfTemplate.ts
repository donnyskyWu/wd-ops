/**
 * 考核模板 - TypeScript类型定义
 */

// ==================== 考核模板核心类型 ====================

/**
 * 计算规则枚举
 */
export enum CalcRule {
  /** 自动计算 */
  AUTO = 'auto',
  /** 人工评分 */
  MANUAL = 'manual',
}

/**
 * 等级枚举
 */
export enum Grade {
  S = 'S',
  A = 'A',
  B = 'B',
  C = 'C',
  D = 'D',
}

/**
 * 评分区间
 */
export interface ScoreRange {
  /** 最低值 */
  min: number
  /** 最高值 */
  max: number
  /** 得分 */
  score: number
  /** 等级 */
  grade: Grade
}

/**
 * 评分标准
 */
export interface ScoreStandard {
  /** 评分区间列表 */
  ranges: ScoreRange[]
}

/**
 * 模板指标项
 */
export interface PerfTemplateItem {
  /** 指标项ID */
  id?: number
  /** 指标ID */
  metricId: number
  /** 指标名称 */
  metricName: string
  /** 指标编码 */
  metricCode: string
  /** 权重（百分比） */
  weight: number
  /** 计算规则 */
  calcRule: CalcRule
  /** 单位 */
  unit?: string
  /** 评分标准 */
  scoreStandard: ScoreStandard
}

/**
 * 考核模板列表项
 */
export interface PerfTemplateListItem {
  /** 模板ID */
  id: number
  /** 岗位名称 */
  position: string
  /** 模板名称 */
  templateName: string
  /** 指标数量 */
  itemCount: number
  /** 是否生效 */
  isActive: boolean
  /** 创建时间 */
  createdAt: string
}

/**
 * 考核模板详情
 */
export interface PerfTemplateDetail {
  /** 模板ID */
  id?: number
  /** 模板名称 */
  templateName: string
  /** 岗位 */
  position: string
  /** 是否立即生效 */
  isActive: boolean
  /** 指标列表 */
  items: PerfTemplateItem[]
}

/**
 * 模板查询参数
 */
export interface PerfTemplateQuery {
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
  /** 岗位筛选 */
  position?: string
  /** 状态筛选 */
  isActive?: boolean
}

/**
 * 可选指标（用于指标选择器）
 */
export interface MetricOption {
  /** 指标ID */
  id: number
  /** 指标名称 */
  metricName: string
  /** 指标编码 */
  metricCode: string
  /** 计算规则 */
  calcRule: CalcRule
  /** 单位 */
  unit: string
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  list: T[]
  total: number
}
