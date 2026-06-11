<template>
  <div class="report-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>状态监控</el-breadcrumb-item>
    </el-breadcrumb>
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="账号">
          <AccountSelect v-model="filter.accountId" style="width: 220px" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap title="状态趋势" style="margin-top: 16px">
      <div ref="trendRef" style="width: 100%; height: 320px" />
    </ContentWrap>
    <ContentWrap title="状态摘要" style="margin-top: 16px">
      <el-row :gutter="16">
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="在线" :value="summary.online || 0" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="异常" :value="summary.abnormal || 0" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="掉线" :value="summary.offline || 0" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="恢复" :value="summary.recovered || 0" /></el-card></el-col>
      </el-row>
    </ContentWrap>
    <ContentWrap title="状态日志" style="margin-top: 16px">
      <el-table :data="logList" border stripe v-loading="loading">
        <el-table-column label="日期" width="120">
          <template #default="{ row }">{{ reportField(row, 'date', 'statDate') }}</template>
        </el-table-column>
        <el-table-column label="账号" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ reportField(row, 'account_name', 'accountName') }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(String(reportField(row, 'status') || ''))" size="small">
              {{ statusLabel(String(reportField(row, 'status') || '')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ reportField(row, 'remark') || '-' }}</template>
        </el-table-column>
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
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import { getAccountStatusTrend, getAccountStatusSummary, getAccountStatusLog } from '@/api/report'
import { unwrapApiData, pickListPage, reportField } from '@/utils'

const loading = ref(false)
const filter = reactive({ accountId: undefined as number | undefined, dateRange: [] as string[] })
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const logList = ref<any[]>([])
const summary = reactive<any>({ online: 0, abnormal: 0, offline: 0, recovered: 0 })
const trendRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

const getStatusType = (s: string) => {
  const map: Record<string, string> = {
    NORMAL: 'success', ONLINE: 'success',
    WARNING: 'warning', ABNORMAL: 'warning',
    OFFLINE: 'danger',
    RECOVERED: 'info',
  }
  return map[s] || ''
}

const statusLabel = (s: string) => {
  const map: Record<string, string> = {
    NORMAL: '正常', ONLINE: '在线',
    WARNING: '预警', ABNORMAL: '异常',
    OFFLINE: '掉线', RECOVERED: '已恢复',
  }
  return map[s] || s || '-'
}

const buildQ = () => {
  const q: Record<string, any> = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filter.accountId) q.accountId = filter.accountId
  if (filter.dateRange?.length === 2) { q.startDate = filter.dateRange[0]; q.endDate = filter.dateRange[1] }
  return q
}

const loadData = async () => {
  loading.value = true
  try {
    const q = buildQ()
    const [trendRes, sumRes, logRes] = await Promise.all([
      getAccountStatusTrend({ accountId: filter.accountId, startDate: q.startDate, endDate: q.endDate }),
      getAccountStatusSummary({ accountId: filter.accountId, startDate: q.startDate, endDate: q.endDate }),
      getAccountStatusLog(q),
    ])
    const t = unwrapApiData(trendRes)
    drawTrend(Array.isArray(t) ? t : [])
    Object.assign(summary, unwrapApiData(sumRes))
    const l = pickListPage(unwrapApiData(logRes))
    logList.value = l.list
    total.value = l.total
  } catch (e) { console.error(e); logList.value = [] }
  finally { loading.value = false }
}

const drawTrend = (rows: any[]) => {
  if (!trendRef.value) return
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) { setTimeout(() => drawTrend(rows), 100); return }
  if (!chart) chart = echarts.init(el)
  const dates = Array.from(new Set(rows.map((r: any) => r.date || r.stat_date))).sort()
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['在线', '异常', '掉线'], top: 0 },
    grid: { left: 40, right: 16, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '在线', type: 'line', smooth: true, data: dates.map(d => rows.find((r: any) => (r.date || r.stat_date) === d)?.online || 0) },
      { name: '异常', type: 'line', smooth: true, data: dates.map(d => rows.find((r: any) => (r.date || r.stat_date) === d)?.abnormal || 0) },
      { name: '掉线', type: 'line', smooth: true, data: dates.map(d => rows.find((r: any) => (r.date || r.stat_date) === d)?.offline || 0) },
    ],
  })
}

onMounted(() => loadData())
</script>
<style scoped>
.report-page { padding: 20px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
