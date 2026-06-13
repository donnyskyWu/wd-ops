<!--
  内容创作/编辑表单（可嵌入页面或弹窗）
-->
<template>
  <div v-loading="loading" class="content-edit-panel">
    <el-alert
      v-if="isTaskMode && !effectiveReadonly && contentStatus && !canSubmitReview"
      type="warning"
      :closable="false"
      show-icon
      :title="submittedStatusTitle"
      :description="submittedStatusDescription"
      style="margin-bottom: 16px"
    />

    <el-alert
      v-if="isTaskMode && !effectiveReadonly && canSubmitReview && contentStatus === 'COMPLETED'"
      type="info"
      :closable="false"
      show-icon
      title="内容已确认完成"
      description="该内容来自旧版「确认」流程。提交审核将先保存并转入审核流。"
      style="margin-bottom: 16px"
    />

    <el-alert
      v-if="isTaskMode && !effectiveReadonly && canSubmitReview && contentStatus !== 'COMPLETED'"
      type="info"
      :closable="false"
      show-icon
      title="任务驱动创作（模式 B）"
      description="保存为草稿，提交审核后进入与内容管理一致的可配置审核流程。"
      style="margin-bottom: 16px"
    />

    <el-alert
      v-if="isTaskMode && effectiveReadonly && isPendingReview"
      type="info"
      :closable="false"
      show-icon
      title="内容审核中"
      description="当前内容处于待审核状态，仅可查看内容与审核流程，不可编辑或提交。"
      style="margin-bottom: 16px"
    />

    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" :disabled="effectiveReadonly">
      <template v-if="isTaskMode">
        <el-form-item label="IP 组" prop="ipGroupId">
          <el-input :model-value="taskIpGroupDisplay" readonly placeholder="来自任务所属 IP 组" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input :model-value="authorLabel" readonly placeholder="切换 IP 组后自动带出" />
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item label="所属IP组" prop="ipGroupId">
          <el-select
            v-model="formData.ipGroupId"
            placeholder="请选择所属 IP 组"
            style="width: 100%"
            filterable
            clearable
            @change="handleIpGroupChange"
          >
            <el-option
              v-for="item in myIpGroupOptions"
              :key="item.ipGroupId"
              :label="item.ipGroupName"
              :value="item.ipGroupId"
            />
          </el-select>
        </el-form-item>
      </template>

      <el-form-item label="关联赛事" prop="competitionId">
        <div class="competition-picker">
          <el-input :model-value="competitionLabel" readonly placeholder="请选择赛事（单选）" />
          <el-button v-if="!effectiveReadonly" type="primary" plain @click="matchDialogVisible = true">选择赛事</el-button>
          <el-button v-if="!effectiveReadonly && competitionId" link type="danger" @click="clearCompetition">清除</el-button>
        </div>
      </el-form-item>

      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入内容标题" maxlength="200" show-word-limit />
      </el-form-item>

      <el-form-item label="内容类型" prop="contentType">
        <DictSelect
          v-model="formData.contentType"
          dict-type="dict_content_type"
          placeholder="请选择类型"
          @change="handleContentTypeChange"
        />
      </el-form-item>

      <el-form-item v-if="showDocumentType" label="文档类型" prop="documentType">
        <DictSelect v-model="formData.documentType" dict-type="dict_document_type" placeholder="请选择文档类型" />
      </el-form-item>

      <el-form-item v-if="showScriptRef" label="引用文案">
        <el-input
          :model-value="scriptRef?.body || '同赛事暂无已完成的短视频文案'"
          type="textarea"
          :rows="6"
          readonly
        />
      </el-form-item>

      <el-form-item label="是否 AI 生成">
        <el-switch v-model="formData.isAi" />
      </el-form-item>

      <template v-if="!isTaskMode">
        <el-form-item label="平台">
          <DictSelect
            v-model="formData.platformTypes"
            dict-type="dict_platform_type"
            placeholder="请选择平台（可选，可多选）"
            multiple
            clearable
          />
        </el-form-item>
        <el-form-item label="发布账号">
          <AccountSelect
            v-model="formData.accountIds"
            :platform-types="formData.platformTypes"
            :ip-group-id="formData.ipGroupId"
            placeholder="请选择账号（可选，可多选）"
            multiple
          />
        </el-form-item>
      </template>

      <el-form-item v-if="showBody" label="正文" prop="body">
        <el-input v-model="formData.body" type="textarea" :rows="12" placeholder="请输入正文或点击生成" />
      </el-form-item>

      <template v-if="showVideoFields">
        <el-form-item label="生成视频">
          <div v-if="formData.generatedVideoUrl" class="video-preview">
            <el-link :href="formData.generatedVideoUrl" target="_blank" type="primary">
              {{ formData.generatedVideoUrl }}
            </el-link>
          </div>
          <span v-else class="text-muted">点击「生成」后展示 AI 视频 URL（BLK-M2-010 占位）</span>
        </el-form-item>
        <el-form-item label="最终视频">
          <el-input v-model="formData.finalVideoUrl" placeholder="上传或填写视频 URL，留空则使用生成视频" />
        </el-form-item>
      </template>

      <el-form-item v-if="!effectiveReadonly">
        <el-button v-if="formData.isAi && showBody" :loading="aiGenerating" @click="openAiDialog">生成</el-button>
        <el-button v-if="isTaskMode && showVideoFields" :loading="generating" @click="handleGenerate">生成视频</el-button>
        <el-button type="primary" :loading="saving" :disabled="!canEdit" @click="handleSaveDraft">保存</el-button>
        <el-button
          v-if="canSubmitReview"
          type="success"
          :loading="saving"
          @click="handleSubmitReview"
        >
          提交审核
        </el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>

    <el-card v-if="effectiveReadonly && reviewProgress.length" class="review-progress-card" shadow="never">
      <template #header>
        <span>审核流程</span>
      </template>
      <el-steps :active="activeReviewStep" finish-status="success" align-center>
        <el-step
          v-for="step in reviewProgress"
          :key="step.stepKey"
          :title="step.label"
          :status="mapStepStatus(step.stepStatus)"
          :description="formatStepDescription(step)"
        />
      </el-steps>
    </el-card>

    <div v-if="effectiveReadonly" class="readonly-footer">
      <el-button @click="handleCancel">关闭</el-button>
    </div>

    <el-dialog v-model="aiDialogVisible" title="AI 辅助生成" width="680px" append-to-body @open="loadAiDialogOptions">
      <el-form label-width="100px">
        <el-form-item label="AI 模型" required>
          <el-select v-model="aiForm.modelId" placeholder="请选择已启用的 AI 模型" style="width: 100%" filterable>
            <el-option v-for="item in aiModelOptions" :key="item.id" :label="item.modelName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="提示词" required>
          <el-select v-model="aiForm.promptId" placeholder="按内容类型/文档类型匹配" style="width: 100%" filterable>
            <el-option v-for="item in aiPromptOptions" :key="item.id" :label="item.templateName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛事信息">
          <el-input :model-value="aiEventInfoLabel" type="textarea" :rows="2" readonly placeholder="来自内容表单所选赛事" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="aiGenerating" @click="handleAiGenerate">生成</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <MatchSelectDialog v-model:visible="matchDialogVisible" @select="handleMatchSelect" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import DictSelect from '@/components/DictSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import MatchSelectDialog from '@/components/selectors/MatchSelectDialog.vue'
