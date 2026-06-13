<template>
  <div class="company-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="公司名称">
        <el-input v-model="searchForm.keyword" placeholder="搜索公司名称" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_company_status" placeholder="全部" clearable />
      </el-form-item>
      <template #extra>
        <el-button type="success" :loading="exportLoading" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增公司
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="companyList" v-loading="loading" stripe>
      <el-table-column prop="name" label="公司名称" min-width="180" />
      <el-table-column prop="creditCode" label="统一信用代码" width="180" />
      <el-table-column prop="legalPerson" label="法人" width="100" />
      <el-table-column prop="mpCapacity" label="公众号容量" width="180" align="center">
        <template #default="{ row }">
          <el-tag :type="getCapacityType(row.registeredCount, row.standardCount)">
            {{ getCapacityText(row.registeredCount, row.standardCount) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="handleExpand(row)">扩容</el-button>
          <el-button link :type="row.status ? 'danger' : 'success'" @click="handleToggleStatus(row)">
            {{ row.status ? '停用' : '启用' }}
          </el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="统一信用代码" prop="creditCode">
          <el-input v-model="formData.creditCode" placeholder="18位统一信用代码" maxlength="18" />
        </el-form-item>
        <el-form-item label="行业">
          <DictSelect v-model="formData.industry" dict-type="dict_industry" placeholder="请选择" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="formData.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="法人姓名" prop="legalPerson">
          <el-input v-model="formData.legalPerson" placeholder="请输入法人姓名" />
        </el-form-item>
        <el-form-item label="公众号容量标准" prop="standardCount">
          <el-input-number v-model="formData.standardCount" :min="1" :max="100" />
          <span class="form-tip">（个）</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="true">启用</el-radio>
            <el-radio :value="false">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 扩容对话框 -->
    <el-dialog v-model="expandDialogVisible" title="扩容公众号容量" width="400px">
      <el-form :model="expandForm" label-width="120px">
        <el-form-item label="公司名称">
          <el-input :value="expandForm.companyName" disabled />
        </el-form-item>
        <el-form-item label="当前容量">
          <el-tag>{{ expandForm.registeredCount }} / {{ expandForm.standardCount }}</el-tag>
        </el-form-item>
        <el-form-item label="新容量标准" prop="newStandardCount">
          <el-input-number v-model="expandForm.newStandardCount" :min="expandForm.registeredCount + 1" :max="100" />
          <span class="form-tip">（个，必须大于当前容量）</span>
        </el-form-item>
        <el-form-item label="扩容原因">
          <el-input v-model="expandForm.reason" placeholder="请输入扩容原因" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="expandDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmExpand">确认扩容</el-button>
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
import DictSelect from '@/components/DictSelect.vue'
import { exportToExcel } from '@/utils'
import {
  createCompany,
  expandCompany,
  getCompanyPage,
  updateCompany,
  type CompanyVO,
} from '@/api/company'

const loading = ref(false)
const exportLoading = ref(false)
const companyList = ref<any[]>([])
const total = ref(0)
const router = useRouter()

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  status: undefined as string | undefined,
})

interface CompanyRow {
  id: number
  name: string
  creditCode: string
  industry?: string
  address?: string
  legalPerson?: string
  standardCount: number
  registeredCount: number
  status: boolean
}

