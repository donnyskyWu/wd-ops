<template>
  <div class="wechat-data-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="日期范围">
        <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="~" />
      </el-form-item>
    </TableSearch>

    <el-empty
      v-if="!loading"
      description="微信数据分析：本期未交付（Phase 2 规划）"
      style="margin: 80px 0;"
    >
      <el-button type="primary" @click="handlePhase2">Phase 2 规划</el-button>
    </el-empty>

    <el-row :gutter="16" class="stats-row" v-else>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="文章阅读数" :value="stats.readCount" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="分享数" :value="stats.shareCount" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="新增关注" :value="stats.newFollowers" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="净增粉丝" :value="stats.netFollowers" /></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import TableSearch from '@/components/TableSearch.vue'

const loading = ref(false)
const searchForm = reactive({ dateRange: [] as string[] })
const stats = reactive({ readCount: 0, shareCount: 0, newFollowers: 0, netFollowers: 0 })

const handleSearch = () => {
  ElMessage.warning('微信数据分析接口未交付（Phase 2 规划中）')
}
const handleReset = () => { searchForm.dateRange = [] }
const handlePhase2 = () => {
  ElMessage.info('微信数据分析需新增 WechatDataController + oa_wechat_daily 表（Sprint 12+）')
}
</script>

<style scoped>
.wechat-data-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
</style>
