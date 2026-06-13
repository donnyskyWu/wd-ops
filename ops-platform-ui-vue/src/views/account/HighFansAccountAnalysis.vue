<template>
  <div class="high-fans-account">
    <el-tabs v-model="activeTab" class="platform-tabs" @tab-change="loadTabData">
      <el-tab-pane label="抖音" name="douyin">
        <TableSearch v-model="searchForm.douyin" @search="() => loadTabData('douyin')" @reset="() => handleReset('douyin')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.douyin.keyword" placeholder="搜索账号" clearable /></el-form-item>
          <template #extra>
            <el-button type="success" :loading="exportLoading" @click="() => handleExport('douyin')">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </template>
        </TableSearch>
        <el-table :data="filteredDouyinList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="180" />
          <el-table-column prop="platform" label="平台" width="120" />
          <el-table-column prop="followerCount" label="粉丝数" width="130"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="netGrowth" label="日净增" width="110"><template #default="{ row }"><span class="text-success">+{{ (row.netGrowth ?? 0).toLocaleString() }}</span></template></el-table-column>
          <el-table-column prop="growthRate" label="增长率" width="100"><template #default="{ row }">{{ formatGrowthRate(row.growthRate) }}</template></el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="handleDetail(row)">详情</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="公众号" name="wechat">
        <TableSearch v-model="searchForm.wechat" @search="() => loadTabData('wechat')" @reset="() => handleReset('wechat')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.wechat.keyword" placeholder="搜索账号" clearable /></el-form-item>
          <template #extra>
            <el-button type="success" :loading="exportLoading" @click="() => handleExport('wechat')">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </template>
        </TableSearch>
        <el-table :data="filteredWechatList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="180" />
          <el-table-column prop="platform" label="平台" width="120" />
          <el-table-column prop="followerCount" label="粉丝数" width="130"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="netGrowth" label="日净增" width="110"><template #default="{ row }"><span class="text-success">+{{ (row.netGrowth ?? 0).toLocaleString() }}</span></template></el-table-column>
          <el-table-column prop="growthRate" label="增长率" width="100"><template #default="{ row }">{{ formatGrowthRate(row.growthRate) }}</template></el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="handleDetail(row)">详情</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="视频号" name="channels">
        <TableSearch v-model="searchForm.channels" @search="() => loadTabData('channels')" @reset="() => handleReset('channels')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.channels.keyword" placeholder="搜索账号" clearable /></el-form-item>
          <template #extra>
            <el-button type="success" :loading="exportLoading" @click="() => handleExport('channels')">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </template>
        </TableSearch>
        <el-table :data="filteredChannelsList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="180" />
          <el-table-column prop="platform" label="平台" width="120" />
          <el-table-column prop="followerCount" label="粉丝数" width="130"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="netGrowth" label="日净增" width="110"><template #default="{ row }"><span class="text-success">+{{ (row.netGrowth ?? 0).toLocaleString() }}</span></template></el-table-column>
          <el-table-column prop="growthRate" label="增长率" width="100"><template #default="{ row }">{{ formatGrowthRate(row.growthRate) }}</template></el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="handleDetail(row)">详情</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="detailVisible" title="高粉账号详情" size="480px">
      <el-descriptions v-if="currentRow" :column="1" border>
        <el-descriptions-item label="排名">{{ currentRow.rank }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ currentRow.accountName }}</el-descriptions-item>
        <el-descriptions-item label="账号ID">{{ currentRow.accountId }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ currentRow.platform }}</el-descriptions-item>
        <el-descriptions-item label="粉丝数">{{ currentRow.followerCount?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="日净增">+{{ (currentRow.netGrowth ?? 0).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="增长率">{{ formatGrowthRate(currentRow.growthRate) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.statDate" label="统计日期">{{ currentRow.statDate }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TableSearch from '@/components/TableSearch.vue'
import { getHighFollowerAccountList } from '@/api/monitor'
import { exportToExcel } from '@/utils'
import { buildMonitorQuery, mapFollowerAccount, pickMonitorAccountPage, type MonitorAccountRow } from '@/utils/monitor-map'

const activeTab = ref('douyin')
const loading = ref(false)
const exportLoading = ref(false)
const searchForm = reactive({
  douyin: reactive({ keyword: undefined as string | undefined }),
  wechat: reactive({ keyword: undefined as string | undefined }),
  channels: reactive({ keyword: undefined as string | undefined }),
})

const douyinList = ref<MonitorAccountRow[]>([])
const wechatList = ref<MonitorAccountRow[]>([])
const channelsList = ref<MonitorAccountRow[]>([])

const filterList = (list: MonitorAccountRow[], keyword?: string) =>
  list.filter(item => !keyword || item.accountName.includes(keyword))

const filteredDouyinList = computed(() => filterList(douyinList.value, searchForm.douyin.keyword))
const filteredWechatList = computed(() => filterList(wechatList.value, searchForm.wechat.keyword))
const filteredChannelsList = computed(() => filterList(channelsList.value, searchForm.channels.keyword))

const detailVisible = ref(false)
const currentRow = ref<MonitorAccountRow | null>(null)

const formatGrowthRate = (rate?: number) => {
  if (rate == null || Number.isNaN(rate)) return '-'
  return `${(rate * 100).toFixed(2)}%`
}

const handleDetail = (row: MonitorAccountRow) => {
  currentRow.value = row
  detailVisible.value = true
}

const loadTabData = async (tab?: string | number) => {
  const key = String(tab ?? activeTab.value)
  loading.value = true
  try {
    const res = await getHighFollowerAccountList(buildMonitorQuery(key, {}, 1, 50))
    const page = pickMonitorAccountPage(res)
    const rows = page.list.map((raw, i) => mapFollowerAccount(raw as unknown as Record<string, unknown>, i))
    if (key === 'douyin') douyinList.value = rows
    else if (key === 'wechat') wechatList.value = rows
    else channelsList.value = rows
  } catch (e) {
    console.error(e)
    if (key === 'douyin') douyinList.value = []
    else if (key === 'wechat') wechatList.value = []
    else channelsList.value = []
  } finally {
    loading.value = false
  }
}

const handleReset = (tab: string) => {
  searchForm[tab as keyof typeof searchForm].keyword = undefined
  loadTabData(tab)
}

const fetchAllFilteredRows = async (tab: string) => {
  const exportPageSize = 500
  const first = await getHighFollowerAccountList(buildMonitorQuery(tab, {}, 1, exportPageSize))
  const page = pickMonitorAccountPage(first)
  let rows = page.list.map((raw, i) => mapFollowerAccount(raw as unknown as Record<string, unknown>, i))
  const total = page.total ?? 0
  if (total > exportPageSize) {
    const totalPages = Math.ceil(total / exportPageSize)
    for (let p = 2; p <= totalPages; p += 1) {
      const res = await getHighFollowerAccountList(buildMonitorQuery(tab, {}, p, exportPageSize))
      const pg = pickMonitorAccountPage(res)
      rows = rows.concat(pg.list.map((raw, i) => mapFollowerAccount(raw as unknown as Record<string, unknown>, i)))
    }
  }
  const keyword = searchForm[tab as keyof typeof searchForm].keyword
  return filterList(rows, keyword)
}

const handleExport = async (tab: string) => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows(tab)
    exportToExcel(
      rows.map((row) => ({
        rank: row.rank ?? '',
        accountName: row.accountName,
        platform: row.platform,
        followerCount: row.followerCount,
        netGrowth: row.netGrowth ?? 0,
        growthRate: formatGrowthRate(row.growthRate),
      })),
      [
        { key: 'rank', label: '排名' },
        { key: 'accountName', label: '账号名称' },
        { key: 'platform', label: '平台' },
        { key: 'followerCount', label: '粉丝数' },
        { key: 'netGrowth', label: '日净增' },
        { key: 'growthRate', label: '增长率' },
      ],
      '高粉账号分析',
    )
  } catch (error) {
    console.error('[HighFans] 导出失败:', error)
    ElMessage.error('导出失败：' + (error instanceof Error ? error.message : String(error)))
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => loadTabData())
</script>

<style scoped>
.high-fans-account { padding: 20px; }
.platform-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.text-success { color: #67c23a; }
</style>
