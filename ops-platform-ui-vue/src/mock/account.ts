/**
 * 账号分析 - Mock数据
 */

import type {
  AccountVO,
  FollowerTrendPoint,
  ContentTrendPoint,
  PageResult,
} from '@/types/account'
import { PlatformType, AccountStatus } from '@/types/account'

/**
 * Mock账号列表数据
 */
export const mockAccountList: AccountVO[] = [
  {
    id: 1001,
    accountName: '知识变现研究院',
    platformType: PlatformType.WECHAT_MP,
    platformName: '公众号',
    ipGroupName: 'A-1组',
    followerCount: 125000,
    contentCount: 156,
    accountStatus: AccountStatus.ENABLED,
    statusText: '启用',
    realName: '张**三',
    operatorName: '李四',
  },
  {
    id: 1002,
    accountName: 'AI技术前沿',
    platformType: PlatformType.DOUYIN,
    platformName: '抖音',
    ipGroupName: 'B-1组',
    followerCount: 580000,
    contentCount: 89,
    accountStatus: AccountStatus.ENABLED,
    statusText: '启用',
    realName: '王**五',
    operatorName: '赵六',
  },
  {
    id: 1003,
    accountName: '职场进阶指南',
    platformType: PlatformType.XIAOHONGSHU,
    platformName: '小红书',
    ipGroupName: 'A-2组',
    followerCount: 32000,
    contentCount: 234,
    accountStatus: AccountStatus.ENABLED,
    statusText: '启用',
    realName: '刘**华',
    operatorName: '孙七',
  },
  {
    id: 1004,
    accountName: '科技评测君',
    platformType: PlatformType.KUAISHOU,
    platformName: '快手',
    ipGroupName: 'C-1组',
    followerCount: 1250000,
    contentCount: 67,
    accountStatus: AccountStatus.ENABLED,
    statusText: '启用',
    realName: '周**明',
    operatorName: '吴八',
  },
  {
    id: 1005,
    accountName: '企业服务号',
    platformType: PlatformType.SERVICE_ACCOUNT,
    platformName: '服务号',
    ipGroupName: 'D-1组',
    followerCount: 8500,
    contentCount: 45,
    accountStatus: AccountStatus.DISABLED,
    statusText: '停用',
    realName: '郑**强',
    operatorName: '钱九',
  },
]

/**
 * 模拟分页查询账号列表
 */
export function mockGetAccountList(
  pageNo: number,
  pageSize: number,
  platformType?: PlatformType
): PageResult<AccountVO> {
  let filteredList = mockAccountList
  
  // 按平台筛选
  if (platformType && platformType !== PlatformType.ALL) {
    filteredList = mockAccountList.filter(item => item.platformType === platformType)
  }
  
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = filteredList.slice(start, end)
  
  return {
    total: filteredList.length,
    list,
  }
}

/**
 * Mock粉丝趋势数据
 */
export function mockGetFollowerTrend(accountId: number): FollowerTrendPoint[] {
  const data: FollowerTrendPoint[] = []
  const baseCount = mockAccountList.find(a => a.id === accountId)?.followerCount || 100000
  
  for (let i = 30; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    
    data.push({
      date: dateStr,
      followerCount: Math.round(baseCount * (1 - i * 0.005 + Math.random() * 0.01)),
      newFollowers: Math.round(Math.random() * 500 + 100),
    })
  }
  
  return data
}

/**
 * Mock内容趋势数据
 */
export function mockGetContentTrend(accountId: number): ContentTrendPoint[] {
  const data: ContentTrendPoint[] = []
  
  for (let i = 30; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    
    data.push({
      date: dateStr,
      contentCount: Math.round(Math.random() * 5),
      playCount: Math.round(Math.random() * 50000 + 10000),
    })
  }
  
  return data
}
