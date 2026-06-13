/**
 * 任务管理 - API（GATE-S4 真实 API）
 */
import { request } from '@/utils/request'
import type {
  TaskVO,
  TaskQuery,
  PageResult,
  TaskExecuteVO,
  TaskAttachmentVO,
} from '@/types/task'

function toTaskListParams(params: TaskQuery) {
  const { pageNo, ...rest } = params
  return { ...rest, pageNum: pageNo }
}

export function getTaskList(params: TaskQuery): Promise<PageResult<TaskVO>> {
  return request.get({ url: '/oa/task/list', params: toTaskListParams(params) })
}

export function getMyTasks(params: TaskQuery): Promise<PageResult<TaskVO>> {
  return request.get({ url: '/oa/task/my-tasks', params: toTaskListParams(params) })
}

export function createTask(data: {
  templateId: number
  nodeId: number
  planName?: string
  assigneeId: number
  ipGroupId?: number
  authorId?: number
  needReview?: number
}): Promise<number> {
  return request.post({ url: '/oa/task/create', data })
}

export function startTask(id: number): Promise<boolean> {
  return request.post({ url: `/oa/task/${id}/start` })
}

export function completeTask(id: number, deliverables?: string): Promise<boolean> {
  return request.post({ url: `/oa/task/${id}/complete`, data: { deliverables } })
}

export function submitTaskReview(id: number): Promise<boolean> {
  return request.post({ url: `/oa/task/${id}/submit-review` })
}

export function getTaskExecute(id: number): Promise<TaskExecuteVO> {
  return request.get({ url: `/oa/task/${id}/execute` })
}

export function saveTaskExecute(id: number, data: {
  deliverables?: string
  deliverableAttachments?: TaskAttachmentVO[]
}): Promise<boolean> {
  return request.post({ url: `/oa/task/${id}/execute/save`, data })
}

export function uploadTaskExecuteAttachment(id: number, file: File): Promise<TaskAttachmentVO> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post({
    url: `/oa/task/${id}/execute/upload`,
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function completeTaskExecute(id: number): Promise<boolean> {
  return request.post({ url: `/oa/task/${id}/execute/complete` })
}

export default {
  getTaskList,
  getMyTasks,
  createTask,
  startTask,
  completeTask,
  submitTaskReview,
  getTaskExecute,
  saveTaskExecute,
  uploadTaskExecuteAttachment,
  completeTaskExecute,
}
