<template>
  <div class="financial-analysis-page" v-loading="loading">
    <ContentWrap>
      <el-form :model="queryForm" inline>
        <el-form-item label="时间">
          <el-date-picker v-model="queryForm.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="queryForm.ipGroupId" style="width: 200px" />
        </el-form-item>
        <el-form-item label="维度">
          <el-select v-model="queryForm.dimension" style="width: 140px">
            <el-option label="IP 组" value="IP_GROUP" />
            <el-option label="平台" value="PLATFORM" />
            <el-option label="账号" value="ACCOUNT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap title="总体财务分析" style="margin-top: 16px">
      <el-row :gutter="16">
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="总营收" :value="Number(analysis?.totalRevenue || 0).toFixed(2)" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="总成本" :value="Number(analysis?.totalCost || 0).toFixed(2)" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="总毛利" :value="Number(analysis?.totalProfit || 0).toFixed(2)" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="综合 ROI" :value="Number(analysis?.overallRoi || 0).toFixed(2)" /></el-card></el-col>
      </el-row>
    </ContentWrap>

    <ContentWrap title="营收趋势" style="margin-top: 16px">
      <div ref="trendRef" style="width: 100%; height: 360px" />
    </ContentWrap>

    <ContentWrap title="维度拆解" style="margin-top: 16px">
      <el-table :data="breakdownRows" border stripe>
        <el-table-column prop="dim_name" label="维度项" min-width="200" />
        <el-table-column prop="revenue" label="营收" width="140" align="right">
          <template #default="{ row }">¥ {{ Number(row.revenue || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="140" align="right">
          <template #default="{ row }">¥ {{ Number(row.cost || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="毛利" width="140" align="right">
          <template #default="{ row }">¥ {{ Number(row.profit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="roi" label="ROI" width="100" align="right">
          <template #default="{ row }">
            <el-tag :type="(row.roi || 0) >= 1 ? 'success' : 'danger'" size="small">{{ Number(row.roi || 0).toFixed(2) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { getRoiAnalysis, getRoiTrend, getRoiBreakdown, exportRoi } from '@/api/finance'
import { exportToExcel, unwrapApiData } from '@/utils'

const loading = ref(false)
const queryForm = reactive({
  dateRange: [] as string[],
  ipGroupId: undefined as number | undefined,
  dimension: 'IP_GROUP',
})

const analysis = ref<any>({})
const breakdownRows = ref<any[]>([])
const trendRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null

const buildQ = () => {
  if (!queryForm.dateRange?.length) return null
  return {
    startDate: queryForm.dateRange[0],
    endDate: queryForm.dateRange[1],
    ipGroupId: queryForm.ipGroupId,
    dimension: queryForm.dimension,
  }
}

const loadData = async () => {
  const q = buildQ()
  if (!q) { ElMessage.warning('请选择时间范围'); return }
  loading.value = true
  try {
    const [aRes, tRes, bRes] = await Promise.all([
      getRoiAnalysis(q),
      getRoiTrend({ startDate: q.startDate, endDate: q.endDate, ipGroupId: q.ipGroupId }),
      getRoiBreakdown({ startDate: q.startDate, endDate: q.endDate, ipGroupId: q.ipGroupId }),
    ])
    analysis.value = unwrapApiData(aRes)
    const t = unwrapApiData(tRes) as { points?: unknown[]; trend?: unknown[] }
    drawTrend(t?.points ?? t?.trend ?? [])
    const b = unwrapApiData(bRes) as { rows?: unknown[]; items?: unknown[] }
    breakdownRows.value = b?.rows ?? b?.items ?? []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const drawTrend = (rows: any[]) => {
  if (!trendRef.value) return
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) { setTimeout(() => drawTrend(rows), 100); return }
  if (!trendChart) trendChart = echarts.init(el)
  const dates = rows.map((r: any) => r.date || r.stat_date)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '成本', '毛利'], top: 0 },
    grid: { left: 50, right: 16, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '营收', type: 'line', smooth: true, data: rows.map((r: any) => r.revenue || 0) },
      { name: '成本', type: 'line', smooth: true, data: rows.map((r: any) => r.cost || 0) },
      { name: '毛利', type: 'line', smooth: true, data: rows.map((r: any) => r.profit || 0) },
    ],
  })
}

const handleExport = async () => {
  const q = buildQ()
  if (!q) { ElMessage.warning('请选择时间范围'); return }
  const columns = [
    { key: 'dim_name', label: '维度项' },
    { key: 'revenue', label: '营收' },
    { key: 'cost', label: '成本' },
    { key: 'profit', label: '毛利' },
    { key: 'roi', label: 'ROI' },
  ]
  try {
    await exportRoi({ startDate: q.startDate, endDate: q.endDate })
    ElMessage.success('导出任务已提交')
  } catch (e: any) {
    console.error('[FinancialAnalysis] 后端导出失败，降级为前端导出:', e)
    exportToExcel(breakdownRows.value, columns, '财务分析')
  }
}

onMounted(() => {
  const today = new Date()
  const start = new Date(today.getTime() - 30 * 24 * 3600 * 1000)
  queryForm.dateRange = [start.toISOString().slice(0, 10), today.toISOString().slice(0, 10)]
  loadData()
})
</script>

<style scoped>
.financial-analysis-page { padding: 20px; }
</style>
