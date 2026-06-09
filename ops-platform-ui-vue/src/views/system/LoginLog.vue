<!--
  系统管理 - 登录日志
  依据: PRD-M9 § FR-M9-006 操作/登录日志
-->
<template>
  <div class="login-log-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>登录日志</el-breadcrumb-item>
    </el-breadcrumb>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="用户名">
        <el-input v-model="searchForm.username" placeholder="搜索用户名" clearable />
      </el-form-item>
      <el-form-item label="IP">
        <el-input v-model="searchForm.ip" placeholder="搜索 IP" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable>
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
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
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="ip" label="IP 地址" width="160" />
        <el-table-column prop="location" label="登录地" width="180" />
        <el-table-column prop="device" label="设备" width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="180" align="center" />
      </el-table>

      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => (pagination.pageNo = val)"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
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

const loading = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({
  username: undefined as string | undefined,
  ip: undefined as string | undefined,
  status: undefined as string | undefined,
  dateRange: [] as string[],
  pageNo: 1,
  pageSize: 20,
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const loadData = async () => {
  loading.value = true
  // TODO: GET /admin-api/system/login-log/page
  await new Promise((r) => setTimeout(r, 300))
  tableData.value = [
    { username: 'admin', realName: '系统管理员', ip: '192.168.1.100', location: '北京', device: 'Chrome 124 / Windows', status: 'SUCCESS', message: '登录成功', loginTime: '2026-06-08 10:00:00' },
    { username: 'operator1', realName: '张三', ip: '192.168.1.101', location: '上海', device: 'Edge 124 / macOS', status: 'SUCCESS', message: '登录成功', loginTime: '2026-06-08 09:30:00' },
    { username: 'unknown', realName: '-', ip: '10.0.0.5', location: '未知', device: 'curl/7.79.1', status: 'FAILED', message: '密码错误', loginTime: '2026-06-08 03:12:00' },
  ]
  total.value = tableData.value.length
  pagination.total = total.value
  loading.value = false
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, { username: undefined, ip: undefined, status: undefined, dateRange: [] })
  pagination.pageNo = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.login-log-page { padding: 20px; }
</style>
