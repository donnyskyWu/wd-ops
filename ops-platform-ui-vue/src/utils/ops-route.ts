import type { RouteLocationRaw } from 'vue-router'

/** Football shell uses hash routes under `/ops/…`; standalone Ops uses flat paths like `/sop`. */
export function isFootballOpsShell(): boolean {
  if (typeof window === 'undefined') return false
  const hashPath = window.location.hash.replace(/^#/, '').split('?')[0]
  const path = hashPath || window.location.pathname
  return path === '/ops' || path.startsWith('/ops/')
}

/** Prefix standalone Ops path with `/ops` when embedded in Football. */
export function opsPath(standalonePath: string): string {
  if (!standalonePath) return standalonePath
  const normalized = standalonePath.startsWith('/') ? standalonePath : `/${standalonePath}`
  if (normalized.startsWith('/ops/') || normalized === '/ops') {
    return normalized
  }
  if (!isFootballOpsShell()) {
    return normalized
  }
  return `/ops${normalized}`
}

/** Normalize router target for standalone vs Football shell. Named routes pass through unchanged. */
export function opsRouteTo(to: RouteLocationRaw): RouteLocationRaw {
  if (typeof to === 'string') {
    return opsPath(to)
  }
  if ('path' in to && typeof to.path === 'string' && to.path) {
    return { ...to, path: opsPath(to.path) }
  }
  return to
}

/** Football 作者信息 SSOT（system_menu 5071） */
export const FOOTBALL_AUTHOR_INFO = '/author/info'

/** Football Admin SSOT（用户/角色/租户等 — D-DEDUP-01 不下沉 OPS） */
export const FOOTBALL_ADMIN_USER = '/system/user'
export const FOOTBALL_ADMIN_ROLE = '/system/role'
export const FOOTBALL_ADMIN_TENANT = '/system/tenant'

/** 跳转 Football 作者信息页（离开 OPS 壳） */
export function navigateToFootballAuthorInfo(): void {
  if (typeof window === 'undefined') return
  window.location.hash = `#${FOOTBALL_AUTHOR_INFO}`
}

/** 跳转 Football Admin 原生页（离开 OPS 壳） */
export function navigateToFootballAdmin(path: string): void {
  if (typeof window === 'undefined') return
  const normalized = path.startsWith('/') ? path : `/${path}`
  window.location.hash = `#${normalized}`
}

/** 是否 Football 原生路由（非 /ops 子应用） */
export function isFootballNativePath(path: string): boolean {
  return path === FOOTBALL_AUTHOR_INFO || path.startsWith(`${FOOTBALL_AUTHOR_INFO}/`)
}

/**
 * Resolve backend /ops/… action URLs for in-app navigation.
 * Standalone shell strips `/ops`; Football shell keeps or adds the prefix.
 * Football 原生路由（如 /author/info）通过 hash 跳转，返回 dashboard 作 router 占位。
 */
export function resolveOpsNavUrl(url: string): string {
  if (!url) {
    return isFootballOpsShell() ? '/ops/dashboard' : '/dashboard'
  }
  const [pathOnly, query = ''] = url.split('?')

  if (
    pathOnly === '/author/info'
    || pathOnly === '/ops/author'
    || pathOnly === '/author'
    || isFootballNativePath(pathOnly)
  ) {
    const target = query ? `${FOOTBALL_AUTHOR_INFO}?${query}` : FOOTBALL_AUTHOR_INFO
    if (typeof window !== 'undefined') {
      window.location.hash = `#${target}`
    }
    return isFootballOpsShell() ? '/ops/dashboard' : '/dashboard'
  }

  if (isFootballOpsShell()) {
    const resolved = opsPath(pathOnly.startsWith('/ops/') ? pathOnly : pathOnly)
    return query ? `${resolved}?${query}` : resolved
  }

  // Phase A：平行系统管理 URL → Football Admin（不再落到 OPS deprecated 页）
  if (
    pathOnly === '/ops/system/user'
    || pathOnly === '/system-user'
    || pathOnly.startsWith('/ops/system/user/')
  ) {
    const target = query ? `${FOOTBALL_ADMIN_USER}?${query}` : FOOTBALL_ADMIN_USER
    if (typeof window !== 'undefined') {
      window.location.hash = `#${target}`
    }
    return isFootballOpsShell() ? '/ops/dashboard' : '/dashboard'
  }
  if (
    pathOnly === '/ops/system/role'
    || pathOnly === '/system-role'
    || pathOnly.startsWith('/ops/system/role/')
  ) {
    const target = query ? `${FOOTBALL_ADMIN_ROLE}?${query}` : FOOTBALL_ADMIN_ROLE
    if (typeof window !== 'undefined') {
      window.location.hash = `#${target}`
    }
    return isFootballOpsShell() ? '/ops/dashboard' : '/dashboard'
  }
  if (
    pathOnly === '/ops/system/tenant'
    || pathOnly === '/system-tenant'
    || pathOnly.startsWith('/ops/system/tenant/')
  ) {
    const target = query ? `${FOOTBALL_ADMIN_TENANT}?${query}` : FOOTBALL_ADMIN_TENANT
    if (typeof window !== 'undefined') {
      window.location.hash = `#${target}`
    }
    return isFootballOpsShell() ? '/ops/dashboard' : '/dashboard'
  }

  const map: Record<string, string> = {
    '/ops/ip-group': '/ip-group',
    '/ops/author': FOOTBALL_AUTHOR_INFO,
    '/ops/account': '/internal-account',
    '/ops/internal-content': '/internal-content',
    '/ops/sop': '/sop',
    '/ops/perf': '/perf-template',
    '/ops/report': '/data-report',
    '/ops/workbench/todos': '/workbench-todos',
  }
  let resolved = pathOnly
  for (const [from, to] of Object.entries(map)) {
    if (resolved === from || resolved.startsWith(`${from}/`)) {
      resolved = resolved.replace(from, to)
      break
    }
  }
  if (resolved.startsWith('/ops/')) {
    resolved = resolved.replace('/ops/', '/')
  }
  const listFallback: Record<string, string> = {
    '/sop/review': '/sop/review',
    '/content/review': '/content/review',
  }
  for (const [prefix, target] of Object.entries(listFallback)) {
    if (resolved.startsWith(`${prefix}/`)) {
      resolved = target
      break
    }
  }
  return query ? `${resolved}?${query}` : resolved
}
