<template>
  <div class="data-screen-fullscreen">
    <div v-if="loading" class="loading-overlay">
      <div class="spinner" />
    </div>

    <div class="top-bar">
      <div class="top-left">
        <span class="title"><el-icon><Monitor /></el-icon> {{ dashboardTitle }}</span>
        <select v-model="selectedDashboardId" class="template-select screen-filter-select" @change="onTemplateChange">
          <option v-for="d in dashboards" :key="d.id" :value="d.id">{{ d.dashboardName }}</option>
        </select>
      </div>
      <div class="top-right">
        <div class="scope-switch">
          <button
            class="scope-btn"
            :class="{ active: currentScope === 'INTERNAL' }"
            @click="switchScope('INTERNAL')"
          >内部数据</button>
          <button
            class="scope-btn"
            :class="{ active: currentScope === 'EXTERNAL' }"
            @click="switchScope('EXTERNAL')"
          >外部数据</button>
        </div>
        <select v-model="dateRangeKey" class="date-select screen-filter-select" @change="onGlobalFilterChange">
          <option value="1d">今日</option>
          <option value="7d">近7天</option>
          <option value="30d">近30天</option>
        </select>
        <select
          v-model="ipGroupIdStr"
          class="filter-ip-group screen-filter-select"
          @change="onGlobalFilterChange"
        >
          <option value="">全部IP组</option>
          <option v-for="opt in ipGroupOptions" :key="opt.id" :value="String(opt.id)">{{ opt.label }}</option>
        </select>
        <select
          v-model="platformTypeStr"
          class="filter-platform screen-filter-select"
          @change="onGlobalFilterChange"
        >
          <option value="">全部平台</option>
          <option v-for="opt in platformOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <select v-model="refreshSeconds" class="refresh-select screen-filter-select" @change="setupAutoRefresh">
          <option :value="30">30秒刷新</option>
          <option :value="60">1分钟刷新</option>
          <option :value="300">5分钟刷新</option>
          <option :value="0">不刷新</option>
        </select>
        <button class="fullscreen-btn" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon> 全屏
        </button>
        <router-link to="/dashboard" class="exit-link">
          <el-icon><Close /></el-icon> 退出
        </router-link>
      </div>
    </div>

    <div v-if="errorMsg" class="error-msg">
      <p>{{ errorMsg }}</p>
      <button class="retry-btn" @click="loadData">重试</button>
    </div>

    <div v-else class="dashboard-content">
      <div v-if="kpiWidgets.length" class="kpi-grid" :style="kpiGridStyle">
        <div v-for="w in kpiWidgets" :key="w.id" class="kpi-card">
          <div class="kpi-label">{{ w.title }}</div>
          <div class="kpi-value">{{ formatWidgetValue(w) }}</div>
          <div v-if="kpiTrendText(w)" class="kpi-trend" :class="kpiTrendClass(w)">{{ kpiTrendText(w) }}</div>
        </div>
      </div>

      <div v-if="statWidgets.length" class="stats-section">
        <div v-if="currentScope === 'INTERNAL'" class="stats-section-title">今日工作概览</div>
        <div class="today-stats" :style="statGridStyle">
          <div v-for="w in statWidgets" :key="w.id" class="today-stat-item">
            <div class="stat-value">{{ formatWidgetValue(w) }}</div>
            <div class="stat-label">{{ w.title }}</div>
          </div>
        </div>
      </div>

      <div v-for="(section, sIdx) in layoutSections" :key="sIdx" class="layout-section">
        <div v-if="section.kind === 'charts'" class="charts-grid">
          <div v-for="w in section.charts" :key="w.id" class="chart-card">
            <div class="chart-title">{{ w.title }}</div>
            <div :ref="(el) => setChartRef(w.id, el as HTMLElement)" class="chart-box" />
          </div>
        </div>
        <div v-else-if="section.kind === 'mixed'" class="charts-grid">
          <div class="chart-card">
            <div class="chart-title">{{ section.chart.title }}</div>
            <div :ref="(el) => setChartRef(section.chart.id, el as HTMLElement)" class="chart-box" />
          </div>
          <div class="chart-card list-card">
            <div class="chart-title">{{ section.list.title }}</div>
            <ScreenListTable :widget="section.list" />
          </div>
        </div>
        <div v-else-if="section.kind === 'list-full'" class="list-full-row">
          <div class="chart-card list-card list-card-full">
            <div class="chart-title">{{ section.list.title }}</div>
            <ScreenListTable :widget="section.list" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteUpdate } from 'vue-router'
