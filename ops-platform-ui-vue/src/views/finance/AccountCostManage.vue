<template>
  <div class="account-cost-page">
    <el-tabs v-model="activePlatform" class="platform-tabs" @tab-change="handlePlatformChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="公众号" name="WECHAT_OFFICIAL" />
      <el-tab-pane label="视频号" name="WECHAT_VIDEO" />
      <el-tab-pane label="抖音" name="DOUYIN" />
      <el-tab-pane label="快手" name="KUAISHOU" />
      <el-tab-pane label="小红书" name="XIAOHONGSHU" />
      <el-tab-pane label="企微" name="WEWORK" />
      <el-tab-pane label="个微" name="WECHAT_PERSONAL" />
    </el-tabs>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="账号名称">
        <el-input v-model="searchForm.keyword" placeholder="搜索账号" clearable />
      </el-form-item>
      <template #extra>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </TableSearch>

    <el-table :data="accountRows" v-loading="loading" border stripe>
      <el-table-column prop="accountName" label="账号名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="platformType" label="平台" width="100" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_platform_type" :value="row.platformType" />
        </template>
      </el-table-column>
      <el-table-column prop="purchaseCost" label="采购成本" width="120" align="right">
        <template #default="{ row }">¥{{ formatMoney(row.purchaseCost) }}</template>
      </el-table-column>
      <el-table-column prop="processCost" label="过程成本" width="120" align="right">
        <template #default="{ row }">¥{{ formatMoney(row.processCost) }}</template>
      </el-table-column>
      <el-table-column prop="totalCost" label="总成本" width="120" align="right">
        <template #default="{ row }">
          <span class="amount-text">¥{{ formatMoney(row.totalCost) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="realName" label="实名人" width="100" />
      <el-table-column prop="operatorName" label="运营人员" width="100" />
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" @click="handleManageCost(row)">成本管理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :current-page="pagination.pageNo"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @update:current-page="(val) => pagination.pageNo = val"
      @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
      @change="loadData"
    />

    <!-- 查看：账号信息 + ROI/LTV + 趋势 + 成本明细 -->
    <el-drawer
      v-model="viewVisible"
      :title="`账号成本详情 - ${currentAccount?.accountName || ''}`"
      size="920px"
      destroy-on-close
      @opened="onViewDrawerOpened"
      @closed="disposeViewChart"
    >
      <el-descriptions v-if="currentAccount" :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="账号名称">{{ currentAccount.accountName }}</el-descriptions-item>
        <el-descriptions-item label="平台">
          <DictLabel dict-type="dict_platform_type" :value="currentAccount.platformType" />
        </el-descriptions-item>
        <el-descriptions-item label="采购成本">¥{{ formatMoney(currentAccount.purchaseCost) }}</el-descriptions-item>
        <el-descriptions-item label="过程成本">¥{{ formatMoney(currentAccount.processCost) }}</el-descriptions-item>
        <el-descriptions-item label="总成本">¥{{ formatMoney(currentAccount.totalCost) }}</el-descriptions-item>
        <el-descriptions-item label="实名人">{{ currentAccount.realName || '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-form inline class="view-filter">
        <el-form-item label="分析周期">
          <el-date-picker
            v-model="viewDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 260px"
            @change="loadViewAnalytics"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="viewAnalyticsLoading" @click="loadViewAnalytics">刷新分析</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">投资回报（ROI）</el-divider>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="metric-hint"
        title="ROI = 营收 ÷ 总成本；毛利率 = (营收 - 成本) ÷ 营收"
      />
      <div class="stat-grid-5" v-loading="viewAnalyticsLoading">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value primary">¥{{ formatMoney(viewStats.totalRevenue) }}</div>
          <div class="stat-label">营收</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value danger">¥{{ formatMoney(viewStats.totalCost) }}</div>
          <div class="stat-label">总成本</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value success">¥{{ formatMoney(viewStats.totalProfit) }}</div>
          <div class="stat-label">利润</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value warning">{{ formatMoney(viewStats.roi) }}</div>
          <div class="stat-label">ROI</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ formatPercent(viewStats.grossMargin) }}</div>
          <div class="stat-label">毛利率</div>
        </el-card>
      </div>

      <el-divider content-position="left">用户价值（LTV / CAC）</el-divider>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="metric-hint"
        title="单粉贡献≈周期营收÷粉丝数；CAC=投放成本÷新增粉丝；LTV/CAC>3 为优"
      />
      <div class="stat-grid-5" v-loading="viewAnalyticsLoading">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value primary">¥{{ formatMoney(viewStats.ltv) }}</div>
          <div class="stat-label">单粉贡献（LTV 近似）</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value primary">¥{{ formatMoney(viewStats.arpu) }}</div>
          <div class="stat-label">ARPU（营收/粉丝）</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value danger">¥{{ formatMoney(viewStats.cac) }}</div>
          <div class="stat-label">CAC（投放/新增粉）</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">
            <el-tag :type="ltvCacTagType(viewStats.ltvCacRatio)" effect="dark" size="large">
              {{ viewStats.ltvCacRatio > 0 ? formatMoney(viewStats.ltvCacRatio) : '—' }}
            </el-tag>
          </div>
          <div class="stat-label">LTV / CAC</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">+{{ formatInteger(viewStats.newFollowers) }}</div>
          <div class="stat-label">新增粉丝</div>
        </el-card>
      </div>
      <el-row :gutter="12" class="view-stats-secondary" v-loading="viewAnalyticsLoading">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ formatInteger(viewStats.totalFollowers) }}</div>
            <div class="stat-label">粉丝总数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value success">+{{ formatInteger(viewStats.netFollowers) }}</div>
            <div class="stat-label">净增粉丝</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ formatPercent(viewStats.growthRate) }}</div>
            <div class="stat-label">粉丝增长率</div>
          </el-card>
        </el-col>
      </el-row>

      <el-divider content-position="left">成本与收入趋势</el-divider>
      <div v-if="!viewTrendPoints.length && !viewAnalyticsLoading" class="empty-hint">所选周期内暂无趋势数据</div>
      <div v-show="viewTrendPoints.length" ref="viewTrendChartRef" class="view-trend-chart" />

      <el-divider content-position="left">成本结构</el-divider>
      <div v-if="!costStructureItems.length && !viewAnalyticsLoading" class="empty-hint">所选周期内暂无成本结构数据</div>
      <div v-show="costStructureItems.length" ref="viewStructureChartRef" class="view-structure-chart" />

      <el-divider content-position="left">成本明细（分析周期内）</el-divider>
      <el-table :data="currentCostDetails" border stripe size="small" empty-text="所选周期内暂无成本明细">
        <el-table-column prop="costType" label="类型" width="110">
          <template #default="{ row }">
            <DictLabel dict-type="dict_cost_type" :value="row.costType" />
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="100" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="payDate" label="支付日期" width="110" />
        <el-table-column prop="period" label="周期" width="80">
          <template #default="{ row }">
            <DictLabel dict-type="dict_cost_period" :value="row.period" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>
    </el-drawer>

    <!-- 成本管理 -->
    <el-dialog v-model="manageVisible" :title="`成本管理 - ${currentAccount?.accountName || ''}`" width="820px">
      <el-card shadow="never" class="cost-section">
        <template #header><span>采购成本（一次性）</span></template>
        <el-form :model="purchaseForm" label-width="100px" inline>
          <el-form-item label="金额">
            <el-input-number v-model="purchaseForm.amount" :min="0.01" :precision="2" />
          </el-form-item>
          <el-form-item label="支付日期">
            <el-date-picker v-model="purchaseForm.payDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="支付方式">
            <DictSelect v-model="purchaseForm.payMethod" dict-type="dict_cost_pay_method" style="width: 140px" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="purchaseForm.remark" placeholder="可选" style="width: 160px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="savePurchaseCost">保存采购成本</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="cost-section">
        <template #header>
          <div class="card-header">
            <span>过程成本</span>
            <el-button type="primary" size="small" @click="openProcessForm()">新增</el-button>
          </div>
        </template>
        <el-table :data="processCosts" border stripe size="small">
          <el-table-column prop="costType" label="类型" width="120">
            <template #default="{ row }">
              <DictLabel dict-type="dict_cost_type" :value="row.costType" />
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="100" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="payDate" label="支付日期" width="110" />
          <el-table-column prop="remark" label="说明" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openProcessForm(row)">编辑</el-button>
              <el-button link type="danger" @click="removeProcessCost(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-dialog>

    <!-- 过程成本表单 -->
    <el-dialog v-model="processFormVisible" :title="processFormTitle" width="520px" append-to-body>
      <el-form :model="processForm" ref="processFormRef" :rules="processRules" label-width="100px">
        <el-form-item label="成本类型" prop="costType">
          <DictSelect v-model="processForm.costType" dict-type="dict_cost_type" placeholder="请选择" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="processForm.amount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付日期" prop="payDate">
          <el-date-picker v-model="processForm.payDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payMethod">
          <DictSelect v-model="processForm.payMethod" dict-type="dict_cost_pay_method" />
        </el-form-item>
        <el-form-item label="周期" prop="period">
          <DictSelect v-model="processForm.period" dict-type="dict_cost_period" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="processForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcessCost">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import { getAccountList } from '@/api/account'
