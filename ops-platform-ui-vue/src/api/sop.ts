/**
 * SOP管理 - API（GATE-S4 真实 API）
 */
import { request } from '@/utils/request'
import type {
  SopTemplateVO,
  SopTemplateQuery,
  PageResult,
  CreateSopTemplateReq,
  SopNodeVO,
  DagValidateRequest,
  DagValidateResponse,
  SopReviewVO,
  ReviewActionReq,
} from '@/types/sop'

export function getSopTemplateList(params: SopTemplateQuery): Promise<PageResult<SopTemplateVO>> {
  return request.get({ url: '/oa/sop/template/list', params })
}

export function createSopTemplate(data: CreateSopTemplateReq): Promise<number> {
  return request.post({ url: '/oa/sop/template/create', data })
}

export function updateSopTemplate(data: CreateSopTemplateReq & { id: number }): Promise<boolean> {
  return request.put({ url: '/oa/sop/template/update', data })
}

export function deleteSopTemplate(id: number): Promise<boolean> {
  return request.delete({ url: `/oa/sop/template/${id}` })
}

export function getSopNodeList(templateId: number): Promise<SopNodeVO[]> {
  return request.get({ url: '/oa/sop/node/list', params: { templateId } })
}

export function createSopNode(data: Partial<SopNodeVO> & { templateId: number; nodeName: string; nodeOrder: number; executorRole: string }): Promise<number> {
  return request.post({ url: '/oa/sop/node/create', data })
}

export function updateSopNode(data: Partial<SopNodeVO> & { id: number }): Promise<boolean> {
  return request.put({ url: '/oa/sop/node/update', data })
}

export function validateDag(data: DagValidateRequest): Promise<DagValidateResponse> {
  return request.post({ url: '/oa/sop/node/validate-dag', data })
}

export function getSopReviewPending(reviewerId?: number): Promise<SopReviewVO[]> {
  return request.get({ url: '/oa/sop/review/pending', params: { reviewerId } })
}

export function approveReview(data: ReviewActionReq): Promise<boolean> {
  return request.post({ url: '/oa/sop/review/approve', data })
}

export function rejectReview(data: ReviewActionReq): Promise<boolean> {
  return request.post({ url: '/oa/sop/review/reject', data })
}

export default {
  getSopTemplateList,
  createSopTemplate,
  updateSopTemplate,
  deleteSopTemplate,
  getSopNodeList,
  createSopNode,
  updateSopNode,
  validateDag,
  getSopReviewPending,
  approveReview,
  rejectReview,
}
