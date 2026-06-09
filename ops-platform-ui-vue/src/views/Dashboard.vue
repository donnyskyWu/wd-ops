<template>
  <div class="dashboard">
    <!-- 核心KPI卡片 -->
    <div class="kpi-cards">
      <el-card shadow="hover" class="kpi-card" v-for="(card, index) in kpiCards" :key="index" @click="navigateTo(card.route)">
        <div class="kpi-content">
          <div class="kpi-icon" :style="{ backgroundColor: card.bgColor }">
            <el-icon :size="24" :color="card.color">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-value">
              <template v-if="kpiData && typeof kpiData[card.key] === 'number'">
                {{ card.format ? card.format(kpiData[card.key]) : formatNumber(kpiData[card.key]) }}
              </template>
              <template v-else>-</template>
            </div>
            <div class="kpi-label">{{ card.label }}</div>
            <div v-if="card.changeKey && kpiData && typeof kpiData[card.changeKey] === 'number'" 
                 class="kpi-change" 
                 :class="kpiData[card.changeKey] > 0 ? 'positive' : 'negative'">
              {{ kpiData[card.changeKey] > 0 ? '+' : '' }}{{ kpiData[card.changeKey] }}%
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 快捷入口 -->
    <el-card class="quick-access" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>快捷入口</span>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="(item, index) in quickAccess" :key="index">
          <div class="quick-item" @click="navigateTo(item.route)">
            <div class="quick-icon">
              <el-icon :size="28"><component :is="item.icon" /></el-icon>
            </div>
            <div class="quick-label">{{ item.label }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-section">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>账号数据概览</span>
            </div>
          </template>
          <div ref="accountChartRef" style="height: 300px" @click="navigateTo('/account-analysis')"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>近7天内容发布趋势</span>
            </div>
          </template>
          <div ref="contentChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办和预警 -->
    <el-row :gutter="16" class="todo-alert-section">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-button type="primary" link @click="navigateTo('/task')">查看全部</el-button>
            </div>
          </template>
          <div class="todo-list">
            <div v-for="(item, index) in displayTodoData" :key="index" class="todo-item" @click="navigateTo(item.route)">
              <div class="todo-dot" :style="{ backgroundColor: getTodoColor(item.type) }"></div>
              <div class="todo-info">
                <div class="todo-title">{{ item.title }}</div>
                <div class="todo-count">{{ item.count }} 项待处理</div>
              </div>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <el-empty v-if="displayTodoData.length === 0" description="暂无待办事项" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>预警通知</span>
              <el-button type="primary" link>查看全部</el-button>
            </div>
          </template>
          <div class="alert-list">
            <div v-for="(item, index) in displayAlertData" :key="index" class="alert-item">
              <el-tag :type="getAlertLevelType(item.alertLevel)" size="small">{{ getAlertLevelText(item.alertLevel) }}</el-tag>
              <div class="alert-info">
                <div class="alert-content">{{ item.alertContent }}</div>
                <div class="alert-time">{{ item.triggerTime }}</div>
              </div>
            </div>
            <el-empty v-if="displayAlertData.length === 0" description="暂无预警通知" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { 
  User, Star, DocumentCopy, Bell, TrendCharts, DataAnalysis,
  Files, ArrowRight
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getDashboardKpi,
  getAccountOverview,
  getContentOverview,
  getAlertList,
  getTodoList
} from '@/api/dashboard'
import type {
  DashboardHomeKpiVO,
  DashboardAccountOverviewVO,
  DashboardContentOverviewVO,
  DashboardAlertItemVO,
  DashboardTodoItemVO,
} from '@/types/dashboard'
import {
  mockDashboardKpi,
  mockAccountOverview,
  mockContentOverview,
  mockAlertList,
  mockTodoList
} from '@/mock/dashboard'

const router = useRouter()

// 当前日期
const currentDate = dayjs().format('YYYY年MM月DD日 dddd')

// 加载状态
const loading = ref(false)

// KPI数据 - 初始值使用Mock数据
const kpiData = ref<DashboardHomeKpiVO | null>(mockDashboardKpi)

// 账号数据概览 - 初始值使用Mock数据
const accountData = ref<DashboardAccountOverviewVO[]>([...mockAccountOverview])

// 内容数据概览 - 初始值使用Mock数据
const contentData = ref<DashboardContentOverviewVO[]>([...mockContentOverview])

// 预警通知列表 - 初始值使用Mock数据
const alertData = ref<DashboardAlertItemVO[]>([...mockAlertList])

// 过滤并限制显示的预警通知（最多3条）
const displayAlertData = computed(() => {
  return alertData.value.slice(0, 3)
})

// 待办事项列表 - 初始值使用Mock数据
const todoData = ref<DashboardTodoItemVO[]>([...mockTodoList])

// 过滤并限制显示的待办事项（最多3条，过滤count=0的项）
const displayTodoData = computed(() => {
  return todoData.value
    .filter(item => item.count > 0)
    .slice(0, 3)
})

// KPI卡片配置（用于显示）- 添加点击跳转路由
const kpiCards = [
  { label: '平台账号数', key: 'totalAccounts' as const, changeKey: 'accountChangeRate' as const, icon: User, color: '#f97316', bgColor: '#fef3c7', route: '/account-analysis' },
  { label: '粉丝总量', key: 'totalFollowers' as const, changeKey: 'followerChangeRate' as const, icon: Star, color: '#ec4899', bgColor: '#fce7f3', format: (val: number) => (val / 10000).toFixed(1) + '万', route: '/fans-analysis' },
  { label: '今日内容', key: 'todayContentCount' as const, changeKey: 'contentChangeRate' as const, icon: DocumentCopy, color: '#8b5cf6', bgColor: '#ede9fe', route: '/content' },
  { label: '待审核', key: 'pendingReviewCount' as const, icon: Bell, color: '#ef4444', bgColor: '#fee2e2', route: '/content' },
  { label: '预警数', key: 'alertCount' as const, icon: Bell, color: '#eab308', bgColor: '#fef9c3', route: '/data-report' },
]

// 快捷入口（符合PRD定义）
const quickAccess = [
  { label: 'IP组管理', icon: User, route: '/ip-group' },
  { label: '作者管理', icon: Star, route: '/author' },
  { label: '账号分析', icon: DataAnalysis, route: '/account-analysis' },
  { label: '内容管理', icon: Files, route: '/content' },
  { label: 'ROI分析', icon: TrendCharts, route: '/roi-analysis' },
  { label: '数据报表', icon: DocumentCopy, route: '/report' },
]

// 图表引用
const accountChartRef = ref<HTMLElement>()
const contentChartRef = ref<HTMLElement>()

// 加载Dashboard数据
const loadDashboardData = async () => {
  loading.value = true
  try {
    // 并行请求所有API
    const [kpi, accounts, contents, alerts, todos] = await Promise.all([
      getDashboardKpi(),
      getAccountOverview(),
      getContentOverview(),
      getAlertList(),
      getTodoList()
    ])
    
    kpiData.value = kpi || mockDashboardKpi
    accountData.value = accounts || [...mockAccountOverview]
    contentData.value = contents || [...mockContentOverview]
    alertData.value = alerts || [...mockAlertList]
    todoData.value = todos || [...mockTodoList]
  } catch (error) {
    console.error('加载Dashboard数据失败，降级使用Mock数据:', error)
    // 降级使用Mock数据
    kpiData.value = mockDashboardKpi
    accountData.value = [...mockAccountOverview]
    contentData.value = [...mockContentOverview]
    alertData.value = [...mockAlertList]
    todoData.value = [...mockTodoList]
  } finally {
    loading.value = false
  }
}

// 初始化图表
onMounted(() => {
  loadDashboardData()
  initAccountChart()
  initContentChart()
})

// 账号数据概览饼图
let accountChart: echarts.ECharts | null = null
const initAccountChart = () => {
  if (!accountChartRef.value || accountChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(initAccountChart, 100)
    return
  }
  if (accountChart) {
    accountChart.dispose()
    accountChart = null
  }
  const chart = echarts.init(accountChartRef.value)
  accountChart = chart
  
  // 监听数据变化，更新图表
  const updateChart = () => {
    const data = accountData.value.length > 0 ? accountData.value : mockAccountOverview
    
    const platformNames: Record<string, string> = {
      WECHAT_MP: '公众号',
      DOUYIN: '抖音',
      KUAISHOU: '快手',
      XIAOHONGSHU: '小红书',
      VIDEO_ACCOUNT: '视频号',
      SERVICE_ACCOUNT: '服务号',
    }
    
    const option = {
      tooltip: { trigger: 'item' },
      legend: { bottom: '5%', left: 'center' },
      series: [
        {
          name: '账号分布',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}: {d}%' },
          data: data.map(item => ({
            value: item.accountCount,
            name: platformNames[item.platformType] || item.platformType
          }))
        }
      ]
    }
    chart.setOption(option)
  }
  
  updateChart()
  window.addEventListener('resize', () => chart.resize())
}

