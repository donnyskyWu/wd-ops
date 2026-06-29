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
          <el-divider content-position="left">开放平台（已认证号采集）</el-divider>
          <el-form-item label="AppId">
            <el-input v-model="credentialForm.appId" placeholder="已认证公众号必填，用于 Open API 粉丝/统计" />
          </el-form-item>
          <el-form-item label="AppSecret">
            <el-input
              v-model="credentialForm.appSecret"
              type="password"
              show-password
              :placeholder="accountInfo.hasAppSecret ? '已配置，留空则不修改' : '已认证公众号必填（AES 加密存储）'"
            />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button
            v-if="supportsQrLogin"
            type="success"
            plain
            :loading="qrStarting"
            @click="openQrLogin"
          >
            扫码登录
          </el-button>
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

    <el-dialog
      v-model="qrDialogVisible"
      title="扫码登录"
      width="420px"
      :close-on-click-modal="false"
      @closed="handleQrDialogClosed"
    >
      <div v-loading="qrStarting" class="qr-dialog-body">
        <p class="qr-status">{{ qrStatusText }}</p>
        <div v-if="qrImageSrc" class="qr-image-wrap">
          <img :src="qrImageSrc" alt="登录二维码" class="qr-image" />
        </div>
        <el-alert
          v-if="qrServerWarning"
          type="warning"
          :closable="false"
          show-icon
          style="margin-top: 12px"
          :title="qrServerWarning"
        />
        <p v-if="qrExpiresHint" class="qr-expires">{{ qrExpiresHint }}</p>
      </div>
      <template #footer>
        <el-button @click="qrDialogVisible = false">关闭</el-button>
        <el-button type="primary" plain :loading="qrStarting" @click="restartQrLogin">
          刷新二维码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
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
  startCollectorQrLogin,
  pollCollectorQrLogin,
  cancelCollectorQrLogin,
  QR_LOGIN_PLATFORMS,
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

const supportsQrLogin = computed(() => QR_LOGIN_PLATFORMS.has(props.platformType))

const qrDialogVisible = ref(false)
const qrStarting = ref(false)
const qrSessionId = ref('')
const qrImageSrc = ref('')
const qrStatusText = ref('正在获取二维码…')
const qrExpiresHint = ref('')
let qrPollTimer: ReturnType<typeof setInterval> | null = null

const QR_SERVER_WARNINGS: Partial<Record<string, string>> = {
  DOUYIN:
    '抖音/快手在 Docker 或云服务器上可能因 CDN 风控无法显示二维码；若失败请改用 Cookie 粘贴或本地 tools/local_qr_login.py。',
  KUAISHOU:
    '快手需创作者后台(cp.kuaishou.com)登录态；服务端 QR 可能受 CDN 限制，失败时请手动粘贴 Cookie。',
}

const qrServerWarning = computed(() => QR_SERVER_WARNINGS[props.platformType] ?? '')

const PLATFORM_HINTS: Record<string, string> = {
  WECHAT_OFFICIAL:
    '公众号可「扫码登录」或手动粘贴 Cookie + MP Token。使用状态为「认证/续费」且配置 AppId+AppSecret 时，粉丝与统计走官方 Open API；图文列表/互动仍依赖 Cookie。扫码需使用已绑定该公众号管理员的微信。',
  DOUYIN:
    '抖音可「扫码登录」或粘贴 Cookie（含 sessionid）。采集粉丝/作品需 sec_uid：绑定后 Collector 会尝试从 Cookie 自动获取；若采集报「无法获取 sec_uid」，请在账号档案「平台账号 ID」填入 sec_uid（MS4wLjAB 开头长串，非短数字抖音号）后重新「同步凭证」。',
  WECHAT_VIDEO: '视频号可「扫码登录」或粘贴 Cookie；绑定后使用 WECHAT_CHANNELS_API 采集源建任务。',
  XIAOHONGSHU: '小红书可「扫码登录」或粘贴 Cookie（web_session + a1 必填）。Cookie 通常 2–7 天失效。',
  KUAISHOU:
    '快手可「扫码登录」或粘贴创作者后台 Cookie + Auth Token。Cookie 须含 cp 域 kuaishou.web.cp.api_st。',
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

const stopQrPolling = () => {
  if (qrPollTimer) {
    clearInterval(qrPollTimer)
    qrPollTimer = null
  }
}

const toQrImageSrc = (base64: string) => {
  const trimmed = base64.trim()
  if (trimmed.startsWith('data:image')) return trimmed
  return `data:image/png;base64,${trimmed}`
}

const pollQrOnce = async () => {
  if (!qrSessionId.value) return
  try {
    const result = await pollCollectorQrLogin(props.accountId, qrSessionId.value)
    qrStatusText.value = result.message || result.status
    if (result.status === 'scanned') {
      qrStatusText.value = '已扫码，请在手机上确认登录'
    }
    if (result.status === 'confirmed') {
      stopQrPolling()
      ElMessage.success(result.message || '扫码登录成功')
      qrDialogVisible.value = false
      resetCredentialInputs()
      emit('accountUpdated')
      await loadBind()
      return
    }
    if (result.status === 'expired' || result.status === 'error') {
      stopQrPolling()
      qrStatusText.value = result.message || '二维码已失效，请刷新重试'
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '轮询失败'
    qrStatusText.value = msg
  }
}

const startQrPolling = () => {
  stopQrPolling()
  qrPollTimer = setInterval(() => {
    void pollQrOnce()
  }, 2000)
}

const beginQrSession = async () => {
  qrStarting.value = true
  qrImageSrc.value = ''
  qrStatusText.value = '正在获取二维码…'
  try {
    const result = await startCollectorQrLogin(props.accountId)
    qrSessionId.value = result.sessionId
    qrImageSrc.value = toQrImageSrc(result.qrcodeBase64)
    qrStatusText.value = result.message || '请使用手机扫码登录'
    if (result.expiresInSeconds) {
      qrExpiresHint.value = `二维码约 ${Math.floor(result.expiresInSeconds / 60)} 分钟内有效`
    }
    startQrPolling()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '获取二维码失败'
    qrStatusText.value = msg
    ElMessage.error(msg)
  } finally {
    qrStarting.value = false
  }
}

const openQrLogin = async () => {
  qrDialogVisible.value = true
  await beginQrSession()
}

const restartQrLogin = async () => {
  if (qrSessionId.value) {
    try {
      await cancelCollectorQrLogin(props.accountId, qrSessionId.value)
    } catch {
      // ignore cancel errors
    }
  }
  stopQrPolling()
  qrSessionId.value = ''
  await beginQrSession()
}

const handleQrDialogClosed = () => {
  stopQrPolling()
  if (qrSessionId.value) {
    void cancelCollectorQrLogin(props.accountId, qrSessionId.value).catch(() => undefined)
  }
  qrSessionId.value = ''
  qrImageSrc.value = ''
  qrExpiresHint.value = ''
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
onBeforeUnmount(stopQrPolling)
</script>

<style scoped>
.collect-tab {
  padding-bottom: 8px;
}
.text-muted {
  color: #909399;
}
.qr-dialog-body {
  text-align: center;
}
.qr-status {
  margin: 0 0 12px;
  color: #606266;
}
.qr-image-wrap {
  display: flex;
  justify-content: center;
}
.qr-image {
  width: 240px;
  height: 240px;
  object-fit: contain;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.qr-expires {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
