<template>
  <div class="message-manage">
    <ContentWrap title="消息管理" subtitle="系统消息通知管理">
      <el-alert 
        title="管理系统发送的消息通知记录（支持邮件、短信、企业微信等渠道）" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 16px" 
      />
      
      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" style="margin-bottom: 16px">
        <el-tab-pane label="全部消息" name="all" />
        <el-tab-pane label="预警通知" name="alert" />
        <el-tab-pane label="系统通知" name="system" />
        <el-tab-pane label="业务通知" name="business" />
      </el-tabs>
      
      <!-- 搜索区 -->
      <TableSearch>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="消息标题">
            <el-input v-model="searchForm.title" placeholder="请输入" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="接收人">
            <el-input v-model="searchForm.receiver" placeholder="请输入" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="已发送" value="SENT" />
              <el-option label="发送失败" value="FAILED" />
              <el-option label="待发送" value="PENDING" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </TableSearch>

      <!-- 工具栏 -->
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          发送消息
        </el-button>
        <el-button 
          type="danger" 
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          删除选中
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table 
        :data="displayList" 
        border 
        stripe 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="title" label="消息标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="消息类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="channel" label="发送渠道" width="120" align="center">
          <template #default="{ row }">
            {{ getChannelLabel(row.channel) }}
          </template>
        </el-table-column>
        <el-table-column prop="receiver" label="接收人" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sendTime" label="发送时间" width="180" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </ContentWrap>

    <!-- 查看详情弹窗 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="消息详情"
      width="700px"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="消息标题">{{ viewData.title }}</el-descriptions-item>
        <el-descriptions-item label="消息类型">
          <el-tag :type="getCategoryTagType(viewData.category)">
            {{ getCategoryLabel(viewData.category) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发送渠道">{{ getChannelLabel(viewData.channel) }}</el-descriptions-item>
        <el-descriptions-item label="接收人">{{ viewData.receiver }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(viewData.status)">
            {{ getStatusLabel(viewData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="消息内容">
          <div style="white-space: pre-wrap; background: #f5f7fa; padding: 12px; border-radius: 4px;">
            {{ viewData.content }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ viewData.sendTime }}</el-descriptions-item>
        <el-descriptions-item label="失败原因" v-if="viewData.failReason">
          <el-text type="danger">{{ viewData.failReason }}</el-text>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 发送消息弹窗 -->
    <el-dialog
      v-model="sendDialogVisible"
      title="发送消息"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="sendFormRef"
        :model="sendFormData"
        :rules="sendRules"
        label-width="120px"
      >
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
        <el-form-item label="发送渠道" prop="channel">
          <el-checkbox-group v-model="sendFormData.channels">
            <el-checkbox label="EMAIL">邮件</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="WECHAT">企业微信</el-checkbox>
            <el-checkbox label="DINGTALK">钉钉</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="接收人" prop="receiver">
          <el-input 
            v-model="sendFormData.receiver" 
            type="textarea"
            :rows="2"
            placeholder="请输入接收人，多个用逗号分隔" 
          />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input 
            v-model="sendFormData.content" 
            type="textarea"
            :rows="6"
            placeholder="请输入消息内容" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendLoading" @click="handleSendSubmit">
          发送
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'

// ==================== 类型定义 ====================
interface MessageRecord {
  id: number
  title: string
  category: string
  channel: string
  receiver: string
  content: string
  status: string
  sendTime: string
  failReason?: string
}

// ==================== 响应式数据 ====================
const loading = ref(false)
const sendLoading = ref(false)
const viewDialogVisible = ref(false)
const sendDialogVisible = ref(false)
const sendFormRef = ref<FormInstance>()
const activeTab = ref('all')

const searchForm = reactive({
  title: '',
  receiver: '',
  status: ''
})

const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref<MessageRecord[]>([])

const viewData = reactive<Partial<MessageRecord>>({})

const sendFormData = reactive({
  title: '',
  category: '',
  channels: ['EMAIL'],
  receiver: '',
  content: ''
})

const sendRules: FormRules = {
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择消息类型', trigger: 'change' }
  ],
  receiver: [
    { required: true, message: '请输入接收人', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入消息内容', trigger: 'blur' }
  ]
}

// ==================== Mock 数据 ====================
const mockData: MessageRecord[] = [
  {
    id: 1,
    title: '【严重】播放量异常下跌预警',
    category: 'ALERT',
    channel: 'EMAIL, WECHAT',
    receiver: 'zhangsan@company.com, 13800138001',
    content: '检测到抖音账号「官方账号A」近3天播放量下降超过50%，当前日均播放量：5万，历史均值：12万。建议立即检查内容质量和发布策略。',
    status: 'SENT',
    sendTime: '2026-05-28 10:30:00'
  },
  {
    id: 2,
    title: '【警告】粉丝增长低于预期',
    category: 'ALERT',
    channel: 'EMAIL',
    receiver: 'lisi@company.com',
    content: '快手账号「品牌号B」本周粉丝增长仅+120人，低于设定阈值+500人。请优化内容策略或增加互动活动。',
    status: 'SENT',
    sendTime: '2026-05-28 09:00:00'
  },
  {
    id: 3,
    title: '系统维护通知',
    category: 'SYSTEM',
    channel: 'WECHAT, DINGTALK',
    receiver: 'all_users',
    content: '系统将于今晚23:00-01:00进行例行维护，期间部分功能可能暂时不可用，请提前保存工作内容。',
    status: 'SENT',
    sendTime: '2026-05-27 18:00:00'
  },
  {
    id: 4,
    title: '数据采集任务完成通知',
    category: 'BUSINESS',
    channel: 'EMAIL',
    receiver: 'admin@company.com',
    content: '今日数据采集任务已完成，共采集抖音数据1250条、快手数据890条、小红书数据560条。详细报告请查看数据分析模块。',
    status: 'SENT',
    sendTime: '2026-05-28 00:05:00'
  },
  {
    id: 5,
    title: 'AI内容生成审核通过',
    category: 'BUSINESS',
    channel: 'WECHAT',
    receiver: 'wangwu@company.com',
    content: '您提交的AI生成文案已通过自动审核，可以发布。文案ID：AI_20260528_001，预计阅读量：8000-12000。',
    status: 'SENT',
    sendTime: '2026-05-27 16:30:00'
  },
  {
    id: 6,
    title: '【提示】月度报告已生成',
    category: 'BUSINESS',
    channel: 'EMAIL',
    receiver: 'manager@company.com',
    content: '2026年4月运营月报已自动生成，包含各平台关键指标分析、趋势预测和优化建议。请点击链接查看完整报告。',
    status: 'SENT',
    sendTime: '2026-05-01 09:00:00'
  },
  {
    id: 7,
    title: '短信发送失败通知',
    category: 'SYSTEM',
    channel: 'EMAIL',
    receiver: 'admin@company.com',
    content: '短信网关连接超时，导致3条预警短信发送失败。已自动切换到备用通道重试。错误代码：SMS_TIMEOUT_001',
    status: 'FAILED',
    sendTime: '2026-05-28 08:15:00',
    failReason: '短信网关连接超时（30秒）'
  },
  {
    id: 8,
    title: '新租户注册欢迎消息',
    category: 'BUSINESS',
    channel: 'EMAIL, WECHAT',
    receiver: 'newuser@startup.com',
    content: '欢迎加入运营数据平台！您的账号已开通，初始配额：10个账号、30天试用期。如需升级请联系客服。',
    status: 'PENDING',
    sendTime: '-'
  }
]

// ==================== 计算属性 ====================
const displayList = computed(() => {
  let filtered = [...mockData]
  
  // 按Tab过滤
  if (activeTab.value === 'alert') {
    filtered = filtered.filter(item => item.category === 'ALERT')
  } else if (activeTab.value === 'system') {
    filtered = filtered.filter(item => item.category === 'SYSTEM')
  } else if (activeTab.value === 'business') {
    filtered = filtered.filter(item => item.category === 'BUSINESS')
  }
  
  if (searchForm.title) {
    filtered = filtered.filter(item => 
      item.title.includes(searchForm.title)
    )
  }
  if (searchForm.receiver) {
    filtered = filtered.filter(item => 
      item.receiver.includes(searchForm.receiver)
    )
  }
  if (searchForm.status) {
    filtered = filtered.filter(item => item.status === searchForm.status)
  }
  
  total.value = filtered.length
  
  const start = (pageNo.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

// ==================== 辅助函数 ====================
const getCategoryLabel = (category: string) => {
  const map: Record<string, string> = {
    ALERT: '预警通知',
    SYSTEM: '系统通知',
    BUSINESS: '业务通知'
  }
  return map[category] || category
}

const getCategoryTagType = (category: string) => {
  const map: Record<string, any> = {
    ALERT: 'danger',
    SYSTEM: 'info',
    BUSINESS: 'success'
  }
  return map[category] || ''
}

const getChannelLabel = (channel: string) => {
  const channels = channel.split(', ').map(c => {
    const map: Record<string, string> = {
      EMAIL: '邮件',
      SMS: '短信',
      WECHAT: '企业微信',
      DINGTALK: '钉钉'
    }
    return map[c] || c
  })
  return channels.join(', ')
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    SENT: '已发送',
    FAILED: '发送失败',
    PENDING: '待发送'
  }
  return map[status] || status
}

const getStatusTagType = (status: string) => {
  const map: Record<string, any> = {
    SENT: 'success',
    FAILED: 'danger',
    PENDING: 'warning'
  }
  return map[status] || ''
}

// ==================== 事件处理 ====================
const handleSearch = () => {
  pageNo.value = 1
  ElMessage.success('查询成功')
}

const handleReset = () => {
  searchForm.title = ''
  searchForm.receiver = ''
  searchForm.status = ''
  pageNo.value = 1
  ElMessage.info('已重置搜索条件')
}

const handleCreate = () => {
  Object.assign(sendFormData, {
    title: '',
    category: '',
    channels: ['EMAIL'],
    receiver: '',
    content: ''
  })
  sendDialogVisible.value = true
}

const handleView = (row: MessageRecord) => {
  Object.assign(viewData, row)
  viewDialogVisible.value = true
}

const handleSendSubmit = async () => {
  if (!sendFormRef.value) return
  
  await sendFormRef.value.validate((valid) => {
    if (valid) {
      sendLoading.value = true
      setTimeout(() => {
        sendLoading.value = false
        sendDialogVisible.value = false
        ElMessage.success('消息发送成功')
      }, 1000)
    }
  })
}

const handleDelete = (row: MessageRecord) => {
  ElMessageBox.confirm(
    `确定要删除消息「${row.title}」吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleSelectionChange = (selection: MessageRecord[]) => {
  selectedRows.value = selection
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 条消息吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('批量删除成功')
    selectedRows.value = []
  }).catch(() => {})
}

const handlePageChange = (page: number) => {
  pageNo.value = page
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNo.value = 1
}

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 300)
})
</script>

<style scoped>
.message-manage {
  padding: 20px;
}
</style>
