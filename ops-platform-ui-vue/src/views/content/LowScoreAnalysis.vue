<template>
  <div class="low-score-page">
    <TableSearch v-model="searchForm" @search="loadData" @reset="handleReset">
      <el-form-item label="完播率阈值"><el-input-number v-model="searchForm.threshold" :min="0" :max="1" :step="0.05" :precision="2" style="width: 120px" /> <span style="margin-left: 8px; color: #909399; font-size: 12px">完播率≤阈值</span></el-form-item>
    </TableSearch>
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="低分作品数" :value="stats.totalLow" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="平均完播率" :value="stats.avgScore" suffix="%" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="最低完播率" :value="stats.minScore" suffix="%" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="需优化" :value="stats.needOptimize" /></el-card></el-col>
    </el-row>
    <el-table :data="lowScoreList" v-loading="loading" stripe>
      <el-table-column prop="title" label="作品标题" min-width="200" />
      <el-table-column prop="platform" label="平台" width="100" />
      <el-table-column prop="score" label="完播率" width="100"><template #default="{ row }"><el-tag :type="row.score < 15 ? 'danger' : 'warning'">{{ row.score }}%</el-tag></template></el-table-column>
      <el-table-column prop="views" label="播放量" width="100"><template #default="{ row }">{{ row.views.toLocaleString() }}</template></el-table-column>
      <el-table-column prop="issues" label="主要问题" min-width="200"><template #default="{ row }"><el-tag v-for="(issue, i) in row.issues" :key="i" size="small" class="issue-tag">{{ issue }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailVisible" title="低分作品详情" size="520px">
      <el-descriptions v-if="currentRow" :column="1" border>
        <el-descriptions-item label="作品标题">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ currentRow.platform }}</el-descriptions-item>
        <el-descriptions-item label="完播率">{{ currentRow.score }}%</el-descriptions-item>
        <el-descriptions-item label="播放量">{{ currentRow.views?.toLocaleString() }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import TableSearch from '@/components/TableSearch.vue'
import { getLowScoreWorkList } from '@/api/monitor'
import { mapExternalWork, pickMonitorPage } from '@/utils/monitor-map'

interface LowScoreRow {
  title: string
  platform: string
  score: number
  views: number
  issues: string[]
}

const loading = ref(false)
const searchForm = reactive({ threshold: 0.2 })
const stats = reactive({ totalLow: 0, avgScore: 0, minScore: 0, needOptimize: 0 })
const lowScoreList = ref<LowScoreRow[]>([])
const detailVisible = ref(false)
const currentRow = ref<LowScoreRow | null>(null)

const buildIssues = (rate?: number) => {
  const issues: string[] = []
  if (rate == null) return ['数据缺失']
  if (rate < 0.15) issues.push('完播率低')
  if (rate < 0.2) issues.push('互动差')
  return issues.length ? issues : ['待观察']
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLowScoreWorkList({ pageNum: 1, pageSize: 100 })
    const page = pickMonitorPage(res)
    const rows = page.list
      .map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i))
      .filter(w => w.completionRate == null || w.completionRate <= searchForm.threshold)
      .map(w => {
        const pct = w.completionRate != null ? Math.round(w.completionRate * 1000) / 10 : 0
        return {
          title: w.title,
          platform: w.platform,
          score: pct,
          views: w.playCount,
          issues: buildIssues(w.completionRate),
        }
      })
    lowScoreList.value = rows
    stats.totalLow = rows.length
    stats.avgScore = rows.length ? Math.round(rows.reduce((s, r) => s + r.score, 0) / rows.length * 10) / 10 : 0
    stats.minScore = rows.length ? Math.min(...rows.map(r => r.score)) : 0
    stats.needOptimize = rows.filter(r => r.score < 15).length
  } catch (e) {
    console.error(e)
    lowScoreList.value = []
  } finally {
    loading.value = false
  }
}

const handleDetail = (row: LowScoreRow) => {
  currentRow.value = row
  detailVisible.value = true
}

const handleReset = () => {
  searchForm.threshold = 0.2
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.low-score-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.issue-tag { margin-right: 4px; }
</style>
