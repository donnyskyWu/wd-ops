/**
 * 账号分析 - TypeScript类型定义
 */

// ==================== 枚举类型 ====================

/**
 * 平台类型枚举
 */
export enum PlatformType {
  ALL = 'ALL',
  WECHAT_MP = 'WECHAT_MP',
  VIDEO_ACCOUNT = 'VIDEO_ACCOUNT',
  DOUYIN = 'DOUYIN',
  KUAISHOU = 'KUAISHOU',
  XIAOHONGSHU = 'XIAOHONGSHU',
  SERVICE_ACCOUNT = 'SERVICE_ACCOUNT',
  WECHAT_WORK = 'WECHAT_WORK',
}

/**
 * 账号状态枚举
 */
export enum AccountStatus {
  ENABLED = 'enabled',
  DISABLED = 'disabled',
}

// ==================== 账号核心类型 ====================

/**
 * 账号列表项
 */
export interface AccountVO {
  /** 账号ID */
  id: number
  /** 账号名称 */
  accountName: string
  /** 平台类型 */
  platformType: PlatformType
  /** 平台名称 */
  platformName: string
  /** 所属IP组 */
  ipGroupName: string
  /** 粉丝数 */
  followerCount: number
  /** 内容数 */
  contentCount: number
  /** 账号状态 */
  accountStatus: AccountStatus
  /** 状态文本 */
  statusText: string
  /** 实名人（脱敏） */
  realName: string
  /** 运营人员 */
  operatorName: string
}

/**
 * 账号查询参数
 */
export interface AccountQuery {
  /** IP组ID */
  ipGroupId?: number
  /** 关键词 */
  keyword?: string
  /** 状态 */
  accountStatus?: AccountStatus
  /** 实名人ID */
  realPersonId?: number
  /** 运营人员ID */
  operatorId?: number
  /** 平台类型 */
  platformType?: PlatformType
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

// ==================== 趋势数据类型 ====================

/**
 * 粉丝趋势数据点
 */
export interface FollowerTrendPoint {
  /** 日期 */
  date: string
  /** 粉丝数 */
  followerCount: number
  /** 新增粉丝 */
  newFollowers: number
}

/**
 * 内容趋势数据点
 */
export interface ContentTrendPoint {
  /** 日期 */
  date: string
  /** 内容数 */
  contentCount: number
  /** 播放量 */
  playCount: number
}
