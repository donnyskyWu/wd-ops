<template>

  <div class="typesetting-panel">

    <el-radio-group v-model="mode" size="small" class="mode-tabs">

      <el-radio-button value="TEMPLATE">按模板排版</el-radio-button>

      <el-radio-button value="AUTO">智能优化排版</el-radio-button>

    </el-radio-group>



    <template v-if="mode === 'TEMPLATE'">

      <p class="panel-desc">选择公推模板，一键将版式骨架套用到正文（文字不变，ADR-020）。</p>

      <el-input

        v-model="keyword"

        placeholder="搜索模板"

        clearable

        size="small"

        @input="loadTemplates"

      />

      <div v-loading="tplLoading" class="template-list">

        <div

          v-for="item in templates"

          :key="item.id"

          class="template-item"

          :class="{ active: selectedTemplate?.id === item.id }"

          @click="selectTemplate(item)"

        >

          <span class="template-name">{{ item.templateName }}</span>

          <span class="template-doc">{{ item.documentType || '通用' }}</span>

        </div>

        <el-empty v-if="!tplLoading && !templates.length" description="暂无模板" :image-size="48" />

      </div>

      <LayoutParamSliders v-model="paramOverrides" :params="paramDefs" />

      <ResourcePreviewPane :html="previewHtml" :title="selectedTemplate?.templateName" :loading="previewLoading" />

    </template>



    <template v-else>

      <p class="panel-desc">智能识别标题并应用样式库规则，文字内容保持不变。下方「预置版式」可一键套用公推模板骨架。</p>

      <div v-loading="loading" class="rule-list">

        <el-checkbox-group v-model="selectedRules">

          <template v-for="rule in autoRules" :key="rule.ruleCode">

            <p v-if="rule === firstTemplateLinkRule" class="rule-section-label">预置版式快捷套用</p>

            <el-checkbox

              :label="rule.ruleCode"

              class="rule-item"

              :class="{ 'rule-item--preset': isTemplateLinkRule(rule) }"

            >

              <span class="rule-name">{{ rule.name }}</span>

              <span v-if="rule.description" class="rule-desc">{{ rule.description }}</span>

              <el-tag v-if="isSmartRule(rule)" size="small" type="success" class="rule-tag">推荐</el-tag>

              <el-tag v-else-if="isTemplateLinkRule(rule)" size="small" type="warning" class="rule-tag">

                {{ getLinkedTemplateName(rule) || '预置模板' }}

              </el-tag>

              <el-tag v-else-if="getLinkedTemplateId(rule)" size="small" type="info" class="rule-tag">

                关联模板

              </el-tag>

            </el-checkbox>

          </template>

        </el-checkbox-group>

        <el-empty v-if="!loading && !autoRules.length" description="暂无排版规则" :image-size="48" />

      </div>

    </template>



    <el-button type="primary" size="small" :loading="typesetting" @click="runTypeset">

      一键排版

    </el-button>



    <TypesetCompareDialog

      v-model="compareVisible"

      :before-html="compareBefore"

      :after-html="compareAfter"

      :plain-text-before="comparePlainBefore"

      :plain-text-after="comparePlainAfter"

      @confirm="applyTypeset"

    />

  </div>

</template>



<script setup lang="ts">

import { ref, computed, onMounted, watch } from 'vue'

import { ElMessage } from 'element-plus'

import {

  listEnabledTypesettingRules,

  typesetContent,

  getLinkedTemplateId,

  isSmartRule,

  isTemplateLinkRule,

  type TypesettingRuleVO,

} from '@/api/typesetting'

import {

  listLayoutTemplateSelect,

  getLayoutTemplate,

  previewTemplateMerge,

} from '@/api/layoutTemplate'

import type { LayoutTemplateSelectVO } from '@/types/layoutTemplate'

import TypesetCompareDialog from './TypesetCompareDialog.vue'

import LayoutParamSliders, { type LayoutParamDef } from './LayoutParamSliders.vue'

import ResourcePreviewPane from './ResourcePreviewPane.vue'



const props = defineProps<{

  html: string

  body?: string

  documentType?: string

}>()



const emit = defineEmits<{

  applied: [payload: { html: string; layoutJson?: unknown; templateId?: number; mode: string }]

}>()



const mode = ref<'TEMPLATE' | 'AUTO'>('AUTO')

const loading = ref(false)

const tplLoading = ref(false)

const previewLoading = ref(false)

const typesetting = ref(false)

const rules = ref<TypesettingRuleVO[]>([])

const selectedRules = ref<string[]>([])

const keyword = ref('')

const templates = ref<LayoutTemplateSelectVO[]>([])

const templateNameById = ref<Record<number, string>>({})

const selectedTemplate = ref<LayoutTemplateSelectVO | null>(null)

const previewHtml = ref('')

const paramOverrides = ref<Record<string, Record<string, string>>>({})

const compareVisible = ref(false)

const compareBefore = ref('')

const compareAfter = ref('')

const comparePlainBefore = ref('')

const comparePlainAfter = ref('')

const pendingResult = ref<{ html: string; layoutJson?: unknown; templateId?: number; mode: string } | null>(null)



const paramDefs: LayoutParamDef[] = [

  { key: 'fontSize', label: '正文字号', min: 14, max: 20, step: 1, unit: 'px', styleRef: 'paragraph', cssProp: 'fontSize' },

  { key: 'lineHeight', label: '行高', min: 14, max: 24, step: 1, unit: 'px', styleRef: 'paragraph', cssProp: 'lineHeight' },

]



