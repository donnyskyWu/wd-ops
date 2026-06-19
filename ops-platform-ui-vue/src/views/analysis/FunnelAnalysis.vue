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

      <el-card class="chart-card" shadow="never">
        <template #header><div class="card-header"><span>漏斗转化图</span></div></template>
        <div ref="funnelChartRef" style="height: 450px" v-loading="loadingData" />
      </el-card>

      <el-card class="table-card" shadow="never">
        <template #header><div class="card-header"><span>转化率明细</span></div></template>
        <el-table :data="funnelSteps" border stripe>
          <el-table-column prop="stepOrder" label="顺序" width="80" align="center" />
          <el-table-column prop="name" label="步骤" min-width="200" />
          <el-table-column prop="count" label="数量" width="120" align="right">
            <template #default="{ row }">{{ formatCount(row.count) }}</template>
          </el-table-column>
          <el-table-column prop="conversionRate" label="转化率(%)" width="140" align="right">
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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
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

const funnelOptions = computed(() => funnelList.value)

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

const formatCount = (count: any) => {
  const v = typeof count === 'number' && !isNaN(count) ? count : 0
  if (v >= 10000) return (v / 10000).toFixed(1) + 'W'
  if (v >= 1000) return (v / 1000).toFixed(1) + 'K'
  return v.toString()
}

const getFunnelTypeLabel = (type: string) => {
  const map: Record<string, string> = { CONTENT: '内容漏斗', FOLLOWER: '粉丝漏斗', ORDER: '订单漏斗' }
  return map[type] || type || '-'
}

const loadFunnelList = async () => {
  loadingFunnels.value = true
  try {
    const res: any = await getFunnelList({ pageNum: 1, pageSize: 50 })
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? []
    funnelList.value = list
    customFunnelList.value = list
    if (list.length > 0 && !queryForm.funnelId) {
      queryForm.funnelId = list[0].id
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
    { key: 'conversionRate', label: '转化率(%)' },
  ]
  exportToExcel(funnelSteps.value, columns, '漏斗分析报告')
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
  const chart = echarts.init(el)
  funnelChart = chart
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [
      {
        name: '漏斗分析',
        type: 'funnel',
        left: '10%',
        top: 40,
        bottom: 40,
        width: '80%',
        min: 0,
        max: 100000,
        minSize: '0%',
        maxSize: '100%',
        sort: 'descending',
        gap: 2,
        label: { show: true, position: 'inside', formatter: '{b}: {c}' },
        itemStyle: { borderColor: '#fff', borderWidth: 1 },
        data: funnelSteps.value.map(s => ({ name: s.name, value: s.count })),
      },
    ],
  })
}

onMounted(() => {
  loadFunnelList()
  loadMetricOptions()
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
