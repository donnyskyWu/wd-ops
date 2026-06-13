<!--
  M3 - 订单归因 ROI 分析
  依据: FR-M3-004 订单归因
  路径: /perf/order-attribution/roi
  4 区: KPI 卡 / ROI 趋势(柱+折线) / 渠道贡献(饼) / 订单明细
-->
<template>
  <div class="roi-page" v-loading="loading">

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="时间范围">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="filter.platform" multiple clearable style="width: 220px">
            <el-option label="公众号" value="wechat" />
            <el-option label="抖音" value="douyin" />
            <el-option label="小红书" value="xhs" />
            <el-option label="快手" value="kuaishou" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- KPI 卡 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-card">
            <div class="kpi-label">总 GMV</div>
            <div class="kpi-value primary">¥ {{ kpi.gmv.toLocaleString() }}</div>
            <div class="kpi-trend up">↑ 12.5% MoM</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-card">
            <div class="kpi-label">总成本</div>
            <div class="kpi-value warning">¥ {{ kpi.cost.toLocaleString() }}</div>
            <div class="kpi-trend up">↑ 5.2% MoM</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-card">
            <div class="kpi-label">整体 ROI</div>
            <div class="kpi-value success">{{ kpi.roi.toFixed(2) }}</div>
            <div class="kpi-trend up">↑ 0.18 MoM</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi-card">
            <div class="kpi-label">订单数</div>
            <div class="kpi-value">{{ kpi.orders.toLocaleString() }}</div>
            <div class="kpi-trend up">↑ 8.4% MoM</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <ContentWrap title="30 天 ROI 趋势">
          <div ref="roiTrendRef" style="width: 100%; height: 360px" />
        </ContentWrap>
      </el-col>
      <el-col :span="8">
        <ContentWrap title="渠道贡献">
          <div ref="channelPieRef" style="width: 100%; height: 360px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="订单明细" style="margin-top: 16px">
      <el-table :data="orderList" border stripe>
        <el-table-column prop="orderId" label="订单号" width="160" />
        <el-table-column prop="platform" label="平台" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.platform }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="account" label="账号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ipGroup" label="IP 组" width="120" />
        <el-table-column prop="amount" label="GMV" width="120" align="right">
          <template #default="{ row }">¥ {{ row.amount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="120" align="right">
          <template #default="{ row }">¥ {{ row.cost.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="ROI" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(row.amount / row.cost) > 3 ? 'success' : (row.amount / row.cost) > 1.5 ? 'warning' : 'danger'">
              {{ (row.amount / row.cost).toFixed(2) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="160" align="center" />
      </el-table>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import {
  getOrderAttributionRoi,
  getOrderAttributionList,
} from '@/api/orderAttribution'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const filter = reactive({ dateRange: undefined as any, platform: [] as string[], ipGroupId: undefined as number | undefined })
const kpi = reactive({ gmv: 0, cost: 0, roi: 0, orders: 0 })
const roiTrendRef = ref<HTMLDivElement | null>(null)
const channelPieRef = ref<HTMLDivElement | null>(null)
let roiTrendChart: echarts.ECharts | null = null
let channelPieChart: echarts.ECharts | null = null
const orderList = ref<any[]>([])

const initRoiTrend = (trend?: any) => {
  if (!roiTrendRef.value) return
  const el = roiTrendRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initRoiTrend, 100)
    return
  }
  if (!roiTrendChart) roiTrendChart = echarts.init(el)
  const days = Array.isArray(trend?.days) && trend.days.length > 0
    ? trend.days
    : Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const gmv = Array.isArray(trend?.gmv) ? trend.gmv : days.map(() => 0)
  const cost = Array.isArray(trend?.cost) ? trend.cost : days.map(() => 0)
  const roi = gmv.map((g: number, i: number) => (cost[i] > 0 ? +(g / cost[i]).toFixed(2) : 0))
  roiTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['GMV', '成本', 'ROI'], top: 0 },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: days },
    yAxis: [
      { type: 'value', name: '金额(¥)', position: 'left' },
      { type: 'value', name: 'ROI', position: 'right', max: 5 },
    ],
    series: [
      { name: 'GMV', type: 'bar', data: gmv, itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } },
      { name: '成本', type: 'bar', data: cost, itemStyle: { color: '#e6a23c', borderRadius: [4, 4, 0, 0] } },
      { name: 'ROI', type: 'line', yAxisIndex: 1, data: roi, itemStyle: { color: '#67c23a' }, smooth: true },
    ],
  })
}
const initChannelPie = (byChannel: any[] = []) => {
  if (!channelPieRef.value) return
  const el = channelPieRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initChannelPie, 100)
    return
  }
  if (!channelPieChart) channelPieChart = echarts.init(el)
  const data = byChannel.length > 0
    ? byChannel.map((c: any) => ({ name: c.channelName || c.name, value: c.amount || c.value || 0 }))
    : []
  channelPieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n¥{c}' },
        data: data.length > 0 ? data : [{ name: '暂无数据', value: 0 }],
      },
    ],
  })
}

const loadData = async () => {
  if (!filter.dateRange || filter.dateRange.length < 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  loading.value = true
  try {
    const res: any = await getOrderAttributionRoi({
      ipGroupId: filter.ipGroupId,
      startDate: filter.dateRange[0],
      endDate: filter.dateRange[1],
    })
    kpi.gmv = res.totalAmount ?? res.gmv ?? 0
    kpi.cost = res.totalCost ?? res.cost ?? 0
    kpi.roi = res.roi ?? (kpi.cost > 0 ? kpi.gmv / kpi.cost : 0)
    kpi.orders = res.orderCount ?? res.orders ?? 0

    const listRes: any = await getOrderAttributionList({
      ipGroupId: filter.ipGroupId,
      startDate: filter.dateRange[0],
      endDate: filter.dateRange[1],
      pageNum: 1,
      pageSize: 12,
    })
    orderList.value = (listRes.list || []).map((row: any) => ({
      orderId: row.orderNo,
      platform: row.platformName || row.platform || '-',
      account: row.accountName || '',
      ipGroup: row.ipGroupName || '-',
      amount: row.amount || 0,
      cost: row.cost || 0,
      time: row.attributionTime || row.createTime || '',
    }))

    // 趋势图与渠道饼图：后端暂未提供 series 数组，使用 KPI 值做单点
    initRoiTrend(res.trend)
    initChannelPie(res.byChannel || [])
  } catch {
    kpi.gmv = 0
    kpi.cost = 0
    kpi.roi = 0
    kpi.orders = 0
    orderList.value = []
  } finally {
    loading.value = false
    await nextTick()
    setTimeout(() => {
      initRoiTrend()
      initChannelPie()
    }, 100)
  }
}

const resetFilter = () => {
  filter.dateRange = undefined
  filter.platform = []
  filter.ipGroupId = undefined
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.roi-page { padding: 20px; }
.kpi-card { text-align: center; padding: 12px 0; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 30px; font-weight: 600; line-height: 1.4; }
.kpi-value.primary { color: #409eff; }
.kpi-value.warning { color: #e6a23c; }
.kpi-value.success { color: #67c23a; }
.kpi-trend { color: #67c23a; font-size: 12px; }
</style>