// 内容发布趋势折线图
let contentChart: echarts.ECharts | null = null
const initContentChart = () => {
  if (!contentChartRef.value || contentChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(initContentChart, 100)
    return
  }
  if (contentChart) {
    contentChart.dispose()
    contentChart = null
  }
  const chart = echarts.init(contentChartRef.value)
  
  // 监听数据变化，更新图表
  const updateChart = () => {
    const data = contentData.value.length > 0 ? contentData.value : mockContentOverview
    
    const dates = data.map(item => item.date.substring(5)) // 提取MM-DD
    const wechatData = data.map(item => item.wechatCount)
    const douyinData = data.map(item => item.douyinCount)
    const xiaohongshuData = data.map(item => item.xiaohongshuCount)
    
    const option = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['公众号', '抖音', '小红书'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '公众号',
          type: 'line',
          smooth: true,
          data: wechatData,
          itemStyle: { color: '#1890ff' }
        },
        {
          name: '抖音',
          type: 'line',
          smooth: true,
          data: douyinData,
          itemStyle: { color: '#ff4d4f' }
        },
        {
          name: '小红书',
          type: 'line',
          smooth: true,
          data: xiaohongshuData,
          itemStyle: { color: '#faad14' }
        }
      ]
    }
    chart.setOption(option)
  }
  
  updateChart()
  window.addEventListener('resize', () => chart.resize())
}

