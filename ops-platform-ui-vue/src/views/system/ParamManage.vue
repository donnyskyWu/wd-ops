<template>
  <div class="param-manage">
    <ContentWrap title="系统参数" subtitle="系统运行参数配置">
      <el-alert 
        title="配置系统运行的关键参数（影响系统行为和性能）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" style="margin-bottom: 16px">
        <el-tab-pane label="基础配置" name="basic" />
        <el-tab-pane label="采集配置" name="collect" />
        <el-tab-pane label="AI配置" name="ai" />
        <el-tab-pane label="通知配置" name="notification" />
      </el-tabs>
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="参数名称">
            <el-input v-model="searchForm.paramName" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="参数键">
            <el-input v-model="searchForm.paramKey" placeholder="请输入" clearable style="width: 180px" />
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
          新增参数
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
        <el-table-column prop="paramName" label="参数名称" min-width="180" />
        <el-table-column prop="paramKey" label="参数键" min-width="200" />
        <el-table-column prop="paramValue" label="参数值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="paramType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getParamTypeTagType(row.paramType)">
              {{ getParamTypeLabel(row.paramType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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
        <el-form-item label="参数名称" prop="paramName">
          <el-input v-model="formData.paramName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键" prop="paramKey">
          <el-input v-model="formData.paramKey" placeholder="请输入参数键（唯一标识）" />
        </el-form-item>
        <el-form-item label="参数值" prop="paramValue">
          <el-input v-model="formData.paramValue" placeholder="请输入参数值" />
        </el-form-item>
        <el-form-item label="参数类型" prop="paramType">
          <el-select v-model="formData.paramType" placeholder="请选择参数类型" style="width: 100%">
            <el-option label="字符串" value="STRING" />
            <el-option label="数字" value="NUMBER" />
            <el-option label="布尔值" value="BOOLEAN" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input 
            v-model="formData.remark" 
            type="textarea"
            :rows="3"
            placeholder="请输入参数说明" 
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

// ==================== 类型定义 ====================
interface SystemParam {
  id: number
  paramName: string
  paramKey: string
  paramValue: string
  paramType: string
  remark?: string
  updateTime: string
}

// ==================== 响应式数据 ====================
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const activeTab = ref('basic')

const searchForm = reactive({
  paramName: '',
  paramKey: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref<SystemParam[]>([])

const formData = reactive<Partial<SystemParam>>({
  paramName: '',
  paramKey: '',
  paramValue: '',
  paramType: 'STRING',
  remark: ''
})

const rules: FormRules = {
  paramName: [
    { required: true, message: '请输入参数名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  paramKey: [
    { required: true, message: '请输入参数键', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_.]*$/, message: '只能包含字母、数字、下划线和点', trigger: 'blur' }
  ],
  paramValue: [
    { required: true, message: '请输入参数值', trigger: 'blur' }
  ],
  paramType: [
    { required: true, message: '请选择参数类型', trigger: 'change' }
  ]
}

// ==================== Mock 数据 ====================
const mockData: SystemParam[] = [
  {
    id: 1,
    paramName: '数据采集间隔（秒）',
    paramKey: 'collect.interval.seconds',
    paramValue: '3600',
    paramType: 'NUMBER',
    remark: '定时采集任务的时间间隔，单位：秒',
    updateTime: '2026-05-28 10:00:00'
  },
  {
    id: 2,
    paramName: '最大并发采集数',
    paramKey: 'collect.max.concurrency',
    paramValue: '10',
    paramType: 'NUMBER',
    remark: '同时进行的采集任务最大数量',
    updateTime: '2026-05-28 09:30:00'
  },
  {
    id: 3,
    paramName: 'AI生成内容审核开关',
    paramKey: 'ai.content.review.enabled',
    paramValue: 'true',
    paramType: 'BOOLEAN',
    remark: '是否启用AI生成内容的自动审核流程',
    updateTime: '2026-05-27 15:00:00'
  },
  {
    id: 4,
    paramName: '默认AI模型',
    paramKey: 'ai.default.model',
    paramValue: 'QWEN',
    paramType: 'STRING',
    remark: '系统默认使用的AI模型类型',
    updateTime: '2026-05-28 08:00:00'
  },
  {
    id: 5,
    paramName: '邮件通知SMTP配置',
    paramKey: 'notification.email.smtp',
    paramValue: '{"host": "smtp.example.com", "port": 587, "username": "noreply@example.com"}',
    paramType: 'JSON',
    remark: '邮件通知的SMTP服务器配置',
    updateTime: '2026-05-26 14:00:00'
  },
  {
    id: 6,
    paramName: '企业微信Webhook地址',
    paramKey: 'notification.wework.webhook',
    paramValue: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxxxx',
    paramType: 'STRING',
    remark: '企业微信群机器人Webhook地址',
    updateTime: '2026-05-25 10:00:00'
  },
  {
    id: 7,
    paramName: '数据保留天数',
    paramKey: 'data.retention.days',
    paramValue: '365',
    paramType: 'NUMBER',
    remark: '历史数据保留的天数，超过自动清理',
    updateTime: '2026-05-20 09:00:00'
  },
  {
    id: 8,
    paramName: 'API请求超时时间（毫秒）',
    paramKey: 'api.timeout.milliseconds',
    paramValue: '30000',
    paramType: 'NUMBER',
    remark: '外部API请求的超时时间',
    updateTime: '2026-05-15 16:00:00'
  }
]

// ==================== 计算属性 ====================
const displayList = computed(() => {
  let filtered = [...mockData]
  
  // 按Tab过滤
  if (activeTab.value === 'basic') {
    filtered = filtered.filter(item => 
      ['data.retention.days', 'api.timeout.milliseconds'].includes(item.paramKey)
    )
  } else if (activeTab.value === 'collect') {
    filtered = filtered.filter(item => 
      item.paramKey.startsWith('collect.')
    )
  } else if (activeTab.value === 'ai') {
    filtered = filtered.filter(item => 
      item.paramKey.startsWith('ai.')
    )
  } else if (activeTab.value === 'notification') {
    filtered = filtered.filter(item => 
      item.paramKey.startsWith('notification.')
    )
  }
  
  if (searchForm.paramName) {
    filtered = filtered.filter(item => 
      item.paramName.includes(searchForm.paramName)
    )
  }
  if (searchForm.paramKey) {
    filtered = filtered.filter(item => 
      item.paramKey.includes(searchForm.paramKey)
    )
  }
  
  total.value = filtered.length
  
  const start = (pageNo.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

const dialogTitle = computed(() => {
  return formData.id ? '编辑参数' : '新增参数'
})

// ==================== 辅助函数 ====================
const getParamTypeLabel = (paramType: string) => {
  const map: Record<string, string> = {
    STRING: '字符串',
    NUMBER: '数字',
    BOOLEAN: '布尔值',
    JSON: 'JSON'
  }
  return map[paramType] || paramType
}

const getParamTypeTagType = (paramType: string) => {
  const map: Record<string, any> = {
    STRING: '',
    NUMBER: 'success',
    BOOLEAN: 'warning',
    JSON: 'info'
  }
  return map[paramType] || ''
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  ElMessage.success('查询成功')
}

const handleReset = () => {
  searchForm.paramName = ''
  searchForm.paramKey = ''
  pageNo.value = 1
  ElMessage.info('已重置搜索条件')
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    paramName: '',
    paramKey: '',
    paramValue: '',
    paramType: 'STRING',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: SystemParam) => {
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        submitLoading.value = false
        dialogVisible.value = false
        ElMessage.success(formData.id ? '编辑成功' : '新增成功')
      }, 500)
    }
  })
}

const handleDelete = (row: SystemParam) => {
  ElMessageBox.confirm(
    `确定要删除参数「${row.paramName}」吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleSelectionChange = (selection: SystemParam[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 条参数吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('批量删除成功')
    selectedRows.value = []
  }).catch(() => {})
}

const handlePageChange = (page: number) => {
  pageNo.value = page
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNo.value = 1
}

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 300)
})
</script>

<style scoped>
.param-manage {
  padding: 20px;
}
</style>
