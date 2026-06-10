<template>
  <div class="funnel-analysis">
    <!-- Tab切换 -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="预设漏斗" name="preset" />
        <el-tab-pane label="自定义漏斗" name="custom" />
      </el-tabs>
    </el-card>

    <!-- 预设漏斗 -->
    <div v-if="activeTab === 'preset'">
      <!-- 查询条件 -->
      <el-card class="search-card" shadow="never">
        <el-form :model="queryForm" inline>
          <el-form-item label="漏斗类型">
            <el-select v-model="queryForm.funnelType" style="width: 150px">
              <el-option label="内容漏斗" value="CONTENT" />
              <el-option label="粉丝漏斗" value="FOLLOWER" />
              <el-option label="订单漏斗" value="ORDER" />
            </el-select>
          </el-form-item>
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
          <el-form-item>
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="success" @click="handleExport">导出报告</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 对比功能 -->
      <el-card class="compare-card" shadow="never">
        <el-switch v-model="enableCompare" active-text="开启对比" />
        <el-form v-if="enableCompare" inline style="margin-left: 16px">
          <el-form-item label="对比时间">
            <el-date-picker
              v-model="compareDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 漏斗图 -->
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>漏斗转化图</span>
          </div>
        </template>
        <div ref="funnelChartRef" style="height: 450px"></div>
      </el-card>

      <!-- 转化率表格 -->
      <el-card class="table-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>转化率明细</span>
          </div>
        </template>
        <el-table :data="funnelSteps" border stripe>
          <el-table-column prop="stepName" label="步骤" width="150" />
          <el-table-column prop="stepCount" label="数量" width="120" align="right">
            <template #default="{ row }">
              {{ formatCount(row.stepCount) }}
            </template>
          </el-table-column>
          <el-table-column prop="conversionRate" label="上一步转化率" width="140" align="right">
            <template #default="{ row }">
              <span v-if="row.conversionRate">{{ row.conversionRate }}%</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="overallConversionRate" label="总体转化率" width="140" align="right">
            <template #default="{ row }">
              {{ row.overallConversionRate }}%
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 自定义漏斗 -->
    <div v-else>
      <el-card class="custom-funnel-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>我的自定义漏斗</span>
            <el-button type="primary" @click="showCreateDialog = true">新建自定义漏斗</el-button>
          </div>
        </template>

        <el-table :data="customFunnelList" border stripe>
          <el-table-column prop="funnelName" label="漏斗名称" />
          <el-table-column prop="stepCount" label="步骤数" width="100" align="center" />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="280" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleExecute(row)">执行</el-button>
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="showCreateDialog" :title="dialogTitle" width="800px">
      <el-form :model="funnelForm" label-width="100px">
        <el-form-item label="漏斗名称">
          <el-input v-model="funnelForm.funnelName" placeholder="请输入漏斗名称" />
        </el-form-item>
        <el-form-item label="步骤列表">
          <div class="steps-list">
            <div v-for="(step, index) in funnelForm.steps" :key="index" class="step-item">
              <el-icon class="drag-handle"><Rank /></el-icon>
              <span class="step-index">{{ index + 1 }}.</span>
              <el-input v-model="step.stepName" placeholder="步骤名称" style="width: 200px" />
              <el-select v-model="step.dataSource" placeholder="数据源" style="width: 150px; margin-left: 8px">
                <el-option label="曝光数据" value="exposure" />
                <el-option label="阅读数据" value="read" />
                <el-option label="互动数据" value="interaction" />
                <el-option label="订单数据" value="order" />
              </el-select>
              <el-icon class="delete-icon" @click="removeStep(index)"><Delete /></el-icon>
            </div>
            <el-button @click="addStep" style="margin-top: 8px">+ 添加步骤</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Rank, Delete } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { exportToExcel } from '@/utils'

// ==================== 类型定义 ====================
interface FunnelStep {
  stepName: string
  stepCount: number
  conversionRate: number | null
  overallConversionRate: number
}

interface CustomFunnel {
  id: number
  funnelName: string
  stepCount: number
  createTime: string
  steps?: Array<{ stepName: string; dataSource: string; sortOrder: number }>
}

// ==================== 响应式数据 ====================

const activeTab = ref('preset')
const enableCompare = ref(false)
const compareDateRange = ref([])
const showCreateDialog = ref(false)

