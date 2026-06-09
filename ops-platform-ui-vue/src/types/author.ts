/**
 * 作者管理 - TypeScript类型定义
 * 
 * 基于PRD v9.0和API接口契约文档
 */

// ==================== 枚举类型 ====================

/**
 * 作者类型枚举
 */
export type AuthorType = 'LIVE' | 'SHORT_VIDEO' | 'BOTH'

/**
 * 作者状态枚举
 */
export type AuthorStatus = 0 | 1 // 0=停用, 1=启用

// ==================== 作者核心类型 ====================

/**
 * 作者列表项（实际由 /oa/author/list 返回，字段以后端 AuthorVO 为准）
 */
export interface AuthorListVO {
  /** 作者ID */
  id: number
  /** 作者名称 */
  authorName: string
  /** 作者类型（字典值 dict_author_type） */
  authorType: string
  /** 作者类型文本（前端可由 dict 渲染） */
  authorTypeText?: string
  /** IP组ID */
  ipGroupId: number
  /** IP组名称 */
  ipGroupName: string
  /** 主推号ID */
  primaryAccountId: number | null
  /** 主推号名称 */
  primaryAccountName: string | null
  /** 系统用户ID（组长绑定） */
  userId: number | null
  /** 系统用户姓名（组长） */
  userName: string | null
  /** 状态 0/1 */
  status: AuthorStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string

  // 以下字段为看板/统计接口字段，list 接口暂未返回（前端兜底显示 "-"）
  /** 粉丝数（看板） */
  followerCount?: number
  /** 内容数（看板） */
  contentCount?: number
  /** 直播时长（小时） */
  liveHours?: number
  /** 视频数量 */
  videoCount?: number
  /** 运营人员姓名列表（详情接口） */
  opsUserNames?: string[]
}

/**
 * 新增/修改作者请求
 */
export interface AuthorSaveReqVO {
  /** 作者ID（修改时必填） */
  id?: number
  /** 作者名称 */
  authorName: string
  /** IP组ID（必须是小组） */
  ipGroupId: number
  /** 作者类型 */
  authorType: AuthorType
  /** 主推号ID */
  primaryAccountId?: number | null
  /** 系统用户ID */
  userId?: number | null
  /** 状态 */
  status: AuthorStatus
}

/**
 * 作者分页查询参数
 */
export interface AuthorPageReqVO {
  /** 作者名称模糊匹配 */
  authorName?: string
  /** 作者类型 */
  authorType?: AuthorType
  /** IP组ID */
  ipGroupId?: number
  /** 状态 */
  status?: AuthorStatus
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
}

/**
 * 作者分页响应
 */
export interface AuthorPageRespVO {
  /** 总记录数 */
  total: number
  /** 数据列表 */
  list: AuthorListVO[]
}

// ==================== 作者数据看板类型 ====================

/**
 * 粉丝趋势数据
 */
export interface FollowerTrendVO {
  /** 日期 */
  date: string
  /** 粉丝数 */
  followerCount: number
  /** 新增粉丝数 */
  newFollower: number
  /** 取关数 */
  unfollowCount: number
}

/**
 * 内容趋势数据
 */
export interface ContentTrendVO {
  /** 日期 */
  date: string
  /** 内容数 */
  contentCount: number
  /** 阅读数 */
  readCount: number
  /** 点赞数 */
  likeCount: number
}

/**
 * 作者数据看板
 */
export interface AuthorDashboardVO {
  /** 作者ID */
  id: number
  /** 作者名称 */
  authorName: string
  /** 作者类型 */
  authorType: AuthorType
  /** IP组名称 */
  ipGroupName: string
  /** 主推号名称 */
  primaryAccountName: string | null
  /** 运营人员姓名列表 */
  opsUserNames: string[]
  /** 粉丝数 */
  followerCount: number
  /** 内容产出数 */
  contentCount: number
  /** 直播时长（小时） */
  liveHours: number
  /** 视频数量 */
  videoCount: number
  /** 爆款内容数 */
  hitContentCount: number
  /** 粉丝趋势（近30日） */
  followerTrend: FollowerTrendVO[]
  /** 内容趋势（近30日） */
  contentTrend: ContentTrendVO[]
}

// ==================== 运营人员类型 ====================

/**
 * 运营人员信息
 */
export interface OpsUserVO {
  /** 用户ID */
  userId: number
  /** 用户姓名 */
  userName: string
  /** 部门名称 */
  deptName: string
  /** 关联时间 */
  relTime: string
}

// ==================== 运营→主播关联类型 ====================

/**
 * 运营→主播关联列表项
 */
export interface OpsAnchorRelVO {
  /** 关联ID */
  id: number
  /** 运营人员用户ID */
  opsUserId: number
  /** 运营人员姓名 */
  opsUserName: string
  /** 主播用户ID */
  anchorUserId: number
  /** 主播姓名 */
  anchorUserName: string
  /** IP组ID */
  ipGroupId: number
  /** IP组名称 */
  ipGroupName: string
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string | null
  /** 创建时间 */
  createTime: string
}

/**
 * 新增/修改运营→主播关联请求
 */
export interface OpsAnchorRelSaveReqVO {
  /** 关联ID（修改时必填） */
  id?: number
  /** 运营人员用户ID */
  opsUserId: number
  /** 主播用户ID */
  anchorUserId: number
  /** IP组ID */
  ipGroupId: number
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate?: string | null
}

/**
 * 运营→主播关联分页查询参数
 */
export interface OpsAnchorRelPageReqVO {
  /** 运营人员用户ID */
  opsUserId?: number
  /** 主播用户ID */
  anchorUserId?: number
  /** IP组ID */
  ipGroupId?: number
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
}

/**
 * 运营→主播关联分页响应
 */
export interface OpsAnchorRelPageRespVO {
  /** 总记录数 */
  total: number
  /** 数据列表 */
  list: OpsAnchorRelVO[]
}

// ==================== 运营统计数据 ====================

/**
 * 运营负责的主播统计
 */
export interface OpsAnchorStatsVO {
  /** 运营人员用户ID */
  opsUserId: number
  /** 运营人员姓名 */
  opsUserName: string
  /** 主播总数 */
  totalAnchorCount: number
  /** 活跃主播数 */
  activeAnchorCount: number
  /** 粉丝总量 */
  totalFollowerCount: number
  /** 内容产出总数 */
  totalContentCount: number
  /** 直播时长总和（小时） */
  totalLiveHours: number
}
