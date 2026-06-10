/**
 * 外部赛事 Mock API（Phase 2 前占位，ADR-011）
 */
export interface CompetitionVO {
  id: string
  name: string
  season: string
  status: string
}

const MOCK_COMPETITIONS: CompetitionVO[] = [
  { id: 'cmp-001', name: '2026 春季城市赛', season: '2026-S1', status: 'ACTIVE' },
  { id: 'cmp-002', name: '2026 夏季邀请赛', season: '2026-S2', status: 'ACTIVE' },
  { id: 'cmp-003', name: '2026 品牌联动赛', season: '2026-S2', status: 'UPCOMING' },
  { id: 'cmp-004', name: '2025 年终总决赛', season: '2025-FY', status: 'CLOSED' },
]

export function searchCompetitions(keyword?: string): Promise<CompetitionVO[]> {
  const kw = keyword?.trim().toLowerCase()
  const list = !kw
    ? MOCK_COMPETITIONS
    : MOCK_COMPETITIONS.filter(
        (item) =>
          item.name.toLowerCase().includes(kw) ||
          item.id.toLowerCase().includes(kw) ||
          item.season.toLowerCase().includes(kw),
      )
  return Promise.resolve(list.filter((item) => item.status !== 'CLOSED'))
}
