/**
 * 任务管理 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  TaskVO,
  PageResult,
} from '@/types/task'
import { TaskStatus } from '@/types/task'

/**
 * Mock任务列表数据
 */
export const mockTaskList: TaskVO[] = [
  {
    id: 1001,
    planName: '5月第1期内容生产',
    nodeName: '写推文',
    assigneeName: '张三',
    executorRole: '公众号运营',
    status: TaskStatus.IN_PROGRESS,
    statusText: '执行中',
    slaDeadline: '2026-05-12 18:00:00',
    isOverdue: false,
  },
  {
    id: 1002,
    planName: '5月第1期内容生产',
    nodeName: '剪辑视频',
    assigneeName: '李四',
    executorRole: '剪辑',
    status: TaskStatus.PENDING,
    statusText: '待执行',
    slaDeadline: '2026-05-13 18:00:00',
    isOverdue: false,
  },
  {
    id: 1003,
    planName: '5月第2期内容生产',
    nodeName: '准备直播',
    assigneeName: '王五',
    executorRole: '直播运营',
    status: TaskStatus.PENDING_REVIEW,
    statusText: '待审核',
    slaDeadline: '2026-05-10 18:00:00',
    isOverdue: true,
  },
  {
    id: 1004,
    planName: '5月第2期内容生产',
    nodeName: '数据分析',
    assigneeName: '张三',
    executorRole: '运营组长',
    status: TaskStatus.COMPLETED,
    statusText: '已完成',
    slaDeadline: '2026-05-15 18:00:00',
    isOverdue: false,
  },
  {
    id: 1005,
    planName: '5月第3期内容生产',
    nodeName: '审核推文',
    assigneeName: '赵六',
    executorRole: '运营组长',
    status: TaskStatus.REVIEW_REJECTED,
    statusText: '审核驳回',
    slaDeadline: '2026-05-11 18:00:00',
    isOverdue: true,
  },
  {
    id: 1006,
    planName: '5月第3期内容生产',
    nodeName: '发布图文',
    assigneeName: '李四',
    executorRole: '公众号运营',
    status: TaskStatus.PENDING,
    statusText: '待执行',
    slaDeadline: '2026-05-14 18:00:00',
    isOverdue: false,
  },
  {
    id: 1007,
    planName: '5月第4期内容生产',
    nodeName: '撰写脚本',
    assigneeName: '王五',
    executorRole: '短视频运营',
    status: TaskStatus.IN_PROGRESS,
    statusText: '执行中',
    slaDeadline: '2026-05-16 18:00:00',
    isOverdue: false,
  },
  {
    id: 1008,
    planName: '5月第4期内容生产',
    nodeName: '视频拍摄',
    assigneeName: '赵六',
    executorRole: '拍摄',
    status: TaskStatus.PENDING,
    statusText: '待执行',
    slaDeadline: '2026-05-17 18:00:00',
    isOverdue: false,
  },
]

/**
 * 模拟分页查询任务列表
 */
export function mockGetTaskList(
  pageNo: number,
  pageSize: number
): PageResult<TaskVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockTaskList.slice(start, end)
  
  return {
    total: mockTaskList.length,
    list,
  }
}

/**
 * 模拟分页查询我的任务列表（过滤已完成）
 */
export function mockGetMyTasks(
  pageNo: number,
  pageSize: number
): PageResult<TaskVO> {
  const myTasks = mockTaskList.filter(task => task.status !== TaskStatus.COMPLETED)
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = myTasks.slice(start, end)
  
  return {
    total: myTasks.length,
    list,
  }
}