const queryForm = reactive({
  funnelType: 'CONTENT',
  dateRange: [],
  ipGroupId: null
})

// ==================== Mock 数据 ====================
// 预设漏斗数据 - 内容漏斗
const contentFunnelData: FunnelStep[] = [
  { stepName: '曝光', stepCount: 100000, conversionRate: null, overallConversionRate: 100 },
  { stepName: '阅读/播放', stepCount: 45000, conversionRate: 45, overallConversionRate: 45 },
  { stepName: '点赞', stepCount: 8200, conversionRate: 18.2, overallConversionRate: 8.2 },
  { stepName: '评论', stepCount: 2100, conversionRate: 25.6, overallConversionRate: 2.1 },
  { stepName: '转发', stepCount: 950, conversionRate: 45.2, overallConversionRate: 0.95 }
]

// 预设漏斗数据 - 粉丝漏斗
const followerFunnelData: FunnelStep[] = [
  { stepName: '视频观看', stepCount: 80000, conversionRate: null, overallConversionRate: 100 },
  { stepName: '进入主页', stepCount: 12000, conversionRate: 15, overallConversionRate: 15 },
  { stepName: '点击关注', stepCount: 3600, conversionRate: 30, overallConversionRate: 4.5 },
  { stepName: '成为粉丝', stepCount: 3200, conversionRate: 88.9, overallConversionRate: 4 }
]

// 预设漏斗数据 - 订单漏斗
const orderFunnelData: FunnelStep[] = [
  { stepName: '商品曝光', stepCount: 50000, conversionRate: null, overallConversionRate: 100 },
  { stepName: '点击进入', stepCount: 8500, conversionRate: 17, overallConversionRate: 17 },
  { stepName: '加入购物车', stepCount: 2100, conversionRate: 24.7, overallConversionRate: 4.2 },
  { stepName: '提交订单', stepCount: 850, conversionRate: 40.5, overallConversionRate: 1.7 },
  { stepName: '完成支付', stepCount: 720, conversionRate: 84.7, overallConversionRate: 1.44 }
]

// 根据漏斗类型动态获取数据
const funnelSteps = computed(() => {
  const map: Record<string, FunnelStep[]> = {
    CONTENT: contentFunnelData,
    FOLLOWER: followerFunnelData,
    ORDER: orderFunnelData
  }
  return map[queryForm.funnelType] || contentFunnelData
})

// 自定义漏斗列表
const customFunnelList = ref<CustomFunnel[]>([
  { 
    id: 1, 
    funnelName: '电商转化漏斗', 
    stepCount: 4, 
    createTime: '2026-05-15 10:30:00',
    steps: [
      { stepName: '访问店铺', dataSource: 'exposure', sortOrder: 1 },
      { stepName: '浏览商品', dataSource: 'read', sortOrder: 2 },
      { stepName: '加入购物车', dataSource: 'interaction', sortOrder: 3 },
      { stepName: '完成购买', dataSource: 'order', sortOrder: 4 }
    ]
  },
  { 
    id: 2, 
    funnelName: '私域引流漏斗', 
    stepCount: 3, 
    createTime: '2026-05-10 14:20:00',
    steps: [
      { stepName: '公域曝光', dataSource: 'exposure', sortOrder: 1 },
      { stepName: '关注公众号', dataSource: 'interaction', sortOrder: 2 },
      { stepName: '添加企微', dataSource: 'interaction', sortOrder: 3 }
    ]
  },
  {
    id: 3,
    funnelName: '直播转化漏斗',
    stepCount: 5,
    createTime: '2026-05-08 09:15:00',
    steps: [
      { stepName: '直播间进入', dataSource: 'exposure', sortOrder: 1 },
      { stepName: '停留5分钟', dataSource: 'read', sortOrder: 2 },
      { stepName: '点击购物车', dataSource: 'interaction', sortOrder: 3 },
      { stepName: '提交订单', dataSource: 'order', sortOrder: 4 },
      { stepName: '完成支付', dataSource: 'order', sortOrder: 5 }
    ]
  }
])

const funnelForm = reactive({
  funnelName: '',
  steps: [
    { stepName: '访问首页', dataSource: 'exposure', sortOrder: 1 },
    { stepName: '点击商品', dataSource: 'read', sortOrder: 2 }
  ]
})

const dialogTitle = computed(() => '新建自定义漏斗')

const funnelChartRef = ref<HTMLElement>()

