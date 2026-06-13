<!--
  M10 - 采集任务列表
  依据: UX-M10 § 2 P-M10-001 / PRD-M10 FR-M10-001
-->
<template>
  <div class="collect-task-page">

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="任务名">
        <el-input v-model="searchForm.name" placeholder="搜索任务名" clearable />
      </el-form-item>
      <el-form-item label="平台">
        <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="采集方式">
        <DictSelect v-model="searchForm.method" dict-type="dict_collect_method" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="频率">
        <DictSelect v-model="searchForm.frequency" dict-type="dict_collect_frequency" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_collect_status" placeholder="全部" clearable />
      </el-form-item>
    </TableSearch>

    <ContentWrap title="采集任务">
      <template #extra>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增任务
        </el-button>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="任务名" min-width="200" show-overflow-tooltip />
        <el-table-column label="平台/账号" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div>
              <DictSelect
                :model-value="row.platformType"
                dict-type="dict_platform_type"
                disabled
                style="display: inline-block; width: auto; margin-right: 6px"
              />
              <span>{{ row.accountName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="方式" width="90" align="center">
          <template #default="{ row }">
            <el-tag>{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="frequency" label="频率" width="100" align="center" />
        <el-table-column prop="cron" label="Cron 表达式" width="140" align="center" />
        <el-table-column prop="lastRunAt" label="最近执行" width="170" align="center" />
        <el-table-column label="成功/失败" width="100" align="center">
          <template #default="{ row }">
            <span style="color: #67c23a">{{ row.runCount || 0 }}</span>
            <span style="margin: 0 4px">/</span>
            <span style="color: #f56c6c">{{ row.failCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleRun(row)">立即执行</el-button>
            <el-button link type="primary" @click="handleViewLog(row)">日志</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="handleToggle(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  getCollectTaskPage,
  runCollectTask,
  toggleCollectTaskStatus,
  deleteCollectTask,
} from '@/api/collect'
import { mockCollectTasks } from '@/mock/collect'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({
  name: undefined as string | undefined,
  platformType: undefined as string | undefined,
  method: undefined as string | undefined,
  frequency: undefined as string | undefined,
  status: undefined as string | undefined,
  pageNo: 1,
  pageSize: 20,
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCollectTaskPage(searchForm as any) as any
    tableData.value = res.list || res.data?.list || []
    pagination.total = res.total ?? res.data?.total ?? tableData.value.length
  } catch {
    tableData.value = [...mockCollectTasks]
    pagination.total = tableData.value.length
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, {
    name: undefined, platformType: undefined, method: undefined, frequency: undefined, status: undefined,
  })
  pagination.pageNo = 1
  loadData()
}

const handleAdd = () => router.push('/collect/task/0')
const handleEdit = (row: any) => router.push(`/collect/task/${row.id}`)

const handleRun = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认立即执行【${row.name}】？`, '提示', { type: 'info' })
    await runCollectTask(row.id)
    ElMessage.success('已触发执行')
  } catch {}
}
const handleViewLog = (row: any) => router.push({ path: '/collect/log', query: { taskId: row.id } })
const handleToggle = async (row: any) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(`确认${newStatus === 'ENABLED' ? '启用' : '停用'}【${row.name}】？`, '提示', { type: 'warning' })
    await toggleCollectTaskStatus(row.id, newStatus)
    ElMessage.success('操作成功')
    loadData()
  } catch {}
}
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除【${row.name}】？`, '危险操作', { type: 'error' })
    await deleteCollectTask(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<style scoped>
.collect-task-page { padding: 20px; }
</style>
