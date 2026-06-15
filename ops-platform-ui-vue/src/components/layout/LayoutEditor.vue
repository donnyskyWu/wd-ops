<template>
  <div class="layout-editor">
    <div class="toolbar">
      <el-button size="small" @click="addBlock('heading')">标题</el-button>
      <el-button size="small" @click="addBlock('paragraph')">段落</el-button>
      <el-button size="small" @click="addBlock('quote')">引用</el-button>
      <el-button size="small" @click="addBlock('divider')">分割线</el-button>
      <el-button size="small" @click="addBlock('image')">图片</el-button>
    </div>

    <div v-if="!localDoc.blocks.length" class="empty-tip">点击上方按钮添加版式块</div>

    <div v-for="(block, index) in localDoc.blocks" :key="index" class="block-item">
      <div class="block-actions">
        <span class="block-type">{{ blockLabel(block.type) }}</span>
        <el-button link type="danger" @click="removeBlock(index)">删除</el-button>
      </div>

      <template v-if="block.type === 'heading'">
        <el-input v-model="block.text" placeholder="标题文字" @input="emitChange" />
        <el-select v-model="block.level" style="width: 120px; margin-top: 8px" @change="emitChange">
          <el-option v-for="n in 6" :key="n" :label="`H${n}`" :value="n" />
        </el-select>
      </template>

      <template v-else-if="block.type === 'paragraph'">
        <el-input
          :model-value="paragraphText(block)"
          type="textarea"
          :rows="4"
          placeholder="段落正文（TipTap 块编辑简化版）"
          @update:model-value="(val: string) => setParagraphText(block, val)"
        />
      </template>

      <template v-else-if="block.type === 'quote'">
        <el-input v-model="block.text" type="textarea" :rows="3" placeholder="引用内容" @input="emitChange" />
      </template>

      <template v-else-if="block.type === 'image'">
        <el-input v-model="block.src" placeholder="图片 URL" @input="emitChange" />
      </template>

      <template v-else-if="block.type === 'divider'">
        <el-divider />
      </template>
    </div>

    <el-card v-if="showPreview && previewHtml" shadow="never" class="preview-card">
      <template #header>预览</template>
      <LayoutViewer :html="previewHtml" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import LayoutViewer from './LayoutViewer.vue'
import type { LayoutBlock, LayoutDocument } from '@/types/layoutTemplate'
import { emptyLayoutDocument } from '@/types/layoutTemplate'

const props = withDefaults(
  defineProps<{
    modelValue: LayoutDocument
    previewHtml?: string
    showPreview?: boolean
  }>(),
  { showPreview: true }
)

const emit = defineEmits<{
  'update:modelValue': [value: LayoutDocument]
}>()

const localDoc = reactive<LayoutDocument>(emptyLayoutDocument())

watch(
  () => props.modelValue,
  (val) => {
    const source = val || emptyLayoutDocument()
    localDoc.version = source.version || 1
    localDoc.blocks = JSON.parse(JSON.stringify(source.blocks || []))
  },
  { immediate: true, deep: true }
)

const previewHtml = computed(() => props.previewHtml)

function blockLabel(type: string) {
  const map: Record<string, string> = {
    heading: '标题',
    paragraph: '段落',
    quote: '引用',
    divider: '分割线',
    image: '图片',
    list: '列表'
  }
  return map[type] || type
}

function emitChange() {
  emit('update:modelValue', JSON.parse(JSON.stringify(localDoc)))
}

function addBlock(type: LayoutBlock['type']) {
  const block: LayoutBlock = { type }
  if (type === 'heading') {
    block.level = 2
    block.text = ''
  } else if (type === 'paragraph') {
    block.align = 'left'
    block.children = [{ text: '', bold: false, italic: false }]
  } else if (type === 'quote') {
    block.text = ''
  } else if (type === 'image') {
    block.src = ''
    block.width = '100%'
  }
  localDoc.blocks.push(block)
  emitChange()
}

function removeBlock(index: number) {
  localDoc.blocks.splice(index, 1)
  emitChange()
}

function paragraphText(block: LayoutBlock) {
  return block.children?.[0]?.text || ''
}

function setParagraphText(block: LayoutBlock, text: string) {
  if (!block.children?.length) {
    block.children = [{ text, bold: false, italic: false }]
  } else {
    block.children[0].text = text
  }
  emitChange()
}
</script>

<style scoped>
.layout-editor {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.block-item {
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}

.block-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.block-type {
  font-size: 12px;
  color: #909399;
}

.empty-tip {
  color: #909399;
  text-align: center;
  padding: 24px 0;
}

.preview-card {
  margin-top: 16px;
}
</style>
