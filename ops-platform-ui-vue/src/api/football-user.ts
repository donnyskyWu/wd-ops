/**
 * Football system-server 用户 API（ADR-049 D4：身份 SSOT = system_users）
 * 经 Gateway /admin-api/system/** → system-server（非 oa-server）
 */
import { request } from '@/utils/request'

export interface FootballSystemUserVO {
  id: number
  username?: string
  nickname: string
  deptId?: number
  deptName?: string
  mobile?: string
  status?: number
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
  simpleListPromise = request
    .get<FootballSystemUserVO[]>({
      url: '/system/user/simple-list',
      headers: { 'tenant-id': '1' },
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

export function clearSystemUserSimpleListCache() {
  simpleListCache = null
}
