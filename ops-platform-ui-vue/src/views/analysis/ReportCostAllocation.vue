<template>
  <div class="report-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>成本分摊</el-breadcrumb-item>
    </el-breadcrumb>
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="账号">
          <AccountSelect v-model="filter.accountId" style="width: 220px" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap title="成本分摊明细" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="账号" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ reportField(row, 'account_name', 'accountName') }}</template>
        </el-table-column>
        <el-table-column label="成本类型" width="120" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_cost_type" :value="reportField(row, 'cost_type', 'costType')" />
          </template>
        </el-table-column>
        <el-table-column label="金额(元)" width="140" align="right">
          <template #default="{ row }">¥ {{ Number(reportField(row, 'amount') || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="占比" width="100" align="right">
          <template #default="{ row }">{{ (Number(reportField(row, 'share_ratio', 'shareRatio') || 0) * 100).toFixed(2) }}%</template>
        </el-table-column>
        <el-table-column label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ reportField(row, 'remark') || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination :current-page="pageNum" :page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next"
        class="pagination" @update:current-page="(v) => { pageNum = v; loadData() }"
        @update:page-size="(v) => { pageSize = v; pageNum = 1; loadData() }" />
    </ContentWrap>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictLabel from '@/components/DictLabel.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import { getCostAllocationList } from '@/api/report'
import { unwrapApiData, pickListPage, reportField } from '@/utils'

const loading = ref(false)
const filter = reactive({ accountId: undefined as number | undefined, dateRange: [] as string[] })
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const list = ref<any[]>([])

const buildQuery = () => {
  const q: Record<string, any> = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filter.accountId) q.accountId = filter.accountId
  if (filter.dateRange?.length === 2) { q.startDate = filter.dateRange[0]; q.endDate = filter.dateRange[1] }
  return q
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCostAllocationList(buildQuery())
    const page = pickListPage(unwrapApiData(res))
    list.value = page.list
    total.value = page.total
  } catch (e) { console.error(e); list.value = [] }
  finally { loading.value = false }
}

onMounted(() => loadData())
</script>
<style scoped>
.report-page { padding: 20px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