import {
  createContent,
  updateContent,
  submitContentReview,
  getContentByTask,
  getContent,
  getScriptRef,
  generateContent,
  aiGenerateContent,
  getAiPromptOptions,
  getMyIpGroups,
} from '@/api/content'
import { toCompetitionId, toCompetitionName, type MatchVO } from '@/api/match'
import { fetchAiModelList } from '@/api/config'
import { getTaskExecute } from '@/api/task'
import { getAuthorPage } from '@/api/author'
import { fetchUserProfile } from '@/api/system-user'
import { formatDateTime } from '@/utils'

const props = defineProps<{
  contentId?: number
  taskId?: number
  competitionId?: string
  embedded?: boolean
  readonly?: boolean
}>()

const emit = defineEmits<{
  saved: [payload?: { contentId?: number; action: 'draft' | 'review' }]
  cancelled: []
}>()

interface ScriptRef {
  contentId?: number
  title?: string
  body?: string
}

interface ReviewProgressStep {
  stepKey: string
  label: string
  stepStatus: string
  completedAt?: string
  reviewerName?: string
  reviewerRole?: string
  reviewerUsers?: string[]
  reviewerDisplay?: string
  comment?: string
}

const reviewProgress = ref<ReviewProgressStep[]>([])

const activeReviewStep = computed(() => {
  const idx = reviewProgress.value.findIndex((s) => s.stepStatus === 'IN_PROGRESS')
  if (idx >= 0) return idx
  const completed = reviewProgress.value.filter((s) => s.stepStatus === 'COMPLETED').length
  return completed > 0 ? completed : 0
})

