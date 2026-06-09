/**
 * SOP管理 - Mock数据
 * 
 * 用于前端独立开发，模拟后端API响应
 */

import type {
  SopTemplateVO,
  SopNodeVO,
  SopReviewVO,
  PageResult,
  DagValidateResponse,
} from '@/types/sop'

/**
 * Mock SOP模板列表数据
 */
export const mockSopTemplateList: SopTemplateVO[] = [
  {
    id: 1,
    templateName: '标准内容生产运营流程',
    contentType: 'ALL',
    platformType: 'ALL',
    nodeCount: 11,
    status: 1,
    statusText: '启用',
    createTime: '2026-01-01 10:00:00',
  },
  {
    id: 2,
    templateName: '短视频发布流程',
    contentType: 'VIDEO',
    platformType: 'DOUYIN',
    nodeCount: 6,
    status: 1,
    statusText: '启用',
    createTime: '2026-02-15 14:30:00',
  },
  {
    id: 3,
    templateName: '直播带货流程',
    contentType: 'LIVE',
    platformType: 'DOUYIN',
    nodeCount: 8,
    status: 1,
    statusText: '启用',
    createTime: '2026-03-10 09:00:00',
  },
  {
    id: 4,
    templateName: '公众号文章发布流程',
    contentType: 'ARTICLE',
    platformType: 'WECHAT_MP',
    nodeCount: 5,
    status: 1,
    statusText: '启用',
    createTime: '2026-03-20 11:00:00',
  },
  {
    id: 5,
    templateName: '多平台同步发布流程',
    contentType: 'ALL',
    platformType: 'ALL',
    nodeCount: 9,
    status: 0,
    statusText: '停用',
    createTime: '2026-04-05 16:00:00',
  },
]

/**
 * Mock SOP节点数据 - 模板1: 标准内容生产运营流程（11节点）
 */
export const mockSopNodes: SopNodeVO[] = [
  {
    id: 1,
    templateId: 1,
    nodeName: '设置运营计划',
    nodeOrder: 1,
    nodeDescription: '制定本周运营计划',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [],
    slaHours: 2,
  },
  {
    id: 2,
    templateId: 1,
    nodeName: '写推文',
    nodeOrder: 2,
    nodeDescription: '撰写公众号推文',
    executorRole: '公众号运营',
    needReview: 1,
    reviewerRole: '运营组长',
    predecessors: [1],
    parallelGroup: 'A',
    slaHours: 4,
  },
  {
    id: 3,
    templateId: 1,
    nodeName: '剪辑视频',
    nodeOrder: 2,
    nodeDescription: '剪辑短视频',
    executorRole: '剪辑',
    needReview: 1,
    reviewerRole: '运营组长',
    predecessors: [1],
    parallelGroup: 'A',
    slaHours: 6,
  },
  {
    id: 4,
    templateId: 1,
    nodeName: '准备直播',
    nodeOrder: 2,
    nodeDescription: '准备直播内容和设备',
    executorRole: '直播运营',
    needReview: 0,
    predecessors: [1],
    parallelGroup: 'A',
    slaHours: 3,
  },
  {
    id: 5,
    templateId: 1,
    nodeName: '审核推文',
    nodeOrder: 3,
    nodeDescription: '审核推文质量',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [2],
    slaHours: 2,
  },
  {
    id: 6,
    templateId: 1,
    nodeName: '审核视频',
    nodeOrder: 3,
    nodeDescription: '审核视频质量',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [3],
    slaHours: 2,
  },
  {
    id: 7,
    templateId: 1,
    nodeName: '推送公众号',
    nodeOrder: 4,
    nodeDescription: '发布公众号推文',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [5],
    slaHours: 1,
  },
  {
    id: 8,
    templateId: 1,
    nodeName: '发布短视频',
    nodeOrder: 4,
    nodeDescription: '发布到抖音平台',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [6],
    parallelGroup: 'B',
    slaHours: 1,
  },
  {
    id: 9,
    templateId: 1,
    nodeName: '开始直播',
    nodeOrder: 4,
    nodeDescription: '进行直播',
    executorRole: '直播运营',
    needReview: 0,
    predecessors: [4],
    parallelGroup: 'B',
    slaHours: 2,
  },
  {
    id: 10,
    templateId: 1,
    nodeName: '数据分析',
    nodeOrder: 5,
    nodeDescription: '分析本周数据',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [7, 8, 9],
    slaHours: 3,
  },
  {
    id: 11,
    templateId: 1,
    nodeName: '推送服务号消息',
    nodeOrder: 6,
    nodeDescription: '推送服务号消息通知',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [10],
    slaHours: 1,
  },
]

/**
 * Mock SOP节点数据 - 模板2: 短视频发布流程（6节点）
 */
