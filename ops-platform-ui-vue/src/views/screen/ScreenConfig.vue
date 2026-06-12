<template>
  <div class="screen-config-page">
    <div class="config-toolbar">
      <span class="toolbar-title">数据大屏配置</span>
      <div class="toolbar-actions">
        <el-select
          v-model="selectedId"
          placeholder="选择模板"
          style="width: 220px"
          filterable
          @change="onTemplateSelect"
        >
          <el-option
            v-for="d in list"
            :key="d.id"
            :label="d.dashboardName"
            :value="d.id"
          />
          <el-option label="+ 新建模板" :value="NEW_TEMPLATE" />
        </el-select>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        <el-button type="success" :disabled="!formMeta.id" @click="previewScreen">预览大屏</el-button>
      </div>
    </div>

    <div class="config-body">
      <div class="config-left">
        <el-card shadow="never" class="config-card">
          <template #header><span class="section-title">模板信息</span></template>
          <el-form :model="formMeta" label-width="88px" size="default">
            <el-form-item label="模板名称" required>
              <el-input v-model="formMeta.dashboardName" maxlength="50" placeholder="如：内部运营大屏" />
            </el-form-item>
            <el-form-item label="数据范围">
              <el-radio-group v-model="layout.scope" @change="onScopeChange">
                <el-radio value="INTERNAL">内部数据</el-radio>
                <el-radio value="EXTERNAL">外部数据</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="刷新间隔">
              <el-select v-model="layout.refreshSeconds" style="width: 160px">
                <el-option label="30秒" :value="30" />
                <el-option label="1分钟" :value="60" />
                <el-option label="5分钟" :value="300" />
                <el-option label="不刷新" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="formMeta.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">指标卡配置（最多6个）</span>
              <el-button type="primary" size="small" :icon="Plus" @click="addWidget('KPI')">添加</el-button>
            </div>
          </template>
          <el-empty v-if="!kpiWidgets.length" description="暂无指标卡" :image-size="48" />
          <div v-for="w in kpiWidgets" :key="w.id" class="widget-item">
            <div class="widget-header">
              <el-tag size="small" type="primary">指标卡</el-tag>
              <div>
                <el-button link type="primary" size="small" @click="openWidgetEdit(w)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeWidgetById(w.id)">删除</el-button>
              </div>
            </div>
            <div class="widget-desc">标题：{{ w.title }} | 数据源：{{ sourceLabel(w) }}</div>
          </div>
        </el-card>

        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">今日统计配置</span>
              <el-button type="primary" size="small" :icon="Plus" @click="addWidget('STAT')">添加</el-button>
            </div>
          </template>
          <el-empty v-if="!statWidgets.length" description="暂无今日统计" :image-size="48" />
          <div v-for="w in statWidgets" :key="w.id" class="widget-item">
            <div class="widget-header">
              <el-tag size="small" type="info">今日统计</el-tag>
              <div>
                <el-button link type="primary" size="small" @click="openWidgetEdit(w)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeWidgetById(w.id)">删除</el-button>
              </div>
            </div>
            <div class="widget-desc">标题：{{ w.title }} | 数据源：{{ sourceLabel(w) }}</div>
          </div>
        </el-card>

        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">图表组件配置（最多4个）</span>
              <el-button type="primary" size="small" :icon="Plus" @click="addWidget('CHART')">添加</el-button>
            </div>
          </template>
          <el-empty v-if="!chartWidgets.length" description="暂无图表" :image-size="48" />
          <div v-for="w in chartWidgets" :key="w.id" class="widget-item">
            <div class="widget-header">
              <el-tag size="small" type="success">{{ chartTypeLabel(w.chartType) }}</el-tag>
              <div>
                <el-button link type="primary" size="small" @click="openWidgetEdit(w)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeWidgetById(w.id)">删除</el-button>
              </div>
            </div>
            <div class="widget-desc">标题：{{ w.title }} | 数据源：{{ sourceLabel(w) }}</div>
          </div>
        </el-card>

        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">列表组件配置</span>
              <el-button type="primary" size="small" :icon="Plus" @click="addWidget('LIST')">添加</el-button>
            </div>
          </template>
          <el-empty v-if="!listWidgets.length" description="暂无列表" :image-size="48" />
          <div v-for="w in listWidgets" :key="w.id" class="widget-item">
            <div class="widget-header">
              <el-tag size="small" type="warning">排行榜</el-tag>
              <div>
                <el-button link type="primary" size="small" @click="openWidgetEdit(w)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeWidgetById(w.id)">删除</el-button>
              </div>
            </div>
            <div class="widget-desc">标题：{{ w.title }} | 数据源：{{ sourceLabel(w) }}</div>
          </div>
        </el-card>
      </div>

      <div class="config-right">
        <el-card shadow="never" class="config-card preview-card-wrap">
          <template #header>
            <div class="section-header">
              <span class="section-title">实时预览</span>
              <el-button link type="primary" size="small" :loading="previewLoading" @click="loadPreviewData">
                刷新数据
              </el-button>
            </div>
          </template>
          <div class="preview-filters">
            <el-select v-model="previewDateRangeKey" size="small" style="width: 110px" @change="loadPreviewData">
              <el-option label="今日" value="1d" />
              <el-option label="近7天" value="7d" />
              <el-option label="近30天" value="30d" />
            </el-select>
            <IpGroupTreeSelect
              v-model="previewIpGroupId"
              clearable
              placeholder="全部IP组"
              style="width: 150px"
              @change="loadPreviewData"
            />
            <DictSelect
              v-model="previewPlatformType"
              dict-type="dict_platform_type"
              clearable
              placeholder="全部平台"
              style="width: 120px"
              @change="loadPreviewData"
            />
          </div>
          <ScreenPreviewPanel
            :layout="layout"
            :widget-results="previewWidgetResults"
            :title="formMeta.dashboardName || '运营数据大屏'"
          />
        </el-card>

        <el-card shadow="never" class="config-card">
          <template #header><span class="section-title">配置说明</span></template>
          <div class="config-hints">
            <p><strong>指标卡</strong>：从内置指标或自定义指标中选择，需满足内/外部数据范围</p>
            <p><strong>图表组件</strong>：支持折线图、柱状图、饼图等 ECharts 类型</p>
            <p><strong>列表组件</strong>：排行榜展示，末行与图表并排显示</p>
            <p><strong>布局</strong>：固定两列布局，图表两两成行，单图与列表并排</p>
            <p><strong>全局筛选</strong>：预览区日期/IP组/平台与全屏一致；内置组件自动生效</p>
            <p><strong>筛选映射</strong>：编辑自定义指标/查询时，为 SQL 中每个占位符（如 <code>:publish_start</code>）选择对应的全局筛选来源；未映射的占位符不受全局筛选影响</p>
            <p><strong>SQL 占位符</strong>：<code>:tenantId</code> 始终自动绑定；日期/IP组/平台需在映射区显式配置后才注入</p>
            <p><strong>今日统计</strong>：STAT 组件始终按当天统计，不受日期选择影响</p>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="editDialogVisible" :title="editDialogTitle" width="680px" destroy-on-close>
      <el-form v-if="editingWidget" label-width="108px" size="default">
        <el-form-item label="标题" required>
          <el-input v-model="editingWidget.title" />
        </el-form-item>
        <el-form-item label="数据源">
          <el-select v-model="editingWidget.sourceType" style="width: 160px" @change="onSourceTypeChange(editingWidget)">
            <el-option label="内置" value="BUILTIN" />
            <el-option label="自定义指标" value="METRIC" />
            <el-option label="自定义查询" value="QUERY" />
          </el-select>
        </el-form-item>

        <template v-if="editingWidget.sourceType === 'BUILTIN'">
          <el-form-item v-if="editingWidget.type === 'KPI' || editingWidget.type === 'STAT'" label="内置指标">
            <el-select v-model="editingWidget.builtinKey" filterable style="width: 100%">
              <el-option v-for="item in filteredBuiltinKpis" :key="item.key" :label="item.label" :value="item.key" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editingWidget.type === 'CHART'" label="内置图表">
            <el-select v-model="editingWidget.builtinKey" filterable style="width: 100%" @change="onBuiltinChartChange(editingWidget)">
              <el-option v-for="item in filteredBuiltinCharts" :key="item.key" :label="item.label" :value="item.key" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editingWidget.type === 'LIST'" label="内置列表">
            <el-select v-model="editingWidget.builtinKey" filterable style="width: 100%" @change="onBuiltinListChange(editingWidget)">
              <el-option v-for="item in filteredBuiltinLists" :key="item.key" :label="item.label" :value="item.key" />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="editingWidget.sourceType === 'METRIC'">
          <el-form-item label="指标">
            <el-select
              v-model="editingWidget.metricId"
              filterable
              style="width: 100%"
              @change="onMetricQuerySourceChange(editingWidget)"
            >
              <el-option v-for="m in metricOptions" :key="m.id" :label="m.metricName" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editingWidget.type === 'KPI' || editingWidget.type === 'STAT'" label="值字段">
            <el-input v-model="editingWidget.valueKey" placeholder="metric_value" />
          </el-form-item>
        </template>

        <template v-if="editingWidget.sourceType === 'QUERY'">
          <el-form-item label="查询">
            <el-select
              v-model="editingWidget.queryId"
              filterable
              style="width: 100%"
              @change="onMetricQuerySourceChange(editingWidget)"
            >
              <el-option v-for="q in queryOptions" :key="q.id" :label="q.queryName" :value="q.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editingWidget.type === 'KPI' || editingWidget.type === 'STAT'" label="值字段">
            <el-input v-model="editingWidget.valueKey" placeholder="value" />
          </el-form-item>
        </template>

        <template v-if="editingWidget.sourceType === 'METRIC' || editingWidget.sourceType === 'QUERY'">
          <el-divider content-position="left">全局筛选映射</el-divider>
          <p class="filter-bind-hint">
            选择本指标/查询中用于承接大屏全局日期、IP 组条件的业务字段；选「不绑定」则不注入对应全局条件。
          </p>
          <el-form-item label="日期字段">
            <el-select
              v-model="globalFilterForm.dateField"
              filterable
              clearable
              placeholder="不绑定"
              style="width: 100%"
              @change="onGlobalFilterDateChange"
            >
              <el-option label="不绑定" value="" />
              <el-option
                v-for="f in dateFieldOptions"
                :key="'date-' + f.name"
                :label="f.label"
                :value="f.name"
              />
            </el-select>
            <span v-if="globalFilterForm.dateField" class="filter-bind-sub">
              全局日期范围将过滤该字段
            </span>
          </el-form-item>
          <el-form-item label="IP组字段">
            <el-select
              v-model="globalFilterForm.ipGroupField"
              filterable
              clearable
              placeholder="不绑定"
              style="width: 100%"
              @change="onGlobalFilterIpGroupChange"
            >
              <el-option label="不绑定" value="" />
              <el-option
                v-for="f in ipGroupFieldOptions"
                :key="'ip-' + f.name"
                :label="f.label"
                :value="f.name"
              />
            </el-select>
            <span v-if="globalFilterForm.ipGroupField" class="filter-bind-sub">
              选择 IP 组后过滤该字段；未选 IP 组时不注入
            </span>
          </el-form-item>
        </template>

        <!-- CHART 轴配置 -->
        <template v-if="editingWidget.type === 'CHART'">
          <el-divider content-position="left">图表配置</el-divider>
          <el-form-item label="图表类型">
            <el-select v-model="editingWidget.chartType" style="width: 120px">
              <el-option label="折线" value="line" />
              <el-option label="柱状" value="bar" />
              <el-option label="饼图" value="pie" />
            </el-select>
          </el-form-item>
          <template v-if="editingWidget.sourceType === 'BUILTIN' && builtinChartMeta">
            <el-form-item label="X轴字段">
              <el-input :model-value="builtinChartMeta.xKey" readonly />
            </el-form-item>
            <el-form-item label="Y轴字段">
              <el-input :model-value="builtinChartMeta.yKey" readonly />
            </el-form-item>
            <el-form-item v-if="builtinChartMeta.groupKey" label="分组字段">
              <el-input :model-value="builtinChartMeta.groupKey" readonly />
            </el-form-item>
          </template>
          <template v-if="editingWidget.sourceType === 'METRIC' || editingWidget.sourceType === 'QUERY'">
            <el-form-item label="X轴字段">
              <el-select
                v-model="editingWidget.xKey"
                filterable
                :loading="chartFieldsLoading"
                placeholder="选择 X 轴字段"
                style="width: 100%"
              >
                <el-option v-for="f in chartFieldOptions" :key="f.key" :label="f.label" :value="f.key" />
              </el-select>
            </el-form-item>
            <el-form-item label="Y轴字段">
              <el-select
                v-model="editingWidget.yKey"
                filterable
                :loading="chartFieldsLoading"
                placeholder="选择 Y 轴字段"
                style="width: 100%"
              >
                <el-option v-for="f in chartFieldOptions" :key="'y-' + f.key" :label="f.label" :value="f.key" />
              </el-select>
            </el-form-item>
            <el-form-item label="分组字段">
              <el-select
                v-model="editingWidget.groupKey"
                filterable
                clearable
                :loading="chartFieldsLoading"
                placeholder="无"
                style="width: 100%"
              >
                <el-option label="无" value="" />
                <el-option v-for="f in chartFieldOptions" :key="'g-' + f.key" :label="f.label" :value="f.key" />
              </el-select>
            </el-form-item>
            <el-form-item label="Y轴聚合">
              <el-select v-model="editingWidget.yAgg" style="width: 140px">
                <el-option label="SUM" value="SUM" />
                <el-option label="AVG" value="AVG" />
                <el-option label="COUNT" value="COUNT" />
                <el-option label="MAX" value="MAX" />
                <el-option label="MIN" value="MIN" />
              </el-select>
            </el-form-item>
          </template>
        </template>

        <!-- LIST 列配置 -->
        <template v-if="editingWidget.type === 'LIST'">
          <el-divider content-position="left">列表配置</el-divider>
          <el-form-item label="显示列">
            <div class="list-field-picker">
              <el-empty v-if="!listFieldRows.length" description="暂无可用字段" :image-size="48" />
              <div v-for="row in listFieldRows" :key="row.key" class="list-field-row">
                <el-checkbox v-model="row.checked">{{ row.key }}</el-checkbox>
                <el-input v-model="row.label" placeholder="列标题" size="small" style="width: 160px" />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="排序字段">
            <el-select v-model="editingWidget.sortKey" filterable allow-create style="width: 160px">
              <el-option
                v-for="row in checkedListFieldRows"
                :key="row.key"
                :label="row.label"
                :value="row.key"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排序方向">
            <el-radio-group v-model="editingWidget.sortOrder">
              <el-radio value="DESC">降序</el-radio>
              <el-radio value="ASC">升序</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="显示条数">
            <el-input-number v-model="editingWidget.limit" :min="1" :max="50" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmWidgetEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ScreenPreviewPanel from '@/components/screen/ScreenPreviewPanel.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  createAnalyticsDashboard,
  getDashboardConfigList,
  getDashboardData,
  updateDashboardFull,
} from '@/api/dashboard'
import { getMetricList, previewMetric } from '@/api/metric'
import { executeCustomQuery, getCustomQueryList } from '@/api/custom-query'
import {
  getFilterableDateFields,
  getFilterableIpGroupFields,
  resolveFieldSqlColumn,
  unpackQueryBuilderParams,
  type MetricFieldMeta,
} from '@/constants/metricSchema'
import {
  BUILTIN_CHART_KEYS,
  BUILTIN_KPI_KEYS,
  BUILTIN_LIST_KEYS,
  DEFAULT_CUSTOM_LIST_FIELDS,
  buildDashboardDataQuery,
  defaultColumnsForList,
  emptyGlobalFilter,
  emptyLayout,
  extractColumnKeysFromRows,
  getAvailableChartFields,
  getAvailableListFields,
  getBuiltinChartMeta,
  normalizeGlobalFilter,
  normalizeFilterBind,
  type ListColumnDef,
  mergeWidgetResultsWithLayout,
  parseLayout,
  type DashboardDateRangeKey,
  type DashboardVO,
  type DashboardWidgetResult,
  type DataScreenLayout,
  type DataScreenScope,
  type DataScreenWidgetDef,
  type WidgetGlobalFilter,
  type WidgetType,
} from '@/types/dataScreen'

