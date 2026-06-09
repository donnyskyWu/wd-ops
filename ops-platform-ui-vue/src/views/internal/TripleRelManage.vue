<template>
  <div class="triple-rel-page">
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="4" v-for="item in statItems" :key="item.key">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value" :class="item.color">{{ stats[item.key] }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关联类型">
        <DictSelect v-model="searchForm.relationType" dict-type="dict_triple_rel_type" placeholder="请选择" clearable />
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增关联
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="wechatName" label="个人微信" width="200">
        <template #default="{ row }">{{ row.wechatName || '--' }}</template>
      </el-table-column>
      <el-table-column prop="videoName" label="视频号" width="160">
        <template #default="{ row }">{{ row.videoName || '--' }}</template>
      </el-table-column>
      <el-table-column prop="weworkName" label="企业微信" width="160">
        <template #default="{ row }">{{ row.weworkName || '--' }}</template>
      </el-table-column>
      <el-table-column prop="relationType" label="关联类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getRelTypeColor(row.relationType)">{{ getRelTypeName(row.relationType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '绑定' : '解绑' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" link type="danger" @click="handleUnbind(row)">解绑</el-button>
          <el-button v-else link type="primary" @click="handleRebind(row)">重新绑定</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :current-page="searchForm.pageNo"
      :page-size="searchForm.pageSize"
      :total="total"
      @update:current-page="(val) => searchForm.pageNo = val"
      @update:page-size="(val) => { searchForm.pageSize = val; loadList() }"
      @change="loadList"
    />

    <el-dialog v-model="dialogVisible" title="新增三方关联" width="520px">
      <el-form :model="formData" ref="formRef" :rules="formRules" label-width="100px">
        <el-form-item label="关联类型" prop="relationType">
          <DictSelect v-model="formData.relationType" dict-type="dict_triple_rel_type" />
        </el-form-item>
        <el-form-item v-if="needsWechat" label="个人微信" prop="personalWechatId">
          <el-select v-model="formData.personalWechatId" filterable placeholder="请选择个微" style="width: 100%">
            <el-option v-for="w in personalWechatOptions" :key="w.id"
              :label="`${w.accountName}(${w.wechatId})`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needsVideo" label="视频号" prop="videoAccountId">
          <AccountSelect v-model="formData.videoAccountId" platform-type="WECHAT_VIDEO" />
        </el-form-item>
        <el-form-item v-if="needsWework" label="企业微信" prop="weworkAccountId">
          <el-select v-model="formData.weworkAccountId" filterable placeholder="请选择企微" style="width: 100%">
            <el-option v-for="w in weworkOptions" :key="w.id"
              :label="`${w.accountName}(${w.corpId})`" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import {
  getTripleRelPage,
  getTripleRelStatistics,
  createTripleRel,
  unbindTripleRel,
  rebindTripleRel,
  type TripleRelVO,
  type TripleRelStatistics,
} from '@/api/triple-rel'
import { getPersonalWechatPage, getWeworkPage, type PersonalWechatVO, type WeworkVO } from '@/api/personal-account'

const loading = ref(false)
const list = ref<TripleRelVO[]>([])
const total = ref(0)

const stats = reactive<TripleRelStatistics>({
  totalBound: 0, fullTriple: 0, wechatVideo: 0, wechatWework: 0, videoWework: 0, unbound: 0,
})

const statItems = [
  { key: 'totalBound' as const, label: '总绑定数', color: '' },
  { key: 'fullTriple' as const, label: '完整三方', color: 'primary' },
  { key: 'wechatVideo' as const, label: '微信+视频', color: 'success' },
  { key: 'wechatWework' as const, label: '微信+企微', color: 'warning' },
  { key: 'videoWework' as const, label: '视频+企微', color: 'info' },
  { key: 'unbound' as const, label: '已解绑', color: 'danger' },
]

const searchForm = reactive({ pageNo: 1, pageSize: 20, relationType: undefined as string | undefined })

const personalWechatOptions = ref<PersonalWechatVO[]>([])
const weworkOptions = ref<WeworkVO[]>([])

const loadStats = async () => {
  const data = await getTripleRelStatistics()
  Object.assign(stats, data)
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getTripleRelPage({
      relationType: searchForm.relationType,
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { searchForm.pageNo = 1; loadList() }
const handleReset = () => { searchForm.relationType = undefined; handleSearch() }

const relTypeNameMap: Record<string, string> = {
  FULL_TRIPLE: '完整三方', WECHAT_VIDEO: '微信+视频', WECHAT_WEWORK: '微信+企微', VIDEO_WEWORK: '视频+企微',
}
const relTypeColorMap: Record<string, string> = {
  FULL_TRIPLE: 'primary', WECHAT_VIDEO: 'success', WECHAT_WEWORK: 'warning', VIDEO_WEWORK: 'info',
}
const getRelTypeName = (type: string) => relTypeNameMap[type] || type
const getRelTypeColor = (type: string) => relTypeColorMap[type] || ''

const dialogVisible = ref(false)
const formRef = ref<any>()
const formData = reactive({
  relationType: 'WECHAT_WEWORK',
  personalWechatId: undefined as number | undefined,
  videoAccountId: undefined as number | undefined,
  weworkAccountId: undefined as number | undefined,
})

const needsWechat = computed(() => ['FULL_TRIPLE', 'WECHAT_VIDEO', 'WECHAT_WEWORK'].includes(formData.relationType))
const needsVideo = computed(() => ['FULL_TRIPLE', 'WECHAT_VIDEO', 'VIDEO_WEWORK'].includes(formData.relationType))
const needsWework = computed(() => ['FULL_TRIPLE', 'WECHAT_WEWORK', 'VIDEO_WEWORK'].includes(formData.relationType))

const formRules = {
  relationType: [{ required: true, message: '请选择关联类型', trigger: 'change' }],
}

const loadOptions = async () => {
  const [pw, ww] = await Promise.all([
    getPersonalWechatPage({ pageNo: 1, pageSize: 100, status: 'ENABLED' }),
    getWeworkPage({ pageNo: 1, pageSize: 100, status: 'ENABLED' }),
  ])
  personalWechatOptions.value = pw.list
  weworkOptions.value = ww.list
}

const handleAdd = async () => {
  await loadOptions()
  Object.assign(formData, {
    relationType: 'WECHAT_WEWORK',
    personalWechatId: undefined,
    videoAccountId: undefined,
    weworkAccountId: undefined,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  await createTripleRel({
    relationType: formData.relationType,
    personalWechatId: formData.personalWechatId,
    weworkAccountId: formData.weworkAccountId,
    videoAccountIds: formData.videoAccountId ? [formData.videoAccountId] : undefined,
  })
  ElMessage.success('新增成功')
  dialogVisible.value = false
  loadList()
  loadStats()
}

const handleUnbind = async (row: TripleRelVO) => {
  await ElMessageBox.confirm('确认解绑该关联？', '提示', { type: 'warning' })
  await unbindTripleRel(row.id)
  ElMessage.success('解绑成功')
  loadList()
  loadStats()
}

const handleRebind = async (row: TripleRelVO) => {
  await rebindTripleRel(row.id)
  ElMessage.success('重新绑定成功')
  loadList()
  loadStats()
}

onMounted(() => { loadList(); loadStats() })
</script>

<style scoped>
.triple-rel-page { padding: 20px; }
.stats-cards { margin-bottom: 20px; }
.stat-item { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-value.primary { color: #409eff; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-value.info { color: #909399; }
.stat-value.danger { color: #f56c6c; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.action-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.total-info { color: #909399; font-size: 14px; }
</style>
