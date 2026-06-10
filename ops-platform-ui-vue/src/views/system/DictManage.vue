<template>
  <div class="dict-manage">
    <ContentWrap title="字典配置" subtitle="数据字典管理">
      <el-empty
        description="字典管理（CRUD）后端未提供（Phase 2 规划中）"
        :image-size="120"
      >
        <template #image>
          <el-icon :size="80" color="#909399"><Notebook /></el-icon>
        </template>
        <el-text type="info">
          当前阶段后端 DictController 仅提供查询端点（GET /oa/dict/data?type=xxx 与 /oa/dict/types），
          供 DictSelect / 前端字典下拉使用。字典类型与字典项的完整 CRUD 属 Phase 2。
        </el-text>
        <div style="margin-top: 16px">
          <el-tag type="warning" effect="plain">P0 spec gap（仅查询可用）</el-tag>
          <el-tag type="info" effect="plain" style="margin-left: 8px">CRUD 待 P-GATE 增量</el-tag>
        </div>
      </el-empty>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { Notebook } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
</script>
