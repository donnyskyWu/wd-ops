<template>
  <div class="layout-schema-editor">
    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="版式骨架编辑"
      description="模板仅存样式与槽位，不含营销正文。套用时将保留内容中的原有文字。"
      class="disclaimer"
    />
    <div class="toolbar">
      <el-button size="small" @click="addBlock('heading')">标题槽</el-button>
      <el-button size="small" @click="addBlock('slot', 'paragraph')">正文槽</el-button>
      <el-button size="small" @click="addBlock('slot', 'quote')">引用槽</el-button>
      <el-button size="small" @click="addBlock('divider')">分割线</el-button>
      <el-button size="small" @click="addBlock('frame')">图片框</el-button>
    </div>

    <div v-if="!localSchema.blocks.length" class="empty-tip">点击上方按钮添加版式槽位</div>

    <div v-for="(block, index) in localSchema.blocks" :key="index" class="block-item">
      <div class="block-actions">
        <span class="block-type">{{ blockLabel(block) }}</span>
        <el-button link type="danger" @click="removeBlock(index)">删除</el-button>
      </div>

      <template v-if="block.type === 'heading'">
        <el-select v-model="block.level" style="width: 120px" @change="emitChange">
          <el-option v-for="n in 6" :key="n" :label="`H${n}`" :value="n" />
        </el-select>
        <el-input v-model="block.styleRef" placeholder="styleRef" @input="emitChange" />
      </template>

      <template v-else-if="block.type === 'slot'">
        <el-tag type="info">{{ slotKindLabel(block.slotKind) }}</el-tag>
        <el-checkbox v-if="block.slotKind === 'paragraph'" v-model="block.repeat" @change="emitChange">
          可重复（循环段落）
        </el-checkbox>
        <el-checkbox v-model="block.optional" @change="emitChange">可选槽</el-checkbox>
        <el-input v-model="block.styleRef" placeholder="styleRef" @input="emitChange" />
      </template>

      <template v-else-if="block.type === 'divider'">
        <el-divider />
      </template>

      <template v-else-if="block.type === 'frame'">
        <el-tag>图片装饰框（可选）</el-tag>
      </template>

      <template v-else-if="block.type === 'fixed'">
        <el-tag type="warning">固定装饰块</el-tag>
      </template>
    </div>

    <el-card v-if="previewHtml" shadow="never" class="preview-card">
      <template #header>骨架预览</template>
      <LayoutViewer :html="previewHtml" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import LayoutViewer from './LayoutViewer.vue'
import type { LayoutSchema, LayoutSchemaBlock } from '@/types/layoutTemplate'
import { emptyLayoutSchema } from '@/types/layoutTemplate'

const props = defineProps<{
  modelValue: LayoutSchema
  previewHtml?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: LayoutSchema]
}>()

const localSchema = reactive<LayoutSchema>(emptyLayoutSchema())
const previewHtml = computed(() => props.previewHtml)

watch(
  () => props.modelValue,
  (val) => {
    const source = val || emptyLayoutSchema()
    localSchema.version = source.version || 2
    localSchema.globalStyles = JSON.parse(JSON.stringify(source.globalStyles || {}))
    localSchema.blocks = JSON.parse(JSON.stringify(source.blocks || []))
  },
  { immediate: true, deep: true }
)

function blockLabel(block: LayoutSchemaBlock) {
  if (block.type === 'slot') return `槽位 · ${slotKindLabel(block.slotKind)}`
  const map: Record<string, string> = {
    heading: '标题',
    divider: '分割线',
    frame: '图片框',
    fixed: '固定装饰'
  }
  return map[block.type] || block.type
}

function slotKindLabel(kind?: string) {
  const map: Record<string, string> = {
    paragraph: '正文',
    quote: '引用',
    heading: '标题',
    list: '列表',
    image: '图片'
  }
  return map[kind || ''] || kind || '未知'
}

function emitChange() {
  emit('update:modelValue', JSON.parse(JSON.stringify(localSchema)))
}

function addBlock(type: LayoutSchemaBlock['type'], slotKind?: string) {
  const block: LayoutSchemaBlock = { type, styleRef: 'paragraph' }
  if (type === 'heading') {
    block.level = 2
    block.styleRef = 'heading2'
    block.slotKind = 'heading'
  } else if (type === 'slot') {
    block.slotKind = slotKind || 'paragraph'
    block.styleRef = slotKind === 'quote' ? 'quote' : 'paragraph'
    if (slotKind === 'paragraph') block.repeat = true
  } else if (type === 'divider') {
    block.styleRef = 'divider'
  } else if (type === 'frame') {
    block.slotKind = 'image'
    block.styleRef = 'image'
    block.optional = true
  }
  localSchema.blocks.push(block)
  emitChange()
}

function removeBlock(index: number) {
  localSchema.blocks.splice(index, 1)
  emitChange()
}
</script>

<style scoped>
.layout-schema-editor {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.disclaimer {
  margin-bottom: 12px;
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
