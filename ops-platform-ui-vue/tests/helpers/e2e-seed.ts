import type { APIRequestContext } from '@playwright/test'
import { OA_API_BASE, OA_AUTH_HEADERS } from './integration-api'

/** E2E dataflow seed (V57) trace identifiers */
export const E2E = {
  trace: 'E2E-DF-20260611',
  accountId: 91001,
  accountName: 'E2E-DF-视频号主号',
  ipGroupId: 92001,
  authorId: 93001,
  followerMin: 2_000_000,
  workTitlePrefix: 'E2E-DF',
  platformType: 'WECHAT_VIDEO',
} as const

type ApiResult<T = unknown> = { code: number; data: T; msg?: string }

/** Returns true when backend is up and E2E seed account is present. */
export async function isE2eSeedReady(request: APIRequestContext): Promise<boolean> {
  try {
    const res = await request.get(`${OA_API_BASE}/oa/content-analysis/list`, {
      headers: OA_AUTH_HEADERS,
      params: { accountId: E2E.accountId, pageNo: 1, pageSize: 5 },
    })
    if (!res.ok()) return false
    const json = (await res.json()) as ApiResult<{ list?: { title?: string }[] }>
    if (json.code !== 0) return false
    return (json.data?.list ?? []).some((row) => row.title?.includes(E2E.workTitlePrefix))
  } catch {
    return false
  }
}

export async function fetchE2eFollowerCount(request: APIRequestContext): Promise<number> {
  const res = await request.get(`${OA_API_BASE}/oa/account-analysis/${E2E.accountId}/followers`, {
    headers: OA_AUTH_HEADERS,
    params: { pageNo: 1, pageSize: 1 },
  })
  if (!res.ok()) throw new Error(`followers HTTP ${res.status()}`)
  const json = (await res.json()) as ApiResult<
    { followerCount?: number }[] | { list?: { followerCount?: number }[] }
  >
  if (json.code !== 0) throw new Error(`followers code=${json.code}`)
  const rows = Array.isArray(json.data) ? json.data : json.data?.list ?? []
  return rows[0]?.followerCount ?? 0
}
