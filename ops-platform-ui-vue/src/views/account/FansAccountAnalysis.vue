<template>
  <div class="fans-account-page">
    <el-tabs v-model="activeTab" @tab-change="loadData">
      <el-tab-pane label="高粉账号(高播放作品)" name="high">
        <el-table :data="highFansList" v-loading="loading" stripe>
          <el-table-column prop="title" label="作品标题" min-width="200" />
          <el-table-column prop="platform" label="平台" width="120" />
          <el-table-column prop="playCount" label="播放量" width="120"><template #default="{ row }">{{ row.playCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="100" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="低粉账号(低播放作品)" name="low">
        <el-table :data="lowFansList" v-loading="loading" stripe>
          <el-table-column prop="title" label="作品标题" min-width="200" />
          <el-table-column prop="platform" label="平台" width="120" />
          <el-table-column prop="playCount" label="播放量" width="120"><template #default="{ row }">{{ row.playCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="100" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHighFollowerWorkList, getLowFollowerWorkList } from '@/api/monitor'
import { mapExternalWork, pickMonitorPage, type MonitorWorkRow } from '@/utils/monitor-map'

const loading = ref(false)
const activeTab = ref('high')
const highFansList = ref<MonitorWorkRow[]>([])
const lowFansList = ref<MonitorWorkRow[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const [highRes, lowRes] = await Promise.all([
      getHighFollowerWorkList({ pageNum: 1, pageSize: 30 }),
      getLowFollowerWorkList({ pageNum: 1, pageSize: 30 }),
    ])
    highFansList.value = pickMonitorPage(highRes).list.map((r, i) => mapExternalWork(r as unknown as Record<string, unknown>, i))
    lowFansList.value = pickMonitorPage(lowRes).list.map((r, i) => mapExternalWork(r as unknown as Record<string, unknown>, i))
  } catch (e) {
    console.error(e)
    highFansList.value = []
    lowFansList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.fans-account-page { padding: 20px; }
</style>
