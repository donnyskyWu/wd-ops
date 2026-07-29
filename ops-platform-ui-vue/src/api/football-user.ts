/**
 * Football system-server 用户 API（ADR-049 D4：身份 SSOT = system_users）
 * 经 Gateway /admin-api/system/** → system-server（非 oa-server）
 */
import service from '@/utils/request'

export interface FootballSystemUserVO {
  id: number | string
  username?: string
  nickname: string
  deptId?: number
  deptName?: string
  mobile?: string
  status?: number
}

/** Preserve snowflake ids (>2^53) as strings before JSON.parse rounds them. */
function parseJsonPreserveLongIds(text: string): unknown {
  const preserved = text.replace(/"id"\s*:\s*(\d{16,})/g, '"id":"$1"')
  return JSON.parse(preserved)
}

function normalizeUserId(id: number | string | undefined | null): string {
  return id == null ? '' : String(id)
}

let simpleListCache: FootballSystemUserVO[] | null = null
let simpleListPromise: Promise<FootballSystemUserVO[]> | null = null

/** GET /admin-api/system/user/simple-list — Football Bearer + tenant-id */
export async function fetchSystemUserSimpleList(force = false): Promise<FootballSystemUserVO[]> {
  if (!force && simpleListCache) {
    return simpleListCache
  }
  if (!force && simpleListPromise) {
    return simpleListPromise
  }
  simpleListPromise = service
    .get('/system/user/simple-list', {
      headers: { 'tenant-id': '1' },
      responseType: 'text',
      transformResponse: [(data) => {
        if (typeof data !== 'string' || !data) {
          return data
        }
        try {
          const envelope = parseJsonPreserveLongIds(data) as {
            code?: number
            data?: FootballSystemUserVO[]
            msg?: string
          }
          if (envelope?.code !== 0 && envelope?.code !== 200) {
            return envelope
          }
          const list = (Array.isArray(envelope.data) ? envelope.data : []).map((u) => ({
            ...u,
            id: normalizeUserId(u.id),
          }))
          return { ...envelope, data: list }
        } catch {
          return data
        }
      }],
    })
    .then((list) => {
      simpleListCache = Array.isArray(list) ? list : []
      return simpleListCache
    })
    .finally(() => {
      simpleListPromise = null
    })
  return simpleListPromise
}

export function filterSystemUsers(
  users: FootballSystemUserVO[],
  opts?: { keyword?: string; deptId?: number; enabledOnly?: boolean },
): FootballSystemUserVO[] {
  const kw = opts?.keyword?.trim().toLowerCase()
  return users.filter((u) => {
    if (opts?.enabledOnly !== false && u.status != null && u.status !== 0) {
      return false
    }
    if (opts?.deptId != null && u.deptId !== opts.deptId) {
      return false
    }
    if (!kw) {
      return true
    }
    const nickname = (u.nickname || '').toLowerCase()
    const username = (u.username || '').toLowerCase()
    return nickname.includes(kw) || username.includes(kw)
  })
}

export { normalizeUserId }

export function clearSystemUserSimpleListCache() {
  simpleListCache = null
}
