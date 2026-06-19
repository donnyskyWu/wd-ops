<template>
  <div class="layout-template-edit">
    <el-page-header :content="pageTitle" @back="router.back()" />

    <el-tag v-if="isPreset" type="warning" class="preset-badge">系统预置 · 只读</el-tag>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="edit-form">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="form.templateName" maxlength="100" show-word-limit :disabled="isPreset" />
      </el-form-item>
      <el-form-item label="文档类型">
        <DictSelect
          v-model="form.documentType"
          dict-type="dict_document_type"
          clearable
          placeholder="留空=通用模板"
          :disabled="isPreset"
        />
      </el-form-item>
      <el-form-item label="状态">
        <DictLabel dict-type="dict_layout_template_status" :value="form.status" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          :disabled="isPreset"
        />
      </el-form-item>

      <el-form-item label="版式编辑" required>
        <el-tabs v-model="editMode" class="layout-edit-tabs">
          <el-tab-pane label="富文本编辑" name="rich">
            <RichTextEditor
              v-if="!isPreset"
              v-model="richHtml"
              placeholder="编辑版式骨架，支持公众号兼容排版与图片缩放"
              min-height="min(620px, calc(100vh - 280px))"
            />
            <LayoutViewer v-else :html="richHtml || previewHtml" />
          </el-tab-pane>
          <el-tab-pane label="结构编辑" name="structure">
            <LayoutSchemaEditor v-model="form.layoutSchema" :preview-html="previewHtml" />
          </el-tab-pane>
        </el-tabs>
      </el-form-item>

      <el-form-item>
        <el-button v-if="!isPreset" type="primary" :loading="saving" @click="handleSave">保存</el-button>
        <el-button v-if="isPreset" type="primary" :loading="saving" @click="handleCopy">复制后编辑</el-button>
        <el-button
          v-if="isEdit && form.status === 'DRAFT' && !isPreset"
          type="success"
          :loading="publishing"
          @click="handlePublish"
        >
          发布
        </el-button>
        <el-button
          v-if="isEdit && form.status === 'ENABLED'"
          type="warning"
          :loading="publishing"
          @click="handleDisable"
        >
          停用
        </el-button>
        <el-button
          v-if="isEdit && form.status === 'DISABLED'"
          type="success"
          :loading="publishing"
          @click="handleEnable"
        >
          重新启用
        </el-button>
        <el-button @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import LayoutSchemaEditor from '@/components/layout/LayoutSchemaEditor.vue'
import LayoutViewer from '@/components/layout/LayoutViewer.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import {
  copyLayoutTemplate,
  createLayoutTemplate,
  disableLayoutTemplate,
  enableLayoutTemplate,
  getLayoutTemplate,
  publishLayoutTemplate,
  updateLayoutTemplate,
} from '@/api/layoutTemplate'
import { emptyLayoutSchema, type LayoutTemplateForm } from '@/types/layoutTemplate'
import { ensureLayoutArticleHtml, htmlToLayoutSchema, sanitizeLayoutHtml, schemaToPreviewHtml } from '@/utils/layoutSync'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const publishing = ref(false)
const previewHtml = ref('')
const richHtml = ref('')
const editMode = ref<'rich' | 'structure'>('rich')
const sourceType = ref('MANUAL')
const syncing = ref(false)

const templateId = computed(() => (route.params.id ? Number(route.params.id) : undefined))
const isEdit = computed(() => route.name === 'LayoutTemplateEdit')
const isPreset = computed(() => sourceType.value === 'PRESET')
const pageTitle = computed(() => (isEdit.value ? '编辑公推模板' : '新建公推模板'))

const form = reactive<LayoutTemplateForm & { description?: string }>({
  templateName: '',
  description: '',
  documentType: null,
  status: 'DRAFT',
  layoutSchema: emptyLayoutSchema(),
})

const rules: FormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
}

function normalizeRichHtml(html: string): string {
  return sanitizeLayoutHtml(html || '').replace(/\s+/g, ' ').trim()
}

function endSync() {
  nextTick(() => {
    syncing.value = false
  })
}

