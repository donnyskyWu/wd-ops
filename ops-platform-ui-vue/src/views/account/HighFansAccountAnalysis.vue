<template>
  <div class="high-fans-account">
    <el-tabs v-model="activeTab" class="platform-tabs">
      <el-tab-pane label="抖音" name="douyin">
        <TableSearch v-model="searchForm.douyin" @search="handleSearch('douyin')" @reset="handleReset('douyin')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.douyin.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredDouyinList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column prop="growthRate" label="30天增长率" width="120"><template #default="{ row }"><span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">{{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate }}%</span></template></el-table-column>
          <el-table-column prop="contentCount" label="作品数" width="100" />
          <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">详情</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="公众号" name="wechat">
        <TableSearch v-model="searchForm.wechat" @search="handleSearch('wechat')" @reset="handleReset('wechat')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.wechat.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredWechatList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="avgReading" label="平均阅读" width="120"><template #default="{ row }">{{ row.avgReading.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="在看率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column prop="growthRate" label="30天增长率" width="120"><template #default="{ row }"><span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">{{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate }}%</span></template></el-table-column>
          <el-table-column prop="contentCount" label="文章数" width="100" />
          <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">详情</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="视频号" name="channels">
        <TableSearch v-model="searchForm.channels" @search="handleSearch('channels')" @reset="handleReset('channels')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.channels.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="filteredChannelsList" v-loading="loading" stripe>
          <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column prop="growthRate" label="30天增长率" width="120"><template #default="{ row }"><span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">{{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate }}%</span></template></el-table-column>
          <el-table-column prop="contentCount" label="视频数" width="100" />
          <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">详情</el-button></template></el-table-column>
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
  { rank: 1, accountName: '科技小达人', followerCount: 5200000, avgViews: 180000, engagement: 5.8, growthRate: 12.5, contentCount: 328 },
  { rank: 2, accountName: '美食探索家', followerCount: 3800000, avgViews: 150000, engagement: 4.2, growthRate: 8.3, contentCount: 256 },
  { rank: 3, accountName: '旅行摄影师', followerCount: 2900000, avgViews: 120000, engagement: 3.9, growthRate: 15.2, contentCount: 189 },
  { rank: 4, accountName: '时尚辣妈', followerCount: 1800000, avgViews: 95000, engagement: 4.5, growthRate: 6.8, contentCount: 420 },
  { rank: 5, accountName: '健身达人', followerCount: 1200000, avgViews: 88000, engagement: 5.1, growthRate: 9.6, contentCount: 312 },
])
const wechatList = ref([
  { rank: 1, accountName: '财经观察', followerCount: 2800000, avgReading: 85000, engagement: 3.2, growthRate: 5.8, contentCount: 520 },
  { rank: 2, accountName: '健康养生堂', followerCount: 1800000, avgReading: 62000, engagement: 2.8, growthRate: 4.2, contentCount: 380 },
  { rank: 3, accountName: '科技前沿', followerCount: 1200000, avgReading: 45000, engagement: 3.5, growthRate: 7.1, contentCount: 290 },
  { rank: 4, accountName: '亲子教育', followerCount: 980000, avgReading: 38000, engagement: 3.1, growthRate: 3.5, contentCount: 420 },
  { rank: 5, accountName: '职场成长', followerCount: 650000, avgReading: 28000, engagement: 2.9, growthRate: 6.3, contentCount: 180 },
])
const channelsList = ref([
  { rank: 1, accountName: '时尚穿搭', followerCount: 3200000, avgViews: 180000, engagement: 4.8, growthRate: 18.5, contentCount: 420 },
  { rank: 2, accountName: '亲子教育', followerCount: 2100000, avgViews: 150000, engagement: 5.2, growthRate: 12.3, contentCount: 320 },
  { rank: 3, accountName: '职场成长', followerCount: 1500000, avgViews: 120000, engagement: 4.5, growthRate: 9.8, contentCount: 180 },
  { rank: 4, accountName: '美食教程', followerCount: 980000, avgViews: 95000, engagement: 4.1, growthRate: 7.2, contentCount: 280 },
  { rank: 5, accountName: '旅行日记', followerCount: 650000, avgViews: 72000, engagement: 3.8, growthRate: 5.6, contentCount: 150 },
])

const filteredDouyinList = computed(() => douyinList.value.filter(item => !searchForm.douyin.keyword || item.accountName.includes(searchForm.douyin.keyword)))
const filteredWechatList = computed(() => wechatList.value.filter(item => !searchForm.wechat.keyword || item.accountName.includes(searchForm.wechat.keyword)))
const filteredChannelsList = computed(() => channelsList.value.filter(item => !searchForm.channels.keyword || item.accountName.includes(searchForm.channels.keyword)))

const handleSearch = (tab: string) => { console.log('搜索', tab) }
const handleReset = (tab: string) => { searchForm[tab as keyof typeof searchForm].keyword = undefined }
</script>

<style scoped>
.high-fans-account { padding: 20px; }
.platform-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
</style>
