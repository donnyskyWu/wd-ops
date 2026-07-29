<!--
  内容创作/编辑表单（可嵌入页面或弹窗）
-->
<template>
  <div v-loading="loading" class="content-edit-panel ops-embedded-panel">
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
          <IpGroupTreeSelect
            v-model="formData.ipGroupId"
            placeholder="请选择所属 IP 组"
            @change="handleIpGroupChange"
          />
        </el-form-item>
        <el-form-item v-if="formData.ipGroupId" label="作者">
          <el-input :model-value="authorLabel" readonly placeholder="切换 IP 组后自动带出" />
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

      <el-form-item v-if="showAiTextGenerate" label="方案分析类型" prop="schemeTypes">
        <DictSelect
          v-model="formData.schemeTypes"
          dict-type="dict_scheme_type"
          multiple
          placeholder="请选择方案分析类型（可多选）"
        />
      </el-form-item>

      <el-form-item v-if="showPriceField" label="售价">
        <div class="price-field">
          <el-input-number
            v-model="formData.price"
            :min="0"
            :step="1"
            :disabled="effectiveReadonly"
            controls-position="right"
          />
          <div v-if="!effectiveReadonly" class="price-presets">
            <el-button
              v-for="preset in PRICE_PRESETS"
              :key="preset"
              size="small"
              link
              type="primary"
              @click="formData.price = preset"
            >
              {{ preset }}
            </el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item v-if="showScriptRef" label="引用文案">
        <el-input
          :model-value="scriptRef?.body || '同赛事暂无已完成的短视频文案'"
          type="textarea"
          :rows="6"
          readonly
        />
      </el-form-item>

      <el-form-item v-if="showArticleLayout && !effectiveReadonly" label="版式模板">
        <el-button type="primary" plain @click="templateDialogVisible = true">选择并应用模板</el-button>
        <span v-if="formData.layoutTemplateId" class="text-muted">已关联模板 #{{ formData.layoutTemplateId }}</span>
      </el-form-item>

      <template v-if="showArticleLayout">
        <template v-if="effectiveReadonly">
          <el-form-item label="付费内容">
            <LayoutViewer
              v-if="!loading"
              :key="`${formData.contentId ?? 'new'}-paid`"
              :html="displayRichHtml"
            />
          </el-form-item>
          <el-form-item label="免费内容">
            <LayoutViewer
              v-if="!loading"
              :key="`${formData.contentId ?? 'new'}-free`"
              :html="displayFreeHtml"
            />
          </el-form-item>
        </template>
        <template v-else>
        <el-form-item v-if="showAiTextGenerate" label="方案正文">
          <el-tabs v-model="bodyContentTab" class="body-content-tabs">
            <el-tab-pane label="付费内容" name="paid" />
            <el-tab-pane label="免费内容" name="free" />
          </el-tabs>
        </el-form-item>
        <div v-show="bodyContentTab === 'paid'" :class="['article-edit-shell', { 'is-maximized': editorMaximized }]">
          <div v-if="!effectiveReadonly" class="article-edit-toolbar">
            <span v-if="editorMaximized" class="article-edit-maximized-title">正文编辑（全屏）</span>
            <div class="article-edit-toolbar-actions">
              <el-button
                v-if="!editorMaximized"
                size="small"
                :icon="FullScreen"
                @click="toggleEditorMaximize"
              >
                最大化
              </el-button>
              <el-button
                v-if="editorMaximized"
                size="small"
                :icon="CloseBold"
                @click="editorMaximized = false"
              >
                还原
              </el-button>
              <el-button
                v-if="!editorMaximized"
                size="small"
                type="primary"
                plain
                @click="openQuickTypesetDialog"
              >
                一键排版
              </el-button>
              <el-button
                v-if="!editorMaximized"
                size="small"
                :icon="layoutPanelCollapsed ? Expand : Fold"
                @click="layoutPanelCollapsed = !layoutPanelCollapsed"
              >
                {{ layoutPanelCollapsed ? '展开版式工作台' : '收起版式工作台' }}
              </el-button>
            </div>
          </div>
          <el-row :gutter="16" class="article-edit-row">
            <el-col
              :xs="24"
              :md="effectiveReadonly || layoutPanelCollapsed || editorMaximized ? 24 : 14"
            >
              <el-form-item label="付费内容" prop="body">
                <RichTextEditor
                  v-if="!effectiveReadonly"
                  ref="richEditorRef"
                  v-model="richBodyHtml"
                  :placeholder="editorMaximized ? '全屏编辑模式，按 Esc 可还原' : '请输入正文，支持富文本排版'"
                  :min-height="editorMaximized ? '0' : EDITOR_FRAME_MIN"
                  :fill-height="editorMaximized"
                />
                <LayoutViewer v-else :html="displayRichHtml" />
              </el-form-item>
            </el-col>
            <el-col v-if="!effectiveReadonly && !layoutPanelCollapsed && !editorMaximized" :xs="24" :md="10">
              <LayoutResourceSidebar
                :document-type="formData.documentType"
                :body="sidebarBodyText"
                :html="richBodyHtml"
                :existing-layout-json="formData.layoutJson"
                :insert-html="insertHtmlAtCursor"
                :apply-style-to-selection="applyStyleToSelection"
                @template-applied="handleSidebarTemplateApplied"
                @typeset-applied="handleSidebarTypesetApplied"
              />
              <el-form-item v-if="formData.bodyFormat === 'LAYOUT'" label="版式结构" class="layout-structure-item">
                <LayoutEditor v-model="formData.layoutJson" :show-preview="false" />
              </el-form-item>
            </el-col>
          </el-row>
          <div v-if="editorMaximized && !effectiveReadonly" class="article-edit-maximized-footer">
            <el-button type="primary" :loading="saving" :disabled="!canEdit" @click="handleSaveDraft">保存</el-button>
            <el-button @click="editorMaximized = false">还原</el-button>
          </div>
        </div>
        <div v-show="bodyContentTab === 'free'" :class="['article-edit-shell', { 'is-maximized': freeEditorMaximized }]">
          <div v-if="!effectiveReadonly" class="article-edit-toolbar">
            <span v-if="freeEditorMaximized" class="article-edit-maximized-title">免费内容编辑（全屏）</span>
            <div class="article-edit-toolbar-actions">
              <el-button
                v-if="!freeEditorMaximized"
                size="small"
                :icon="FullScreen"
                @click="toggleFreeEditorMaximize"
              >
                最大化
              </el-button>
              <el-button
                v-if="freeEditorMaximized"
                size="small"
                :icon="CloseBold"
                @click="freeEditorMaximized = false"
              >
                还原
              </el-button>
              <el-button
                v-if="!freeEditorMaximized"
                size="small"
                type="primary"
                plain
                @click="openQuickTypesetDialog"
              >
                一键排版
              </el-button>
              <el-button
                v-if="!freeEditorMaximized"
                size="small"
                :icon="freeLayoutPanelCollapsed ? Expand : Fold"
                @click="freeLayoutPanelCollapsed = !freeLayoutPanelCollapsed"
              >
                {{ freeLayoutPanelCollapsed ? '展开版式工作台' : '收起版式工作台' }}
              </el-button>
            </div>
          </div>
          <el-row :gutter="16" class="article-edit-row">
            <el-col
              :xs="24"
              :md="effectiveReadonly || freeLayoutPanelCollapsed || freeEditorMaximized ? 24 : 14"
            >
              <el-form-item label="免费内容">
                <RichTextEditor
                  v-if="!effectiveReadonly"
                  ref="freeEditorRef"
                  v-model="freeBodyHtml"
                  :placeholder="freeEditorMaximized ? '全屏编辑模式，按 Esc 可还原' : '请输入免费内容（预览/引流文案，可选）'"
                  :min-height="freeEditorMaximized ? '0' : EDITOR_FRAME_MIN"
                  :fill-height="freeEditorMaximized"
                />
                <LayoutViewer v-else :html="displayFreeHtml" />
              </el-form-item>
            </el-col>
            <el-col v-if="!effectiveReadonly && !freeLayoutPanelCollapsed && !freeEditorMaximized" :xs="24" :md="10">
              <LayoutResourceSidebar
                :document-type="formData.documentType"
                :body="sidebarFreeBodyText"
                :html="freeBodyHtml"
                :existing-layout-json="freeLayoutJson"
                :insert-html="insertHtmlAtCursorFree"
                :apply-style-to-selection="applyStyleToSelectionFree"
                @template-applied="handleFreeSidebarTemplateApplied"
                @typeset-applied="handleFreeSidebarTypesetApplied"
              />
            </el-col>
          </el-row>
          <div v-if="freeEditorMaximized && !effectiveReadonly" class="article-edit-maximized-footer">
            <el-button type="primary" :loading="saving" :disabled="!canEdit" @click="handleSaveDraft">保存</el-button>
            <el-button @click="freeEditorMaximized = false">还原</el-button>
          </div>
        </div>
        </template>
      </template>

      <template v-else-if="showBody">
        <el-form-item v-if="effectiveReadonly" label="内容">
          <LayoutViewer
            v-if="!loading"
            :key="formData.contentId ?? 'new'"
            :html="displayCombinedHtml"
          />
        </el-form-item>
        <template v-else>
        <el-form-item v-if="showAiTextGenerate" label="方案正文">
          <el-tabs v-model="bodyContentTab" class="body-content-tabs">
            <el-tab-pane label="付费内容" name="paid" />
            <el-tab-pane label="免费内容" name="free" />
          </el-tabs>
        </el-form-item>
        <el-form-item v-show="bodyContentTab === 'paid'" label="付费内容" prop="body">
          <RichTextEditor
            v-model="richBodyHtml"
            placeholder="请输入付费内容，支持富文本排版"
            :min-height="EDITOR_FRAME_MIN"
          />
        </el-form-item>
        <el-form-item v-show="bodyContentTab === 'free'" label="免费内容">
          <RichTextEditor
            v-model="freeBodyHtml"
            placeholder="请输入免费内容（预览/引流文案，可选）"
            :min-height="EDITOR_FRAME_MIN"
          />
        </el-form-item>
        </template>
      </template>

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
        <el-button v-if="showAiTextGenerate" @click="openAiContentDrawer">AI生成</el-button>
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

    <AiContentDrawer
      v-model="aiContentDrawerVisible"
      :context="aiDrawerContext"
      :pending-conversation="pendingAiConversation"
      @adopted="handleAiContentAdopted"
      @pending-buffered="handleAiPendingBuffered"
    />

    <MatchSelectDialog v-model:visible="matchDialogVisible" @select="handleMatchSelect" />
    <LayoutTemplateSelectDialog
      v-model="templateDialogVisible"
      :document-type="formData.documentType"
      @select="handleApplyTemplate"
    />
    <WechatQuickTypesetDialog
      v-model="quickTypesetVisible"
      :title="formData.title"
      :author="authorLabel"
      :body-html="quickTypesetSourceHtml"
      @apply="handleQuickTypesetApply"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CloseBold, Expand, Fold, FullScreen } from '@element-plus/icons-vue'
