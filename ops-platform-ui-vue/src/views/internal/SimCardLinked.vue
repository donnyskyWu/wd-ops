<template>
  <div class="sim-linked-page" v-loading="loading">

    <el-card v-if="summary" shadow="never">
      <div class="header">
        <div>
          <h2 style="margin: 0">
            <el-icon style="vertical-align: middle"><Iphone /></el-icon>
            {{ summary.phoneNumberMasked }}
            <el-tag style="margin-left: 8px">{{ OPERATOR_MAP[summary.operator || ''] || summary.operator }}</el-tag>
          </h2>
          <p class="meta">
            <span>关联账号：{{ summary.totalCount ?? 0 }} 个</span>
            <el-divider direction="vertical" />
            <span>覆盖平台：{{ platformCount }} 个</span>
          </p>
        </div>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <ContentWrap title="平台账号矩阵">
          <div class="filters">
            <DictSelect
              v-model="filterPlatform"
              dict-type="dict_platform_type"
              placeholder="全部平台"
              clearable
              style="width: 180px"
              @change="loadDetail"
            />
            <DictSelect
              v-model="filterOperator"
              dict-type="dict_sim_operator"
              placeholder="全部运营商"
              clearable
              style="width: 180px; margin-left: 12px"
              @change="loadDetail"
            />
          </div>
          <el-input v-model="keyword" placeholder="搜索账号名" clearable style="margin: 12px 0" />
          <el-table :data="filteredAccounts" border stripe>
            <el-table-column prop="accountName" label="账号名" min-width="160" show-overflow-tooltip />
            <el-table-column prop="platformLabel" label="平台" width="100" align="center">
              <template #default="{ row }">
                <el-tag>{{ row.platformLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="accountId" label="平台ID" min-width="120" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">
                  {{ row.status === 'NORMAL' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="linkedAt" label="关联时间" width="170" />
          </el-table>
        </ContentWrap>
      </el-col>
      <el-col :span="10">
        <ContentWrap title="平台分布">
          <div ref="platformChartRef" style="width: 100%; height: 320px" />
        </ContentWrap>
        <ContentWrap title="关联链路" style="margin-top: 16px">
          <el-steps direction="vertical" :active="accounts.length">
            <el-step title="SIM 卡" :description="summary?.phoneNumberMasked" />
            <el-step
              v-for="(a, i) in accounts"
              :key="i"
              :title="a.platformLabel + ' / ' + a.accountName"
              :description="a.accountId || ''"
            />
          </el-steps>
        </ContentWrap>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Iphone } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import { getLinkedAccounts, type LinkedAccountVO, type LinkedAccountsResult } from '@/api/simcard'

const OPERATOR_MAP: Record<string, string> = {
  MOBILE: '中国移动',
  UNICOM: '中国联通',
  TELECOM: '中国电信',
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const summary = ref<LinkedAccountsResult | null>(null)
const accounts = ref<LinkedAccountVO[]>([])
const keyword = ref('')
const filterPlatform = ref<string>()
const filterOperator = ref<string>()
const platformChartRef = ref<HTMLDivElement | null>(null)
let platformChart: echarts.ECharts | null = null

const filteredAccounts = computed(() => {
  if (!keyword.value) return accounts.value
  const k = keyword.value.toLowerCase()
  return accounts.value.filter((a) => a.accountName.toLowerCase().includes(k))
})
const platformCount = computed(() => new Set(accounts.value.map((a) => a.platformType)).size)

const initPlatformChart = () => {
  if (!platformChartRef.value) return
  const el = platformChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(initPlatformChart, 100)
    return
  }
  if (!platformChart) {
    platformChart = echarts.init(el)
  }
  const groups: Record<string, number> = {}
  accounts.value.forEach((a) => {
    groups[a.platformLabel] = (groups[a.platformLabel] || 0) + 1
  })
  platformChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: Object.entries(groups).map(([n, v]) => ({ name: n, value: v })),
    }],
  })
}

const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getLinkedAccounts(id, {
      platformType: filterPlatform.value,
      operator: filterOperator.value,
    })
    summary.value = res
    accounts.value = res.accounts || []
  } catch {
    summary.value = null
    accounts.value = []
  } finally {
    loading.value = false
    await nextTick()
    setTimeout(initPlatformChart, 100)
  }
}

watch(accounts, () => setTimeout(initPlatformChart, 50), { deep: true })
onMounted(loadDetail)
</script>

<style scoped>
.sim-linked-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-start; }
.meta { color: #909399; font-size: 13px; margin: 8px 0 0 0; }
.filters { display: flex; align-items: center; }
</style>
