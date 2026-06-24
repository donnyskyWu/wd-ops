<!--
  M4 平台账号 — 采集 Tab（ADR-047 · Channel-A 凭证 SSOT）
-->
<template>
  <div class="collect-tab" v-loading="pageLoading">
    <ContentWrap title="绑定状态">
      <div style="margin-bottom: 12px">
        <el-button type="primary" plain :loading="batchImporting" @click="handleBatchImport">
          批量绑定未绑定账号
        </el-button>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="绑定状态">
          <DictLabel
            v-if="bindInfo?.bindStatus"
            dict-type="dict_collector_bind_status"
            :value="bindInfo.bindStatus"
          />
          <span v-else class="text-muted">未绑定</span>
        </el-descriptions-item>
        <el-descriptions-item label="连接状态">
          <DictLabel
            v-if="bindInfo?.connStatus"
            dict-type="dict_conn_status"
            :value="bindInfo.connStatus"
          />
          <span v-else class="text-muted">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="Collector 账号 ID" :span="2">
          {{ bindInfo?.collectorAccountId || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="最近绑定">
          {{ bindInfo?.lastBindAt || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="最近探活">
          {{ bindInfo?.lastHealthCheckAt || '—' }}
        </el-descriptions-item>
      </el-descriptions>
    </ContentWrap>

    <ContentWrap title="采集凭证" style="margin-top: 16px">
      <el-alert
        v-if="platformHint"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        :title="platformHint"
      />
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
        title="凭证保存在平台账号（M4 SSOT），绑定采集服务后由 Unified Collector 使用。"
      />
      <el-form :model="credentialForm" label-width="130px" style="max-width: 900px">
        <el-form-item label="Cookie">
          <el-input
            v-model="credentialForm.cookie"
            type="textarea"
            :rows="4"
            :placeholder="accountInfo.hasCookie ? '已配置，留空则不修改' : '请输入 Cookie'"
          />
        </el-form-item>

        <template v-if="platformType === 'WECHAT_OFFICIAL'">
          <el-form-item label="MP Token">
            <el-input
              v-model="credentialForm.mpToken"
              type="password"
              show-password
              :placeholder="accountInfo.hasMpToken ? '已配置，留空则不修改' : '请输入公众号后台 Token'"
            />
          </el-form-item>
        </template>

        <template v-if="platformType === 'KUAISHOU'">
          <el-form-item label="Auth Token">
            <el-input
              v-model="credentialForm.authToken"
              type="password"
              show-password
              :placeholder="accountInfo.hasAuthToken ? '已配置，留空则不修改' : '请输入快手 cp 域 Auth Token'"
            />
          </el-form-item>
          <el-form-item label="字段映射">
            <el-input
              v-model="credentialForm.fieldMapping"
              type="textarea"
              :rows="3"
              placeholder='JSON 如 {"fans":"fan_count"}'
            />
          </el-form-item>
        </template>

        <template v-if="platformType === 'WECHAT_OFFICIAL'">
          <el-divider content-position="left">档案/开放平台（不参与采集）</el-divider>
          <el-form-item label="AppId">
            <el-input v-model="credentialForm.appId" placeholder="可选，仅档案/续费/OpenAPI Phase 2" />
          </el-form-item>
          <el-form-item label="AppSecret">
            <el-input
              v-model="credentialForm.appSecret"
              type="password"
              show-password
              :placeholder="accountInfo.hasAppSecret ? '已配置，留空则不修改' : '可选，不参与 MVP 采集'"
            />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="savingCredentials" @click="saveCredentials">
            保存凭证
          </el-button>
          <el-button :loading="binding" @click="handleBind">绑定采集服务</el-button>
          <el-button :loading="testing" @click="handleTestConnection">测试连接</el-button>
          <el-button :loading="syncing" :disabled="!bindInfo?.collectorAccountId" @click="handleSync">
            同步凭证
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import DictLabel from '@/components/DictLabel.vue'
import { updatePlatformAccount, type PlatformAccountUpdateReq } from '@/api/platform-account'
import {
  getCollectorBind,
  bindCollectorAccount,
  syncCollectorCredentials,
  testCollectorConnection,
  batchImportCollectorAccounts,
  type CollectorAccountBindVO,
} from '@/api/collector-bind'

export interface CollectAccountInfo {
  hasCookie?: boolean
  hasMpToken?: boolean
  hasAuthToken?: boolean
  hasAppSecret?: boolean
  appId?: string
  fieldMapping?: string
}

const props = defineProps<{
  accountId: number
  platformType: string
  accountInfo: CollectAccountInfo
}>()

const emit = defineEmits<{
  accountUpdated: []
}>()

const pageLoading = ref(false)
const savingCredentials = ref(false)
const binding = ref(false)
const testing = ref(false)
const syncing = ref(false)
const batchImporting = ref(false)
const bindInfo = ref<CollectorAccountBindVO | null>(null)

const PLATFORM_HINTS: Record<string, string> = {
  DOUYIN:
    '抖音需 Cookie（含 sessionid）。采集粉丝/作品需 sec_uid：绑定后 Collector 会尝试从 Cookie 自动获取；若采集报「无法获取 sec_uid」，请在账号档案「平台账号 ID」填入 sec_uid（MS4wLjAB 开头长串，非短数字抖音号）后重新「同步凭证」。',
  WECHAT_VIDEO: '视频号需 Cookie（扫码或导入）；绑定后使用 WECHAT_CHANNELS_API 采集源建任务。',
  XIAOHONGSHU: '小红书需 Cookie（web_session + a1 必填）。F12 → Application → Cookies → xiaohongshu.com 复制全部后保存并「同步凭证」。Cookie 通常 2–7 天失效。',
  BILIBILI: 'Bilibili 需 Cookie（官方 QR 登录）；失效后请重新导入 Cookie 并同步至采集服务。',
}

const platformHint = computed(() => PLATFORM_HINTS[props.platformType] ?? '')

const credentialForm = reactive({
  cookie: '',
  mpToken: '',
  authToken: '',
  appId: '',
  appSecret: '',
  fieldMapping: '',
})

const resetCredentialInputs = () => {
  Object.assign(credentialForm, {
    cookie: '',
    mpToken: '',
    authToken: '',
    appSecret: '',
  })
}

const syncArchivalFields = () => {
  credentialForm.appId = props.accountInfo.appId || ''
  credentialForm.fieldMapping = props.accountInfo.fieldMapping || ''
}

const loadBind = async () => {
  pageLoading.value = true
  try {
    bindInfo.value = await getCollectorBind(props.accountId)
  } catch {
    bindInfo.value = null
  } finally {
    pageLoading.value = false
  }
}

const saveCredentials = async () => {
  savingCredentials.value = true
  try {
    const payload: PlatformAccountUpdateReq = { id: props.accountId }
    if (credentialForm.cookie.trim()) payload.cookie = credentialForm.cookie.trim()
    if (credentialForm.mpToken.trim()) payload.mpToken = credentialForm.mpToken.trim()
    if (credentialForm.authToken.trim()) payload.authToken = credentialForm.authToken.trim()
    if (credentialForm.appSecret.trim()) payload.appSecret = credentialForm.appSecret.trim()
    if (props.platformType === 'KUAISHOU') {
      payload.fieldMapping = credentialForm.fieldMapping.trim() || undefined
    }
    if (props.platformType === 'WECHAT_OFFICIAL' && credentialForm.appId.trim()) {
      payload.appId = credentialForm.appId.trim()
    }
    await updatePlatformAccount(payload)
    ElMessage.success('凭证已保存')
    resetCredentialInputs()
    emit('accountUpdated')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存失败'
    ElMessage.error(msg || '保存凭证失败')
  } finally {
    savingCredentials.value = false
  }
}

const handleBind = async () => {
  binding.value = true
  try {
    bindInfo.value = await bindCollectorAccount(props.accountId)
    ElMessage.success('绑定采集服务成功')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '绑定失败'
    ElMessage.error(msg || '绑定采集服务失败')
  } finally {
    binding.value = false
  }
}

const handleTestConnection = async () => {
  testing.value = true
  try {
    const result = await testCollectorConnection(props.accountId)
    if (result.success) {
      ElMessage.success(result.message || '连接测试成功')
    } else {
      ElMessage.warning(result.message || '连接测试未通过')
    }
    if (result.connStatus && bindInfo.value) {
      bindInfo.value = { ...bindInfo.value, connStatus: result.connStatus }
    }
    await loadBind()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '测试失败'
    ElMessage.error(msg || '测试连接失败')
  } finally {
    testing.value = false
  }
}

const handleSync = async () => {
  syncing.value = true
  try {
    bindInfo.value = await syncCollectorCredentials(props.accountId)
    ElMessage.success('凭证已同步至采集服务')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '同步失败'
    ElMessage.error(msg || '同步凭证失败')
  } finally {
    syncing.value = false
  }
}

const handleBatchImport = async () => {
  try {
    await ElMessageBox.confirm(
      '将扫描租户内凭证齐全且尚未绑定的 Channel-A 账号，并批量导入采集服务。是否继续？',
      '批量绑定',
      { type: 'info', confirmButtonText: '开始绑定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  batchImporting.value = true
  try {
    const result = await batchImportCollectorAccounts()
    ElMessage.success(
      `批量绑定完成：扫描 ${result.scanned}，成功 ${result.imported}，跳过 ${result.skipped}，失败 ${result.failed}`,
    )
    await loadBind()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '批量绑定失败'
    ElMessage.error(msg || '批量绑定失败')
  } finally {
    batchImporting.value = false
  }
}

watch(
  () => props.accountInfo,
  () => syncArchivalFields(),
  { deep: true, immediate: true },
)

onMounted(loadBind)
</script>

<style scoped>
.collect-tab {
  padding-bottom: 8px;
}
.text-muted {
  color: #909399;
}
</style>
