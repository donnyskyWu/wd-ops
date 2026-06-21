<!--
  M10-AO-S-07: 私域桥接待审核队列
-->
<template>
  <div class="private-domain-bridge-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="审核状态">
        <DictSelect v-model="searchForm.reviewStatus" dict-type="dict_private_domain_review_status" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="来源类型">
        <DictSelect v-model="searchForm.sourceType" dict-type="dict_private_domain_identity_type" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="匹配方式">
        <DictSelect v-model="searchForm.matchMethod" dict-type="dict_private_domain_match_method" placeholder="全部" clearable />
      </el-form-item>
    </TableSearch>

    <ContentWrap title="私域桥接">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="来源" min-width="180">
          <template #default="{ row }">
            <DictSelect :model-value="row.sourceType" dict-type="dict_private_domain_identity_type" disabled style="display:inline-block;width:auto;margin-right:6px" />
            <span>{{ row.sourceLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="目标" min-width="180">
          <template #default="{ row }">
            <DictSelect :model-value="row.targetType" dict-type="dict_private_domain_identity_type" disabled style="display:inline-block;width:auto;margin-right:6px" />
            <span>{{ row.targetLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="matchMethod" label="匹配方式" width="110" align="center">
          <template #default="{ row }">
            <DictSelect :model-value="row.matchMethod" dict-type="dict_private_domain_match_method" disabled style="display:inline-block;width:auto" />
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="90" align="center" />
        <el-table-column prop="reviewStatus" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <DictSelect :model-value="row.reviewStatus" dict-type="dict_private_domain_review_status" disabled style="display:inline-block;width:auto" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.reviewStatus === 'PENDING'">
              <el-button link type="primary" @click="handleConfirm(row)">确认</el-button>
              <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
            </template>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNo"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </ContentWrap>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import { confirmPrivateDomainBridge, getPrivateDomainBridgePage, rejectPrivateDomainBridge } from '@/api/collect'

const loading = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({
  reviewStatus: 'PENDING',
  sourceType: '',
  matchMethod: ''
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPrivateDomainBridgePage({
      ...searchForm,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadData()
}

const handleReset = () => {
  searchForm.reviewStatus = 'PENDING'
  searchForm.sourceType = ''
  searchForm.matchMethod = ''
  handleSearch()
}

const handleConfirm = async (row: any) => {
  await ElMessageBox.confirm('确认通过该身份桥接关联？', '确认桥接')
  await confirmPrivateDomainBridge(row.id)
  ElMessage.success('已确认')
  loadData()
}

const handleReject = async (row: any) => {
  const { value } = await ElMessageBox.prompt('请输入驳回原因（可选）', '驳回桥接', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputPlaceholder: '误匹配等原因'
  })
  await rejectPrivateDomainBridge(row.id, value ? { reason: value } : undefined)
  ElMessage.success('已驳回')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.private-domain-bridge-page { padding: 20px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.text-muted { color: #909399; }
</style>
