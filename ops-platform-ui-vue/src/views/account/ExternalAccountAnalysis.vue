<template>
  <div class="external-account-page">
    <el-tabs v-model="activeTab" class="platform-tabs">
      <el-tab-pane label="抖音" name="douyin">
        <TableSearch v-model="searchForm.douyin" @search="handleSearch('douyin')" @reset="handleReset('douyin')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.douyin.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="douyinList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="likeCount" label="获赞数" width="120"><template #default="{ row }">{{ row.likeCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="contentCount" label="作品数" width="100" />
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">详情</el-button><el-button link type="primary">同步</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="公众号" name="wechat">
        <TableSearch v-model="searchForm.wechat" @search="handleSearch('wechat')" @reset="handleReset('wechat')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.wechat.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="wechatList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="readingCount" label="阅读数" width="120"><template #default="{ row }">{{ row.readingCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="contentCount" label="文章数" width="100" />
          <el-table-column prop="avgReading" label="平均阅读" width="120"><template #default="{ row }">{{ row.avgReading.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="在看率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">详情</el-button><el-button link type="primary">同步</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="视频号" name="channels">
        <TableSearch v-model="searchForm.channels" @search="handleSearch('channels')" @reset="handleReset('channels')">
          <el-form-item label="账号名称"><el-input v-model="searchForm.channels.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <el-table :data="channelsList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" min-width="150" />
          <el-table-column prop="followerCount" label="粉丝数" width="120"><template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="views" label="播放量" width="120"><template #default="{ row }">{{ row.views.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="contentCount" label="视频数" width="100" />
          <el-table-column prop="avgViews" label="平均播放" width="120"><template #default="{ row }">{{ row.avgViews.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="engagement" label="互动率" width="100"><template #default="{ row }">{{ row.engagement }}%</template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">详情</el-button><el-button link type="primary">同步</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import TableSearch from '@/components/TableSearch.vue'

const activeTab = ref('douyin')
const loading = ref(false)
const searchForm = reactive({
  douyin: reactive({ keyword: undefined as string | undefined }),
  wechat: reactive({ keyword: undefined as string | undefined }),
  channels: reactive({ keyword: undefined as string | undefined }),
})

const douyinList = ref([
  { accountName: '科技小达人', followerCount: 1250000, likeCount: 5800000, contentCount: 328, avgViews: 45000, engagement: 4.2 },
  { accountName: '美食探索家', followerCount: 890000, likeCount: 3200000, contentCount: 256, avgViews: 32000, engagement: 3.8 },
  { accountName: '旅行摄影师', followerCount: 560000, likeCount: 1800000, contentCount: 189, avgViews: 28000, engagement: 3.5 },
])
const wechatList = ref([
  { accountName: '财经观察', followerCount: 450000, readingCount: 890000, contentCount: 520, avgReading: 1700, engagement: 0.8 },
  { accountName: '健康养生堂', followerCount: 320000, readingCount: 650000, contentCount: 380, avgReading: 1700, engagement: 0.9 },
  { accountName: '科技前沿', followerCount: 280000, readingCount: 520000, contentCount: 290, avgReading: 1800, engagement: 0.7 },
])
const channelsList = ref([
  { accountName: '时尚穿搭', followerCount: 680000, views: 12000000, contentCount: 420, avgViews: 28000, engagement: 2.8 },
  { accountName: '亲子教育', followerCount: 450000, views: 8500000, contentCount: 320, avgViews: 26000, engagement: 3.2 },
  { accountName: '职场成长', followerCount: 320000, views: 5600000, contentCount: 180, avgViews: 31000, engagement: 2.5 },
])

const handleSearch = (tab: string) => { console.log('搜索', tab) }
const handleReset = (tab: string) => { searchForm[tab as keyof typeof searchForm].keyword = undefined }
</script>

<style scoped>
.external-account-page { padding: 20px; }
.platform-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>
