<template>
  <div class="low-fans-account">
    <el-tabs v-model="activeTab" class="platform-tabs">
      <el-tab-pane label="抖音" name="douyin">
        <TableSearch v-model="searchForm.douyin" @search="handleSearch('douyin')" @reset="handleReset('douyin')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.douyin.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredDouyinList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag type="warning">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column prop="growthRate" label="30天增长率" width="120"><template #default="{ row }"><span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">{{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate }}%</span></template></el-table-column>
          <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === '活跃' ? 'success' : row.status === '沉寂' ? 'info' : 'danger'">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="180" fixed="right"><template #default><el-button link type="primary">详情</el-button><el-button link type="warning">激活</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="公众号" name="wechat">
        <TableSearch v-model="searchForm.wechat" @search="handleSearch('wechat')" @reset="handleReset('wechat')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.wechat.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredWechatList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag type="warning">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="avgReading" label="平均阅读" width="120"><template #default="{ row }">{{ row.avgReading.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="在看率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column prop="growthRate" label="30天增长率" width="120"><template #default="{ row }"><span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">{{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate }}%</span></template></el-table-column>
          <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === '活跃' ? 'success' : row.status === '沉寂' ? 'info' : 'danger'">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="180" fixed="right"><template #default><el-button link type="primary">详情</el-button><el-button link type="warning">激活</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="视频号" name="channels">
        <TableSearch v-model="searchForm.channels" @search="handleSearch('channels')" @reset="handleReset('channels')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.channels.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredChannelsList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag type="warning">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column prop="growthRate" label="30天增长率" width="120"><template #default="{ row }"><span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">{{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate }}%</span></template></el-table-column>
          <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === '活跃' ? 'success' : row.status === '沉寂' ? 'info' : 'danger'">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="180" fixed="right"><template #default><el-button link type="primary">详情</el-button><el-button link type="warning">激活</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import TableSearch from '@/components/TableSearch.vue'

const activeTab = ref('douyin')
const loading = ref(false)
const searchForm = reactive({
  douyin: reactive({ keyword: undefined as string | undefined }),
  wechat: reactive({ keyword: undefined as string | undefined }),
  channels: reactive({ keyword: undefined as string | undefined }),
})

const douyinList = ref([
  { rank: 1, accountName: '新手小白爱分享', followerCount: 1250, avgViews: 180, engagement: 1.2, growthRate: -5.8, status: '沉寂' },
  { rank: 2, accountName: '小众爱好者', followerCount: 890, avgViews: 95, engagement: 0.8, growthRate: -2.3, status: '沉寂' },
  { rank: 3, accountName: '日常记录者', followerCount: 560, avgViews: 68, engagement: 0.5, growthRate: -8.2, status: '停更' },
  { rank: 4, accountName: '测试账号001', followerCount: 320, avgViews: 45, engagement: 0.3, growthRate: -12.5, status: '沉寂' },
  { rank: 5, accountName: '试运营账号', followerCount: 180, avgViews: 28, engagement: 0.2, growthRate: -3.5, status: '活跃' },
])
const wechatList = ref([
  { rank: 1, accountName: '个人生活记', followerCount: 890, avgReading: 85, engagement: 0.3, growthRate: -4.2, status: '沉寂' },
  { rank: 2, accountName: '随手写写', followerCount: 650, avgReading: 62, engagement: 0.2, growthRate: -6.8, status: '停更' },
  { rank: 3, accountName: '小号一号', followerCount: 420, avgReading: 38, engagement: 0.1, growthRate: -1.5, status: '沉寂' },
  { rank: 4, accountName: '测试号', followerCount: 280, avgReading: 22, engagement: 0.1, growthRate: -9.3, status: '停更' },
  { rank: 5, accountName: '备用号', followerCount: 150, avgReading: 12, engagement: 0.1, growthRate: -2.8, status: '沉寂' },
])
const channelsList = ref([
  { rank: 1, accountName: '新手视频', followerCount: 980, avgViews: 120, engagement: 0.8, growthRate: -4.5, status: '沉寂' },
  { rank: 2, accountName: '随手拍', followerCount: 720, avgViews: 85, engagement: 0.5, growthRate: -7.2, status: '停更' },
  { rank: 3, accountName: '生活点滴', followerCount: 450, avgViews: 52, engagement: 0.3, growthRate: -3.8, status: '沉寂' },
  { rank: 4, accountName: '测试频道', followerCount: 280, avgViews: 32, engagement: 0.2, growthRate: -11.5, status: '停更' },
  { rank: 5, accountName: '小号频道', followerCount: 120, avgViews: 15, engagement: 0.1, growthRate: -5.2, status: '活跃' },
])

const filteredDouyinList = computed(() => douyinList.value.filter(item => !searchForm.douyin.keyword || item.accountName.includes(searchForm.douyin.keyword)))
const filteredWechatList = computed(() => wechatList.value.filter(item => !searchForm.wechat.keyword || item.accountName.includes(searchForm.wechat.keyword)))
const filteredChannelsList = computed(() => channelsList.value.filter(item => !searchForm.channels.keyword || item.accountName.includes(searchForm.channels.keyword)))

const handleSearch = (tab: string) => { console.log('搜索', tab) }
const handleReset = (tab: string) => { searchForm[tab as keyof typeof searchForm].keyword = undefined }
</script>

<style scoped>
.low-fans-account { padding: 20px; }
.platform-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
</style>
