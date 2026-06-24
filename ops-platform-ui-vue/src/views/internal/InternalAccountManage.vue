<template>
  <div class="internal-account-page">
    <div class="page-header">
      <h1 class="page-title">平台账号管理</h1>
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
        新增账号
      </el-button>
      <span class="total-info">共 {{ pagination.total }} 条</span>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="accountName" label="账号名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="externalAccountId" label="平台账号ID" width="160" />
      <el-table-column v-if="isWechatOfficial" prop="accountType" label="账号类型" width="100">
        <template #default="{ row }">
          <DictLabel dict-type="dict_account_type" :value="row.accountType" />
        </template>
      </el-table-column>
      <el-table-column v-if="isWechatOfficial" prop="usageStatus" label="使用状态" width="100">
        <template #default="{ row }">
          <DictLabel dict-type="dict_wechat_usage_status" :value="row.usageStatus" />
        </template>
      </el-table-column>
      <el-table-column v-if="isWechatOfficial" prop="certCount" label="认证次数" width="90" align="center" />
      <el-table-column v-if="!isWechatOfficial" prop="companyName" label="公司" width="140" />
      <el-table-column v-if="!isWechatOfficial" prop="realName" label="实名人" width="100" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_account_status" :value="row.status" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px" destroy-on-close>
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="130px">
        <el-form-item label="平台" prop="platformType">
          <DictSelect v-model="formData.platformType" dict-type="dict_platform_type" disabled />
        </el-form-item>
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="formData.accountName" maxlength="128" />
        </el-form-item>
        <el-form-item label="账号ID" prop="externalAccountId">
          <el-input v-model="formData.externalAccountId" maxlength="64" :disabled="!!formData.id" />
        </el-form-item>

        <template v-if="isWechatOfficial">
          <el-form-item label="原公众号名称">
            <el-input v-model="formData.originalAccountName" maxlength="128" />
          </el-form-item>
          <el-form-item label="所属IP组">
            <IpGroupTreeSelect v-model="formData.ipGroupId" />
          </el-form-item>
          <el-form-item label="商标名称">
            <el-input v-model="formData.trademarkName" maxlength="128" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="formData.email" maxlength="128" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="formData.password"
              type="password"
              show-password
              maxlength="128"
              :placeholder="formData.id && formData.hasPassword ? '留空则不修改' : '请输入登录密码'"
            />
          </el-form-item>
          <el-form-item label="账号类型" prop="accountType">
            <DictSelect
              v-model="formData.accountType"
              dict-type="dict_account_type"
              :include-values="['SUBSCRIPTION_ACCOUNT', 'SERVICE_ACCOUNT']"
            />
          </el-form-item>
          <el-form-item label="资质类型" prop="qualificationType">
            <DictSelect v-model="formData.qualificationType" dict-type="dict_qualification_type" />
          </el-form-item>
          <el-form-item label="使用状态">
            <DictSelect v-model="formData.usageStatus" dict-type="dict_wechat_usage_status" />
          </el-form-item>
          <el-form-item label="认证到期时间">
            <el-date-picker
              v-model="formData.certExpiryTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item v-if="formData.id" label="认证次数">
            <el-input :model-value="String(formData.certCount ?? 0)" disabled />
          </el-form-item>
          <el-form-item v-if="formData.qualificationType === 'ENTERPRISE'" label="企业名称" prop="companyId">
            <CompanySelect v-model="formData.companyId" />
          </el-form-item>
          <el-form-item v-if="formData.qualificationType === 'PERSONAL'" label="实名人" prop="realnameId">
            <RealNameSelect v-model="formData.realnameId" :exclude-bound="true" />
          </el-form-item>
          <el-form-item label="关联视频号">
            <AccountSelect
              v-model="formData.linkedVideoAccountId"
              platform-type="WECHAT_VIDEO"
              @change="handleLinkedVideoChange"
            />
          </el-form-item>
          <el-form-item v-if="formData.videoAccountRegisteredAt" label="视频号注册时间">
            <el-input :model-value="formData.videoAccountRegisteredAt" disabled />
          </el-form-item>
          <el-form-item label="管理员">
            <UserSelect v-model="formData.adminUserId" @change="handleAdminUserChange" />
          </el-form-item>
          <el-form-item v-if="formData.adminPhoneMasked" label="管理员手机号">
            <el-input :model-value="formData.adminPhoneMasked" disabled />
          </el-form-item>
          <el-form-item label="管理员身份证">
            <el-input
              v-model="formData.adminIdCard"
              maxlength="32"
              :placeholder="formData.id && formData.hasAdminIdCard ? '留空则不修改' : '请输入身份证号'"
            />
          </el-form-item>
        </template>

        <template v-else>
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
        </template>

        <el-form-item v-if="!isWechatOfficial" label="所属IP组">
          <IpGroupTreeSelect v-model="formData.ipGroupId" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="formData.status" dict-type="dict_account_status" />
        </el-form-item>
        <el-form-item v-if="formData.id" label="采集配置">
          <el-link type="primary" @click="goToCollectConfig">前往详情配置采集</el-link>
          <span class="collect-hint">（凭证与绑定请在详情页「采集」Tab 维护）</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import CompanySelect from '@/components/selectors/CompanySelect.vue'
