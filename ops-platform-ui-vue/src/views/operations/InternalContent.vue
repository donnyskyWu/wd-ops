<template>
  <div class="internal-content-page">
    <!-- 7平台Tab -->
    <el-tabs v-model="activePlatform" @tab-change="handleTabChange" class="platform-tabs">
      <el-tab-pane v-for="platform in platforms" :key="platform.value" :label="platform.label" :name="platform.value" />
    </el-tabs>

    <div class="internal-content-search-card">
      <el-form :model="searchForm" label-width="72px" @submit.prevent="handleSearch">
        <el-row :gutter="16" class="search-row" align="middle">
          <el-col :xs="24" :sm="12" :lg="6" class="ip-group-col">
            <el-form-item label="IP组">
              <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="4">
            <el-form-item label="内容类型">
              <DictSelect v-model="searchForm.contentType" dict-type="dict_content_type" placeholder="全部" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="4">
            <el-form-item label="关键词">
              <el-input v-model="searchForm.keyword" placeholder="内容标题" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="5">
            <el-form-item label="日期范围">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :lg="5" class="search-actions-col">
            <div class="search-actions">
              <el-button type="success" :loading="exportLoading" @click="handleExport">
                <el-icon><Download /></el-icon>
                导出
              </el-button>
              <el-button type="primary" native-type="submit">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="handleReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </div>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <span class="total-info">共 {{ pagination.total }} 条</span>
      <el-button type="primary" plain @click="openMyImports">我的补录</el-button>
      <el-button type="warning" plain @click="openImportReview">补录审核</el-button>
    </div>

    <ContentWrap>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="类型" width="90" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_content_type" :value="row.contentType" />
          </template>
        </el-table-column>
        <el-table-column prop="platformType" label="平台" width="100">
          <template #default="{ row }">
            <DictLabel dict-type="dict_platform_type" :value="row.platformType" />
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="账号" width="140" />
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读量" width="100" align="right">
          <template #default="{ row }">{{ (row.readCount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewTrend(row)">趋势</el-button>
            <el-button link type="warning" @click="handleRowImport(row)">补录</el-button>
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
    </ContentWrap>

    <!-- 趋势侧抽屉 -->
    <el-drawer v-model="trendDrawerVisible" :title="`趋势分析 - ${currentContent.title}`" size="640px">
      <div class="trend-drawer-toolbar">
        <el-date-picker
          v-model="trendDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="reloadTrendChart"
        />
        <el-radio-group v-model="trendQuickRange" @change="handleTrendQuickRange">
          <el-radio-button label="7d">近 7 日</el-radio-button>
          <el-radio-button label="30d">近 30 日</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="trendChartRef" style="height: 400px;"></div>
    </el-drawer>

    <!-- 补录列表（我的补录 / 待审核） -->
    <el-dialog v-model="myImportsVisible" :title="myImportsDialogTitle" width="900px">
      <el-table v-loading="myImportsLoading" :data="myImportsList" border stripe>
        <el-table-column prop="contentTitle" label="内容标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="statDate" label="数据日期" width="120" />
        <el-table-column label="补录类型" width="120">
          <template #default="{ row }">
            {{ getImportTypeLabel(row.importType) }}
          </template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读量" width="100" align="right" />
        <el-table-column prop="likeCount" label="点赞数" width="100" align="right" />
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getReviewStatusTag(row.reviewStatus)">
              {{ getReviewStatusLabel(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitterName" label="提交人" width="120" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.reviewStatus === 0" link type="success" size="small" @click="handleReview(row, 1)">通过</el-button>
            <el-button v-if="row.reviewStatus === 0" link type="danger" size="small" @click="handleReview(row, 2)">驳回</el-button>
            <span v-else style="color: #909399; font-size: 12px">已完结</span>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :current-page="myImportsPagination.pageNo"
        :page-size="myImportsPagination.pageSize"
        :total="myImportsPagination.total"
        @update:current-page="(v) => { myImportsPagination.pageNo = v; loadMyImports() }"
        @update:page-size="(v) => { myImportsPagination.pageSize = v; loadMyImports() }"
        @change="loadMyImports"
      />
    </el-dialog>

    <!-- 数据补录对话框 -->
    <el-dialog v-model="importDialogVisible" title="数据补录" width="600px">
      <el-form :model="importForm" :rules="importRules" ref="importFormRef" label-width="100px">
        <el-form-item label="内容ID" prop="contentId">
          <el-select v-model="importForm.contentId" placeholder="搜索选择内容" filterable style="width: 100%">
            <el-option v-for="item in contentOptions" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据日期" prop="statDate">
          <el-date-picker v-model="importForm.statDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="补录类型" prop="importType">
          <DictSelect v-model="importForm.importType" dict-type="dict_content_import_type" placeholder="请选择补录类型" />
        </el-form-item>
        <el-form-item label="阅读量">
          <el-input-number v-model="importForm.readCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="点赞数">
          <el-input-number v-model="importForm.likeCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评论数">
          <el-input-number v-model="importForm.commentCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="转发数">
          <el-input-number v-model="importForm.forwardCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="补录原因">
          <el-input v-model="importForm.remark" type="textarea" :rows="3" placeholder="可选，200字内" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleImportSubmit">提交补录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// P-GATE-UNMOCK-R S-R2-E：去 mock 接真 API
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Download, Search, Refresh } from '@element-plus/icons-vue'
import { getInternalContentList, getInternalContentTrend, submitContentImport, getContentImportList, reviewContentImport } from '@/api/internal-content'
import { normalizePlatform } from '@/utils/enum-alias'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { exportToExcel, formatDateTime } from '@/utils'

const route = useRoute()

// 补录类型：前端不再硬编码 mapping，由 DictSelect 直接显示 dict_content_import_type 真实值
// （S-R5 P2：之前硬编码 4 个含 2 个 dict 没有的 ACCOUNT_BANNED/OTHER，删）

// S-R3：平台 Tab 用后端 dict 真实值；删除"服务号"（不是平台，是 account_type）
const activePlatform = ref('ALL')
const platforms = [
  { label: '全部', value: 'ALL' },
  { label: '公众号', value: 'WECHAT_OFFICIAL' },
  { label: '视频号', value: 'WECHAT_VIDEO' },
  { label: '抖音', value: 'DOUYIN' },
  { label: '快手', value: 'KUAISHOU' },
  { label: '小红书', value: 'XIAOHONGSHU' },
  { label: '企微', value: 'WEWORK' },
]

function getDefaultWeekRange(): string[] {
  const end = dayjs().format('YYYY-MM-DD')
  const start = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
  return [start, end]
}

const searchForm = reactive({
  keyword: '',
  contentType: undefined as string | undefined,
  dateRange: [] as string[],
  ipGroupId: undefined as number | undefined,
  // S-R7-B4：删 importType（content 列表表无此字段，importType 是 oa_content_import 表的字段）
})
const loading = ref(false)
const exportLoading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const trendDrawerVisible = ref(false)
const currentContent = ref<any>({})
const trendChartRef = ref<HTMLElement>()
const trendDateRange = ref<string[]>(getDefaultWeekRange())
const trendQuickRange = ref<'7d' | '30d' | 'custom'>('7d')

// ==================== 我的补录 / 补录审核 ====================
const myImportsVisible = ref(false)
const myImportsLoading = ref(false)
const myImportsList = ref<any[]>([])
const myImportsPagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const myImportsReviewOnly = ref(false)
const importReviewStatusFilter = ref<number | undefined>(undefined)
const myImportsDialogTitle = computed(() =>
  myImportsReviewOnly.value ? '补录审核（待处理）' : '我的补录',
)
// S-R5 P2：审核状态 + 补录类型 label 映射（与 dict_content_import_type 对齐）
const REVIEW_STATUS_MAP: Record<number, { label: string; tag: 'info' | 'success' | 'danger' }> = {
  0: { label: '待审核', tag: 'info' },
  1: { label: '已通过', tag: 'success' },
  2: { label: '已驳回', tag: 'danger' },
}
const getReviewStatusLabel = (s: number) => REVIEW_STATUS_MAP[s]?.label ?? '-'
const getReviewStatusTag = (s: number) => REVIEW_STATUS_MAP[s]?.tag ?? 'info'
const getImportTypeLabel = (t: string) => {
  // 用 backend dict 真值（之前硬编码 4 个含 2 个 dict 没有的）
  const map: Record<string, string> = {
    API_EXCEPTION: '接口异常',
    OFFLINE: '线下补录',
  }
  return map[t] ?? t ?? '-'
}

// ==================== 数据补录相关 ====================
const importDialogVisible = ref(false)
const importFormRef = ref()
const submitLoading = ref(false)
const contentOptions = ref<any[]>([])

const importForm = reactive({
  contentId: undefined as number | undefined,
  statDate: '',
  importType: '' as string,
  readCount: 0,
  likeCount: 0,
  commentCount: 0,
  forwardCount: 0,
  remark: '',
})

const importRules = {
  contentId: [{ required: true, message: '请选择内容', trigger: 'change' }],
  statDate: [{ required: true, message: '请选择数据日期', trigger: 'change' }],
  importType: [{ required: true, message: '请选择补录类型', trigger: 'change' }],
}

const handleRowImport = (row: any) => {
  importForm.contentId = row.id
  importForm.statDate = ''
  importForm.importType = ''
  importForm.readCount = 0
  importForm.likeCount = 0
  importForm.commentCount = 0
  importForm.forwardCount = 0
  importForm.remark = ''
  importDialogVisible.value = true
}

const handleImportSubmit = async () => {
  if (!importFormRef.value) return
  await importFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    // S-R5 P0：调真 API submitContentImport
    submitLoading.value = true
    try {
      const id = await submitContentImport({
        contentId: importForm.contentId,
        statDate: importForm.statDate,
        importType: importForm.importType,
        readCount: importForm.readCount || 0,
        likeCount: importForm.likeCount || 0,
        commentCount: importForm.commentCount || 0,
        forwardCount: importForm.forwardCount || 0,
        remark: importForm.remark,
      })
      ElMessage.success(`补录数据已提交，ID=${id}，等待审核`)
      importDialogVisible.value = false
      importFormRef.value?.resetFields()
      loadData()
    } catch (e: any) {
      console.error('[InternalContent] 补录提交失败:', e)
      const msg = e?.response?.data?.msg || e?.message || '未知错误'
      ElMessage.error('补录提交失败：' + msg)
    } finally {
      submitLoading.value = false
    }
  })
}

