/**
 * 订单归因 - API
 */
import { request } from '@/utils/request'

export interface OrderAttributionQuery {
  ipGroupId?: number
  accountId?: number
  startDate: string
  endDate: string
  pageNum?: number
  pageSize?: number
}

export function getOrderAttributionList(params: OrderAttributionQuery) {
  return request.get({ url: '/oa/order-attribution/list', params })
}

export function getOrderAttributionRoi(params: Omit<OrderAttributionQuery, 'pageNum' | 'pageSize'>) {
  return request.get({ url: '/oa/order-attribution/roi', params })
}

export function exportOrderAttribution(startDate: string, endDate: string) {
  return request.post({
    url: '/oa/order-attribution/export',
    params: { startDate, endDate },
  })
}
