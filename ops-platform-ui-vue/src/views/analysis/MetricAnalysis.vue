<template>

  <div class="metric-analysis">

    <el-card class="search-card" shadow="never">

      <el-form :model="queryForm" ref="queryFormRef" inline>

        <el-form-item label="选择指标" prop="metricIds">

          <el-select v-model="queryForm.metricIds" multiple collapse-tags collapse-tags-tooltip placeholder="从指标库选择" style="width: 320px" filterable>

            <el-option v-for="m in metricOptions" :key="m.id" :label="`${m.metricName} (${m.metricCode})`" :value="m.id" />

          </el-select>

        </el-form-item>

        <template v-if="metricParams.length">

          <el-form-item

            v-for="param in metricParams"

            :key="param.key"

            :label="param.label"

          >

            <MetricConditionInput

              v-model="queryForm.bindParams[param.key]"

              :query-condition-type="param.queryConditionType"

              :dict-type="param.dictType"

              :placeholder="`可选：${param.label}`"

              clearable

              style="width: 240px"

            />

          </el-form-item>

        </template>

        <el-form-item>

          <el-button type="primary" :loading="loading" @click="handleQuery">运行分析</el-button>

          <el-button @click="handleReset">重置</el-button>

          <el-button type="success" :loading="exportLoading" :disabled="!metricResults.length" @click="handleExport">

            <el-icon><Download /></el-icon>

            导出

          </el-button>

        </el-form-item>

      </el-form>

    </el-card>



    <el-row :gutter="16" class="stats-row" v-if="metricResults.length">

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



    <el-tabs v-model="activeViewTab" class="metric-view-tabs" @tab-change="handleViewTabChange">

      <el-tab-pane label="指标明细" name="detail">

        <el-empty v-if="!metricResults.length && !loading" description="请选择指标并运行分析" />

        <el-tabs v-else v-model="activeMetricTab" type="card" class="metric-result-tabs">

          <el-tab-pane

            v-for="result in metricResults"

            :key="result.metricId"

            :label="result.metricName"

            :name="String(result.metricId)"

          >

            <el-card class="table-card" shadow="never">

              <template #header>

                <div class="result-header">

                  <span>{{ result.metricName }}</span>

                  <span class="row-count">共 {{ result.rows.length }} 行</span>

                </div>

              </template>

              <el-table :data="result.rows" border stripe max-height="480" v-loading="loading">

                <template #empty><el-empty description="暂无数据" /></template>

                <el-table-column

                  v-for="col in result.columns"

                  :key="col.prop"

                  :prop="col.prop"

                  :label="col.label"

                  min-width="120"

                  show-overflow-tooltip

                />

              </el-table>

            </el-card>

          </el-tab-pane>

        </el-tabs>

      </el-tab-pane>

      <el-tab-pane label="指标趋势" name="trend">

        <el-empty v-if="!metricResults.length && !loading" description="请选择指标并运行分析" />

        <template v-else>

          <el-tabs v-if="metricResults.length > 1" v-model="activeMetricTab" type="card" class="metric-result-tabs" @tab-change="handleTrendMetricChange">

            <el-tab-pane

              v-for="result in metricResults"

              :key="result.metricId"

              :label="result.metricName"

              :name="String(result.metricId)"

            />

          </el-tabs>

          <el-card class="chart-card" shadow="never">

            <div class="chart-config">

              <el-form :inline="true" size="small">

                <el-form-item label="图表类型">

                  <el-select v-model="activeChartConfig.chartType" style="width: 120px" @change="drawTrendChart">

                    <el-option v-for="t in QUERY_CHART_TYPES" :key="t.value" :label="t.label" :value="t.value" />

                  </el-select>

                </el-form-item>

                <el-form-item v-if="activeChartConfig.chartType !== 'pie'" label="X轴字段">

                  <el-select v-model="activeChartConfig.xField" style="width: 180px" @change="drawTrendChart">

                    <el-option v-for="c in activeChartColumns" :key="c.prop" :label="c.label" :value="c.prop" />

                  </el-select>

                </el-form-item>

                <el-form-item v-if="activeChartConfig.chartType === 'pie'" label="分类字段">

                  <el-select v-model="activeChartConfig.xField" style="width: 180px" @change="drawTrendChart">

                    <el-option v-for="c in activeChartColumns" :key="c.prop" :label="c.label" :value="c.prop" />

                  </el-select>

                </el-form-item>

                <el-form-item :label="activeChartConfig.chartType === 'pie' ? '数值字段' : 'Y轴字段'">

                  <el-select v-model="activeChartConfig.yField" style="width: 180px" :disabled="!activeNumericColumns.length" @change="drawTrendChart">

                    <el-option v-for="c in activeNumericColumns" :key="c.prop" :label="c.label" :value="c.prop" />

                  </el-select>

                </el-form-item>

                <el-form-item v-if="activeChartConfig.chartType !== 'pie'" label="系列字段">

                  <el-select v-model="activeChartConfig.seriesField" clearable style="width: 180px" @change="drawTrendChart">

                    <el-option v-for="c in activeChartColumns" :key="c.prop" :label="c.label" :value="c.prop" />

                  </el-select>

                </el-form-item>

              </el-form>

            </div>

            <el-alert

              v-if="activeMetricResult && !activeNumericColumns.length"

              type="warning"

              :closable="false"

              show-icon

              title="当前指标结果中未检测到数值列，无法绘制 Y 轴/数值字段，请切换指标或调整 SQL 分组字段"

              class="chart-hint"

            />

            <el-empty v-if="activeMetricResult && !activeMetricResult.rows.length" description="当前指标暂无数据" />

            <div v-else-if="activeNumericColumns.length" ref="trendChartRef" class="chart-area" v-loading="loading" />

          </el-card>

        </template>

      </el-tab-pane>

    </el-tabs>

  </div>

