<template>
  <div class="account-analysis-page">
    <!-- 平台Tab（S-R3: PlatformType 全部用 dict 真实值，名字从 PLATFORM_LABEL 拿）-->
    <el-tabs v-model="activePlatform" class="platform-tabs">
      <el-tab-pane label="全部" :name="'ALL'" />
      <el-tab-pane label="公众号" :name="'WECHAT_OFFICIAL'" />
      <el-tab-pane label="视频号" :name="'WECHAT_VIDEO'" />
      <el-tab-pane label="抖音" :name="'DOUYIN'" />
      <el-tab-pane label="快手" :name="'KUAISHOU'" />
      <el-tab-pane label="小红书" :name="'XIAOHONGSHU'" />
      <el-tab-pane label="企微" :name="'WEWORK'" />
      <!-- S-R6-TODO4：V30 补 dict_platform_type.个微=WECHAT_PERSONAL -->
      <el-tab-pane label="个微" :name="'WECHAT_PERSONAL'" />
    </el-tabs>

    <!-- 筛选区 -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="IP组">
        <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="账号名称/ID" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.accountStatus" dict-type="dict_account_status" placeholder="全部" clearable />
      </el-form-item>
    </TableSearch>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
      <el-button type="primary" @click="trendDialogVisible = true">
        <el-icon><TrendCharts /></el-icon>
        趋势对比
      </el-button>
    </div>

    <!-- 内容区 -->
    <ContentWrap>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="accountName" label="账号名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="platformName" label="平台" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformTagType(row.platformType)">
              {{ getPlatformLabel(row.platformType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipGroupName" label="所属IP组" width="100" />
        <el-table-column prop="followerCount" label="粉丝数" width="110" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.followerCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="contentCount" label="内容数" width="90" align="center" />
        <el-table-column prop="accountStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.accountStatus === AccountStatus.ENABLED ? 'success' : 'info'">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="实名人" width="100" />
        <el-table-column prop="operatorName" label="运营人员" width="100" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row, 'follower')">
              粉丝详情
            </el-button>
            <el-button link type="primary" @click="handleViewDetail(row, 'content')">
              作品详情
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

    <!-- 趋势对比对话框 -->
    <el-dialog v-model="trendDialogVisible" title="趋势对比" width="900px">
      <el-alert
        title="选择多个账号进行粉丝/内容趋势对比"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <div ref="trendChartRef" style="height: 400px;"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// P-GATE-UNMOCK-R S-R2-B：去 mock + 跳真实详情
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Download, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAccountList } from '@/api/account'
import { getAccountAnalysisList } from '@/api/account-analysis'
import { getFollowerTrend } from '@/api/follower'
import { PlatformType, AccountStatus } from '@/types/account'
import type { AccountQuery } from '@/types/account'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { exportToExcel } from '@/utils'
import { normalizePlatform, normalizeAccountStatus, PLATFORM_LABEL, type PlatformType as DictPlatform } from '@/utils/enum-alias'

const router = useRouter()

const PlatformTypeEnum = PlatformType
const AccountStatusEnum = AccountStatus

// ==================== 响应式数据 ====================

// S-R3：默认 Tab=公众号（后端 dict 真实值 WECHAT_OFFICIAL）
const activePlatform = ref<DictPlatform>('WECHAT_OFFICIAL')
const searchForm = reactive({
  ipGroupId: undefined as number | undefined,
  keyword: '',
  // S-R3：AccountStatus 用后端真实值
  accountStatus: undefined as 'NORMAL' | 'DISABLED' | undefined,
})
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const trendDialogVisible = ref(false)
const trendChartRef = ref<HTMLElement>()

// ==================== 方法 ====================

const loadData = async () => {
  loading.value = true
  try {
    const params: AccountQuery = {
      ipGroupId: searchForm.ipGroupId,
      keyword: searchForm.keyword || undefined,
      // S-R3：用 normalize 转换前端值到后端 dict 真实值
      accountStatus: normalizeAccountStatus(searchForm.accountStatus),
      // S-R3：用 normalize 转换 PlatformType（前端 ALL→后端 ALL，其他按 alias）
      platform: normalizePlatform(activePlatform.value),
      // S-R3：后端 controller 收 page/size 不是 pageNo/size
      page: pagination.pageNo,
      size: pagination.pageSize,
    } as any

    // spec: 调用 /oa/account-analysis/list（不是 /oa/account/list）
    const result = await getAccountAnalysisList(params as any)
    tableData.value = result?.list || []
    pagination.total = result?.total ?? 0
  } catch (error) {
    console.error('[AccountAnalysis] 加载失败:', error)
    ElMessage.error('账号数据加载失败：' + (error instanceof Error ? error.message : String(error)))
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => { pagination.pageNo = 1; loadData() }

// S-R3：v-model activePlatform 改变时也重新加载（@tab-click 在某些 element-plus 版本下不触发）
watch(activePlatform, () => { pagination.pageNo = 1; loadData() })
const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  searchForm.ipGroupId = undefined
  searchForm.keyword = ''
  searchForm.accountStatus = undefined
  pagination.pageNo = 1
  loadData()
}

const handleExport = () => {
  const columns = [
    { key: 'accountName', label: '账号名称' },
    { key: 'platformName', label: '平台' },
    { key: 'ipGroupName', label: '所属IP组' },
    { key: 'followerCount', label: '粉丝数' },
    { key: 'contentCount', label: '内容数' },
    { key: 'statusText', label: '状态' },
    { key: 'realName', label: '实名人' },
    { key: 'operatorName', label: '运营人员' },
  ]
  exportToExcel(tableData.value, columns, '账号数据分析')
}

// P-GATE-UNMOCK-R S-R2-B：跳真实详情（后端已补 /{id}/followers 和 /{id}/contents）
const handleViewDetail = (row: any, tab: string) => {
  const tabName = tab === 'follower' ? 'followers' : 'contents'
  const accountId = row.accountId ?? row.id
  router.push({ path: `/analysis/account/${accountId}/detail`, query: { tab: tabName } })
}

// S-R3：获取平台标签类型（用后端真实 enum 值）
const getPlatformTagType = (platformType: string) => {
  const types: Record<string, string> = {
    WECHAT_OFFICIAL: 'success',
    WECHAT_VIDEO: 'primary',
    DOUYIN: 'danger',
    KUAISHOU: 'warning',
    XIAOHONGSHU: '',
    WEWORK: 'info',
  }
  return types[platformType] || ''
}

// S-R3：显示平台中文 label
const getPlatformLabel = (platformType: string) => {
  return PLATFORM_LABEL[platformType as DictPlatform] ?? platformType
}

// 格式化数字（千分位）
const formatNumber = (num: number) => {
  return num.toLocaleString('zh-CN')
}

// 渲染趋势图
let trendChart: echarts.ECharts | null = null
const renderTrendChart = async () => {
  await nextTick()

  if (!trendChartRef.value) {
    setTimeout(renderTrendChart, 100)
    return
  }
  if (trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(renderTrendChart, 100)
    return
  }
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  const chart = echarts.init(trendChartRef.value)
  trendChart = chart

  // P-GATE-UNMOCK-R S-R2-C：真粉丝趋势（不再用 mock）
  const trendData = await getFollowerTrend({
    startDate: '', endDate: '',
  } as any).catch(() => [])

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['粉丝数', '新增粉丝'] },
    xAxis: {
      type: 'category',
      data: trendData.map(d => d.date),
    },
    yAxis: [
      { type: 'value', name: '粉丝数' },
      { type: 'value', name: '新增粉丝' },
    ],
    series: [
      {
        name: '粉丝数',
        type: 'line',
        data: trendData.map((d: any) => d.followerCount ?? 0),
        smooth: true,
      },
      {
        name: '新增粉丝',
        type: 'bar',
        yAxisIndex: 1,
        data: trendData.map((d: any) => d.newFollower ?? d.newFollowers ?? 0),
      },
    ],
  }
  
  chart.setOption(option)
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})

// 监听对话框打开
watch(trendDialogVisible, async (val) => {
  if (val) {
    await nextTick()
    renderTrendChart()
  }
})
</script>

<style scoped lang="scss">
.account-analysis-page {
  .platform-tabs {
    margin-bottom: 16px;
  }
  
  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
