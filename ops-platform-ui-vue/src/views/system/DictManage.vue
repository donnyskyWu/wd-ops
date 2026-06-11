<template>
  <div class="dict-manage">
    <ContentWrap title="字典配置" subtitle="数据字典管理">
      <el-alert
        title="管理系统中的枚举值和字典数据（用于下拉选项、状态标识等）"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
        <el-form-item label="字典名称">
          <el-input v-model="searchForm.dictName" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="searchForm.dictType" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </TableSearch>

      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增字典
        </el-button>
      </div>

      <el-table :data="tableList" border stripe v-loading="loading">
        <el-table-column prop="dictName" label="字典名称" min-width="180" />
        <el-table-column prop="dictType" label="字典类型" min-width="180" />
        <el-table-column prop="dictLabel" label="字典标签" min-width="150" />
        <el-table-column prop="dictValue" label="字典值" min-width="150" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @update:current-page="handlePageChange"
        @update:page-size="handleSizeChange"
        @change="loadData"
      />
    </ContentWrap>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="formData.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="formData.dictType" placeholder="dict_xxx 格式" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="formData.dictLabel" placeholder="请输入显示标签" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="formData.dictValue" placeholder="全大写+下划线" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="formData.status"
            active-value="ENABLED"
            inactive-value="DISABLED"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import {
  fetchDictAdminList,
  createDictType,
  updateDictType,
  deleteDictData,
  type DictAdminRow,
} from '@/api/system-dict'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const tableList = ref<DictAdminRow[]>([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({ dictName: '', dictType: '', status: '' })
const formData = reactive<Partial<DictAdminRow>>({
  dictName: '',
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  status: 'ENABLED',
  remark: '',
})

const rules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' },
    { pattern: /^dict_[a-z0-9_]+$/, message: '须 dict_ 前缀+小写下划线', trigger: 'blur' },
  ],
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [
    { required: true, message: '请输入字典值', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '须全大写+下划线', trigger: 'blur' },
  ],
}

const dialogTitle = computed(() => (formData.id ? '编辑字典项' : '新增字典'))

async function loadData() {
  loading.value = true
  try {
    const res = await fetchDictAdminList({
      dictName: searchForm.dictName || undefined,
      dictType: searchForm.dictType || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    tableList.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNo.value = 1
  loadData()
}

function handleReset() {
  searchForm.dictName = ''
  searchForm.dictType = ''
  searchForm.status = ''
  pageNo.value = 1
  loadData()
}

function handleCreate() {
  Object.assign(formData, {
    id: undefined,
    typeId: undefined,
    dictName: '',
    dictType: '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
    status: 'ENABLED',
    remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: DictAdminRow) {
  Object.assign(formData, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const item = {
        id: formData.id,
        dictLabel: formData.dictLabel!,
        dictValue: formData.dictValue!,
        sort: formData.sort ?? 0,
        status: formData.status,
        remark: formData.remark,
      }
      if (formData.id && formData.typeId) {
        await updateDictType({
          id: formData.typeId,
          dictName: formData.dictName!,
          items: [item],
        })
        ElMessage.success('编辑成功')
      } else {
        await createDictType({
          dictType: formData.dictType!,
          dictName: formData.dictName!,
          items: [item],
        })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

function handleDelete(row: DictAdminRow) {
  ElMessageBox.confirm(`确定要删除字典项「${row.dictLabel}」吗？停用项才可删除。`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteDictData(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

function handlePageChange(page: number) {
  pageNo.value = page
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
}

onMounted(loadData)
</script>

<style scoped>
.dict-manage {
  padding: 20px;
}
</style>
