<template>
  <div class="dashboard" v-loading="loading">
    <!-- 顶部操作栏 F-IP / F-DATE-RANGE / BTN-REFRESH -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-form :inline="true" class="toolbar-form">
          <el-form-item label="IP 组筛选">
            <IpGroupTreeSelect v-model="filters.ipGroupId" placeholder="全部" clearable style="width: 220px" />
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              :shortcuts="dateShortcuts"
              style="width: 260px"
            />
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="refreshing" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>

    <el-alert
      v-if="loadError"
      type="error"
      :title="loadError"
      show-icon
      class="error-banner"
      :closable="false"
    >
      <template #default>
        <el-button type="primary" link @click="loadAllData">重试</el-button>
      </template>
    </el-alert>

    <!-- 快捷入口 -->
    <el-card v-if="quickActions.length" class="quick-access" shadow="hover">
      <template #header>
        <span class="card-header">快捷入口</span>
      </template>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :md="6" :lg="3" v-for="(item, index) in quickActions" :key="index">
          <div
            class="quick-item"
            role="link"
            tabindex="0"
            @click="navigateTo(resolveOpsUrl(item.url))"
            @keydown.enter="navigateTo(resolveOpsUrl(item.url))"
          >
            <div class="quick-icon">
              <el-icon :size="28"><component :is="resolveQuickIcon(item.icon)" /></el-icon>
            </div>
            <div class="quick-label">{{ item.name }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 4 个核心指标卡 -->
    <div class="kpi-cards">
      <el-card shadow="hover" class="kpi-card" v-for="card in kpiCards" :key="card.key">
        <div class="kpi-content">
          <div class="kpi-icon" :style="{ backgroundColor: card.bgColor }">
            <el-icon :size="24" :color="card.color">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-value">{{ formatMetricValue(card) }}</div>
            <div class="kpi-label">{{ card.label }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-section">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header chart-header">
              <span>内容发布趋势</span>
              <div class="chart-filter">
                <span class="filter-label">分组</span>
                <el-select v-model="trendGroupBy" style="width: 120px" @change="loadTrendOnly">
                  <el-option label="按平台" value="PLATFORM" />
                  <el-option label="按 IP 小组" value="IP_GROUP" />
                </el-select>
                <span class="filter-label">平台</span>
                <DictSelect
                  v-model="chartPlatform"
                  dict-type="dict_platform_type"
                  placeholder="全部"
                  clearable
                  style="width: 160px"
                  @change="loadTrendOnly"
                />
              </div>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-box"></div>
          <el-empty v-if="!loading && trendPoints.length === 0" description="暂无趋势数据" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>
            <span class="card-header">平台分布</span>
          </template>
          <div ref="pieChartRef" class="chart-box"></div>
          <el-empty v-if="!loading && platformDist.length === 0" description="暂无分布数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办提醒 -->
    <el-card shadow="hover" class="todo-section">
      <template #header>
        <span class="card-header">待办提醒</span>
      </template>
      <el-table :data="todoList" stripe style="width: 100%" @row-click="handleTodoClick">
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ formatTodoSource(row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="navigateTo(resolveOpsUrl(row.actionUrl))">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && todoList.length === 0" description="暂无待办" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  User, Star, DocumentCopy, TrendCharts, DataAnalysis, Files, Refresh, Setting, OfficeBuilding,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import DictSelect from '@/components/DictSelect.vue'
import { PLATFORM_LABEL, type PlatformType } from '@/utils/enum-alias'
import {
  getHomeMetrics,
  getHomeTrend,
  getPlatformDist,
  getHomeTodos,
  getQuickActions,
  refreshHome,
  type HomeMetricsVO,
  type TrendPointVO,
  type PlatformDistVO,
  type HomeTodoVO,
  type QuickActionVO,
  type HomeQueryParams,
} from '@/api/dashboard'

const router = useRouter()

const loading = ref(false)
const refreshing = ref(false)
const loadError = ref('')

const filters = reactive({
  ipGroupId: undefined as number | undefined,
  dateRange: getDefaultDateRange() as string[],
})

const chartPlatform = ref<string | undefined>(undefined)
const trendGroupBy = ref<'PLATFORM' | 'IP_GROUP'>('PLATFORM')

const metrics = ref<HomeMetricsVO | null>(null)
const trendPoints = ref<TrendPointVO[]>([])
const platformDist = ref<PlatformDistVO[]>([])
const todoList = ref<HomeTodoVO[]>([])
const quickActions = ref<QuickActionVO[]>([])

const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const dateShortcuts = [
  {
    text: '今天',
    value: () => {
      const d = dayjs().format('YYYY-MM-DD')
      return [d, d]
    },
  },
  {
    text: '近 7 天',
    value: () => getDefaultDateRange(),
  },
  {
    text: '近 30 天',
    value: () => {
      const end = dayjs()
      const start = end.subtract(29, 'day')
      return [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
    },
  },
]

const kpiCards = [
  { label: '总作者数', key: 'totalAuthors' as const, icon: User, color: '#1890ff', bgColor: '#e6f7ff', isPercent: false },
  { label: '内容总数', key: 'totalContent' as const, icon: DocumentCopy, color: '#8b5cf6', bgColor: '#ede9fe', isPercent: false },
  { label: 'SOP 完成率', key: 'sopCompletionRate' as const, icon: TrendCharts, color: '#52c41a', bgColor: '#f6ffed', isPercent: true },
  { label: '平均绩效', key: 'avgPerfGrade' as const, icon: Star, color: '#fa8c16', bgColor: '#fff7e6', isPercent: false, isGrade: true },
]

const SOURCE_LABEL: Record<string, string> = {
  SOP: 'SOP',
  PUBLISH: '发布',
  PERF: '绩效',
  INTEGRATION: '集成',
}

const QUICK_ICON_MAP: Record<string, typeof User> = {
  'icon-ip-group': User,
  'icon-author': Star,
  'icon-account': DataAnalysis,
  'icon-sop': Files,
  'icon-perf': TrendCharts,
  'icon-report': DocumentCopy,
  'icon-user': Setting,
  'icon-tenant': OfficeBuilding,
}

function getDefaultDateRange(): string[] {
  const end = dayjs()
  const start = end.subtract(6, 'day')
  return [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
}

function buildQueryParams(): HomeQueryParams {
  const params: HomeQueryParams = {
    ipGroupId: filters.ipGroupId,
  }
  if (filters.dateRange?.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  return params
}

function resolveOpsUrl(url: string): string {
  if (!url) return '/dashboard'
  const map: Record<string, string> = {
    '/ops/ip-group': '/ip-group',
    '/ops/author': '/author',
    '/ops/account': '/internal-account',
    '/ops/sop': '/sop',
    '/ops/perf': '/perf-template',
    '/ops/report': '/data-report',
    '/ops/system/user': '/system-user',
    '/ops/system/tenant': '/system-tenant',
  }
  for (const [from, to] of Object.entries(map)) {
    if (url === from || url.startsWith(from + '/')) {
      return url.replace(from, to)
    }
  }
  if (url.startsWith('/ops/')) {
    return url.replace('/ops/', '/')
  }
  return url
}

function resolveQuickIcon(icon: string) {
  return QUICK_ICON_MAP[icon] || Files
}

function formatCompactNumber(num: number): string {
  if (num >= 1_000_000) return '100w+'
  if (num >= 10_000) return (num / 10_000).toFixed(1) + 'w'
  return num.toLocaleString()
}

function formatMetricValue(card: (typeof kpiCards)[number]): string {
  if (!metrics.value) return '-'
  const raw = metrics.value[card.key]
  if (card.isGrade) {
    return raw && raw !== '-' ? String(raw) : '-'
  }
  if (typeof raw !== 'number') return '-'
  if (card.isPercent) return `${raw.toFixed(2)}%`
  return formatCompactNumber(raw)
}

function formatTodoSource(source: string) {
  return SOURCE_LABEL[source] || source
}

function platformLabel(platform: string) {
  return PLATFORM_LABEL[platform as PlatformType] || platform
}

async function loadAllData() {
  loading.value = true
  loadError.value = ''
  const params = buildQueryParams()
  try {
    const [m, trend, dist, todos, actions] = await Promise.all([
      getHomeMetrics(params),
      getHomeTrend({ ...params, platformType: chartPlatform.value, groupBy: trendGroupBy.value }),
      getPlatformDist(params),
      getHomeTodos({ ipGroupId: params.ipGroupId, limit: 10 }),
      getQuickActions(),
    ])
    metrics.value = m
    trendPoints.value = trend || []
    platformDist.value = dist || []
    todoList.value = todos || []
    quickActions.value = actions || []
    await nextTick()
    renderTrendChart()
    renderPieChart()
  } catch (e: any) {
    loadError.value = '首页加载失败，请稍后重试'
    console.error('[Dashboard] load failed:', e)
  } finally {
    loading.value = false
  }
}

async function loadTrendOnly() {
  try {
    const params = buildQueryParams()
    trendPoints.value = await getHomeTrend({ ...params, platformType: chartPlatform.value, groupBy: trendGroupBy.value })
    await nextTick()
    renderTrendChart()
  } catch (e) {
    console.error('[Dashboard] trend load failed:', e)
  }
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshHome()
    await loadAllData()
  } finally {
    refreshing.value = false
  }
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(renderTrendChart, 100)
    return
  }
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
    window.addEventListener('resize', handleResize)
  }

  const points = trendPoints.value
  if (!points.length) {
    trendChart.clear()
    return
  }

  const dates = [...new Set(points.map(p => p.date))].sort()

  let series: Array<{ name: string; type: 'line'; smooth: boolean; data: number[] }>
  if (trendGroupBy.value === 'IP_GROUP') {
    const groupNames = [...new Set(points.map(p => p.ipGroupName).filter(Boolean))] as string[]
    series = groupNames.length
      ? groupNames.map(name => ({
          name,
          type: 'line' as const,
          smooth: true,
          data: dates.map(date => {
            const hit = points.find(p => p.date === date && p.ipGroupName === name)
            return hit?.count ?? 0
          }),
        }))
      : [{
          name: '内容数',
          type: 'line' as const,
          smooth: true,
          data: dates.map(date => points.filter(p => p.date === date).reduce((s, p) => s + p.count, 0)),
        }]
  } else {
    const platforms = chartPlatform.value
      ? [chartPlatform.value]
      : [...new Set(points.map(p => p.platform).filter(Boolean))] as string[]
    series = platforms.length
      ? platforms.map(platform => ({
          name: platformLabel(platform),
          type: 'line' as const,
          smooth: true,
          data: dates.map(date => {
            const hit = points.find(p => p.date === date && p.platform === platform)
            return hit?.count ?? 0
          }),
        }))
      : [{
          name: '内容数',
          type: 'line' as const,
          smooth: true,
          data: dates.map(date => points.filter(p => p.date === date).reduce((s, p) => s + p.count, 0)),
        }]
  }

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name), bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates.map(d => d.substring(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series,
  }, true)
}

function renderPieChart() {
  if (!pieChartRef.value) return
  if (pieChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(renderPieChart, 100)
    return
  }
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
    window.addEventListener('resize', handleResize)
  }

  const data = platformDist.value
  if (!data.length) {
    pieChart.clear()
    return
  }

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center' },
    series: [{
      name: '平台分布',
      type: 'pie',
      radius: ['40%', '70%'],
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      data: data.map(item => ({
        name: platformLabel(item.platform),
        value: item.count,
      })),
    }],
  }, true)
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