const buildListParams = (page: number, size: number) => ({
  platformType: activePlatform.value === 'ALL' ? undefined : normalizePlatform(activePlatform.value),
  contentType: searchForm.contentType || undefined,
  ipGroupId: searchForm.ipGroupId,
  keyword: searchForm.keyword || undefined,
  startDate: searchForm.dateRange?.length === 2 ? searchForm.dateRange[0] : undefined,
  endDate: searchForm.dateRange?.length === 2 ? searchForm.dateRange[1] : undefined,
  page,
  size,
} as any)

const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first: any = await getInternalContentList(buildListParams(1, exportPageSize))
  const total = first?.total ?? 0
  let rows: any[] = first?.list || []
  if (total > exportPageSize) {
    const totalPages = Math.ceil(total / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res: any = await getInternalContentList(buildListParams(page, exportPageSize))
      rows = rows.concat(res?.list || [])
    }
  }
  return rows
}

const loadData = async () => {
  loading.value = true
  try {
    const params = buildListParams(pagination.pageNo, pagination.pageSize)
    const res: any = await getInternalContentList(params)
    tableData.value = res?.list || []
    pagination.total = res?.total ?? 0
    // S-R5 P1：contentOptions 拉全量（size=200）覆盖弹窗所有可选内容
    try {
      const allRes: any = await getInternalContentList({ page: 1, size: 200 } as any)
      contentOptions.value = (allRes?.list || []).map((c: any) => ({ id: c.id, title: c.title }))
    } catch { /* 主列表已成功，contentOptions 失败不阻塞 */ }
  } catch (error) {
    console.error('[InternalContent] 加载失败:', error)
    ElMessage.error('内部内容加载失败：' + (error instanceof Error ? error.message : String(error)))
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => { pagination.pageNo = 1; pagination.total = 0; loadData() }
const handleSearch = () => { pagination.pageNo = 1; pagination.total = 0; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.contentType = undefined
  searchForm.dateRange = []
  searchForm.ipGroupId = undefined
  // S-R7-B4：删 importType reset
  pagination.pageNo = 1
  loadData()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows()
    const exportData = rows.map((row) => ({
      title: row.title,
      contentType: row.contentType,
      platformType: row.platformType,
      accountName: row.accountName,
      publishTime: row.publishTime ? formatDateTime(row.publishTime) : '',
      readCount: row.readCount ?? 0,
      likeCount: row.likeCount ?? 0,
      dataSource: row.dataSource ?? '',
      isHit: row.isHit ? '是' : '否',
    }))
    const columns = [
      { key: 'title', label: '标题' },
      { key: 'contentType', label: '类型' },
      { key: 'platformType', label: '平台' },
      { key: 'accountName', label: '账号' },
      { key: 'publishTime', label: '发布时间' },
      { key: 'readCount', label: '阅读量' },
      { key: 'likeCount', label: '点赞' },
      { key: 'dataSource', label: '数据来源' },
      { key: 'isHit', label: '是否爆款' },
    ]
    exportToExcel(exportData, columns, '内部内容分析')
  } catch (error) {
    console.error('[InternalContent] 导出失败:', error)
    ElMessage.error('导出失败：' + (error instanceof Error ? error.message : String(error)))
  } finally {
    exportLoading.value = false
  }
}

let internalTrendChart: echarts.ECharts | null = null

const handleTrendQuickRange = (val: string) => {
  if (val === '7d') {
    trendDateRange.value = getDefaultWeekRange()
  } else if (val === '30d') {
    trendDateRange.value = [
      dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ]
  }
  reloadTrendChart()
}

const reloadTrendChart = async () => {
  if (!currentContent.value?.id) return
  await renderTrendChart(currentContent.value)
}

const renderTrendChart = async (row: any) => {
  await nextTick()
  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(() => renderTrendChart(row), 100)
    return
  }
  if (internalTrendChart) {
    internalTrendChart.dispose()
    internalTrendChart = null
  }
  const chart = echarts.init(trendChartRef.value!)
  internalTrendChart = chart
  const [startDate, endDate] = trendDateRange.value || []
  try {
    const data: any = await getInternalContentTrend(row.id ?? row.contentId, { startDate, endDate })
    const series: any[] = data?.series || []
    if (series.length === 0) {
      chart.setOption({
        title: { text: '该内容暂无趋势数据', left: 'center', top: 'middle', textStyle: { color: '#909399', fontSize: 14 } },
      })
      return
    }
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['阅读量', '播放量'] },
      xAxis: { type: 'category', data: series.map((p: any) => p.date) },
      yAxis: [{ type: 'value', name: '阅读量' }, { type: 'value', name: '播放量' }],
      series: [
        { name: '阅读量', type: 'line', data: series.map((p: any) => p.readCount ?? 0), smooth: true },
        { name: '播放量', type: 'line', yAxisIndex: 1, data: series.map((p: any) => p.playCount ?? 0), smooth: true },
      ],
    })
  } catch (e) {
    ElMessage.error('趋势加载失败：' + (e instanceof Error ? e.message : String(e)))
    chart.setOption({
      title: { text: '趋势加载失败', left: 'center', top: 'middle', textStyle: { color: '#F56C6C', fontSize: 14 } },
    })
  }
}

