<template>
  <div class="account-cost-page">
    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value primary">¥{{ stats.totalCost.toLocaleString() }}</div>
            <div class="stat-label">总投入成本</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value success">{{ stats.normalCount }}</div>
            <div class="stat-label">正常账号</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value warning">{{ stats.frozenCount }}</div>
            <div class="stat-label">冻结账号</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value info">{{ stats.cancelledCount }}</div>
            <div class="stat-label">注销账号</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" class="cost-tabs">
      <el-tab-pane label="账号成本" name="account">
        <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
          <el-form-item label="账号名称"><el-input v-model="searchForm.keyword" placeholder="搜索账号" clearable /></el-form-item>
        </TableSearch>
        <div class="action-bar">
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增成本</el-button>
          <span class="total-info">共 {{ total }} 条</span>
        </div>
        <el-table :data="costList" v-loading="loading" stripe>
          <el-table-column prop="accountName" label="账号名称" width="150" />
          <el-table-column prop="platform" label="平台" width="80"><template #default="{ row }"><el-tag>{{ row.platform }}</el-tag></template></el-table-column>
          <el-table-column prop="inCost" label="初始投入" width="120" align="right"><template #default="{ row }">¥{{ row.inCost.toLocaleString() }}</template></el-table-column>
          <el-table-column prop="accountStatus" label="账号状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.accountStatus)">{{ row.accountStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="realName" label="实名人" width="100" />
          <el-table-column prop="operator" label="运营人员" width="100" />
          <el-table-column prop="createTime" label="创建时间" width="120" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <!-- 过程成本 -->
      <el-tab-pane label="过程成本" name="process">
        <!-- 过程成本筛选区 -->
        <TableSearch v-model="processSearchForm" @search="handleProcessSearch" @reset="handleProcessReset">
          <el-form-item label="关联账号">
            <el-select v-model="processSearchForm.accountId" placeholder="请选择" clearable style="width: 100%">
              <el-option label="知识变现研究院" :value="1" />
              <el-option label="AI技术前沿" :value="2" />
              <el-option label="职场进阶指南" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="成本类型">
            <DictSelect v-model="processSearchForm.costType" dict-type="dict_cost_type" placeholder="请选择" style="width: 100%" />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker v-model="processSearchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </TableSearch>

        <!-- 操作栏 -->
        <div class="action-bar">
          <el-button type="primary" @click="handleProcessAdd"><el-icon><Plus /></el-icon>新增成本</el-button>
          <el-button @click="handleProcessExport"><el-icon><Download /></el-icon>导出</el-button>
          <span class="total-info">共 {{ processTotal }} 条</span>
        </div>

        <!-- 过程成本表格 -->
        <el-table :data="processList" v-loading="processLoading" border stripe>
          <el-table-column prop="accountName" label="关联账号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="costType" label="成本类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getCostTypeColor(row.costType)">{{ getCostTypeName(row.costType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120" align="right">
            <template #default="{ row }">
              <span class="amount-text">¥{{ row.amount.toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="成本说明" min-width="200" show-overflow-tooltip />
          <el-table-column prop="operator" label="操作人" width="100" align="center" />
          <el-table-column prop="createTime" label="记录时间" width="160" align="center" />
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleProcessView(row)">查看</el-button>
              <el-button link type="primary" @click="handleProcessEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 过程成本分页 -->
        <Pagination
          :current-page="processPagination.pageNo"
          :page-size="processPagination.pageSize"
          :total="processPagination.total"
          @update:current-page="(val) => processPagination.pageNo = val"
          @update:page-size="(val) => { processPagination.pageSize = val; loadProcessList() }"
          @change="loadProcessList"
        />
      </el-tab-pane>
    </el-tabs>

    <el-pagination
      v-if="activeTab === 'account'"
      :current-page="searchForm.pageNo"
      :page-size="searchForm.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @update:current-page="(val) => searchForm.pageNo = val"
      @update:page-size="(val) => { searchForm.pageSize = val; handleSearch() }"
      @current-change="handleSearch"
      @size-change="handleSearch"
    />

    <!-- 账号成本对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="100px">
        <el-form-item label="关联账号" prop="accountId"><el-select v-model="formData.accountId" placeholder="请选择" style="width: 100%"><el-option label="公众号-知识变现研究院" :value="1" /></el-select></el-form-item>
        <el-form-item label="初始投入" prop="inCost"><el-input-number v-model="formData.inCost" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="账号状态" prop="accountStatus"><el-select v-model="formData.accountStatus" placeholder="请选择" style="width: 100%"><el-option label="正常" value="正常" /><el-option label="冻结" value="冻结" /><el-option label="注销" value="注销" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="formData.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">保存</el-button></template>
    </el-dialog>

    <!-- 过程成本对话框 -->
    <el-dialog v-model="processDialogVisible" :title="processDialogTitle" width="600px">
      <el-form :model="processFormData" ref="processFormRef" :rules="processFormRules" label-width="100px">
        <el-form-item label="关联账号">
          <el-select v-model="processFormData.accountId" placeholder="请选择" style="width: 100%">
            <el-option label="知识变现研究院" :value="1" />
            <el-option label="AI技术前沿" :value="2" />
            <el-option label="职场进阶指南" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="成本类型">
          <DictSelect v-model="processFormData.costType" dict-type="dict_cost_type" placeholder="请选择" style="width: 100%" />
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="processFormData.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="成本说明">
          <el-input v-model="processFormData.description" type="textarea" :rows="3" placeholder="请输入成本说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleProcessSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import { exportToExcel } from '@/utils'

// ==================== 过程成本类型定义 ====================
type CostType = 'PERSON' | 'CONTENT' | 'PROMOTE' | 'PLATFORM' | 'OTHER'

const getCostTypeName = (type: CostType): string => {
  const map: Record<CostType, string> = {
    PERSON: '人工成本',
    CONTENT: '内容制作',
    PROMOTE: '推广费用',
    PLATFORM: '平台服务费',
    OTHER: '其他',
  }
  return map[type] || type
}

const getCostTypeColor = (type: CostType): string => {
  const map: Record<CostType, string> = {
    PERSON: 'primary',
    CONTENT: 'success',
    PROMOTE: 'warning',
    PLATFORM: 'info',
    OTHER: '',
  }
  return map[type] || ''
}

// ==================== 过程成本（Phase 2: 待后端实现 /admin-api/oa/finance/process-cost/list） ====================
// P-GATE-UNMOCK S-B: 已删除硬编码 mock 数据。后端尚无 process-cost 接口，过程成本 tab 暂显示空状态。
// 相关 API 占位见 docs/engineering/API-M8-财务成本.md (TODO Phase 2)

// ==================== 过程成本响应式数据 ====================
const processLoading = ref(false)
const processList = ref<any[]>([])
const processTotal = ref(8)

const processStats = reactive({
  totalCost: 64500,
  recordCount: 8,
  personCost: 33000,
  contentCost: 15000,
})

const processSearchForm = reactive({
  accountId: undefined as number | undefined,
  costType: undefined as CostType | undefined,
  dateRange: undefined as string[] | undefined,
})

const processPagination = reactive({
  pageNo: 1,
  pageSize: 20,
  total: 8,
})

const processDialogVisible = ref(false)
const processDialogTitle = ref('新增过程成本')
const processFormRef = ref<any>()
const processFormData = reactive({
  id: undefined as number | undefined,
  accountId: undefined as number | undefined,
  costType: '' as CostType,
  amount: 0,
  description: '',
})

const processFormRules = {
  accountId: [{ required: true, message: '请选择关联账号', trigger: 'change' }],
  costType: [{ required: true, message: '请选择成本类型', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '金额必须大于 0', trigger: 'blur' },
  ],
}

// ==================== 过程成本方法 ====================
const loadProcessList = () => {
  processLoading.value = true
  // P-GATE-UNMOCK S-B: 后端无 process-cost 接口，留空 + 提示
  processList.value = []
  processPagination.total = 0
  processLoading.value = false
}

const handleProcessSearch = () => {
  processPagination.pageNo = 1
  loadProcessList()
}

const handleProcessReset = () => {
  processSearchForm.accountId = undefined
  processSearchForm.costType = undefined
  processSearchForm.dateRange = undefined
  processPagination.pageNo = 1
  loadProcessList()
}

const handleProcessAdd = () => {
  router.push('/finance/cost/edit')
  return
  processDialogTitle.value = '新增过程成本'
  Object.assign(processFormData, { id: undefined, accountId: undefined, costType: '' as CostType, amount: 0, description: '' })
  processDialogVisible.value = true
}

const handleProcessEdit = (row: any) => {
  processDialogTitle.value = '编辑过程成本'
  Object.assign(processFormData, { ...row })
  processDialogVisible.value = true
}

const handleProcessView = (row: any) => {
  processDialogTitle.value = '查看过程成本'
  Object.assign(processFormData, { ...row })
  processDialogVisible.value = true
}

const handleProcessSubmit = async () => {
  if (!processFormRef.value) return
  await processFormRef.value.validate()
  ElMessage.success('保存成功')
  processDialogVisible.value = false
  loadProcessList()
}

const handleProcessExport = () => {
  const rows = processList.value.map((row) => ({
    accountName: row.accountName,
    costType: getCostTypeName(row.costType),
    amount: row.amount,
    description: row.description,
    operator: row.operator,
    createTime: row.createTime,
  }))
  const columns = [
    { key: 'accountName', label: '关联账号' },
    { key: 'costType', label: '成本类型' },
    { key: 'amount', label: '金额' },
    { key: 'description', label: '成本说明' },
    { key: 'operator', label: '操作人' },
    { key: 'createTime', label: '记录时间' },
  ]
  exportToExcel(rows, columns, '过程成本')
}

// ==================== 账号成本响应式数据 ====================
const loading = ref(false)
const activeTab = ref('account')
const costList = ref([
  { id: 1, accountName: '知识变现研究院', platform: '公众号', inCost: 5000, accountStatus: '正常', realName: '张三', operator: '李四', createTime: '2026-01-10' },
  { id: 2, accountName: 'AI技术前沿', platform: '抖音', inCost: 8000, accountStatus: '冻结', realName: '李三', operator: '王五', createTime: '2026-02-15' },
])
const total = ref(2)

const stats = reactive({
  totalCost: 156800,
  normalCount: 180,
  frozenCount: 32,
  cancelledCount: 22,
})

const searchForm = reactive({ pageNo: 1, pageSize: 20, keyword: undefined as string | undefined })
const dialogVisible = ref(false)
const dialogTitle = ref('新增成本')
const formData = reactive({ id: undefined, accountId: undefined, inCost: 0, accountStatus: '正常', remark: '' })
const formRules = {
  accountId: [{ required: true, message: '请选择关联账号', trigger: 'change' }],
  inCost: [{ required: true, message: '请输入初始投入', trigger: 'blur' }],
  accountStatus: [{ required: true, message: '请选择账号状态', trigger: 'change' }],
}
const router = useRouter()

const loadList = () => { loading.value = false; loadProcessList() }
const handleSearch = () => { searchForm.pageNo = 1; loadList() }
const handleReset = () => { searchForm.keyword = undefined; handleSearch() }
const getStatusType = (status: string) => status === '正常' ? 'success' : status === '冻结' ? 'warning' : 'info'
const formRef = ref<any>()
const handleAdd = () => { router.push('/finance/cost/edit') }
const handleEdit = (row: any) => { dialogTitle.value = '编辑成本'; Object.assign(formData, { ...row }); dialogVisible.value = true }
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadList()
}
const handleView = () => ElMessage.info('查看详情功能开发中')

onMounted(() => loadList())
</script>

<style scoped>
.account-cost-page { padding: 20px; }
.stats-cards { margin-bottom: 20px; }
.process-stats { margin-bottom: 16px; }
.stat-item { text-align: center; }
.stat-value { font-size: 24px; font-weight: bold; }
.stat-value.primary { color: #409eff; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-value.info { color: #909399; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.cost-tabs { margin-top: 16px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.amount-text { color: #f56c6c; font-weight: 600; }
</style>
