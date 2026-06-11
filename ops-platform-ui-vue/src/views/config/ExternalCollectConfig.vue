<template>
  <div class="external-collect-config">
    <ContentWrap title="外部采集配置" subtitle="外部账号监控与关键词配置">
      <el-alert
        title="管理待监控的外部账号与内容监测关键词，外部账号支持 CSV 批量导入"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="外部账号" name="account" />
        <el-tab-pane label="关键词配置" name="keyword" />
      </el-tabs>

      <!-- 外部账号 Tab -->
      <template v-if="activeTab === 'account'">
        <TableSearch>
          <el-form :inline="true" :model="accountSearch">
            <el-form-item label="账号名称">
              <el-input v-model="accountSearch.configName" placeholder="模糊搜索" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="平台">
              <DictSelect v-model="accountSearch.platformType" dict-type="dict_third_platform" placeholder="请选择" style="width: 150px" />
            </el-form-item>
            <el-form-item label="状态">
              <DictSelect v-model="accountSearch.status" dict-type="dict_config_status" placeholder="请选择" style="width: 120px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
              <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
            </el-form-item>
          </el-form>
        </TableSearch>

        <div style="margin-bottom: 16px">
          <el-button type="primary" @click="handleCreateAccount"><el-icon><Plus /></el-icon>新增账号</el-button>
          <el-button @click="triggerImport"><el-icon><Upload /></el-icon>CSV 导入</el-button>
          <input ref="fileInputRef" type="file" accept=".csv,text/csv" style="display: none" @change="handleImportFile" />
        </div>

        <el-table :data="accountList" border stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="platformType" label="平台" width="120" align="center">
            <template #default="{ row }">
              <DictLabel dict-type="dict_third_platform" :value="row.platformType" />
            </template>
          </el-table-column>
          <el-table-column prop="configName" label="账号名称" min-width="140" />
          <el-table-column prop="accountIdentifier" label="账号标识" min-width="140" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEditAccount(row)">编辑</el-button>
              <el-button link type="warning" @click="handleToggleAccountStatus(row)">
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="handleDeleteAccount(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 关键词 Tab -->
      <template v-else>
        <TableSearch>
          <el-form :inline="true" :model="keywordSearch">
            <el-form-item label="平台">
              <DictSelect v-model="keywordSearch.platform" dict-type="dict_platform_type" placeholder="请选择" style="width: 150px" />
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="keywordSearch.keyword" placeholder="模糊搜索" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="状态">
              <DictSelect v-model="keywordSearch.status" dict-type="dict_config_status" placeholder="请选择" style="width: 120px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
              <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
            </el-form-item>
          </el-form>
        </TableSearch>

        <div style="margin-bottom: 16px">
          <el-button type="primary" @click="handleCreateKeyword"><el-icon><Plus /></el-icon>新增关键词</el-button>
        </div>

        <el-table :data="keywordList" border stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="platform" label="平台" width="120" align="center">
            <template #default="{ row }">
              <DictLabel dict-type="dict_platform_type" :value="row.platform" />
            </template>
          </el-table-column>
          <el-table-column prop="keyword" label="关键词" min-width="160" />
          <el-table-column prop="matchType" label="匹配类型" width="110" align="center">
            <template #default="{ row }">
              <DictLabel dict-type="dict_match_type" :value="row.matchType" />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEditKeyword(row)">编辑</el-button>
              <el-button link type="warning" @click="handleToggleKeywordStatus(row)">
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="handleDeleteKeyword(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </ContentWrap>

    <!-- 外部账号弹窗 -->
    <el-dialog v-model="accountDialogVisible" :title="accountDialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-width="100px">
        <el-form-item label="平台" prop="platformType">
          <DictSelect v-model="accountForm.platformType" dict-type="dict_third_platform" placeholder="请选择平台" style="width: 100%" />
        </el-form-item>
        <el-form-item label="账号名称" prop="configName">
          <el-input v-model="accountForm.configName" placeholder="请输入账号名称" />
        </el-form-item>
        <el-form-item label="账号标识" prop="accountIdentifier">
          <el-input v-model="accountForm.accountIdentifier" placeholder="请输入账号标识" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="accountForm.status" active-value="ENABLED" inactive-value="DISABLED" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitAccount">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关键词弹窗 -->
    <el-dialog v-model="keywordDialogVisible" :title="keywordDialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="keywordFormRef" :model="keywordForm" :rules="keywordRules" label-width="100px">
        <el-form-item label="平台" prop="platform">
          <DictSelect v-model="keywordForm.platform" dict-type="dict_platform_type" placeholder="请选择平台" style="width: 100%" />
        </el-form-item>
        <el-form-item label="关键词" prop="keyword">
          <el-input v-model="keywordForm.keyword" placeholder="请输入关键词" />
        </el-form-item>
        <el-form-item label="匹配类型" prop="matchType">
          <DictSelect v-model="keywordForm.matchType" dict-type="dict_match_type" placeholder="请选择" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="keywordForm.status" active-value="ENABLED" inactive-value="DISABLED" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="keywordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitKeyword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Upload } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import {
  externalCollectApi,
  type CollectConfigVO,
  type KeywordConfigVO,
} from '@/api/config'

