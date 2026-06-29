<template>
  <div class="funnel-analysis">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="预设漏斗" name="preset" />
        <el-tab-pane label="自定义漏斗" name="custom" />
      </el-tabs>
    </el-card>

    <!-- 预设漏斗 -->
    <div v-if="activeTab === 'preset'">
      <div class="funnel-search-card">
        <el-form :model="queryForm" label-width="72px" @submit.prevent="loadFunnelData">
          <el-row :gutter="16" class="search-row" align="middle">
            <el-col :xs="24" :sm="12" :lg="7" class="funnel-select-col">
              <el-form-item label="选择漏斗">
                <el-select
                  v-model="queryForm.funnelId"
                  placeholder="请选择"
                  :loading="loadingFunnels"
                  clearable
                  @change="loadFunnelData"
                >
                  <el-option v-for="f in funnelOptions" :key="f.id" :label="f.funnelName" :value="f.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="9">
              <el-form-item label="时间范围">
                <el-date-picker
                  v-model="queryForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="YYYY-MM-DD"
                  clearable
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="平台">
                <DictSelect v-model="queryForm.platformType" dict-type="dict_platform_type" placeholder="全部" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="24" :lg="6" class="search-actions-col">
              <div class="search-actions">
                <el-button type="primary" native-type="submit">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
                <el-button type="success" @click="handleExport">导出报告</el-button>
              </div>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <el-alert
        v-if="selectedPresetFunnel?.funnelType === 'PRIVATE_DOMAIN'"
        type="info"
        :closable="false"
        show-icon
        class="private-domain-hint"
        title="私域转化漏斗基于奥创好友与已通过的身份桥接（oa_private_domain_conversion_bridge）统计，与平台筛选无关。"
      />

      <el-card class="chart-card" shadow="never">
        <template #header><div class="card-header"><span>漏斗转化图</span></div></template>
        <div v-if="funnelMetrics.length" class="funnel-summary">
          <div class="summary-item">
            <span class="summary-label">首步总量</span>
            <span class="summary-value">{{ formatFullCount(funnelSummary.firstCount) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">末步留存</span>
            <span class="summary-value">{{ formatFullCount(funnelSummary.lastCount) }}</span>
          </div>
          <div class="summary-item highlight">
            <span class="summary-label">总转化率</span>
            <span class="summary-value">{{ funnelSummary.overallRate }}%</span>
          </div>
          <div v-if="funnelSummary.maxDropStep" class="summary-item warn">
            <span class="summary-label">最大流失环节</span>
            <span class="summary-value">{{ funnelSummary.maxDropStep.name }} · -{{ funnelSummary.maxDropStep.dropOffRate }}%</span>
          </div>
        </div>
        <div ref="funnelChartRef" class="funnel-chart" v-loading="loadingData" />
      </el-card>

      <el-card class="table-card" shadow="never">
        <template #header><div class="card-header"><span>转化率明细</span></div></template>
        <el-table :data="funnelMetrics" border stripe>
          <el-table-column prop="stepOrder" label="顺序" width="80" align="center" />
          <el-table-column prop="name" label="步骤" min-width="160" />
          <el-table-column prop="count" label="数量" width="120" align="right">
            <template #default="{ row }">{{ formatFullCount(row.count) }}</template>
          </el-table-column>
          <el-table-column label="较上步(%)" width="120" align="right">
            <template #default="{ row }">
              <span v-if="row.stepRate == null">-</span>
              <span v-else :class="{ 'rate-low': row.stepRate < 50 }">{{ row.stepRate }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="流失数" width="120" align="right">
            <template #default="{ row }">
              <span v-if="row.dropOff == null">-</span>
              <span v-else class="drop-off">-{{ formatFullCount(row.dropOff) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="conversionRate" label="总转化(%)" width="120" align="right">
            <template #default="{ row }">{{ row.conversionRate ?? '-' }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 自定义漏斗 -->
    <div v-else>
      <el-card class="custom-funnel-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>我的自定义漏斗</span>
            <el-button type="primary" @click="openCreateDialog">新建自定义漏斗</el-button>
          </div>
        </template>
        <el-table :data="customFunnelList" border stripe v-loading="loadingFunnels">
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="funnelName" label="漏斗名称" min-width="200" />
          <el-table-column prop="funnelType" label="类型" width="120" align="center">
            <template #default="{ row }">{{ getFunnelTypeLabel(row.funnelType) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleExecute(row)">查看数据</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 新建对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建自定义漏斗" width="700px">
      <el-form :model="funnelForm" label-width="100px">
        <el-form-item label="漏斗名称">
          <el-input v-model="funnelForm.funnelName" placeholder="请输入漏斗名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="漏斗类型">
          <DictSelect v-model="funnelForm.funnelType" dict-type="dict_funnel_type" placeholder="请选择" style="width: 200px" />
        </el-form-item>
        <el-form-item label="步骤列表">
          <div class="steps-list">
            <div v-for="(step, index) in funnelForm.steps" :key="index" class="step-item">
              <span class="step-index">{{ index + 1 }}.</span>
              <el-input v-model="step.stepName" placeholder="步骤名称" style="width: 180px" />
              <el-select v-model="step.eventCode" placeholder="选择指标" filterable style="width: 280px; margin-left: 8px">
                <el-option v-for="m in metricOptions" :key="m.metricCode" :label="`${m.metricName} (${m.metricCode})`" :value="m.metricCode" />
              </el-select>
              <el-button type="danger" link @click="removeStep(index)">删除</el-button>
            </div>
            <el-button @click="addStep" style="margin-top: 8px">+ 添加步骤</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getFunnelList, getFunnelData, createFunnel } from '@/api/funnel'
import { getMetricList } from '@/api/metric'
import DictSelect from '@/components/DictSelect.vue'
import { exportToExcel, unwrapApiData, pickListPage } from '@/utils'

interface FunnelVO {
  id: number
  funnelName: string
  funnelType: string
  status: number
}

interface FunnelStepVO {
  stepOrder: number
  name: string
  count: number
  conversionRate: number | null
}

interface FunnelStepMetrics extends FunnelStepVO {
  stepRate: number | null
  dropOff: number | null
  dropOffRate: number | null
}

const FUNNEL_COLORS = ['#1890ff', '#13c2c2', '#52c41a', '#faad14', '#fa8c16', '#f5222d']
const FUNNEL_COLORS_DARK = ['#096dd9', '#08979c', '#389e0d', '#d48806', '#d46b08', '#cf1322']

const activeTab = ref('preset')
const showCreateDialog = ref(false)
const loadingFunnels = ref(false)
const loadingData = ref(false)
const saving = ref(false)

const funnelList = ref<FunnelVO[]>([])
const funnelSteps = ref<FunnelStepVO[]>([])
const customFunnelList = ref<FunnelVO[]>([])
const metricOptions = ref<{ metricName: string; metricCode: string }[]>([])

const queryForm = reactive({
  funnelId: null as number | null,
  dateRange: [] as string[],
  platformType: '' as string,
})

const funnelOptions = computed(() =>
  funnelList.value.filter((f) => f.funnelType === 'PRIVATE_DOMAIN' || f.funnelType === 'CONVERSION')
)

const selectedPresetFunnel = computed(() =>
  funnelList.value.find((f) => f.id === queryForm.funnelId) ?? null
)

const defaultFunnelSteps = () => [
  { stepName: '阅读总量', eventCode: 'CONTENT_READ_TOTAL', stepOrder: 1 },
  { stepName: '互动总量', eventCode: 'CONTENT_INTERACTION_TOTAL', stepOrder: 2 },
]

const funnelForm = reactive({
  funnelName: '',
  funnelType: 'CUSTOM',
  steps: defaultFunnelSteps(),
})

const funnelChartRef = ref<HTMLElement>()

const formatCount = (count: number) => {
  const v = typeof count === 'number' && !isNaN(count) ? count : 0
  if (v >= 10000) return (v / 10000).toFixed(1) + 'W'
  if (v >= 1000) return (v / 1000).toFixed(1) + 'K'
  return v.toString()
}

const formatFullCount = (count: number | null | undefined) => {
  const v = typeof count === 'number' && !isNaN(count) ? count : 0
  return v.toLocaleString('zh-CN')
}

const enrichFunnelSteps = (steps: FunnelStepVO[]): FunnelStepMetrics[] =>
  steps.map((step, index) => {
    const count = step.count ?? 0
    const prevCount = index > 0 ? (steps[index - 1].count ?? 0) : 0
    return {
      ...step,
      stepRate: index === 0 ? null : prevCount > 0 ? Math.round((count * 10000) / prevCount) / 100 : 0,
      dropOff: index === 0 ? null : Math.max(0, prevCount - count),
      dropOffRate:
        index === 0 ? null : prevCount > 0 ? Math.round(((prevCount - count) * 10000) / prevCount) / 100 : 0,
    }
  })

const funnelMetrics = computed(() => enrichFunnelSteps(funnelSteps.value))

const funnelSummary = computed(() => {
  const metrics = funnelMetrics.value
  if (metrics.length === 0) {
    return { firstCount: 0, lastCount: 0, overallRate: 0, maxDropStep: null as FunnelStepMetrics | null }
  }
  const first = metrics[0]
  const last = metrics[metrics.length - 1]
  const firstCount = first.count ?? 0
  const lastCount = last.count ?? 0
  const overallRate = firstCount > 0 ? Math.round((lastCount * 10000) / firstCount) / 100 : 0
  const maxDropStep = metrics
    .filter((m) => m.dropOffRate != null)
    .reduce<FunnelStepMetrics | null>((max, cur) => {
      if (!max || (cur.dropOffRate ?? 0) > (max.dropOffRate ?? 0)) return cur
      return max
    }, null)
  return { firstCount, lastCount, overallRate, maxDropStep }
})

const getFunnelTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    CONTENT: '内容漏斗',
    FOLLOWER: '粉丝漏斗',
    ORDER: '订单漏斗',
    PRIVATE_DOMAIN: '私域转化',
    CONVERSION: '内容转化',
    CUSTOM: '自定义',
  }
  return map[type] || type || '-'
}

const loadFunnelList = async () => {
  loadingFunnels.value = true
  try {
    const res: any = await getFunnelList({ pageNum: 1, pageSize: 50 })
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? []
    funnelList.value = list
    customFunnelList.value = list.filter((f) => f.funnelType === 'CUSTOM')
    if (list.length > 0 && !queryForm.funnelId) {
      const privateDomain = list.find((f) => f.funnelType === 'PRIVATE_DOMAIN')
      queryForm.funnelId = privateDomain?.id ?? funnelOptions.value[0]?.id ?? list[0].id
      await loadFunnelData()
    }
  } catch (e) {
    console.error('loadFunnelList failed', e)
  } finally {
    loadingFunnels.value = false
  }
}

const loadMetricOptions = async () => {
  try {
    const res = await getMetricList({ pageNum: 1, pageSize: 200, status: 1 })
    const page = pickListPage(unwrapApiData(res))
    metricOptions.value = page.list
      .filter((m: { status?: number; metricFormula?: string; metricCode?: string; metricName?: string }) =>
        m.status === 1 && m.metricCode && m.metricName && !!m.metricFormula)
      .map((m: { metricName: string; metricCode: string }) => ({
        metricName: m.metricName,
        metricCode: m.metricCode,
      }))
  } catch (e) {
    console.error('load metrics failed', e)
  }
}

const buildDataQuery = () => {
  const q: Record<string, unknown> = {}
  if (queryForm.dateRange?.length === 2) {
    q.startDate = queryForm.dateRange[0]
    q.endDate = queryForm.dateRange[1]
  }
  if (queryForm.platformType) q.platformType = queryForm.platformType
  return q
}

const loadFunnelData = async () => {
  if (!queryForm.funnelId) return
  loadingData.value = true
  try {
    const res: any = await getFunnelData(queryForm.funnelId, buildDataQuery())
    const data = res?.data ?? res
    funnelSteps.value = data?.steps ?? []
    await nextTick()
    initFunnelChart()
  } catch (e) {
    console.error('loadFunnelData failed', e)
    funnelSteps.value = []
  } finally {
    loadingData.value = false
  }
}

const handleReset = () => {
  queryForm.dateRange = []
  queryForm.platformType = ''
  if (queryForm.funnelId) loadFunnelData()
  else funnelSteps.value = []
}

const handleExport = () => {
  if (!funnelSteps.value || funnelSteps.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const columns = [
    { key: 'stepOrder', label: '顺序' },
    { key: 'name', label: '步骤' },
    { key: 'count', label: '数量' },
    { key: 'stepRate', label: '较上步(%)' },
    { key: 'dropOff', label: '流失数' },
    { key: 'conversionRate', label: '总转化(%)' },
  ]
  exportToExcel(
    funnelMetrics.value.map((row) => ({
      ...row,
      stepRate: row.stepRate == null ? '-' : row.stepRate,
      dropOff: row.dropOff == null ? '-' : row.dropOff,
    })),
    columns,
    '漏斗分析报告',
  )
}

const addStep = () => {
  if (funnelForm.steps.length >= 10) {
    ElMessage.warning('最多10个步骤')
    return
  }
  funnelForm.steps.push({ stepName: '', eventCode: '', stepOrder: funnelForm.steps.length + 1 })
}

const removeStep = (index: number) => {
  if (funnelForm.steps.length <= 2) {
    ElMessage.warning('至少需要2个步骤')
    return
  }
  funnelForm.steps.splice(index, 1)
}

const openCreateDialog = () => {
  funnelForm.funnelName = ''
  funnelForm.funnelType = 'CUSTOM'
  funnelForm.steps = defaultFunnelSteps()
  showCreateDialog.value = true
}

const handleSave = async () => {
  if (!funnelForm.funnelName) {
    ElMessage.warning('请输入漏斗名称')
    return
  }
  if (funnelForm.steps.length < 2) {
    ElMessage.warning('至少需要2个步骤')
    return
  }
  if (funnelForm.steps.some(s => !s.stepName || !s.eventCode)) {
    ElMessage.warning('每个步骤必须填写名称和指标')
    return
  }
  saving.value = true
  try {
    const steps = funnelForm.steps.map((s, i) => ({ ...s, stepOrder: i + 1 }))
    await createFunnel({
      funnelName: funnelForm.funnelName,
      funnelType: funnelForm.funnelType,
      steps,
    })
    ElMessage.success('保存成功')
    showCreateDialog.value = false
    await loadFunnelList()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleExecute = async (row: FunnelVO) => {
  activeTab.value = 'preset'
  queryForm.funnelId = row.id
  await loadFunnelData()
}

const handleDelete = async (row: FunnelVO) => {
  try {
    await ElMessageBox.confirm(`确认删除漏斗"${row.funnelName}"？`, '提示', { type: 'warning' })
    ElMessage.success('删除成功（演示）')
  } catch {}
}

let funnelChart: echarts.ECharts | null = null

const toFunnelCount = (count: number | null | undefined) => {
  const n = Number(count)
  return Number.isFinite(n) && n >= 0 ? n : 0
}

/** 各层宽度按自身 count / 首步 count 比例绘制，避免 ECharts 内置 funnel 中间层视觉等宽 */
const buildFunnelChartOption = (metrics: FunnelStepMetrics[]): echarts.EChartsOption => {
  if (metrics.length === 0) {
    return {
      title: {
        text: '暂无漏斗数据',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#909399', fontSize: 14, fontWeight: 400 },
      },
    }
  }

  const counts = metrics.map((step) => toFunnelCount(step.count))
  const maxValue = Math.max(counts[0] ?? 0, 1)
  const isMobile = typeof window !== 'undefined' && window.innerWidth < 768
  const chartTop = isMobile ? 72 : 64
  const chartBottom = 24
  const chartLeftRatio = isMobile ? 0.08 : 0.12
  const chartWidthRatio = isMobile ? 0.84 : 0.76
  const layerGap = 4

  const funnelGradient = (index: number) =>
    new echarts.graphic.LinearGradient(0, 0, 1, 0, [
      { offset: 0, color: FUNNEL_COLORS[index % FUNNEL_COLORS.length] },
      { offset: 1, color: FUNNEL_COLORS_DARK[index % FUNNEL_COLORS_DARK.length] },
    ])

  return {
    title: {
      text: `整体转化 ${funnelSummary.value.overallRate}%`,
      subtext: `首步 ${formatFullCount(funnelSummary.value.firstCount)} → 末步 ${formatFullCount(funnelSummary.value.lastCount)}`,
      left: 'center',
      top: 8,
      textStyle: { fontSize: isMobile ? 14 : 16, fontWeight: 600, color: '#303133' },
      subtextStyle: { fontSize: isMobile ? 11 : 12, color: '#909399' },
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#303133', fontSize: 13 },
      formatter: (params) => {
        const item = params as { dataIndex: number; name: string; value: number }
        const m = metrics[item.dataIndex]
        if (!m) return ''
        const lines = [
          `<strong>${item.name}</strong>`,
          `数量：${formatFullCount(item.value)}`,
          `总转化率：${m.conversionRate ?? '-'}%`,
        ]
        if (item.dataIndex > 0 && m.stepRate != null) {
          lines.push(`较上步：${m.stepRate}%`)
          lines.push(`流失：-${formatFullCount(m.dropOff)} (${m.dropOffRate}%)`)
        }
        return lines.join('<br/>')
      },
    },
    series: [
      {
        name: '漏斗分析',
        type: 'custom',
        coordinateSystem: 'none',
        data: metrics.map((step, index) => ({
          name: step.name,
          value: counts[index],
          index,
        })),
        renderItem: (params, api) => {
          const dataIndex = params.dataIndex ?? 0
          const count = counts[dataIndex] ?? 0
          const chartWidth = api.getWidth()
          const chartHeight = api.getHeight()
          const funnelLeft = chartWidth * chartLeftRatio
          const funnelWidth = chartWidth * chartWidthRatio
          const funnelTop = chartTop
          const funnelHeight = chartHeight - chartTop - chartBottom
          const stepCount = metrics.length
          const layerHeight = funnelHeight / stepCount
          const y = funnelTop + dataIndex * layerHeight + layerGap / 2
          const h = Math.max(layerHeight - layerGap, 8)
          const centerX = funnelLeft + funnelWidth / 2

          const widthRatio = (value: number) => Math.max(value / maxValue, 0.02)
          const topW = funnelWidth * widthRatio(count)
          const nextCount = dataIndex < stepCount - 1 ? counts[dataIndex + 1] ?? 0 : count
          const bottomW = funnelWidth * widthRatio(nextCount)

          const topLeft = centerX - topW / 2
          const topRight = centerX + topW / 2
          const bottomLeft = centerX - bottomW / 2
          const bottomRight = centerX + bottomW / 2

          const colorIndex = dataIndex
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const children: any[] = [
            {
              type: 'polygon',
              shape: {
                points: [
                  [topLeft, y],
                  [topRight, y],
                  [bottomRight, y + h],
                  [bottomLeft, y + h],
                ],
              },
              style: {
                fill: funnelGradient(colorIndex),
                stroke: '#fff',
                lineWidth: 2,
                shadowBlur: 8,
                shadowColor: 'rgba(24, 144, 255, 0.15)',
              },
              emphasis: {
                style: {
                  shadowBlur: 14,
                  shadowColor: 'rgba(24, 144, 255, 0.28)',
                },
              },
            },
          ]

          const showInside = isMobile || topW > funnelWidth * 0.15
          if (showInside) {
            const m = metrics[dataIndex]
            const insideLines = isMobile
              ? dataIndex === 0
                ? `${metrics[dataIndex].name}\n${formatCount(count)}`
                : `${metrics[dataIndex].name}\n${formatCount(count)}\n↓${m?.dropOffRate ?? 0}%`
              : `${metrics[dataIndex].name}\n${formatCount(count)}`
            children.push({
              type: 'text',
              style: {
                text: insideLines,
                x: centerX,
                y: y + h / 2,
                fill: '#fff',
                fontSize: isMobile ? 12 : 13,
                fontWeight: 600,
                align: 'center',
                verticalAlign: 'middle',
                lineHeight: isMobile ? 18 : 20,
              },
            })
          }

          if (!isMobile) {
            const m = metrics[dataIndex]
            const labelX = funnelLeft + funnelWidth + 16
            const labelY = y + h / 2
            const labelLines =
              dataIndex === 0
                ? [`{title|${metrics[dataIndex].name}}`, `{count|${formatFullCount(count)}}`, '{rate|基准 100%}']
                : [
                    `{title|${metrics[dataIndex].name}}`,
                    `{count|${formatFullCount(count)}}`,
                    `{rate|较上步 ${m?.stepRate ?? 0}%}`,
                    `{loss|流失 -${formatFullCount(m?.dropOff ?? 0)} (-${m?.dropOffRate ?? 0}%)}`,
                  ]
            children.push({
              type: 'text',
              style: {
                text: labelLines.join('\n'),
                x: labelX,
                y: labelY,
                fill: '#303133',
                fontSize: 13,
                align: 'left',
                verticalAlign: 'middle',
                rich: {
                  title: { fontSize: 13, fill: '#303133', fontWeight: 600, lineHeight: 20 },
                  count: { fontSize: 14, fill: '#1890ff', fontWeight: 700, lineHeight: 22 },
                  rate: { fontSize: 12, fill: '#52c41a', lineHeight: 18 },
                  loss: { fontSize: 12, fill: '#f5222d', lineHeight: 18 },
                },
              },
            })
            children.push({
              type: 'line',
              shape: {
                x1: topRight,
                y1: labelY,
                x2: labelX - 4,
                y2: labelY,
              },
              style: {
                stroke: '#dcdfe6',
                lineWidth: 1,
                lineDash: [4, 4],
              },
            })
          }

          return {
            type: 'group',
            children,
          } as echarts.CustomSeriesRenderItemReturn
        },
      } as echarts.SeriesOption,
    ],
  }
}

const handleChartResize = () => {
  funnelChart?.resize()
}

const initFunnelChart = () => {
  if (!funnelChartRef.value) return
  const el = funnelChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initFunnelChart, 100)
    return
  }
  if (funnelChart) {
    funnelChart.dispose()
    funnelChart = null
  }
  funnelChart = echarts.init(el)
  funnelChart.setOption(buildFunnelChartOption(funnelMetrics.value), true)
  funnelChart.resize()
}

onMounted(() => {
  loadFunnelList()
  loadMetricOptions()
  window.addEventListener('resize', handleChartResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleChartResize)
  funnelChart?.dispose()
  funnelChart = null
})
</script>

<style scoped lang="scss">
.funnel-analysis {
  .funnel-search-card {
    margin-bottom: 16px;
    background-color: #fff;
    border-radius: 12px;
    padding: 16px 20px 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    overflow-x: hidden;

    .search-row {
      width: 100%;
      flex-wrap: nowrap;
    }

    .funnel-select-col {
      @media (min-width: 1200px) {
        flex: 0 0 auto;
        width: auto;
        min-width: 280px;
        max-width: 320px;
      }
    }

    :deep(.el-form-item) {
      width: 100%;
      margin-bottom: 12px;
      margin-right: 0;
    }

    :deep(.el-form-item__label) {
      font-size: 14px;
      color: #606266;
      padding-right: 8px;
    }

    :deep(.el-form-item__content) {
      flex: 1;
      min-width: 0;
    }

    :deep(.el-input),
    :deep(.el-select),
    :deep(.el-date-editor) {
      width: 100%;
    }

    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      border-radius: 6px;
    }

    .search-actions-col {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      min-width: 0;
      flex-shrink: 0;

      @media (min-width: 1200px) {
        flex: 1 1 auto;
      }
    }

    .search-actions {
      display: flex;
      flex-wrap: nowrap;
      align-items: center;
      justify-content: flex-end;
      gap: 8px;
      width: 100%;
      padding-bottom: 12px;
      white-space: nowrap;

      :deep(.el-button) {
        flex-shrink: 0;
      }
    }

    :deep(.el-button--primary) {
      background-color: #1890ff;
      border-color: #1890ff;
      border-radius: 6px;

      &:hover {
        background-color: #40a9ff;
        border-color: #40a9ff;
      }
    }

    :deep(.el-button:not(.is-text-button)) {
      border-radius: 6px;
      font-weight: 500;
    }
  }

  .compare-card,
  .chart-card,
  .table-card,
  .custom-funnel-card {
    margin-bottom: 16px;
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .funnel-summary {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 12px;
    margin-bottom: 12px;
    padding: 0 4px 8px;
  }

  .summary-item {
    background: linear-gradient(135deg, #f5f9ff 0%, #eef6ff 100%);
    border: 1px solid #d6e8ff;
    border-radius: 10px;
    padding: 12px 14px;

    &.highlight {
      background: linear-gradient(135deg, #e6fffb 0%, #f0fffc 100%);
      border-color: #87e8de;
    }

    &.warn {
      background: linear-gradient(135deg, #fff7e6 0%, #fff1f0 100%);
      border-color: #ffd591;
    }
  }

  .summary-label {
    display: block;
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;
  }

  .summary-value {
    display: block;
    font-size: 18px;
    font-weight: 700;
    color: #303133;
    line-height: 1.3;
    word-break: break-word;
  }

  .funnel-chart {
    height: 480px;
    width: 100%;
    min-height: 360px;

    @media (max-width: 768px) {
      height: 420px;
      min-height: 320px;
    }
  }

  .rate-low {
    color: #f5222d;
    font-weight: 600;
  }

  .drop-off {
    color: #f5222d;
    font-weight: 500;
  }
  .step-item {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    .step-index {
      width: 30px;
      font-weight: 600;
    }
  }
}
</style>
