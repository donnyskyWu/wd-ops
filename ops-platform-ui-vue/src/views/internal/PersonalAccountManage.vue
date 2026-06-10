<template>
  <div class="personal-account-page">
    <el-tabs v-model="activePlatform" @tab-change="handleTabChange" class="platform-tabs">
      <el-tab-pane label="企业微信" name="WEWORK" />
      <el-tab-pane label="个人微信" name="PERSONAL_WX" />
    </el-tabs>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="账号名称">
        <el-input v-model="searchForm.accountName" placeholder="请输入账号名称" clearable />
      </el-form-item>
      <el-form-item v-if="activePlatform === 'WEWORK'" label="Corp ID">
        <el-input v-model="searchForm.corpId" placeholder="请输入 Corp ID" clearable />
      </el-form-item>
      <el-form-item v-else label="微信号">
        <el-input v-model="searchForm.wechatId" placeholder="请输入微信号" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable>
          <el-option label="正常" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增账号
      </el-button>
      <span class="total-info">共 {{ pagination.total }} 条</span>
    </div>

    <!-- 企微：应用配置 + 员工账号 -->
    <template v-if="activePlatform === 'WEWORK'">
      <el-card shadow="never" class="wework-config-card">
        <template #header>
          <div class="card-header">
            <span>企业微信应用配置</span>
            <el-button type="primary" link @click="handleEditWeworkConfig">
              {{ weworkConfig ? '编辑配置' : '新增配置' }}
            </el-button>
          </div>
        </template>
        <el-descriptions v-if="weworkConfig" :column="2" border size="small">
          <el-descriptions-item label="账号名称">{{ weworkConfig.accountName }}</el-descriptions-item>
          <el-descriptions-item label="Corp ID">{{ weworkConfig.corpId }}</el-descriptions-item>
          <el-descriptions-item label="Agent ID">{{ weworkConfig.agentId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="weworkConfig.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ weworkConfig.status === 'ENABLED' ? '正常' : '停用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="尚未配置企业微信应用，请点击右上角新增" :image-size="64" />
      </el-card>

      <div class="section-title">
        <span>员工企微账号</span>
        <el-button type="primary" size="small" :disabled="!weworkConfig" @click="handleAddEmployee">
          <el-icon><Plus /></el-icon>
          新增员工账号
        </el-button>
      </div>
      <el-table :data="employeeList" v-loading="employeeLoading" border stripe>
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="weworkUserId" label="企微 ID" width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="position" label="岗位" width="120" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEditEmployee(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteEmployee(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!employeeLoading && employeeList.length === 0" description="暂无员工企微账号数据" :image-size="80" />
    </template>

    <!-- 个微列表 -->
    <template v-else>
      <el-table :data="personalList" v-loading="loading" border stripe>
      <el-table-column prop="accountName" label="微信名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="wechatId" label="微信号" width="160" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ row.status === 'ENABLED' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleViewPersonal(row)">详情</el-button>
          <el-button link type="primary" @click="handleEditPersonal(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDeletePersonal(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </template>

    <Pagination
      v-if="activePlatform === 'PERSONAL_WX'"
      :current-page="pagination.pageNo"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @update:current-page="(val) => pagination.pageNo = val"
      @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
      @change="loadData"
    />

    <!-- 企微表单 -->
    <el-dialog v-model="weworkDialogVisible" :title="weworkDialogTitle" width="560px">
      <el-form :model="weworkForm" ref="weworkFormRef" :rules="weworkRules" label-width="100px">
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="weworkForm.accountName" maxlength="100" />
        </el-form-item>
        <el-form-item label="Corp ID" prop="corpId">
          <el-input v-model="weworkForm.corpId" maxlength="64" :disabled="!!weworkForm.id" />
        </el-form-item>
        <el-form-item label="Agent ID" prop="agentId">
          <el-input v-model="weworkForm.agentId" maxlength="64" :disabled="!!weworkForm.id" />
        </el-form-item>
        <el-form-item label="Secret" prop="secret">
          <el-input v-model="weworkForm.secret" type="password" show-password maxlength="128"
            :placeholder="weworkForm.id ? '留空则不修改' : '请输入 Secret'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="weworkForm.status" style="width: 100%">
            <el-option label="正常" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="weworkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWework">保存</el-button>
      </template>
    </el-dialog>

    <!-- 企微员工表单 -->
    <el-dialog v-model="employeeDialogVisible" :title="employeeDialogTitle" width="640px" destroy-on-close>
      <el-form :model="employeeForm" ref="employeeFormRef" :rules="employeeRules" label-width="120px" class="employee-form">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="employeeForm.nickname" maxlength="100" placeholder="请输入员工昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企微 ID" prop="weworkUserId">
              <el-input v-model="employeeForm.weworkUserId" maxlength="64" placeholder="企业微信 UserID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="employeeForm.phone" maxlength="11" placeholder="11 位手机号（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="employeeForm.status" style="width: 100%">
                <el-option label="正常" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">组织信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="employeeForm.department" maxlength="100" placeholder="所属部门" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-input v-model="employeeForm.position" maxlength="100" placeholder="岗位名称" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="employeeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEmployee">保存</el-button>
      </template>
    </el-dialog>

    <!-- 个微表单 -->
    <el-dialog v-model="personalDialogVisible" :title="personalDialogTitle" width="560px">
      <el-form :model="personalForm" ref="personalFormRef" :rules="personalRules" label-width="100px">
        <el-form-item label="微信名" prop="accountName">
          <el-input v-model="personalForm.accountName" maxlength="100" />
        </el-form-item>
        <el-form-item label="微信号" prop="wechatId">
          <el-input v-model="personalForm.wechatId" maxlength="64" :disabled="!!personalForm.id" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input
            v-model="personalForm.contactPhone"
            placeholder="个微联系手机号（手动填写，非公司手机资产）"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="personalForm.status" style="width: 100%">
            <el-option label="正常" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="personalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPersonal">保存</el-button>
      </template>
    </el-dialog>

    <!-- 个微详情（奥创只读） -->
    <el-drawer v-model="detailVisible" title="个微详情" size="480px">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="微信名">{{ detailData.accountName }}</el-descriptions-item>
        <el-descriptions-item label="微信号">{{ detailData.wechatId }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone || detailData.phoneNumberMasked || '--' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailData.status === 'ENABLED' ? '正常' : '停用' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">奥创接口（只读脱敏）</el-divider>
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="API URL">{{ detailData.apiUrl || '--' }}</el-descriptions-item>
        <el-descriptions-item label="App ID">{{ detailData.appId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="App Secret">{{ detailData.appSecret || '--' }}</el-descriptions-item>
        <el-descriptions-item label="Token">{{ detailData.token || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import {
  getPersonalWechatPage,
  getPersonalWechat,
  createPersonalWechat,
  updatePersonalWechat,
  deletePersonalWechat,
  getWeworkPage,
  createWework,
  updateWework,
  deleteWework,
  getWeworkEmployeePage,
  createWeworkEmployee,
  updateWeworkEmployee,
  deleteWeworkEmployee,
  type PersonalWechatVO,
  type WeworkVO,
  type WeworkEmployeeVO,
} from '@/api/personal-account'

type PlatformType = 'WEWORK' | 'PERSONAL_WX'

const activePlatform = ref<PlatformType>('WEWORK')
const loading = ref(false)
const employeeLoading = ref(false)
const personalList = ref<PersonalWechatVO[]>([])
const weworkList = ref<WeworkVO[]>([])
const employeeList = ref<WeworkEmployeeVO[]>([])
const weworkConfig = computed(() => weworkList.value[0] ?? null)

const searchForm = reactive({
  accountName: '',
  wechatId: '',
  corpId: '',
  status: undefined as string | undefined,
})

const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const weworkDialogVisible = ref(false)
const weworkDialogTitle = ref('新增企微')
const weworkFormRef = ref<any>()
const weworkForm = reactive({
  id: undefined as number | undefined,
  accountName: '',
  corpId: '',
  agentId: '',
  secret: '',
  status: 'ENABLED',
})
const weworkRules = {
  accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  corpId: [{ required: true, message: '请输入 Corp ID', trigger: 'blur' }],
  agentId: [{ required: true, message: '请输入 Agent ID', trigger: 'blur' }],
  secret: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (!weworkForm.id && !val) cb(new Error('请输入 Secret'))
      else cb()
    },
    trigger: 'blur',
  }],
}

const personalDialogVisible = ref(false)
const personalDialogTitle = ref('新增个微')
const personalFormRef = ref<any>()
const personalForm = reactive({
  id: undefined as number | undefined,
  accountName: '',
  wechatId: '',
  contactPhone: '',
  status: 'ENABLED',
})
const personalRules = {
  accountName: [{ required: true, message: '请输入微信名', trigger: 'blur' }],
  wechatId: [{ required: true, message: '请输入微信号', trigger: 'blur' }],
  contactPhone: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (val && !/^1[3-9]\d{9}$/.test(val)) cb(new Error('联系电话格式不正确'))
      else cb()
    },
    trigger: 'blur',
  }],
}

const employeeDialogVisible = ref(false)
const employeeDialogTitle = ref('新增员工账号')
const employeeFormRef = ref<any>()
const employeeForm = reactive({
  id: undefined as number | undefined,
  nickname: '',
  weworkUserId: '',
  phone: '',
  department: '',
  position: '',
  status: 'ENABLED',
})
const employeeRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  weworkUserId: [{ required: true, message: '请输入企微 ID', trigger: 'blur' }],
  phone: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (val && !/^1[3-9]\d{9}$/.test(val)) cb(new Error('手机号格式不正确'))
      else cb()
    },
    trigger: 'blur',
  }],
}

