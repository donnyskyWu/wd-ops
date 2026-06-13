<!--
  M1 - 内部内容补录详情
  依据: UX-M1 § 8 P-M1-006 + STATE-M1 内容补录状态机
  路径: /internal-content/:id
  状态机: PENDING → APPROVED/REJECTED/CANCELLED
-->
<template>
  <div class="internal-content-detail" v-loading="loading">

    <el-card shadow="never" v-if="detail">
      <template #header>
        <div class="detail-header">
          <div>
            <span style="font-size: 16px; font-weight: 600">补录详情</span>
            <el-tag :type="getStatusType(detail.status)" style="margin-left: 12px">{{ statusText(detail.status) }}</el-tag>
            <el-tag :type="getSourceType(detail.sourceType)" style="margin-left: 6px">
              {{ sourceText(detail.sourceType) }}
            </el-tag>
          </div>
          <div>
            <el-button v-if="detail.status === 'PENDING'" type="success" @click="handleApprove">通过</el-button>
            <el-button v-if="detail.status === 'PENDING'" type="danger" @click="handleReject">驳回</el-button>
            <el-button v-if="detail.status === 'PENDING'" @click="handleCancel">撤销</el-button>
            <el-button @click="router.back()">返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="补录 ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="内容标题">
          <el-link type="primary" @click="handleViewContent(detail.contentId)">{{ detail.contentTitle }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="数据日期">{{ detail.dataDate }}</el-descriptions-item>
        <el-descriptions-item label="补录类型">
          <el-tag :type="getSourceType(detail.sourceType)" effect="dark">{{ sourceText(detail.sourceType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="补录人">{{ detail.createdByName }}</el-descriptions-item>
        <el-descriptions-item label="补录时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.reviewedByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detail.reviewedAt || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h3 style="margin: 20px 0 12px 0; font-size: 15px">数据明细</h3>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="label">阅读/播放量</div>
              <div class="value">{{ formatNumber(detail.readCount || 0) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="label">点赞数</div>
              <div class="value">{{ formatNumber(detail.likeCount || 0) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="label">评论数</div>
              <div class="value">{{ formatNumber(detail.commentCount || 0) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="label">转发/分享</div>
              <div class="value">{{ formatNumber(detail.forwardCount || 0) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric-item">
              <div class="label">粉丝变化</div>
              <div class="value" :class="{ up: (detail.followerChange || 0) > 0, down: (detail.followerChange || 0) < 0 }">
                {{ (detail.followerChange || 0) > 0 ? '+' : '' }}{{ detail.followerChange || 0 }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <h3 style="margin: 20px 0 12px 0; font-size: 15px">补录原因</h3>
      <el-input
        :model-value="detail.remark"
        type="textarea"
        :rows="3"
        readonly
        placeholder="无说明"
      />

      <h3 v-if="detail.reviewComment" style="margin: 20px 0 12px 0; font-size: 15px">审核意见</h3>
      <el-alert
        v-if="detail.reviewComment"
        :title="detail.reviewComment"
        :type="detail.status === 'APPROVED' ? 'success' : detail.status === 'REJECTED' ? 'error' : 'warning'"
        :closable="false"
        show-icon
      />
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewDialogVisible" :title="reviewAction === 'APPROVED' ? '审核通过' : '审核驳回'" width="500px">
      <el-form label-width="80px">
        <el-form-item label="审核意见" required>
          <el-input v-model="reviewForm.comment" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button
          :type="reviewAction === 'APPROVED' ? 'success' : 'danger'"
          :loading="reviewSubmitting"
          @click="submitReview"
        >
          确认{{ reviewAction === 'APPROVED' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<any>(null)
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewAction = ref<'APPROVED' | 'REJECTED'>('APPROVED')
const reviewForm = reactive({ comment: '' })

const statusText = (s: string) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已撤销' }[s] || s)
const getStatusType = (s: string) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s] || '')
const sourceText = (s: string) => ({ API: 'API', API_EXCEPTION: '接口补录', ACCOUNT_BANNED: '封号补录', OFFLINE_PRACTICE: '线下补录', OTHER: '其他补录' }[s] || s)
const getSourceType = (s: string) => ({ API: 'primary', API_EXCEPTION: 'warning', ACCOUNT_BANNED: 'danger', OFFLINE_PRACTICE: 'warning', OTHER: 'info' }[s] || '')
const formatNumber = (n: number) => n >= 10000 ? (n / 10000).toFixed(1) + '万' : n.toString()

const loadDetail = async () => {
  loading.value = true
  const id = Number(route.params.id)
  // TODO: GET /admin-api/oa/internal-content/{id}
  await new Promise((r) => setTimeout(r, 300))
  detail.value = {
    id,
    contentId: 100,
    contentTitle: '5 月内容运营复盘',
    dataDate: '2026-05-31',
    sourceType: 'API_EXCEPTION',
    status: 'PENDING',
    readCount: 12800,
    likeCount: 320,
    commentCount: 88,
    forwardCount: 45,
    followerChange: 128,
    remark: '原 API 报错,数据由运营同学手动补录',
    createdByName: '张三',
    createdAt: '2026-06-01 10:30:00',
    reviewedByName: null,
    reviewedAt: null,
    reviewComment: null,
  }
  loading.value = false
}

const handleApprove = () => {
  reviewAction.value = 'APPROVED'
  reviewForm.comment = ''
  reviewDialogVisible.value = true
}
const handleReject = () => {
  reviewAction.value = 'REJECTED'
  reviewForm.comment = ''
  reviewDialogVisible.value = true
}
const submitReview = async () => {
  if (!reviewForm.comment.trim()) {
    ElMessage.warning('请输入审核意见')
    return
  }
  reviewSubmitting.value = true
  // TODO: POST /admin-api/oa/internal-content/{id}/review
  await new Promise((r) => setTimeout(r, 400))
  detail.value.status = reviewAction.value
  detail.value.reviewComment = reviewForm.comment
  reviewSubmitting.value = false
  reviewDialogVisible.value = false
  ElMessage.success(reviewAction.value === 'APPROVED' ? '审核通过' : '已驳回')
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确认撤销此补录？撤销后不可恢复', '撤销确认', { type: 'warning' })
    detail.value.status = 'CANCELLED'
    ElMessage.success('已撤销')
  } catch {}
}

const handleViewContent = (id: number) => {
  router.push(`/content/edit/${id}`)
}

onMounted(loadDetail)
</script>

<style scoped>
.internal-content-detail { padding: 20px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; }
.metric-item { text-align: center; padding: 8px 0; }
.metric-item .label { color: #909399; font-size: 13px; }
.metric-item .value { font-size: 24px; font-weight: 600; line-height: 1.4; }
.metric-item .value.up { color: #67c23a; }
.metric-item .value.down { color: #f56c6c; }
</style>
