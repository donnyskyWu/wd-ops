<template>
  <div class="personal-account-page">
    <el-tabs v-model="activePlatform" @tab-change="handleTabChange" class="platform-tabs">
      <el-tab-pane label="企业微信" name="WEWORK" />
      <el-tab-pane label="个人微信" name="PERSONAL_WX" />
    </el-tabs>

    <TableSearch v-if="activePlatform === 'PERSONAL_WX'" v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="账号名称">
        <el-input v-model="searchForm.accountName" placeholder="请输入账号名称" clearable />
      </el-form-item>
      <el-form-item label="微信号">
        <el-input v-model="searchForm.wechatId" placeholder="请输入微信号" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_status_enabled" placeholder="全部" clearable />
      </el-form-item>
      <template #extra>
        <el-button type="success" :loading="exportLoading" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </TableSearch>

    <div v-if="activePlatform === 'PERSONAL_WX'" class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增账号
      </el-button>
      <el-button type="warning" :loading="syncLoading" @click="handleSyncDevices">
        同步个微设备
      </el-button>
      <span class="total-info">共 {{ pagination.total }} 条</span>
    </div>

    <!-- 企微：应用配置 + 员工账号 -->
    <template v-if="activePlatform === 'WEWORK'">
      <WeworkAppConfigPanel class="wework-config-card" @config-change="handleWeworkConfigChange" />

      <div class="section-title">
        <span>员工企微账号</span>
        <div class="section-actions">
          <el-button type="success" size="small" :loading="exportLoading" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <el-button type="primary" size="small" :disabled="!weworkConfig" @click="handleAddEmployee">
            <el-icon><Plus /></el-icon>
            新增员工账号
          </el-button>
        </div>
      </div>
      <el-table :data="employeeList" v-loading="employeeLoading" border stripe>
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="weworkUserId" label="企微 ID" width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="position" label="岗位" width="120" />
        <el-table-column label="关联个微" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.linkedPersonalWechatName ? `${row.linkedPersonalWechatName} (${row.linkedWechatId || '-'})` : '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_status_enabled" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEditEmployee(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteEmployee(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!employeeLoading && employeeList.length === 0" description="暂无员工企微账号数据" :image-size="80" />
    </template>

    <!-- 个微列表 -->
    <template v-else>
      <el-table :data="personalList" v-loading="loading" border stripe>
      <el-table-column prop="accountName" label="微信名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="wechatId" label="微信号" width="160" />
      <el-table-column label="奥创设备" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.aochuangWechatAccountId || '--' }}
        </template>
      </el-table-column>
      <el-table-column label="绑定状态" width="100" align="center">
        <template #default="{ row }">
          <DictLabel v-if="row.aochuangBindStatus" dict-type="dict_aochuang_bind_status" :value="row.aochuangBindStatus" />
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column label="采集状态" width="100" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_collect_status" :value="row.collectStatus || 'PENDING'" />
        </template>
      </el-table-column>
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column label="关联企微" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.linkedWeworkEmployeeName ? `${row.linkedWeworkEmployeeName} (${row.linkedWeworkUserId || '-'})` : '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <DictLabel dict-type="dict_status_enabled" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="380" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleViewPersonal(row)">详情</el-button>
          <el-button
            v-if="row.aochuangWechatAccountId"
            link
            type="warning"
            :loading="friendSyncLoadingId === row.id"
            @click="handleSyncFriends(row)"
          >
            同步好友
          </el-button>
          <el-button
            v-if="row.aochuangWechatAccountId"
            link
            type="primary"
            @click="handleViewFriends(row)"
          >
            好友
          </el-button>
          <el-button
            v-if="row.aochuangWechatAccountId"
            link
            type="warning"
            :loading="messageSyncLoadingId === row.id"
            @click="handleSyncMessages(row)"
          >
            同步消息
          </el-button>
          <el-button
            v-if="row.aochuangWechatAccountId"
            link
            type="primary"
            @click="handleViewMessages(row)"
          >
            消息
          </el-button>
          <el-button link type="primary" @click="handleEditPersonal(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDeletePersonal(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </template>

    <Pagination
      v-if="activePlatform === 'PERSONAL_WX'"
      :current-page="pagination.pageNo"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @update:current-page="(val) => pagination.pageNo = val"
      @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
      @change="loadData"
    />

    <!-- 企微员工表单 -->
    <el-dialog v-model="employeeDialogVisible" :title="employeeDialogTitle" width="640px" destroy-on-close>
      <el-form :model="employeeForm" ref="employeeFormRef" :rules="employeeRules" label-width="120px" class="employee-form">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="employeeForm.nickname" maxlength="100" placeholder="请输入员工昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企微 ID" prop="weworkUserId">
              <el-input v-model="employeeForm.weworkUserId" maxlength="64" placeholder="企业微信 UserID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="employeeForm.phone" maxlength="11" placeholder="11 位手机号（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <DictSelect v-model="employeeForm.status" dict-type="dict_status_enabled" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">组织信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="employeeForm.department" maxlength="100" placeholder="所属部门" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-input v-model="employeeForm.position" maxlength="100" placeholder="岗位名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">关联个微</el-divider>
        <el-form-item label="关联个微">
          <el-select
            v-model="employeeForm.linkedPersonalWechatId"
            clearable
            filterable
            remote
            :remote-method="searchPersonalWechatOptions"
            :loading="personalWechatOptionsLoading"
            placeholder="选择关联的个人微信"
            style="width: 100%"
          >
            <el-option
              v-for="item in personalWechatOptions"
              :key="item.id"
              :label="`${item.accountName} (${item.wechatId})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="employeeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEmployee">保存</el-button>
      </template>
    </el-dialog>

    <!-- 个微表单 -->
    <el-dialog v-model="personalDialogVisible" :title="personalDialogTitle" width="560px">
      <el-form :model="personalForm" ref="personalFormRef" :rules="personalRules" label-width="100px">
        <el-form-item label="微信名" prop="accountName">
          <el-input v-model="personalForm.accountName" maxlength="100" />
        </el-form-item>
        <el-form-item label="微信号" prop="wechatId">
          <el-input v-model="personalForm.wechatId" maxlength="64" :disabled="!!personalForm.id" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input
            v-model="personalForm.contactPhone"
            placeholder="个微联系手机号（手动填写，非公司手机资产）"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="personalForm.status" dict-type="dict_status_enabled" style="width: 100%" />
        </el-form-item>
        <el-form-item label="关联企微">
          <el-select
            v-model="personalForm.linkedWeworkEmployeeId"
            clearable
            filterable
            remote
            :remote-method="searchWeworkEmployeeOptions"
            :loading="weworkEmployeeOptionsLoading"
            placeholder="选择关联的企微员工"
            style="width: 100%"
          >
            <el-option
              v-for="item in weworkEmployeeOptions"
              :key="item.id"
              :label="`${item.nickname} (${item.weworkUserId})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="personalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPersonal">保存</el-button>
      </template>
    </el-dialog>

    <!-- 个微详情 -->
    <el-drawer v-model="detailVisible" title="个微详情" size="480px">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="微信名">{{ detailData.accountName }}</el-descriptions-item>
        <el-descriptions-item label="微信号">{{ detailData.wechatId }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone || detailData.phoneNumberMasked || '--' }}</el-descriptions-item>
        <el-descriptions-item label="关联企微">
          {{ detailData.linkedWeworkEmployeeName
            ? `${detailData.linkedWeworkEmployeeName} (${detailData.linkedWeworkUserId || '-'})`
            : '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictLabel dict-type="dict_status_enabled" :value="detailData.status" />
        </el-descriptions-item>
        <el-descriptions-item label="奥创设备 ID">{{ detailData.aochuangWechatAccountId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="绑定状态">
          <DictLabel v-if="detailData.aochuangBindStatus" dict-type="dict_aochuang_bind_status" :value="detailData.aochuangBindStatus" />
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="采集状态">
          <DictLabel dict-type="dict_collect_status" :value="detailData.collectStatus || 'PENDING'" />
        </el-descriptions-item>
        <el-descriptions-item label="奥创昵称">{{ detailData.aochuangNickname || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近设备同步">{{ detailData.lastDeviceSyncAt || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近好友同步">{{ detailData.lastFriendSyncAt || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近消息同步">{{ detailData.lastMessageSyncAt || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 好友列表 -->
    <el-dialog v-model="friendsDialogVisible" :title="friendsDialogTitle" width="720px" destroy-on-close>
      <el-table :data="friendList" v-loading="friendsLoading" border stripe max-height="420">
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="wechatId" label="微信号" width="140" />
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column prop="syncedAt" label="同步时间" width="170" />
      </el-table>
      <div v-if="friendsPagination.total > friendsPagination.pageSize" class="friends-pagination">
        <el-pagination
          v-model:current-page="friendsPagination.pageNo"
          v-model:page-size="friendsPagination.pageSize"
          :total="friendsPagination.total"
          layout="total, prev, pager, next"
          @current-change="loadFriends"
        />
      </div>
      <template #footer>
        <el-button @click="friendsDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="friendSyncLoadingId === friendsPersonalId" @click="syncFriendsInDialog">
          同步好友
        </el-button>
      </template>
    </el-dialog>

    <!-- 消息列表 -->
    <el-dialog v-model="messagesDialogVisible" :title="messagesDialogTitle" width="820px" destroy-on-close>
      <el-table :data="messageList" v-loading="messagesLoading" border stripe max-height="420">
        <el-table-column prop="messageTime" label="时间" width="170" />
        <el-table-column prop="direction" label="方向" width="80" align="center">
          <template #default="{ row }">
            <DictLabel v-if="row.direction" dict-type="dict_aochuang_message_direction" :value="row.direction" />
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="msgType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <DictLabel v-if="row.msgType" dict-type="dict_aochuang_message_type" :value="row.msgType" />
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="aochuangFriendId" label="好友 ID" width="140" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      </el-table>
      <div v-if="messagesPagination.total > messagesPagination.pageSize" class="friends-pagination">
        <el-pagination
          v-model:current-page="messagesPagination.pageNo"
          v-model:page-size="messagesPagination.pageSize"
          :total="messagesPagination.total"
          layout="total, prev, pager, next"
          @current-change="loadMessages"
        />
      </div>
      <template #footer>
        <el-button @click="messagesDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="messageSyncLoadingId === messagesPersonalId" @click="syncMessagesInDialog">
          同步消息
        </el-button>
      </template>
    </el-dialog>

    <!-- 设备同步结果 -->
    <el-dialog v-model="syncDialogVisible" title="同步个微设备" width="880px" destroy-on-close>
      <el-alert
        v-if="syncResult"
        :title="`自动绑定 ${syncResult.autoBoundCount} 个，更新快照 ${syncResult.updatedSnapshotCount} 个，待绑定 ${syncResult.pendingCount} 个`"
        type="info"
        :closable="false"
        show-icon
        class="sync-summary"
      />
      <el-table v-if="syncResult?.pendingDevices?.length" :data="syncResult.pendingDevices" border stripe max-height="360">
        <el-table-column prop="nickname" label="奥创昵称" min-width="120" />
        <el-table-column prop="wechatId" label="微信号" width="140" />
        <el-table-column prop="aochuangWechatAccountId" label="设备 ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="aochuangAccountName" label="奥创账号" width="120" />
        <el-table-column label="建议匹配" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.fuzzyScore && row.fuzzyScore >= 0.6" type="warning">
              {{ (row.fuzzyScore * 100).toFixed(0) }}%
            </el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.suggestedPersonalWechatId"
              link
              type="primary"
              @click="handleBindPendingToExisting(row)"
            >
              绑定建议档案
            </el-button>
            <el-button link type="success" @click="openCreateBindDialog(row)">新建并绑定</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无待绑定设备" :image-size="80" />
      <template #footer>
        <el-button @click="syncDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleSyncDevices">重新同步</el-button>
      </template>
    </el-dialog>

    <!-- 新建并绑定 -->
    <el-dialog v-model="createBindDialogVisible" title="新建个微并绑定设备" width="480px" destroy-on-close>
      <el-form :model="createBindForm" ref="createBindFormRef" :rules="createBindRules" label-width="100px">
        <el-form-item label="微信名" prop="accountName">
          <el-input v-model="createBindForm.accountName" maxlength="100" />
        </el-form-item>
        <el-form-item label="微信号" prop="wechatId">
          <el-input v-model="createBindForm.wechatId" maxlength="64" />
        </el-form-item>
        <el-form-item label="设备 ID">
          <el-input v-model="createBindForm.aochuangWechatAccountId" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createBindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createBindLoading" @click="submitCreateBind">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import TableSearch from '@/components/TableSearch.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import DictLabel from '@/components/DictLabel.vue'
import WeworkAppConfigPanel from '@/components/WeworkAppConfigPanel.vue'
import { exportToExcel } from '@/utils'
import {
  getPersonalWechatPage,
  getPersonalWechat,
  createPersonalWechat,
  updatePersonalWechat,
  deletePersonalWechat,
  syncPersonalWechatDevices,
  bindPersonalWechatDevice,
  createAndBindPersonalWechat,
  syncPersonalWechatFriends,
  getPersonalWechatFriends,
  syncPersonalWechatMessages,
  getPersonalWechatMessages,
  getWeworkEmployeePage,
  createWeworkEmployee,
  updateWeworkEmployee,
  deleteWeworkEmployee,
  type PersonalWechatVO,
  type PersonalWechatSyncDevicesResult,
  type AochuangPendingDeviceVO,
  type AochuangFriendVO,
  type AochuangMessageVO,
  type WeworkVO,
  type WeworkEmployeeVO,
} from '@/api/personal-account'

type PlatformType = 'WEWORK' | 'PERSONAL_WX'

const activePlatform = ref<PlatformType>('WEWORK')
const loading = ref(false)
const syncLoading = ref(false)
const friendSyncLoadingId = ref<number | null>(null)
const messageSyncLoadingId = ref<number | null>(null)
const createBindLoading = ref(false)
const exportLoading = ref(false)
const employeeLoading = ref(false)
const personalList = ref<PersonalWechatVO[]>([])
const weworkConfig = ref<WeworkVO | null>(null)
const employeeList = ref<WeworkEmployeeVO[]>([])

const searchForm = reactive({
  accountName: '',
  wechatId: '',
  corpId: '',
  status: undefined as string | undefined,
})

const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const personalDialogVisible = ref(false)
const personalDialogTitle = ref('新增个微')
const personalFormRef = ref<any>()
const personalForm = reactive({
  id: undefined as number | undefined,
  accountName: '',
  wechatId: '',
  contactPhone: '',
  linkedWeworkEmployeeId: undefined as number | undefined,
  status: 'ENABLED',
})
const originalLinkedWeworkEmployeeId = ref<number | undefined>()
const personalRules = {
  accountName: [{ required: true, message: '请输入微信名', trigger: 'blur' }],
  wechatId: [{ required: true, message: '请输入微信号', trigger: 'blur' }],
  contactPhone: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (val && !/^1[3-9]\d{9}$/.test(val)) cb(new Error('联系电话格式不正确'))
      else cb()
    },
    trigger: 'blur',
  }],
}

const employeeDialogVisible = ref(false)
const employeeDialogTitle = ref('新增员工账号')
const employeeFormRef = ref<any>()
const employeeForm = reactive({
  id: undefined as number | undefined,
  nickname: '',
  weworkUserId: '',
  phone: '',
  department: '',
  position: '',
  linkedPersonalWechatId: undefined as number | undefined,
  status: 'ENABLED',
})
const originalLinkedPersonalWechatId = ref<number | undefined>()
const employeeRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  weworkUserId: [{ required: true, message: '请输入企微 ID', trigger: 'blur' }],
  phone: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (val && !/^1[3-9]\d{9}$/.test(val)) cb(new Error('手机号格式不正确'))
      else cb()
    },
    trigger: 'blur',
  }],
}

const detailVisible = ref(false)
const detailData = ref<PersonalWechatVO | null>(null)
const friendsDialogVisible = ref(false)
const friendsDialogTitle = ref('好友列表')
const friendsPersonalId = ref<number | null>(null)
const friendList = ref<AochuangFriendVO[]>([])
const friendsLoading = ref(false)
const friendsPagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const messagesDialogVisible = ref(false)
const messagesDialogTitle = ref('消息列表')
const messagesPersonalId = ref<number | null>(null)
const messageList = ref<AochuangMessageVO[]>([])
const messagesLoading = ref(false)
const messagesPagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const syncDialogVisible = ref(false)
const syncResult = ref<PersonalWechatSyncDevicesResult | null>(null)
const createBindDialogVisible = ref(false)
const createBindFormRef = ref<any>()
const createBindForm = reactive({
  accountName: '',
  wechatId: '',
  aochuangWechatAccountId: '',
  aochuangAccountRefId: undefined as number | undefined,
  aochuangNickname: '',
})
const createBindRules = {
  accountName: [{ required: true, message: '请输入微信名', trigger: 'blur' }],
  wechatId: [{ required: true, message: '请输入微信号', trigger: 'blur' }],
}
const weworkEmployeeOptions = ref<WeworkEmployeeVO[]>([])
const weworkEmployeeOptionsLoading = ref(false)
const personalWechatOptions = ref<PersonalWechatVO[]>([])
const personalWechatOptionsLoading = ref(false)

const searchWeworkEmployeeOptions = async (keyword = '') => {
  if (!weworkConfig.value) {
    weworkEmployeeOptions.value = []
    return
  }
  weworkEmployeeOptionsLoading.value = true
  try {
    const res = await getWeworkEmployeePage({
      weworkAccountId: weworkConfig.value.id,
      nickname: keyword || undefined,
      pageNo: 1,
      pageSize: 50,
    })
    weworkEmployeeOptions.value = res.list
  } finally {
    weworkEmployeeOptionsLoading.value = false
  }
}

const searchPersonalWechatOptions = async (keyword = '') => {
  personalWechatOptionsLoading.value = true
  try {
    const res = await getPersonalWechatPage({
      accountName: keyword || undefined,
      pageNo: 1,
      pageSize: 50,
    })
    personalWechatOptions.value = res.list
  } finally {
    personalWechatOptionsLoading.value = false
  }
}

const ensureWeworkEmployeeOption = (row?: WeworkEmployeeVO | PersonalWechatVO | null) => {
  if (!row || !('linkedWeworkEmployeeId' in row) || !row.linkedWeworkEmployeeId) return
  const exists = weworkEmployeeOptions.value.some((item) => item.id === row.linkedWeworkEmployeeId)
  if (!exists) {
    weworkEmployeeOptions.value.unshift({
      id: row.linkedWeworkEmployeeId,
      weworkAccountId: weworkConfig.value?.id || 0,
      nickname: row.linkedWeworkEmployeeName || `员工#${row.linkedWeworkEmployeeId}`,
      weworkUserId: row.linkedWeworkUserId || '',
      status: 'ENABLED',
    })
  }
}

const ensurePersonalWechatOption = (row?: WeworkEmployeeVO | PersonalWechatVO | null) => {
  if (!row || !('linkedPersonalWechatId' in row) || !row.linkedPersonalWechatId) return
  const exists = personalWechatOptions.value.some((item) => item.id === row.linkedPersonalWechatId)
  if (!exists) {
    personalWechatOptions.value.unshift({
      id: row.linkedPersonalWechatId,
      accountName: row.linkedPersonalWechatName || `个微#${row.linkedPersonalWechatId}`,
      wechatId: row.linkedWechatId || '',
      status: 'ENABLED',
    })
  }
}

const loadEmployees = async () => {
  if (!weworkConfig.value) {
    employeeList.value = []
    return
  }
  employeeLoading.value = true
  try {
    const res = await getWeworkEmployeePage({
      weworkAccountId: weworkConfig.value.id,
      pageNo: 1,
      pageSize: 100,
    })
    employeeList.value = res.list
  } finally {
    employeeLoading.value = false
  }
}

const handleWeworkConfigChange = (config: WeworkVO | null) => {
  weworkConfig.value = config
  loadEmployees()
}

const loadData = async () => {
  loading.value = true
  try {
    if (activePlatform.value === 'WEWORK') {
      pagination.total = weworkConfig.value ? 1 : 0
      await loadEmployees()
    } else {
      const res = await getPersonalWechatPage({
        accountName: searchForm.accountName || undefined,
        wechatId: searchForm.wechatId || undefined,
        status: searchForm.status,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize,
      })
      personalList.value = res.list
      pagination.total = res.total
    }
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  searchForm.accountName = ''
  searchForm.wechatId = ''
  searchForm.corpId = ''
  searchForm.status = undefined
  pagination.pageNo = 1
  loadData()
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => handleTabChange()

const buildPersonalListParams = (pageNo: number, pageSize: number) => ({
  accountName: searchForm.accountName || undefined,
  wechatId: searchForm.wechatId || undefined,
  status: searchForm.status,
  pageNo,
  pageSize,
})

const fetchAllPersonalRows = async () => {
  const exportPageSize = 500
  const first = await getPersonalWechatPage(buildPersonalListParams(1, exportPageSize))
  let rows = first.list || []
  const totalCount = first.total ?? 0
  if (totalCount > exportPageSize) {
    const totalPages = Math.ceil(totalCount / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res = await getPersonalWechatPage(buildPersonalListParams(page, exportPageSize))
      rows = rows.concat(res.list || [])
    }
  }
  return rows
}

const fetchAllEmployeeRows = async () => {
  if (!weworkConfig.value) return []
  const exportPageSize = 500
  const params = {
    weworkAccountId: weworkConfig.value.id,
    pageNo: 1,
    pageSize: exportPageSize,
  }
  const first = await getWeworkEmployeePage(params)
  let rows = first.list || []
  const totalCount = first.total ?? 0
  if (totalCount > exportPageSize) {
    const totalPages = Math.ceil(totalCount / exportPageSize)
    for (let page = 2; page <= totalPages; page += 1) {
      const res = await getWeworkEmployeePage({ ...params, pageNo: page })
      rows = rows.concat(res.list || [])
    }
  }
  return rows
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    if (activePlatform.value === 'WEWORK') {
      if (!weworkConfig.value) {
        ElMessage.warning('请先配置企业微信应用')
        return
      }
      const rows = await fetchAllEmployeeRows()
      const exportData = rows.map((row) => ({
        nickname: row.nickname,
        weworkUserId: row.weworkUserId,
        phone: row.phone || '',
        department: row.department || '',
        position: row.position || '',
        status: row.status === 'ENABLED' ? '正常' : '停用',
      }))
      const columns = [
        { key: 'nickname', label: '昵称' },
        { key: 'weworkUserId', label: '企微 ID' },
        { key: 'phone', label: '手机号' },
        { key: 'department', label: '部门' },
        { key: 'position', label: '岗位' },
        { key: 'status', label: '状态' },
      ]
      exportToExcel(exportData, columns, '个人账号管理')
    } else {
      const rows = await fetchAllPersonalRows()
      const exportData = rows.map((row) => ({
        accountName: row.accountName,
        wechatId: row.wechatId,
        contactPhone: row.contactPhone || '',
        status: row.status === 'ENABLED' ? '正常' : '停用',
        createTime: row.createTime || '',
      }))
      const columns = [
        { key: 'accountName', label: '微信名' },
        { key: 'wechatId', label: '微信号' },
        { key: 'contactPhone', label: '联系电话' },
        { key: 'status', label: '状态' },
        { key: 'createTime', label: '创建时间' },
      ]
      exportToExcel(exportData, columns, '个人账号管理')
    }
  } catch {
    ElMessage.error('导出失败，请重试')
  } finally {
    exportLoading.value = false
  }
}

const handleAdd = () => {
  if (activePlatform.value !== 'WEWORK') {
    personalDialogTitle.value = '新增个微'
    Object.assign(personalForm, {
      id: undefined,
      accountName: '',
      wechatId: '',
      contactPhone: '',
      linkedWeworkEmployeeId: undefined,
      status: 'ENABLED',
    })
    originalLinkedWeworkEmployeeId.value = undefined
    searchWeworkEmployeeOptions()
    personalDialogVisible.value = true
  }
}

const handleEditPersonal = (row: PersonalWechatVO) => {
  personalDialogTitle.value = '编辑个微'
  Object.assign(personalForm, {
    id: row.id,
    accountName: row.accountName,
    wechatId: row.wechatId,
    contactPhone: row.contactPhone || '',
    linkedWeworkEmployeeId: row.linkedWeworkEmployeeId,
    status: row.status,
  })
  originalLinkedWeworkEmployeeId.value = row.linkedWeworkEmployeeId
  searchWeworkEmployeeOptions()
  ensureWeworkEmployeeOption(row)
  personalDialogVisible.value = true
}

const handleAddEmployee = () => {
  if (!weworkConfig.value) {
    ElMessage.warning('请先配置企业微信应用')
    return
  }
  employeeDialogTitle.value = '新增员工账号'
  Object.assign(employeeForm, {
    id: undefined,
    nickname: '',
    weworkUserId: '',
    phone: '',
    department: '',
    position: '',
    linkedPersonalWechatId: undefined,
    status: 'ENABLED',
  })
  originalLinkedPersonalWechatId.value = undefined
  searchPersonalWechatOptions()
  employeeDialogVisible.value = true
}

const handleEditEmployee = (row: WeworkEmployeeVO) => {
  employeeDialogTitle.value = '编辑员工账号'
  Object.assign(employeeForm, {
    id: row.id,
    nickname: row.nickname,
    weworkUserId: row.weworkUserId,
    phone: row.phone || '',
    department: row.department || '',
    position: row.position || '',
    linkedPersonalWechatId: row.linkedPersonalWechatId,
    status: row.status,
  })
  originalLinkedPersonalWechatId.value = row.linkedPersonalWechatId
  searchPersonalWechatOptions()
  ensurePersonalWechatOption(row)
  employeeDialogVisible.value = true
}

const handleDeleteEmployee = async (row: WeworkEmployeeVO) => {
  await ElMessageBox.confirm(`确定删除员工「${row.nickname}」？`, '提示', { type: 'warning' })
  await deleteWeworkEmployee(row.id)
  ElMessage.success('删除成功')
  loadEmployees()
}

const submitEmployee = async () => {
  if (!employeeFormRef.value || !weworkConfig.value) return
  await employeeFormRef.value.validate()
  if (employeeForm.id) {
    const payload: Parameters<typeof updateWeworkEmployee>[0] = {
      id: employeeForm.id,
      nickname: employeeForm.nickname,
      weworkUserId: employeeForm.weworkUserId,
      phone: employeeForm.phone || undefined,
      department: employeeForm.department || undefined,
      position: employeeForm.position || undefined,
      status: employeeForm.status,
    }
    if (employeeForm.linkedPersonalWechatId != null) {
      payload.linkedPersonalWechatId = employeeForm.linkedPersonalWechatId
    } else if (originalLinkedPersonalWechatId.value != null) {
      payload.clearLinkedPersonalWechat = true
    }
    await updateWeworkEmployee(payload)
  } else {
    await createWeworkEmployee({
      weworkAccountId: weworkConfig.value.id,
      nickname: employeeForm.nickname,
      weworkUserId: employeeForm.weworkUserId,
      phone: employeeForm.phone || undefined,
      department: employeeForm.department || undefined,
      position: employeeForm.position || undefined,
      linkedPersonalWechatId: employeeForm.linkedPersonalWechatId,
      status: employeeForm.status,
    })
  }
  ElMessage.success('保存成功')
  employeeDialogVisible.value = false
  loadEmployees()
}

const handleViewPersonal = async (row: PersonalWechatVO) => {
  detailData.value = await getPersonalWechat(row.id)
  detailVisible.value = true
}

const handleSyncDevices = async () => {
  syncLoading.value = true
  try {
    syncResult.value = await syncPersonalWechatDevices()
    syncDialogVisible.value = true
    ElMessage.success('设备同步完成')
    loadData()
  } catch {
    ElMessage.error('设备同步失败')
  } finally {
    syncLoading.value = false
  }
}

const handleSyncFriends = async (row: PersonalWechatVO) => {
  friendSyncLoadingId.value = row.id
  try {
    const result = await syncPersonalWechatFriends(row.id, { fullSync: true })
    ElMessage.success(`好友同步完成：新增 ${result.createdCount}，更新 ${result.updatedCount}`)
    loadData()
  } catch {
    ElMessage.error('好友同步失败')
  } finally {
    friendSyncLoadingId.value = null
  }
}

const loadFriends = async () => {
  if (!friendsPersonalId.value) return
  friendsLoading.value = true
  try {
    const res = await getPersonalWechatFriends(friendsPersonalId.value, {
      pageNo: friendsPagination.pageNo,
      pageSize: friendsPagination.pageSize,
    })
    friendList.value = res.list
    friendsPagination.total = res.total
  } finally {
    friendsLoading.value = false
  }
}

const handleViewFriends = async (row: PersonalWechatVO) => {
  friendsPersonalId.value = row.id
  friendsDialogTitle.value = `好友列表 · ${row.accountName}`
  friendsPagination.pageNo = 1
  friendsDialogVisible.value = true
  await loadFriends()
}

const syncFriendsInDialog = async () => {
  if (!friendsPersonalId.value) return
  friendSyncLoadingId.value = friendsPersonalId.value
  try {
    const result = await syncPersonalWechatFriends(friendsPersonalId.value, { fullSync: true })
    ElMessage.success(`好友同步完成：新增 ${result.createdCount}，更新 ${result.updatedCount}`)
    await loadFriends()
    loadData()
  } catch {
    ElMessage.error('好友同步失败')
  } finally {
    friendSyncLoadingId.value = null
  }
}

const handleSyncMessages = async (row: PersonalWechatVO) => {
  messageSyncLoadingId.value = row.id
  try {
    const result = await syncPersonalWechatMessages(row.id, { fullSync: true })
    ElMessage.success(`消息同步完成：新增 ${result.createdCount}，更新 ${result.updatedCount}，日统计 ${result.dailyStatsDays} 天`)
    loadData()
  } catch {
    ElMessage.error('消息同步失败')
  } finally {
    messageSyncLoadingId.value = null
  }
}

const loadMessages = async () => {
  if (!messagesPersonalId.value) return
  messagesLoading.value = true
  try {
    const res = await getPersonalWechatMessages(messagesPersonalId.value, {
      pageNo: messagesPagination.pageNo,
      pageSize: messagesPagination.pageSize,
    })
    messageList.value = res.list
    messagesPagination.total = res.total
  } finally {
    messagesLoading.value = false
  }
}

const handleViewMessages = async (row: PersonalWechatVO) => {
  messagesPersonalId.value = row.id
  messagesDialogTitle.value = `消息列表 · ${row.accountName}`
  messagesPagination.pageNo = 1
  messagesDialogVisible.value = true
  await loadMessages()
}

const syncMessagesInDialog = async () => {
  if (!messagesPersonalId.value) return
  messageSyncLoadingId.value = messagesPersonalId.value
  try {
    const result = await syncPersonalWechatMessages(messagesPersonalId.value, { fullSync: true })
    ElMessage.success(`消息同步完成：新增 ${result.createdCount}，更新 ${result.updatedCount}，日统计 ${result.dailyStatsDays} 天`)
    await loadMessages()
    loadData()
  } catch {
    ElMessage.error('消息同步失败')
  } finally {
    messageSyncLoadingId.value = null
  }
}

const handleBindPendingToExisting = async (row: AochuangPendingDeviceVO) => {
  if (!row.suggestedPersonalWechatId || !row.aochuangAccountRefId) return
  await bindPersonalWechatDevice(row.suggestedPersonalWechatId, {
    aochuangWechatAccountId: row.aochuangWechatAccountId,
    aochuangAccountRefId: row.aochuangAccountRefId,
    bindStatus: 'MANUAL',
    aochuangNickname: row.nickname,
    aochuangAvatar: row.avatar,
    aochuangIsAlive: row.isAlive,
  })
  ElMessage.success('绑定成功')
  await handleSyncDevices()
}

const openCreateBindDialog = (row: AochuangPendingDeviceVO) => {
  createBindForm.accountName = row.nickname || ''
  createBindForm.wechatId = row.wechatId || row.alias || ''
  createBindForm.aochuangWechatAccountId = row.aochuangWechatAccountId
  createBindForm.aochuangAccountRefId = row.aochuangAccountRefId
  createBindForm.aochuangNickname = row.nickname || ''
  createBindDialogVisible.value = true
}

const submitCreateBind = async () => {
  if (!createBindFormRef.value || !createBindForm.aochuangAccountRefId) return
  await createBindFormRef.value.validate()
  createBindLoading.value = true
  try {
    await createAndBindPersonalWechat({
      accountName: createBindForm.accountName,
      wechatId: createBindForm.wechatId,
      aochuangWechatAccountId: createBindForm.aochuangWechatAccountId,
      aochuangAccountRefId: createBindForm.aochuangAccountRefId,
      aochuangNickname: createBindForm.aochuangNickname || undefined,
    })
    ElMessage.success('新建并绑定成功')
    createBindDialogVisible.value = false
    await handleSyncDevices()
  } finally {
    createBindLoading.value = false
  }
}

const submitPersonal = async () => {
  if (!personalFormRef.value) return
  await personalFormRef.value.validate()
  if (personalForm.id) {
    const payload: Parameters<typeof updatePersonalWechat>[0] = {
      id: personalForm.id,
      accountName: personalForm.accountName,
      wechatId: personalForm.wechatId,
      contactPhone: personalForm.contactPhone.trim(),
      status: personalForm.status,
    }
    if (personalForm.linkedWeworkEmployeeId != null) {
      payload.linkedWeworkEmployeeId = personalForm.linkedWeworkEmployeeId
    } else if (originalLinkedWeworkEmployeeId.value != null) {
      payload.clearLinkedWeworkEmployee = true
    }
    await updatePersonalWechat(payload)
  } else {
    await createPersonalWechat({
      accountName: personalForm.accountName,
      wechatId: personalForm.wechatId,
      contactPhone: personalForm.contactPhone.trim() || undefined,
      linkedWeworkEmployeeId: personalForm.linkedWeworkEmployeeId,
      status: personalForm.status,
    })
  }
  ElMessage.success('保存成功')
  personalDialogVisible.value = false
  loadData()
}

const handleDeletePersonal = async (row: PersonalWechatVO) => {
  await ElMessageBox.confirm(`确定删除个微「${row.accountName}」？`, '提示', { type: 'warning' })
  await deletePersonalWechat(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.personal-account-page {
  .platform-tabs { margin-bottom: 16px; }
  .wework-config-card { margin-bottom: 20px; }
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    font-weight: 600;
  }
  .section-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .action-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }
  .total-info { color: #909399; font-size: 14px; }
  .sync-summary { margin-bottom: 16px; }
  .friends-pagination { margin-top: 12px; display: flex; justify-content: flex-end; }
  .employee-form {
    :deep(.el-divider__text) { font-weight: 600; color: #303133; }
  }
}
</style>
