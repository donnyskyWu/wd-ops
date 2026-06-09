/**
 * 作者管理 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  AuthorListVO,
  AuthorDashboardVO,
  OpsUserVO,
  OpsAnchorRelVO,
  OpsAnchorStatsVO,
} from '@/types/author'

/**
 * Mock作者列表数据
 */
export const mockAuthorList: AuthorListVO[] = [
  {
    id: 1,
    authorName: '张三',
    authorType: 'BOTH',
    authorTypeText: '直播+短视频',
    ipGroupId: 11,
    ipGroupName: 'A-1组',
    primaryAccountId: 100,
    primaryAccountName: '公众号A',
    opsUserNames: ['李四', '王五'],
    followerCount: 100000,
    contentCount: 120,
    liveHours: 256.5,
    videoCount: 80,
    status: 1,
    createTime: '2026-05-01 10:00:00',
  },
  {
    id: 2,
    authorName: '李四',
    authorType: 'LIVE',
    authorTypeText: '直播',
    ipGroupId: 12,
    ipGroupName: 'A-2组',
    primaryAccountId: 101,
    primaryAccountName: '抖音号A',
    opsUserNames: ['赵六'],
    followerCount: 250000,
    contentCount: 85,
    liveHours: 420.0,
    videoCount: 0,
    status: 1,
    createTime: '2026-05-02 10:00:00',
  },
  {
    id: 3,
    authorName: '王五',
    authorType: 'SHORT_VIDEO',
    authorTypeText: '短视频',
    ipGroupId: 11,
    ipGroupName: 'A-1组',
    primaryAccountId: 102,
    primaryAccountName: '快手号A',
    opsUserNames: ['孙七', '周八'],
    followerCount: 180000,
    contentCount: 200,
    liveHours: 0,
    videoCount: 200,
    status: 1,
    createTime: '2026-05-03 10:00:00',
  },
]

/**
 * Mock作者数据看板
 */
export const mockAuthorDashboard: Record<number, AuthorDashboardVO> = {
  1: {
    id: 1,
    authorName: '张三',
    authorType: 'BOTH',
    ipGroupName: 'A-1组',
    primaryAccountName: '公众号A',
    opsUserNames: ['李四', '王五'],
    followerCount: 100000,
    contentCount: 120,
    liveHours: 256.5,
    videoCount: 80,
    hitContentCount: 12,
    followerTrend: [
      { date: '2026-05-01', followerCount: 99000, newFollower: 500, unfollowCount: 100 },
      { date: '2026-05-02', followerCount: 100000, newFollower: 1200, unfollowCount: 200 },
      { date: '2026-05-03', followerCount: 101500, newFollower: 1800, unfollowCount: 300 },
    ],
    contentTrend: [
      { date: '2026-05-01', contentCount: 3, readCount: 50000, likeCount: 3000 },
      { date: '2026-05-02', contentCount: 5, readCount: 80000, likeCount: 5000 },
      { date: '2026-05-03', contentCount: 4, readCount: 65000, likeCount: 4200 },
    ],
  },
  2: {
    id: 2,
    authorName: '李四',
    authorType: 'LIVE',
    ipGroupName: 'A-2组',
    primaryAccountName: '抖音号A',
    opsUserNames: ['赵六'],
    followerCount: 250000,
    contentCount: 85,
    liveHours: 420.0,
    videoCount: 0,
    hitContentCount: 8,
    followerTrend: [],
    contentTrend: [],
  },
}

/**
 * Mock运营人员列表
 */
export const mockOpsUsers: Record<number, OpsUserVO[]> = {
  1: [
    {
      userId: 200,
      userName: '李四',
      deptName: '运营一部',
      relTime: '2026-01-01 10:00:00',
    },
    {
      userId: 201,
      userName: '王五',
      deptName: '运营二部',
      relTime: '2026-03-01 10:00:00',
    },
  ],
  2: [
    {
      userId: 202,
      userName: '赵六',
      deptName: '运营三部',
      relTime: '2026-02-01 10:00:00',
    },
  ],
}

/**
 * Mock运营→主播关联列表
 */
export const mockOpsAnchorRels: OpsAnchorRelVO[] = [
  {
    id: 1,
    opsUserId: 200,
    opsUserName: '李四',
    anchorUserId: 100,
    anchorUserName: '张三',
    ipGroupId: 11,
    ipGroupName: 'A-1组',
    startDate: '2026-01-01',
    endDate: '2026-12-31',
    createTime: '2026-01-01 10:00:00',
  },
  {
    id: 2,
    opsUserId: 201,
    opsUserName: '王五',
    anchorUserId: 100,
    anchorUserName: '张三',
    ipGroupId: 11,
    ipGroupName: 'A-1组',
    startDate: '2026-03-01',
    endDate: null,
    createTime: '2026-03-01 10:00:00',
  },
  {
    id: 3,
    opsUserId: 202,
    opsUserName: '赵六',
    anchorUserId: 101,
    anchorUserName: '李四',
    ipGroupId: 12,
    ipGroupName: 'A-2组',
    startDate: '2026-02-01',
    endDate: null,
    createTime: '2026-02-01 10:00:00',
  },
]

/**
 * Mock运营统计数据
 */
export const mockOpsAnchorStats: Record<number, OpsAnchorStatsVO> = {
  200: {
    opsUserId: 200,
    opsUserName: '李四',
    totalAnchorCount: 5,
    activeAnchorCount: 4,
    totalFollowerCount: 500000,
    totalContentCount: 350,
    totalLiveHours: 800.5,
  },
  201: {
    opsUserId: 201,
    opsUserName: '王五',
    totalAnchorCount: 3,
    activeAnchorCount: 3,
    totalFollowerCount: 300000,
    totalContentCount: 200,
    totalLiveHours: 500.0,
  },
}
