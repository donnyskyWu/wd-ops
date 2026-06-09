<template>
  <div class="metric-analysis">
    <!-- 查询条件 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryForm" :rules="queryRules" ref="queryFormRef" inline>
        <el-form-item label="指标类型" prop="metricType">
          <el-select v-model="queryForm.metricType" placeholder="全部" clearable style="width: 150px">
            <el-option label="流量指标" value="traffic" />
            <el-option label="互动指标" value="interaction" />
            <el-option label="转化指标" value="conversion" />
            <el-option label="营收指标" value="revenue" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围" prop="dateRange">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="IP组" prop="ipGroupId">
          <el-select v-model="queryForm.ipGroupId" placeholder="全部" clearable style="width: 150px">
            <el-option label="IP大组A" :value="1" />
            <el-option label="IP大组B" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">监控指标数</div>
            <div class="stat-value">{{ stats.totalMetrics }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">达标指标</div>
            <div class="stat-value success">{{ stats.qualifiedMetrics }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">未达标指标</div>
            <div class="stat-value danger">{{ stats.unqualifiedMetrics }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">达标率</div>
            <div class="stat-value">{{ stats.qualificationRate }}%</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 指标趋势图 -->
    <el-card class="chart-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>指标趋势分析</span>
        </div>
      </template>
      <div ref="trendChartRef" style="height: 400px"></div>
    </el-card>

    <!-- 指标列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>指标明细</span>
        </div>
      </template>
      <el-table :data="metricList" border stripe v-loading="loading">
        <template #empty>
          <el-empty description="暂无指标数据" />
        </template>
        <el-table-column prop="metricName" label="指标名称" width="180" />
        <el-table-column prop="metricType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getMetricTypeTag(row.metricType)">{{ row.metricType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentValue" label="当前值" width="120" align="right" />
        <el-table-column prop="targetValue" label="目标值" width="120" align="right" />
        <el-table-column prop="achievementRate" label="达成率" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.achievementRate >= 100 ? 'success' : 'danger'">
              {{ row.achievementRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="trend" label="趋势" width="100" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.trend === 'up'" color="#67c23a"><Top /></el-icon>
            <el-icon v-else-if="row.trend === 'down'" color="#f56c6c"><Bottom /></el-icon>
            <el-icon v-else color="#909399"><Minus /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'qualified' ? 'success' : 'danger'">
              {{ row.status === 'qualified' ? '达标' : '未达标' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Top, Bottom, Minus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const queryFormRef = ref<FormInstance>()
const loading = ref(false)

const queryForm = reactive({
  metricType: '',
  dateRange: [] as string[],
  ipGroupId: null as number | null
})

const queryRules = reactive<FormRules>({
  metricType: [
    { required: true, message: '请选择指标类型', trigger: 'change' }
  ],
  dateRange: [
    { required: true, message: '请选择时间范围', trigger: 'change' }
  ]
})

const stats = reactive({
  totalMetrics: 48,
  qualifiedMetrics: 36,
  unqualifiedMetrics: 12,
  qualificationRate: 75
})

const metricList = ref([
  { metricName: '粉丝增长量', metricType: '流量', currentValue: 12500, targetValue: 10000, achievementRate: 125, trend: 'up', status: 'qualified' },
  { metricName: '作品播放量', metricType: '流量', currentValue: 850000, targetValue: 1000000, achievementRate: 85, trend: 'down', status: 'unqualified' },
  { metricName: '点赞数', metricType: '互动', currentValue: 45000, targetValue: 40000, achievementRate: 112.5, trend: 'up', status: 'qualified' },
  { metricName: '评论数', metricType: '互动', currentValue: 8200, targetValue: 10000, achievementRate: 82, trend: 'stable', status: 'unqualified' },
  { metricName: '转化率', metricType: '转化', currentValue: 3.5, targetValue: 3.0, achievementRate: 116.7, trend: 'up', status: 'qualified' },
  { metricName: '订单数', metricType: '营收', currentValue: 2350, targetValue: 2500, achievementRate: 94, trend: 'down', status: 'unqualified' }
])

const trendChartRef = ref<HTMLElement>()

const getMetricTypeTag = (type: string) => {
  const map: Record<string, any> = {
    '流量': '',
    '互动': 'success',
    '转化': 'warning',
    '营收': 'danger'
  }
  return map[type] || ''
}

const handleQuery = async () => {
  if (!queryFormRef.value) return
  
  try {
    await queryFormRef.value.validate()
    loading.value = true
    // TODO: 调用API查询
    console.log('查询指标分析', queryForm)
    ElMessage.success('查询成功')
  } catch (error) {
    console.error('表单校验失败', error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryForm.metricType = ''
  queryForm.dateRange = []
  queryForm.ipGroupId = null
  handleQuery()
}

let metricTrendChart: echarts.ECharts | null = null
const initTrendChart = () => {
  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(initTrendChart, 100)
    return
  }
  if (metricTrendChart) {
    metricTrendChart.dispose()
    metricTrendChart = null
  }
  const chart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['粉丝增长', '播放量', '点赞数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value' },
    series: [
      { name: '粉丝增长', type: 'line', data: [1200, 1320, 1010, 1340, 900, 2300, 2100] },
      { name: '播放量', type: 'line', data: [22000, 18200, 19100, 23400, 29000, 33000, 31000] },
      { name: '点赞数', type: 'line', data: [1500, 2320, 2010, 1940, 2900, 3300, 3100] }
    ]
  }
  chart.setOption(option)
}

onMounted(() => {
  initTrendChart()
})
</script>

<style scoped lang="scss">
.metric-analysis {
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
      font-size: 28px;
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

  .success {
    color: #67c23a;
  }

  .danger {
    color: #f56c6c;
  }
}
</style>