const activeTab = ref<'account' | 'keyword'>('account')
const loading = ref(false)
const submitLoading = ref(false)
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const fileInputRef = ref<HTMLInputElement>()

const accountList = ref<CollectConfigVO[]>([])
const keywordList = ref<KeywordConfigVO[]>([])

const accountSearch = reactive({ configName: '', platformType: '', status: '' })
const keywordSearch = reactive({ platform: '', keyword: '', status: '' })

const accountDialogVisible = ref(false)
const keywordDialogVisible = ref(false)
const accountFormRef = ref<FormInstance>()
const keywordFormRef = ref<FormInstance>()

const accountForm = reactive({
  id: undefined as number | undefined,
  platformType: '',
  configName: '',
  accountIdentifier: '',
  status: 'ENABLED' as 'ENABLED' | 'DISABLED',
})

const keywordForm = reactive({
  id: undefined as number | undefined,
  platform: '',
  keyword: '',
  matchType: 'FUZZY',
  status: 'ENABLED' as 'ENABLED' | 'DISABLED',
})

const accountRules: FormRules = {
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  configName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  accountIdentifier: [{ required: true, message: '请输入账号标识', trigger: 'blur' }],
}

const keywordRules: FormRules = {
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  keyword: [{ required: true, message: '请输入关键词', trigger: 'blur' }],
  matchType: [{ required: true, message: '请选择匹配类型', trigger: 'change' }],
}

const accountDialogTitle = computed(() => (accountForm.id ? '编辑外部账号' : '新增外部账号'))
const keywordDialogTitle = computed(() => (keywordForm.id ? '编辑关键词' : '新增关键词'))