const mapRow = (item: CompanyVO): CompanyRow => ({
  id: item.id,
  name: item.companyName,
  creditCode: item.creditCode,
  industry: item.industry,
  address: item.address,
  legalPerson: item.legalName,
  standardCount: item.mpCapacityStandard ?? 0,
  registeredCount: item.mpRegisteredCount ?? 0,
  status: item.status === 'ENABLED',
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getCompanyPage({
      companyName: searchForm.keyword,
      status: searchForm.status,
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    companyList.value = (res.list || []).map(mapRow)
    total.value = res.total ?? 0
  } catch {
    companyList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadList()
}

const handleReset = () => {
  searchForm.keyword = undefined
  searchForm.status = undefined
  handleSearch()
}

const buildListParams = (pageNo: number, pageSize: number) => ({
  companyName: searchForm.keyword,
  status: searchForm.status,
  pageNo,
  pageSize,
})

const fetchAllFilteredRows = async () => {
  const exportPageSize = 500
  const first = await getCompanyPage(buildListParams(1, exportPageSize))
  let rows = (first.list || []).map(mapRow)
  const totalCount = first.total ?? 0
  if (totalCount > exportPageSize) {
    const totalPages = Math.ceil(totalCount / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res = await getCompanyPage(buildListParams(page, exportPageSize))
      rows = rows.concat((res.list || []).map(mapRow))
    }
  }
  return rows
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const rows = await fetchAllFilteredRows()
    const exportData = rows.map((row) => ({
      name: row.name,
      creditCode: row.creditCode,
      legalPerson: row.legalPerson || '',
      mpCapacity: getCapacityText(row.registeredCount, row.standardCount),
      status: row.status ? '启用' : '停用',
    }))
    const columns = [
      { key: 'name', label: '公司名称' },
      { key: 'creditCode', label: '统一信用代码' },
      { key: 'legalPerson', label: '法人' },
      { key: 'mpCapacity', label: '公众号容量' },
      { key: 'status', label: '状态' },
    ]
    exportToExcel(exportData, columns, '公司管理')
  } catch {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

const getCapacityType = (registered: number, standard: number) => {
  const remaining = standard - registered
  if (remaining <= 0) return 'danger'
  if (remaining <= 2) return 'warning'
  return 'success'
}

const getCapacityText = (registered: number, standard: number) => {
  const remaining = standard - registered
  if (remaining <= 0) return `${registered}/${standard} 容量已满`
  if (remaining <= 2) return `${registered}/${standard} ⚠️容量不足`
  return `${registered}/${standard} 剩余${remaining}`
}

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增公司')
const formRef = ref()

const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  creditCode: '',
  industry: '',
  address: '',
  legalPerson: '',
  standardCount: 5,
  status: true,
})

const formRules = {
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一信用代码', trigger: 'blur' }],
  legalPerson: [{ required: true, message: '请输入法人姓名', trigger: 'blur' }],
  standardCount: [{ required: true, message: '请设置容量标准', trigger: 'blur' }],
}

const handleAdd = () => {
  dialogTitle.value = '新增公司'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑公司'
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    creditCode: row.creditCode,
    industry: row.industry,
    address: row.address,
    legalPerson: row.legalPerson,
    standardCount: row.standardCount,
    status: row.status,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const payload = {
        companyName: formData.name,
        creditCode: formData.creditCode,
        industry: formData.industry || undefined,
        address: formData.address || undefined,
        legalName: formData.legalPerson,
        mpCapacityStandard: formData.standardCount,
        status: formData.status ? 'ENABLED' : 'DISABLED',
      }
      if (formData.id) {
        await updateCompany({ id: formData.id, ...payload })
      } else {
        await createCompany(payload)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } catch {
      /* 错误已由 request 拦截器提示 */
    }
  })
}

const handleView = (row: any) => {
  router.push(`/company/${row.id}`)
}

// 扩容
const expandDialogVisible = ref(false)
const expandForm = reactive({
  companyId: 0,
  companyName: '',
  registeredCount: 0,
  standardCount: 0,
  newStandardCount: 0,
  reason: '业务扩容',
})

const handleExpand = (row: any) => {
  expandForm.companyId = row.id
  expandForm.companyName = row.name
  expandForm.registeredCount = row.registeredCount
  expandForm.standardCount = row.standardCount
  expandForm.newStandardCount = row.standardCount + 5
  expandDialogVisible.value = true
}

const handleConfirmExpand = async () => {
  if (expandForm.newStandardCount <= expandForm.standardCount) {
    ElMessage.warning('新容量必须大于当前容量')
    return
  }
  try {
    await expandCompany(expandForm.companyId, {
      newCapacity: expandForm.newStandardCount,
      reason: expandForm.reason || '业务扩容',
    })
    ElMessage.success('扩容成功')
    expandDialogVisible.value = false
    loadList()
  } catch {
    /* 错误已由 request 拦截器提示 */
  }
}

const handleToggleStatus = async (row: CompanyRow) => {
  const nextStatus = row.status ? 'DISABLED' : 'ENABLED'
  try {
    await updateCompany({ id: row.id, status: nextStatus })
    row.status = !row.status
    ElMessage.success(row.status ? '已启用' : '已停用')
  } catch {
    /* 错误已由 request 拦截器提示 */
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.company-page {
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
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
