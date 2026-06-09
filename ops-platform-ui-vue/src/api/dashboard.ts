/**
 * 首页仪表盘 - API接口封装
 * 
 * 基于PRD v9.0和API路径规范
 */

import { request } from '@/utils/request'
import type {
  DashboardHomeKpiVO,
  DashboardAccountOverviewVO,
  DashboardContentOverviewVO,
  DashboardAlertItemVO,
  DashboardTodoItemVO,
} from '@/types/dashboard'

/** API-M0 首页指标 */
export interface HomeMetricsVO {
  totalAuthors: number
  totalContent: number
  sopCompletionRate: number
  avgPerfGrade: string
}

export interface TrendPointVO {
  date: string
  count: number
  platform: string
}

export interface PlatformDistVO {
  platform: string
  count: number
  percentage: number
}

export interface HomeTodoVO {
  title: string
  source: string
  time: string
  actionUrl: string
}

export interface QuickActionVO {
  name: string
  icon: string
  url: string
  permission: string
}

export interface HomeQueryParams {
  ipGroupId?: number
  startDate?: string
  endDate?: string
  platformType?: string
  type?: 'CONTENT' | 'FOLLOWER'
  limit?: number
}

/**
 * 获取首页KPI数据
 * @returns KPI数据
 */
export function getDashboardKpi(): Promise<DashboardHomeKpiVO> {
  return request.get({ url: '/oa/dashboard/home/kpi' })
}

/**
 * 获取账号数据概览
 * @returns 各平台账号分布数据
 */
export function getAccountOverview(): Promise<DashboardAccountOverviewVO[]> {
  return request.get({ url: '/oa/dashboard/home/account-overview' })
}

/**
 * 获取内容数据概览
 * @returns 近7天内容发布趋势数据
 */
export function getContentOverview(): Promise<DashboardContentOverviewVO[]> {
  return request.get({ url: '/oa/dashboard/home/content-overview' })
}

/**
 * 获取预警通知列表
 * @returns 最新5条未处理预警
 */
export function getAlertList(): Promise<DashboardAlertItemVO[]> {
  return request.get({ url: '/oa/dashboard/home/alert-list' })
}

/**
 * 获取待办事项列表
 * @returns 待办事项汇总
 */
export function getTodoList(): Promise<DashboardTodoItemVO[]> {
  return request.get({ url: '/oa/dashboard/home/todo-list' })
}

/** API-M0：核心指标 */
export function getHomeMetrics(params?: HomeQueryParams): Promise<HomeMetricsVO> {
  return request.get({ url: '/oa/dashboard/home/metrics', params })
}

/** API-M0：趋势数据 */
export function getHomeTrend(params?: HomeQueryParams): Promise<TrendPointVO[]> {
  return request.get({ url: '/oa/dashboard/home/trend', params })
}

/** API-M0：平台分布 */
export function getPlatformDist(params?: HomeQueryParams): Promise<PlatformDistVO[]> {
  return request.get({ url: '/oa/dashboard/home/platform-dist', params })
}

/** API-M0：待办提醒（明细） */
export function getHomeTodos(params?: HomeQueryParams): Promise<HomeTodoVO[]> {
  return request.get({ url: '/oa/dashboard/home/todos', params })
}

/** API-M0：快捷入口 */
export function getQuickActions(): Promise<QuickActionVO[]> {
  return request.get({ url: '/oa/dashboard/home/quick-actions' })
}

/** API-M0：刷新缓存 */
export function refreshHome(): Promise<boolean> {
  return request.post({ url: '/oa/dashboard/home/refresh' })
}

/** M6 分析大屏 */
export function getAnalyticsDashboard(id: number) {
  return request.get({ url: `/oa/dashboard/${id}` })
}

export function createAnalyticsDashboard(data: Record<string, unknown>) {
  return request.post({ url: '/oa/dashboard/create', data })
}

export function getDashboardConfigList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/dashboard-config/list', params })
}

export function updateDashboardConfig(data: Record<string, unknown>) {
  return request.post({ url: '/oa/dashboard-config/update', data })
}

export default {
  getDashboardKpi,
  getAccountOverview,
  getContentOverview,
  getAlertList,
  getTodoList,
  getHomeMetrics,
  getHomeTrend,
  getPlatformDist,
  getHomeTodos,
  getQuickActions,
  refreshHome,
}
