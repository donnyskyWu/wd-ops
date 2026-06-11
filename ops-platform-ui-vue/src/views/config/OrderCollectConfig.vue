<template>
  <div class="order-collect-config">
    <ContentWrap title="订单采集配置" subtitle="订单数据库连接与采集规则">
      <el-alert title="配置订单数据库连接，支持增量/全量采集模式" type="info" :closable="false" style="margin-bottom: 16px" />

      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="配置名称">
            <el-input v-model="searchForm.configName" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="状态">
            <DictSelect v-model="searchForm.status" dict-type="dict_config_status" style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </TableSearch>

      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreate">新增配置</el-button>
      </div>

      <el-table :data="configList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="configName" label="名称" min-width="120" />
        <el-table-column prop="dbHost" label="主机" width="130" />
        <el-table-column prop="dbPort" label="端口" width="70" />
        <el-table-column prop="dbName" label="数据库" width="120" />
        <el-table-column prop="tableName" label="表名" width="130" />
        <el-table-column prop="syncMode" label="采集模式" width="90" />
        <el-table-column prop="connStatus" label="连接状态" width="90" />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleTest(row)">连接测试</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination :total="total" :current-page="pageNo" :page-size="pageSize"
        @current-change="p => { pageNo = p; loadList() }" @size-change="s => { pageSize = s; loadList() }" />
    </ContentWrap>

    <el-dialog v-model="dialogVisible" :title="formData.id ? '编辑配置' : '新增配置'" width="640px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="configName"><el-input v-model="formData.configName" /></el-form-item>
        <el-form-item label="主机" prop="dbHost"><el-input v-model="formData.dbHost" /></el-form-item>
        <el-form-item label="端口" prop="dbPort"><el-input-number v-model="formData.dbPort" :min="1" :max="65535" /></el-form-item>
        <el-form-item label="数据库" prop="dbName"><el-input v-model="formData.dbName" /></el-form-item>
        <el-form-item label="用户名" prop="dbUsername"><el-input v-model="formData.dbUsername" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="formData.dbPassword" type="password" show-password /></el-form-item>
        <el-form-item label="表名"><el-input v-model="formData.tableName" placeholder="pay_all_order" /></el-form-item>
        <el-form-item label="采集模式">
          <DictSelect v-model="formData.syncMode" dict-type="dict_sync_mode" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" active-value="ENABLED" inactive-value="DISABLED" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import { orderCollectApi, type CollectConfigVO } from '@/api/config'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const configList = ref<CollectConfigVO[]>([])
const searchForm = reactive({ configName: '', status: '' })
const formData = reactive<Record<string, unknown>>({
  configName: '', dbHost: '', dbPort: 3306, dbName: '', dbUsername: '', dbPassword: '',
  tableName: 'pay_all_order', syncMode: 'INCREMENTAL', status: 'ENABLED',
})

const rules: FormRules = {
  configName: [{ required: true, message: '必填', trigger: 'blur' }],
  dbHost: [{ required: true, message: '必填', trigger: 'blur' }],
  dbName: [{ required: true, message: '必填', trigger: 'blur' }],
  dbUsername: [{ required: true, message: '必填', trigger: 'blur' }],
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await orderCollectApi.list({
      configName: searchForm.configName || undefined,
      status: searchForm.status || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    configList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pageNo.value = 1; loadList() }
const handleReset = () => { searchForm.configName = ''; searchForm.status = ''; handleSearch() }

const handleCreate = () => {
  Object.assign(formData, {
    id: undefined, configName: '', dbHost: '', dbPort: 3306, dbName: '', dbUsername: '',
    dbPassword: '', tableName: 'pay_all_order', syncMode: 'INCREMENTAL', status: 'ENABLED',
  })
  dialogVisible.value = true
}

const handleEdit = (row: CollectConfigVO) => {
  Object.assign(formData, { ...row, dbPassword: '' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  if (!(await formRef.value.validate().catch(() => false))) return
  submitLoading.value = true
  try {
    const payload: Record<string, unknown> = { ...formData }
    if (!payload.dbPassword) delete payload.dbPassword
    if (formData.id) {
      await orderCollectApi.update(payload)
    } else {
      await orderCollectApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleTest = async (row: CollectConfigVO) => {
  const ok = await orderCollectApi.testConnection(row.id)
  ElMessage[ok ? 'success' : 'error'](ok ? '连接成功' : '连接失败')
  await loadList()
}

const handleDelete = async (row: CollectConfigVO) => {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await orderCollectApi.delete(row.id)
  ElMessage.success('已删除')
  await loadList()
}

onMounted(loadList)
</script>

<style scoped>.order-collect-config { padding: 20px; }</style>
