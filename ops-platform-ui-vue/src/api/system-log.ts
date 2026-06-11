/**
 * M9 日志 API
 */
import { request } from '@/utils/request'

export interface OperationLogVO {
  id: number
  userName: string
  module: string
  action: string
  level?: string
  content: string
  method?: string
  params?: string
  response?: string
  ip?: string
  status: string
  createTime: string
}

export interface LoginLogVO {
  id: number
  userName: string
  ip: string
  userAgent?: string
  status: string
  message?: string
  createTime: string
}

export interface LogPageResult<T> {
  list: T[]
  total: number
}

export function fetchOperationLogs(params: {
  username?: string
  module?: string
  level?: string
  startTime?: string
  endTime?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<LogPageResult<OperationLogVO>>({ url: '/oa/system/log/operation', params })
}

export function fetchLoginLogs(params: {
  username?: string
  ip?: string
  status?: string
  startTime?: string
  endTime?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<LogPageResult<LoginLogVO>>({ url: '/oa/system/log/login', params })
}

export const LOG_MODULE_LABEL: Record<string, string> = {
  SYSTEM: '系统',
  USER: '用户',
  ACCOUNT: '账号',
  CONTENT: '内容',
  FINANCE: '财务',
  CONFIG: '配置',
  ANALYTICS: '分析',
  REPORT: '报表',
  COLLECT: '采集',
}
