<!--
  M6-6 ROI 分析
  依据: FR-M6-006
  路径: /analysis/report/roi
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>ROI 分析</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="filter.platform" multiple clearable style="width: 220px">
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="公众号" value="WECHAT_MP" />
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="快手" value="KUAISHOU" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="goTrend">趋势详细分析</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card><div class="kpi-label">总 GMV</div><div class="kpi-value primary">¥ {{ kpi.gmv.toLocaleString() }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="kpi-label">总成本</div><div class="kpi-value warning">¥ {{ kpi.cost.toLocaleString() }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="kpi-label">整体 ROI</div><div class="kpi-value success">{{ kpi.roi.toFixed(2) }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="kpi-label">订单数</div><div class="kpi-value">{{ kpi.orders.toLocaleString() }}</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <ContentWrap title="ROI 趋势">
          <div ref="trendRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
      <el-col :span="8">
        <ContentWrap title="平台 ROI">
          <div ref="pieRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'

const router = useRouter()
const loading = ref(false)
const filter = reactive({ dateRange: undefined as any, platform: [] as string[] })
const kpi = reactive({ gmv: 0, cost: 0, roi: 0, orders: 0 })
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
  const gmv = days.map(() => 8000 + Math.floor(Math.random() * 10000))
  const cost = days.map(() => 2000 + Math.floor(Math.random() * 3000))
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['GMV', '成本', 'ROI'], top: 0 },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: days },
    yAxis: [
      { type: 'value', name: '金额(¥)', position: 'left' },
      { type: 'value', name: 'ROI', position: 'right', max: 6 },
    ],
    series: [
      { name: 'GMV', type: 'bar', data: gmv, itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } },
      { name: '成本', type: 'bar', data: cost, itemStyle: { color: '#e6a23c', borderRadius: [4, 4, 0, 0] } },
      { name: 'ROI', type: 'line', yAxisIndex: 1, data: gmv.map((g, i) => g / cost[i]), smooth: true, itemStyle: { color: '#67c23a' }, lineStyle: { width: 3 } },
    ],
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
        { name: '抖音', value: 4.2 },
        { name: '公众号', value: 3.5 },
        { name: '小红书', value: 3.8 },
        { name: '快手', value: 2.9 },
      ],
    }],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 400))
  kpi.gmv = 291000
  kpi.cost = 88000
  kpi.roi = 3.31
  kpi.orders = 1486
  loading.value = false
  await nextTick()
  setTimeout(() => { initTrend(); initPie() }, 100)
}

const goTrend = () => router.push('/finance/roi/trend')

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.4; text-align: center; padding: 8px 0; }
.kpi-value.primary { color: #409eff; }
.kpi-value.warning { color: #e6a23c; }
.kpi-value.success { color: #67c23a; }
</style>
