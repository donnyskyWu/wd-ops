import { request } from '@/utils/request'

export interface MatchVO {
  scheduleId: string
  displayName: string
  sClassId?: string
  sClassName?: string
  homeTeamName?: string
  guestTeamName?: string
  matchTime?: string
  matchTimeRaw?: number
  lotteryType?: string
}

export interface MatchLeagueVO {
  id: string
  name: string
  nameEn?: string
  shortName?: string
}

export interface MatchPageResult {
  list: MatchVO[]
  total: number
}

/** 计划/任务侧 competitionId 映射为外部 scheduleId */
export function toCompetitionId(match: MatchVO): string {
  return match.scheduleId
}

export function toCompetitionName(match: MatchVO): string {
  return match.displayName
}

export function getMatchPage(params: {
  date?: string
  pageNo?: number
  pageSize?: number
  leagueId?: string
  teamKeyword?: string
  lotteryType?: string
}): Promise<MatchPageResult> {
  return request.get({ url: '/oa/match/list', params })
}

export function getMatchLeagues(): Promise<MatchLeagueVO[]> {
  return request.get({ url: '/oa/match/leagues' })
}

export const LOTTERY_TYPE_LABELS: Record<string, string> = {
  jc: '竞彩',
}

export function lotteryTypeLabel(value?: string): string {
  if (!value) return '--'
  return LOTTERY_TYPE_LABELS[value] ?? value
}
