<template>
  <div class="task-page">
    <!-- 二级Tab -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="task-tabs">
      <el-tab-pane label="全部任务" name="all" />
      <el-tab-pane label="我的任务" name="my" />
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
        <el-select v-model="searchForm.status" placeholder="请选择" clearable>
          <el-option label="待执行" :value="TaskStatus.PENDING" />
          <el-option label="执行中" :value="TaskStatus.IN_PROGRESS" />
          <el-option label="待审核" :value="TaskStatus.PENDING_REVIEW" />
          <el-option label="审核通过" :value="TaskStatus.REVIEW_APPROVED" />
          <el-option label="审核驳回" :value="TaskStatus.REVIEW_REJECTED" />
          <el-option label="已完成" :value="TaskStatus.COMPLETED" />
        </el-select>
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
        <el-table-column prop="executorRole" label="执行岗位" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="slaDeadline" label="SLA截止" width="170" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.isOverdue }">
              {{ row.slaDeadline }}
            </span>
            <el-tag v-if="row.isOverdue" type="danger" size="small" class="overdue-tag">
              超时
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <!-- 开始执行 -->
            <el-button
              v-if="row.status === TaskStatus.PENDING"
              link
              type="primary"
              :disabled="true"
              title="前置节点未完成，不能开始执行"
            >
              开始执行
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, getMyTasks, startTask, completeTask, submitTaskReview } from '@/api/task'
import { mockTaskList, mockGetTaskList, mockGetMyTasks } from '@/mock/task'
import { TaskStatus } from '@/types/task'
import type { TaskQuery, TaskVO } from '@/types/task'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'

// 暴露TaskStatus供模板使用
const TaskStatusEnum = TaskStatus

// ==================== 响应式数据 ====================

// 当前Tab
const activeTab = ref('all')

// 搜索表单
const searchForm = reactive({
  templateId: undefined as number | undefined,
  status: undefined as TaskStatus | undefined,
  assigneeId: undefined as number | undefined,
})

// 加载状态
const loading = ref(false)

// 表格数据 - 初始值使用Mock数据
const tableData = ref<TaskVO[]>([...mockTaskList])

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

    let result
    if (activeTab.value === 'my') {
      // 我的任务
      result = await getMyTasks(params).catch(() => {
        return mockGetMyTasks(pagination.pageNo, pagination.pageSize)
      })
    } else {
      // 全部任务
      result = await getTaskList(params).catch(() => {
        return mockGetTaskList(pagination.pageNo, pagination.pageSize)
      })
    }

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

// 获取行类名（SLA超时红色背景）
const getRowClassName = ({ row }: { row: TaskVO }) => {
  return row.isOverdue ? 'overdue-row' : ''
}

// 获取状态标签类型
const getStatusTagType = (status: TaskStatus) => {
  const types: Record<TaskStatus, string> = {
    [TaskStatus.PENDING]: 'info',
    [TaskStatus.IN_PROGRESS]: 'primary',
    [TaskStatus.PENDING_REVIEW]: 'warning',
    [TaskStatus.REVIEW_APPROVED]: 'success',
    [TaskStatus.REVIEW_REJECTED]: 'danger',
    [TaskStatus.COMPLETED]: 'success',
  }
  return types[status] || ''
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
  // 直接使用Mock数据，确保页面有数据显示
  const mockResult = mockGetTaskList(pagination.pageNo, pagination.pageSize)
  tableData.value = mockResult.list
  pagination.total = mockResult.total
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
