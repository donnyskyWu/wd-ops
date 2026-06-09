<template>
  <div class="metric-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="指标名称/编码">
        <el-input v-model="searchForm.keyword" placeholder="搜索指标" clearable />
      </el-form-item>
      <el-form-item label="指标类型">
        <el-select v-model="searchForm.metricType" placeholder="请选择" clearable style="width: 120px">
          <el-option label="基础" value="BASIC" />
          <el-option label="计算" value="CALCULATED" />
          <el-option label="派生" value="DERIVED" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增指标</el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="metricList" v-loading="loading" stripe>
      <el-table-column prop="metricName" label="指标名称" width="150" />
      <el-table-column prop="metricCode" label="指标编码" width="180" />
      <el-table-column prop="metricType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeColor(row.metricType)">{{ getTypeName(row.metricType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dataSource" label="数据源" width="150" />
      <el-table-column prop="unit" label="单位" width="80" align="center" />
      <el-table-column prop="formula" label="计算公式" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="100px">
        <el-form-item label="指标名称" prop="metricName">
          <el-input v-model="formData.metricName" placeholder="1-50字，全局唯一" maxlength="50" />
        </el-form-item>
        <el-form-item label="指标编码" prop="metricCode">
          <el-input v-model="formData.metricCode" placeholder="英文+下划线，全局唯一" />
        </el-form-item>
        <el-form-item label="指标类型" prop="metricType">
          <el-radio-group v-model="formData.metricType">
            <el-radio label="BASIC">基础</el-radio>
            <el-radio label="CALCULATED">计算</el-radio>
            <el-radio label="DERIVED">派生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据源" prop="dataSource">
          <el-select v-model="formData.dataSource" placeholder="请选择数据表" style="width: 100%">
            <el-option label="oa_follower" value="oa_follower" />
            <el-option label="oa_content" value="oa_content" />
            <el-option label="oa_order" value="oa_order" />
          </el-select>
        </el-form-item>
        <el-form-item label="计算公式">
          <el-input v-model="formData.formula" type="textarea" :rows="3" placeholder="SQL表达式" />
          <div class="form-tip">⚠️ 仅允许白名单中的表，禁止DROP/UPDATE/DELETE等操作</div>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="formData.unit" placeholder="如：人/元/%/次/分" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'

// ==================== 类型定义 ====================
interface MetricItem {
  id?: number
  metricName: string
  metricCode: string
  metricType: 'BASIC' | 'CALCULATED' | 'DERIVED'
  dataSource: string
  formula?: string
  unit?: string
}

// ==================== 响应式数据 ====================
const loading = ref(false)
const metricList = ref([
  { id: 1, metricName: '粉丝增长数', metricCode: 'FOLLOWER_GROWTH', metricType: 'BASIC', dataSource: 'oa_follower', unit: '人', formula: 'COUNT(follower_id)' },
  { id: 2, metricName: '内容发布率', metricCode: 'CONTENT_PUBLISH', metricType: 'CALCULATED', dataSource: 'oa_content', unit: '%', formula: 'COUNT(published) / COUNT(total) * 100' },
  { id: 3, metricName: '综合转化率', metricCode: 'CONV_RATE_CMPST', metricType: 'DERIVED', dataSource: '计算公式', unit: '%', formula: 'CONV_RATE * ENGAGEMENT_RATE' },
])
const total = ref(3)

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  metricType: undefined as string | undefined,
})

const loadList = () => { loading.value = false }
const handleSearch = () => { searchForm.pageNo = 1; loadList() }
const handleReset = () => { searchForm.keyword = undefined; searchForm.metricType = undefined; handleSearch() }

const getTypeName = (type: string) => {
  const map: Record<string, string> = { BASIC: '基础', CALCULATED: '计算', DERIVED: '派生' }
  return map[type] || type
}

const getTypeColor = (type: string) => {
  const map: Record<string, string> = { BASIC: '', CALCULATED: 'warning', DERIVED: 'danger' }
  return map[type] || ''
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增指标')
const formRef = ref()
const formData = reactive({
  id: undefined,
  metricName: '',
  metricCode: '',
  metricType: 'BASIC',
  dataSource: '',
  formula: '',
  unit: '',
})

const formRules = {
  metricName: [
    { required: true, message: '请输入指标名称', trigger: 'blur' },
    { max: 50, message: '指标名称不超过 50 字', trigger: 'blur' },
  ],
  metricCode: [
    { required: true, message: '请输入指标编码', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]*$/, message: '编码需以字母开头，仅含字母数字下划线', trigger: 'blur' },
  ],
  metricType: [{ required: true, message: '请选择指标类型', trigger: 'change' }],
  dataSource: [{ required: true, message: '请选择数据源', trigger: 'change' }],
}

const handleAdd = () => { dialogTitle.value = '新增指标'; dialogVisible.value = true }
const handleEdit = (row: MetricItem) => {
  dialogTitle.value = '编辑指标'
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadList()
}
const handleView = () => ElMessage.info('查看详情功能开发中')
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确认删除该指标？', '提示', { type: 'warning' })
    ElMessage.success('删除成功')
    loadList()
  } catch {}
}

onMounted(() => loadList())
</script>

<style scoped>
.metric-page { padding: 20px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.form-tip { font-size: 12px; color: #e6a23c; margin-top: 4px; }
</style>
