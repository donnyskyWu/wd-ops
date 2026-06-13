<template>
  <div class="simcard-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="ICCID">
        <el-input v-model="searchForm.keyword" placeholder="搜索ICCID" clearable />
      </el-form-item>
      <el-form-item label="运营商">
        <DictSelect v-model="searchForm.operator" dict-type="dict_sim_operator" placeholder="全部" clearable />
      </el-form-item>
      <template #extra>
        <el-button type="success" :loading="exportLoading" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增手机卡
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="iccidMasked" label="ICCID" width="160">
        <template #default="{ row }">
          <span class="masked-text">{{ row.iccidMasked || '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="phoneNumberMasked" label="手机号" width="130">
        <template #default="{ row }">
          <span class="masked-text">{{ row.phoneNumberMasked || '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="operator" label="运营商" width="100">
        <template #default="{ row }">{{ OPERATOR_MAP[row.operator] || row.operator }}</template>
      </el-table-column>
      <el-table-column prop="packageName" label="套餐" width="120" />
      <el-table-column prop="totalLinkedAccounts" label="关联账号" width="100" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleLinked(row)">
            {{ row.totalLinkedAccounts ?? 0 }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="assignedUserName" label="归属人" width="100" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ row.status === 'ENABLED' ? '在用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="warning" @click="handleLinked(row)">跨平台查询</el-button>
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
        <el-form-item label="实名人" prop="realnameId">
          <RealNameSelect v-model="formData.realnameId" />
        </el-form-item>
        <el-form-item label="手机号" prop="phoneId">
          <PhoneSelect
            v-model="formData.phoneId"
            :real-name-id="formData.realnameId"
          />
        </el-form-item>
        <el-form-item label="ICCID" prop="iccid">
          <el-input v-model="formData.iccid" placeholder="ICCID编码" maxlength="30" />
        </el-form-item>
        <el-form-item label="运营商" prop="operator">
          <DictSelect v-model="formData.operator" dict-type="dict_sim_operator" placeholder="请选择" />
        </el-form-item>
        <el-form-item label="主卡">
          <DictSelect v-model="formData.isPrimary" dict-type="dict_yes_no" />
        </el-form-item>
        <el-form-item label="套餐">
          <el-input v-model="formData.packageName" placeholder="套餐名称" />
        </el-form-item>
        <el-form-item label="归属人" prop="assignedUserId">
          <UserSelect v-model="formData.assignedUserId" placeholder="请选择归属人" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="formData.status" dict-type="dict_sim_status" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import RealNameSelect from '@/components/selectors/RealNameSelect.vue'
import PhoneSelect from '@/components/selectors/PhoneSelect.vue'
import { exportToExcel } from '@/utils'
import {
  createSimCard,
  deleteSimCard,
  getSimCardPage,
  updateSimCard,
  type SimCardVO,
} from '@/api/simcard'

const OPERATOR_MAP: Record<string, string> = {
  MOBILE: '中国移动',
  UNICOM: '中国联通',
  TELECOM: '中国电信',
}

const loading = ref(false)
const exportLoading = ref(false)
const list = ref<SimCardVO[]>([])
const total = ref(0)
const router = useRouter()

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  operator: undefined as string | undefined,
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getSimCardPage({
      iccid: searchForm.keyword,
      operator: searchForm.operator,
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    list.value = res.list || []
    total.value = res.total ?? 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadList()
const handleReset = () => {
  searchForm.keyword = undefined
  searchForm.operator = undefined
  searchForm.pageNo = 1
  loadList()
}

const buildListParams = (pageNo: number, pageSize: number) => ({
  iccid: searchForm.keyword,
  operator: searchForm.operator,
  pageNo,
  pageSize,
})

const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first = await getSimCardPage(buildListParams(1, exportPageSize))
  let rows = first.list || []
  const totalCount = first.total ?? 0
  if (totalCount > exportPageSize) {
    const totalPages = Math.ceil(totalCount / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res = await getSimCardPage(buildListParams(page, exportPageSize))
      rows = rows.concat(res.list || [])
    }
  }
  return rows
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows()
    const exportData = rows.map((row) => ({
      iccidMasked: row.iccidMasked || '',
      phoneNumberMasked: row.phoneNumberMasked || '',
      operator: OPERATOR_MAP[row.operator] || row.operator || '',
      packageName: row.packageName || '',
      totalLinkedAccounts: row.totalLinkedAccounts ?? 0,
      assignedUserName: row.assignedUserName || '',
      status: row.status === 'ENABLED' ? '在用' : '停用',
    }))
    const columns = [
      { key: 'iccidMasked', label: 'ICCID' },
      { key: 'phoneNumberMasked', label: '手机号' },
      { key: 'operator', label: '运营商' },
      { key: 'packageName', label: '套餐' },
      { key: 'totalLinkedAccounts', label: '关联账号' },
      { key: 'assignedUserName', label: '归属人' },
      { key: 'status', label: '状态' },
    ]
    exportToExcel(exportData, columns, '手机卡管理')
  } catch {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

const handleLinked = (row: SimCardVO) => {
  router.push(`/simcard/${row.id}/linked`)
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增手机卡')
const formRef = ref()

const formData = reactive({
  id: undefined as number | undefined,
  realnameId: undefined as number | undefined,
  phoneId: undefined as number | undefined,
  iccid: '',
  operator: '',
  isPrimary: 'YES',
  packageName: '',
  assignedUserId: undefined as number | undefined,
  status: 'ENABLED',
})

const formRules = {
  realnameId: [{ required: true, message: '请选择实名人', trigger: 'change' }],
  phoneId: [{ required: true, message: '请选择手机号', trigger: 'change' }],
  iccid: [{ required: true, message: '请输入 ICCID', trigger: 'blur' }],
  operator: [{ required: true, message: '请选择运营商', trigger: 'change' }],
  assignedUserId: [{ required: true, message: '请选择归属人', trigger: 'change' }],
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    realnameId: undefined,
    phoneId: undefined,
    iccid: '',
    operator: '',
    isPrimary: 'YES',
    packageName: '',
    assignedUserId: undefined as number | undefined,
    status: 'ENABLED',
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增手机卡'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: SimCardVO) => {
  dialogTitle.value = '编辑手机卡'
  Object.assign(formData, {
    id: row.id,
    realnameId: undefined,
    phoneId: row.phoneId,
    iccid: '',
    operator: row.operator || '',
    isPrimary: row.isPrimary || 'YES',
    packageName: row.packageName || '',
    assignedUserId: row.assignedUserId,
    status: row.status || 'ENABLED',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const payload = {
        operator: formData.operator,
        isPrimary: formData.isPrimary,
        packageName: formData.packageName || undefined,
        assignedUserId: formData.assignedUserId,
        status: formData.status,
        phoneId: formData.phoneId,
      }
      if (formData.id) {
        const updatePayload: Record<string, unknown> = { id: formData.id, ...payload }
        if (formData.iccid) updatePayload.iccid = formData.iccid
        await updateSimCard(updatePayload as any)
      } else {
        await createSimCard({
          ...payload,
          iccid: formData.iccid,
        })
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } catch {
      /* 拦截器已提示 */
    }
  })
}

const handleDelete = async (row: SimCardVO) => {
  try {
    await ElMessageBox.confirm('确认删除该手机卡？', '提示', { type: 'warning' })
    await deleteSimCard(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    /* 取消或错误 */
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.simcard-page { padding: 20px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.masked-text { font-family: monospace; }
</style>
