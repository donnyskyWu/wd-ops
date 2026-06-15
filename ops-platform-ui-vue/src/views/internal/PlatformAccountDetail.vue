<!--
  M4 - 平台账号详情 / 编辑
  依据: UX-M4 § P-M4-009 / FR-M4-005
  路径: /account/platform/:id
-->
<template>
  <div class="pa-detail-page" v-loading="loading">

    <template v-if="detail">
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 style="margin: 0">
              <el-tag size="default">
                <DictLabel dict-type="dict_platform_type" :value="detail.platformType || detail.platformName" />
              </el-tag>
              {{ detail.accountName }}
              <el-tag size="default" style="margin-left: 8px">
                <DictLabel dict-type="dict_account_status" :value="detail.status" />
              </el-tag>
            </h2>
            <p class="meta">
              <span>IP 组：{{ detail.ipGroupName }}</span>
              <el-divider direction="vertical" />
              <span>粉丝：{{ detail.followerCount.toLocaleString() }}</span>
              <el-divider direction="vertical" />
              <span>总作品：{{ detail.workCount }}</span>
            </p>
          </div>
          <div>
            <el-button @click="router.back()">返回</el-button>
            <el-button type="primary" @click="editMode = !editMode">
              {{ editMode ? '取消编辑' : '编辑' }}
            </el-button>
          </div>
        </div>
      </el-card>

      <ContentWrap :title="editMode ? '编辑账号' : '账号信息'" style="margin-top: 16px">
        <el-form :model="form" label-width="120px" :disabled="!editMode" style="max-width: 900px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="平台">
                <DictLabel v-if="!editMode" dict-type="dict_platform_type" :value="detail.platformType || detail.platformName" />
                <el-input v-else :model-value="platformLabel(detail.platformType || detail.platformName)" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="账号名" required>
                <el-input v-model="form.accountName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="账号 ID">
                <el-input v-model="form.platformAccountId" disabled />
              </el-form-item>
            </el-col>
            <template v-if="showWechatOfficial">
              <el-col :span="12">
                <el-form-item label="原公众号名称">
                  <el-input v-model="form.originalAccountName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属 IP 组" required>
                  <IpGroupTreeSelect v-model="form.ipGroupId" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="商标名称">
                  <el-input v-model="form.trademarkName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="密码">
                  <el-input
                    v-model="form.password"
                    type="password"
                    show-password
                    :placeholder="detail.hasPassword ? '留空则不修改' : '请输入密码'"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="账号类型">
                  <DictLabel v-if="!editMode" dict-type="dict_account_type" :value="form.accountType" />
                  <DictSelect
                    v-else
                    v-model="form.accountType"
                    dict-type="dict_account_type"
                    :include-values="['SUBSCRIPTION_ACCOUNT', 'SERVICE_ACCOUNT']"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="资质类型">
                  <DictLabel v-if="!editMode" dict-type="dict_qualification_type" :value="form.qualificationType" />
                  <DictSelect v-else v-model="form.qualificationType" dict-type="dict_qualification_type" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="使用状态">
                  <DictLabel v-if="!editMode" dict-type="dict_wechat_usage_status" :value="form.usageStatus" />
                  <DictSelect v-else v-model="form.usageStatus" dict-type="dict_wechat_usage_status" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="认证到期时间">
                  <el-date-picker
                    v-if="editMode"
                    v-model="form.certExpiryTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    style="width: 100%"
                  />
                  <span v-else>{{ form.certExpiryTime || '-' }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="认证次数">
                  <span>{{ form.certCount ?? 0 }}</span>
                </el-form-item>
              </el-col>
              <el-col v-if="form.qualificationType === 'ENTERPRISE'" :span="12">
                <el-form-item label="企业名称">
                  <CompanySelect v-if="editMode" v-model="form.companyId" />
                  <span v-else>{{ detail.companyName || '-' }}</span>
                </el-form-item>
              </el-col>
              <el-col v-if="form.qualificationType === 'PERSONAL'" :span="12">
                <el-form-item label="实名人">
                  <RealNameSelect v-if="editMode" v-model="form.realNameId" />
                  <span v-else>{{ detail.realName || '-' }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="关联视频号">
                  <AccountSelect
                    v-if="editMode"
                    v-model="form.linkedVideoAccountId"
                    platform-type="WECHAT_VIDEO"
                    @change="handleLinkedVideoChange"
                  />
                  <span v-else>{{ detail.linkedVideoAccountName || '-' }}</span>
                </el-form-item>
              </el-col>
              <el-col v-if="form.videoAccountRegisteredAt" :span="12">
                <el-form-item label="视频号注册时间">
                  <span>{{ form.videoAccountRegisteredAt }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="管理员">
                  <UserSelect v-if="editMode" v-model="form.adminUserId" @change="handleAdminUserChange" />
                  <span v-else>{{ detail.adminUserName || detail.adminName || '-' }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="管理员手机号">
                  <span>{{ adminPhoneDisplay || '-' }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="管理员身份证">
                  <el-input
                    v-if="editMode"
                    v-model="form.adminIdCard"
                    :placeholder="detail.hasAdminIdCard ? '留空则不修改' : '请输入身份证号'"
                  />
                  <span v-else>{{ detail.hasAdminIdCard ? '已录入' : '-' }}</span>
                </el-form-item>
              </el-col>
            </template>
            <el-col v-if="!showWechatOfficial" :span="12">
              <el-form-item label="所属 IP 组" required>
                <IpGroupTreeSelect v-model="form.ipGroupId" />
              </el-form-item>
            </el-col>
            <template v-if="!showWechatOfficial">
              <el-col :span="12">
                <el-form-item label="归属人">
                  <RealNameSelect v-model="form.realNameId" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机">
                  <PhoneSelect v-model="form.phoneId" />
                </el-form-item>
              </el-col>
            </template>
            <el-col :span="12">
              <el-form-item label="状态">
                <DictLabel v-if="!editMode" dict-type="dict_account_status" :value="form.status" />
                <DictSelect v-else v-model="form.status" dict-type="dict_account_status" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="登录方式">
                <el-select v-model="form.loginMethod" style="width: 100%">
                  <el-option label="手机号" value="phone" />
                  <el-option label="微信扫码" value="wechat" />
                  <el-option label="账号密码" value="password" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="2" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item v-if="editMode">
            <el-button @click="editMode = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
          </el-form-item>
        </el-form>
      </ContentWrap>

      <ContentWrap
        v-if="showWechatOfficial"
        title="续费认证记录"
        style="margin-top: 16px"
      >
        <div class="fan-group-toolbar">
          <el-button type="primary" size="small" @click="openRenewalDialog()">
            新增续费记录
          </el-button>
        </div>
        <el-table :data="renewalRecords" v-loading="renewalLoading" border stripe empty-text="暂无续费记录">
          <el-table-column prop="renewalTime" label="续费时间" width="170" />
          <el-table-column prop="renewerName" label="续费人" width="120" />
          <el-table-column prop="renewalAmount" label="续费金额(元)" width="120" align="right">
            <template #default="{ row }">{{ row.renewalAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="录入时间" width="170" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeRenewal(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </ContentWrap>

      <ContentWrap
        v-if="showFanGroups"
        title="粉丝群管理"
        style="margin-top: 16px"
      >
        <div class="fan-group-toolbar">
          <el-button type="primary" size="small" @click="openFanGroupDialog()">
            新增粉丝群
          </el-button>
        </div>
        <el-table :data="fanGroups" v-loading="fanGroupLoading" border stripe empty-text="暂无粉丝群">
          <el-table-column prop="groupName" label="粉丝群名称" min-width="160" />
          <el-table-column prop="memberCount" label="粉丝群人数" width="120" align="right" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openFanGroupDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeFanGroup(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </ContentWrap>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="12">
          <ContentWrap title="30 天粉丝趋势">
            <div ref="followerChartRef" style="width: 100%; height: 280px" />
          </ContentWrap>
        </el-col>
        <el-col :span="12">
          <ContentWrap title="内容产出(近 30 天)">
            <div ref="contentChartRef" style="width: 100%; height: 280px" />
          </ContentWrap>
        </el-col>
      </el-row>
    </template>
    <el-empty v-else-if="loadError" :description="loadError">
      <el-button type="primary" @click="loadDetail">重试</el-button>
    </el-empty>

    <el-dialog v-model="renewalDialogVisible" title="新增续费认证记录" width="480px" destroy-on-close>
      <el-form :model="renewalForm" ref="renewalFormRef" :rules="renewalRules" label-width="100px">
        <el-form-item label="续费时间" prop="renewalTime">
          <el-date-picker
            v-model="renewalForm.renewalTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="续费人">
          <UserSelect v-model="renewalForm.renewerUserId" />
        </el-form-item>
        <el-form-item label="续费金额" prop="renewalAmount">
          <el-input-number v-model="renewalForm.renewalAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="renewalSubmitting" @click="submitRenewal">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fanGroupDialogVisible" :title="fanGroupDialogTitle" width="480px" destroy-on-close>
      <el-form :model="fanGroupForm" ref="fanGroupFormRef" :rules="fanGroupRules" label-width="100px">
        <el-form-item label="群名称" prop="groupName">
          <el-input v-model="fanGroupForm.groupName" maxlength="100" placeholder="请输入粉丝群名称" />
        </el-form-item>
        <el-form-item label="群人数" prop="memberCount">
          <el-input-number v-model="fanGroupForm.memberCount" :min="0" :max="9999999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fanGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fanGroupSubmitting" @click="submitFanGroup">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import RealNameSelect from '@/components/selectors/RealNameSelect.vue'
import PhoneSelect from '@/components/selectors/PhoneSelect.vue'
import CompanySelect from '@/components/selectors/CompanySelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import {
  getPlatformAccountDetail,
  updatePlatformAccount as updatePlatformAccountV2,
  getFollowerTrend,
  getContentTrend,
  getPlatformAccountFanGroups,
  createPlatformAccountFanGroup,
  updatePlatformAccountFanGroup,
  deletePlatformAccountFanGroup,
  type FanGroupVO,
} from '@/api/account'
import {
  getWechatCertRenewals,
  createWechatCertRenewal,
  deleteWechatCertRenewal,
  getPlatformAccount,
  type WechatCertRenewalVO,
  type PlatformAccountVO,
} from '@/api/platform-account'
import { PLATFORM_LABEL, type PlatformType } from '@/utils/enum-alias'
import { isAccountBindingConflict, promptAccountForceReplace } from '@/utils/account-binding-conflict'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const editMode = ref(false)
const submitting = ref(false)
const detail = ref<any>(null)
const form = reactive<any>({})
const adminPhoneDisplay = ref('')
const followerChartRef = ref<HTMLDivElement | null>(null)
const contentChartRef = ref<HTMLDivElement | null>(null)
let followerChart: echarts.ECharts | null = null
let contentChart: echarts.ECharts | null = null

const fanGroups = ref<FanGroupVO[]>([])
const fanGroupLoading = ref(false)
const fanGroupDialogVisible = ref(false)
const fanGroupDialogTitle = ref('新增粉丝群')
const fanGroupSubmitting = ref(false)
const fanGroupFormRef = ref<any>()
const fanGroupForm = reactive({
  id: undefined as number | undefined,
  groupName: '',
  memberCount: 0,
})
const fanGroupRules = {
  groupName: [{ required: true, message: '请输入粉丝群名称', trigger: 'blur' }],
  memberCount: [{ required: true, message: '请输入粉丝群人数', trigger: 'change' }],
}

const showFanGroups = computed(() => {
  const platform = detail.value?.platformType || detail.value?.platformName
  return platform === 'DOUYIN' || platform === 'KUAISHOU'
})

const showWechatOfficial = computed(() => {
  const platform = detail.value?.platformType || detail.value?.platformName
  return platform === 'WECHAT_OFFICIAL'
})

const renewalRecords = ref<WechatCertRenewalVO[]>([])
const renewalLoading = ref(false)
const renewalDialogVisible = ref(false)
const renewalSubmitting = ref(false)
const renewalFormRef = ref<any>()
const renewalForm = reactive({
  renewalTime: '',
  renewerUserId: undefined as number | undefined,
  renewalAmount: 300,
})
const renewalRules = {
  renewalTime: [{ required: true, message: '请选择续费时间', trigger: 'change' }],
  renewalAmount: [{ required: true, message: '请输入续费金额', trigger: 'change' }],
}

const initFollowerChart = (dates: string[] = [], vals: number[] = []) => {
  if (!followerChartRef.value) return
  const el = followerChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(() => initFollowerChart(dates, vals), 100)
    return
  }
  if (!followerChart) followerChart = echarts.init(el)
  const xs = dates.length > 0 ? dates : Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const data = vals.length > 0 ? vals : xs.map(() => 0)
  followerChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 16, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: xs, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data, itemStyle: { color: '#409eff' } }],
  })
}
const initContentChart = (dates: string[] = [], vals: number[] = []) => {
  if (!contentChartRef.value) return
  const el = contentChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(() => initContentChart(dates, vals), 100)
    return
  }
  if (!contentChart) contentChart = echarts.init(el)
  const xs = dates.length > 0 ? dates : Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const data = vals.length > 0 ? vals : xs.map(() => 0)
  contentChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 16, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: xs, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data, itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] } }],
  })
}

