<template>
  <div class="custom-query-page">
    <el-tabs v-model="pageTab" class="page-tabs">
      <!-- Tab 1: 自定义查询 -->
      <el-tab-pane label="自定义查询" name="builder">
        <el-card
          shadow="never"
          class="config-card"
          :class="{ 'is-collapsed': !configExpanded }"
        >
          <template #header>
            <div class="card-header">
              <div class="header-title collapsible-title" @click.stop="toggleConfig">
                <el-icon class="collapse-icon" :class="{ 'is-collapsed': !configExpanded }">
                  <ArrowDown />
                </el-icon>
                <span>查询配置</span>
              </div>
              <div class="header-actions">
                <el-button type="primary" :loading="executing" @click="executeInline">执行查询</el-button>
                <el-button type="success" @click="openSaveDialog">保存为我的查询</el-button>
              </div>
            </div>
          </template>
          <div v-show="configExpanded">
            <QueryBuilder v-model="builderConfig" v-model:sql-text="inlineSql" collapsible-conditions />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: 我的查询 -->
      <el-tab-pane label="我的查询" name="saved">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-header">
              <span>已保存查询</span>
              <span class="total-info">共 {{ total }} 条</span>
            </div>
          </template>

          <el-form :inline="true" class="filter-form">
            <el-form-item label="名称">
              <el-input v-model="filterName" placeholder="筛选名称" clearable style="width: 200px" @keyup.enter="loadList" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 140px" @change="loadList">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="queryList" v-loading="loading" stripe>
            <el-table-column prop="queryName" label="查询名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="creator" label="创建人" width="120" align="center">
              <template #default="{ row }">{{ row.creator || '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
                  {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="180" align="center">
              <template #default="{ row }">{{ row.updateTime || row.createTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="executeSaved(row)">执行</el-button>
                <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="publishById(row)">发布</el-button>
                <el-button link type="danger" @click="deleteById(row)">删除</el-button>
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
      </el-tab-pane>
    </el-tabs>

    <!-- 内联结果放在 page tabs 外，避免嵌套 el-tabs 导致结果区 tab 头不渲染 -->
    <QueryResultPanel
      v-if="pageTab === 'builder' && (inlineRows.length > 0 || inlineExecuted)"
      :key="inlineResultKey"
      :rows="inlineRows"
      :builder-config="inlineBuilderConfig"
      :sql-text="inlineSql"
      collapsible-conditions
    />

    <!-- 保存对话框 -->
    <el-dialog v-model="showSaveDialog" :title="saveDialogTitle" width="480px">
      <el-form :model="saveForm" label-width="90px">
        <el-form-item label="查询名称" required>
          <el-input v-model="saveForm.queryName" placeholder="1-50字符" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="saveForm.status" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑查询" width="960px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="查询名称" required>
          <el-input v-model="editForm.queryName" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 200px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
      </el-form>
      <QueryBuilder v-model="editBuilderConfig" v-model:sql-text="editSql" />
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdate">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 执行结果弹窗 -->
    <el-dialog
      v-model="showResultDialog"
      :title="resultDialogTitle"
      width="90%"
      top="4vh"
      destroy-on-close
      class="result-dialog"
    >
      <QueryResultPanel
        :rows="dialogRows"
        :builder-config="dialogBuilderConfig"
        :sql-text="dialogSql"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import QueryBuilder from '@/components/QueryBuilder.vue'
import QueryResultPanel from '@/components/QueryResultPanel.vue'
import {
  createCustomQuery,
  executeCustomQuery,
  getCustomQueryList,
  previewCustomQuery,
  publishCustomQuery,
  updateCustomQuery,
} from '@/api/custom-query'
import { createEmptyQueryConfig, type QueryBuilderConfig } from '@/constants/metricSchema'

interface CustomQueryItem {
  id: number
  queryName: string
  status: string
  sqlText: string
  paramsJson?: string
  creator?: string
  createTime?: string
  updateTime?: string
}

interface StoredParams {
  builder?: QueryBuilderConfig
}

const pageTab = ref('builder')
const configExpanded = ref(true)

function toggleConfig() {
  configExpanded.value = !configExpanded.value
}

const loading = ref(false)
const saving = ref(false)
const executing = ref(false)

const builderConfig = ref<QueryBuilderConfig>(createEmptyQueryConfig())
const inlineBuilderConfig = ref<QueryBuilderConfig>(createEmptyQueryConfig())
const inlineSql = ref('')
const inlineRows = ref<Record<string, unknown>[]>([])
const inlineExecuted = ref(false)
const inlineResultKey = ref(0)

const queryList = ref<CustomQueryItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const filterName = ref('')
const filterStatus = ref('')

const showSaveDialog = ref(false)
const saveMode = ref<'create' | 'from-inline'>('from-inline')
const saveForm = ref({ queryName: '', status: 'DRAFT' })

const showEditDialog = ref(false)
const editForm = ref({ id: 0, queryName: '', status: 'DRAFT' })
const editBuilderConfig = ref<QueryBuilderConfig>(createEmptyQueryConfig())
const editSql = ref('')

const showResultDialog = ref(false)
const resultDialogTitle = ref('查询结果')
const dialogRows = ref<Record<string, unknown>[]>([])
const dialogBuilderConfig = ref<QueryBuilderConfig | null>(null)
const dialogSql = ref('')

const saveDialogTitle = computed(() =>
  saveMode.value === 'from-inline' ? '保存为我的查询' : '新建查询',
)

function packParams(builder: QueryBuilderConfig): string {
  return JSON.stringify({ builder })
}

function unpackParams(paramsJson?: string): QueryBuilderConfig | null {
  if (!paramsJson) return null
  try {
    const parsed = JSON.parse(paramsJson) as StoredParams
    if (parsed.builder?.dataSource) {
      return { ...createEmptyQueryConfig(), ...parsed.builder }
    }
  } catch {
    /* ignore */
  }
  return null
}

function extractRows(data: unknown): Record<string, unknown>[] {
  const payload = (data as { data?: unknown })?.data ?? data
  const rows = (payload as { rows?: unknown })?.rows ?? []
  return Array.isArray(rows) ? rows as Record<string, unknown>[] : []
}

function snapshotBuilderConfig(source: QueryBuilderConfig): QueryBuilderConfig {
  return {
    ...source,
    displayFields: [...source.displayFields],
    groupByFields: [...source.groupByFields],
    joinTables: [...source.joinTables],
    conditions: source.conditions.map((c) => ({ ...c })),
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res: unknown = await getCustomQueryList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: filterStatus.value || undefined,
    })
    const data = (res as { data?: { list?: CustomQueryItem[]; records?: CustomQueryItem[]; total?: number } })?.data ?? res
    const list = (data as { list?: CustomQueryItem[] }).list
      ?? (data as { records?: CustomQueryItem[] }).records
      ?? []
    queryList.value = filterName.value
      ? list.filter((q) => (q.queryName || '').toLowerCase().includes(filterName.value.toLowerCase()))
      : list
    total.value = (data as { total?: number }).total ?? queryList.value.length
  } catch (e) {
    console.error('loadList failed', e)
    queryList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const executeInline = async () => {
  const sql = inlineSql.value?.trim()
  if (!sql) {
    ElMessage.warning('请先生成或填写 SQL')
    return
  }
  executing.value = true
  inlineExecuted.value = true
  inlineBuilderConfig.value = snapshotBuilderConfig(builderConfig.value)
  try {
    const res = await previewCustomQuery({ sqlText: sql })
    inlineRows.value = extractRows(res)
    inlineResultKey.value += 1
    configExpanded.value = false
    ElMessage.success('执行成功')
  } catch (e: unknown) {
    inlineRows.value = []
    ElMessage.error((e as Error)?.message || '执行失败')
  } finally {
    executing.value = false
  }
}

const executeSaved = async (row: CustomQueryItem) => {
  executing.value = true
  try {
    const res = await executeCustomQuery(row.id)
    dialogRows.value = extractRows(res)
    dialogBuilderConfig.value = unpackParams(row.paramsJson)
    dialogSql.value = row.sqlText
    resultDialogTitle.value = `查询结果 — ${row.queryName}`
    showResultDialog.value = true
    ElMessage.success('执行成功')
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '执行失败')
  } finally {
    executing.value = false
  }
}

