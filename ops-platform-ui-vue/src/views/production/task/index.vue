<template>
  <div class="task-page">
    <!-- 二级Tab -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="task-tabs">
      <el-tab-pane label="我的任务" name="my" />
      <el-tab-pane label="全部任务" name="all" />
    </el-tabs>

    <!-- 筛选区 -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="模板">
        <el-select v-model="searchForm.templateId" placeholder="请选择" clearable>
          <el-option label="标准内容生产运营流程" :value="1" />
          <el-option label="短视频流程" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_sop_node_status" placeholder="请选择" clearable />
      </el-form-item>
      <el-form-item label="执行人">
        <el-select v-model="searchForm.assigneeId" placeholder="请选择" clearable filterable>
          <el-option label="张三" :value="101" />
          <el-option label="李四" :value="102" />
          <el-option label="王五" :value="103" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <!-- 内容区 -->
    <ContentWrap>
      <!-- 任务列表表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        :row-class-name="getRowClassName"
        :empty-text="'暂无任务数据'"
      >
        <el-table-column prop="planName" label="任务名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="nodeName" label="节点名称" width="120" />
        <el-table-column prop="assigneeName" label="执行人" width="100" />
        <el-table-column prop="executorRole" label="执行岗位" width="120" align="center">
          <template #default="{ row }">
            <DictLabel
              dict-type="dict_position"
              :value="row.executorRole"
              :fallback="row.executorRoleText || row.executorRole || '—'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_sop_node_status" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.scheduledStart) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.scheduledEnd) }}</template>
        </el-table-column>
        <el-table-column prop="slaDeadline" label="SLA截止" width="170" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row) }">
              {{ formatSlaDeadline(row.slaDeadline) }}
            </span>
            <el-tag v-if="isOverdue(row)" type="danger" size="small" class="overdue-tag">
              超时
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <!-- 我的任务：执行页入口（AC-M2-002-5） -->
            <el-button
              v-if="activeTab === 'my' && canOpenExecute(row)"
              link
              type="primary"
              @click="handleExecute(row)"
            >
              执行
            </el-button>

            <!-- 完成 -->
            <el-button
              v-if="row.status === TaskStatus.IN_PROGRESS"
              link
              type="success"
              @click="handleComplete(row)"
            >
              完成
            </el-button>

            <!-- 提交审核 -->
            <el-button
              v-if="row.status === TaskStatus.IN_PROGRESS"
              link
              type="warning"
              @click="handleSubmitReview(row)"
            >
              提交审核
            </el-button>

            <!-- 审核 -->
            <el-button
              v-if="row.status === TaskStatus.PENDING_REVIEW"
              link
              type="primary"
              @click="handleReview(row)"
            >
              审核
            </el-button>

            <!-- 重新执行（审核驳回） -->
            <el-button
              v-if="row.status === TaskStatus.REVIEW_REJECTED"
              link
              type="danger"
              @click="handleRestart(row)"
            >
              重新执行
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => pagination.pageNo = val"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <!-- 提交审核对话框 -->
    <el-dialog v-model="reviewDialogVisible" title="提交审核" width="500px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            placeholder="可填写说明（选填）"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReviewSubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, getMyTasks, startTask, completeTask, submitTaskReview } from '@/api/task'
import { TaskStatus } from '@/types/task'
import type { TaskQuery, TaskVO } from '@/types/task'
import { formatDateTime } from '@/utils/index'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'

const router = useRouter()

// ==================== 响应式数据 ====================

// 当前Tab
const activeTab = ref('my')

// 搜索表单
const searchForm = reactive({
  templateId: undefined as number | undefined,
  status: undefined as TaskStatus | undefined,
  assigneeId: undefined as number | undefined,
})

// 加载状态
const loading = ref(false)

const tableData = ref<TaskVO[]>([])

// 分页参数 - 初始值使用Mock数据
const pagination = reactive({
  pageNo: 1,
  pageSize: 20,
  total: 0,
})

// 审核对话框
const reviewDialogVisible = ref(false)
const reviewForm = reactive({
  taskId: 0,
  comment: '',
})

// ==================== 方法 ====================

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params: TaskQuery = {
      templateId: searchForm.templateId,
      status: searchForm.status,
      assigneeId: searchForm.assigneeId,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    }

    const result = activeTab.value === 'my'
      ? await getMyTasks(params)
      : await getTaskList(params)

    tableData.value = result.list
    pagination.total = result.total
  } catch (error) {
    console.error('加载任务数据失败:', error)
    ElMessage.error('数据加载失败，请重试')
  } finally {
    loading.value = false
  }
}

// Tab切换
const handleTabChange = () => {
  pagination.pageNo = 1
  loadData()
}

// 搜索
const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.templateId = undefined
  searchForm.status = undefined
  searchForm.assigneeId = undefined
  pagination.pageNo = 1
  loadData()
}

const formatSlaDeadline = (value?: string) => {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

const isOverdue = (row: TaskVO) => {
  if (!row.slaDeadline) return false
  const deadline = new Date(row.slaDeadline)
  return !Number.isNaN(deadline.getTime()) && deadline.getTime() < Date.now()
    && row.status !== TaskStatus.DONE
    && row.status !== TaskStatus.COMPLETED
}

const canOpenExecute = (row: TaskVO) =>
  row.status === TaskStatus.PENDING
  || row.status === TaskStatus.IN_PROGRESS
  || row.status === TaskStatus.REVIEW_REJECTED

// 获取行类名（SLA超时红色背景）
const getRowClassName = ({ row }: { row: TaskVO }) => {
  return isOverdue(row) ? 'overdue-row' : ''
}

const handleExecute = (row: TaskVO) => {
  router.push(`/task/${row.id}/execute`)
}

// 完成任务
const handleComplete = async (row: TaskVO) => {
  try {
    await ElMessageBox.confirm('确定要完成该任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await completeTask(row.id).catch(() => {
      return Promise.resolve()
    })

    ElMessage.success('任务已完成')
    loadData()
  } catch (error) {
    // 用户取消
  }
}

// 提交审核
const handleSubmitReview = (row: TaskVO) => {
  reviewForm.taskId = row.id
  reviewForm.comment = ''
  reviewDialogVisible.value = true
}

// 提交审核确认
const handleReviewSubmit = async () => {
  try {
    await submitTaskReview(reviewForm.taskId).catch(() => {
      return Promise.resolve()
    })

    ElMessage.success('已提交审核')
    reviewDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  }
}

// 审核
const handleReview = (row: TaskVO) => {
  ElMessage.info('跳转到审核页面（待实现）')
  // TODO: 跳转到审核页面
}

// 重新执行（审核驳回）
const handleRestart = async (row: TaskVO) => {
  try {
    await ElMessageBox.confirm('确定要重新执行该任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await startTask(row.id).catch(() => {
      return Promise.resolve()
    })

    ElMessage.success('任务已重新开始')
    loadData()
  } catch (error) {
    // 用户取消
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.task-page {
  .task-tabs {
    margin-bottom: 16px;
  }

  :deep(.overdue-row) {
    background-color: #fef0f0 !important;
  }

  .text-danger {
    color: #F56C6C;
    font-weight: bold;
  }

  .overdue-tag {
    margin-left: 4px;
  }
}
</style>
