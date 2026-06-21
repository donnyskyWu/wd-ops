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
      <el-form-item v-if="!isPersonalWechatTab" label="IP组">
        <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="账号名称/ID" clearable />
      </el-form-item>
      <el-form-item v-if="!isPersonalWechatTab" label="状态">
        <DictSelect v-model="searchForm.accountStatus" dict-type="dict_account_status" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item v-if="isPersonalWechatTab" label="统计日期">
        <el-date-picker
          v-model="searchForm.statDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="默认最新"
          clearable
          style="width: 160px"
        />
      </el-form-item>
    </TableSearch>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
      <el-button
        v-if="!isPersonalWechatTab"
        type="primary"
        :disabled="!selectedRows.length"
        @click="trendDialogVisible = true"
      >
        <el-icon><TrendCharts /></el-icon>
        趋势对比
      </el-button>
    </div>

    <!-- 内容区 -->
    <ContentWrap>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column v-if="!isPersonalWechatTab" type="selection" width="50" align="center" />
        <el-table-column prop="accountName" label="账号名称" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="!isPersonalWechatTab" prop="platformName" label="平台" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformTagType(row.platformType)">
              {{ getPlatformLabel(row.platformType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="!isPersonalWechatTab" prop="ipGroupName" label="所属IP组" width="100" />
        <el-table-column
          prop="followerCount"
          :label="isPersonalWechatTab ? '好友数' : '粉丝数'"
          width="110"
          align="right"
        >
          <template #default="{ row }">
            {{ formatNumber(row.followerCount) }}
          </template>
        </el-table-column>
        <template v-if="isPersonalWechatTab">
          <el-table-column prop="messagesSent" label="发送消息" width="100" align="right">
            <template #default="{ row }">{{ formatNumber(row.messagesSent ?? 0) }}</template>
          </el-table-column>
          <el-table-column prop="messagesReceived" label="接收消息" width="100" align="right">
            <template #default="{ row }">{{ formatNumber(row.messagesReceived ?? 0) }}</template>
          </el-table-column>
          <el-table-column prop="statDate" label="统计日期" width="120" align="center">
            <template #default="{ row }">{{ row.statDate || '--' }}</template>
          </el-table-column>
          <el-table-column prop="collectStatus" label="采集状态" width="100" align="center">
            <template #default="{ row }">
              <DictLabel dict-type="dict_collect_status" :value="row.collectStatus || 'PENDING'" />
            </template>
          </el-table-column>
        </template>
        <el-table-column v-else prop="contentCount" label="内容数" width="90" align="center" />
        <el-table-column v-if="!isPersonalWechatTab" prop="accountStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.accountStatus === AccountStatus.ENABLED ? 'success' : 'info'">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="!isPersonalWechatTab" prop="realName" label="实名人" width="100" />
        <el-table-column v-if="!isPersonalWechatTab" prop="operatorName" label="运营人员" width="100" />
        <el-table-column v-if="!isPersonalWechatTab" label="操作" width="180" align="center" fixed="right">
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
    <el-dialog v-model="trendDialogVisible" title="趋势对比" width="900px" @opened="renderTrendChart">
      <el-alert
        :title="trendHint"
        :type="selectedRows.length ? 'info' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-form inline style="margin-bottom: 12px;">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="trendDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="renderTrendChart">刷新图表</el-button>
        </el-form-item>
      </el-form>
      <div ref="trendChartRef" style="height: 400px;"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// P-GATE-UNMOCK-R S-R2-B：去 mock + 跳真实详情
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
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
import DictLabel from '@/components/DictLabel.vue'
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
  accountStatus: undefined as 'NORMAL' | 'DISABLED' | undefined,
  statDate: undefined as string | undefined,
})
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const trendDialogVisible = ref(false)
const trendChartRef = ref<HTMLElement>()
const selectedRows = ref<any[]>([])
const trendDateRange = ref<string[]>(getDefaultTrendRange())

function getDefaultTrendRange(): string[] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
}

