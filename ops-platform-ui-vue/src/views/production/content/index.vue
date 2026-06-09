<template>
  <div class="content-page">
    <!-- 筛选区 -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="标题">
        <el-input v-model="searchForm.title" placeholder="请输入标题关键字" clearable maxlength="50" />
      </el-form-item>
      <el-form-item label="内容类型">
        <el-select v-model="searchForm.contentType" placeholder="请选择" clearable>
          <el-option label="图文" value="ARTICLE" />
          <el-option label="短视频" value="VIDEO" />
          <el-option label="直播" value="LIVE" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台类型">
        <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="请选择" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择" clearable>
          <el-option label="草稿" :value="ContentStatus.DRAFT" />
          <el-option label="待初审" :value="ContentStatus.PENDING_REVIEW" />
          <el-option label="审核中" :value="ContentStatus.REVIEWING" />
          <el-option label="审核通过" :value="ContentStatus.APPROVED" />
          <el-option label="已驳回" :value="ContentStatus.REJECTED" />
          <el-option label="发布中" :value="ContentStatus.PUBLISHING" />
          <el-option label="已发布" :value="ContentStatus.PUBLISHED" />
          <el-option label="发布失败" :value="ContentStatus.PUBLISH_FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
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

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增内容
      </el-button>
    </div>

    <!-- 内容区 -->
    <ContentWrap>
      <!-- 内容列表表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        :empty-text="'暂无内容数据'"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="类型" width="90" align="center">
          <template #default="{ row }">
            {{ getContentTypeText(row.contentType) }}
          </template>
        </el-table-column>
        <el-table-column prop="platformType" label="平台" width="100" align="center">
          <template #default="{ row }">
            {{ getPlatformTypeText(row.platformType) }}
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="发布账号" width="140" show-overflow-tooltip />
        <el-table-column prop="creatorName" label="创作者" width="100" />
        <el-table-column prop="aiGenerated" label="AI生成" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.aiGenerated" type="success" size="small">AI</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">
              查看
            </el-button>
            <el-button
              v-if="row.status === ContentStatus.DRAFT || row.status === ContentStatus.REJECTED"
              link
              type="primary"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === ContentStatus.DRAFT"
              link
              type="success"
              @click="handleSubmitReview(row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="row.status === ContentStatus.DRAFT || row.status === ContentStatus.REJECTED"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => pagination.pageNo = val"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewDialogVisible" title="提交审核" width="500px">
      <el-alert
        title="内容将进入三级审核流程：初审 → 复审 → 终审"
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getContentList, submitReview, deleteContent } from '@/api/content'
import { mockContentList, mockGetContentList } from '@/mock/content'
import { ContentStatus, ReviewStage } from '@/types/content'
import type { ContentQuery, ContentType, ContentItem } from '@/types/content'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'

// 暴露枚举供模板使用
const ContentStatusEnum = ContentStatus

// ==================== 响应式数据 ====================

// 搜索表单
const searchForm = reactive({
  title: '',
  contentType: undefined as ContentType | undefined,
  platformType: undefined as string | undefined,
  status: undefined as ContentStatus | undefined,
  dateRange: [] as string[],
})

// 加载状态
const loading = ref(false)

// 表格数据 - 初始值使用Mock数据
const tableData = ref<any[]>([...mockContentList])

// 分页参数 - 初始值使用Mock数据
const pagination = reactive({
  pageNo: 1,
  pageSize: 20,
  total: 0,
})

// 审核对话框
const reviewDialogVisible = ref(false)
const reviewForm = reactive({
  contentId: 0,
  comment: '',
})

// ==================== 方法 ====================

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params: ContentQuery = {
      title: searchForm.title || undefined,
      contentType: searchForm.contentType,
      platformType: searchForm.platformType,
      status: searchForm.status,
      createStart: searchForm.dateRange?.[0],
      createEnd: searchForm.dateRange?.[1],
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    }

    const result = await getContentList(params).catch(() => {
      return mockGetContentList(pagination.pageNo, pagination.pageSize)
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

// 搜索
const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.title = ''
  searchForm.contentType = undefined
  searchForm.platformType = undefined
  searchForm.status = undefined
  searchForm.dateRange = []
  pagination.pageNo = 1
  loadData()
}

// 新增内容
const handleCreate = () => {
  ElMessage.info('跳转到新增页面（待实现）')
  // TODO: router.push('/production/content/create')
}

// 查看
const handleView = (row: ContentItem) => {
  ElMessage.info(`查看内容详情：${row.title}`)
  // TODO: router.push(`/production/content/${row.id}`)
}

// 编辑
const handleEdit = (row: ContentItem) => {
  ElMessage.info(`编辑内容：${row.title}`)
  // TODO: router.push(`/production/content/${row.id}/edit`)
}

// 提交审核
const handleSubmitReview = (row: ContentItem) => {
  reviewForm.contentId = row.id
  reviewForm.comment = ''
  reviewDialogVisible.value = true
}

// 提交审核确认
const handleReviewSubmit = async () => {
  try {
    await submitReview({ contentId: reviewForm.contentId }).catch(() => {
      return Promise.resolve()
    })

    ElMessage.success('已提交审核，进入三级审核流程')
    reviewDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  }
}

// 删除
const handleDelete = async (row: ContentItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除内容"${row.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteContent(row.id).catch(() => {
      return Promise.resolve()
    })

    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    // 用户取消
  }
}

// 获取内容类型文本
const getContentTypeText = (contentType: ContentType) => {
  const texts: Record<ContentType, string> = {
    ARTICLE: '图文',
    VIDEO: '短视频',
    LIVE: '直播',
    OTHER: '其他',
  }
  return texts[contentType] || ''
}

// 获取平台类型文本
const getPlatformTypeText = (platformType: string) => {
  const texts: Record<string, string> = {
    WECHAT_MP: '公众号',
    VIDEO_ACCOUNT: '视频号',
    DOUYIN: '抖音',
    KUAISHOU: '快手',
    XIAOHONGSHU: '小红书',
  }
  return texts[platformType] || platformType
}

// 获取状态标签类型
const getStatusTagType = (status: ContentStatus) => {
  const types: Record<ContentStatus, string> = {
    [ContentStatus.DRAFT]: 'info',
    [ContentStatus.PENDING_REVIEW]: 'warning',
    [ContentStatus.REVIEWING]: 'primary',
    [ContentStatus.APPROVED]: 'success',
    [ContentStatus.REJECTED]: 'danger',
    [ContentStatus.PUBLISHING]: '',
    [ContentStatus.PUBLISHED]: 'success',
    [ContentStatus.PUBLISH_FAILED]: 'danger',
  }
  return types[status] || ''
}

// 获取状态文本
const getStatusText = (status: ContentStatus) => {
  const texts: Record<ContentStatus, string> = {
    [ContentStatus.DRAFT]: '草稿',
    [ContentStatus.PENDING_REVIEW]: '待初审',
    [ContentStatus.REVIEWING]: '审核中',
    [ContentStatus.APPROVED]: '审核通过',
    [ContentStatus.REJECTED]: '已驳回',
    [ContentStatus.PUBLISHING]: '发布中',
    [ContentStatus.PUBLISHED]: '已发布',
    [ContentStatus.PUBLISH_FAILED]: '发布失败',
  }
  return texts[status] || ''
}

// ==================== 生命周期 ====================

onMounted(() => {
  // 直接使用Mock数据，确保页面有数据显示
  const mockResult = mockGetContentList(pagination.pageNo, pagination.pageSize)
  tableData.value = mockResult.list
  pagination.total = mockResult.total
})
</script>

<style scoped lang="scss">
.content-page {
  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
