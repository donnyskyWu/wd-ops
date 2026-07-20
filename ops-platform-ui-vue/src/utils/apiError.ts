/**
 * 从 API 异常中提取可读消息。
 * ops-platform-ui-vue axios 拦截器 reject Error(msg)；Football requestClient reject { code, msg }。
 */
export function extractApiErrorMessage(e: unknown, fallback: string): string {
  if (typeof e === 'string' && e.trim()) {
    return e.trim()
  }
  if (e instanceof Error && e.message && e.message !== 'Network Error') {
    return e.message
  }
  const resp = e as {
    msg?: string
    message?: string
    error?: string
    data?: { msg?: string; message?: string; error?: string }
    response?: { data?: { msg?: string; message?: string; error?: string } }
  }
  return (
    resp?.msg ??
    resp?.message ??
    resp?.error ??
    resp?.data?.msg ??
    resp?.data?.message ??
    resp?.data?.error ??
    resp?.response?.data?.msg ??
    resp?.response?.data?.message ??
    resp?.response?.data?.error ??
    fallback
  )
}
