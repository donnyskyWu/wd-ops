<!--
  系统管理 - 操作日志
  依据: PRD-M9 § FR-M9-006
-->
<template>
  <div class="log-manage">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>操作日志</el-breadcrumb-item>
    </el-breadcrumb>

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="操作用户">
        <el-input v-model="searchForm.userName" placeholder="搜索用户" clearable />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-input v-model="searchForm.action" placeholder="如：新增、修改、删除" clearable />
      </el-form-item>
      <el-form-item label="操作模块">
        <el-select v-model="searchForm.module" placeholder="全部" clearable>
          <el-option label="IP组" value="OA_IP_GROUP" />
          <el-option label="作者" value="OA_AUTHOR" />
          <el-option label="账号" value="OA_ACCOUNT" />
          <el-option label="内容" value="OA_CONTENT" />
          <el-option label="系统" value="SYSTEM" />
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
      <el-table :data="pagedData" border stripe v-loading="loading">
        <template #empty>
          <el-empty description="暂无日志数据" />
        </template>
        <el-table-column prop="userName" label="操作用户" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作类型" width="120" />
        <el-table-column prop="status" label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
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
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => (pagination.pageNo = val)"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="操作日志详情" size="500px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="操作用户">{{ current.userName }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ current.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ current.action }}</el-descriptions-item>
        <el-descriptions-item label="操作内容">{{ current.content }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ current.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre style="white-space: pre-wrap; word-break: break-all; font-size: 12px">{{ current.params || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果">
          <pre style="white-space: pre-wrap; word-break: break-all; font-size: 12px">{{ current.response || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ current.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ current.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'

interface LogItem {
  id: number
  userName: string
  module: string
  action: string
  content: string
  method?: string
  params?: string
  response?: string
  ip: string
  createTime: string
  status: 'success' | 'fail'
}

const loading = ref(false)
const logList = ref<LogItem[]>([])
const searchForm = reactive({
  userName: undefined as string | undefined,
  action: undefined as string | undefined,
  module: undefined as string | undefined,
  dateRange: [] as string[],
  pageNo: 1,
  pageSize: 20,
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })
const detailVisible = ref(false)
const current = ref<LogItem | null>(null)

const pagedData = computed(() => {
  const start = (pagination.pageNo - 1) * pagination.pageSize
  return logList.value.slice(start, start + pagination.pageSize)
})

const loadData = async () => {
  loading.value = true
  // TODO: GET /admin-api/system/operation-log/page
  await new Promise((r) => setTimeout(r, 300))
  logList.value = [
    { id: 1, userName: 'admin', module: 'IP组', action: '新增', content: '新增 IP 组 A-3', method: 'POST /oa/ip-group/create', params: '{"groupName":"A-3组"}', response: '{"code":0}', ip: '192.168.1.100', createTime: '2026-06-08 10:30:00', status: 'success' },
    { id: 2, userName: 'operator1', module: '账号', action: '修改', content: '修改 抖音-AI技术前沿 账号', method: 'PUT /oa/account/123', params: '{}', response: '{"code":0}', ip: '192.168.1.101', createTime: '2026-06-08 09:20:00', status: 'success' },
    { id: 3, userName: 'admin', module: '系统', action: '登录', content: '管理员登录', method: 'POST /system/auth/login', params: '{"username":"admin"}', response: '{"code":0}', ip: '192.168.1.100', createTime: '2026-06-08 09:00:00', status: 'success' },
    { id: 4, userName: 'editor1', module: '内容', action: '提交', content: '提交内容 C-20260608-001 待审核', method: 'POST /oa/content/submit', params: '{"contentId":1}', response: '{"code":0}', ip: '192.168.1.102', createTime: '2026-06-08 08:50:00', status: 'success' },
    { id: 5, userName: 'finance1', module: '财务', action: '审批通过', content: '审批通过 成本单 C20260608001', method: 'PUT /finance/cost/approve/1', params: '{"action":"approve"}', response: '{"code":0}', ip: '192.168.1.103', createTime: '2026-06-08 08:30:00', status: 'success' },
    { id: 6, userName: 'operator2', module: '账号', action: '删除', content: '删除 SIM 卡 89860000000000000099', method: 'DELETE /oa/sim-card/99', params: '{}', response: '{"code":500,"msg":"该卡仍有关联账号"}', ip: '192.168.1.104', createTime: '2026-06-07 18:45:00', status: 'fail' },
  ]
  pagination.total = logList.value.length
  loading.value = false
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, { userName: undefined, action: undefined, module: undefined, dateRange: [] })
  pagination.pageNo = 1
  loadData()
}
const handleView = (row: LogItem) => {
  current.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.log-manage { padding: 20px; }
</style>