const trendHint = computed(() =>
  selectedRows.length
    ? `已选 ${selectedRows.length} 个账号，对比粉丝净增趋势（请在列表勾选账号）`
    : '请先在列表勾选 1~5 个账号，再打开趋势对比'
)

const isPersonalWechatTab = computed(() => activePlatform.value === 'WECHAT_PERSONAL')

const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
}

// ==================== 方法 ====================

const loadData = async () => {
  loading.value = true
  try {
    const params: AccountQuery = {
      ipGroupId: searchForm.ipGroupId,
      keyword: searchForm.keyword || undefined,
      accountStatus: normalizeAccountStatus(searchForm.accountStatus),
      platform: normalizePlatform(activePlatform.value),
      page: pagination.pageNo,
      size: pagination.pageSize,
    } as any
    if (isPersonalWechatTab.value && searchForm.statDate) {
      ;(params as any).statDate = searchForm.statDate
    }

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
  searchForm.statDate = undefined
  pagination.pageNo = 1
  loadData()
}

const handleExport = () => {
  const columns = isPersonalWechatTab.value
    ? [
        { key: 'accountName', label: '账号名称' },
        { key: 'followerCount', label: '好友数' },
        { key: 'messagesSent', label: '发送消息' },
        { key: 'messagesReceived', label: '接收消息' },
        { key: 'statDate', label: '统计日期' },
        { key: 'collectStatus', label: '采集状态' },
      ]
    : [
        { key: 'accountName', label: '账号名称' },
        { key: 'platformName', label: '平台' },
        { key: 'ipGroupName', label: '所属IP组' },
        { key: 'followerCount', label: '粉丝数' },
        { key: 'contentCount', label: '内容数' },
        { key: 'statusText', label: '状态' },
        { key: 'realName', label: '实名人' },
        { key: 'operatorName', label: '运营人员' },
      ]
  exportToExcel(tableData.value, columns, isPersonalWechatTab.value ? '个微数据分析' : '账号数据分析')
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
    WECHAT_PERSONAL: 'success',
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

  const accounts = selectedRows.value.slice(0, 5)
  if (!accounts.length) {
    chart.setOption({
      title: { text: '请先在列表勾选账号', left: 'center', top: 'middle', textStyle: { color: '#909399', fontSize: 14 } },
    })
    return
  }

  const [startDate, endDate] = trendDateRange.value?.length === 2 ? trendDateRange.value : getDefaultTrendRange()
  const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']

  const seriesList = await Promise.all(
    accounts.map(async (row, idx) => {
      const accountId = row.accountId ?? row.id
      const trendData = await getFollowerTrend({
        startDate,
        endDate,
        accountId,
        ipGroupId: searchForm.ipGroupId,
        platformType: normalizePlatform(activePlatform.value),
      } as any).catch(() => [] as any[])
      const pointMap = new Map<string, number>()
      trendData.forEach((d: any) => {
        const key = d.timePeriod || d.date
        if (key) pointMap.set(key, Number(d.netGrowth ?? d.newFollower ?? 0))
      })
      return {
        name: row.accountName || `账号${accountId}`,
        color: palette[idx % palette.length],
        pointMap,
      }
    })
  )

  const dates = Array.from(new Set(seriesList.flatMap(s => [...s.pointMap.keys()]))).sort()
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: seriesList.map(s => s.name), top: 0 },
    grid: { left: 48, right: 24, top: 40, bottom: 32 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '净增粉丝' },
    series: seriesList.map(s => ({
      name: s.name,
      type: 'line',
      smooth: true,
      itemStyle: { color: s.color },
      data: dates.map(d => s.pointMap.get(d) ?? 0),
    })),
  }

  chart.setOption(option, true)
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})

watch(trendDialogVisible, async (val) => {
  if (val) await nextTick()
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