const platformLabel = (platform: string) =>
  PLATFORM_LABEL[platform as PlatformType] || platform || '-'

const resolveVideoRegisteredAt = (account: PlatformAccountVO): string | undefined =>
  account.createTime || account.linkedAt || undefined

const handleLinkedVideoChange = async (videoAccountId: number | undefined) => {
  if (!videoAccountId) {
    form.videoAccountRegisteredAt = undefined
    return
  }
  try {
    const video = await getPlatformAccount(videoAccountId)
    form.videoAccountRegisteredAt = resolveVideoRegisteredAt(video)
  } catch {
    form.videoAccountRegisteredAt = undefined
  }
}

const handleAdminUserChange = (_userId: number | undefined, item?: { phoneMasked?: string }) => {
  adminPhoneDisplay.value = item?.phoneMasked || ''
}

const loadRenewals = async () => {
  if (!detail.value?.id || !showWechatOfficial.value) {
    renewalRecords.value = []
    return
  }
  renewalLoading.value = true
  try {
    renewalRecords.value = await getWechatCertRenewals(detail.value.id)
  } catch {
    renewalRecords.value = []
  } finally {
    renewalLoading.value = false
  }
}

const openRenewalDialog = () => {
  Object.assign(renewalForm, {
    renewalTime: '',
    renewerUserId: undefined,
    renewalAmount: 300,
  })
  renewalDialogVisible.value = true
}

