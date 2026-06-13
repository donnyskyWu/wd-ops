<template>
  <div class="ip-theme-page">
    <div class="page-header">
      <h3 class="page-title">IP主题与行业数据</h3>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="main-tabs" @tab-change="handleTabChange">
      <!-- IP主题数据 -->
      <el-tab-pane label="IP主题数据" name="ip-theme" :lazy="false">
        <div class="tab-content">
          <el-card shadow="never" class="filter-card">
            <el-form :model="ipThemeForm" inline>
              <el-form-item label="IP组">
                <IpGroupTreeSelect v-model="ipThemeForm.ipGroupId" style="width: 220px" @change="loadIpThemeData" />
              </el-form-item>
              <el-form-item>
                <el-button type="success" :loading="exportLoading" @click="handleExport">
                  <el-icon><Download /></el-icon>
                  导出
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 统计卡片 -->
          <el-row :gutter="16" class="stats-row">
            <el-col :span="8">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-value primary">{{ ipThemeStats.relatedAccountCount }}</div>
                <div class="stat-label">关联账号</div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-value success">{{ ipThemeStats.totalInteraction.toLocaleString() }}</div>
                <div class="stat-label">总互动量</div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-value" :class="ipThemeStats.avgGrowthRate >= 0 ? 'text-success' : 'text-danger'">
                  {{ ipThemeStats.avgGrowthRate >= 0 ? '+' : '' }}{{ ipThemeStats.avgGrowthRate }}%
                </div>
                <div class="stat-label">平均增长率</div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 数据图表 -->
          <el-row :gutter="16" class="chart-row">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header><div class="card-header">竞品账号分布</div></template>
                <div ref="ipThemeBarRef" style="height: 300px;"></div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header><div class="card-header">互动趋势</div></template>
                <div ref="ipThemeLineRef" style="height: 300px;"></div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 竞品账号列表 -->
          <el-card shadow="hover" class="table-card">
            <template #header><div class="card-header">竞品账号列表</div></template>
            <el-table :data="competitorList" v-loading="loading" stripe>
              <el-table-column prop="platform" label="平台" width="100">
                <template #default="{ row }">
                  <el-tag>{{ row.platform }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="accountName" label="账号名称" min-width="150" />
              <el-table-column prop="followerCount" label="粉丝数" width="120">
                <template #default="{ row }">{{ row.followerCount.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column prop="interactionCount" label="互动量" width="120">
                <template #default="{ row }">{{ row.interactionCount.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column prop="contentCount" label="内容数" width="100" />
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goToExternalAccount(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 行业数据 -->
      <el-tab-pane label="行业数据" name="industry" :lazy="false">
        <div class="tab-content">
          <el-card shadow="never" class="filter-card">
            <el-form :model="industryForm" inline>
              <el-form-item label="行业">
                <el-select v-model="industryForm.industryId" placeholder="全部" clearable style="width: 160px">
                  <el-option label="科技数码" value="1" />
                  <el-option label="美妆护肤" value="2" />
                  <el-option label="食品饮料" value="3" />
                  <el-option label="家居生活" value="4" />
                </el-select>
              </el-form-item>
              <el-form-item label="子行业">
                <el-select v-model="industryForm.subIndustryId" placeholder="全部" clearable style="width: 160px">
                  <el-option label="手机数码" value="1" />
                  <el-option label="电脑办公" value="2" />
                  <el-option label="智能穿戴" value="3" />
                </el-select>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 行业概览卡片 -->
          <el-row :gutter="16" class="stats-row">
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-value primary">{{ industryStats.totalAccountCount.toLocaleString() }}</div>
                <div class="stat-label">行业账号</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-value success">{{ industryStats.totalInteraction.toLocaleString() }}</div>
                <div class="stat-label">行业互动</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-value" :class="industryStats.growthRate >= 0 ? 'text-success' : 'text-danger'">
                  {{ industryStats.growthRate >= 0 ? '+' : '' }}{{ industryStats.growthRate }}%
                </div>
                <div class="stat-label">行业增速</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-value warning">{{ industryStats.topRatio }}%</div>
                <div class="stat-label">头部占比</div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 行业分布图 -->
          <el-card shadow="hover" class="chart-card">
            <template #header><div class="card-header">子行业分布</div></template>
            <div ref="industryPieRef" style="height: 300px;"></div>
          </el-card>

          <!-- 行业排行 -->
          <el-card shadow="hover" class="table-card">
            <template #header><div class="card-header">行业排行</div></template>
            <el-table :data="subIndustryRankList" v-loading="loading" stripe>
              <el-table-column prop="rank" label="排名" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.rank <= 3 ? 'danger' : ''">{{ row.rank }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="子行业" min-width="120" />
              <el-table-column prop="accountCount" label="账号数" width="120">
                <template #default="{ row }">{{ row.accountCount.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column prop="interactionCount" label="互动量" width="120">
                <template #default="{ row }">{{ row.interactionCount.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column prop="growthRate" label="增速" width="100">
                <template #default="{ row }">
                  <span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">
                    {{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate }}%
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { exportToExcel, unwrapApiData } from '@/utils'
import { getIpThemeStats, getExternalWorkList } from '@/api/monitor'
import { mapExternalWork, pickMonitorPage } from '@/utils/monitor-map'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'

const router = useRouter()
const activeTab = ref('ip-theme')
const loading = ref(false)
const exportLoading = ref(false)

// IP主题数据
const ipThemeForm = reactive({ ipGroupId: undefined as number | undefined })
const ipThemeStats = reactive({ relatedAccountCount: 0, totalInteraction: 0, avgGrowthRate: 0 })
const competitorList = ref<Array<{ platform: string; accountName: string; followerCount: number; interactionCount: number; contentCount: number }>>([])

// 行业数据
const industryForm = reactive({
  industryId: undefined as string | undefined,
  subIndustryId: undefined as string | undefined
})
const industryStats = reactive({ totalAccountCount: 1234, totalInteraction: 567890, growthRate: 8.9, topRatio: 35 })
const subIndustryRankList = ref([
  { rank: 1, name: '美妆', accountCount: 456, interactionCount: 89012, growthRate: 12.5 },
  { rank: 2, name: '护肤', accountCount: 389, interactionCount: 75600, growthRate: 10.2 },
  { rank: 3, name: '个护', accountCount: 256, interactionCount: 48000, growthRate: 8.6 },
  { rank: 4, name: '发型', accountCount: 198, interactionCount: 32000, growthRate: 6.3 },
  { rank: 5, name: '穿搭', accountCount: 145, interactionCount: 28000, growthRate: 5.8 },
])
// 子行业分布饼图数据
const subIndustryPieData = computed(() =>
  subIndustryRankList.value.map(item => ({ value: item.accountCount, name: item.name }))
)

// 图表 refs
const ipThemeBarRef = ref<HTMLElement>()
const ipThemeLineRef = ref<HTMLElement>()
const industryPieRef = ref<HTMLElement>()

// 导出
const buildListParams = (pageNum: number, pageSize: number) => ({
  ipGroupId: ipThemeForm.ipGroupId,
  pageNum,
  pageSize,
})

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
    platform: w.platform,
    accountName: w.accountName,
    title: w.title,
    followerCount: w.playCount,
    interactionCount: w.likeCount,
    contentCount: 1,
  }))
}

