<!--
  M6-7 团队配置
  依据: FR-M6-007
  路径: /analysis/report/team-config
-->
<template>
  <div class="report-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>团队配置</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="部门">
          <el-select v-model="filter.dept" clearable style="width: 200px">
            <el-option label="抖音矩阵组" value="dy" />
            <el-option label="公众号组" value="mp" />
            <el-option label="小红书组" value="xhs" />
            <el-option label="视频号组" value="sp" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card><div class="kpi-label">在岗人数</div><div class="kpi-value primary">{{ kpi.total }}</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="kpi-label">人均账号</div><div class="kpi-value success">{{ kpi.avgAccount.toFixed(1) }}</div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><div class="kpi-label">人均 GMV</div><div class="kpi-value warning">¥ {{ kpi.avgGmv.toLocaleString() }}</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <ContentWrap title="岗位人数分布">
          <div ref="pieRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
      <el-col :span="12">
        <ContentWrap title="各组人效">
          <div ref="barRef" style="width: 100%; height: 320px" />
        </ContentWrap>
      </el-col>
    </el-row>

    <ContentWrap title="员工明细" style="margin-top: 16px">
      <el-table :data="list" border stripe>
        <el-table-column prop="name" label="员工" min-width="120" />
        <el-table-column prop="position" label="岗位" width="120" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.position }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="dept" label="部门" min-width="120" />
        <el-table-column prop="accounts" label="管理账号" width="100" align="right" />
        <el-table-column prop="gmv" label="GMV" width="120" align="right">
          <template #default="{ row }">¥ {{ row.gmv.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="人效" width="100" align="right">
          <template #default="{ row }">
            <el-tag :type="row.efficiency > 50000 ? 'success' : 'info'" size="small">
              ¥ {{ Math.round(row.efficiency).toLocaleString() }}
            </el-tag>
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

const loading = ref(false)
const filter = reactive({ dept: undefined as string | undefined })
const kpi = reactive({ total: 0, avgAccount: 0, avgGmv: 0 })
const list = ref<any[]>([])
const pieRef = ref<HTMLDivElement | null>(null)
const barRef = ref<HTMLDivElement | null>(null)
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

const initPie = () => {
  if (!pieRef.value) return
  const el = pieRef.value
  if (el.getBoundingClientRect().width === 0) return setTimeout(initPie, 100)
  if (!pieChart) pieChart = echarts.init(el)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: '70%', itemStyle: { borderRadius: 6 },
      data: [
        { name: '运营专员', value: 24 },
        { name: '运营主管', value: 8 },
        { name: '内容编辑', value: 16 },
        { name: '主播', value: 6 },
        { name: '其他', value: 4 },
      ],
    }],
  })
}
const initBar = () => {
  if (!barRef.value) return
  const el = barRef.value
  if (el.getBoundingClientRect().width === 0) return setTimeout(initBar, 100)
  if (!barChart) barChart = echarts.init(el)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 16, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: ['抖音矩阵组', '公众号组', '小红书组', '视频号组', '快手组'] },
    yAxis: { type: 'value', name: '万元' },
    series: [{ type: 'bar', data: [12, 8, 6, 3, 2], itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] } }],
  })
}

const loadData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 400))
  kpi.total = 58
  kpi.avgAccount = 1.86
  kpi.avgGmv = 22068
  list.value = Array.from({ length: 10 }, (_, i) => {
    const accounts = 1 + Math.floor(Math.random() * 4)
    const gmv = 20000 + Math.floor(Math.random() * 80000)
    return {
      name: ['张三', '李四', '王五', '赵六', '钱七', '孙八', '周九', '吴十', '郑一', '王二'][i],
      position: ['运营专员', '运营主管', '内容编辑', '主播'][i % 4],
      dept: ['抖音矩阵组', '公众号组', '小红书组', '视频号组', '快手组'][i % 5],
      accounts,
      gmv,
      efficiency: gmv / Math.max(1, accounts),
    }
  })
  loading.value = false
  await nextTick()
  setTimeout(() => { initPie(); initBar() }, 100)
}

onMounted(loadData)
</script>

<style scoped>
.report-page { padding: 20px; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.4; text-align: center; padding: 8px 0; }
.kpi-value.primary { color: #409eff; }
.kpi-value.success { color: #67c23a; }
.kpi-value.warning { color: #e6a23c; }
</style>
