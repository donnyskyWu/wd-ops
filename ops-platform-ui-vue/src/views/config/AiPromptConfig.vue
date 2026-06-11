<template>
  <div class="ai-prompt-config">
    <ContentWrap title="AI提示词配置" subtitle="AI提示词模板管理">
      <el-alert 
        title="管理AI生成内容的提示词模板（支持文案生成、数据分析、报告撰写等场景）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" style="margin-bottom: 16px">
        <el-tab-pane label="文案生成" name="copywriting" />
        <el-tab-pane label="数据分析" name="analysis" />
        <el-tab-pane label="报告撰写" name="report" />
        <el-tab-pane label="其他场景" name="other" />
      </el-tabs>
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="模板名称">
            <el-input v-model="searchForm.templateName" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="应用场景">
            <DictSelect v-model="searchForm.scene" dict-type="dict_ai_scene" placeholder="请选择" style="width: 140px" />
          </el-form-item>
          <el-form-item label="状态">
            <DictSelect v-model="searchForm.status" dict-type="dict_config_status" placeholder="请选择" style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </TableSearch>

      <!-- 工具栏 -->
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增模板
        </el-button>
        <el-button 
          type="danger" 
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          删除选中
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table 
        :data="displayList" 
        border 
        stripe 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="scene" label="应用场景" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getSceneTagType(row.scene)">
              {{ getSceneLabel(row.scene) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="promptContent" label="提示词内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="info" @click="handleView(row)">查看</el-button>
            <el-button link type="warning" @click="handleToggleStatus(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </ContentWrap>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="formData.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="应用场景" prop="scene">
          <DictSelect v-model="formData.scene" dict-type="dict_ai_scene" placeholder="请选择应用场景" style="width: 100%" />
        </el-form-item>
        <el-form-item label="提示词内容" prop="promptContent">
          <el-input 
            v-model="formData.promptContent" 
            type="textarea"
            :rows="8"
            placeholder="请输入提示词模板内容，使用{{变量名}}作为占位符" 
          />
        </el-form-item>
        <el-form-item label="变量说明">
          <el-input 
            v-model="formData.variableDesc" 
            type="textarea"
            :rows="3"
            placeholder="说明模板中使用的变量，如：{{product_name}} - 产品名称" 
          />
        </el-form-item>
        <el-form-item label="温度参数">
          <el-slider v-model="formData.temperature" :min="0" :max="1" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="formData.remark" 
            type="textarea"
            :rows="2"
            placeholder="请输入备注信息" 
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch 
            v-model="formData.status" 
            active-value="ENABLED" 
            inactive-value="DISABLED"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看详情弹窗 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="提示词详情"
      width="800px"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模板名称">{{ viewData.templateName }}</el-descriptions-item>
        <el-descriptions-item label="应用场景">
          <el-tag :type="getSceneTagType(viewData.scene)">
            {{ getSceneLabel(viewData.scene) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="版本">{{ viewData.version }}</el-descriptions-item>
        <el-descriptions-item label="提示词内容">
          <div style="white-space: pre-wrap; background: #f5f7fa; padding: 12px; border-radius: 4px;">
            {{ viewData.promptContent }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="变量说明" v-if="viewData.variableDesc">
          {{ viewData.variableDesc }}
        </el-descriptions-item>
        <el-descriptions-item label="温度参数">{{ viewData.temperature }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.status === 'ENABLED' ? 'success' : 'danger'">
            {{ viewData.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" v-if="viewData.remark">
          {{ viewData.remark }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ viewData.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  fetchAiPromptList,
  createAiPrompt,
  updateAiPrompt,
  deleteAiPrompt,
  type AiPromptConfigVO,
} from '@/api/config'

interface AiPromptTemplate {
  id: number
  templateName: string
  scene: string
  promptContent: string
  variableDesc?: string
  temperature: number
  version: string
  status: 'ENABLED' | 'DISABLED'
  updateTime: string
  remark?: string
}

const TAB_SCENES: Record<string, string[]> = {
  copywriting: ['SHORT_VIDEO', 'LIVE_SCRIPT', 'XIAOHONGSHU', 'WECHAT_ARTICLE'],
  analysis: ['DATA_ANALYSIS', 'COMPETITOR'],
  report: ['REPORT'],
  other: [],
}

const ALL_KNOWN_SCENES = [
  'SHORT_VIDEO', 'LIVE_SCRIPT', 'XIAOHONGSHU', 'WECHAT_ARTICLE',
  'DATA_ANALYSIS', 'REPORT', 'COMPETITOR',
]

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const activeTab = ref('copywriting')

const searchForm = reactive({
  templateName: '',
  scene: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const templateList = ref<AiPromptTemplate[]>([])
const selectedRows = ref<AiPromptTemplate[]>([])

const formData = reactive<Partial<AiPromptTemplate>>({
  templateName: '',
  scene: '',
  promptContent: '',
  variableDesc: '',
  temperature: 0.7,
  version: 'v1.0',
  status: 'ENABLED',
  remark: ''
})

const viewData = reactive<Partial<AiPromptTemplate>>({})

const rules: FormRules = {
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  scene: [
    { required: true, message: '请选择应用场景', trigger: 'change' }
  ],
  promptContent: [
    { required: true, message: '请输入提示词内容', trigger: 'blur' }
  ]
}

function mapRow(row: AiPromptConfigVO): AiPromptTemplate {
  return {
    id: row.id,
    templateName: row.templateName,
    scene: row.scene || '',
    promptContent: row.promptContent,
    variableDesc: row.variableDesc,
    temperature: row.temperature ?? 0.7,
    version: 'v1.0',
    status: (row.status as 'ENABLED' | 'DISABLED') || 'ENABLED',
    updateTime: row.createTime || '-',
    remark: row.remark,
  }
}

const displayList = computed(() => {
  const tabScenes = TAB_SCENES[activeTab.value]
  return templateList.value.filter((item) => {
    if (activeTab.value === 'other') {
      if (ALL_KNOWN_SCENES.includes(item.scene)) return false
    } else if (tabScenes?.length && !tabScenes.includes(item.scene)) {
      return false
    }
    if (searchForm.scene && item.scene !== searchForm.scene) return false
    return true
  })
})

const dialogTitle = computed(() => (formData.id ? '编辑模板' : '新增模板'))

const loadList = async () => {
  loading.value = true
  try {
    const res = await fetchAiPromptList({
      templateName: searchForm.templateName || undefined,
      scene: searchForm.scene || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    templateList.value = (res.list || []).map(mapRow)
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  pageNo.value = 1
})

// ==================== 辅助函数 ====================
const getSceneLabel = (scene: string) => {
  const map: Record<string, string> = {
    SHORT_VIDEO: '短视频文案',
    LIVE_SCRIPT: '直播脚本',
    XIAOHONGSHU: '小红书笔记',
    WECHAT_ARTICLE: '公众号文章',
    DATA_ANALYSIS: '数据分析',
    REPORT: '周报月报',
    COMPETITOR: '竞品分析'
  }
  return map[scene] || scene
}

const getSceneTagType = (scene: string) => {
  const map: Record<string, any> = {
    SHORT_VIDEO: '',
    LIVE_SCRIPT: 'success',
    XIAOHONGSHU: 'warning',
    WECHAT_ARTICLE: 'danger',
    DATA_ANALYSIS: 'info',
    REPORT: '',
    COMPETITOR: 'success'
  }
  return map[scene] || ''
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.templateName = ''
  searchForm.scene = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    templateName: '',
    scene: '',
    promptContent: '',
    variableDesc: '',
    temperature: 0.7,
    version: 'v1.0',
    status: 'ENABLED',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: AiPromptTemplate) => {
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload: Record<string, unknown> = {
      templateName: formData.templateName,
      scene: formData.scene,
      promptContent: formData.promptContent,
      variableDesc: formData.variableDesc,
      temperature: formData.temperature,
      status: formData.status,
      remark: formData.remark,
    }
    if (formData.id) {
      payload.id = formData.id
      await updateAiPrompt(payload)
      ElMessage.success('编辑成功')
    } else {
      await createAiPrompt(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleView = (row: AiPromptTemplate) => {
  Object.assign(viewData, row)
  viewDialogVisible.value = true
}

const handleToggleStatus = async (row: AiPromptTemplate) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}模板「${row.templateName}」吗？`,
      '提示',
      { type: 'warning' }
    )
    await updateAiPrompt({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: AiPromptTemplate) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板「${row.templateName}」吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteAiPrompt(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: AiPromptTemplate[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条模板吗？`,
      '批量删除确认',
      { type: 'warning' }
    )
    for (const row of selectedRows.value) {
      await deleteAiPrompt(row.id)
    }
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    await loadList()
  } catch {}
}

const handlePageChange = (page: number) => {
  pageNo.value = page
  loadList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNo.value = 1
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.ai-prompt-config {
  padding: 20px;
}
</style>
