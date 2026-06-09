<template>
  <div class="financial-analysis">
    <!-- 查询条件 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryForm" inline>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="IP组">
          <el-select v-model="queryForm.ipGroupId" placeholder="全部" clearable style="width: 150px">
            <el-option label="IP大组A" :value="1" />
            <el-option label="IP大组B" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台类型">
          <el-select v-model="queryForm.platformType" placeholder="全部" clearable style="width: 150px">
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="快手" value="KUAISHOU" />
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="视频号" value="VIDEO_ACCOUNT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">总营收</div>
            <div class="stat-value">¥{{ formatNumber(summary.totalRevenue) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">总成本</div>
            <div class="stat-value">¥{{ formatNumber(summary.totalCost) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">总利润</div>
            <div class="stat-value" :class="summary.totalProfit >= 0 ? 'success' : 'danger'">
              ¥{{ formatNumber(summary.totalProfit) }}
              <el-tag v-if="summary.totalProfit < 0" type="danger" size="small">亏损</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">综合ROI</div>
            <div class="stat-value" :class="summary.overallRoi > 1 ? 'success' : 'danger'">
              {{ summary.overallRoi || 'N/A' }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：双栏 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>按IP组分组营收</span>
            </div>
          </template>
          <div ref="ipGroupChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>按平台分组营收</span>
            </div>
          </template>
          <div ref="platformChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成本结构 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>成本结构</span>
            </div>
          </template>
          <div ref="costChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="table-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>成本明细</span>
            </div>
          </template>
          <el-table :data="costDetailList" border stripe>
            <el-table-column prop="costType" label="成本类型" />
            <el-table-column prop="costAmount" label="金额" align="right">
              <template #default="{ row }">
                ¥{{ formatNumber(row.costAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="costRatio" label="占比" align="right">
              <template #default="{ row }">
                {{ row.costRatio }}%
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { exportToExcel } from '@/utils'

// ==================== 类型定义 ====================
interface CostDetail {
  costType: string
  costAmount: number
  costRatio: number
}

interface IpGroupRevenue {
  name: string
  revenue: number
}

interface PlatformRevenue {
  name: string
  value: number
}

// ==================== 响应式数据 ====================

const queryForm = reactive({
  dateRange: [],
  ipGroupId: null,
  platformType: ''
})

const summary = reactive({
  totalRevenue: 12345678.90,
  totalCost: 8901234.56,
  totalProfit: 3444444.34,
  overallRoi: 1.39
})

// ==================== Mock 数据 ====================
const costDetailList = ref<CostDetail[]>([
  { costType: '购买成本', costAmount: 5000000, costRatio: 56.2 },
  { costType: '过程成本', costAmount: 2500000, costRatio: 28.1 },
  { costType: '人工成本', costAmount: 890000, costRatio: 10.0 },
  { costType: '设备成本', costAmount: 340000, costRatio: 3.8 },
  { costType: '其他成本', costAmount: 171234.56, costRatio: 1.9 }
])

// IP组营收数据
const ipGroupData: IpGroupRevenue[] = [
  { name: 'IP大组A', revenue: 520 },
  { name: 'IP大组B', revenue: 480 },
  { name: 'IP大组C', revenue: 350 },
  { name: 'IP大组D', revenue: 280 },
  { name: 'IP大组E', revenue: 210 }
]

// 平台营收数据
const platformData: PlatformRevenue[] = [
  { name: '抖音', value: 520 },
  { name: '快手', value: 380 },
  { name: '小红书', value: 180 },
  { name: '视频号', value: 154 }
]

const ipGroupChartRef = ref<HTMLElement>()
const platformChartRef = ref<HTMLElement>()
const costChartRef = ref<HTMLElement>()

const formatNumber = (num: number) => {
  return (num / 10000).toFixed(2) + '万'
}

const handleQuery = () => {
  console.log('查询总体财务分析', queryForm)
  ElMessage.success('查询成功')
}

const handleReset = () => {
  queryForm.dateRange = []
  queryForm.ipGroupId = null
  queryForm.platformType = ''
  handleQuery()
}

const handleExport = () => {
  const columns = [
    { key: 'costType', label: '成本类型' },
    { key: 'costAmount', label: '金额' },
    { key: 'costRatio', label: '占比(%)' },
  ]
  exportToExcel(costDetailList.value, columns, '财务分析-成本明细')
}

let finIpGroupChart: echarts.ECharts | null = null
let finPlatformChart: echarts.ECharts | null = null
let finCostChart: echarts.ECharts | null = null

const initIpGroupChart = () => {
  if (!ipGroupChartRef.value || ipGroupChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(initIpGroupChart, 100)
    return
  }
  if (finIpGroupChart) {
    finIpGroupChart.dispose()
    finIpGroupChart = null
  }
  const chart = echarts.init(ipGroupChartRef.value)
  finIpGroupChart = chart
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', name: '营收（万元）' },
    yAxis: {
      type: 'category',
      data: ipGroupData.map(item => item.name),
      inverse: true
    },
    series: [
      {
        name: '营收',
        type: 'bar',
        data: ipGroupData.map(item => item.revenue),
        itemStyle: { color: '#409eff' }
      }
    ]
  }
  chart.setOption(option)
}

const initPlatformChart = () => {
  if (!platformChartRef.value || platformChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(initPlatformChart, 100)
    return
  }
  if (finPlatformChart) {
    finPlatformChart.dispose()
    finPlatformChart = null
  }
  const chart = echarts.init(platformChartRef.value)
  finPlatformChart = chart
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c}万 ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '平台营收',
        type: 'pie',
        radius: '50%',
        data: [
          { value: 520, name: '抖音' },
          { value: 380, name: '快手' },
          { value: 180, name: '小红书' },
          { value: 154, name: '视频号' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  chart.setOption(option)
}

const initCostChart = () => {
  if (!costChartRef.value || costChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(initCostChart, 100)
    return
  }
  if (finCostChart) {
    finCostChart.dispose()
    finCostChart = null
  }
  const chart = echarts.init(costChartRef.value)
  finCostChart = chart
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c}万 ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '成本结构',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        data: [
          { value: 500, name: '购买成本' },
          { value: 250, name: '过程成本' },
          { value: 89, name: '人工成本' },
          { value: 34, name: '设备成本' },
          { value: 17, name: '其他成本' }
        ]
      }
    ]
  }
  chart.setOption(option)
}

onMounted(() => {
  initIpGroupChart()
  initPlatformChart()
  initCostChart()
})
</script>

<style scoped lang="scss">
.financial-analysis {
  .search-card {
    margin-bottom: 16px;
  }

  .stats-row {
    margin-bottom: 16px;
  }

  .stat-item {
    text-align: center;
    padding: 10px 0;

    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #303133;

      &.success {
        color: #67c23a;
      }

      &.danger {
        color: #f56c6c;
      }
    }
  }

  .chart-card,
  .table-card {
    margin-bottom: 16px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>