const loadAccountList = async () => {
  loading.value = true
  try {
    const res = await externalCollectApi.list({
      subType: 'account',
      configName: accountSearch.configName || undefined,
      platformType: accountSearch.platformType || undefined,
      status: accountSearch.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    accountList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const loadKeywordList = async () => {
  loading.value = true
  try {
    const res = await externalCollectApi.keywordList({
      platform: keywordSearch.platform || undefined,
      keyword: keywordSearch.keyword || undefined,
      status: keywordSearch.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    keywordList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const loadList = () => {
  if (activeTab.value === 'account') {
    loadAccountList()
  } else {
    loadKeywordList()
  }
}

const handleTabChange = () => {
  pageNo.value = 1
  loadList()
}

const handleSearch = () => {
  pageNo.value = 1
  loadList()
}

const handleReset = () => {
  if (activeTab.value === 'account') {
    accountSearch.configName = ''
    accountSearch.platformType = ''
    accountSearch.status = ''
  } else {
    keywordSearch.platform = ''
    keywordSearch.keyword = ''
    keywordSearch.status = ''
  }
  pageNo.value = 1
  loadList()
}

const handleCreateAccount = () => {
  Object.assign(accountForm, {
    id: undefined,
    platformType: '',
    configName: '',
    accountIdentifier: '',
    status: 'ENABLED',
  })
  accountDialogVisible.value = true
}

const handleEditAccount = (row: CollectConfigVO) => {
  Object.assign(accountForm, {
    id: row.id,
    platformType: row.platformType || '',
    configName: row.configName,
    accountIdentifier: row.accountIdentifier || '',
    status: row.status || 'ENABLED',
  })
  accountDialogVisible.value = true
}

const handleSubmitAccount = async () => {
  if (!accountFormRef.value) return
  const valid = await accountFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      subType: 'account',
      platformType: accountForm.platformType,
      configName: accountForm.configName,
      accountIdentifier: accountForm.accountIdentifier,
      status: accountForm.status,
    }
    if (accountForm.id) {
      await externalCollectApi.update({ id: accountForm.id, ...payload })
      ElMessage.success('编辑成功')
    } else {
      await externalCollectApi.create(payload)
      ElMessage.success('新增成功')
    }
    accountDialogVisible.value = false
    await loadAccountList()
  } finally {
    submitLoading.value = false
  }
}

const handleToggleAccountStatus = async (row: CollectConfigVO) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}账号「${row.configName}」吗？`,
      '提示',
      { type: 'warning' },
    )
    await externalCollectApi.toggleStatus(row.id, newStatus)
    ElMessage.success('操作成功')
    await loadAccountList()
  } catch {}
}

const handleDeleteAccount = async (row: CollectConfigVO) => {
  try {
    await ElMessageBox.confirm(`确定删除账号「${row.configName}」吗？`, '删除确认', { type: 'warning' })
    await externalCollectApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadAccountList()
  } catch {}
}

const handleCreateKeyword = () => {
  Object.assign(keywordForm, {
    id: undefined,
    platform: '',
    keyword: '',
    matchType: 'FUZZY',
    status: 'ENABLED',
  })
  keywordDialogVisible.value = true
}

const handleEditKeyword = (row: KeywordConfigVO) => {
  Object.assign(keywordForm, {
    id: row.id,
    platform: row.platform,
    keyword: row.keyword,
    matchType: row.matchType,
    status: row.status,
  })
  keywordDialogVisible.value = true
}

const handleSubmitKeyword = async () => {
  if (!keywordFormRef.value) return
  const valid = await keywordFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      platform: keywordForm.platform,
      keyword: keywordForm.keyword,
      matchType: keywordForm.matchType,
      status: keywordForm.status,
    }
    if (keywordForm.id) {
      await externalCollectApi.keywordUpdate({ id: keywordForm.id, ...payload })
      ElMessage.success('编辑成功')
    } else {
      await externalCollectApi.keywordCreate(payload)
      ElMessage.success('新增成功')
    }
    keywordDialogVisible.value = false
    await loadKeywordList()
  } finally {
    submitLoading.value = false
  }
}

const handleToggleKeywordStatus = async (row: KeywordConfigVO) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ENABLED' ? '启用' : '停用'}关键词「${row.keyword}」吗？`,
      '提示',
      { type: 'warning' },
    )
    await externalCollectApi.keywordUpdate({ id: row.id, status: newStatus })
    ElMessage.success('操作成功')
    await loadKeywordList()
  } catch {}
}

const handleDeleteKeyword = async (row: KeywordConfigVO) => {
  try {
    await ElMessageBox.confirm(`确定删除关键词「${row.keyword}」吗？`, '删除确认', { type: 'warning' })
    await externalCollectApi.keywordDelete(row.id)
    ElMessage.success('删除成功')
    await loadKeywordList()
  } catch {}
}

const triggerImport = () => {
  fileInputRef.value?.click()
}

const handleImportFile = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 5MB')
    input.value = ''
    return
  }
  try {
    const content = await file.text()
    const result = await externalCollectApi.importCsv(content)
    const msg = `导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`
    if (result.failCount > 0 && result.failReasons?.length) {
      ElMessage.warning(`${msg}；${result.failReasons.slice(0, 3).join('；')}`)
    } else {
      ElMessage.success(msg)
    }
    await loadAccountList()
  } catch {
    ElMessage.error('导入失败')
  } finally {
    input.value = ''
  }
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
.external-collect-config {
  padding: 20px;
}
</style>
