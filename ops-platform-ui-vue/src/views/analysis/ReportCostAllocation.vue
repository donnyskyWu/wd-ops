<!--
  M6-5 成本分摊
  依据: FR-M6-005
  路径: /analysis/report/cost-allocation
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>成本分摊</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
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

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <ContentWrap title="成本分项占比">
          <div ref="pieRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="分摊明细" style="margin-top: 16px">
      <el-table :data="list" border stripe>
        <el-table-column prop="account" label="账号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipGroup" label="IP 组" min-width="120" />
        <el-table-column prop="platform" label="平台" width="100" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="ads" label="投流" width="120" align="right">
          <template #default="{ row }">¥ {{ row.ads.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="labor" label="人力" width="120" align="right">
          <template #default="{ row }">¥ {{ row.labor.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="device" label="设备" width="120" align="right">
          <template #default="{ row }">¥ {{ row.device.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="other" label="其他" width="120" align="right">
          <template #default="{ row }">¥ {{ row.other.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="total" label="合计" width="120" align="right">
          <template #default="{ row }">
            <span class="total">¥ {{ row.total.toLocaleString() }}</span>
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
const filter = reactive({ ipGroupId: undefined as number | undefined, dateRange: undefined as any })
const pieRef = ref<HTMLDivElement | null>(null)
let pieChart: echarts.ECharts | null = null
const list = ref<any[]>([])

const initPie = () => {
  if (!pieRef.value) return
  const el = pieRef.value
  if (el.getBoundingClientRect().width === 0) return setTimeout(initPie, 100)
  if (!pieChart) pieChart = echarts.init(el)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'], itemStyle: { borderRadius: 6 },
      data: [
        { name: '投流费', value: 58000 },
        { name: '人力', value: 32000 },
        { name: '设备', value: 12000 },
        { name: '差旅', value: 6000 },
        { name: '其他', value: 8000 },
      ],
    }],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 400))
  list.value = Array.from({ length: 8 }, (_, i) => {
    const ads = 5000 + Math.floor(Math.random() * 8000)
    const labor = 3000 + Math.floor(Math.random() * 4000)
    const device = 1000 + Math.floor(Math.random() * 2000)
    const other = 500 + Math.floor(Math.random() * 1500)
    return {
      account: `账号-${i + 1}`,
      ipGroup: ['头部 IP', '腰部 IP', '尾部 IP'][i % 3],
      platform: ['抖音', '公众号', '小红书', '快手'][i % 4],
      ads, labor, device, other,
      total: ads + labor + device + other,
    }
  })
  loading.value = false
  await nextTick()
  setTimeout(initPie, 100)
}

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
.total { color: #409eff; font-weight: 600; }
</style>
