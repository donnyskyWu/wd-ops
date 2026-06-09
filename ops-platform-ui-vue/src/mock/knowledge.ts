/**
 * 内容知识库 - Mock数据
 */

import type {
  KnowledgeVO,
  KnowledgeDetailVO,
  PageResult,
} from '@/types/knowledge'
import { KnowledgeCategory } from '@/types/knowledge'

/**
 * Mock知识列表数据
 */
export const mockKnowledgeList: KnowledgeVO[] = [
  {
    id: 501,
    title: '抖音爆款文案模板',
    category: KnowledgeCategory.TEMPLATE,
    tags: ['抖音', '模板', '爆款'],
    isPublic: true,
    creatorName: '张三',
    viewCount: 123,
    likeCount: 12,
    createdAt: '2026-05-24 09:00:00',
  },
  {
    id: 502,
    title: '618活动案例分析',
    category: KnowledgeCategory.CASE,
    tags: ['618', '活动', '电商'],
    isPublic: false,
    creatorName: '李四',
    viewCount: 89,
    likeCount: 7,
    createdAt: '2026-05-23 14:30:00',
  },
  {
    id: 503,
    title: '2026新媒体趋势分析报告',
    category: KnowledgeCategory.INDUSTRY,
    tags: ['行业', '趋势', '新媒体'],
    isPublic: true,
    creatorName: '王五',
    viewCount: 205,
    likeCount: 23,
    createdAt: '2026-05-22 10:15:00',
  },
  {
    id: 504,
    title: '公众号增粉技巧总结',
    category: KnowledgeCategory.EXPERIENCE,
    tags: ['公众号', '技巧', '增粉'],
    isPublic: true,
    creatorName: '赵六',
    viewCount: 67,
    likeCount: 4,
    createdAt: '2026-05-21 16:45:00',
  },
  {
    id: 505,
    title: '小红书种草文案写作指南',
    category: KnowledgeCategory.TEMPLATE,
    tags: ['小红书', '种草', '文案'],
    isPublic: true,
    creatorName: '张三',
    viewCount: 156,
    likeCount: 18,
    createdAt: '2026-05-20 11:20:00',
  },
  {
    id: 506,
    title: '直播话术大全',
    category: KnowledgeCategory.TEMPLATE,
    tags: ['直播', '话术', '技巧'],
    isPublic: true,
    creatorName: '李四',
    viewCount: 234,
    likeCount: 31,
    createdAt: '2026-05-19 14:00:00',
  },
  {
    id: 507,
    title: '抖音双11营销案例复盘',
    category: KnowledgeCategory.CASE,
    tags: ['抖音', '双11', '案例'],
    isPublic: true,
    creatorName: '王五',
    viewCount: 178,
    likeCount: 15,
    createdAt: '2026-05-18 10:30:00',
  },
  {
    id: 508,
    title: '2026年短视频行业趋势报告',
    category: KnowledgeCategory.INDUSTRY,
    tags: ['行业', '趋势', '报告'],
    isPublic: false,
    creatorName: '赵六',
    viewCount: 312,
    likeCount: 42,
    createdAt: '2026-05-17 09:15:00',
  },
  {
    id: 509,
    title: '私域流量运营心得',
    category: KnowledgeCategory.EXPERIENCE,
    tags: ['私域', '运营', '心得'],
    isPublic: true,
    creatorName: '张三',
    viewCount: 89,
    likeCount: 8,
    createdAt: '2026-05-16 16:00:00',
  },
  {
    id: 510,
    title: '企业微信养号技巧',
    category: KnowledgeCategory.EXPERIENCE,
    tags: ['企业微信', '养号', '技巧'],
    isPublic: true,
    creatorName: '李四',
    viewCount: 145,
    likeCount: 21,
    createdAt: '2026-05-15 11:30:00',
  },
]

/**
 * Mock知识详情数据
 */
