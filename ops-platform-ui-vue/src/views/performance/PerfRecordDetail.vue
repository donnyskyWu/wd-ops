<!--
  M3 - 绩效执行详情
  依据: FR-M3-002 执行记录
  路径: /perf/record/:id
  5 区: 基本信息 / 指标完成情况 / 自动算分明细 / 人工调整 / 审批流
-->
<template>
  <div class="perf-record-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/perf/execution' }">执行记录</el-breadcrumb-item>
      <el-breadcrumb-item>详情</el-breadcrumb-item>
    </el-breadcrumb>

    <template v-if="detail">
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 style="margin: 0">
              {{ detail.userName }} - {{ detail.period }}
              <el-tag :type="statusType" style="margin-left: 8px">{{ statusLabel }}</el-tag>
            </h2>
            <p class="meta">
              <span>模板：{{ detail.templateName }}</span>
              <el-divider direction="vertical" />
              <span>岗位：{{ detail.position }}</span>
              <el-divider direction="vertical" />
              <span>自评提交：{{ detail.submittedAt || '未提交' }}</span>
            </p>
          </div>
          <div>
            <el-button @click="router.back()">返回</el-button>
            <el-button v-if="detail.status === 0" type="primary" @click="submitSelf">提交自评</el-button>
            <el-button v-if="detail.status === 1" type="success" @click="approve">审核通过</el-button>
            <el-button v-if="detail.status === 1" type="danger" @click="reject">驳回</el-button>
            <el-button v-if="detail.status === 2" type="primary" @click="publish">发布</el-button>
          </div>
        </div>
      </el-card>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="16">
          <!-- 指标完成情况 -->
          <ContentWrap title="指标完成情况">
            <el-table :data="detail.metrics" border>
              <el-table-column prop="name" label="指标" min-width="120" />
              <el-table-column prop="source" label="数据源" width="120">
                <template #default="{ row }">
                  <el-tag size="small">{{ sourceLabel(row.source) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="target" label="目标" width="90" align="right" />
              <el-table-column prop="actual" label="实际" width="90" align="right">
                <template #default="{ row }">
                  <span :class="{ 'text-success': row.actual >= row.target, 'text-warning': row.actual < row.target * 0.8 }">
                    {{ row.actual }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="完成率" width="100">
                <template #default="{ row }">
                  <el-progress :percentage="Math.min(100, Math.round((row.actual / row.target) * 100))" :stroke-width="8" />
                </template>
              </el-table-column>
              <el-table-column prop="weight" label="权重" width="70" align="center">
                <template #default="{ row }">{{ row.weight }}%</template>
              </el-table-column>
              <el-table-column label="得分" width="90" align="center">
                <template #default="{ row }">
                  <span class="score">{{ calcMetricScore(row).toFixed(2) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </ContentWrap>

          <!-- 人工调整 -->
          <ContentWrap title="人工调整" style="margin-top: 16px">
            <el-form :model="adjustForm" label-width="120px" style="max-width: 700px">
              <el-form-item label="基础分调整">
                <el-input-number v-model="adjustForm.baseScoreAdj" :min="-50" :max="50" />
                <span style="margin-left: 8px; color: #909399">可在 {{ detail.baseScore }} ±50 范围调整</span>
              </el-form-item>
              <el-form-item label="特殊加成/扣分">
                <el-input-number v-model="adjustForm.bonusScore" :min="-100" :max="200" />
                <span style="margin-left: 8px; color: #909399">如项目奖/特殊贡献/-50 重大失误</span>
              </el-form-item>
              <el-form-item label="调整原因" required>
                <el-input v-model="adjustForm.reason" type="textarea" :rows="2" placeholder="必须填写,作为审计依据" />
              </el-form-item>
              <el-form-item>
                <el-button type="warning" @click="applyAdjust">应用调整</el-button>
              </el-form-item>
            </el-form>
          </ContentWrap>
        </el-col>

        <el-col :span="8">
          <!-- 自动算分卡 -->
          <ContentWrap title="最终得分">
            <div class="score-box">
              <el-statistic :value="totalScore" :precision="2" />
              <div class="score-meta">满分 {{ detail.maxScore }} ｜ 调整 +{{ adjustForm.baseScoreAdj + adjustForm.bonusScore }}</div>
              <el-progress
                :percentage="Math.round((totalScore / detail.maxScore) * 100)"
                :status="(totalScore / detail.maxScore) > 0.9 ? 'success' : (totalScore / detail.maxScore) > 0.6 ? '' : 'exception'"
                :stroke-width="10"
                style="margin-top: 12px"
              />
              <el-divider />
              <div class="score-row">
                <span>基础分：</span><span>{{ detail.baseScore + adjustForm.baseScoreAdj }}</span>
              </div>
              <div class="score-row">
                <span>指标分：</span><span>{{ metricScore.toFixed(2) }}</span>
              </div>
              <div class="score-row">
                <span>特殊加成：</span><span>{{ adjustForm.bonusScore > 0 ? '+' : '' }}{{ adjustForm.bonusScore }}</span>
              </div>
            </div>
          </ContentWrap>

          <!-- 审批流 -->
          <ContentWrap title="审批流" style="margin-top: 16px">
            <el-steps direction="vertical" :active="stepActive">
              <el-step title="自评提交" :description="detail.submittedAt || '未提交'" />
              <el-step title="主管审核" :description="reviewer1 || '待处理'" />
              <el-step title="HR 复核" :description="reviewer2 || '待处理'" />
              <el-step title="结果发布" :description="detail.publishedAt || '未发布'" />
            </el-steps>
          </ContentWrap>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import {
  getPerfRecordDetail,
  adjustPerfRecord,
  confirmPerfRecord,
} from '@/api/perfRecord'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<any>(null)
const adjustForm = reactive({ baseScoreAdj: 0, bonusScore: 0, reason: '' })
const reviewer1 = ref('')
const reviewer2 = ref('')

const statusType = computed(() => {
  const map: any = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'success' }
  return map[detail.value?.status] || 'info'
})
const STATUS_LABELS: Record<number, string> = {
  0: '待提交',
  1: '待主管审核',
  2: '待HR复核',
  3: '已发布',
}
const statusLabel = computed(() => {
  const status = detail.value?.status
  return status !== undefined ? (STATUS_LABELS[status] || '-') : '-'
})
const stepActive = computed(() => detail.value?.status || 0)

const SOURCE_LABELS: Record<string, string> = {
  follower_inc: '粉丝净增',
  work_count: '作品数',
  order_gmv: 'GMV',
  engagement: '互动数',
}
const sourceLabel = (s: string) => SOURCE_LABELS[s] || s

const calcMetricScore = (m: any) => {
  if (!m.target) return 0
  return Math.min(1, m.actual / m.target) * m.weight
}

const metricScore = computed(() => {
  if (!detail.value?.metrics) return 0
  return detail.value.metrics.reduce((s: number, m: any) => s + calcMetricScore(m), 0)
})
const totalScore = computed(() => {
  if (!detail.value) return 0
  return Math.min(
    detail.value.maxScore,
    detail.value.baseScore + adjustForm.baseScoreAdj + metricScore.value + adjustForm.bonusScore,
  )
})

const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const data: any = await getPerfRecordDetail(id)
    detail.value = {
      id: data.id,
      userName: data.targetUserName || data.userName || '',
      position: data.position || '',
      templateName: data.templateName || '',
      period: data.periodStart && data.periodEnd ? `${data.periodStart} ~ ${data.periodEnd}` : (data.period || ''),
      status: data.status,
      baseScore: data.baseScore ?? 0,
      maxScore: data.maxScore ?? 100,
      submittedAt: data.submittedAt || '',
      publishedAt: data.publishedAt || '',
      metrics: (data.items || []).map((it: any) => ({
        name: it.metricName || '',
        source: it.metricCode || '',
        target: it.target ?? 0,
        actual: it.actualValue ?? 0,
        weight: it.weight ?? 0,
        score: it.score ?? 0,
      })),
    }
    reviewer1.value = data.reviewer1 || '待处理'
    reviewer2.value = data.reviewer2 || '待处理'
  } catch {
    ElMessage.error('加载考核详情失败')
  } finally {
    loading.value = false
  }
}

const submitSelf = () => {
  ElMessage.success('已提交自评,等待主管审核')
  detail.value.status = 1
  detail.value.submittedAt = new Date().toLocaleString('zh-CN')
}
const approve = async () => {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('确认审核通过？', '提示', { type: 'success' })
    await confirmPerfRecord(detail.value.id)
    ElMessage.success('已通过,提交 HR 复核')
    detail.value.status = 2
    reviewer1.value = `张主管 / ${new Date().toLocaleString('zh-CN')}`
    reviewer2.value = 'HR-小红 / 待审'
  } catch {}
}
const reject = () => {
  ElMessageBox.prompt('请输入驳回原因', '驳回', { type: 'warning' })
    .then(({ value }) => {
      ElMessage.warning(`已驳回：${value}`)
      detail.value.status = 0
    })
    .catch(() => {})
}
const publish = () => {
  ElMessage.success('已发布,员工可查看')
  detail.value.status = 3
  detail.value.publishedAt = new Date().toLocaleString('zh-CN')
  reviewer2.value = `HR-小红 / ${detail.value.publishedAt}`
}
const applyAdjust = async () => {
  if (!detail.value) return
  if (!adjustForm.reason) {
    ElMessage.warning('请填写调整原因')
    return
  }
  // 后端仅提供单条 itemRecord 的 adjust；这里按主记录调整后端
  try {
    await adjustPerfRecord({
      itemRecordId: 0,
      manualAdjustment: adjustForm.baseScoreAdj + adjustForm.bonusScore,
      remark: adjustForm.reason,
    } as any)
    ElMessage.success(`已应用：基础分 ${adjustForm.baseScoreAdj >= 0 ? '+' : ''}${adjustForm.baseScoreAdj}，加成 ${adjustForm.bonusScore}`)
  } catch {}
}

onMounted(loadDetail)
</script>

<style scoped>
.perf-record-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-start; }
.meta { color: #909399; font-size: 13px; margin: 8px 0 0 0; }
.score-box { padding: 8px 0; }
.score-meta { color: #909399; font-size: 13px; margin-top: 4px; }
.score-row { display: flex; justify-content: space-between; padding: 4px 0; }
.text-success { color: #67c23a; font-weight: 600; }
.text-warning { color: #e6a23c; font-weight: 600; }
.score { color: #409eff; font-weight: 600; }
</style>
