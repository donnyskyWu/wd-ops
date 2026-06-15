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
            <el-option label="账号" value="ACCOUNT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="数据来源于订单归因（营收/归因成本）与账号成本台账；开发环境为 seed 演示数据，非外部财务系统实时同步。"
        style="margin-top: 8px"
      />
    </ContentWrap>

    <ContentWrap title="总体财务分析" style="margin-top: 16px">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="总营收" :value="summary.totalRevenue" :precision="2" prefix="¥" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="总成本" :value="summary.totalCost" :precision="2" prefix="¥" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="总毛利" :value="summary.totalProfit" :precision="2" prefix="¥" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="综合 ROI" :value="summary.roi" :precision="2" />
          </el-card>
        </el-col>
      </el-row>
    </ContentWrap>

    <ContentWrap title="营收趋势" style="margin-top: 16px">
      <div v-if="!trendPoints.length" class="empty-hint">所选时间范围内暂无趋势数据</div>
      <div v-show="trendPoints.length" ref="trendRef" style="width: 100%; height: 360px" />
    </ContentWrap>

    <ContentWrap title="维度拆解" style="margin-top: 16px">
      <el-table :data="breakdownRows" border stripe empty-text="暂无拆解数据">
        <el-table-column prop="name" label="维度项" min-width="200" show-overflow-tooltip />
        <el-table-column label="营收" width="140" align="right">
          <template #default="{ row }">¥ {{ formatMoney(row.revenue) }}</template>
        </el-table-column>
        <el-table-column label="成本" width="140" align="right">
          <template #default="{ row }">¥ {{ formatMoney(row.cost) }}</template>
        </el-table-column>
        <el-table-column label="毛利" width="140" align="right">
          <template #default="{ row }">¥ {{ formatMoney(row.profit) }}</template>
        </el-table-column>
        <el-table-column label="ROI" width="100" align="right">
          <template #default="{ row }">
            <el-tag :type="Number(row.roi) >= 1 ? 'success' : 'danger'" size="small">
              {{ formatMoney(row.roi) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <ContentWrap title="成本结构" style="margin-top: 16px">
      <el-table :data="costTypeRows" border stripe empty-text="暂无成本结构数据">
        <el-table-column label="成本类型" min-width="160">
          <template #default="{ row }">
            <DictLabel v-if="row.type" dict-type="dict_cost_type" :value="row.type" />
            <span v-else>{{ row.typeLabel || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="占比" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.percentage) }}%</template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import { getRoiAnalysis, getRoiTrend, getRoiBreakdown, exportRoi } from '@/api/finance'
import { exportToExcel, unwrapApiData } from '@/utils'

interface TrendPoint {
  statDate?: string
  date?: string
  revenue?: number
  cost?: number
  roi?: number
}

interface RoiDetailRow {
  name?: string
  revenue?: number
  cost?: number
  roi?: number
  profit?: number
}

interface CostTypeRow {
  type?: string
  typeLabel?: string
  amount?: number
  percentage?: number
}

const loading = ref(false)
const queryForm = reactive({
  dateRange: [] as string[],
  ipGroupId: undefined as number | undefined,
  dimension: 'IP_GROUP',
})

const summary = reactive({
  totalRevenue: 0,
  totalCost: 0,
  totalProfit: 0,
  roi: 0,
})

const breakdownRows = ref<RoiDetailRow[]>([])
const costTypeRows = ref<CostTypeRow[]>([])
const trendPoints = ref<TrendPoint[]>([])
const trendRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null

const toNum = (v: unknown) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

const formatMoney = (v: unknown) => toNum(v).toFixed(2)

const buildQ = () => {
  if (!queryForm.dateRange?.length) return null
  return {
    startDate: queryForm.dateRange[0],
    endDate: queryForm.dateRange[1],
    ipGroupId: queryForm.ipGroupId,
    dimension: queryForm.dimension,
  }
}

const mapDetails = (details: unknown[]): RoiDetailRow[] =>
  (details ?? []).map((raw) => {
    const row = raw as RoiDetailRow
    const revenue = toNum(row.revenue)
    const cost = toNum(row.cost)
    return {
      name: row.name || '—',
      revenue,
      cost,
      profit: revenue - cost,
      roi: toNum(row.roi),
    }
  })

const loadData = async () => {
  const q = buildQ()
  if (!q) {
    ElMessage.warning('请选择时间范围')
    return
  }
  loading.value = true
  try {
    const [aRes, tRes, bRes] = await Promise.all([
      getRoiAnalysis(q),
      getRoiTrend({ startDate: q.startDate, endDate: q.endDate, ipGroupId: q.ipGroupId }),
      getRoiBreakdown({ startDate: q.startDate, endDate: q.endDate, ipGroupId: q.ipGroupId }),
    ])
    const analysis = unwrapApiData(aRes) as {
      totalRevenue?: number
      totalCost?: number
      roi?: number
      details?: unknown[]
    }
    summary.totalRevenue = toNum(analysis?.totalRevenue)
    summary.totalCost = toNum(analysis?.totalCost)
    summary.totalProfit = summary.totalRevenue - summary.totalCost
    summary.roi = toNum(analysis?.roi)

    breakdownRows.value = mapDetails(analysis?.details ?? [])

    const trendVo = unwrapApiData(tRes) as { points?: TrendPoint[] }
    trendPoints.value = trendVo?.points ?? []
    await nextTick()
    drawTrend(trendPoints.value)

    const breakdownVo = unwrapApiData(bRes) as { byType?: CostTypeRow[] }
    costTypeRows.value = breakdownVo?.byType ?? []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载财务分析数据失败')
  } finally {
    loading.value = false
  }
}

const drawTrend = (rows: TrendPoint[]) => {
  if (!trendRef.value || !rows.length) {
    trendChart?.clear()
    return
  }
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(() => drawTrend(rows), 100)
    return
  }
  if (!trendChart) trendChart = echarts.init(el)
  const dates = rows.map((r) => r.statDate || r.date || '')
  const revenues = rows.map((r) => toNum(r.revenue))
  const costs = rows.map((r) => toNum(r.cost))
  const profits = rows.map((r) => toNum(r.revenue) - toNum(r.cost))
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '成本', '毛利'], top: 0 },
    grid: { left: 50, right: 16, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '营收', type: 'line', smooth: true, data: revenues },
      { name: '成本', type: 'line', smooth: true, data: costs },
      { name: '毛利', type: 'line', smooth: true, data: profits },
    ],
  })
}

