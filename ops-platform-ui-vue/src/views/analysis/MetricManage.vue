<template>
  <div class="metric-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="指标名称/编码">
        <el-input v-model="searchForm.keyword" placeholder="搜索指标" clearable />
      </el-form-item>
      <el-form-item label="指标类型">
        <el-select v-model="searchForm.metricType" placeholder="请选择" clearable style="width: 140px">
          <el-option label="基础指标" value="BASIC" />
          <el-option label="复合指标" value="COMPOSITE" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增指标</el-button>
      <el-button type="success" @click="handleExport"><el-icon><Download /></el-icon>导出</el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="metricList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="metricName" label="指标名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="metricCode" label="指标编码" width="180" show-overflow-tooltip />
      <el-table-column prop="metricType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeColor(row.metricType)" size="small">{{ getTypeName(row.metricType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="searchForm.pageNum"
      :page-size="searchForm.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @update:current-page="(v) => { searchForm.pageNum = v; loadList() }"
      @update:page-size="(v) => { searchForm.pageSize = v; searchForm.pageNum = 1; loadList() }"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @closed="handleDialogClosed">
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="100px">
        <el-form-item label="指标名称" prop="metricName">
          <el-input v-model="formData.metricName" placeholder="1-50字，全局唯一" maxlength="50" />
        </el-form-item>
        <el-form-item label="指标编码" prop="metricCode">
          <el-input v-model="formData.metricCode" placeholder="英文+下划线，全局唯一" />
        </el-form-item>
        <el-form-item label="指标类型" prop="metricType">
          <el-radio-group v-model="formData.metricType">
            <el-radio label="BASIC">基础</el-radio>
            <el-radio label="COMPOSITE">复合</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="formData.unit" placeholder="如：人/元/%/次/分" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="指标说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { exportToExcel } from '@/utils'
import TableSearch from '@/components/TableSearch.vue'
import { getMetricList, createMetric, updateMetric, deleteMetric } from '@/api/metric'

interface MetricVO {
  id: number
  metricName: string
  metricCode: string
  metricType: string
  unit: string
  category: string
  status: number
  description: string
}

const loading = ref(false)
const saving = ref(false)
const metricList = ref<MetricVO[]>([])
const total = ref(0)

const searchForm = reactive({
  pageNum: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  metricType: undefined as string | undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增指标')
const isEdit = ref(false)
const formRef = ref()
const formData = reactive({
  id: undefined as number | undefined,
  metricName: '',
  metricCode: '',
  metricType: 'BASIC',
  unit: '',
  description: '',
})

const formRules = {
  metricName: [
    { required: true, message: '请输入指标名称', trigger: 'blur' },
    { max: 50, message: '指标名称不超过 50 字', trigger: 'blur' },
  ],
  metricCode: [
    { required: true, message: '请输入指标编码', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]*$/, message: '编码需以字母开头，仅含字母数字下划线', trigger: 'blur' },
  ],
  metricType: [{ required: true, message: '请选择指标类型', trigger: 'change' }],
}

const getTypeName = (type: string) => {
  const map: Record<string, string> = { BASIC: '基础', COMPOSITE: '复合' }
  return map[type] || type || '-'
}

const getTypeColor = (type: string) => {
  const map: Record<string, string> = { BASIC: '', COMPOSITE: 'warning' }
  return map[type] || ''
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getMetricList({
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
      metricType: searchForm.metricType,
    })
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? []
    let filtered = list
    if (searchForm.keyword) {
      const kw = searchForm.keyword.toLowerCase()
      filtered = list.filter((m: MetricVO) =>
        (m.metricName || '').toLowerCase().includes(kw) ||
        (m.metricCode || '').toLowerCase().includes(kw),
      )
    }
    metricList.value = filtered
    total.value = data?.total ?? filtered.length
  } catch (e) {
    console.error('loadList failed', e)
    metricList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNum = 1
  loadList()
}

const handleReset = () => {
  searchForm.keyword = undefined
  searchForm.metricType = undefined
  searchForm.pageNum = 1
  loadList()
}

const handleExport = () => {
  const rows = metricList.value.map((row) => ({
    id: row.id,
    metricName: row.metricName,
    metricCode: row.metricCode,
    metricType: getTypeName(row.metricType),
    unit: row.unit,
    status: row.status === 1 ? '启用' : '停用',
  }))
  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'metricName', label: '指标名称' },
    { key: 'metricCode', label: '指标编码' },
    { key: 'metricType', label: '类型' },
    { key: 'unit', label: '单位' },
    { key: 'status', label: '状态' },
  ]
  exportToExcel(rows, columns, '指标列表')
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增指标'
  Object.assign(formData, { id: undefined, metricName: '', metricCode: '', metricType: 'BASIC', unit: '', description: '' })
  dialogVisible.value = true
}

const handleEdit = (row: MetricVO) => {
  isEdit.value = true
  dialogTitle.value = '编辑指标'
  Object.assign(formData, {
    id: row.id,
    metricName: row.metricName,
    metricCode: row.metricCode,
    metricType: row.metricType || row.category || 'BASIC',
    unit: row.unit || '',
    description: row.description || '',
  })
  dialogVisible.value = true
}

const handleDialogClosed = () => {
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      metricName: formData.metricName,
      metricCode: formData.metricCode,
      metricType: formData.metricType,
      unit: formData.unit || undefined,
      description: formData.description || undefined,
    }
    if (isEdit.value && formData.id) {
      await updateMetric({ id: formData.id, ...payload })
    } else {
      await createMetric(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row: MetricVO) => {
  try {
    await ElMessageBox.confirm(`确认删除指标"${row.metricName}"？`, '提示', { type: 'warning' })
    await deleteMetric(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.metric-page { padding: 20px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