</template>



<script setup lang="ts">

import { ref, reactive, onMounted, onBeforeUnmount, nextTick, computed, watch } from 'vue'

import type { FormInstance } from 'element-plus'

import { ElMessage } from 'element-plus'

import { Download } from '@element-plus/icons-vue'

import * as echarts from 'echarts'

import { getMetricList, previewMetric } from '@/api/metric'

import MetricConditionInput from '@/components/MetricConditionInput.vue'

import { ensureMetricSchemasLoaded } from '@/composables/useMetricSchemas'

import {

  unpackMetricBuilderParams,

  extractMetricParameters,

  resolveParamKey,

  buildRuntimeMetricSql,

  isMetricBindParamActive,

  resolveFieldLabel,

  resolveMetricResultColumnLabel,

  QUERY_CHART_TYPES,

  type MetricFilterCondition,

  type MetricBuilderConfig,

  type QueryChartConfig,

} from '@/constants/metricSchema'

import { exportToExcel, unwrapApiData, pickListPage } from '@/utils'

import {

  defaultMetricChartConfig,

  buildMetricChartOption,

  detectNumericColumnProps,

} from '@/utils/metricChart'



interface MetricOption {

  id: number

  metricName: string

  metricCode: string

  metricType: string

  metricFormula: string

  paramsJson?: string

  unit?: string

}



interface MetricParamField {

  key: string

  label: string

  queryConditionType: MetricFilterCondition['queryConditionType']

  dictType?: string

}



interface MetricResultColumn {

  prop: string

  label: string

}



interface MetricAnalysisResult {

  metricId: number

  metricName: string

  metricType: string

  unit: string

  rows: Record<string, unknown>[]

  columns: MetricResultColumn[]

  builderConfig: MetricBuilderConfig | null

  status: 'qualified' | 'unqualified'

  trendPoints: number[]

  currentValue: number | string

}



const queryFormRef = ref<FormInstance>()

const loading = ref(false)

const exportLoading = ref(false)

const activeViewTab = ref<'trend' | 'detail'>('detail')

const activeMetricTab = ref('')

const metricOptions = ref<MetricOption[]>([])



const queryForm = reactive({

  metricIds: [] as number[],

  bindParams: {} as Record<string, string>,

})



function resolveParamLabel(metric: MetricOption, cond: MetricFilterCondition): string {

  const builder = unpackMetricBuilderParams(metric.paramsJson)

  if (builder?.dataSource) {

    return resolveFieldLabel(cond.field, builder.dataSource, builder.joinTables)

  }

  return cond.field

}



const metricParams = computed<MetricParamField[]>(() => {

  const selected = metricOptions.value.filter((m) => queryForm.metricIds.includes(m.id))

  const seen = new Set<string>()

  const fields: MetricParamField[] = []

  for (const metric of selected) {

    const builder = unpackMetricBuilderParams(metric.paramsJson)

    for (const cond of extractMetricParameters(builder)) {

      const pk = resolveParamKey(cond)

      const label = resolveParamLabel(metric, cond)

      if (cond.queryConditionType === 'DATE_RANGE') {

        if (!seen.has(pk)) {

          seen.add(pk)

          fields.push({

            key: pk,

            label,

            queryConditionType: 'DATE_RANGE',

          })

        }

        continue

      }

      if (seen.has(pk)) continue

      seen.add(pk)

      fields.push({

        key: pk,

        label,

        queryConditionType: cond.queryConditionType,

        dictType: cond.dictType,

      })

    }

  }

  return fields

})