import RealNameSelect from '@/components/selectors/RealNameSelect.vue'
import PhoneSelect from '@/components/selectors/PhoneSelect.vue'
import SimCardSelect from '@/components/selectors/SimCardSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import { exportToExcel } from '@/utils'
import {
  createPlatformAccount,
  deletePlatformAccount,
  getPlatformAccount,
  getPlatformAccountPage,
  updatePlatformAccount,
  type PlatformAccountVO,
} from '@/api/platform-account'
import { isAccountBindingConflict, promptAccountForceReplace } from '@/utils/account-binding-conflict'

const PLATFORM_LABEL_MAP: Record<string, string> = {
  WECHAT_OFFICIAL: '公众号',
  WECHAT_VIDEO: '视频号',
  DOUYIN: '抖音',
  KUAISHOU: '快手',
  XIAOHONGSHU: '小红书',
}

const activePlatform = ref('WECHAT_OFFICIAL')
const isWechatOfficial = computed(() => activePlatform.value === 'WECHAT_OFFICIAL')
const router = useRouter()
const loading = ref(false)
const exportLoading = ref(false)
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
  accountType: 'SUBSCRIPTION_ACCOUNT',
  companyId: undefined as number | undefined,
  realnameId: undefined as number | undefined,
  phoneId: undefined as number | undefined,
  simCardId: undefined as number | undefined,
  ipGroupId: undefined as number | undefined,
  status: 'NORMAL',
  trademarkName: '',
  email: '',
  password: '',
  hasPassword: false,
  qualificationType: 'ENTERPRISE' as string | undefined,
  usageStatus: 'REGISTERED' as string | undefined,
  originalAccountName: '',
  certExpiryTime: undefined as string | undefined,
  certCount: 0,
  linkedVideoAccountId: undefined as number | undefined,
  videoAccountRegisteredAt: undefined as string | undefined,
  adminUserId: undefined as number | undefined,
  adminPhoneMasked: '',
  adminIdCard: '',
  hasAdminIdCard: false,
})

const formRules = computed(() => {
  const rules: Record<string, unknown[]> = {
    platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
    accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
    externalAccountId: [{ required: true, message: '请输入账号ID', trigger: 'blur' }],
  }
  if (isWechatOfficial.value) {
    rules.accountType = [{ required: true, message: '请选择账号类型', trigger: 'change' }]
    rules.qualificationType = [{ required: true, message: '请选择资质类型', trigger: 'change' }]
    if (formData.qualificationType === 'ENTERPRISE') {
      rules.companyId = [{ required: true, message: '请选择企业', trigger: 'change' }]
    }
    if (formData.qualificationType === 'PERSONAL') {
      rules.realnameId = [{ required: true, message: '请选择实名人', trigger: 'change' }]
    }
  } else {
    rules.companyId = [{ required: true, message: '请选择公司', trigger: 'change' }]
    rules.realnameId = [{ required: true, message: '请选择实名人', trigger: 'change' }]
  }
  return rules
})

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

const buildListParams = (pageNo: number, pageSize: number) => ({
  platformType: activePlatform.value,
  accountName: searchForm.accountName || undefined,
  status: searchForm.status,
  pageNo,
  pageSize,
})

const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first = await getPlatformAccountPage(buildListParams(1, exportPageSize))
  let rows = first.list || []
  const totalCount = first.total ?? 0
  if (totalCount > exportPageSize) {
    const totalPages = Math.ceil(totalCount / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res = await getPlatformAccountPage(buildListParams(page, exportPageSize))
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
      platformLabel: PLATFORM_LABEL_MAP[row.platformType] || row.platformType || '',
      accountName: row.accountName,
      externalAccountId: row.externalAccountId || '',
      companyName: row.companyName || '',
      realName: row.realName || '',
      status: row.status === 'NORMAL' ? '正常' : '停用',
      createTime: row.createTime || '',
    }))
    const columns = [
      { key: 'platformLabel', label: '平台' },
      { key: 'accountName', label: '账号名称' },
      { key: 'externalAccountId', label: '平台账号ID' },
      { key: 'companyName', label: '公司' },
      { key: 'realName', label: '实名人' },
      { key: 'status', label: '状态' },
      { key: 'createTime', label: '创建时间' },
    ]
    exportToExcel(exportData, columns, '平台账号管理')
  } catch {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    platformType: activePlatform.value,
    accountName: '',
    externalAccountId: '',
    accountType: activePlatform.value === 'WECHAT_OFFICIAL' ? 'SUBSCRIPTION_ACCOUNT' : 'OFFICIAL_ACCOUNT',
    companyId: undefined,
    realnameId: undefined,
    phoneId: undefined,
    simCardId: undefined,
    ipGroupId: undefined,
    status: 'NORMAL',
    trademarkName: '',
    email: '',
    password: '',
    hasPassword: false,
    qualificationType: 'ENTERPRISE',
    usageStatus: 'REGISTERED',
    originalAccountName: '',
    certExpiryTime: undefined,
    certCount: 0,
    linkedVideoAccountId: undefined,
    videoAccountRegisteredAt: undefined,
    adminUserId: undefined,
    adminPhoneMasked: '',
    adminIdCard: '',
    hasAdminIdCard: false,
  })
}

