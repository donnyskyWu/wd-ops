<!--
  OPS 作者列表已迁移至 Football「作者信息」(author/info)。
  保留此路由供 /ops/author 书签兼容，自动跳转 Football SSOT。
-->
<template>
  <div v-loading="true" class="author-redirect" />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { navigateToFootballAuthorInfo } from '@/utils/ops-route'

onMounted(() => {
  navigateToFootballAuthorInfo()
})
</script>

<style scoped>
.author-redirect {
  min-height: 200px;
}
</style>
