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
      <template #extra>
        <el-button type="success" :loading="exportLoading" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
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
import { reactive, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TableSearch from '@/components/TableSearch.vue'
import { getExternalWorkList } from '@/api/monitor'
import { exportToExcel, formatDateTime } from '@/utils'
import { mapExternalWork, pickMonitorPage } from '@/utils/monitor-map'

const searchForm = reactive({ industry: undefined as string | undefined })
const exportLoading = ref(false)

const handleSearch = () => ElMessage.warning('行业数据接口未交付（Phase 2 规划中）')
const handleReset = () => { searchForm.industry = undefined }
const handlePhase2 = () => ElMessage.info('行业数据需新增 IndustryDataController + oa_industry_bench 表（Sprint 12+）')

const buildListParams = (pageNum: number, pageSize: number) => ({
  pageNum,
  pageSize,
  industry: searchForm.industry || undefined,
})

/** 复用外部作品 list API（industry 筛选）；专用行业排行 API 未交付 */
const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first = await getExternalWorkList(buildListParams(1, exportPageSize))
  const page = pickMonitorPage(first)
  let works = page.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i))
  const total = page.total ?? 0
  if (total > exportPageSize) {
    const totalPages = Math.ceil(total / exportPageSize)
    for (let p = 2; p <= totalPages; p += 1) {
      const res = await getExternalWorkList(buildListParams(p, exportPageSize))
      const pg = pickMonitorPage(res)
      works = works.concat(pg.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i)))
    }
  }
  return works.map((w) => ({
    title: w.title,
    platform: w.platform,
    contentType: w.contentType ?? '',
    industry: w.industry ?? '',
    playCount: w.playCount,
    likeCount: w.likeCount,
    completionRate: w.completionRate != null ? `${Math.round(w.completionRate * 1000) / 10}%` : '',
    publishTime: w.publishTime ? formatDateTime(w.publishTime) : '',
  }))
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows()
    exportToExcel(rows, [
      { key: 'title', label: '作品标题' },
      { key: 'platform', label: '平台' },
      { key: 'contentType', label: '类型' },
      { key: 'industry', label: '行业' },
      { key: 'playCount', label: '播放量' },
      { key: 'likeCount', label: '点赞' },
      { key: 'completionRate', label: '完播率' },
      { key: 'publishTime', label: '发布时间' },
    ], '行业数据')
  } catch (error) {
    console.error('[IndustryData] 导出失败:', error)
    ElMessage.error('导出失败：' + (error instanceof Error ? error.message : String(error)))
  } finally {
    exportLoading.value = false
  }
}
</script>

<style scoped>
.industry-data-page { padding: 20px; }
</style>
