<template>
  <div class="metadata-manage">
    <ContentWrap title="元数据维护" subtitle="物理表映射与字段查询条件配置">
      <el-alert
        title="将数据库表映射为元数据实体，配置各字段的「指标查询条件类别」，供指标与自定义查询自动渲染筛选组件"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="实体名称">
            <el-input v-model="searchForm.entityName" placeholder="请输入" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="实体编码">
            <el-input v-model="searchForm.entityCode" placeholder="请输入" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item label="状态">
            <DictSelect
              v-model="searchForm.status"
              dict-type="dict_metadata_entity_status"
              placeholder="全部"
              clearable
              style="width: 120px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
            <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
          </el-form-item>
        </el-form>
      </TableSearch>

      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="openAddDialog"><el-icon><Plus /></el-icon>新增元数据</el-button>
      </div>

      <el-table :data="entityList" border stripe v-loading="loading">
        <el-table-column prop="entityCode" label="实体编码" width="140" />
        <el-table-column prop="entityName" label="实体名称" min-width="140" />
        <el-table-column prop="physicalTable" label="物理表" min-width="160" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_metadata_entity_status" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openFieldEditor(row)">字段维护</el-button>
            <el-button link type="primary" @click="openEditEntity(row)">编辑</el-button>
            <el-button v-hasPermi="'ROLE_OA_ADMIN'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        :current-page="pageNum"
        :page-size="pageSize"
        @update:current-page="(v: number) => (pageNum = v)"
        @update:page-size="(v: number) => (pageSize = v)"
        @change="loadList"
      />
    </ContentWrap>

    <!-- 新增：从未映射表选择 -->
    <el-dialog v-model="addDialogVisible" title="新增元数据实体" width="640px" :close-on-click-modal="false">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="120px">
        <el-form-item label="物理表" prop="physicalTable">
          <el-select
            v-model="addForm.physicalTable"
            filterable
            placeholder="选择未映射的数据库表"
            style="width: 100%"
            @change="onTableSelect"
          >
            <el-option
              v-for="t in unmappedTables"
              :key="t.tableName"
              :label="`${t.tableName} (${t.suggestedEntityName})`"
              :value="t.tableName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实体编码" prop="entityCode">
          <el-input v-model="addForm.entityCode" placeholder="小写字母开头，如 content" />
        </el-form-item>
        <el-form-item label="实体名称" prop="entityName">
          <el-input v-model="addForm.entityName" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="addForm.status" dict-type="dict_metadata_entity_status" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAddSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 编辑实体 -->
    <el-dialog v-model="editDialogVisible" title="编辑元数据实体" width="520px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" label-width="120px">
        <el-form-item label="实体编码">
          <el-input :model-value="editForm.entityCode" disabled />
        </el-form-item>
        <el-form-item label="物理表">
          <el-input :model-value="editForm.physicalTable" disabled />
        </el-form-item>
        <el-form-item label="实体名称" required>
          <el-input v-model="editForm.entityName" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="editForm.status" dict-type="dict_metadata_entity_status" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字段维护 -->
    <el-drawer v-model="fieldDrawerVisible" :title="`字段维护 — ${currentEntity?.entityName ?? ''}`" size="72%">
      <div v-loading="fieldLoading">
        <el-alert
          title="配置各字段的「指标查询条件类别」；枚举字典 / 平台选择须从字典类型列表中选择"
          type="info"
          :closable="false"
          style="margin-bottom: 12px"
        />
        <el-table :data="fieldList" border stripe>
          <el-table-column prop="fieldCode" label="字段编码" width="140" />
          <el-table-column prop="columnName" label="列名" width="140" />
          <el-table-column label="显示名称" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.fieldName" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="dataType" label="数据类型" width="100" />
          <el-table-column label="查询条件类别" min-width="180">
            <template #default="{ row }">
              <DictSelect
                v-model="row.queryConditionType"
                dict-type="dict_metadata_query_condition_type"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="字典类型" min-width="200">
            <template #default="{ row }">
              <el-select
                v-model="row.dictType"
                size="small"
                filterable
                clearable
                placeholder="请选择字典类型"
                :disabled="!needsDictType(row.queryConditionType)"
                :loading="dictTypeLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="dt in dictTypeOptions"
                  :key="dt.type"
                  :label="dt.name"
                  :value="dt.type"
                >
                  <span>{{ dt.name }}</span>
                  <span style="color: var(--el-text-color-secondary); font-size: 12px; margin-left: 8px">{{ dt.type }}</span>
                </el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.sort" size="small" :min="0" :max="9999" controls-position="right" />
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 16px; text-align: right">
          <el-button @click="fieldDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleFieldSave">保存字段配置</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import { fetchDictTypes, type DictTypeVO } from '@/api/dict'
