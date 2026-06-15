<template>
  <div class="layout-template-preview" v-loading="loading">
    <el-page-header :content="detail?.templateName || '模板预览'" @back="router.back()" />
    <el-descriptions v-if="detail" :column="2" border class="meta">
      <el-descriptions-item label="状态">
        <DictLabel dict-type="dict_layout_template_status" :value="detail.status" />
      </el-descriptions-item>
      <el-descriptions-item label="来源">
        <DictLabel dict-type="dict_layout_template_source" :value="detail.sourceType" />
      </el-descriptions-item>
      <el-descriptions-item label="文档类型">
        <DictLabel
          v-if="detail.documentType"
          dict-type="dict_document_type"
          :value="detail.documentType"
        />
        <span v-else>通用</span>
      </el-descriptions-item>
      <el-descriptions-item label="创建人">{{ detail.creatorName }}</el-descriptions-item>
    </el-descriptions>
    <el-card shadow="never" class="preview-card">
      <LayoutViewer :html="detail?.previewHtml || detail?.layoutHtml" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LayoutViewer from '@/components/layout/LayoutViewer.vue'
import DictLabel from '@/components/DictLabel.vue'
import { getLayoutTemplate } from '@/api/layoutTemplate'
import type { LayoutTemplateDetailVO } from '@/types/layoutTemplate'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<LayoutTemplateDetailVO | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getLayoutTemplate(Number(route.params.id))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.layout-template-preview {
  padding: 16px;
}

.meta {
  margin: 16px 0;
}

.preview-card {
  margin-top: 16px;
}
</style>
