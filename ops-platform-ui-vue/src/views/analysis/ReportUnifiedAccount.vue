<template>
  <div class="report-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/data-report' }">数据报表</el-breadcrumb-item>
      <el-breadcrumb-item>统一视图</el-breadcrumb-item>
    </el-breadcrumb>
    <ContentWrap>
      <el-form :model="filter" inline>
        <el-form-item label="IP 组">
          <IpGroupTreeSelect v-model="filter.ipGroupId" style="width: 220px" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="filter.platformType" placeholder="全部" clearable style="width: 160px">
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="快手" value="KUAISHOU" />
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="视频号" value="VIDEO_ACCOUNT" />
            <el-option label="公众号" value="WECHAT_OFFICIAL" />
            <el-option label="个微" value="WECHAT_PERSONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filter.dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap title="全平台账号 KPI" style="margin-top: 16px">
      <el-row :gutter="16">
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="账号总数" :value="stats.totalAccounts || 0" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="总粉丝" :value="stats.totalFollowers || 0" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="总营收" :value="Number(stats.totalRevenue || 0).toFixed(2)" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-statistic title="综合 ROI" :value="Number(stats.overallRoi || 0).toFixed(2)" /></el-card></el-col>
      </el-row>
    </ContentWrap>
    <ContentWrap title="账号明细" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="账号" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ reportField(row, 'account_name', 'accountName') }}</template>
        </el-table-column>
        <el-table-column label="平台" width="100" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_platform_type" :value="reportField(row, 'platform_type', 'platformType')" />
          </template>
        </el-table-column>
        <el-table-column label="IP 组" width="150">
          <template #default="{ row }">{{ reportField(row, 'ip_group_name', 'ipGroupName') }}</template>
        </el-table-column>
        <el-table-column label="粉丝数" width="120" align="right">
          <template #default="{ row }">{{ reportField(row, 'follower_count', 'followerCount') }}</template>
        </el-table-column>
        <el-table-column label="营收" width="120" align="right">
          <template #default="{ row }">{{ reportField(row, 'revenue') }}</template>
        </el-table-column>
        <el-table-column label="成本" width="120" align="right">
          <template #default="{ row }">{{ reportField(row, 'cost') }}</template>
        </el-table-column>
        <el-table-column label="ROI" width="100" align="right">
          <template #default="{ row }">{{ reportField(row, 'roi') }}</template>
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
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import { getUnifiedAccountList, getUnifiedAccountStats } from '@/api/report'
import { unwrapApiData, pickListPage, reportField } from '@/utils'

const loading = ref(false)
const filter = reactive({ ipGroupId: undefined as number | undefined, platformType: undefined as string | undefined, dateRange: [] as string[] })
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const list = ref<any[]>([])
const stats = reactive<any>({ totalAccounts: 0, totalFollowers: 0, totalRevenue: 0, overallRoi: 0 })

const buildQuery = () => {
  const q: Record<string, any> = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filter.ipGroupId) q.ipGroupId = filter.ipGroupId
  if (filter.platformType) q.platformType = filter.platformType
  if (filter.dateRange?.length === 2) { q.startDate = filter.dateRange[0]; q.endDate = filter.dateRange[1] }
  return q
}

const loadData = async () => {
  loading.value = true
  try {
    const q = buildQuery()
    const [listRes, statsRes] = await Promise.all([getUnifiedAccountList(q), getUnifiedAccountStats(q)])
    const l = pickListPage(unwrapApiData(listRes))
    list.value = l.list
    total.value = l.total
    Object.assign(stats, unwrapApiData(statsRes))
  } catch (e) { console.error(e); list.value = [] }
  finally { loading.value = false }
}

onMounted(() => loadData())
</script>
<style scoped>
.report-page { padding: 20px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
