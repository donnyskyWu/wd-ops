<template>
  <div class="fans-analysis-page">
    <!-- 筛选区（spec: S-R2-Fix-2 导出按钮与查询同行；时间维度默认全部）-->
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
        <DictSelect v-model="searchForm.dimension" dict-type="dict_time_dimension" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label=" ">
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </el-form-item>
      <el-form-item label="快速范围">
        <el-radio-group v-model="searchForm.quickRange" @change="handleQuickRangeChange">
          <el-radio-button label="7d">近 7 日</el-radio-button>
          <el-radio-button label="30d">近 30 日</el-radio-button>
          <el-radio-button label="custom">自定义</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </TableSearch>

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
            <div class="stat-rate">增长率 {{ (stats.growthRate || 0).toFixed(2) }}%</div>
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
        <el-table-column prop="statDate" label="时间" width="120" />
        <el-table-column prop="accountName" label="账号名称" min-width="150" />
        <el-table-column prop="ipGroupName" label="所属IP组" width="100" />
        <el-table-column prop="followerCount" label="粉丝总数" width="120" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.followerCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="newFollower" label="新增" width="90" align="right">
          <template #default="{ row }">
            <span class="text-success">+{{ row.newFollower || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unfollowCount" label="取消" width="90" align="right">
          <template #default="{ row }">
            <span class="text-danger">-{{ row.unfollowCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="netGrowth" label="净增" width="90" align="right">
          <template #default="{ row }">
            <span class="text-primary">+{{ row.netGrowth || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="growthRate" label="增长率" width="100" align="right">
          <template #default="{ row }">
            {{ (row.growthRate || 0).toFixed(2) }}%
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
// P-GATE-UNMOCK-R S-R2-C：粉丝分析接真 API，去 mock
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getFollowerTrend, getFollowerList, getFollowerStats, exportFollowerAnalysis } from '@/api/follower'
import type { FollowerQuery, FollowerStats } from '@/types/follower'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { normalizePlatform, normalizeTimeDimension } from '@/utils/enum-alias'

// ==================== 响应式数据 ====================

const searchForm = reactive({
  ipGroupId: undefined as number | undefined,
  platformType: '',
  dateRange: getDefaultDateRange(),
  // S-R2-Fix-2：spec 要求时间维度默认"全部"（空字符串 = 不传 dim 给后端）
  dimension: '' as string,
  // S-R6-B7：快速范围（7日/30日/自定义）默认 30d
  quickRange: '30d' as '7d' | '30d' | 'custom',
})

// 初始化为空（real-or-empty 策略）
const stats = ref<FollowerStats>({ totalFollowers: 0, newFollowers: 0, unfollowers: 0, netFollowers: 0, growthRate: 0 })

const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const trendChartRef = ref<HTMLElement>()

function getDefaultDateRange(): string[] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
}

function getDateRangeByDays(days: number): string[] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - (days - 1))
  return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
}

/**
 * S-R6-B7：快速范围切换（7日/30日/自定义）。
 * - 7d/30d：自动覆盖 dateRange 为近 N 天
 * - custom：保留当前 dateRange 供用户手动修改
 */
const handleQuickRangeChange = (val: string | number | boolean | undefined) => {
  const v = String(val)
  if (v === '7d') {
    searchForm.dateRange = getDateRangeByDays(7)
    handleSearch()
  } else if (v === '30d') {
    searchForm.dateRange = getDateRangeByDays(30)
    handleSearch()
  }
  // 'custom' 不自动触发，等用户改 dateRange 后点"搜索"
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params: FollowerQuery = {
      ipGroupId: searchForm.ipGroupId,
      // S-R3：粉丝 controller 收 platformType（不是 platform）；用 normalize 转换
      platformType: normalizePlatform(searchForm.platformType),
      startDate: searchForm.dateRange?.[0],
      endDate: searchForm.dateRange?.[1],
      // S-R6-B2/B5：归一化时间维度（小写 day/week/month → 大写 DAY/WEEK/MONTH）
      dimension: normalizeTimeDimension(searchForm.dimension),
      // S-R3：后端收 page/size 不是 pageNo/size
      page: pagination.pageNo,
      size: pagination.pageSize,
    } as any

    // 列表 + 趋势 + 聚合 stats 并行请求
    const [listResult, trendData, statsResult] = await Promise.all([
      getFollowerList(params),
      getFollowerTrend(params).catch(() => []),
      // S-R6-B1+B4：聚合 stats 走独立端点（不再前端 list.reduce 累加 + list[0].growthRate）
      getFollowerStats({ ...params, page: undefined, size: undefined } as any).catch(() => null),
    ])
    const list = listResult?.list || []
    tableData.value = list
    pagination.total = listResult?.total ?? 0

    // stats：后端聚合
    if (statsResult) {
      stats.value = {
        totalFollowers: Number(statsResult.totalFollowers ?? 0),
        newFollowers: Number(statsResult.newFollowers ?? 0),
        unfollowers: Number(statsResult.unfollowers ?? 0),
        netFollowers: Number(statsResult.netFollowers ?? 0),
        growthRate: Number(statsResult.growthRate ?? 0),
      }
    } else {
      stats.value = { totalFollowers: 0, newFollowers: 0, unfollowers: 0, netFollowers: 0, growthRate: 0 }
    }

    // 趋势图（独立端点）
    await renderTrendChart(trendData)
  } catch (error) {
    console.error('[FansAnalysis] 加载失败:', error)
    ElMessage.error('粉丝数据加载失败：' + (error instanceof Error ? error.message : String(error)))
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 渲染趋势图
let fansTrendChart: echarts.ECharts | null = null
const renderTrendChart = async (trendData: any[]) => {
  await nextTick()

  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(() => renderTrendChart(trendData), 100)
    return
  }
  if (fansTrendChart) {
    fansTrendChart.dispose()
    fansTrendChart = null
  }

  const chart = echarts.init(trendChartRef.value)
  fansTrendChart = chart

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增粉丝', '取消关注', '净增粉丝'] },
    xAxis: {
      type: 'category',
      data: trendData.map((d: any) => d.timePeriod || d.date),
      axisLabel: { rotate: 45 },
    },
    yAxis: { type: 'value' },
    series: [
      { name: '新增粉丝', type: 'bar', data: trendData.map((d: any) => d.newFollower ?? 0), itemStyle: { color: '#67C23A' } },
      { name: '取消关注', type: 'bar', data: trendData.map((d: any) => d.unfollowCount ?? 0), itemStyle: { color: '#F56C6C' } },
      { name: '净增粉丝', type: 'line', data: trendData.map((d: any) => d.netGrowth ?? 0), smooth: true, itemStyle: { color: '#409EFF' } },
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
  // S-R2-Fix-2：reset 也走"全部"
  searchForm.dimension = ''
  // S-R6-B7：reset 回到默认 30d
  searchForm.quickRange = '30d'
  pagination.pageNo = 1
  loadData()
}

// S-R6-B3：导出调后端 /export 端点（替代前端 exportToExcel mock）
const handleExport = async () => {
  try {
    const params: FollowerQuery = {
      ipGroupId: searchForm.ipGroupId,
      platformType: normalizePlatform(searchForm.platformType),
      startDate: searchForm.dateRange?.[0],
      endDate: searchForm.dateRange?.[1],
      dimension: normalizeTimeDimension(searchForm.dimension),
    } as any
    const blob = await exportFollowerAnalysis(params)
    // 从 Content-Disposition 解析后端给的文件名；缺失则用兜底名
    const disposition = (blob as any)?.type || ''
    const filename = `follower_analysis_${new Date().toISOString().replace(/[-:T.Z]/g, '').slice(0, 14)}.csv`
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success(`导出成功：${filename}`)
  } catch (error) {
    console.error('[FansAnalysis] 导出失败:', error)
    ElMessage.error('导出失败：' + (error instanceof Error ? error.message : String(error)))
  }
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
