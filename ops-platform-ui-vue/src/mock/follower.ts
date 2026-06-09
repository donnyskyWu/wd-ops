/**
 * 粉丝分析 - Mock数据
 */

import type {
  FollowerStats,
  FollowerTrendPoint,
  FollowerDetailVO,
  PageResult,
} from '@/types/follower'
import { TimeDimension } from '@/types/follower'

/**
 * Mock粉丝统计数据
 */
export const mockFollowerStats: FollowerStats = {
  totalFollowers: 125000,
  newFollowers: 1500,
  unfollowers: 300,
  netFollowers: 1200,
  growthRate: 0.96,
}

/**
 * Mock粉丝趋势数据
 */
export const mockFollowerTrend: FollowerTrendPoint[] = mockGetFollowerTrend()

export function mockGetFollowerTrend(): FollowerTrendPoint[] {
  const data: FollowerTrendPoint[] = []
  let baseCount = 120000
  
  // 一周数据要有波动起伏
  const weeklyFluctuation = [
    { day: 6, newBase: 120000 },      // 周一
    { day: 5, newBase: 120150 },      // 周二 - 涨
    { day: 4, newBase: 119980 },      // 周三 - 跌
    { day: 3, newBase: 120280 },      // 周四 - 涨
    { day: 2, newBase: 119850 },      // 周五 - 跌
    { day: 1, newBase: 120400 },      // 周六 - 大涨
    { day: 0, newBase: 120600 },      // 周日 - 继续涨
  ]
  
  for (const { day, newBase } of weeklyFluctuation) {
    const date = new Date()
    date.setDate(date.getDate() - day)
    const dateStr = date.toISOString().split('T')[0]
    
    // 每天的增减有一定随机波动
    const newFollowers = Math.round(600 + Math.random() * 400)  // 600-1000
    const unfollowers = Math.round(300 + Math.random() * 200)   // 300-500
    
    baseCount = newBase
    
    data.push({
      date: dateStr,
      totalFollowers: Math.round(baseCount + Math.random() * 50),
      newFollowers,
      unfollowers,
      netFollowers: newFollowers - unfollowers,
    })
  }
  
  return data
}

/**
 * Mock粉丝明细数据
 */
export const mockFollowerList: FollowerDetailVO[] = [
  {
    id: 1,
    date: '2026-05-28',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    totalFollowers: 125000,
    newFollowers: 520,
    unfollowers: 85,
    netFollowers: 435,
    growthRate: 0.35,
  },
  {
    id: 2,
    date: '2026-05-28',
    accountName: 'AI技术前沿',
    ipGroupName: 'B-1组',
    totalFollowers: 580000,
    newFollowers: 1200,
    unfollowers: 180,
    netFollowers: 1020,
    growthRate: 0.18,
  },
  {
    id: 3,
    date: '2026-05-28',
    accountName: '职场进阶指南',
    ipGroupName: 'A-2组',
    totalFollowers: 32000,
    newFollowers: 280,
    unfollowers: 45,
    netFollowers: 235,
    growthRate: 0.73,
  },
]

/**
 * 模拟分页查询粉丝明细
 */
export function mockGetFollowerList(
  pageNo: number,
  pageSize: number
): PageResult<FollowerDetailVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockFollowerList.slice(start, end)
  
  return {
    total: mockFollowerList.length,
    list,
  }
}