import DictSelect from '@/components/DictSelect.vue'
import AiContentDrawer from './AiContentDrawer.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import MatchSelectDialog from '@/components/selectors/MatchSelectDialog.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import LayoutEditor from '@/components/layout/LayoutEditor.vue'
import LayoutViewer from '@/components/layout/LayoutViewer.vue'
import LayoutTemplateSelectDialog from '@/components/layout/LayoutTemplateSelectDialog.vue'
import LayoutResourceSidebar from '@/components/layout/LayoutResourceSidebar.vue'
import WechatQuickTypesetDialog from '@/components/layout/WechatQuickTypesetDialog.vue'
import {
  extractPlainText,
  parseHtmlToLayoutDocument,
  plainTextToHtml,
  renderLayoutHtml,
  sanitizeLayoutHtml,
  ensureLayoutArticleHtml,
  combineContentHtml,
  resolvePaidContentHtml,
  resolveFreeContentHtml,
} from '@/utils/layoutSync'
import { markdownToHtml } from '@/utils/markdownHtml'
import { applyLayoutTemplate, previewApplyLayoutTemplate, previewTemplateMerge } from '@/api/layoutTemplate'
import { emptyLayoutDocument, type LayoutDocument } from '@/types/layoutTemplate'
import {
  createContent,
  updateContent,
  submitContentReview,
  getContentByTask,
  getContent,
  getScriptRef,
  generateContent,
  getMyIpGroups,
  fetchFootballScheme,
} from '@/api/content'
import { toCompetitionId, toCompetitionName, type MatchVO } from '@/api/match'
import { getTaskExecute } from '@/api/task'
import { getAuthorPage } from '@/api/author'
import { fetchUserProfile } from '@/api/system-user'
import {
  generateAiPreferenceSummary,
  saveAiConversationHistory,
} from '@/api/aiContent'
import type { PendingAiConversation } from '@/composables/useAiContentSession'
import type { FootballSchemeVO } from '@/types/content'