import { getCostList, createCost, updateCost, deleteCost, getRoiAnalysis, getRoiTrend, getRoiBreakdown } from '@/api/finance'
import { getFollowerStats } from '@/api/follower'
import { exportToExcel, unwrapApiData } from '@/utils'

interface AccountCostRow {
  id: number
  accountName: string
  platformType?: string
  platformName?: string
  realName?: string
  operatorName?: string
  purchaseCost: number
  processCost: number
  totalCost: number
}

interface CostRecord {
  id: number
  accountId: number
  costType: string
  amount: number
  payMethod?: string
  payDate?: string
  period?: string
  remark?: string
}

const PURCHASE_TYPE = 'PURCHASE'
const AD_SPEND_TYPE = 'AD_SPEND'

const COST_TYPE_LABELS: Record<string, string> = {
  PURCHASE: '购买成本',
  PROCESS_HUMAN: '人力成本',
  AD_SPEND: '投放成本',
  PROCESS: '过程成本',
}

const activePlatform = ref('ALL')
const loading = ref(false)
const accountRows = ref<AccountCostRow[]>([])
const allCosts = ref<CostRecord[]>([])

const searchForm = reactive({ keyword: '' })
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const viewVisible = ref(false)
const manageVisible = ref(false)
const currentAccount = ref<AccountCostRow | null>(null)
const currentCostDetails = ref<CostRecord[]>([])
const processCosts = ref<CostRecord[]>([])

