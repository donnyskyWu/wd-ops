<template>
  <div class="log-manage">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>操作日志</el-breadcrumb-item>
    </el-breadcrumb>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="操作用户">
        <el-input v-model="searchForm.userName" placeholder="搜索用户" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="操作模块">
        <el-select v-model="searchForm.module" placeholder="全部" clearable style="width: 120px">
          <el-option label="系统" value="SYSTEM" />
          <el-option label="用户" value="USER" />
          <el-option label="账号" value="ACCOUNT" />
          <el-option label="内容" value="CONTENT" />
          <el-option label="财务" value="FINANCE" />
          <el-option label="配置" value="CONFIG" />
          <el-option label="分析" value="ANALYTICS" />
          <el-option label="报表" value="REPORT" />
          <el-option label="采集" value="COLLECT" />
        </el-select>
      </el-form-item>
      <el-form-item label="日志级别">
        <el-select v-model="searchForm.level" placeholder="全部" clearable style="width: 120px">
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作时间">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
    </TableSearch>

    <ContentWrap title="操作日志">
      <el-table :data="tableList" border stripe v-loading="loading">
        <template #empty>
          <el-empty description="暂无日志数据" />
        </template>
        <el-table-column prop="userName" label="操作用户" width="120" />
        <el-table-column prop="module" label="模块" width="120">
          <template #default="{ row }">{{ LOG_MODULE_LABEL[row.module] || row.module }}</template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="120" />
        <el-table-column prop="status" label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="操作内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :current-page="pageNo"
        :page-size="pageSize"
        :total="total"
        @update:current-page="handlePageChange"
        @update:page-size="handleSizeChange"
        @change="loadData"
      />
    </ContentWrap>

    <el-drawer v-model="detailVisible" title="操作日志详情" size="500px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="操作用户">{{ current.userName }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ LOG_MODULE_LABEL[current.module] || current.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ current.action }}</el-descriptions-item>
        <el-descriptions-item label="操作内容">{{ current.content }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ current.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre style="white-space: pre-wrap; word-break: break-all; font-size: 12px">{{ current.params || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果">
          <pre style="white-space: pre-wrap; word-break: break-all; font-size: 12px">{{ current.response || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ current.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ current.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import { fetchOperationLogs, LOG_MODULE_LABEL, type OperationLogVO } from '@/api/system-log'

const loading = ref(false)
const tableList = ref<OperationLogVO[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const detailVisible = ref(false)
const current = ref<OperationLogVO | null>(null)

const searchForm = reactive({
  userName: '',
  module: '',
  level: '',
  dateRange: [] as string[],
})

async function loadData() {
  loading.value = true
  try {
    const res = await fetchOperationLogs({
      username: searchForm.userName || undefined,
      module: searchForm.module || undefined,
      level: searchForm.level || undefined,
      startTime: searchForm.dateRange?.[0],
      endTime: searchForm.dateRange?.[1],
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    tableList.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNo.value = 1
  loadData()
}

function handleReset() {
  searchForm.userName = ''
  searchForm.module = ''
  searchForm.level = ''
  searchForm.dateRange = []
  pageNo.value = 1
  loadData()
}

function handleView(row: OperationLogVO) {
  current.value = row
  detailVisible.value = true
}

function handlePageChange(page: number) {
  pageNo.value = page
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
}

onMounted(loadData)
</script>

<style scoped>
.log-manage {
  padding: 20px;
}
</style>
