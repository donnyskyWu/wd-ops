/**
 * 9 大模块路由清单（与 router/index.ts 完全对齐）
 *
 * 用于自动化测试 - 路由可达性烟雾测试
 * 任何前端修改如果导致路由变 404/白屏 都会被这里捕获
 *
 * 模块归属:
 *   M0 首页         M1 运营管理       M2 内容生产
 *   M3 绩效核算     M4 账号管理       M5 财务管理
 *   M6 数据分析     M7 作品监测       M8 配置管理
 *   M9 系统管理     M10 数据采集
 */
export interface RouteEntry {
  module: string
  path: string
  title: string
  /** 测试时可跳过（动态参数路由 / 拖拽编辑等需要特殊上下文的） */
  skip?: boolean
  skipReason?: string
}

export const ALL_ROUTES: RouteEntry[] = [
  // M0 首页
  { module: 'M0', path: '/dashboard', title: '首页仪表盘' },

  // M1 运营管理
  { module: 'M1', path: '/ip-group', title: 'IP组管理' },
  { module: 'M1', path: '/author', title: '作者管理' },
  { module: 'M1', path: '/account-analysis', title: '账号分析' },
  { module: 'M1', path: '/fans-analysis', title: '粉丝分析' },
  { module: 'M1', path: '/works-analysis', title: '作品分析' },
  { module: 'M1', path: '/internal-content', title: '内部内容分析' },
  { module: 'M1', path: '/internal-content/1', title: '内部内容详情', skip: true, skipReason: '需特定 ID 数据' },
  { module: 'M1', path: '/author/1/dashboard', title: '作者看板', skip: true, skipReason: '需特定 ID 数据' },
  { module: 'M1', path: '/efficiency', title: '人效盘点' },

  // M2 内容生产
  { module: 'M2', path: '/sop', title: 'SOP管理' },
  { module: 'M2', path: '/sop/review', title: 'SOP审核' },
  { module: 'M2', path: '/sop/1/edit', title: 'SOP编辑', skip: true, skipReason: 'LogicFlow 拖拽需宽屏' },
  { module: 'M2', path: '/task', title: '任务管理' },
  { module: 'M2', path: '/task/1/execute', title: '任务执行', skip: true, skipReason: '需有效任务 ID 与执行人权限' },
  { module: 'M2', path: '/content', title: '内容管理' },
  { module: 'M2', path: '/content/edit', title: '内容新增' },
  { module: 'M2', path: '/content/review', title: '内容审核' },
  { module: 'M2', path: '/knowledge', title: '知识库' },
  { module: 'M2', path: '/plan', title: '计划管理' },

  // M3 绩效核算
  { module: 'M3', path: '/perf-template', title: '考核模板' },
  { module: 'M3', path: '/perf-execution', title: '考核执行' },
  { module: 'M3', path: '/perf-result', title: '绩效结果' },
  { module: 'M3', path: '/order-attribution', title: '订单归因' },
  { module: 'M3', path: '/perf/order-attribution/roi', title: '订单归因ROI', skip: true, skipReason: '需父级数据' },
  { module: 'M3', path: '/perf/template/new', title: '模板编辑', skip: true, skipReason: '新增模板特殊路由' },
  { module: 'M3', path: '/perf/record/1', title: '考核记录详情', skip: true, skipReason: '需特定 ID' },
  { module: 'M3', path: '/perf/result/1/trend', title: '个人绩效趋势', skip: true, skipReason: '需特定 userId' },

  // M4 账号管理
  { module: 'M4', path: '/company', title: '公司管理' },
  { module: 'M4', path: '/company/1', title: '公司详情', skip: true, skipReason: '需特定 ID' },
  { module: 'M4', path: '/realname', title: '实名人' },
  { module: 'M4', path: '/realname/1', title: '实名人详情', skip: true, skipReason: '需特定 ID' },
  { module: 'M4', path: '/phone', title: '手机管理' },
  { module: 'M4', path: '/simcard', title: '手机卡管理' },
  { module: 'M4', path: '/simcard/1/linked', title: '跨平台查询', skip: true, skipReason: '需特定 ID' },
  { module: 'M4', path: '/internal-account', title: '内部平台账号' },
  { module: 'M4', path: '/platform-account/1', title: '平台账号详情', skip: true, skipReason: '需特定 ID' },
  { module: 'M4', path: '/personal-account', title: '个人账号' },
  { module: 'M4', path: '/triple-rel', title: '三方关联' },

  // M6 数据分析
  { module: 'M6', path: '/metric', title: '指标管理' },
  { module: 'M6', path: '/metric-analysis', title: '指标分析' },
  { module: 'M6', path: '/data-report', title: '数据报表中心' },
  { module: 'M6', path: '/analysis/report/unified-account', title: '统一账号报表' },
  { module: 'M6', path: '/analysis/report/account-status', title: '账号状态报表' },
  { module: 'M6', path: '/analysis/report/video-output', title: '视频产出报表' },
  { module: 'M6', path: '/analysis/report/live-duration', title: '直播时长报表' },
  { module: 'M6', path: '/analysis/report/cost-allocation', title: '成本分摊报表' },
  { module: 'M6', path: '/analysis/report/roi', title: 'ROI 报表' },
  { module: 'M6', path: '/analysis/report/team-config', title: '团队配置报表' },
  { module: 'M6', path: '/analysis/report/account-alert', title: '账号预警报表' },
  { module: 'M6', path: '/custom-query', title: '自定义查询' },
  { module: 'M6', path: '/data-screen', title: '数据大屏' },
  { module: 'M6', path: '/screen-config', title: '大屏配置' },
  { module: 'M6', path: '/financial-analysis', title: '财务分析' },
  { module: 'M6', path: '/funnel-analysis', title: '漏斗分析' },
  { module: 'M6', path: '/wechat-data', title: '微信数据分析' },
  { module: 'M6', path: '/industry-data', title: '行业数据' },

  // M5 财务管理
  { module: 'M5', path: '/account-cost', title: '账号成本' },
  { module: 'M5', path: '/roi-analysis', title: 'ROI分析' },
  { module: 'M5', path: '/finance/roi/trend', title: 'ROI趋势' },
  { module: 'M5', path: '/finance/cost/edit', title: '成本录入' },

  // M7 作品监测
  { module: 'M7', path: '/external-account', title: '外部账号分析' },
  { module: 'M7', path: '/low-score', title: '低分作品' },
  { module: 'M7', path: '/hot-works', title: '爆款作品' },
  { module: 'M7', path: '/high-fans-account', title: '高粉账号' },
  { module: 'M7', path: '/low-fans-account', title: '低粉账号' },
  { module: 'M7', path: '/ip-theme', title: 'IP主题数据' },

  // M8 配置管理
  { module: 'M8', path: '/config-internal-collect', title: '内部采集配置' },
  { module: 'M8', path: '/config-external-collect', title: '外部采集配置' },
  { module: 'M8', path: '/config-external-data', title: '外部数据配置' },
  { module: 'M8', path: '/config-order-collect', title: '订单采集配置' },
  { module: 'M8', path: '/config-threshold', title: '阈值规则' },
  { module: 'M8', path: '/config-ai-model', title: 'AI模型配置' },
  { module: 'M8', path: '/config-ai-prompt', title: 'AI提示词配置' },

  // M9 系统管理
  { module: 'M9', path: '/system-user', title: '用户管理' },
  { module: 'M9', path: '/system-role', title: '角色权限' },
  { module: 'M9', path: '/system-tenant', title: '租户管理' },
  { module: 'M9', path: '/system-param', title: '系统参数' },
  { module: 'M9', path: '/system-dict', title: '字典配置' },
  { module: 'M9', path: '/system-log/operation', title: '操作日志' },
  { module: 'M9', path: '/system-log/login', title: '登录日志' },
  { module: 'M9', path: '/system-message', title: '消息管理' },

  // M10 数据采集
  { module: 'M10', path: '/collect/task', title: '采集任务' },
  { module: 'M10', path: '/collect/task/1', title: '采集任务详情', skip: true, skipReason: '需特定 ID' },
  { module: 'M10', path: '/collect/log', title: '采集日志' },
  { module: 'M10', path: '/collect/quality', title: '数据质量' },
]

/** 仅"可访问"的路由（去掉 skip） */
export const ACCESSIBLE_ROUTES = ALL_ROUTES.filter((r) => !r.skip)
