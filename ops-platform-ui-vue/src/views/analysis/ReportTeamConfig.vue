<template>
  <div class="report-page">
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" style="width: 220px" />
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
    <ContentWrap title="团队配置概览" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="IP 组" min-width="200">
          <template #default="{ row }">{{ reportField(row, 'ip_group_name', 'ipGroupName') }}</template>
        </el-table-column>
        <el-table-column label="人员数" width="100" align="right">
          <template #default="{ row }">{{ reportField(row, 'user_count', 'userCount') }}</template>
        </el-table-column>
        <el-table-column label="账号数" width="100" align="right">
          <template #default="{ row }">{{ reportField(row, 'account_count', 'accountCount') }}</template>
        </el-table-column>
        <el-table-column label="人均账号" width="120" align="right">
          <template #default="{ row }">{{ reportField(row, 'avg_account_per_user', 'avgAccountPerUser') }}</template>
        </el-table-column>
        <el-table-column label="人均营收" width="140" align="right">
          <template #default="{ row }">{{ reportField(row, 'revenue_per_user', 'revenuePerUser') }}</template>
        </el-table-column>
        <el-table-column label="人效" width="120" align="right">
          <template #default="{ row }">{{ reportField(row, 'efficiency') }}</template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { getTeamConfigList } from '@/api/report'
import { exportToExcel, unwrapApiData, reportField } from '@/utils'

const loading = ref(false)
const exportLoading = ref(false)
const filter = reactive({ ipGroupId: undefined as number | undefined })
const list = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTeamConfigList({ ipGroupId: filter.ipGroupId })
    const data = unwrapApiData(res)
    list.value = Array.isArray(data) ? data : []
  } catch (e) { console.error(e); list.value = [] }
  finally { loading.value = false }
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const res = await getTeamConfigList({ ipGroupId: filter.ipGroupId })
    const data = unwrapApiData(res)
    const rows = Array.isArray(data) ? data : []
    const exportData = rows.map(row => ({
      ipGroupName: reportField(row, 'ip_group_name', 'ipGroupName'),
      userCount: reportField(row, 'user_count', 'userCount'),
      accountCount: reportField(row, 'account_count', 'accountCount'),
      avgAccountPerUser: reportField(row, 'avg_account_per_user', 'avgAccountPerUser'),
      revenuePerUser: reportField(row, 'revenue_per_user', 'revenuePerUser'),
      efficiency: reportField(row, 'efficiency'),
    }))
    exportToExcel(
      exportData,
      [
        { key: 'ipGroupName', label: 'IP 组' },
        { key: 'userCount', label: '人员数' },
        { key: 'accountCount', label: '账号数' },
        { key: 'avgAccountPerUser', label: '人均账号' },
        { key: 'revenuePerUser', label: '人均营收' },
        { key: 'efficiency', label: '人效' },
      ],
      '团队配置',
    )
  } catch (e) {
    console.error(e)
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => loadData())
</script>
<style scoped>
.report-page { padding: 20px; }
</style>