import { Close, FullScreen, Monitor } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import ScreenListTable from '@/components/screen/ScreenListTable.vue'
import { getDashboardConfigList, getDashboardData } from '@/api/dashboard'
import { getIpGroupTree } from '@/api/ip-group'
import { fetchDictData, type DictItemVO } from '@/api/dict'
import type { IpGroupTreeVO } from '@/types/ip-group'
import {
  buildDashboardDataQuery,
  buildLayoutSections,
  formatNumber,
  mergeWidgetResultsWithLayout,
  parseLayout,
  type DashboardDataResponse,
  type DashboardDateRangeKey,
  type DashboardVO,
  type DashboardWidgetResult,
  type DataScreenScope,
} from '@/types/dataScreen'

const DEFAULT_INTERNAL_ID = 98601
const DEFAULT_EXTERNAL_ID = 98602

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')
const dashboards = ref<DashboardVO[]>([])
const selectedDashboardId = ref<number>(DEFAULT_INTERNAL_ID)
const currentScope = ref<DataScreenScope>('INTERNAL')
const dateRangeKey = ref<DashboardDateRangeKey>('7d')
const ipGroupIdStr = ref('')
const platformTypeStr = ref('')
const ipGroupOptions = ref<{ id: number; label: string }[]>([])
const platformOptions = ref<DictItemVO[]>([])
const refreshSeconds = ref(60)
const dashboardTitle = ref('运营数据大屏')
const widgetResults = ref<DashboardWidgetResult[]>([])
const chartRefs = new Map<string, HTMLElement>()
const chartInstances = new Map<string, echarts.ECharts>()
let refreshTimer: ReturnType<typeof setInterval> | null = null

const kpiWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'KPI'))
const statWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'STAT'))
const chartWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'CHART'))
const listWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'LIST'))
const layoutSections = computed(() => buildLayoutSections(chartWidgets.value, listWidgets.value))

const CHART_COLORS = ['#4fc3f7', '#ffa726', '#66bb6a', '#ab47bc', '#ef5350']

const kpiGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.min(Math.max(kpiWidgets.value.length, 1), 6)}, 1fr)`,
}))

const statGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.min(Math.max(statWidgets.value.length, 1), 5)}, 1fr)`,
}))

const setChartRef = (id: string, el: HTMLElement | null) => {
  if (el) chartRefs.set(id, el)
}

const formatWidgetValue = (w: DashboardWidgetResult) => {
  const val = w.payload?.value
  const unit = w.payload?.unit
  const formatted = formatNumber(val)
  return unit ? `${formatted}${unit}` : formatted
}

const kpiTrendText = (w: DashboardWidgetResult) => {
  const payload = w.payload as Record<string, unknown> | undefined
  const trend = payload?.trend ?? payload?.trendText
  if (trend != null && trend !== '') return String(trend)
  const pct = payload?.trendPct ?? payload?.trend_pct
  if (pct == null || pct === '') return ''
  const n = Number(pct)
  if (Number.isNaN(n)) return String(pct)
  const sign = n > 0 ? '+' : ''
  return `${sign}${n.toFixed(1)}%`
}

const kpiTrendClass = (w: DashboardWidgetResult) => {
  const payload = w.payload as Record<string, unknown> | undefined
  const trendDir = payload?.trendDirection ?? payload?.trend_direction
  if (trendDir === 'up' || trendDir === 'down') return trendDir
  const pct = Number(payload?.trendPct ?? payload?.trend_pct)
  if (!Number.isNaN(pct) && pct !== 0) return pct > 0 ? 'up' : 'down'
  const text = kpiTrendText(w)
  if (text.startsWith('+')) return 'up'
  if (text.startsWith('-')) return 'down'
  return ''
}

