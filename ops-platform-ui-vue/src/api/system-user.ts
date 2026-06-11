/**
 * M9 S-01 用户/角色/权限 API
 * 契约: /admin-api/oa/system/user|role|permission/*（S-R23 规范路径；旧 /system/* 仍兼容）
 */
import { request } from '@/utils/request'

export interface UserVO {
  id: number
  username: string
  nickname: string
  email?: string
  phoneMasked?: string
  position?: string
  ipGroupId?: number
  deptId?: number
  deptName?: string
  dingUserId?: string
  status: string
  remark?: string
  roleIds?: number[]
  roleNames?: string[]
  createTime?: string
}

export interface UserPageResult {
  list: UserVO[]
  total: number
}

export interface RoleVO {
  id: number
  code: string
  name: string
  status?: string
  remark?: string
  permissionIds?: number[]
  permissionCodes?: string[]
  createTime?: string
}

export interface RolePageResult {
  list: RoleVO[]
  total: number
}

export interface PermissionVO {
  id: number
  code: string
  name: string
  module?: string
}

export function fetchUserList(params: {
  username?: string
  nickname?: string
  roleId?: number
  deptId?: number
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<UserPageResult>({ url: '/oa/system/user/list', params })
}

export function createUser(data: {
  username: string
  nickname: string
  email?: string
  phone?: string
  position?: string
  ipGroupId?: number
  deptId?: number
  status?: string
  roleIds: number[]
  remark?: string
}) {
  return request.post<number>({ url: '/oa/system/user/create', data })
}

export function updateUser(data: {
  id: number
  nickname?: string
  email?: string
  phone?: string
  position?: string
  ipGroupId?: number
  deptId?: number
  status?: string
  roleIds?: number[]
  remark?: string
}) {
  return request.put<boolean>({ url: '/oa/system/user/update', data })
}

export function deleteUser(id: number) {
  return request.delete<boolean>({ url: '/oa/system/user/delete', params: { id } })
}

export function fetchRoleList(params?: { name?: string; code?: string; pageNo?: number; pageSize?: number }) {
  return request.get<RolePageResult>({ url: '/oa/system/role/list', params: { pageNo: 1, pageSize: 100, ...params } })
}

export function createRole(data: { code: string; name: string; remark?: string }) {
  return request.post<number>({ url: '/oa/system/role/create', data })
}

export function updateRole(data: { id: number; name?: string; remark?: string }) {
  return request.put<boolean>({ url: '/oa/system/role/update', data })
}

export function deleteRole(id: number) {
  return request.delete<boolean>({ url: '/oa/system/role/delete', params: { id } })
}

export function assignRolePermission(data: { roleId: number; permissionIds: number[] }) {
  return request.post<boolean>({ url: '/oa/system/role/assign-permission', data })
}

export function fetchRolePermissions(roleId: number) {
  return request.get<PermissionVO[]>({ url: '/oa/system/role/permissions', params: { roleId } })
}

export function fetchPermissionList() {
  return request.get<PermissionVO[]>({ url: '/oa/system/permission/list' })
}