import { formatDateTime } from '@/utils'

const PRICE_PRESETS = [88, 128, 168, 208] as const

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
  if (step.stepKey === 'DRAFT' || step.stepKey === 'PENDING_PUBLISH'
      || (step.stepKey === 'PUBLISHED' && step.stepStatus === 'WAITING')) {
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
const titleFromCompetition = ref(false)
const competitionLabel = computed(() => competitionName.value || competitionId.value || '')
const matchDialogVisible = ref(false)
const scriptRef = ref<ScriptRef | null>(null)
const taskIpGroupName = ref('')
const authorLabel = ref('')
/** 作者缓存（来自 GET /oa/user/ip-groups，供切换 IP 组时快速带出作者） */
const myIpGroupAuthorCache = ref<Array<{ ipGroupId: number; ipGroupName: string; authorId?: number; authorName?: string }>>([])

const taskIpGroupDisplay = computed(() => taskIpGroupName.value || '')

const formData = reactive({
  contentId: undefined as number | undefined,
  title: '',
  platformTypes: [] as string[],
  contentType: undefined as string | undefined,
  documentType: undefined as string | undefined,
  schemeTypes: ['COMPREHENSIVE'] as string[],
  accountIds: [] as number[],
  isAi: true,
  body: '',
  bodyFormat: 'PLAIN' as string,
  layoutJson: emptyLayoutDocument() as LayoutDocument,
  layoutHtml: '',
  layoutTemplateId: undefined as number | undefined,
  taskId: undefined as number | undefined,
  ipGroupId: undefined as number | undefined,
  authorId: undefined as number | undefined,
  generatedVideoUrl: '',
  finalVideoUrl: '',
  creatorUserId: undefined as number | undefined,
  competitionId: '',
  paidBody: '',
  freeBody: '',
  price: 88,
  privilegeTypes: ['2'] as string[],
  refundType: 0,
})

const footballScheme = reactive({
  authorArticleId: undefined as number | undefined,
  shelfStatus: undefined as number | undefined,
  footballSyncError: undefined as string | undefined,
  syncFootballAt: undefined as string | undefined,
})
const bodyContentTab = ref<'paid' | 'free'>('paid')
const freeBodyHtml = ref('<p></p>')
const freeSyncing = ref(false)
const freeEditorRef = ref<InstanceType<typeof RichTextEditor> | null>(null)
const freeEditorMaximized = ref(false)
const freeLayoutPanelCollapsed = ref(true)
const freeLayoutJson = ref<LayoutDocument>(emptyLayoutDocument())

const showDocumentType = computed(() => formData.contentType === 'ARTICLE')
const showArticleLayout = computed(() => formData.contentType === 'ARTICLE')
const templateDialogVisible = ref(false)
const quickTypesetVisible = ref(false)
const richBodyHtml = ref('<p></p>')
const richSyncing = ref(false)
/** Blocks layoutJson→HTML sync while hydrating detail from API (layoutHtml is SSOT). */
const contentHydrating = ref(false)
const layoutJsonFromRichEditor = ref(false)
const editorMaximized = ref(false)
const layoutPanelCollapsed = ref(true)
const richEditorRef = ref<InstanceType<typeof RichTextEditor> | null>(null)

/** Default editor body frame — taller than legacy 480px, capped on short viewports. */
const EDITOR_FRAME_MIN = 'min(620px, calc(100vh - 280px))'

function insertHtmlAtCursor(html: string) {
  richEditorRef.value?.insertHtmlAtCursor(html)
}

function insertHtmlAtCursorFree(html: string) {
  freeEditorRef.value?.insertHtmlAtCursor(html)
}

function applyStyleToSelection(html: string) {
  richEditorRef.value?.applyStyleToSelection(html)
}

function applyStyleToSelectionFree(html: string) {
  freeEditorRef.value?.applyStyleToSelection(html)
}

function handleSidebarTemplateApplied(payload: {
  layoutJson: unknown
  layoutHtml: string
  templateId: number
  mode: string
}) {
  formData.bodyFormat = 'LAYOUT'
  formData.layoutJson = payload.layoutJson as LayoutDocument
  formData.layoutHtml = payload.layoutHtml
  formData.layoutTemplateId = payload.templateId
  richSyncing.value = true
  try {
    richBodyHtml.value = payload.layoutHtml || ''
    formData.body = extractPlainText(richBodyHtml.value)
    formData.paidBody = formData.body
  } finally {
    richSyncing.value = false
  }
}

function handleFreeSidebarTemplateApplied(payload: {
  layoutJson: unknown
  layoutHtml: string
  templateId: number
  mode: string
}) {
  freeSyncing.value = true
  try {
    freeLayoutJson.value = payload.layoutJson as LayoutDocument
    freeBodyHtml.value = payload.layoutHtml || ''
    syncFreeToForm(freeBodyHtml.value)
  } finally {
    nextTick(() => {
      freeSyncing.value = false
    })
  }
}

function handleFreeSidebarTypesetApplied(payload: {
  html: string
  layoutJson?: unknown
  templateId?: number
  mode: string
}) {
  freeSyncing.value = true
  try {
    if (payload.mode === 'TEMPLATE' && payload.layoutJson) {
      freeLayoutJson.value = payload.layoutJson as LayoutDocument
    }
    freeBodyHtml.value = payload.html
    syncFreeToForm(payload.html)
  } finally {
    nextTick(() => {
      freeSyncing.value = false
    })
  }
}

function openQuickTypesetDialog() {
  const isFreeTab = bodyContentTab.value === 'free'
  const html = isFreeTab ? freeBodyHtml.value : richBodyHtml.value
  const bodyText = extractPlainText(html) || (isFreeTab ? formData.freeBody?.trim() : formData.body?.trim())
  if (!formData.title?.trim() && !bodyText) {
    ElMessage.warning('请先填写标题或正文，再使用一键排版')
    return
  }
  quickTypesetVisible.value = true
}

function handleQuickTypesetApply(html: string) {
  if (bodyContentTab.value === 'free') {
    freeSyncing.value = true
    try {
      const layoutHtml = ensureLayoutArticleHtml(html)
      freeBodyHtml.value = layoutHtml
      freeLayoutJson.value = parseHtmlToLayoutDocument(layoutHtml)
      syncFreeToForm(layoutHtml)
      ElMessage.success('排版已应用到免费内容')
    } finally {
      nextTick(() => {
        freeSyncing.value = false
      })
    }
    return
  }
  richSyncing.value = true
  try {
    formData.bodyFormat = 'LAYOUT'
    formData.layoutHtml = ensureLayoutArticleHtml(html)
    richBodyHtml.value = formData.layoutHtml
    syncRichToForm(formData.layoutHtml)
    ElMessage.success('排版已应用到正文')
  } finally {
    richSyncing.value = false
  }
}

function handleSidebarTypesetApplied(payload: {
  html: string
  layoutJson?: unknown
  templateId?: number
  mode: string
}) {
  richSyncing.value = true
  try {
    if (payload.mode === 'TEMPLATE' && payload.layoutJson) {
      formData.bodyFormat = 'LAYOUT'
      formData.layoutJson = payload.layoutJson as LayoutDocument
      formData.layoutHtml = payload.html
      if (payload.templateId) {
        formData.layoutTemplateId = payload.templateId
      }
    }
    richBodyHtml.value = payload.html
    syncRichToForm(payload.html)
  } finally {
    richSyncing.value = false
  }
}

const sidebarBodyText = computed(
  () => extractPlainText(richBodyHtml.value) || formData.body?.trim() || '',
)

const sidebarFreeBodyText = computed(
  () => extractPlainText(freeBodyHtml.value) || formData.freeBody?.trim() || '',
)

const quickTypesetSourceHtml = computed(() =>
  bodyContentTab.value === 'free' ? freeBodyHtml.value : richBodyHtml.value,
)

function toggleEditorMaximize() {
  editorMaximized.value = !editorMaximized.value
  if (editorMaximized.value) {
    layoutPanelCollapsed.value = true
  }
}

function toggleFreeEditorMaximize() {
  freeEditorMaximized.value = !freeEditorMaximized.value
  if (freeEditorMaximized.value) {
    freeLayoutPanelCollapsed.value = true
  }
}

function onMaximizeKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && editorMaximized.value) {
    editorMaximized.value = false
  }
}

function onFreeMaximizeKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && freeEditorMaximized.value) {
    freeEditorMaximized.value = false
  }
}

watch(editorMaximized, (maximized) => {
  if (maximized) {
    document.body.classList.add('content-editor-maximized-open')
    document.addEventListener('keydown', onMaximizeKeydown)
  } else {
    document.body.classList.remove('content-editor-maximized-open')
    document.removeEventListener('keydown', onMaximizeKeydown)
  }
})

watch(freeEditorMaximized, (maximized) => {
  if (maximized) {
    document.body.classList.add('content-editor-maximized-open')
    document.addEventListener('keydown', onFreeMaximizeKeydown)
  } else {
    document.body.classList.remove('content-editor-maximized-open')
    document.removeEventListener('keydown', onFreeMaximizeKeydown)
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove('content-editor-maximized-open')
  document.removeEventListener('keydown', onMaximizeKeydown)
  document.removeEventListener('keydown', onFreeMaximizeKeydown)
})

function normalizeRichHtml(html: string): string {
  return sanitizeLayoutHtml(html || '').replace(/\s+/g, ' ').trim()
}

function endRichSync() {
  nextTick(() => {
    richSyncing.value = false
    layoutJsonFromRichEditor.value = false
  })
}

const displayRichHtml = computed(() =>
  resolvePaidContentHtml({
    bodyFormat: formData.bodyFormat,
    layoutHtml: formData.layoutHtml,
    layoutJson: formData.layoutJson,
    paidBody: formData.paidBody,
    body: formData.body,
    editorHtml: richBodyHtml.value,
  }),
)

const displayFreeHtml = computed(() =>
  resolveFreeContentHtml({
    freeBody: formData.freeBody,
    editorHtml: freeBodyHtml.value,
  }),
)

const displayCombinedHtml = computed(() =>
  combineContentHtml(displayFreeHtml.value, displayRichHtml.value),
)

function syncRichToForm(html: string) {
  richSyncing.value = true
  layoutJsonFromRichEditor.value = true
  try {
    const cleaned = html || '<p></p>'
    formData.body = extractPlainText(cleaned)
    formData.paidBody = formData.body
    if (formData.contentType === 'ARTICLE') {
      formData.bodyFormat = 'LAYOUT'
      // Preserve editor HTML verbatim for WeChat publish (inline styles, img width, etc.)
      formData.layoutHtml = ensureLayoutArticleHtml(cleaned)
      // Best-effort structure sync for layout panel; does not overwrite layoutHtml
      formData.layoutJson = parseHtmlToLayoutDocument(cleaned)
    }
  } finally {
    endRichSync()
  }
}

function initRichBodyFromForm() {
  richSyncing.value = true
  try {
    richBodyHtml.value = resolvePaidContentHtml({
      bodyFormat: formData.bodyFormat,
      layoutHtml: formData.layoutHtml,
      layoutJson: formData.layoutJson,
      paidBody: formData.paidBody,
      body: formData.body,
    })
  } finally {
    endRichSync()
  }
}

function initFreeBodyFromForm() {
  freeSyncing.value = true
  try {
    freeBodyHtml.value = resolveFreeContentHtml({ freeBody: formData.freeBody })
    if (formData.contentType === 'ARTICLE') {
      freeLayoutJson.value = parseHtmlToLayoutDocument(freeBodyHtml.value)
    }
  } finally {
    nextTick(() => {
      freeSyncing.value = false
    })
  }
}

function syncFreeToForm(html: string) {
  freeSyncing.value = true
  try {
    const cleaned = html || '<p></p>'
    formData.freeBody = formData.contentType === 'ARTICLE'
      ? ensureLayoutArticleHtml(cleaned)
      : extractPlainText(cleaned) || cleaned
  } finally {
    nextTick(() => {
      freeSyncing.value = false
    })
  }
}

watch(richBodyHtml, (html) => {
  if (richSyncing.value) return
  const cleaned = html || '<p></p>'
  if (formData.contentType === 'ARTICLE') {
    if (
      formData.bodyFormat === 'LAYOUT' &&
      normalizeRichHtml(cleaned) === normalizeRichHtml(formData.layoutHtml || '')
    ) {
      return
    }
    syncRichToForm(cleaned)
    return
  }
  if (showBody.value) {
    formData.bodyFormat = 'PLAIN'
    formData.body = extractPlainText(cleaned)
    formData.paidBody = formData.body
  }
})

watch(freeBodyHtml, (html) => {
  if (freeSyncing.value) return
  syncFreeToForm(html || '<p></p>')
})

watch(
  () => formData.layoutJson,
  (val) => {
    if (
      contentHydrating.value ||
      richSyncing.value ||
      layoutJsonFromRichEditor.value ||
      formData.contentType !== 'ARTICLE' ||
      formData.bodyFormat !== 'LAYOUT'
    ) {
      return
    }
    if (!val?.blocks?.length) return
    const rendered = renderLayoutHtml(val)
    if (normalizeRichHtml(rendered) === normalizeRichHtml(richBodyHtml.value)) return
    richSyncing.value = true
    try {
      // Panel-driven structure edits update editor + layoutHtml together.
      formData.layoutHtml = rendered
      richBodyHtml.value = rendered
      formData.body = extractPlainText(rendered)
    } finally {
      endRichSync()
    }
  },
  { deep: true }
)
const showScriptRef = computed(() => isTaskMode.value && formData.contentType === 'SHORT_VIDEO')
const showBody = computed(
  () => !!formData.contentType && formData.contentType !== 'SHORT_VIDEO' && formData.contentType !== 'ARTICLE',
)
/** AI 文本生成：文章走 showArticleLayout，其它非短视频类型走 showBody */
const showAiTextGenerate = computed(
  () => !!formData.contentType && formData.contentType !== 'SHORT_VIDEO',
)
/** ADR-054：正式方案（OFFICIAL_PLAN）需维护售价；编辑已有内容时始终展示 */
const showPriceField = computed(
  () =>
    showAiTextGenerate.value &&
    (!!formData.contentId || formData.documentType === 'OFFICIAL_PLAN'),
)
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

const aiContentDrawerVisible = ref(false)
/** 新建内容尚无 contentId 时，AI 对话暂存于此，保存内容后再写入后端 */
const pendingAiConversation = ref<PendingAiConversation | null>(null)
const aiDrawerContext = computed(() => ({
  matchName: competitionLabel.value || competitionName.value || '',
  authorName: authorLabel.value || '',
  schemeTypes: [...formData.schemeTypes],
  contentId: formData.contentId,
  authorId: formData.authorId,
}))

const syncCompetitionToForm = () => {
  formData.competitionId = competitionId.value
}

const applyCompetitionTitle = (name: string) => {
  const trimmed = name?.trim()
  if (!trimmed) return
  if (!formData.title.trim() || titleFromCompetition.value) {
    formData.title = trimmed
    titleFromCompetition.value = true
  }
}

watch(
  () => formData.title,
  () => {
    if (titleFromCompetition.value && formData.title.trim() !== competitionName.value.trim()) {
      titleFromCompetition.value = false
    }
  },
)

const resetForm = () => {
  Object.assign(formData, {
    contentId: undefined,
    title: '',
    platformTypes: [],
    contentType: undefined,
    documentType: undefined,
    schemeTypes: ['COMPREHENSIVE'],
    accountIds: [],
    isAi: true,
    body: '',
    bodyFormat: 'PLAIN',
    layoutJson: emptyLayoutDocument(),
    layoutHtml: '',
    layoutTemplateId: undefined,
    taskId: undefined,
    ipGroupId: undefined,
    authorId: undefined,
    generatedVideoUrl: '',
    finalVideoUrl: '',
    creatorUserId: undefined,
    competitionId: '',
    paidBody: '',
    freeBody: '',
    price: 88,
    privilegeTypes: ['2'],
    refundType: 0,
  })
  Object.assign(footballScheme, {
    authorArticleId: undefined,
    shelfStatus: undefined,
    footballSyncError: undefined,
    syncFootballAt: undefined,
  })
  competitionId.value = ''
  competitionName.value = ''
  titleFromCompetition.value = false
  authorLabel.value = ''
  taskIpGroupName.value = ''
  myIpGroupAuthorCache.value = []
  scriptRef.value = null
  contentStatus.value = undefined
  reviewProgress.value = []
  richBodyHtml.value = '<p></p>'
  freeBodyHtml.value = '<p></p>'
  freeLayoutJson.value = emptyLayoutDocument()
  freeEditorMaximized.value = false
  freeLayoutPanelCollapsed.value = true
  bodyContentTab.value = 'paid'
  pendingAiConversation.value = null
}

const applyFootballScheme = (scheme: FootballSchemeVO) => {
  footballScheme.authorArticleId = scheme.authorArticleId
  footballScheme.shelfStatus = scheme.shelfStatus
  footballScheme.footballSyncError = scheme.footballSyncError
  footballScheme.syncFootballAt = scheme.syncFootballAt
  if (scheme.price != null) formData.price = scheme.price
  if (scheme.privilegeTypes?.length) formData.privilegeTypes = [...scheme.privilegeTypes]
  if (scheme.refundType != null) formData.refundType = scheme.refundType
}

const loadFootballScheme = async (contentId: number) => {
  try {
    applyFootballScheme(await fetchFootballScheme(contentId))
  } catch {
    // P2/P3：后端未就绪时不阻断编辑
  }
}

const handleMatchSelect = (match: MatchVO) => {
  competitionId.value = toCompetitionId(match)
  competitionName.value = toCompetitionName(match)
  syncCompetitionToForm()
  applyCompetitionTitle(competitionName.value)
  loadScriptRef()
}

const clearCompetition = () => {
  if (titleFromCompetition.value) {
    formData.title = ''
    titleFromCompetition.value = false
  }
  competitionId.value = ''
  competitionName.value = ''
  syncCompetitionToForm()
  scriptRef.value = null
}

const normalizeAuthorId = (id?: number | string | null): number | undefined => {
  if (id == null || id === '') return undefined
  const num = Number(id)
  return Number.isFinite(num) && num > 0 ? num : undefined
}

const mapAuthorListItem = (item: {
  id?: number | string
  authorUserId?: number | string
  authorName?: string
  nickname?: string
  anchorUserName?: string
}) => {
  const id = normalizeAuthorId(item.id) ?? normalizeAuthorId(item.authorUserId)
  const authorName = String(item.authorName || item.nickname || item.anchorUserName || '').trim()
  return id ? { id, authorName } : null
}

const loadAuthorForGroup = async (ipGroupId?: number, preferAuthorId?: number) => {
  if (!ipGroupId) {
    authorLabel.value = ''
    formData.authorId = undefined
    return
  }
  const preferredId = normalizeAuthorId(preferAuthorId)
  if (preferredId) {
    formData.authorId = preferredId
    if (authorLabel.value) {
      return
    }
    const cached = myIpGroupAuthorCache.value.find((item) => item.ipGroupId === ipGroupId)
    if (normalizeAuthorId(cached?.authorId) === preferredId && cached?.authorName) {
      authorLabel.value = cached.authorName
      return
    }
    try {
      const page = await getAuthorPage({ ipGroupId, status: 1, page: 1, size: 100 })
      const author = page?.list
        ?.map((item) => mapAuthorListItem(item))
        .find((item) => item && item.id === preferredId)
      authorLabel.value = author?.authorName || '—'
    } catch {
      authorLabel.value = '—'
    }
    return
  }
  const cached = myIpGroupAuthorCache.value.find((item) => item.ipGroupId === ipGroupId)
  if (cached?.authorId) {
    formData.authorId = normalizeAuthorId(cached.authorId)
    authorLabel.value = cached.authorName || ''
    return
  }
  try {
    const page = await getAuthorPage({ ipGroupId, status: 1, page: 1, size: 1 })
    const author = page?.list?.[0] ? mapAuthorListItem(page.list[0]) : null
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

const handleApplyTemplate = async (template: { id: number; templateName: string }) => {
  const isFreeTab = bodyContentTab.value === 'free'
  const bodyText = isFreeTab
    ? extractPlainText(freeBodyHtml.value) || formData.freeBody?.trim()
    : extractPlainText(richBodyHtml.value) || formData.body?.trim()
  if (!bodyText) {
    ElMessage.warning('请先填写正文，再套用版式模板')
    return
  }
  if (!isFreeTab && formData.bodyFormat === 'LAYOUT' && formData.layoutJson?.blocks?.length) {
    try {
      await ElMessageBox.confirm('应用模板将覆盖当前富版式样式，正文文字将保留。是否继续？', '确认覆盖', {
        type: 'warning',
      })
    } catch {
      return
    }
  } else if (isFreeTab && freeLayoutJson.value?.blocks?.length) {
    try {
      await ElMessageBox.confirm('应用模板将覆盖当前免费内容版式样式，正文文字将保留。是否继续？', '确认覆盖', {
        type: 'warning',
      })
    } catch {
      return
    }
  }
  try {
    let preview
    if (!isFreeTab && formData.contentId) {
      preview = await previewApplyLayoutTemplate(formData.contentId, template.id)
    } else {
      preview = await previewTemplateMerge(
        template.id,
        bodyText,
        isFreeTab ? freeLayoutJson.value : formData.layoutJson,
      )
    }
    await ElMessageBox.confirm(
      '左侧为原文，右侧为套用后版式预览。确认应用？',
      '套用预览',
      {
        type: 'info',
        dangerouslyUseHTMLString: true,
        message: `<div style="display:flex;gap:12px;max-height:320px;overflow:auto">
          <div style="flex:1"><strong>原文</strong><pre style="white-space:pre-wrap">${bodyText}</pre></div>
          <div style="flex:1"><strong>套用效果</strong><div>${preview.layoutHtml || ''}</div></div>
        </div>`,
      }
    )
    if (!isFreeTab && formData.contentId) {
      const updated = (await applyLayoutTemplate(formData.contentId, template.id, true)) as Record<string, any>
      applyContentRecord(updated, isTaskMode.value)
      ElMessage.success(`已应用模板：${template.templateName}`)
      return
    }
    if (isFreeTab) {
      freeSyncing.value = true
      try {
        freeLayoutJson.value = (preview.layoutJson as LayoutDocument) || emptyLayoutDocument()
        freeBodyHtml.value = preview.layoutHtml || ''
        syncFreeToForm(freeBodyHtml.value)
      } finally {
        nextTick(() => {
          freeSyncing.value = false
        })
      }
      ElMessage.success(`已应用模板：${template.templateName}`)
      return
    }
    formData.bodyFormat = 'LAYOUT'
    formData.layoutJson = preview.layoutJson
    formData.layoutHtml = preview.layoutHtml || ''
    formData.layoutTemplateId = template.id
    richSyncing.value = true
    try {
      richBodyHtml.value = preview.layoutHtml || ''
      formData.body = extractPlainText(richBodyHtml.value)
    } finally {
      richSyncing.value = false
    }
    ElMessage.success(`已应用模板：${template.templateName}`)
  } catch {
    // cancel or error
  }
}

const handleContentTypeChange = () => {
  if (!showDocumentType.value) {
    formData.documentType = undefined
    formData.bodyFormat = 'PLAIN'
    formData.layoutJson = emptyLayoutDocument()
    formData.layoutHtml = ''
    formData.layoutTemplateId = undefined
    freeLayoutJson.value = emptyLayoutDocument()
  }
  if (formData.contentType && formData.contentType !== 'SHORT_VIDEO') {
    initRichBodyFromForm()
    initFreeBodyFromForm()
  }
  loadScriptRef()
}


watch(() => formData.contentType, loadScriptRef)

const buildPayload = () => {
  syncCompetitionToForm()
  if (formData.contentType === 'ARTICLE') {
    syncRichToForm(richBodyHtml.value)
  } else if (formData.contentType && formData.contentType !== 'SHORT_VIDEO') {
    formData.bodyFormat = 'PLAIN'
    formData.body = extractPlainText(richBodyHtml.value || '')
    formData.paidBody = formData.body
  }
  syncFreeToForm(freeBodyHtml.value)
  return {
    title: formData.title,
    contentType: formData.contentType!,
    body: formData.body || formData.paidBody || '',
    paidBody: formData.paidBody || formData.body || '',
    freeBody: formData.freeBody || undefined,
    bodyFormat: formData.bodyFormat,
    layoutJson: formData.bodyFormat === 'LAYOUT' ? formData.layoutJson : undefined,
    layoutHtml: formData.bodyFormat === 'LAYOUT' ? formData.layoutHtml : undefined,
    layoutTemplateId: formData.layoutTemplateId,
    aiGenerated: formData.isAi ? 1 : 0,
    creatorUserId: formData.creatorUserId!,
    taskId: formData.taskId,
    competitionId: competitionId.value || undefined,
    competitionName: competitionName.value || undefined,
    documentType: formData.documentType,
    schemeTypes: formData.schemeTypes?.length ? formData.schemeTypes : undefined,
    ipGroupId: formData.ipGroupId,
    authorId: formData.authorId,
    generatedVideoUrl: formData.generatedVideoUrl || undefined,
    finalVideoUrl: formData.finalVideoUrl || undefined,
    price: formData.price,
    privilegeTypes: formData.privilegeTypes?.length ? formData.privilegeTypes : ['2'],
    refundType: formData.refundType,
  }
}

const persistContent = async (): Promise<number> => {
  const payload = buildPayload()
  const wasNew = !formData.contentId
  if (formData.contentId) {
    await updateContent({ ...payload, id: formData.contentId })
    if (contentStatus.value === 'REJECTED' || contentStatus.value === 'COMPLETED') {
      contentStatus.value = 'DRAFT'
    }
    await loadFootballScheme(formData.contentId)
    return formData.contentId
  }
  const id = await createContent(payload)
  formData.contentId = id
  contentStatus.value = 'DRAFT'
  if (wasNew) {
    await flushPendingAiConversation(id)
  }
  await loadFootballScheme(id)
  return id
}

const flushPendingAiConversation = async (contentId: number) => {
  const pending = pendingAiConversation.value
  if (!pending?.conversationHistory?.length) {
    pendingAiConversation.value = null
    return
  }
  try {
    await saveAiConversationHistory({
      sessionId: pending.sessionId,
      conversationHistory: pending.conversationHistory,
      authorId: formData.authorId,
      contentId,
    })
    if (pending.persistContext) {
      await generateAiPreferenceSummary({
        sessionId: pending.sessionId,
        conversationHistory: pending.conversationHistory,
        context: pending.persistContext,
        authorId: formData.authorId,
        contentId,
        preferenceSummary: pending.preferenceSummary,
      })
    }
  } catch {
    // 保存内容已成功，对话持久化失败不阻断主流程
  } finally {
    pendingAiConversation.value = null
  }
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
    if (footballScheme.footballSyncError) {
      ElMessage.warning(`草稿已保存，Football 方案同步失败：${footballScheme.footballSyncError}`)
    } else {
      ElMessage.success('草稿已保存')
    }
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
    if (result?.body) {
      formData.body = result.body
      if (formData.contentType && formData.contentType !== 'SHORT_VIDEO') {
        richBodyHtml.value = plainTextToHtml(result.body)
        if (formData.contentType === 'ARTICLE') {
          syncRichToForm(richBodyHtml.value)
        }
      }
    }
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
  pendingAiConversation.value = null
  emit('cancelled')
}

const openAiContentDrawer = async () => {
  if (!formData.title?.trim()) {
    ElMessage.warning('请先填写标题')
    return
  }
  if (!formData.ipGroupId) {
    ElMessage.warning('请先选择所属 IP 组')
    return
  }
  if (!competitionId.value) {
    ElMessage.warning('请先选择关联赛事')
    return
  }
  if (!formData.schemeTypes?.length) {
    ElMessage.warning('请先选择方案分析类型')
    return
  }
  if (!formData.authorId && formData.ipGroupId) {
    await loadAuthorForGroup(formData.ipGroupId)
  }
  if (!authorLabel.value?.trim()) {
    ElMessage.warning('请先确认作者信息')
    return
  }
  aiContentDrawerVisible.value = true
}

const handleAiContentAdopted = (payload: { content: string; target: 'PAID' | 'FREE' }) => {
  const generated = payload.content || ''
  formData.isAi = true
  if (!formData.contentType || formData.contentType === 'SHORT_VIDEO') return

  const html = markdownToHtml(generated)
  if (payload.target === 'FREE') {
    bodyContentTab.value = 'free'
    freeBodyHtml.value = html
    syncFreeToForm(html)
    return
  }

  bodyContentTab.value = 'paid'
  richBodyHtml.value = html
  formData.body = extractPlainText(richBodyHtml.value) || generated
  if (formData.contentType === 'ARTICLE') {
    syncRichToForm(richBodyHtml.value)
  } else {
    formData.bodyFormat = 'PLAIN'
    formData.paidBody = formData.body
  }
}

const handleAiPendingBuffered = (snapshot: PendingAiConversation | null) => {
  pendingAiConversation.value = snapshot
}

const applyContentRecord = (existing: Record<string, any>, taskMode = false) => {
  // ADR-021: layoutHtml is SSOT — block layoutJson→HTML sync while hydrating from API.
  contentHydrating.value = true
  richSyncing.value = true
  try {
    Object.assign(formData, {
      contentId: existing.id,
      title: existing.title,
      contentType: existing.contentType,
      documentType: existing.documentType,
      schemeTypes: existing.schemeTypes?.length
        ? existing.schemeTypes
        : existing.schemeType
          ? [existing.schemeType]
          : ['COMPREHENSIVE'],
      body: existing.body,
      paidBody: existing.paidBody || existing.body || '',
      freeBody: existing.freeBody || '',
      price: existing.price ?? 88,
      privilegeTypes: existing.privilegeTypes?.length ? existing.privilegeTypes : ['2'],
      refundType: existing.refundType ?? 0,
      bodyFormat: existing.bodyFormat || 'PLAIN',
      layoutJson: existing.layoutJson || emptyLayoutDocument(),
      layoutHtml: existing.layoutHtml || '',
      layoutTemplateId: existing.layoutTemplateId,
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
    titleFromCompetition.value = false
    syncCompetitionToForm()
    if (!taskMode) {
      authorLabel.value = existing.authorName || ''
    }
    if (existing.contentType && existing.contentType !== 'SHORT_VIDEO') {
      initRichBodyFromForm()
      initFreeBodyFromForm()
    }
    if (existing.id) {
      void loadFootballScheme(existing.id)
    }
  } finally {
    endRichSync()
    nextTick(() => {
      contentHydrating.value = false
    })
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
    if (!existing && competitionName.value) {
      applyCompetitionTitle(competitionName.value)
    }
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
    myIpGroupAuthorCache.value = groups || []
    formData.creatorUserId = profile?.id
    if (props.contentId) {
      const existing = await getContent(props.contentId)
      applyContentRecord(existing)
      if (existing.ipGroupId) {
        await loadAuthorForGroup(existing.ipGroupId, existing.authorId || undefined)
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
.article-edit-row {
  width: 100%;
}
.article-edit-section {
  width: 100%;
}
.article-edit-shell.is-maximized {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  box-sizing: border-box;
  overflow: hidden;
  background: var(--el-bg-color);
}
.article-edit-shell.is-maximized .article-edit-row {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: stretch;
}
.article-edit-shell.is-maximized .article-edit-row :deep(.el-col) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.article-edit-shell.is-maximized .article-edit-row :deep(.el-form-item) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  margin-bottom: 0;
}
.article-edit-shell.is-maximized .article-edit-row :deep(.el-form-item__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.article-edit-shell.is-maximized .article-edit-row :deep(.rich-text-editor) {
  flex: 1;
  min-height: 0;
  height: auto;
  max-height: none;
}
.article-edit-shell.is-maximized .article-edit-row :deep(.rte-content) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.article-edit-toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 0 0 8px 110px;
}
.article-edit-shell.is-maximized .article-edit-toolbar {
  margin-left: 0;
}
.article-edit-toolbar-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}
.article-edit-maximized-title {
  font-size: 16px;
  font-weight: 600;
}
.article-edit-maximized-footer {
  display: flex;
  flex-shrink: 0;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.price-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.price-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.body-content-tabs {
  width: 100%;
}
.free-body-editor {
  width: 100%;
}
</style>
