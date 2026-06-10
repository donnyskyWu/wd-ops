<template>
  <div class="login-log-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>登录日志</el-breadcrumb-item>
    </el-breadcrumb>

    <ContentWrap title="登录日志">
      <el-empty
        description="登录日志后端未提供（Phase 2 规划中）"
        :image-size="120"
      >
        <template #image>
          <el-icon :size="80" color="#909399"><Lock /></el-icon>
        </template>
        <el-text type="info">
          当前阶段后端尚未提供 sys_login_log Controller。S-R17 走查已确认属 Phase 2 spec gap。
        </el-text>
        <div style="margin-top: 16px">
          <el-tag type="warning" effect="plain">P0 spec gap</el-tag>
          <el-tag type="info" effect="plain" style="margin-left: 8px">待 P-GATE 后端增量</el-tag>
        </div>
      </el-empty>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { Lock } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
</script>