const viewDateRange = ref<string[]>([
  dayjs().subtract(6, 'month').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD'),
])
const viewAnalyticsLoading = ref(false)
const viewTrendPoints = ref<Array<{ statDate?: string; revenue?: number; cost?: number }>>([])
const viewTrendChartRef = ref<HTMLElement>()
const viewStructureChartRef = ref<HTMLElement>()
let viewTrendChart: echarts.ECharts | null = null
let viewStructureChart: echarts.ECharts | null = null

const viewStats = reactive({
  totalRevenue: 0,
  totalCost: 0,
  totalProfit: 0,
  roi: 0,
  grossMargin: 0,
  ltv: 0,
  arpu: 0,
  cac: 0,
  ltvCacRatio: 0,
  totalFollowers: 0,
  newFollowers: 0,
  netFollowers: 0,
  growthRate: 0,
})

const costStructureItems = ref<Array<{ type: string; typeLabel: string; amount: number; percentage: number }>>([])

const resetViewStats = () => {
  Object.assign(viewStats, {
    totalRevenue: 0,
    totalCost: 0,
    totalProfit: 0,
    roi: 0,
    grossMargin: 0,
    ltv: 0,
    arpu: 0,
    cac: 0,
    ltvCacRatio: 0,
    totalFollowers: 0,
    newFollowers: 0,
    netFollowers: 0,
    growthRate: 0,
  })
  viewTrendPoints.value = []
  costStructureItems.value = []
  viewTrendChart?.clear()
  viewStructureChart?.clear()
}

