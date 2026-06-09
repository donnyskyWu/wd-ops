/**
 * 任务管理 - API（GATE-S4 真实 API）
 */
import { request } from '@/utils/request'
import type {
  TaskVO,
  TaskQuery,
  PageResult,
} from '@/types/task'

export function getTaskList(params: TaskQuery): Promise<PageResult<TaskVO>> {
  return request.get({ url: '/oa/task/list', params })
}

export function getMyTasks(params: TaskQuery): Promise<PageResult<TaskVO>> {
  return request.get({ url: '/oa/task/my-tasks', params })
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

export default {
  getTaskList,
  getMyTasks,
  createTask,
  startTask,
  completeTask,
  submitTaskReview,
}
