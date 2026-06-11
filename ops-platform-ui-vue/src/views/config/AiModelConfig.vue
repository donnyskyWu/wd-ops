<template>
  <div class="ai-model-config">
    <ContentWrap title="AI模型配置" subtitle="AI模型连接配置管理">
      <el-alert title="管理 AI 大模型接入配置，支持连接测试与默认模型设置" type="info" :closable="false" style="margin-bottom: 16px" />

      <el-row :gutter="16" style="margin-bottom: 16px">
        <el-col :span="6"><el-statistic title="模型总数" :value="stats.total" /></el-col>
        <el-col :span="6"><el-statistic title="已启用" :value="stats.enabled" /></el-col>
        <el-col :span="6"><el-statistic title="连接正常" :value="stats.connected" /></el-col>
        <el-col :span="6"><el-statistic title="默认模型" :value="stats.defaultCount" /></el-col>
      </el-row>

      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="模型名称">
            <el-input v-model="searchForm.modelName" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="模型类型">
            <DictSelect v-model="searchForm.modelType" dict-type="dict_ai_model_type" placeholder="请选择" style="width: 140px" />
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
          新增模型
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
        <el-table-column prop="modelName" label="模型名称" min-width="150" />
        <el-table-column prop="modelType" label="模型类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getModelTypeTagType(row.modelType)">
              {{ getModelTypeLabel(row.modelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="apiEndpoint" label="API地址" min-width="250" show-overflow-tooltip />
        <el-table-column prop="maxTokens" label="最大Token" width="100" align="center" />
        <el-table-column prop="temperature" label="Temperature" width="110" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleTest(row)">测试连接</el-button>
            <el-button link type="primary" :disabled="row.isDefault" @click="handleSetDefault(row)">设默认</el-button>
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
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="formData.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <DictSelect v-model="formData.modelType" dict-type="dict_ai_model_type" placeholder="请选择模型类型" style="width: 100%" />
        </el-form-item>
        <el-form-item label="API地址" prop="apiEndpoint">
          <el-input v-model="formData.apiEndpoint" placeholder="请输入API Endpoint" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input 
            v-model="formData.apiKey" 
            type="password"
            show-password
            placeholder="请输入API Key（加密存储）" 
          />
        </el-form-item>
        <el-form-item label="最大Token" prop="maxTokens">
          <el-input-number v-model="formData.maxTokens" :min="1" :max="32000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-slider v-model="formData.temperature" :min="0" :max="1" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="Top P">
          <el-slider v-model="formData.topP" :min="0" :max="1" :step="0.1" show-input />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  fetchAiModelList,
  fetchAiModelStats,
  createAiModel,
  updateAiModel,
  deleteAiModel,
  testAiModelConnection,
  setDefaultAiModel,
  type AiModelConfigVO,
  type AiModelStatsVO,
} from '@/api/config'

interface AiModelConfig {
  id: number
  modelName: string
  modelId?: string
  modelType: string
  apiEndpoint: string
  apiKey?: string
  maxTokens: number
  timeout?: number
  isDefault?: boolean
  connStatus?: string
  temperature: number
  topP: number
  status: 'ENABLED' | 'DISABLED'
  remark?: string
}

const stats = ref<AiModelStatsVO>({ total: 0, enabled: 0, connected: 0, defaultCount: 0 })
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  modelName: '',
  modelType: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const modelList = ref<AiModelConfig[]>([])
const selectedRows = ref<AiModelConfig[]>([])

const formData = reactive<Partial<AiModelConfig>>({
  modelName: '',
  modelType: '',
  apiEndpoint: '',
  apiKey: '',
  maxTokens: 2048,
  temperature: 0.7,
  topP: 0.9,
  status: 'ENABLED',
  remark: ''
})

const rules: FormRules = {
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  modelType: [
    { required: true, message: '请选择模型类型', trigger: 'change' }
  ],
  apiEndpoint: [
    { required: true, message: '请输入API地址', trigger: 'blur' }
  ],
  apiKey: [
    {
      validator: (_rule, value, callback) => {
        if (!formData.id && !value) callback(new Error('请输入API密钥'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  maxTokens: [
    { required: true, message: '请输入最大Token数', trigger: 'blur' }
  ]
}

function mapRow(row: AiModelConfigVO): AiModelConfig {
  return {
    id: row.id,
    modelName: row.modelName,
    modelId: row.modelId,
    modelType: row.modelType || '',
    apiEndpoint: row.apiEndpoint || '',
    maxTokens: row.maxTokens ?? 2048,
    timeout: row.timeout,
    isDefault: row.isDefault,
    connStatus: row.connStatus,
    temperature: row.temperature ?? 0.7,
    topP: row.topP ?? 0.9,
    status: (row.status as 'ENABLED' | 'DISABLED') || 'ENABLED',
    remark: row.remark,
  }
}

const loadStats = async () => {
  stats.value = await fetchAiModelStats()
}

const displayList = computed(() => {
  return modelList.value.filter((item) => {
    if (searchForm.modelType && item.modelType !== searchForm.modelType) return false
    return true
  })
})

const dialogTitle = computed(() => (formData.id ? '编辑模型' : '新增模型'))

const loadList = async () => {
  loading.value = true
  try {
    const res = await fetchAiModelList({
      modelName: searchForm.modelName || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    modelList.value = (res.list || []).map(mapRow)
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

// ==================== 辅助函数 ====================
const getModelTypeLabel = (modelType: string) => {
  const map: Record<string, string> = {
    QWEN: '通义千问',
    ERNIE: '文心一言',
    GPT: 'ChatGPT',
    GLM: '智谱AI',
    MOONSHOT: '月之暗面'
  }
  return map[modelType] || modelType
}

const getModelTypeTagType = (modelType: string) => {
  const map: Record<string, any> = {
    QWEN: '',
    ERNIE: 'success',
    GPT: 'warning',
    GLM: 'danger',
    MOONSHOT: 'info'
  }
  return map[modelType] || ''
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.modelName = ''
  searchForm.modelType = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    modelName: '',
    modelType: '',
    apiEndpoint: '',
    apiKey: '',
    maxTokens: 2048,
    temperature: 0.7,
    topP: 0.9,
    status: 'ENABLED',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: AiModelConfig) => {
  Object.assign(formData, { ...row, apiKey: '' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload: Record<string, unknown> = {
      modelName: formData.modelName,
      modelType: formData.modelType,
      apiEndpoint: formData.apiEndpoint,
      maxTokens: formData.maxTokens,
      temperature: formData.temperature,
      topP: formData.topP,
      status: formData.status,
      remark: formData.remark,
    }
    if (formData.apiKey) {
      payload.apiKey = formData.apiKey
    }
    if (formData.id) {
      payload.id = formData.id
      await updateAiModel(payload)
      ElMessage.success('编辑成功')
    } else {
      await createAiModel(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleTest = async (row: AiModelConfig) => {
  const ok = await testAiModelConnection(row.id)
  ElMessage[ok ? 'success' : 'error'](ok ? '连接成功' : '连接失败')
  await loadList()
  await loadStats()
}

const handleSetDefault = async (row: AiModelConfig) => {
  await setDefaultAiModel(row.id)
  ElMessage.success('已设为默认模型')
  await loadList()
  await loadStats()
}

const handleToggleStatus = async (row: AiModelConfig) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}模型「${row.modelName}」吗？`,
      '提示',
      { type: 'warning' }
    )
    await updateAiModel({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: AiModelConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型「${row.modelName}」吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteAiModel(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: AiModelConfig[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条模型配置吗？`,
      '批量删除确认',
      { type: 'warning' }
    )
    for (const row of selectedRows.value) {
      await deleteAiModel(row.id)
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

onMounted(() => { loadList(); loadStats() })
</script>

<style scoped>
.ai-model-config {
  padding: 20px;
}
</style>
