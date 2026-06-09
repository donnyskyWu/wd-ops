/**
 * M9 S-01 用户/角色/权限 API
 * 契约: /admin-api/system/user|role|permission/*
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
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<UserPageResult>({ url: '/system/user/list', params })
}

export function createUser(data: {
  username: string
  nickname: string
  email?: string
  phone?: string
  position?: string
  ipGroupId?: number
  status?: string
  roleIds: number[]
  remark?: string
}) {
  return request.post<number>({ url: '/system/user/create', data })
}

export function updateUser(data: {
  id: number
  nickname?: string
  email?: string
  phone?: string
  position?: string
  ipGroupId?: number
  status?: string
  roleIds?: number[]
  remark?: string
}) {
  return request.put<boolean>({ url: '/system/user/update', data })
}

export function deleteUser(id: number) {
  return request.delete<boolean>({ url: '/system/user/delete', params: { id } })
}

export function fetchRoleList(params?: { name?: string; code?: string; pageNo?: number; pageSize?: number }) {
  return request.get<RolePageResult>({ url: '/system/role/list', params: { pageNo: 1, pageSize: 100, ...params } })
}

export function createRole(data: { code: string; name: string; remark?: string }) {
  return request.post<number>({ url: '/system/role/create', data })
}

export function updateRole(data: { id: number; name?: string; remark?: string }) {
  return request.put<boolean>({ url: '/system/role/update', data })
}

export function deleteRole(id: number) {
  return request.delete<boolean>({ url: '/system/role/delete', params: { id } })
}

export function assignRolePermission(data: { roleId: number; permissionIds: number[] }) {
  return request.post<boolean>({ url: '/system/role/assign-permission', data })
}

export function fetchRolePermissions(roleId: number) {
  return request.get<PermissionVO[]>({ url: '/system/role/permissions', params: { roleId } })
}

export function fetchPermissionList() {
  return request.get<PermissionVO[]>({ url: '/system/permission/list' })
}
