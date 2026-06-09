/**
 * IP组管理 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  IpGroupTreeVO,
  IpGroupListVO,
  IpGroupStatsVO,
  IpGroupMemberVO,
  IpGroupAccountVO,
  IpGroupAnchorVO,
} from '@/types/ip-group'

/**
 * Mock IP组树形数据
 */
export const mockIpGroupTree: IpGroupTreeVO[] = [
  {
    id: 1,
    groupName: 'A大组',
    groupType: 1,
    parentId: null,
    parentName: null,
    leaderId: 100,
    leaderName: '张三',
    accountCount: 15,
    anchorCount: 3,
    status: 1,
    createTime: '2026-05-01 10:00:00',
    children: [
      {
        id: 11,
        groupName: 'A-1组',
        groupType: 2,
        parentId: 1,
        parentName: 'A大组',
        leaderId: 200,
        leaderName: '李四',
        memberCount: 8,
        accountCount: 5,
        anchorCount: 2,
        status: 1,
        createTime: '2026-05-02 10:00:00',
        children: [],
      },
      {
        id: 12,
        groupName: 'A-2组',
        groupType: 2,
        parentId: 1,
        parentName: 'A大组',
        leaderId: 201,
        leaderName: '王五',
        memberCount: 6,
        accountCount: 4,
        anchorCount: 1,
        status: 1,
        createTime: '2026-05-03 10:00:00',
        children: [],
      },
    ],
  },
  {
    id: 2,
    groupName: 'B大组',
    groupType: 1,
    parentId: null,
    parentName: null,
    leaderId: 101,
    leaderName: '赵六',
    accountCount: 12,
    anchorCount: 2,
    status: 1,
    createTime: '2026-05-04 10:00:00',
    children: [
      {
        id: 21,
        groupName: 'B-1组',
        groupType: 2,
        parentId: 2,
        parentName: 'B大组',
        leaderId: 202,
        leaderName: '孙七',
        memberCount: 5,
        accountCount: 6,
        anchorCount: 1,
        status: 1,
        createTime: '2026-05-05 10:00:00',
        children: [],
      },
    ],
  },
]

/**
 * Mock IP组平铺列表数据
 */
export const mockIpGroupList: IpGroupListVO[] = [
  {
    id: 1,
    groupName: 'A大组',
    groupType: 1,
    parentId: null,
    parentName: null,
    leaderName: '张三',
    memberCount: 0,
    accountCount: 15,
    anchorCount: 3,
    status: 1,
    createTime: '2026-05-01 10:00:00',
  },
  {
    id: 11,
    groupName: 'A-1组',
    groupType: 2,
    parentId: 1,
    parentName: 'A大组',
    leaderName: '李四',
    memberCount: 8,
    accountCount: 5,
    anchorCount: 2,
    status: 1,
    createTime: '2026-05-02 10:00:00',
  },
  {
    id: 12,
    groupName: 'A-2组',
    groupType: 2,
    parentId: 1,
    parentName: 'A大组',
    leaderName: '王五',
    memberCount: 6,
    accountCount: 4,
    anchorCount: 1,
    status: 1,
    createTime: '2026-05-03 10:00:00',
  },
  {
    id: 2,
    groupName: 'B大组',
    groupType: 1,
    parentId: null,
    parentName: null,
    leaderName: '赵六',
    memberCount: 0,
    accountCount: 12,
    anchorCount: 2,
    status: 1,
    createTime: '2026-05-04 10:00:00',
  },
  {
    id: 21,
    groupName: 'B-1组',
    groupType: 2,
    parentId: 2,
    parentName: 'B大组',
    leaderName: '孙七',
    memberCount: 5,
    accountCount: 6,
    anchorCount: 1,
    status: 1,
    createTime: '2026-05-05 10:00:00',
  },
]

/**
 * Mock IP组统计信息
 */
export const mockIpGroupStats: Record<number, IpGroupStatsVO> = {
  1: {
    memberCount: 14,
    accountCount: 15,
    anchorCount: 3,
    totalFollowers: 1200000,
    totalContent: 3500,
    totalLiveHours: 450.5,
  },
  11: {
    memberCount: 8,
    accountCount: 5,
    anchorCount: 2,
    totalFollowers: 500000,
    totalContent: 1200,
    totalLiveHours: 200.5,
  },
  12: {
    memberCount: 6,
    accountCount: 4,
    anchorCount: 1,
    totalFollowers: 350000,
    totalContent: 900,
    totalLiveHours: 150.0,
  },
  2: {
    memberCount: 5,
    accountCount: 12,
    anchorCount: 2,
    totalFollowers: 980000,
    totalContent: 2800,
    totalLiveHours: 380.0,
  },
  21: {
    memberCount: 5,
    accountCount: 6,
    anchorCount: 1,
    totalFollowers: 600000,
    totalContent: 1600,
    totalLiveHours: 220.0,
  },
}

/**
 * Mock成员列表
 */
export const mockMembers: Record<number, IpGroupMemberVO[]> = {
  11: [
    {
      memberId: 1,
      userId: 200,
      userName: '李四',
      position: 'OPS_LEADER',
      positionText: '运营组长',
      isLeader: true,
      joinTime: '2026-05-01 10:00:00',
    },
    {
      memberId: 2,
      userId: 201,
      userName: '张三',
      position: 'OPERATOR',
      positionText: '运营人员',
      isLeader: false,
      joinTime: '2026-05-02 10:00:00',
    },
  ],
  12: [
    {
      memberId: 3,
      userId: 202,
      userName: '王五',
      position: 'OPS_LEADER',
      positionText: '运营组长',
      isLeader: true,
      joinTime: '2026-05-03 10:00:00',
    },
  ],
}

/**
 * Mock关联账号列表
 */
export const mockAccounts: Record<number, IpGroupAccountVO[]> = {
  11: [
    {
      accountId: 10,
      accountName: '公众号A',
      platform: 'WECHAT_MP',
      platformText: '公众号',
      followerCount: 100000,
      accountRole: 'PRIMARY',
      accountRoleText: '主号',
    },
    {
      accountId: 11,
      accountName: '抖音号A',
      platform: 'DOUYIN',
      platformText: '抖音',
      followerCount: 250000,
      accountRole: 'PRIMARY',
      accountRoleText: '主号',
    },
  ],
  12: [
    {
      accountId: 12,
      accountName: '快手号A',
      platform: 'KUAISHOU',
      platformText: '快手',
      followerCount: 180000,
      accountRole: 'SECONDARY',
      accountRoleText: '辅号',
    },
  ],
}

/**
 * Mock关联主播列表
 */
export const mockAnchors: Record<number, IpGroupAnchorVO[]> = {
  11: [
    {
      anchorUserId: 300,
      anchorName: '主播A',
      anchorType: 'BOTH',
      anchorTypeText: '直播+短视频',
      relTime: '2026-05-01 10:00:00',
    },
    {
      anchorUserId: 301,
      anchorName: '主播B',
      anchorType: 'LIVE',
      anchorTypeText: '直播',
      relTime: '2026-05-02 10:00:00',
    },
  ],
  12: [
    {
      anchorUserId: 302,
      anchorName: '主播C',
      anchorType: 'VIDEO',
      anchorTypeText: '短视频',
      relTime: '2026-05-03 10:00:00',
    },
  ],
}