const flattenIpGroupTree = (nodes: IpGroupTreeVO[], depth = 0): { id: number; label: string }[] => {
  const out: { id: number; label: string }[] = []
  for (const node of nodes) {
    const prefix = depth > 0 ? `${'　'.repeat(depth)}` : ''
    out.push({ id: node.id, label: `${prefix}${node.groupName}` })
    if (node.children?.length) out.push(...flattenIpGroupTree(node.children, depth + 1))
  }
  return out
}

const loadFilterOptions = async () => {
  try {
    const tree = await getIpGroupTree()
    ipGroupOptions.value = flattenIpGroupTree(tree ?? [])
  } catch {
    ipGroupOptions.value = []
  }
  try {
    const res: any = await fetchDictData('dict_platform_type')
    platformOptions.value = (res?.list ?? res?.data?.list ?? []) as DictItemVO[]
  } catch {
    platformOptions.value = []
  }
}

const loadDashboardList = async () => {
  const res: any = await getDashboardConfigList({ pageNum: 1, pageSize: 50 })
  const data = res?.data ?? res
  dashboards.value = data?.list ?? data?.records ?? []
  if (!dashboards.value.length) {
    dashboards.value = [
      { id: DEFAULT_INTERNAL_ID, dashboardName: '内部运营大屏', dashboardType: 'OPS', layout: '', status: 1 },
      { id: DEFAULT_EXTERNAL_ID, dashboardName: '外部竞品大屏', dashboardType: 'OPS', layout: '', status: 1 },
    ]
  }
}

const loadData = async () => {
  if (!selectedDashboardId.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const res: any = await getDashboardData(
      selectedDashboardId.value,
      buildDashboardDataQuery({
        dateRangeKey: dateRangeKey.value,
        ipGroupId: ipGroupIdStr.value ? Number(ipGroupIdStr.value) : undefined,
        platformType: platformTypeStr.value || undefined,
      }),
    )
    const data = (res?.data ?? res) as DashboardDataResponse
    const layout = parseLayout(data?.dashboard?.layout)
    widgetResults.value = mergeWidgetResultsWithLayout(layout, data?.widgets ?? [])
    dashboardTitle.value = data?.dashboard?.dashboardName?.trim() || '运营数据大屏'
    currentScope.value = layout.scope
    if (layout.refreshSeconds != null) refreshSeconds.value = layout.refreshSeconds
    syncDashboardMeta(data?.dashboard)
    await nextTick()
    renderCharts()
  } catch (e: any) {
    errorMsg.value = e?.message || '加载大屏数据失败'
    widgetResults.value = []
  } finally {
    loading.value = false
  }
}

const syncDashboardMeta = (dashboard?: DashboardVO) => {
  if (!dashboard?.id) return
  const idx = dashboards.value.findIndex((d) => d.id === dashboard.id)
  if (idx >= 0) {
    dashboards.value[idx] = { ...dashboards.value[idx], ...dashboard }
  } else {
    dashboards.value.push(dashboard)
  }
}

const darkChartTheme = {
  backgroundColor: 'transparent',
  textStyle: { color: '#90a4ae' },
}

