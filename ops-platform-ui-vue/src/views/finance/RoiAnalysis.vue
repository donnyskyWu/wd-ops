<template>
  <div class="roi-analysis-page">
    <!-- 搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" :rules="searchRules" ref="searchFormRef" :inline="true">
        <el-form-item label="日期范围" prop="dateRange">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="~" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="维度" prop="dimension">
          <DictSelect v-model="searchForm.dimension" dict-type="dict_roi_dimension" placeholder="请选择" style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleTrend">趋势分析</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleExport">导出报告</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value primary">¥{{ stats.totalRevenue.toLocaleString() }}</div>
            <div class="stat-label">总营收</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value danger">¥{{ stats.totalCost.toLocaleString() }}</div>
            <div class="stat-label">总成本</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value success">¥{{ stats.totalProfit.toLocaleString() }}</div>
            <div class="stat-label">总利润</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value warning">{{ stats.roi.toFixed(2) }}</div>
            <div class="stat-label">ROI</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" class="roi-tabs">
      <el-tab-pane label="ROI对比" name="compare">
        <el-table :data="roiTable" stripe v-loading="loading">
          <template #empty>
            <el-empty description="暂无ROI数据" />
          </template>
          <el-table-column prop="name" label="维度名称" width="150" />
          <el-table-column prop="revenue" label="营收" width="140" align="right"><template #default="{ row }">¥{{ row.revenue.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="cost" label="总成本" width="140" align="right"><template #default="{ row }">¥{{ row.cost.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="profit" label="利润" width="140" align="right"><template #default="{ row }">¥{{ row.profit.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="roi" label="ROI" width="100" align="center"><template #default="{ row }"><el-tag :type="row.roi >= 3 ? 'success' : row.roi >= 2 ? 'warning' : 'danger'">{{ row.roi.toFixed(2) }}</el-tag></template></el-table-column>
          <el-table-column prop="trend" label="趋势" width="80" align="center"><template #default="{ row }">{{ row.trend }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
      <!-- 趋势分析 -->
      <el-tab-pane label="趋势分析" name="trend">
        <!-- 趋势图表 -->
        <div ref="trendChartRef" style="height: 400px"></div>
      </el-tab-pane>

      <!-- 成本结构 -->
      <el-tab-pane label="成本结构" name="structure">
        <!-- 成本结构图表 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><div class="card-header">成本类型占比（饼图）</div></template>
              <div ref="structurePieRef" style="height: 350px;"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><div class="card-header">成本类型趋势（柱状图）</div></template>
              <div ref="structureBarRef" style="height: 350px;"></div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 成本明细表格 -->
        <el-card shadow="hover" style="margin-top: 16px;">
          <template #header><div class="card-header">成本明细</div></template>
          <el-table :data="costDetailList" border stripe>
            <el-table-column prop="costType" label="成本类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getCostTypeColor(row.type)">{{ row.costType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额（元）" width="140" align="right">
              <template #default="{ row }">¥{{ row.amount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="ratio" label="占比" width="100" align="center">
              <template #default="{ row }">{{ (row.ratio * 100).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column prop="avgPerAccount" label="账号均摊" width="140" align="right">
              <template #default="{ row }">¥{{ row.avgPerAccount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="changeRate" label="环比变化" width="100" align="center">
              <template #default="{ row }">
                <span :class="row.changeRate >= 0 ? 'text-danger' : 'text-success'">
                  {{ row.changeRate >= 0 ? '+' : '' }}{{ (row.changeRate * 100).toFixed(1) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as echarts from 'echarts'
import { exportToExcel } from '@/utils'
import DictSelect from '@/components/DictSelect.vue'

const searchFormRef = ref<FormInstance>()
const loading = ref(false)
const router = useRouter()

const searchForm = reactive({
  dateRange: ['2026-05-01', '2026-05-31'] as string[],
  dimension: 'ip_group'
})

const searchRules = reactive<FormRules>({
  dateRange: [
    { required: true, message: '请选择日期范围', trigger: 'change' }
  ],
  dimension: [
    { required: true, message: '请选择分析维度', trigger: 'change' }
  ]
})

const activeTab = ref('compare')
const trendChartRef = ref<HTMLElement>()
const structurePieRef = ref<HTMLElement>()
const structureBarRef = ref<HTMLElement>()

// ==================== 趋势分析数据 ====================
const trendStats = reactive({
  avgRoi: 2.94,
  roiGrowth: 12.5,
  avgRevenue: 77284,
  avgCost: 25760,
})

// ==================== 成本结构数据 ====================
const structureStats = reactive({
  totalCost: 128800,
  personCost: 55000,
  contentCost: 42800,
  promoteCost: 31000,
})

const costDetailList = ref([
  { type: 'primary', costType: '人工成本', amount: 55000, ratio: 0.427, avgPerAccount: 12800, changeRate: 0.05, description: '内容运营人员薪酬及社保' },
  { type: 'success', costType: '内容制作', amount: 42800, ratio: 0.332, avgPerAccount: 9970, changeRate: -0.02, description: '视频拍摄、剪辑、配音等制作费用' },
  { type: 'warning', costType: '推广费用', amount: 31000, ratio: 0.241, avgPerAccount: 7230, changeRate: 0.15, description: 'dou+、信息流广告投放费用' },
])

const getCostTypeColor = (type: string): string => {
  const map: Record<string, string> = {
    primary: 'primary',
    success: 'success',
    warning: 'warning',
    info: 'info',
  }
  return map[type] || ''
}

const stats = reactive({
  totalRevenue: 386420,
  totalCost: 128800,
  totalProfit: 257620,
  roi: 3.00,
})

const roiTable = ref([
  { name: 'A组', revenue: 220000, cost: 65000, profit: 155000, roi: 3.38, trend: '📈' },
  { name: 'B组', revenue: 166420, cost: 63800, profit: 102620, roi: 2.61, trend: '📉' },
])

const handleSearch = async () => {
  if (!searchFormRef.value) return
  
  try {
    await searchFormRef.value.validate()
    loading.value = true
    ElMessage.success('查询成功')
    loadChart()
  } catch (error) {
    console.error('表单校验失败', error)
  } finally {
    loading.value = false
  }
}
const handleTrend = () => {
  router.push('/finance/roi/trend')
}
const handleExport = () => {
  const columns = [
    { key: 'name', label: '维度名称' },
    { key: 'revenue', label: '营收' },
    { key: 'cost', label: '总成本' },
    { key: 'profit', label: '利润' },
    { key: 'roi', label: 'ROI' },
    { key: 'trend', label: '趋势' }
  ]
  
  // 格式化数据
  const exportData = roiTable.value.map(row => ({
    ...row,
    revenue: `¥${row.revenue.toLocaleString()}`,
    cost: `¥${row.cost.toLocaleString()}`,
    profit: `¥${row.profit.toLocaleString()}`,
    roi: row.roi.toFixed(2)
  }))
  
  exportToExcel(exportData, columns, 'ROI分析报告')
}

const loadChart = async () => {
  await nextTick()
  
  // 趋势分析图表
  if (activeTab.value === 'trend' && trendChartRef.value) {
    const chart = echarts.init(trendChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['营收', '成本', 'ROI'] },
      xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月'] },
      yAxis: [
        { type: 'value', name: '金额(元)', position: 'left' },
        { type: 'value', name: 'ROI', position: 'right', min: 0, max: 5 }
      ],
      series: [
        { name: '营收', type: 'bar', data: [50000, 60000, 70000, 80000, 126420] },
        { name: '成本', type: 'bar', data: [20000, 25000, 28000, 30000, 25800] },
        { name: 'ROI', type: 'line', yAxisIndex: 1, data: [2.5, 2.4, 2.5, 2.67, 4.9] }
      ]
    })
  }
  
  // 成本结构-饼图
  if (activeTab.value === 'structure' && structurePieRef.value) {
    const pieChart = echarts.init(structurePieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%' },
        data: [
          { value: 55000, name: '人工成本', itemStyle: { color: '#409eff' } },
          { value: 42800, name: '内容制作', itemStyle: { color: '#67c23a' } },
          { value: 31000, name: '推广费用', itemStyle: { color: '#e6a23c' } },
        ]
      }]
    })
  }
  
  // 成本结构-柱状图
  if (activeTab.value === 'structure' && structureBarRef.value) {
    const barChart = echarts.init(structureBarRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['人工成本', '内容制作', '推广费用'] },
      xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月'] },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [
        { name: '人工成本', type: 'bar', data: [10000, 11000, 10500, 11500, 12000] },
        { name: '内容制作', type: 'bar', data: [8000, 8500, 8200, 8800, 9300] },
        { name: '推广费用', type: 'bar', data: [5000, 5500, 5800, 6500, 8200] },
      ]
    })
  }
}

onMounted(() => loadChart())

// 监听标签页切换，重新渲染图表
watch(activeTab, () => {
  loadChart()
})

// 窗口变化时重新渲染图表
const handleResize = () => {
  loadChart()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.roi-analysis-page { padding: 20px; }
.search-card { margin-bottom: 16px; }
.stats-cards { margin-bottom: 20px; }
.trend-stats { margin-bottom: 16px; }
.structure-stats { margin-bottom: 16px; }
.stat-item { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-value.primary { color: #409eff; }
.stat-value.danger { color: #f56c6c; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-value.info { color: #909399; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.roi-tabs { margin-top: 16px; }
.card-header { font-weight: 600; font-size: 14px; }
.text-danger { color: #f56c6c; }
.text-success { color: #67c23a; }
</style>
