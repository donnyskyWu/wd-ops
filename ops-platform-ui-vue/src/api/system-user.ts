/**
 * M9 用户/角色 API（保留非管理调用）
 *
 * Phase A / D-DEDUP-01：平行用户/角色 CRUD 管理不下沉 OPS。
 * - 管理 UI → Football Admin `#/system/user|role|tenant`（FootballAdminRedirect）
 * - UserSelect → `@/api/football-user`（simple-list）
 * 本文件仅保留 Layout / ParamManage / ContentEditPanel 所需的 profile / 角色列表。
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

export function fetchUserProfile() {
  return request.get<UserVO>({ url: '/oa/system/user/profile' })
}

/** 系统参数等页面所需的角色下拉；非平行角色管理 CRUD */
export function fetchRoleList(params?: { name?: string; code?: string; pageNo?: number; pageSize?: number }) {
  return request.get<RolePageResult>({ url: '/oa/system/role/list', params: { pageNo: 1, pageSize: 100, ...params } })
}