watch(metricParams, (params) => {

  const next: Record<string, string> = {}

  for (const p of params) {

    next[p.key] = queryForm.bindParams[p.key] ?? ''

  }

  queryForm.bindParams = next

})



const stats = reactive({

  totalMetrics: 0,

  qualifiedMetrics: 0,

  unqualifiedMetrics: 0,

  qualificationRate: 0,

})



const metricResults = ref<MetricAnalysisResult[]>([])

const trendChartRef = ref<HTMLElement>()

const chartConfigs = ref<Record<number, QueryChartConfig>>({})



const extractScalar = (rows: Record<string, unknown>[]) => {

  if (!rows?.length) return 0

  const first = rows[0]

  if ('metric_value' in first) {

    const val = first.metric_value

    if (typeof val === 'number') return val

    if (val != null && !Number.isNaN(Number(val))) return Number(val)

  }

  for (const val of Object.values(first)) {

    if (typeof val === 'number') return val

    if (val != null && !Number.isNaN(Number(val))) return Number(val)

  }

  return rows.length

}



function buildResultColumns(

  rows: Record<string, unknown>[],

  builderConfig: MetricBuilderConfig | null,

): MetricResultColumn[] {

  if (!rows.length) return []

  return Object.keys(rows[0]).map((prop) => ({

    prop,

    label: resolveMetricResultColumnLabel(prop, builderConfig),

  }))

}



const loadMetricOptions = async () => {

  try {

    await ensureMetricSchemasLoaded()

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

    const results: MetricAnalysisResult[] = []

    for (const m of selected) {

      const builderConfig = unpackMetricBuilderParams(m.paramsJson)

      if (!m.metricFormula) {

        results.push({

          metricId: m.id,

          metricName: m.metricName,

          metricType: m.metricType,

          unit: m.unit || '',

          rows: [],

          columns: [],

          builderConfig,

          status: 'unqualified',

          trendPoints: [],

          currentValue: '-',

        })

        continue

      }

      const previewRes = await previewMetric({

        metricFormula: buildRuntimeMetricSql(m.metricFormula, m.paramsJson, queryForm.bindParams),

        bindParams: buildBindParamsForMetric(m),

      })

      const data = unwrapApiData(previewRes)

      const rows = (data?.rows ?? []) as Record<string, unknown>[]

      const scalar = extractScalar(rows)

      results.push({

        metricId: m.id,

        metricName: m.metricName,

        metricType: m.metricType,

        unit: m.unit || '',

        rows,

        columns: buildResultColumns(rows, builderConfig),

        builderConfig,

        status: rows.length > 0 ? 'qualified' : 'unqualified',

        trendPoints: rows.slice(0, 7).map((row) => extractScalar([row])),

        currentValue: scalar,

      })

    }

    metricResults.value = results

    chartConfigs.value = {}

    activeMetricTab.value = results.length ? String(results[0].metricId) : ''

    stats.totalMetrics = results.length

    stats.qualifiedMetrics = results.filter(r => r.status === 'qualified').length

    stats.unqualifiedMetrics = results.length - stats.qualifiedMetrics

    stats.qualificationRate = results.length ? Math.round((stats.qualifiedMetrics / results.length) * 100) : 0

    await nextTick()

    if (activeViewTab.value === 'trend') {

      drawTrendChart()

    }

    ElMessage.success('分析完成')

  } catch (e) {

    console.error(e)

    ElMessage.error('指标运行失败')

  } finally {

    loading.value = false

  }

}



const buildBindParamsForMetric = (metric: MetricOption): Record<string, string> => {

  const builder = unpackMetricBuilderParams(metric.paramsJson)

  const params: Record<string, string> = {}

  for (const cond of extractMetricParameters(builder)) {

    const pk = resolveParamKey(cond)

    if (cond.queryConditionType === 'DATE_RANGE') {

      const raw = queryForm.bindParams[pk]

      if (raw) {

        const [start, end] = raw.split(',')

        if (isMetricBindParamActive(start)) params[`${pk}_start`] = start!.trim()

        if (isMetricBindParamActive(end)) params[`${pk}_end`] = end!.trim()

      }

    } else {

      const val = queryForm.bindParams[pk]

      if (isMetricBindParamActive(val)) params[pk] = String(val).trim()

    }

  }

  return params

}



const handleReset = () => {

  queryForm.metricIds = []

  queryForm.bindParams = {}

  metricResults.value = []

  chartConfigs.value = {}

  activeMetricTab.value = ''

  disposeTrendChart()

  stats.totalMetrics = 0

  stats.qualifiedMetrics = 0

  stats.unqualifiedMetrics = 0

  stats.qualificationRate = 0

}



