import { unwrapApiData, pickListPage } from '@/utils'

export const MONITOR_PLATFORM_TAB: Record<string, string> = {
  douyin: 'DOUYIN',
  wechat: 'WECHAT_OFFICIAL',
  channels: 'WECHAT_VIDEO',
}

export interface MonitorWorkRow {
  id: number
  accountId: number
  accountName: string
  platform: string
  title: string
  playCount: number
  likeCount: number
  completionRate?: number
  publishTime?: string
  industry?: string
  rank?: number
}

export function pickMonitorPage(res: unknown) {
  return pickListPage<MonitorWorkRow>(unwrapApiData(res) as { list?: MonitorWorkRow[]; records?: MonitorWorkRow[]; total?: number })
}

export function mapExternalWork(raw: Record<string, unknown>, index?: number): MonitorWorkRow {
  const accountId = Number(raw.accountId ?? raw.account_id ?? 0)
  return {
    id: Number(raw.id ?? 0),
    accountId,
    accountName: String(raw.accountName ?? raw.account_name ?? `账号 #${accountId}`),
    platform: String(raw.platformType ?? raw.platform_type ?? '-'),
    title: String(raw.title ?? '-'),
    playCount: Number(raw.playCount ?? raw.play_count ?? 0),
    likeCount: Number(raw.likeCount ?? raw.like_count ?? 0),
    completionRate: raw.completionRate != null ? Number(raw.completionRate) : undefined,
    publishTime: raw.publishTime != null ? String(raw.publishTime) : undefined,
    industry: raw.industry != null ? String(raw.industry) : undefined,
    rank: index != null ? index + 1 : undefined,
  }
}

/** 外部作品按账号聚合（外部账号分析页） */
export function aggregateWorksByAccount(works: MonitorWorkRow[]) {
  const map = new Map<number, {
    accountId: number
    accountName: string
    platform: string
    followerCount: number
    likeCount: number
    contentCount: number
    avgViews: number
    engagement: number
    readingCount?: number
    views?: number
    avgReading?: number
  }>()

  for (const w of works) {
    const cur = map.get(w.accountId) ?? {
      accountId: w.accountId,
      accountName: w.accountName,
      platform: w.platform,
      followerCount: 0,
      likeCount: 0,
      contentCount: 0,
      avgViews: 0,
      engagement: 0,
    }
    cur.contentCount += 1
    cur.likeCount += w.likeCount
    cur.followerCount = Math.max(cur.followerCount, w.playCount)
    cur.views = (cur.views ?? 0) + w.playCount
    cur.readingCount = (cur.readingCount ?? 0) + w.playCount
    map.set(w.accountId, cur)
  }

  return Array.from(map.values()).map((row) => {
    const totalViews = row.views ?? 0
    const avg = row.contentCount > 0 ? Math.round(totalViews / row.contentCount) : 0
    const engagement = totalViews > 0 ? Math.round((row.likeCount / totalViews) * 10000) / 100 : 0
    return {
      ...row,
      avgViews: avg,
      avgReading: avg,
      engagement: Number.isFinite(engagement) ? engagement : 0,
    }
  })
}

export function buildMonitorQuery(
  platformTab: string,
  extra: Record<string, unknown> = {},
  pageNum = 1,
  pageSize = 50,
) {
  const q: Record<string, unknown> = { pageNum, pageSize, ...extra }
  const pt = MONITOR_PLATFORM_TAB[platformTab]
  if (pt) q.platformType = pt
  return q
}
