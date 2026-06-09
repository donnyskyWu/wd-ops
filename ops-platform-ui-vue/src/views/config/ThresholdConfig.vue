<template>
  <div class="threshold-config">
    <ContentWrap title="阈值规则配置" subtitle="预警阈值规则管理">
      <el-alert 
        title="配置各项指标的预警阈值规则（支持播放量、点赞数、转化率等多维度指标）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" style="margin-bottom: 16px">
        <el-tab-pane label="内容指标阈值" name="content" />
        <el-tab-pane label="账号指标阈值" name="account" />
        <el-tab-pane label="订单指标阈值" name="order" />
      </el-tabs>
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="指标名称">
            <el-input v-model="searchForm.metricName" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="预警级别">
            <DictSelect v-model="searchForm.alertLevel" dict-type="dict_alert_level" placeholder="请选择" style="width: 120px" />
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
          新增规则
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
        <el-table-column prop="metricName" label="指标名称" min-width="150" />
        <el-table-column prop="metricType" label="指标类型" width="120" align="center">
          <template #default="{ row }">
            {{ getMetricTypeLabel(row.metricType) }}
          </template>
        </el-table-column>
        <el-table-column prop="thresholdValue" label="阈值" width="100" align="center" />
        <el-table-column prop="compareOperator" label="比较符" width="80" align="center">
          <template #default="{ row }">
            {{ row.compareOperator }}
          </template>
        </el-table-column>
        <el-table-column prop="alertLevel" label="预警级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getAlertLevelTagType(row.alertLevel)">
              {{ getAlertLevelLabel(row.alertLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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
        <el-form-item label="指标名称" prop="metricName">
          <el-input v-model="formData.metricName" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="指标类型" prop="metricType">
          <DictSelect v-model="formData.metricType" dict-type="dict_threshold_metric" placeholder="请选择指标类型" style="width: 100%" />
        </el-form-item>
        <el-form-item label="比较符" prop="compareOperator">
          <el-select v-model="formData.compareOperator" placeholder="请选择比较符" style="width: 100%">
            <el-option label="大于" value=">" />
            <el-option label="小于" value="<" />
            <el-option label="等于" value="=" />
            <el-option label="大于等于" value=">=" />
            <el-option label="小于等于" value="<=" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="thresholdValue">
          <el-input-number v-model="formData.thresholdValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警级别" prop="alertLevel">
          <el-radio-group v-model="formData.alertLevel">
            <el-radio label="CRITICAL">
              <el-tag type="danger">严重</el-tag>
            </el-radio>
            <el-radio label="WARNING">
              <el-tag type="warning">警告</el-tag>
            </el-radio>
            <el-radio label="INFO">
              <el-tag type="info">提示</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="通知方式">
          <el-checkbox-group v-model="formData.notifyMethods">
            <el-checkbox label="EMAIL">邮件</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="WECHAT">企业微信</el-checkbox>
            <el-checkbox label="DINGTALK">钉钉</el-checkbox>
          </el-checkbox-group>
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  fetchThresholdList,
  createThreshold,
  updateThreshold,
  deleteThreshold,
  type ThresholdConfigVO,
} from '@/api/config'

interface ThresholdRule {
  id: number
  metricName: string
  metricType: string
  thresholdValue: number
  compareOperator: string
  alertLevel: string
  notifyMethods: string[]
  status: 'ENABLED' | 'DISABLED'
  remark?: string
}

const TAB_METRIC_TYPES: Record<string, string[]> = {
  content: ['PLAY_COUNT', 'LIKE_COUNT', 'COMMENT_COUNT', 'SHARE_COUNT'],
  account: ['FANS_GROWTH'],
  order: ['CONVERSION_RATE', 'GMV'],
}

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const activeTab = ref('content')

const searchForm = reactive({
  metricName: '',
  alertLevel: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const ruleList = ref<ThresholdRule[]>([])
const selectedRows = ref<ThresholdRule[]>([])

const formData = reactive<Partial<ThresholdRule>>({
  metricName: '',
  metricType: '',
  thresholdValue: 0,
  compareOperator: '>',
  alertLevel: 'WARNING',
  notifyMethods: ['EMAIL'],
  status: 'ENABLED',
  remark: ''
})

const rules: FormRules = {
  metricName: [
    { required: true, message: '请输入指标名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  metricType: [
    { required: true, message: '请选择指标类型', trigger: 'change' }
  ],
  thresholdValue: [
    { required: true, message: '请输入阈值', trigger: 'blur' }
  ],
  compareOperator: [
    { required: true, message: '请选择比较符', trigger: 'change' }
  ],
  alertLevel: [
    { required: true, message: '请选择预警级别', trigger: 'change' }
  ]
}

function parseNotifyMethods(value?: string): string[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return value.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

function mapRow(row: ThresholdConfigVO): ThresholdRule {
  return {
    id: row.id,
    metricName: row.metricName,
    metricType: row.metricType,
    thresholdValue: row.thresholdValue,
    compareOperator: row.compareOperator || '>',
    alertLevel: row.alertLevel || 'WARNING',
    notifyMethods: parseNotifyMethods(row.notifyMethods),
    status: (row.status as 'ENABLED' | 'DISABLED') || 'ENABLED',
    remark: row.remark,
  }
}

const displayList = computed(() => {
  const tabTypes = TAB_METRIC_TYPES[activeTab.value] || []
  return ruleList.value.filter((item) => {
    if (tabTypes.length && !tabTypes.includes(item.metricType)) return false
    if (searchForm.alertLevel && item.alertLevel !== searchForm.alertLevel) return false
    return true
  })
})

const dialogTitle = computed(() => (formData.id ? '编辑规则' : '新增规则'))

const loadList = async () => {
  loading.value = true
  try {
    const res = await fetchThresholdList({
      metricName: searchForm.metricName || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    ruleList.value = (res.list || []).map(mapRow)
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  pageNo.value = 1
})

// ==================== 辅助函数 ====================
const getMetricTypeLabel = (metricType: string) => {
  const map: Record<string, string> = {
    PLAY_COUNT: '播放量',
    LIKE_COUNT: '点赞数',
    COMMENT_COUNT: '评论数',
    SHARE_COUNT: '转发数',
    FANS_GROWTH: '粉丝增长',
    CONVERSION_RATE: '转化率',
    GMV: 'GMV'
  }
  return map[metricType] || metricType
}

const getAlertLevelLabel = (alertLevel: string) => {
  const map: Record<string, string> = {
    CRITICAL: '严重',
    WARNING: '警告',
    INFO: '提示'
  }
  return map[alertLevel] || alertLevel
}

const getAlertLevelTagType = (alertLevel: string) => {
  const map: Record<string, any> = {
    CRITICAL: 'danger',
    WARNING: 'warning',
    INFO: 'info'
  }
  return map[alertLevel] || ''
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.metricName = ''
  searchForm.alertLevel = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    metricName: '',
    metricType: '',
    thresholdValue: 0,
    compareOperator: '>',
    alertLevel: 'WARNING',
    notifyMethods: ['EMAIL'],
    status: 'ENABLED',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: ThresholdRule) => {
  Object.assign(formData, {
    ...row,
    notifyMethods: [...(row.notifyMethods || [])],
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      metricName: formData.metricName!,
      metricType: formData.metricType!,
      compareOperator: formData.compareOperator,
      thresholdValue: formData.thresholdValue!,
      alertLevel: formData.alertLevel,
      notifyMethods: JSON.stringify(formData.notifyMethods || []),
      status: formData.status,
      remark: formData.remark,
    }
    if (formData.id) {
      await updateThreshold({ id: formData.id, ...payload })
      ElMessage.success('编辑成功')
    } else {
      await createThreshold(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async (row: ThresholdRule) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}规则「${row.metricName}」吗？`,
      '提示',
      { type: 'warning' }
    )
    await updateThreshold({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: ThresholdRule) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除规则「${row.metricName}」吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteThreshold(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: ThresholdRule[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条规则吗？`,
      '批量删除确认',
      { type: 'warning' }
    )
    for (const row of selectedRows.value) {
      await deleteThreshold(row.id)
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
.threshold-config {
  padding: 20px;
}
</style>
