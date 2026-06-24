<template>
  <div class="wechat-data-page">
    <el-tabs v-model="activeTab" class="platform-tabs">
      <el-tab-pane label="企业微信" name="wework" />
      <el-tab-pane label="个人微信" name="personal" />
    </el-tabs>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="账号名称">
        <el-input v-model="searchForm.accountName" placeholder="模糊搜索" clearable />
      </el-form-item>
      <el-form-item label="统计日期">
        <el-date-picker
          v-model="searchForm.statDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="默认今日"
          clearable
          style="width: 160px"
        />
      </el-form-item>
    </TableSearch>

    <ContentWrap>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="accountName" label="账号名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="totalFriends" label="好友总数" width="110" align="right">
          <template #default="{ row }">{{ formatNumber(row.totalFriends) }}</template>
        </el-table-column>

        <template v-if="activeTab === 'wework'">
          <el-table-column prop="todayFriendInteractions" width="130" align="right">
            <template #header>
              <span>今日好友互动</span>
              <el-tooltip content="映射企微 API chat_cnt（有主动发消息的单聊总数）" placement="top">
                <el-icon style="margin-left: 4px; vertical-align: middle"><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
            <template #default="{ row }">{{ formatNumber(row.todayFriendInteractions) }}</template>
          </el-table-column>
          <el-table-column prop="todayMessagesSent" label="今日发消息" width="120" align="right">
            <template #default="{ row }">{{ formatNumber(row.todayMessagesSent) }}</template>
          </el-table-column>
        </template>

        <template v-else>
          <el-table-column prop="newFriends" label="今日新增好友" width="130" align="right">
            <template #default="{ row }">{{ formatNumber(row.newFriends) }}</template>
          </el-table-column>
          <el-table-column prop="messagesSent" label="今日发消息" width="120" align="right">
            <template #default="{ row }">{{ formatNumber(row.messagesSent) }}</template>
          </el-table-column>
        </template>

        <el-table-column prop="statDate" label="统计日期" width="120" align="center">
          <template #default="{ row }">{{ row.statDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详细数据</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        @update:current-page="(v) => { pagination.page = v; loadList() }"
        @update:page-size="(v) => { pagination.size = v; pagination.page = 1; loadList() }"
        @change="loadList"
      />
    </ContentWrap>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="780px" destroy-on-close @closed="handleDetailClosed">
      <el-row :gutter="16" class="detail-kpi" v-if="detailStats.length">
        <el-col v-for="kpi in detailKpis" :key="kpi.label" :span="8">
          <el-statistic :title="kpi.label" :value="kpi.value" />
        </el-col>
      </el-row>

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
        <el-radio-group v-model="detailQuickRange" @change="(val) => handleDetailQuickRange(String(val))">
          <el-radio-button label="7d">近 7 日</el-radio-button>
          <el-radio-button label="30d">近 30 日</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="trendChartRef" class="trend-chart" />

      <el-table v-loading="detailLoading" :data="detailStats" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column prop="totalFriends" label="好友总数" width="100" align="right" />
        <template v-if="activeTab === 'wework'">
          <el-table-column prop="todayFriendInteractions" label="好友互动" width="100" align="right" />
          <el-table-column prop="todayMessagesSent" label="发消息" width="100" align="right" />
          <el-table-column prop="syncedAt" label="同步时间" min-width="160" />
        </template>
        <template v-else>
          <el-table-column prop="newFriends" label="新增好友" width="100" align="right" />
          <el-table-column prop="messagesSent" label="发消息" width="90" align="right" />
          <el-table-column prop="messagesReceived" label="收消息" width="90" align="right" />
          <el-table-column prop="groupCount" label="群数量" width="90" align="right" />
        </template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import {
  getWeworkAnalysisList,
  getWeworkAnalysisDetail,
  getPersonalAnalysisList,
  getPersonalAnalysisDetail,
  type WeworkAnalysisListItem,
  type WeworkDailyStatsItem,
  type PersonalAnalysisListItem,
  type PersonalDailyStatsItem,
} from '@/api/wechat-analysis'

type AnalysisListItem = WeworkAnalysisListItem | PersonalAnalysisListItem
type DailyStatsItem = WeworkDailyStatsItem | PersonalDailyStatsItem

