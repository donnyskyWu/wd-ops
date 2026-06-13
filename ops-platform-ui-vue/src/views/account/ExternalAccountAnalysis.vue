<template>
  <div class="external-account-page">
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
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="总播放(估)" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="likeCount" label="获赞数" width="120"><template #default="{ row }">{{ row.likeCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="contentCount" label="作品数" width="100" />
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            </template>
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
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="readingCount" label="阅读数" width="120"><template #default="{ row }">{{ row.readingCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="contentCount" label="文章数" width="100" />
          <el-table-column prop="avgReading" label="平均阅读" width="120"><template #default="{ row }">{{ row.avgReading.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            </template>
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
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="views" label="播放量" width="120"><template #default="{ row }">{{ row.views.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="contentCount" label="视频数" width="100" />
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="detailVisible" title="外部账号详情" size="480px">
      <el-descriptions v-if="currentRow" :column="1" border>
        <el-descriptions-item label="账号名称">{{ currentRow.accountName }}</el-descriptions-item>
        <el-descriptions-item label="账号ID">{{ currentRow.accountId }}</el-descriptions-item>
        <el-descriptions-item label="作品数">{{ currentRow.contentCount }}</el-descriptions-item>
        <el-descriptions-item label="总播放/阅读">{{ (currentRow.views ?? currentRow.readingCount ?? currentRow.followerCount)?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="互动率">{{ currentRow.engagement }}%</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TableSearch from '@/components/TableSearch.vue'
import { getExternalWorkList } from '@/api/monitor'
import { exportToExcel } from '@/utils'
import { aggregateWorksByAccount, buildMonitorQuery, mapExternalWork, pickMonitorPage } from '@/utils/monitor-map'

type AccountRow = ReturnType<typeof aggregateWorksByAccount>[number]

const activeTab = ref('douyin')
const loading = ref(false)
const exportLoading = ref(false)
const searchForm = reactive({
  douyin: reactive({ keyword: undefined as string | undefined }),
  wechat: reactive({ keyword: undefined as string | undefined }),
  channels: reactive({ keyword: undefined as string | undefined }),
})

const douyinList = ref<AccountRow[]>([])
const wechatList = ref<AccountRow[]>([])
const channelsList = ref<AccountRow[]>([])

const filterByKeyword = (list: AccountRow[], keyword?: string) =>
  list.filter(item => !keyword || item.accountName.includes(keyword))

const filteredDouyinList = computed(() => filterByKeyword(douyinList.value, searchForm.douyin.keyword))
const filteredWechatList = computed(() => filterByKeyword(wechatList.value, searchForm.wechat.keyword))
const filteredChannelsList = computed(() => filterByKeyword(channelsList.value, searchForm.channels.keyword))

const detailVisible = ref(false)
const currentRow = ref<AccountRow | null>(null)

const handleDetail = (row: AccountRow) => {
  currentRow.value = row
  detailVisible.value = true
}

const loadTabData = async (tab?: string | number) => {
  const key = String(tab ?? activeTab.value)
  loading.value = true
  try {
    const res = await getExternalWorkList(buildMonitorQuery(key, {}, 1, 200))
    const page = pickMonitorPage(res)
    const aggregated = aggregateWorksByAccount(page.list.map((r, i) => mapExternalWork(r as unknown as Record<string, unknown>, i)))
    if (key === 'douyin') douyinList.value = aggregated
    else if (key === 'wechat') wechatList.value = aggregated
    else channelsList.value = aggregated
  } catch (e) {
    console.error('load external works failed', e)
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
  const first = await getExternalWorkList(buildMonitorQuery(tab, {}, 1, exportPageSize))
  const page = pickMonitorPage(first)
  let works = page.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i))
  const total = page.total ?? 0
  if (total > exportPageSize) {
    const totalPages = Math.ceil(total / exportPageSize)
    for (let p = 2; p <= totalPages; p += 1) {
      const res = await getExternalWorkList(buildMonitorQuery(tab, {}, p, exportPageSize))
      const pg = pickMonitorPage(res)
      works = works.concat(pg.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i)))
    }
  }
  const aggregated = aggregateWorksByAccount(works)
  const keyword = searchForm[tab as keyof typeof searchForm].keyword
  return filterByKeyword(aggregated, keyword)
}

const handleExport = async (tab: string) => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows(tab)
    if (tab === 'douyin') {
      exportToExcel(
        rows.map((row) => ({
          accountName: row.accountName,
          followerCount: row.followerCount,
          likeCount: row.likeCount,
          contentCount: row.contentCount,
          avgViews: row.avgViews,
          engagement: `${row.engagement}%`,
        })),
        [
          { key: 'accountName', label: '账号名称' },
          { key: 'followerCount', label: '总播放(估)' },
          { key: 'likeCount', label: '获赞数' },
          { key: 'contentCount', label: '作品数' },
          { key: 'avgViews', label: '平均播放' },
          { key: 'engagement', label: '互动率' },
        ],
        '外部账号分析',
      )
    } else if (tab === 'wechat') {
      exportToExcel(
        rows.map((row) => ({
          accountName: row.accountName,
          readingCount: row.readingCount ?? 0,
          contentCount: row.contentCount,
          avgReading: row.avgReading ?? 0,
          engagement: `${row.engagement}%`,
        })),
        [
          { key: 'accountName', label: '账号名称' },
          { key: 'readingCount', label: '阅读数' },
          { key: 'contentCount', label: '文章数' },
          { key: 'avgReading', label: '平均阅读' },
          { key: 'engagement', label: '互动率' },
        ],
        '外部账号分析',
      )
    } else {
      exportToExcel(
        rows.map((row) => ({
          accountName: row.accountName,
          views: row.views ?? 0,
          contentCount: row.contentCount,
          avgViews: row.avgViews,
          engagement: `${row.engagement}%`,
        })),
        [
          { key: 'accountName', label: '账号名称' },
          { key: 'views', label: '播放量' },
          { key: 'contentCount', label: '视频数' },
          { key: 'avgViews', label: '平均播放' },
          { key: 'engagement', label: '互动率' },
        ],
        '外部账号分析',
      )
    }
  } catch (error) {
    console.error('[ExternalAccount] 导出失败:', error)
    ElMessage.error('导出失败：' + (error instanceof Error ? error.message : String(error)))
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => loadTabData())
</script>

<style scoped>
.external-account-page { padding: 20px; }
.platform-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>
