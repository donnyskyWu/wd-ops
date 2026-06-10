<template>
  <div class="perf-template-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item>绩效核算</el-breadcrumb-item>
      <el-breadcrumb-item>考核模板</el-breadcrumb-item>
    </el-breadcrumb>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="岗位">
        <el-select v-model="searchForm.position" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="运营组长" value="OPS_LEADER" />
          <el-option label="公众号运营" value="MP_OPS" />
          <el-option label="抖音运营" value="DOUYIN_OPS" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.isActive" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="生效" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增模板
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="templateList" v-loading="loading" stripe>
      <el-table-column prop="position" label="岗位" width="120" />
      <el-table-column prop="templateName" label="模板名称" min-width="180" />
      <el-table-column prop="itemCount" label="指标数量" width="100" align="center">
        <template #default="{ row }">{{ row.itemCount }}项</template>
      </el-table-column>
      <el-table-column prop="isActive" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'">
            {{ row.isActive ? '生效' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="140" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button
            v-if="!row.isActive"
            link
            type="success"
            @click="handleActivate(row)"
          >启用</el-button>
          <el-button
            v-else
            link
            type="warning"
            @click="handleDeactivate(row)"
          >停用</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" @close="handleDialogClose">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="formData.templateName" placeholder="请输入模板名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="适用岗位" prop="position">
          <el-select v-model="formData.position" placeholder="请选择岗位" style="width: 100%">
            <el-option label="运营组长" value="OPS_LEADER" />
            <el-option label="公众号运营" value="MP_OPS" />
            <el-option label="抖音运营" value="DOUYIN_OPS" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否立即生效" prop="isActive">
          <el-switch v-model="formData.isActive" />
          <span class="form-tip">（启用后同岗位旧模板自动停用）</span>
        </el-form-item>

        <el-divider>考核指标配置</el-divider>

        <el-button type="primary" size="small" @click="handleAddItem" style="margin-bottom: 12px">
          <el-icon><Plus /></el-icon>
          添加指标
        </el-button>

        <el-table :data="formData.items" border size="small">
          <el-table-column label="#" width="50" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="指标名称" min-width="140">
            <template #default="{ row }">
              <el-select v-model="row.metricId" placeholder="选择指标" @change="handleMetricChange(row)">
                <el-option
                  v-for="opt in metricOptions"
                  :key="opt.id"
                  :label="opt.metricName"
                  :value="opt.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="权重(%)" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.weight" :min="0" :max="100" :precision="2" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="计算规则" width="100">
            <template #default="{ row }">
              <el-tag :type="row.calcRule === 'auto' ? 'primary' : 'warning'" size="small">
                {{ row.calcRule === 'auto' ? '自动' : '人工' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评分标准" width="100">
            <template #default="{ row }">
              <el-button size="small" @click="handleConfigScore(row)">配置</el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="handleRemoveItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="weight-summary" :class="{ 'is-error': totalWeight !== 100 }">
          权重合计: <strong>{{ totalWeight }}%</strong>
          <span v-if="totalWeight !== 100" class="error-tip">⚠️ 必须等于100%</span>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="totalWeight !== 100">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 评分标准配置对话框 -->
    <el-dialog v-model="scoreDialogVisible" title="评分标准配置" width="600px">
      <el-table :data="currentScoreRanges" border size="small">
        <el-table-column label="区间" width="80" align="center">
          <template #default="{ $index }">区间{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="最低值">
          <template #default="{ row }">
            <el-input-number v-model="row.min" :min="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="最高值">
          <template #default="{ row }">
            <el-input-number v-model="row.max" :min="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="得分" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.score" :min="0" :max="100" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-select v-model="row.grade" size="small">
              <el-option label="S" value="S" />
              <el-option label="A" value="A" />
              <el-option label="B" value="B" />
              <el-option label="C" value="C" />
              <el-option label="D" value="D" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <el-button size="small" @click="addScoreRange" style="margin-top: 12px">添加区间</el-button>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveScore">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import {
  getTemplateList,
  createTemplate,
  updateTemplate,
  activateTemplate,
} from '@/api/perfTemplate'
import {
  getMetricOptions,
} from '@/api/metric'
import type { PerfTemplateListItem, PerfTemplateItem, ScoreRange } from '@/types/perfTemplate'
import { CalcRule, Grade } from '@/types/perfTemplate'

const loading = ref(false)
const templateList = ref<PerfTemplateListItem[]>([])
const total = ref(0)
const router = useRouter()

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  position: undefined as string | undefined,
  isActive: undefined as boolean | undefined,
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getTemplateList({
      position: searchForm.position,
      isActive: searchForm.isActive === undefined ? undefined : (searchForm.isActive ? 1 : 0),
      pageNum: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    templateList.value = (res.list || []).map((row: any) => ({
      id: row.id,
      position: row.position,
      templateName: row.templateName,
      itemCount: row.itemCount ?? 0,
      isActive: row.isActive === 1 || row.isActive === true,
      createdAt: row.createTime || row.createdAt || '',
    })) as PerfTemplateListItem[]
    total.value = res.total ?? 0
  } catch (e) {
    templateList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNo = 1
  loadList()
}

const handleReset = () => {
  searchForm.position = undefined
  searchForm.isActive = undefined
  handleSearch()
}

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增模板')
const formRef = ref()
const metricOptions = ref<any[]>([])

const loadMetricOptions = async () => {
  try {
    const list = await getMetricOptions()
    metricOptions.value = (list || []).map((m: any) => ({
      id: m.id,
      metricName: m.metricName,
      metricCode: m.metricCode,
      calcRule: m.metricType === 'MANUAL' ? CalcRule.MANUAL : CalcRule.AUTO,
      unit: m.unit || '',
    }))
  } catch {
    metricOptions.value = []
  }
}

const formData = reactive<any>({
  id: undefined as number | undefined,
  templateName: '',
  position: '',
  isActive: true,
  items: [] as any[],
})

const formRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  position: [{ required: true, message: '请选择岗位', trigger: 'change' }],
}

const totalWeight = computed(() => {
  return formData.items.reduce((sum, item) => sum + item.weight, 0)
})

const handleAdd = () => {
  dialogTitle.value = '新增模板'
  Object.assign(formData, {
    id: undefined,
    templateName: '',
    position: '',
    isActive: true,
    items: [],
  })
  dialogVisible.value = true
}

const handleEdit = (row: PerfTemplateListItem) => {
  router.push(`/perf/template/${row.id}`)
}

const handleAddItem = () => {
  formData.items.push({
    metricId: 0,
    metricName: '',
    metricCode: '',
    weight: 0,
    calcRule: CalcRule.AUTO,
    scoreStandard: { ranges: [] },
  })
}

const handleRemoveItem = (index: number) => {
  formData.items.splice(index, 1)
}

const handleMetricChange = (row: PerfTemplateItem) => {
  const opt = metricOptions.value.find(m => m.id === row.metricId)
  if (opt) {
    row.metricName = opt.metricName
    row.metricCode = opt.metricCode
    row.calcRule = opt.calcRule
    row.unit = opt.unit
  }
}

// 评分标准配置
const scoreDialogVisible = ref(false)
const currentItem = ref<PerfTemplateItem | null>(null)
const currentScoreRanges = ref<ScoreRange[]>([])

const handleConfigScore = (row: PerfTemplateItem) => {
  currentItem.value = row
  currentScoreRanges.value = row.scoreStandard.ranges.length > 0
    ? JSON.parse(JSON.stringify(row.scoreStandard.ranges))
    : [
        { min: 0, max: 60, score: 0, grade: Grade.D },
        { min: 60, max: 75, score: 60, grade: Grade.C },
        { min: 75, max: 85, score: 75, grade: Grade.B },
        { min: 85, max: 95, score: 85, grade: Grade.A },
        { min: 95, max: 9999, score: 100, grade: Grade.S },
      ]
  scoreDialogVisible.value = true
}

const addScoreRange = () => {
  currentScoreRanges.value.push({ min: 0, max: 0, score: 0, grade: Grade.D })
}

const handleSaveScore = () => {
  if (currentItem.value) {
    currentItem.value.scoreStandard.ranges = JSON.parse(JSON.stringify(currentScoreRanges.value))
  }
  scoreDialogVisible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid || totalWeight.value !== 100) return
    try {
      const payload = {
        id: formData.id,
        templateName: formData.templateName,
        position: formData.position,
        isActive: formData.isActive ? 1 : 0,
        items: formData.items.map((it: any) => ({
          metricId: it.metricId,
          weight: it.weight,
          calcRule: it.calcRule,
          scoreStandard: it.scoreStandard,
        })),
      }
      if (formData.id) {
        await updateTemplate(payload)
        ElMessage.success('更新成功')
      } else {
        await createTemplate(payload)
        ElMessage.success('保存成功')
      }
      dialogVisible.value = false
      loadList()
    } catch (e) {
      // 错误已由全局拦截器处理
    }
  })
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: undefined,
    templateName: '',
    position: '',
    isActive: true,
    items: [],
  })
}

