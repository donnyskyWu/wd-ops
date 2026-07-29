<template>
  <el-drawer
    direction="rtl"
    :model-value="visible"
    :title="dialogTitle"
    size="70%"
    destroy-on-close
    append-to-body
    class="content-edit-drawer"
    @update:model-value="(val: boolean) => emit('update:visible', val)"
  >
    <ContentEditPanel
      v-if="visible"
      embedded
      :content-id="contentId"
      :task-id="taskId"
      :competition-id="competitionId"
      :readonly="readonly"
      @saved="handleSaved"
      @cancelled="emit('update:visible', false)"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ContentEditPanel from './ContentEditPanel.vue'

const props = defineProps<{
  visible: boolean
  contentId?: number
  taskId?: number
  competitionId?: string
  readonly?: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: [payload?: { contentId?: number; action: 'draft' | 'review' }]
}>()

const dialogTitle = computed(() => {
  if (props.readonly && props.contentId && !props.taskId) return '查看内容'
  if (props.readonly && props.taskId) return '查看任务内容'
  if (props.taskId) return props.contentId ? '编辑任务内容' : '任务内容创作'
  return props.contentId ? '编辑内容' : '新增内容'
})

const handleSaved = (payload?: { contentId?: number; action: 'draft' | 'review' }) => {
  emit('saved', payload)
  emit('update:visible', false)
}
</script>

<style scoped>
.content-edit-drawer,
.content-edit-drawer :deep(.el-drawer__body) {
  background-color: var(--el-bg-color, #fff);
}
</style>