const detailVisible = ref(false)
const detailData = ref<PersonalWechatVO | null>(null)

const loadEmployees = async () => {
  if (!weworkConfig.value) {
    employeeList.value = []
    return
  }
  employeeLoading.value = true
  try {
    const res = await getWeworkEmployeePage({
      weworkAccountId: weworkConfig.value.id,
      pageNo: 1,
      pageSize: 100,
    })
    employeeList.value = res.list
  } finally {
    employeeLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
    if (activePlatform.value === 'WEWORK') {
      const res = await getWeworkPage({
        accountName: searchForm.accountName || undefined,
        corpId: searchForm.corpId || undefined,
        status: searchForm.status,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize,
      })
      weworkList.value = res.list
      pagination.total = res.total
      await loadEmployees()
    } else {
      const res = await getPersonalWechatPage({
        accountName: searchForm.accountName || undefined,
        wechatId: searchForm.wechatId || undefined,
        status: searchForm.status,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize,
      })
      personalList.value = res.list
      pagination.total = res.total
    }
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  searchForm.accountName = ''
  searchForm.wechatId = ''
  searchForm.corpId = ''
  searchForm.status = undefined
  pagination.pageNo = 1
  loadData()
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => handleTabChange()

const handleAdd = () => {
  if (activePlatform.value === 'WEWORK') {
    handleEditWeworkConfig()
  } else {
    personalDialogTitle.value = '新增个微'
    Object.assign(personalForm, { id: undefined, accountName: '', wechatId: '', contactPhone: '', status: 'ENABLED' })
    personalDialogVisible.value = true
  }
}

const handleEditWeworkConfig = () => {
  if (weworkConfig.value) {
    handleEditWework(weworkConfig.value)
  } else {
    weworkDialogTitle.value = '新增企微应用配置'
    Object.assign(weworkForm, { id: undefined, accountName: '', corpId: '', agentId: '', secret: '', status: 'ENABLED' })
    weworkDialogVisible.value = true
  }
}

const handleEditWework = (row: WeworkVO) => {
  weworkDialogTitle.value = '编辑企微'
  Object.assign(weworkForm, { id: row.id, accountName: row.accountName, corpId: row.corpId, agentId: row.agentId, secret: '', status: row.status })
  weworkDialogVisible.value = true
}

const handleEditPersonal = (row: PersonalWechatVO) => {
  personalDialogTitle.value = '编辑个微'
  Object.assign(personalForm, {
    id: row.id,
    accountName: row.accountName,
    wechatId: row.wechatId,
    contactPhone: row.contactPhone || '',
    status: row.status,
  })
  personalDialogVisible.value = true
}

const handleAddEmployee = () => {
  if (!weworkConfig.value) {
    ElMessage.warning('请先配置企业微信应用')
    return
  }
  employeeDialogTitle.value = '新增员工账号'
  Object.assign(employeeForm, {
    id: undefined,
    nickname: '',
    weworkUserId: '',
    phone: '',
    department: '',
    position: '',
    status: 'ENABLED',
  })
  employeeDialogVisible.value = true
}

const handleEditEmployee = (row: WeworkEmployeeVO) => {
  employeeDialogTitle.value = '编辑员工账号'
  Object.assign(employeeForm, {
    id: row.id,
    nickname: row.nickname,
    weworkUserId: row.weworkUserId,
    phone: row.phone || '',
    department: row.department || '',
    position: row.position || '',
    status: row.status,
  })
  employeeDialogVisible.value = true
}

const handleDeleteEmployee = async (row: WeworkEmployeeVO) => {
  await ElMessageBox.confirm(`确定删除员工「${row.nickname}」？`, '提示', { type: 'warning' })
  await deleteWeworkEmployee(row.id)
  ElMessage.success('删除成功')
  loadEmployees()
}

const submitEmployee = async () => {
  if (!employeeFormRef.value || !weworkConfig.value) return
  await employeeFormRef.value.validate()
  if (employeeForm.id) {
    await updateWeworkEmployee({
      id: employeeForm.id,
      nickname: employeeForm.nickname,
      weworkUserId: employeeForm.weworkUserId,
      phone: employeeForm.phone || undefined,
      department: employeeForm.department || undefined,
      position: employeeForm.position || undefined,
      status: employeeForm.status,
    })
  } else {
    await createWeworkEmployee({
      weworkAccountId: weworkConfig.value.id,
      nickname: employeeForm.nickname,
      weworkUserId: employeeForm.weworkUserId,
      phone: employeeForm.phone || undefined,
      department: employeeForm.department || undefined,
      position: employeeForm.position || undefined,
      status: employeeForm.status,
    })
  }
  ElMessage.success('保存成功')
  employeeDialogVisible.value = false
  loadEmployees()
}

const handleViewPersonal = async (row: PersonalWechatVO) => {
  detailData.value = await getPersonalWechat(row.id)
  detailVisible.value = true
}

const submitWework = async () => {
  if (!weworkFormRef.value) return
  await weworkFormRef.value.validate()
  if (weworkForm.id) {
    await updateWework({
      id: weworkForm.id,
      accountName: weworkForm.accountName,
      secret: weworkForm.secret || undefined,
      status: weworkForm.status,
    })
  } else {
    await createWework({
      accountName: weworkForm.accountName,
      corpId: weworkForm.corpId,
      agentId: weworkForm.agentId,
      secret: weworkForm.secret,
      status: weworkForm.status,
    })
  }
  ElMessage.success('保存成功')
  weworkDialogVisible.value = false
  loadData()
}

const submitPersonal = async () => {
  if (!personalFormRef.value) return
  await personalFormRef.value.validate()
  if (personalForm.id) {
    await updatePersonalWechat({
      id: personalForm.id,
      accountName: personalForm.accountName,
      wechatId: personalForm.wechatId,
      contactPhone: personalForm.contactPhone.trim(),
      status: personalForm.status,
    })
  } else {
    await createPersonalWechat({
      accountName: personalForm.accountName,
      wechatId: personalForm.wechatId,
      contactPhone: personalForm.contactPhone.trim() || undefined,
      status: personalForm.status,
    })
  }
  ElMessage.success('保存成功')
  personalDialogVisible.value = false
  loadData()
}

const handleDeleteWework = async (row: WeworkVO) => {
  await ElMessageBox.confirm(`确定删除企微「${row.accountName}」？`, '提示', { type: 'warning' })
  await deleteWework(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleDeletePersonal = async (row: PersonalWechatVO) => {
  await ElMessageBox.confirm(`确定删除个微「${row.accountName}」？`, '提示', { type: 'warning' })
  await deletePersonalWechat(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.personal-account-page {
  .platform-tabs { margin-bottom: 16px; }
  .wework-config-card { margin-bottom: 20px; }
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    font-weight: 600;
  }
  .action-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }
  .total-info { color: #909399; font-size: 14px; }
  .employee-form {
    :deep(.el-divider__text) { font-weight: 600; color: #303133; }
  }
}
</style>
