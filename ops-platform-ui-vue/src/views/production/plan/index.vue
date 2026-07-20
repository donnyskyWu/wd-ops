<template>
  <div class="plan-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="计划名称">
        <el-input v-model="searchForm.planName" placeholder="搜索计划" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_plan_status" placeholder="全部" clearable />
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增计划
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="planList" v-loading="loading" stripe border>
      <el-table-column prop="planName" label="计划名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="templateName" label="SOP 模板" width="140" show-overflow-tooltip />
      <el-table-column prop="ipGroupName" label="IP 组" width="120" show-overflow-tooltip />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="status" label="状态" width="110" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_plan_status" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="progress" label="进度" width="140">
        <template #default="{ row }">
          <el-progress :percentage="row.progress ?? 0" :stroke-width="8" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">详情</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="handleStart(row)">启动</el-button>
          <el-button v-if="row.status === 'IN_PROGRESS'" link type="warning" @click="handleTerminate(row)">申请终止</el-button>
          <el-button v-if="row.status === 'TERMINATE_PENDING'" link type="success" @click="handleApproveTerminate(row)">批准终止</el-button>
          <el-button v-if="row.status === 'TERMINATE_PENDING'" link type="warning" @click="handleRejectTerminate(row)">驳回终止</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1200px" destroy-on-close @close="resetForm">
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划名称" prop="planName">
              <el-input v-model="formData.planName" maxlength="100" show-word-limit placeholder="请输入计划名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日期范围" prop="dateRange">
              <el-date-picker
                v-model="formData.dateRange"
                type="daterange"
                range-separator="~"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                @change="handleDateRangeChange"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SOP 模板" prop="templateId">
              <el-select
                v-model="formData.templateId"
                placeholder="请选择 SOP 模板"
                filterable
                style="width: 100%"
                :disabled="!!editingPlanId"
                @change="handleTemplateChange"
              >
                <el-option
                  v-for="item in templateOptions"
                  :key="item.id"
                  :label="item.templateName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="IP 组" prop="ipGroupId">
              <IpGroupTreeSelect
                v-model="formData.ipGroupId"
                placeholder="请选择 IP 组"
                :disabled="!!editingPlanId"
                @change="handleIpGroupChange"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="计划描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>

        <!-- 新增：按日选赛事矩阵 -->
        <template v-if="!editingPlanId">
          <el-divider content-position="left">每日赛事安排</el-divider>
          <p v-if="!planDays.length" class="section-tip">请先选择日期范围</p>
          <div v-else class="day-matrix-wrap">
            <div class="day-matrix">
              <div v-for="day in planDays" :key="day" class="day-column">
                <div class="day-header">{{ day }}</div>
                <div class="day-matches">
                  <el-tag
                    v-for="item in dayMatches[day] || []"
                    :key="item.scheduleId"
                    closable
                    size="small"
                    class="day-match-tag"
                    @close="removeDayMatch(day, item.scheduleId)"
                  >
                    {{ item.displayName }}
                  </el-tag>
                </div>
                <el-button type="primary" link size="small" @click="openMatchDialog(day)">+ 选择赛事</el-button>
              </div>
            </div>
          </div>

          <el-divider content-position="left">任务预览（赛事 × 流程节点）</el-divider>
          <p v-if="!taskPreviewRows.length" class="section-tip">
            选择 SOP 模板、IP 组并为至少一天安排赛事后，将自动生成任务预览
          </p>
          <el-table v-else :data="taskPreviewRows" border size="small" max-height="360">
            <el-table-column prop="planDate" label="日期" width="110" />
            <el-table-column prop="competitionName" label="赛事" min-width="200" show-overflow-tooltip />
            <el-table-column prop="nodeName" label="步骤" width="120" show-overflow-tooltip />
            <el-table-column label="执行岗位" width="110">
              <template #default="{ row }">
                <DictLabel dict-type="dict_position" :value="row.executorRole" :fallback="row.executorRole" />
              </template>
            </el-table-column>
            <el-table-column label="执行人" min-width="160">
              <template #default="{ row }">
                <UserSelect
                  v-model="row.assigneeId"
                  :ip-group-id="formData.ipGroupId"
                  :disabled="!formData.ipGroupId"
                  placeholder="选择执行人"
                  @change="() => { row.assigneeFallback = false }"
                />
                <el-tag v-if="row.assigneeFallback" type="warning" size="small" class="fallback-tag">组长默认</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开始时间" width="190">
              <template #default="{ row }">
                <el-date-picker
                  v-model="row.scheduledStart"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column label="结束时间" width="190">
              <template #default="{ row }">
                <el-date-picker
                  v-model="row.scheduledEnd"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                  size="small"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 编辑草稿：沿用步骤分配 -->
        <template v-else>
          <el-form-item label="关联赛事" prop="selectedCompetitions">
            <div class="competition-picker">
              <el-tag
                v-for="item in formData.selectedCompetitions"
                :key="item.competitionId"
                closable
                class="competition-tag"
                @close="removeCompetition(item.competitionId)"
              >
                {{ item.competitionName }}
              </el-tag>
              <el-button type="primary" link @click="matchDialogVisible = true">+ 选择赛事</el-button>
            </div>
          </el-form-item>
          <el-divider content-position="left">SOP 步骤分配</el-divider>
          <el-table :data="formData.steps" border size="small" empty-text="请先选择 SOP 模板">
            <el-table-column prop="nodeOrder" label="#" width="50" align="center" />
            <el-table-column prop="nodeName" label="步骤名称" min-width="120" />
            <el-table-column label="赛事" min-width="260">
              <template #default="{ row }">
                <el-select
                  v-model="row.competitionIds"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="选择赛事（可多选）"
                  style="width: 100%"
                  :disabled="!formData.selectedCompetitions.length"
                >
                  <el-option
                    v-for="item in formData.selectedCompetitions"
                    :key="item.competitionId"
                    :label="item.competitionName"
                    :value="item.competitionId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="执行岗位" width="110">
              <template #default="{ row }">
                <DictLabel dict-type="dict_position" :value="row.executorRole" :fallback="row.executorRole" />
              </template>
            </el-table-column>
            <el-table-column label="执行人" min-width="180">
              <template #default="{ row }">
                <UserSelect
                  v-model="row.assigneeId"
                  :ip-group-id="formData.ipGroupId"
                  :disabled="!formData.ipGroupId"
                  placeholder="选择 IP 组成员"
                />
              </template>
            </el-table-column>
            <el-table-column label="开始时间" width="190">
              <template #default="{ row }">
                <el-date-picker
                  v-model="row.scheduledStart"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="默认计划开始"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="结束时间" width="190">
              <template #default="{ row }">
                <el-date-picker
                  v-model="row.scheduledEnd"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="默认计划结束"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存草稿</el-button>
      </template>
    </el-dialog>

    <MatchSelectDialog
      v-model:visible="matchDialogVisible"
      multiple
      :initial-date="matchDialogDate"
      :exclude-ids="allSelectedMatchIds"
      @confirm="handleMatchConfirm"
    />

    <el-drawer
      direction="rtl"
      append-to-body
      v-model="detailDrawerVisible"
      :title="detailDrawerTitle"
      size="80%"
      destroy-on-close
      class="plan-detail-drawer"
    >
      <PlanDetailPanel v-if="viewingPlanId" :plan-id="viewingPlanId" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PlanDetailPanel from './detail.vue'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import MatchSelectDialog from '@/components/selectors/MatchSelectDialog.vue'
