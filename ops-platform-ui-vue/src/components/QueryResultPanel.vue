<template>
  <div class="query-result-panel">
    <el-card
      shadow="never"
      class="conditions-card"
      :class="{ 'is-collapsed': collapsibleConditions && !conditionsExpanded }"
    >
      <template #header>
        <div class="panel-header">
          <div
            class="panel-title"
            :class="{ 'collapsible-title': collapsibleConditions }"
            @click.stop="toggleConditions"
          >
            <el-icon
              v-if="collapsibleConditions"
              class="collapse-icon"
              :class="{ 'is-collapsed': !conditionsExpanded }"
            >
              <ArrowDown />
            </el-icon>
            <span>查询条件</span>
          </div>
          <el-button v-if="showExport" size="small" @click.stop="handleExport">导出</el-button>
        </div>
      </template>
      <el-descriptions v-show="!collapsibleConditions || conditionsExpanded" :column="2" border size="small">
        <el-descriptions-item label="数据源">{{ summary.dataSourceLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="展示字段">{{ summary.displayFields || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计算方式">{{ summary.calcMethod || '无（明细查询）' }}</el-descriptions-item>
        <el-descriptions-item label="汇总字段">{{ summary.groupByFields || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联表">{{ summary.joinTables || '-' }}</el-descriptions-item>
        <el-descriptions-item label="查询条件">{{ summary.conditions || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="data-card">
      <template #header>
        <div class="panel-header">
          <span>查询结果</span>
          <span class="row-count">共 {{ rows.length }} 行</span>
        </div>
      </template>

      <el-empty v-if="rows.length === 0" description="暂无数据" />

      <el-tabs v-else v-model="resultTab" class="result-tabs" @tab-change="onTabChange">
        <el-tab-pane label="结果列表" name="list">
          <el-table :data="rows" stripe max-height="420">
            <el-table-column
              v-for="col in tableColumns"
              :key="col.prop"
              :prop="col.prop"
              :label="col.label"
              min-width="120"
              show-overflow-tooltip
            />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="图表展示" name="chart">
          <div class="chart-config">
            <el-form :inline="true" size="small">
              <el-form-item label="图表类型">
                <el-select v-model="chartConfig.chartType" style="width: 120px" @change="drawChart">
                  <el-option v-for="t in QUERY_CHART_TYPES" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="chartConfig.chartType !== 'pie'" label="X轴字段">
                <el-select v-model="chartConfig.xField" style="width: 160px" @change="drawChart">
                  <el-option v-for="c in tableColumns" :key="c.prop" :label="c.label" :value="c.prop" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="chartConfig.chartType === 'pie'" label="分类字段">
                <el-select v-model="chartConfig.xField" style="width: 160px" @change="drawChart">
                  <el-option v-for="c in tableColumns" :key="c.prop" :label="c.label" :value="c.prop" />
                </el-select>
              </el-form-item>
              <el-form-item :label="chartConfig.chartType === 'pie' ? '数值字段' : 'Y轴字段'">
                <el-select v-model="chartConfig.yField" style="width: 160px" @change="drawChart">
                  <el-option v-for="c in numericColumns" :key="c.prop" :label="c.label" :value="c.prop" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="chartConfig.chartType !== 'pie'" label="系列字段">
                <el-select v-model="chartConfig.seriesField" clearable style="width: 160px" @change="drawChart">
                  <el-option v-for="c in tableColumns" :key="c.prop" :label="c.label" :value="c.prop" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
          <div ref="chartRef" class="chart-area" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  METRIC_CALC_METHODS,
  METRIC_FILTER_OPERATORS,
  METRIC_TABLE_SCHEMAS,
  QUERY_CHART_TYPES,
  defaultQueryChartConfig,
  getAvailableFields,
  type QueryBuilderConfig,
  type QueryChartConfig,
} from '@/constants/metricSchema'
import { exportToExcel } from '@/utils'

const props = withDefaults(defineProps<{
  rows: Record<string, unknown>[]
  builderConfig?: QueryBuilderConfig | null
  sqlText?: string
  showExport?: boolean
  collapsibleConditions?: boolean
}>(), {
  builderConfig: null,
  sqlText: '',
  showExport: true,
  collapsibleConditions: false,
})

const conditionsExpanded = ref(true)

function toggleConditions() {
  if (!props.collapsibleConditions) return
  conditionsExpanded.value = !conditionsExpanded.value
}

const chartRef = ref<HTMLElement>()
const resultTab = ref<'list' | 'chart'>('list')
let chartInstance: echarts.ECharts | null = null

const columns = computed(() =>
  props.rows.length > 0 ? Object.keys(props.rows[0]) : [],
)

function resolveColumnLabel(col: string, cfg: QueryBuilderConfig | null | undefined): string {
  if (col === 'metric_value') {
    if (cfg?.calcMethod) {
      return METRIC_CALC_METHODS.find((m) => m.value === cfg.calcMethod)?.label ?? '指标值'
    }
    return '指标值'
  }
  if (!cfg?.dataSource) return col

  const schema = METRIC_TABLE_SCHEMAS[cfg.dataSource]
  const mainField = schema?.fields.find((f) => f.name === col)
  if (mainField) return mainField.label

  const available = getAvailableFields(cfg.dataSource, cfg.joinTables)
  const exact = available.find((f) => f.name === col)
  if (exact) return exact.label

  const suffix = col.includes('.') ? col.split('.').pop()! : col
  const bySuffix = available.find((f) => f.name === suffix || f.name.endsWith(`.${suffix}`))
  if (bySuffix) return bySuffix.label

  const fromDisplay = cfg.displayFields.find((f) => f === col || f.endsWith(`.${suffix}`) || f === suffix)
  if (fromDisplay) {
    const match = available.find((f) => f.name === fromDisplay || f.name.endsWith(`.${fromDisplay}`))
    if (match) return match.label
    const displaySuffix = fromDisplay.includes('.') ? fromDisplay.split('.').pop()! : fromDisplay
    const byDisplaySuffix = schema?.fields.find((f) => f.name === displaySuffix)
    if (byDisplaySuffix) return byDisplaySuffix.label
  }

  return col
}

const tableColumns = computed(() => {
  const cfg = props.builderConfig
  return columns.value.map((prop) => ({ prop, label: resolveColumnLabel(prop, cfg) }))
})

const numericColumns = computed(() => {
  if (props.rows.length === 0) return tableColumns.value
  const first = props.rows[0]
  return tableColumns.value.filter((c) => typeof first[c.prop] === 'number' || !Number.isNaN(Number(first[c.prop])))
})

const chartConfig = ref<QueryChartConfig>(defaultQueryChartConfig([]))

const summary = computed(() => {
  const cfg = props.builderConfig
  if (!cfg?.dataSource) {
    return {
      dataSourceLabel: props.sqlText ? '自定义 SQL' : '-',
      displayFields: '-',
      calcMethod: '-',
      groupByFields: '-',
      joinTables: '-',
      conditions: props.sqlText || '-',
    }
  }
  const schema = METRIC_TABLE_SCHEMAS[cfg.dataSource]
  const fieldLabel = (name: string) => schema?.fields.find((f) => f.name === name)?.label ?? name
  const calcLabel = cfg.calcMethod
    ? METRIC_CALC_METHODS.find((m) => m.value === cfg.calcMethod)?.label ?? cfg.calcMethod
    : ''
  const condText = cfg.conditions
    .filter((c) => c.field)
    .map((c) => {
      const op = METRIC_FILTER_OPERATORS.find((o) => o.value === c.operator)?.label ?? c.operator
      if (c.operator === 'IS NULL' || c.operator === 'IS NOT NULL') return `${fieldLabel(c.field)} ${op}`
      return `${fieldLabel(c.field)} ${op} ${c.value}`
    })
    .join('；')

  return {
    dataSourceLabel: schema?.label ?? cfg.dataSource,
    displayFields: cfg.displayFields.map(fieldLabel).join('、') || '-',
    calcMethod: calcLabel,
    groupByFields: cfg.groupByFields.map(fieldLabel).join('、') || '-',
    joinTables: cfg.joinTables.join('、') || '-',
    conditions: condText || '无',
  }
})

watch(
  () => props.rows,
  async (rows) => {
    const cols = rows.length > 0 ? Object.keys(rows[0]) : []
    chartConfig.value = defaultQueryChartConfig(cols)
    resultTab.value = 'list'
    await nextTick()
    if (resultTab.value === 'chart') drawChart()
  },
  { deep: true },
)

function onTabChange(name: string | number) {
  if (name === 'chart') {
    nextTick(() => drawChart())
  } else {
    disposeChart()
  }
}

function disposeChart() {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

function drawChart() {
  disposeChart()
  if (!chartRef.value || props.rows.length === 0) return

  chartInstance = echarts.init(chartRef.value)
  const { chartType, xField, yField, seriesField } = chartConfig.value
  const xData = props.rows.map((r) => String(r[xField] ?? ''))

  if (chartType === 'pie') {
    chartInstance.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '55%',
        data: props.rows.map((r) => ({
          name: String(r[xField] ?? ''),
          value: Number(r[yField] ?? 0),
        })),
      }],
    })
    return
  }

  if (seriesField) {
    const seriesNames = [...new Set(props.rows.map((r) => String(r[seriesField] ?? '默认')))]
    const series = seriesNames.map((name) => ({
      name,
      type: chartType,
      data: props.rows
        .filter((r) => String(r[seriesField] ?? '默认') === name)
        .map((r) => Number(r[yField] ?? 0)),
    }))
    const categories = [...new Set(props.rows.map((r) => String(r[xField] ?? '')))]
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0 },
      xAxis: { type: 'category', data: categories },
      yAxis: { type: 'value' },
      series,
    })
    return
  }

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: xData },
    yAxis: { type: 'value' },
    series: [{
      type: chartType,
      data: props.rows.map((r) => Number(r[yField] ?? 0)),
    }],
  })
}

function handleExport() {
  if (!props.rows.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const cols = tableColumns.value.map((c) => ({ key: c.prop, label: c.label }))
  exportToExcel(props.rows, cols, '自定义查询结果')
}

onBeforeUnmount(disposeChart)
</script>

<style scoped>
.query-result-panel { display: flex; flex-direction: column; gap: 12px; }
.conditions-card.is-collapsed :deep(.el-card__body) { display: none; }
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.panel-title { display: flex; align-items: center; }
.collapsible-title { cursor: pointer; user-select: none; }
.collapse-icon {
  margin-right: 6px;
  transition: transform 0.2s ease;
}
.collapse-icon.is-collapsed { transform: rotate(-90deg); }
.row-count { color: #909399; font-size: 13px; }
.result-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.chart-config {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.chart-area { height: 360px; }
</style>