// 路由跳转
const navigateTo = (route: string) => {
  router.push(route)
}

// 格式化数字（千分位）
const formatNumber = (num: number) => {
  return num.toLocaleString()
}

// 获取待办颜色
const getTodoColor = (type: string) => {
  const colors: Record<string, string> = {
    REVIEW: '#ef4444',
    TASK: '#3b82f6',
    EXPIRE: '#eab308',
  }
  return colors[type] || '#909399'
}

// 获取预警级别类型
const getAlertLevelType = (level: string) => {
  const types: Record<string, string> = {
    CRITICAL: 'danger',
    WARNING: 'warning',
    INFO: 'info',
  }
  return types[level] || 'info'
}

// 获取预警级别文本
const getAlertLevelText = (level: string) => {
  const texts: Record<string, string> = {
    CRITICAL: '严重',
    WARNING: '警告',
    INFO: '提示',
  }
  return texts[level] || level
}
</script>

<style scoped lang="scss">
.dashboard {
  padding: 20px 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);

  // 页面标题栏
  .dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 0 4px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }

    .header-actions {
      display: flex;
      gap: 8px;

      .el-button {
        background: #fff;
        border: 1px solid #ebeef5;
        color: #606266;

        &:hover {
          background: #ecf5ff;
          border-color: #1890ff;
          color: #1890ff;
        }
      }
    }
  }


  .kpi-cards {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 16px;
    margin-bottom: 20px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(3, 1fr);
    }

    .kpi-card {
      cursor: pointer;
      border: none;
      border-radius: 12px;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }

      :deep(.el-card__body) {
        padding: 20px;
      }

      .kpi-content {
        display: flex;
        align-items: center;
        gap: 16px;

        .kpi-icon {
          width: 56px;
          height: 56px;
          border-radius: 14px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .kpi-info {
          flex: 1;
          min-width: 0;

          .kpi-value {
            font-size: 24px;
            font-weight: 700;
            color: #303133;
            line-height: 1.2;
          }

          .kpi-label {
            font-size: 14px;
            color: #909399;
            margin-top: 6px;
          }

          .kpi-change {
            font-size: 12px;
            margin-top: 4px;
            font-weight: 500;

            &.positive {
              color: #67c23a;
            }

            &.negative {
              color: #f56c6c;
            }
          }
        }
      }
    }
  }

  // 快捷入口
  .quick-access {
    margin-bottom: 20px;
    border: none;
    border-radius: 12px;

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #f0f0f0;
    }

    :deep(.el-card__body) {
      padding: 16px 20px;
    }

    .card-header {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .quick-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 12px;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
        transform: translateY(-4px);

        .quick-icon {
          background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
          color: #fff;
          transform: scale(1.1);
        }
      }

      .quick-icon {
        width: 52px;
        height: 52px;
        border-radius: 12px;
        background: linear-gradient(135deg, #e6f7ff 0%, #b3e0ff 100%);
        color: #1890ff;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 12px;
        transition: all 0.3s ease;
      }

      .quick-label {
        font-size: 14px;
        font-weight: 500;
        color: #606266;
      }
    }
  }

  // 图表区域
  .chart-section {
    margin-bottom: 20px;

    .el-card {
      border: none;
      border-radius: 12px;
      height: 100%;

      :deep(.el-card__header) {
        padding: 16px 20px;
        border-bottom: 1px solid #f0f0f0;
      }

      :deep(.el-card__body) {
        padding: 16px 20px;
      }

      .card-header {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }
  }

  // 待办和预警
  .todo-alert-section {
    .el-card {
      border: none;
      border-radius: 12px;
      height: 100%;

      :deep(.el-card__header) {
        padding: 16px 20px;
        border-bottom: 1px solid #f0f0f0;
      }

      :deep(.el-card__body) {
        padding: 12px 20px;
      }

      .card-header {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .todo-list, .alert-list {
      .todo-item, .alert-item {
        display: flex;
        align-items: center;
        padding: 14px 12px;
        border-radius: 8px;
        transition: all 0.3s ease;
        cursor: pointer;
        margin-bottom: 8px;
        background: #fafafa;

        &:last-child {
          margin-bottom: 0;
        }

        &:hover {
          background: linear-gradient(135deg, #e6f7ff 0%, #b3e0ff 100%);
          transform: translateX(4px);
        }

        .todo-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          margin-right: 14px;
          flex-shrink: 0;
        }

        .todo-info, .alert-info {
          flex: 1;
          min-width: 0;

          .todo-title, .alert-content {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
            margin-bottom: 4px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .todo-count, .alert-time {
            font-size: 12px;
            color: #909399;
          }
        }

        .el-tag {
          margin-right: 12px;
          flex-shrink: 0;
        }
      }
    }
  }
}
</style>
