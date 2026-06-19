<template>
  <DictSelect
    v-if="queryConditionType === 'DICT' || queryConditionType === 'PLATFORM_SELECT'"
    :model-value="modelValue"
    :dict-type="dictType || (queryConditionType === 'PLATFORM_SELECT' ? 'dict_platform_type' : '')"
    :placeholder="placeholder"
    clearable
    :style="inputStyle"
    @update:model-value="emitValue"
  />
  <AccountSelect
    v-else-if="queryConditionType === 'ACCOUNT_SELECT'"
    :model-value="numValue"
    :placeholder="placeholder"
    :style="inputStyle"
    @update:model-value="emitNumValue"
  />
  <IpGroupTreeSelect
    v-else-if="queryConditionType === 'IP_GROUP_SELECT'"
    :model-value="numValue"
    :placeholder="placeholder"
    :style="inputStyle"
    @update:model-value="emitNumValue"
  />
  <UserSelect
    v-else-if="queryConditionType === 'USER_SELECT'"
    :model-value="numValue"
    :placeholder="placeholder"
    :style="inputStyle"
    @update:model-value="emitNumValue"
  />
  <el-input-number
    v-else-if="queryConditionType === 'NUMBER'"
    :model-value="numValue"
    :placeholder="placeholder"
    controls-position="right"
    :style="inputStyle"
    @update:model-value="emitNumValue"
  />
  <el-date-picker
    v-else-if="queryConditionType === 'DATE'"
    :model-value="modelValue || undefined"
    type="date"
    value-format="YYYY-MM-DD"
    :placeholder="placeholder"
    :style="inputStyle"
    @update:model-value="emitValue"
  />
  <el-date-picker
    v-else-if="queryConditionType === 'DATE_RANGE'"
    :model-value="dateRangeValue"
    type="daterange"
    range-separator="至"
    start-placeholder="开始"
    end-placeholder="结束"
    value-format="YYYY-MM-DD"
    :style="inputStyle"
    @update:model-value="emitDateRange"
  />
  <el-input
    v-else
    :model-value="modelValue"
    :placeholder="placeholder"
    :style="inputStyle"
    @update:model-value="emitValue"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DictSelect from '@/components/DictSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import type { MetadataQueryConditionType } from '@/api/metadata'

const props = withDefaults(defineProps<{
  modelValue: string
  queryConditionType?: MetadataQueryConditionType
  dictType?: string
  placeholder?: string
  style?: string | Record<string, string>
}>(), {
  queryConditionType: 'TEXT',
  placeholder: '请输入',
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
}>()

const inputStyle = computed(() => props.style ?? 'width: 100%')

const numValue = computed(() => {
  if (!props.modelValue) return undefined
  const n = Number(props.modelValue)
  return Number.isNaN(n) ? undefined : n
})

const dateRangeValue = computed((): [string, string] | undefined => {
  if (!props.modelValue) return undefined
  const parts = props.modelValue.split(',')
  if (parts.length >= 2 && parts[0] && parts[1]) return [parts[0], parts[1]]
  return undefined
})

function emitValue(v: string | undefined) {
  emit('update:modelValue', v ?? '')
}

function emitNumValue(v: number | undefined) {
  emit('update:modelValue', v != null ? String(v) : '')
}

function emitDateRange(v: [string, string] | null | undefined) {
  emit('update:modelValue', v?.length === 2 ? `${v[0]},${v[1]}` : '')
}
</script>
