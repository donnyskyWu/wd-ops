/** Auth query params for browser img src (DevAuthFilter file GET fallback). */

export function getFileAuthParams(): { token: string; tenantId: string } {
  const token = import.meta.env.VITE_API_TOKEN || localStorage.getItem('token') || ''
  const tenantId = import.meta.env.VITE_TENANT_ID || localStorage.getItem('tenantId') || '1'
  return { token, tenantId }
}

export function isOaFileUrl(url: string): boolean {
  return /\/oa\/file\/(?:view|download)/.test(url)
}

export function appendFileAuth(url: string): string {
  if (!url || url.startsWith('data:') || url.startsWith('blob:') || !isOaFileUrl(url)) {
    return url
  }
  if (/[?&]token=/i.test(url)) {
    return url
  }
  const { token, tenantId } = getFileAuthParams()
  if (!token) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}token=${encodeURIComponent(token)}&tenantId=${encodeURIComponent(tenantId)}`
}

export function stripFileAuthFromUrl(url: string): string {
  if (!url) return url
  const qIndex = url.indexOf('?')
  if (qIndex < 0) return url
  const path = url.slice(0, qIndex)
  const query = url.slice(qIndex + 1)
  const kept = query
    .split('&')
    .filter((part) => part && !/^token=/i.test(part) && !/^tenantId=/i.test(part))
  return kept.length ? `${path}?${kept.join('&')}` : path
}

/** Strip auth tokens from img src in HTML before persisting. */
export function stripFileAuthFromHtml(html: string): string {
  if (!html) return ''
  return html.replace(/(<img[^>]+src=["'])([^"']+)(["'])/gi, (_m, pre, src, post) => {
    return `${pre}${stripFileAuthFromUrl(src)}${post}`
  })
}

/** Append auth tokens to img src for in-editor display. */
export function appendFileAuthToHtml(html: string): string {
  if (!html) return ''
  return html.replace(/(<img[^>]+src=["'])([^"']+)(["'])/gi, (_m, pre, src, post) => {
    return `${pre}${appendFileAuth(src)}${post}`
  })
}
