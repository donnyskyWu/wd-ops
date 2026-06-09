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
        <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="补录类型">
        <DictSelect v-model="searchForm.importType" dict-type="dict_import_source" placeholder="全部" clearable />
      </el-form-item>
    </TableSearch>

    <!-- 操作栏 -->
    <div class="action-bar">
      <span class="total-info">共 {{ pagination.total }} 条</span>
    </div>

    <ContentWrap>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platformName" label="平台" width="100" />
        <el-table-column prop="accountName" label="账号" width="140" />
        <el-table-column prop="publishTime" label="发布时间" width="160" />
        <el-table-column prop="viewCount" label="阅读量" width="100" align="right">
          <template #default="{ row }">{{ row.viewCount.toLocaleString() }}</template>
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
    <el-drawer v-model="trendDrawerVisible" :title="`趋势分析 - ${currentContent.title}`" size="600px">
      <div ref="trendChartRef" style="height: 400px;"></div>
    </el-drawer>

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
          <DictSelect v-model="importForm.importType" dict-type="dict_import_source" placeholder="请选择补录类型" />
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
        <el-button type="primary" @click="handleImportSubmit">提交补录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Plus, Upload } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'

// ==================== 补录类型配置 ====================
const IMPORT_TYPE_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
  API_EXCEPTION: { label: '接口补录', color: '#E6A23C', bg: '#FDF6EC' },
  ACCOUNT_BANNED: { label: '封禁补录', color: '#F56C6C', bg: '#FEF0F0' },
  OFFLINE_PRACTICE: { label: '练习数据', color: '#909399', bg: '#F4F4F5' },
  OTHER: { label: '其他补录', color: '#E6A23C', bg: '#FAF3E0' },
}

const activePlatform = ref('ALL')
const platforms = [
  { label: '全部', value: 'ALL' },
  { label: '公众号', value: 'WECHAT_MP' },
  { label: '视频号', value: 'VIDEO_ACCOUNT' },
  { label: '抖音', value: 'DOUYIN' },
  { label: '快手', value: 'KUAISHOU' },
  { label: '小红书', value: 'XIAOHONGSHU' },
  { label: '服务号', value: 'SERVICE_ACCOUNT' },
  { label: '企微', value: 'WECHAT_WORK' },
]

const searchForm = reactive({
  keyword: '',
  dateRange: [] as string[],
  ipGroupId: undefined as number | undefined,
  importType: '' as string,
})
const loading = ref(false)
const tableData = ref([
  { id: 1, title: '内部培训资料', platformName: '公众号', accountName: '内部账号1', publishTime: '2026-05-25', viewCount: 1200 },
  { id: 2, title: '产品更新通知', platformName: '企微', accountName: '内部账号2', publishTime: '2026-05-24', viewCount: 850 },
])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 2 })
const trendDrawerVisible = ref(false)
const currentContent = ref<any>({})
const trendChartRef = ref<HTMLElement>()

// ==================== 数据补录相关 ====================
const importDialogVisible = ref(false)
const importFormRef = ref()
const contentOptions = ref<any[]>([
  { id: 1, title: '2026年知识付费趋势分析' },
  { id: 2, title: '职场进阶指南：如何提升沟通能力' },
  { id: 3, title: 'AI技术前沿：最新大模型解读' },
])

const importForm = reactive({
  contentId: undefined as number | undefined,
  statDate: '',
  importType: '' as 'API_EXCEPTION' | 'ACCOUNT_BANNED' | 'OFFLINE_PRACTICE' | 'OTHER' | '',
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
  // 预填充内容ID
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
    ElMessage.success('补录数据已提交，等待审核')
    importDialogVisible.value = false
  })
}

const loadData = () => { loading.value = false }
const handleTabChange = () => { pagination.pageNo = 1; loadData() }
const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.dateRange = []; pagination.pageNo = 1; loadData() }

let internalTrendChart: echarts.ECharts | null = null
const handleViewTrend = async (row: any) => {
  currentContent.value = row
  trendDrawerVisible.value = true
  await nextTick()
  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(() => handleViewTrend(row), 100)
    return
  }
  if (internalTrendChart) {
    internalTrendChart.dispose()
    internalTrendChart = null
  }
  const chart = echarts.init(trendChartRef.value!)
  internalTrendChart = chart
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['5-20', '5-21', '5-22', '5-23', '5-24', '5-25'] },
    yAxis: { type: 'value' },
    series: [{ name: '阅读量', type: 'line', data: [120, 200, 150, 320, 280, 400], smooth: true }],
  })
}

onMounted(() => loadData())
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
}
</style>