const safeApiCall = async <T>(label: string, fn: () => Promise<T>, fallback: T): Promise<T> => {
  try {
    return await fn()
  } catch (e) {
    console.warn(`[AccountCostManage] ${label} skipped:`, e)
    return fallback
  }
}

const normalizeTrendPoints = (
  raw: Array<{ statDate?: string; date?: string; revenue?: number; cost?: number }> | undefined,
) => {
  const points = (raw ?? []).map((p) => ({
    statDate: String(p.statDate || p.date || ''),
    revenue: toNum(p.revenue),
    cost: toNum(p.cost),
  }))
  const hasData = points.some((p) => p.revenue > 0 || p.cost > 0)
  return hasData ? points : []
}

const purchaseForm = reactive({
  id: undefined as number | undefined,
  amount: 0,
  payDate: dayjs().format('YYYY-MM-DD'),
  payMethod: 'WECHAT',
  remark: '',
})

const processFormVisible = ref(false)
const processFormTitle = ref('新增过程成本')
const processFormRef = ref()
const processForm = reactive({
  id: undefined as number | undefined,
  costType: '',
  amount: 0,
  payDate: dayjs().format('YYYY-MM-DD'),
  payMethod: 'WECHAT',
  period: 'MONTH',
  remark: '',
})
const processRules = {
  costType: [{ required: true, message: '请选择成本类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  payDate: [{ required: true, message: '请选择支付日期', trigger: 'change' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
  period: [{ required: true, message: '请选择周期', trigger: 'change' }],
}

const formatMoney = (val?: number) => Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const formatInteger = (val?: number) => Number(val || 0).toLocaleString('zh-CN')
const formatPercent = (val?: number) => `${toNum(val).toFixed(2)}%`
const toNum = (v: unknown) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

const isPurchase = (costType: string) => costType === PURCHASE_TYPE

const isInDateRange = (payDate?: string, start?: string, end?: string) => {
  if (!payDate || !start || !end) return true
  return payDate >= start && payDate <= end
}

const sumAdSpendInRange = (accountId: number, start: string, end: string) => {
  return loadAccountCosts(accountId)
    .filter((c) => c.costType === AD_SPEND_TYPE && isInDateRange(c.payDate, start, end))
    .reduce((s, c) => s + Number(c.amount || 0), 0)
}

const ltvCacTagType = (ratio: number): 'success' | 'warning' | 'danger' | 'info' => {
  if (ratio <= 0) return 'info'
  if (ratio > 3) return 'success'
  if (ratio >= 1.5) return 'warning'
  return 'danger'
}

const aggregateCosts = (accountId: number) => {
  const rows = allCosts.value.filter((c) => c.accountId === accountId)
  const purchaseCost = rows.filter((c) => isPurchase(c.costType)).reduce((s, c) => s + Number(c.amount || 0), 0)
  const processCost = rows.filter((c) => !isPurchase(c.costType)).reduce((s, c) => s + Number(c.amount || 0), 0)
  return { purchaseCost, processCost, totalCost: purchaseCost + processCost }
}

const loadCosts = async () => {
  try {
    const res: any = await getCostList({ pageNum: 1, pageSize: 500 })
    allCosts.value = (res?.list || res?.data?.list || []) as CostRecord[]
  } catch {
    allCosts.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    await loadCosts()
    const params: Record<string, unknown> = {
      page: pagination.pageNo,
      size: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
    }
    if (activePlatform.value !== 'ALL') {
      params.platformType = activePlatform.value
    }
    const res: any = await getAccountList(params as any)
    const list = res?.list || []
    pagination.total = res?.total ?? list.length
    accountRows.value = list.map((acc: any) => {
      const costs = aggregateCosts(acc.id)
      return {
        id: acc.id,
        accountName: acc.accountName,
        platformType: acc.platformType,
        platformName: acc.platformName,
        realName: acc.realName,
        operatorName: acc.operatorName,
        ...costs,
      }
    })
  } catch {
    accountRows.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handlePlatformChange = () => {
  pagination.pageNo = 1
  loadData()
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

const handleExport = () => {
  const columns = [
    { key: 'accountName', label: '账号名称' },
    { key: 'platformName', label: '平台' },
    { key: 'purchaseCost', label: '采购成本' },
    { key: 'processCost', label: '过程成本' },
    { key: 'totalCost', label: '总成本' },
    { key: 'realName', label: '实名人' },
    { key: 'operatorName', label: '运营人员' },
  ]
  const exportData = accountRows.value.map((row) => ({
    ...row,
    platformName: row.platformName || row.platformType || '',
    purchaseCost: `¥${formatMoney(row.purchaseCost)}`,
    processCost: `¥${formatMoney(row.processCost)}`,
    totalCost: `¥${formatMoney(row.totalCost)}`,
  }))
  exportToExcel(exportData, columns, '账号成本')
}

const loadAccountCosts = (accountId: number) => {
  return allCosts.value.filter((c) => c.accountId === accountId)
}

const handleView = (row: AccountCostRow) => {
  currentAccount.value = row
  currentCostDetails.value = loadAccountCosts(row.id)
  viewVisible.value = true
}

const buildViewQuery = () => {
  if (!currentAccount.value || viewDateRange.value.length < 2) return null
  return {
    accountId: currentAccount.value.id,
    startDate: viewDateRange.value[0],
    endDate: viewDateRange.value[1],
  }
}

const drawViewTrendChart = (
  points: Array<{ statDate?: string; revenue?: number; cost?: number }>,
  retry = 0,
) => {
  if (!points.length) {
    viewTrendChart?.clear()
    return
  }
  if (!viewTrendChartRef.value) return
  const el = viewTrendChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    if (retry < 12) setTimeout(() => drawViewTrendChart(points, retry + 1), 100)
    return
  }
  try {
    if (!viewTrendChart) viewTrendChart = echarts.init(el)
    const dates = points.map((p) => p.statDate || '')
    const revenues = points.map((p) => toNum(p.revenue))
    const costs = points.map((p) => toNum(p.cost))
    viewTrendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['营收', '成本'], top: 0 },
      grid: { left: 56, right: 16, top: 36, bottom: 28 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', axisLabel: { formatter: (v: number) => `¥${v}` } },
      series: [
        { name: '营收', type: 'line', smooth: true, data: revenues, itemStyle: { color: '#409eff' } },
        { name: '成本', type: 'line', smooth: true, data: costs, itemStyle: { color: '#f56c6c' } },
      ],
    })
  } catch (e) {
    console.warn('[AccountCostManage] trend chart render failed', e)
    viewTrendChart?.clear()
  }
}

const drawViewStructureChart = (
  items: Array<{ type?: string; typeLabel: string; amount: number }>,
  retry = 0,
) => {
  const chartData = items.filter((item) => toNum(item.amount) > 0)
  if (!chartData.length) {
    viewStructureChart?.clear()
    return
  }
  if (!viewStructureChartRef.value) return
  const el = viewStructureChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    if (retry < 12) setTimeout(() => drawViewStructureChart(items, retry + 1), 100)
    return
  }
  try {
    if (!viewStructureChart) viewStructureChart = echarts.init(el)
    viewStructureChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'middle' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['62%', '50%'],
          data: chartData.map((item) => ({
            name: item.typeLabel || item.type || '—',
            value: toNum(item.amount),
          })),
          emphasis: { itemStyle: { shadowBlur: 8, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' } },
        },
      ],
    })
  } catch (e) {
    console.warn('[AccountCostManage] structure chart render failed', e)
    viewStructureChart?.clear()
  }
}

