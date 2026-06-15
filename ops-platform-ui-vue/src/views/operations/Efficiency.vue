<template>
  <div class="efficiency-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="统计周期">
        <DictSelect v-model="searchForm.timeDimension" dict-type="dict_time_dimension" style="width: 120px" />
      </el-form-item>
      <el-form-item label="统计日期">
        <el-date-picker v-model="searchForm.statDate" type="month" placeholder="选择月份" style="width: 160px" />
      </el-form-item>
      <el-form-item label="IP组">
        <IpGroupTreeSelect v-model="searchForm.ipGroupId" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="经办人姓名" clearable maxlength="50" />
      </el-form-item>
    </TableSearch>

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <span>共 <strong>{{ totalCount }}</strong> 条记录</span>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon> 导出
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="main-tabs">
        <!-- 公众号运营数据 -->
        <el-tab-pane label="公众号运营数据统计" name="wechat">
          <el-table :data="wechatList" v-loading="loading" border stripe row-key="userId" @expand-change="handleExpand">
            <el-table-column type="expand" width="50">
              <template #default="{ row }">
                <div class="expand-content">
                  <el-row :gutter="16">
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#409eff"><Document /></el-icon> 任务完成情况</div>
                          <div class="expand-row"><span>已完成任务: <b>{{ row.taskCompleted }}</b></span><span>完成率: <b>{{ formatPct(row.completionRate) }}</b></span></div>
                          <el-progress :percentage="row.completionRate || 0" :stroke-width="6" />
                          <div class="expand-desc">总任务: {{ row.taskTotal }} | 进行中: {{ row.taskInProgress }} | 超时: {{ row.taskOverdue }}</div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#e6a23c"><Coin /></el-icon> 财务指标</div>
                          <div class="expand-row"><span>投入成本: <b>¥{{ formatNumber(row.costAmount) }}</b></span><span>产出价值: <b style="color: #67c23a">¥{{ formatNumber(row.revenue) }}</b></span></div>
                          <div class="expand-row"><span>ROI: <b style="color: #67c23a">{{ formatPct(row.roi) }}</b></span><span>人效: <b :class="getEfficiencyClass(row.completionRate)">{{ getEfficiency(row.completionRate) }}</b></span></div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#409eff"><Document /></el-icon> 内容产出</div>
                          <div class="expand-row"><span>发布作品: <b>{{ row.contentOutput }}</b></span><span>平均阅读: <b style="color: #409eff">{{ formatNumber(row.avgRead) }}</b></span></div>
                          <div class="expand-row"><span>爆款数: <b>{{ row.hitCount }}</b></span></div>
                          <div v-if="!row.contentOutput && !row.hitCount" class="expand-desc">该周期暂无内容产出</div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#f56c6c"><TrendCharts /></el-icon> 时间趋势</div>
                          <div class="expand-desc">近 30 天营收 / 任务完成</div>
                          <div class="mini-chart" :ref="el => setWechatChartRef(el, row.userId)"></div>
                        </div>
                      </el-card>
                    </el-col>
                  </el-row>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="userName" label="经办人" min-width="100" />
            <el-table-column prop="position" label="岗位" width="100">
              <template #default="{ row }">
                <DictLabel dict-type="dict_position" :value="row.position" :fallback="row.position || '--'" />
              </template>
            </el-table-column>
            <el-table-column prop="ipGroupName" label="IP组" min-width="120" />
            <el-table-column prop="taskTotal" label="任务总数" width="100" align="right" />
            <el-table-column prop="taskCompleted" label="已完成" width="90" align="right" />
            <el-table-column prop="completionRate" label="完成率" width="100" align="right">
              <template #default="{ row }">{{ formatPct(row.completionRate) }}</template>
            </el-table-column>
            <el-table-column prop="costAmount" label="账号成本" width="120" align="right">
              <template #default="{ row }">¥{{ formatNumber(row.costAmount) }}</template>
            </el-table-column>
            <el-table-column prop="revenue" label="营收" width="120" align="right">
              <template #default="{ row }">¥{{ formatNumber(row.revenue) }}</template>
            </el-table-column>
            <el-table-column prop="roi" label="ROI" width="100" align="right">
              <template #default="{ row }">{{ formatPct(row.roi) }}</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="90" align="right" />
          </el-table>
        </el-tab-pane>

        <!-- 短视频运营数据（同 VO 字段，仅展示侧不同） -->
        <el-tab-pane label="短视频运营数据统计" name="video">
          <el-table :data="productivityList" v-loading="loading" border stripe row-key="userId" @expand-change="handleExpand">
            <el-table-column type="expand" width="50">
              <template #default="{ row }">
                <div class="expand-content">
                  <el-row :gutter="16">
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#409eff"><VideoCamera /></el-icon> 视频产出</div>
                          <div class="expand-row"><span>发布视频: <b>{{ row.contentOutput }}</b></span><span>平均播放: <b style="color: #409eff">{{ formatNumber(row.avgPlay) }}</b></span></div>
                          <div v-if="!row.contentOutput" class="expand-desc">该周期暂无视频产出</div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#e6a23c"><Star /></el-icon> 互动指标</div>
                          <div class="expand-row"><span>平均阅读: <b>{{ formatNumber(row.avgRead) }}</b></span><span>爆款数: <b>{{ row.hitCount }}</b></span></div>
                          <div v-if="!row.hitCount && !row.avgRead" class="expand-desc">该周期暂无互动数据</div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#67c23a"><User /></el-icon> 粉丝增长</div>
                          <div class="expand-row"><span>营收: <b style="color: #67c23a">¥{{ formatNumber(row.revenue) }}</b></span></div>
                          <div class="expand-row"><span>粉丝 ROI: <b>{{ formatPct(row.roi) }}</b></span></div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#f56c6c"><TrendCharts /></el-icon> 时间趋势</div>
                          <div class="expand-desc">近 30 天营收</div>
                          <div class="mini-chart" :ref="el => setVideoChartRef(el, row.userId)"></div>
                        </div>
                      </el-card>
                    </el-col>
                  </el-row>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="userName" label="经办人" min-width="100" />
            <el-table-column prop="position" label="岗位" width="100">
              <template #default="{ row }">
                <DictLabel dict-type="dict_position" :value="row.position" :fallback="row.position || '--'" />
              </template>
            </el-table-column>
            <el-table-column prop="ipGroupName" label="IP组" min-width="120" />
            <el-table-column prop="contentOutput" label="发布数" width="100" align="right" />
            <el-table-column prop="avgPlay" label="平均播放" width="120" align="right">
              <template #default="{ row }">{{ formatNumber(row.avgPlay) }}</template>
            </el-table-column>
            <el-table-column prop="revenue" label="营收" width="120" align="right">
              <template #default="{ row }">¥{{ formatNumber(row.revenue) }}</template>
            </el-table-column>
            <el-table-column prop="roi" label="ROI" width="100" align="right">
              <template #default="{ row }">{{ formatPct(row.roi) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 个人管理账号的粉丝情况（暂无 oa_user_account 关联表 → 用 task 替代） -->
        <el-tab-pane label="个人管理账号的粉丝情况" name="fans">
          <el-table :data="fansList" v-loading="loading" border stripe>
            <el-table-column prop="userName" label="经办人" min-width="100" />
            <el-table-column prop="ipGroupName" label="IP组" min-width="120" />
            <el-table-column prop="orderCount" label="订单数" width="120" align="right" />
            <el-table-column prop="revenue" label="营收" width="120" align="right">
              <template #default="{ row }">¥{{ formatNumber(row.revenue) }}</template>
            </el-table-column>
            <el-table-column prop="taskCompleted" label="完成任务" width="120" align="right" />
            <el-table-column prop="completionRate" label="完成率" width="120" align="right">
              <template #default="{ row }">{{ formatPct(row.completionRate) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 个人管理的会员情况（用 oa_order + oa_task 替代） -->
        <el-tab-pane label="个人管理的会员情况" name="vip">
          <el-table :data="vipList" v-loading="loading" border stripe>
            <el-table-column prop="userName" label="经办人" min-width="100" />
            <el-table-column prop="ipGroupName" label="IP组" min-width="120" />
            <el-table-column prop="orderCount" label="订单数" width="100" align="right" />
            <el-table-column prop="revenue" label="营收" width="120" align="right">
              <template #default="{ row }">¥{{ formatNumber(row.revenue) }}</template>
            </el-table-column>
            <el-table-column prop="costAmount" label="成本" width="120" align="right">
              <template #default="{ row }">¥{{ formatNumber(row.costAmount) }}</template>
            </el-table-column>
            <el-table-column prop="roi" label="ROI" width="100" align="right">
              <template #default="{ row }">{{ formatPct(row.roi) }}</template>
            </el-table-column>
            <el-table-column prop="taskTotal" label="任务总数" width="100" align="right" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// S-R9: 4 tab 全部接真 VO 字段，移除 × 系数假数据 + Math.random mini-chart
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getProductivityList, exportProductivityCsv, getProductivityDetail } from '@/api/productivity'
import { exportToExcel } from '@/utils'
import type { ProductivityReviewVO, ProductivityReviewQuery } from '@/types/productivity'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { Download, Document, Coin, TrendCharts, VideoCamera, Star, User } from '@element-plus/icons-vue'