const resolveVideoRegisteredAt = (account: PlatformAccountVO): string | undefined =>
  account.createTime || account.linkedAt || undefined

const handleLinkedVideoChange = async (videoAccountId: number | undefined) => {
  if (!videoAccountId) {
    formData.videoAccountRegisteredAt = undefined
    return
  }
  try {
    const video = await getPlatformAccount(videoAccountId)
    formData.videoAccountRegisteredAt = resolveVideoRegisteredAt(video)
  } catch {
    formData.videoAccountRegisteredAt = undefined
  }
}

const handleAdminUserChange = (_userId: number | undefined, item?: { phoneMasked?: string }) => {
  formData.adminPhoneMasked = item?.phoneMasked || ''
}

const handleAdd = () => {
  dialogTitle.value = '新增账号'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row: PlatformAccountVO) => {
  dialogTitle.value = '编辑账号'
  try {
    const detail = await getPlatformAccount(row.id)
    Object.assign(formData, {
      id: detail.id,
      platformType: detail.platformType,
      accountName: detail.accountName,
      externalAccountId: detail.externalAccountId,
      accountType: detail.accountType || (isWechatOfficial.value ? 'SUBSCRIPTION_ACCOUNT' : 'OFFICIAL_ACCOUNT'),
      companyId: detail.companyId,
      realnameId: detail.realnameId,
      phoneId: detail.phoneId,
      simCardId: detail.simCardId,
      ipGroupId: detail.ipGroupId,
      status: detail.status || 'NORMAL',
      trademarkName: detail.trademarkName || '',
      email: detail.email || '',
      password: '',
      hasPassword: detail.hasPassword,
      qualificationType: detail.qualificationType || 'ENTERPRISE',
      usageStatus: detail.usageStatus || 'REGISTERED',
      originalAccountName: detail.originalAccountName || '',
      certExpiryTime: detail.certExpiryTime,
      certCount: detail.certCount ?? 0,
      linkedVideoAccountId: detail.linkedVideoAccountId,
      videoAccountRegisteredAt: detail.videoAccountRegisteredAt,
      adminUserId: detail.adminUserId,
      adminPhoneMasked: detail.adminPhoneMasked || '',
      adminIdCard: '',
      hasAdminIdCard: detail.hasAdminIdCard,
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载账号详情失败')
  }
}

const handleView = (row: PlatformAccountVO) => {
  router.push(`/platform-account/${row.id}`)
}

const goToCollectConfig = () => {
  if (!formData.id) return
  dialogVisible.value = false
  router.push({ path: `/platform-account/${formData.id}`, query: { tab: 'collect' } })
}

const buildPayload = (forceReplace: boolean, reason?: string) => {
  const base = {
    platformType: formData.platformType,
    accountName: formData.accountName,
    externalAccountId: formData.externalAccountId,
    accountType: formData.accountType,
    ipGroupId: formData.ipGroupId,
    status: formData.status,
    forceReplace,
    reason,
  }
  if (isWechatOfficial.value) {
    return {
      ...base,
      companyId: formData.qualificationType === 'ENTERPRISE' ? formData.companyId : undefined,
      realnameId: formData.qualificationType === 'PERSONAL' ? formData.realnameId : undefined,
      trademarkName: formData.trademarkName || undefined,
      email: formData.email || undefined,
      password: formData.password || undefined,
      qualificationType: formData.qualificationType,
      usageStatus: formData.usageStatus,
      originalAccountName: formData.originalAccountName || undefined,
      certExpiryTime: formData.certExpiryTime || undefined,
      linkedVideoAccountId: formData.linkedVideoAccountId,
      adminUserId: formData.adminUserId,
      adminIdCard: formData.adminIdCard || undefined,
    }
  }
  return {
    ...base,
    companyId: formData.companyId!,
    realnameId: formData.realnameId!,
    phoneId: formData.phoneId,
    simCardId: formData.simCardId,
  }
}

const submitWithForceReplace = async (forceReplace: boolean, reason?: string) => {
  const payload = buildPayload(forceReplace, reason)
  if (formData.id) {
    await updatePlatformAccount({ id: formData.id, ...payload })
  } else {
    await createPlatformAccount(payload as Parameters<typeof createPlatformAccount>[0])
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
      if (isAccountBindingConflict(err)) {
        const ok = await promptAccountForceReplace(err, (reason) => submitWithForceReplace(true, reason))
        if (ok) {
          ElMessage.success('保存成功（已强制替换）')
          dialogVisible.value = false
          loadData()
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
  .page-title { font-size: 22px; font-weight: 600; color: #303133; margin: 0; }
  .platform-tabs { margin-bottom: 16px; }
  .action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
  .total-info { color: #909399; font-size: 14px; }
  .collect-hint { margin-left: 8px; color: #909399; font-size: 13px; }
}
</style>
