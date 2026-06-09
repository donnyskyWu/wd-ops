/**
 * 首页仪表盘 - TypeScript类型定义
 * 
 * 基于PRD v9.0和页面规格文档
 */

// ==================== KPI数据类型 ====================

/**
 * 首页KPI数据
 */
export interface DashboardHomeKpiVO {
  /** 平台账号数 */
  totalAccounts: number
  /** 账号数环比变化率（%） */
  accountChangeRate: number
  /** 粉丝总量 */
  totalFollowers: number
  /** 粉丝量环比变化率（%） */
  followerChangeRate: number
  /** 今日内容数 */
  todayContentCount: number
  /** 内容数环比变化率（%） */
  contentChangeRate: number
  /** 待审核数量 */
  pendingReviewCount: number
  /** 预警数量 */
  alertCount: number
}

// ==================== 账号数据概览类型 ====================

/**
 * 平台类型枚举
 */
export type PlatformType = 
  | 'WECHAT_MP'      // 公众号
  | 'VIDEO_ACCOUNT'  // 视频号
  | 'DOUYIN'         // 抖音
  | 'KUAISHOU'       // 快手
  | 'XIAOHONGSHU'    // 小红书
  | 'SERVICE_ACCOUNT' // 服务号
  | 'WEWORK'         // 企业微信

/**
 * 账号数据概览项
 */
export interface DashboardAccountOverviewVO {
  /** 平台类型 */
  platformType: PlatformType
  /** 账号数量 */
  accountCount: number
  /** 粉丝数量 */
  followerCount: number
}

// ==================== 内容数据概览类型 ====================

/**
 * 内容数据概览项（按日期）
 */
export interface DashboardContentOverviewVO {
  /** 日期（YYYY-MM-DD） */
  date: string
  /** 公众号内容数 */
  wechatCount: number
  /** 抖音内容数 */
  douyinCount: number
  /** 快手内容数 */
  kuaishouCount: number
  /** 小红书内容数 */
  xiaohongshuCount: number
  /** 视频号内容数 */
  videoAccountCount: number
}

// ==================== 预警通知类型 ====================

/**
 * 预警级别枚举
 */
export type AlertLevel = 'CRITICAL' | 'WARNING' | 'INFO'

/**
 * 预警通知项
 */
export interface DashboardAlertItemVO {
  /** 预警ID */
  alertId: number
  /** 预警级别 */
  alertLevel: AlertLevel
  /** 预警内容 */
  alertContent: string
  /** 触发时间 */
  triggerTime: string
}

// ==================== 待办事项类型 ====================

/**
 * 待办类型枚举
 */
export type TodoType = 'REVIEW' | 'TASK' | 'EXPIRE'

/**
 * 待办事项项
 */
export interface DashboardTodoItemVO {
  /** 待办类型 */
  type: TodoType
  /** 待办标题 */
  title: string
  /** 待办数量 */
  count: number
  /** 跳转路由 */
  route: string
}

// ==================== 快捷入口类型 ====================

/**
 * 快捷入口项
 */
export interface QuickAccessItem {
  /** 入口名称 */
  label: string
  /** 图标组件名 */
  icon: string
  /** 跳转路由 */
  route: string
}
