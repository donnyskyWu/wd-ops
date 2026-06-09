<template>
  <div class="wechat-data-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="日期范围"><el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="~" /></el-form-item>
    </TableSearch>
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="文章阅读数" :value="stats.readCount" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="分享数" :value="stats.shareCount" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="新增关注" :value="stats.newFollowers" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="净增粉丝" :value="stats.netFollowers" /></el-card></el-col>
    </el-row>
    <el-card class="chart-card" shadow="never"><div ref="trendRef" style="height: 350px"></div></el-card>
    <el-table :data="wechatList" v-loading="loading" stripe style="margin-top: 16px">
      <template #empty>
        <el-empty description="暂无微信数据" />
      </template>
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="title" label="文章标题" min-width="200" />
      <el-table-column prop="readCount" label="阅读数" width="100" />
      <el-table-column prop="shareCount" label="分享数" width="100" />
      <el-table-column prop="newFollowers" label="新增关注" width="100" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import TableSearch from '@/components/TableSearch.vue'
import * as echarts from 'echarts'

// ==================== 类型定义 ====================
interface WechatDataItem {
  date: string
  title: string
  readCount: number
  shareCount: number
  newFollowers: number
}

// ==================== 响应式数据 ====================
const loading = ref(false)
const searchForm = reactive({ dateRange: [] as string[] })
const stats = reactive({ readCount: 125000, shareCount: 3200, newFollowers: 1580, netFollowers: 1250 })
const wechatList = ref<WechatDataItem[]>([
  { date: '2026-05-27', title: '5月运营总结', readCount: 8500, shareCount: 320, newFollowers: 150 },
  { date: '2026-05-26', title: '产品发布通知', readCount: 6200, shareCount: 180, newFollowers: 95 },
])
const trendRef = ref<HTMLElement>()

const handleSearch = () => {}
const handleReset = () => { searchForm.dateRange = [] }

let wechatChart: echarts.ECharts | null = null
const loadChart = async () => {
  await nextTick()
  if (!trendRef.value || trendRef.value.getBoundingClientRect().width === 0) {
    setTimeout(loadChart, 100)
    return
  }
  if (wechatChart) {
    wechatChart.dispose()
    wechatChart = null
  }
  const chart = echarts.init(trendRef.value)
  wechatChart = chart
  chart.setOption({
    title: { text: '微信数据趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['阅读数', '分享数', '新增关注'] },
    xAxis: { type: 'category', data: ['5/21', '5/22', '5/23', '5/24', '5/25', '5/26', '5/27'] },
    yAxis: { type: 'value' },
    series: [
      { name: '阅读数', type: 'line', data: [5000, 6000, 7000, 6500, 7500, 6200, 8500], smooth: true },
      { name: '分享数', type: 'bar', data: [200, 250, 300, 280, 350, 180, 320] },
      { name: '新增关注', type: 'line', data: [100, 120, 150, 130, 180, 95, 150], smooth: true },
    ]
  })
}

onMounted(() => loadChart())
</script>

<style scoped>
.wechat-data-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.chart-card { margin-bottom: 16px; }
</style>
