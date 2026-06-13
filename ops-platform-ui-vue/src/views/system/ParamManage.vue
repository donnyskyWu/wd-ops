<template>
  <div class="param-manage">
    <ContentWrap title="系统参数" subtitle="系统运行参数配置">
      <el-alert
        title="配置系统运行的关键参数（影响系统行为和性能）"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-tabs v-model="activeTab" style="margin-bottom: 16px" @tab-change="handleTabChange">
        <el-tab-pane label="基础配置" name="basic" />
        <el-tab-pane label="采集配置" name="collect" />
        <el-tab-pane label="AI配置" name="ai" />
        <el-tab-pane label="通知配置" name="notification" />
        <el-tab-pane label="内容审核" name="contentReview" />
      </el-tabs>

      <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
        <el-form-item label="参数名称">
          <el-input v-model="searchForm.paramName" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="参数键">
          <el-input v-model="searchForm.paramKey" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
      </TableSearch>

      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增参数
        </el-button>
      </div>

      <el-table :data="tableList" border stripe v-loading="loading">
        <el-table-column prop="paramName" label="参数名称" min-width="180" />
        <el-table-column prop="paramKey" label="参数键" min-width="200" />
        <el-table-column prop="paramValue" label="参数值" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatParamValue(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="paramType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="PARAM_TYPE_TAG[row.paramType] || 'info'">
              {{ PARAM_TYPE_LABEL[row.paramType] || row.paramType }}
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

      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @update:current-page="handlePageChange"
        @update:page-size="handleSizeChange"
        @change="loadData"
      />
    </ContentWrap>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="参数名称" prop="paramName">
          <el-input v-model="formData.paramName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键" prop="paramKey">
          <el-input v-model="formData.paramKey" placeholder="请输入参数键（唯一标识）" />
        </el-form-item>
        <el-form-item label="参数值" prop="paramValue">
          <el-select
            v-if="isReviewRoleParam(formData.paramKey)"
            v-model="formData.paramValue"
            placeholder="请选择审核角色"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="opt in dialogRoleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-input v-else v-model="formData.paramValue" placeholder="请输入参数值" />
        </el-form-item>
        <el-form-item label="参数类型" prop="paramType">
          <el-select v-model="formData.paramType" placeholder="请选择参数类型" style="width: 100%">
            <el-option label="字符串" value="STRING" />
            <el-option label="数字" value="NUMBER" />
            <el-option label="布尔" value="BOOLEAN" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入参数说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import {
  fetchParamList,
  createParam,
  updateParam,
  deleteParam,
  PARAM_CATEGORY_TAB,
  PARAM_TYPE_LABEL,
  PARAM_TYPE_TAG,
  type ParamVO,
} from '@/api/system-param'
import { fetchRoleList, type RoleVO } from '@/api/system-user'

const REVIEW_ROLE_PARAM_KEYS = ['content.review.level1.role', 'content.review.level2.role'] as const
const IP_GROUP_LEADER_OPTION = { label: 'IP组长', value: 'OPS_LEADER' }

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const activeTab = ref('basic')
const tableList = ref<ParamVO[]>([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({ paramName: '', paramKey: '' })
const formData = reactive<Partial<ParamVO>>({
  paramName: '',
  paramKey: '',
  paramValue: '',
  paramType: 'STRING',
  category: 'BASIC',
  remark: '',
})

const rules: FormRules = {
  paramName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  paramKey: [
    { required: true, message: '请输入参数键', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_.]*$/, message: '只能包含字母、数字、下划线和点', trigger: 'blur' },
  ],
  paramValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }],
  paramType: [{ required: true, message: '请选择参数类型', trigger: 'change' }],
}

const dialogTitle = computed(() => (formData.id ? '编辑参数' : '新增参数'))

const roles = ref<RoleVO[]>([])

const reviewRoleOptions = computed(() => {
  const options = [IP_GROUP_LEADER_OPTION]
  for (const role of roles.value) {
    if (role.code !== IP_GROUP_LEADER_OPTION.value) {
      options.push({ label: role.name, value: role.code })
    }
  }
  return options
})

const dialogRoleOptions = computed(() => {
  const base = reviewRoleOptions.value
  const current = formData.paramValue
  if (current && !base.some((opt) => opt.value === current)) {
    return [...base, { label: String(current), value: String(current) }]
  }
  return base
})

function isReviewRoleParam(paramKey?: string) {
  return paramKey != null && (REVIEW_ROLE_PARAM_KEYS as readonly string[]).includes(paramKey)
}

function getReviewRoleLabel(code: string) {
  if (code === IP_GROUP_LEADER_OPTION.value) {
    return IP_GROUP_LEADER_OPTION.label
  }
  const role = roles.value.find((item) => item.code === code)
  return role?.name ?? code
}

function formatParamValue(row: ParamVO) {
  return isReviewRoleParam(row.paramKey) ? getReviewRoleLabel(row.paramValue) : row.paramValue
}

async function loadRoles() {
  try {
    const res = await fetchRoleList({ pageSize: 200 })
    roles.value = res.list || []
  } catch {
    roles.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchParamList({
      paramName: searchForm.paramName || undefined,
      paramKey: searchForm.paramKey || undefined,
      category: PARAM_CATEGORY_TAB[activeTab.value],
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    tableList.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  pageNo.value = 1
  loadData()
}

function handleSearch() {
  pageNo.value = 1
  loadData()
}

function handleReset() {
  searchForm.paramName = ''
  searchForm.paramKey = ''
  pageNo.value = 1
  loadData()
}

function handleCreate() {
  Object.assign(formData, {
    id: undefined,
    paramName: '',
    paramKey: '',
    paramValue: '',
    paramType: 'STRING',
    category: PARAM_CATEGORY_TAB[activeTab.value],
    remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: ParamVO) {
  Object.assign(formData, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = {
        paramName: formData.paramName!,
        paramKey: formData.paramKey!,
        paramValue: formData.paramValue!,
        paramType: formData.paramType!,
        category: formData.category || PARAM_CATEGORY_TAB[activeTab.value],
        remark: formData.remark,
      }
      if (formData.id) {
        await updateParam({ ...payload, id: formData.id })
        ElMessage.success('编辑成功')
      } else {
        await createParam(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

function handleDelete(row: ParamVO) {
  ElMessageBox.confirm(`确定要删除参数「${row.paramName}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteParam(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

function handlePageChange(page: number) {
  pageNo.value = page
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
}

onMounted(() => {
  loadRoles()
  loadData()
})
</script>

<style scoped>
.param-manage {
  padding: 20px;
}
</style>
