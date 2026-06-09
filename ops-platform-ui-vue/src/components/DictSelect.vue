<template>
  <el-select
    v-model="model"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="filterable"
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
import { ref, watch, onMounted } from 'vue'
import { fetchDictData, type DictItemVO } from '@/api/dict'

const props = defineProps<{
  type: string
  modelValue?: string | number | null
  placeholder?: string
  clearable?: boolean
  filterable?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: string | number | null): void
}>()

const model = ref<string | number | null>(props.modelValue ?? null)
const options = ref<DictItemVO[]>([])
const loading = ref(false)

watch(() => props.modelValue, (v) => {
  model.value = v ?? null
})
watch(model, (v) => {
  emit('update:modelValue', v)
})

const load = async () => {
  if (!props.type) return
  loading.value = true
  try {
    const res: any = await fetchDictData(props.type)
    options.value = (res?.list || []) as DictItemVO[]
  } catch (e) {
    options.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.type, load)
</script>
