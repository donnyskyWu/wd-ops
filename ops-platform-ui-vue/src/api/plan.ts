import { request } from '@/utils/request'

export interface ContentPlanVO {
  id: number
  planName: string
  templateId: number
  templateName?: string
  ipGroupId: number
  ipGroupName?: string
  startDate: string
  endDate: string
  description?: string
  status: string
  progress?: number
  competitions?: ContentPlanCompetitionVO[]
  steps?: ContentPlanStepVO[]
  createTime?: string
}

export interface ContentPlanCompetitionVO {
  competitionId: string
  competitionName: string
}

export interface ContentPlanStepVO {
  nodeId: number
  nodeName?: string
  nodeOrder?: number
  executorRole?: string
  assigneeIds?: number[]
  scheduledStart?: string
  scheduledEnd?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export function getContentPlanPage(params: {
  planName?: string
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<PageResult<ContentPlanVO>> {
  return request.get({ url: '/oa/plan/list', params })
}

export function getContentPlan(id: number): Promise<ContentPlanVO> {
  return request.get({ url: '/oa/plan/get', params: { id } })
}

export function createContentPlan(data: {
  planName: string
  templateId: number
  ipGroupId: number
  startDate: string
  endDate: string
  description?: string
  competitions: ContentPlanCompetitionVO[]
  steps: Array<{
    nodeId: number
    assigneeIds: number[]
    scheduledStart?: string
    scheduledEnd?: string
  }>
}): Promise<number> {
  return request.post({ url: '/oa/plan/create', data })
}

export function startContentPlan(id: number): Promise<boolean> {
  return request.post({ url: `/oa/plan/${id}/start` })
}

export function submitTerminatePlan(id: number, reason?: string): Promise<boolean> {
  return request.post({ url: `/oa/plan/${id}/terminate`, data: { reason } })
}

export function approveTerminatePlan(id: number): Promise<boolean> {
  return request.post({ url: `/oa/plan/${id}/terminate/approve` })
}

export function rejectTerminatePlan(id: number): Promise<boolean> {
  return request.post({ url: `/oa/plan/${id}/terminate/reject` })
}

export function deleteContentPlan(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/plan/delete', params: { id } })
}
