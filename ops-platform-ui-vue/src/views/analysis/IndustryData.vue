<template>
  <div class="industry-data-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="行业"><el-select v-model="searchForm.industry" placeholder="全部" clearable><el-option label="科技" value="tech" /><el-option label="生活" value="life" /><el-option label="美食" value="food" /></el-select></el-form-item>
    </TableSearch>
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="行业基准ROI" :value="stats.benchRoi" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="平均互动率" :value="stats.avgEngagement" suffix="%" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="内容增速" :value="stats.contentGrowth" suffix="%" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="对标账号数" :value="stats.benchAccounts" /></el-card></el-col>
    </el-row>
    <el-card class="chart-card" shadow="never"><div ref="chartRef" style="height: 350px"></div></el-card>
    <el-table :data="industryList" v-loading="loading" stripe style="margin-top: 16px">
      <template #empty>
        <el-empty description="暂无行业数据" />
      </template>
      <el-table-column prop="industry" label="行业" width="120" />
      <el-table-column prop="avgFollowers" label="平均粉丝" width="120" />
      <el-table-column prop="avgViews" label="平均播放" width="120" />
      <el-table-column prop="avgEngagement" label="互动率" width="100" />
      <el-table-column prop="roi" label="ROI" width="80" />
      <el-table-column prop="trend" label="趋势" width="100"><template #default="{ row }"><el-tag :type="row.trend === '上升' ? 'success' : row.trend === '下降' ? 'danger' : ''">{{ row.trend }}</el-tag></template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import TableSearch from '@/components/TableSearch.vue'
import * as echarts from 'echarts'

const loading = ref(false)
const searchForm = reactive({ industry: undefined as string | undefined })
const stats = reactive({ benchRoi: 2.1, avgEngagement: 4.5, contentGrowth: 12, benchAccounts: 256 })
const industryList = ref([
  { industry: '科技', avgFollowers: '85万', avgViews: '15万', avgEngagement: '5.2%', roi: 2.8, trend: '上升' },
  { industry: '生活', avgFollowers: '62万', avgViews: '12万', avgEngagement: '4.8%', roi: 2.3, trend: '平稳' },
  { industry: '美食', avgFollowers: '78万', avgViews: '18万', avgEngagement: '6.1%', roi: 2.5, trend: '上升' },
])
const chartRef = ref<HTMLElement>()

const handleSearch = () => {}
const handleReset = () => { searchForm.industry = undefined }

let industryChart: echarts.ECharts | null = null
const loadChart = async () => {
  await nextTick()
  if (!chartRef.value || chartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(loadChart, 100)
    return
  }
  if (industryChart) {
    industryChart.dispose()
    industryChart = null
  }
  const chart = echarts.init(chartRef.value)
  industryChart = chart
  chart.setOption({
    title: { text: '行业对标分析', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['我方ROI', '行业基准'] },
    xAxis: { type: 'category', data: ['科技', '生活', '美食', '旅行', '教育'] },
    yAxis: { type: 'value' },
    series: [
      { name: '我方ROI', type: 'bar', data: [2.8, 2.3, 2.5, 2.1, 1.9] },
      { name: '行业基准', type: 'line', data: [2.1, 2.1, 2.1, 2.1, 2.1] }
    ]
  })
}

onMounted(() => loadChart())
</script>

<style scoped>
.industry-data-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.chart-card { margin-bottom: 16px; }
</style>
