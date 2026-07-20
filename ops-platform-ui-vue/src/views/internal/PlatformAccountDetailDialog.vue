<template>
  <el-drawer
    direction="rtl"
    :model-value="visible"
    :title="drawerTitle"
    size="80%"
    destroy-on-close
    append-to-body
    class="platform-account-detail-drawer"
    @update:model-value="(val: boolean) => emit('update:visible', val)"
  >
    <PlatformAccountDetail
      v-if="accountId"
      embedded
      :account-id="accountId"
      :initial-tab="initialTab"
      @cancelled="emit('update:visible', false)"
      @saved="handleSaved"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import PlatformAccountDetail from './PlatformAccountDetail.vue'

const props = defineProps<{
  visible: boolean
  accountId?: number
  initialTab?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const drawerTitle = '平台账号详情'

const handleSaved = () => {
  emit('saved')
}
</script>

<style scoped>
.platform-account-detail-drawer :deep(.el-drawer) {
  min-width: 720px;
}

.platform-account-detail-drawer,
.platform-account-detail-drawer :deep(.el-drawer__body) {
  background-color: var(--el-bg-color, #fff);
}
</style>
