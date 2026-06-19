<template>
  <div class="layout-param-sliders">
    <div v-for="param in params" :key="param.key" class="param-row">
      <span class="param-label">{{ param.label }}</span>
      <el-slider
        v-model="localValues[param.key]"
        :min="param.min"
        :max="param.max"
        :step="param.step"
        show-input
        size="small"
        @change="emitChange"
      />
    </div>
    <el-empty v-if="!params.length" description="该模板无可调参数" :image-size="40" />
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

export interface LayoutParamDef {
  key: string
  label: string
  min: number
  max: number
  step: number
  unit: string
  styleRef: string
  cssProp: string
}

const props = defineProps<{
  params: LayoutParamDef[]
  modelValue?: Record<string, Record<string, string>>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, Record<string, string>>]
}>()

const localValues = reactive<Record<string, number>>({})

watch(
  () => props.params,
  (list) => {
    for (const p of list) {
      if (localValues[p.key] == null) {
        localValues[p.key] = Math.round((p.min + p.max) / 2)
      }
    }
  },
  { immediate: true },
)

function emitChange() {
  const overrides: Record<string, Record<string, string>> = {}
  for (const p of props.params) {
    const val = localValues[p.key]
    if (val == null) continue
    if (!overrides[p.styleRef]) overrides[p.styleRef] = {}
    overrides[p.styleRef][p.cssProp] = `${val}${p.unit}`
  }
  emit('update:modelValue', overrides)
}
</script>

<style scoped>
.layout-param-sliders {
  padding: 4px 0;
}

.param-row {
  margin-bottom: 12px;
}

.param-label {
  display: block;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
</style>
