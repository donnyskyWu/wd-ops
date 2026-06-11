<template>
  <div class="data-report">
    <!-- 入口: 8 张报表卡片（同时保留原 8 Tab 兜底，老 URL 也能用） -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <h2 style="margin: 0 0 4px 0">数据报表中心</h2>
      <p style="color: #909399; margin: 0 0 16px 0">点击任一卡片查看对应报表详细数据</p>
      <el-row :gutter="16">
        <el-col :span="6" v-for="r in reports" :key="r.path" style="margin-bottom: 12px">
          <el-card shadow="hover" class="report-card" @click="goReport(r.path)" body-style="padding: 12px; cursor: pointer">
            <div style="display: flex; align-items: center; gap: 12px">
              <div :style="{ background: r.color, width: '40px', height: '40px', borderRadius: '6px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }">
                <el-icon :size="20"><component :is="r.icon" /></el-icon>
              </div>
              <div style="flex: 1; min-width: 0">
                <div style="font-weight: 600">{{ r.title }}</div>
                <div style="color: #909399; font-size: 12px">{{ r.desc }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
    <!-- Tab导航 (老 URL 兜底) -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="统一视图" name="unified-account" />
        <el-tab-pane label="状态监控" name="account-status" />
        <el-tab-pane label="短视频产出" name="video-output" />
        <el-tab-pane label="直播时长" name="live-duration" />
        <el-tab-pane label="成本分摊" name="cost-allocation" />
        <el-tab-pane label="ROI分析" name="roi-analysis" />
        <el-tab-pane label="团队配置" name="team-config" />
        <el-tab-pane label="异常预警" name="account-alert" />
      </el-tabs>
    </el-card>

    <!-- 查询条件 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryForm" inline>
        <el-form-item label="平台类型">
          <el-select v-model="queryForm.platformType" placeholder="全部" clearable style="width: 150px">
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="快手" value="KUAISHOU" />
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="视频号" value="VIDEO_ACCOUNT" />
            <el-option label="公众号" value="WECHAT_MP" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP组">
          <el-select v-model="queryForm.ipGroupId" placeholder="全部" clearable style="width: 150px">
            <el-option label="IP大组A" :value="1" />
            <el-option label="IP大组B" :value="2" />
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
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6" v-for="(card, index) in statCards" :key="index">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :class="card.colorClass">{{ card.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-card class="chart-card" shadow="never" v-show="showChart">
      <template #header>
        <div class="card-header">
          <span>数据趋势</span>
        </div>
      </template>
      <div ref="chartRef" style="height: 400px; background: #fafafa;"></div>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column 
          v-for="col in tableColumns" 
          :key="col.prop"
          :prop="col.prop" 
          :label="col.label" 
          :width="col.width"
          :align="col.align || 'left'"
        >
          <template #default="{ row }" v-if="col.render">
            <component :is="col.render(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        :current-page="pagination.current"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @update:current-page="(val) => pagination.current = val"
        @update:page-size="(val) => { pagination.pageSize = val; handleQuery() }"
        @current-change="handleQuery"
        @size-change="handleQuery"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { DataLine, Warning, VideoPlay, Headset, Money, TrendCharts, User, Bell } from '@element-plus/icons-vue'
import { exportToExcel } from '@/utils'

const router = useRouter()
const reports = [
  { path: '/analysis/report/unified-account', title: '统一视图', desc: '账号/粉丝/GMV 全局', tag: '概览', tagType: 'primary', color: '#409eff', icon: DataLine },
  { path: '/analysis/report/account-status', title: '状态监控', desc: '账号健康实时监控', tag: '运营', tagType: 'success', color: '#67c23a', icon: Warning },
  { path: '/analysis/report/video-output', title: '短视频产出', desc: '各平台产出趋势', tag: '内容', tagType: 'warning', color: '#e6a23c', icon: VideoPlay },
  { path: '/analysis/report/live-duration', title: '直播时长', desc: '直播场次/时长/在线', tag: '直播', tagType: 'danger', color: '#f56c6c', icon: Headset },
  { path: '/analysis/report/cost-allocation', title: '成本分摊', desc: '投流/人力/设备占比', tag: '财务', tagType: 'info', color: '#909399', icon: Money },
  { path: '/analysis/report/roi', title: 'ROI 分析', desc: 'ROI 拆解与趋势', tag: '财务', tagType: 'success', color: '#67c23a', icon: TrendCharts },
  { path: '/analysis/report/team-config', title: '团队配置', desc: '人员/账号人均/人效', tag: '人力', tagType: 'primary', color: '#409eff', icon: User },
  { path: '/analysis/report/account-alert', title: '异常预警', desc: '三级预警', tag: '风控', tagType: 'danger', color: '#f56c6c', icon: Bell },
] as any[]
const goReport = (path: string) => router.push(path)

const activeTab = ref('unified-account')

const queryForm = reactive({
  platformType: '',
  ipGroupId: null,
  dateRange: []
})

const loading = ref(false)

// 统计卡片配置
const statCards = computed(() => {
  const cardsMap: Record<string, any[]> = {
    'unified-account': [
      { label: '账号总数', value: '1,234', colorClass: '' },
      { label: '正常账号', value: '1,150', colorClass: 'success' },
      { label: '异常账号', value: '64', colorClass: 'warning' },
      { label: '封禁账号', value: '20', colorClass: 'danger' }
    ],
    'account-status': [
      { label: '正常账号', value: '1,150', colorClass: 'success' },
      { label: '减少推荐', value: '35', colorClass: 'warning' },
      { label: '禁止私信', value: '18', colorClass: 'warning' },
      { label: '封禁率', value: '1.6%', colorClass: 'success' }
    ],
    'video-output': [
      { label: '短视频总数', value: '8,567', colorClass: '' },
      { label: '日均产出', value: '285.6', colorClass: '' },
      { label: '达标率', value: '92.3%', colorClass: 'success' }
    ],
    'live-duration': [
      { label: '直播总时长', value: '1,234.5h', colorClass: '' },
      { label: '场均时长', value: '2.8h', colorClass: '' },
      { label: '开播场次', value: '438', colorClass: '' },
      { label: '场均观看', value: '3.2万', colorClass: '' }
    ],
    'cost-allocation': [
      { label: '本月成本', value: '¥45.6万', colorClass: '' },
      { label: '人均成本', value: '¥1.2万', colorClass: '' },
      { label: '内容成本', value: '¥28.3万', colorClass: '' },
      { label: '运营成本', value: '¥17.3万', colorClass: '' }
    ],
    'roi-analysis': [
      { label: '整体ROI', value: '3.8', colorClass: 'success' },
      { label: '抖音ROI', value: '4.2', colorClass: 'success' },
      { label: '快手ROI', value: '3.5', colorClass: '' },
      { label: '小红书ROI', value: '3.1', colorClass: '' }
    ],
    'team-config': [
      { label: '团队总人数', value: '38', colorClass: '' },
      { label: '运营人数', value: '15', colorClass: '' },
      { label: '内容人数', value: '12', colorClass: '' },
      { label: '策划人数', value: '11', colorClass: '' }
    ],
    'account-alert': [
      { label: '预警总数', value: '23', colorClass: 'warning' },
      { label: '严重预警', value: '5', colorClass: 'danger' },
      { label: '一般预警', value: '18', colorClass: '' },
      { label: '已处理', value: '19', colorClass: 'success' }
    ]
  }
  return cardsMap[activeTab.value] || []
})

const showChart = computed(() => {
  return ['account-status', 'video-output', 'live-duration', 'cost-allocation', 'roi-analysis'].includes(activeTab.value)
})

// 表格列配置
const tableColumns = computed(() => {
  const columnsMap: Record<string, any[]> = {
    'unified-account': [
      { prop: 'platformType', label: '平台类型', width: 100 },
      { prop: 'accountName', label: '账号名称', width: 180 },
      { prop: 'platformAccountId', label: '平台账号ID', width: 140 },
      { prop: 'ipGroupName', label: '所属IP组', width: 120 },
      { prop: 'followerCount', label: '粉丝数', width: 100, align: 'right' },
      { prop: 'accountStatus', label: '账号状态', width: 100 }
    ],
    'account-status': [
      { prop: 'timePeriod', label: '时间周期', width: 120 },
      { prop: 'platformType', label: '平台类型', width: 100 },
      { prop: 'normalCount', label: '正常数', width: 80, align: 'right' },
      { prop: 'reduceRecommendCount', label: '减少推荐', width: 100, align: 'right' },
      { prop: 'banPrivateMsgCount', label: '禁止私信', width: 100, align: 'right' },
      { prop: 'bannedCount', label: '封禁数', width: 80, align: 'right' },
      { prop: 'banRate', label: '封禁率', width: 80, align: 'right' }
    ],
    'video-output': [
      { prop: 'timePeriod', label: '时间周期', width: 120 },
      { prop: 'userName', label: '主播姓名', width: 120 },
      { prop: 'ipGroupName', label: 'IP组', width: 120 },
      { prop: 'platformType', label: '平台', width: 100 },
      { prop: 'videoCount', label: '产出数量', width: 100, align: 'right' },
      { prop: 'dailyAvg', label: '日均产出', width: 100, align: 'right' },
      { prop: 'achieveRate', label: '达标率', width: 80, align: 'right' }
    ],
    'live-duration': [
      { prop: 'timePeriod', label: '时间周期', width: 120 },
      { prop: 'userName', label: '主播姓名', width: 120 },
      { prop: 'platformType', label: '平台', width: 100 },
      { prop: 'totalDuration', label: '总时长(h)', width: 100, align: 'right' },
      { prop: 'avgDuration', label: '场均时长(h)', width: 120, align: 'right' },
      { prop: 'sessionCount', label: '开播场次', width: 100, align: 'right' },
      { prop: 'avgViewers', label: '场均观看', width: 100, align: 'right' }
    ],
    'cost-allocation': [
      { prop: 'timePeriod', label: '时间周期', width: 120 },
      { prop: 'costType', label: '成本类型', width: 120 },
      { prop: 'ipGroupName', label: 'IP组', width: 120 },
      { prop: 'platformType', label: '平台', width: 100 },
      { prop: 'contentCost', label: '内容成本', width: 100, align: 'right' },
      { prop: 'operationCost', label: '运营成本', width: 100, align: 'right' },
      { prop: 'totalCost', label: '总成本', width: 100, align: 'right' },
      { prop: 'costPerUnit', label: '单位成本', width: 100, align: 'right' }
    ],
    'roi-analysis': [
      { prop: 'timePeriod', label: '时间周期', width: 120 },
      { prop: 'platformType', label: '平台', width: 100 },
      { prop: 'ipGroupName', label: 'IP组', width: 120 },
      { prop: 'revenue', label: '收益', width: 100, align: 'right' },
      { prop: 'cost', label: '成本', width: 100, align: 'right' },
      { prop: 'roi', label: 'ROI', width: 80, align: 'right' },
      { prop: 'roiChange', label: '环比变化', width: 100, align: 'right' }
    ],
    'team-config': [
      { prop: 'teamName', label: '团队名称', width: 150 },
      { prop: 'roleType', label: '角色类型', width: 120 },
      { prop: 'memberName', label: '成员姓名', width: 120 },
      { prop: 'platformType', label: '负责平台', width: 100 },
      { prop: 'accountCount', label: '账号数量', width: 100, align: 'right' },
      { prop: 'workload', label: '工作量', width: 100, align: 'right' }
    ],
    'account-alert': [
      { prop: 'alertTime', label: '预警时间', width: 160 },
      { prop: 'platformType', label: '平台', width: 100 },
      { prop: 'accountName', label: '账号名称', width: 180 },
      { prop: 'alertType', label: '预警类型', width: 120 },
      { prop: 'alertLevel', label: '预警等级', width: 100 },
      { prop: 'description', label: '预警描述', width: 200 },
      { prop: 'status', label: '处理状态', width: 100 }
    ]
  }
  return columnsMap[activeTab.value] || []
})

// 表格数据
const tableData = ref<any[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 图表配置映射
const getChartOption = (tabName: string) => {
  const chartOptionsMap: Record<string, any> = {
    'account-status': {
      title: { text: '账号状态趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: { data: ['正常', '减少推荐', '禁止私信', '封禁'], top: 30 },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '20%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: ['5.22', '5.23', '5.24', '5.25', '5.26', '5.27', '5.28'] },
      yAxis: { type: 'value', name: '账号数' },
      series: [
        { name: '正常', type: 'line', data: [1100, 1120, 1110, 1130, 1150, 1140, 1150] },
        { name: '减少推荐', type: 'line', data: [40, 38, 42, 35, 36, 37, 35] },
        { name: '禁止私信', type: 'line', data: [20, 22, 18, 19, 20, 18, 18] },
        { name: '封禁', type: 'line', data: [15, 18, 16, 17, 20, 18, 20] }
      ]
    },
    'video-output': {
      title: { text: '短视频产出趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: { data: ['产出数量', '达标率'], top: 30 },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '20%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: ['5.22', '5.23', '5.24', '5.25', '5.26', '5.27', '5.28'] },
      yAxis: [
        { type: 'value', name: '产出数' },
        { type: 'value', name: '达标率%', max: 100 }
      ],
      series: [
        { name: '产出数量', type: 'bar', data: [280, 295, 270, 310, 285, 275, 290] },
        { name: '达标率', type: 'line', yAxisIndex: 1, data: [92, 95, 88, 96, 93, 91, 94] }
      ]
    },
    'live-duration': {
      title: { text: '直播时长趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: { data: ['直播时长(h)', '场均观看(万)'], top: 30 },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '20%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: ['5.22', '5.23', '5.24', '5.25', '5.26', '5.27', '5.28'] },
      yAxis: [
        { type: 'value', name: '时长(h)' },
        { type: 'value', name: '观看(万)' }
      ],
      series: [
        { name: '直播时长(h)', type: 'line', areaStyle: {}, data: [42.5, 38.2, 45.8, 40.3, 48.6, 52.1, 45.5] },
        { name: '场均观看(万)', type: 'line', yAxisIndex: 1, data: [3.2, 2.8, 3.5, 3.1, 3.8, 4.2, 3.9] }
      ]
    },
    'cost-allocation': {
      title: { text: '成本分摊趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: { data: ['内容成本', '运营成本', '总成本'], top: 30 },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '20%', containLabel: true },
      xAxis: { type: 'category', data: ['5.22', '5.23', '5.24', '5.25', '5.26', '5.27', '5.28'] },
      yAxis: { type: 'value', name: '成本(万)' },
      series: [
        { name: '内容成本', type: 'bar', stack: '总成本', data: [1.2, 1.3, 1.1, 1.4, 1.2, 1.3, 1.2] },
        { name: '运营成本', type: 'bar', stack: '总成本', data: [0.8, 0.9, 0.7, 0.8, 0.9, 0.8, 0.7] },
        { name: '总成本', type: 'line', data: [2.0, 2.2, 1.8, 2.2, 2.1, 2.1, 1.9] }
      ]
    },
    'roi-analysis': {
      title: { text: 'ROI趋势分析', left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: { data: ['抖音ROI', '快手ROI', '小红书ROI', '整体ROI'], top: 30 },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '20%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: ['5.22', '5.23', '5.24', '5.25', '5.26', '5.27', '5.28'] },
      yAxis: { type: 'value', name: 'ROI值', min: 0 },
      series: [
        { name: '抖音ROI', type: 'line', data: [4.0, 4.1, 3.9, 4.2, 4.1, 4.3, 4.2] },
        { name: '快手ROI', type: 'line', data: [3.3, 3.4, 3.2, 3.5, 3.4, 3.6, 3.5] },
        { name: '小红书ROI', type: 'line', data: [2.9, 3.0, 2.8, 3.1, 3.0, 3.2, 3.1] },
        { name: '整体ROI', type: 'line', data: [3.6, 3.7, 3.5, 3.8, 3.7, 3.9, 3.8] }
      ]
    }
  }
  return chartOptionsMap[tabName] || null
}

const tabRouteMap: Record<string, string> = {
  'unified-account': '/analysis/report/unified-account',
  'account-status': '/analysis/report/account-status',
  'video-output': '/analysis/report/video-output',
  'live-duration': '/analysis/report/live-duration',
  'cost-allocation': '/analysis/report/cost-allocation',
  'roi-analysis': '/analysis/report/roi',
  'team-config': '/analysis/report/team-config',
  'account-alert': '/analysis/report/account-alert',
}

const handleTabChange = (tabName: string | number) => {
  const tab = String(tabName)
  activeTab.value = tab
  const path = tabRouteMap[tab]
  if (path) router.push(path)
}

const handleQuery = () => {
  const path = tabRouteMap[activeTab.value]
  if (path) router.push(path)
}

const handleReset = () => {
  queryForm.platformType = ''
  queryForm.ipGroupId = null
  queryForm.dateRange = []
  handleQuery()
}

const handleExport = () => {
  const columns = tableColumns.value.map(col => ({ key: col.prop, label: col.label }))
  exportToExcel(tableData.value, columns, '数据报表')
}

const handleDetail = (row: any) => {
  console.log('查看详情:', row)
  ElMessage.info('详情功能开发中')
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const option = getChartOption(activeTab.value)
  if (option) {
    chartInstance.setOption(option, true)
  }
}

onMounted(() => {
  handleQuery()
  // 延迟初始化图表，确保 DOM 已渲染
  nextTick(() => {
    if (chartRef.value) {
      chartInstance = echarts.init(chartRef.value)
      updateChart()
      console.log('图表初始化完成')
    }
  })
})
</script>

<style scoped lang="scss">
.data-report {
  .search-card {
    margin: 16px 0;
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

      &.warning {
        color: #e6a23c;
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

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