const renderCharts = () => {
  chartInstances.forEach((c) => c.dispose())
  chartInstances.clear()

  for (const w of chartWidgets.value) {
    const el = chartRefs.get(w.id)
    if (!el) continue
    const chart = echarts.init(el)
    chartInstances.set(w.id, chart)
    const payload = w.payload || {}
    const chartData = payload.chart || {}
    const chartType = payload.chartType || 'line'

    if (chartType === 'pie') {
      const series = chartData.series?.[0]
      const pieData = (series?.data || []).map((item: unknown, i: number) => {
        if (item && typeof item === 'object' && !Array.isArray(item)) {
          const row = item as Record<string, unknown>
          return {
            ...row,
            itemStyle: { color: CHART_COLORS[i % CHART_COLORS.length] },
          }
        }
        return item
      })
      chart.setOption({
        ...darkChartTheme,
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#90a4ae', fontSize: 11 } },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['35%', '50%'],
          data: pieData,
          label: { color: '#b0bec5', fontSize: 12 },
        }],
      })
      continue
    }

    chart.setOption({
      ...darkChartTheme,
      tooltip: { trigger: 'axis' },
      legend: {
        data: chartData.legend || [],
        textStyle: { color: '#90a4ae', fontSize: 11 },
        top: 0,
      },
      grid: { left: 50, right: 20, top: 30, bottom: 30 },
      xAxis: {
        type: 'category',
        data: chartData.xAxis || [],
        axisLine: { lineStyle: { color: '#1e4976' } },
        axisLabel: { color: '#78909c', fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#1e4976' } },
        axisLabel: { color: '#78909c', fontSize: 11 },
        splitLine: { lineStyle: { color: '#1e4976' } },
      },
      series: (chartData.series || []).map((s, i) => {
        const color = CHART_COLORS[i % CHART_COLORS.length]
        return {
          ...s,
          smooth: s.type === 'line',
          color,
          itemStyle: { color },
        }
      }),
    })
  }
}

const switchScope = (scope: DataScreenScope) => {
  currentScope.value = scope
  const target = dashboards.value.find((d) => {
    const layout = parseLayout(d.layout)
    return layout.scope === scope
  })
  if (target && target.id !== selectedDashboardId.value) {
    selectedDashboardId.value = target.id
    router.replace({ path: `/screen/${target.id}` })
    loadData()
  }
}

const onTemplateChange = () => {
  router.replace({ path: `/screen/${selectedDashboardId.value}` })
  const d = dashboards.value.find((x) => x.id === selectedDashboardId.value)
  if (d) currentScope.value = parseLayout(d.layout).scope
  loadData()
}

const onGlobalFilterChange = () => loadData()

const setupAutoRefresh = () => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (refreshSeconds.value > 0) {
    refreshTimer = setInterval(loadData, refreshSeconds.value * 1000)
  }
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

const onResize = () => {
  chartInstances.forEach((c) => c.resize())
}

watch(() => route.params.id, (id) => {
  if (id) {
    selectedDashboardId.value = Number(id)
    loadData()
  }
})

watch(() => route.query._t, () => {
  if (route.params.id) loadData()
})

onBeforeRouteUpdate((to) => {
  if (to.params.id) {
    selectedDashboardId.value = Number(to.params.id)
    loadData()
  }
})

onMounted(async () => {
  await Promise.all([loadDashboardList(), loadFilterOptions()])
  const routeId = route.params.id ? Number(route.params.id) : null
  if (routeId) {
    selectedDashboardId.value = routeId
  } else {
    selectedDashboardId.value = DEFAULT_INTERNAL_ID
    router.replace({ path: `/screen/${DEFAULT_INTERNAL_ID}` })
  }
  await loadData()
  setupAutoRefresh()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', onResize)
  chartInstances.forEach((c) => c.dispose())
})
</script>