const activeMetricResult = computed(() =>

  metricResults.value.find(r => String(r.metricId) === activeMetricTab.value) ?? metricResults.value[0] ?? null,

)



const activeChartColumns = computed(() => activeMetricResult.value?.columns ?? [])



const activeNumericColumns = computed(() => {

  const result = activeMetricResult.value

  if (!result?.rows.length) return []

  const numericProps = detectNumericColumnProps(result.rows, result.columns.map(c => c.prop))

  return result.columns.filter(c => numericProps.includes(c.prop))

})



function ensureChartConfig(result: MetricAnalysisResult): QueryChartConfig {

  const existing = chartConfigs.value[result.metricId]

  if (existing) return existing

  const config = defaultMetricChartConfig(result.rows, result.columns.map(c => c.prop))

  chartConfigs.value = { ...chartConfigs.value, [result.metricId]: config }

  return config

}



const activeChartConfig = ref<QueryChartConfig>(defaultMetricChartConfig([], []))



watch(activeMetricResult, (result) => {

  if (!result) {

    activeChartConfig.value = defaultMetricChartConfig([], [])

    return

  }

  activeChartConfig.value = { ...ensureChartConfig(result) }

}, { immediate: true })



watch(activeChartConfig, (config) => {

  const result = activeMetricResult.value

  if (!result) return

  chartConfigs.value = { ...chartConfigs.value, [result.metricId]: { ...config } }

}, { deep: true })



const handleExport = () => {

  if (!metricResults.value.length) {

    ElMessage.warning('请先运行分析后再导出')

    return

  }

  if (activeViewTab.value === 'detail') {

    const result = activeMetricResult.value

    if (!result?.rows.length) {

      ElMessage.warning('当前指标暂无明细数据可导出')

      return

    }

    exportToExcel(

      result.rows,

      result.columns.map(c => ({ key: c.prop, label: c.label })),

      `${result.metricName}-指标明细`,

    )

    return

  }

  const maxPoints = Math.max(...metricResults.value.map(r => r.trendPoints.length), 1)

  const columns = [

    { key: 'metricName', label: '指标名称' },

    ...Array.from({ length: maxPoints }, (_, i) => ({ key: `p${i + 1}`, label: `点${i + 1}` })),

  ]

  const exportData = metricResults.value.map(row => {

    const item: Record<string, string | number> = { metricName: row.metricName }

    for (let i = 0; i < maxPoints; i += 1) {

      item[`p${i + 1}`] = row.trendPoints[i] ?? (i === 0 ? row.currentValue : '')

    }

    return item

  })

  exportToExcel(exportData, columns, '指标分析趋势')

}



let metricTrendChart: echarts.ECharts | null = null

const disposeTrendChart = () => {

  if (metricTrendChart) {

    metricTrendChart.dispose()

    metricTrendChart = null

  }

}

const drawTrendChart = () => {

  const result = activeMetricResult.value

  if (!trendChartRef.value || !result?.rows.length) {

    disposeTrendChart()

    return

  }

  const el = trendChartRef.value

  if (el.getBoundingClientRect().width === 0) {

    setTimeout(() => drawTrendChart(), 100)

    return

  }

  if (!activeNumericColumns.value.length) {

    disposeTrendChart()

    return

  }

  const config = activeChartConfig.value

  const yLabel = result.columns.find(c => c.prop === config.yField)?.label ?? config.yField

  const option = buildMetricChartOption(result.rows, config, { seriesLabel: yLabel })

  if (!option) {

    disposeTrendChart()

    return

  }

  if (!metricTrendChart) {

    metricTrendChart = echarts.init(el)

  } else {

    metricTrendChart.resize()

  }

  metricTrendChart.setOption(option, true)

}

const handleTrendMetricChange = () => {

  nextTick(() => drawTrendChart())

}

const handleViewTabChange = (tabName: string | number) => {

  if (String(tabName) === 'trend') {

    nextTick(() => drawTrendChart())

  } else {

    disposeTrendChart()

  }

}

onMounted(() => loadMetricOptions())

onBeforeUnmount(() => disposeTrendChart())

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

  .metric-result-tabs {

    :deep(.el-tabs__header) { margin-bottom: 12px; }

  }

  .result-header {

    display: flex;

    justify-content: space-between;

    align-items: center;

  }

  .row-count { color: #909399; font-size: 13px; }

  .chart-card, .table-card {

    margin-bottom: 16px;

    .card-header { display: flex; justify-content: space-between; align-items: center; }

  }

  .chart-config {

    margin-bottom: 12px;

    padding-bottom: 8px;

    border-bottom: 1px dashed var(--el-border-color-lighter);

  }

  .chart-hint { margin-bottom: 12px; }

  .chart-area { height: 400px; }

}

</style>

