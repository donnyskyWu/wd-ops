<!--
  内容审核
  依据: UX-M2 § 6 内容审核 + STATE-M2 内容状态机（可配置二级审核）
-->
<template>
  <div class="content-review-page">

    <el-tabs v-if="enabledStages.length" v-model="activeStage" class="stage-tabs" @tab-change="handleStageChange">
      <el-tab-pane v-for="stage in enabledStages" :key="stage.key" :label="stage.label" :name="stage.key" />
    </el-tabs>
    <el-alert v-else type="info" :closable="false" show-icon title="当前未开启任何审核级别，提交后将直接发布" />

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="标题">
        <el-input v-model="searchForm.title" placeholder="搜索标题" clearable />
      </el-form-item>
      <el-form-item label="平台">
        <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="提交人">
        <el-input v-model="searchForm.submitter" placeholder="搜索提交人" clearable />
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
    </TableSearch>

    <ContentWrap title="审核队列">
      <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无待审核内容">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentTypeLabel" label="类型" width="100" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_content_type" :value="row.contentType" />
          </template>
        </el-table-column>
        <el-table-column prop="competitionName" label="赛事" width="160" show-overflow-tooltip />
        <el-table-column prop="platformName" label="平台" width="100" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_platform_type" :value="row.platformType" />
          </template>
        </el-table-column>
        <el-table-column prop="submitter" label="提交人" width="100" />
        <el-table-column prop="submittedAt" label="提交时间" width="170" align="center" />
        <el-table-column prop="stage" label="当前阶段" width="100" align="center">
          <template #default>
            <DictLabel dict-type="dict_review_stage" :value="STAGE_API[activeStage]" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_content_status" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="isPendingReview(row.status)"
              link
              type="success"
              @click="handleApprove(row)"
            >通过</el-button>
            <el-button
              v-if="isPendingReview(row.status)"
              link
              type="danger"
              @click="handleReject(row)"
            >驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => (pagination.pageNo = val)"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <el-drawer v-model="detailVisible" :title="`内容审核 - ${current?.title || ''}`" size="60%">
      <div v-if="current">
        <el-descriptions title="基础信息" :column="2" border>
          <el-descriptions-item label="标题">{{ current.title || '—' }}</el-descriptions-item>
          <el-descriptions-item label="内容类型">
            <DictLabel dict-type="dict_content_type" :value="current.contentType" />
          </el-descriptions-item>
          <el-descriptions-item v-if="current.documentType" label="文档类型">
            <DictLabel dict-type="dict_document_type" :value="current.documentType" />
          </el-descriptions-item>
          <el-descriptions-item label="赛事">
            {{ current.competitionName || current.competitionId || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="平台">
            <DictLabel dict-type="dict_platform_type" :value="current.platformType" />
          </el-descriptions-item>
          <el-descriptions-item label="发布账号">{{ current.accountName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创作者">{{ current.creatorUserName || current.submitter || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ current.submittedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <DictLabel dict-type="dict_content_status" :value="current.status" />
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">{{ current.bodyFormat === 'LAYOUT' ? '富版式正文' : '正文' }}</el-divider>
        <LayoutViewer v-if="current.bodyFormat === 'LAYOUT'" :html="current.layoutHtml" />
        <div v-else class="body">{{ current.body || '—' }}</div>

        <el-divider />
        <el-form v-if="current && isPendingReview(current.status)" label-width="80px">
          <el-form-item label="审核意见">
            <el-input v-model="comment" type="textarea" :rows="3" placeholder="请输入审核意见" />
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="submitReview('APPROVED')">通过</el-button>
            <el-button type="danger" @click="submitReview('REJECTED')">驳回</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getContentList, reviewContent, getContent, getContentReviewConfig } from '@/api/content'
import { formatDateTime } from '@/utils'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import LayoutViewer from '@/components/layout/LayoutViewer.vue'

const STAGE_STATUS: Record<string, string> = {
  FIRST: 'PENDING_FIRST_REVIEW',
  SECOND: 'PENDING_SECOND_REVIEW',
}
const STAGE_API: Record<string, string> = {
  FIRST: 'FIRST_REVIEW',
  SECOND: 'SECOND_REVIEW',
}
const STAGE_LABEL: Record<string, string> = {
  FIRST: '一级审核',
  SECOND: '二级审核',
}

const reviewConfig = ref({
  level1Enabled: true,
  level2Enabled: true,
  level1Role: 'OPS_LEADER',
  level2Role: 'OA_ADMIN',
})

const enabledStages = computed(() => {
  const stages: Array<{ key: string; label: string }> = []
  if (reviewConfig.value.level1Enabled) {
    stages.push({ key: 'FIRST', label: STAGE_LABEL.FIRST })
  }
  if (reviewConfig.value.level2Enabled) {
    stages.push({ key: 'SECOND', label: STAGE_LABEL.SECOND })
  }
  return stages
})

const activeStage = ref('FIRST')
const loading = ref(false)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const current = ref<any>(null)
const comment = ref('')

const searchForm = reactive({
  title: undefined as string | undefined,
  platformType: undefined as string | undefined,
  submitter: undefined as string | undefined,
  dateRange: [] as string[],
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const pendingStatuses = computed(() =>
  enabledStages.value.map((s) => STAGE_STATUS[s.key]).filter(Boolean),
)

const isPendingReview = (status: string) => pendingStatuses.value.includes(status)

const loadReviewConfig = async () => {
  try {
    reviewConfig.value = await getContentReviewConfig()
    if (enabledStages.value.length && !enabledStages.value.some((s) => s.key === activeStage.value)) {
      activeStage.value = enabledStages.value[0].key
    }
  } catch {
    // 默认两级均开启
  }
}

const loadData = async () => {
  if (!enabledStages.value.length) {
    tableData.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const result = await getContentList({
      title: searchForm.title || undefined,
      platformType: searchForm.platformType,
      status: STAGE_STATUS[activeStage.value] as any,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    })
    const stageLabel = STAGE_LABEL[activeStage.value] || activeStage.value
    tableData.value = (result.list || []).map((row: any) => ({
      ...row,
      submitter: row.creatorUserName || '—',
      submittedAt: formatDateTime(row.createTime),
      stage: stageLabel,
      competitionName: row.competitionName || row.competitionId || '—',
    }))
    pagination.total = result.total ?? tableData.value.length
  } catch (error) {
    console.error('加载审核队列失败:', error)
    tableData.value = []
    pagination.total = 0
    ElMessage.error('数据加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, { title: undefined, platformType: undefined, submitter: undefined, dateRange: [] })
  pagination.pageNo = 1
  loadData()
}
const handleStageChange = () => {
  pagination.pageNo = 1
  loadData()
}

const handleView = async (row: any) => {
  current.value = { ...row }
  comment.value = ''
  detailVisible.value = true
  try {
    const detail = await getContent(row.id)
    if (detail) {
      current.value = {
        ...current.value,
        ...detail,
        body: detail.body || '',
        title: detail.title || current.value.title,
        submitter: detail.creatorUserName || current.value.submitter,
        submittedAt: formatDateTime(detail.createTime),
      }
    }
  } catch {
    // 列表数据已足够展示
  }
}

const doReview = async (row: any, action: 'APPROVE' | 'REJECT', reviewComment = '') => {
  await reviewContent(row.id, {
    action,
    stage: STAGE_API[activeStage.value],
    comment: reviewComment,
  } as any)
  ElMessage.success(action === 'APPROVE' ? '审核通过' : '审核驳回')
  detailVisible.value = false
  loadData()
}

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认通过【${row.title}】？`, '提示', { type: 'success' })
    await doReview(row, 'APPROVE')
  } catch {}
}
const handleReject = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt(`请填写驳回【${row.title}】的原因`, '驳回', {
      type: 'warning',
      inputPlaceholder: '请输入审核意见',
      inputValidator: (v) => !!v?.trim() || '请填写审核意见',
    })
    await doReview(row, 'REJECT', value.trim())
  } catch {}
}
const submitReview = async (action: 'APPROVED' | 'REJECTED') => {
  if (!comment.value.trim()) {
    ElMessage.warning('请填写审核意见')
    return
  }
  if (!current.value) return
  try {
    await doReview(
      current.value,
      action === 'APPROVED' ? 'APPROVE' : 'REJECT',
      comment.value.trim(),
    )
  } catch {
    ElMessage.error('审核操作失败，请重试')
  }
}

onMounted(async () => {
  await loadReviewConfig()
  await loadData()
})
</script>

<style scoped>
.content-review-page { padding: 20px; }
.body { white-space: pre-wrap; line-height: 1.8; }
</style>