const mapStepStatus = (status: string) => {
  if (status === 'REJECTED') return 'error'
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return 'process'
  return 'wait'
}

const formatStepReviewerLabel = (step: ReviewProgressStep) => {
  if (step.stepKey === 'DRAFT' || (step.stepKey === 'PUBLISHED' && step.stepStatus === 'WAITING')) {
    return undefined
  }
  if (step.reviewerDisplay) {
    return `审核人：${step.reviewerDisplay}`
  }
  if (step.reviewerRole && step.reviewerUsers?.length) {
    return `审核人：${step.reviewerRole}：${step.reviewerUsers.join('、')}`
  }
  if (step.reviewerName) {
    return `审核人：${step.reviewerName}`
  }
  if (step.stepStatus === 'IN_PROGRESS' || step.stepStatus === 'WAITING') {
    return '审核人：待审核'
  }
  return undefined
}

const isPendingReview = computed(() => {
  const status = contentStatus.value
  return !!status && status.startsWith('PENDING')
})

const effectiveReadonly = computed(() => props.readonly || isPendingReview.value)

const formatStepDescription = (step: ReviewProgressStep) => {
  const parts: string[] = []
  const reviewer = formatStepReviewerLabel(step)
  if (reviewer) parts.push(reviewer)
  if (step.completedAt) parts.push(formatDateTime(step.completedAt))
  if (step.comment) parts.push(step.comment)
  return parts.length ? parts.join(' · ') : undefined
}

const isTaskMode = computed(() => !!props.taskId)
const contentStatus = ref<string | undefined>()
const canSubmitReview = computed(() => {
  const status = contentStatus.value
  if (!status || status === 'DRAFT' || status === 'REJECTED') return true
  return isTaskMode.value && status === 'COMPLETED'
})
const canEdit = computed(() => {
  const status = contentStatus.value
  if (!status || status === 'DRAFT' || status === 'REJECTED') return true
  return isTaskMode.value && status === 'COMPLETED'
})
const submittedStatusTitle = computed(() => {
  if (contentStatus.value === 'COMPLETED') return '内容已确认完成'
  if (contentStatus.value?.startsWith('PENDING')) return '内容已提交审核'
  return '当前状态不可再次提交'
})
const submittedStatusDescription = computed(() => {
  if (contentStatus.value === 'COMPLETED') {
    return '该内容来自旧版「确认」流程。请先保存修改，再提交审核进入审核流。'
  }
  if (contentStatus.value?.startsWith('PENDING')) {
    return `当前状态为 ${contentStatus.value}，请等待审核结果；驳回后可重新编辑并提交。`
  }
  return `当前状态为 ${contentStatus.value || '未知'}，不可提交审核。`
})
const loading = ref(false)
const saving = ref(false)
const generating = ref(false)
const formRef = ref<FormInstance>()

const competitionId = ref('')
const competitionName = ref('')
const competitionLabel = computed(() => competitionName.value || competitionId.value || '')
const matchDialogVisible = ref(false)
const scriptRef = ref<ScriptRef | null>(null)
const taskIpGroupName = ref('')
const authorLabel = ref('')
const myIpGroupOptions = ref<Array<{ ipGroupId: number; ipGroupName: string; authorId?: number; authorName?: string }>>([])

const taskIpGroupDisplay = computed(() => taskIpGroupName.value || '')

const formData = reactive({
  contentId: undefined as number | undefined,
  title: '',
  platformTypes: [] as string[],
  contentType: undefined as string | undefined,
  documentType: undefined as string | undefined,
  accountIds: [] as number[],
  isAi: false,
  body: '',
  taskId: undefined as number | undefined,
  ipGroupId: undefined as number | undefined,
  authorId: undefined as number | undefined,
  generatedVideoUrl: '',
  finalVideoUrl: '',
  creatorUserId: undefined as number | undefined,
  competitionId: '',
})

