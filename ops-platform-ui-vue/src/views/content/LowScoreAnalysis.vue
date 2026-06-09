<template>
  <div class="low-score-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="分数阈值"><el-input-number v-model="searchForm.threshold" :min="0" :max="100" :step="5" style="width: 120px" /> <span style="margin-left: 8px; color: #909399; font-size: 12px">综合评分≤阈值</span></el-form-item>
    </TableSearch>
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="低分作品数" :value="stats.totalLow" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="平均评分" :value="stats.avgScore" suffix="分" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="最低评分" :value="stats.minScore" suffix="分" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="需优化" :value="stats.needOptimize" /></el-card></el-col>
    </el-row>
    <el-table :data="lowScoreList" v-loading="loading" stripe>
      <el-table-column prop="title" label="作品标题" min-width="200" />
      <el-table-column prop="platform" label="平台" width="100" />
      <el-table-column prop="score" label="综合评分" width="100"><template #default="{ row }"><el-tag :type="row.score < 40 ? 'danger' : 'warning'">{{ row.score }}分</el-tag></template></el-table-column>
      <el-table-column prop="views" label="播放量" width="100" />
      <el-table-column prop="issues" label="主要问题" min-width="200"><template #default="{ row }"><el-tag v-for="(issue, i) in row.issues" :key="i" size="small" class="issue-tag">{{ issue }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="150" fixed="right"><template #default><el-button link type="primary">优化建议</el-button><el-button link type="warning">标记</el-button></template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import TableSearch from '@/components/TableSearch.vue'

const loading = ref(false)
const searchForm = reactive({ threshold: 60 })
const stats = reactive({ totalLow: 89, avgScore: 52, minScore: 28, needOptimize: 45 })
const lowScoreList = ref([
  { title: '产品介绍视频v1', platform: '抖音', score: 35, views: 1200, issues: ['完播率低', '互动差'] },
  { title: '教程内容粗糙', platform: '快手', score: 48, views: 2800, issues: ['画质模糊', '标题不吸引人'] },
  { title: '日常分享', platform: '小红书', score: 55, views: 3500, issues: ['内容同质化'] },
])
const handleSearch = () => {}
const handleReset = () => { searchForm.threshold = 60 }
</script>

<style scoped>
.low-score-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.issue-tag { margin-right: 4px; }
</style>