export const mockSopNodesTemplate2: SopNodeVO[] = [
  {
    id: 201,
    templateId: 2,
    nodeName: '确定选题',
    nodeOrder: 1,
    nodeDescription: '确定短视频选题和方向',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [],
    slaHours: 2,
  },
  {
    id: 202,
    templateId: 2,
    nodeName: '撰写脚本',
    nodeOrder: 2,
    nodeDescription: '撰写短视频拍摄脚本',
    executorRole: '公众号运营',
    needReview: 1,
    reviewerRole: '运营组长',
    predecessors: [201],
    slaHours: 4,
  },
  {
    id: 203,
    templateId: 2,
    nodeName: '拍摄素材',
    nodeOrder: 3,
    nodeDescription: '拍摄短视频素材',
    executorRole: '剪辑',
    needReview: 0,
    predecessors: [202],
    slaHours: 6,
  },
  {
    id: 204,
    templateId: 2,
    nodeName: '剪辑制作',
    nodeOrder: 4,
    nodeDescription: '剪辑和后期制作',
    executorRole: '剪辑',
    needReview: 1,
    reviewerRole: '运营组长',
    predecessors: [203],
    slaHours: 8,
  },
  {
    id: 205,
    templateId: 2,
    nodeName: '审核视频',
    nodeOrder: 5,
    nodeDescription: '审核视频质量和内容',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [204],
    slaHours: 2,
  },
  {
    id: 206,
    templateId: 2,
    nodeName: '发布抖音',
    nodeOrder: 6,
    nodeDescription: '发布到抖音平台',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [205],
    slaHours: 1,
  },
]

/**
 * Mock SOP节点数据 - 模板3: 直播带货流程（8节点）
 */
export const mockSopNodesTemplate3: SopNodeVO[] = [
  {
    id: 301,
    templateId: 3,
    nodeName: '选品策划',
    nodeOrder: 1,
    nodeDescription: '选择直播商品和策划方案',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [],
    slaHours: 4,
  },
  {
    id: 302,
    templateId: 3,
    nodeName: '准备直播脚本',
    nodeOrder: 2,
    nodeDescription: '编写直播话术和流程',
    executorRole: '直播运营',
    needReview: 1,
    reviewerRole: '运营组长',
    predecessors: [301],
    slaHours: 3,
  },
  {
    id: 303,
    templateId: 3,
    nodeName: '布置直播间',
    nodeOrder: 3,
    nodeDescription: '准备直播设备和场景',
    executorRole: '直播运营',
    needReview: 0,
    predecessors: [301],
    parallelGroup: 'A',
    slaHours: 2,
  },
  {
    id: 304,
    templateId: 3,
    nodeName: '预热宣传',
    nodeOrder: 3,
    nodeDescription: '发布直播预告和宣传',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [301],
    parallelGroup: 'A',
    slaHours: 2,
  },
  {
    id: 305,
    templateId: 3,
    nodeName: '正式直播',
    nodeOrder: 4,
    nodeDescription: '进行直播带货',
    executorRole: '直播运营',
    needReview: 0,
    predecessors: [302, 303, 304],
    slaHours: 3,
  },
  {
    id: 306,
    templateId: 3,
    nodeName: '订单处理',
    nodeOrder: 5,
    nodeDescription: '处理直播订单',
    executorRole: '销售',
    needReview: 0,
    predecessors: [305],
    slaHours: 4,
  },
  {
    id: 307,
    templateId: 3,
    nodeName: '数据分析',
    nodeOrder: 6,
    nodeDescription: '分析直播数据',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [305],
    slaHours: 2,
  },
  {
    id: 308,
    templateId: 3,
    nodeName: '复盘总结',
    nodeOrder: 7,
    nodeDescription: '直播复盘和经验总结',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [306, 307],
    slaHours: 2,
  },
]

/**
 * Mock SOP节点数据 - 模板4: 公众号文章发布流程（5节点）
 */
export const mockSopNodesTemplate4: SopNodeVO[] = [
  {
    id: 401,
    templateId: 4,
    nodeName: '确定主题',
    nodeOrder: 1,
    nodeDescription: '确定文章主题和方向',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [],
    slaHours: 2,
  },
  {
    id: 402,
    templateId: 4,
    nodeName: '撰写文章',
    nodeOrder: 2,
    nodeDescription: '撰写公众号文章',
    executorRole: '公众号运营',
    needReview: 1,
    reviewerRole: '运营组长',
    predecessors: [401],
    slaHours: 6,
  },
  {
    id: 403,
    templateId: 4,
    nodeName: '排版设计',
    nodeOrder: 3,
    nodeDescription: '文章排版和配图',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [402],
    slaHours: 2,
  },
  {
    id: 404,
    templateId: 4,
    nodeName: '审核文章',
    nodeOrder: 4,
    nodeDescription: '审核文章内容和质量',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [403],
    slaHours: 1,
  },
  {
    id: 405,
    templateId: 4,
    nodeName: '发布推送',
    nodeOrder: 5,
    nodeDescription: '发布公众号推送',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [404],
    slaHours: 1,
  },
]

/**
 * Mock SOP节点数据 - 模板5: 多平台同步发布流程（9节点）
 */