const openSaveDialog = () => {
  if (!inlineSql.value?.trim()) {
    ElMessage.warning('请先生成 SQL 再保存')
    return
  }
  saveMode.value = 'from-inline'
  saveForm.value = { queryName: '', status: 'DRAFT' }
  showSaveDialog.value = true
}

const handleSave = async () => {
  if (!saveForm.value.queryName) {
    ElMessage.warning('请输入查询名称')
    return
  }
  saving.value = true
  try {
    await createCustomQuery({
      queryName: saveForm.value.queryName,
      status: saveForm.value.status,
      sqlText: inlineSql.value,
      params: packParams(builderConfig.value),
    })
    ElMessage.success('保存成功')
    showSaveDialog.value = false
    await loadList()
    pageTab.value = 'saved'
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const openEditDialog = (row: CustomQueryItem) => {
  editForm.value = { id: row.id, queryName: row.queryName, status: row.status || 'DRAFT' }
  editSql.value = row.sqlText
  editBuilderConfig.value = unpackParams(row.paramsJson) ?? createEmptyQueryConfig()
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!editForm.value.queryName || !editSql.value?.trim()) {
    ElMessage.warning('请填写查询名称和 SQL')
    return
  }
  saving.value = true
  try {
    await updateCustomQuery({
      id: editForm.value.id,
      queryName: editForm.value.queryName,
      status: editForm.value.status,
      sqlText: editSql.value,
      params: packParams(editBuilderConfig.value),
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadList()
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

const publishById = async (row: CustomQueryItem) => {
  try {
    await ElMessageBox.confirm(`确认发布查询「${row.queryName}」？`, '提示', { type: 'warning' })
    await publishCustomQuery(row.id)
    ElMessage.success('发布成功')
    loadList()
  } catch (e: unknown) {
    if (e !== 'cancel') ElMessage.error((e as Error)?.message || '发布失败')
  }
}

const deleteById = async (row: CustomQueryItem) => {
  try {
    await ElMessageBox.confirm(`确认删除查询「${row.queryName}」？`, '提示', { type: 'warning' })
    ElMessage.success('删除成功（演示，后端暂无 DELETE 端点）')
  } catch {
    /* cancelled */
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.custom-query-page { padding: 20px; }
.page-tabs > :deep(.el-tabs__header) { margin-bottom: 16px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-title { display: flex; align-items: center; }
.collapsible-title { cursor: pointer; user-select: none; }
.collapse-icon {
  margin-right: 6px;
  transition: transform 0.2s ease;
}
.collapse-icon.is-collapsed { transform: rotate(-90deg); }
.header-actions { display: flex; gap: 8px; }
.config-card.is-collapsed :deep(.el-card__body) { display: none; }
.config-card, .list-card { margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.filter-form { margin-bottom: 12px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.result-dialog :deep(.el-dialog__body) { max-height: 75vh; overflow-y: auto; }
</style>
