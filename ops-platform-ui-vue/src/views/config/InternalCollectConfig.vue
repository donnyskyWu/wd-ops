<template>
  <div class="internal-collect-config">
    <ContentWrap title="内部采集配置" subtitle="各平台账号采集配置管理">
      <el-alert
        :title="alertTitle"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane v-for="tab in platformTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>

      <template v-if="usesStandardCollectUi">
        <TableSearch>
          <el-form :inline="true" :model="searchForm">
            <el-form-item label="账号名称">
              <el-input v-model="searchForm.configName" placeholder="模糊搜索" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="状态">
              <DictSelect v-model="searchForm.status" dict-type="dict_config_status" placeholder="请选择" style="width: 120px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
              <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
            </el-form-item>
          </el-form>
        </TableSearch>

        <div style="margin-bottom: 16px">
          <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>新增配置</el-button>
        </div>

        <el-table :data="configList" border stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="configName" label="账号名称" min-width="140">
            <template #default="{ row }">{{ row.accountName || row.configName }}</template>
          </el-table-column>
          <el-table-column prop="accountIdentifier" label="账号标识" min-width="140" />
          <el-table-column v-if="usesPlatformAccount" prop="accountId" label="平台账号ID" width="110" align="center" />
          <el-table-column prop="appId" label="APPID" min-width="120" show-overflow-tooltip />
          <el-table-column label="直播号" width="80" align="center">
            <template #default="{ row }">{{ row.isLive ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
                {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="170" />
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="warning" @click="handleToggleStatus(row)">
                {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <Pagination
          :total="total"
          :current-page="pageNo"
          :page-size="pageSize"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </template>

      <WeworkAppConfigPanel v-else-if="isWeworkTab" />

      <el-card v-else-if="isPersonalWechatTab" header="奥创接口配置（管理员）">
        <el-form :model="aoForm" label-width="120px" style="max-width: 600px">
          <el-form-item label="接口地址"><el-input v-model="aoForm.apiUrl" /></el-form-item>
          <el-form-item label="AppID"><el-input v-model="aoForm.appId" /></el-form-item>
          <el-form-item label="AppSecret">
            <el-input v-model="aoForm.appSecret" type="password" :placeholder="aoForm.appSecretMasked ? '****' : '请输入'" />
          </el-form-item>
          <el-form-item label="Token">
            <el-input v-model="aoForm.token" type="password" :placeholder="aoForm.tokenMasked ? '****' : '请输入'" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveAoCreate">保存奥创配置</el-button>
          </el-form-item>
        </el-form>

        <el-divider content-position="left">奥创账号列表</el-divider>
        <div style="margin-bottom: 12px">
          <el-button type="primary" @click="handleAoAccountCreate"><el-icon><Plus /></el-icon>新增奥创账号</el-button>
        </div>
        <el-table :data="aoAccountList" border stripe v-loading="aoAccountLoading">
          <el-table-column prop="accountName" label="账号名称" min-width="140" />
          <el-table-column prop="aochuangAccountId" label="奥创 AccountId" min-width="140" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <DictLabel dict-type="dict_config_status" :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="connStatus" label="连接状态" width="110" align="center">
            <template #default="{ row }">
              <DictLabel v-if="row.connStatus" dict-type="dict_conn_status" :value="row.connStatus" />
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="170" />
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleAoAccountEdit(row)">编辑</el-button>
              <el-button link type="success" :loading="aoTestLoadingId === row.id" @click="handleAoAccountTest(row)">测试连接</el-button>
              <el-button link type="danger" @click="handleAoAccountDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </ContentWrap>

    <el-dialog v-if="usesStandardCollectUi" v-model="dialogVisible" :title="dialogTitle" width="640px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
        <template v-if="usesPlatformAccount">
          <el-form-item label="平台账号" prop="accountId">
            <AccountSelect
              v-model="formData.accountId"
              :platform-type="accountSelectPlatformType"
              :account-type="accountSelectAccountType"
              @change="handleAccountChange"
            />
          </el-form-item>
          <el-form-item label="账号名称" prop="configName">
            <el-input v-model="formData.configName" disabled placeholder="选择平台账号后自动填充" />
          </el-form-item>
          <el-form-item label="账号标识">
            <el-input v-model="formData.accountIdentifier" disabled placeholder="选择平台账号后自动填充" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="账号名称" prop="configName">
            <el-input v-model="formData.configName" />
          </el-form-item>
          <el-form-item label="账号标识" prop="accountIdentifier">
            <el-input v-model="formData.accountIdentifier" />
          </el-form-item>
        </template>
        <el-form-item label="APPID"><el-input v-model="formData.appId" /></el-form-item>
        <el-form-item label="APPSECRET">
          <el-input v-model="formData.appSecret" type="password" show-password placeholder="留空不修改" />
        </el-form-item>
        <template v-if="activeTab === 'KUAISHOU'">
          <el-form-item label="Cookie"><el-input v-model="formData.cookie" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="Auth Token">
            <el-input v-model="formData.authToken" type="password" show-password />
          </el-form-item>
          <el-form-item label="字段映射">
            <el-input v-model="formData.fieldMapping" type="textarea" :rows="2" placeholder='JSON 如 {"fans":"fan_count"}' />
          </el-form-item>
          <el-form-item label="直播号">
            <el-switch v-model="formData.isLive" />
          </el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="formData.remark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" active-value="ENABLED" inactive-value="DISABLED" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aoAccountDialogVisible" :title="aoAccountDialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="aoAccountFormRef" :model="aoAccountForm" :rules="aoAccountRules" label-width="130px">
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="aoAccountForm.accountName" placeholder="如：华东客服组" />
        </el-form-item>
        <el-form-item label="奥创 AccountId" prop="aochuangAccountId">
          <el-input v-model="aoAccountForm.aochuangAccountId" placeholder="奥创工作台 accountId" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <DictSelect v-model="aoAccountForm.status" dict-type="dict_config_status" placeholder="请选择" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aoAccountDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aoAccountSubmitLoading" @click="handleAoAccountSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import WeworkAppConfigPanel from '@/components/WeworkAppConfigPanel.vue'
import { internalCollectApi, type CollectConfigVO, type AoCreateAccountVO } from '@/api/config'

const SPECIAL_TABS = new Set(['WEWORK', 'WECHAT_PERSONAL'])

const PLATFORM_ACCOUNT_TABS = new Set([
  'WECHAT_OFFICIAL', 'DOUYIN', 'KUAISHOU', 'WECHAT_VIDEO', 'SERVICE_ACCOUNT',
])

const platformTabs = [
  { label: '公众号', value: 'WECHAT_OFFICIAL' },
  { label: '抖音', value: 'DOUYIN' },
  { label: '快手', value: 'KUAISHOU' },
  { label: '视频号', value: 'WECHAT_VIDEO' },
  { label: '服务号', value: 'SERVICE_ACCOUNT' },
  { label: '企业微信', value: 'WEWORK' },
  { label: '个人微信', value: 'WECHAT_PERSONAL' },
]

const activeTab = ref('WECHAT_OFFICIAL')
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const configList = ref<CollectConfigVO[]>([])

const searchForm = reactive({ configName: '', status: '' })
const formData = reactive<Record<string, unknown>>({
  accountId: undefined as number | undefined,
  configName: '', accountIdentifier: '', appId: '', appSecret: '', cookie: '', authToken: '',
  fieldMapping: '', isLive: false, status: 'ENABLED', remark: '',
})
const aoForm = reactive<Record<string, unknown>>({
  apiUrl: '', appId: '', appSecret: '', token: '', appSecretMasked: '', tokenMasked: '',
})
const aoAccountList = ref<AoCreateAccountVO[]>([])
const aoAccountLoading = ref(false)
const aoAccountDialogVisible = ref(false)
const aoAccountSubmitLoading = ref(false)
const aoTestLoadingId = ref<number | null>(null)
const aoAccountFormRef = ref<FormInstance>()
const aoAccountForm = reactive({
  id: undefined as number | undefined,
  accountName: '',
  aochuangAccountId: '',
  status: 'ENABLED',
})
const aoAccountRules: FormRules = {
  accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  aochuangAccountId: [{ required: true, message: '请输入奥创 AccountId', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const usesStandardCollectUi = computed(() => !SPECIAL_TABS.has(activeTab.value))
const isWeworkTab = computed(() => activeTab.value === 'WEWORK')
const isPersonalWechatTab = computed(() => activeTab.value === 'WECHAT_PERSONAL')

const alertTitle = computed(() => {
  if (isWeworkTab.value) return '企业微信 Tab 与账号管理中的企业微信应用配置共用同一套配置'
  if (isPersonalWechatTab.value) return '个人微信 Tab：奥创接口凭证 + 多奥创账号管理（管理员）'
  return '按平台管理内部采集账号'
})

const usesPlatformAccount = computed(() => PLATFORM_ACCOUNT_TABS.has(activeTab.value))
const accountSelectPlatformType = computed(() =>
  activeTab.value === 'SERVICE_ACCOUNT' ? 'WECHAT_OFFICIAL' : activeTab.value,
)
const accountSelectAccountType = computed(() =>
  activeTab.value === 'SERVICE_ACCOUNT' ? 'SERVICE_ACCOUNT' : undefined,
)

const rules = computed<FormRules>(() => {
  if (usesPlatformAccount.value) {
    return {
      accountId: [{ required: true, message: '请选择平台账号', trigger: 'change' }],
      configName: [{ required: true, message: '请选择平台账号', trigger: 'change' }],
    }
  }
  return {
    configName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
    accountIdentifier: [{ required: true, message: '请输入账号标识', trigger: 'blur' }],
  }
})

const dialogTitle = computed(() => (formData.id ? '编辑账号配置' : '新增账号配置'))
const aoAccountDialogTitle = computed(() => (aoAccountForm.id ? '编辑奥创账号' : '新增奥创账号'))

const handleAccountChange = (_id: number | undefined, item?: { accountName?: string; externalAccountId?: string }) => {
  if (item) {
    formData.configName = item.accountName || ''
    formData.accountIdentifier = item.externalAccountId || ''
  }
}

const loadList = async () => {
  if (!usesStandardCollectUi.value) return
  loading.value = true
  try {
    const res = await internalCollectApi.list({
      platformType: activeTab.value,
      configName: searchForm.configName || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    configList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const loadAoCreate = async () => {
  if (!isPersonalWechatTab.value) return
  const data = await internalCollectApi.getAoCreate()
  if (data) {
    Object.assign(aoForm, { ...data, appSecret: '', token: '' })
  }
  await loadAoAccounts()
}

const loadAoAccounts = async () => {
  if (!isPersonalWechatTab.value) return
  aoAccountLoading.value = true
  try {
    const res = await internalCollectApi.listAoCreateAccounts({ pageNo: 1, pageSize: 100 })
    aoAccountList.value = res.list || []
  } finally {
    aoAccountLoading.value = false
  }
}

const handleTabChange = () => {
  pageNo.value = 1
  if (usesStandardCollectUi.value) {
    loadList()
  } else if (isPersonalWechatTab.value) {
    loadAoCreate()
  }
}

const handleSearch = () => { pageNo.value = 1; loadList() }
const handleReset = () => { searchForm.configName = ''; searchForm.status = ''; handleSearch() }

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined, accountId: undefined, configName: '', accountIdentifier: '', appId: '', appSecret: '',
    cookie: '', authToken: '', fieldMapping: '', isLive: false, status: 'ENABLED', remark: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row: CollectConfigVO) => {
  Object.assign(formData, { ...row, appSecret: '', authToken: '' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload: Record<string, unknown> = {
      configName: formData.configName,
      accountIdentifier: formData.accountIdentifier,
      platformType: activeTab.value,
      appId: formData.appId,
      status: formData.status,
      remark: formData.remark,
      collectMethod: 'INTERNAL',
    }
    if (usesPlatformAccount.value && formData.accountId) {
      payload.accountId = formData.accountId
    }
    if (formData.appSecret) payload.appSecret = formData.appSecret
    if (activeTab.value === 'KUAISHOU') {
      payload.cookie = formData.cookie
      if (formData.authToken) payload.authToken = formData.authToken
      payload.fieldMapping = formData.fieldMapping
      payload.isLive = formData.isLive
    }
    if (formData.id) {
      payload.id = formData.id
      await internalCollectApi.update(payload)
      ElMessage.success('保存成功')
    } else {
      await internalCollectApi.create(payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async (row: CollectConfigVO) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await internalCollectApi.toggleStatus(row.id, newStatus)
  ElMessage.success('状态已更新')
  await loadList()
}

const handleDelete = async (row: CollectConfigVO) => {
  await ElMessageBox.confirm(`确定删除账号「${row.configName}」？`, '删除确认', { type: 'warning' })
  await internalCollectApi.delete(row.id)
  ElMessage.success('删除成功')
  await loadList()
}

const saveAoCreate = async () => {
  await internalCollectApi.saveAoCreate({
    apiUrl: aoForm.apiUrl,
    appId: aoForm.appId,
    appSecret: aoForm.appSecret || undefined,
    token: aoForm.token || undefined,
  })
  ElMessage.success('奥创配置已保存')
  await loadAoCreate()
}

const handleAoAccountCreate = () => {
  Object.assign(aoAccountForm, { id: undefined, accountName: '', aochuangAccountId: '', status: 'ENABLED' })
  aoAccountDialogVisible.value = true
}

const handleAoAccountEdit = (row: AoCreateAccountVO) => {
  Object.assign(aoAccountForm, {
    id: row.id,
    accountName: row.accountName,
    aochuangAccountId: row.aochuangAccountId,
    status: row.status || 'ENABLED',
  })
  aoAccountDialogVisible.value = true
}

const handleAoAccountSubmit = async () => {
  if (!aoAccountFormRef.value) return
  const valid = await aoAccountFormRef.value.validate().catch(() => false)
  if (!valid) return
  aoAccountSubmitLoading.value = true
  try {
    if (aoAccountForm.id) {
      await internalCollectApi.updateAoCreateAccount({ ...aoAccountForm })
      ElMessage.success('保存成功')
    } else {
      await internalCollectApi.createAoCreateAccount({ ...aoAccountForm })
      ElMessage.success('创建成功')
    }
    aoAccountDialogVisible.value = false
    await loadAoAccounts()
  } finally {
    aoAccountSubmitLoading.value = false
  }
}

const handleAoAccountTest = async (row: AoCreateAccountVO) => {
  aoTestLoadingId.value = row.id
  try {
    const result = await internalCollectApi.testAoCreateAccountConnection(row.id)
    if (result.success) {
      ElMessage.success(result.message || `连接成功，发现 ${result.deviceCount} 个设备`)
    } else {
      ElMessage.error(result.message || '连接失败')
    }
    await loadAoAccounts()
  } finally {
    aoTestLoadingId.value = null
  }
}

const handleAoAccountDelete = async (row: AoCreateAccountVO) => {
  await ElMessageBox.confirm(`确定删除奥创账号「${row.accountName}」？`, '删除确认', { type: 'warning' })
  await internalCollectApi.deleteAoCreateAccount(row.id)
  ElMessage.success('删除成功')
  await loadAoAccounts()
}

const handlePageChange = (p: number) => { pageNo.value = p; loadList() }
const handleSizeChange = (s: number) => { pageSize.value = s; pageNo.value = 1; loadList() }

onMounted(() => { loadList() })
</script>

<style scoped>
.internal-collect-config { padding: 20px; }
</style>
