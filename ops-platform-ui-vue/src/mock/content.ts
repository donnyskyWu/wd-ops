/**
 * 内容管理 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  ContentListItem,
  ContentDetailVO,
  ReviewRecordVO,
  PageResult,
} from '@/types/content'
import { ContentStatus, ReviewStage } from '@/types/content'

/**
 * Mock内容列表数据
 */
export const mockContentList: ContentListItem[] = [
  {
    id: 2001,
    title: '双11大促文案',
    contentType: 'ARTICLE',
    platformType: 'WECHAT_MP',
    accountName: '知识变现研究院',
    creatorName: '张三',
    aiGenerated: true,
    status: ContentStatus.PENDING_REVIEW,
    reviewStage: ReviewStage.INITIAL,
    createdAt: '2026-05-25 10:00:00',
  },
  {
    id: 2002,
    title: '直播预告短视频',
    contentType: 'VIDEO',
    platformType: 'DOUYIN',
    accountName: 'AI技术前沿',
    creatorName: '李四',
    aiGenerated: false,
    status: ContentStatus.PUBLISHED,
    reviewStage: null,
    createdAt: '2026-05-24 14:30:00',
    publishedAt: '2026-05-25 09:00:00',
  },
  {
    id: 2003,
    title: '618活动长图',
    contentType: 'ARTICLE',
    platformType: 'XIAOHONGSHU',
    accountName: '职场进阶指南',
    creatorName: '王五',
    aiGenerated: true,
    status: ContentStatus.DRAFT,
    reviewStage: null,
    createdAt: '2026-05-23 16:20:00',
  },
  {
    id: 2004,
    title: '产品评测视频',
    contentType: 'VIDEO',
    platformType: 'KUAISHOU',
    accountName: '科技评测君',
    creatorName: '赵六',
    aiGenerated: false,
    status: ContentStatus.REVIEWING,
    reviewStage: ReviewStage.SECOND,
    createdAt: '2026-05-22 11:00:00',
  },
  {
    id: 2005,
    title: '知识分享直播',
    contentType: 'LIVE',
    platformType: 'VIDEO_ACCOUNT',
    accountName: '职场进阶指南',
    creatorName: '张三',
    aiGenerated: false,
    status: ContentStatus.REJECTED,
    reviewStage: ReviewStage.FINAL,
    createdAt: '2026-05-21 09:30:00',
  },
  {
    id: 2006,
    title: '母亲节促销图文',
    contentType: 'ARTICLE',
    platformType: 'WECHAT_MP',
    accountName: '营销中心',
    creatorName: '李四',
    aiGenerated: true,
    status: ContentStatus.PUBLISHED,
    reviewStage: null,
    createdAt: '2026-05-20 14:00:00',
    publishedAt: '2026-05-21 08:00:00',
  },
  {
    id: 2007,
    title: '618预热短视频',
    contentType: 'VIDEO',
    platformType: 'DOUYIN',
    accountName: '电商达人',
    creatorName: '王五',
    aiGenerated: false,
    status: ContentStatus.PENDING_REVIEW,
    reviewStage: ReviewStage.INITIAL,
    createdAt: '2026-05-19 16:30:00',
  },
  {
    id: 2008,
    title: '端午节直播预告',
    contentType: 'LIVE',
    platformType: 'KUAISHOU',
    accountName: '生活频道',
    creatorName: '赵六',
    aiGenerated: true,
    status: ContentStatus.DRAFT,
    reviewStage: null,
    createdAt: '2026-05-18 11:00:00',
  },
  {
    id: 2009,
    title: '新品发布长文',
    contentType: 'ARTICLE',
    platformType: 'SERVICE_ACCOUNT',
    accountName: '科技前沿',
    creatorName: '张三',
    aiGenerated: false,
    status: ContentStatus.REVIEWING,
    reviewStage: ReviewStage.SECOND,
    createdAt: '2026-05-17 09:00:00',
  },
  {
    id: 2010,
    title: '夏日清凉视频',
    contentType: 'VIDEO',
    platformType: 'XIAOHONGSHU',
    accountName: '生活美学',
    creatorName: '李四',
    aiGenerated: true,
    status: ContentStatus.PUBLISH_FAILED,
    reviewStage: null,
    createdAt: '2026-05-16 15:00:00',
  },
]

/**
 * Mock内容详情数据
 */
export const mockContentDetail: ContentDetailVO = {
  id: 2001,
  title: '双11大促文案',
  content: '这是一篇由AI生成的双11大促文案...\n\n正文内容...',
  contentType: 'ARTICLE',
  platformType: 'WECHAT_MP',
  accountId: 1001,
  accountName: '知识变现研究院',
  creatorId: 101,
  creatorName: '张三',
  aiGenerated: true,
  status: ContentStatus.PENDING_REVIEW,
  reviewStage: ReviewStage.INITIAL,
  coverUrl: '',
  createdAt: '2026-05-25 10:00:00',
  updatedAt: '2026-05-25 10:30:00',
}

/**
 * Mock审核记录数据
 */
export const mockReviewRecords: ReviewRecordVO[] = [
  {
    id: 3001,
    contentId: 2001,
    reviewStage: ReviewStage.INITIAL,
    reviewerName: '审核员A',
    reviewResult: 'approved',
    reviewComment: '内容质量不错，通过初审',
    reviewedAt: '2026-05-25 11:00:00',
  },
]

/**
 * 模拟分页查询内容列表
 */
export function mockGetContentList(
  pageNo: number,
  pageSize: number
): PageResult<ContentListItem> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockContentList.slice(start, end)
  
  return {
    total: mockContentList.length,
    list,
  }
}
