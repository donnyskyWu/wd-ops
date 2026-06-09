/**
 * 内部内容分析 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  InternalContentVO,
  PageResult,
  ContentTrendDetailVO,
} from '@/types/internal-content'

/**
 * Mock内部内容列表数据（公众号）
 */
export const mockWechatMPContent: InternalContentVO[] = [
  {
    contentId: 3001,
    title: '2026年知识付费趋势分析',
    contentType: 'ARTICLE',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    publishTime: '2026-05-20 09:00:00',
    readCount: 5200,
    likeCount: 320,
    commentCount: 45,
    forwardCount: 120,
    isHit: true,
  },
  {
    contentId: 3002,
    title: '如何构建个人IP品牌',
    contentType: 'ARTICLE',
    accountName: '职场进阶指南',
    ipGroupName: 'A-2组',
    publishTime: '2026-05-18 10:30:00',
    readCount: 3800,
    likeCount: 250,
    commentCount: 32,
    forwardCount: 85,
    isHit: false,
  },
]

/**
 * Mock内部内容列表数据（抖音）
 */
export const mockDouyinContent: InternalContentVO[] = [
  {
    contentId: 3003,
    title: 'AI工具提升工作效率实战',
    contentType: 'VIDEO',
    accountName: 'AI技术前沿',
    ipGroupName: 'B-1组',
    publishTime: '2026-05-15 14:00:00',
    readCount: 8500,
    likeCount: 680,
    commentCount: 120,
    forwardCount: 230,
    isHit: true,
  },
  {
    contentId: 3004,
    title: '短视频运营技巧分享',
    contentType: 'VIDEO',
    accountName: '科技评测君',
    ipGroupName: 'C-1组',
    publishTime: '2026-05-10 16:20:00',
    readCount: 6200,
    likeCount: 450,
    commentCount: 78,
    forwardCount: 150,
    isHit: false,
  },
]

/**
 * Mock内容趋势数据
 */
export const mockInternalContentTrend: ContentTrendDetailVO = {
  contentId: 3001,
  title: '2026年知识付费趋势分析',
  series: [
    { date: '2026-05-20', readCount: 3500, likeCount: 210, commentCount: 30, forwardCount: 80 },
    { date: '2026-05-21', readCount: 1200, likeCount: 80, commentCount: 10, forwardCount: 30 },
    { date: '2026-05-22', readCount: 800, likeCount: 50, commentCount: 8, forwardCount: 15 },
    { date: '2026-05-23', readCount: 500, likeCount: 30, commentCount: 5, forwardCount: 10 },
    { date: '2026-05-24', readCount: 350, likeCount: 20, commentCount: 3, forwardCount: 8 },
    { date: '2026-05-25', readCount: 250, likeCount: 15, commentCount: 2, forwardCount: 5 },
    { date: '2026-05-26', readCount: 180, likeCount: 10, commentCount: 1, forwardCount: 3 },
  ],
}

/**
 * 根据平台类型获取Mock内容列表
 */
export function getMockContentList(platformType: string): InternalContentVO[] {
  switch (platformType) {
    case 'WECHAT_MP':
      return mockWechatMPContent
    case 'DOUYIN':
      return mockDouyinContent
    default:
      return mockWechatMPContent
  }
}

/**
 * 模拟分页查询内部内容列表
 */
export function mockGetInternalContentList(
  platformType: string,
  pageNo: number,
  pageSize: number
): PageResult<InternalContentVO> {
  const allData = getMockContentList(platformType)
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = allData.slice(start, end)
  
  return {
    total: allData.length,
    list,
  }
}