import {
  fetchMetadataList,
  fetchMetadataDetail,
  fetchUnmappedTables,
  createMetadataEntity,
  updateMetadataEntity,
  updateMetadataFields,
  deleteMetadataEntity,
  type MetadataEntityVO,
  type MetadataFieldVO,
  type UnmappedTableVO,
} from '@/api/metadata'

const loading = ref(false)
const submitLoading = ref(false)
const fieldLoading = ref(false)
const entityList = ref<MetadataEntityVO[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchForm = reactive({ entityName: '', entityCode: '', status: '' })

const addDialogVisible = ref(false)
const editDialogVisible = ref(false)
const fieldDrawerVisible = ref(false)
const addFormRef = ref<FormInstance>()
const unmappedTables = ref<UnmappedTableVO[]>([])

const addForm = reactive({
  physicalTable: '',
  entityCode: '',
  entityName: '',
  status: 'ENABLED',
  remark: '',
})

const addRules: FormRules = {
  physicalTable: [{ required: true, message: '请选择物理表', trigger: 'change' }],
  entityCode: [
    { required: true, message: '请输入实体编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '须小写字母开头', trigger: 'blur' },
  ],
  entityName: [{ required: true, message: '请输入实体名称', trigger: 'blur' }],
}

const editForm = reactive<Partial<MetadataEntityVO>>({})

const currentEntity = ref<MetadataEntityVO | null>(null)
const fieldList = ref<MetadataFieldVO[]>([])
const dictTypeOptions = ref<DictTypeVO[]>([])
const dictTypeLoading = ref(false)

function needsDictType(queryConditionType: string | undefined) {
  return queryConditionType === 'DICT' || queryConditionType === 'PLATFORM_SELECT'
}

async function loadDictTypes() {
  if (dictTypeOptions.value.length > 0) return
  dictTypeLoading.value = true
  try {
    const res = await fetchDictTypes()
    dictTypeOptions.value = (res.list ?? []).filter((d) => d.status === 'ENABLED')
  } finally {
    dictTypeLoading.value = false
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await fetchMetadataList({
      ...searchForm,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    entityList.value = res.list ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  searchForm.entityName = ''
  searchForm.entityCode = ''
  searchForm.status = ''
  handleSearch()
}

async function openAddDialog() {
  unmappedTables.value = await fetchUnmappedTables()
  addForm.physicalTable = ''
  addForm.entityCode = ''
  addForm.entityName = ''
  addForm.status = 'ENABLED'
  addForm.remark = ''
  addDialogVisible.value = true
}

function onTableSelect(tableName: string) {
  const found = unmappedTables.value.find((t) => t.tableName === tableName)
  if (found) {
    addForm.entityCode = found.suggestedEntityCode
    addForm.entityName = found.suggestedEntityName
  }
}

async function handleAddSubmit() {
  await addFormRef.value?.validate()
  submitLoading.value = true
  try {
    await createMetadataEntity({ ...addForm })
    ElMessage.success('创建成功，已自动导入表字段')
    addDialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function openEditEntity(row: MetadataEntityVO) {
  Object.assign(editForm, row)
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  if (!editForm.id) return
  submitLoading.value = true
  try {
    await updateMetadataEntity({
      id: editForm.id,
      entityName: editForm.entityName,
      status: editForm.status,
      remark: editForm.remark,
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function openFieldEditor(row: MetadataEntityVO) {
  currentEntity.value = row
  fieldDrawerVisible.value = true
  fieldLoading.value = true
  await loadDictTypes()
  try {
    const detail = await fetchMetadataDetail(row.id)
    fieldList.value = (detail.fields ?? []).map((f) => ({ ...f }))
  } finally {
    fieldLoading.value = false
  }
}

async function handleFieldSave() {
  if (!currentEntity.value) return
  for (const f of fieldList.value) {
    if (needsDictType(f.queryConditionType) && !f.dictType) {
      ElMessage.warning(`字段 ${f.fieldCode} 须选择字典类型`)
      return
    }
  }
  submitLoading.value = true
  try {
    await updateMetadataFields(
      currentEntity.value.id,
      fieldList.value.map((f) => ({
        id: f.id,
        fieldName: f.fieldName,
        queryConditionType: f.queryConditionType,
        dictType: f.dictType,
        sort: f.sort,
      })),
    )
    ElMessage.success('字段配置已保存')
    fieldDrawerVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: MetadataEntityVO) {
  await ElMessageBox.confirm(`确定删除元数据「${row.entityName}」？此操作不可恢复。`, '超级管理员确认', {
    type: 'warning',
  })
  await deleteMetadataEntity(row.id)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>
