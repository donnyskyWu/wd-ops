/**
 * 作者 API（GATE-S3 真实 API）
 * 基础 CRUD 已迁移 Football author/info；保留 list/dashboard/ext 供 OPS 扩展面。
 */
import { request } from '@/utils/request'
import type {
  AuthorPageReqVO,
  AuthorPageRespVO,
  AuthorDashboardVO,
  OpsUserVO,
  OpsAnchorRelPageReqVO,
  OpsAnchorRelPageRespVO,
  OpsAnchorRelSaveReqVO,
  OpsAnchorStatsVO,
  AuthorExtVO,
  AuthorExtUpdateReqVO,
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

/** @deprecated 基础 CRUD 已迁移 Football，调用将返回 410 */
export function createAuthor(_data: unknown): Promise<number> {
  return request.post({ url: '/oa/author/create', data: _data })
}

/** @deprecated 基础 CRUD 已迁移 Football，调用将返回 410 */
export function updateAuthor(_data: unknown): Promise<boolean> {
  return request.put({ url: '/oa/author/update', data: _data })
}

/** @deprecated 基础 CRUD 已迁移 Football，调用将返回 410 */
export function deleteAuthor(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/author/delete', params: { id } })
}

export function getAuthorExt(authorUserId: number): Promise<AuthorExtVO> {
  return request.get({ url: `/oa/author-ext/${authorUserId}` })
}

export function updateAuthorExt(authorUserId: number, data: AuthorExtUpdateReqVO): Promise<boolean> {
  return request.put({ url: `/oa/author-ext/${authorUserId}`, data })
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
  getAuthorExt,
  updateAuthorExt,
  getAuthorDashboard,
  getAuthorOpsList,
  getOpsAnchorRelPage,
  createOpsAnchorRel,
  updateOpsAnchorRel,
  deleteOpsAnchorRel,
  getOpsAnchorStats,
}
