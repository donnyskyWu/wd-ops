<template>
  <div class="internal-account-page">
    <div class="page-header">
      <h1 class="page-title">平台账号管理</h1>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>账号管理</el-breadcrumb-item>
        <el-breadcrumb-item>平台账号管理</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-tabs v-model="activePlatform" @tab-change="handleTabChange" class="platform-tabs">
      <el-tab-pane label="公众号" name="WECHAT_OFFICIAL" />
      <el-tab-pane label="视频号" name="WECHAT_VIDEO" />
      <el-tab-pane label="抖音" name="DOUYIN" />
      <el-tab-pane label="快手" name="KUAISHOU" />
      <el-tab-pane label="小红书" name="XIAOHONGSHU" />
    </el-tabs>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="账号名称">
        <el-input v-model="searchForm.accountName" placeholder="请输入账号名称" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_account_status" placeholder="全部" clearable />
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增账号
      </el-button>
      <span class="total-info">共 {{ pagination.total }} 条</span>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="accountName" label="账号名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="externalAccountId" label="平台账号ID" width="160" />
      <el-table-column prop="companyName" label="公司" width="140" />
      <el-table-column prop="realName" label="实名人" width="100" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">
            {{ row.status === 'NORMAL' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :current-page="pagination.pageNo"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @update:current-page="(val) => pagination.pageNo = val"
      @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
      @change="loadData"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="110px">
        <el-form-item label="平台" prop="platformType">
          <DictSelect v-model="formData.platformType" dict-type="dict_platform_type" disabled />
        </el-form-item>
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="formData.accountName" maxlength="128" />
        </el-form-item>
        <el-form-item label="平台账号ID" prop="externalAccountId">
          <el-input v-model="formData.externalAccountId" maxlength="64" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="账号类型">
          <DictSelect v-model="formData.accountType" dict-type="dict_account_type" />
        </el-form-item>
        <el-form-item label="公司" prop="companyId">
          <CompanySelect v-model="formData.companyId" />
        </el-form-item>
        <el-form-item label="实名人" prop="realnameId">
          <RealNameSelect
            v-model="formData.realnameId"
            :company-id="formData.companyId"
            :exclude-bound="true"
          />
        </el-form-item>
        <el-form-item label="手机">
          <PhoneSelect
            v-model="formData.phoneId"
            :real-name-id="formData.realnameId"
            :exclude-bound="true"
          />
        </el-form-item>
        <el-form-item label="手机卡">
          <SimCardSelect
            v-model="formData.simCardId"
            :phone-id="formData.phoneId"
            :exclude-bound="true"
          />
        </el-form-item>
        <el-form-item label="IP组">
          <IpGroupTreeSelect v-model="formData.ipGroupId" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="formData.status" dict-type="dict_account_status" />
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
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import CompanySelect from '@/components/selectors/CompanySelect.vue'
import RealNameSelect from '@/components/selectors/RealNameSelect.vue'
import PhoneSelect from '@/components/selectors/PhoneSelect.vue'
import SimCardSelect from '@/components/selectors/SimCardSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import {
  createPlatformAccount,
  deletePlatformAccount,
  getPlatformAccountPage,
  updatePlatformAccount,
  type PlatformAccountVO,
} from '@/api/platform-account'

const activePlatform = ref('WECHAT_OFFICIAL')
const router = useRouter()
const loading = ref(false)
const tableData = ref<PlatformAccountVO[]>([])
const formRef = ref()

const searchForm = reactive({
  accountName: '',
  status: undefined as string | undefined,
})

const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })
const dialogVisible = ref(false)
const dialogTitle = ref('新增账号')

const formData = reactive({
  id: undefined as number | undefined,
  platformType: 'WECHAT_OFFICIAL',
  accountName: '',
  externalAccountId: '',
  accountType: 'OFFICIAL_ACCOUNT',
  companyId: undefined as number | undefined,
  realnameId: undefined as number | undefined,
  phoneId: undefined as number | undefined,
  simCardId: undefined as number | undefined,
  ipGroupId: undefined as number | undefined,
  status: 'NORMAL',
})

const formRules = {
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  externalAccountId: [{ required: true, message: '请输入平台账号ID', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择公司', trigger: 'change' }],
  realnameId: [{ required: true, message: '请选择实名人', trigger: 'change' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPlatformAccountPage({
      platformType: activePlatform.value,
      accountName: searchForm.accountName || undefined,
      status: searchForm.status,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    })
    tableData.value = res.list || []
    pagination.total = res.total ?? 0
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  searchForm.accountName = ''
  searchForm.status = undefined
  pagination.pageNo = 1
  loadData()
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

const handleReset = () => {
  searchForm.accountName = ''
  searchForm.status = undefined
  pagination.pageNo = 1
  loadData()
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    platformType: activePlatform.value,
    accountName: '',
    externalAccountId: '',
    accountType: 'OFFICIAL_ACCOUNT',
    companyId: undefined,
    realnameId: undefined,
    phoneId: undefined,
    simCardId: undefined,
    ipGroupId: undefined,
    status: 'NORMAL',
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增账号'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: PlatformAccountVO) => {
  dialogTitle.value = '编辑账号'
  Object.assign(formData, {
    id: row.id,
    platformType: row.platformType,
    accountName: row.accountName,
    externalAccountId: row.externalAccountId,
    accountType: row.accountType || 'OFFICIAL_ACCOUNT',
    companyId: row.companyId,
    realnameId: row.realnameId,
    phoneId: row.phoneId,
    simCardId: row.simCardId,
    ipGroupId: row.ipGroupId,
    status: row.status || 'NORMAL',
  })
  dialogVisible.value = true
}

const handleView = (row: PlatformAccountVO) => {
  router.push(`/platform-account/${row.id}`)
}

const submitWithForceReplace = async (forceReplace: boolean, reason?: string) => {
  const payload = {
    platformType: formData.platformType,
    accountName: formData.accountName,
    externalAccountId: formData.externalAccountId,
    accountType: formData.accountType,
    companyId: formData.companyId!,
    realnameId: formData.realnameId!,
    phoneId: formData.phoneId,
    simCardId: formData.simCardId,
    ipGroupId: formData.ipGroupId,
    status: formData.status,
    forceReplace,
    reason,
  }
  if (formData.id) {
    await updatePlatformAccount({ id: formData.id, ...payload })
  } else {
    await createPlatformAccount(payload)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      await submitWithForceReplace(false)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } catch (err: any) {
      if (err?.message?.includes('已被引用') || err?.message?.includes('1502')) {
        try {
          const { value } = await ElMessageBox.prompt('关联资源已被占用，请填写强制替换原因（5-200字）', '强制替换', {
            confirmButtonText: '确认替换',
            cancelButtonText: '取消',
            inputPattern: /^.{5,200}$/,
            inputErrorMessage: '原因长度需 5-200 字',
          })
          await submitWithForceReplace(true, value)
          ElMessage.success('保存成功（已强制替换）')
          dialogVisible.value = false
          loadData()
        } catch {
          /* 取消 */
        }
      }
    }
  })
}

const handleDelete = async (row: PlatformAccountVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除账号「${row.accountName}」吗？`, '提示', { type: 'warning' })
    await deletePlatformAccount(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    /* 取消或错误 */
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.internal-account-page {
  padding: 20px;
  .page-header { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; }
  .page-title { font-size: 22px; font-weight: 600; color: #303133; margin: 0 0 12px 0; }
  .platform-tabs { margin-bottom: 16px; }
  .action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
  .total-info { color: #909399; font-size: 14px; }
}
</style>
