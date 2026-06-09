<!--
  M6-2 状态监控报表
  依据: FR-M6-002
  路径: /analysis/report/account-status
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>状态监控</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card><div class="kpi-label">正常</div><div class="kpi-value success">{{ stats.normal }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="kpi-label">限流</div><div class="kpi-value warning">{{ stats.limited }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="kpi-label">封禁</div><div class="kpi-value danger">{{ stats.banned }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="kpi-label">待激活</div><div class="kpi-value">{{ stats.pending }}</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <ContentWrap title="状态分布">
          <div ref="pieRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
      <el-col :span="12">
        <ContentWrap title="7 天状态变更趋势">
          <div ref="lineRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="异常账号列表" style="margin-top: 16px">
      <el-table :data="abnormal" border stripe>
        <el-table-column prop="account" label="账号" min-width="200" />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="ipGroup" label="IP 组" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '封禁' ? 'danger' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="time" label="变更时间" width="160" align="center" />
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
const filter = reactive({ ipGroupId: undefined as number | undefined })
const stats = reactive({ normal: 0, limited: 0, banned: 0, pending: 0 })
const abnormal = ref<any[]>([])
const pieRef = ref<HTMLDivElement | null>(null)
const lineRef = ref<HTMLDivElement | null>(null)
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

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
        { name: '正常', value: stats.normal, itemStyle: { color: '#67c23a' } },
        { name: '限流', value: stats.limited, itemStyle: { color: '#e6a23c' } },
        { name: '封禁', value: stats.banned, itemStyle: { color: '#f56c6c' } },
        { name: '待激活', value: stats.pending, itemStyle: { color: '#909399' } },
      ],
    }],
  })
}
const initLine = () => {
  if (!lineRef.value) return
  const el = lineRef.value
  if (el.getBoundingClientRect().width === 0) return setTimeout(initLine, 100)
  if (!lineChart) lineChart = echarts.init(el)
  const days = Array.from({ length: 7 }, (_, i) => `${i + 1}日`)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['限流', '封禁'], top: 0 },
    grid: { left: 40, right: 16, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value' },
    series: [
      { name: '限流', type: 'line', data: [3, 5, 2, 7, 4, 6, 3], itemStyle: { color: '#e6a23c' }, smooth: true },
      { name: '封禁', type: 'line', data: [1, 0, 2, 1, 3, 0, 1], itemStyle: { color: '#f56c6c' }, smooth: true },
    ],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 300))
  stats.normal = 85
  stats.limited = 8
  stats.banned = 3
  stats.pending = 12
  abnormal.value = [
    { account: '抖音-知识变现研究院', platform: '抖音', ipGroup: '头部 IP', status: '封禁', reason: '涉嫌违规营销', time: '2026-06-07 14:30' },
    { account: '公众号-AI 技术前沿', platform: '公众号', ipGroup: '腰部 IP', status: '限流', reason: '近期互动率下降', time: '2026-06-06 09:15' },
    { account: '小红书-种草官 03', platform: '小红书', ipGroup: '尾部 IP', status: '限流', reason: '笔记重复度高', time: '2026-06-05 11:00' },
    { account: '快手-健身达人 05', platform: '快手', ipGroup: '尾部 IP', status: '封禁', reason: '违反社区规定', time: '2026-06-04 16:20' },
  ]
  loading.value = false
  await nextTick()
  setTimeout(() => { initPie(); initLine() }, 100)
}

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.4; text-align: center; padding: 8px 0; }
.kpi-value.success { color: #67c23a; }
.kpi-value.warning { color: #e6a23c; }
.kpi-value.danger { color: #f56c6c; }
</style>
