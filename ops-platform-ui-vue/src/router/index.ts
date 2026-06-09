import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      // 1. 首页仪表盘（合并简化版和完整版）
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页仪表盘' }
      },
      // 旧路由 /dashboard-simple 和 /dashboard-full 已删除（统一用 /dashboard）
      // 运营管理
      {
        path: '/ip-group',
        name: 'IpGroup',
        component: () => import('@/views/operations/IpGroup.vue'),
        meta: { title: 'IP组管理' }
      },
      {
        path: '/author',
        name: 'Author',
        component: () => import('@/views/operations/Author.vue'),
        meta: { title: '作者管理' }
      },
      {
        path: '/account-analysis',
        name: 'AccountAnalysis',
        component: () => import('@/views/operations/AccountAnalysis.vue'),
        meta: { title: '账号分析' }
      },
      {
        path: '/fans-analysis',
        name: 'FansAnalysis',
        component: () => import('@/views/operations/FansAnalysis.vue'),
        meta: { title: '粉丝分析' }
      },
      {
        path: '/works-analysis',
        name: 'WorksAnalysis',
        component: () => import('@/views/operations/WorksAnalysis.vue'),
        meta: { title: '作品分析' }
      },
      {
        path: '/internal-content',
        name: 'InternalContent',
        component: () => import('@/views/operations/InternalContent.vue'),
        meta: { title: '内部内容分析' }
      },
      {
        path: '/internal-content/:id',
        name: 'InternalContentDetail',
        component: () => import('@/views/operations/InternalContentDetail.vue'),
        meta: { title: '补录详情' }
      },
      {
        path: '/author/:id/dashboard',
        name: 'AuthorDashboard',
        component: () => import('@/views/operations/AuthorDashboard.vue'),
        meta: { title: '作者看板' }
      },
      {
        path: '/efficiency',
        name: 'Efficiency',
        component: () => import('@/views/operations/Efficiency.vue'),
        meta: { title: '人效盘点' }
      },
      // 运营管理 - 内容分析(已迁移到 M1 新路径，删除老路径)
      // 运营管理 - 粉丝数据统计(已迁移到 M1 新路径，删除老路径)
      // 运营管理 - 账号数据统计(已迁移到 /account-analysis，删除老路径)
      // 内容生产 - SOP管理
      {
        path: '/sop',
        name: 'Sop',
        component: () => import('@/views/production/sop/index.vue'),
        meta: { title: 'SOP管理' }
      },
      // 内容生产 - SOP审核（独立页）
      {
        path: '/sop/review',
        name: 'SopReview',
        component: () => import('@/views/production/sop/review.vue'),
        meta: { title: 'SOP审核' }
      },
      // 内容生产 - SOP编辑
      {
        path: '/sop/:id/edit',
        name: 'SopEdit',
        component: () => import('@/views/production/sop/edit.vue'),
        meta: { title: '编辑SOP模板' }
      },
      // 内容生产 - 任务管理
      {
        path: '/task',
        name: 'Task',
        component: () => import('@/views/production/task/index.vue'),
        meta: { title: '任务管理' }
      },
      // 内容生产 - 内容管理
      {
        path: '/content',
        name: 'Content',
        component: () => import('@/views/production/content/index.vue'),
        meta: { title: '内容管理' }
      },
      // 内容生产 - 内容创作/编辑
      {
        path: '/content/edit',
        name: 'ContentEdit',
        component: () => import('@/views/production/content/edit.vue'),
        meta: { title: '内容创作' }
      },
      {
        path: '/content/edit/:id',
        name: 'ContentEditWithId',
        component: () => import('@/views/production/content/edit.vue'),
        meta: { title: '内容编辑' }
      },
      // 内容生产 - 内容审核
      {
        path: '/content/review',
        name: 'ContentReview',
        component: () => import('@/views/production/content/review.vue'),
        meta: { title: '内容审核' }
      },
      // 内容生产 - 内容知识库
      {
        path: '/knowledge',
        name: 'Knowledge',
        component: () => import('@/views/production/knowledge/index.vue'),
        meta: { title: '内容知识库' }
      },
      // 内容生产 - 计划管理
      {
        path: '/plan',
        name: 'Plan',
        component: () => import('@/views/production/plan/index.vue'),
        meta: { title: '计划管理' }
      },
      // 绩效核算 - 考核模板
      {
        path: '/perf-template',
        name: 'PerfTemplate',
        component: () => import('@/views/performance/PerfTemplate.vue'),
        meta: { title: '考核模板' }
      },
      // 绩效核算 - 考核执行
      {
        path: '/perf-execution',
        name: 'PerfExecution',
        component: () => import('@/views/performance/PerfExecution.vue'),
        meta: { title: '考核执行' }
      },
      // 绩效核算 - 绩效结果
      {
        path: '/perf-result',
        name: 'PerfResult',
        component: () => import('@/views/performance/PerfResult.vue'),
        meta: { title: '绩效结果' }
      },
      // 绩效核算 - 订单归因分析
      {
        path: '/order-attribution',
        name: 'OrderAttribution',
        component: () => import('@/views/performance/OrderAttribution.vue'),
        meta: { title: '订单归因分析' }
      },
      // 旧路由 /perf/order-attribution 已删除（统一用 /order-attribution）
      {
        path: '/perf/order-attribution/roi',
        name: 'OrderAttributionRoi',
        component: () => import('@/views/performance/OrderAttributionRoi.vue'),
        meta: { title: 'ROI 分析' }
      },
      {
        path: '/perf/template/:id',
        name: 'PerfTemplateEdit',
        component: () => import('@/views/performance/PerfTemplateEdit.vue'),
        meta: { title: '模板编辑' }
      },
      {
        path: '/perf/record/:id',
        name: 'PerfRecordDetail',
        component: () => import('@/views/performance/PerfRecordDetail.vue'),
        meta: { title: '执行详情' }
      },
      {
        path: '/perf/result/:userId/trend',
        name: 'PerfUserTrend',
        component: () => import('@/views/performance/PerfUserTrend.vue'),
        meta: { title: '个人绩效趋势' }
      },
      // 内部管理 - 公司管理
      {
        path: '/company',
        name: 'Company',
        component: () => import('@/views/internal/CompanyManage.vue'),
        meta: { title: '公司管理' }
      },
      {
        path: '/company/:id',
        name: 'CompanyDetail',
        component: () => import('@/views/internal/CompanyDetail.vue'),
        meta: { title: '公司详情' }
      },
      // 内部管理 - 实名人管理
      {
        path: '/realname',
        name: 'Realname',
        component: () => import('@/views/internal/RealnameManage.vue'),
        meta: { title: '实名人管理' }
      },
      {
        path: '/realname/:id',
        name: 'RealnameDetail',
        component: () => import('@/views/internal/RealnameDetail.vue'),
        meta: { title: '实名人详情' }
      },
      // 内部管理 - 手机管理
      {
        path: '/phone',
        name: 'Phone',
        component: () => import('@/views/internal/PhoneManage.vue'),
        meta: { title: '手机管理' }
      },
      // 内部管理 - 手机卡管理
      {
        path: '/simcard',
        name: 'Simcard',
        component: () => import('@/views/internal/SimcardManage.vue'),
        meta: { title: '手机卡管理' }
      },
      {
        path: '/simcard/:id/linked',
        name: 'SimCardLinked',
        component: () => import('@/views/internal/SimCardLinked.vue'),
        meta: { title: '跨平台查询' }
      },
      // 内部管理 - 内部平台账号管理
      {
        path: '/internal-account',
        name: 'InternalAccount',
        component: () => import('@/views/internal/InternalAccountManage.vue'),
        meta: { title: '内部平台账号管理' }
      },
      {
        path: '/platform-account/:id',
        name: 'PlatformAccountDetail',
        component: () => import('@/views/internal/PlatformAccountDetail.vue'),
        meta: { title: '平台账号详情' }
      },
      // 内部管理 - 个人账号管理
      {
        path: '/personal-account',
        name: 'PersonalAccount',
        component: () => import('@/views/internal/PersonalAccountManage.vue'),
        meta: { title: '个人账号管理' }
      },
      // 内部管理 - 三方关联统计
      {
        path: '/triple-rel',
        name: 'TripleRel',
        component: () => import('@/views/internal/TripleRelManage.vue'),
        meta: { title: '三方关联统计' }
      },
      // 数据分析 - 指标管理
      {
        path: '/metric',
        name: 'Metric',
        component: () => import('@/views/analysis/MetricManage.vue'),
        meta: { title: '指标管理' }
      },
      // 数据分析 - 指标分析（新增）
      {
        path: '/metric-analysis',
        name: 'MetricAnalysis',
        component: () => import('@/views/analysis/MetricAnalysis.vue'),
        meta: { title: '指标分析' }
      },
      // 数据分析 - 数据报表（入口页）
      {
        path: '/data-report',
        name: 'DataReport',
        component: () => import('@/views/analysis/ReportCenter.vue'),
        meta: { title: '数据报表' }
      },
      {
        path: '/analysis/report/unified-account',
        name: 'ReportUnifiedAccount',
        component: () => import('@/views/analysis/ReportUnifiedAccount.vue'),
        meta: { title: '统一视图' }
      },
      {
        path: '/analysis/report/account-status',
        name: 'ReportAccountStatus',
        component: () => import('@/views/analysis/ReportAccountStatus.vue'),
        meta: { title: '状态监控' }
      },
      {
        path: '/analysis/report/video-output',
        name: 'ReportVideoOutput',
        component: () => import('@/views/analysis/ReportVideoOutput.vue'),
        meta: { title: '短视频产出' }
      },
      {
        path: '/analysis/report/live-duration',
        name: 'ReportLiveDuration',
        component: () => import('@/views/analysis/ReportLiveDuration.vue'),
        meta: { title: '直播时长' }
      },
      {
        path: '/analysis/report/cost-allocation',
        name: 'ReportCostAllocation',
        component: () => import('@/views/analysis/ReportCostAllocation.vue'),
        meta: { title: '成本分摊' }
      },
      {
        path: '/analysis/report/roi',
        name: 'ReportRoi',
        component: () => import('@/views/analysis/ReportRoi.vue'),
        meta: { title: 'ROI 分析' }
      },
      {
        path: '/analysis/report/team-config',
        name: 'ReportTeamConfig',
        component: () => import('@/views/analysis/ReportTeamConfig.vue'),
        meta: { title: '团队配置' }
      },
      {
        path: '/analysis/report/account-alert',
        name: 'ReportAccountAlert',
        component: () => import('@/views/analysis/ReportAccountAlert.vue'),
        meta: { title: '异常预警' }
      },
      // 数据分析 - 自定义查询
      {
        path: '/custom-query',
        name: 'CustomQuery',
        component: () => import('@/views/analysis/CustomQuery.vue'),
        meta: { title: '自定义查询' }
      },
      // 数据分析 - 数据大屏
      {
        path: '/data-screen',
        name: 'DataScreen',
        component: () => import('@/views/screen/DataScreen.vue'),
        meta: { title: '数据大屏' }
      },
      // 数据分析 - 大屏配置
      {
        path: '/screen-config',
        name: 'ScreenConfig',
        component: () => import('@/views/screen/ScreenConfig.vue'),
        meta: { title: '大屏配置' }
      },
      // 数据分析 - 总体财务分析（新增）
      {
        path: '/financial-analysis',
        name: 'FinancialAnalysis',
        component: () => import('@/views/finance/FinancialAnalysis.vue'),
        meta: { title: '总体财务分析' }
      },
      // 数据分析 - 漏斗分析（新增）
      {
        path: '/funnel-analysis',
        name: 'FunnelAnalysis',
        component: () => import('@/views/analysis/FunnelAnalysis.vue'),
        meta: { title: '漏斗分析' }
      },
      // 数据分析 - 微信数据分析
      {
        path: '/wechat-data',
        name: 'WechatData',
        component: () => import('@/views/analysis/WechatDataAnalysis.vue'),
        meta: { title: '微信数据分析' }
      },
      // 数据分析 - 行业数据
      {
        path: '/industry-data',
        name: 'IndustryData',
        component: () => import('@/views/analysis/IndustryData.vue'),
        meta: { title: '行业数据' }
      },
      // 财务管理 - 账号成本管理
      {
        path: '/account-cost',
        name: 'AccountCost',
        component: () => import('@/views/finance/AccountCostManage.vue'),
        meta: { title: '账号成本管理' }
      },
      // 财务管理 - ROI分析
      {
        path: '/roi-analysis',
        name: 'RoiAnalysis',
        component: () => import('@/views/finance/RoiAnalysis.vue'),
        meta: { title: 'ROI分析' }
      },
      {
        path: '/finance/roi/trend',
        name: 'RoiTrend',
        component: () => import('@/views/finance/RoiTrend.vue'),
        meta: { title: 'ROI 趋势' }
      },
      {
        path: '/finance/cost/edit',
        name: 'CostEntry',
        component: () => import('@/views/finance/CostEntry.vue'),
        meta: { title: '成本录入' }
      },
      // 作品监测 - 外部账号分析
      {
        path: '/external-account',
        name: 'ExternalAccount',
        component: () => import('@/views/account/ExternalAccountAnalysis.vue'),
        meta: { title: '外部账号分析' }
      },
      // 作品监测 - 低分作品分析
      {
        path: '/low-score',
        name: 'LowScore',
        component: () => import('@/views/content/LowScoreAnalysis.vue'),
        meta: { title: '低分作品分析' }
      },
      // 作品监测 - 爆款作品分析
      {
        path: '/hot-works',
        name: 'HotWorks',
        component: () => import('@/views/content/HotWorksAnalysis.vue'),
        meta: { title: '爆款作品分析' }
      },
      // 作品监测 - 高粉账号分析
      {
        path: '/high-fans-account',
        name: 'HighFansAccount',
        component: () => import('@/views/account/HighFansAccountAnalysis.vue'),
        meta: { title: '高粉账号分析' }
      },
      // 作品监测 - 低粉账号分析
      {
        path: '/low-fans-account',
        name: 'LowFansAccount',
        component: () => import('@/views/account/LowFansAccountAnalysis.vue'),
        meta: { title: '低粉账号分析' }
      },
      // 作品监测 - IP主题数据
      {
        path: '/ip-theme',
        name: 'IPTheme',
        component: () => import('@/views/content/IPThemeData.vue'),
        meta: { title: 'IP主题数据' }
      },
      // 作品监测 - IP主题数据（行业数据已迁移到 M6 /analysis/industry-data）
      // ===== 9. 配置管理（7个独立子页面）=====
      // 配置管理 - 内部采集配置
      {
        path: '/config-internal-collect',
        name: 'ConfigInternalCollect',
        component: () => import('@/views/config/InternalCollectConfig.vue'),
        meta: { title: '内部采集配置' }
      },
      // 配置管理 - 外部采集配置
      {
        path: '/config-external-collect',
        name: 'ConfigExternalCollect',
        component: () => import('@/views/config/ExternalCollectConfig.vue'),
        meta: { title: '外部采集配置' }
      },
      // 配置管理 - 外部数据配置
      {
        path: '/config-external-data',
        name: 'ConfigExternalData',
        component: () => import('@/views/config/ExternalDataConfig.vue'),
        meta: { title: '外部数据配置' }
      },
      // 配置管理 - 订单采集配置
      {
        path: '/config-order-collect',
        name: 'ConfigOrderCollect',
        component: () => import('@/views/config/OrderCollectConfig.vue'),
        meta: { title: '订单采集配置' }
      },
      // 配置管理 - 阈值规则配置
      {
        path: '/config-threshold',
        name: 'ConfigThreshold',
        component: () => import('@/views/config/ThresholdConfig.vue'),
        meta: { title: '阈值规则配置' }
      },
      // 配置管理 - AI模型
      {
        path: '/config-ai-model',
        name: 'ConfigAiModel',
        component: () => import('@/views/config/AiModelConfig.vue'),
        meta: { title: 'AI模型' }
      },
      // 配置管理 - AI提示词
      {
        path: '/config-ai-prompt',
        name: 'ConfigAiPrompt',
        component: () => import('@/views/config/AiPromptConfig.vue'),
        meta: { title: 'AI提示词' }
      },

      // ===== 10. 系统管理（7个独立子页面）=====
      // 系统管理 - 用户管理
      {
        path: '/system-user',
        name: 'SystemUser',
        component: () => import('@/views/system/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      // 系统管理 - 角色权限
      {
        path: '/system-role',
        name: 'SystemRole',
        component: () => import('@/views/system/RoleManage.vue'),
        meta: { title: '角色权限' }
      },
      // 系统管理 - 租户管理
      {
        path: '/system-tenant',
        name: 'SystemTenant',
        component: () => import('@/views/system/TenantManage.vue'),
        meta: { title: '租户管理' }
      },
      // 系统管理 - 系统参数
      {
        path: '/system-param',
        name: 'SystemParam',
        component: () => import('@/views/system/ParamManage.vue'),
        meta: { title: '系统参数' }
      },
      // 系统管理 - 字典配置
      {
        path: '/system-dict',
        name: 'SystemDict',
        component: () => import('@/views/system/DictManage.vue'),
        meta: { title: '字典配置' }
      },
      // 系统管理 - 操作日志
      {
        path: '/system-log/operation',
        name: 'SystemLogOperation',
        component: () => import('@/views/system/LogManage.vue'),
        meta: { title: '操作日志' }
      },
      // 系统管理 - 登录日志
      {
        path: '/system-log/login',
        name: 'SystemLogLogin',
        component: () => import('@/views/system/LoginLog.vue'),
        meta: { title: '登录日志' }
      },
      // 系统管理 - 消息管理
      {
        path: '/system-message',
        name: 'SystemMessage',
        component: () => import('@/views/system/MessageManage.vue'),
        meta: { title: '消息管理' }
      },

      // ===== 11. 数据采集(M10)=====
      {
        path: '/collect/task',
        name: 'CollectTask',
        component: () => import('@/views/collect/task.vue'),
        meta: { title: '采集任务' }
      },
      {
        path: '/collect/task/:id',
        name: 'CollectTaskEdit',
        component: () => import('@/views/collect/task-edit.vue'),
        meta: { title: '编辑采集任务' }
      },
      {
        path: '/collect/log',
        name: 'CollectLog',
        component: () => import('@/views/collect/log.vue'),
        meta: { title: '采集日志' }
      },
      {
        path: '/collect/quality',
        name: 'CollectQuality',
        component: () => import('@/views/collect/quality.vue'),
        meta: { title: '数据质量' }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
