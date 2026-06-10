<template>
  <div class="message-manage">
    <ContentWrap title="消息管理" subtitle="系统消息通知管理">
      <el-empty
        description="消息通知后端未提供（Phase 2 规划中）"
        :image-size="120"
      >
        <template #image>
          <el-icon :size="80" color="#909399"><Bell /></el-icon>
        </template>
        <el-text type="info">
          当前阶段后端尚未提供消息通知 sys_message Controller。S-R17 走查已确认属 Phase 2 spec gap。
          渠道（邮件/短信/企业微信）属 M10 范畴。
        </el-text>
        <div style="margin-top: 16px">
          <el-tag type="warning" effect="plain">P0 spec gap</el-tag>
          <el-tag type="info" effect="plain" style="margin-left: 8px">M10 采集/SSO 规划中</el-tag>
        </div>
      </el-empty>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
</script>