async function navigateTo(route: string) {
  if (route) await router.push(route)
}

function handleTodoClick(row: HomeTodoVO) {
  navigateTo(resolveOpsUrl(row.actionUrl))
}

watch(
  () => [filters.ipGroupId, filters.dateRange?.[0], filters.dateRange?.[1]],
  () => {
    loadAllData()
  },
)

onMounted(() => {
  loadAllData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
  trendChart = null
  pieChart = null
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 20px 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);

  .toolbar-card {
    margin-bottom: 16px;
    border: none;
    border-radius: 12px;

    :deep(.el-card__body) {
      padding: 12px 20px;
    }
  }

  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;

    .toolbar-form {
      margin-bottom: 0;

      :deep(.el-form-item) {
        margin-bottom: 0;
        margin-right: 16px;
      }
    }
  }

  .error-banner {
    margin-bottom: 16px;
  }

  .card-header {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    .chart-filter {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: normal;
      font-size: 14px;

      .filter-label {
        color: #909399;
      }
    }
  }

  .quick-access {
    margin-bottom: 16px;
    border: none;
    border-radius: 12px;

    .quick-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px 8px;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background: #f5f7fa;
        transform: translateY(-2px);

        .quick-icon {
          background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
          color: #fff;
        }
      }

      .quick-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        background: #e6f7ff;
        color: #1890ff;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 8px;
        transition: all 0.3s ease;
      }

      .quick-label {
        font-size: 13px;
        color: #606266;
        text-align: center;
      }
    }
  }

  .kpi-cards {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 16px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .kpi-card {
      border: none;
      border-radius: 12px;

      :deep(.el-card__body) {
        padding: 20px;
      }

      .kpi-content {
        display: flex;
        align-items: center;
        gap: 16px;

        .kpi-icon {
          width: 52px;
          height: 52px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .kpi-value {
          font-size: 24px;
          font-weight: 700;
          color: #303133;
          line-height: 1.2;
        }

        .kpi-label {
          font-size: 14px;
          color: #909399;
          margin-top: 4px;
        }
      }
    }
  }

  .chart-section {
    margin-bottom: 16px;

    .el-card {
      border: none;
      border-radius: 12px;
    }

    .chart-box {
      height: 300px;
      width: 100%;
    }
  }

  .todo-section {
    border: none;
    border-radius: 12px;

    :deep(.el-table__row) {
      cursor: pointer;
    }
  }
}
</style>
