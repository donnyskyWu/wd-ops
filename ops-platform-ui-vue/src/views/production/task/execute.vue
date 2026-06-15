<!--
  任务执行页 P-M2-012
  依据: UX-M2 §4.3, PRD-M2 §4.2.5.1, ADR-016
-->
<template>
  <div v-loading="loading" class="task-execute-page">

    <el-card v-if="context" shadow="never" class="section-card">
      <template #header>
        <span>基本信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务名称">{{ context.planName }}</el-descriptions-item>
        <el-descriptions-item label="节点名称">{{ context.nodeName }}</el-descriptions-item>
        <el-descriptions-item label="IP 组">{{ context.ipGroupName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="赛事">{{ context.competitionName || context.competitionId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="SLA 截止">{{ formatDateTime(context.slaDeadline) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictLabel dict-type="dict_sop_node_status" :value="context.status" />
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <span>执行说明</span>
      </template>
      <p class="instruction-text">{{ context?.executionInstruction || '—' }}</p>
    </el-card>

    <el-card shadow="never" class="section-card delivery-card">
      <template #header>
        <span>交付说明</span>
      </template>
      <el-input
        v-model="deliverables"
        type="textarea"
        :rows="6"
        placeholder="填写交付说明或发布链接"
        maxlength="500"
        show-word-limit
        class="delivery-textarea"
      />

      <div class="attachment-section">
        <div class="attachment-section__title">附件</div>
        <div v-if="referenceAttachments.length" class="reference-attachments">
          <div class="attachment-section__subtitle">参考附件（SOP 节点）</div>
          <ul class="attachment-list">
            <li v-for="(item, index) in referenceAttachments" :key="`ref-${index}`">
              <el-link :href="item.url" target="_blank" type="primary">{{ item.name }}</el-link>
            </li>
          </ul>
        </div>
        <ul v-if="deliverableAttachments.length" class="attachment-list">
          <li v-for="(item, index) in deliverableAttachments" :key="`del-${index}`">
            <el-link :href="item.url" target="_blank" type="primary">{{ item.name }}</el-link>
            <el-button link type="danger" @click="removeAttachment(index)">删除</el-button>
          </li>
        </ul>
        <el-upload
          :show-file-list="false"
          :http-request="handleUpload"
          :disabled="uploading"
        >
          <el-button type="primary" plain :loading="uploading">上传附件</el-button>
        </el-upload>
        <div class="upload-tip">支持上传交付附件，单文件不超过 50MB</div>
      </div>
    </el-card>

    <el-card v-if="context?.nodeType === 'CONTENT_GENERATION' || context?.nodeType === 'CONTENT_PUBLISH'" shadow="never" class="section-card">
      <template #header>
        <span>操作区</span>
      </template>

      <div v-if="context?.nodeType === 'CONTENT_GENERATION'" class="action-block">
        <template v-if="context.linkedContent">
          <el-alert type="info" :closable="false" show-icon class="content-summary">
            已关联内容：{{ context.linkedContent.title }}（
            <DictLabel dict-type="dict_content_status" :value="context.linkedContent.status" />
            ）
          </el-alert>
          <el-button type="primary" @click="openContentDialog">
            {{ isLinkedContentPendingReview ? '查看内容' : '编辑内容' }}
          </el-button>
        </template>
        <template v-else>
          <el-alert type="warning" :closable="false" show-icon title="请先进入内容创作" />
          <el-button type="primary" class="mt-12" @click="openContentDialog">进入内容创作</el-button>
        </template>
      </div>

      <div v-else-if="context?.nodeType === 'CONTENT_PUBLISH'" class="action-block">
        <el-alert type="info" :closable="false" show-icon title="内容发布指引">
          <p>请在本节点完成内容发布：将已审核/已完成的内容发布到目标平台，并在上方填写发布说明（如发布链接、发布时间等）。</p>
          <p>本期按普通节点处理：填写交付说明后点击「完成」即可标记任务完成。平台自动发布对接待产品确认（BLK-M2-009）。</p>
        </el-alert>
      </div>
    </el-card>

    <div class="footer-actions">
      <el-button @click="goBack">返回</el-button>
      <el-button type="primary" plain :loading="saving" @click="handleSave">保存</el-button>
      <el-tooltip
        :disabled="canComplete"
        content="内容生成节点须先保存并提交内容审核"
      >
        <el-button type="success" :loading="completing" :disabled="!canComplete" @click="handleComplete">
          完成
        </el-button>
      </el-tooltip>
    </div>

    <ContentEditDialog
      v-model:visible="contentDialogVisible"
      :content-id="contentDialogContentId"
      :task-id="taskId"
      :competition-id="context?.competitionId"
      :readonly="contentDialogReadonly"
      @saved="handleContentSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { completeTaskExecute, getTaskExecute, saveTaskExecute, uploadTaskExecuteAttachment } from '@/api/task'
import type { TaskAttachmentVO, TaskExecuteVO } from '@/types/task'
import ContentEditDialog from '@/views/production/content/ContentEditDialog.vue'
import DictLabel from '@/components/DictLabel.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const completing = ref(false)
const uploading = ref(false)
const context = ref<TaskExecuteVO | null>(null)
const deliverables = ref('')
const referenceAttachments = ref<TaskAttachmentVO[]>([])
const deliverableAttachments = ref<TaskAttachmentVO[]>([])
const contentDialogVisible = ref(false)
const contentDialogContentId = ref<number | undefined>()
const contentDialogReadonly = ref(false)

const taskId = computed(() => Number(route.params.id))

const isPendingReviewStatus = (status?: string) => !!status && status.startsWith('PENDING')

const isLinkedContentPendingReview = computed(() =>
  isPendingReviewStatus(context.value?.linkedContent?.status),
)

const isLinkedContentSubmitted = (status?: string) =>
  !!status && status !== 'DRAFT' && status !== 'REJECTED'

const canComplete = computed(() => {
  if (!context.value) return false
  if (context.value.nodeType === 'CONTENT_GENERATION') {
    return isLinkedContentSubmitted(context.value.linkedContent?.status)
  }
  return true
})

const formatDateTime = (value?: string) => value || '—'

const loadContext = async () => {
  loading.value = true
  try {
    context.value = await getTaskExecute(taskId.value)
    deliverables.value = context.value.deliverables || ''
    referenceAttachments.value = context.value.attachments || []
    deliverableAttachments.value = context.value.deliverableAttachments || []
  } catch {
    ElMessage.error('加载任务执行信息失败')
    router.push('/task')
  } finally {
    loading.value = false
  }
}

const openContentDialog = () => {
  if (!context.value) return
  contentDialogContentId.value = context.value.linkedContent?.id
  contentDialogReadonly.value = isPendingReviewStatus(context.value.linkedContent?.status)
  contentDialogVisible.value = true
}

const handleContentSaved = async () => {
  await loadContext()
}

const handleUpload = async (options: UploadRequestOptions) => {
  uploading.value = true
  try {
    const uploaded = await uploadTaskExecuteAttachment(taskId.value, options.file as File)
    deliverableAttachments.value = [...deliverableAttachments.value, uploaded]
    ElMessage.success('附件上传成功')
    options.onSuccess?.(uploaded as any)
  } catch {
    ElMessage.error('附件上传失败')
    options.onError?.(new Error('upload failed') as any)
  } finally {
    uploading.value = false
  }
}

const removeAttachment = (index: number) => {
  deliverableAttachments.value = deliverableAttachments.value.filter((_, i) => i !== index)
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveTaskExecute(taskId.value, {
      deliverables: deliverables.value,
      deliverableAttachments: deliverableAttachments.value,
    })
    ElMessage.success('已保存')
    await loadContext()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleComplete = async () => {
  try {
    await ElMessageBox.confirm('确定要完成该任务吗？', '提示', { type: 'warning' })
    completing.value = true
    await saveTaskExecute(taskId.value, {
      deliverables: deliverables.value,
      deliverableAttachments: deliverableAttachments.value,
    })
    await completeTaskExecute(taskId.value)
    ElMessage.success('任务已完成')
    router.push('/task')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('完成任务失败，请确认内容创作已完成')
    }
  } finally {
    completing.value = false
  }
}

const goBack = () => {
  router.push('/task')
}

onMounted(loadContext)
</script>

<style scoped lang="scss">
.task-execute-page {
  .section-card {
    margin-bottom: 16px;
  }

  .instruction-text {
    margin: 0;
    line-height: 1.6;
    color: var(--el-text-color-regular);
  }

  .delivery-card {
    :deep(.el-card__body) {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
  }

  .delivery-textarea {
    width: 100%;

    :deep(.el-textarea__inner) {
      width: 100%;
    }
  }

  .attachment-section {
    width: 100%;
    border-top: 1px solid var(--el-border-color-lighter);
    padding-top: 16px;

    &__title {
      font-weight: 600;
      margin-bottom: 12px;
    }

    &__subtitle {
      color: var(--el-text-color-secondary);
      font-size: 13px;
      margin-bottom: 8px;
    }
  }

  .attachment-list {
    margin: 0 0 12px;
    padding-left: 18px;

    li {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 6px;
    }
  }

  .upload-tip {
    margin-top: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .action-block {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .content-summary {
    width: 100%;
  }

  .mt-12 {
    margin-top: 0;
  }

  .footer-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 8px;
  }
}
</style>
