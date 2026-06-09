/**
 * IP组管理 - TypeScript类型定义
 * 
 * 基于PRD v9.0和API接口契约文档
 */

// ==================== 枚举类型 ====================

/**
 * IP组类型枚举
 */
export type IpGroupType = 1 | 2 // 1=大组, 2=小组

/**
 * IP组状态枚举
 */
export type IpGroupStatus = 0 | 1 // 0=停用, 1=启用

/**
 * 成员职位枚举
 */
export type MemberPosition = 'OPS_LEADER' | 'OPERATOR' | 'ANCHOR'

/**
 * 账号角色枚举
 */
export type AccountRole = 'PRIMARY' | 'SECONDARY' | 'BACKUP'

/**
 * 主播类型枚举
 */
export type AnchorType = 'LIVE' | 'VIDEO' | 'BOTH'

// ==================== IP组核心类型 ====================

/**
 * IP组信息（树形结构）
 */
export interface IpGroupTreeVO {
  /** IP组ID */
  id: number
  /** IP组名称 */
  groupName: string
  /** IP组类型：1=大组，2=小组 */
  groupType: IpGroupType
  /** 父级ID */
  parentId: number | null
  /** 父级名称 */
  parentName: string | null
  /** 组长ID */
  leaderId: number | null
  /** 组长姓名 */
  leaderName: string | null
  /** 成员数量（仅小组有） */
  memberCount?: number
  /** 关联账号数 */
  accountCount: number
  /** 关联主播数 */
  anchorCount: number
  /** 状态：0=停用，1=启用 */
  status: IpGroupStatus
  /** 创建时间 */
  createTime: string
  /** 子组列表 */
  children: IpGroupTreeVO[]
}

/**
 * IP组信息（平铺结构）
 */
export interface IpGroupListVO {
  /** IP组ID */
  id: number
  /** IP组名称 */
  groupName: string
  /** IP组类型：1=大组，2=小组 */
  groupType: IpGroupType
  /** 父级ID */
  parentId: number | null
  /** 父级名称 */
  parentName: string | null
  /** 组长姓名 */
  leaderName: string | null
  /** 成员数量 */
  memberCount: number
  /** 关联账号数 */
  accountCount: number
  /** 关联主播数 */
  anchorCount: number
  /** 状态：0=停用，1=启用 */
  status: IpGroupStatus
  /** 创建时间 */
  createTime: string
}

/**
 * 新增/修改IP组请求
 */
export interface IpGroupSaveReqVO {
  /** IP组ID（修改时必填） */
  id?: number
  /** IP组名称 */
  groupName: string
  /** IP组类型：1=大组，2=小组 */
  groupType: IpGroupType
  /** 父级ID（小组必填，大组必须为null） */
  parentId: number | null
  /** 组长ID */
  leaderId?: number | null
  /** 状态：0=停用，1=启用 */
  status: IpGroupStatus
  /** 备注 */
  remark?: string
}

/**
 * IP组分页查询参数
 */
export interface IpGroupPageReqVO {
  /** 组名称模糊匹配 */
  groupName?: string
  /** 组类型：1=大组，2=小组 */
  groupType?: IpGroupType
  /** 状态：0=停用，1=启用 */
  status?: IpGroupStatus
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
}

/**
 * IP组分页响应
 */
export interface IpGroupPageRespVO {
  /** 总记录数 */
  total: number
  /** 数据列表 */
  list: IpGroupListVO[]
}

// ==================== 统计信息类型 ====================

/**
 * IP组统计信息
 */
export interface IpGroupStatsVO {
  /** 成员数量 */
  memberCount: number
  /** 关联账号数 */
  accountCount: number
  /** 关联主播数 */
  anchorCount: number
  /** 粉丝总量 */
  totalFollowers: number
  /** 内容产出总数 */
  totalContent: number
  /** 直播时长总和（小时） */
  totalLiveHours: number
}

// ==================== 成员管理类型 ====================

/**
 * 成员信息
 */
export interface IpGroupMemberVO {
  /** 成员ID */
  memberId: number
  /** 用户ID */
  userId: number
  /** 用户姓名 */
  userName: string
  /** 职位 */
  position: MemberPosition
  /** 职位文本 */
  positionText: string
  /** 是否组长 */
  isLeader: boolean
  /** 加入时间 */
  joinTime: string
}

/**
 * 添加成员请求
 */
export interface IpGroupMemberSaveReqVO {
  /** 用户ID */
  userId: number
  /** 职位 */
  position: MemberPosition
  /** 是否组长 */
  isLeader?: boolean
}

// ==================== 关联账号类型 ====================

/**
 * 关联账号信息
 */
export interface IpGroupAccountVO {
  /** 账号ID */
  accountId: number
  /** 账号名称 */
  accountName: string
  /** 平台类型 */
  platform: string
  /** 平台文本 */
  platformText: string
  /** 粉丝数 */
  followerCount: number
  /** 账号角色 */
  accountRole: AccountRole
  /** 账号角色文本 */
  accountRoleText: string
}

/**
 * 关联账号请求
 */
export interface IpGroupAccountBindReqVO {
  /** 账号ID列表 */
  accountIds: number[]
  /** 账号角色 */
  accountRole: AccountRole
}

// ==================== 关联主播类型 ====================

/**
 * 关联主播信息
 */
export interface IpGroupAnchorVO {
  /** 主播用户ID */
  anchorUserId: number
  /** 主播姓名 */
  anchorName: string
  /** 主播类型 */
  anchorType: AnchorType
  /** 主播类型文本 */
  anchorTypeText: string
  /** 关联时间 */
  relTime: string
}

/**
 * 关联主播请求
 */
export interface IpGroupAnchorBindReqVO {
  /** 主播用户ID列表 */
  anchorUserIds: number[]
}