const activeTab = ref('wechat')
const loading = ref(false)
const totalCount = ref(0)
const searchForm = reactive({
  timeDimension: 'WEEK',
  statDate: undefined as string | undefined,
  ipGroupId: undefined as number | undefined,
  keyword: undefined as string | undefined,
})
const productivityList = ref<ProductivityReviewVO[]>([])

// 4 tab 共享同一份 VO 数据，展示侧不同
const wechatList = computed(() => productivityList.value)
const videoList = computed(() => productivityList.value)
const fansList = computed(() => productivityList.value)
const vipList = computed(() => productivityList.value)

// mini-chart 容器引用
const wechatChartRefs = reactive<Record<number, HTMLElement | null>>({})
const videoChartRefs = reactive<Record<number, HTMLElement | null>>({})
const wechatCharts: Record<number, echarts.ECharts> = {}
const videoCharts: Record<number, echarts.ECharts> = {}
const setWechatChartRef = (el: any, userId: number) => { if (el) wechatChartRefs[userId] = el as HTMLElement }
const setVideoChartRef = (el: any, userId: number) => { if (el) videoChartRefs[userId] = el as HTMLElement }

// ==================== 工具 ====================
const formatNumber = (num?: number | null) => {
  if (num == null || isNaN(num)) return '0'
  return num.toLocaleString('zh-CN')
}
const formatPct = (val?: number | null) => {
  if (val == null || isNaN(val)) return '0%'
  return val.toFixed(2) + '%'
}
const getEfficiency = (rate?: number) => (rate ?? 0) >= 80 ? 'A+' : (rate ?? 0) >= 60 ? 'A' : 'B+'
const getEfficiencyClass = (rate?: number) => (rate ?? 0) >= 60 ? 'text-success' : 'text-warning'

