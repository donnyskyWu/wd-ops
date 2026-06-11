/**
 * M9 部门 + 钉钉同步 API
 */
import { request } from '@/utils/request'

export interface DeptTreeNode {
  id: number
  parentId?: number
  name: string
  dingDeptId?: number
  sort?: number
  status?: string
  children?: DeptTreeNode[]
}

export interface DingTalkSyncResult {
  created: number
  updated: number
  skipped: number
}

export function fetchDeptTree() {
  return request.get<DeptTreeNode[]>({ url: '/oa/system/dept/tree' })
}

export function createDept(data: { parentId?: number; name: string; sort?: number; status?: string }) {
  return request.post<number>({ url: '/oa/system/dept/create', data })
}

export function updateDept(data: { id: number; parentId?: number; name?: string; sort?: number; status?: string }) {
  return request.put<boolean>({ url: '/oa/system/dept/update', data })
}

export function deleteDept(id: number) {
  return request.delete<boolean>({ url: '/oa/system/dept/delete', params: { id } })
}

export function syncDingTalkDepts() {
  return request.post<DingTalkSyncResult>({ url: '/oa/system/dept/sync-dingtalk' })
}

export function syncDingTalkUsers() {
  return request.post<DingTalkSyncResult>({ url: '/oa/system/dept/sync-dingtalk-users' })
}