import { getSopTemplateList, getSopNodeList } from '@/api/sop'
import { toCompetitionId, toCompetitionName, type MatchVO } from '@/api/match'
import { getIpGroupDetail, getIpGroupMembers } from '@/api/ip-group'
import {
  buildIpGroupContext,
  buildStepsFromTasks,
  collectAllMatches,
  defaultPlanDateRange,
  enumeratePlanDays,
  generatePlanTasks,
  type PlanTaskPreviewRow,
} from '@/utils/planTaskGeneration'
import type { SopNodeVO } from '@/types/sop'
import {
  getContentPlanPage,
  getContentPlan,
  createContentPlan,
  updateContentPlan,
  startContentPlan,
  submitTerminatePlan,
  approveTerminatePlan,
  rejectTerminatePlan,
  deleteContentPlan,
  type ContentPlanCompetitionVO,
  type ContentPlanStepVO,
  type ContentPlanVO,
} from '@/api/plan'

interface StepFormRow {
  nodeId: number
  nodeName: string
  nodeOrder: number
  executorRole: string
  competitionIds: string[]
  assigneeId?: number
  scheduledStart?: string
  scheduledEnd?: string
}

const loading = ref(false)
const detailDrawerVisible = ref(false)
const viewingPlanId = ref<number | null>(null)
const detailDrawerTitle = ref('计划详情')
const submitting = ref(false)
const planList = ref<ContentPlanVO[]>([])
const total = ref(0)
const templateOptions = ref<Array<{ id: number; templateName: string }>>([])
const matchDialogVisible = ref(false)
const matchDialogDate = ref<string>()
const matchDialogDay = ref<string>()
const sopNodes = ref<SopNodeVO[]>([])
const dayMatches = reactive<Record<string, MatchVO[]>>({})
const taskPreviewRows = ref<PlanTaskPreviewRow[]>([])
const ipGroupLeaderId = ref<number | null>(null)
const ipGroupMembers = ref<Awaited<ReturnType<typeof getIpGroupMembers>>>([])
const searchForm = reactive({ pageNo: 1, pageSize: 20, planName: undefined as string | undefined, status: undefined as string | undefined })
const dialogVisible = ref(false)
const dialogTitle = ref('新增计划')
const editingPlanId = ref<number | null>(null)
const formRef = ref<any>()
const formData = reactive({
  planName: '',
  dateRange: [] as string[],
  templateId: undefined as number | undefined,
  ipGroupId: undefined as number | undefined,
  selectedCompetitions: [] as ContentPlanCompetitionVO[],
  description: '',
  steps: [] as StepFormRow[],
})

