<!--
  内容审核
  依据: UX-M2 § 6 内容审核 + STATE-M2 内容状态机
-->
<template>
  <div class="content-review-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item>内容管理</el-breadcrumb-item>
      <el-breadcrumb-item>内容审核</el-breadcrumb-item>
    </el-breadcrumb>

    <el-tabs v-model="activeStage" class="stage-tabs">
      <el-tab-pane label="初审核" name="FIRST" />
      <el-tab-pane label="复审核" name="SECOND" />
      <el-tab-pane label="终审核" name="FINAL" />
    </el-tabs>

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
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="platformName" label="平台" width="100" align="center" />
        <el-table-column prop="accountName" label="账号" width="140" />
        <el-table-column prop="submitter" label="提交人" width="100" />
        <el-table-column prop="submittedAt" label="提交时间" width="170" align="center" />
        <el-table-column prop="stage" label="当前阶段" width="100" align="center">
          <template #default="{ row }">
            <el-tag>{{ row.stage }}</el-tag>
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
            >通过</el-button>
            <el-button
              v-if="row.status === '待审核'"
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
        <h3>{{ current.title }}</h3>
        <p style="color: #909399; font-size: 13px">
          {{ current.platformName }} / {{ current.accountName }} / 提交人:{{ current.submitter }} / {{ current.submittedAt }}
        </p>
        <el-divider />
        <div class="body">{{ current.body }}</div>
        <el-divider />
        <el-form label-width="80px" v-if="current.status === '待审核'">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'

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
  pageNo: 1,
  pageSize: 20,
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const getStatusType = (s: string) => {
  const map: Record<string, string> = { 待审核: 'warning', 已通过: 'success', 已驳回: 'danger' }
  return map[s] || ''
}

const loadData = async () => {
  loading.value = true
  // TODO: /admin-api/oa/content/review-page?stage=FIRST
  await new Promise((r) => setTimeout(r, 300))
  tableData.value = [
    { id: 1, title: '5 月运营技巧分享', platformName: '公众号', accountName: '知识变现研究院', submitter: '张三', submittedAt: '2026-06-08 10:30:00', stage: '初审核', status: '待审核', body: '内容正文...', platformType: 'WECHAT_MP' },
    { id: 2, title: 'AI 工具大盘点', platformName: '视频号', accountName: 'AI技术前沿', submitter: '李四', submittedAt: '2026-06-08 09:00:00', stage: '初审核', status: '待审核', body: '内容正文...', platformType: 'VIDEO_ACCOUNT' },
  ]
  total.value = tableData.value.length
  pagination.total = total.value
  loading.value = false
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, { title: undefined, platformType: undefined, submitter: undefined, dateRange: [] })
  pagination.pageNo = 1
  loadData()
}

const handleView = (row: any) => {
  current.value = row
  comment.value = ''
  detailVisible.value = true
}

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认通过【${row.title}】？`, '提示', { type: 'success' })
    row.status = '已通过'
    ElMessage.success('已通过')
    loadData()
  } catch {}
}
const handleReject = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认驳回【${row.title}】？`, '提示', { type: 'warning' })
    row.status = '已驳回'
    ElMessage.success('已驳回')
    loadData()
  } catch {}
}
const submitReview = (action: 'APPROVED' | 'REJECTED') => {
  if (!comment.value.trim()) {
    ElMessage.warning('请填写审核意见')
    return
  }
  if (current.value) {
    current.value.status = action === 'APPROVED' ? '已通过' : '已驳回'
  }
  ElMessage.success(action === 'APPROVED' ? '审核通过' : '审核驳回')
  detailVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.content-review-page { padding: 20px; }
.body { white-space: pre-wrap; line-height: 1.8; }
</style>
