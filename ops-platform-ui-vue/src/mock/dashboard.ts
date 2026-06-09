/**
 * 首页仪表盘 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 * 更新: 2026-05-29 - 增强数据多样性和真实性
 */

import type {
  DashboardHomeKpiVO,
  DashboardAccountOverviewVO,
  DashboardContentOverviewVO,
  DashboardAlertItemVO,
  DashboardTodoItemVO,
} from '@/types/dashboard'

/**
 * Mock KPI数据
 * 模拟真实运营数据场景
 */
export const mockDashboardKpi: DashboardHomeKpiVO = {
  totalAccounts: 256,
  accountChangeRate: 2.1,
  totalFollowers: 12345000,
  followerChangeRate: 0.8,
  todayContentCount: 12,
  contentChangeRate: -1,
  pendingReviewCount: 5,
  alertCount: 3,
}

/**
 * Mock账号数据概览
 * 各平台账号分布及粉丝数
 */
export const mockAccountOverview: DashboardAccountOverviewVO[] = [
  { platformType: 'WECHAT_MP', accountCount: 45, followerCount: 3200000 },
  { platformType: 'DOUYIN', accountCount: 38, followerCount: 5600000 },
  { platformType: 'KUAISHOU', accountCount: 32, followerCount: 2100000 },
  { platformType: 'XIAOHONGSHU', accountCount: 28, followerCount: 1800000 },
  { platformType: 'VIDEO_ACCOUNT', accountCount: 25, followerCount: 950000 },
  { platformType: 'SERVICE_ACCOUNT', accountCount: 18, followerCount: 680000 },
]

/**
 * Mock内容数据概览（近7天）
 * 更真实的内容发布趋势
 */
export const mockContentOverview: DashboardContentOverviewVO[] = [
  { date: '2026-05-22', wechatCount: 8, douyinCount: 12, kuaishouCount: 6, xiaohongshuCount: 5, videoAccountCount: 3 },
  { date: '2026-05-23', wechatCount: 10, douyinCount: 15, kuaishouCount: 8, xiaohongshuCount: 7, videoAccountCount: 4 },
  { date: '2026-05-24', wechatCount: 7, douyinCount: 10, kuaishouCount: 5, xiaohongshuCount: 4, videoAccountCount: 2 },
  { date: '2026-05-25', wechatCount: 12, douyinCount: 18, kuaishouCount: 9, xiaohongshuCount: 8, videoAccountCount: 5 },
  { date: '2026-05-26', wechatCount: 9, douyinCount: 14, kuaishouCount: 7, xiaohongshuCount: 6, videoAccountCount: 3 },
  { date: '2026-05-27', wechatCount: 11, douyinCount: 16, kuaishouCount: 8, xiaohongshuCount: 7, videoAccountCount: 4 },
  { date: '2026-05-28', wechatCount: 10, douyinCount: 13, kuaishouCount: 7, xiaohongshuCount: 6, videoAccountCount: 3 },
]

/**
 * Mock预警通知列表
 * 真实业务场景的预警信息
 */
export const mockAlertList: DashboardAlertItemVO[] = [
  {
    alertId: 1,
    alertLevel: 'CRITICAL',
    alertContent: '「科技前沿观察」公众号认证即将到期',
    triggerTime: '2小时前',
  },
  {
    alertId: 2,
    alertLevel: 'WARNING',
    alertContent: '「职场成长笔记」账号状态异常需处理',
    triggerTime: '5小时前',
  },
  {
    alertId: 3,
    alertLevel: 'INFO',
    alertContent: '「生活小妙招」账号近7日产出为0',
    triggerTime: '1天前',
  },
]

/**
 * Mock待办事项列表
 * 真实的运营待办任务
 */
export const mockTodoList: DashboardTodoItemVO[] = [
  {
    type: 'REVIEW',
    title: '待审核内容',
    count: 5,
    route: '/content-manage',
  },
  {
    type: 'TASK',
    title: '待处理任务',
    count: 3,
    route: '/task-manage',
  },
  {
    type: 'EXPIRE',
    title: '即将到期',
    count: 2,
    route: '/report',
  },
]
