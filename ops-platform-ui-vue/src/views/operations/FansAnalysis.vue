<template>
  <div class="fans-analysis-page">
    <!-- 筛选区 -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="IP组">
        <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
      </el-form-item>
      <el-form-item label="平台">
        <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="时间维度">
        <DictSelect v-model="searchForm.dimension" dict-type="dict_time_dimension" />
      </el-form-item>
    </TableSearch>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">粉丝总数</div>
            <div class="stat-value">{{ formatNumber(stats.totalFollowers) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">新增粉丝</div>
            <div class="stat-value text-success">+{{ formatNumber(stats.newFollowers) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">取消关注</div>
            <div class="stat-value text-danger">-{{ formatNumber(stats.unfollowers) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">净增粉丝</div>
            <div class="stat-value text-primary">+{{ formatNumber(stats.netFollowers) }}</div>
            <div class="stat-rate">增长率 {{ ((stats.growthRate || 0) * 100).toFixed(2) }}%</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 内容区 -->
    <ContentWrap>
      <!-- 粉丝增长趋势图 -->
      <el-card class="trend-chart-card">
        <template #header>
          <div class="card-header">
            <span>粉丝增长趋势</span>
          </div>
        </template>
        <div ref="trendChartRef" style="height: 350px;"></div>
      </el-card>

      <!-- 数据明细表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%; margin-top: 16px;">
        <el-table-column prop="date" label="时间" width="120" />
        <el-table-column prop="accountName" label="账号名称" min-width="150" />
        <el-table-column prop="ipGroupName" label="所属IP组" width="100" />
        <el-table-column prop="totalFollowers" label="粉丝总数" width="120" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.totalFollowers) }}
          </template>
        </el-table-column>
        <el-table-column prop="newFollowers" label="新增" width="90" align="right">
          <template #default="{ row }">
            <span class="text-success">+{{ row.newFollowers || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unfollowers" label="取消" width="90" align="right">
          <template #default="{ row }">
            <span class="text-danger">-{{ row.unfollowers || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="netFollowers" label="净增" width="90" align="right">
          <template #default="{ row }">
            <span class="text-primary">+{{ row.netFollowers || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="growthRate" label="增长率" width="100" align="right">
          <template #default="{ row }">
            {{ ((row.growthRate || 0) * 100).toFixed(2) }}%
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getFollowerStats, getFollowerTrend, getFollowerList } from '@/api/follower'
import { mockFollowerStats, mockFollowerTrend, mockGetFollowerList, mockFollowerList } from '@/mock/follower'
import { TimeDimension } from '@/types/follower'
import type { FollowerQuery, FollowerStats } from '@/types/follower'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { exportToExcel } from '@/utils'

// 暴露枚举供模板使用
const TimeDimensionEnum = TimeDimension

// ==================== 响应式数据 ====================

// 搜索表单
const searchForm = reactive({
  ipGroupId: undefined as number | undefined,
  platformType: '',
  dateRange: getDefaultDateRange(),
  dimension: TimeDimension.DAY,
})

// 统计数据 - 初始值使用Mock数据
const stats = ref<FollowerStats>({...mockFollowerStats})

// 加载状态
const loading = ref(false)

// 表格数据 - 初始值使用Mock数据
const tableData = ref<any[]>([...mockFollowerList])

// 分页参数 - 初始值使用Mock数据
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: mockFollowerList.length,
})

// 趋势图
const trendChartRef = ref<HTMLElement>()

// ==================== 方法 ====================

// 获取默认日期范围（近30天）
function getDefaultDateRange(): string[] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  return [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0],
  ]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ipGroupId: searchForm.ipGroupId,
      platformType: searchForm.platformType || undefined,
      startDate: searchForm.dateRange[0],
      endDate: searchForm.dateRange[1],
    }

    // 加载统计数据
    const statsData = await getFollowerStats(params).catch(() => null)
    // 确保 statsData 有效，否则使用 Mock 数据
    stats.value = statsData && typeof statsData === 'object' && 'totalFollowers' in statsData
      ? statsData
      : mockFollowerStats

    // 加载明细列表
    const listParams: FollowerQuery = {
      ...params,
      dimension: searchForm.dimension,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    }
    const mockResult = mockGetFollowerList(pagination.pageNo, pagination.pageSize)
    const listResult = await getFollowerList(listParams).catch(() => null)
    
    // 确保使用Mock数据作为降级方案
    tableData.value = listResult?.list?.length ? listResult.list : mockResult.list
    pagination.total = listResult?.total || mockResult.total

    // 渲染趋势图
    await renderTrendChart(params)
  } catch (error) {
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// 渲染趋势图
let fansTrendChart: echarts.ECharts | null = null
const renderTrendChart = async (params: any) => {
  await nextTick()

  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(() => renderTrendChart(params), 100)
    return
  }
  if (fansTrendChart) {
    fansTrendChart.dispose()
    fansTrendChart = null
  }

  // 直接使用 Mock 数据（后端接口未提供）
  const trendData = mockFollowerTrend

  const chart = echarts.init(trendChartRef.value)
  fansTrendChart = chart

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增粉丝', '取消关注', '净增粉丝'] },
    xAxis: {
      type: 'category',
      data: trendData.map(d => d.date),
      axisLabel: { rotate: 45 },
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '新增粉丝',
        type: 'bar',
        data: trendData.map(d => d.newFollowers),
        itemStyle: { color: '#67C23A' },
      },
      {
        name: '取消关注',
        type: 'bar',
        data: trendData.map(d => d.unfollowers),
        itemStyle: { color: '#F56C6C' },
      },
      {
        name: '净增粉丝',
        type: 'line',
        data: trendData.map(d => d.netFollowers),
        smooth: true,
        itemStyle: { color: '#409EFF' },
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
  searchForm.dateRange = getDefaultDateRange()
  searchForm.dimension = TimeDimension.DAY
  pagination.pageNo = 1
  loadData()
}

// 导出
const handleExport = () => {
  const columns = [
    { key: 'date', label: '时间' },
    { key: 'accountName', label: '账号名称' },
    { key: 'ipGroupName', label: '所属IP组' },
    { key: 'totalFollowers', label: '粉丝总数' },
    { key: 'newFollowers', label: '新增' },
    { key: 'unfollowers', label: '取消' },
    { key: 'netFollowers', label: '净增' },
    { key: 'growthRate', label: '增长率' },
  ]
  exportToExcel(tableData.value, columns, '粉丝数据分析')
}

// 格式化数字（千分位）
const formatNumber = (num: number | undefined | null) => {
  return (num || 0).toLocaleString('zh-CN')
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.fans-analysis-page {
  .toolbar {
    margin-bottom: 16px;
  }

  .stats-cards {
    margin-bottom: 16px;

    .stat-card {
      text-align: center;

      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-bottom: 8px;
      }

      .stat-value {
        font-size: 24px;
        font-weight: bold;
        margin-bottom: 4px;
      }

      .stat-rate {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .trend-chart-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}

.text-success {
  color: #67C23A;
}

.text-danger {
  color: #F56C6C;
}

.text-primary {
  color: #409EFF;
}
</style>
