<template>
  <div class="layout-resource-sidebar" :class="{ collapsed: collapsed }">
    <div class="sidebar-header">
      <span v-if="!collapsed" class="sidebar-title">版式资源</span>
      <el-button
        :icon="collapsed ? Expand : Fold"
        size="small"
        text
        :title="collapsed ? '展开' : '收起'"
        @click="toggleCollapse"
      />
    </div>
    <template v-if="!collapsed">
      <el-tabs v-model="activeTab" class="sidebar-tabs">
        <el-tab-pane label="样式" name="style" />
        <el-tab-pane label="模板" name="template" />
        <el-tab-pane label="一键排版" name="typeset" />
      </el-tabs>
      <div class="sidebar-body">
        <StyleLibraryPanel
          v-if="activeTab === 'style'"
          :insert-html="insertHtml"
          :apply-style-to-selection="applyStyleToSelection"
        />
        <TemplateLibraryPanel
          v-else-if="activeTab === 'template'"
          :document-type="documentType"
          :body="body"
          :existing-layout-json="existingLayoutJson"
          @applied="onTemplateApplied"
        />
        <TypesettingPanel
          v-else-if="activeTab === 'typeset'"
          :html="html"
          :body="body"
          :document-type="documentType"
          @applied="onTypesetApplied"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Expand, Fold } from '@element-plus/icons-vue'
import StyleLibraryPanel from './StyleLibraryPanel.vue'
import TemplateLibraryPanel from './TemplateLibraryPanel.vue'
import TypesettingPanel from './TypesettingPanel.vue'

const STORAGE_KEY = 'm2-layout-sidebar-collapsed'
const STORAGE_TAB_KEY = 'm2-layout-sidebar-tab'

const props = defineProps<{
  documentType?: string
  body: string
  html: string
  existingLayoutJson?: unknown
  insertHtml?: (html: string) => void
  applyStyleToSelection?: (html: string) => void
}>()

const emit = defineEmits<{
  templateApplied: [payload: { layoutJson: unknown; layoutHtml: string; templateId: number; mode: string }]
  typesetApplied: [payload: { html: string; layoutJson?: unknown; templateId?: number; mode: string }]
}>()

const collapsed = ref(false)
const activeTab = ref('style')

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem(STORAGE_KEY, collapsed.value ? '1' : '0')
}

function onTemplateApplied(payload: { layoutJson: unknown; layoutHtml: string; templateId: number; mode: string }) {
  emit('templateApplied', payload)
}

function onTypesetApplied(payload: { html: string; layoutJson?: unknown; templateId?: number; mode: string }) {
  emit('typesetApplied', payload)
}

onMounted(() => {
  collapsed.value = localStorage.getItem(STORAGE_KEY) === '1'
  const savedTab = localStorage.getItem(STORAGE_TAB_KEY)
  if (savedTab === 'style' || savedTab === 'template' || savedTab === 'typeset') {
    activeTab.value = savedTab
  }
})

watch(activeTab, (tab) => {
  localStorage.setItem(STORAGE_TAB_KEY, tab)
})
</script>

<style scoped>
.layout-resource-sidebar {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}

.layout-resource-sidebar.collapsed {
  min-height: auto;
  width: 40px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
}

.sidebar-tabs {
  padding: 0 8px;
}

.sidebar-body {
  flex: 1;
  overflow: hidden;
  padding: 8px 10px 12px;
}
</style>