const showDocumentType = computed(() => formData.contentType === 'ARTICLE')
const showScriptRef = computed(() => isTaskMode.value && formData.contentType === 'SHORT_VIDEO')
const showBody = computed(() => formData.contentType !== 'SHORT_VIDEO')
const showVideoFields = computed(() => isTaskMode.value && formData.contentType === 'SHORT_VIDEO')

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {
    title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
    contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
    competitionId: [{ required: true, message: '请选择赛事', trigger: 'change' }],
  }
  if (isTaskMode.value) {
    rules.ipGroupId = [{ required: true, message: '请选择 IP 组', trigger: 'change' }]
    if (showDocumentType.value) {
      rules.documentType = [{ required: true, message: '请选择文档类型', trigger: 'change' }]
      rules.body = [{ required: true, message: '请输入正文', trigger: 'blur' }]
    }
  } else {
    rules.ipGroupId = [{ required: true, message: '请选择所属 IP 组', trigger: 'change' }]
    rules.body = [{ required: true, message: '请输入正文', trigger: 'blur' }]
  }
  return rules
})

const aiDialogVisible = ref(false)
const aiForm = reactive({ modelId: undefined as number | undefined, promptId: undefined as number | undefined })
const aiModelOptions = ref<{ id: number; modelName: string }[]>([])
const aiPromptOptions = ref<{ id: number; templateName: string }[]>([])
const aiGenerating = ref(false)
const aiEventInfoLabel = computed(() => competitionLabel.value || '—')

const syncCompetitionToForm = () => {
  formData.competitionId = competitionId.value
}

const resetForm = () => {
  Object.assign(formData, {
    contentId: undefined,
    title: '',
    platformTypes: [],
    contentType: undefined,
    documentType: undefined,
    accountIds: [],
    isAi: false,
    body: '',
    taskId: undefined,
    ipGroupId: undefined,
    authorId: undefined,
    generatedVideoUrl: '',
    finalVideoUrl: '',
    creatorUserId: undefined,
    competitionId: '',
  })
  competitionId.value = ''
  competitionName.value = ''
  authorLabel.value = ''
  taskIpGroupName.value = ''
  myIpGroupOptions.value = []
  scriptRef.value = null
  contentStatus.value = undefined
  reviewProgress.value = []
}

const handleMatchSelect = (match: MatchVO) => {
  competitionId.value = toCompetitionId(match)
  competitionName.value = toCompetitionName(match)
  syncCompetitionToForm()
  loadScriptRef()
}

const clearCompetition = () => {
  competitionId.value = ''
  competitionName.value = ''
  syncCompetitionToForm()
  scriptRef.value = null
}

const loadAuthorForGroup = async (ipGroupId?: number) => {
  if (!ipGroupId) {
    authorLabel.value = ''
    formData.authorId = undefined
    return
  }
  const cached = myIpGroupOptions.value.find((item) => item.ipGroupId === ipGroupId)
  if (cached?.authorId) {
    formData.authorId = cached.authorId
    authorLabel.value = cached.authorName || ''
    return
  }
  try {
    const page = await getAuthorPage({ ipGroupId, status: 1, page: 1, size: 1 })
    const author = page?.list?.[0]
    if (author) {
      formData.authorId = author.id
      authorLabel.value = author.authorName
    } else {
      formData.authorId = undefined
      authorLabel.value = '—'
    }
  } catch {
    authorLabel.value = '—'
  }
}

const handleIpGroupChange = async (ipGroupId?: number) => {
  formData.accountIds = []
  await loadAuthorForGroup(ipGroupId)
}

const loadScriptRef = async () => {
  if (!showScriptRef.value || !competitionId.value) {
    scriptRef.value = null
    return
  }
  try {
    scriptRef.value = await getScriptRef(competitionId.value)
  } catch {
    scriptRef.value = null
  }
}

const handleContentTypeChange = () => {
  if (!showDocumentType.value) {
    formData.documentType = undefined
  }
  loadScriptRef()
}

watch(() => formData.contentType, loadScriptRef)

