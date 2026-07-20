<!--
  M4 平台账号 — 采集 Tab（ADR-047 · Channel-A 凭证 SSOT）
-->
<template>
  <div class="collect-tab" v-loading="pageLoading">
    <ContentWrap title="采集状态">
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
        title="凭证保存在平台账号（M4 SSOT）。点击「保存凭证」后将自动绑定采集服务并测试连接。"
      />
      <el-form
        :model="credentialForm"
        label-width="130px"
        style="max-width: 900px"
        v-loading="setupInProgress"
        :element-loading-text="setupStatus"
      >
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
          <el-button type="primary" :loading="setupInProgress" @click="saveCredentials">
            保存凭证
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
import { ElMessage } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import DictLabel from '@/components/DictLabel.vue'
import { updatePlatformAccount, type PlatformAccountUpdateReq } from '@/api/platform-account'
import {
  getCollectorBind,
  bindCollectorAccount,
  syncCollectorCredentials,
  testCollectorConnection,
  startCollectorQrLogin,
  pollCollectorQrLogin,
  cancelCollectorQrLogin,
  QR_LOGIN_PLATFORMS,
  type CollectorAccountBindVO,
  type CollectorQrLoginStartResult,
} from '@/api/collector-bind'
import { extractApiErrorMessage } from '@/utils/apiError'

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
const setupInProgress = ref(false)
const setupStatus = ref('')
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
    '抖音可「扫码登录」或粘贴 Cookie（含 sessionid）。采集粉丝/作品需 sec_uid：绑定后 Collector 会尝试从 Cookie 自动获取；若采集报「无法获取 sec_uid」，请在账号档案「平台账号 ID」填入 sec_uid（MS4wLjAB 开头长串，非短数字抖音号）后重新「保存凭证」。',
  WECHAT_VIDEO: '视频号可「扫码登录」或粘贴 Cookie；绑定后使用 WECHAT_CHANNELS_API 采集源建任务。',
  XIAOHONGSHU: '小红书可「扫码登录」或粘贴 Cookie（web_session + a1 必填）。Cookie 通常 2–7 天失效。',
  KUAISHOU:
    '快手可「扫码登录」或粘贴创作者后台 Cookie + Auth Token。Cookie 须含 cp 域 kuaishou.web.cp.api_st。',
  BILIBILI: 'Bilibili 需 Cookie（官方 QR 登录）；失效后请重新导入 Cookie 并保存凭证。',
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

const loadBind = async (silent = false) => {
  if (!silent) {
    pageLoading.value = true
  }
  try {
    bindInfo.value = await getCollectorBind(props.accountId)
  } catch {
    bindInfo.value = null
  } finally {
    if (!silent) {
      pageLoading.value = false
    }
  }
}

const errorMessage = extractApiErrorMessage

const isCollectorBound = () =>
  bindInfo.value?.bindStatus === 'BOUND' && !!bindInfo.value?.collectorAccountId

/** 保存凭证后：绑定或同步采集服务 → 测试连接 */
const runPostSaveSetup = async () => {
  await loadBind(true)
  const alreadyBound = isCollectorBound()

  setupStatus.value = alreadyBound ? '正在同步凭证至采集服务…' : '正在绑定采集服务…'
  try {
    bindInfo.value = alreadyBound
      ? await syncCollectorCredentials(props.accountId)
      : await bindCollectorAccount(props.accountId)
  } catch (e: unknown) {
    const action = alreadyBound ? '同步凭证' : '绑定采集服务'
    ElMessage.warning(`凭证已保存，但${action}失败：${errorMessage(e, `${action}失败`)}`)
    await loadBind(true)
    return
  }

  setupStatus.value = '正在测试连接…'
  try {
    const result = await testCollectorConnection(props.accountId)
    if (result.success) {
      ElMessage.success('凭证已保存，采集服务已绑定，连接测试通过')
    } else {
      ElMessage.warning(`凭证已保存，但连接测试失败：${result.message || '连接未通过'}`)
    }
    if (result.connStatus && bindInfo.value) {
      bindInfo.value = { ...bindInfo.value, connStatus: result.connStatus }
    }
  } catch (e: unknown) {
    ElMessage.warning(`凭证已保存，但连接测试失败：${errorMessage(e, '测试连接失败')}`)
  } finally {
    await loadBind(true)
  }
}

const saveCredentials = async () => {
  setupInProgress.value = true
  setupStatus.value = '正在保存凭证…'
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
    resetCredentialInputs()
    emit('accountUpdated')
    await runPostSaveSetup()
  } catch (e: unknown) {
    ElMessage.error(errorMessage(e, '保存凭证失败'))
  } finally {
    setupInProgress.value = false
    setupStatus.value = ''
  }
}

const stopQrPolling = () => {
  if (qrPollTimer) {
    clearInterval(qrPollTimer)
    qrPollTimer = null
  }
}

const toQrImageSrc = (base64?: string, url?: string) => {
  const directUrl = url?.trim()
  if (directUrl && /^https?:\/\//i.test(directUrl)) return directUrl
  const trimmed = (base64 ?? '').trim()
  if (!trimmed) return ''
  if (trimmed.startsWith('data:image')) return trimmed
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  return `data:image/png;base64,${trimmed}`
}

const resolveQrStartPayload = (result: CollectorQrLoginStartResult) => ({
  sessionId: result.sessionId || result.session_id || '',
  imageSrc: toQrImageSrc(
    result.qrcodeBase64 || result.qrcode_base64,
    result.qrcodeUrl || result.qrcode_url,
  ),
  message: result.message,
  expiresInSeconds: result.expiresInSeconds ?? result.expires_in_seconds,
})

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
    const msg = errorMessage(e, '轮询失败')
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
    const payload = resolveQrStartPayload(result)
    if (!payload.sessionId) {
      throw new Error('未返回扫码会话 ID')
    }
    if (!payload.imageSrc) {
      throw new Error('未返回二维码图片')
    }
    qrSessionId.value = payload.sessionId
    qrImageSrc.value = payload.imageSrc
    qrStatusText.value = payload.message || '请使用手机扫码登录'
    if (payload.expiresInSeconds) {
      qrExpiresHint.value = `二维码约 ${Math.floor(payload.expiresInSeconds / 60)} 分钟内有效`
    }
    startQrPolling()
  } catch (e: unknown) {
    const msg = errorMessage(e, '获取二维码失败')
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
