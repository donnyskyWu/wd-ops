<template>
  <div class="metric-analysis">
    <el-card class="search-card" shadow="never">
      <el-form :model="queryForm" ref="queryFormRef" inline>
        <el-form-item label="选择指标" prop="metricIds">
          <el-select v-model="queryForm.metricIds" multiple collapse-tags collapse-tags-tooltip placeholder="从指标库选择" style="width: 320px" filterable>
            <el-option v-for="m in metricOptions" :key="m.id" :label="`${m.metricName} (${m.metricCode})`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="queryForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleQuery">运行分析</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" :loading="exportLoading" :disabled="!metricList.length" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">已选指标</div>
            <div class="stat-value">{{ stats.totalMetrics }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">有数据指标</div>
            <div class="stat-value success">{{ stats.qualifiedMetrics }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">无数据指标</div>
            <div class="stat-value danger">{{ stats.unqualifiedMetrics }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">数据覆盖率</div>
            <div class="stat-value">{{ stats.qualificationRate }}%</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeViewTab" class="metric-view-tabs">
      <el-tab-pane label="指标明细" name="detail">
        <el-card class="table-card" shadow="never">
          <el-table :data="metricList" border stripe v-loading="loading">
            <template #empty><el-empty description="请选择指标并运行分析" /></template>
            <el-table-column prop="metricName" label="指标名称" width="180" />
            <el-table-column prop="metricType" label="类型" width="100">
              <template #default="{ row }"><DictLabel dict-type="dict_perf_metric_type" :value="row.metricType" /></template>
            </el-table-column>
            <el-table-column prop="currentValue" label="当前值" width="120" align="right" />
            <el-table-column prop="unit" label="单位" width="80" align="center" />
            <el-table-column prop="rowCount" label="明细行数" width="100" align="right" />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'qualified' ? 'success' : 'danger'">{{ row.status === 'qualified' ? '有数据' : '无数据' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="指标趋势" name="trend">
        <el-card class="chart-card" shadow="never">
          <div ref="trendChartRef" style="height: 400px" v-loading="loading" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getMetricList, previewMetric } from '@/api/metric'
import DictLabel from '@/components/DictLabel.vue'
import { exportToExcel, unwrapApiData, pickListPage } from '@/utils'

interface MetricOption {
  id: number
  metricName: string
  metricCode: string
  metricType: string
  metricFormula: string
  unit?: string
}

interface MetricResultRow {
  metricName: string
  metricType: string
  currentValue: number | string
  unit: string
  rowCount: number
  status: 'qualified' | 'unqualified'
  trendPoints: number[]
}

const queryFormRef = ref<FormInstance>()
const loading = ref(false)
const exportLoading = ref(false)
const activeViewTab = ref<'trend' | 'detail'>('detail')
const metricOptions = ref<MetricOption[]>([])

const queryForm = reactive({
  metricIds: [] as number[],
  dateRange: [] as string[],
})

const stats = reactive({
  totalMetrics: 0,
  qualifiedMetrics: 0,
  unqualifiedMetrics: 0,
  qualificationRate: 0,
})

const metricList = ref<MetricResultRow[]>([])
const trendChartRef = ref<HTMLElement>()

const extractScalar = (rows: Record<string, unknown>[]) => {
  if (!rows?.length) return 0
  const first = rows[0]
  for (const val of Object.values(first)) {
    if (typeof val === 'number') return val
    if (val != null && !Number.isNaN(Number(val))) return Number(val)
  }
  return rows.length
}

const loadMetricOptions = async () => {
  try {
    const res = await getMetricList({ pageNum: 1, pageSize: 200 })
    const page = pickListPage(unwrapApiData(res))
    metricOptions.value = page.list as MetricOption[]
  } catch (e) {
    console.error(e)
  }
}

const handleQuery = async () => {
  if (!queryForm.metricIds.length) {
    ElMessage.warning('请至少选择一个指标')
    return
  }
  loading.value = true
  try {
    const selected = metricOptions.value.filter(m => queryForm.metricIds.includes(m.id))
    const results: MetricResultRow[] = []
    for (const m of selected) {
      if (!m.metricFormula) {
        results.push({
          metricName: m.metricName,
          metricType: m.metricType,
          currentValue: '-',
          unit: m.unit || '',
          rowCount: 0,
          status: 'unqualified',
          trendPoints: [],
        })
        continue
      }
      const previewRes = await previewMetric({ metricFormula: m.metricFormula })
      const data = unwrapApiData(previewRes)
      const rows = (data?.rows ?? []) as Record<string, unknown>[]
      const scalar = extractScalar(rows)
      results.push({
        metricName: m.metricName,
        metricType: m.metricType,
        currentValue: scalar,
        unit: m.unit || '',
        rowCount: rows.length,
        status: rows.length > 0 ? 'qualified' : 'unqualified',
        trendPoints: rows.slice(0, 7).map((_, i) => extractScalar([rows[i] ?? {}])),
      })
    }
    metricList.value = results
    stats.totalMetrics = results.length
    stats.qualifiedMetrics = results.filter(r => r.status === 'qualified').length
    stats.unqualifiedMetrics = results.length - stats.qualifiedMetrics
    stats.qualificationRate = results.length ? Math.round((stats.qualifiedMetrics / results.length) * 100) : 0
    await nextTick()
    initTrendChart(results)
    ElMessage.success('分析完成')
  } catch (e) {
    console.error(e)
    ElMessage.error('指标运行失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryForm.metricIds = []
  queryForm.dateRange = []
  metricList.value = []
  stats.totalMetrics = 0
  stats.qualifiedMetrics = 0
  stats.unqualifiedMetrics = 0
  stats.qualificationRate = 0
}

const METRIC_TYPE_LABEL: Record<string, string> = {
  BASIC: '基础指标',
  COMPOSITE: '复合指标',
  DERIVED: '派生指标',
}

const handleExport = () => {
  if (!metricList.value.length) {
    ElMessage.warning('请先运行分析后再导出')
    return
  }
  if (activeViewTab.value === 'detail') {
    const exportData = metricList.value.map(row => ({
      metricName: row.metricName,
      metricType: METRIC_TYPE_LABEL[row.metricType] || row.metricType,
      currentValue: row.currentValue,
      unit: row.unit,
      rowCount: row.rowCount,
      status: row.status === 'qualified' ? '有数据' : '无数据',
    }))
    exportToExcel(
      exportData,
      [
        { key: 'metricName', label: '指标名称' },
        { key: 'metricType', label: '类型' },
        { key: 'currentValue', label: '当前值' },
        { key: 'unit', label: '单位' },
        { key: 'rowCount', label: '明细行数' },
        { key: 'status', label: '状态' },
      ],
      '指标分析明细',
    )
    return
  }
  const maxPoints = Math.max(...metricList.value.map(r => r.trendPoints.length), 1)
  const columns = [
    { key: 'metricName', label: '指标名称' },
    ...Array.from({ length: maxPoints }, (_, i) => ({ key: `p${i + 1}`, label: `点${i + 1}` })),
  ]
  const exportData = metricList.value.map(row => {
    const item: Record<string, string | number> = { metricName: row.metricName }
    for (let i = 0; i < maxPoints; i += 1) {
      item[`p${i + 1}`] = row.trendPoints[i] ?? (i === 0 ? row.currentValue : '')
    }
    return item
  })
  exportToExcel(exportData, columns, '指标分析趋势')
}

let metricTrendChart: echarts.ECharts | null = null
const initTrendChart = (results: MetricResultRow[]) => {
  if (!trendChartRef.value) return
  if (metricTrendChart) {
    metricTrendChart.dispose()
    metricTrendChart = null
  }
  const chart = echarts.init(trendChartRef.value)
  metricTrendChart = chart
  const labels = results.map((_, i) => `点${i + 1}`)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: results.map(r => r.metricName) },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels },
    yAxis: { type: 'value' },
    series: results.map(r => ({
      name: r.metricName,
      type: 'line',
      data: r.trendPoints.length ? r.trendPoints : [r.currentValue],
    })),
  })
}

onMounted(() => loadMetricOptions())
</script>

<style scoped lang="scss">
.metric-analysis {
  .search-card { margin-bottom: 16px; }
  .stats-row { margin-bottom: 16px; }
  .stat-item {
    text-align: center;
    padding: 10px 0;
    .stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
    .stat-value {
      font-size: 28px; font-weight: bold; color: #303133;
      &.success { color: #67c23a; }
      &.danger { color: #f56c6c; }
    }
  }
  .metric-view-tabs {
    margin-top: 8px;
  }
  .chart-card, .table-card {
    margin-bottom: 16px;
    .card-header { display: flex; justify-content: space-between; align-items: center; }
  }
}
</style>
