<template>
  <div class="report-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>团队配置</el-breadcrumb-item>
    </el-breadcrumb>
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" style="width: 220px" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
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
import ContentWrap from '@/components/ContentWrap.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { getTeamConfigList } from '@/api/report'
import { unwrapApiData, reportField } from '@/utils'

const loading = ref(false)
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

onMounted(() => loadData())
</script>
<style scoped>
.report-page { padding: 20px; }
</style>