interface ListFieldRow {
  key: string
  label: string
  defaultLabel: string
  checked: boolean
}

const NEW_TEMPLATE = -1
interface QueryOption { id: number; queryName: string; sqlText?: string; paramsJson?: string }
interface MetricOption { id: number; metricName: string; metricFormula?: string; dataSource?: string }

interface SourceFieldContext {
  dataSource: string
  joinTables: string[]
}

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const previewLoading = ref(false)
const previewDateRangeKey = ref<DashboardDateRangeKey>('7d')
const previewIpGroupId = ref<number | undefined>()
const previewPlatformType = ref<string | undefined>()
const list = ref<DashboardVO[]>([])
const selectedId = ref<number | typeof NEW_TEMPLATE>(NEW_TEMPLATE)
const editDialogVisible = ref(false)
const editDialogTitle = ref('编辑组件')
const editingWidget = ref<DataScreenWidgetDef | null>(null)
const listFieldRows = ref<ListFieldRow[]>([])
const chartFieldOptions = ref<ListColumnDef[]>([])
const chartFieldsLoading = ref(false)
const previewDataWidgets = ref<DashboardWidgetResult[]>([])
const metricOptions = ref<MetricOption[]>([])
const queryOptions = ref<QueryOption[]>([])
const globalFilterForm = ref<WidgetGlobalFilter>(emptyGlobalFilter())
const dateFieldOptions = ref<MetricFieldMeta[]>([])
const ipGroupFieldOptions = ref<MetricFieldMeta[]>([])
const sourceFieldContext = ref<SourceFieldContext>({ dataSource: '', joinTables: [] })

