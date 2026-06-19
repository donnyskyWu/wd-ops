<template>
  <div class="resource-preview-pane">
    <div v-if="title" class="preview-title">{{ title }}</div>
    <div v-if="loading" class="preview-loading">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="html" class="preview-body" v-html="html" />
    <el-empty v-else description="选择样式或模板预览" :image-size="48" />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  html?: string
  title?: string
  loading?: boolean
}>()
</script>

<style scoped>
.resource-preview-pane {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fff;
  min-height: 120px;
  overflow: hidden;
}

.preview-title {
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.preview-loading {
  padding: 12px;
}

.preview-body {
  padding: 12px;
  max-height: 480px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.6;
}

.preview-body :deep(#js_content),
.preview-body :deep(.rich_media_content) {
  visibility: visible !important;
  opacity: 1 !important;
}

.preview-body :deep(img) {
  max-width: 100%;
  height: auto;
}
</style>
