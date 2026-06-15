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

<style scoped>
.layout-viewer :deep(section.layout-article) {
  max-width: 100%;
  margin: 0;
  line-height: 1.8;
}

.layout-viewer :deep(img) {
  height: auto;
  vertical-align: bottom;
}

/* Cap only when inline width is absent (ensureImageWidthStyles adds width + max-width). */
.layout-viewer :deep(img:not([style*='width'])) {
  max-width: 100%;
}

.layout-viewer :deep(blockquote) {
  border-left: 4px solid #dcdfe6;
  margin: 12px 0;
  padding: 8px 16px;
  color: #606266;
  background: #f5f7fa;
}
</style>
