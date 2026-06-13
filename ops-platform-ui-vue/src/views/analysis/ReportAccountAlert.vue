<template>
  <div class="report-page">
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" :loading="exportLoading" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </el-form-item>
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
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictLabel from '@/components/DictLabel.vue'
import { getAccountAlertList } from '@/api/report'
import { exportToExcel, unwrapApiData, pickListPage, reportField, fetchAllPaginated } from '@/utils'

const loading = ref(false)
const exportLoading = ref(false)
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

const buildQ = (page = pageNum.value, size = pageSize.value) => {
  const q: Record<string, any> = { pageNum: page, pageSize: size }
  if (filter.dateRange?.length === 2) { q.startDate = filter.dateRange[0]; q.endDate = filter.dateRange[1] }
  return q
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllPaginated(async (page, size) =>
      pickListPage(unwrapApiData(await getAccountAlertList(buildQ(page, size)))),
    )
    const exportData = rows.map(row => ({
      date: reportField(row, 'date', 'statDate'),
      accountName: reportField(row, 'account_name', 'accountName'),
      level: String(reportField(row, 'level', 'alertLevel') || ''),
      type: alertTypeLabel(String(reportField(row, 'type', 'alertType') || '')),
      message: reportField(row, 'message', 'description'),
    }))
    exportToExcel(
      exportData,
      [
        { key: 'date', label: '日期' },
        { key: 'accountName', label: '账号' },
        { key: 'level', label: '级别' },
        { key: 'type', label: '类型' },
        { key: 'message', label: '预警内容' },
      ],
      '异常预警',
    )
  } catch (e) {
    console.error(e)
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
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
