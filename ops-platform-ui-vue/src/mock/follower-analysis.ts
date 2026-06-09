/**
 * 粉丝分析 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  FollowerAnalysisVO,
  PageResult,
  FollowerStatsVO,
  FollowerTrendVO,
} from '@/types/follower-analysis'

/**
 * Mock粉丝统计数据
 */
export const mockFollowerStats: FollowerStatsVO = {
  totalFollower: 125000,
  newFollower: 1500,
  unfollowCount: 300,
  netGrowth: 1200,
}

/**
 * Mock粉丝分析列表数据
 */
export const mockFollowerAnalysisList: FollowerAnalysisVO[] = [
  {
    timePeriod: '2026-05-01',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    followerCount: 12380,
    newFollower: 45,
    unfollowCount: 10,
    netGrowth: 35,
    growthRate: '0.28%',
  },
  {
    timePeriod: '2026-05-02',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    followerCount: 12410,
    newFollower: 50,
    unfollowCount: 20,
    netGrowth: 30,
    growthRate: '0.24%',
  },
  {
    timePeriod: '2026-05-03',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    followerCount: 12435,
    newFollower: 35,
    unfollowCount: 10,
    netGrowth: 25,
    growthRate: '0.20%',
  },
  {
    timePeriod: '2026-05-04',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    followerCount: 12450,
    newFollower: 25,
    unfollowCount: 10,
    netGrowth: 15,
    growthRate: '0.12%',
  },
  {
    timePeriod: '2026-05-05',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    followerCount: 12465,
    newFollower: 30,
    unfollowCount: 15,
    netGrowth: 15,
    growthRate: '0.12%',
  },
  {
    timePeriod: '2026-05-06',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    followerCount: 12480,
    newFollower: 20,
    unfollowCount: 5,
    netGrowth: 15,
    growthRate: '0.12%',
  },
  {
    timePeriod: '2026-05-07',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    followerCount: 12500,
    newFollower: 35,
    unfollowCount: 15,
    netGrowth: 20,
    growthRate: '0.16%',
  },
  {
    timePeriod: '2026-05-01',
    accountName: '职场进阶指南',
    ipGroupName: 'A-2组',
    followerCount: 8800,
    newFollower: 30,
    unfollowCount: 8,
    netGrowth: 22,
    growthRate: '0.25%',
  },
  {
    timePeriod: '2026-05-02',
    accountName: '职场进阶指南',
    ipGroupName: 'A-2组',
    followerCount: 8825,
    newFollower: 35,
    unfollowCount: 10,
    netGrowth: 25,
    growthRate: '0.28%',
  },
  {
    timePeriod: '2026-05-03',
    accountName: '职场进阶指南',
    ipGroupName: 'A-2组',
    followerCount: 8850,
    newFollower: 28,
    unfollowCount: 3,
    netGrowth: 25,
    growthRate: '0.28%',
  },
]

/**
 * Mock粉丝趋势数据（多账号）
 */
export const mockFollowerTrend: FollowerTrendVO[] = [
  {
    accountId: 1001,
    accountName: '知识变现研究院',
    series: [
      { date: '2026-05-01', followerCount: 12380, newFollower: 45, netGrowth: 35 },
      { date: '2026-05-02', followerCount: 12410, newFollower: 50, netGrowth: 30 },
      { date: '2026-05-03', followerCount: 12435, newFollower: 35, netGrowth: 25 },
      { date: '2026-05-04', followerCount: 12450, newFollower: 25, netGrowth: 15 },
      { date: '2026-05-05', followerCount: 12465, newFollower: 30, netGrowth: 15 },
      { date: '2026-05-06', followerCount: 12480, newFollower: 20, netGrowth: 15 },
      { date: '2026-05-07', followerCount: 12500, newFollower: 35, netGrowth: 20 },
    ],
  },
  {
    accountId: 1002,
    accountName: '职场进阶指南',
    series: [
      { date: '2026-05-01', followerCount: 8800, newFollower: 30, netGrowth: 22 },
      { date: '2026-05-02', followerCount: 8825, newFollower: 35, netGrowth: 25 },
      { date: '2026-05-03', followerCount: 8850, newFollower: 28, netGrowth: 25 },
      { date: '2026-05-04', followerCount: 8870, newFollower: 22, netGrowth: 20 },
      { date: '2026-05-05', followerCount: 8890, newFollower: 25, netGrowth: 20 },
      { date: '2026-05-06', followerCount: 8910, newFollower: 28, netGrowth: 20 },
      { date: '2026-05-07', followerCount: 8935, newFollower: 30, netGrowth: 25 },
    ],
  },
]

/**
 * 模拟分页查询粉丝分析列表
 */
export function mockGetFollowerAnalysisList(
  pageNo: number,
  pageSize: number
): PageResult<FollowerAnalysisVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockFollowerAnalysisList.slice(start, end)
  
  return {
    total: mockFollowerAnalysisList.length,
    list,
    stats: mockFollowerStats,
  }
}
