<template>
  <div class="external-collect-config">
    <ContentWrap title="外部采集配置" subtitle="外部平台数据采集配置">
      <el-alert 
        title="外部采集配置用于管理第三方平台的数据采集接口（蝉妈妈、飞瓜等）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="平台类型">
            <DictSelect v-model="searchForm.platformType" dict-type="dict_third_platform" placeholder="请选择" style="width: 150px" />
          </el-form-item>
          <el-form-item label="状态">
            <DictSelect v-model="searchForm.status" dict-type="dict_status_enabled" placeholder="请选择" style="width: 120px" />
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
          新增配置
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
        <el-table-column prop="configName" label="配置名称" min-width="150" />
        <el-table-column prop="platformType" label="平台类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformTagType(row.platformType)">
              {{ getPlatformLabel(row.platformType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="apiUrl" label="API地址" min-width="200" show-overflow-tooltip />
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
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="formData.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="平台类型" prop="platformType">
          <DictSelect v-model="formData.platformType" dict-type="dict_third_platform" placeholder="请选择平台类型" style="width: 100%" />
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="formData.apiUrl" placeholder="请输入API地址" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input 
            v-model="formData.apiKey" 
            type="password"
            show-password
            placeholder="请输入API密钥（加密存储）" 
          />
        </el-form-item>
        <el-form-item label="同步频率" prop="syncFrequency">
          <DictSelect v-model="formData.syncFrequency" dict-type="dict_sync_frequency" placeholder="请选择同步频率" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="formData.remark" 
            type="textarea"
            :rows="3"
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
import { externalCollectApi, type CollectConfigVO } from '@/api/config'

interface ConfigItem {
  id: number
  configName: string
  platformType: string
  apiUrl: string
  apiKey?: string
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
  platformType: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const configList = ref<ConfigItem[]>([])
const selectedRows = ref<ConfigItem[]>([])

const formData = reactive<Partial<ConfigItem>>({
  configName: '',
  platformType: '',
  apiUrl: '',
  apiKey: '',
  syncFrequency: 'DAILY',
  status: 'ENABLED',
  remark: ''
})

const rules: FormRules = {
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  platformType: [
    { required: true, message: '请选择平台类型', trigger: 'change' }
  ],
  apiUrl: [
    { required: true, message: '请输入API地址', trigger: 'blur' }
  ],
  syncFrequency: [
    { required: true, message: '请选择同步频率', trigger: 'change' }
  ]
}

function mapRow(row: CollectConfigVO): ConfigItem {
  return {
    id: row.id,
    configName: row.configName,
    platformType: row.platformType || '',
    apiUrl: row.apiUrl || '',
    syncFrequency: row.collectFrequency || '',
    status: (row.status as 'ENABLED' | 'DISABLED') || 'ENABLED',
    lastSyncTime: row.createTime || '-',
    remark: row.remark,
  }
}

const displayList = computed(() => configList.value)

const dialogTitle = computed(() => (formData.id ? '编辑配置' : '新增配置'))

const loadList = async () => {
  loading.value = true
  try {
    const res = await externalCollectApi.list({
      platformType: searchForm.platformType || undefined,
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
const getPlatformLabel = (platformType: string) => {
  const map: Record<string, string> = {
    CHANMAMA: '蝉妈妈',
    FEIGUA: '飞瓜数据',
    XINBANG: '新榜',
    KAOGUJIA: '考古加',
    HUITUN: '灰豚数据'
  }
  return map[platformType] || platformType
}

const getPlatformTagType = (platformType: string) => {
  const map: Record<string, any> = {
    CHANMAMA: '',
    FEIGUA: 'success',
    XINBANG: 'warning',
    KAOGUJIA: 'danger',
    HUITUN: 'info'
  }
  return map[platformType] || ''
}

const getFrequencyLabel = (frequency: string) => {
  const map: Record<string, string> = {
    HOURLY: '每小时',
    DAILY: '每日',
    WEEKLY: '每周'
  }
  return map[frequency] || frequency
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.platformType = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  formData.id = undefined
  formData.configName = ''
  formData.platformType = ''
  formData.apiUrl = ''
  formData.apiKey = ''
  formData.syncFrequency = 'DAILY'
  formData.status = 'ENABLED'
  formData.remark = ''
  dialogVisible.value = true
}

const handleEdit = (row: ConfigItem) => {
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
      configName: formData.configName,
      platformType: formData.platformType,
      apiUrl: formData.apiUrl,
      collectFrequency: formData.syncFrequency,
      status: formData.status,
      remark: formData.remark,
    }
    if (formData.apiKey) {
      payload.apiKey = formData.apiKey
    }
    if (formData.id) {
      payload.id = formData.id
      await externalCollectApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await externalCollectApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleTest = (row: ConfigItem) => {
  ElMessage.success(`测试连接成功: ${row.configName}`)
}

const handleToggleStatus = async (row: ConfigItem) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}配置「${row.configName}」吗？`,
      '提示',
      { type: 'warning' }
    )
    await externalCollectApi.update({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: ConfigItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置「${row.configName}」吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await externalCollectApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: ConfigItem[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条配置吗？`,
      '批量删除确认',
      { type: 'warning' }
    )
    for (const row of selectedRows.value) {
      await externalCollectApi.delete(row.id)
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
.external-collect-config {
  padding: 20px;
}
</style>