const loadViewAnalytics = async () => {
  const q = buildViewQuery()
  if (!q) return
  viewAnalyticsLoading.value = true
  resetViewStats()
  try {
    const [analysisRes, trendRes, followerRes, breakdownRes] = await Promise.all([
      safeApiCall('roi analysis', () => getRoiAnalysis({ ...q, dimension: 'ACCOUNT' }), null),
      safeApiCall('roi trend', () => getRoiTrend(q), null),
      safeApiCall('follower stats', () => getFollowerStats({
        accountId: q.accountId,
        startDate: q.startDate,
        endDate: q.endDate,
      }), null),
      safeApiCall('roi breakdown', () => getRoiBreakdown(q), null),
    ])

    const analysis = unwrapApiData(analysisRes) as {
      totalRevenue?: number
      totalCost?: number
      roi?: number
    } | null
    if (analysis) {
      viewStats.totalRevenue = toNum(analysis.totalRevenue)
      viewStats.totalCost = toNum(analysis.totalCost)
      viewStats.totalProfit = viewStats.totalRevenue - viewStats.totalCost
      viewStats.roi = toNum(analysis.roi)
      viewStats.grossMargin = viewStats.totalRevenue > 0
        ? (viewStats.totalProfit / viewStats.totalRevenue) * 100
        : 0
    }

    const follower = unwrapApiData(followerRes) as {
      totalFollowers?: number
      newFollowers?: number
      netFollowers?: number
      growthRate?: number
    } | null
    if (follower) {
      viewStats.totalFollowers = toNum(follower.totalFollowers)
      viewStats.newFollowers = toNum(follower.newFollowers)
      viewStats.netFollowers = toNum(follower.netFollowers)
      viewStats.growthRate = toNum(follower.growthRate)
    }

    const perFollower = viewStats.totalFollowers > 0
      ? viewStats.totalRevenue / viewStats.totalFollowers
      : 0
    viewStats.ltv = perFollower
    viewStats.arpu = perFollower

    const adSpend = sumAdSpendInRange(q.accountId, q.startDate, q.endDate)
    viewStats.cac = viewStats.newFollowers > 0 ? adSpend / viewStats.newFollowers : 0
    viewStats.ltvCacRatio = viewStats.cac > 0 ? viewStats.ltv / viewStats.cac : 0

    const breakdown = unwrapApiData(breakdownRes) as {
      byType?: Array<{ type?: string; typeLabel?: string; amount?: number; percentage?: number }>
    } | null
    costStructureItems.value = (breakdown?.byType ?? [])
      .map((item) => ({
        type: item.type || '',
        typeLabel: COST_TYPE_LABELS[item.type || ''] || item.typeLabel || item.type || '—',
        amount: toNum(item.amount),
        percentage: toNum(item.percentage),
      }))
      .filter((item) => item.amount > 0)

    if (currentAccount.value) {
      currentCostDetails.value = loadAccountCosts(currentAccount.value.id).filter((c) =>
        isInDateRange(c.payDate, q.startDate, q.endDate),
      )
    } else {
      currentCostDetails.value = []
    }

    const trendVo = unwrapApiData(trendRes) as {
      points?: Array<{ statDate?: string; date?: string; revenue?: number; cost?: number }>
    } | null
    viewTrendPoints.value = normalizeTrendPoints(trendVo?.points)

    await nextTick()
    drawViewTrendChart(viewTrendPoints.value)
    drawViewStructureChart(costStructureItems.value)
  } catch (e) {
    console.error('[AccountCostManage] view analytics failed', e)
    resetViewStats()
    if (currentAccount.value) {
      currentCostDetails.value = loadAccountCosts(currentAccount.value.id)
    }
    ElMessage.warning('部分分析数据暂无，已展示空状态')
  } finally {
    viewAnalyticsLoading.value = false
  }
}