const validateCompetitions = (_rule: unknown, _value: ContentPlanCompetitionVO[], callback: (err?: Error) => void) => {
  if (editingPlanId.value && !formData.selectedCompetitions?.length) {
    callback(new Error('请至少选择一个赛事'))
    return
  }
  callback()
}

const planDays = computed(() => {
  if (formData.dateRange?.length !== 2) return []
  return enumeratePlanDays(formData.dateRange[0], formData.dateRange[1])
})

const allSelectedMatchIds = computed(() => collectAllMatches(dayMatches).map((m) => m.scheduleId))

const formRules = {
  planName: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { max: 100, message: '计划名称不超过 100 字', trigger: 'blur' },
  ],
  dateRange: [{ required: true, message: '请选择日期范围', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择 SOP 模板', trigger: 'change' }],
  ipGroupId: [{ required: true, message: '请选择 IP 组', trigger: 'change' }],
  selectedCompetitions: [{ validator: validateCompetitions, trigger: 'change' }],
}

const clearDayMatches = () => {
  Object.keys(dayMatches).forEach((k) => delete dayMatches[k])
  taskPreviewRows.value = []
}

const syncDayMatchKeys = () => {
  const days = new Set(planDays.value)
  Object.keys(dayMatches).forEach((day) => {
    if (!days.has(day)) delete dayMatches[day]
  })
  planDays.value.forEach((day) => {
    if (!dayMatches[day]) dayMatches[day] = []
  })
}

const loadIpGroupContext = async (ipGroupId?: number) => {
  if (!ipGroupId) {
    ipGroupLeaderId.value = null
    ipGroupMembers.value = []
    return
  }
  const [detail, members] = await Promise.all([
    getIpGroupDetail(ipGroupId),
    getIpGroupMembers(ipGroupId),
  ])
  ipGroupLeaderId.value = detail.leaderId ?? null
  ipGroupMembers.value = members
}

const regenerateTaskPreview = () => {
  if (!sopNodes.value.length || !formData.ipGroupId) {
    taskPreviewRows.value = []
    return
  }
  const ctx = buildIpGroupContext(ipGroupMembers.value, ipGroupLeaderId.value)
  taskPreviewRows.value = generatePlanTasks(
    sopNodes.value,
    dayMatches,
    ctx,
    taskPreviewRows.value,
  )
}

watch(
  () => [formData.templateId, formData.ipGroupId, dayMatches, sopNodes.value.length] as const,
  () => regenerateTaskPreview(),
  { deep: true },
)

const loadList = async () => {
  loading.value = true
  try {
    const res = await getContentPlanPage({
      planName: searchForm.planName,
      status: searchForm.status,
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    planList.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  const templates = await getSopTemplateList({ pageNum: 1, pageSize: 200, status: 1 })
  templateOptions.value = templates.list
}

const handleSearch = () => { searchForm.pageNo = 1; loadList() }
const handleReset = () => { searchForm.planName = undefined; searchForm.status = undefined; handleSearch() }

const resetForm = () => {
  editingPlanId.value = null
  sopNodes.value = []
  clearDayMatches()
  matchDialogDay.value = undefined
  Object.assign(formData, {
    planName: '',
    dateRange: [],
    templateId: undefined,
    ipGroupId: undefined,
    selectedCompetitions: [],
    description: '',
    steps: [],
  })
}

const openMatchDialog = (day: string) => {
  matchDialogDay.value = day
  matchDialogDate.value = day
  matchDialogVisible.value = true
}

const removeDayMatch = (day: string, scheduleId: string) => {
  dayMatches[day] = (dayMatches[day] || []).filter((m) => m.scheduleId !== scheduleId)
  regenerateTaskPreview()
}

const handleMatchConfirm = (matches: MatchVO[]) => {
  if (editingPlanId.value) {
    const existingIds = new Set(formData.selectedCompetitions.map((item) => item.competitionId))
    matches.forEach((match) => {
      const competitionId = toCompetitionId(match)
      if (existingIds.has(competitionId)) return
      formData.selectedCompetitions.push({
        competitionId,
        competitionName: toCompetitionName(match),
      })
      existingIds.add(competitionId)
    })
    if (formData.selectedCompetitions.length === 1) {
      const onlyId = formData.selectedCompetitions[0].competitionId
      formData.steps.forEach((step) => {
        if (!step.competitionIds?.length) step.competitionIds = [onlyId]
      })
    }
    formRef.value?.validateField('selectedCompetitions')
    return
  }
  const day = matchDialogDay.value
  if (!day) return
  if (!dayMatches[day]) dayMatches[day] = []
  const existing = new Set(dayMatches[day].map((m) => m.scheduleId))
  matches.forEach((match) => {
    if (!existing.has(match.scheduleId)) {
      dayMatches[day].push(match)
      existing.add(match.scheduleId)
    }
  })
  regenerateTaskPreview()
}

const removeCompetition = (competitionId: string) => {
  formData.selectedCompetitions = formData.selectedCompetitions.filter((item) => item.competitionId !== competitionId)
  formData.steps.forEach((step) => {
    step.competitionIds = (step.competitionIds || []).filter((id) => id !== competitionId)
  })
  formRef.value?.validateField('selectedCompetitions')
}

const handleIpGroupChange = async (ipGroupId?: number) => {
  await loadIpGroupContext(ipGroupId ?? formData.ipGroupId)
  if (editingPlanId.value) {
    formData.steps.forEach((step) => {
      step.assigneeId = undefined
    })
    return
  }
  regenerateTaskPreview()
}

const handleAdd = () => {
  dialogTitle.value = '新增计划'
  resetForm()
  formData.dateRange = [...defaultPlanDateRange()]
  syncDayMatchKeys()
  dialogVisible.value = true
}

const handleEdit = async (row: ContentPlanVO) => {
  const detail = await getContentPlan(row.id)
  editingPlanId.value = detail.id
  dialogTitle.value = '编辑草稿计划'
  Object.assign(formData, {
    planName: detail.planName,
    dateRange: [detail.startDate, detail.endDate],
    templateId: detail.templateId,
    ipGroupId: detail.ipGroupId,
    selectedCompetitions: detail.competitions || [],
    description: detail.description || '',
    steps: (detail.steps || []).map((step) => ({
      nodeId: step.nodeId,
      nodeName: step.nodeName || '',
      nodeOrder: step.nodeOrder || 0,
      executorRole: step.executorRole || '',
      competitionIds: step.competitionIds?.length
        ? [...step.competitionIds]
        : step.competitionId
          ? [step.competitionId]
          : [],
      assigneeId: step.assigneeIds?.[0],
      scheduledStart: step.scheduledStart,
      scheduledEnd: step.scheduledEnd,
    })),
  })
  dialogVisible.value = true
}

const handleTemplateChange = async (templateId: number) => {
  if (!templateId) {
    sopNodes.value = []
    formData.steps = []
    taskPreviewRows.value = []
    return
  }
  const nodes = await getSopNodeList(templateId)
  sopNodes.value = nodes.sort((a, b) => a.nodeOrder - b.nodeOrder)
  if (editingPlanId.value) {
    formData.steps = sopNodes.value.map((node) => ({
      nodeId: node.id,
      nodeName: node.nodeName,
      nodeOrder: node.nodeOrder,
      executorRole: node.executorRole,
      competitionIds: formData.selectedCompetitions.length === 1
        ? [formData.selectedCompetitions[0].competitionId]
        : [],
      assigneeId: undefined,
      scheduledStart: undefined,
      scheduledEnd: undefined,
    }))
  } else {
    regenerateTaskPreview()
  }
}

const handleDateRangeChange = () => {
  if (editingPlanId.value && formData.dateRange?.length === 2) {
    formData.steps.forEach((step) => {
      if (!step.scheduledStart) step.scheduledStart = `${formData.dateRange[0]} 00:00:00`
      if (!step.scheduledEnd) step.scheduledEnd = `${formData.dateRange[1]} 23:59:59`
    })
    return
  }
  syncDayMatchKeys()
  regenerateTaskPreview()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (editingPlanId.value) {
    if (formData.steps.length === 0) {
      ElMessage.warning('请先选择包含节点的 SOP 模板')
      return
    }
    if (formData.steps.some((step) => !step.assigneeId)) {
      ElMessage.warning('请为每个 SOP 步骤分配执行人')
      return
    }
    if (formData.steps.some((step) => !step.competitionIds?.length)) {
      ElMessage.warning('请为每个 SOP 步骤分配赛事')
      return
    }
  } else {
    if (!taskPreviewRows.value.length) {
      ElMessage.warning('请为计划日期安排赛事并生成任务')
      return
    }
    if (taskPreviewRows.value.some((row) => !row.assigneeId)) {
      ElMessage.warning('请为每条任务分配执行人')
      return
    }
  }

  submitting.value = true
  try {
    if (editingPlanId.value) {
      const payload = {
        planName: formData.planName,
        startDate: formData.dateRange[0],
        endDate: formData.dateRange[1],
        description: formData.description || undefined,
        competitions: formData.selectedCompetitions.map((item) => ({
          competitionId: item.competitionId,
          competitionName: item.competitionName,
        })),
        steps: formData.steps.map((step) => ({
          nodeId: step.nodeId,
          competitionIds: step.competitionIds,
          assigneeIds: step.assigneeId ? [step.assigneeId] : [],
          scheduledStart: step.scheduledStart,
          scheduledEnd: step.scheduledEnd,
        })),
      }
      await updateContentPlan({ id: editingPlanId.value, ...payload })
      ElMessage.success('草稿计划已更新')
    } else {
      const allMatches = collectAllMatches(dayMatches)
      const competitions = allMatches.map((m) => ({
        competitionId: toCompetitionId(m),
        competitionName: toCompetitionName(m),
      }))
      const steps = buildStepsFromTasks(taskPreviewRows.value)
      const tasks = taskPreviewRows.value.map((row) => ({
        nodeId: row.nodeId,
        competitionId: row.competitionId,
        assigneeId: row.assigneeId!,
        scheduledStart: row.scheduledStart,
        scheduledEnd: row.scheduledEnd,
      }))
      await createContentPlan({
        planName: formData.planName,
        startDate: formData.dateRange[0],
        endDate: formData.dateRange[1],
        description: formData.description || undefined,
        templateId: formData.templateId!,
        ipGroupId: formData.ipGroupId!,
        competitions,
        steps,
        tasks,
      })
      ElMessage.success('计划已保存为草稿')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

const handleView = (row: ContentPlanVO) => {
  detailDrawerTitle.value = `计划详情 - ${row.planName}`
  viewingPlanId.value = row.id
  detailDrawerVisible.value = true
}

const handleStart = async (row: ContentPlanVO) => {
  await ElMessageBox.confirm(`确认启动计划「${row.planName}」？启动后关联任务将出现在任务列表。`, '提示', { type: 'warning' })
  await startContentPlan(row.id)
  ElMessage.success('计划已启动')
  loadList()
}

const handleTerminate = async (row: ContentPlanVO) => {
  const { value } = await ElMessageBox.prompt('请输入终止原因', '申请终止计划', {
    confirmButtonText: '提交审批',
    cancelButtonText: '取消',
    inputPlaceholder: '终止原因',
  })
  await submitTerminatePlan(row.id, value)
  ElMessage.success('已提交终止审批')
  loadList()
}

const handleApproveTerminate = async (row: ContentPlanVO) => {
  await ElMessageBox.confirm(`确认批准终止计划「${row.planName}」？`, '组长审批', { type: 'warning' })
  await approveTerminatePlan(row.id)
  ElMessage.success('计划已终止')
  loadList()
}

const handleRejectTerminate = async (row: ContentPlanVO) => {
  await ElMessageBox.confirm(`确认驳回终止申请，计划「${row.planName}」继续执行？`, '组长审批', { type: 'info' })
  await rejectTerminatePlan(row.id)
  ElMessage.success('已驳回终止申请')
  loadList()
}

const handleDelete = async (row: ContentPlanVO) => {
  await ElMessageBox.confirm(`确认删除草稿计划「${row.planName}」？`, '提示', { type: 'warning' })
  await deleteContentPlan(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(async () => {
  await loadOptions()
  await loadList()
})
</script>

<style scoped lang="scss">
.plan-page {
  padding: 20px;
  .action-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }
  .total-info { color: #909399; font-size: 14px; }
  .competition-picker {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    width: 100%;
  }
  .competition-tag { max-width: 360px; }
  .section-tip { color: #909399; font-size: 13px; margin: 0 0 12px; }
  .day-matrix-wrap { overflow-x: auto; margin-bottom: 16px; }
  .day-matrix { display: flex; gap: 12px; min-width: min-content; }
  .day-column {
    flex: 0 0 200px;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    padding: 10px;
    background: #fafafa;
  }
  .day-header { font-weight: 600; font-size: 13px; margin-bottom: 8px; color: #303133; }
  .day-matches { display: flex; flex-direction: column; gap: 6px; min-height: 40px; margin-bottom: 8px; }
  .day-match-tag { max-width: 100%; }
  .fallback-tag { margin-top: 4px; }
  .pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
  :deep(.el-divider__text) { font-weight: 600; color: #303133; }
}
</style>

<style lang="scss">
.plan-detail-drawer.el-drawer {
  min-width: 720px;
}
</style>
