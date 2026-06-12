<template>
  <div class="screen-preview-panel">
    <div class="preview-header">{{ title || '运营数据大屏' }}</div>
    <div v-if="kpiWidgets.length" class="preview-kpi-grid">
      <div v-for="w in kpiWidgets" :key="w.id" class="preview-kpi">
        <div class="pkpi-label">{{ w.title }}</div>
        <div class="pkpi-value">{{ displayValue(w) }}</div>
      </div>
    </div>
    <div v-if="statWidgets.length" class="preview-stat-grid">
      <div v-for="w in statWidgets" :key="w.id" class="preview-stat">
        <div class="pstat-value">{{ displayValue(w) }}</div>
        <div class="pstat-label">{{ w.title }}</div>
      </div>
    </div>
    <div v-for="(section, idx) in layoutSections" :key="idx" class="preview-section">
      <div v-if="section.kind === 'charts'" class="preview-row-2">
        <div v-for="w in section.charts" :key="w.id" class="preview-chart">
          <div class="pchart-title">{{ w.title }}</div>
          <div :ref="(el) => setChartRef(w.id, el as HTMLElement)" class="pchart-box" />
        </div>
      </div>
      <div v-else-if="section.kind === 'mixed'" class="preview-row-2">
        <div v-if="section.chart" class="preview-chart">
          <div class="pchart-title">{{ section.chart.title }}</div>
          <div :ref="(el) => setChartRef(section.chart!.id, el as HTMLElement)" class="pchart-box" />
        </div>
        <div v-if="section.list" class="preview-list">
          <div class="plist-title">{{ section.list.title }}</div>
          <div class="plist-rows">
            <div v-for="(row, ri) in listRows(section.list).slice(0, 3)" :key="ri" class="plist-row">
              <span class="plist-rank">{{ row.rank ?? ri + 1 }}</span>
              <span class="plist-text">{{ row.title ?? row.account_name ?? '-' }}</span>
              <span class="plist-num">{{ formatNum(row.read_count ?? row.play_count ?? row.follower_count) }}</span>
            </div>
            <div v-if="!listRows(section.list).length" class="plist-empty">暂无数据</div>
          </div>
        </div>
      </div>
      <div v-else-if="section.kind === 'list-full' && section.list" class="preview-list preview-list-full">
        <div class="plist-title">{{ section.list.title }}</div>
        <div class="plist-rows">
          <div v-for="(row, ri) in listRows(section.list).slice(0, 5)" :key="ri" class="plist-row">
            <span class="plist-rank">{{ row.rank ?? ri + 1 }}</span>
            <span class="plist-text">{{ row.title ?? row.account_name ?? '-' }}</span>
            <span class="plist-num">{{ formatNum(row.read_count ?? row.play_count ?? row.follower_count) }}</span>
          </div>
        </div>
      </div>
    </div>
    <div v-if="!widgetResults.length" class="preview-empty">添加组件后在此预览</div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import {
  buildLayoutSections,
  formatNumber,
  type DashboardWidgetResult,
  type DataScreenLayout,
} from '@/types/dataScreen'

const props = defineProps<{
  layout: DataScreenLayout
  widgetResults?: DashboardWidgetResult[]
  title?: string
}>()

const widgetResults = computed(() => props.widgetResults ?? [])
const kpiWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'KPI'))
const statWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'STAT'))
const chartWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'CHART'))
const listWidgets = computed(() => widgetResults.value.filter((w) => w.type === 'LIST'))
const layoutSections = computed(() => buildLayoutSections(chartWidgets.value, listWidgets.value))

const chartRefs = new Map<string, HTMLElement>()
const chartInstances = new Map<string, echarts.ECharts>()

const setChartRef = (id: string, el: HTMLElement | null) => {
  if (el) chartRefs.set(id, el)
}

const resultMap = computed(() => {
  const m = new Map<string, DashboardWidgetResult>()
  for (const w of widgetResults.value) m.set(w.id, w)
  return m
})

const displayValue = (w: DashboardWidgetResult) => {
  const val = w.payload?.value
  return val == null || val === '' ? '--' : formatNumber(val)
}

const formatNum = (val: unknown) => {
  if (typeof val === 'number') return formatNumber(val)
  return val ?? '-'
}

const listRows = (w: DashboardWidgetResult) => w.payload?.rows || []

const renderCharts = async () => {
  await nextTick()
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
      chart.setOption({
        backgroundColor: 'transparent',
        series: [{
          type: 'pie', radius: ['35%', '65%'], center: ['50%', '50%'],
          data: series?.data || [],
          label: { show: false },
        }],
      })
      continue
    }
    chart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 28, right: 8, top: 8, bottom: 20 },
      xAxis: {
        type: 'category', data: chartData.xAxis || [],
        axisLabel: { color: '#78909c', fontSize: 9 },
        axisLine: { lineStyle: { color: '#1e4976' } },
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#78909c', fontSize: 9 },
        splitLine: { lineStyle: { color: '#1e4976' } },
      },
      series: (chartData.series || []).map((s) => ({ ...s, smooth: s.type === 'line' })),
    })
  }
}

watch(() => props.widgetResults, () => renderCharts(), { deep: true })
watch(layoutSections, () => renderCharts())

onUnmounted(() => {
  chartInstances.forEach((c) => c.dispose())
})

// expose for parent to attach list payloads to section.list
defineExpose({ resultMap })
</script>

<style scoped>
.screen-preview-panel {
  background: #0a1929; border-radius: 6px; padding: 10px; min-height: 420px;
}
.preview-header {
  text-align: center; padding: 4px; margin-bottom: 8px; border-bottom: 1px solid #1e4976;
  font-size: 12px; font-weight: 700; color: #4fc3f7; letter-spacing: 2px;
}
.preview-kpi-grid {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; margin-bottom: 8px;
}
.preview-kpi {
  background: #1a3a5c; border: 1px solid #1e4976; border-radius: 4px; padding: 6px; text-align: center;
}
.pkpi-label { font-size: 9px; color: #90a4ae; }
.pkpi-value { font-size: 13px; font-weight: 700; color: #4fc3f7; }
.preview-stat-grid {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; margin-bottom: 8px;
}
.preview-stat {
  background: #0d2137; border: 1px solid #1e4976; border-radius: 4px; padding: 6px; text-align: center;
}
.pstat-value { font-size: 12px; font-weight: 700; color: #4fc3f7; }
.pstat-label { font-size: 8px; color: #78909c; margin-top: 2px; }
.preview-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-bottom: 8px; }
.preview-chart, .preview-list {
  background: #0d2137; border: 1px solid #1e4976; border-radius: 4px; padding: 6px; min-height: 90px;
}
.pchart-title, .plist-title { font-size: 10px; color: #b0bec5; margin-bottom: 4px; }
.pchart-box { height: 72px; }
.plist-rows { font-size: 9px; color: #90a4ae; line-height: 1.6; }
.plist-row { display: flex; align-items: center; gap: 4px; padding: 2px 0; }
.plist-rank { width: 14px; text-align: center; color: #4fc3f7; }
.plist-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.plist-num { color: #4fc3f7; text-align: right; min-width: 48px; }
.plist-empty, .preview-empty { text-align: center; color: #78909c; font-size: 12px; padding: 24px; }
.preview-list-full { margin-bottom: 8px; }
</style>
