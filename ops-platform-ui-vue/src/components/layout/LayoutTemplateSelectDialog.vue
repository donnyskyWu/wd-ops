<template>
  <el-dialog v-model="visible" title="选择公推模板" width="720px" destroy-on-close @open="loadOptions">
    <el-table v-loading="loading" :data="options" highlight-current-row @current-change="handleSelect">
      <el-table-column prop="templateName" label="模板名称" min-width="200" />
      <el-table-column prop="documentType" label="文档类型" width="140">
        <template #default="{ row }">
          {{ row.documentType || '通用' }}
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selected" @click="confirm">应用模板</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { listLayoutTemplateSelect } from '@/api/layoutTemplate'
import type { LayoutTemplateSelectVO } from '@/types/layoutTemplate'

const props = defineProps<{
  modelValue: boolean
  documentType?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  select: [template: LayoutTemplateSelectVO]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const options = ref<LayoutTemplateSelectVO[]>([])
const selected = ref<LayoutTemplateSelectVO | null>(null)

async function loadOptions() {
  loading.value = true
  selected.value = null
  try {
    options.value = await listLayoutTemplateSelect('ARTICLE', props.documentType)
  } finally {
    loading.value = false
  }
}

function handleSelect(row: LayoutTemplateSelectVO | undefined) {
  selected.value = row || null
}

function confirm() {
  if (selected.value) {
    emit('select', selected.value)
    visible.value = false
  }
}
</script>
