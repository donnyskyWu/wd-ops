<template>
  <div class="content-page">
    <!-- 筛选区 -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="标题">
        <el-input v-model="searchForm.title" placeholder="请输入标题关键字" clearable maxlength="50" />
      </el-form-item>
      <el-form-item label="内容类型">
        <DictSelect v-model="searchForm.contentType" dict-type="dict_content_type" placeholder="请选择" clearable />
      </el-form-item>
      <el-form-item label="平台类型">
        <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="请选择" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_content_status" placeholder="请选择" clearable />
      </el-form-item>
    </TableSearch>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增内容
      </el-button>
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
    </div>

    <!-- 内容区 -->
    <ContentWrap>
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        empty-text="暂无内容数据"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_content_type" :value="row.contentType" />
          </template>
        </el-table-column>
        <el-table-column prop="platformType" label="平台" width="100" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_platform_type" :value="row.platformType" />
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="发布账号" width="140" show-overflow-tooltip />
        <el-table-column prop="creatorUserName" label="创作者" width="100" />
        <el-table-column prop="aiGenerated" label="AI生成" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.aiGenerated === 1" type="success" size="small">AI</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_content_status" :value="row.status" />
            <el-tag v-if="row.transferredToKnowledge === 1" type="info" size="small" style="margin-left: 4px;">
              已转知识库
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="340" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              link
              type="primary"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="success"
              @click="handleSubmitReview(row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_PUBLISH'"
              link
              type="success"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 'PUBLISHED' && row.transferredToKnowledge !== 1"
              link
              type="warning"
              @click="handleTransferKnowledge(row)"
            >
              转知识库
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => pagination.pageNo = val"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <ContentEditDialog
      v-model:visible="editDialogVisible"
      :content-id="editContentId"
      :readonly="editReadonly"
      @saved="handleEditSaved"
    />

    <el-dialog v-model="reviewDialogVisible" title="提交审核" width="500px">
      <el-alert
        :title="reviewHint"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核说明">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            placeholder="可填写说明（选填）"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReviewSubmit">确认提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishDialogVisible" title="发布内容" width="520px" @open="loadPublishOptions">
      <el-form :model="publishForm" label-width="90px">
        <el-form-item label="平台" required>
          <DictSelect
            v-model="publishForm.platformType"
            dict-type="dict_platform_type"
            placeholder="请选择平台"
            :include-values="publishablePlatformTypes"
            @change="onPublishPlatformChange"
          />
        </el-form-item>
        <el-form-item label="发布账号" required>
          <el-select
            v-model="publishForm.accountIds"
            multiple
            filterable
            :disabled="!publishForm.platformType"
            placeholder="请选择发布账号（可多选）"
            style="width: 100%"
          >
            <el-option
              v-for="acc in filteredPublishAccounts"
              :key="acc.id"
              :label="acc.accountName"
              :value="acc.id"
            />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="publishForm.platformType && filteredPublishAccounts.length === 0"
          title="当前平台暂无已配置发布权限的账号"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="publishSubmitting"
          :disabled="!publishForm.platformType || !publishForm.accountIds.length"
          @click="handlePublishSubmit"
        >
          确认发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { exportToExcel, formatDateTime } from '@/utils'
import {
  getContentList,
  submitContentReview,
  deleteContent,
  getContentReviewConfig,
  getContentPublishOptions,
  publishContent,
  transferContentToKnowledge,
  type ContentPublishPlatformOption,
} from '@/api/content'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import ContentEditDialog from './ContentEditDialog.vue'

const searchForm = reactive({
  title: '',
  contentType: undefined as string | undefined,
  platformType: undefined as string | undefined,
  status: undefined as string | undefined,
})

const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 20,
  total: 0,
})

const reviewDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editContentId = ref<number | undefined>()
const editReadonly = ref(false)
const reviewConfig = ref({ level1Enabled: true, level2Enabled: true })

const reviewHint = computed(() => {
  const { level1Enabled, level2Enabled } = reviewConfig.value
  if (level1Enabled && level2Enabled) return '内容将进入一级审核 → 二级审核 → 待发布'
  if (level1Enabled) return '内容将进入一级审核（二级关闭，通过后进入待发布）'
  if (level2Enabled) return '内容将进入二级审核（一级关闭，通过后进入待发布）'
  return '审核已关闭，提交后将进入待发布'
})

const reviewForm = reactive({
  contentId: 0,
  comment: '',
})

const publishDialogVisible = ref(false)
const publishSubmitting = ref(false)
const publishOptions = ref<ContentPublishPlatformOption[]>([])
const publishForm = reactive({
  contentId: 0,
  platformType: undefined as string | undefined,
  accountIds: [] as number[],
})