export const mockKnowledgeDetails: Record<number, KnowledgeDetailVO> = {
  501: {
    id: 501,
    title: '抖音爆款文案模板',
    category: KnowledgeCategory.TEMPLATE,
    tags: ['抖音', '模板', '爆款'],
    isPublic: true,
    creatorName: '张三',
    viewCount: 124,
    likeCount: 12,
    createdAt: '2026-05-24 09:00:00',
    updatedAt: '2026-05-24 09:00:00',
    isLiked: false,
    content: `
      <h2>抖音爆款文案结构模板</h2>
      <h3>一、开头抓人（前3秒）</h3>
      <p>• 痛点引入：你是不是也经常...？</p>
      <p>• 数据震撼：90%的人都不知道...</p>
      <p>• 悬念设置：最后一种方法绝了！</p>
      
      <h3>二、中间干货（15-45秒）</h3>
      <p>• 方法1：具体步骤+案例演示</p>
      <p>• 方法2：对比效果+数据支撑</p>
      <p>• 方法3：实操截图+要点标注</p>
      
      <h3>三、结尾引导（最后3秒）</h3>
      <p>• 点赞收藏：觉得有用就点个赞吧</p>
      <p>• 评论互动：你用的是哪种方法？</p>
      <p>• 关注引导：关注我，每天分享实用技巧</p>
      
      <h3>四、标签建议</h3>
      <p>#抖音运营 #文案技巧 #爆款秘籍 #新媒体运营</p>
    `,
  },
  502: {
    id: 502,
    title: '618活动案例分析',
    category: KnowledgeCategory.CASE,
    tags: ['618', '活动', '电商'],
    isPublic: false,
    creatorName: '李四',
    viewCount: 89,
    likeCount: 7,
    createdAt: '2026-05-23 14:30:00',
    updatedAt: '2026-05-23 14:30:00',
    isLiked: false,
    content: `
      <h2>618大促活动案例复盘</h2>
      <h3>活动背景</h3>
      <p>• 活动时间：2026年6月1日-6月18日</p>
      <p>• 参与账号：15个抖音账号+8个小红书账号</p>
      <p>• 总预算：50万元</p>
      
      <h3>活动策略</h3>
      <p>• 预售期（6.1-6.10）：种草预热，发布产品测评视频</p>
      <p>• 爆发期（6.11-6.17）：直播带货，每日3场专场直播</p>
      <p>• 返场期（6.18）：最后一波优惠，冲刺销量</p>
      
      <h3>数据成果</h3>
      <p>• 总曝光量：2,850万次</p>
      <p>• 总互动量：125万次</p>
      <p>• 总销售额：320万元</p>
      <p>• ROI：1:6.4</p>
      
      <h3>经验总结</h3>
      <p>✅ 成功要素：提前蓄水、多账号矩阵、直播节奏把控</p>
      <p>⚠️ 改进空间：客服响应速度、库存管理、物流协调</p>
    `,
  },
}

/**
 * 模拟分页查询
 */
export function queryKnowledgeList(params: any): PageResult<KnowledgeVO> {
  let filteredList = [...mockKnowledgeList]

  // 关键词搜索
  if (params.keyword) {
    const keyword = params.keyword.toLowerCase()
    filteredList = filteredList.filter(item =>
      item.title.toLowerCase().includes(keyword) ||
      item.tags.some(tag => tag.toLowerCase().includes(keyword))
    )
  }

  // 分类筛选
  if (params.category) {
    filteredList = filteredList.filter(item => item.category === params.category)
  }

  // 标签筛选
  if (params.tags && params.tags.length > 0) {
    filteredList = filteredList.filter(item =>
      params.tags.some((tag: string) => item.tags.includes(tag))
    )
  }

  // 公开筛选
  if (params.isPublic !== undefined) {
    filteredList = filteredList.filter(item => item.isPublic === params.isPublic)
  }

  // 分页
  const start = (params.pageNo - 1) * params.pageSize
  const end = start + params.pageSize
  const list = filteredList.slice(start, end)

  return {
    list,
    total: filteredList.length,
  }
}
