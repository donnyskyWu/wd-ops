<template>
  <div class="style-library-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索样式"
        clearable
        size="small"
        @input="debouncedLoad"
      />
    </div>
    <el-tabs v-model="activeCategory" @tab-change="loadStyles">
      <el-tab-pane
        v-for="cat in categories"
        :key="cat.value"
        :label="cat.label"
        :name="cat.value"
      />
    </el-tabs>
    <div v-loading="loading" class="style-cards">
      <div
        v-for="item in styles"
        :key="item.id"
        class="style-card"
        :class="{ active: selected?.id === item.id }"
        @click="selectStyle(item)"
      >
        <div class="style-card-name">{{ item.name }}</div>
        <div class="style-card-preview" v-html="item.htmlSnippet" />
      </div>
      <el-empty v-if="!loading && !styles.length" description="暂无样式" :image-size="48" />
    </div>
    <ResourcePreviewPane :html="selected?.htmlSnippet" :title="selected?.name" />
    <div class="panel-actions">
      <el-button type="primary" size="small" :disabled="!selected" @click="insertSelected">
        插入光标处
      </el-button>
      <el-button size="small" :disabled="!selected" @click="applyToSelection">
        应用到选中
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listEnabledLayoutStyles, type LayoutStyleVO } from '@/api/layoutStyle'
import ResourcePreviewPane from './ResourcePreviewPane.vue'

const props = defineProps<{
  insertHtml?: (html: string) => void
  applyStyleToSelection?: (html: string) => void
}>()

const categories = [
  { label: '全部', value: '' },
  { label: '标题', value: 'HEADING' },
  { label: '正文', value: 'BODY' },
  { label: '图文', value: 'IMAGE_TEXT' },
  { label: '引导', value: 'GUIDE' },
  { label: '分隔', value: 'DIVIDER' },
]

const activeCategory = ref('')
const keyword = ref('')
const loading = ref(false)
const styles = ref<LayoutStyleVO[]>([])
const selected = ref<LayoutStyleVO | null>(null)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function debouncedLoad() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(loadStyles, 300)
}

async function loadStyles() {
  loading.value = true
  try {
    styles.value = await listEnabledLayoutStyles(
      activeCategory.value || undefined,
      keyword.value || undefined,
    )
  } finally {
    loading.value = false
  }
}

function selectStyle(item: LayoutStyleVO) {
  selected.value = item
}

function insertSelected() {
  if (!selected.value || !props.insertHtml) {
    ElMessage.warning('请先选择样式')
    return
  }
  props.insertHtml(selected.value.htmlSnippet)
  ElMessage.success('已插入样式')
}

function applyToSelection() {
  if (!selected.value || !props.applyStyleToSelection) {
    ElMessage.warning('请先选中编辑器内容')
    return
  }
  props.applyStyleToSelection(selected.value.htmlSnippet)
  ElMessage.success('已应用到选中内容')
}

onMounted(loadStyles)
</script>

<style scoped>
.style-library-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}

.panel-toolbar {
  flex-shrink: 0;
}

.style-cards {
  flex: 1;
  overflow-y: auto;
  max-height: 200px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.style-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 6px;
  cursor: pointer;
  transition: border-color 0.2s;
  overflow: hidden;
}

.style-card:hover,
.style-card.active {
  border-color: var(--el-color-primary);
}

.style-card-name {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.style-card-preview {
  font-size: 11px;
  transform: scale(0.85);
  transform-origin: top left;
  max-height: 48px;
  overflow: hidden;
  pointer-events: none;
}

.panel-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
