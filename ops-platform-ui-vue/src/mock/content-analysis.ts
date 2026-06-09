/**
 * 作品分析 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  ContentAnalysisVO,
  PageResult,
  ContentStatsVO,
  ContentTrendDetailVO,
} from '@/types/content-analysis'

/**
 * Mock作品统计数据
 */
export const mockContentStats: ContentStatsVO = {
  totalContent: 200,
  totalRead: 520000,
  totalLike: 32000,
  totalComment: 4500,
  totalForward: 12000,
  hitCount: 15,
  avgReadCount: 2600,
}

/**
 * Mock作品分析列表数据
 */
export const mockContentAnalysisList: ContentAnalysisVO[] = [
  {
    contentId: 2001,
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
    contentId: 2002,
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
  {
    contentId: 2003,
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
    contentId: 2004,
    title: '内容创作方法论总结',
    contentType: 'ARTICLE',
    accountName: '知识变现研究院',
    ipGroupName: 'A-1组',
    publishTime: '2026-05-12 11:15:00',
    readCount: 2900,
    likeCount: 180,
    commentCount: 28,
    forwardCount: 65,
    isHit: false,
  },
  {
    contentId: 2005,
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
  {
    contentId: 2006,
    title: '直播带货全流程解析',
    contentType: 'LIVE',
    accountName: '美食探店日记',
    ipGroupName: 'C-2组',
    publishTime: '2026-05-08 20:00:00',
    readCount: 12000,
    likeCount: 890,
    commentCount: 230,
    forwardCount: 340,
    isHit: true,
  },
  {
    contentId: 2007,
    title: '职场沟通技巧大全',
    contentType: 'ARTICLE',
    accountName: '职场进阶指南',
    ipGroupName: 'A-2组',
    publishTime: '2026-05-05 09:30:00',
    readCount: 4100,
    likeCount: 280,
    commentCount: 52,
    forwardCount: 95,
    isHit: false,
  },
  {
    contentId: 2008,
    title: '机器学习入门教程',
    contentType: 'VIDEO',
    accountName: 'AI技术前沿',
    ipGroupName: 'B-1组',
    publishTime: '2026-05-03 15:00:00',
    readCount: 7800,
    likeCount: 620,
    commentCount: 145,
    forwardCount: 210,
    isHit: true,
  },
]

/**
 * Mock作品趋势数据
 */
export const mockContentTrend: ContentTrendDetailVO = {
  contentId: 2001,
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
 * 模拟分页查询作品分析列表
 */
export function mockGetContentAnalysisList(
  pageNo: number,
  pageSize: number
): PageResult<ContentAnalysisVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockContentAnalysisList.slice(start, end)
  
  return {
    total: mockContentAnalysisList.length,
    list,
    stats: mockContentStats,
  }
}
