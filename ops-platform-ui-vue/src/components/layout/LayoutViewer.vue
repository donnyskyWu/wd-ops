<template>
  <div class="layout-viewer" v-html="safeHtml" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { appendFileAuthToHtml } from '@/utils/fileUrl'
import { ensureImageWidthStyles } from '@/utils/layoutSync'

const props = defineProps<{
  html?: string | null
}>()

const safeHtml = computed(() => {
  const raw = props.html || '<p class="text-muted">暂无版式内容</p>'
  return appendFileAuthToHtml(ensureImageWidthStyles(raw))
})
</script>

<style scoped lang="scss">
@import '@/styles/layout-viewer-content.scss';
</style>