const onViewDrawerOpened = () => {
  loadViewAnalytics()
}

const disposeViewChart = () => {
  viewTrendChart?.dispose()
  viewTrendChart = null
  viewStructureChart?.dispose()
  viewStructureChart = null
  resetViewStats()
}

const handleViewResize = () => {
  viewTrendChart?.resize()
  viewStructureChart?.resize()
}

const handleManageCost = (row: AccountCostRow) => {
  currentAccount.value = row
  const costs = loadAccountCosts(row.id)
  const purchase = costs.find((c) => isPurchase(c.costType))
  Object.assign(purchaseForm, {
    id: purchase?.id,
    amount: purchase ? Number(purchase.amount) : 0,
    payDate: purchase?.payDate || dayjs().format('YYYY-MM-DD'),
    payMethod: purchase?.payMethod || 'WECHAT',
    remark: purchase?.remark || '',
  })
  processCosts.value = costs.filter((c) => !isPurchase(c.costType))
  manageVisible.value = true
}

const savePurchaseCost = async () => {
  if (!currentAccount.value) return
  if (!purchaseForm.amount || purchaseForm.amount < 0.01) {
    ElMessage.warning('请输入有效采购成本金额')
    return
  }
  const payload = {
    accountId: currentAccount.value.id,
    costType: PURCHASE_TYPE,
    amount: purchaseForm.amount,
    payMethod: purchaseForm.payMethod,
    payDate: purchaseForm.payDate,
    period: 'ONCE',
    remark: purchaseForm.remark || undefined,
  }
  if (purchaseForm.id) {
    await updateCost({ id: purchaseForm.id, ...payload })
  } else {
    const id = await createCost(payload)
    purchaseForm.id = Number(id)
  }
  ElMessage.success('采购成本已保存')
  await loadData()
  if (currentAccount.value) {
    handleManageCost(accountRows.value.find((r) => r.id === currentAccount.value!.id) || currentAccount.value)
  }
}