const formMeta = reactive({
  id: undefined as number | undefined,
  dashboardName: '',
  dashboardType: 'OPS',
  status: 1,
})

const layout = reactive<DataScreenLayout>(emptyLayout('INTERNAL'))

const kpiWidgets = computed(() => layout.widgets.filter((w) => w.type === 'KPI'))
const statWidgets = computed(() => layout.widgets.filter((w) => w.type === 'STAT'))
const chartWidgets = computed(() => layout.widgets.filter((w) => w.type === 'CHART'))
const listWidgets = computed(() => layout.widgets.filter((w) => w.type === 'LIST'))

const filteredBuiltinKpis = computed(() =>
  BUILTIN_KPI_KEYS.filter((k) => k.scope === layout.scope),
)
const filteredBuiltinCharts = computed(() =>
  BUILTIN_CHART_KEYS.filter((k) => k.scope === layout.scope),
)
const filteredBuiltinLists = computed(() =>
  BUILTIN_LIST_KEYS.filter((k) => k.scope === layout.scope),
)

const builtinChartMeta = computed(() =>
  editingWidget.value?.type === 'CHART' && editingWidget.value.sourceType === 'BUILTIN'
    ? getBuiltinChartMeta(editingWidget.value.builtinKey)
    : undefined,
)

const checkedListFieldRows = computed(() => listFieldRows.value.filter((r) => r.checked))

