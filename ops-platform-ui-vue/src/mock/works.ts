/**
 * 作品分析 - Mock数据
 */

import type {
  ContentAnalysisVO,
  ContentStats,
  ContentTrendPoint,
  PageResult,
} from '@/types/works'

/**
 * Mock作品统计数据
 */
export const mockContentStats: ContentStats = {
  totalPublished: 156,
  totalViews: 2850000,
  totalLikes: 125000,
  totalComments: 8500,
  totalShares: 12000,
}

/**
 * Mock作品列表数据
 */
export const mockContentList: ContentAnalysisVO[] = [
  {
    id: 3001,
    title: '双11大促文案怎么写？',
    contentType: '文章',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    publishTime: '2026-05-25 10:00:00',
    viewCount: 125000,
    likeCount: 8500,
    commentCount: 320,
    shareCount: 1200,
    isViral: true,
  },
  {
    id: 3002,
    title: 'AI技术最新突破',
    contentType: '短视频',
    accountName: 'AI技术前沿',
    ipGroupName: 'B-1组',
    publishTime: '2026-05-24 14:30:00',
    viewCount: 580000,
    likeCount: 42000,
    commentCount: 2100,
    shareCount: 5800,
    isViral: true,
  },
  {
    id: 3003,
    title: '职场新人避坑指南',
    contentType: '文章',
    accountName: '职场进阶指南',
    ipGroupName: 'A-2组',
    publishTime: '2026-05-23 16:20:00',
    viewCount: 32000,
    likeCount: 1800,
    commentCount: 95,
    shareCount: 420,
    isViral: false,
  },
]

/**
 * 模拟分页查询作品列表
 */
export function mockGetContentList(
  pageNo: number,
  pageSize: number
): PageResult<ContentAnalysisVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockContentList.slice(start, end)
  
  return {
    total: mockContentList.length,
    list,
  }
}

/**
 * Mock作品趋势数据
 */
export function mockGetContentTrend(contentId: number): ContentTrendPoint[] {
  const data: ContentTrendPoint[] = []
  const baseViews = mockContentList.find(c => c.id === contentId)?.viewCount || 100000
  
  for (let i = 14; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    
    const viewCount = Math.round(baseViews * (1 - i * 0.05 + Math.random() * 0.1))
    const interactionCount = Math.round(viewCount * 0.08)
    
    data.push({
      date: dateStr,
      viewCount,
      interactionCount,
    })
  }
  
  return data
}
