<template>
  <div class="realname-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="姓名">
        <el-input v-model="searchForm.keyword" placeholder="搜索姓名" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_realname_status" placeholder="全部" clearable />
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
        新增实名人
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="realnameList" v-loading="loading" stripe>
      <el-table-column prop="realName" label="真实姓名" width="100" />
      <el-table-column prop="idCardMasked" label="身份证号(脱敏)" width="200">
        <template #default="{ row }">
          <span class="masked-text">{{ row.idCardMasked || '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="phoneMasked" label="手机号" width="130">
        <template #default="{ row }">
          <span class="masked-text">{{ row.phoneMasked || '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="wechat" label="微信号" width="130" />
      <el-table-column prop="companyName" label="所属公司" min-width="150" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_realname_status" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
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
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="所属公司">
          <CompanySelect v-model="formData.companyId" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="证件类型" prop="idType">
          <DictSelect v-model="formData.idType" dict-type="dict_id_type" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="formData.idCard" placeholder="18位身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="性别">
          <DictSelect v-model="formData.gender" dict-type="dict_gender" clearable />
        </el-form-item>
        <el-form-item label="微信号">
          <el-input v-model="formData.wechat" placeholder="请输入微信号" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="formData.status" dict-type="dict_realname_status" />
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
import DictLabel from '@/components/DictLabel.vue'
import CompanySelect from '@/components/selectors/CompanySelect.vue'
import { exportToExcel } from '@/utils'
import {
  createRealname,
  deleteRealname,
  getRealnamePage,
  updateRealname,
  type RealnameVO,
} from '@/api/realname'

const loading = ref(false)
const exportLoading = ref(false)
const realnameList = ref<RealnameVO[]>([])
const total = ref(0)
const router = useRouter()

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  status: undefined as string | undefined,
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getRealnamePage({
      realName: searchForm.keyword,
      status: searchForm.status,
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    realnameList.value = res.list || []
    total.value = res.total ?? 0
  } catch {
    realnameList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadList()
}

const handleReset = () => {
  searchForm.keyword = undefined
  searchForm.status = undefined
  searchForm.pageNo = 1
  loadList()
}

const buildListParams = (pageNo: number, pageSize: number) => ({
  realName: searchForm.keyword,
  status: searchForm.status,
  pageNo,
  pageSize,
})

const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first = await getRealnamePage(buildListParams(1, exportPageSize))
  let rows = first.list || []
  const totalCount = first.total ?? 0
  if (totalCount > exportPageSize) {
    const totalPages = Math.ceil(totalCount / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res = await getRealnamePage(buildListParams(page, exportPageSize))
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
      realName: row.realName,
      idCardMasked: row.idCardMasked || '',
      phoneMasked: row.phoneMasked || '',
      wechat: row.wechat || '',
      companyName: row.companyName || '',
      status: row.status === 'ENABLED' ? '启用' : '停用',
    }))
    const columns = [
      { key: 'realName', label: '真实姓名' },
      { key: 'idCardMasked', label: '身份证号(脱敏)' },
      { key: 'phoneMasked', label: '手机号' },
      { key: 'wechat', label: '微信号' },
      { key: 'companyName', label: '所属公司' },
      { key: 'status', label: '状态' },
    ]
    exportToExcel(exportData, columns, '实名人管理')
  } catch {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增实名人')
const formRef = ref()

const formData = reactive({
  id: undefined as number | undefined,
  companyId: undefined as number | undefined,
  realName: '',
  idType: 'ID_CARD',
  idCard: '',
  phone: '',
  wechat: '',
  gender: undefined as string | undefined,
  status: 'ENABLED',
})

const formRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    companyId: undefined,
    realName: '',
    idType: 'ID_CARD',
    idCard: '',
    phone: '',
    wechat: '',
    gender: undefined,
    status: 'ENABLED',
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增实名人'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: RealnameVO) => {
  dialogTitle.value = '编辑实名人'
  Object.assign(formData, {
    id: row.id,
    companyId: row.companyId,
    realName: row.realName,
    idType: row.idType || 'ID_CARD',
    idCard: '',
    phone: '',
    wechat: row.wechat || '',
    gender: row.gender,
    status: row.status || 'ENABLED',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      if (formData.id) {
        const payload: Record<string, unknown> = {
          id: formData.id,
          companyId: formData.companyId,
          realName: formData.realName,
          idType: formData.idType,
          wechat: formData.wechat,
          gender: formData.gender,
          status: formData.status,
        }
        if (formData.idCard) payload.idCard = formData.idCard
        if (formData.phone) payload.phone = formData.phone
        await updateRealname(payload as any)
      } else {
        await createRealname({
          companyId: formData.companyId,
          realName: formData.realName,
          idType: formData.idType,
          idCard: formData.idCard,
          phone: formData.phone,
          wechat: formData.wechat,
          gender: formData.gender,
          status: formData.status,
        })
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } catch {
      /* 错误已由 request 拦截器提示 */
    }
  })
}

const handleView = (row: RealnameVO) => {
  router.push(`/realname/${row.id}`)
}

const handleDelete = async (row: RealnameVO) => {
  try {
    await ElMessageBox.confirm('确认删除该实名人？', '提示', { type: 'warning' })
    await deleteRealname(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    /* 取消或业务错误 */
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.realname-page {
  padding: 20px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.total-info {
  color: #909399;
  font-size: 14px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.masked-text {
  font-family: monospace;
  color: #606266;
}
</style>
