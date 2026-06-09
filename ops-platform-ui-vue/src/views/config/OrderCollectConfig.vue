<template>
  <div class="order-collect-config">
    <ContentWrap title="订单采集配置" subtitle="订单数据自动采集配置">
      <el-alert 
        title="配置订单数据自动采集规则和频率（支持抖音小店、快手小店、淘宝等电商平台）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="店铺名称">
            <el-input v-model="searchForm.shopName" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="平台类型">
            <DictSelect v-model="searchForm.platformType" dict-type="dict_ecom_platform" placeholder="请选择" style="width: 140px" />
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
        <el-table-column prop="shopName" label="店铺名称" min-width="150" />
        <el-table-column prop="platformType" label="平台类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getPlatformTagType(row.platformType)">
              {{ getPlatformLabel(row.platformType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="collectFrequency" label="采集频率" width="100" align="center">
          <template #default="{ row }">
            {{ getFrequencyLabel(row.collectFrequency) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastCollectTime" label="最后采集时间" width="180" />
        <el-table-column prop="todayOrderCount" label="今日订单数" width="110" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleSync(row)">立即同步</el-button>
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
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="formData.shopName" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="平台类型" prop="platformType">
          <DictSelect v-model="formData.platformType" dict-type="dict_ecom_platform" placeholder="请选择平台类型" style="width: 100%" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input 
            v-model="formData.apiKey" 
            type="password"
            show-password
            placeholder="请输入平台API Key" 
          />
        </el-form-item>
        <el-form-item label="采集频率" prop="collectFrequency">
          <DictSelect v-model="formData.collectFrequency" dict-type="dict_sync_frequency" placeholder="请选择采集频率" style="width: 100%" />
        </el-form-item>
        <el-form-item label="采集字段">
          <el-checkbox-group v-model="formData.collectFields">
            <el-checkbox label="ORDER_BASIC">订单基本信息</el-checkbox>
            <el-checkbox label="ORDER_ITEMS">订单商品明细</el-checkbox>
            <el-checkbox label="ORDER_PAYMENT">支付信息</el-checkbox>
            <el-checkbox label="ORDER_LOGISTICS">物流信息</el-checkbox>
            <el-checkbox label="ORDER_REFUND">退款信息</el-checkbox>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import { orderCollectApi, type CollectConfigVO } from '@/api/config'

interface OrderCollectConfig {
  id: number
  shopName: string
  platformType: string
  apiKey?: string
  collectFrequency: string
  collectFields: string[]
  lastCollectTime: string
  todayOrderCount: number
  status: 'ENABLED' | 'DISABLED'
  remark?: string
}

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const searchForm = reactive({
  shopName: '',
  platformType: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const configList = ref<OrderCollectConfig[]>([])
const selectedRows = ref<OrderCollectConfig[]>([])

const formData = reactive<Partial<OrderCollectConfig>>({
  shopName: '',
  platformType: '',
  apiKey: '',
  collectFrequency: 'HOURLY',
  collectFields: ['ORDER_BASIC', 'ORDER_ITEMS'],
  status: 'ENABLED',
  remark: ''
})

const rules: FormRules = {
  shopName: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度1-50个字符', trigger: 'blur' }
  ],
  platformType: [
    { required: true, message: '请选择平台类型', trigger: 'change' }
  ],
  collectFrequency: [
    { required: true, message: '请选择采集频率', trigger: 'change' }
  ]
}

function parseCollectFields(value?: string): string[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return value.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

function mapRow(row: CollectConfigVO): OrderCollectConfig {
  return {
    id: row.id,
    shopName: row.configName,
    platformType: row.platformType || '',
    collectFrequency: row.collectFrequency || '',
    collectFields: parseCollectFields(row.collectFields),
    lastCollectTime: row.createTime || '-',
    todayOrderCount: 0,
    status: (row.status as 'ENABLED' | 'DISABLED') || 'ENABLED',
    remark: row.remark,
  }
}

const displayList = computed(() => configList.value)

const dialogTitle = computed(() => (formData.id ? '编辑配置' : '新增配置'))

const loadList = async () => {
  loading.value = true
  try {
    const res = await orderCollectApi.list({
      configName: searchForm.shopName || undefined,
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
    DOUYIN_SHOP: '抖音小店',
    KUAISHOU_SHOP: '快手小店',
    TAOBAO: '淘宝',
    JD: '京东',
    PDD: '拼多多'
  }
  return map[platformType] || platformType
}

const getPlatformTagType = (platformType: string) => {
  const map: Record<string, any> = {
    DOUYIN_SHOP: '',
    KUAISHOU_SHOP: 'success',
    TAOBAO: 'warning',
    JD: 'danger',
    PDD: 'info'
  }
  return map[platformType] || ''
}

const getFrequencyLabel = (frequency: string) => {
  const map: Record<string, string> = {
    HOURLY: '每小时',
    EVERY_4H: '每4小时',
    DAILY: '每日'
  }
  return map[frequency] || frequency
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.shopName = ''
  searchForm.platformType = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined,
    shopName: '',
    platformType: '',
    apiKey: '',
    collectFrequency: 'HOURLY',
    collectFields: ['ORDER_BASIC', 'ORDER_ITEMS'],
    status: 'ENABLED',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: OrderCollectConfig) => {
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
      configName: formData.shopName,
      platformType: formData.platformType,
      collectFrequency: formData.collectFrequency,
      collectFields: JSON.stringify(formData.collectFields || []),
      status: formData.status,
      remark: formData.remark,
    }
    if (formData.apiKey) {
      payload.apiKey = formData.apiKey
    }
    if (formData.id) {
      payload.id = formData.id
      await orderCollectApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await orderCollectApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleSync = (row: OrderCollectConfig) => {
  ElMessage.success(`开始同步 ${row.shopName} 的订单数据...`)
}

const handleToggleStatus = async (row: OrderCollectConfig) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}店铺「${row.shopName}」吗？`,
      '提示',
      { type: 'warning' }
    )
    await orderCollectApi.update({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: OrderCollectConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除店铺「${row.shopName}」的配置吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await orderCollectApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: OrderCollectConfig[]) => {
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
      await orderCollectApi.delete(row.id)
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
.order-collect-config {
  padding: 20px;
}
</style>
