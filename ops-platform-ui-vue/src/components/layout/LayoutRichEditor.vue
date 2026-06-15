<template>
  <div class="layout-rich-editor">
    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="富文本版式编辑"
      description="直接编辑版式骨架的渲染效果。切换至「结构编辑」可调整槽位；两处内容双向同步（复杂样式可能无法完全还原）。"
      class="disclaimer"
    />
    <RichTextEditor
      v-model="localHtml"
      :disabled="disabled"
      :readonly="readonly"
      :placeholder="placeholder"
      :min-height="minHeight"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    disabled?: boolean
    readonly?: boolean
    placeholder?: string
    minHeight?: string
  }>(),
  {
    modelValue: '',
    disabled: false,
    readonly: false,
    placeholder: '编辑版式骨架预览 HTML…',
    minHeight: '320px',
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const localHtml = ref(props.modelValue || '')

watch(
  () => props.modelValue,
  (val) => {
    const next = val || ''
    if (localHtml.value !== next) localHtml.value = next
  },
  { immediate: true }
)

watch(localHtml, (val) => {
  emit('update:modelValue', val || '')
})
</script>

<style scoped>
.layout-rich-editor .disclaimer {
  margin-bottom: 12px;
}
</style>