const submitRenewal = async () => {
  if (!renewalFormRef.value || !detail.value?.id) return
  await renewalFormRef.value.validate()
  renewalSubmitting.value = true
  try {
    await createWechatCertRenewal({
      accountId: detail.value.id,
      renewalTime: renewalForm.renewalTime,
      renewerUserId: renewalForm.renewerUserId,
      renewalAmount: renewalForm.renewalAmount,
    })
    ElMessage.success('续费记录已添加，认证次数已更新')
    renewalDialogVisible.value = false
    await loadDetail()
  } finally {
    renewalSubmitting.value = false
  }
}

const removeRenewal = async (row: WechatCertRenewalVO) => {
  await ElMessageBox.confirm('确定删除该续费记录？（不会回退认证次数）', '提示', { type: 'warning' })
  await deleteWechatCertRenewal(row.id)
  ElMessage.success('删除成功')
  await loadRenewals()
}

const loadFanGroups = async () => {
  if (!detail.value?.id || !showFanGroups.value) {
    fanGroups.value = []
    return
  }
  fanGroupLoading.value = true
  try {
    fanGroups.value = await getPlatformAccountFanGroups(detail.value.id)
  } catch {
    fanGroups.value = []
  } finally {
    fanGroupLoading.value = false
  }
}

const openFanGroupDialog = (row?: FanGroupVO) => {
  if (row) {
    fanGroupDialogTitle.value = '编辑粉丝群'
    Object.assign(fanGroupForm, { id: row.id, groupName: row.groupName, memberCount: row.memberCount })
  } else {
    fanGroupDialogTitle.value = '新增粉丝群'
    Object.assign(fanGroupForm, { id: undefined, groupName: '', memberCount: 0 })
  }
  fanGroupDialogVisible.value = true
}

