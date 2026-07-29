<!--
  Phase A / D-DEDUP-01：OPS 平行用户/角色/租户管理页已废弃。
  历史书签进入后跳转 Football Admin SSOT（非 OPS CRUD）。
-->
<template>
  <div v-loading="true" class="football-admin-redirect" />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  FOOTBALL_ADMIN_ROLE,
  FOOTBALL_ADMIN_TENANT,
  FOOTBALL_ADMIN_USER,
  navigateToFootballAdmin,
} from '@/utils/ops-route'

const route = useRoute()

function resolveFootballAdminPath(): string {
  const fromMeta = route.meta.footballAdminPath
  if (typeof fromMeta === 'string' && fromMeta) {
    return fromMeta
  }
  const path = route.path || ''
  const name = String(route.name || '')
  if (path.includes('system-role') || name === 'SystemRole') {
    return FOOTBALL_ADMIN_ROLE
  }
  if (path.includes('system-tenant') || name === 'SystemTenant') {
    return FOOTBALL_ADMIN_TENANT
  }
  return FOOTBALL_ADMIN_USER
}

onMounted(() => {
  navigateToFootballAdmin(resolveFootballAdminPath())
})
</script>

<style scoped>
.football-admin-redirect {
  min-height: 200px;
}
</style>
