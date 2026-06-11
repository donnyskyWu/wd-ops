<template>
  <div class="login-log-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>登录日志</el-breadcrumb-item>
    </el-breadcrumb>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="用户名">
        <el-input v-model="searchForm.userName" placeholder="搜索用户" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="IP 地址">
        <el-input v-model="searchForm.ip" placeholder="搜索 IP" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
        </el-select>
      </el-form-item>
      <el-form-item label="登录时间">
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

    <ContentWrap title="登录日志">
      <el-table :data="tableList" border stripe v-loading="loading">
        <template #empty>
          <el-empty description="暂无登录日志" />
        </template>
        <el-table-column prop="userName" label="用户名" width="140" />
        <el-table-column prop="ip" label="IP 地址" width="140" />
        <el-table-column prop="userAgent" label="设备/浏览器" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="登录时间" width="180" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import { fetchLoginLogs, type LoginLogVO } from '@/api/system-log'

const loading = ref(false)
const tableList = ref<LoginLogVO[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchForm = reactive({
  userName: '',
  ip: '',
  status: '',
  dateRange: [] as string[],
})

async function loadData() {
  loading.value = true
  try {
    const res = await fetchLoginLogs({
      username: searchForm.userName || undefined,
      ip: searchForm.ip || undefined,
      status: searchForm.status || undefined,
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
  searchForm.ip = ''
  searchForm.status = ''
  searchForm.dateRange = []
  pageNo.value = 1
  loadData()
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
.login-log-page {
  padding: 20px;
}
</style>
