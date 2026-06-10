<template>
  <div class="industry-data-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="行业">
        <el-select v-model="searchForm.industry" placeholder="全部" clearable>
          <el-option label="科技" value="tech" />
          <el-option label="生活" value="life" />
          <el-option label="美食" value="food" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <el-empty
      description="行业数据：本期未交付（Phase 2 规划）"
      style="margin: 80px 0;"
    >
      <el-button type="primary" @click="handlePhase2">Phase 2 规划</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import TableSearch from '@/components/TableSearch.vue'

const searchForm = reactive({ industry: undefined as string | undefined })
const handleSearch = () => ElMessage.warning('行业数据接口未交付（Phase 2 规划中）')
const handleReset = () => { searchForm.industry = undefined }
const handlePhase2 = () => ElMessage.info('行业数据需新增 IndustryDataController + oa_industry_bench 表（Sprint 12+）')
</script>

<style scoped>
.industry-data-page { padding: 20px; }
</style>
