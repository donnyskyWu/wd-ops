<template>
  <div class="hot-works-page">
    <TableSearch v-model="searchForm" @search="loadData" @reset="handleReset">
      <el-form-item label="作品类型">
        <DictSelect v-model="searchForm.contentType" dict-type="dict_content_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="爆款阈值"><el-input-number v-model="searchForm.threshold" :min="1000" :step="1000" style="width: 150px" /> <span style="margin-left: 8px; color: #909399; font-size: 12px">播放量≥阈值</span></el-form-item>
      <template #extra>
        <el-button type="success" :loading="exportLoading" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
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
      <el-table-column prop="contentType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_content_type" :value="row.contentType" />
        </template>
      </el-table-column>
      <el-table-column prop="platform" label="平台" width="100">
        <template #default="{ row }">
          <DictLabel dict-type="dict_platform_type" :value="row.platform" />
        </template>
      </el-table-column>
      <el-table-column prop="views" label="播放量" width="120"><template #default="{ row }">{{ row.views.toLocaleString() }}</template></el-table-column>
      <el-table-column prop="likes" label="点赞" width="100"><template #default="{ row }">{{ row.likes.toLocaleString() }}</template></el-table-column>
      <el-table-column prop="hotScore" label="爆款指数" width="120"><template #default="{ row }"><el-rate v-model="row.hotScore" disabled /></template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailVisible" title="爆款作品详情" size="520px">
      <el-descriptions v-if="currentRow" :column="1" border>
        <el-descriptions-item label="作品标题">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="作品类型">
          <DictLabel dict-type="dict_content_type" :value="currentRow.contentType" />
        </el-descriptions-item>
        <el-descriptions-item label="平台">{{ currentRow.platform }}</el-descriptions-item>
        <el-descriptions-item label="播放量">{{ currentRow.views?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="点赞">{{ currentRow.likes?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="爆款指数">{{ currentRow.hotScore }} / 5</el-descriptions-item>
        <el-descriptions-item label="排名">{{ currentRow.rank }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import { getHitWorkList } from '@/api/monitor'
import { exportToExcel } from '@/utils'
import { mapExternalWork, pickMonitorPage } from '@/utils/monitor-map'

interface HotWorkRow {
  rank: number
  title: string
  contentType?: string
  platform: string
  views: number
  likes: number
  hotScore: number
}

const loading = ref(false)
const exportLoading = ref(false)
const searchForm = reactive({ threshold: 1000000, contentType: undefined as string | undefined })
const stats = reactive({ totalHot: 0, totalViews: 0, avgLikes: 0, avgShares: 0 })
const hotWorksList = ref<HotWorkRow[]>([])
const detailVisible = ref(false)
const currentRow = ref<HotWorkRow | null>(null)

const calcHotScore = (views: number) => {
  if (views >= 3_000_000) return 5
  if (views >= 2_000_000) return 4
  if (views >= 1_000_000) return 3
  return 2
}

const buildListParams = (pageNum: number, pageSize: number) => ({
  pageNum,
  pageSize,
  contentType: searchForm.contentType || undefined,
})

const mapHotWorkRows = (list: ReturnType<typeof mapExternalWork>[]) =>
  list
    .filter(w => w.playCount >= searchForm.threshold)
    .map((w, i) => ({
      rank: i + 1,
      title: w.title,
      contentType: w.contentType ?? '',
      platform: w.platform,
      views: w.playCount,
      likes: w.likeCount,
      hotScore: calcHotScore(w.playCount),
    }))

const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first = await getHitWorkList(buildListParams(1, exportPageSize))
  const page = pickMonitorPage(first)
  let works = page.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i))
  const total = page.total ?? 0
  if (total > exportPageSize) {
    const totalPages = Math.ceil(total / exportPageSize)
    for (let p = 2; p <= totalPages; p += 1) {
      const res = await getHitWorkList(buildListParams(p, exportPageSize))
      const pg = pickMonitorPage(res)
      works = works.concat(pg.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i)))
    }
  }
  return mapHotWorkRows(works)
}

const loadData = async () => {
  loading.value = true
  try {
    const page = pickMonitorPage(await getHitWorkList(buildListParams(1, 100)))
    const rows = mapHotWorkRows(page.list.map((raw, i) => mapExternalWork(raw as unknown as Record<string, unknown>, i)))
    hotWorksList.value = rows
    stats.totalHot = rows.length
    stats.totalViews = rows.reduce((s, r) => s + r.views, 0)
    stats.avgLikes = rows.length ? Math.round(rows.reduce((s, r) => s + r.likes, 0) / rows.length) : 0
    stats.avgShares = 0
  } catch (e) {
    console.error(e)
    hotWorksList.value = []
  } finally {
    loading.value = false
  }
}

const handleDetail = (row: HotWorkRow) => {
  currentRow.value = row
  detailVisible.value = true
}

const handleReset = () => {
  searchForm.threshold = 1000000
  searchForm.contentType = undefined
  loadData()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows()
    exportToExcel(
      rows.map((row) => ({ ...row, hotScore: `${row.hotScore} / 5` })),
      [
        { key: 'rank', label: '排名' },
        { key: 'title', label: '作品标题' },
        { key: 'contentType', label: '类型' },
        { key: 'platform', label: '平台' },
        { key: 'views', label: '播放量' },
        { key: 'likes', label: '点赞' },
        { key: 'hotScore', label: '爆款指数' },
      ],
      '爆款作品分析',
    )
  } catch (error) {
    console.error('[HotWorks] 导出失败:', error)
    ElMessage.error('导出失败：' + (error instanceof Error ? error.message : String(error)))
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.hot-works-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
</style>
