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
            <el-option label="短视频" value="SHORT_VIDEO" />
            <el-option label="视频" value="VIDEO" />
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
              <el-button link type="primary" @click.stop="handleEdit(row)">
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
          <el-table-column prop="planName" label="任务名称" width="150" />
          <el-table-column prop="nodeName" label="节点名称" width="120" />
          <el-table-column prop="reviewerRole" label="执行人岗位" width="120" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getReviewStatusType(row.status)">
                {{ getReviewStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="160" align="center" />
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                link
                type="success"
                @click="handleApprove(row)"
              >
                审核通过
              </el-button>
              <el-button
                v-if="row.status === 'PENDING'"
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
          <DictSelect v-model="createForm.contentType" dict-type="dict_content_type" placeholder="请选择" />
        </el-form-item>
        <el-form-item label="适用平台">
          <DictSelect v-model="createForm.platformType" dict-type="dict_platform_type" placeholder="请选择" />
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
  getSopReviewPending,
  approveReview,
  rejectReview,
} from '@/api/sop'
// P-GATE-UNMOCK-R S-R2-G：去 mock
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
import { PLATFORM_LABEL, type PlatformType as DictPlatform } from '@/utils/enum-alias'

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
    // P-GATE-UNMOCK-R S-R2-G：去 mock 接真 API
    // S-R11 B1: 后端 SopTemplateController.list 收 pageNum，spec API-M2 §1.1 显式定义 pageNum
    const query: SopTemplateQuery = {
      templateName: searchForm.templateName || undefined,
      contentType: searchForm.contentType,
      status: searchForm.status,
      pageNum: pagination.pageNo,
      pageSize: pagination.pageSize,
    }
    const result = await getSopTemplateList(query)
    
    console.log('获取到的模板数据:', result)
    
    tableData.value = result.list || []
    templateList.value = result.list || []
    pagination.total = result.total || 0
    
    // P-GATE-UNMOCK-R S-R2-G：去 mock（保留结构）
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
    // S-R11 B2/B3 修复：后端只有 /pending（返回 List 不分页），前端不再调不存在的 /list
    // S-R11 B8 修复：字段名对齐后端 SopReviewVO（id / status / createTime）
    const result: any = await getSopReviewPending()
    
    console.log('获取到的审核数据:', result)
    
    reviewTableData.value = result || []
    reviewPagination.total = (result || []).length
    
    // 计算待审核数
    pendingCount.value = (result || []).filter((item: SopReviewVO) => item.status === 'PENDING').length
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
    const newId = typeof result === 'number' ? result : (result as { id?: number })?.id
    if (newId) {
      router.push({
        name: 'SopEdit',
        params: { id: String(newId) },
        state: { templateName: createForm.templateName },
      })
    }
  } catch (error: any) {
    if (error.response?.status === 409) {
      ElMessage.error('模板名称已存在')
    } else {
      ElMessage.error(error?.response?.data?.msg || '创建失败，请重试')
    }
  }
}

// 编辑模板
const handleEdit = (row: SopTemplateVO) => {
  if (!row?.id) {
    ElMessage.error('模板 ID 无效')
    return
  }
  router.push({
    name: 'SopEdit',
    params: { id: String(row.id) },
    state: { templateName: row.templateName },
  })
}

// 删除模板
const handleDelete = async (row: SopTemplateVO) => {
  try {
    await deleteSopTemplate(row.id)
    ElMessage.success('删除成功')
    loadTemplateList()
  } catch (error: any) {
    // S-R11 B9 修复：不再静默吞错，让真实错误透传给用户
    const msg = error?.response?.data?.msg || '删除失败'
    ElMessage.error(msg.includes('任务') ? msg : `${msg}，可能该模板下存在执行中的任务`)
  }
}

// 状态切换
const handleStatusChange = async (row: any) => {
  try {
    await updateSopTemplateStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '状态更新失败')
    row.status = row.status === 1 ? 0 : 1 // 恢复原状态
  }
}

// 审核通过
const handleApprove = (row: any) => {
  approveForm.reviewId = row.id  // S-R11 B8 修复：后端 SopReviewVO 字段是 id
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

    await approveReview(data)
    
    ElMessage.success('审核通过')
    approveDialogVisible.value = false
    loadReviewList()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '审核失败，请重试')
  }
}

// 审核驳回
const handleReject = (row: any) => {
  rejectForm.reviewId = row.id  // S-R11 B8 修复
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

    await rejectReview(data)
    
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadReviewList()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '驳回失败，请重试')
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

// 获取平台类型文本（dict 真实值，含 ALL / WECHAT_OFFICIAL 等）
const getPlatformTypeText = (platformType: PlatformType) => {
  return PLATFORM_LABEL[platformType as DictPlatform] ?? platformType
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