const previewWidgetResults = computed((): DashboardWidgetResult[] =>
  mergeWidgetResultsWithLayout(layout, previewDataWidgets.value),
)

const sourceLabel = (w: DataScreenWidgetDef) => {
  if (w.sourceType === 'BUILTIN') {
    const all = [...BUILTIN_KPI_KEYS, ...BUILTIN_CHART_KEYS, ...BUILTIN_LIST_KEYS]
    return all.find((k) => k.key === w.builtinKey)?.label || w.builtinKey || '内置'
  }
  if (w.sourceType === 'METRIC') {
    return metricOptions.value.find((m) => m.id === w.metricId)?.metricName || '自定义指标'
  }
  return queryOptions.value.find((q) => q.id === w.queryId)?.queryName || '自定义查询'
}

const chartTypeLabel = (t?: string) => {
  if (t === 'pie') return '饼图'
  if (t === 'bar') return '柱状图'
  return '折线图'
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getDashboardConfigList({ pageNum: 1, pageSize: 50 })
    const data = res?.data ?? res
    list.value = data?.list ?? data?.records ?? []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

const extractPreviewRows = (data: unknown): Record<string, unknown>[] => {
  const payload = (data as { data?: unknown })?.data ?? data
  const rows = (payload as { rows?: unknown })?.rows ?? []
  return Array.isArray(rows) ? rows as Record<string, unknown>[] : []
}

const resolveSourceFieldContext = (widget: DataScreenWidgetDef): SourceFieldContext => {
  if (widget.sourceType === 'QUERY' && widget.queryId) {
    const query = queryOptions.value.find((q) => q.id === widget.queryId)
    const builder = unpackQueryBuilderParams(query?.paramsJson)
    if (builder?.dataSource) {
      return { dataSource: builder.dataSource, joinTables: builder.joinTables ?? [] }
    }
  }
  if (widget.sourceType === 'METRIC' && widget.metricId) {
    const metric = metricOptions.value.find((m) => m.id === widget.metricId)
    if (metric?.dataSource) {
      return { dataSource: metric.dataSource, joinTables: [] }
    }
  }
  return { dataSource: '', joinTables: [] }
}

const refreshGlobalFilterOptions = (widget: DataScreenWidgetDef) => {
  const ctx = resolveSourceFieldContext(widget)
  sourceFieldContext.value = ctx
  if (!ctx.dataSource) {
    dateFieldOptions.value = []
    ipGroupFieldOptions.value = []
    return
  }
  dateFieldOptions.value = getFilterableDateFields(ctx.dataSource, ctx.joinTables)
  ipGroupFieldOptions.value = getFilterableIpGroupFields(ctx.dataSource, ctx.joinTables)
}

const syncGlobalFilterForm = (widget: DataScreenWidgetDef) => {
  globalFilterForm.value = normalizeGlobalFilter(widget.globalFilter)
  refreshGlobalFilterOptions(widget)
  if (globalFilterForm.value.dateField && !dateFieldOptions.value.some((f) => f.name === globalFilterForm.value.dateField)) {
    globalFilterForm.value.dateField = undefined
  }
  if (globalFilterForm.value.ipGroupField && !ipGroupFieldOptions.value.some((f) => f.name === globalFilterForm.value.ipGroupField)) {
    globalFilterForm.value.ipGroupField = undefined
  }
}

const resolveGlobalFilterColumns = (): WidgetGlobalFilter => {
  const ctx = sourceFieldContext.value
  const form = { ...globalFilterForm.value }
  if (form.dateField && ctx.dataSource) {
    form.dateColumn = resolveFieldSqlColumn(form.dateField, ctx.dataSource)
    const meta = dateFieldOptions.value.find((f) => f.name === form.dateField)
    form.dateFieldType = meta?.type === 'datetime' ? 'datetime' : 'date'
  }
  if (form.ipGroupField && ctx.dataSource) {
    form.ipGroupColumn = resolveFieldSqlColumn(form.ipGroupField, ctx.dataSource)
  }
  return form
}

const onGlobalFilterDateChange = () => {
  if (!globalFilterForm.value.dateField) {
    globalFilterForm.value.dateColumn = undefined
    globalFilterForm.value.dateFieldType = undefined
  }
}

const onGlobalFilterIpGroupChange = () => {
  if (!globalFilterForm.value.ipGroupField) {
    globalFilterForm.value.ipGroupColumn = undefined
  }
}

const loadChartFieldOptions = async (widget: DataScreenWidgetDef) => {
  if (widget.type !== 'CHART' || widget.sourceType === 'BUILTIN') {
    chartFieldOptions.value = []
    return
  }
  chartFieldsLoading.value = true
  let dynamicKeys: string[] = []
  try {
    if (widget.sourceType === 'METRIC' && widget.metricId) {
      const metric = metricOptions.value.find((m) => m.id === widget.metricId)
      if (metric?.metricFormula) {
        const res = await previewMetric({ metricFormula: metric.metricFormula })
        dynamicKeys = extractColumnKeysFromRows(extractPreviewRows(res))
      }
    } else if (widget.sourceType === 'QUERY' && widget.queryId) {
      const res = await executeCustomQuery(widget.queryId)
      dynamicKeys = extractColumnKeysFromRows(extractPreviewRows(res))
    }
  } catch {
    dynamicKeys = []
  } finally {
    chartFieldOptions.value = getAvailableChartFields(widget.sourceType, undefined, dynamicKeys)
    chartFieldsLoading.value = false
  }
}

const ensureChartFieldDefaults = (widget: DataScreenWidgetDef) => {
  const keys = new Set(chartFieldOptions.value.map((f) => f.key))
  if (!widget.xKey || !keys.has(widget.xKey)) {
    widget.xKey = chartFieldOptions.value[0]?.key ?? 'stat_date'
  }
  if (!widget.yKey || !keys.has(widget.yKey)) {
    widget.yKey = chartFieldOptions.value.find((f) => f.key === 'value' || f.key === 'metric_value')?.key
      ?? chartFieldOptions.value[1]?.key
      ?? 'value'
  }
  if (widget.groupKey && !keys.has(widget.groupKey)) {
    widget.groupKey = undefined
  }
}

const loadPickers = async () => {
  try {
    const res: any = await getMetricList({ pageNum: 1, pageSize: 200 })
    const data = res?.data ?? res
    metricOptions.value = data?.list ?? data?.records ?? []
  } catch { metricOptions.value = [] }
  try {
    const res: any = await getCustomQueryList({ pageNum: 1, pageSize: 200, status: 'PUBLISHED' })
    const data = res?.data ?? res
    queryOptions.value = data?.list ?? data?.records ?? []
  } catch { queryOptions.value = [] }
}

const resetForm = (scope: DataScreenScope = 'INTERNAL') => {
  Object.assign(formMeta, { id: undefined, dashboardName: '', dashboardType: 'OPS', status: 1 })
  Object.assign(layout, emptyLayout(scope))
  previewDataWidgets.value = []
}

const loadTemplate = (row: DashboardVO) => {
  Object.assign(formMeta, {
    id: row.id,
    dashboardName: row.dashboardName,
    dashboardType: row.dashboardType,
    status: row.status,
  })
  const parsed = parseLayout(row.layout)
  Object.assign(layout, parsed)
  loadPreviewData()
}

const onTemplateSelect = (id: number) => {
  if (id === NEW_TEMPLATE) {
    selectedId.value = NEW_TEMPLATE
    resetForm('INTERNAL')
    return
  }
  const row = list.value.find((d) => d.id === id)
  if (row) loadTemplate(row)
}

const onScopeChange = () => {
  layout.widgets = layout.widgets.filter((w) => {
    if (w.sourceType !== 'BUILTIN' || !w.builtinKey) return true
    const scope = layout.scope
    const ok = [...BUILTIN_KPI_KEYS, ...BUILTIN_CHART_KEYS, ...BUILTIN_LIST_KEYS]
      .some((k) => k.key === w.builtinKey && k.scope === scope)
    return ok
  })
}

const loadPreviewData = async () => {
  if (!formMeta.id) {
    previewDataWidgets.value = []
    return
  }
  previewLoading.value = true
  try {
    const res: any = await getDashboardData(
      formMeta.id,
      buildDashboardDataQuery({
        dateRangeKey: previewDateRangeKey.value,
        ipGroupId: previewIpGroupId.value,
        platformType: previewPlatformType.value,
      }),
    )
    const data = res?.data ?? res
    previewDataWidgets.value = data?.widgets ?? []
  } catch {
    previewDataWidgets.value = []
  } finally {
    previewLoading.value = false
  }
}

const previewScreen = () => {
  if (!formMeta.id) {
    ElMessage.warning('请先保存模板后再预览')
    return
  }
  router.push({ path: `/screen/${formMeta.id}`, query: { _t: String(Date.now()) } })
}

const newWidgetId = () => `w${Date.now()}${Math.floor(Math.random() * 1000)}`

const addWidget = (type: WidgetType) => {
  if (type === 'KPI' && kpiWidgets.value.length >= 6) {
    ElMessage.warning('指标卡最多6个')
    return
  }
  if (type === 'CHART' && chartWidgets.value.length >= 4) {
    ElMessage.warning('图表组件最多4个')
    return
  }
  const widget: DataScreenWidgetDef = {
    id: newWidgetId(),
    type,
    title: type === 'KPI' ? '新指标' : type === 'STAT' ? '今日统计' : type === 'CHART' ? '新图表' : '新列表',
    sourceType: 'BUILTIN',
    builtinKey: filteredBuiltinKpis.value[0]?.key,
    chartType: 'line',
    valueKey: 'metric_value',
    xKey: 'stat_date',
    yKey: 'value',
    yAgg: 'SUM',
    sortOrder: 'DESC',
    limit: 10,
  }
  if (type === 'CHART') {
    widget.builtinKey = filteredBuiltinCharts.value[0]?.key
    widget.chartType = filteredBuiltinCharts.value[0]?.chartType ?? 'line'
  }
  if (type === 'LIST') {
    widget.builtinKey = filteredBuiltinLists.value[0]?.key
    widget.columns = defaultColumnsForList(widget.builtinKey)
    widget.sortKey = widget.columns[0]?.key ?? 'rank'
    widget.limit = 10
  }
  layout.widgets.push(widget)
  openWidgetEdit(widget)
}

const removeWidgetById = (id: string) => {
  const idx = layout.widgets.findIndex((w) => w.id === id)
  if (idx >= 0) layout.widgets.splice(idx, 1)
}

const initListFieldRows = (widget: DataScreenWidgetDef) => {
  const available = getAvailableListFields(widget.sourceType, widget.builtinKey, widget.columns)
  const selected = new Map((widget.columns ?? []).map((c) => [c.key, c.label]))
  listFieldRows.value = available.map((f) => ({
    key: f.key,
    defaultLabel: f.label,
    label: selected.get(f.key) ?? f.label,
    checked: selected.has(f.key),
  }))
  if (listFieldRows.value.length && !listFieldRows.value.some((r) => r.checked)) {
    listFieldRows.value.forEach((r) => { r.checked = true })
  }
}

const buildColumnsFromListFieldRows = (): { key: string; label: string }[] =>
  listFieldRows.value
    .filter((r) => r.checked)
    .map((r) => ({ key: r.key, label: r.label.trim() || r.defaultLabel }))

const openWidgetEdit = (w: DataScreenWidgetDef) => {
  const copy: DataScreenWidgetDef = {
    ...w,
    columns: w.columns?.map((c) => ({ ...c })) ?? (w.type === 'LIST' ? defaultColumnsForList(w.builtinKey) : undefined),
    sortOrder: w.sortOrder ?? 'DESC',
    limit: w.limit ?? 10,
    yAgg: w.yAgg ?? 'SUM',
    valueKey: w.valueKey ?? (w.sourceType === 'METRIC' ? 'metric_value' : 'value'),
    filterBind: w.filterBind ? normalizeFilterBind(w.filterBind) : undefined,
    globalFilter: w.globalFilter ? normalizeGlobalFilter(w.globalFilter) : undefined,
  }
  if (copy.type === 'LIST' && !copy.sortKey && copy.columns?.length) {
    copy.sortKey = copy.columns[0].key
  }
  editingWidget.value = copy
  if (copy.sourceType === 'METRIC' || copy.sourceType === 'QUERY') {
    syncGlobalFilterForm(copy)
  } else {
    globalFilterForm.value = emptyGlobalFilter()
    dateFieldOptions.value = []
    ipGroupFieldOptions.value = []
  }
  if (copy.type === 'LIST') {
    initListFieldRows(copy)
    chartFieldOptions.value = []
  } else if (copy.type === 'CHART' && copy.sourceType !== 'BUILTIN') {
    listFieldRows.value = []
    void loadChartFieldOptions(copy).then(() => ensureChartFieldDefaults(copy))
  } else {
    listFieldRows.value = []
    chartFieldOptions.value = []
  }
  editDialogTitle.value = `编辑${w.type === 'KPI' ? '指标卡' : w.type === 'STAT' ? '今日统计' : w.type === 'CHART' ? '图表' : '列表'}`
  editDialogVisible.value = true
}

const confirmWidgetEdit = () => {
  if (!editingWidget.value?.title?.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (editingWidget.value.type === 'LIST') {
    const columns = buildColumnsFromListFieldRows()
    if (!columns.length) {
      ElMessage.warning('请至少选择一个显示列')
      return
    }
    editingWidget.value.columns = columns
    if (!columns.some((c) => c.key === editingWidget.value!.sortKey)) {
      editingWidget.value.sortKey = columns[0].key
    }
  }
  if (editingWidget.value.sourceType === 'METRIC' || editingWidget.value.sourceType === 'QUERY') {
    const gf = resolveGlobalFilterColumns()
    if (gf.dateField || gf.ipGroupField) {
      editingWidget.value.globalFilter = gf
    } else {
      delete editingWidget.value.globalFilter
    }
    delete editingWidget.value.filterBind
  } else {
    delete editingWidget.value.globalFilter
    delete editingWidget.value.filterBind
  }
  const idx = layout.widgets.findIndex((w) => w.id === editingWidget.value!.id)
  if (idx >= 0) {
    layout.widgets[idx] = { ...editingWidget.value }
  }
  editDialogVisible.value = false
}

const onSourceTypeChange = (w: DataScreenWidgetDef) => {
  if (w.sourceType === 'METRIC') {
    w.metricId = metricOptions.value[0]?.id
    w.valueKey = 'metric_value'
    delete w.globalFilter
    delete w.filterBind
    if (editingWidget.value?.id === w.id) syncGlobalFilterForm(w)
    if (w.type === 'CHART' && editingWidget.value?.id === w.id) {
      w.yAgg = w.yAgg ?? 'SUM'
      void loadChartFieldOptions(w).then(() => ensureChartFieldDefaults(w))
    }
  }
  if (w.sourceType === 'QUERY') {
    w.queryId = queryOptions.value[0]?.id
    w.valueKey = 'value'
    delete w.globalFilter
    delete w.filterBind
    if (editingWidget.value?.id === w.id) syncGlobalFilterForm(w)
    if (w.type === 'CHART' && editingWidget.value?.id === w.id) {
      w.yAgg = w.yAgg ?? 'SUM'
      void loadChartFieldOptions(w).then(() => ensureChartFieldDefaults(w))
    }
  }
  if (w.sourceType === 'BUILTIN') {
    delete w.globalFilter
    delete w.filterBind
    if (editingWidget.value?.id === w.id) {
      globalFilterForm.value = emptyGlobalFilter()
      dateFieldOptions.value = []
      ipGroupFieldOptions.value = []
    }
  }
  if (w.type === 'LIST') {
    w.columns = w.sourceType === 'BUILTIN'
      ? defaultColumnsForList(w.builtinKey)
      : DEFAULT_CUSTOM_LIST_FIELDS.map((f) => ({ ...f }))
    w.sortKey = w.columns?.[0]?.key
    if (editingWidget.value?.id === w.id) initListFieldRows(w)
  }
}

const onMetricQuerySourceChange = (w: DataScreenWidgetDef) => {
  if (editingWidget.value?.id === w.id) {
    globalFilterForm.value = emptyGlobalFilter()
    syncGlobalFilterForm(w)
  }
  if (w.type === 'CHART' && w.sourceType !== 'BUILTIN') {
    void loadChartFieldOptions(w).then(() => ensureChartFieldDefaults(w))
  }
}

const onBuiltinChartChange = (w: DataScreenWidgetDef) => {
  const found = BUILTIN_CHART_KEYS.find((k) => k.key === w.builtinKey)
  if (found) w.chartType = found.chartType
  const meta = getBuiltinChartMeta(w.builtinKey)
  if (meta) {
    w.xKey = meta.xKey
    w.yKey = meta.yKey
    w.groupKey = meta.groupKey
  }
}

const onBuiltinListChange = (w: DataScreenWidgetDef) => {
  const found = BUILTIN_LIST_KEYS.find((k) => k.key === w.builtinKey)
  if (found) w.title = found.label
  w.columns = defaultColumnsForList(w.builtinKey)
  w.sortKey = w.columns?.[0]?.key ?? 'rank'
  if (editingWidget.value?.id === w.id) initListFieldRows(w)
}

const handleSave = async () => {
  if (!formMeta.dashboardName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  saving.value = true
  const layoutJson = JSON.stringify({
    version: layout.version,
    scope: layout.scope,
    refreshSeconds: layout.refreshSeconds,
    widgets: layout.widgets,
  })
  try {
    if (formMeta.id) {
      await updateDashboardFull({
        id: formMeta.id,
        dashboardName: formMeta.dashboardName,
        dashboardType: formMeta.dashboardType,
        layout: layoutJson,
        status: formMeta.status,
      })
    } else {
      const res: any = await createAnalyticsDashboard({
        dashboardName: formMeta.dashboardName,
        dashboardType: formMeta.dashboardType,
        layout: layoutJson,
      })
      const newId = typeof res === 'object' ? (res?.data ?? res?.id) : res
      if (newId) {
        formMeta.id = Number(newId)
        selectedId.value = formMeta.id
      }
    }
    ElMessage.success('保存成功')
    await loadList()
    if (formMeta.id) {
      const saved = list.value.find((d) => d.id === formMeta.id)
      if (saved) {
        formMeta.dashboardName = saved.dashboardName
      }
      await loadPreviewData()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

watch(() => layout.widgets, () => {
  if (formMeta.id) {
    // 结构变化后仍用已保存 id 拉数；保存后 layout 与 DB 一致
  }
}, { deep: true })

onMounted(async () => {
  await loadList()
  await loadPickers()
  if (list.value.length) {
    selectedId.value = list.value[0].id
    loadTemplate(list.value[0])
  }
})
</script>

<style scoped>
.screen-config-page {
  min-height: calc(100vh - 56px);
  background: #f0f2f5;
}
.config-toolbar {
  height: 56px; background: #fff; display: flex; align-items: center;
  justify-content: space-between; padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08); position: sticky; top: 0; z-index: 10;
}
.toolbar-title { font-size: 16px; font-weight: 600; color: #303133; }
.toolbar-actions { display: flex; align-items: center; gap: 12px; }
.config-body {
  display: grid; grid-template-columns: 1fr 1fr; gap: 24px; padding: 24px;
}
.config-card { margin-bottom: 0; }
.section-title { font-size: 14px; font-weight: 600; }
.section-header { display: flex; align-items: center; justify-content: space-between; }
.widget-item {
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 12px; margin-bottom: 8px;
}
.widget-item:hover { border-color: #409eff; }
.widget-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.widget-desc { font-size: 13px; color: #606266; }
.preview-filters {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
  margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid var(--el-border-color-lighter);
}
.preview-card-wrap :deep(.screen-preview-panel) { min-height: 480px; }
.config-hints { font-size: 13px; color: #606266; line-height: 1.8; }
.config-hints p { margin: 0 0 8px; }
.list-field-picker { width: 100%; }
.list-field-row {
  display: flex; align-items: center; gap: 12px; margin-bottom: 8px;
}
.list-field-row :deep(.el-checkbox) { min-width: 140px; }
.filter-bind-hint {
  font-size: 12px; color: #909399; line-height: 1.6; margin: 0 0 12px 108px;
}
.filter-bind-sub {
  display: block; font-size: 12px; color: #909399; margin-top: 4px;
}
@media (max-width: 1200px) {
  .config-body { grid-template-columns: 1fr; }
}
</style>
