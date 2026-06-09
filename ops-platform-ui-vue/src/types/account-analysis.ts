/**
 * 账号分析 - TypeScript类型定义
 * 
 * 基于PRD v9.0和API接口契约文档
 */

// ==================== 枚举类型 ====================

/**
 * 平台类型枚举
 */
export type PlatformType = 
  | 'WECHAT_MP'           // 公众号
  | 'VIDEO_ACCOUNT'       // 视频号
  | 'DOUYIN'              // 抖音
  | 'KUAISHOU'            // 快手
  | 'XIAOHONGSHU'         // 小红书
  | 'SERVICE_ACCOUNT'     // 服务号
  | 'WEWORK'              // 企业微信
  | 'ALL'                 // 全部

/**
 * 账号状态枚举
 */
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'BANNED'

/**
 * 内容类型枚举
 */
export type ContentType = 'ARTICLE' | 'VIDEO' | 'LIVE'

/**
 * 时间维度枚举
 */
export type TimeDimension = 'DAY' | 'WEEK' | 'MONTH'

/**
 * 排序字段枚举
 */
export type ContentOrderBy = 'publish_time' | 'read_count' | 'like_count'

/**
 * 排序方向枚举
 */
export type OrderDirection = 'asc' | 'desc'

// ==================== 账号分析核心类型 ====================

/**
 * 账号分析列表项
 */
export interface AccountAnalysisVO {
  /** 账号系统ID */
  accountId: number
  /** 平台账号ID */
  platformAccountId: string
  /** 账号名称 */
  accountName: string
  /** 平台类型 */
  platformType: PlatformType
  /** 平台类型文本 */
  platformTypeText: string
  /** IP组名称 */
  ipGroupName: string
  /** 粉丝数 */
  followerCount: number
  /** 内容数 */
  contentCount: number
  /** 账号状态 */
  accountStatus: AccountStatus
  /** 账号状态文本 */
  accountStatusText: string
  /** 实名人（脱敏） */
  realName: string
  /** 运营人员 */
  operatorName: string
  /** 创建时间 */
  createTime: string
}

/**
 * 账号分析查询参数
 */
export interface AccountAnalysisQueryVO {
  /** 平台类型 */
  platformType: PlatformType
  /** IP组ID */
  ipGroupId?: number
  /** 关键词（账号名称/ID模糊搜索） */
  keyword?: string
  /** 账号状态 */
  accountStatus?: AccountStatus
  /** 实名人ID */
  realnameId?: number
  /** 运营人员ID */
  operatorUserId?: number
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

// ==================== 粉丝详情类型 ====================

/**
 * 粉丝趋势数据点
 */
export interface FollowerTrendPoint {
  /** 日期 */
  date: string
  /** 粉丝总数 */
  followerCount: number
  /** 新增粉丝 */
  newFollower: number
  /** 取关数 */
  unfollowCount: number
  /** 净增 */
  netGrowth: number
}

/**
 * 性别比例
 */
export interface GenderRatio {
  male: number
  female: number
}

/**
 * 年龄分组
 */
export interface AgeGroup {
  group: string
  ratio: number
}

/**
 * 城市分布
 */
export interface CityDistribution {
  city: string
  ratio: number
}

/**
 * 粉丝画像
 */
export interface FollowerPortrait {
  /** 性别比例 */
  genderRatio: GenderRatio
  /** 年龄分布 */
  ageGroups: AgeGroup[]
  /** 城市分布 */
  topCities: CityDistribution[]
}

/**
 * 粉丝详情
 */
export interface FollowerDetailVO {
  /** 账号ID */
  accountId: number
  /** 账号名称 */
  accountName: string
  /** 粉丝总数 */
  totalFollower: number
  /** 新增粉丝 */
  newFollower: number
  /** 取关数 */
  unfollowCount: number
  /** 净增 */
  netGrowth: number
  /** 趋势数据 */
  trend: FollowerTrendPoint[]
  /** 粉丝画像 */
  portrait: FollowerPortrait
}

/**
 * 粉丝详情查询参数
 */
export interface FollowerDetailQueryVO {
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 时间维度 */
  timeDimension?: TimeDimension
}

// ==================== 作品详情类型 ====================

/**
 * 作品列表项
 */
export interface ContentVO {
  /** 内容ID */
  contentId: number
  /** 标题 */
  title: string
  /** 内容类型 */
  contentType: ContentType
  /** 发布时间 */
  publishTime: string
  /** 阅读量 */
  readCount: number
  /** 点赞数 */
  likeCount: number
  /** 评论数 */
  commentCount: number
  /** 转发数 */
  forwardCount: number
  /** 是否爆款 */
  isHit: boolean
}

/**
 * 作品查询参数
 */
export interface ContentQueryVO {
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 内容类型 */
  contentType?: ContentType
  /** 排序字段 */
  orderBy?: ContentOrderBy
  /** 排序方向 */
  orderDir?: OrderDirection
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
}
