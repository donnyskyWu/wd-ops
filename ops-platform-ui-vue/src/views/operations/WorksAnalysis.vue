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

    <!-- 筛选区 + 导出按钮 -->
    <div class="search-row">
      <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset" class="search-form">
        <el-form-item label="IP组">
          <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
        </el-form-item>
        <el-form-item label="平台">
          <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="全部" clearable />
        </el-form-item>
        <el-form-item label="内容类型">
          <DictSelect v-model="searchForm.contentType" dict-type="dict_content_type" placeholder="全部" clearable />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="作品标题" clearable maxlength="50" />
        </el-form-item>
      </TableSearch>
      <div class="export-btn">
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <!-- 内容区 -->
    <ContentWrap>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="类型" width="90" align="center" />
        <el-table-column prop="accountName" label="账号" width="140" />
        <el-table-column prop="ipGroupName" label="IP组" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="160" />
        <el-table-column prop="viewCount" label="阅读量" width="110" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.viewCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="90" align="right" />
        <el-table-column prop="commentCount" label="评论" width="90" align="right" />
        <el-table-column prop="shareCount" label="转发" width="90" align="right" />
        <el-table-column prop="isViral" label="爆款" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.isViral" class="viral-tag">🔥爆款</span>
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
    <el-dialog v-model="detailDialogVisible" :title="`作品详情 - ${currentContent.title}`" width="900px">
      <div ref="trendChartRef" style="height: 350px;"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getContentAnalysisList, getContentStats, getContentTrend } from '@/api/works'
import { mockGetContentList, mockContentStats, mockGetContentTrend, mockContentList } from '@/mock/works'
import type { ContentAnalysisQuery, ContentStats } from '@/types/works'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { exportToExcel } from '@/utils'

// ==================== 响应式数据 ====================

// 搜索表单
const searchForm = reactive({
  ipGroupId: undefined as number | undefined,
  platformType: '',
  contentType: '',
  keyword: '',
})

// 统计数据
const stats = ref<ContentStats>({
  totalPublished: 0,
  totalViews: 0,
  totalLikes: 0,
  totalComments: 0,
  totalShares: 0,
})

// 统计卡片
const statCards = computed(() => [
  { label: '发布总数', value: formatNumber(stats.value?.totalPublished || 0), color: '#409EFF' },
  { label: '阅读/播放', value: formatNumber(stats.value?.totalViews || 0), color: '#67C23A' },
  { label: '点赞总数', value: formatNumber(stats.value?.totalLikes || 0), color: '#E6A23C' },
  { label: '评论总数', value: formatNumber(stats.value?.totalComments || 0), color: '#F56C6C' },
  { label: '转发总数', value: formatNumber(stats.value?.totalShares || 0), color: '#909399' },
])

// 加载状态
const loading = ref(false)

// 表格数据 - 初始值使用Mock数据
const tableData = ref<any[]>([...mockContentList])

// 分页参数 - 初始值使用Mock数据
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: mockContentList.length,
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentContent = ref<any>({})
const trendChartRef = ref<HTMLElement>()

// ==================== 方法 ====================

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params: ContentAnalysisQuery = {
      ipGroupId: searchForm.ipGroupId,
      platformType: searchForm.platformType || undefined,
      contentType: searchForm.contentType || undefined,
      keyword: searchForm.keyword || undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    }

    // 加载统计数据
    const statsData = await getContentStats(params).catch(() => mockContentStats)
    stats.value = statsData

    // 加载列表 - 确保使用Mock数据作为降级方案
    const mockResult = mockGetContentList(pagination.pageNo, pagination.pageSize)
    const listResult = await getContentAnalysisList(params).catch(() => mockResult)
    tableData.value = listResult?.list?.length ? listResult.list : mockResult.list
    pagination.total = listResult?.total || mockResult.total
  } catch (error) {
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// 查看详情
const handleViewDetail = async (row: any) => {
  currentContent.value = row
  detailDialogVisible.value = true
  
  await nextTick()
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

  const trendData = await getContentTrend(contentId).catch(() => {
    return mockGetContentTrend(contentId)
  })

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
        data: trendData.map(d => d.viewCount),
        smooth: true,
        lineStyle: { width: 3 },
      },
      {
        name: '互动数',
        type: 'bar',
        yAxisIndex: 1,
        data: trendData.map(d => d.interactionCount),
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
  pagination.pageNo = 1
  loadData()
}

// 导出
const handleExport = () => {
  const columns = [
    { key: 'title', label: '标题' },
    { key: 'contentType', label: '类型' },
    { key: 'accountName', label: '账号' },
    { key: 'ipGroupName', label: 'IP组' },
    { key: 'publishTime', label: '发布时间' },
    { key: 'viewCount', label: '阅读量' },
    { key: 'likeCount', label: '点赞' },
    { key: 'commentCount', label: '评论' },
    { key: 'shareCount', label: '转发' },
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
</script>

<style scoped lang="scss">
.works-analysis-page {
  .stats-cards {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    overflow-x: auto;

    .stat-card-item {
      flex: 1;
      min-width: 140px;
      max-width: 200px;

      .el-card {
        text-align: center;
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

  .search-row {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 16px;

    .search-form {
      flex: 1;
    }

    .export-btn {
      padding-top: 6px;
    }
  }
}

.viral-tag {
  color: #F56C6C;
  font-weight: bold;
  font-size: 14px;
}
</style>
