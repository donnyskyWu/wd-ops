<template>
  <div class="report-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>直播时长</el-breadcrumb-item>
    </el-breadcrumb>
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" style="width: 220px" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap title="直播时长趋势" style="margin-top: 16px">
      <div ref="trendRef" style="width: 100%; height: 320px" />
    </ContentWrap>
    <ContentWrap title="直播时长明细" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="account_name" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="session_count" label="场次" width="100" align="right" />
        <el-table-column prop="total_duration" label="总时长(分钟)" width="140" align="right" />
        <el-table-column prop="avg_duration" label="均时长" width="120" align="right" />
        <el-table-column prop="peak_viewers" label="峰值在线" width="120" align="right" />
      </el-table>
      <el-pagination :current-page="pageNum" :page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next"
        class="pagination" @update:current-page="(v) => { pageNum = v; loadData() }"
        @update:page-size="(v) => { pageSize = v; pageNum = 1; loadData() }" />
    </ContentWrap>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { getLiveDurationList, getLiveDurationTrend } from '@/api/report'

const loading = ref(false)
const filter = reactive({ ipGroupId: undefined as number | undefined, dateRange: [] as string[] })
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const list = ref<any[]>([])
const trendRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

const buildQuery = () => {
  const q: Record<string, any> = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filter.ipGroupId) q.ipGroupId = filter.ipGroupId
  if (filter.dateRange?.length === 2) { q.startDate = filter.dateRange[0]; q.endDate = filter.dateRange[1] }
  return q
}

const loadData = async () => {
  loading.value = true
  try {
    const q = buildQuery()
    const [listRes, trendRes] = await Promise.all([
      getLiveDurationList(q),
      getLiveDurationTrend({ ipGroupId: filter.ipGroupId, startDate: q.startDate, endDate: q.endDate }),
    ])
    const l = (listRes as any)?.data ?? listRes
    list.value = l?.list ?? l?.records ?? []
    total.value = l?.total ?? list.value.length
    const t = (trendRes as any)?.data ?? trendRes
    drawTrend(Array.isArray(t) ? t : [])
  } catch (e) {
    console.error(e); list.value = []
  } finally { loading.value = false }
}

const drawTrend = (rows: any[]) => {
  if (!trendRef.value) return
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) { setTimeout(() => drawTrend(rows), 100); return }
  if (!chart) chart = echarts.init(el)
  const dates = Array.from(new Set(rows.map((r: any) => r.date || r.stat_date))).sort()
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['场次', '总时长'], top: 0 },
    grid: { left: 40, right: 40, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: [{ type: 'value', name: '场次' }, { type: 'value', name: '分钟' }],
    series: [
      { name: '场次', type: 'bar', data: dates.map(d => rows.find((r: any) => (r.date || r.stat_date) === d)?.session_count || 0) },
      { name: '总时长', type: 'line', yAxisIndex: 1, smooth: true, data: dates.map(d => rows.find((r: any) => (r.date || r.stat_date) === d)?.total_duration || 0) },
    ],
  })
}

onMounted(() => loadData())
</script>
<style scoped>
.report-page { padding: 20px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
