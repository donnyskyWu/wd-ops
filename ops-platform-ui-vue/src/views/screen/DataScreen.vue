<template>
  <div class="data-screen-page">
    <div class="screen-header">
      <h1>运营数据平台 - 数据大屏</h1>
      <div>
        <el-button @click="handleRefresh" :icon="Refresh">刷新</el-button>
        <el-button type="primary" @click="toggleFullscreen" :icon="FullScreen">全屏</el-button>
      </div>
    </div>
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="4" v-for="kpi in kpis" :key="kpi.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
          <div class="kpi-label">{{ kpi.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" class="chart-row">
      <el-col :span="6">
        <el-card class="chart-card"><div ref="contentTrendRef" style="height: 250px"></div></el-card>
        <el-card class="chart-card"><div ref="followerTrendRef" style="height: 250px"></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="chart-card"><div ref="platformDistRef" style="height: 250px"></div></el-card>
        <el-card class="chart-card"><div ref="roiTrendRef" style="height: 250px"></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="chart-card">
          <div class="alert-list">
            <div v-for="(alert, i) in alerts" :key="i" class="alert-item">
              <el-tag :type="alert.type" size="small">{{ alert.tag }}</el-tag>
              <span>{{ alert.msg }}</span>
            </div>
          </div>
        </el-card>
        <el-card class="chart-card">
          <div class="rank-list">
            <div v-for="(item, i) in rankings" :key="i" class="rank-item">
              <span class="rank-num">{{ i + 1 }}</span>
              <span class="rank-name">{{ item.name }}</span>
              <span class="rank-value">¥{{ item.value }}万</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, FullScreen } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const kpis = ref([
  { label: '总粉丝', value: '1,234.5万', color: '#409EFF' },
  { label: '总营收', value: '¥890.23万', color: '#67C23A' },
  { label: '总成本', value: '¥500.10万', color: '#E6A23C' },
  { label: '综合ROI', value: '1.78', color: '#67C23A' },
  { label: '活跃号', value: '156个', color: '#409EFF' },
])

const alerts = ref([
  { type: 'danger', tag: '紧急', msg: '账号A粉丝异常下降' },
  { type: 'warning', tag: '警告', msg: 'ROI低于预期阈值' },
  { type: 'info', tag: '提示', msg: '新账号B达到考核标准' },
])

const rankings = ref([
  { name: 'IP组-科技', value: 120 },
  { name: 'IP组-生活', value: 98 },
  { name: 'IP组-美食', value: 85 },
  { name: 'IP组-旅行', value: 72 },
  { name: 'IP组-教育', value: 65 },
])

const contentTrendRef = ref<HTMLElement>()
const followerTrendRef = ref<HTMLElement>()
const platformDistRef = ref<HTMLElement>()
const roiTrendRef = ref<HTMLElement>()

const loadCharts = async () => {
  await nextTick()
  const contentChart = echarts.init(contentTrendRef.value!)
  contentChart.setOption({
    title: { text: '内容发布趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['5/1', '5/5', '5/10', '5/15', '5/20', '5/25', '5/27'] },
    yAxis: { type: 'value' },
    series: [{ name: '发布数', type: 'line', data: [120, 132, 101, 134, 90, 230, 210], smooth: true }]
  })

  const followerChart = echarts.init(followerTrendRef.value!)
  followerChart.setOption({
    title: { text: '粉丝增长趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['5/1', '5/5', '5/10', '5/15', '5/20', '5/25', '5/27'] },
    yAxis: { type: 'value' },
    series: [{ name: '粉丝数', type: 'line', data: [1000, 1200, 1500, 1800, 2100, 2400, 2600], areaStyle: {}, smooth: true }]
  })

  const platformChart = echarts.init(platformDistRef.value!)
  platformChart.setOption({
    title: { text: '平台分布', left: 'center' },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '60%', data: [
      { value: 35, name: '抖音' }, { value: 25, name: '快手' },
      { value: 20, name: '小红书' }, { value: 15, name: 'B站' }, { value: 5, name: '其他' }
    ]}]
  })

  const roiChart = echarts.init(roiTrendRef.value!)
  roiChart.setOption({
    title: { text: 'ROI趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月'] },
    yAxis: { type: 'value', min: 0, max: 3 },
    series: [{ name: 'ROI', type: 'line', data: [1.5, 1.6, 1.7, 1.75, 1.78], smooth: true }]
  })
}

const handleRefresh = () => { ElMessage.success('数据已刷新'); loadCharts() }
const toggleFullscreen = () => { document.documentElement.requestFullscreen() }

onMounted(() => loadCharts())
</script>

<style scoped>
.data-screen-page { padding: 20px; background: #0a1929; color: #fff; min-height: 100vh; }
.screen-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.screen-header h1 { color: #00d4ff; margin: 0; }
.kpi-row { margin-bottom: 16px; }
.kpi-card { text-align: center; background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); }
.kpi-value { font-size: 28px; font-weight: bold; margin-bottom: 8px; }
.kpi-label { font-size: 14px; color: #aaa; }
.chart-row .chart-card { margin-bottom: 16px; background: rgba(255, 255, 255, 0.05); }
.alert-list { max-height: 250px; overflow-y: auto; }
.alert-item { padding: 8px; border-bottom: 1px solid rgba(255, 255, 255, 0.1); display: flex; gap: 8px; align-items: center; }
.rank-list { padding: 10px; }
.rank-item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.1); }
.rank-num { width: 24px; height: 24px; background: #409EFF; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; }
.rank-name { flex: 1; margin-left: 12px; }
.rank-value { color: #00d4ff; font-weight: bold; }
</style>
