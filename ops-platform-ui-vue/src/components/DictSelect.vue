<template>
  <el-select
    v-model="model"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="filterable"
    :multiple="multiple"
    :loading="loading"
    :style="{ width: '100%' }"
  >
    <el-option
      v-for="opt in options"
      :key="opt.value"
      :label="opt.label"
      :value="opt.value"
    />
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { fetchDictData, type DictItemVO } from '@/api/dict'

// P-GATE-UNMOCK-R (2026-06-09)：原 prop 名 `type`（必填）导致 30+ 调用方（用 `dict-type`）全部不匹配
// 修复：prop 改可选 + 同时接受 `type`（旧 Author.vue 风格）和 `dictType`（kebab-case `dict-type`）
// 优先级：dictType > type（前者是规范，后者兼容）
const props = defineProps<{
  type?: string
  dictType?: string
  modelValue?: string | number | string[] | number[] | null
  placeholder?: string
  clearable?: boolean
  filterable?: boolean
  multiple?: boolean
  includeValues?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: string | number | string[] | number[] | null): void
  (e: 'change', val: string | number | string[] | number[] | null): void
}>()

const model = ref<string | number | string[] | number[] | null>(props.modelValue ?? (props.multiple ? [] : null))
const options = ref<DictItemVO[]>([])
const loading = ref(false)

/** 统一取值：dictType 优先，兼容旧 `type` */
const effectiveType = computed(() => props.dictType || props.type || '')

watch(() => props.modelValue, (v) => {
  model.value = v ?? (props.multiple ? [] : null)
})
watch(model, (v) => {
  emit('update:modelValue', v)
  emit('change', v)
})

const load = async () => {
  const t = effectiveType.value
  if (!t) {
    options.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await fetchDictData(t)
    options.value = ((res?.list || []) as DictItemVO[]).filter((item) => {
      if (!props.includeValues?.length) return true
      return props.includeValues.includes(String(item.value))
    })
  } catch (e) {
    options.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(effectiveType, load)
// P-GATE-UNMOCK-R S-R2-H：清除"全部"等空 type 缓存的脏数据
watch(() => options.value, (v) => {
  if (import.meta.env.DEV) console.log('[DictSelect] type=', effectiveType.value, 'count=', v.length)
})
</script>