const handleExport = async () => {
  if (!ipThemeForm.ipGroupId) {
    ElMessage.warning('请先选择 IP 组')
    return
  }
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows()
    exportToExcel(rows, [
      { key: 'platform', label: '平台' },
      { key: 'accountName', label: '账号名称' },
      { key: 'title', label: '作品标题' },
      { key: 'followerCount', label: '播放量' },
      { key: 'interactionCount', label: '互动量' },
      { key: 'contentCount', label: '内容数' },
    ], 'IP主题数据')
  } catch (error) {
    console.error('[IPTheme] 导出失败:', error)
    ElMessage.error('导出失败：' + (error instanceof Error ? error.message : String(error)))
  } finally {
    exportLoading.value = false
  }
}
// 刷新
const handleRefresh = () => { loadIpThemeData() }

const loadIpThemeData = async () => {
  if (!ipThemeForm.ipGroupId) return
  loading.value = true
  try {
    const [themeRes, worksRes] = await Promise.all([
      getIpThemeStats(ipThemeForm.ipGroupId),
      getExternalWorkList({ ipGroupId: ipThemeForm.ipGroupId, pageNum: 1, pageSize: 100 }),
    ])
    const theme = unwrapApiData(themeRes) as { workCount?: number; totalPlay?: number; topTitles?: string[] }
    ipThemeStats.relatedAccountCount = theme.workCount ?? 0
    ipThemeStats.totalInteraction = theme.totalPlay ?? 0
    ipThemeStats.avgGrowthRate = 0
    const page = pickMonitorPage(worksRes)
    competitorList.value = page.list.map((raw, i) => {
      const w = mapExternalWork(raw as unknown as Record<string, unknown>, i)
      return {
        platform: w.platform,
        accountName: w.title,
        followerCount: w.playCount,
        interactionCount: w.likeCount,
        contentCount: 1,
      }
    })
    loadCharts()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
// 跳转外部账号分析
const goToExternalAccount = (row: any) => { router.push('/external-account') }

// 加载图表
const loadCharts = async () => {
  await nextTick()

  // IP主题 - 竞品账号分布柱状图
  if (ipThemeBarRef.value) {
    const barChart = echarts.init(ipThemeBarRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['科技小达人', '数码测评官', '手机评测师', '科技前沿', '数码之家', '科技爱好者'] },
      yAxis: { type: 'value', name: '粉丝数' },
      series: [{ type: 'bar', data: [520, 280, 180, 89, 56, 32], itemStyle: { color: '#409eff' } }]
    })
  }

  // IP主题 - 互动趋势折线图
  if (ipThemeLineRef.value) {
    const lineChart = echarts.init(ipThemeLineRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: [32000, 38000, 42000, 38000, 45000, 48000], smooth: true, areaStyle: {} }]
    })
  }

  // 行业分布饼图
  if (industryPieRef.value) {
    initIndustryChart()
  }
}

