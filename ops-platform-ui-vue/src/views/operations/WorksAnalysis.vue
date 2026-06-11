<template>
  <div class="works-analysis-page">
    <!-- 统计卡片（放在搜索条件最上面，横排显示） -->
    <div class="stats-cards">
      <div v-for="card in statCards" :key="card.label" class="stat-card-item">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 筛选区（导出与搜索同行） -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="IP组">
        <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
      </el-form-item>
      <el-form-item label="平台">
        <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="内容类型">
        <DictSelect v-model="searchForm.contentType" dict-type="dict_content_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          clearable
          @change="searchForm.quickRange = searchForm.dateRange?.length === 2 ? 'custom' : 'all'"
        />
      </el-form-item>
      <el-form-item label="快捷">
        <el-radio-group v-model="searchForm.quickRange" @change="handleQuickRange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="7d">近 7 日</el-radio-button>
          <el-radio-button label="30d">近 30 日</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="作品标题" clearable maxlength="50" />
      </el-form-item>
      <template #extra>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </TableSearch>

    <!-- 内容区 -->
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
        <el-table-column prop="ipGroupName" label="IP组" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读量" width="110" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.readCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="90" align="right" />
        <el-table-column prop="commentCount" label="评论" width="90" align="right" />
        <el-table-column prop="forwardCount" label="转发" width="90" align="right" />
        <el-table-column prop="isHit" label="爆款" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.isHit" class="viral-tag">🔥爆款</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => pagination.pageNo = val"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="`作品详情 - ${currentContent.title || ''}`" width="900px">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
        <el-descriptions-item label="标题" :span="2">{{ currentContent.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ currentContent.accountName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP组">{{ currentContent.ipGroupName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <DictLabel dict-type="dict_content_type" :value="currentContent.contentType" />
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatDateTime(currentContent.publishTime) }}</el-descriptions-item>
        <el-descriptions-item label="阅读量">{{ formatNumber(currentContent.readCount) }}</el-descriptions-item>
        <el-descriptions-item label="点赞">{{ formatNumber(currentContent.likeCount) }}</el-descriptions-item>
        <el-descriptions-item label="评论">{{ formatNumber(currentContent.commentCount) }}</el-descriptions-item>
        <el-descriptions-item label="转发">{{ formatNumber(currentContent.forwardCount) }}</el-descriptions-item>
        <el-descriptions-item label="爆款">{{ currentContent.isHit ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="数据来源">{{ currentContent.dataSource || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">
          {{ currentContent.summary || currentContent.description || '（正文未单独采集，展示标题与互动指标）' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">互动趋势</el-divider>
      <div class="detail-trend-toolbar">
        <el-date-picker
          v-model="detailDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDetailDateChange"
        />
        <el-radio-group v-model="detailQuickRange" @change="handleDetailQuickRange">
          <el-radio-button label="7d">近 7 日</el-radio-button>
          <el-radio-button label="30d">近 30 日</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="trendChartRef" style="height: 320px;"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getContentAnalysisList, getContentStats, getContentTrend } from '@/api/works'
import type { ContentAnalysisQuery, ContentStats } from '@/types/works'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { exportToExcel, formatDateTime } from '@/utils'
import { normalizePlatform } from '@/utils/enum-alias'

function getDefaultWeekRange(): string[] {
  const end = dayjs().format('YYYY-MM-DD')
  const start = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
  return [start, end]
}

// ==================== 响应式数据 ====================

// 搜索表单
const searchForm = reactive({
  ipGroupId: undefined as number | undefined,
  platformType: '',
  contentType: '',
  keyword: '',
  dateRange: [] as string[],
  quickRange: 'all' as 'all' | '7d' | '30d' | 'custom',
})

// 统计数据（S-R8 B1: 5 KPI 卡走 stats 端点全量聚合）
const stats = ref<ContentStats>({
  totalCount: 0,
  hitCount: 0,
  totalRead: 0,
  avgRead: 0,
  totalPublished: 0,
  totalViews: 0,
  totalLikes: 0,
  totalComments: 0,
  totalShares: 0,
})

// 统计卡片
const statCards = computed(() => [
  { label: '发布总数', value: formatNumber(stats.value?.totalCount || 0), color: '#409EFF' },
  { label: '阅读/播放', value: formatNumber(stats.value?.totalRead || 0), color: '#67C23A' },
  { label: '点赞总数', value: formatNumber(stats.value?.totalLikes || 0), color: '#E6A23C' },
  { label: '评论总数', value: formatNumber(stats.value?.totalComments || 0), color: '#F56C6C' },
  { label: '转发总数', value: formatNumber(stats.value?.totalShares || 0), color: '#909399' },
])

// S-R8 B1: 后端 ContentStatsVO 扩了 totalLikes/totalComments/totalShares，前端直接用 stats 端点全量数据，不再从 list 累加
const statFromList = computed(() => null)

// 加载状态
const loading = ref(false)

// 表格数据
const tableData = ref<any[]>([])

// 分页参数
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentContent = ref<any>({})
const trendChartRef = ref<HTMLElement>()
const detailDateRange = ref<string[]>(getDefaultWeekRange())
const detailQuickRange = ref<'7d' | '30d' | 'custom'>('7d')

// ==================== 方法 ====================

const buildDateParams = () => {
  if (searchForm.dateRange?.length !== 2) {
    return { startDate: undefined, endDate: undefined }
  }
  const [startDate, endDate] = searchForm.dateRange
  return { startDate, endDate }
}

const handleQuickRange = (val: string) => {
  if (val === 'all') {
    searchForm.dateRange = []
  } else if (val === '7d') {
    searchForm.dateRange = getDefaultWeekRange()
  } else if (val === '30d') {
    searchForm.dateRange = [
      dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ]
  }
  handleSearch()
}

const handleDetailQuickRange = (val: string) => {
  if (val === '7d') {
    detailDateRange.value = getDefaultWeekRange()
  } else if (val === '30d') {
    detailDateRange.value = [
      dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ]
  }
  if (currentContent.value?.id) {
    renderTrendChart(currentContent.value.id)
  }
}

const handleDetailDateChange = () => {
  detailQuickRange.value = 'custom'
  if (currentContent.value?.id) {
    renderTrendChart(currentContent.value.id)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const { startDate, endDate } = buildDateParams()
    const params: ContentAnalysisQuery = {
      ipGroupId: searchForm.ipGroupId,
      // S-R3：用 normalize 转换 PlatformType
      platformType: normalizePlatform(searchForm.platformType) as any,
      contentType: searchForm.contentType || undefined,
      keyword: searchForm.keyword || undefined,
      startDate,
      endDate,
      // S-R8 B6: 后端收 page/size
      page: pagination.pageNo,
      size: pagination.pageSize,
    }

    // 加载列表
    const listResult = await getContentAnalysisList(params)
    const list = listResult?.list || []
    tableData.value = list
    pagination.total = listResult?.total ?? 0

    // S-R8 B1: stats 端点直接覆盖 5 KPI（含 totalLikes/totalComments/totalShares）
    try {
      const statsData = await getContentStats(params)
      if (statsData) {
        stats.value = {
          totalCount: statsData.totalCount ?? 0,
          hitCount: statsData.hitCount ?? 0,
          totalRead: statsData.totalRead ?? 0,
          avgRead: statsData.avgRead ?? 0,
          totalPublished: statsData.totalCount ?? list.length,
          totalViews: statsData.totalRead ?? 0,
          totalLikes: statsData.totalLikes ?? 0,
          totalComments: statsData.totalComments ?? 0,
          totalShares: statsData.totalShares ?? 0,
        }
      }
    } catch (e) {
      // ignore - 卡片保持 0
    }
  } catch (error) {
    console.error('[WorksAnalysis] 加载失败:', error)
    tableData.value = []
    pagination.total = 0
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// 查看详情
const handleViewDetail = async (row: any) => {
  currentContent.value = row
  detailDateRange.value = getDefaultWeekRange()
  detailQuickRange.value = '7d'
  detailDialogVisible.value = true

  await nextTick()
  // S-R8 B3: 后端 /trend 端点收 contentId query param（不再是 path param）
  renderTrendChart(row.id)
}

// 渲染趋势图（双Y轴）
let worksTrendChart: echarts.ECharts | null = null
const renderTrendChart = async (contentId: number) => {
  await nextTick()

  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(() => renderTrendChart(contentId), 100)
    return
  }
  if (worksTrendChart) {
    worksTrendChart.dispose()
    worksTrendChart = null
  }

  const [startDate, endDate] = detailDateRange.value?.length === 2
    ? detailDateRange.value
    : getDefaultWeekRange()
  const trendData = await getContentTrend({ contentId, startDate, endDate })

  const chart = echarts.init(trendChartRef.value)
  worksTrendChart = chart

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['阅读量', '互动数'] },
    xAxis: {
      type: 'category',
      data: trendData.map(d => d.date),
    },
    yAxis: [
      { type: 'value', name: '阅读量', position: 'left' },
      { type: 'value', name: '互动数', position: 'right' },
    ],
    series: [
      {
        name: '阅读量',
        type: 'line',
        // S-R8 B3: 后端实返 readCount（不是 viewCount）
        data: trendData.map(d => d.readCount),
        smooth: true,
        lineStyle: { width: 3 },
      },
      {
        name: '互动数',
        type: 'bar',
        yAxisIndex: 1,
        // S-R8 B3: 后端实返 like+comment+forward
        data: trendData.map(d => (d.likeCount || 0) + (d.commentCount || 0) + (d.forwardCount || 0)),
        itemStyle: { color: '#67C23A' },
      },
    ],
  }

  chart.setOption(option)
}

// 搜索
const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.ipGroupId = undefined
  searchForm.platformType = ''
  searchForm.contentType = ''
  searchForm.keyword = ''
  searchForm.dateRange = []
  searchForm.quickRange = 'all'
  pagination.pageNo = 1
  loadData()
}

// 导出
const handleExport = () => {
  // S-R8 B2: 导出 columns 字段名与后端 ContentAnalysisVO 对齐
  const columns = [
    { key: 'title', label: '标题' },
    { key: 'contentType', label: '类型' },
    { key: 'platformType', label: '平台' },
    { key: 'accountName', label: '账号' },
    { key: 'ipGroupName', label: 'IP组' },
    { key: 'publishTime', label: '发布时间' },
    { key: 'readCount', label: '阅读量' },
    { key: 'likeCount', label: '点赞' },
    { key: 'commentCount', label: '评论' },
    { key: 'forwardCount', label: '转发' },
    { key: 'isHit', label: '是否爆款' },
  ]
  exportToExcel(tableData.value, columns, '作品数据分析')
}

// 格式化数字
const formatNumber = (num: number) => {
  return num.toLocaleString('zh-CN')
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})

watch(detailDialogVisible, (visible) => {
  if (!visible && worksTrendChart) {
    worksTrendChart.dispose()
    worksTrendChart = null
  }
})
</script>

<style scoped lang="scss">
.works-analysis-page {
  .stats-cards {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;

    .stat-card-item {
      min-width: 0;

      .el-card {
        text-align: center;
        height: 100%;
      }
    }

    .stat-card {
      text-align: center;

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-bottom: 6px;
      }

      .stat-value {
        font-size: 18px;
        font-weight: bold;
      }
    }
  }

}

.viral-tag {
  color: #F56C6C;
  font-weight: bold;
  font-size: 14px;
}

.detail-trend-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
