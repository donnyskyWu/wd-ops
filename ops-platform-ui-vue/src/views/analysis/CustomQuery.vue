<template>
  <div class="custom-query-page">
    
    <el-card class="config-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>查询配置</span>
          <div>
            <el-button type="primary" @click="executeQuery">执行查询</el-button>
            <el-button @click="handleSave">保存查询</el-button>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>

      <el-form :model="queryForm" label-width="100px">
        <el-form-item label="查询名称"><el-input v-model="queryForm.queryName" placeholder="1-50字符" maxlength="50" style="width: 300px" /></el-form-item>
        <el-form-item label="数据源"><el-select v-model="queryForm.dataSource" placeholder="请选择" style="width: 300px"><el-option label="oa_content_daily" value="oa_content_daily" /><el-option label="oa_follower" value="oa_follower" /><el-option label="oa_order" value="oa_order" /></el-select></el-form-item>
        
        <el-divider content-position="left">字段选择</el-divider>
        <el-checkbox-group v-model="queryForm.selectedFields">
          <el-checkbox label="stat_date">统计日期</el-checkbox>
          <el-checkbox label="account_name">账号名称</el-checkbox>
          <el-checkbox label="follower_count">粉丝数</el-checkbox>
          <el-checkbox label="content_count">内容数</el-checkbox>
          <el-checkbox label="view_count">播放量</el-checkbox>
          <el-checkbox label="like_count">点赞数</el-checkbox>
        </el-checkbox-group>

        <el-divider content-position="left">过滤条件</el-divider>
        <div v-for="(condition, index) in queryForm.conditions" :key="index" class="condition-row">
          <el-select v-model="condition.field" placeholder="字段" style="width: 150px"><el-option label="统计日期" value="stat_date" /><el-option label="账号名称" value="account_name" /></el-select>
          <el-select v-model="condition.operator" placeholder="操作符" style="width: 120px"><el-option label="等于" value="EQ" /><el-option label="介于" value="BETWEEN" /><el-option label="包含" value="LIKE" /></el-select>
          <el-input v-model="condition.value" placeholder="值" style="width: 200px" />
          <el-button type="danger" :icon="Delete" @click="removeCondition(index)" circle />
        </div>
        <el-button type="primary" @click="addCondition" :icon="Plus">添加条件</el-button>

        <el-divider content-position="left">分组/排序</el-divider>
        <el-form-item label="分组字段"><el-input v-model="queryForm.groupBy" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="排序规则"><el-input v-model="queryForm.orderBy" placeholder="如: stat_date DESC" /></el-form-item>
      </el-form>
    </el-card>

    <el-card class="result-card" shadow="never">
      <el-tabs v-model="resultTab">
        <el-tab-pane label="列表视图" name="list">
          <el-table :data="queryResult" v-loading="loading" stripe>
            <el-table-column v-for="field in queryForm.selectedFields" :key="field" :prop="field" :label="getFieldLabel(field)" />
          </el-table>
          <el-pagination :current-page="pageNo" :page-size="pageSize" :total="total" layout="total, prev, pager, next" class="pagination" @current-change="handlePageChange" />
        </el-tab-pane>
        <el-tab-pane label="图表视图" name="chart">
          <div ref="chartRef" style="height: 400px"></div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { exportToExcel } from '@/utils'

// ==================== 类型定义 ====================
interface QueryResultItem {
  stat_date: string
  account_name: string
  follower_count: number
  content_count: number
  view_count: number
  like_count: number
}

// ==================== 响应式数据 ====================
const queryForm = reactive({
  queryName: '',
  dataSource: 'oa_content_daily',
  selectedFields: ['stat_date', 'account_name', 'follower_count'],
  conditions: [{ field: 'stat_date', operator: 'BETWEEN', value: '2026-05-01~2026-05-31' }],
  groupBy: '',
  orderBy: 'stat_date DESC',
})

const loading = ref(false)
const resultTab = ref('list')
const queryResult = ref<QueryResultItem[]>([
  { stat_date: '2026-05-27', account_name: '测试账号1', follower_count: 12345, content_count: 150, view_count: 50000, like_count: 3200 },
  { stat_date: '2026-05-26', account_name: '测试账号2', follower_count: 23456, content_count: 200, view_count: 80000, like_count: 5100 },
])
const total = ref(2)
const pageNo = ref(1)
const pageSize = ref(20)
const chartRef = ref<HTMLElement>()

const getFieldLabel = (field: string) => {
  const map: Record<string, string> = { stat_date: '统计日期', account_name: '账号名称', follower_count: '粉丝数', content_count: '内容数', view_count: '播放量', like_count: '点赞数' }
  return map[field] || field
}

const addCondition = () => { queryForm.conditions.push({ field: '', operator: '', value: '' }) }
const removeCondition = (index: number) => { queryForm.conditions.splice(index, 1) }

const executeQuery = async () => {
  loading.value = true
  await new Promise(resolve => setTimeout(resolve, 500))
  loading.value = false
  ElMessage.success('查询成功')
  loadChart()
}

const handleSave = () => ElMessage.success('查询已保存')
const handleExport = () => {
  if (!queryResult.value || queryResult.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  
  const columns = Object.keys(queryResult.value[0] || {}).map(key => ({
    key,
    label: key
  }))
  
  exportToExcel(queryResult.value, columns, '自定义查询结果')
}
const handlePageChange = (page: number) => {
  pageNo.value = page
  executeQuery()
}

let customChart: echarts.ECharts | null = null
const loadChart = async () => {
  if (resultTab.value !== 'chart' || !chartRef.value) return
  await nextTick()
  if (chartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(loadChart, 100)
    return
  }
  if (customChart) {
    customChart.dispose()
    customChart = null
  }
  const chart = echarts.init(chartRef.value)
  customChart = chart
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: queryResult.value.map((r: QueryResultItem) => r.stat_date) },
    yAxis: { type: 'value' },
    series: [{ name: '粉丝数', type: 'line', data: queryResult.value.map((r: QueryResultItem) => r.follower_count) }]
  })
}
</script>

<style scoped>
.custom-query-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.config-card { margin-bottom: 16px; }
.condition-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
