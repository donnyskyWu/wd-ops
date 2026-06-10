<template>
  <div class="phone-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="手机号">
        <el-input v-model="searchForm.keyword" placeholder="搜索手机号/编码/机型" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_phone_status" placeholder="全部" clearable />
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增手机信息
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="phoneList" v-loading="loading" stripe>
      <el-table-column prop="phoneNumberMasked" label="手机号" width="130">
        <template #default="{ row }">
          <span class="masked-text">{{ row.phoneNumberMasked || '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="phoneCode" label="手机编码" width="120" />
      <el-table-column prop="phoneModel" label="机型" width="140" />
      <el-table-column prop="keeperName" label="保管人" width="100" />
      <el-table-column prop="wechatBound" label="绑定微信" width="120" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ row.status === 'ENABLED' ? '在用' : '停用' }}
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
        <el-form-item v-if="formData.id" label="手机号">
          <el-input :model-value="editingPhoneMasked" disabled placeholder="已登记号码" />
        </el-form-item>
        <el-form-item v-else label="手机号" prop="phoneNumber">
          <el-input
            v-model="formData.phoneNumber"
            placeholder="11位手机号"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="手机编码">
          <el-input v-model="formData.phoneCode" placeholder="内部编号，可选" />
        </el-form-item>
        <el-form-item label="机型">
          <el-input v-model="formData.phoneModel" placeholder="如 iPhone 15 Pro" maxlength="100" />
        </el-form-item>
        <el-form-item label="保管人" prop="keeperId">
          <UserSelect v-model="formData.keeperId" placeholder="请选择保管人" />
        </el-form-item>
        <el-form-item label="绑定微信">
          <el-input v-model="formData.wechatBound" placeholder="绑定微信号" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="formData.status" dict-type="dict_phone_status" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import {
  createPhone,
  deletePhone,
  getPhonePage,
  updatePhone,
  type PhoneVO,
} from '@/api/phone'

const loading = ref(false)
const phoneList = ref<PhoneVO[]>([])
const total = ref(0)

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  status: undefined as string | undefined,
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getPhonePage({
      phoneNumber: searchForm.keyword,
      status: searchForm.status,
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    phoneList.value = res.list || []
    total.value = res.total ?? 0
  } catch {
    phoneList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadList()

const handleReset = () => {
  searchForm.keyword = undefined
  searchForm.status = undefined
  searchForm.pageNo = 1
  loadList()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增手机信息')
const formRef = ref()
const editingPhoneMasked = ref('')

const formData = reactive({
  id: undefined as number | undefined,
  phoneNumber: '',
  phoneCode: '',
  phoneModel: '',
  keeperId: undefined as number | undefined,
  wechatBound: '',
  status: 'ENABLED',
})

const formRules = {
  phoneNumber: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (formData.id) { cb(); return }
      if (!val || !/^1[3-9]\d{9}$/.test(val)) cb(new Error('请输入正确的11位手机号'))
      else cb()
    },
    trigger: 'blur',
  }],
  keeperId: [{ required: true, message: '请选择保管人', trigger: 'change' }],
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    phoneNumber: '',
    phoneCode: '',
    phoneModel: '',
    keeperId: undefined,
    wechatBound: '',
    status: 'ENABLED',
  })
  editingPhoneMasked.value = ''
}

const handleAdd = () => {
  dialogTitle.value = '新增手机信息'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: PhoneVO) => {
  dialogTitle.value = '编辑手机信息'
  editingPhoneMasked.value = row.phoneNumberMasked || '--'
  Object.assign(formData, {
    id: row.id,
    phoneNumber: '',
    phoneCode: row.phoneCode || '',
    phoneModel: row.phoneModel || '',
    keeperId: row.keeperId,
    wechatBound: row.wechatBound || '',
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
        phoneCode: formData.phoneCode || undefined,
        phoneModel: formData.phoneModel || undefined,
        keeperId: formData.keeperId!,
        wechatBound: formData.wechatBound || undefined,
        status: formData.status,
      }
      if (formData.id) {
        await updatePhone({ id: formData.id, ...payload })
      } else {
        await createPhone({ ...payload, phoneNumber: formData.phoneNumber })
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } catch {
      /* 错误已由拦截器提示 */
    }
  })
}

const handleDelete = async (row: PhoneVO) => {
  try {
    await ElMessageBox.confirm('确认删除该手机记录？', '提示', { type: 'warning' })
    await deletePhone(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    /* 取消或错误 */
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.phone-page { padding: 20px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.masked-text { font-family: monospace; }
</style>