const submitFanGroup = async () => {
  if (!fanGroupFormRef.value || !detail.value?.id) return
  await fanGroupFormRef.value.validate()
  fanGroupSubmitting.value = true
  try {
    if (fanGroupForm.id) {
      await updatePlatformAccountFanGroup({
        id: fanGroupForm.id,
        groupName: fanGroupForm.groupName.trim(),
        memberCount: fanGroupForm.memberCount,
      })
    } else {
      await createPlatformAccountFanGroup({
        accountId: detail.value.id,
        groupName: fanGroupForm.groupName.trim(),
        memberCount: fanGroupForm.memberCount,
      })
    }
    ElMessage.success('保存成功')
    fanGroupDialogVisible.value = false
    await loadFanGroups()
  } finally {
    fanGroupSubmitting.value = false
  }
}

const removeFanGroup = async (row: FanGroupVO) => {
  await ElMessageBox.confirm(`确定删除粉丝群「${row.groupName}」？`, '提示', { type: 'warning' })
  await deletePlatformAccountFanGroup(row.id)
  ElMessage.success('删除成功')
  await loadFanGroups()
}

const loadDetail = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const id = Number(route.params.id)
    const a: any = await getPlatformAccountDetail(id)
    detail.value = {
      ...a,
      platformName: a.platformType || a.platformName,
      platformAccountId: a.externalAccountId,
      ipGroupName: a.ipGroupName || (a.ipGroupId ? `IP组#${a.ipGroupId}` : '-'),
      followerCount: a.followerCount ?? 0,
      workCount: a.workCount ?? 0,
    }
    Object.assign(form, {
      accountName: a.accountName,
      platformAccountId: a.externalAccountId,
      ipGroupId: a.ipGroupId,
      realNameId: a.realnameId,
      phoneId: a.phoneId,
      status: a.status,
      accountType: a.accountType,
      trademarkName: a.trademarkName || '',
      email: a.email || '',
      password: '',
      qualificationType: a.qualificationType || 'ENTERPRISE',
      usageStatus: a.usageStatus || 'REGISTERED',
      originalAccountName: a.originalAccountName || '',
      certExpiryTime: a.certExpiryTime,
      certCount: a.certCount ?? 0,
      companyId: a.companyId,
      linkedVideoAccountId: a.linkedVideoAccountId,
      videoAccountRegisteredAt: a.videoAccountRegisteredAt,
      adminUserId: a.adminUserId,
      adminIdCard: '',
    })
    adminPhoneDisplay.value = a.adminPhoneMasked || ''
    // 趋势图：尝试调 getFollowerTrend/getContentTrend
    try {
      const fts: any = await getFollowerTrend(id)
      const dates = (fts || []).map((p: any) => p.date || p.day || '')
      const vals = (fts || []).map((p: any) => p.followerCount || p.value || 0)
      initFollowerChart(dates, vals)
    } catch {
      initFollowerChart()
    }
    try {
      const cts: any = await getContentTrend(id)
      const dates = (cts || []).map((p: any) => p.date || p.day || '')
      const vals = (cts || []).map((p: any) => p.workCount || p.value || 0)
      initContentChart(dates, vals)
    } catch {
      initContentChart()
    }
    await loadFanGroups()
    await loadRenewals()
  } catch (e: any) {
    detail.value = null
    loadError.value = e?.message?.includes('403') || e?.message?.includes('无权限')
      ? '无权限查看该账号，请联系管理员'
      : '账号详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
const submitWithForceReplace = async (forceReplace: boolean, reason?: string) => {
  const payload: Record<string, unknown> = {
    id: detail.value.id,
    accountName: form.accountName,
    ipGroupId: form.ipGroupId,
    realnameId: form.realNameId,
    phoneId: form.phoneId,
    status: form.status,
    forceReplace,
    reason,
  }
  if (showWechatOfficial.value) {
    Object.assign(payload, {
      accountType: form.accountType,
      trademarkName: form.trademarkName || undefined,
      email: form.email || undefined,
      password: form.password || undefined,
      qualificationType: form.qualificationType,
      usageStatus: form.usageStatus,
      originalAccountName: form.originalAccountName || undefined,
      certExpiryTime: form.certExpiryTime || undefined,
      companyId: form.qualificationType === 'ENTERPRISE' ? form.companyId : undefined,
      linkedVideoAccountId: form.linkedVideoAccountId,
      adminUserId: form.adminUserId,
      adminIdCard: form.adminIdCard || undefined,
    })
    if (form.qualificationType === 'PERSONAL') {
      payload.realnameId = form.realNameId
    }
  }
  await updatePlatformAccountV2(payload)
}

const submit = async () => {
  if (!detail.value?.id) return
  submitting.value = true
  try {
    await submitWithForceReplace(false)
    await loadDetail()
    editMode.value = false
    ElMessage.success('保存成功')
  } catch (err: unknown) {
    if (isAccountBindingConflict(err)) {
      const ok = await promptAccountForceReplace(err, (reason) => submitWithForceReplace(true, reason))
      if (ok) {
        await loadDetail()
        editMode.value = false
        ElMessage.success('保存成功（已强制替换）')
      }
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.pa-detail-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-start; }
.meta { color: #909399; font-size: 13px; margin: 8px 0 0 0; }
.fan-group-toolbar { margin-bottom: 12px; }
</style>
