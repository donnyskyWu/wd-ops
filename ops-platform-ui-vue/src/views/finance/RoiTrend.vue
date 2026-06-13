<!--
  M5 - ROI 趋势独立页
  依据: FR-M5-002 ROI 趋势
  路径: /finance/roi/trend
  4 区: 筛选 / KPI / 多维趋势图 / 排行
-->
<template>
  <div class="roi-trend-page" v-loading="loading">

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="时间范围">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="filter.platform" multiple clearable style="width: 220px">
            <el-option label="公众号" value="wechat" />
            <el-option label="抖音" value="douyin" />
            <el-option label="小红书" value="xhs" />
            <el-option label="快手" value="kuaishou" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" />
        </el-form-item>
        <el-form-item label="粒度">
          <el-radio-group v-model="filter.granularity">
            <el-radio-button value="day">日</el-radio-button>
            <el-radio-button value="week">周</el-radio-button>
            <el-radio-button value="month">月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- KPI -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card>
          <el-statistic title="累计 GMV" :value="kpi.gmv" :precision="0" prefix="¥" />
          <div class="trend up">↑ 12.5%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="累计成本" :value="kpi.cost" :precision="0" prefix="¥" />
          <div class="trend up">↑ 5.2%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="平均 ROI" :value="kpi.roi" :precision="2" />
          <div class="trend up">↑ 0.18</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="最佳 ROI 日" :value="kpi.bestDay" :precision="2" />
          <div class="trend">最近 30 天</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <ContentWrap title="ROI 多维趋势">
          <div ref="trendRef" style="width: 100%; height: 360px" />
        </ContentWrap>
      </el-col>
      <el-col :span="8">
        <ContentWrap title="平台 ROI 占比">
          <div ref="pieRef" style="width: 100%; height: 360px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="账号 ROI 排行" style="margin-top: 16px">
      <el-table :data="rank" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="account" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.platform }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipGroup" label="IP 组" width="120" />
        <el-table-column prop="gmv" label="GMV" width="120" align="right">
          <template #default="{ row }">¥ {{ row.gmv.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="120" align="right">
          <template #default="{ row }">¥ {{ row.cost.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="ROI" width="120" align="right">
          <template #default="{ row }">
            <span :class="{ 'top': row.roi > 4 }">{{ row.roi.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="趋势" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.trend > 0 ? 'up' : 'down'">
              {{ row.trend > 0 ? '↑' : '↓' }} {{ Math.abs(row.trend * 100).toFixed(0) }}%
            </span>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'

const loading = ref(false)
const filter = reactive({
  dateRange: undefined as any,
  platform: [] as string[],
  ipGroupId: undefined as number | undefined,
  granularity: 'day',
})
const kpi = reactive({ gmv: 0, cost: 0, roi: 0, bestDay: 0 })
const trendRef = ref<HTMLDivElement | null>(null)
const pieRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
const rank = ref<any[]>([])

const initTrend = () => {
  if (!trendRef.value) return
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initTrend, 100)
    return
  }
  if (!trendChart) trendChart = echarts.init(el)
  const days = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const gmv = days.map(() => 8000 + Math.floor(Math.random() * 10000))
  const cost = days.map(() => 2000 + Math.floor(Math.random() * 3000))
  const roi = gmv.map((g, i) => g / cost[i])
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
      { name: 'GMV', type: 'bar', stack: 'gmv', data: gmv, itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } },
      { name: '成本', type: 'bar', data: cost, itemStyle: { color: '#e6a23c', borderRadius: [4, 4, 0, 0] } },
      { name: 'ROI', type: 'line', yAxisIndex: 1, data: roi, smooth: true, itemStyle: { color: '#67c23a' }, lineStyle: { width: 3 } },
    ],
  })
}
const initPie = () => {
  if (!pieRef.value) return
  const el = pieRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initPie, 100)
    return
  }
  if (!pieChart) pieChart = echarts.init(el)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\nROI: {c}' },
        data: [
          { name: '抖音', value: 4.2 },
          { name: '公众号', value: 3.5 },
          { name: '小红书', value: 3.8 },
          { name: '快手', value: 2.9 },
        ],
      },
    ],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 400))
  kpi.gmv = 291000
  kpi.cost = 88000
  kpi.roi = 291000 / 88000
  kpi.bestDay = 4.85
  rank.value = Array.from({ length: 10 }, (_, i) => ({
    account: ['知识变现研究院', 'AI 技术前沿', '种草官', '健身达人', '美妆发现号', '财经早班车', '情感夜话', '旅行背包客', '读书时光', '美食家'][i],
    platform: ['抖音', '公众号', '小红书', '快手', '抖音', '公众号', '小红书', '快手', '抖音', '小红书'][i],
    ipGroup: ['头部 IP', '腰部 IP', '尾部 IP'][i % 3],
    gmv: 80000 - i * 5000 + Math.floor(Math.random() * 3000),
    cost: 20000 - i * 1000 + Math.floor(Math.random() * 1000),
    roi: 4.5 - i * 0.25,
    trend: (Math.random() - 0.4) * 0.3,
  }))
  loading.value = false
  await nextTick()
  setTimeout(() => {
    initTrend()
    initPie()
  }, 100)
}

const reset = () => {
  filter.dateRange = undefined
  filter.platform = []
  filter.ipGroupId = undefined
  filter.granularity = 'day'
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.roi-trend-page { padding: 20px; }
.trend { font-size: 12px; margin-top: 4px; }
.trend.up { color: #67c23a; }
.trend.down { color: #f56c6c; }
.top { color: #f56c6c; font-weight: 600; }
</style>