const formatCount = (count: any) => {
  const v = typeof count === 'number' && !isNaN(count) ? count : 0
  if (v >= 1000) {
    return (v / 1000).toFixed(1) + 'K'
  }
  return v.toString()
}

const handleQuery = () => {
  console.log('查询漏斗分析', queryForm)
  ElMessage.success(`查询成功：${getFunnelTypeLabel(queryForm.funnelType)}`)
  initFunnelChart()
}

const getFunnelTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    CONTENT: '内容漏斗',
    FOLLOWER: '粉丝漏斗',
    ORDER: '订单漏斗'
  }
  return map[type] || type
}

const handleReset = () => {
  queryForm.funnelType = 'CONTENT'
  queryForm.dateRange = []
  queryForm.ipGroupId = null
  handleQuery()
}

const handleExport = () => {
  const columns = [
    { key: 'stepName', label: '漏斗步骤' },
    { key: 'stepCount', label: '用户数' },
    { key: 'conversionRate', label: '转化率' },
    { key: 'overallConversionRate', label: '总转化率' }
  ]
  
  const exportData = funnelData.value.map(row => ({
    ...row,
    conversionRate: row.conversionRate ? `${row.conversionRate}%` : '-',
    overallConversionRate: `${row.overallConversionRate}%`
  }))
  
  exportToExcel(exportData, columns, '漏斗分析报告')
}

const addStep = () => {
  if (funnelForm.steps.length >= 10) {
    ElMessage.warning('最多10个步骤')
    return
  }
  funnelForm.steps.push({
    stepName: '',
    dataSource: '',
    sortOrder: funnelForm.steps.length + 1
  })
}

const removeStep = (index: number) => {
  if (funnelForm.steps.length <= 2) {
    ElMessage.warning('至少需要2个步骤')
    return
  }
  funnelForm.steps.splice(index, 1)
}

const handleSave = () => {
  if (!funnelForm.funnelName) {
    ElMessage.warning('请输入漏斗名称')
    return
  }
  if (funnelForm.steps.length < 2) {
    ElMessage.warning('至少需要2个步骤')
    return
  }
  ElMessage.success('保存成功')
  showCreateDialog.value = false
}

const handleExecute = (row: CustomFunnel) => {
  ElMessage.info('执行自定义漏斗: ' + row.funnelName)
}

const handleEdit = (row: CustomFunnel) => {
  ElMessage.info('编辑漏斗: ' + row.funnelName)
}

const handleDelete = (row: CustomFunnel) => {
  ElMessageBox.confirm('确定删除该漏斗吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {})
}

let funnelChart: echarts.ECharts | null = null
const initFunnelChart = () => {
  if (!funnelChartRef.value || funnelChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(initFunnelChart, 100)
    return
  }
  if (funnelChart) {
    funnelChart.dispose()
    funnelChart = null
  }
  const chart = echarts.init(funnelChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    series: [
      {
        name: '漏斗分析',
        type: 'funnel',
        left: '10%',
        top: 60,
        bottom: 60,
        width: '80%',
        min: 0,
        max: 100000,
        minSize: '0%',
        maxSize: '100%',
        sort: 'descending',
        gap: 2,
        label: {
          show: true,
          position: 'inside',
          formatter: '{b}: {c}'
        },
        labelLine: {
          length: 10,
          lineStyle: {
            width: 1,
            type: 'solid'
          }
        },
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 1
        },
        emphasis: {
          label: {
            fontSize: 20
          }
        },
        data: funnelSteps.value.map(step => ({
          name: step.stepName,
          value: step.stepCount
        }))
      }
    ]
  }
  chart.setOption(option)
}

onMounted(() => {
  initFunnelChart()
})
</script>

<style scoped lang="scss">
.funnel-analysis {
  .search-card,
  .compare-card,
  .chart-card,
  .table-card,
  .custom-funnel-card {
    margin-bottom: 16px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .steps-list {
    .step-item {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      padding: 8px;
      background: #f5f7fa;
      border-radius: 4px;

      .drag-handle {
        cursor: move;
        margin-right: 8px;
        color: #909399;
      }

      .step-index {
        font-weight: bold;
        margin-right: 8px;
      }

      .delete-icon {
        margin-left: 8px;
        cursor: pointer;
        color: #f56c6c;
        
        &:hover {
          color: #f78989;
        }
      }
    }
  }
}
</style>
