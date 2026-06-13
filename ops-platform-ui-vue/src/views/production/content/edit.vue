<!--
  内容创作/编辑（页面模式，路由保留供深链；菜单已隐藏）
-->
<template>
  <div class="content-edit-page">
    <el-card shadow="never">
      <ContentEditPanel
        :content-id="contentId"
        :task-id="taskId"
        :competition-id="competitionId"
        @saved="handleSaved"
        @cancelled="handleCancel"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ContentEditPanel from './ContentEditPanel.vue'

const route = useRoute()
const router = useRouter()

const taskId = computed(() => {
  const raw = route.query.taskId
  return raw ? Number(raw) : undefined
})
const contentId = computed(() => (route.params.id ? Number(route.params.id) : undefined))
const competitionId = computed(() => String(route.query.competitionId || '') || undefined)
const isTaskMode = computed(() => !!taskId.value)
const pageTitle = computed(() =>
  isTaskMode.value ? '任务内容创作' : contentId.value ? '编辑内容' : '创作内容',
)

const handleSaved = () => {
  if (isTaskMode.value && taskId.value) {
    router.push(`/task/${taskId.value}/execute`)
  } else {
    router.push('/content')
  }
}

const handleCancel = () => {
  if (isTaskMode.value && taskId.value) {
    router.push(`/task/${taskId.value}/execute`)
  } else {
    router.push('/content')
  }
}
</script>

<style scoped>
.content-edit-page {
  padding: 20px;
}
</style>