const openProcessForm = (row?: CostRecord) => {
  if (row) {
    processFormTitle.value = '编辑过程成本'
    Object.assign(processForm, {
      id: row.id,
      costType: row.costType,
      amount: Number(row.amount),
      payDate: row.payDate || dayjs().format('YYYY-MM-DD'),
      payMethod: row.payMethod || 'WECHAT',
      period: row.period || 'MONTH',
      remark: row.remark || '',
    })
  } else {
    processFormTitle.value = '新增过程成本'
    Object.assign(processForm, {
      id: undefined,
      costType: 'PROCESS_HUMAN',
      amount: 0,
      payDate: dayjs().format('YYYY-MM-DD'),
      payMethod: 'WECHAT',
      period: 'MONTH',
      remark: '',
    })
  }
  processFormVisible.value = true
}

const submitProcessCost = async () => {
  if (!processFormRef.value || !currentAccount.value) return
  await processFormRef.value.validate()
  if (processForm.costType === PURCHASE_TYPE) {
    ElMessage.warning('过程成本不能选择采购类型')
    return
  }
  const payload = {
    accountId: currentAccount.value.id,
    costType: processForm.costType,
    amount: processForm.amount,
    payMethod: processForm.payMethod,
    payDate: processForm.payDate,
    period: processForm.period,
    remark: processForm.remark || undefined,
  }
  if (processForm.id) {
    await updateCost({ id: processForm.id, ...payload })
  } else {
    await createCost(payload)
  }
  ElMessage.success('过程成本已保存')
  processFormVisible.value = false
  await loadData()
  if (currentAccount.value) {
    handleManageCost(accountRows.value.find((r) => r.id === currentAccount.value!.id) || currentAccount.value)
  }
}

const removeProcessCost = async (row: CostRecord) => {
  await ElMessageBox.confirm('确认删除该过程成本记录？', '提示', { type: 'warning' })
  await deleteCost(row.id)
  ElMessage.success('已删除')
  await loadData()
  if (currentAccount.value) {
    handleManageCost(accountRows.value.find((r) => r.id === currentAccount.value!.id) || currentAccount.value)
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleViewResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleViewResize)
  disposeViewChart()
})
</script>

<style scoped>
.account-cost-page { padding: 20px; }
.platform-tabs { margin-bottom: 16px; }
.amount-text { color: #f56c6c; font-weight: 600; }
.cost-section { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.view-filter { margin-bottom: 8px; }
.metric-hint { margin-bottom: 12px; }
.stat-grid-5 {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}
@media (max-width: 900px) {
  .stat-grid-5 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
.view-stats-secondary { margin-bottom: 8px; }
.stat-card { text-align: center; }
.stat-value {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
}
.stat-value.primary { color: #409eff; }
.stat-value.danger { color: #f56c6c; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
.view-trend-chart { width: 100%; height: 320px; margin-bottom: 8px; }
.view-structure-chart { width: 100%; height: 300px; margin-bottom: 8px; }
.empty-hint { color: #909399; text-align: center; padding: 24px 0; font-size: 13px; }
</style>
