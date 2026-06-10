<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>短视频产出</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" style="width: 220px" />
        </el-form-item>
        <el-form-item label="账号">
          <AccountSelect v-model="filter.accountId" :ip-group-id="filter.ipGroupId" style="width: 200px" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <ContentWrap title="日产出趋势">
          <div ref="trendRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="账号产出排行" style="margin-top: 16px">
      <el-table :data="ranking" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="account_name" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.platform || row.platform_type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="output_count" label="产出数" width="100" align="right" />
        <el-table-column prop="avg_view" label="均播放" width="120" align="right">
          <template #default="{ row }">{{ formatK(row.avg_view) }}</template>
        </el-table-column>
        <el-table-column prop="top_count" label="爆款数" width="100" align="right">
          <template #default="{ row }">
            <el-tag :type="(row.top_count || 0) > 3 ? 'success' : 'info'" size="small">{{ row.top_count || 0 }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <ContentWrap title="产出明细" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="account_name" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platform_type" label="平台" width="100" align="center" />
        <el-table-column prop="output_count" label="产出数" width="100" align="right" />
        <el-table-column prop="read_count" label="阅读数" width="120" align="right" />
        <el-table-column prop="like_count" label="点赞数" width="100" align="right" />
      </el-table>
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @update:current-page="(v) => { pageNum = v; loadData() }"
        @update:page-size="(v) => { pageSize = v; pageNum = 1; loadData() }"
      />
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import { getVideoOutputList, getVideoOutputTrend, getVideoOutputRanking } from '@/api/report'

const loading = ref(false)
const filter = reactive({
  ipGroupId: undefined as number | undefined,
  accountId: undefined as number | undefined,
  dateRange: [] as string[],
})
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const list = ref<any[]>([])
const ranking = ref<any[]>([])
const trendRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null

const formatK = (v: any) => {
  const n = Number(v)
  if (!n) return 0
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n
}

const buildQuery = () => {
  const q: Record<string, any> = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filter.ipGroupId) q.ipGroupId = filter.ipGroupId
  if (filter.accountId) q.accountId = filter.accountId
  if (filter.dateRange?.length === 2) {
    q.startDate = filter.dateRange[0]
    q.endDate = filter.dateRange[1]
  }
  return q
}

const loadData = async () => {
  loading.value = true
  try {
    const q = buildQuery()
    const [listRes, rankRes, trendRes] = await Promise.all([
      getVideoOutputList(q),
      getVideoOutputRanking({ startDate: q.startDate, endDate: q.endDate, limit: 10 }),
      getVideoOutputTrend({ accountId: filter.accountId, startDate: q.startDate, endDate: q.endDate }),
    ])
    const l = (listRes as any)?.data ?? listRes
    list.value = l?.list ?? l?.records ?? []
    total.value = l?.total ?? list.value.length
    const r = (rankRes as any)?.data ?? rankRes
    ranking.value = Array.isArray(r) ? r : []
    const t = (trendRes as any)?.data ?? trendRes
    drawTrend(Array.isArray(t) ? t : [])
  } catch (e) {
    console.error('loadData failed', e)
    list.value = []
    ranking.value = []
  } finally {
    loading.value = false
  }
}

const drawTrend = (rows: any[]) => {
  if (!trendRef.value) return
  const el = trendRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(() => drawTrend(rows), 100)
    return
  }
  if (!trendChart) trendChart = echarts.init(el)
  const dates = Array.from(new Set(rows.map((r: any) => r.date || r.stat_date))).sort()
  const platforms = Array.from(new Set(rows.map((r: any) => r.platform || r.platform_type)))
  const series = platforms.map(p => ({
    name: p,
    type: 'line',
    smooth: true,
    data: dates.map(d => {
      const row = rows.find((r: any) => (r.date || r.stat_date) === d && (r.platform || r.platform_type) === p)
      return row ? (row.output_count || row.count || 0) : 0
    }),
  }))
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: platforms, top: 0 },
    grid: { left: 40, right: 16, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series,
  })
}

const handleReset = () => {
  filter.ipGroupId = undefined
  filter.accountId = undefined
  filter.dateRange = []
  pageNum.value = 1
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.report-page { padding: 20px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
