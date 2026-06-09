<template>
  <div class="fans-account-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="高粉账号" name="high">
        <el-table :data="highFansList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="platform" label="平台" width="100" />
          <el-table-column prop="followerCount" label="粉丝数" width="120" />
          <el-table-column prop="growth" label="月增长" width="100"><template #default="{ row }"><span style="color: #67C23A">+{{ row.growth }}%</span></template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100" />
          <el-table-column label="操作" width="100" fixed="right"><template #default><el-button link type="primary">详情</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="低粉账号" name="low">
        <el-table :data="lowFansList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="platform" label="平台" width="100" />
          <el-table-column prop="followerCount" label="粉丝数" width="120" />
          <el-table-column prop="growth" label="月增长" width="100"><template #default="{ row }"><span :style="{ color: row.growth > 0 ? '#67C23A' : '#F56C6C' }">{{ row.growth > 0 ? '+' : ''}}{{ row.growth }}%</span></template></el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="row.status === '需关注' ? 'warning' : 'success'">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">详情</el-button><el-button link type="warning">扶持</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const loading = ref(false)
const activeTab = ref('high')

const highFansList = ref([
  { accountName: '科技达人A', platform: '抖音', followerCount: '125万', growth: 15, engagement: '5.2%' },
  { accountName: '生活博主B', platform: '小红书', followerCount: '89万', growth: 12, engagement: '6.8%' },
])

const lowFansList = ref([
  { accountName: '新账号C', platform: '快手', followerCount: '1200', growth: -3, status: '需关注' },
  { accountName: '测试账号D', platform: 'B站', followerCount: '850', growth: 2, status: '正常' },
])
</script>

<style scoped>
.fans-account-page { padding: 20px; }
</style>
