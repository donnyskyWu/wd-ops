<template>
  <div class="tenant-manage">
    <ContentWrap title="租户管理" subtitle="多租户管理">
      <el-alert 
        title="管理系统中的租户信息（支持SaaS多租户架构）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="租户名称">
            <el-input v-model="searchForm.tenantName" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="searchForm.contactName" placeholder="请输入" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="正常" value="NORMAL" />
              <el-option label="试用" value="TRIAL" />
              <el-option label="已到期" value="EXPIRED" />
              <el-option label="已停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </TableSearch>

      <!-- 工具栏 -->
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增租户
        </el-button>
        <el-button 
          type="danger" 
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          删除选中
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table 
        :data="displayList" 
        border 
        stripe 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="tenantName" label="租户名称" min-width="180" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="expireTime" label="到期时间" width="180" />
        <el-table-column prop="accountCount" label="账号数量" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="TENANT_STATUS_TAG[row.status] || 'info'">
              {{ TENANT_STATUS_LABEL[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleToggleStatus(row)">
              {{ row.status === 'DISABLED' ? '启用' : '停用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </ContentWrap>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="formData.tenantName" placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="contactEmail">
          <el-input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="到期时间" prop="expireTime">
          <el-date-picker 
            v-model="formData.expireTime" 
            type="datetime"
            placeholder="选择到期时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="最大账号数">
          <el-input-number v-model="formData.maxAccounts" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="formData.remark" 
            type="textarea"
            :rows="2"
            placeholder="请输入备注信息" 
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="formData.status" style="width: 100%">
            <el-option label="试用" value="TRIAL" />
            <el-option label="正常" value="NORMAL" />
            <el-option label="已到期" value="EXPIRED" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import {
  fetchTenantList,
  createTenant,
  updateTenant,
  deleteTenant,
  TENANT_STATUS_LABEL,
  TENANT_STATUS_TAG,
  type TenantVO,
} from '@/api/system-tenant'

type TenantInfo = TenantVO

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  tenantName: '',
  contactName: '',
  status: '',
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tenantList = ref<TenantInfo[]>([])
const selectedRows = ref<TenantInfo[]>([])

const formData = reactive<Partial<TenantInfo & { expireTime: string | Date }>>({
  tenantName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  expireTime: '',
  maxAccounts: 10,
  accountCount: 0,
  status: 'TRIAL',
  remark: '',
})

const rules: FormRules = {
  tenantName: [
    { required: true, message: '请输入租户名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' },
  ],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  expireTime: [{ required: true, message: '请选择到期时间', trigger: 'change' }],
}

const displayList = computed(() => tenantList.value)

const dialogTitle = computed(() => (formData.id ? '编辑租户' : '新增租户'))

function formatDateTime(value: string | Date | undefined): string | undefined {
  if (!value) return undefined
  if (value instanceof Date) {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
  }
  return value.includes('T') ? value : value.replace(' ', 'T')
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await fetchTenantList({
      tenantName: searchForm.tenantName || undefined,
      contactName: searchForm.contactName || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    tenantList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.tenantName = ''
  searchForm.contactName = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    tenantName: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    expireTime: '',
    maxAccounts: 10,
    accountCount: 0,
    status: 'TRIAL',
    remark: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row: TenantInfo) => {
  Object.assign(formData, { ...row, expireTime: row.expireTime || '' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const expireTime = formatDateTime(formData.expireTime)
    if (formData.id) {
      await updateTenant({
        id: formData.id,
        tenantName: formData.tenantName,
        contactName: formData.contactName,
        contactPhone: formData.contactPhone,
        contactEmail: formData.contactEmail,
        expireTime,
        maxAccounts: formData.maxAccounts,
        status: formData.status,
        remark: formData.remark,
      })
      ElMessage.success('编辑成功')
    } else {
      await createTenant({
        tenantName: formData.tenantName!,
        contactName: formData.contactName!,
        contactPhone: formData.contactPhone!,
        contactEmail: formData.contactEmail,
        expireTime: expireTime!,
        maxAccounts: formData.maxAccounts,
        status: formData.status,
        remark: formData.remark,
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async (row: TenantInfo) => {
  const newStatus = row.status === 'DISABLED' ? 'NORMAL' : 'DISABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'DISABLED' ? '停用' : '启用'}租户「${row.tenantName}」吗？`,
      '提示',
      { type: 'warning' },
    )
    await updateTenant({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: TenantInfo) => {
  try {
    await ElMessageBox.confirm(`确定要删除租户「${row.tenantName}」吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteTenant(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: TenantInfo[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条租户吗？`,
      '批量删除确认',
      { type: 'warning' },
    )
    for (const row of selectedRows.value) {
      await deleteTenant(row.id)
    }
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    await loadList()
  } catch {}
}

const handlePageChange = (page: number) => {
  pageNo.value = page
  loadList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNo.value = 1
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.tenant-manage {
  padding: 20px;
}
</style>
