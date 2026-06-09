/**
 * 人效盘点 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  ProductivityVO,
  PageResult,
  ProductivityDetailVO,
} from '@/types/productivity'

/**
 * Mock人效盘点列表数据
 */
export const mockProductivityList: ProductivityVO[] = [
  {
    userId: 101,
    userName: '张三',
    ipGroupName: 'A-1组',
    position: '运营组长',
    completedCount: 15,
    inProgressCount: 3,
    overdueCount: 1,
    completionRate: '83.33%',
    totalCost: 50000,
    totalRevenue: 80000,
    roi: '160.00%',
    contentPublished: 20,
    avgPlayCount: 12000,
    hitContentCount: 3,
  },
  {
    userId: 102,
    userName: '李四',
    ipGroupName: 'A-1组',
    position: '内容剪辑',
    completedCount: 12,
    inProgressCount: 2,
    overdueCount: 0,
    completionRate: '85.71%',
    totalCost: 30000,
    totalRevenue: 60000,
    roi: '200.00%',
    contentPublished: 15,
    avgPlayCount: 15000,
    hitContentCount: 5,
  },
  {
    userId: 103,
    userName: '王五',
    ipGroupName: 'A-2组',
    position: '运营人员',
    completedCount: 10,
    inProgressCount: 4,
    overdueCount: 2,
    completionRate: '62.50%',
    totalCost: 25000,
    totalRevenue: 40000,
    roi: '160.00%',
    contentPublished: 12,
    avgPlayCount: 8000,
    hitContentCount: 2,
  },
]

/**
 * Mock人效详情数据
 */
export const mockProductivityDetail: ProductivityDetailVO = {
  userId: 101,
  userName: '张三',
  taskDimension: {
    completedCount: 15,
    inProgressCount: 3,
    overdueCount: 1,
    completionRate: '83.33%',
    tasks: [
      { taskName: '写推文-5月第1期', status: '已完成', completeTime: '2026-05-03 14:00:00' },
      { taskName: '策划直播活动', status: '已完成', completeTime: '2026-05-05 16:30:00' },
      { taskName: '数据分析报告', status: '进行中', completeTime: '-' },
      { taskName: '视频剪辑审核', status: '超时', completeTime: '-' },
    ],
  },
  financeDimension: {
    totalCost: 50000,
    totalRevenue: 80000,
    roi: '160.00%',
    accountBreakdown: [
      { accountName: '知识变现研究院', cost: 20000, revenue: 35000 },
      { accountName: '职场进阶指南', cost: 15000, revenue: 25000 },
      { accountName: 'AI技术前沿', cost: 15000, revenue: 20000 },
    ],
  },
  contentDimension: {
    contentPublished: 20,
    avgPlayCount: 12000,
    hitContentCount: 3,
    trend: [
      { period: '2026-W20', publishCount: 5, avgPlay: 11000 },
      { period: '2026-W21', publishCount: 6, avgPlay: 12500 },
      { period: '2026-W22', publishCount: 4, avgPlay: 13000 },
      { period: '2026-W23', publishCount: 5, avgPlay: 11500 },
    ],
  },
  trendDimension: {
    series: [
      { period: '2026-W20', completionRate: 0.8, roi: 1.5, contentCount: 5 },
      { period: '2026-W21', completionRate: 0.85, roi: 1.6, contentCount: 6 },
      { period: '2026-W22', completionRate: 0.82, roi: 1.55, contentCount: 4 },
      { period: '2026-W23', completionRate: 0.83, roi: 1.6, contentCount: 5 },
    ],
  },
}

/**
 * 模拟分页查询人效列表
 */
export function mockGetProductivityList(
  pageNo: number,
  pageSize: number
): PageResult<ProductivityVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockProductivityList.slice(start, end)
  
  return {
    total: mockProductivityList.length,
    list,
  }
}
