<!--
  M10 - 采集日志
  依据: UX-M10 § 4 P-M10-003
-->
<template>
  <div class="collect-log-page">

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="任务">
        <el-select v-model="searchForm.taskId" placeholder="全部任务" clearable filterable>
          <el-option
            v-for="t in taskOptions"
            :key="t.id"
            :label="t.name"
            :value="t.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_collect_status" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="执行时间">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
    </TableSearch>

    <ContentWrap title="采集日志">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <template #empty>
          <el-empty description="暂无日志" />
        </template>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="taskName" label="任务名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startAt" label="开始时间" width="170" align="center" />
        <el-table-column prop="durationMs" label="耗时" width="100" align="right">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="采集记录" width="100" align="right" />
        <el-table-column prop="retryCount" label="重试次数" width="90" align="center" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="240">
          <template #default="{ row }">
            <el-button v-if="row.errorMessage" link type="danger" @click="showError(row.errorMessage)">
              查看
            </el-button>
            <span v-else style="color: #67c23a">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewTask(row)">查看任务</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => (pagination.pageNo = val)"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <el-drawer v-model="errorVisible" title="错误详情" size="500px">
      <pre style="white-space: pre-wrap; word-break: break-all; color: #f56c6c; line-height: 1.6">{{ errorText }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import { getCollectLogPage, getCollectTaskPage } from '@/api/collect'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const taskOptions = ref<any[]>([])
const errorVisible = ref(false)
const errorText = ref('')

const searchForm = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined as number | undefined,
  status: undefined as string | undefined,
  dateRange: [] as string[],
  pageNo: 1,
  pageSize: 20,
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const statusText = (s: string) => ({ SUCCESS: '成功', FAILED: '失败', PARTIAL: '部分成功' }[s] || s)
const getStatusType = (s: string) => ({ SUCCESS: 'success', FAILED: 'danger', PARTIAL: 'warning' }[s] || '')

const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m${Math.floor((ms % 60000) / 1000)}s`
}

const loadTasks = async () => {
  try {
    const res = await getCollectTaskPage({ pageNo: 1, pageSize: 200 }) as any
    taskOptions.value = res.list || res.data?.list || []
  } catch {
    taskOptions.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      taskId: searchForm.taskId,
      status: searchForm.status,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    }
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getCollectLogPage(params) as any
    tableData.value = res.list || res.data?.list || []
    pagination.total = res.total ?? res.data?.total ?? tableData.value.length
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, { taskId: undefined, status: undefined, dateRange: [] })
  pagination.pageNo = 1
  loadData()
}

const showError = (text: string) => {
  errorText.value = text
  errorVisible.value = true
}
const handleViewTask = (row: any) => router.push(`/collect/task/${row.taskId}`)

onMounted(() => {
  loadTasks()
  loadData()
})
</script>

<style scoped>
.collect-log-page { padding: 20px; }
</style>
