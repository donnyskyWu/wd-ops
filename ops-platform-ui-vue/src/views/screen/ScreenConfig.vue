<template>
  <div class="screen-config-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>Dashboard 配置列表</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建 Dashboard</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="dashboardName" label="名称" min-width="200" />
        <el-table-column prop="dashboardType" label="类型" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑布局</el-button>
            <el-button link type="danger" @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination :current-page="pageNum" :page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next"
        class="pagination" @update:current-page="(v) => { pageNum = v; loadList() }"
        @update:page-size="(v) => { pageSize = v; pageNum = 1; loadList() }" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="Dashboard 名称">
          <el-input v-model="formData.dashboardName" placeholder="1-50字符" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="formData.dashboardType" placeholder="请选择" style="width: 200px">
            <el-option label="运营 OPS" value="OPS" />
            <el-option label="财务 FIN" value="FIN" />
            <el-option label="绩效 PERF" value="PERF" />
            <el-option label="内容 PROD" value="PROD" />
          </el-select>
        </el-form-item>
        <el-form-item label="布局 JSON">
          <el-input v-model="formData.layout" type="textarea" :rows="6" placeholder='{"kpi":[{"k":"totalFollowers"}],"charts":[]}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getDashboardConfigList, createAnalyticsDashboard, updateDashboardConfig } from '@/api/dashboard'

interface DashboardVO {
  id: number
  dashboardName: string
  dashboardType: string
  status: number
  layout: string
}

const loading = ref(false)
const saving = ref(false)
const list = ref<DashboardVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const dialogTitle = ref('新建 Dashboard')
const isEdit = ref(false)
const formData = reactive({ id: undefined as number | undefined, dashboardName: '', dashboardType: 'OPS', layout: '{}' })

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getDashboardConfigList({ pageNum: pageNum.value, pageSize: pageSize.value })
    const data = res?.data ?? res
    list.value = data?.list ?? data?.records ?? []
    total.value = data?.total ?? list.value.length
  } catch (e) { console.error(e); list.value = [] }
  finally { loading.value = false }
}

const openCreate = () => {
  isEdit.value = false
  dialogTitle.value = '新建 Dashboard'
  Object.assign(formData, { id: undefined, dashboardName: '', dashboardType: 'OPS', layout: '{}' })
  dialogVisible.value = true
}

const openEdit = (row: DashboardVO) => {
  isEdit.value = true
  dialogTitle.value = `编辑 Dashboard #${row.id}`
  Object.assign(formData, { id: row.id, dashboardName: row.dashboardName, dashboardType: row.dashboardType, layout: row.layout || '{}' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.dashboardName) { ElMessage.warning('请输入名称'); return }
  saving.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateDashboardConfig({ id: formData.id, layout: formData.layout })
    } else {
      await createAnalyticsDashboard({ dashboardName: formData.dashboardName, dashboardType: formData.dashboardType, layout: formData.layout })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e: any) { ElMessage.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

const removeItem = async (row: DashboardVO) => {
  try {
    await ElMessageBox.confirm(`确认删除 Dashboard "${row.dashboardName}"？`, '提示', { type: 'warning' })
    ElMessage.success('删除成功（演示，后端暂无 DELETE 端点）')
  } catch {}
}

onMounted(() => loadList())
</script>

<style scoped>
.screen-config-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
