<template>
  <div class="knowledge-page">
    <!-- 筛选区 -->
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="搜索标题或标签" clearable />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="searchForm.category" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="案例库" value="case" />
          <el-option label="模板库" value="template" />
          <el-option label="行业资料" value="industry" />
          <el-option label="运营经验" value="experience" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否公开">
        <el-switch v-model="searchForm.isPublic" />
      </el-form-item>
    </TableSearch>

    <!-- 操作区 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增知识
      </el-button>
      <el-radio-group v-model="viewMode" class="view-switch">
        <el-radio-button value="list">
          <el-icon><List /></el-icon>
          列表视图
        </el-radio-button>
        <el-radio-button value="card">
          <el-icon><Grid /></el-icon>
          卡片视图
        </el-radio-button>
      </el-radio-group>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <!-- 列表视图 -->
    <el-table v-if="viewMode === 'list'" :data="knowledgeList" v-loading="loading" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120">
        <template #default="{ row }">
          <el-tag :type="getCategoryType(row.category)">
            {{ getCategoryLabel(row.category) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tags" label="标签" width="180">
        <template #default="{ row }">
          <el-tag v-for="tag in row.tags" :key="tag" size="small" class="tag-item">
            {{ tag }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isPublic" label="是否公开" width="100" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.isPublic" color="#67C23A"><View /></el-icon>
          <el-icon v-else color="#E6A23C"><Lock /></el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="创建者" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 卡片视图 -->
    <el-row v-else :gutter="16" v-loading="loading" class="card-view">
      <el-col v-for="item in knowledgeList" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="knowledge-card" @click="handleView(item)">
          <div class="card-header">
            <el-tag :type="getCategoryType(item.category)" size="small">
              {{ getCategoryLabel(item.category) }}
            </el-tag>
          </div>
          <h3 class="card-title">{{ item.title }}</h3>
          <div class="card-tags">
            <el-tag v-for="tag in item.tags.slice(0, 3)" :key="tag" size="small" effect="plain">
              {{ tag }}
            </el-tag>
          </div>
          <div class="card-footer">
            <span><el-icon><View /></el-icon> {{ item.viewCount }}</span>
            <span><el-icon><Star /></el-icon> {{ item.likeCount }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分页 -->
    <el-pagination
      :current-page="searchForm.pageNo"
      :page-size="searchForm.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @update:current-page="(val) => searchForm.pageNo = val"
      @update:page-size="(val) => { searchForm.pageSize = val; handleSearch() }"
      @current-change="handleSearch"
      @size-change="handleSearch"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入标题（1-100字）" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-radio-group v-model="formData.category">
            <el-radio value="case">案例库</el-radio>
            <el-radio value="template">模板库</el-radio>
            <el-radio value="industry">行业资料</el-radio>
            <el-radio value="experience">运营经验</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="formData.tags"
            multiple
            filterable
            allow-create
            placeholder="请输入标签（最多10个）"
            :max-collapse-tags="10"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="formData.isPublic" />
          <span class="form-tip">（公开后团队所有成员可见）</span>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="12"
            placeholder="请输入知识内容（支持HTML富文本）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="知识详情" size="600px">
      <template v-if="currentDetail">
        <div class="detail-header">
          <h2>{{ currentDetail.title }}</h2>
          <div class="detail-meta">
            <el-tag :type="getCategoryType(currentDetail.category)">
              {{ getCategoryLabel(currentDetail.category) }}
            </el-tag>
            <span class="meta-item">
              <el-tag v-for="tag in currentDetail.tags" :key="tag" size="small">
                {{ tag }}
              </el-tag>
            </span>
            <el-tag :type="currentDetail.isPublic ? 'success' : 'warning'">
              {{ currentDetail.isPublic ? '公开' : '私有' }}
            </el-tag>
            <span class="meta-item">创建者: {{ currentDetail.creatorName }}</span>
            <span class="meta-item">{{ currentDetail.createdAt }}</span>
          </div>
        </div>

        <el-divider />

        <div class="detail-content" v-html="currentDetail.content" />

        <el-divider />

        <div class="detail-footer">
          <div class="stats">
            <span><el-icon><View /></el-icon> 阅读 {{ currentDetail.viewCount }}</span>
            <span><el-icon><Star /></el-icon> 收藏 {{ currentDetail.likeCount }}</span>
          </div>
          <el-button
            :type="currentDetail.isLiked ? 'warning' : 'primary'"
            @click="handleToggleLike"
          >
            <el-icon><Star /></el-icon>
            {{ currentDetail.isLiked ? '取消收藏' : '收藏' }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, List, Grid, View, Lock, Star } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import {
  getKnowledgeList,
  getKnowledgeDetail,
  createKnowledge,
  updateKnowledge,
  deleteKnowledge,
  toggleLike,
} from '@/api/knowledge'
import { mockKnowledgeList, queryKnowledgeList, mockKnowledgeDetails } from '@/mock/knowledge'
import type { KnowledgeVO, KnowledgeDetailVO, KnowledgeFormData } from '@/types/knowledge'
import { KnowledgeCategory, CATEGORY_LABELS } from '@/types/knowledge'

// ==================== 列表数据 ====================

const loading = ref(false)
const knowledgeList = ref<KnowledgeVO[]>([...mockKnowledgeList])
const total = ref(mockKnowledgeList.length)
const viewMode = ref<'list' | 'card'>('list')

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  category: undefined as string | undefined,
  isPublic: undefined as boolean | undefined,
})

// ==================== 加载列表 ====================

const loadList = async () => {
  loading.value = true
  try {
    // 优先使用API，失败时降级到Mock
    try {
      const res = await getKnowledgeList(searchForm)
      knowledgeList.value = res.list
      total.value = res.total
    } catch {
      const res = queryKnowledgeList(searchForm)
      knowledgeList.value = res.list
      total.value = res.total
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNo = 1
  loadList()
}

const handleReset = () => {
  searchForm.keyword = undefined
  searchForm.category = undefined
  searchForm.isPublic = undefined
  handleSearch()
}

// ==================== 分类工具函数 ====================

const getCategoryLabel = (category: string) => {
  return CATEGORY_LABELS[category as KnowledgeCategory] || category
}

const getCategoryType = (category: string) => {
  const typeMap: Record<string, any> = {
    case: 'danger',
    template: 'primary',
    industry: 'info',
    experience: 'success',
  }
  return typeMap[category] || ''
}

// ==================== 新增/编辑 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('新增知识')
const submitLoading = ref(false)
const formRef = ref()

const formData = reactive<KnowledgeFormData>({
  title: '',
  category: KnowledgeCategory.TEMPLATE,
  content: '',
  tags: [],
  isPublic: true,
})

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

const handleAdd = () => {
  dialogTitle.value = '新增知识'
  dialogVisible.value = true
}

const handleEdit = (row: KnowledgeVO) => {
  dialogTitle.value = '编辑知识'
  Object.assign(formData, {
    id: row.id,
    title: row.title,
    category: row.category,
    content: '<p>富文本内容...</p>',
    tags: [...row.tags],
    isPublic: row.isPublic,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    // 标签数量限制
    if (formData.tags.length > 10) {
      ElMessage.warning('最多10个标签')
      return
    }

    submitLoading.value = true
    try {
      if (formData.id) {
        await updateKnowledge(formData)
        ElMessage.success('更新成功')
      } else {
        await createKnowledge(formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadList()
    } catch (error) {
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: undefined,
    title: '',
    category: KnowledgeCategory.TEMPLATE,
    content: '',
    tags: [],
    isPublic: true,
  })
}

// ==================== 查看详情 ====================

const detailVisible = ref(false)
const currentDetail = ref<KnowledgeDetailVO | null>(null)

const handleView = async (row: KnowledgeVO) => {
  try {
    // 优先使用API，失败时降级到Mock
    try {
      currentDetail.value = await getKnowledgeDetail(row.id)
    } catch {
      currentDetail.value = mockKnowledgeDetails[row.id] || {
        ...row,
        content: '<p>暂无内容</p>',
        isLiked: false,
        updatedAt: row.createdAt,
      }
    }
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

// ==================== 删除 ====================

const handleDelete = async (row: KnowledgeVO) => {
  try {
    await ElMessageBox.confirm('确认删除该知识？删除后不可恢复', '提示', {
      type: 'warning',
    })

    await deleteKnowledge(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    // 用户取消或删除失败
  }
}

// ==================== 收藏 ====================

const handleToggleLike = async () => {
  if (!currentDetail.value) return

  try {
    const action = currentDetail.value.isLiked ? 'unlike' : 'like'
    await toggleLike({
      id: currentDetail.value.id,
      action,
    })

    currentDetail.value.isLiked = !currentDetail.value.isLiked
    currentDetail.value.likeCount += action === 'like' ? 1 : -1

    ElMessage.success(action === 'like' ? '收藏成功' : '取消收藏')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// ==================== 初始化 ====================

onMounted(() => {
  // 直接使用Mock数据，确保页面有数据显示
  const mockResult = queryKnowledgeList(searchForm)
  knowledgeList.value = mockResult.list
  total.value = mockResult.total
})
</script>

<style scoped>
.knowledge-page {
  padding: 20px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.view-switch {
  margin-left: auto;
}

.total-info {
  color: #909399;
  font-size: 14px;
}

.tag-item {
  margin-right: 4px;
  margin-bottom: 4px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 卡片视图 */
.card-view {
  margin-bottom: 16px;
}

.knowledge-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.3s;
}

.knowledge-card:hover {
  transform: translateY(-4px);
}

.card-header {
  margin-bottom: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  margin-bottom: 12px;
}

.card-tags .el-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 14px;
}

.card-footer .el-icon {
  margin-right: 4px;
}

/* 详情抽屉 */
.detail-header h2 {
  margin: 0 0 12px 0;
  font-size: 20px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-item {
  color: #606266;
  font-size: 14px;
}

.detail-content {
  line-height: 1.8;
  font-size: 15px;
}

.detail-content :deep(h2) {
  font-size: 18px;
  margin-top: 24px;
  margin-bottom: 12px;
}

.detail-content :deep(h3) {
  font-size: 16px;
  margin-top: 20px;
  margin-bottom: 8px;
}

.detail-content :deep(p) {
  margin: 8px 0;
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats {
  display: flex;
  gap: 24px;
  color: #606266;
  font-size: 14px;
}

.stats .el-icon {
  margin-right: 4px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
