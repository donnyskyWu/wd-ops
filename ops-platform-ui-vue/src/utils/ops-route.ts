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

/**
 * Resolve backend /ops/… action URLs for in-app navigation.
 * Standalone shell strips `/ops`; Football shell keeps or adds the prefix.
 */
export function resolveOpsNavUrl(url: string): string {
  if (!url) {
    return isFootballOpsShell() ? '/ops/dashboard' : '/dashboard'
  }
  const [pathOnly, query = ''] = url.split('?')

  if (isFootballOpsShell()) {
    const resolved = opsPath(pathOnly.startsWith('/ops/') ? pathOnly : pathOnly)
    return query ? `${resolved}?${query}` : resolved
  }

  const map: Record<string, string> = {
    '/ops/ip-group': '/ip-group',
    '/ops/author': '/author',
    '/ops/account': '/internal-account',
    '/ops/internal-content': '/internal-content',
    '/ops/sop': '/sop',
    '/ops/perf': '/perf-template',
    '/ops/report': '/data-report',
    '/ops/system/user': '/system-user',
    '/ops/system/tenant': '/system-tenant',
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