// ==================== 操作 ====================
const handleSearch = () => { loadData() }
const handleReset = () => {
  searchForm.timeDimension = 'WEEK'
  searchForm.statDate = undefined
  searchForm.ipGroupId = undefined
  searchForm.keyword = undefined
  loadData()
}
const EFFICIENCY_EXPORT_COLUMNS = [
  { key: 'userName', label: '经办人' },
  { key: 'position', label: '岗位' },
  { key: 'ipGroupName', label: 'IP组' },
  { key: 'taskTotal', label: '任务总数' },
  { key: 'taskCompleted', label: '已完成' },
  { key: 'completionRate', label: '完成率(%)' },
  { key: 'costAmount', label: '账号成本' },
  { key: 'revenue', label: '营收' },
  { key: 'roi', label: 'ROI(%)' },
  { key: 'orderCount', label: '订单数' },
  { key: 'contentOutput', label: '发布作品' },
  { key: 'avgRead', label: '平均阅读' },
  { key: 'avgPlay', label: '平均播放' },
  { key: 'hitCount', label: '爆款数' },
]

const handleExport = async () => {
  try {
    await exportProductivityCsv({
      startDate: statDateToRange('start'),
      endDate: statDateToRange('end'),
      timeDimension: searchForm.timeDimension,
      ipGroupId: searchForm.ipGroupId,
      userId: undefined,
      keyword: searchForm.keyword,
    })
    ElMessage.success('导出任务已提交')
  } catch (e) {
    console.error('[Efficiency] 后端导出失败，降级为前端导出:', e)
    exportToExcel(productivityList.value, '人效分析', { columns: EFFICIENCY_EXPORT_COLUMNS })
  }
}

// statDate ('2026-05') 转为 start/end LocalDate 范围
const statDateToRange = (kind: 'start' | 'end'): string => {
  const [y, m] = (searchForm.statDate || '2026-05').split('-').map(Number)
  if (kind === 'start') {
    return `${y}-${String(m).padStart(2, '0')}-01`
  } else {
    const lastDay = new Date(y, m, 0).getDate()
    return `${y}-${String(m).padStart(2, '0')}-${lastDay}`
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: ProductivityReviewQuery = {
      timeDimension: searchForm.timeDimension,
      ipGroupId: searchForm.ipGroupId,
      keyword: searchForm.keyword,
      page: 1,
      size: 50,
      ...(searchForm.statDate
        ? { startDate: statDateToRange('start'), endDate: statDateToRange('end') }
        : {}),
    }
    const res = await getProductivityList(params)
    productivityList.value = res?.list || []
    totalCount.value = res?.total ?? 0
  } catch (e) {
    console.error('[Efficiency] 加载失败:', e)
    ElMessage.error('人效数据加载失败：' + (e instanceof Error ? e.message : String(e)))
    productivityList.value = []
    totalCount.value = 0
  } finally {
    loading.value = false
  }
}