// 行业分布饼图初始化(独立函数,用于 Tab 切换重新调用)
const initIndustryChart = () => {
  if (!industryPieRef.value) {
    console.warn('[IPTheme] industryPieRef is null, 重试中...')
    setTimeout(initIndustryChart, 100)
    return
  }
  const el = industryPieRef.value
  // 检测容器是否隐藏(display:none) — el-tab-pane 隐藏时 width/height 为 0
  const rect = el.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) {
    console.warn('[IPTheme] 饼图容器为 0,等待后重试...')
    setTimeout(initIndustryChart, 100)
    return
  }
  // 销毁旧实例(避免重复 init 报错)
  const existing = echarts.getInstanceByDom(el)
  if (existing) existing.dispose()
  const pieChart = echarts.init(el)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    color: ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b59b6', '#1abc9c', '#e74c3c'],
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: subIndustryPieData.value
    }]
  })
  console.log('[IPTheme] 饼图初始化完成, 容器尺寸:', rect.width, 'x', rect.height)
  window.addEventListener('resize', () => pieChart.resize())
}

// Tab 切换处理(用 el-tabs 自带的 tab-change 事件,比 watch 更可靠)
const handleTabChange = (val: string | number) => {
  if (val === 'industry') {
    // el-tab-pane 切换后会异步挂载,需要等容器可见后再画图
    // 50ms 后第一次尝试,容器不可见时 setTimeout 自重试
    setTimeout(() => initIndustryChart(), 50)
  }
}

onMounted(() => {
  loadCharts()
  // industry tab 用 :lazy="false" 之后,pane 已渲染,这里也初始化一次
  // (如果用户停留 ip-theme tab,初次挂载时 industry div 仍 display:none,
  //  initIndustryChart 内部有 setTimeout 自重试,会等到 tab 切换再画)
  setTimeout(() => initIndustryChart(), 100)
})
</script>

<style scoped>
.ip-theme-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.header-actions { display: flex; gap: 8px; }
.main-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.tab-content { display: flex; flex-direction: column; gap: 16px; }
.filter-card { margin-bottom: 0; }
.stats-row { margin-bottom: 0; }
.chart-row { margin-bottom: 0; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: 600; }
.stat-value.primary { color: #409eff; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
.card-header { font-weight: 600; }
.table-card { margin-top: 0; }
.chart-card { margin-bottom: 0; }
</style>
