/**
 * 人效盘点 - API（GATE-S3 真实 API）
 */
import { request } from '@/utils/request'
import type {
  ProductivityVO,
  ProductivityQuery,
  PageResult,
  ProductivityDetailVO,
} from '@/types/productivity'

export function getProductivityList(params: ProductivityQuery): Promise<PageResult<ProductivityVO>> {
  return request.get({ url: '/oa/productivity-review/list', params })
}

export function getProductivityDetail(
  userId: number,
  params: { startDate: string; endDate: string },
): Promise<ProductivityDetailVO> {
  return request.get({ url: `/oa/productivity-review/detail/${userId}`, params })
}