const buildPayload = () => {
  syncCompetitionToForm()
  return {
    title: formData.title,
    contentType: formData.contentType!,
    platformTypes: formData.platformTypes?.length ? formData.platformTypes : undefined,
    platformType: formData.platformTypes?.[0],
    accountIds: formData.accountIds?.length ? formData.accountIds : undefined,
    accountId: formData.accountIds?.[0],
    body: formData.body || '',
    aiGenerated: formData.isAi ? 1 : 0,
    creatorUserId: formData.creatorUserId!,
    taskId: formData.taskId,
    competitionId: competitionId.value || undefined,
    competitionName: competitionName.value || undefined,
    documentType: formData.documentType,
    ipGroupId: formData.ipGroupId,
    authorId: formData.authorId,
    generatedVideoUrl: formData.generatedVideoUrl || undefined,
    finalVideoUrl: formData.finalVideoUrl || undefined,
  }
}

const persistContent = async (): Promise<number> => {
  const payload = buildPayload()
  if (formData.contentId) {
    await updateContent({ ...payload, id: formData.contentId })
    if (contentStatus.value === 'REJECTED' || contentStatus.value === 'COMPLETED') {
      contentStatus.value = 'DRAFT'
    }
    return formData.contentId
  }
  const id = await createContent(payload)
  formData.contentId = id
  contentStatus.value = 'DRAFT'
  return id
}

const finish = (action: 'draft' | 'review', contentId?: number) => {
  emit('saved', { contentId: contentId ?? formData.contentId, action })
}

