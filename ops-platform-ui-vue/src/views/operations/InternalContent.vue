<template>
  <div class="internal-content-page">
    <!-- 7平台Tab -->
    <el-tabs v-model="activePlatform" @tab-click="handleTabChange" class="platform-tabs">
      <el-tab-pane v-for="platform in platforms" :key="platform.value" :label="platform.label" :name="platform.value" />
    </el-tabs>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="IP组">
        <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
      </el-form-item>
      <el-form-item label="关键词"><el-input v-model="searchForm.keyword" placeholder="内容标题" clearable /></el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          clearable
        />
      </el-form-item>
      <!-- S-R7-B4：删"补录类型"——content 列表不该用 importType 筛（importType 是 oa_content_import 表的字段） -->
    </TableSearch>

    <!-- 操作栏 -->
    <div class="action-bar">
      <span class="total-info">共 {{ pagination.total }} 条</span>
      <el-button type="primary" plain @click="openMyImports">我的补录</el-button>
    </div>

    <ContentWrap>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platformName" label="平台" width="100" />
        <el-table-column prop="accountName" label="账号" width="140" />
        <el-table-column prop="publishTime" label="发布时间" width="160" />
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

    <!-- 我的补录对话框 -->
    <el-dialog v-model="myImportsVisible" title="我的补录" width="900px">
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
        <el-table-column prop="createTime" label="提交时间" width="170" />
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Plus, Upload } from '@element-plus/icons-vue'
import { getInternalContentList, getInternalContentTrend, submitContentImport, getContentImportList, reviewContentImport } from '@/api/internal-content'
import { normalizePlatform } from '@/utils/enum-alias'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'

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
  dateRange: [] as string[],
  ipGroupId: undefined as number | undefined,
  // S-R7-B4：删 importType（content 列表表无此字段，importType 是 oa_content_import 表的字段）
})
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const trendDrawerVisible = ref(false)
const currentContent = ref<any>({})
const trendChartRef = ref<HTMLElement>()
const trendDateRange = ref<string[]>(getDefaultWeekRange())
const trendQuickRange = ref<'7d' | '30d' | 'custom'>('7d')

// ==================== 我的补录 ====================
const myImportsVisible = ref(false)
const myImportsLoading = ref(false)
const myImportsList = ref<any[]>([])
const myImportsPagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
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

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      // S-R3：用 normalize 转换（前端 ALL→后端 ALL，其他按 alias）
      platformType: activePlatform.value === 'ALL' ? undefined : normalizePlatform(activePlatform.value),
      ipGroupId: searchForm.ipGroupId,
      keyword: searchForm.keyword || undefined,
      // S-R7-B4：删 importType（content 列表表无此字段）
      startDate: searchForm.dateRange?.length === 2 ? searchForm.dateRange[0] : undefined,
      endDate: searchForm.dateRange?.length === 2 ? searchForm.dateRange[1] : undefined,
      // S-R3：后端收 page/size
      page: pagination.pageNo,
      size: pagination.pageSize,
    } as any
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
  searchForm.dateRange = []
  searchForm.ipGroupId = undefined
  // S-R7-B4：删 importType reset
  pagination.pageNo = 1
  loadData()
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

onMounted(() => loadData())

// ==================== 我的补录方法 ====================
const openMyImports = async () => {
  myImportsVisible.value = true
  myImportsPagination.pageNo = 1
  await loadMyImports()
}

const loadMyImports = async () => {
  myImportsLoading.value = true
  try {
    const res: any = await getContentImportList({
      page: myImportsPagination.pageNo,
      size: myImportsPagination.pageSize,
    })
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
