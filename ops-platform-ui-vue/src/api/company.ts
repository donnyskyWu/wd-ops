/**
 * M4 S-01 公司管理 API
 * 契约: GET/POST/PUT/DELETE /admin-api/oa/company/*
 */
import { request } from '@/utils/request'

export interface CompanyVO {
  id: number
  companyName: string
  creditCode: string
  industry?: string
  address?: string
  legalName?: string
  legalIdCardMasked?: string
  mpCapacityStandard: number
  mpRegisteredCount: number
  mpRemaining?: number
  status: string
  businessLicenseKeys?: string[]
  businessLicenseUrls?: string[]
  createTime?: string
}

export interface CompanyPageResult {
  list: CompanyVO[]
  total: number
}

export interface CompanyCreateReq {
  companyName: string
  creditCode: string
  industry?: string
  address?: string
  legalName?: string
  legalIdCard?: string
  mpCapacityStandard?: number
  status?: string
  businessLicenseKeys?: string[]
}

export interface CompanyUpdateReq {
  id: number
  companyName?: string
  industry?: string
  address?: string
  legalName?: string
  legalIdCard?: string
  mpCapacityStandard?: number
  status?: string
  businessLicenseKeys?: string[]
}

export interface CompanyExpandReq {
  newCapacity: number
  reason: string
}

export function getCompanyPage(params: {
  companyName?: string
  status?: string
  pageNo?: number
  pageSize?: number
}): Promise<CompanyPageResult> {
  return request.get({ url: '/oa/company/list', params })
}

export function getCompany(id: number): Promise<CompanyVO> {
  return request.get({ url: `/oa/company/${id}` })
}

export function createCompany(data: CompanyCreateReq): Promise<number> {
  return request.post({ url: '/oa/company/create', data })
}

export function updateCompany(data: CompanyUpdateReq): Promise<boolean> {
  return request.put({ url: '/oa/company/update', data })
}

export function deleteCompany(id: number): Promise<boolean> {
  return request.delete({ url: '/oa/company/delete', params: { id } })
}

export function expandCompany(id: number, data: CompanyExpandReq): Promise<boolean> {
  return request.post({ url: `/oa/company/${id}/expand`, data })
}