<style scoped>
.data-screen-fullscreen {
  min-height: 100vh;
  background: #0a1929;
  color: #e0e8f0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.loading-overlay {
  position: fixed; inset: 0; background: rgba(10, 25, 41, 0.9);
  display: flex; align-items: center; justify-content: center; z-index: 100;
}
.spinner {
  width: 40px; height: 40px; border: 3px solid #1e4976;
  border-top-color: #4fc3f7; border-radius: 50%; animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.top-bar {
  background: linear-gradient(90deg, #0d2137 0%, #1a3a5c 50%, #0d2137 100%);
  border-bottom: 1px solid #1e4976; padding: 8px 24px;
  display: flex; align-items: center; justify-content: space-between;
  flex-wrap: nowrap; gap: 12px;
}
.top-left {
  display: flex; align-items: center; gap: 16px;
  flex-shrink: 0; min-width: 0;
}
.top-right {
  display: flex; align-items: center; gap: 12px;
  flex-shrink: 0; flex-wrap: nowrap;
}
.title {
  font-size: 18px; font-weight: 700; color: #4fc3f7; letter-spacing: 2px;
  display: flex; align-items: center; gap: 8px;
  flex-shrink: 0; white-space: nowrap;
}
.screen-filter-select,
.template-select,
.date-select,
.refresh-select {
  background: #0d2137;
  border: 1px solid #1e4976;
  color: #e0e8f0;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.4;
  height: 28px;
  box-sizing: border-box;
  outline: none;
  cursor: pointer;
  flex-shrink: 0;
  color-scheme: dark;
  appearance: auto;
}
.screen-filter-select:focus,
.template-select:focus,
.date-select:focus,
.refresh-select:focus {
  border-color: #4fc3f7;
}
.screen-filter-select option {
  background: #0d2137;
  color: #e0e8f0;
}
.template-select { min-width: 148px; }
.date-select { min-width: 88px; }
.filter-ip-group { min-width: 140px; max-width: 180px; }
.filter-platform { min-width: 120px; max-width: 140px; }
.refresh-select { min-width: 108px; }
.scope-switch {
  display: flex; border-radius: 4px; overflow: hidden;
  border: 1px solid #1e4976; flex-shrink: 0;
}
.scope-btn {
  padding: 6px 16px; font-size: 13px; cursor: pointer; background: transparent;
  color: #90a4ae; border: none; transition: all 0.3s;
}
.scope-btn.active { background: #4fc3f7; color: #0a1929; font-weight: 600; }
.fullscreen-btn {
  background: transparent; border: 1px solid #1e4976; color: #90a4ae;
  padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 13px;
  display: inline-flex; align-items: center; gap: 4px;
  flex-shrink: 0; white-space: nowrap; height: 28px; box-sizing: border-box;
}
.fullscreen-btn:hover { border-color: #4fc3f7; color: #4fc3f7; }
.exit-link {
  color: #90a4ae; text-decoration: none; font-size: 13px;
  display: inline-flex; align-items: center; gap: 4px;
  flex-shrink: 0; white-space: nowrap;
}
.dashboard-content { padding: 16px 24px; }
.kpi-grid { display: grid; gap: 12px; margin-bottom: 16px; }
.kpi-card {
  background: linear-gradient(135deg, #1a3a5c 0%, #0d2137 100%);
  border: 1px solid #1e4976; border-radius: 8px; padding: 16px; text-align: center;
}
.kpi-value { font-size: 28px; font-weight: 700; color: #4fc3f7; margin: 8px 0 4px; }
.kpi-label { font-size: 13px; color: #90a4ae; }
.kpi-trend { font-size: 12px; }
.kpi-trend.up { color: #66bb6a; }
.kpi-trend.down { color: #ef5350; }
.stats-section { margin-bottom: 16px; }
.stats-section-title {
  font-size: 14px; font-weight: 600; color: #b0bec5; margin-bottom: 8px;
  border-left: 3px solid #4fc3f7; padding-left: 8px;
}
.today-stats { display: grid; gap: 12px; }
.today-stat-item {
  background: #0d2137; border: 1px solid #1e4976; border-radius: 6px; padding: 12px; text-align: center;
}
.stat-value { font-size: 22px; font-weight: 700; color: #4fc3f7; }
.stat-label { font-size: 12px; color: #78909c; margin-top: 4px; }
.layout-section { margin-bottom: 16px; }
.charts-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
}
.list-full-row { display: block; }
.chart-card { background: #0d2137; border: 1px solid #1e4976; border-radius: 8px; padding: 16px; min-width: 0; }
.list-card-full { width: 100%; }
.chart-title {
  font-size: 14px; font-weight: 600; color: #b0bec5; margin-bottom: 12px;
  border-left: 3px solid #4fc3f7; padding-left: 8px;
}
.chart-box { height: 280px; }
.list-card { grid-column: span 1; }
.error-msg { text-align: center; padding: 40px; color: #ef5350; }
.retry-btn {
  background: #4fc3f7; color: #0a1929; border: none; padding: 8px 24px;
  border-radius: 4px; cursor: pointer; font-size: 14px; margin-top: 12px;
}
.exit-link:hover { color: #4fc3f7; }
</style>
