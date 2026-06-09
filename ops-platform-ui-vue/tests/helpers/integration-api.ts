import type { APIRequestContext } from '@playwright/test'

/** 直连后端（联调前置：8080 dev 已启动） */
export const OA_API_BASE = process.env.OA_API_BASE ?? 'http://localhost:8080/admin-api'

export const OA_AUTH_HEADERS = {
  Authorization: 'Bearer dev-token-oa-admin',
  'X-Tenant-Id': '1',
  'Content-Type': 'application/json',
}

export interface Sl05Fixture {
  companyAName: string
  companyBName: string
  realName: string
}

const SL05 = {
  companyAName: 'INT-SL05-公司A',
  companyBName: 'INT-SL05-公司B',
  companyACredit: '91110000MA0SL05A01',
  companyBCredit: '91110000MA0SL05B01',
  realName: 'INT-SL05-实名人',
  idCard: '330101199001011234',
  realPhone: '13800138050',
  phoneCode: 'INT-SL05-PH',
  phoneNumber: '13800138051',
} as const

type ApiResult<T = unknown> = { code: number; data: T; msg?: string }

async function apiGet<T>(request: APIRequestContext, path: string, params?: Record<string, string | number>) {
  const res = await request.get(`${OA_API_BASE}${path}`, {
    headers: OA_AUTH_HEADERS,
    params,
  })
  if (!res.ok()) {
    throw new Error(`GET ${path} HTTP ${res.status()}`)
  }
  const json = (await res.json()) as ApiResult<T>
  if (json.code !== 0) {
    throw new Error(`GET ${path} code=${json.code} msg=${json.msg}`)
  }
  return json.data
}

async function apiPost<T>(request: APIRequestContext, path: string, data: unknown) {
  const res = await request.post(`${OA_API_BASE}${path}`, {
    headers: OA_AUTH_HEADERS,
    data,
  })
  if (!res.ok()) {
    throw new Error(`POST ${path} HTTP ${res.status()}`)
  }
  const json = (await res.json()) as ApiResult<T>
  if (json.code !== 0) {
    throw new Error(`POST ${path} code=${json.code} msg=${json.msg}`)
  }
  return json.data
}

async function findCompanyId(request: APIRequestContext, name: string): Promise<number | null> {
  const data = await apiGet<{ list: { id: number; companyName: string }[] }>(request, '/oa/company/list', {
    companyName: name,
    pageSize: 10,
  })
  const hit = data.list?.find((c) => c.companyName === name)
  return hit?.id ?? null
}

async function ensureCompany(request: APIRequestContext, name: string, creditCode: string): Promise<number> {
  const existing = await findCompanyId(request, name)
  if (existing) return existing
  return (await apiPost<number>(request, '/oa/company/create', {
    companyName: name,
    creditCode,
    mpCapacityStandard: 10,
    status: 'ENABLED',
  })) as number
}

async function findRealnameId(request: APIRequestContext, companyId: number, name: string): Promise<number | null> {
  const data = await apiGet<{ list: { id: number; realName: string }[] }>(request, '/oa/realname/list', {
    companyId,
    realName: name,
    pageSize: 10,
  })
  const hit = data.list?.find((r) => r.realName === name)
  return hit?.id ?? null
}

async function ensureRealname(request: APIRequestContext, companyId: number): Promise<number> {
  const existing = await findRealnameId(request, companyId, SL05.realName)
  if (existing) return existing
  return (await apiPost<number>(request, '/oa/realname/create', {
    realName: SL05.realName,
    idType: 'ID_CARD',
    idCard: SL05.idCard,
    phone: SL05.realPhone,
    companyId,
    status: 'ENABLED',
  })) as number
}

async function ensurePhone(request: APIRequestContext, realnameId: number): Promise<void> {
  const data = await apiGet<{ list: { id: number; phoneCode?: string }[] }>(request, '/oa/phone/list', {
    realnameId,
    pageSize: 20,
  })
  if (data.list?.some((p) => p.phoneCode === SL05.phoneCode)) return
  await apiPost<number>(request, '/oa/phone/create', {
    phoneNumber: SL05.phoneNumber,
    phoneCode: SL05.phoneCode,
    phoneModel: 'INT-SL05 联调机',
    realnameId,
    keeperId: 1001,
    status: 'ENABLED',
  })
}

/**
 * 为 INT-SL05 准备：2 家公司 + 公司 A 下实名人 + 手机。
 * 幂等：已存在则跳过创建。
 */
export async function ensureSl05Fixture(request: APIRequestContext): Promise<Sl05Fixture> {
  const companyAId = await ensureCompany(request, SL05.companyAName, SL05.companyACredit)
  await ensureCompany(request, SL05.companyBName, SL05.companyBCredit)
  const realnameId = await ensureRealname(request, companyAId)
  await ensurePhone(request, realnameId)
  return {
    companyAName: SL05.companyAName,
    companyBName: SL05.companyBName,
    realName: SL05.realName,
  }
}
