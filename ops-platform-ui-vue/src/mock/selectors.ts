/**
 * 7 大强选择器通用 mock 数据
 * 用于前端独立开发,各选择器组件 fallback
 */
export const mockCompanyList = [
  { id: 1, companyName: '知识变现研究院', legalPerson: '张三', usedQuota: 8, totalQuota: 20, status: 1 },
  { id: 2, companyName: 'AI 前沿科技', legalPerson: '李四', usedQuota: 5, totalQuota: 15, status: 1 },
  { id: 3, companyName: '财经日报', legalPerson: '王五', usedQuota: 12, totalQuota: 30, status: 1 },
  { id: 4, companyName: '新媒体工作室', legalPerson: '赵六', usedQuota: 3, totalQuota: 10, status: 1 },
  { id: 5, companyName: '文娱传媒', legalPerson: '孙七', usedQuota: 6, totalQuota: 20, status: 0 },
]

export const mockRealNameList = [
  { id: 1, realName: '张三', idCardMask: '110102********1234', companyId: 1, companyName: '知识变现研究院', status: 1 },
  { id: 2, realName: '李四', idCardMask: '310101********5678', companyId: 1, companyName: '知识变现研究院', status: 1 },
  { id: 3, realName: '王五', idCardMask: '440106********9012', companyId: 2, companyName: 'AI 前沿科技', status: 1 },
  { id: 4, realName: '赵六', idCardMask: '320102********3456', companyId: 2, companyName: 'AI 前沿科技', status: 1 },
  { id: 5, realName: '孙七', idCardMask: '510104********7890', companyId: 3, companyName: '财经日报', status: 1 },
  { id: 6, realName: '周八', idCardMask: '330106********2345', companyId: 3, companyName: '财经日报', status: 1 },
  { id: 7, realName: '吴九', idCardMask: '420103********6789', companyId: 4, companyName: '新媒体工作室', status: 1 },
]

export const mockPhoneList = [
  { id: 1, phoneMask: '138****1234', carrier: '中国移动', realNameId: 1, status: 1 },
  { id: 2, phoneMask: '139****5678', carrier: '中国移动', realNameId: 1, status: 1 },
  { id: 3, phoneMask: '186****9012', carrier: '中国联通', realNameId: 2, status: 1 },
  { id: 4, phoneMask: '187****3456', carrier: '中国联通', realNameId: 3, status: 1 },
  { id: 5, phoneMask: '188****7890', carrier: '中国电信', realNameId: 3, status: 1 },
  { id: 6, phoneMask: '189****2345', carrier: '中国电信', realNameId: 4, status: 1 },
  { id: 7, phoneMask: '131****6789', carrier: '中国移动', realNameId: 5, status: 1 },
]

export const mockSimCardList = [
  { id: 1, simNoMask: '8986****0001', carrier: '中国移动', phoneId: 1, activatedAt: '2025-01-15' },
  { id: 2, simNoMask: '8986****0002', carrier: '中国移动', phoneId: 1, activatedAt: '2025-03-20' },
  { id: 3, simNoMask: '8986****0003', carrier: '中国联通', phoneId: 3, activatedAt: '2025-02-10' },
  { id: 4, simNoMask: '8986****0004', carrier: '中国电信', phoneId: 5, activatedAt: '2024-12-05' },
  { id: 5, simNoMask: '8986****0005', carrier: '中国电信', phoneId: 6, activatedAt: '2025-04-18' },
]

export const mockAccountList = [
  { id: 1001, accountName: '知识变现研究院', platformType: 'WECHAT_MP', platformName: '公众号', ipGroupId: 1, ipGroupName: 'A 大组', status: 1 },
  { id: 1002, accountName: 'AI 技术前沿', platformType: 'DOUYIN', platformName: '抖音', ipGroupId: 1, ipGroupName: 'A 大组', status: 1 },
  { id: 1003, accountName: '财经日报视频号', platformType: 'VIDEO_ACCOUNT', platformName: '视频号', ipGroupId: 2, ipGroupName: 'B 大组', status: 1 },
  { id: 1004, accountName: '小红书知识站', platformType: 'XIAOHONGSHU', platformName: '小红书', ipGroupId: 1, ipGroupName: 'A 大组', status: 1 },
  { id: 1005, accountName: '快手知识变现', platformType: 'KUAISHOU', platformName: '快手', ipGroupId: 2, ipGroupName: 'B 大组', status: 1 },
  { id: 1006, accountName: '服务号-企业咨询', platformType: 'SERVICE_ACCOUNT', platformName: '服务号', ipGroupId: 1, ipGroupName: 'A 大组', status: 1 },
  { id: 1007, accountName: '企微-客服中心', platformType: 'WECHAT_WORK', platformName: '企微', ipGroupId: 2, ipGroupName: 'B 大组', status: 1 },
]

export const mockUserList = [
  { id: 1, username: 'admin', nickname: '系统管理员', deptName: '技术部', roleNames: ['超级管理员'], status: 1 },
  { id: 100, username: 'zhangsan', nickname: '张三', deptName: '运营一部', roleNames: ['运营主管'], status: 1 },
  { id: 101, username: 'lisi', nickname: '李四', deptName: '运营一部', roleNames: ['内容运营'], status: 1 },
  { id: 102, username: 'wangwu', nickname: '王五', deptName: '运营二部', roleNames: ['运营主管'], status: 1 },
  { id: 200, username: 'zhaoliu', nickname: '赵六', deptName: '运营二部', roleNames: ['内容编辑'], status: 1 },
  { id: 201, username: 'sunqi', nickname: '孙七', deptName: '内容部', roleNames: ['内容编辑'], status: 1 },
  { id: 300, username: 'zhuboa', nickname: '主播 A', deptName: '主播部', roleNames: ['主播'], status: 1 },
  { id: 301, username: 'zhubob', nickname: '主播 B', deptName: '主播部', roleNames: ['主播'], status: 1 },
]
