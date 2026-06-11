<template>
  <div class="message-manage">
    <ContentWrap title="消息管理" subtitle="系统消息通知管理">
      <el-tabs v-model="activeTab" style="margin-bottom: 16px" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="预警通知" name="ALERT" />
        <el-tab-pane label="系统通知" name="SYSTEM" />
        <el-tab-pane label="业务通知" name="BUSINESS" />
      </el-tabs>

      <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
        <el-form-item label="消息标题">
          <el-input v-model="searchForm.title" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="接收人">
          <el-input v-model="searchForm.receiver" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="待发送" value="PENDING" />
            <el-option label="已发送" value="SENT" />
            <el-option label="发送失败" value="FAILED" />
          </el-select>
        </el-form-item>
      </TableSearch>

      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="openSendDialog">
          <el-icon><Plus /></el-icon>
          发送消息
        </el-button>
      </div>

      <el-table :data="tableList" border stripe v-loading="loading">
        <el-table-column prop="title" label="消息标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="类型" width="120">
          <template #default="{ row }">{{ MESSAGE_CATEGORY_LABEL[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column prop="channel" label="渠道" width="160" show-overflow-tooltip />
        <el-table-column prop="receiver" label="接收人" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="MESSAGE_STATUS_TAG[row.status] || 'info'" size="small">
              {{ MESSAGE_STATUS_LABEL[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sendTime" label="发送时间" width="180" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @update:current-page="handlePageChange"
        @update:page-size="handleSizeChange"
        @change="loadData"
      />
    </ContentWrap>

    <el-dialog v-model="viewDialogVisible" title="消息详情" width="700px">
      <el-descriptions v-if="viewData.id" :column="1" border>
        <el-descriptions-item label="标题">{{ viewData.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ MESSAGE_CATEGORY_LABEL[viewData.category!] || viewData.category }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ viewData.channel }}</el-descriptions-item>
        <el-descriptions-item label="接收人">{{ viewData.receiver }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ viewData.content }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ MESSAGE_STATUS_LABEL[viewData.status!] || viewData.status }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ viewData.sendTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="sendDialogVisible" title="发送消息" width="700px" :close-on-click-modal="false">
      <el-form ref="sendFormRef" :model="sendFormData" :rules="sendRules" label-width="120px">
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="sendFormData.title" placeholder="请输入消息标题" />
        </el-form-item>
        <el-form-item label="消息类型" prop="category">
          <el-select v-model="sendFormData.category" placeholder="请选择消息类型" style="width: 100%">
            <el-option label="预警通知" value="ALERT" />
            <el-option label="系统通知" value="SYSTEM" />
            <el-option label="业务通知" value="BUSINESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送渠道" prop="channels">
          <el-checkbox-group v-model="sendFormData.channels">
            <el-checkbox label="EMAIL">邮件</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="WECHAT">企业微信</el-checkbox>
            <el-checkbox label="DINGTALK">钉钉</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="接收人" prop="receiver">
          <el-input v-model="sendFormData.receiver" type="textarea" :rows="2" placeholder="多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="sendFormData.content" type="textarea" :rows="6" placeholder="请输入消息内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendLoading" @click="handleSendSubmit">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import {
  fetchMessageList,
  getMessage,
  sendMessage,
  deleteMessage,
  MESSAGE_CATEGORY_LABEL,
  MESSAGE_STATUS_LABEL,
  MESSAGE_STATUS_TAG,
  type MessageVO,
} from '@/api/system-message'

const loading = ref(false)
const sendLoading = ref(false)
const viewDialogVisible = ref(false)
const sendDialogVisible = ref(false)
const sendFormRef = ref<FormInstance>()
const activeTab = ref('all')
const tableList = ref<MessageVO[]>([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const viewData = reactive<Partial<MessageVO>>({})

const searchForm = reactive({ title: '', receiver: '', status: '' })
const sendFormData = reactive({
  title: '',
  category: 'SYSTEM',
  channels: ['EMAIL'] as string[],
  receiver: '',
  content: '',
})

const sendRules: FormRules = {
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  receiver: [{ required: true, message: '请输入接收人', trigger: 'blur' }],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchMessageList({
      title: searchForm.title || undefined,
      receiver: searchForm.receiver || undefined,
      status: searchForm.status || undefined,
      category: activeTab.value === 'all' ? undefined : activeTab.value,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    tableList.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  pageNo.value = 1
  loadData()
}

function handleSearch() {
  pageNo.value = 1
  loadData()
}

function handleReset() {
  searchForm.title = ''
  searchForm.receiver = ''
  searchForm.status = ''
  pageNo.value = 1
  loadData()
}

function openSendDialog() {
  Object.assign(sendFormData, {
    title: '',
    category: 'SYSTEM',
    channels: ['EMAIL'],
    receiver: '',
    content: '',
  })
  sendDialogVisible.value = true
}

async function handleView(row: MessageVO) {
  const detail = await getMessage(row.id)
  Object.assign(viewData, detail)
  viewDialogVisible.value = true
}

async function handleSendSubmit() {
  if (!sendFormRef.value) return
  await sendFormRef.value.validate(async (valid) => {
    if (!valid) return
    sendLoading.value = true
    try {
      await sendMessage({ ...sendFormData })
      ElMessage.success('发送成功（站内记录）')
      sendDialogVisible.value = false
      loadData()
    } finally {
      sendLoading.value = false
    }
  })
}

function handleDelete(row: MessageVO) {
  ElMessageBox.confirm(`确定删除消息「${row.title}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteMessage(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
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
.message-manage {
  padding: 20px;
}
</style>
