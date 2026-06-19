<template>
  <div class="template-library-panel">
    <el-input
      v-model="keyword"
      placeholder="搜索模板"
      clearable
      size="small"
      class="search-input"
      @input="loadTemplates"
    />
    <div v-loading="loading" class="template-list">
      <div
        v-for="item in templates"
        :key="item.id"
        class="template-item"
        :class="{ active: selected?.id === item.id }"
        @click="selectTemplate(item)"
      >
        <span class="template-name">{{ item.templateName }}</span>
        <span class="template-doc">{{ item.documentType || '通用' }}</span>
      </div>
      <el-empty v-if="!loading && !templates.length" description="暂无模板" :image-size="48" />
    </div>
    <LayoutParamSliders v-model="paramOverrides" :params="paramDefs" />
    <ResourcePreviewPane :html="previewHtml" :title="selected?.templateName" :loading="previewLoading" />
    <div class="panel-actions">
      <el-button type="primary" size="small" :disabled="!selected" :loading="applying" @click="applyFull">
        全部使用
      </el-button>
      <el-button size="small" :disabled="!selected" :loading="applying" @click="applyPartial">
        分开使用
      </el-button>
      <el-button size="small" :disabled="!selected" :loading="applying" @click="applyBg">
        使用背景
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listLayoutTemplateSelect,
  getLayoutTemplate,
  previewTemplateMerge,
  partialApplyTemplate,
  applyTemplateBackground,
} from '@/api/layoutTemplate'
import type { LayoutTemplateSelectVO, LayoutTemplateDetailVO } from '@/types/layoutTemplate'
import LayoutParamSliders, { type LayoutParamDef } from './LayoutParamSliders.vue'
import ResourcePreviewPane from './ResourcePreviewPane.vue'

const props = defineProps<{
  documentType?: string
  body: string
  existingLayoutJson?: unknown
}>()

const emit = defineEmits<{
  applied: [payload: { layoutJson: unknown; layoutHtml: string; templateId: number; mode: string }]
}>()

const keyword = ref('')
const loading = ref(false)
const previewLoading = ref(false)
const applying = ref(false)
const templates = ref<LayoutTemplateSelectVO[]>([])
const selected = ref<LayoutTemplateSelectVO | null>(null)
const detail = ref<LayoutTemplateDetailVO | null>(null)
const previewHtml = ref('')
const paramOverrides = ref<Record<string, Record<string, string>>>({})

const paramDefs: LayoutParamDef[] = [
  { key: 'fontSize', label: '正文字号', min: 14, max: 20, step: 1, unit: 'px', styleRef: 'paragraph', cssProp: 'fontSize' },
  { key: 'lineHeight', label: '行高', min: 14, max: 20, step: 1, unit: 'px', styleRef: 'paragraph', cssProp: 'lineHeight' },
  { key: 'headingColor', label: '标题色', min: 0, max: 255, step: 1, unit: '', styleRef: 'heading2', cssProp: 'color' },
]

async function loadTemplates() {
  loading.value = true
  try {
    const list = await listLayoutTemplateSelect('ARTICLE', props.documentType)
    templates.value = keyword.value
      ? list.filter((t) => t.templateName.includes(keyword.value))
      : list
  } finally {
    loading.value = false
  }
}

async function selectTemplate(item: LayoutTemplateSelectVO) {
  selected.value = item
  previewLoading.value = true
  try {
    detail.value = await getLayoutTemplate(item.id)
    previewHtml.value = detail.value.previewHtml || detail.value.layoutHtml || ''
    if (props.body.trim()) {
      const preview = await previewTemplateMerge(
        item.id,
        props.body,
        props.existingLayoutJson,
        paramOverrides.value,
      )
      previewHtml.value = preview.layoutHtml || previewHtml.value
    }
  } finally {
    previewLoading.value = false
  }
}

watch(paramOverrides, async () => {
  if (!selected.value || !props.body.trim()) return
  previewLoading.value = true
  try {
    const preview = await previewTemplateMerge(
      selected.value.id,
      props.body,
      props.existingLayoutJson,
      paramOverrides.value,
    )
    previewHtml.value = preview.layoutHtml || previewHtml.value
  } finally {
    previewLoading.value = false
  }
}, { deep: true })

async function applyFull() {
  if (!selected.value) return
  applying.value = true
  try {
    const result = await previewTemplateMerge(
      selected.value.id,
      props.body,
      props.existingLayoutJson,
      paramOverrides.value,
    )
    emit('applied', {
      layoutJson: result.layoutJson,
      layoutHtml: result.layoutHtml,
      templateId: selected.value.id,
      mode: 'full',
    })
    ElMessage.success('已应用模板（全部）')
  } finally {
    applying.value = false
  }
}

async function applyPartial() {
  if (!selected.value) return
  applying.value = true
  try {
    const result = await partialApplyTemplate(
      selected.value.id,
      props.body,
      ['heading', 'slot'],
      paramOverrides.value,
      props.existingLayoutJson,
    )
    emit('applied', {
      layoutJson: result.layoutJson,
      layoutHtml: result.layoutHtml,
      templateId: selected.value.id,
      mode: 'partial',
    })
    ElMessage.success('已应用模板（分开使用）')
  } finally {
    applying.value = false
  }
}

async function applyBg() {
  if (!selected.value) return
  applying.value = true
  try {
    const result = await applyTemplateBackground(
      selected.value.id,
      props.body,
      paramOverrides.value,
      props.existingLayoutJson,
    )
    emit('applied', {
      layoutJson: result.layoutJson,
      layoutHtml: result.layoutHtml,
      templateId: selected.value.id,
      mode: 'background',
    })
    ElMessage.success('已应用背景样式')
  } finally {
    applying.value = false
  }
}

onMounted(loadTemplates)
</script>

<style scoped>
.template-library-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}

.search-input {
  flex-shrink: 0;
}

.template-list {
  max-height: 140px;
  overflow-y: auto;
}

.template-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.template-item:hover,
.template-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.template-doc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