const activeTab = ref<'wework' | 'personal'>('wework')
const loading = ref(false)
const tableData = ref<AnalysisListItem[]>([])
const searchForm = reactive({ accountName: '', statDate: '' as string | undefined })
const pagination = reactive({ page: 1, size: 20, total: 0 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailAccountId = ref<number | null>(null)
const detailAccountName = ref('')
const detailStats = ref<DailyStatsItem[]>([])
const detailDateRange = ref<[string, string]>(getDefaultWeekRange())
const detailQuickRange = ref('7d')
const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null

const detailTitle = computed(() =>
  detailAccountName.value ? `${detailAccountName.value} · 详细数据` : '详细数据',
)

const latestStat = computed(() => detailStats.value[0])

const detailKpis = computed(() => {
  const stat = latestStat.value
  if (!stat) return []
  if (activeTab.value === 'wework') {
    const w = stat as WeworkDailyStatsItem
    return [
      { label: '好友总数', value: w.totalFriends ?? 0 },
      { label: '今日好友互动', value: w.todayFriendInteractions ?? 0 },
      { label: '今日发消息', value: w.todayMessagesSent ?? 0 },
    ]
  }
  const p = stat as PersonalDailyStatsItem
  return [
    { label: '好友总数', value: p.totalFriends ?? 0 },
    { label: '今日新增好友', value: p.newFriends ?? 0 },
    { label: '今日发消息', value: p.messagesSent ?? 0 },
  ]
})

function getDefaultWeekRange(): [string, string] {
  return [
    dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD'),
  ]
}

function getDefaultMonthRange(): [string, string] {
  return [
    dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD'),
  ]
}

function formatNumber(val?: number) {
  if (val == null) return '0'
  return val.toLocaleString()
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      accountName: searchForm.accountName || undefined,
      statDate: searchForm.statDate || undefined,
      page: pagination.page,
      size: pagination.size,
    }
    const res = activeTab.value === 'wework'
      ? await getWeworkAnalysisList(params)
      : await getPersonalAnalysisList(params)
    tableData.value = res.list ?? []
    pagination.total = res.total ?? 0
  } catch {
    ElMessage.error(activeTab.value === 'wework' ? '加载企微分析数据失败' : '加载个微分析数据失败')
  } finally {
    loading.value = false
  }
}

async function loadDetailStats() {
  if (detailAccountId.value == null) return
  detailLoading.value = true
  try {
    const [startDate, endDate] = detailDateRange.value?.length === 2
      ? detailDateRange.value
      : getDefaultWeekRange()
    const detail = activeTab.value === 'wework'
      ? await getWeworkAnalysisDetail({ accountId: detailAccountId.value, startDate, endDate })
      : await getPersonalAnalysisDetail({ accountId: detailAccountId.value, startDate, endDate })
    detailStats.value = detail.dailyStats ?? []
    await nextTick()
    renderTrendChart()
  } catch {
    ElMessage.error('加载详细数据失败')
    detailStats.value = []
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(row: AnalysisListItem) {
  detailAccountId.value = row.accountId
  detailAccountName.value = row.accountName
  detailDateRange.value = getDefaultWeekRange()
  detailQuickRange.value = '7d'
  detailVisible.value = true
  await loadDetailStats()
}

function handleDetailQuickRange(val: string) {
  if (val === '7d') {
    detailDateRange.value = getDefaultWeekRange()
  } else if (val === '30d') {
    detailDateRange.value = getDefaultMonthRange()
  }
  loadDetailStats()
}

function handleDetailDateChange() {
  detailQuickRange.value = 'custom'
  loadDetailStats()
}

function handleDetailClosed() {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  detailStats.value = []
}

function renderTrendChart() {
  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(renderTrendChart, 100)
    return
  }
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }

  const statsAsc = [...detailStats.value].reverse()
  const dates = statsAsc.map(d => d.statDate)

  let legend: string[]
  let series: echarts.SeriesOption[]

  if (activeTab.value === 'wework') {
    const wStats = statsAsc as WeworkDailyStatsItem[]
    legend = ['好友总数', '好友互动', '发消息']
    series = [
      {
        name: '好友总数',
        type: 'line',
        data: wStats.map(d => d.totalFriends ?? 0),
        smooth: true,
        lineStyle: { width: 2 },
      },
      {
        name: '好友互动',
        type: 'line',
        data: wStats.map(d => d.todayFriendInteractions ?? 0),
        smooth: true,
        lineStyle: { width: 2 },
      },
      {
        name: '发消息',
        type: 'line',
        data: wStats.map(d => d.todayMessagesSent ?? 0),
        smooth: true,
        lineStyle: { width: 2 },
      },
    ]
  } else {
    const pStats = statsAsc as PersonalDailyStatsItem[]
    legend = ['好友总数', '新增好友', '发消息', '收消息']
    series = [
      {
        name: '好友总数',
        type: 'line',
        data: pStats.map(d => d.totalFriends ?? 0),
        smooth: true,
        lineStyle: { width: 2 },
      },
      {
        name: '新增好友',
        type: 'line',
        data: pStats.map(d => d.newFriends ?? 0),
        smooth: true,
        lineStyle: { width: 2 },
      },
      {
        name: '发消息',
        type: 'line',
        data: pStats.map(d => d.messagesSent ?? 0),
        smooth: true,
        lineStyle: { width: 2 },
      },
      {
        name: '收消息',
        type: 'line',
        data: pStats.map(d => d.messagesReceived ?? 0),
        smooth: true,
        lineStyle: { width: 2 },
      },
    ]
  }

  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: legend },
    grid: { left: 48, right: 24, top: 48, bottom: 32 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series,
  })
}

function handleSearch() {
  pagination.page = 1
  loadList()
}

function handleReset() {
  searchForm.accountName = ''
  searchForm.statDate = ''
  pagination.page = 1
  loadList()
}

watch(activeTab, () => {
  pagination.page = 1
  loadList()
})

onMounted(() => loadList())
</script>

<style scoped>
.wechat-data-page { padding: 20px; }
.platform-tabs { margin-bottom: 16px; }
.detail-kpi { margin-bottom: 8px; }
.detail-trend-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.trend-chart { height: 320px; width: 100%; }
</style>