const handleSaveDraft = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    const id = await persistContent()
    ElMessage.success('草稿已保存')
    finish('draft', id)
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleGenerate = async () => {
  if (!formData.contentType) {
    ElMessage.warning('请先选择内容类型')
    return
  }
  generating.value = true
  try {
    const id = await persistContent()
    const result = await generateContent(id)
    if (result?.body) formData.body = result.body
    if (result?.generatedVideoUrl) formData.generatedVideoUrl = result.generatedVideoUrl
    formData.isAi = true
    ElMessage.success(result?.message || '生成完成（占位）')
  } catch {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

const handleSubmitReview = async () => {
  if (!canSubmitReview.value) {
    ElMessage.warning(submittedStatusDescription.value)
    return
  }
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    const id = await persistContent()
    await submitContentReview(id)
    contentStatus.value = 'PENDING_FIRST_REVIEW'
    ElMessage.success('已提交审核')
    finish('review', id)
  } catch {
    // request interceptor already shows API error message (e.g. 2010)
  } finally {
    saving.value = false
  }
}

const handleCancel = () => {
  emit('cancelled')
}

const openAiDialog = async () => {
  if (!formData.contentType) {
    ElMessage.warning('请先选择内容类型')
    return
  }
  aiDialogVisible.value = true
}

const loadAiDialogOptions = async () => {
  try {
    const [models, prompts] = await Promise.all([
      fetchAiModelList({ status: 'ENABLED', pageNo: 1, pageSize: 100 }),
      getAiPromptOptions(formData.contentType!, formData.documentType),
    ])
    aiModelOptions.value = (models?.list || []).map((m) => ({ id: m.id, modelName: m.modelName }))
    aiPromptOptions.value = prompts || []
    if (!aiForm.modelId && aiModelOptions.value.length) aiForm.modelId = aiModelOptions.value[0].id
    if (!aiForm.promptId && aiPromptOptions.value.length) aiForm.promptId = aiPromptOptions.value[0].id
  } catch {
    ElMessage.error('加载 AI 配置失败')
  }
}

const handleAiGenerate = async () => {
  if (!aiForm.modelId || !aiForm.promptId) {
    ElMessage.warning('请选择 AI 模型和提示词')
    return
  }
  if (!competitionId.value) {
    ElMessage.warning('请先选择关联赛事')
    return
  }
  aiGenerating.value = true
  try {
    const res = await aiGenerateContent({
      modelId: aiForm.modelId,
      promptId: aiForm.promptId,
      contentType: formData.contentType,
      documentType: formData.documentType,
      competitionId: competitionId.value,
      competitionName: competitionName.value || undefined,
      taskId: props.taskId,
    })
    formData.body = res?.content || ''
    formData.isAi = true
    aiDialogVisible.value = false
    if (res?.mock) {
      ElMessage.info(res.message || '占位生成完成，已写入正文')
    } else {
      ElMessage.success(res?.message || 'AI 生成完成，已写入正文')
    }
  } catch {
    ElMessage.error('AI 生成失败')
  } finally {
    aiGenerating.value = false
  }
}

const applyContentRecord = (existing: Record<string, any>, taskMode = false) => {
  Object.assign(formData, {
    contentId: existing.id,
    title: existing.title,
    contentType: existing.contentType,
    documentType: existing.documentType,
    body: existing.body,
    platformTypes: existing.platformTypes?.length
      ? existing.platformTypes
      : existing.platformType
        ? [existing.platformType]
        : [],
    accountIds: existing.accountIds?.length
      ? existing.accountIds
      : existing.accountId
        ? [existing.accountId]
        : [],
    ipGroupId: taskMode ? formData.ipGroupId : existing.ipGroupId,
    authorId: taskMode ? formData.authorId : existing.authorId,
    generatedVideoUrl: existing.generatedVideoUrl || '',
    finalVideoUrl: existing.finalVideoUrl || '',
    isAi: existing.aiGenerated === 1,
  })
  contentStatus.value = existing.status
  reviewProgress.value = existing.reviewProgress || []
  competitionId.value = existing.competitionId || ''
  competitionName.value = existing.competitionName || ''
  syncCompetitionToForm()
  if (!taskMode) {
    authorLabel.value = existing.authorName || ''
  }
}

const applyTaskIpGroupContext = async (taskCtx: { ipGroupId?: number; ipGroupName?: string }) => {
  if (!taskCtx.ipGroupId) {
    ElMessage.error('任务缺少 IP 组，无法创作内容')
    emit('cancelled')
    return false
  }
  formData.ipGroupId = taskCtx.ipGroupId
  taskIpGroupName.value = taskCtx.ipGroupName || taskIpGroupName.value
  await loadAuthorForGroup(taskCtx.ipGroupId)
  return true
}

const initTaskMode = async () => {
  if (!props.taskId) return
  formData.taskId = props.taskId
  loading.value = true
  try {
    const [taskCtx, profile, existing] = await Promise.all([
      getTaskExecute(props.taskId),
      fetchUserProfile(),
      getContentByTask(props.taskId).catch(() => null),
    ])
    competitionId.value = taskCtx.competitionId || props.competitionId || ''
    competitionName.value = taskCtx.competitionName || ''
    syncCompetitionToForm()
    formData.creatorUserId = profile?.id
    taskIpGroupName.value = taskCtx.ipGroupName || ''
    if (taskCtx.ipGroupId) {
      formData.ipGroupId = taskCtx.ipGroupId
    }
    if (existing) {
      applyContentRecord(existing, true)
    }
    const ok = await applyTaskIpGroupContext(taskCtx)
    if (!ok) return
    await loadScriptRef()
  } catch {
    ElMessage.error('加载任务创作上下文失败')
    emit('cancelled')
  } finally {
    loading.value = false
  }
}

const initNormalMode = async () => {
  loading.value = true
  try {
    const [profile, groups] = await Promise.all([
      fetchUserProfile(),
      getMyIpGroups().catch(() => []),
    ])
    myIpGroupOptions.value = groups || []
    formData.creatorUserId = profile?.id
    if (props.contentId) {
      const existing = await getContent(props.contentId)
      applyContentRecord(existing)
      if (existing.ipGroupId) {
        const inOptions = myIpGroupOptions.value.some((item) => item.ipGroupId === existing.ipGroupId)
        if (!inOptions) {
          myIpGroupOptions.value.push({
            ipGroupId: existing.ipGroupId,
            ipGroupName: existing.ipGroupName || `IP#${existing.ipGroupId}`,
          })
        }
        await loadAuthorForGroup(existing.ipGroupId)
      }
    } else if (props.competitionId) {
      competitionId.value = props.competitionId
      syncCompetitionToForm()
    }
  } catch {
    ElMessage.error('加载内容失败')
    emit('cancelled')
  } finally {
    loading.value = false
  }
}

const load = async () => {
  resetForm()
  if (props.taskId) {
    await initTaskMode()
  } else {
    await initNormalMode()
  }
}

watch(
  () => [props.contentId, props.taskId, props.competitionId] as const,
  () => {
    load()
  },
  { immediate: true },
)
</script>

<style scoped>
.text-muted {
  color: #909399;
  font-size: 13px;
}
.video-preview {
  word-break: break-all;
}
.review-progress-card {
  margin-top: 16px;
}
.readonly-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  text-align: right;
}
.competition-picker {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
.competition-picker .el-input {
  flex: 1;
}
</style>
