<template>
  <div class="perf-execution-page">

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="被考核人">
        <el-select v-model="searchForm.evaluateeId" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="张三" :value="1" />
          <el-option label="李四" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="草稿" value="draft" />
          <el-option label="计算中" value="calculating" />
          <el-option label="待确认" value="calculated" />
          <el-option label="已确认" value="confirmed" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建考核
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="recordList" v-loading="loading" stripe>
      <template #empty>
        <el-empty description="暂无考核记录" />
      </template>
      <el-table-column prop="evaluateeName" label="被考核人" width="100" />
      <el-table-column prop="position" label="岗位" width="120" />
      <el-table-column prop="cycleDisplay" label="考核周期" width="140" />
      <el-table-column prop="totalScore" label="总分" width="100" align="center">
        <template #default="{ row }">
          {{ row.totalScore ? `${row.totalScore}分` : '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="evaluatorName" label="考核人" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="140" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button v-if="row.status === 'draft'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'draft'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
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

    <!-- 创建考核对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建考核" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="被考核人" prop="evaluateeId">
          <el-select v-model="createForm.evaluateeId" placeholder="请选择成员" style="width: 100%">
            <el-option label="张三 - 公众号运营" :value="1" />
            <el-option label="李四 - 抖音运营" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="考核模板">
          <el-input v-model="createForm.templateName" placeholder="自动关联" disabled />
          <div class="form-tip">自动关联被考核人岗位的生效模板</div>
        </el-form-item>
        <el-form-item label="周期类型" prop="cycleType">
          <el-radio-group v-model="createForm.cycleType">
            <el-radio value="month">月度</el-radio>
            <el-radio value="week">周度</el-radio>
            <el-radio value="custom">自定义</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.cycleType === 'month'" label="考核月份" prop="month">
          <el-date-picker v-model="createForm.month" type="month" placeholder="选择月份" value-format="YYYY-MM" style="width: 100%" />
        </el-form-item>
        <el-form-item v-else label="日期范围" prop="dateRange">
          <el-date-picker v-model="createForm.dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 考核详情抽屉 -->
    <el-drawer v-model="detailVisible" title="考核详情" size="800px">
      <template v-if="currentDetail">
        <div class="detail-header">
          <div class="header-info">
            <span>被考核人: <strong>{{ currentDetail.evaluateeName }}</strong></span>
            <span>岗位: <strong>{{ currentDetail.position }}</strong></span>
            <span>周期: <strong>{{ currentDetail.cycleDisplay }}</strong></span>
          </div>
          <el-tag :type="getStatusType(currentDetail.status)" size="large">
            {{ getStatusLabel(currentDetail.status) }}
          </el-tag>
        </div>

        <el-card class="score-card" shadow="never">
          <div class="score-display">
            <div class="score-main">
              <div class="score-label">总分</div>
              <div class="score-value">{{ currentDetail.totalScore || '--' }}</div>
              <div class="score-unit">分</div>
            </div>
            <div class="grade-display">
              <div class="grade-label">等级</div>
              <div class="grade-value">{{ currentDetail.grade || '--' }}</div>
            </div>
          </div>
          <div class="score-actions">
            <el-button v-if="currentDetail.status === 'draft' || currentDetail.status === 'calculated'" type="primary" @click="handleRecalculate">
              重新计算
            </el-button>
            <el-button v-if="currentDetail.status === 'calculated'" type="success" @click="handleConfirm">
              确认考核结果
            </el-button>
          </div>
        </el-card>

        <h3 class="section-title">指标明细</h3>
        <el-table :data="currentDetail.items" border stripe>
          <el-table-column prop="metricName" label="指标名称" min-width="140" />
          <el-table-column prop="weight" label="权重" width="80" align="center">
            <template #default="{ row }">{{ row.weight }}%</template>
          </el-table-column>
          <el-table-column prop="actualValue" label="实际值" width="100" align="center" />
          <el-table-column prop="score" label="得分" width="80" align="center" />
          <el-table-column prop="weightedScore" label="加权得分" width="100" align="center" />
          <el-table-column prop="grade" label="等级" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getGradeType(row.grade)" size="small">{{ row.grade }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isManualAdjust" label="调整" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isManualAdjust" type="warning" size="small">是</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import {
  getPerfRecordList,
  createPerfRecord,
  calculatePerfRecord,
  confirmPerfRecord,
} from '@/api/perfRecord'
import type { PerfRecordListItem, PerfRecordDetail, CreatePerfRecordRequest } from '@/types/perfExecution'
import { PerfRecordStatus, CycleType } from '@/types/perfExecution'

const loading = ref(false)
const recordList = ref<PerfRecordListItem[]>([])
const total = ref(0)
const router = useRouter()
const createFormRef = ref<FormInstance>()

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  evaluateeId: undefined as number | undefined,
  status: undefined as string | undefined,
})

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getPerfRecordList({
      targetUserId: searchForm.evaluateeId,
      status: searchForm.status,
      pageNum: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    recordList.value = (res.list || []).map((row: any) => ({
      id: row.id,
      evaluateeName: row.targetUserName || row.evaluateeName || '',
      position: row.position || '',
      cycleDisplay: row.periodDisplay || (row.periodType || '') + ' ' + (row.periodStart || ''),
      totalScore: row.totalScore,
      grade: row.grade,
      status: row.status || 'draft',
      evaluatorName: row.evaluatorName || '',
      createdAt: row.createTime || row.createdAt || '',
    }))
    total.value = res.total ?? 0
  } catch {
    recordList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNo = 1
  loadList()
}

const handleReset = () => {
  searchForm.evaluateeId = undefined
  searchForm.status = undefined
  handleSearch()
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    draft: 'info',
    calculating: '',
    calculated: 'warning',
    confirmed: 'success',
  }
  return map[status] || ''
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    draft: '草稿',
    calculating: '计算中',
    calculated: '待确认',
    confirmed: '已确认',
  }
  return map[status] || status
}

