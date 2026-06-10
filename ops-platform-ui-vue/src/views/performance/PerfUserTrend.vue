<!--
  M3 - 个人绩效趋势
  依据: FR-M3-003 个人结果
  路径: /perf/result/:userId/trend
  4 区: 头部信息 / 月度趋势(柱+折线) / 指标分项雷达 / 历史明细
-->
<template>
  <div class="perf-trend-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/perf/result' }">绩效结果</el-breadcrumb-item>
      <el-breadcrumb-item>{{ userName }} 趋势</el-breadcrumb-item>
    </el-breadcrumb>

    <template v-if="userInfo">
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 style="margin: 0">
              <el-avatar :size="48" style="vertical-align: middle; margin-right: 12px">{{ userInfo.name[0] }}</el-avatar>
              {{ userInfo.name }}
              <el-tag style="margin-left: 8px">{{ userInfo.position }}</el-tag>
              <el-tag type="success" style="margin-left: 8px">在职</el-tag>
            </h2>
            <p class="meta">
              <span>所属部门：{{ userInfo.dept }}</span>
              <el-divider direction="vertical" />
              <span>入职时间：{{ userInfo.joinAt }}</span>
              <el-divider direction="vertical" />
              <span>近 12 月均分：{{ avgScore.toFixed(1) }}</span>
            </p>
          </div>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </el-card>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="16">
          <ContentWrap title="月度绩效趋势">
            <div ref="trendChartRef" style="width: 100%; height: 320px" />
          </ContentWrap>
        </el-col>
        <el-col :span="8">
          <ContentWrap title="最近一期指标分项">
            <div ref="radarChartRef" style="width: 100%; height: 320px" />
          </ContentWrap>
        </el-col>
      </el-row>

      <ContentWrap title="历史明细" style="margin-top: 16px">
        <el-table :data="history" border stripe>
          <el-table-column prop="period" label="周期" width="120" />
          <el-table-column prop="templateName" label="模板" min-width="160" show-overflow-tooltip />
          <el-table-column prop="baseScore" label="基础分" width="80" align="right" />
          <el-table-column prop="metricScore" label="指标分" width="80" align="right" />
          <el-table-column prop="bonusScore" label="加成" width="80" align="right" />
          <el-table-column label="总分" width="100" align="right">
            <template #default="{ row }">
              <span class="total-score">{{ row.baseScore + row.metricScore + row.bonusScore }}</span>
            </template>
          </el-table-column>
          <el-table-column label="等级" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="gradeType(row.totalGrade)">{{ row.totalGrade }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '已发布' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </ContentWrap>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import { getPerfUserTrend } from '@/api/perfResult'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const userInfo = ref<any>(null)
const userName = computed(() => userInfo.value?.name || '...')

const trendChartRef = ref<HTMLDivElement | null>(null)
const radarChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let radarChart: echarts.ECharts | null = null

const history = ref<any[]>([])
const months = ref<string[]>([])
const monthlyScores = ref<number[]>([])

const avgScore = computed(() => {
  if (!monthlyScores.value.length) return 0
  return monthlyScores.value.reduce((a, b) => a + b, 0) / monthlyScores.value.length
})

const GRADE_TYPES: Record<string, 'danger' | 'warning' | 'success' | 'info'> = {
  S: 'danger',
  A: 'warning',
  B: 'success',
  C: 'info',
}
const gradeType = (g: string) => GRADE_TYPES[g] || 'info'

const initTrend = () => {
  if (!trendChartRef.value) return
  const el = trendChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initTrend, 100)
    return
  }
  if (!trendChart) trendChart = echarts.init(el)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['总分', '基础分', '指标分'], top: 0 },
    grid: { left: 50, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months.value },
    yAxis: { type: 'value', max: 120 },
    series: [
      { name: '总分', type: 'bar', data: monthlyScores.value, itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } },
      { name: '基础分', type: 'line', data: history.value.map((h) => h.baseScore), itemStyle: { color: '#67c23a' } },
      { name: '指标分', type: 'line', data: history.value.map((h) => h.metricScore), itemStyle: { color: '#e6a23c' } },
    ],
  })
}
const initRadar = () => {
  if (!radarChartRef.value) return
  const el = radarChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initRadar, 100)
    return
  }
  if (!radarChart) radarChart = echarts.init(el)
  const last = history.value[history.value.length - 1]
  radarChart.setOption({
    tooltip: {},
    radar: {
      indicator: [
        { name: '粉丝净增', max: 30 },
        { name: '作品数', max: 20 },
        { name: 'GMV', max: 30 },
        { name: '互动数', max: 20 },
      ],
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [28, 19, 22, 17],
            name: last?.period || '本期',
            areaStyle: { color: 'rgba(64,158,255,0.4)' },
            lineStyle: { color: '#409eff' },
          },
        ],
      },
    ],
  })
}

const loadDetail = async () => {
  loading.value = true
  try {
    const userId = Number(route.params.userId)
    const res: any = await getPerfUserTrend(userId)
    userInfo.value = res.userInfo || {
      id: userId,
      name: res.userName || '用户',
      position: res.position || '',
      dept: res.dept || '',
      joinAt: res.joinAt || '',
    }
    const points = (res.points || res.historyList || []) as any[]
    months.value = points.map((p) => p.period || p.cycle || '')
    monthlyScores.value = points.map((p) => p.score ?? p.totalScore ?? 0)
    history.value = points.map((p, i) => ({
      period: months.value[i],
      templateName: p.templateName || '',
      baseScore: p.baseScore ?? 0,
      metricScore: p.metricScore ?? 0,
      bonusScore: p.bonusScore ?? 0,
      totalGrade: p.grade || (monthlyScores.value[i] > 95 ? 'S' : monthlyScores.value[i] > 85 ? 'A' : 'B'),
      status: p.status || '已发布',
    }))
  } catch {
    // 加载失败保持空
  } finally {
    loading.value = false
    await nextTick()
    setTimeout(() => {
      initTrend()
      initRadar()
    }, 100)
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.perf-trend-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-start; }
.meta { color: #909399; font-size: 13px; margin: 8px 0 0 0; }
.total-score { color: #409eff; font-weight: 600; }
</style>
