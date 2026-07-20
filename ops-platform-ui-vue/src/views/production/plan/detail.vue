<!-- 计划详情面板（列表页抽屉内嵌） -->
<template>
  <div class="plan-detail-panel" v-loading="loading">
    <template v-if="detail">
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 style="margin: 0">
              {{ detail.planName }}
              <el-tag :type="planStatusType" style="margin-left: 8px">
                <DictLabel dict-type="dict_plan_status" :value="detail.status" />
              </el-tag>
            </h2>
            <p class="meta">
              <span>SOP 模板：{{ detail.templateName || '—' }}</span>
              <el-divider direction="vertical" />
              <span>IP 组：{{ detail.ipGroupName || '—' }}</span>
              <el-divider direction="vertical" />
              <span>{{ detail.startDate }} ~ {{ detail.endDate }}</span>
            </p>
            <div class="progress-row">
              <span class="progress-label">进度</span>
              <el-progress :percentage="detail.progress ?? 0" :stroke-width="8" style="flex: 1; max-width: 320px" />
            </div>
          </div>
        </div>
      </el-card>

      <ContentWrap title="计划信息" style="margin-top: 16px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="计划名称" :span="2">{{ detail.planName }}</el-descriptions-item>
          <el-descriptions-item label="SOP 模板">{{ detail.templateName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="IP 组">{{ detail.ipGroupName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ detail.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ detail.endDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <DictLabel dict-type="dict_plan_status" :value="detail.status" />
          </el-descriptions-item>
          <el-descriptions-item label="进度">{{ detail.progress ?? 0 }}%</el-descriptions-item>
          <el-descriptions-item label="关联赛事" :span="2">
            {{ competitionText }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
        </el-descriptions>
      </ContentWrap>

      <ContentWrap title="生成的任务记录">
        <el-table :data="detail.tasks || []" border stripe empty-text="暂无任务（保存草稿后生成）">
          <el-table-column prop="nodeName" label="节点" min-width="120" show-overflow-tooltip />
          <el-table-column label="赛事" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.competitionName || row.competitionId || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="assigneeName" label="执行人" width="100" show-overflow-tooltip />
          <el-table-column label="执行岗位" width="110">
            <template #default="{ row }">
              <DictLabel
                dict-type="dict_position"
                :value="row.executorRole"
                :fallback="row.executorRoleText || row.executorRole || '—'"
              />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="{ row }">
              <DictLabel dict-type="dict_sop_node_status" :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="开始时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.scheduledStart) }}</template>
          </el-table-column>
          <el-table-column label="结束时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.scheduledEnd) }}</template>
          </el-table-column>
          <el-table-column label="SLA 截止" width="170">
            <template #default="{ row }">{{ formatDateTime(row.slaDeadline) }}</template>
          </el-table-column>
        </el-table>
      </ContentWrap>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictLabel from '@/components/DictLabel.vue'
import { formatDateTime } from '@/utils/index'
import { getContentPlan, type ContentPlanVO } from '@/api/plan'

const props = defineProps<{
  planId: number
}>()

const loading = ref(false)
const detail = ref<ContentPlanVO | null>(null)

const competitionText = computed(() => {
  const names = detail.value?.competitions?.map((c) => c.competitionName).filter(Boolean)
  return names?.length ? names.join('、') : '—'
})

const planStatusType = computed(() => {
  const map: Record<string, 'info' | 'primary' | 'warning' | 'danger' | 'success'> = {
    DRAFT: 'info',
    IN_PROGRESS: 'primary',
    TERMINATE_PENDING: 'warning',
    TERMINATED: 'danger',
    COMPLETED: 'success',
  }
  return map[detail.value?.status || ''] || 'info'
})

const loadDetail = async (planId: number) => {
  loading.value = true
  detail.value = null
  try {
    detail.value = await getContentPlan(planId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.planId,
  (planId) => {
    if (planId) loadDetail(planId)
  },
  { immediate: true },
)
</script>

<style scoped lang="scss">
.plan-detail-panel {
  min-height: 120px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 8px 0 0;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.progress-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  flex-shrink: 0;
}
</style>