function refreshPreviewFromSchema() {
  previewHtml.value = schemaToPreviewHtml(form.layoutSchema)
}

function syncRichToSchema(html: string) {
  syncing.value = true
  try {
    const cleaned = html || '<p></p>'
    form.layoutSchema = htmlToLayoutSchema(cleaned, form.layoutSchema)
    previewHtml.value = ensureLayoutArticleHtml(cleaned)
  } finally {
    endSync()
  }
}

watch(
  () => form.layoutSchema,
  () => {
    if (syncing.value || editMode.value !== 'structure') return
    syncing.value = true
    try {
      refreshPreviewFromSchema()
      richHtml.value = previewHtml.value
    } finally {
      endSync()
    }
  },
  { deep: true }
)

watch(richHtml, (html) => {
  if (syncing.value || editMode.value !== 'rich') return
  const cleaned = html || '<p></p>'
  if (normalizeRichHtml(cleaned) === normalizeRichHtml(previewHtml.value)) return
  syncRichToSchema(cleaned)
})

watch(editMode, (mode) => {
  syncing.value = true
  try {
    if (mode === 'rich') {
      richHtml.value = previewHtml.value || schemaToPreviewHtml(form.layoutSchema)
    } else {
      form.layoutSchema = htmlToLayoutSchema(richHtml.value, form.layoutSchema)
      refreshPreviewFromSchema()
    }
  } finally {
    endSync()
  }
})

async function loadDetail() {
  if (!templateId.value) {
    refreshPreviewFromSchema()
    richHtml.value = previewHtml.value
    return
  }
  const detail = await getLayoutTemplate(templateId.value)
  form.templateName = detail.templateName
  form.description = detail.description
  form.documentType = detail.documentType ?? null
  form.status = detail.status
  sourceType.value = detail.sourceType
  form.layoutSchema = detail.layoutSchema || emptyLayoutSchema()
  previewHtml.value = detail.previewHtml || detail.layoutHtml || schemaToPreviewHtml(form.layoutSchema)
  richHtml.value = previewHtml.value
}

async function handleSave() {
  await formRef.value?.validate()
  if (editMode.value === 'rich') {
    syncRichToSchema(richHtml.value)
  }
  saving.value = true
  try {
    if (isEdit.value && templateId.value) {
      await updateLayoutTemplate({ ...form, id: templateId.value })
      ElMessage.success('已更新')
    } else {
      await createLayoutTemplate(form)
      ElMessage.success('已创建')
    }
    router.push('/layout-template')
  } finally {
    saving.value = false
  }
}

async function handleCopy() {
  if (!templateId.value) return
  saving.value = true
  try {
    const newId = await copyLayoutTemplate(templateId.value)
    ElMessage.success('已复制，可编辑副本')
    router.push(`/layout-template/${newId}/edit`)
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  if (!templateId.value) return
  await ElMessageBox.confirm('确定发布该模板？发布后可被内容创作选用。', '发布模板', { type: 'info' })
  publishing.value = true
  try {
    await publishLayoutTemplate(templateId.value)
    form.status = 'ENABLED'
    ElMessage.success('已发布')
  } finally {
    publishing.value = false
  }
}

async function handleDisable() {
  if (!templateId.value) return
  await ElMessageBox.confirm('确定停用该模板？停用后不可在内容创作中选择。', '停用模板', { type: 'warning' })
  publishing.value = true
  try {
    await disableLayoutTemplate(templateId.value)
    form.status = 'DISABLED'
    ElMessage.success('已停用')
  } finally {
    publishing.value = false
  }
}

async function handleEnable() {
  if (!templateId.value) return
  await ElMessageBox.confirm('确定重新启用该模板？', '重新启用', { type: 'info' })
  publishing.value = true
  try {
    await enableLayoutTemplate(templateId.value)
    form.status = 'ENABLED'
    ElMessage.success('已重新启用')
  } finally {
    publishing.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.layout-template-edit {
  padding: 16px;
}

.preset-badge {
  margin-top: 12px;
}

.edit-form {
  margin-top: 16px;
  max-width: 1080px;
}

.layout-edit-tabs {
  width: 100%;
}
</style>
