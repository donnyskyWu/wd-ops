<template>
  <div class="report-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>异常预警</el-breadcrumb-item>
    </el-breadcrumb>
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap title="异常预警" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="日期" width="120">
          <template #default="{ row }">{{ reportField(row, 'date', 'statDate') }}</template>
        </el-table-column>
        <el-table-column label="账号" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ reportField(row, 'account_name', 'accountName') }}</template>
        </el-table-column>
        <el-table-column label="级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelType(String(reportField(row, 'level', 'alertLevel') || ''))" size="small">
              <DictLabel dict-type="dict_alert_level" :value="reportField(row, 'level', 'alertLevel')" />
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">{{ alertTypeLabel(String(reportField(row, 'type', 'alertType') || '')) }}</template>
        </el-table-column>
        <el-table-column label="预警内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ reportField(row, 'message', 'description') }}</template>
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
import { getAccountAlertList } from '@/api/report'
import { unwrapApiData, pickListPage, reportField } from '@/utils'

const loading = ref(false)
const filter = reactive({ dateRange: [] as string[] })
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const list = ref<any[]>([])

const getLevelType = (l: string) => {
  const map: Record<string, string> = {
    CRITICAL: 'danger', HIGH: 'danger', URGENT: 'danger',
    WARNING: 'warning', MEDIUM: 'warning', IMPORTANT: 'warning',
    INFO: 'info', LOW: 'info',
  }
  return map[l] || ''
}

const alertTypeLabel = (t: string) => {
  const map: Record<string, string> = {
    ACCOUNT_OFFLINE: '账号掉线',
    DATA_ABNORMAL: '数据异常',
    FAN_FLUCTUATION: '粉丝波动',
  }
  return map[t] || t || '-'
}

const buildQ = () => {
  const q: Record<string, any> = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filter.dateRange?.length === 2) { q.startDate = filter.dateRange[0]; q.endDate = filter.dateRange[1] }
  return q
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAccountAlertList(buildQ())
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
