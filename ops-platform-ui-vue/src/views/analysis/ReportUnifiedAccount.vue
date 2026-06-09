<!--
  M6-1 统一视图
  依据: FR-M6-001
  路径: /analysis/report/unified-account
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>统一视图</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="平台">
          <el-select v-model="filter.platform" multiple clearable style="width: 240px">
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="快手" value="KUAISHOU" />
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="视频号" value="VIDEO_ACCOUNT" />
            <el-option label="公众号" value="WECHAT_MP" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- KPI -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card>
          <div class="kpi-label">账号总数</div>
          <div class="kpi-value primary">{{ kpi.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="kpi-label">在用账号</div>
          <div class="kpi-value success">{{ kpi.active }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="kpi-label">粉丝总数</div>
          <div class="kpi-value">{{ kpi.followers.toLocaleString() }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="kpi-label">30 天 GMV</div>
          <div class="kpi-value warning">¥ {{ kpi.gmv.toLocaleString() }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <ContentWrap title="平台账号分布">
          <div ref="platformPieRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
      <el-col :span="12">
        <ContentWrap title="IP 组账号排行">
          <div ref="ipGroupBarRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="账号明细" style="margin-top: 16px">
      <el-table :data="list" border stripe>
        <el-table-column prop="account" label="账号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.platform }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipGroup" label="IP 组" min-width="120" />
        <el-table-column prop="follower" label="粉丝" width="120" align="right" />
        <el-table-column prop="work" label="作品" width="80" align="right" />
        <el-table-column prop="gmv" label="GMV" width="120" align="right">
          <template #default="{ row }">¥ {{ row.gmv.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'

const loading = ref(false)
const filter = reactive({ platform: [] as string[], ipGroupId: undefined as number | undefined, dateRange: undefined as any })
const kpi = reactive({ total: 0, active: 0, followers: 0, gmv: 0 })
const list = ref<any[]>([])
const platformPieRef = ref<HTMLDivElement | null>(null)
const ipGroupBarRef = ref<HTMLDivElement | null>(null)
let platformPieChart: echarts.ECharts | null = null
let ipGroupBarChart: echarts.ECharts | null = null

const initPlatformPie = () => {
  if (!platformPieRef.value) return
  const el = platformPieRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initPlatformPie, 100)
    return
  }
  if (!platformPieChart) platformPieChart = echarts.init(el)
  platformPieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6 },
        data: [
          { name: '抖音', value: 32 },
          { name: '快手', value: 18 },
          { name: '小红书', value: 24 },
          { name: '视频号', value: 12 },
          { name: '公众号', value: 22 },
        ],
      },
    ],
  })
}
const initIpGroupBar = () => {
  if (!ipGroupBarRef.value) return
  const el = ipGroupBarRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initIpGroupBar, 100)
    return
  }
  if (!ipGroupBarChart) ipGroupBarChart = echarts.init(el)
  ipGroupBarChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 100, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['尾部 IP', '腰部 IP', '头部 IP', '潜力 IP', '战略 IP'] },
    series: [
      { type: 'bar', data: [22, 35, 18, 28, 12], itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] } },
    ],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 400))
  kpi.total = 108
  kpi.active = 92
  kpi.followers = 8_680_000
  kpi.gmv = 1_280_000
  list.value = Array.from({ length: 8 }, (_, i) => ({
    account: `账号-${i + 1}`,
    platform: ['抖音', '公众号', '小红书', '快手'][i % 4],
    ipGroup: ['头部 IP', '腰部 IP', '尾部 IP', '潜力 IP'][i % 4],
    follower: 10000 + i * 5000,
    work: 20 + i,
    gmv: 5000 + i * 2000,
    status: i % 5 === 0 ? '停用' : '在用',
  }))
  loading.value = false
  await nextTick()
  setTimeout(() => {
    initPlatformPie()
    initIpGroupBar()
  }, 100)
}

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.4; }
.kpi-value.primary { color: #409eff; }
.kpi-value.success { color: #67c23a; }
.kpi-value.warning { color: #e6a23c; }
</style>
