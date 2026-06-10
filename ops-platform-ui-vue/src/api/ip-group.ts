/**
 * IP组管理 - API接口封装
 * 
 * 基于PRD v9.0和API接口契约文档
 */

import { request } from '@/utils/request'
import type {
  IpGroupTreeVO,
  IpGroupPageReqVO,
  IpGroupPageRespVO,
  IpGroupSaveReqVO,
  IpGroupDetailVO,
  IpGroupStatsVO,
  IpGroupMemberVO,
  IpGroupMemberSaveReqVO,
  IpGroupAccountVO,
  IpGroupAccountBindReqVO,
  IpGroupAnchorVO,
  IpGroupAnchorBindReqVO,
} from '@/types/ip-group'

// ==================== IP组 CRUD ====================

/**
 * 获取IP组树形结构
 */
export function getIpGroupTree(): Promise<IpGroupTreeVO[]> {
  return request.get<IpGroupTreeVO[]>({ url: '/oa/ip-group/tree' })
}

/**
 * 分页查询IP组列表
 */
export function getIpGroupPage(params: IpGroupPageReqVO): Promise<IpGroupPageRespVO> {
  return request.get<IpGroupPageRespVO>({ url: '/oa/ip-group/list', params })
}

/**
 * 新增IP组
 */
export function createIpGroup(data: IpGroupSaveReqVO): Promise<number> {
  return request.post<number>({ url: '/oa/ip-group/create', data })
}

/**
 * 修改IP组
 */
export function updateIpGroup(data: IpGroupSaveReqVO): Promise<boolean> {
  return request.put<boolean>({ url: '/oa/ip-group/update', data })
}

/**
 * 删除IP组
 */
export function deleteIpGroup(id: number): Promise<boolean> {
  return request.delete<boolean>({ url: '/oa/ip-group/delete', params: { id } })
}

/**
 * 切换IP组状态
 */
export function updateIpGroupStatus(id: number, status: 0 | 1): Promise<boolean> {
  return request.put<boolean>({ url: `/oa/ip-group/${id}/status`, data: { status } })
}

/**
 * 获取IP组统计信息
 */
export function getIpGroupStats(id: number): Promise<IpGroupStatsVO> {
  return request.get<IpGroupStatsVO>({ url: `/oa/ip-group/${id}/stats` })
}

/**
 * S-R12 B2/B3 修复：获取 IP 组详情（包含 sortOrder/remark 等 TreeVO 没有的字段）
 * 用于"编辑"对话框回填，避免 TreeVO 字段缺失
 */
export function getIpGroupDetail(id: number): Promise<IpGroupDetailVO> {
  return request.get<IpGroupDetailVO>({ url: `/oa/ip-group/${id}` })
}

// ==================== 成员管理 ====================

/**
 * 获取成员列表
 */
export function getIpGroupMembers(groupId: number): Promise<IpGroupMemberVO[]> {
  return request.get<IpGroupMemberVO[]>({ url: `/oa/ip-group/${groupId}/members` })
}

/**
 * 添加成员
 */
export function addIpGroupMember(groupId: number, data: IpGroupMemberSaveReqVO): Promise<boolean> {
  return request.post<boolean>({ url: `/oa/ip-group/${groupId}/members`, data })
}

/**
 * 修改成员
 */
export function updateIpGroupMember(
  groupId: number,
  memberId: number,
  data: Partial<IpGroupMemberSaveReqVO>,
): Promise<boolean> {
  return request.put<boolean>({ url: `/oa/ip-group/${groupId}/members/${memberId}`, data })
}

/**
 * 删除成员
 */
export function deleteIpGroupMember(groupId: number, memberId: number): Promise<boolean> {
  return request.delete<boolean>({ url: `/oa/ip-group/${groupId}/members/${memberId}` })
}

// ==================== 关联账号 ====================

/**
 * 获取关联账号列表
 */
export function getIpGroupAccounts(groupId: number): Promise<IpGroupAccountVO[]> {
  return request.get<IpGroupAccountVO[]>({ url: `/oa/ip-group/${groupId}/accounts` })
}

/**
 * 关联账号
 */
export function bindIpGroupAccounts(
  groupId: number,
  data: IpGroupAccountBindReqVO,
): Promise<boolean> {
  return request.post<boolean>({ url: `/oa/ip-group/${groupId}/accounts`, data })
}

/**
 * 移除关联账号
 */
export function unbindIpGroupAccount(groupId: number, accountId: number): Promise<boolean> {
  return request.delete<boolean>({ url: `/oa/ip-group/${groupId}/accounts/${accountId}` })
}

// ==================== 关联主播 ====================

/**
 * 获取关联主播列表
 */
export function getIpGroupAnchors(groupId: number): Promise<IpGroupAnchorVO[]> {
  return request.get<IpGroupAnchorVO[]>({ url: `/oa/ip-group/${groupId}/anchors` })
}

/**
 * 关联主播
 */
export function bindIpGroupAnchors(
  groupId: number,
  data: IpGroupAnchorBindReqVO,
): Promise<boolean> {
  return request.post<boolean>({ url: `/oa/ip-group/${groupId}/anchors`, data })
}

/**
 * 移除关联主播
 */
export function unbindIpGroupAnchor(groupId: number, anchorUserId: number): Promise<boolean> {
  return request.delete<boolean>({ url: `/oa/ip-group/${groupId}/anchors/${anchorUserId}` })
}

export default {
  // IP组 CRUD
  getIpGroupTree,
  getIpGroupPage,
  createIpGroup,
  updateIpGroup,
  deleteIpGroup,
  updateIpGroupStatus,
  getIpGroupStats,
  getIpGroupDetail,
  // 成员管理
  getIpGroupMembers,
  addIpGroupMember,
  updateIpGroupMember,
  deleteIpGroupMember,
  // 关联账号
  getIpGroupAccounts,
  bindIpGroupAccounts,
  unbindIpGroupAccount,
  // 关联主播
  getIpGroupAnchors,
  bindIpGroupAnchors,
  unbindIpGroupAnchor,
}
