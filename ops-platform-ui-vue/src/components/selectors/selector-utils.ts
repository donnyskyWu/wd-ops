/** 强关联选择器共用工具 */

export const PLATFORM_LABELS: Record<string, string> = {
  WECHAT_OFFICIAL: '公众号',
  WECHAT_MP: '公众号',
  WECHAT_VIDEO: '视频号',
  VIDEO_ACCOUNT: '视频号',
  DOUYIN: '抖音',
  KUAISHOU: '快手',
  XIAOHONGSHU: '小红书',
  WECHAT_WORK: '企微',
  SERVICE_ACCOUNT: '服务号',
}

export const OPERATOR_LABELS: Record<string, string> = {
  MOBILE: '中国移动',
  UNICOM: '中国联通',
  TELECOM: '中国电信',
}

export function platformLabel(type?: string): string {
  if (!type) return '-'
  return PLATFORM_LABELS[type] || type
}

export function operatorLabel(op?: string): string {
  if (!op) return '-'
  return OPERATOR_LABELS[op] || op
}

/** 远端搜索防抖（默认 200ms） */
export function debounceSearch(fn: (kw: string) => void, ms = 200) {
  let timer: ReturnType<typeof setTimeout> | null = null
  return (kw: string) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(kw), ms)
  }
}

type BoundItem = { id: number; accountBoundCount?: number; totalLinkedAccounts?: number }

/**
 * 默认过滤已绑定实体；编辑时保留当前选中项。
 */
export function applyExcludeBound<T extends BoundItem>(
  list: T[],
  excludeBound: boolean,
  currentValue?: number | number[],
): T[] {
  if (!excludeBound) return list
  const keepIds = new Set(
    Array.isArray(currentValue) ? currentValue : currentValue != null ? [currentValue] : [],
  )
  return list.filter((item) => {
    if (keepIds.has(item.id)) return true
    const bound = item.accountBoundCount ?? item.totalLinkedAccounts ?? 0
    return bound <= 0
  })
}

export function useMockFallback<T>(list: T[], mockList: T[]): T[] {
  if (list.length) return list
  return import.meta.env.VITE_USE_MOCK === 'true' ? mockList : []
}
