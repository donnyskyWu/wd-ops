/**
 * 人效盘点 - API（S-R9 字段对齐）
 */
import { request } from '@/utils/request'
import type {
  ProductivityReviewVO,
  ProductivityReviewQuery,
  PageResult,
  ProductivityReviewDetailVO,
} from '@/types/productivity'

export function getProductivityList(params: ProductivityReviewQuery): Promise<PageResult<ProductivityReviewVO>> {
  return request.get({ url: '/oa/productivity-review/list', params })
}

export function getProductivityDetail(
  userId: number,
  params: { startDate?: string; endDate?: string },
): Promise<ProductivityReviewDetailVO> {
  return request.get({ url: `/oa/productivity-review/detail/${userId}`, params })
}

export function getProductivityAnchors(
  ipGroupId: number,
  params: { startDate?: string; endDate?: string },
): Promise<ProductivityReviewVO[]> {
  return request.get({ url: '/oa/productivity-review/detail/anchors', params: { ipGroupId, ...params } })
}

/** S-R9 B3: 导出 CSV（后端 controller 是 ResponseEntity<byte[]> + Content-Disposition，前端直接 GET/POST 触发下载） */
export function exportProductivityCsv(params: {
  startDate?: string
  endDate?: string
  timeDimension?: string
  ipGroupId?: number
  userId?: number
  position?: string
  keyword?: string
}): Promise<any> {
  // S-R9: 同步 CSV，直接 window.open/iframe 触发（简化处理，避免 axios blob 兼容性）
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v != null && v !== '') qs.append(k, String(v))
  })
  const url = `/admin-api/oa/productivity-review/export?${qs.toString()}`
  window.open(url, '_blank')
  return Promise.resolve()
}