const publishablePlatformTypes = computed(() =>
  publishOptions.value.map((p) => p.platformType),
)

const filteredPublishAccounts = computed(() => {
  if (!publishForm.platformType) return []
  const platform = publishOptions.value.find((p) => p.platformType === publishForm.platformType)
  return platform?.accounts ?? []
})

const loadReviewConfig = async () => {
  try {
    reviewConfig.value = await getContentReviewConfig()
  } catch {
    // 使用默认配置
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const result = await getContentList({
      title: searchForm.title || undefined,
      contentType: searchForm.contentType as any,
      platformType: searchForm.platformType,
      status: searchForm.status as any,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    })
    tableData.value = result.list
    pagination.total = result.total
  } catch (error) {
    console.error('加载内容数据失败:', error)
    ElMessage.error('数据加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

const handleReset = () => {
  searchForm.title = ''
  searchForm.contentType = undefined
  searchForm.platformType = undefined
  searchForm.status = undefined
  pagination.pageNo = 1
  loadData()
}

const handleCreate = () => {
  editContentId.value = undefined
  editReadonly.value = false
  editDialogVisible.value = true
}

const handleView = (row: { id: number }) => {
  editContentId.value = row.id
  editReadonly.value = true
  editDialogVisible.value = true
}

const handleEdit = (row: { id: number }) => {
  editContentId.value = row.id
  editReadonly.value = false
  editDialogVisible.value = true
}

const handleEditSaved = () => {
  loadData()
}

const handleSubmitReview = (row: { id: number }) => {
  reviewForm.contentId = row.id
  reviewForm.comment = ''
  reviewDialogVisible.value = true
}

const handleReviewSubmit = async () => {
  try {
    await submitContentReview(reviewForm.contentId)
    ElMessage.success('已提交审核')
    reviewDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('提交失败，请重试')
  }
}

const handlePublish = (row: { id: number }) => {
  publishForm.contentId = row.id
  publishForm.platformType = undefined
  publishForm.accountIds = []
  publishOptions.value = []
  publishDialogVisible.value = true
}

const loadPublishOptions = async () => {
  try {
    const res = await getContentPublishOptions(publishForm.contentId)
    publishOptions.value = res.platforms ?? []
  } catch {
    publishOptions.value = []
    ElMessage.error('加载发布选项失败')
  }
}

const onPublishPlatformChange = () => {
  publishForm.accountIds = []
}

const handlePublishSubmit = async () => {
  if (!publishForm.platformType || !publishForm.accountIds.length) return
  publishSubmitting.value = true
  try {
    const result = await publishContent(publishForm.contentId, {
      platformType: publishForm.platformType,
      accountIds: publishForm.accountIds,
    })
    ElMessage.success(result.mock ? '发布成功（dev mock）' : '发布成功')
    publishDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('发布失败，请重试')
  } finally {
    publishSubmitting.value = false
  }
}

const handleTransferKnowledge = async (row: { id: number; title: string }) => {
  try {
    await ElMessageBox.confirm(
      `确定将内容「${row.title}」转入知识库（案例库）吗？转入后不可重复操作。`,
      '转知识库',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const result = await transferContentToKnowledge(row.id)
    ElMessage.success(`已转入知识库（ID: ${result.knowledgeId}）`)
    loadData()
  } catch {
    // 用户取消或请求失败（全局拦截器已提示）
  }
}

const handleExport = () => {
  const rows = tableData.value.map((row) => ({
    title: row.title,
    contentType: row.contentType,
    platformType: row.platformType,
    accountName: row.accountName,
    creatorUserName: row.creatorUserName,
    aiGenerated: row.aiGenerated === 1 ? '是' : '否',
    status: row.status,
    createTime: row.createTime,
  }))
  const columns = [
    { key: 'title', label: '标题' },
    { key: 'contentType', label: '类型' },
    { key: 'platformType', label: '平台' },
    { key: 'accountName', label: '发布账号' },
    { key: 'creatorUserName', label: '创作者' },
    { key: 'aiGenerated', label: 'AI生成' },
    { key: 'status', label: '状态' },
    { key: 'createTime', label: '创建时间' },
  ]
  exportToExcel(rows, columns, '内容列表')
}

const handleDelete = async (row: { id: number; title: string }) => {
  try {
    await ElMessageBox.confirm(`确定要删除内容"${row.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteContent(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 用户取消
  }
}

onMounted(async () => {
  await loadReviewConfig()
  await loadData()
})
</script>

<style scoped lang="scss">
.content-page {
  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
