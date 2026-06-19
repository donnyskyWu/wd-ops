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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
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
        <el-form-item v-if="formData.id" label="身份证号">
          <el-input :model-value="editingIdCardMasked" disabled placeholder="已登记证件号" />
        </el-form-item>
        <el-form-item v-else label="身份证号" prop="idCard">
          <el-input v-model="formData.idCard" placeholder="18位身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item v-if="formData.id" label="新身份证号">
          <el-input v-model="formData.idCard" placeholder="不修改请留空" maxlength="18" />
        </el-form-item>
        <el-form-item v-if="formData.id" label="手机号">
          <el-input :model-value="editingPhoneMasked" disabled placeholder="已登记手机号" />
        </el-form-item>
        <el-form-item v-else label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="formData.id" label="新手机号">
          <el-input v-model="formData.phone" placeholder="不修改请留空" maxlength="11" />
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

        <el-divider content-position="left">证件图片</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="身份证正面">
              <ImageUploadField
                :key="`front-${formData.id ?? 'new'}-${formData.idCardFrontKey || 'empty'}`"
                v-model="formData.idCardFrontKey"
                v-model:preview-url="formData.idCardFrontUrl"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证反面">
              <ImageUploadField
                :key="`back-${formData.id ?? 'new'}-${formData.idCardBackKey || 'empty'}`"
                v-model="formData.idCardBackKey"
                v-model:preview-url="formData.idCardBackUrl"
              />
            </el-form-item>
          </el-col>
        </el-row>
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
import ImageUploadField from '@/components/ImageUploadField.vue'
import { exportToExcel } from '@/utils'
import {
  createRealname,
  deleteRealname,
  getRealname,
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
const editingIdCardMasked = ref('')
const editingPhoneMasked = ref('')
const loadedImageKeys = ref({ front: '', back: '' })

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
  idCardFrontKey: '',
  idCardFrontUrl: '',
  idCardBackKey: '',
  idCardBackUrl: '',
})

const formRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idCard: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (formData.id) { cb(); return }
      if (!val || !/^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(val)) {
        cb(new Error('请输入正确的18位身份证号'))
      } else cb()
    },
    trigger: 'blur',
  }],
  phone: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (formData.id) { cb(); return }
      if (!val || !/^1[3-9]\d{9}$/.test(val)) cb(new Error('请输入正确的11位手机号'))
      else cb()
    },
    trigger: 'blur',
  }],
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
    idCardFrontKey: '',
    idCardFrontUrl: '',
    idCardBackKey: '',
    idCardBackUrl: '',
  })
  editingIdCardMasked.value = ''
  editingPhoneMasked.value = ''
  loadedImageKeys.value = { front: '', back: '' }
}

const handleAdd = () => {
  dialogTitle.value = '新增实名人'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row: RealnameVO) => {
  dialogTitle.value = '编辑实名人'
  try {
    const detail = await getRealname(row.id)
    editingIdCardMasked.value = detail.idCardMasked || '--'
    editingPhoneMasked.value = detail.phoneMasked || '--'
    loadedImageKeys.value = {
      front: detail.idCardFrontKey || '',
      back: detail.idCardBackKey || '',
    }
    Object.assign(formData, {
      id: detail.id,
      companyId: detail.companyId,
      realName: detail.realName,
      idType: detail.idType || 'ID_CARD',
      idCard: '',
      phone: '',
      wechat: detail.wechat || '',
      gender: detail.gender,
      status: detail.status || 'ENABLED',
      idCardFrontKey: detail.idCardFrontKey || '',
      idCardFrontUrl: detail.idCardFrontUrl || '',
      idCardBackKey: detail.idCardBackKey || '',
      idCardBackUrl: detail.idCardBackUrl || '',
    })
    dialogVisible.value = true
  } catch {
    /* 错误已由拦截器提示 */
  }
}

const buildImageKeyPayload = () => {
  const payload: Record<string, string | undefined> = {}
  if (formData.idCardFrontKey) {
    payload.idCardFrontKey = formData.idCardFrontKey
  } else if (loadedImageKeys.value.front) {
    payload.idCardFrontKey = ''
  }
  if (formData.idCardBackKey) {
    payload.idCardBackKey = formData.idCardBackKey
  } else if (loadedImageKeys.value.back) {
    payload.idCardBackKey = ''
  }
  return payload
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (formData.id && formData.idCard
        && !/^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(formData.idCard)) {
      ElMessage.warning('新身份证号格式不正确')
      return
    }
    if (formData.id && formData.phone && !/^1[3-9]\d{9}$/.test(formData.phone)) {
      ElMessage.warning('新手机号格式不正确')
      return
    }
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
          ...buildImageKeyPayload(),
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
          idCardFrontKey: formData.idCardFrontKey || undefined,
          idCardBackKey: formData.idCardBackKey || undefined,
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
