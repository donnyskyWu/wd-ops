<template>
  <div class="dict-manage">
    <ContentWrap title="字典配置" subtitle="数据字典管理">
      <el-alert 
        title="管理系统中的枚举值和字典数据（用于下拉选项、状态标识等）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="字典名称">
            <el-input v-model="searchForm.dictName" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="字典类型">
            <el-input v-model="searchForm.dictType" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
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
          新增字典
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
        <el-table-column prop="dictName" label="字典名称" min-width="180" />
        <el-table-column prop="dictType" label="字典类型" min-width="180" />
        <el-table-column prop="dictLabel" label="字典标签" min-width="150" />
        <el-table-column prop="dictValue" label="字典值" min-width="150" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
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
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="formData.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="formData.dictType" placeholder="请输入字典类型（唯一标识）" />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="formData.dictLabel" placeholder="请输入显示标签" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="formData.dictValue" placeholder="请输入存储值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sort" :min="0" :max="999" style="width: 100%" />
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

// ==================== 类型定义 ====================
interface DictItem {
  id: number
  dictName: string
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  status: 'ENABLED' | 'DISABLED'
  remark?: string
}

// ==================== 响应式数据 ====================
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  dictName: '',
  dictType: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref<DictItem[]>([])

const formData = reactive<Partial<DictItem>>({
  dictName: '',
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  status: 'ENABLED',
  remark: ''
})

const rules: FormRules = {
  dictName: [
    { required: true, message: '请输入字典名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  dictLabel: [
    { required: true, message: '请输入字典标签', trigger: 'blur' }
  ],
  dictValue: [
    { required: true, message: '请输入字典值', trigger: 'blur' }
  ]
}

// ==================== Mock 数据 ====================
const mockData: DictItem[] = [
  {
    id: 1,
    dictName: '平台类型',
    dictType: 'PLATFORM_TYPE',
    dictLabel: '抖音',
    dictValue: 'DOUYIN',
    sort: 1,
    status: 'ENABLED',
    remark: '短视频平台-抖音'
  },
  {
    id: 2,
    dictName: '平台类型',
    dictType: 'PLATFORM_TYPE',
    dictLabel: '快手',
    dictValue: 'KUAISHOU',
    sort: 2,
    status: 'ENABLED',
    remark: '短视频平台-快手'
  },
  {
    id: 3,
    dictName: '平台类型',
    dictType: 'PLATFORM_TYPE',
    dictLabel: '小红书',
    dictValue: 'XIAOHONGSHU',
    sort: 3,
    status: 'ENABLED',
    remark: '社交平台-小红书'
  },
  {
    id: 4,
    dictName: '预警级别',
    dictType: 'ALERT_LEVEL',
    dictLabel: '严重',
    dictValue: 'CRITICAL',
    sort: 1,
    status: 'ENABLED',
    remark: '最高级别预警'
  },
  {
    id: 5,
    dictName: '预警级别',
    dictType: 'ALERT_LEVEL',
    dictLabel: '警告',
    dictValue: 'WARNING',
    sort: 2,
    status: 'ENABLED',
    remark: '中级预警'
  },
  {
    id: 6,
    dictName: '预警级别',
    dictType: 'ALERT_LEVEL',
    dictLabel: '提示',
    dictValue: 'INFO',
    sort: 3,
    status: 'ENABLED',
    remark: '低级提示'
  },
  {
    id: 7,
    dictName: '同步频率',
    dictType: 'SYNC_FREQUENCY',
    dictLabel: '每小时',
    dictValue: 'HOURLY',
    sort: 1,
    status: 'ENABLED',
    remark: '高频同步'
  },
  {
    id: 8,
    dictName: '同步频率',
    dictType: 'SYNC_FREQUENCY',
    dictLabel: '每日',
    dictValue: 'DAILY',
    sort: 2,
    status: 'ENABLED',
    remark: '日常同步'
  },
  {
    id: 9,
    dictName: '同步频率',
    dictType: 'SYNC_FREQUENCY',
    dictLabel: '每周',
    dictValue: 'WEEKLY',
    sort: 3,
    status: 'ENABLED',
    remark: '低频同步'
  },
  {
    id: 10,
    dictName: 'AI模型类型',
    dictType: 'AI_MODEL_TYPE',
    dictLabel: '通义千问',
    dictValue: 'QWEN',
    sort: 1,
    status: 'ENABLED',
    remark: '阿里云大模型'
  }
]

// ==================== 计算属性 ====================
const displayList = computed(() => {
  let filtered = [...mockData]
  
  if (searchForm.dictName) {
    filtered = filtered.filter(item => 
      item.dictName.includes(searchForm.dictName)
    )
  }
  if (searchForm.dictType) {
    filtered = filtered.filter(item => 
      item.dictType.includes(searchForm.dictType)
    )
  }
  if (searchForm.status) {
    filtered = filtered.filter(item => item.status === searchForm.status)
  }
  
  total.value = filtered.length
  
  const start = (pageNo.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

const dialogTitle = computed(() => {
  return formData.id ? '编辑字典' : '新增字典'
})

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  ElMessage.success('查询成功')
}

const handleReset = () => {
  searchForm.dictName = ''
  searchForm.dictType = ''
  searchForm.status = ''
  pageNo.value = 1
  ElMessage.info('已重置搜索条件')
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    dictName: '',
    dictType: '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
    status: 'ENABLED',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: DictItem) => {
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

const handleDelete = (row: DictItem) => {
  ElMessageBox.confirm(
    `确定要删除字典「${row.dictLabel}」吗？`,
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

const handleSelectionChange = (selection: DictItem[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 条字典吗？`,
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
.dict-manage {
  padding: 20px;
}
</style>
