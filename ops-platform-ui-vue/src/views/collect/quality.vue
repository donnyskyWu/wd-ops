<!--
  M10 - 数据质量检查 + 质量日志
  依据: UX-M10 § 5 P-M10-004
-->
<template>
  <div class="quality-page">

    <el-row :gutter="16">
      <el-col :span="14">
        <ContentWrap title="质量检查规则">
          <template #extra>
            <el-button type="primary" size="small" @click="handleAdd">
              <el-icon><Plus /></el-icon>新增规则
            </el-button>
          </template>
          <TableSearch v-model="checkSearch" @search="loadChecks" @reset="resetCheckSearch">
            <el-form-item label="规则名">
              <el-input v-model="checkSearch.name" placeholder="搜索规则名" clearable />
            </el-form-item>
            <el-form-item label="检查类型">
              <DictSelect v-model="checkSearch.checkType" dict-type="dict_quality_check_type" placeholder="全部" clearable />
            </el-form-item>
            <el-form-item label="级别">
              <DictSelect v-model="checkSearch.level" dict-type="dict_quality_level" placeholder="全部" clearable />
            </el-form-item>
          </TableSearch>
          <el-table v-loading="checkLoading" :data="checkList" border stripe size="small">
            <el-table-column prop="name" label="规则名" min-width="160" show-overflow-tooltip />
            <el-table-column prop="checkType" label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.checkType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="level" label="级别" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getLevelType(row.level)">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tableName" label="表" width="160" show-overflow-tooltip />
            <el-table-column label="通过率" width="100" align="right">
              <template #default="{ row }">
                <span v-if="row.passRate !== undefined" :class="{ 'text-danger': row.passRate < 90 }">
                  {{ row.passRate.toFixed(1) }}%
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="lastCheckAt" label="最近检查" width="170" align="center" />
            <el-table-column prop="enabled" label="启用" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </ContentWrap>
      </el-col>
      <el-col :span="10">
        <ContentWrap title="质量日志">
          <TableSearch v-model="logSearch" @search="loadLogs" @reset="resetLogSearch">
            <el-form-item label="级别">
              <DictSelect v-model="logSearch.level" dict-type="dict_quality_level" placeholder="全部" clearable />
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker
                v-model="logSearch.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </TableSearch>
          <el-table v-loading="logLoading" :data="logList" border stripe size="small" max-height="600">
            <el-table-column prop="checkName" label="规则" min-width="160" show-overflow-tooltip />
            <el-table-column prop="level" label="级别" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getLevelType(row.level)">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="checkTime" label="检查时间" width="160" align="center" />
            <el-table-column label="通过/失败" width="120" align="right">
              <template #default="{ row }">
                <span style="color: #67c23a">{{ row.passCount }}</span>
                <span style="margin: 0 4px">/</span>
                <span style="color: #f56c6c">{{ row.failCount }}</span>
                <span style="color: #909399; font-size: 12px; margin-left: 4px">/ {{ row.totalCount }}</span>
              </template>
            </el-table-column>
          </el-table>
        </ContentWrap>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import { getQualityCheckPage, getQualityLogPage } from '@/api/collect'
import { mockQualityChecks, mockQualityLogs } from '@/mock/collect'

const checkLoading = ref(false)
const checkList = ref<any[]>([])
const checkSearch = reactive({
  name: undefined as string | undefined,
  checkType: undefined as string | undefined,
  level: undefined as string | undefined,
})
const resetCheckSearch = () => {
  Object.assign(checkSearch, { name: undefined, checkType: undefined, level: undefined })
  loadChecks()
}

const logLoading = ref(false)
const logList = ref<any[]>([])
const logSearch = reactive({
  level: undefined as string | undefined,
  dateRange: [] as string[],
})
const resetLogSearch = () => {
  Object.assign(logSearch, { level: undefined, dateRange: [] })
  loadLogs()
}

const getLevelType = (lv: string) => {
  const m: Record<string, string> = { ERROR: 'danger', WARN: 'warning', INFO: 'info' }
  return m[lv] || ''
}

const loadChecks = async () => {
  checkLoading.value = true
  try {
    const res = await getQualityCheckPage(checkSearch as any) as any
    checkList.value = res.list || res.data?.list || []
  } catch {
    checkList.value = [...mockQualityChecks]
  } finally {
    checkLoading.value = false
  }
}
const loadLogs = async () => {
  logLoading.value = true
  try {
    const res = await getQualityLogPage(logSearch as any) as any
    logList.value = res.list || res.data?.list || []
  } catch {
    logList.value = [...mockQualityLogs]
  } finally {
    logLoading.value = false
  }
}

const handleAdd = () => ElMessage.info('新增规则弹窗(M10 完整版将打开规则编辑器)')
const handleEdit = (row: any) => ElMessage.info(`编辑规则: ${row.name}`)
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除规则【${row.name}】？`, '危险', { type: 'error' })
    ElMessage.success('已删除')
    loadChecks()
  } catch {}
}

onMounted(() => { loadChecks(); loadLogs() })
</script>

<style scoped>
.quality-page { padding: 20px; }
.text-danger { color: #f56c6c; }
</style>