const handleExport = async () => {
  const q = buildQ()
  if (!q) {
    ElMessage.warning('请选择时间范围')
    return
  }
  const columns = [
    { key: 'name', label: '维度项' },
    { key: 'revenue', label: '营收' },
    { key: 'cost', label: '成本' },
    { key: 'profit', label: '毛利' },
    { key: 'roi', label: 'ROI' },
  ]
  try {
    await exportRoi({ startDate: q.startDate, endDate: q.endDate })
    ElMessage.success('导出任务已提交')
  } catch (e: unknown) {
    console.error('[FinancialAnalysis] 后端导出失败，降级为前端导出:', e)
    exportToExcel(breakdownRows.value, columns, '财务分析')
  }
}

const handleResize = () => trendChart?.resize()

onMounted(() => {
  const today = new Date()
  const start = new Date(today.getTime() - 30 * 24 * 3600 * 1000)
  queryForm.dateRange = [start.toISOString().slice(0, 10), today.toISOString().slice(0, 10)]
  window.addEventListener('resize', handleResize)
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  trendChart = null
})
</script>

<style scoped>
.financial-analysis-page {
  padding: 20px;
}
.empty-hint {
  color: var(--el-text-color-secondary);
  text-align: center;
  padding: 48px 0;
}
</style>
