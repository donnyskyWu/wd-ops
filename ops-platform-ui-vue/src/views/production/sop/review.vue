<!--
  SOP 审核独立页
  依据: UX-M2 § 3 SOP 模板列表中的"待审核任务"Tab 抽离成独立页
  来源规范: docs/product/UX-M2-内容生产.md § 2 + PRD-M2 § FR-M2-001
-->
<template>
  <div class="sop-review-page">

    <!-- 顶部汇总卡 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value warning">{{ stats.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value success">{{ stats.approvedToday }}</div>
            <div class="stat-label">今日通过</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value danger">{{ stats.rejectedToday }}</div>
            <div class="stat-label">今日驳回</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.avgReviewHours }}h</div>
            <div class="stat-label">平均审核时长</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="模板名称">
        <el-input v-model="searchForm.templateName" placeholder="请输入模板名称" clearable maxlength="50" />
      </el-form-item>
      <el-form-item label="提交人">
        <el-input v-model="searchForm.submitter" placeholder="请输入提交人" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_review_status" placeholder="全部" clearable />
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

    <ContentWrap title="SOP 审核队列">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="templateName" label="模板名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="适用类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag>{{ row.contentType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitter" label="提交人" width="100" />
        <el-table-column prop="submittedAt" label="提交时间" width="170" align="center" />
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === '待审核'"
              link
              type="success"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === '待审核'"
              link
              type="danger"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
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

    <!-- 审核详情抽屉 -->
    <el-drawer v-model="reviewDrawerVisible" title="SOP 模板审核" size="60%">
      <div v-if="currentTemplate" class="review-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模板名称">{{ currentTemplate.templateName }}</el-descriptions-item>
          <el-descriptions-item label="适用类型">{{ currentTemplate.contentType }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ currentTemplate.submitter }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentTemplate.submittedAt }}</el-descriptions-item>
          <el-descriptions-item label="说明" :span="2">{{ currentTemplate.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 16px">节点列表</h4>
        <el-table :data="currentTemplate.nodes" border size="small">
          <el-table-column prop="name" label="节点" />
          <el-table-column prop="executorRole" label="执行岗位" />
          <el-table-column prop="slaHours" label="SLA(h)" width="80" align="right" />
          <el-table-column prop="needReview" label="需要审核" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.needReview ? 'success' : 'info'" size="small">
                {{ row.needReview ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-form
          v-if="currentTemplate.status === '待审核'"
          :model="reviewForm"
          label-width="80px"
          style="margin-top: 16px"
        >
          <el-form-item label="审核意见">
            <el-input v-model="reviewForm.comment" type="textarea" :rows="3" placeholder="请输入审核意见" />
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="submitReview('APPROVED')">通过</el-button>
            <el-button type="danger" @click="submitReview('REJECTED')">驳回</el-button>
            <el-button @click="reviewDrawerVisible = false">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const stats = reactive({
  pending: 5,
  approvedToday: 12,
  rejectedToday: 2,
  avgReviewHours: 4.5,
})

const searchForm = reactive({
  templateName: undefined as string | undefined,
  submitter: undefined as string | undefined,
  status: undefined as string | undefined,
  dateRange: [] as string[],
  pageNo: 1,
  pageSize: 20,
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const reviewDrawerVisible = ref(false)
const currentTemplate = ref<any>(null)
const reviewForm = reactive({ comment: '' })

const getPriorityType = (p: string) => {
  const map: Record<string, string> = { 高: 'danger', 中: 'warning', 低: 'info' }
  return map[p] || ''
}
const getStatusType = (s: string) => {
  const map: Record<string, string> = { 待审核: 'warning', 已通过: 'success', 已驳回: 'danger' }
  return map[s] || ''
}

const loadData = async () => {
  loading.value = true
  // TODO: 接通真实 API /admin-api/oa/sop/review-page
  await new Promise((r) => setTimeout(r, 300))
  tableData.value = [
    { id: 1, templateName: '标准内容生产运营流程', contentType: '文章', submitter: '张三', submittedAt: '2026-06-08 10:00:00', priority: '高', status: '待审核', remark: '更新了 3 个节点', nodes: [{ name: '选题', executorRole: '内容编辑', slaHours: 4, needReview: false }, { name: '撰写', executorRole: '内容编辑', slaHours: 8, needReview: true }] },
    { id: 2, templateName: '短视频内容流程', contentType: '视频', submitter: '李四', submittedAt: '2026-06-08 09:30:00', priority: '中', status: '待审核', remark: '', nodes: [] },
    { id: 3, templateName: '直播前中后流程', contentType: '直播', submitter: '王五', submittedAt: '2026-06-07 16:00:00', priority: '低', status: '已通过', remark: '', nodes: [] },
  ]
  total.value = tableData.value.length
  pagination.total = total.value
  loading.value = false
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, { templateName: undefined, submitter: undefined, status: undefined, dateRange: [] })
  pagination.pageNo = 1
  loadData()
}

const handleView = (row: any) => {
  currentTemplate.value = row
  reviewForm.comment = ''
  reviewDrawerVisible.value = true
}
const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认通过【${row.templateName}】的审核？`, '审核确认', { type: 'success' })
    row.status = '已通过'
    ElMessage.success('已通过')
    loadData()
  } catch {}
}
const handleReject = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认驳回【${row.templateName}】的审核？`, '审核确认', { type: 'warning' })
    row.status = '已驳回'
    ElMessage.success('已驳回')
    loadData()
  } catch {}
}
const submitReview = (action: 'APPROVED' | 'REJECTED') => {
  if (!reviewForm.comment.trim()) {
    ElMessage.warning('请填写审核意见')
    return
  }
  if (currentTemplate.value) {
    currentTemplate.value.status = action === 'APPROVED' ? '已通过' : '已驳回'
  }
  ElMessage.success(action === 'APPROVED' ? '审核已通过' : '审核已驳回')
  reviewDrawerVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.sop-review-page { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.stat-item { text-align: center; padding: 8px 0; }
.stat-value { font-size: 28px; font-weight: 600; line-height: 1; }
.stat-label { color: #909399; font-size: 13px; margin-top: 8px; }
.stat-value.warning { color: #e6a23c; }
.stat-value.success { color: #67c23a; }
.stat-value.danger { color: #f56c6c; }
.review-detail h4 { font-weight: 600; color: #303133; }
</style>
