<template>
  <div class="threshold-config">
    <ContentWrap title="阈值规则配置" subtitle="预警、粉丝、作品阈值及账号覆盖">
      <el-alert
        title="按类别配置阈值规则；账号覆盖优先级高于全局阈值"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-tabs v-model="activeTab" style="margin-bottom: 16px" @tab-change="handleTabChange">
        <el-tab-pane label="预警阈值" name="ALERT" />
        <el-tab-pane label="粉丝阈值" name="FANS" />
        <el-tab-pane label="作品阈值" name="WORK" />
        <el-tab-pane label="账号覆盖" name="OVERRIDE" />
      </el-tabs>

      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item v-if="activeTab === 'ALERT' || activeTab === 'WORK' || activeTab === 'OVERRIDE'" label="指标名称">
            <el-input v-model="searchForm.metricName" placeholder="请输入" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item v-if="activeTab !== 'OVERRIDE'" label="平台">
            <DictSelect v-model="searchForm.platformType" dict-type="dict_platform_type" placeholder="请选择" style="width: 150px" />
          </el-form-item>
          <el-form-item label="状态">
            <DictSelect v-model="searchForm.status" dict-type="dict_config_status" placeholder="请选择" style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
            <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
          </el-form-item>
        </el-form>
      </TableSearch>

      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>新增规则</el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>删除选中
        </el-button>
      </div>

      <el-table
        :data="ruleList"
        border
        stripe
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />

        <!-- 预警阈值列 -->
        <template v-if="activeTab === 'ALERT'">
          <el-table-column prop="metricName" label="指标名称" min-width="140" />
          <el-table-column prop="platformType" label="平台" width="120" align="center">
            <template #default="{ row }"><DictLabel dict-type="dict_platform_type" :value="row.platformType" /></template>
          </el-table-column>
          <el-table-column prop="thresholdType" label="阈值类型" width="100" align="center">
            <template #default="{ row }"><DictLabel dict-type="dict_threshold_type" :value="row.thresholdType" /></template>
          </el-table-column>
          <el-table-column prop="compareOperator" label="比较符" width="90" align="center">
            <template #default="{ row }"><DictLabel dict-type="dict_compare_operator" :value="row.compareOperator" /></template>
          </el-table-column>
          <el-table-column prop="thresholdValue" label="阈值" width="100" align="center" />
          <el-table-column prop="notifyMethods" label="通知渠道" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ formatNotifyMethods(row.notifyMethods) }}</template>
          </el-table-column>
        </template>

        <!-- 粉丝阈值列 -->
        <template v-else-if="activeTab === 'FANS'">
          <el-table-column prop="platformType" label="平台" width="120" align="center">
            <template #default="{ row }"><DictLabel dict-type="dict_platform_type" :value="row.platformType" /></template>
          </el-table-column>
          <el-table-column prop="lowFans" label="低粉阈值" width="110" align="center" />
          <el-table-column prop="highFans" label="高粉阈值" width="110" align="center" />
          <el-table-column prop="dailyLow" label="日增低粉" width="110" align="center" />
          <el-table-column prop="dailyHigh" label="日增高粉" width="110" align="center" />
        </template>

        <!-- 作品阈值列 -->
        <template v-else-if="activeTab === 'WORK'">
          <el-table-column prop="platformType" label="平台" width="120" align="center">
            <template #default="{ row }"><DictLabel dict-type="dict_platform_type" :value="row.platformType" /></template>
          </el-table-column>
          <el-table-column prop="contentType" label="内容类型" width="100" align="center">
            <template #default="{ row }"><DictLabel dict-type="dict_content_type" :value="row.contentType" /></template>
          </el-table-column>
          <el-table-column prop="metricName" label="指标" min-width="120" />
          <el-table-column prop="hotValue" label="爆款阈值" width="110" align="center" />
          <el-table-column prop="lowValue" label="低分阈值" width="110" align="center" />
          <el-table-column prop="judgeMode" label="判定模式" width="100" align="center">
            <template #default="{ row }"><DictLabel dict-type="dict_judge_mode" :value="row.judgeMode" /></template>
          </el-table-column>
        </template>

        <!-- 账号覆盖列 -->
        <template v-else>
          <el-table-column prop="overrideAccountId" label="覆盖账号ID" width="120" align="center" />
          <el-table-column prop="metricName" label="指标名称" min-width="140" />
          <el-table-column prop="overrideValue" label="覆盖值" width="110" align="center" />
        </template>

        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleToggleStatus(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </ContentWrap>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="activeRules" label-width="110px">
        <!-- 预警阈值表单 -->
        <template v-if="activeTab === 'ALERT'">
          <el-form-item label="指标名称" prop="metricName">
            <el-input v-model="formData.metricName" placeholder="如：阅读量骤降" />
          </el-form-item>
          <el-form-item label="平台" prop="platformType">
            <DictSelect v-model="formData.platformType" dict-type="dict_platform_type" style="width: 100%" />
          </el-form-item>
          <el-form-item label="阈值类型" prop="thresholdType">
            <DictSelect v-model="formData.thresholdType" dict-type="dict_threshold_type" style="width: 100%" />
          </el-form-item>
          <el-form-item label="比较符" prop="compareOperator">
            <DictSelect v-model="formData.compareOperator" dict-type="dict_compare_operator" style="width: 100%" />
          </el-form-item>
          <el-form-item label="阈值" prop="thresholdValue">
            <el-input-number v-model="formData.thresholdValue" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="通知渠道">
            <el-checkbox-group v-model="formData.notifyMethods">
              <el-checkbox label="IN_APP">站内消息</el-checkbox>
              <el-checkbox label="DINGTALK">钉钉</el-checkbox>
              <el-checkbox label="SMS">短信</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>

        <!-- 粉丝阈值表单 -->
        <template v-else-if="activeTab === 'FANS'">
          <el-form-item label="平台" prop="platformType">
            <DictSelect v-model="formData.platformType" dict-type="dict_platform_type" style="width: 100%" />
          </el-form-item>
          <el-form-item label="低粉阈值" prop="lowFans">
            <el-input-number v-model="formData.lowFans" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="高粉阈值" prop="highFans">
            <el-input-number v-model="formData.highFans" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="日增低粉" prop="dailyLow">
            <el-input-number v-model="formData.dailyLow" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="日增高粉" prop="dailyHigh">
            <el-input-number v-model="formData.dailyHigh" :min="0" style="width: 100%" />
          </el-form-item>
        </template>

        <!-- 作品阈值表单 -->
        <template v-else-if="activeTab === 'WORK'">
          <el-form-item label="平台" prop="platformType">
            <DictSelect v-model="formData.platformType" dict-type="dict_platform_type" style="width: 100%" />
          </el-form-item>
          <el-form-item label="内容类型" prop="contentType">
            <DictSelect v-model="formData.contentType" dict-type="dict_content_type" style="width: 100%" />
          </el-form-item>
          <el-form-item label="指标" prop="metricName">
            <el-input v-model="formData.metricName" placeholder="如：播放量" />
          </el-form-item>
          <el-form-item label="爆款阈值" prop="hotValue">
            <el-input-number v-model="formData.hotValue" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="低分阈值" prop="lowValue">
            <el-input-number v-model="formData.lowValue" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="判定模式" prop="judgeMode">
            <DictSelect v-model="formData.judgeMode" dict-type="dict_judge_mode" style="width: 100%" />
          </el-form-item>
        </template>

        <!-- 账号覆盖表单 -->
        <template v-else>
          <el-form-item label="平台" prop="platformType">
            <DictSelect v-model="formData.platformType" dict-type="dict_platform_type" style="width: 100%" />
          </el-form-item>
          <el-form-item label="覆盖账号" prop="overrideAccountId">
            <AccountSelect v-model="formData.overrideAccountId" :platform-type="formData.platformType" />
          </el-form-item>
          <el-form-item label="指标名称" prop="metricName">
            <el-input v-model="formData.metricName" placeholder="如：低粉阈值/爆款阈值" />
          </el-form-item>
          <el-form-item label="覆盖值" prop="overrideValue">
            <el-input-number v-model="formData.overrideValue" :min="0" style="width: 100%" />
          </el-form-item>
        </template>

        <el-form-item label="状态">
          <el-switch v-model="formData.status" active-value="ENABLED" inactive-value="DISABLED" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
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
import DictLabel from '@/components/DictLabel.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import {
  fetchThresholdList,
  createThreshold,
  updateThreshold,
  deleteThreshold,
  type ThresholdConfigVO,
} from '@/api/config'

