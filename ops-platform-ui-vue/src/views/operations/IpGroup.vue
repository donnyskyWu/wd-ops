<!--
  IP 组管理
  改造说明(2026-06-08): 从"树表 + 弹窗"重构为"左树 + 右 5 Tab"
  依据: UX-M1 § 3.1 IP 组管理布局 + API-M1 § 2
  5 Tab: 基本信息 / 成员管理 / 关联账号 / 关联主播 / 统计
  保护删除: ADR-M1-002 - 成员/账号/主播未迁移不允许删除 IP 组
-->
<template>
  <div class="ip-group-page">
    <div class="ip-group-layout">
      <!-- 左侧：IP 组树 -->
      <div class="ip-group-tree-panel">
        <div class="tree-header">
          <span class="title">IP 组结构</span>
          <el-button link type="primary" size="small" @click="handleCreateRoot">
            <el-icon><Plus /></el-icon>新建大组
          </el-button>
        </div>
        <div class="tree-search">
          <el-input
            v-model="treeSearchKeyword"
            placeholder="搜索组名"
            clearable
            size="small"
            :prefix-icon="Search"
          />
        </div>
        <el-scrollbar class="tree-scroll" v-loading="treeLoading">
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            node-key="id"
            highlight-current
            :filter-node-method="filterNode"
            :default-expand-all="false"
            empty-text="暂无 IP 组"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node-label">
                <el-icon v-if="data.groupType === 1" class="folder-icon"><Folder /></el-icon>
                <el-icon v-else class="sub-icon"><FolderOpened /></el-icon>
                <span class="name">{{ node.label }}</span>
                <span class="meta">
                  ({{ data.memberCount || 0 }}/{{ data.accountCount || 0 }}/{{ data.anchorCount || 0 }})
                </span>
                <el-tag v-if="data.status === 0" type="danger" size="small" effect="plain">停用</el-tag>
              </span>
            </template>
          </el-tree>
        </el-scrollbar>
      </div>

      <!-- 右侧：详情 Tab -->
      <div class="ip-group-detail-panel">
        <div v-if="!currentNode" class="empty-tip">
          <el-empty description="请在左侧选择一个 IP 组" :image-size="120" />
        </div>

        <template v-else>
          <div class="detail-header">
            <div class="info">
              <h2>
                <el-tag v-if="currentNode.groupType === 1" type="success" size="default" effect="dark">大组</el-tag>
                <el-tag v-else type="info" size="default" effect="dark">小组</el-tag>
                {{ currentNode.groupName }}
                <el-tag v-if="currentNode.status === 0" type="danger">已停用</el-tag>
              </h2>
              <p class="subtitle">
                <span>组长：{{ currentNode.leaderName || '未指定' }}</span>
                <el-divider direction="vertical" />
                <span>上级：{{ currentNode.parentName || '顶级' }}</span>
                <el-divider direction="vertical" />
                <span>创建于：{{ currentNode.createdAt || '-' }}</span>
              </p>
            </div>
            <div class="actions">
              <el-button v-if="currentNode.groupType === 1" type="primary" @click="handleCreateChild">
                <el-icon><Plus /></el-icon>新建小组
              </el-button>
              <el-button @click="handleEdit">编辑</el-button>
              <el-button
                :type="currentNode.status === 1 ? 'warning' : 'success'"
                @click="handleToggleStatus"
              >
                {{ currentNode.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button type="danger" @click="handleDelete">删除</el-button>
            </div>
          </div>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <!-- Tab 1: 基本信息 -->
            <el-tab-pane label="基本信息" name="basic">
              <el-descriptions :column="2" border style="max-width: 900px">
                <el-descriptions-item label="组名">{{ currentNode.groupName }}</el-descriptions-item>
                <el-descriptions-item label="组类型">{{ currentNode.groupType === 1 ? '大组' : '小组' }}</el-descriptions-item>
                <el-descriptions-item label="上级组">{{ currentNode.parentName || '顶级' }}</el-descriptions-item>
                <el-descriptions-item label="组长">{{ currentNode.leaderName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="currentNode.status === 1 ? 'success' : 'danger'">
                    {{ currentNode.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="排序">{{ currentNode.sortOrder || 0 }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ currentNode.remark || '-' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ currentNode.createdAt || '-' }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ currentNode.updatedAt || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <!-- Tab 2: 成员管理 -->
            <el-tab-pane label="成员管理" name="member">
              <div class="tab-actions">
                <el-button type="primary" @click="handleAddMember">
                  <el-icon><Plus /></el-icon>添加成员
                </el-button>
                <el-button @click="loadMembers">刷新</el-button>
              </div>
              <el-table v-loading="memberLoading" :data="memberList" border stripe>
                <el-table-column type="index" label="#" width="60" align="center" />
                <el-table-column prop="userName" label="姓名" width="120" />
                <el-table-column prop="positionText" label="组内角色" width="140">
                  <template #default="{ row }">
                    <el-tag>{{ row.positionText || row.position || '成员' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="joinTime" label="加入时间" width="170" align="center" />
                <el-table-column label="操作" width="120" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="handleRemoveMember(row)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- Tab 3: 关联账号 -->
            <el-tab-pane label="关联账号" name="account">
              <div class="tab-actions">
                <el-button type="primary" @click="handleBindAccount">
                  <el-icon><Plus /></el-icon>关联账号
                </el-button>
                <el-button @click="loadAccounts">刷新</el-button>
              </div>
              <el-table v-loading="accountLoading" :data="accountList" border stripe>
                <el-table-column type="index" label="#" width="60" align="center" />
                <el-table-column prop="accountName" label="账号名称" min-width="160" />
                <el-table-column prop="platformName" label="平台" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag>{{ row.platformName }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="followers" label="粉丝数" width="120" align="right" />
                <el-table-column prop="contentCount" label="作品数" width="100" align="right" />
                <el-table-column prop="boundAt" label="绑定时间" width="170" align="center" />
                <el-table-column label="操作" width="120" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="handleUnbindAccount(row)">解绑</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- Tab 4: 关联主播 -->
            <el-tab-pane label="关联主播" name="anchor">
              <div class="tab-actions">
                <el-button type="primary" @click="handleBindAnchor">
                  <el-icon><Plus /></el-icon>添加主播
                </el-button>
                <el-button @click="loadAnchors">刷新</el-button>
              </div>
              <el-table v-loading="anchorLoading" :data="anchorList" border stripe>
                <el-table-column type="index" label="#" width="60" align="center" />
                <el-table-column prop="anchorName" label="主播姓名" width="140" />
                <el-table-column prop="platformName" label="主要平台" width="120" align="center" />
                <el-table-column prop="liveHours" label="本月直播时长(h)" width="140" align="right" />
                <el-table-column prop="gpm" label="GPM" width="100" align="right" />
                <el-table-column prop="boundAt" label="绑定时间" width="170" align="center" />
                <el-table-column label="操作" width="120" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="handleUnbindAnchor(row)">解绑</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- Tab 5: 统计 -->
            <el-tab-pane label="统计" name="stats">
              <el-row :gutter="16" v-loading="statsLoading">
                <el-col :span="6">
                  <el-card shadow="hover">
                    <div class="stat-item">
                      <div class="stat-value">{{ statsData?.memberCount || 0 }}</div>
                      <div class="stat-label">成员数</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="hover">
                    <div class="stat-item">
                      <div class="stat-value">{{ statsData?.accountCount || 0 }}</div>
                      <div class="stat-label">关联账号数</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="hover">
                    <div class="stat-item">
                      <div class="stat-value">{{ statsData?.anchorCount || 0 }}</div>
                      <div class="stat-label">关联主播数</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="hover">
                    <div class="stat-item">
                      <div class="stat-value">{{ formatNumber(statsData?.totalFollowers || 0) }}</div>
                      <div class="stat-label">粉丝总量</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover">
                    <div class="stat-item">
                      <div class="stat-value">{{ statsData?.totalContent || 0 }}</div>
                      <div class="stat-label">内容产出</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover">
                    <div class="stat-item">
                      <div class="stat-value">{{ statsData?.totalLiveHours || 0 }} h</div>
                      <div class="stat-label">直播时长</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover">
                    <div class="stat-item">
                      <div class="stat-value" :class="{ negative: (statsData?.roi || 0) < 1 }">
                        {{ (statsData?.roi || 0).toFixed(2) }}
                      </div>
                      <div class="stat-label">ROI</div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
              <el-alert
                style="margin-top: 16px"
                type="info"
                :closable="false"
                show-icon
                title="聚合说明"
                description="统计基于当前 IP 组及其子组的账号/主播/内容数据汇总，每日凌晨 02:00 更新"
              />
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </div>

    <!-- 新建/编辑 IP 组对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建 IP 组' : '编辑 IP 组'" width="600px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="组名" prop="groupName">
          <el-input v-model="formData.groupName" placeholder="请输入组名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="组类型" prop="groupType">
          <el-radio-group v-model="formData.groupType" :disabled="dialogMode === 'edit'">
            <el-radio :value="1">大组</el-radio>
            <el-radio :value="2">小组</el-radio>
          </el-radio-group>
          <span style="margin-left: 12px; color: #909399; font-size: 12px">组类型创建后不可修改</span>
        </el-form-item>
        <el-form-item label="上级组" prop="parentId">
          <IpGroupTreeSelect
            v-model="formData.parentId"
            :multiple="false"
            placeholder="不选则为顶级"
          />
        </el-form-item>
        <el-form-item label="组长" prop="leaderId">
          <UserSelect v-model="formData.leaderId" placeholder="请选择组长" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加成员 -->
    <el-dialog v-model="memberDialogVisible" title="添加成员" width="500px">
      <el-form :model="memberForm" label-width="80px">
        <el-form-item label="成员" required>
          <UserSelect v-model="memberForm.userId" placeholder="请选择系统用户" />
        </el-form-item>
        <el-form-item label="组内角色">
          <el-input v-model="memberForm.position" placeholder="如 OPERATOR / OPS_LEADER" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="memberSubmitting" @click="handleSubmitMember">确定</el-button>
      </template>
    </el-dialog>

    <!-- 关联账号 -->
    <el-dialog v-model="accountDialogVisible" title="关联账号" width="500px">
      <el-form :model="accountForm" label-width="80px">
        <el-form-item label="账号" required>
          <AccountSelect v-model="accountForm.accountId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="accountSubmitting" @click="handleSubmitAccount">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加主播 -->
    <el-dialog v-model="anchorDialogVisible" title="添加主播" width="500px">
      <el-form :model="anchorForm" label-width="80px">
        <el-form-item label="主播" required>
          <UserSelect v-model="anchorForm.anchorUserId" role-code="ANCHOR" />
        </el-form-item>
        <el-form-item label="主要平台">
          <DictSelect v-model="anchorForm.platformType" dict-type="dict_platform_type" placeholder="请选择" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="anchorDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="anchorSubmitting" @click="handleSubmitAnchor">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Folder, FolderOpened, Search } from '@element-plus/icons-vue'
import {
  getIpGroupTree,
  createIpGroup,
  updateIpGroup,
  deleteIpGroup,
  updateIpGroupStatus,
  getIpGroupStats,
  getIpGroupMembers,
  addIpGroupMember,
  deleteIpGroupMember,
  getIpGroupAccounts,
  bindIpGroupAccounts,
  unbindIpGroupAccount,
  getIpGroupAnchors,
  bindIpGroupAnchors,
  unbindIpGroupAnchor,
} from '@/api/ip-group'
import type { IpGroupTreeVO, IpGroupStatsVO } from '@/types/ip-group'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import UserSelect from '@/components/selectors/UserSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import DictSelect from '@/components/DictSelect.vue'

// ============== 左侧树 ==============
const treeRef = ref()
const treeLoading = ref(false)
const treeData = ref<IpGroupTreeVO[]>([])
const treeSearchKeyword = ref('')
const treeProps = { children: 'children', label: 'groupName' }

watch(treeSearchKeyword, (val) => {
  treeRef.value?.filter(val)
})

const filterNode = (keyword: string, data: any) => {
  if (!keyword) return true
  return data.groupName?.includes(keyword)
}

const loadTree = async () => {
  treeLoading.value = true
  try {
    treeData.value = await getIpGroupTree()
  } catch (e) {
    console.error('[IpGroup] 加载 IP 组树失败:', e)
    treeData.value = []
    ElMessage.error('IP 组树加载失败，请稍后重试')
  } finally {
    treeLoading.value = false
  }
}

// ============== 右侧详情 ==============
const currentNode = ref<any>(null)
const activeTab = ref('basic')

const statsData = ref<IpGroupStatsVO | null>(null)
const statsLoading = ref(false)

const memberList = ref<any[]>([])
const memberLoading = ref(false)

const accountList = ref<any[]>([])
const accountLoading = ref(false)

const anchorList = ref<any[]>([])
const anchorLoading = ref(false)

const handleNodeClick = (data: any) => {
  currentNode.value = data
  activeTab.value = 'basic'
  loadAllTabData()
}

const loadAllTabData = () => {
  if (!currentNode.value) return
  loadStats()
  loadMembers()
  loadAccounts()
  loadAnchors()
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const data = await getIpGroupStats(currentNode.value.id)
    statsData.value = data
  } catch (e) {
    console.error('[IpGroup] 加载统计数据失败:', e)
    statsData.value = null
    ElMessage.error('统计数据加载失败')
  } finally {
    statsLoading.value = false
  }
}

const loadMembers = async () => {
  memberLoading.value = true
  try {
    const data = await getIpGroupMembers(currentNode.value.id)
    memberList.value = data
  } catch (e) {
    console.error('[IpGroup] 加载成员失败:', e)
    memberList.value = []
    ElMessage.error('成员列表加载失败')
  } finally {
    memberLoading.value = false
  }
}

const loadAccounts = async () => {
  accountLoading.value = true
  try {
    const data = await getIpGroupAccounts(currentNode.value.id)
    accountList.value = data
  } catch (e) {
    console.error('[IpGroup] 加载账号失败:', e)
    accountList.value = []
    ElMessage.error('账号列表加载失败')
  } finally {
    accountLoading.value = false
  }
}

const loadAnchors = async () => {
  anchorLoading.value = true
  try {
    const data = await getIpGroupAnchors(currentNode.value.id)
    anchorList.value = data
  } catch (e) {
    console.error('[IpGroup] 加载主播失败:', e)
    anchorList.value = []
    ElMessage.error('主播列表加载失败')
  } finally {
    anchorLoading.value = false
  }
}

// ============== 新建/编辑 IP 组 ==============
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit' | 'createChild'>('create')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

// 递归查找树节点（用于编辑后从新树中定位已保存节点，避免 currentNode 引用旧对象）
const findNodeById = (nodes: any[], id: number): any => {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children?.length) {
      const f = findNodeById(n.children, id)
      if (f) return f
    }
  }
  return null
}

const formData = reactive({
  id: undefined as number | undefined,
  groupName: '',
  groupType: 1 as 1 | 2,
  parentId: undefined as number | undefined,
  leaderId: undefined as number | undefined,
  sortOrder: 0,
  status: 1 as 0 | 1,
  remark: '',
})
const formRules: FormRules = {
  groupName: [{ required: true, message: '请输入组名', trigger: 'blur' }],
  groupType: [{ required: true, message: '请选择组类型', trigger: 'change' }],
}

const handleCreateRoot = () => {
  dialogMode.value = 'create'
  Object.assign(formData, {
    id: undefined, groupName: '', groupType: 1, parentId: undefined,
    leaderId: undefined, sortOrder: 0, status: 1, remark: '',
  })
  dialogVisible.value = true
}

const handleCreateChild = () => {
  dialogMode.value = 'createChild'
  Object.assign(formData, {
    id: undefined, groupName: '', groupType: 2, parentId: currentNode.value?.id,
    leaderId: undefined, sortOrder: 0, status: 1, remark: '',
  })
  dialogVisible.value = true
}

const handleEdit = () => {
  if (!currentNode.value) return
  dialogMode.value = 'edit'
  Object.assign(formData, {
    id: currentNode.value.id,
    groupName: currentNode.value.groupName,
    groupType: currentNode.value.groupType,
    parentId: currentNode.value.parentId,
    leaderId: currentNode.value.leaderId,
    sortOrder: currentNode.value.sortOrder || 0,
    status: currentNode.value.status,
    remark: currentNode.value.remark || '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (formData.id) {
      await updateIpGroup(formData)
      ElMessage.success('修改成功')
    } else {
      await createIpGroup(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadTree()
    // 重新选中并刷新右侧详情
    // P-GATE-UNMOCK: setCurrentKey 只高亮不触发 node-click，currentNode.value 仍指向旧对象，
    // 需手动从新树中找出节点并赋给 currentNode，否则右侧详情显示旧值。
    const savedId = formData.id
    if (savedId) {
      const found = findNodeById(treeData.value, savedId)
      if (found) {
        currentNode.value = found
      }
      nextTick(() => {
        if (treeRef.value) treeRef.value.setCurrentKey(savedId)
      })
      // 重新拉详情 tab
      loadAllTabData()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async () => {
  if (!currentNode.value) return
  const newStatus = currentNode.value.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确认${newStatus === 1 ? '启用' : '停用'}【${currentNode.value.groupName}】？`,
      '提示',
      { type: 'warning' }
    )
    await updateIpGroupStatus(currentNode.value.id, newStatus as 0 | 1)
    ElMessage.success('操作成功')
    await loadTree()
    currentNode.value.status = newStatus
  } catch {}
}

const handleDelete = async () => {
  if (!currentNode.value) return
  // ADR-M1-002 保护删除: 有成员/账号/主播时不允许删除
  const hasChildren = (currentNode.value.memberCount > 0) ||
    (currentNode.value.accountCount > 0) ||
    (currentNode.value.anchorCount > 0)
  try {
    await ElMessageBox.confirm(
      hasChildren
        ? `【${currentNode.value.groupName}】下还有成员/账号/主播，请先迁移后再删除`
        : `确认删除【${currentNode.value.groupName}】？删除后可在回收站恢复`,
      hasChildren ? '无法删除' : '删除确认',
      { type: hasChildren ? 'warning' : 'error', confirmButtonText: hasChildren ? '去迁移' : '确认删除' }
    ).catch(() => { throw new Error('cancel') })
    if (hasChildren) {
      ElMessage.info('请先移除成员/账号/主播')
      activeTab.value = 'member'
      return
    }
    await deleteIpGroup(currentNode.value.id)
    ElMessage.success('删除成功')
    currentNode.value = null
    await loadTree()
  } catch (e: any) {
    if (e?.message !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

// ============== Tab 2: 成员管理 ==============
const memberDialogVisible = ref(false)
const memberSubmitting = ref(false)
const memberForm = reactive({ userId: undefined as number | undefined, position: '' })

const handleAddMember = () => {
  Object.assign(memberForm, { userId: undefined, position: '' })
  memberDialogVisible.value = true
}

const handleSubmitMember = async () => {
  if (!memberForm.userId) {
    ElMessage.warning('请选择成员')
    return
  }
  memberSubmitting.value = true
  try {
    await addIpGroupMember(currentNode.value.id, {
      userId: memberForm.userId,
      position: memberForm.position as any,
    })
    ElMessage.success('添加成功')
    memberDialogVisible.value = false
    await loadMembers()
    await loadTree()
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  } finally {
    memberSubmitting.value = false
  }
}

const handleRemoveMember = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认移除【${row.userName}】？`, '提示', { type: 'warning' })
    await deleteIpGroupMember(currentNode.value.id, row.memberId)
    ElMessage.success('已移除')
    await loadMembers()
    await loadTree()
  } catch {}
}

// ============== Tab 3: 关联账号 ==============
const accountDialogVisible = ref(false)
const accountSubmitting = ref(false)
const accountForm = reactive({ accountId: undefined as number | undefined })

const handleBindAccount = () => {
  accountForm.accountId = undefined
  accountDialogVisible.value = true
}
const handleSubmitAccount = async () => {
  if (!accountForm.accountId) { ElMessage.warning('请选择账号'); return }
  accountSubmitting.value = true
  try {
    await bindIpGroupAccounts(currentNode.value.id, {
      accountIds: [accountForm.accountId],
      accountRole: 'PRIMARY',
    })
    ElMessage.success('已关联')
    accountDialogVisible.value = false
    await loadAccounts()
    await loadTree()
  } catch (e: any) {
    ElMessage.error(e?.message || '关联失败')
  } finally {
    accountSubmitting.value = false
  }
}
const handleUnbindAccount = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认解绑【${row.accountName}】？`, '提示', { type: 'warning' })
    await unbindIpGroupAccount(currentNode.value.id, row.accountId)
    ElMessage.success('已解绑')
    await loadAccounts()
    await loadTree()
  } catch {}
}

// ============== Tab 4: 关联主播 ==============
const anchorDialogVisible = ref(false)
const anchorSubmitting = ref(false)
const anchorForm = reactive({ anchorUserId: undefined as number | undefined, platformType: undefined as string | undefined })

const handleBindAnchor = () => {
  Object.assign(anchorForm, { anchorUserId: undefined, platformType: undefined })
  anchorDialogVisible.value = true
}
const handleSubmitAnchor = async () => {
  if (!anchorForm.anchorUserId) { ElMessage.warning('请选择主播'); return }
  anchorSubmitting.value = true
  try {
    await bindIpGroupAnchors(currentNode.value.id, {
      anchorUserId: anchorForm.anchorUserId,
      platformType: anchorForm.platformType,
    })
    ElMessage.success('已添加')
    anchorDialogVisible.value = false
    await loadAnchors()
    await loadTree()
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  } finally {
    anchorSubmitting.value = false
  }
}
const handleUnbindAnchor = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认解绑【${row.anchorName}】？`, '提示', { type: 'warning' })
    await unbindIpGroupAnchor(currentNode.value.id, row.id)
    ElMessage.success('已解绑')
    await loadAnchors()
    await loadTree()
  } catch {}
}

// ============== 工具 ==============
const formatNumber = (n: number) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toString()
}

onMounted(loadTree)
</script>

<style scoped>
.ip-group-page { padding: 16px; height: calc(100vh - 120px); }
.ip-group-layout { display: flex; gap: 16px; height: 100%; }

.ip-group-tree-panel {
  width: 320px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}
.tree-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
}
.tree-header .title { font-weight: 600; font-size: 14px; }
.tree-search { padding: 8px 12px; border-bottom: 1px solid #ebeef5; }
.tree-scroll { flex: 1; padding: 8px; }
.tree-node-label { display: inline-flex; align-items: center; gap: 6px; flex: 1; }
.tree-node-label .name { font-weight: 500; }
.tree-node-label .meta { color: #909399; font-size: 12px; }
.folder-icon { color: #e6a23c; }
.sub-icon { color: #909399; }

.ip-group-detail-panel { flex: 1; background: #fff; border-radius: 4px; border: 1px solid #e4e7ed; padding: 16px; overflow: auto; }
.empty-tip { display: flex; align-items: center; justify-content: center; height: 100%; }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; padding-bottom: 12px; border-bottom: 1px solid #ebeef5; }
.detail-header h2 { margin: 0 0 4px 0; font-size: 18px; display: flex; align-items: center; gap: 8px; }
.detail-header .subtitle { color: #909399; font-size: 13px; margin: 0; }
.detail-tabs { margin-top: 12px; }
.tab-actions { margin-bottom: 12px; }
.stat-item { text-align: center; padding: 12px 0; }
.stat-value { font-size: 28px; font-weight: 600; line-height: 1; color: #409eff; }
.stat-value.negative { color: #f56c6c; }
.stat-label { color: #909399; font-size: 13px; margin-top: 8px; }
</style>
