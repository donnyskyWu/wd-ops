<template>
  <div class="plan-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="计划名称"><el-input v-model="searchForm.keyword" placeholder="搜索计划" clearable /></el-form-item>
    </TableSearch>
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增计划</el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>
    <el-table :data="planList" v-loading="loading" stripe>
      <el-table-column prop="planName" label="计划名称" min-width="150" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === '进行中' ? 'success' : 'info'">{{ row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="progress" label="进度" width="150"><template #default="{ row }"><el-progress :percentage="row.progress" /></template></el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      :current-page="searchForm.pageNo"
      :page-size="searchForm.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @update:current-page="(val) => searchForm.pageNo = val"
      @update:page-size="(val) => { searchForm.pageSize = val; handleSearch() }"
      @current-change="handleSearch"
      @size-change="handleSearch"
    />
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="100px">
        <el-form-item label="计划名称" prop="planName"><el-input v-model="formData.planName" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="日期范围" prop="dateRange"><el-date-picker v-model="formData.dateRange" type="daterange" range-separator="~" style="width: 100%" /></el-form-item>
        <el-form-item label="计划描述"><el-input v-model="formData.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'

// ==================== 类型定义 ====================
interface PlanItem {
  id?: number
  planName: string
  startDate: string
  endDate: string
  status: string
  progress: number
}

// ==================== 响应式数据 ====================
const loading = ref(false)
const planList = ref<PlanItem[]>([
  { id: 1, planName: '5月内容发布计划', startDate: '2026-05-01', endDate: '2026-05-31', status: '进行中', progress: 65 },
  { id: 2, planName: 'Q2运营推广计划', startDate: '2026-04-01', endDate: '2026-06-30', status: '进行中', progress: 40 },
])
const total = ref(2)
const searchForm = reactive({ pageNo: 1, pageSize: 20, keyword: undefined as string | undefined })
const dialogVisible = ref(false)
const dialogTitle = ref('新增计划')
const formRef = ref<any>()
const formData = reactive({ id: undefined, planName: '', dateRange: [], description: '' })

const formRules = {
  planName: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { max: 100, message: '计划名称不超过 100 字', trigger: 'blur' },
  ],
  dateRange: [{ required: true, message: '请选择日期范围', trigger: 'change' }],
}

const loadList = () => { loading.value = false }
const handleSearch = () => { searchForm.pageNo = 1; loadList() }
const handleReset = () => { searchForm.keyword = undefined; handleSearch() }
const handleAdd = () => { dialogTitle.value = '新增计划'; dialogVisible.value = true }
const handleEdit = (row: PlanItem) => { dialogTitle.value = '编辑计划'; Object.assign(formData, { ...row }); dialogVisible.value = true }
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadList()
}
const handleDelete = async () => { try { await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }); ElMessage.success('删除成功'); loadList() } catch {} }
onMounted(() => loadList())
</script>

<style scoped>
.plan-page { padding: 20px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
