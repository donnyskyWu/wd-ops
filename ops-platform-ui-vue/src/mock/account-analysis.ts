/**
 * 账号分析 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  AccountAnalysisVO,
  PageResult,
  FollowerDetailVO,
  ContentVO,
} from '@/types/account-analysis'

/**
 * Mock账号列表数据（公众号）
 */
const mockWechatMPAccounts: AccountAnalysisVO[] = [
  {
    accountId: 1001,
    platformAccountId: 'gh_abc123',
    accountName: '知识变现研究院',
    platformType: 'WECHAT_MP',
    platformTypeText: '公众号',
    ipGroupName: 'A-1组',
    followerCount: 12500,
    contentCount: 320,
    accountStatus: 'ACTIVE',
    accountStatusText: '正常运营',
    realName: '张**',
    operatorName: '李四',
    createTime: '2026-01-01 10:00:00',
  },
  {
    accountId: 1002,
    platformAccountId: 'gh_def456',
    accountName: '职场进阶指南',
    platformType: 'WECHAT_MP',
    platformTypeText: '公众号',
    ipGroupName: 'A-2组',
    followerCount: 8900,
    contentCount: 156,
    accountStatus: 'ACTIVE',
    accountStatusText: '正常运营',
    realName: '王**',
    operatorName: '赵六',
    createTime: '2026-02-15 14:30:00',
  },
  {
    accountId: 1003,
    platformAccountId: 'gh_ghi789',
    accountName: 'AI技术前沿',
    platformType: 'WECHAT_MP',
    platformTypeText: '公众号',
    ipGroupName: 'B-1组',
    followerCount: 25600,
    contentCount: 480,
    accountStatus: 'ACTIVE',
    accountStatusText: '正常运营',
    realName: '刘**',
    operatorName: '孙七',
    createTime: '2025-12-20 09:15:00',
  },
]

/**
 * Mock账号列表数据（抖音）
 */
const mockDouyinAccounts: AccountAnalysisVO[] = [
  {
    accountId: 2001,
    platformAccountId: 'dy_user123',
    accountName: '科技评测君',
    platformType: 'DOUYIN',
    platformTypeText: '抖音',
    ipGroupName: 'C-1组',
    followerCount: 156000,
    contentCount: 230,
    accountStatus: 'ACTIVE',
    accountStatusText: '正常运营',
    realName: '陈**',
    operatorName: '周八',
    createTime: '2026-03-10 11:20:00',
  },
  {
    accountId: 2002,
    platformAccountId: 'dy_user456',
    accountName: '美食探店日记',
    platformType: 'DOUYIN',
    platformTypeText: '抖音',
    ipGroupName: 'C-2组',
    followerCount: 89000,
    contentCount: 180,
    accountStatus: 'ACTIVE',
    accountStatusText: '正常运营',
    realName: '吴**',
    operatorName: '郑九',
    createTime: '2026-01-25 16:45:00',
  },
]

/**
 * Mock账号列表数据（视频号）
 */
const mockVideoAccountAccounts: AccountAnalysisVO[] = [
  {
    accountId: 3001,
    platformAccountId: 'vx_video123',
    accountName: '财经观察室',
    platformType: 'VIDEO_ACCOUNT',
    platformTypeText: '视频号',
    ipGroupName: 'D-1组',
    followerCount: 45000,
    contentCount: 95,
    accountStatus: 'ACTIVE',
    accountStatusText: '正常运营',
    realName: '钱**',
    operatorName: '冯十',
    createTime: '2026-04-05 10:00:00',
  },
]

/**
 * Mock粉丝详情数据
 */
export const mockFollowerDetail: FollowerDetailVO = {
  accountId: 1001,
  accountName: '知识变现研究院',
  totalFollower: 12500,
  newFollower: 150,
  unfollowCount: 30,
  netGrowth: 120,
  trend: [
    { date: '2026-05-01', followerCount: 12380, newFollower: 45, unfollowCount: 10, netGrowth: 35 },
    { date: '2026-05-02', followerCount: 12410, newFollower: 50, unfollowCount: 20, netGrowth: 30 },
    { date: '2026-05-03', followerCount: 12435, newFollower: 35, unfollowCount: 10, netGrowth: 25 },
    { date: '2026-05-04', followerCount: 12450, newFollower: 25, unfollowCount: 10, netGrowth: 15 },
    { date: '2026-05-05', followerCount: 12465, newFollower: 30, unfollowCount: 15, netGrowth: 15 },
    { date: '2026-05-06', followerCount: 12480, newFollower: 20, unfollowCount: 5, netGrowth: 15 },
    { date: '2026-05-07', followerCount: 12500, newFollower: 35, unfollowCount: 15, netGrowth: 20 },
  ],
  portrait: {
    genderRatio: { male: 0.45, female: 0.55 },
    ageGroups: [
      { group: '18-24', ratio: 0.2 },
      { group: '25-34', ratio: 0.45 },
      { group: '35-44', ratio: 0.25 },
      { group: '45+', ratio: 0.1 },
    ],
    topCities: [
      { city: '北京', ratio: 0.15 },
      { city: '上海', ratio: 0.12 },
      { city: '广州', ratio: 0.1 },
      { city: '深圳', ratio: 0.09 },
      { city: '杭州', ratio: 0.08 },
    ],
  },
}

/**
 * Mock作品列表数据
 */
export const mockContentList: ContentVO[] = [
  {
    contentId: 2001,
    title: '2026年知识付费趋势分析',
    contentType: 'ARTICLE',
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
    publishTime: '2026-05-10 16:20:00',
    readCount: 6200,
    likeCount: 450,
    commentCount: 78,
    forwardCount: 150,
    isHit: false,
  },
]

/**
 * 根据平台类型获取Mock账号列表
 */
export function getMockAccountList(platformType: string): AccountAnalysisVO[] {
  switch (platformType) {
    case 'WECHAT_MP':
      return mockWechatMPAccounts
    case 'DOUYIN':
      return mockDouyinAccounts
    case 'VIDEO_ACCOUNT':
      return mockVideoAccountAccounts
    default:
      return [...mockWechatMPAccounts, ...mockDouyinAccounts, ...mockVideoAccountAccounts]
  }
}

/**
 * 模拟分页查询账号列表
 */
export function mockGetAccountAnalysisList(
  platformType: string,
  pageNo: number,
  pageSize: number
): PageResult<AccountAnalysisVO> {
  const allData = getMockAccountList(platformType)
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = allData.slice(start, end)
  
  return {
    total: allData.length,
    list,
  }
}
