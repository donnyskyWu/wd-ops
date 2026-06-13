<template>
  <div class="report-page">
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" style="width: 220px" />
        </el-form-item>
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
    <ContentWrap title="ROI 明细" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="ip_group_name" label="IP 组" min-width="180" />
        <el-table-column prop="platform" label="平台" width="100" align="center" />
        <el-table-column prop="revenue" label="营收" width="140" align="right">
          <template #default="{ row }">¥ {{ Number(row.revenue || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="cost" label="成本" width="140" align="right">
          <template #default="{ row }">¥ {{ Number(row.cost || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="roi" label="ROI" width="100" align="right">
          <template #default="{ row }">
            <el-tag :type="(row.roi || 0) >= 1 ? 'success' : 'danger'" size="small">
              {{ Number(row.roi || 0).toFixed(2) }}
            </el-tag>
          </template>
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
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { getReportRoiList } from '@/api/report'
import { exportToExcel, unwrapApiData, pickListPage, reportField, fetchAllPaginated } from '@/utils'

const loading = ref(false)
const exportLoading = ref(false)
const filter = reactive({ ipGroupId: undefined as number | undefined, dateRange: [] as string[] })
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const list = ref<any[]>([])

const buildQ = (page = pageNum.value, size = pageSize.value) => {
  const q: Record<string, any> = { pageNum: page, pageSize: size }
  if (filter.ipGroupId) q.ipGroupId = filter.ipGroupId
  if (filter.dateRange?.length === 2) { q.startDate = filter.dateRange[0]; q.endDate = filter.dateRange[1] }
  return q
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllPaginated(async (page, size) =>
      pickListPage(unwrapApiData(await getReportRoiList(buildQ(page, size)))),
    )
    const exportData = rows.map(row => ({
      date: reportField(row, 'date', 'statDate'),
      ipGroupName: reportField(row, 'ip_group_name', 'ipGroupName'),
      platform: reportField(row, 'platform', 'platformType'),
      revenue: Number(reportField(row, 'revenue') || 0).toFixed(2),
      cost: Number(reportField(row, 'cost') || 0).toFixed(2),
      roi: Number(reportField(row, 'roi') || 0).toFixed(2),
    }))
    exportToExcel(
      exportData,
      [
        { key: 'date', label: '日期' },
        { key: 'ipGroupName', label: 'IP 组' },
        { key: 'platform', label: '平台' },
        { key: 'revenue', label: '营收' },
        { key: 'cost', label: '成本' },
        { key: 'roi', label: 'ROI' },
      ],
      '数据报表ROI分析',
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
    const res: any = await getReportRoiList(buildQ())
    const data = res?.data ?? res
    list.value = data?.list ?? data?.records ?? []
    total.value = data?.total ?? list.value.length
  } catch (e) { console.error(e); list.value = [] }
  finally { loading.value = false }
}

onMounted(() => loadData())
</script>
<style scoped>
.report-page { padding: 20px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
