<template>
  <div class="fans-account-page">
    <el-tabs v-model="activeTab" @tab-change="loadData">
      <el-tab-pane label="高粉账号" name="high">
        <el-table :data="highFansList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" min-width="180" />
          <el-table-column prop="platform" label="平台" width="120" />
          <el-table-column prop="followerCount" label="粉丝数" width="130"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="netGrowth" label="日净增" width="110"><template #default="{ row }">+{{ (row.netGrowth ?? 0).toLocaleString() }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="低粉账号" name="low">
        <el-table :data="lowFansList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" min-width="180" />
          <el-table-column prop="platform" label="平台" width="120" />
          <el-table-column prop="followerCount" label="粉丝数" width="130"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="netGrowth" label="日净增" width="110"><template #default="{ row }">{{ (row.netGrowth ?? 0).toLocaleString() }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHighFollowerAccountList, getLowFollowerAccountList } from '@/api/monitor'
import { mapFollowerAccount, pickMonitorAccountPage, type MonitorAccountRow } from '@/utils/monitor-map'

const loading = ref(false)
const activeTab = ref('high')
const highFansList = ref<MonitorAccountRow[]>([])
const lowFansList = ref<MonitorAccountRow[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const [highRes, lowRes] = await Promise.all([
      getHighFollowerAccountList({ pageNum: 1, pageSize: 30 }),
      getLowFollowerAccountList({ pageNum: 1, pageSize: 30 }),
    ])
    highFansList.value = pickMonitorAccountPage(highRes).list.map((r, i) => mapFollowerAccount(r as unknown as Record<string, unknown>, i))
    lowFansList.value = pickMonitorAccountPage(lowRes).list.map((r, i) => mapFollowerAccount(r as unknown as Record<string, unknown>, i))
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
