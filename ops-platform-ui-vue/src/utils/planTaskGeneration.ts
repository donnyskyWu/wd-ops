/**
 * 计划任务生成：赛事 × SOP 节点 → 任务行（执行人/时间）
 */
import type { MatchVO } from '@/api/match'
import type { SopNodeVO } from '@/types/sop'
import type { IpGroupMemberVO } from '@/types/ip-group'

export interface PlanTaskPreviewRow {
  key: string
  planDate: string
  competitionId: string
  competitionName: string
  matchTimeRaw?: number
  nodeId: number
  nodeName: string
  nodeOrder: number
  executorRole: string
  assigneeId?: number
  assigneeFallback?: boolean
  positionWarning?: string
  scheduledStart?: string
  scheduledEnd?: string
}

export interface IpGroupContext {
  leaderId: number | null
  members: IpGroupMemberVO[]
  memberPositions: Set<string>
}

export function formatDateYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 默认今天起 7 天（含首尾） */
export function defaultPlanDateRange(): [string, string] {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  const end = new Date(start)
  end.setDate(end.getDate() + 6)
  return [formatDateYmd(start), formatDateYmd(end)]
}

export function enumeratePlanDays(startDate: string, endDate: string): string[] {
  if (!startDate || !endDate) return []
  const days: string[] = []
  const cur = new Date(`${startDate}T00:00:00`)
  const last = new Date(`${endDate}T00:00:00`)
  while (cur <= last) {
    days.push(formatDateYmd(cur))
    cur.setDate(cur.getDate() + 1)
  }
  return days
}