const handleViewTrend = async (row: any) => {
  currentContent.value = row
  trendDateRange.value = getDefaultWeekRange()
  trendQuickRange.value = '7d'
  trendDrawerVisible.value = true
  await renderTrendChart(row)
}

onMounted(async () => {
  await loadData()
  const importsQuery = route.query.imports as string | undefined
  if (importsQuery === 'review' || importsQuery === '1') {
    const reviewStatus = route.query.reviewStatus != null
      ? Number(route.query.reviewStatus)
      : importsQuery === 'review' ? 0 : undefined
    await openImportReview(reviewStatus)
  }
})

// ==================== 我的补录 / 补录审核方法 ====================
const openMyImports = async () => {
  myImportsReviewOnly.value = false
  importReviewStatusFilter.value = undefined
  myImportsVisible.value = true
  myImportsPagination.pageNo = 1
  await loadMyImports()
}

const openImportReview = async (reviewStatus = 0) => {
  myImportsReviewOnly.value = true
  importReviewStatusFilter.value = reviewStatus
  myImportsVisible.value = true
  myImportsPagination.pageNo = 1
  await loadMyImports()
}

const loadMyImports = async () => {
  myImportsLoading.value = true
  try {
    const params: Record<string, unknown> = {
      page: myImportsPagination.pageNo,
      size: myImportsPagination.pageSize,
    }
    if (importReviewStatusFilter.value != null) {
      params.reviewStatus = importReviewStatusFilter.value
    }
    const res: any = await getContentImportList(params)
    myImportsList.value = res?.list || []
    myImportsPagination.total = res?.total ?? 0
  } catch (e) {
    console.error('[InternalContent] 我的补录加载失败:', e)
    ElMessage.error('我的补录加载失败')
  } finally {
    myImportsLoading.value = false
  }
}

const handleReview = async (row: any, status: 1 | 2) => {
  const action = status === 1 ? '通过' : '驳回'
  const remark = status === 2 ? (window.prompt(`请输入驳回原因（${action}补录 ID=${row.id}）`) || '') : '审核通过'
  if (status === 2 && !remark) {
    ElMessage.warning('驳回必须填写原因')
    return
  }
  try {
    await reviewContentImport(row.id, { reviewStatus: status, remark })
    ElMessage.success(`已${action}补录 ID=${row.id}`)
    await loadMyImports()
    loadData() // 刷新主列表（审核通过后 trend 数据会变）
  } catch (e: any) {
    ElMessage.error(`${action}失败：` + (e?.response?.data?.msg || e?.message))
  }
}
</script>

<style scoped lang="scss">
.internal-content-page {
  padding: 20px;
  
  .platform-tabs { margin-bottom: 16px; }

  .internal-content-search-card {
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

    .ip-group-col {
      @media (min-width: 1200px) {
        :deep(.el-select),
        :deep(.el-tree-select) {
          min-width: 220px;
        }
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
        margin: 0;
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

  .action-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }
  
  .total-info {
    color: #909399;
    font-size: 14px;
  }

  .trend-drawer-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }
}
</style>
