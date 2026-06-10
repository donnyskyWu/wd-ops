<template>
  <div class="custom-query-page">

    <el-card class="config-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>查询配置（SQL Builder 模式）</span>
          <div>
            <el-button type="primary" @click="executeSelected" :loading="executing" :disabled="!selectedId">执行选中</el-button>
            <el-button type="success" @click="openCreateDialog">新建查询</el-button>
          </div>
        </div>
      </template>

      <el-form :model="queryForm" label-width="100px">
        <el-form-item label="查询名称">
          <el-input v-model="queryForm.queryName" placeholder="1-50字符" maxlength="50" style="width: 300px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable style="width: 200px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="SQL 文本">
          <el-input
            v-model="queryForm.sqlText"
            type="textarea"
            :rows="6"
            placeholder="SELECT * FROM oa_content_daily WHERE stat_date BETWEEN ? AND ? LIMIT 100"
            spellcheck="false"
          />
          <div class="form-tip">⚠️ 仅允许 SELECT，禁止 DROP/UPDATE/DELETE/INSERT 等写操作</div>
        </el-form-item>
        <el-form-item label="参数 JSON">
          <el-input v-model="queryForm.params" type="textarea" :rows="2" placeholder='{"start":"2026-05-01","end":"2026-05-31"}' />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="result-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的查询</span>
          <span class="total-info">共 {{ total }} 条</span>
        </div>
      </template>

      <el-table :data="queryList" v-loading="loading" stripe @row-click="onSelectRow" highlight-current-row>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="queryName" label="查询名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sqlText" label="SQL" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="executeById(row)">执行</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click.stop="publishById(row)">发布</el-button>
            <el-button link type="danger" @click.stop="deleteById(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @update:current-page="(v) => { pageNum = v; loadList() }"
        @update:page-size="(v) => { pageSize = v; pageNum = 1; loadList() }"
      />
    </el-card>

    <el-card class="result-card" shadow="never" v-if="lastResult">
      <template #header>
        <div class="card-header">
          <span>最近执行结果</span>
          <el-button size="small" @click="handleExport">导出</el-button>
        </div>
      </template>
      <el-tabs v-model="resultTab">
        <el-tab-pane label="列表视图" name="list">
          <el-table :data="lastResultRows" stripe>
            <el-table-column v-for="col in lastResultColumns" :key="col" :prop="col" :label="col" />
          </el-table>
          <el-empty v-if="lastResultRows.length === 0" description="暂无数据" />
        </el-tab-pane>
        <el-tab-pane label="JSON 视图" name="json">
          <pre class="json-pre">{{ JSON.stringify(lastResult, null, 2) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="新建查询" width="700px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="查询名称">
          <el-input v-model="createForm.queryName" placeholder="1-50字符" maxlength="50" />
        </el-form-item>
        <el-form-item label="SQL 文本">
          <el-input v-model="createForm.sqlText" type="textarea" :rows="6" placeholder="SELECT ..." />
        </el-form-item>
        <el-form-item label="参数 JSON">
          <el-input v-model="createForm.params" type="textarea" :rows="2" placeholder='{"k":"v"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCustomQueryList, createCustomQuery, executeCustomQuery, publishCustomQuery } from '@/api/custom-query'
import { exportToExcel } from '@/utils'

interface CustomQueryItem {
  id: number
  queryName: string
  status: string
  sqlText: string
  paramsJson: string
  createTime?: string
}

const loading = ref(false)
const saving = ref(false)
const executing = ref(false)
const queryList = ref<CustomQueryItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const selectedId = ref<number | null>(null)
const showCreateDialog = ref(false)
const resultTab = ref('list')

const queryForm = reactive({
  queryName: '',
  status: '' as string,
  sqlText: '',
  params: '',
})

const createForm = reactive({
  queryName: '',
  sqlText: '',
  params: '',
})

const lastResult = ref<any>(null)
const lastResultRows = ref<any[]>([])
const lastResultColumns = ref<string[]>([])

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getCustomQueryList({ pageNum: pageNum.value, pageSize: pageSize.value, status: queryForm.status || undefined })
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? []
    let filtered = list
    if (queryForm.queryName) {
      const kw = queryForm.queryName.toLowerCase()
      filtered = list.filter((q: CustomQueryItem) => (q.queryName || '').toLowerCase().includes(kw))
    }
    queryList.value = filtered
    total.value = data?.total ?? filtered.length
  } catch (e) {
    console.error('loadList failed', e)
    queryList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const onSelectRow = (row: CustomQueryItem) => {
  selectedId.value = row.id
  queryForm.queryName = row.queryName
  queryForm.sqlText = row.sqlText
  queryForm.params = row.paramsJson || ''
}

const executeById = async (row: CustomQueryItem) => {
  executing.value = true
  try {
    const res: any = await executeCustomQuery(row.id)
    const data = res?.data ?? res
    lastResult.value = data
    const rows = data?.rows ?? []
    lastResultRows.value = Array.isArray(rows) ? rows : [rows]
    lastResultColumns.value = lastResultRows.value.length > 0
      ? Object.keys(lastResultRows.value[0])
      : []
    ElMessage.success('执行成功')
    await nextTick()
  } catch (e: any) {
    ElMessage.error(e?.message || '执行失败')
  } finally {
    executing.value = false
  }
}

const executeSelected = () => {
  if (!selectedId.value) return
  const row = queryList.value.find(q => q.id === selectedId.value)
  if (row) executeById(row)
}

const publishById = async (row: CustomQueryItem) => {
  try {
    await ElMessageBox.confirm(`确认发布查询"${row.queryName}"？发布后状态不可回退为草稿（演示）。`, '提示', { type: 'warning' })
    await publishCustomQuery(row.id)
    ElMessage.success('发布成功')
    loadList()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

const deleteById = async (row: CustomQueryItem) => {
  try {
    await ElMessageBox.confirm(`确认删除查询"${row.queryName}"？`, '提示', { type: 'warning' })
    ElMessage.success('删除成功（演示，后端暂无 DELETE 端点）')
  } catch {}
}

const openCreateDialog = () => {
  createForm.queryName = ''
  createForm.sqlText = 'SELECT * FROM oa_content_daily LIMIT 100'
  createForm.params = ''
  showCreateDialog.value = true
}

const handleSave = async () => {
  if (!createForm.queryName) {
    ElMessage.warning('请输入查询名称')
    return
  }
  if (!createForm.sqlText) {
    ElMessage.warning('请输入 SQL 文本')
    return
  }
  saving.value = true
  try {
    await createCustomQuery({
      queryName: createForm.queryName,
      status: 'DRAFT',
      sqlText: createForm.sqlText,
      params: createForm.params || undefined,
    })
    ElMessage.success('保存成功')
    showCreateDialog.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleExport = () => {
  if (!lastResultRows.value || lastResultRows.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const cols = lastResultColumns.value.map(c => ({ key: c, label: c }))
  exportToExcel(lastResultRows.value, cols, '自定义查询结果')
}

onMounted(() => loadList())
</script>

<style scoped>
.custom-query-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.config-card, .result-card { margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.form-tip { font-size: 12px; color: #e6a23c; margin-top: 4px; }
.json-pre { background: #f5f7fa; padding: 12px; border-radius: 4px; max-height: 400px; overflow: auto; font-size: 12px; }
</style>
