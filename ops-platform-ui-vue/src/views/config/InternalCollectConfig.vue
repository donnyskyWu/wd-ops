<template>
  <div class="internal-collect-config">
    <ContentWrap title="内部采集配置" subtitle="企微 / 个微奥创采集（平台类凭证已迁至 M4）">
      <el-alert
        title="平台采集凭证（公众号/抖音/快手/视频号/服务号）已迁至「平台账号管理 → 采集 Tab」，本页仅保留企业微信与个微奥创配置。"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane v-for="tab in platformTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>

      <WeworkAppConfigPanel v-if="isWeworkTab" />

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
import { Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import WeworkAppConfigPanel from '@/components/WeworkAppConfigPanel.vue'
import { internalCollectApi, type AoCreateAccountVO } from '@/api/config'

const platformTabs = [
  { label: '企业微信', value: 'WEWORK' },
  { label: '个人微信', value: 'WECHAT_PERSONAL' },
]

const activeTab = ref('WEWORK')
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

const isWeworkTab = computed(() => activeTab.value === 'WEWORK')
const isPersonalWechatTab = computed(() => activeTab.value === 'WECHAT_PERSONAL')
const aoAccountDialogTitle = computed(() => (aoAccountForm.id ? '编辑奥创账号' : '新增奥创账号'))

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
  if (isPersonalWechatTab.value) {
    loadAoCreate()
  }
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

onMounted(() => {
  if (isPersonalWechatTab.value) {
    loadAoCreate()
  }
})
</script>

<style scoped>
.internal-collect-config { padding: 20px; }
</style>
