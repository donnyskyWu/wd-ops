<template>
  <div class="sop-page">
    <!-- 二级Tab -->
    <el-tabs v-model="activeTab" class="sop-tabs">
      <el-tab-pane label="SOP模板列表" name="template" />
      <el-tab-pane name="review">
        <template #label>
          <span>待审核任务</span>
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="review-badge" />
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- SOP模板列表 -->
    <div v-show="activeTab === 'template'" class="tab-content">
      <!-- 搜索区 -->
      <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
        <el-form-item label="模板名称">
          <el-input v-model="searchForm.templateName" placeholder="请输入模板名称" clearable maxlength="50" />
        </el-form-item>
        <el-form-item label="内容类型">
          <el-select v-model="searchForm.contentType" placeholder="请选择" clearable>
            <el-option label="全部" value="ALL" />
            <el-option label="文章" value="ARTICLE" />
            <el-option label="短视频" value="VIDEO" />
            <el-option label="直播" value="LIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </TableSearch>

      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增模板
        </el-button>
      </div>

      <!-- 数据表格 -->
      <ContentWrap>
        <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          style="width: 100%"
          :empty-text="'暂无SOP模板'"
        >
          <el-table-column prop="templateName" label="模板名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="contentType" label="适用内容类型" width="120" align="center">
            <template #default="{ row }">
              {{ getContentTypeText(row.contentType) }}
            </template>
          </el-table-column>
          <el-table-column prop="platformType" label="适用平台" width="120" align="center">
            <template #default="{ row }">
              {{ getPlatformTypeText(row.platformType) }}
            </template>
          </el-table-column>
          <el-table-column prop="nodeCount" label="节点数" width="90" align="center" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                @change="handleStatusChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-popconfirm
                title="确定删除该模板吗？"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button link type="danger">
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <Pagination
          :current-page="pagination.pageNo"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @update:current-page="(val) => pagination.pageNo = val"
          @update:page-size="(val) => { pagination.pageSize = val; loadTemplateList() }"
          @change="loadTemplateList"
        />
      </ContentWrap>
    </div>

    <!-- 待审核任务 -->
    <div v-show="activeTab === 'review'" class="tab-content">
      <!-- 筛选区 -->
      <TableSearch v-model="reviewSearchForm" @search="loadReviewList">
        <el-form-item label="模板">
          <el-select v-model="reviewSearchForm.templateId" placeholder="请选择" clearable>
            <el-option
              v-for="item in templateList"
              :key="item.id"
              :label="item.templateName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="reviewSearchForm.reviewStatus" dict-type="dict_review_status" placeholder="请选择" />
        </el-form-item>
      </TableSearch>

      <!-- 审核列表 -->
      <ContentWrap>
        <el-table
          v-loading="reviewLoading"
          :data="reviewTableData"
          border
          stripe
          style="width: 100%"
          :empty-text="'暂无审核任务'"
        >
          <el-table-column prop="taskName" label="任务名称" width="150" />
          <el-table-column prop="nodeName" label="节点名称" width="120" />
          <el-table-column prop="executorName" label="执行人" width="100" />
          <el-table-column prop="reviewStatus" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getReviewStatusType(row.reviewStatus)">
                {{ getReviewStatusText(row.reviewStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="160" align="center" />
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.reviewStatus === 'PENDING'"
                link
                type="success"
                @click="handleApprove(row)"
              >
                审核通过
              </el-button>
              <el-button
                v-if="row.reviewStatus === 'PENDING'"
                link
                type="danger"
                @click="handleReject(row)"
              >
                驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <Pagination
          :current-page="reviewPagination.pageNo"
          :page-size="reviewPagination.pageSize"
          :total="reviewPagination.total"
          @update:current-page="(val) => reviewPagination.pageNo = val"
          @update:page-size="(val) => { reviewPagination.pageSize = val; loadReviewList() }"
          @change="loadReviewList"
        />
      </ContentWrap>
    </div>

    <!-- 新增模板对话框 -->
    <el-dialog v-model="createDialogVisible" title="新增SOP模板" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="createForm.templateName" placeholder="请输入模板名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="内容类型">
          <el-select v-model="createForm.contentType" placeholder="请选择">
            <el-option label="全部" value="ALL" />
            <el-option label="文章" value="ARTICLE" />
            <el-option label="短视频" value="VIDEO" />
            <el-option label="直播" value="LIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用平台">
          <el-select v-model="createForm.platformType" placeholder="请选择">
            <el-option label="全部" value="ALL" />
            <el-option label="公众号" value="WECHAT_MP" />
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="快手" value="KUAISHOU" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 审核通过对话框 -->
    <el-dialog v-model="approveDialogVisible" title="审核通过" width="500px">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="审核意见">
          <el-input
            v-model="approveForm.comment"
            type="textarea"
            :rows="3"
            placeholder="可填写审核意见（选填）"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApproveSubmit">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 审核驳回对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="审核驳回" width="500px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="驳回意见" prop="comment">
          <el-input
            v-model="rejectForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请填写驳回意见（必填）"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleRejectSubmit">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getSopTemplateList,
  createSopTemplate,
  updateSopTemplateStatus,
  deleteSopTemplate,
  getSopReviewList,
  approveReview,
  rejectReview,
} from '@/api/sop'
import {
  mockGetSopTemplateList,
  mockGetSopReviewList,
} from '@/mock/sop'
import type {
  SopTemplateQuery,
  SopReviewQuery,
  CreateSopTemplateReq,
  ReviewActionReq,
  ContentType,
  PlatformType,
  SopStatus,
  ReviewStatus,
  SopTemplateVO,
  SopReviewVO,
} from '@/types/sop'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'

const router = useRouter()

// ==================== 响应式数据 ====================

// 当前Tab
const activeTab = ref('template')

// 搜索表单
const searchForm = reactive({
  templateName: '',
  contentType: undefined as ContentType | undefined,
  status: undefined as SopStatus | undefined,
})

// 加载状态
const loading = ref(false)

// 表格数据
const tableData = ref<SopTemplateVO[]>([])
const templateList = ref<SopTemplateVO[]>([])

// 分页参数
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

// 审核相关
const reviewLoading = ref(false)
const reviewSearchForm = reactive({
  templateId: undefined as number | undefined,
  reviewStatus: undefined as ReviewStatus | undefined,
})
const reviewTableData = ref<SopReviewVO[]>([])
const reviewPagination = reactive({
  pageNo: 1,
  pageSize: 20,
  total: 0,
})
const pendingCount = ref(0)

// 新增模板对话框
const createDialogVisible = ref(false)
const createFormRef = ref()
const createForm = reactive<CreateSopTemplateReq>({
  templateName: '',
  contentType: 'ALL',
  platformType: 'ALL',
  description: '',
})
const createRules = {
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在1到100个字符', trigger: 'blur' },
  ],
}

// 审核对话框
const approveDialogVisible = ref(false)
const approveForm = reactive<ReviewActionReq>({
  reviewId: 0,
  comment: '',
})

const rejectDialogVisible = ref(false)
const rejectFormRef = ref()
const rejectForm = reactive<ReviewActionReq>({
  reviewId: 0,
  comment: '',
})
const rejectRules = {
  comment: [
    { required: true, message: '请填写驳回意见', trigger: 'blur' },
    { min: 1, max: 200, message: '长度在1到200个字符', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

// 加载模板列表
const loadTemplateList = async () => {
  loading.value = true
  try {
    // 直接使用Mock数据
    console.log('加载模板列表, 页码:', pagination.pageNo, '每页:', pagination.pageSize)
    const result = mockGetSopTemplateList(pagination.pageNo, pagination.pageSize)
    
    console.log('获取到的模板数据:', result)
    
    tableData.value = result.list || []
    templateList.value = result.list || []
    pagination.total = result.total || 0
    
    if (result.list.length === 0) {
      ElMessage.warning('暂无SOP模板数据')
    }
  } catch (error) {
    console.error('加载SOP模板列表失败:', error)
    ElMessage.error('数据加载失败，请重试')
  } finally {
    loading.value = false
  }
}

// 加载审核列表
const loadReviewList = async () => {
  reviewLoading.value = true
  try {
    // 直接使用Mock数据
    console.log('加载审核列表, 页码:', reviewPagination.pageNo, '每页:', reviewPagination.pageSize)
    const result = mockGetSopReviewList(reviewPagination.pageNo, reviewPagination.pageSize)
    
    console.log('获取到的审核数据:', result)
    
    reviewTableData.value = result.list || []
    reviewPagination.total = result.total || 0
    
    // 计算待审核数
    pendingCount.value = (result.list || []).filter((item: SopReviewVO) => item.reviewStatus === 'PENDING').length
  } catch (error) {
    console.error('加载审核列表失败:', error)
    ElMessage.error('数据加载失败，请重试')
  } finally {
    reviewLoading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNo = 1
  loadTemplateList()
}

// 重置
const handleReset = () => {
  searchForm.templateName = ''
  searchForm.contentType = undefined
  searchForm.status = undefined
  pagination.pageNo = 1
  loadTemplateList()
}

// 新增模板
const handleCreate = () => {
  createForm.templateName = ''
  createForm.contentType = 'ALL'
  createForm.platformType = 'ALL'
  createForm.description = ''
  createDialogVisible.value = true
}

// 提交新增
const handleCreateSubmit = async () => {
  await createFormRef.value.validate()
  
  try {
    const data: CreateSopTemplateReq = {
      templateName: createForm.templateName,
      contentType: createForm.contentType,
      platformType: createForm.platformType,
      description: createForm.description,
    }

    const result = await createSopTemplate(data)
    
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadTemplateList()
    
    // 跳转到DAG编辑页
    router.push(`/sop/${result.id}/edit`)
  } catch (error: unknown) {
    if (error.response?.status === 409) {
      ElMessage.error('模板名称已存在')
    } else {
      ElMessage.error('创建失败，请重试')
    }
  }
}

// 编辑模板
const handleEdit = (row: SopTemplateVO) => {
  router.push(`/sop/${row.id}/edit`)
}

// 删除模板
const handleDelete = async (row: SopTemplateVO) => {
  try {
    await deleteSopTemplate(row.id).catch(() => {
      // Mock删除
      return Promise.resolve()
    })
    ElMessage.success('删除成功')
    loadTemplateList()
  } catch (error) {
    ElMessage.error('该模板下存在执行中的任务，无法删除')
  }
}

// 状态切换
const handleStatusChange = async (row: any) => {
  try {
    await updateSopTemplateStatus(row.id, row.status).catch(() => {
      return Promise.resolve()
    })
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
    row.status = row.status === 1 ? 0 : 1 // 恢复原状态
  }
}

// 审核通过
const handleApprove = (row: any) => {
  approveForm.reviewId = row.reviewId
  approveForm.comment = ''
  approveDialogVisible.value = true
}

// 提交审核通过
const handleApproveSubmit = async () => {
  try {
    const data: ReviewActionReq = {
      reviewId: approveForm.reviewId,
      comment: approveForm.comment,
    }

    await approveReview(data).catch(() => {
      return Promise.resolve()
    })
    
    ElMessage.success('审核通过')
    approveDialogVisible.value = false
    loadReviewList()
  } catch (error) {
    ElMessage.error('审核失败，请重试')
  }
}

// 审核驳回
const handleReject = (row: any) => {
  rejectForm.reviewId = row.reviewId
  rejectForm.comment = ''
  rejectDialogVisible.value = true
}

// 提交审核驳回
const handleRejectSubmit = async () => {
  await rejectFormRef.value.validate()
  
  try {
    const data: ReviewActionReq = {
      reviewId: rejectForm.reviewId,
      comment: rejectForm.comment,
    }

    await rejectReview(data).catch(() => {
      return Promise.resolve()
    })
    
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadReviewList()
  } catch (error) {
    ElMessage.error('驳回失败，请重试')
  }
}

// 获取内容类型文本
const getContentTypeText = (contentType: ContentType) => {
  const texts: Record<ContentType, string> = {
    ALL: '全部',
    ARTICLE: '文章',
    VIDEO: '短视频',
    LIVE: '直播',
  }
  return texts[contentType] || ''
}

// 获取平台类型文本
const getPlatformTypeText = (platformType: PlatformType) => {
  const texts: Record<PlatformType, string> = {
    ALL: '全部',
    WECHAT_MP: '公众号',
    VIDEO_ACCOUNT: '视频号',
    DOUYIN: '抖音',
    KUAISHOU: '快手',
    XIAOHONGSHU: '小红书',
    SERVICE_ACCOUNT: '服务号',
    WEWORK: '企业微信',
  }
  return texts[platformType] || ''
}

// 获取审核状态类型
const getReviewStatusType = (status: ReviewStatus) => {
  const types: Record<ReviewStatus, string> = {
    PENDING: 'warning',
    REVIEWING: 'primary',
    APPROVED: 'success',
    REJECTED: 'danger',
  }
  return types[status] || ''
}

// 获取审核状态文本
const getReviewStatusText = (status: ReviewStatus) => {
  const texts: Record<ReviewStatus, string> = {
    PENDING: '待审核',
    REVIEWING: '审核中',
    APPROVED: '已通过',
    REJECTED: '已驳回',
  }
  return texts[status] || ''
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadTemplateList()
  loadReviewList()
})
</script>

<style scoped lang="scss">
.sop-page {
  .sop-tabs {
    margin-bottom: 16px;

    .review-badge {
      margin-left: 4px;
    }
  }

  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
