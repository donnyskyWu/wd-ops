<template>
  <div class="param-manage">
    <ContentWrap title="系统参数" subtitle="系统运行参数配置">
      <el-empty
        description="系统参数模块后端未提供 API（Phase 2 规划中）"
        :image-size="120"
      >
        <template #image>
          <el-icon :size="80" color="#909399"><Setting /></el-icon>
        </template>
        <el-text type="info">
          当前阶段（M1–M7）后端尚未提供系统参数（sys_param）Controller。S-R17 走查已确认 L-γ 的 5 个 M8/M9 页属 Phase 2 spec gap。
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
import { Setting } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
</script>