export function padDateTime(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}:${s}`
}

/** scheduledStart = 比赛开始 - 24h；scheduledEnd = 比赛开始 */
export function computeTaskSchedule(matchTimeRaw?: number, competitionName?: string, planDate?: string): {
  scheduledStart: string
  scheduledEnd: string
} {
  let matchStart: Date
  if (matchTimeRaw && matchTimeRaw > 0) {
    matchStart = new Date(matchTimeRaw)
  } else if (competitionName) {
    const tail = competitionName.slice(competitionName.lastIndexOf('-') + 1).trim()
    const parsed = tail.length >= 16 ? new Date(tail.replace(' ', 'T')) : null
    if (parsed && !Number.isNaN(parsed.getTime())) {
      matchStart = parsed
    } else {
      matchStart = new Date(`${planDate || formatDateYmd(new Date())}T20:00:00`)
    }
  } else {
    matchStart = new Date(`${planDate || formatDateYmd(new Date())}T20:00:00`)
  }
  const taskStart = new Date(matchStart.getTime() - 24 * 60 * 60 * 1000)
  return { scheduledStart: padDateTime(taskStart), scheduledEnd: padDateTime(matchStart) }
}

export function resolveAssigneeByRole(
  executorRole: string,
  ctx: IpGroupContext,
): { assigneeId: number; assigneeFallback: boolean; positionWarning?: string } {
  const role = executorRole?.trim()
  if (role && ctx.members.some((m) => m.position === role)) {
    const member = ctx.members.find((m) => m.position === role)!
    return { assigneeId: member.userId, assigneeFallback: false }
  }
  const warning = role && !ctx.memberPositions.has(role)
    ? `IP 组内无岗位「${role}」成员，已默认 IP 组长`
    : undefined
  const leaderMember = ctx.members.find((m) => m.isLeader)
  if (leaderMember) {
    return { assigneeId: leaderMember.userId, assigneeFallback: true, positionWarning: warning }
  }
  if (ctx.leaderId != null) {
    return { assigneeId: ctx.leaderId, assigneeFallback: true, positionWarning: warning }
  }
  if (ctx.members.length) {
    return { assigneeId: ctx.members[0].userId, assigneeFallback: true, positionWarning: warning }
  }
  throw new Error('IP 组无可用成员')
}

export function buildIpGroupContext(members: IpGroupMemberVO[], leaderId: number | null): IpGroupContext {
  const memberPositions = new Set(members.map((m) => m.position).filter(Boolean))
  return { leaderId, members, memberPositions }
}

export function generatePlanTasks(
  nodes: SopNodeVO[],
  matchesByDay: Record<string, MatchVO[]>,
  ctx: IpGroupContext,
  existing?: PlanTaskPreviewRow[],
): PlanTaskPreviewRow[] {
  const manual = new Map((existing || []).map((r) => [r.key, r]))
  const sortedNodes = [...nodes].sort((a, b) => a.nodeOrder - b.nodeOrder)
  const rows: PlanTaskPreviewRow[] = []

  for (const [planDate, matches] of Object.entries(matchesByDay)) {
    for (const match of matches) {
      for (const node of sortedNodes) {
        const key = `${planDate}:${match.scheduleId}:${node.id}`
        const prev = manual.get(key)
        const schedule = computeTaskSchedule(match.matchTimeRaw, match.displayName, planDate)
        const resolved = resolveAssigneeByRole(node.executorRole, ctx)
        rows.push({
          key,
          planDate,
          competitionId: match.scheduleId,
          competitionName: match.displayName,
          matchTimeRaw: match.matchTimeRaw,
          nodeId: node.id,
          nodeName: node.nodeName,
          nodeOrder: node.nodeOrder,
          executorRole: node.executorRole,
          assigneeId: prev?.assigneeId ?? resolved.assigneeId,
          assigneeFallback: prev ? prev.assigneeFallback : resolved.assigneeFallback,
          positionWarning: prev?.positionWarning ?? resolved.positionWarning,
          scheduledStart: prev?.scheduledStart ?? schedule.scheduledStart,
          scheduledEnd: prev?.scheduledEnd ?? schedule.scheduledEnd,
        })
      }
    }
  }

  rows.sort((a, b) => {
    const d = a.planDate.localeCompare(b.planDate)
    if (d !== 0) return d
    const c = a.competitionId.localeCompare(b.competitionId)
    if (c !== 0) return c
    return a.nodeOrder - b.nodeOrder
  })
  return rows
}

export function collectAllMatches(matchesByDay: Record<string, MatchVO[]>): MatchVO[] {
  const seen = new Set<string>()
  const list: MatchVO[] = []
  for (const matches of Object.values(matchesByDay)) {
    for (const m of matches) {
      if (!seen.has(m.scheduleId)) {
        seen.add(m.scheduleId)
        list.push(m)
      }
    }
  }
  return list
}

export function buildStepsFromTasks(tasks: PlanTaskPreviewRow[]) {
  const byNode = new Map<number, { competitionIds: Set<string>; assigneeId?: number; scheduledStart?: string; scheduledEnd?: string }>()
  for (const task of tasks) {
    let agg = byNode.get(task.nodeId)
    if (!agg) {
      agg = { competitionIds: new Set(), assigneeId: task.assigneeId, scheduledStart: task.scheduledStart, scheduledEnd: task.scheduledEnd }
      byNode.set(task.nodeId, agg)
    }
    agg.competitionIds.add(task.competitionId)
    if (!agg.assigneeId && task.assigneeId) agg.assigneeId = task.assigneeId
    if (task.scheduledStart && (!agg.scheduledStart || task.scheduledStart < agg.scheduledStart)) {
      agg.scheduledStart = task.scheduledStart
    }
    if (task.scheduledEnd && (!agg.scheduledEnd || task.scheduledEnd > agg.scheduledEnd)) {
      agg.scheduledEnd = task.scheduledEnd
    }
  }
  return Array.from(byNode.entries()).map(([nodeId, agg]) => ({
    nodeId,
    competitionIds: Array.from(agg.competitionIds),
    assigneeIds: agg.assigneeId ? [agg.assigneeId] : [],
    scheduledStart: agg.scheduledStart,
    scheduledEnd: agg.scheduledEnd,
  }))
}
