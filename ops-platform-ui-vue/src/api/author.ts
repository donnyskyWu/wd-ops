/**
 * 作者管理 - API（GATE-S3 真实 API）
 */
import { request } from '@/utils/request'
import type {
  AuthorPageReqVO,
  AuthorPageRespVO,
  AuthorSaveReqVO,
  AuthorDashboardVO,
  OpsUserVO,
  OpsAnchorRelPageReqVO,
  OpsAnchorRelPageRespVO,
  OpsAnchorRelSaveReqVO,
  OpsAnchorStatsVO,
} from '@/types/author'

export function getAuthorPage(params: AuthorPageReqVO): Promise<AuthorPageRespVO> {
  return request.get({ url: '/oa/author/list', params })
}

// 后端 author/list 实际入参：ipGroupId / keyword / status / page / size
export type AuthorListQuery = {
  ipGroupId?: number
  keyword?: string
  status?: number
  page?: number
  size?: number
}

export function createAuthor(data: AuthorSaveReqVO): Promise<number> {
  return request.post({ url: '/oa/author/create', data })
}

export function updateAuthor(data: AuthorSaveReqVO): Promise<boolean> {
  return request.put({ url: '/oa/author/update', data })
}

export function deleteAuthor(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/author/delete', params: { id } })
}

export function getAuthorDashboard(id: number): Promise<AuthorDashboardVO> {
  return request.get({ url: `/oa/author/${id}/dashboard` })
}

export function getAuthorOpsList(id: number): Promise<OpsUserVO[]> {
  return request.get({ url: `/oa/author/${id}/ops-list` })
}

export function getOpsAnchorRelPage(params: OpsAnchorRelPageReqVO): Promise<OpsAnchorRelPageRespVO> {
  return request.get({ url: '/oa/ops-anchor/list', params })
}

export function createOpsAnchorRel(data: OpsAnchorRelSaveReqVO): Promise<boolean> {
  return request.post({ url: '/oa/ops-anchor/create', data })
}

export function updateOpsAnchorRel(data: OpsAnchorRelSaveReqVO): Promise<boolean> {
  return request.put({ url: '/oa/ops-anchor/update', data })
}

export function deleteOpsAnchorRel(opsUserId: number, anchorUserId: number): Promise<boolean> {
  return request.delete({ url: '/oa/ops-anchor/delete', params: { opsUserId, anchorUserId } })
}

export function getOpsAnchorStats(userId: number): Promise<OpsAnchorStatsVO> {
  return request.get({ url: `/oa/ops/${userId}/anchor-stats` })
}

export default {
  getAuthorPage,
  createAuthor,
  updateAuthor,
  deleteAuthor,
  getAuthorDashboard,
  getAuthorOpsList,
  getOpsAnchorRelPage,
  createOpsAnchorRel,
  updateOpsAnchorRel,
  deleteOpsAnchorRel,
  getOpsAnchorStats,
}
