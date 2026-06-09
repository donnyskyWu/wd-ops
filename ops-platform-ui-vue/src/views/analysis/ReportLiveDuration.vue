<!--
  M6-4 直播时长报表
  依据: FR-M6-004
  路径: /analysis/report/live-duration
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>直播时长</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="平台">
          <el-select v-model="filter.platform" multiple clearable style="width: 220px">
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="快手" value="KUAISHOU" />
            <el-option label="视频号" value="VIDEO_ACCOUNT" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card><div class="kpi-label">总场次</div><div class="kpi-value primary">{{ kpi.sessions }}</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="kpi-label">总时长(h)</div><div class="kpi-value success">{{ kpi.hours.toFixed(1) }}</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="kpi-label">平均在线</div><div class="kpi-value warning">{{ kpi.avgOnline.toLocaleString() }}</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <ContentWrap title="30 天直播时长趋势">
          <div ref="trendRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
      <el-col :span="8">
        <ContentWrap title="平台时长占比">
          <div ref="pieRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="直播明细" style="margin-top: 16px">
      <el-table :data="list" border stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="account" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="100" align="right">
          <template #default="{ row }">{{ (row.duration / 60).toFixed(1) }}h</template>
        </el-table-column>
        <el-table-column prop="peak" label="峰值在线" width="120" align="right" />
        <el-table-column prop="avg" label="均在线" width="120" align="right" />
        <el-table-column prop="gmv" label="GMV" width="120" align="right">
          <template #default="{ row }">¥ {{ row.gmv.toLocaleString() }}</template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'

const loading = ref(false)
const filter = reactive({ platform: [] as string[], dateRange: undefined as any })
const kpi = reactive({ sessions: 0, hours: 0, avgOnline: 0 })
const list = ref<any[]>([])
const trendRef = ref<HTMLDivElement | null>(null)
const pieRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const initTrend = () => {
  if (!trendRef.value) return
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) return setTimeout(initTrend, 100)
  if (!trendChart) trendChart = echarts.init(el)
  const days = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value', name: '小时' },
    series: [{ type: 'bar', data: days.map(() => 2 + Math.random() * 8), itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } }],
  })
}
const initPie = () => {
  if (!pieRef.value) return
  const el = pieRef.value
  if (el.getBoundingClientRect().width === 0) return setTimeout(initPie, 100)
  if (!pieChart) pieChart = echarts.init(el)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'], itemStyle: { borderRadius: 6 },
      data: [
        { name: '抖音', value: 128 },
        { name: '快手', value: 76 },
        { name: '视频号', value: 42 },
      ],
    }],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 400))
  kpi.sessions = 86
  kpi.hours = 246.5
  kpi.avgOnline = 1280
  list.value = Array.from({ length: 8 }, (_, i) => ({
    date: `2026-06-${String(1 + i).padStart(2, '0')}`,
    account: ['知识变现研究院', 'AI 技术前沿', '种草官', '健身达人'][i % 4] + (i + 1),
    platform: ['抖音', '快手', '视频号'][i % 3],
    duration: 240 + Math.floor(Math.random() * 360),
    peak: 1200 + Math.floor(Math.random() * 2000),
    avg: 800 + Math.floor(Math.random() * 1000),
    gmv: 5000 + Math.floor(Math.random() * 30000),
  }))
  loading.value = false
  await nextTick()
  setTimeout(() => { initTrend(); initPie() }, 100)
}

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.4; text-align: center; padding: 8px 0; }
.kpi-value.primary { color: #409eff; }
.kpi-value.success { color: #67c23a; }
.kpi-value.warning { color: #e6a23c; }
</style>