type ThresholdTab = 'ALERT' | 'FANS' | 'WORK' | 'OVERRIDE'

const activeTab = ref<ThresholdTab>('ALERT')
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const searchForm = reactive({ metricName: '', platformType: '', status: '' })
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const ruleList = ref<ThresholdConfigVO[]>([])
const selectedRows = ref<ThresholdConfigVO[]>([])

const formData = reactive<Record<string, unknown>>({
  id: undefined,
  metricName: '',
  platformType: '',
  thresholdType: 'PERCENTAGE',
  compareOperator: 'GTE',
  thresholdValue: 0,
  notifyMethods: ['IN_APP'] as string[],
  contentType: '',
  judgeMode: 'AND',
  lowFans: 0,
  highFans: 0,
  dailyLow: 0,
  dailyHigh: 0,
  hotValue: 0,
  lowValue: 0,
  overrideAccountId: undefined as number | undefined,
  overrideValue: 0,
  status: 'ENABLED',
  remark: '',
})

const alertRules: FormRules = {
  metricName: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  thresholdType: [{ required: true, message: '请选择阈值类型', trigger: 'change' }],
  compareOperator: [{ required: true, message: '请选择比较符', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
}

const fansRules: FormRules = {
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  lowFans: [{ required: true, message: '请输入低粉阈值', trigger: 'blur' }],
  highFans: [{ required: true, message: '请输入高粉阈值', trigger: 'blur' }],
}

const workRules: FormRules = {
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  metricName: [{ required: true, message: '请输入指标', trigger: 'blur' }],
  hotValue: [{ required: true, message: '请输入爆款阈值', trigger: 'blur' }],
  lowValue: [{ required: true, message: '请输入低分阈值', trigger: 'blur' }],
  judgeMode: [{ required: true, message: '请选择判定模式', trigger: 'change' }],
}

const overrideRules: FormRules = {
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  overrideAccountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  metricName: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
  overrideValue: [{ required: true, message: '请输入覆盖值', trigger: 'blur' }],
}

const activeRules = computed(() => {
  const map: Record<ThresholdTab, FormRules> = {
    ALERT: alertRules,
    FANS: fansRules,
    WORK: workRules,
    OVERRIDE: overrideRules,
  }
  return map[activeTab.value]
})

const dialogTitle = computed(() => {
  const labels: Record<ThresholdTab, string> = {
    ALERT: '预警阈值',
    FANS: '粉丝阈值',
    WORK: '作品阈值',
    OVERRIDE: '账号覆盖',
  }
  return (formData.id ? '编辑' : '新增') + labels[activeTab.value]
})

function parseNotifyMethods(value?: string): string[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return value.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

function formatNotifyMethods(value?: string): string {
  const methods = parseNotifyMethods(value)
  const labelMap: Record<string, string> = {
    IN_APP: '站内消息',
    DINGTALK: '钉钉',
    SMS: '短信',
  }
  return methods.map((m) => labelMap[m] || m).join('、') || '-'
}

function defaultForm(): Record<string, unknown> {
  return {
    id: undefined,
    metricName: '',
    platformType: '',
    thresholdType: 'PERCENTAGE',
    compareOperator: 'GTE',
    thresholdValue: 0,
    notifyMethods: ['IN_APP'],
    contentType: '',
    judgeMode: 'AND',
    lowFans: 0,
    highFans: 0,
    dailyLow: 0,
    dailyHigh: 0,
    hotValue: 0,
    lowValue: 0,
    overrideAccountId: undefined,
    overrideValue: 0,
    status: 'ENABLED',
    remark: '',
  }
}

function buildPayload(): Record<string, unknown> {
  const base = { thresholdCategory: activeTab.value, status: formData.status, remark: formData.remark }
  switch (activeTab.value) {
    case 'ALERT':
      return {
        ...base,
        metricName: formData.metricName,
        platformType: formData.platformType,
        thresholdType: formData.thresholdType,
        compareOperator: formData.compareOperator,
        thresholdValue: formData.thresholdValue,
        notifyMethods: JSON.stringify(formData.notifyMethods || []),
      }
    case 'FANS':
      return {
        ...base,
        platformType: formData.platformType,
        lowFans: formData.lowFans,
        highFans: formData.highFans,
        dailyLow: formData.dailyLow,
        dailyHigh: formData.dailyHigh,
      }
    case 'WORK':
      return {
        ...base,
        platformType: formData.platformType,
        contentType: formData.contentType,
        metricName: formData.metricName,
        hotValue: formData.hotValue,
        lowValue: formData.lowValue,
        judgeMode: formData.judgeMode,
      }
    case 'OVERRIDE':
      return {
        ...base,
        platformType: formData.platformType,
        overrideAccountId: formData.overrideAccountId,
        metricName: formData.metricName,
        overrideValue: formData.overrideValue,
      }
    default:
      return base
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await fetchThresholdList({
      thresholdCategory: activeTab.value,
      metricName: searchForm.metricName || undefined,
      platformType: searchForm.platformType || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    ruleList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  searchForm.metricName = ''
  searchForm.platformType = ''
  searchForm.status = ''
  pageNo.value = 1
  selectedRows.value = []
  loadList()
}

const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.metricName = ''
  searchForm.platformType = ''
  searchForm.status = ''
  pageNo.value = 1
  loadList()
}

const handleCreate = () => {
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

const handleEdit = (row: ThresholdConfigVO) => {
  Object.assign(formData, defaultForm(), {
    id: row.id,
    metricName: row.metricName || '',
    platformType: row.platformType || '',
    thresholdType: row.thresholdType || 'PERCENTAGE',
    compareOperator: row.compareOperator || 'GTE',
    thresholdValue: row.thresholdValue ?? 0,
    notifyMethods: parseNotifyMethods(row.notifyMethods),
    contentType: row.contentType || '',
    judgeMode: row.judgeMode || 'AND',
    lowFans: row.lowFans ?? 0,
    highFans: row.highFans ?? 0,
    dailyLow: row.dailyLow ?? 0,
    dailyHigh: row.dailyHigh ?? 0,
    hotValue: row.hotValue ?? 0,
    lowValue: row.lowValue ?? 0,
    overrideAccountId: row.overrideAccountId,
    overrideValue: row.overrideValue ?? 0,
    status: row.status || 'ENABLED',
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = buildPayload()
    if (formData.id) {
      await updateThreshold({ id: formData.id as number, ...payload })
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

const handleToggleStatus = async (row: ThresholdConfigVO) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const label = row.metricName || row.platformType || String(row.id)
  try {
    await ElMessageBox.confirm(`确定${newStatus === 'ENABLED' ? '启用' : '停用'}规则「${label}」吗？`, '提示', { type: 'warning' })
    await updateThreshold({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadList()
  } catch {}
}

const handleDelete = async (row: ThresholdConfigVO) => {
  const label = row.metricName || row.platformType || String(row.id)
  try {
    await ElMessageBox.confirm(`确定删除规则「${label}」吗？`, '删除确认', { type: 'warning' })
    await deleteThreshold(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleSelectionChange = (selection: ThresholdConfigVO[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 条规则吗？`, '批量删除', { type: 'warning' })
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
