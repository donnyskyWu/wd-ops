<template>
  <span>{{ displayLabel }}</span>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { fetchDictData } from '@/api/dict'

const props = defineProps<{
  dictType: string
  value?: string | number | null
  fallback?: string
}>()

const labelMap = ref<Record<string, string>>({})

const load = async () => {
  if (!props.dictType) return
  try {
    const res = await fetchDictData(props.dictType)
    const list = res?.list || []
    labelMap.value = Object.fromEntries(list.map((item) => [item.value, item.label]))
  } catch {
    labelMap.value = {}
  }
}

const displayLabel = computed(() => {
  const v = props.value
  if (v == null || v === '') return props.fallback ?? '--'
  const key = String(v)
  return labelMap.value[key] ?? key
})

onMounted(load)
watch(() => props.dictType, load)
</script>