// 创建考核
const createDialogVisible = ref(false)
const createForm = reactive<CreatePerfRecordRequest & { dateRange?: string[]; templateName?: string }>({
  evaluateeId: 0,
  templateId: 0,
  cycleType: CycleType.MONTH,
  month: undefined,
  dateRange: undefined,
  templateName: '公众号运营考核模板（自动关联）',
})

const createRules = reactive<FormRules>({
  evaluateeId: [
    { required: true, message: '请选择被考核人', trigger: 'change', type: 'number', min: 1 }
  ],
  cycleType: [
    { required: true, message: '请选择周期类型', trigger: 'change' }
  ],
  month: [
    { required: true, message: '请选择考核月份', trigger: 'change' }
  ],
  dateRange: [
    { required: true, message: '请选择日期范围', trigger: 'change' }
  ]
})

const handleCreate = () => {
  createDialogVisible.value = true
}

const resolvePeriodRange = (): [string, string] | null => {
  if (createForm.cycleType === CycleType.MONTH && createForm.month) {
    const [year, month] = createForm.month.split('-').map(Number)
    const lastDay = new Date(year, month, 0).getDate()
    return [
      `${createForm.month}-01`,
      `${createForm.month}-${String(lastDay).padStart(2, '0')}`,
    ]
  }
  if (createForm.dateRange && createForm.dateRange.length >= 2) {
    return [createForm.dateRange[0], createForm.dateRange[1]]
  }
  return null
}

const handleConfirmCreate = async () => {
  if (!createFormRef.value) return

  try {
    await createFormRef.value.validate()
    const periodRange = resolvePeriodRange()
    if (!periodRange) {
      ElMessage.warning('请选择考核周期')
      return
    }
    await createPerfRecord({
      targetUserId: createForm.evaluateeId,
      periodType: createForm.cycleType,
      periodStart: periodRange[0],
      periodEnd: periodRange[1],
    })
    ElMessage.success('创建成功，已自动计算指标得分')
    createDialogVisible.value = false
    loadList()
  } catch (error) {
    console.error('表单校验失败', error)
  }
}

// 查看详情
const detailVisible = ref(false)
const currentDetail = ref<PerfRecordDetail | null>(null)

const handleView = (row: PerfRecordListItem) => {
  router.push(`/perf/record/${row.id}`)
}

const handleEdit = (row: PerfRecordListItem) => {
  ElMessage.info('编辑功能开发中')
}

const handleDelete = async (row: PerfRecordListItem) => {
  try {
    await ElMessageBox.confirm('确认删除该考核记录？', '提示', { type: 'warning' })
    // 后端暂未提供 delete 端点（需 P-GATE 增补），提示用户
    ElMessage.warning('删除接口待后端支持')
    loadList()
  } catch {}
}

const handleRecalculate = async () => {
  if (!currentDetail.value) return
  try {
    await calculatePerfRecord(currentDetail.value.id)
    ElMessage.success('重新计算完成')
  } catch {}
}

const handleConfirm = async () => {
  if (!currentDetail.value) return
  try {
    await ElMessageBox.confirm('确认考核结果？确认后不可修改', '提示', { type: 'warning' })
    await confirmPerfRecord(currentDetail.value.id)
    ElMessage.success('考核结果已确认')
    if (currentDetail.value) {
      currentDetail.value.status = PerfRecordStatus.CONFIRMED
    }
    loadList()
  } catch {}
}

const getGradeType = (grade: string) => {
  const map: Record<string, any> = {
    S: 'success',
    A: '',
    B: 'warning',
    C: 'danger',
    D: 'info',
  }
  return map[grade] || ''
}

onMounted(() => loadList())
</script>

<style scoped>
.perf-execution-page {
  padding: 20px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.total-info {
  color: #909399;
  font-size: 14px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-info {
  display: flex;
  gap: 24px;
  font-size: 14px;
}

.score-card {
  margin-bottom: 24px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 40px;
  margin-bottom: 16px;
}

.score-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.score-label {
  font-size: 14px;
  color: #606266;
}

.score-value {
  font-size: 48px;
  font-weight: bold;
  color: #409eff;
}

.score-unit {
  font-size: 16px;
  color: #606266;
}

.grade-display {
  text-align: center;
}

.grade-label {
  font-size: 14px;
  color: #606266;
}

.grade-value {
  font-size: 36px;
  font-weight: bold;
  color: #67c23a;
}

.score-actions {
  display: flex;
  gap: 12px;
}

.section-title {
  margin: 24px 0 12px;
  font-size: 16px;
  font-weight: 600;
}
</style>
