/**
 * M9 S-02 租户管理 API
 * 契约: /admin-api/oa/system/tenant/*（S-R23 规范路径）
 */
import { request } from '@/utils/request'

export interface TenantVO {
  id: number
  tenantName: string
  contactName: string
  contactPhone: string
  contactEmail?: string
  expireTime?: string
  maxAccounts?: number
  accountCount?: number
  status: string
  remark?: string
  createTime?: string
}

export interface TenantPageResult {
  list: TenantVO[]
  total: number
}

export function fetchTenantList(params: {
  tenantName?: string
  contactName?: string
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<TenantPageResult>({ url: '/oa/system/tenant/list', params })
}

export function createTenant(data: {
  tenantName: string
  contactName: string
  contactPhone: string
  contactEmail?: string
  expireTime: string
  maxAccounts?: number
  status?: string
  remark?: string
}) {
  return request.post<number>({ url: '/oa/system/tenant/create', data })
}

export function updateTenant(data: {
  id: number
  tenantName?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  expireTime?: string
  maxAccounts?: number
  status?: string
  remark?: string
}) {
  return request.put<boolean>({ url: '/oa/system/tenant/update', data })
}

export function deleteTenant(id: number) {
  return request.delete<boolean>({ url: '/oa/system/tenant/delete', params: { id } })
}

export const TENANT_STATUS_LABEL: Record<string, string> = {
  NORMAL: '正常',
  TRIAL: '试用',
  EXPIRED: '已到期',
  DISABLED: '已停用',
}

export const TENANT_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  NORMAL: 'success',
  TRIAL: 'info',
  EXPIRED: 'warning',
  DISABLED: 'danger',
}