// mini-chart 渲染（基于后端 detail 接口返回的趋势，按需加载）
const renderMiniChart = (el: HTMLElement, data: { date: string; revenue: number; orderCount: number }[], chartStore: Record<number, echarts.ECharts>, userId: number) => {
  if (!el || el.clientWidth === 0) {
    setTimeout(() => renderMiniChart(el, data, chartStore, userId), 100)
    return
  }
  if (chartStore[userId]) chartStore[userId].dispose()
  const chart = echarts.init(el)
  chartStore[userId] = chart
  // 空数据时显示 placeholder
  if (data.length === 0) {
    chart.setOption({
      grid: { left: 0, right: 0, top: 0, bottom: 0 },
      xAxis: { type: 'category', show: false, data: [] },
      yAxis: { type: 'value', show: false, min: 0, max: 1 },
      series: [{ type: 'bar', data: [], itemStyle: { color: '#909399' } }],
      title: { text: '暂无趋势数据', textStyle: { color: '#909399', fontSize: 11 }, left: 'center', top: 'middle' },
    })
    return
  }
  chart.setOption({
    grid: { left: 0, right: 0, top: 0, bottom: 0 },
    tooltip: { trigger: 'axis', formatter: (params: any) => `${params[0].axisValue}<br/>营收: ¥${params[0].value.toLocaleString()}` },
    xAxis: { type: 'category', show: false, data: data.map(d => d.date) },
    yAxis: { type: 'value', show: false },
    series: [{ type: 'bar', data: data.map(d => Math.max(d.revenue, 1)), itemStyle: { color: '#67c23a', borderRadius: [2, 2, 0, 0] }, barWidth: '60%' }],
  })
}

// 按需加载趋势（S-R10 补 B10：展开行时调 detail 接口拿 trend 数据）
const expandedUserIds = new Set<number>()
const handleExpand = async (row: ProductivityReviewVO, expandedRows: ProductivityReviewVO[]) => {
  const userId = row.userId
  if (expandedRows.find(r => r.userId === userId) && !expandedUserIds.has(userId)) {
    // 展开时拉数据
    expandedUserIds.add(userId)
    try {
      const detail = await getProductivityDetail(userId, {})
      const trend = detail.trend || []
      await nextTick()
      const we = wechatChartRefs[userId]
      const ve = videoChartRefs[userId]
      if (we) renderMiniChart(we, trend, wechatCharts, userId)
      if (ve) renderMiniChart(ve, trend, videoCharts, userId)
    } catch (e) {
      console.error('[Efficiency] 加载趋势失败:', e)
      // 失败时渲染空
      await nextTick()
      const we = wechatChartRefs[userId]
      const ve = videoChartRefs[userId]
      if (we) renderMiniChart(we, [], wechatCharts, userId)
      if (ve) renderMiniChart(ve, [], videoCharts, userId)
    }
  } else if (!expandedRows.find(r => r.userId === userId)) {
    // 折叠时清理
    expandedUserIds.delete(userId)
    if (wechatCharts[userId]) {
      wechatCharts[userId].dispose()
      delete wechatCharts[userId]
    }
    if (videoCharts[userId]) {
      videoCharts[userId].dispose()
      delete videoCharts[userId]
    }
  }
}

onMounted(async () => {
  await loadData()
})
</script>

<style scoped lang="scss">
.efficiency-page { padding: 20px; }
.main-card { margin-top: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.main-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.text-success { color: #67c23a; font-weight: 600; }
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.expand-content { padding: 16px 24px; background: #f5f7fa; }
.expand-card .expand-title { display: flex; align-items: center; font-size: 13px; color: #909399; margin-bottom: 8px; gap: 4px; }
.expand-card .expand-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; }
.expand-card .expand-row b { color: #303133; }
.expand-card .expand-desc { font-size: 12px; color: #909399; margin-top: 4px; }
.mini-chart { height: 40px; margin-top: 8px; }
</style>
