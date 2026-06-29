import { describe, expect, it } from 'vitest'
import {
  buildIpGroupContext,
  computeTaskSchedule,
  defaultPlanDateRange,
  enumeratePlanDays,
  generatePlanTasks,
  resolveAssigneeByRole,
} from './planTaskGeneration'
import type { IpGroupMemberVO } from '@/types/ip-group'
import type { SopNodeVO } from '@/types/sop'
import type { MatchVO } from '@/api/match'

const members: IpGroupMemberVO[] = [
  { memberId: 1, userId: 1003, userName: '运营A', position: 'OPERATOR', positionText: '运营', isLeader: false, joinTime: '' },
]

describe('planTaskGeneration', () => {
  it('defaultPlanDateRange spans 7 days', () => {
    const [start, end] = defaultPlanDateRange()
    const days = enumeratePlanDays(start, end)
    expect(days).toHaveLength(7)
  })

  it('computeTaskSchedule subtracts 24h from match start', () => {
    const matchMs = new Date('2026-06-15T20:00:00').getTime()
    const { scheduledStart, scheduledEnd } = computeTaskSchedule(matchMs)
    expect(scheduledEnd).toBe('2026-06-15 20:00:00')
    expect(scheduledStart).toBe('2026-06-14 20:00:00')
  })

  it('resolveAssigneeByRole matches position then falls back to leader', () => {
    const ctx = buildIpGroupContext(members, 1002)
    expect(resolveAssigneeByRole('OPERATOR', ctx).assigneeId).toBe(1003)
    const fb = resolveAssigneeByRole('OPS_LEADER', ctx)
    expect(fb.assigneeId).toBe(1002)
    expect(fb.assigneeFallback).toBe(true)
    expect(fb.positionWarning).toContain('OPS_LEADER')
  })

  it('generatePlanTasks creates match × node rows', () => {
    const nodes: SopNodeVO[] = [
      { id: 1, templateId: 1, nodeName: 'N1', nodeOrder: 1, nodeType: 'NORMAL', executorRole: 'OPERATOR', needReview: 0 } as SopNodeVO,
      { id: 2, templateId: 1, nodeName: 'N2', nodeOrder: 2, nodeType: 'NORMAL', executorRole: 'OPS_LEADER', needReview: 0 } as SopNodeVO,
    ]
    const match: MatchVO = {
      scheduleId: 'm1',
      displayName: '英超-A VS B-2026-06-15 20:00',
      matchTimeRaw: new Date('2026-06-15T20:00:00').getTime(),
    }
    const ctx = buildIpGroupContext(members, 1002)
    const rows = generatePlanTasks(nodes, { '2026-06-15': [match] }, ctx)
    expect(rows).toHaveLength(2)
    expect(rows[0].assigneeId).toBe(1003)
    expect(rows[1].assigneeFallback).toBe(true)
  })
})
