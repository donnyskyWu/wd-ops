<template>
  <div class="high-fans-account">
    <el-tabs v-model="activeTab" class="platform-tabs" @tab-change="loadTabData">
      <el-tab-pane label="抖音" name="douyin">
        <TableSearch v-model="searchForm.douyin" @search="() => loadTabData('douyin')" @reset="() => handleReset('douyin')">
          <el-form-item label="作品标题"><el-input v-model="searchForm.douyin.keyword" placeholder="搜索" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredDouyinList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="title" label="作品标题" min-width="200" />
          <el-table-column prop="platform" label="平台" width="100" />
          <el-table-column prop="playCount" label="播放量" width="120"><template #default="{ row }">{{ row.playCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="100" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="handleDetail(row)">详情</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="公众号" name="wechat">
        <TableSearch v-model="searchForm.wechat" @search="() => loadTabData('wechat')" @reset="() => handleReset('wechat')">
          <el-form-item label="作品标题"><el-input v-model="searchForm.wechat.keyword" placeholder="搜索" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredWechatList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="title" label="作品标题" min-width="200" />
          <el-table-column prop="platform" label="平台" width="100" />
          <el-table-column prop="playCount" label="阅读量" width="120"><template #default="{ row }">{{ row.playCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="100" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="handleDetail(row)">详情</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="视频号" name="channels">
        <TableSearch v-model="searchForm.channels" @search="() => loadTabData('channels')" @reset="() => handleReset('channels')">
          <el-form-item label="作品标题"><el-input v-model="searchForm.channels.keyword" placeholder="搜索" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredChannelsList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="title" label="作品标题" min-width="200" />
          <el-table-column prop="platform" label="平台" width="100" />
          <el-table-column prop="playCount" label="播放量" width="120"><template #default="{ row }">{{ row.playCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="100" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="handleDetail(row)">详情</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="detailVisible" title="高播放作品详情" size="480px">
      <el-descriptions v-if="currentRow" :column="1" border>
        <el-descriptions-item label="排名">{{ currentRow.rank }}</el-descriptions-item>
        <el-descriptions-item label="作品">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ currentRow.platform }}</el-descriptions-item>
        <el-descriptions-item label="播放量">{{ currentRow.playCount?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="点赞">{{ currentRow.likeCount?.toLocaleString() }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import TableSearch from '@/components/TableSearch.vue'
import { getHighFollowerWorkList } from '@/api/monitor'
import { buildMonitorQuery, mapExternalWork, pickMonitorPage, type MonitorWorkRow } from '@/utils/monitor-map'

const activeTab = ref('douyin')
const loading = ref(false)
const searchForm = reactive({
  douyin: reactive({ keyword: undefined as string | undefined }),
  wechat: reactive({ keyword: undefined as string | undefined }),
  channels: reactive({ keyword: undefined as string | undefined }),
})

const douyinList = ref<MonitorWorkRow[]>([])
const wechatList = ref<MonitorWorkRow[]>([])
const channelsList = ref<MonitorWorkRow[]>([])

const filterList = (list: MonitorWorkRow[], keyword?: string) =>
  list.filter(item => !keyword || item.title.includes(keyword))

const filteredDouyinList = computed(() => filterList(douyinList.value, searchForm.douyin.keyword))
const filteredWechatList = computed(() => filterList(wechatList.value, searchForm.wechat.keyword))
const filteredChannelsList = computed(() => filterList(channelsList.value, searchForm.channels.keyword))

const detailVisible = ref(false)
const currentRow = ref<MonitorWorkRow | null>(null)

const handleDetail = (row: MonitorWorkRow) => {
  currentRow.value = row
  detailVisible.value = true
}

const loadTabData = async (tab?: string | number) => {
  const key = String(tab ?? activeTab.value)
  loading.value = true
  try {
    const res = await getHighFollowerWorkList(buildMonitorQuery(key, {}, 1, 50))
    const page = pickMonitorPage(res)
    const rows = page.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i))
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

onMounted(() => loadTabData())
</script>

<style scoped>
.high-fans-account { padding: 20px; }
.platform-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>