const handleView = (row: PerfTemplateListItem) => {
  router.push(`/perf/template/${row.id}`)
}

const handleActivate = async (row: PerfTemplateListItem) => {
  try {
    await ElMessageBox.confirm('启用后将停用该岗位现有生效模板，确认操作？', '提示', { type: 'warning' })
    await activateTemplate(row.id)
    ElMessage.success('启用成功')
    loadList()
  } catch (e) {
    // 用户取消或请求失败
  }
}

const handleDeactivate = async (row: PerfTemplateListItem) => {
  // 后端目前只提供 activate，停用通过 update(isActive=0) 实现
  try {
    await updateTemplate({ id: row.id, isActive: 0 } as any)
    ElMessage.success('停用成功')
    loadList()
  } catch {}
}

const handleDelete = async (row: PerfTemplateListItem) => {
  try {
    await ElMessageBox.confirm('确认删除该模板？', '提示', { type: 'warning' })
    // 后端未提供删除端点，标记为软删除后续补
    ElMessage.warning('删除接口待后端支持')
    loadList()
  } catch {}
}

onMounted(() => {
  loadList()
  loadMetricOptions()
})
</script>

<style scoped>
.perf-template-page {
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

.weight-summary {
  margin-top: 16px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 4px;
  font-size: 14px;
}

.weight-summary.is-error {
  background: #fef0f0;
}

.error-tip {
  color: #f56c6c;
  margin-left: 8px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
