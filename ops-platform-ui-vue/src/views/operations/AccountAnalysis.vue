<template>
  <div class="account-analysis-page">
    <!-- 平台Tab -->
    <el-tabs v-model="activePlatform" @tab-click="handleTabChange" class="platform-tabs">
      <el-tab-pane label="全部" :name="PlatformType.ALL" />
      <el-tab-pane label="公众号" :name="PlatformType.WECHAT_MP" />
      <el-tab-pane label="视频号" :name="PlatformType.VIDEO_ACCOUNT" />
      <el-tab-pane label="抖音" :name="PlatformType.DOUYIN" />
      <el-tab-pane label="快手" :name="PlatformType.KUAISHOU" />
      <el-tab-pane label="小红书" :name="PlatformType.XIAOHONGSHU" />
      <el-tab-pane label="服务号" :name="PlatformType.SERVICE_ACCOUNT" />
      <el-tab-pane label="企微" :name="PlatformType.WECHAT_WORK" />
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
              {{ row.platformName }}
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
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAccountList } from '@/api/account'
import { mockGetAccountList, mockGetFollowerTrend, mockAccountList } from '@/mock/account'
import { PlatformType, AccountStatus } from '@/types/account'
import type { AccountQuery } from '@/types/account'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { exportToExcel } from '@/utils'

// 暴露枚举供模板使用
const PlatformTypeEnum = PlatformType
const AccountStatusEnum = AccountStatus

// ==================== 响应式数据 ====================

// 当前平台Tab
const activePlatform = ref<PlatformType>(PlatformType.ALL)

// 搜索表单
const searchForm = reactive({
  ipGroupId: undefined as number | undefined,
  keyword: '',
  accountStatus: undefined as AccountStatus | undefined,
})

// 加载状态
const loading = ref(false)

// 表格数据 - 初始值使用Mock数据
const tableData = ref<any[]>([...mockAccountList])

// 分页参数
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

// 趋势对比对话框
const trendDialogVisible = ref(false)
const trendChartRef = ref<HTMLElement>()

// ==================== 方法 ====================

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params: AccountQuery = {
      ipGroupId: searchForm.ipGroupId,
      keyword: searchForm.keyword || undefined,
      accountStatus: searchForm.accountStatus,
      platformType: activePlatform.value === PlatformType.ALL ? undefined : activePlatform.value,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    }

    const result = await getAccountList(params).catch(() => {
      return mockGetAccountList(pagination.pageNo, pagination.pageSize, activePlatform.value)
    })

    // 确保使用Mock数据作为降级方案
    const mockResult = mockGetAccountList(pagination.pageNo, pagination.pageSize, activePlatform.value)
    tableData.value = result?.list?.length ? result.list : mockResult.list
    pagination.total = result?.total || mockResult.total
  } catch (error) {
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// Tab切换
const handleTabChange = () => {
  pagination.pageNo = 1
  loadData()
}

// 搜索
const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.ipGroupId = undefined
  searchForm.keyword = ''
  searchForm.accountStatus = undefined
  pagination.pageNo = 1
  loadData()
}

// 导出
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

// 查看详情
const handleViewDetail = (row: any, tab: string) => {
  ElMessage.info(`查看${row.accountName}的${tab === 'follower' ? '粉丝' : '作品'}详情`)
  // TODO: router.push(`/analysis/account/${row.id}/detail?tab=${tab}`)
}

// 获取平台标签类型
const getPlatformTagType = (platformType: PlatformType) => {
  const types: Record<PlatformType, string> = {
    [PlatformType.ALL]: '',
    [PlatformType.WECHAT_MP]: 'success',
    [PlatformType.VIDEO_ACCOUNT]: 'primary',
    [PlatformType.DOUYIN]: 'danger',
    [PlatformType.KUAISHOU]: 'warning',
    [PlatformType.XIAOHONGSHU]: '',
    [PlatformType.SERVICE_ACCOUNT]: 'info',
    [PlatformType.WECHAT_WORK]: 'success',
  }
  return types[platformType] || ''
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
  
  // 获取Mock数据
  const trendData = mockGetFollowerTrend(1001)
  
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
        data: trendData.map(d => d.followerCount),
        smooth: true,
      },
      {
        name: '新增粉丝',
        type: 'bar',
        yAxisIndex: 1,
        data: trendData.map(d => d.newFollowers),
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
