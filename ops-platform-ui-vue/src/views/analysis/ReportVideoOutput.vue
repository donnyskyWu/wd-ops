<!--
  M6-3 短视频产出
  依据: FR-M6-003
  路径: /analysis/report/video-output
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>短视频产出</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="平台">
          <el-select v-model="filter.platform" multiple clearable style="width: 240px">
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="快手" value="KUAISHOU" />
            <el-option label="小红书" value="XIAOHONGSHU" />
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
      <el-col :span="24">
        <ContentWrap title="日产出趋势">
          <div ref="trendRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="账号产出排行" style="margin-top: 16px">
      <el-table :data="rank" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="account" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="count" label="产出数" width="100" align="right" />
        <el-table-column prop="avgView" label="均播放" width="120" align="right">
          <template #default="{ row }">{{ (row.avgView / 1000).toFixed(1) }}k</template>
        </el-table-column>
        <el-table-column prop="top" label="爆款数" width="100" align="right">
          <template #default="{ row }">
            <el-tag :type="row.top > 3 ? 'success' : 'info'" size="small">{{ row.top }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="engage" label="互动率" width="100" align="center">
          <template #default="{ row }">{{ (row.engage * 100).toFixed(1) }}%</template>
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
const trendRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
const rank = ref<any[]>([])

const initTrend = () => {
  if (!trendRef.value) return
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) return setTimeout(initTrend, 100)
  if (!trendChart) trendChart = echarts.init(el)
  const days = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['抖音', '快手', '小红书', '视频号'], top: 0 },
    grid: { left: 40, right: 16, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value' },
    series: [
      { name: '抖音', type: 'line', stack: 'cnt', data: days.map(() => 20 + Math.floor(Math.random() * 15)), smooth: true, itemStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64,158,255,0.3)' } },
      { name: '快手', type: 'line', stack: 'cnt', data: days.map(() => 10 + Math.floor(Math.random() * 8)), smooth: true, itemStyle: { color: '#e6a23c' } },
      { name: '小红书', type: 'line', stack: 'cnt', data: days.map(() => 15 + Math.floor(Math.random() * 10)), smooth: true, itemStyle: { color: '#f56c6c' } },
      { name: '视频号', type: 'line', stack: 'cnt', data: days.map(() => 5 + Math.floor(Math.random() * 5)), smooth: true, itemStyle: { color: '#67c23a' } },
    ],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 400))
  rank.value = Array.from({ length: 10 }, (_, i) => ({
    account: ['知识变现研究院', 'AI 技术前沿', '种草官', '健身达人', '美妆发现号', '财经早班车', '情感夜话', '旅行背包客', '读书时光', '美食家'][i],
    platform: ['抖音', '小红书', '抖音', '快手', '小红书', '视频号', '抖音', '快手', '视频号', '小红书'][i],
    count: 60 - i * 4,
    avgView: 50000 - i * 3000,
    top: Math.max(0, 5 - Math.floor(i / 2)),
    engage: 0.08 - i * 0.005,
  }))
  loading.value = false
  await nextTick()
  setTimeout(initTrend, 100)
}

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
</style>
