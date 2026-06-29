<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string | null
    nickname?: string | null
    size?: number
  }>(),
  { size: 36 },
)

const failed = ref(false)

watch(
  () => props.src,
  () => {
    failed.value = false
  },
)

const initial = () => {
  const name = props.nickname?.trim()
  return name ? name.slice(0, 1) : '?'
}
</script>

<template>
  <el-avatar v-if="src && !failed" :size="size">
    <img :src="src" referrerpolicy="no-referrer" alt="" @error="failed = true" />
  </el-avatar>
  <el-avatar v-else :size="size">{{ initial() }}</el-avatar>
</template>
