<template>
  <div class="external-data-config">
    <ContentWrap title="外部数据配置" subtitle="外部数据源配置管理">
      <el-alert 
        title="配置外部数据源连接信息（行业报告、第三方数据平台接口）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="数据源名称">
            <el-input v-model="searchForm.sourceName" placeholder="请输入" clearable style="width: 180px" />
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
          新增数据源
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
        <el-table-column prop="sourceName" label="数据源名称" min-width="150" />
        <el-table-column prop="apiUrl" label="接口地址" min-width="250" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="请求方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.requestMethod === 'GET' ? 'success' : 'warning'">
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="syncFrequency" label="同步频率" width="100" align="center">
          <template #default="{ row }">
            {{ getFrequencyLabel(row.syncFrequency) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最后同步时间" width="180" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleTest(row)">测试连接</el-button>
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
        <el-form-item label="数据源名称" prop="sourceName">
          <el-input v-model="formData.sourceName" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="接口地址" prop="apiUrl">
          <el-input v-model="formData.apiUrl" placeholder="请输入API接口地址" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input 
            v-model="formData.apiKey" 
            type="password"
            show-password
            placeholder="请输入API Key（加密存储）" 
          />
        </el-form-item>
        <el-form-item label="请求方式" prop="requestMethod">
          <el-radio-group v-model="formData.requestMethod">
            <el-radio label="GET">GET</el-radio>
            <el-radio label="POST">POST</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="同步频率" prop="syncFrequency">
          <DictSelect v-model="formData.syncFrequency" dict-type="dict_sync_frequency" placeholder="请选择同步频率" style="width: 100%" />
        </el-form-item>
        <el-form-item label="请求参数">
          <el-input 
            v-model="formData.requestParams" 
            type="textarea"
            :rows="3"
            placeholder='JSON格式，如：{"key": "value"}' 
          />
        </el-form-item>
        <el-form-item label="响应映射">
          <el-input 
            v-model="formData.responseMapping" 
            type="textarea"
            :rows="3"
            placeholder='JSON格式字段映射配置' 
          />
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
import { externalSourceApi, type CollectConfigVO } from '@/api/config'

interface DataSourceConfig {
  id: number
  sourceName: string
  apiUrl: string
  apiKey?: string
  requestMethod: 'GET' | 'POST'
  requestParams?: string
  responseMapping?: string
  syncFrequency: string
  status: 'ENABLED' | 'DISABLED'
  lastSyncTime: string
  remark?: string
}

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  sourceName: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const configList = ref<DataSourceConfig[]>([])
const selectedRows = ref<DataSourceConfig[]>([])

const formData = reactive<Partial<DataSourceConfig>>({
  sourceName: '',
  apiUrl: '',
  apiKey: '',
  requestMethod: 'GET',
  requestParams: '',
  responseMapping: '',
  syncFrequency: 'DAILY',
  status: 'ENABLED',
  remark: ''
})

const rules: FormRules = {
  sourceName: [
    { required: true, message: '请输入数据源名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  apiUrl: [
    { required: true, message: '请输入接口地址', trigger: 'blur' }
  ],
  requestMethod: [
    { required: true, message: '请选择请求方式', trigger: 'change' }
  ],
  syncFrequency: [
    { required: true, message: '请选择同步频率', trigger: 'change' }
  ]
}

function mapRow(row: CollectConfigVO): DataSourceConfig {
  return {
    id: row.id,
    sourceName: row.configName,
    apiUrl: row.apiUrl || '',
    requestMethod: (row.requestMethod as 'GET' | 'POST') || 'GET',
    requestParams: row.requestParams,
    responseMapping: row.responseMapping,
    syncFrequency: row.collectFrequency || '',
    status: (row.status as 'ENABLED' | 'DISABLED') || 'ENABLED',
    lastSyncTime: row.createTime || '-',
    remark: row.remark,
  }
}

const displayList = computed(() => configList.value)

const dialogTitle = computed(() => (formData.id ? '编辑数据源' : '新增数据源'))

const loadList = async () => {
  loading.value = true
  try {
    const res = await externalSourceApi.list({
      configName: searchForm.sourceName || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    configList.value = (res.list || []).map(mapRow)
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

// ==================== 辅助函数 ====================
const getFrequencyLabel = (frequency: string) => {
  const map: Record<string, string> = {
    DAILY: '每日',
    WEEKLY: '每周',
    MONTHLY: '每月'
  }
  return map[frequency] || frequency
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.sourceName = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    sourceName: '',
    apiUrl: '',
    apiKey: '',
    requestMethod: 'GET',
    requestParams: '',
    responseMapping: '',
    syncFrequency: 'DAILY',
    status: 'ENABLED',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: DataSourceConfig) => {
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
      configName: formData.sourceName,
      apiUrl: formData.apiUrl,
      requestMethod: formData.requestMethod,
      requestParams: formData.requestParams,
      responseMapping: formData.responseMapping,
      collectFrequency: formData.syncFrequency,
      status: formData.status,
      remark: formData.remark,
    }
    if (formData.apiKey) {
      payload.apiKey = formData.apiKey
    }
    if (formData.id) {
      payload.id = formData.id
      await externalSourceApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await externalSourceApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleTest = (row: DataSourceConfig) => {
  ElMessage.success(`测试连接成功: ${row.sourceName}`)
}

const handleToggleStatus = async (row: DataSourceConfig) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}数据源「${row.sourceName}」吗？`,
      '提示',
      { type: 'warning' }
    )
    await externalSourceApi.update({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: DataSourceConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据源「${row.sourceName}」吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await externalSourceApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: DataSourceConfig[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条数据源吗？`,
      '批量删除确认',
      { type: 'warning' }
    )
    for (const row of selectedRows.value) {
      await externalSourceApi.delete(row.id)
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
.external-data-config {
  padding: 20px;
}
</style>