export const mockSopNodesTemplate5: SopNodeVO[] = [
  {
    id: 501,
    templateId: 5,
    nodeName: '内容策划',
    nodeOrder: 1,
    nodeDescription: '策划多平台发布内容',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [],
    slaHours: 3,
  },
  {
    id: 502,
    templateId: 5,
    nodeName: '制作内容',
    nodeOrder: 2,
    nodeDescription: '制作图文和视频内容',
    executorRole: '公众号运营',
    needReview: 1,
    reviewerRole: '运营组长',
    predecessors: [501],
    slaHours: 8,
  },
  {
    id: 503,
    templateId: 5,
    nodeName: '公众号发布',
    nodeOrder: 3,
    nodeDescription: '发布到公众号',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [502],
    parallelGroup: 'A',
    slaHours: 1,
  },
  {
    id: 504,
    templateId: 5,
    nodeName: '抖音发布',
    nodeOrder: 3,
    nodeDescription: '发布到抖音',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [502],
    parallelGroup: 'A',
    slaHours: 1,
  },
  {
    id: 505,
    templateId: 5,
    nodeName: '快手发布',
    nodeOrder: 3,
    nodeDescription: '发布到快手',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [502],
    parallelGroup: 'A',
    slaHours: 1,
  },
  {
    id: 506,
    templateId: 5,
    nodeName: '小红书发布',
    nodeOrder: 3,
    nodeDescription: '发布到小红书',
    executorRole: '公众号运营',
    needReview: 0,
    predecessors: [502],
    parallelGroup: 'A',
    slaHours: 1,
  },
  {
    id: 507,
    templateId: 5,
    nodeName: '数据监控',
    nodeOrder: 4,
    nodeDescription: '监控各平台数据',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [503, 504, 505, 506],
    slaHours: 2,
  },
  {
    id: 508,
    templateId: 5,
    nodeName: '数据分析',
    nodeOrder: 5,
    nodeDescription: '分析多平台数据表现',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [507],
    slaHours: 3,
  },
  {
    id: 509,
    templateId: 5,
    nodeName: '优化策略',
    nodeOrder: 6,
    nodeDescription: '根据数据优化发布策略',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [508],
    slaHours: 2,
  },
]

/**
 * Mock审核任务列表数据
 */
export const mockSopReviewList: SopReviewVO[] = [
  {
    reviewId: 101,
    taskName: '5月第1期',
    nodeName: '写推文',
    executorName: '张三',
    reviewStatus: 'PENDING',
    submitTime: '2026-05-10 14:30:00',
  },
  {
    reviewId: 102,
    taskName: '5月第2期',
    nodeName: '剪辑视频',
    executorName: '李四',
    reviewStatus: 'PENDING',
    submitTime: '2026-05-12 16:00:00',
  },
]

/**
 * 模拟分页查询SOP模板列表
 */
export function mockGetSopTemplateList(
  pageNo: number,
  pageSize: number
): PageResult<SopTemplateVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockSopTemplateList.slice(start, end)
  
  return {
    total: mockSopTemplateList.length,
    list,
  }
}

/**
 * 模拟获取节点列表
 */
export function mockGetSopNodeList(templateId: number): SopNodeVO[] {
  const templateNodesMap: Record<number, SopNodeVO[]> = {
    1: mockSopNodes,
    2: mockSopNodesTemplate2,
    3: mockSopNodesTemplate3,
    4: mockSopNodesTemplate4,
    5: mockSopNodesTemplate5,
  }
  
  return templateNodesMap[templateId] || []
}

/**
 * 模拟获取节点列表（别名）
 */
export function mockGetSopNodes(templateId: number): SopNodeVO[] {
  return mockGetSopNodeList(templateId)
}

/**
 * 模拟DAG校验
 */
export function mockValidateDag(nodes: Array<{ id: number; predecessors: number[] }>): DagValidateResponse {
  // 简单的循环检测
  const visited = new Set<number>()
  const recStack = new Set<number>()

  function hasCycle(nodeId: number): boolean {
    visited.add(nodeId)
    recStack.add(nodeId)

    const node = nodes.find(n => n.id === nodeId)
    if (node) {
      for (const predId of node.predecessors) {
        if (!visited.has(predId)) {
          if (hasCycle(predId)) return true
        } else if (recStack.has(predId)) {
          return true
        }
      }
    }

    recStack.delete(nodeId)
    return false
  }

  for (const node of nodes) {
    if (!visited.has(node.id)) {
      if (hasCycle(node.id)) {
        return {
          valid: false,
          message: '流程存在循环依赖，请检查节点关系',
        }
      }
    }
  }

  return { valid: true }
}

/**
 * 模拟分页查询审核任务列表
 */
export function mockGetSopReviewList(
  pageNo: number,
  pageSize: number
): PageResult<SopReviewVO> {
  const start = (pageNo - 1) * pageSize
  const end = start + pageSize
  const list = mockSopReviewList.slice(start, end)
  
  return {
    total: mockSopReviewList.length,
    list,
  }
}
