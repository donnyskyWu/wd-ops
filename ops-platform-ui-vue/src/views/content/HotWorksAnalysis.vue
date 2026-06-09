<template>
  <div class="hot-works-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="爆款阈值"><el-input-number v-model="searchForm.threshold" :min="1000" :step="1000" style="width: 150px" /> <span style="margin-left: 8px; color: #909399; font-size: 12px">播放量≥阈值</span></el-form-item>
    </TableSearch>
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="爆款总数" :value="stats.totalHot" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="总播放量" :value="stats.totalViews" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="平均点赞" :value="stats.avgLikes" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="平均分享" :value="stats.avgShares" /></el-card></el-col>
    </el-row>
    <el-table :data="hotWorksList" v-loading="loading" stripe>
      <el-table-column prop="rank" label="排名" width="80" align="center"><template #default="{ row }"><el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag></template></el-table-column>
      <el-table-column prop="title" label="作品标题" min-width="200" />
      <el-table-column prop="platform" label="平台" width="100" />
      <el-table-column prop="views" label="播放量" width="120" />
      <el-table-column prop="likes" label="点赞" width="100" />
      <el-table-column prop="shares" label="分享" width="100" />
      <el-table-column prop="hotScore" label="爆款指数" width="120"><template #default="{ row }"><el-rate v-model="row.hotScore" disabled /></template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right"><template #default><el-button link type="primary">详情</el-button></template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import TableSearch from '@/components/TableSearch.vue'

const loading = ref(false)
const searchForm = reactive({ threshold: 10000 })
const stats = reactive({ totalHot: 156, totalViews: '850万', avgLikes: 3200, avgShares: 850 })
const hotWorksList = ref([
  { rank: 1, title: '5月运营技巧分享', platform: '抖音', views: 125000, likes: 8500, shares: 2100, hotScore: 5 },
  { rank: 2, title: '产品使用教程', platform: '快手', views: 98000, likes: 6200, shares: 1800, hotScore: 4 },
  { rank: 3, title: '行业趋势分析', platform: 'B站', views: 75000, likes: 4800, shares: 1200, hotScore: 4 },
])
const handleSearch = () => {}
const handleReset = () => { searchForm.threshold = 10000 }
</script>

<style scoped>
.hot-works-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
</style>