const autoRules = computed(() => {
  const visible = rules.value.filter((r) => !isTemplateLinkRule(r) || getLinkedTemplateId(r))
  const smart = visible.filter(isSmartRule)
  const preset = visible.filter(isTemplateLinkRule).sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
  const basic = visible.filter((r) => !isSmartRule(r) && !isTemplateLinkRule(r)).sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
  return [...smart, ...preset, ...basic]
})

const firstTemplateLinkRule = computed(() => autoRules.value.find(isTemplateLinkRule))

function getLinkedTemplateName(rule: TypesettingRuleVO): string | undefined {
  const id = getLinkedTemplateId(rule)
  return id ? templateNameById.value[id] : undefined
}



async function loadRules() {

  loading.value = true

  try {

    rules.value = await listEnabledTypesettingRules()

    const smart = rules.value.find(isSmartRule)

    selectedRules.value = smart

      ? [smart.ruleCode]

      : rules.value.filter((r) => !isTemplateLinkRule(r)).map((r) => r.ruleCode)

  } finally {

    loading.value = false

  }

}



async function loadTemplateNames() {

  const all = await listLayoutTemplateSelect('ARTICLE')

  templateNameById.value = Object.fromEntries(all.map((t) => [t.id, t.templateName]))

}



async function loadTemplates() {

  tplLoading.value = true

  try {

    const list = await listLayoutTemplateSelect('ARTICLE', props.documentType)

    templates.value = keyword.value

      ? list.filter((t) => t.templateName.includes(keyword.value))

      : list

  } finally {

    tplLoading.value = false

  }

}



async function selectTemplate(item: LayoutTemplateSelectVO) {

  selectedTemplate.value = item

  previewLoading.value = true

  try {

    const body = props.body?.trim() || ''

    if (body) {

      const preview = await previewTemplateMerge(item.id, body, undefined, paramOverrides.value)

      previewHtml.value = preview.layoutHtml || ''

    } else {

      const detail = await getLayoutTemplate(item.id)

      previewHtml.value = detail.previewHtml || detail.layoutHtml || ''

    }

  } finally {

    previewLoading.value = false

  }

}



watch(paramOverrides, async () => {

  if (mode.value !== 'TEMPLATE' || !selectedTemplate.value || !props.body?.trim()) return

  previewLoading.value = true

  try {

    const preview = await previewTemplateMerge(

      selectedTemplate.value.id,

      props.body,

      undefined,

      paramOverrides.value,

    )

    previewHtml.value = preview.layoutHtml || ''

  } finally {

    previewLoading.value = false

  }

}, { deep: true })



async function runTypeset() {

  if (!props.html?.trim() && !props.body?.trim()) {

    ElMessage.warning('正文为空，无法排版')

    return

  }

  if (mode.value === 'TEMPLATE' && !selectedTemplate.value) {

    ElMessage.warning('请选择模板')

    return

  }

  typesetting.value = true

  try {

    const result = await typesetContent({

      html: props.html,

      body: props.body,

      mode: mode.value,

      templateId: mode.value === 'TEMPLATE' ? selectedTemplate.value!.id : undefined,

      ruleCodes: mode.value === 'AUTO' && selectedRules.value.length ? selectedRules.value : undefined,

      paramOverrides: mode.value === 'TEMPLATE' ? paramOverrides.value : undefined,

    })

    compareBefore.value = props.html

    compareAfter.value = result.html

    comparePlainBefore.value = result.plainTextBefore

    comparePlainAfter.value = result.plainTextAfter

    pendingResult.value = {

      html: result.html,

      layoutJson: result.layoutJson,

      templateId: result.templateId,

      mode: result.mode || mode.value,

    }

    compareVisible.value = true

  } finally {

    typesetting.value = false

  }

}



function applyTypeset() {

  if (!pendingResult.value) return

  emit('applied', pendingResult.value)

  ElMessage.success('排版已应用')

}



onMounted(() => {

  loadRules()

  loadTemplateNames()

  loadTemplates()

})

</script>



<style scoped>

.typesetting-panel {

  display: flex;

  flex-direction: column;

  gap: 10px;

}



.mode-tabs {

  width: 100%;

}



.panel-desc {

  font-size: 12px;

  color: var(--el-text-color-secondary);

  margin: 0;

}



.rule-list,

.template-list {

  max-height: 140px;

  overflow-y: auto;

}



.rule-item {

  display: flex;

  flex-wrap: wrap;

  align-items: center;

  gap: 4px;

  margin-bottom: 8px;

  height: auto;

}



.template-item {

  display: flex;

  justify-content: space-between;

  padding: 8px 10px;

  border-radius: 4px;

  cursor: pointer;

  font-size: 13px;

  margin-bottom: 4px;

}



.template-item:hover,

.template-item.active {

  background: var(--el-color-primary-light-9);

  color: var(--el-color-primary);

}



.rule-name,

.template-name {

  font-size: 13px;

}



.rule-desc,

.template-doc {

  font-size: 11px;

  color: var(--el-text-color-secondary);

  width: 100%;

}



.rule-tag {

  margin-left: 4px;

}

.rule-section-label {

  font-size: 12px;

  font-weight: 600;

  color: var(--el-color-warning);

  margin: 8px 0 4px;

}

.rule-item--preset {

  background: var(--el-color-warning-light-9);

  border-radius: 4px;

  padding: 4px 6px;

}

</style>


