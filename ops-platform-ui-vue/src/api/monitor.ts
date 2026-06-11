/**
 * M7 作品监测 - API接口封装
 */
import { request } from '@/utils/request'

export function getExternalWorkList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/monitor/external/list', params })
}

export function getHitWorkList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/monitor/hit/list', params })
}

export function getLowScoreWorkList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/monitor/low-score/list', params })
}

export function getHighFollowerAccountList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/monitor/high-follower/list', params })
}

export function getLowFollowerAccountList(params: Record<string, unknown>) {
  return request.get({ url: '/oa/monitor/low-follower/list', params })
}

/** @deprecated use getHighFollowerAccountList */
export function getHighFollowerWorkList(params: Record<string, unknown>) {
  return getHighFollowerAccountList(params)
}

/** @deprecated use getLowFollowerAccountList */
export function getLowFollowerWorkList(params: Record<string, unknown>) {
  return getLowFollowerAccountList(params)
}

export function getIpThemeStats(id: number) {
  return request.get({ url: `/oa/monitor/ip-theme/${id}` })
}

export function getIndustryStats(id: number) {
  return request.get({ url: `/oa/monitor/industry/${id}` })
}
