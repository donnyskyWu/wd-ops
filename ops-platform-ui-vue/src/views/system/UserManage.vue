<!--
  M9 - 用户管理（部门树 + 用户列表）
  依据: FR-M9-001 + ADR-013 钉钉组织同步
-->
<template>
  <div class="user-manage">
    <ContentWrap title="用户管理" subtitle="部门组织与用户账号管理">
      <div class="layout">
        <!-- 左侧部门树 -->
        <div class="dept-panel">
          <div class="dept-toolbar">
            <el-input
              v-model="deptFilter"
              placeholder="搜索部门"
              clearable
              prefix-icon="Search"
              size="small"
            />
            <div class="dept-actions">
              <el-button size="small" type="primary" plain @click="handleSyncDepts" :loading="syncingDepts">
                同步钉钉部门
              </el-button>
              <el-button size="small" type="success" plain @click="handleSyncUsers" :loading="syncingUsers">
                同步钉钉人员
              </el-button>
              <el-button size="small" @click="handleAddDept">
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </div>
          <el-tree
            ref="deptTreeRef"
            :data="deptTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            highlight-current
            default-expand-all
            :filter-node-method="filterDeptNode"
            @node-click="onDeptClick"
            v-loading="deptLoading"
            class="dept-tree"
          >
            <template #default="{ node, data }">
              <span class="dept-node">
                <span>{{ node.label }}</span>
                <span class="dept-node-actions" @click.stop>
                  <el-button link type="primary" size="small" @click="handleEditDept(data)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteDept(data)">删</el-button>
                </span>
              </span>
            </template>
          </el-tree>
          <el-button link type="info" size="small" @click="clearDeptFilter" style="margin-top: 8px">
            显示全部用户
          </el-button>
        </div>

        <!-- 右侧用户列表 -->
        <div class="user-panel">
          <el-form :model="search" inline @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="search.username" placeholder="支持模糊搜索" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="search.realName" placeholder="支持模糊搜索" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="search.roleId" clearable style="width: 140px">
                <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="search.status" clearable style="width: 100px">
                <el-option label="启用" value="active" />
                <el-option label="禁用" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">查询</el-button>
              <el-button @click="resetSearch">重置</el-button>
              <el-button type="success" @click="handleAdd">
                <el-icon><Plus /></el-icon>新增用户
              </el-button>
            </el-form-item>
          </el-form>

          <el-table :data="pagedList" border stripe v-loading="loading" style="margin-top: 12px">
            <el-table-column type="index" label="#" width="60" align="center" />
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="realName" label="姓名" width="110" />
            <el-table-column prop="deptName" label="部门" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.deptName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="roleName" label="角色" width="130">
              <template #default="{ row }">
                <el-tag size="small">{{ row.roleName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机" width="130" />
            <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
            <el-table-column prop="statusUi" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="row.statusUi"
                  active-value="active"
                  inactive-value="inactive"
                  @change="toggleStatus(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
                <el-button link type="warning" @click="handleResetPwd(row)">重置密码</el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="search.pageNo"
            v-model:page-size="search.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 16px; justify-content: flex-end"
            @current-change="loadList"
            @size-change="loadList"
          />
        </div>
      </div>
    </ContentWrap>

    <!-- 用户新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号,创建后不可改" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="不填则使用默认 123456" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 部门新增/编辑 -->
    <el-dialog v-model="deptDialogVisible" :title="deptDialogTitle" width="480px" @close="resetDeptForm">
      <el-form :model="deptForm" :rules="deptRules" ref="deptFormRef" label-width="80px">
        <el-form-item label="上级">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="无（根部门）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="deptForm.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDeptSubmit" :loading="deptSubmitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import {
  fetchUserList,
  createUser,
  updateUser,
  deleteUser,
  fetchRoleList,
  type UserVO,
  type RoleVO,
} from '@/api/system-user'
import {
  fetchDeptTree,
  createDept,
  updateDept,
  deleteDept,
  syncDingTalkDepts,
  syncDingTalkUsers,
  type DeptTreeNode,
} from '@/api/system-dept'

interface UserItem extends UserVO {
  realName: string
  roleId?: number
  roleName: string
  phone?: string
  statusUi: 'active' | 'inactive'
  lastLogin: string
}

const loading = ref(false)
const submitting = ref(false)
const deptLoading = ref(false)
const syncingDepts = ref(false)
const syncingUsers = ref(false)
const deptSubmitting = ref(false)
const userList = ref<UserItem[]>([])
const total = ref(0)
const deptTree = ref<DeptTreeNode[]>([])
const deptFilter = ref('')
const deptTreeRef = ref()
const selectedDeptId = ref<number | undefined>()

const search = reactive({
  username: '',
  realName: '',
  roleId: undefined as number | undefined,
  status: '',
  pageNo: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref<FormInstance>()
const form = reactive<Partial<UserItem & { password?: string }>>({})

const deptDialogVisible = ref(false)
const deptDialogTitle = ref('新增部门')
const deptFormRef = ref<FormInstance>()
const deptForm = reactive<{ id?: number; parentId?: number; name: string }>({ name: '' })

const roles = ref<RoleVO[]>([])

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '长度 3-32', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const deptRules: FormRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

watch(deptFilter, (val) => {
  deptTreeRef.value?.filter(val)
})

function filterDeptNode(value: string, data: DeptTreeNode) {
  if (!value) return true
  return data.name.includes(value)
}

function toUiStatus(status: string): 'active' | 'inactive' {
  return status === 'ENABLED' ? 'active' : 'inactive'
}

function toApiStatus(statusUi: 'active' | 'inactive'): string {
  return statusUi === 'active' ? 'ENABLED' : 'DISABLED'
}

function mapUser(u: UserVO): UserItem {
  return {
    ...u,
    realName: u.nickname,
    roleId: u.roleIds?.[0],
    roleName: u.roleNames?.join(', ') || '-',
    phone: u.phoneMasked,
    statusUi: toUiStatus(u.status),
    lastLogin: '-',
  }
}

const pagedList = computed(() => userList.value)

const loadDeptTree = async () => {
  deptLoading.value = true
  try {
    deptTree.value = (await fetchDeptTree()) || []
  } catch {
    deptTree.value = []
  } finally {
    deptLoading.value = false
  }
}

const loadRoles = async () => {
  try {
    const res = await fetchRoleList()
    roles.value = res.list || []
  } catch {
    roles.value = []
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await fetchUserList({
      username: search.username || undefined,
      nickname: search.realName || undefined,
      roleId: search.roleId,
      deptId: selectedDeptId.value,
      status: search.status ? toApiStatus(search.status as 'active' | 'inactive') : undefined,
      pageNo: search.pageNo,
      pageSize: search.pageSize,
    })
    userList.value = (res.list || []).map(mapUser)
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const onDeptClick = (data: DeptTreeNode) => {
  selectedDeptId.value = data.id
  search.pageNo = 1
  loadList()
}

const clearDeptFilter = () => {
  selectedDeptId.value = undefined
  deptTreeRef.value?.setCurrentKey(null)
  search.pageNo = 1
  loadList()
}

const resetSearch = () => {
  search.username = ''
  search.realName = ''
  search.roleId = undefined
  search.status = ''
  search.pageNo = 1
  loadList()
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    username: '',
    realName: '',
    password: '',
    roleId: undefined,
    deptId: selectedDeptId.value,
    phone: '',
    email: '',
    statusUi: 'active' as const,
    remark: '',
  })
  formRef.value?.clearValidate()
}

const resetDeptForm = () => {
  Object.assign(deptForm, { id: undefined, parentId: selectedDeptId.value, name: '' })
  deptFormRef.value?.clearValidate()
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

const handleEdit = (row: UserItem) => {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完整填写')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateUser({
        id: form.id,
        nickname: form.realName,
        email: form.email,
        phone: form.phone,
        deptId: form.deptId,
        status: toApiStatus(form.statusUi || 'active'),
        roleIds: form.roleId ? [form.roleId] : undefined,
        remark: form.remark,
      })
      ElMessage.success('修改成功')
    } else {
      await createUser({
        username: form.username!,
        nickname: form.realName!,
        email: form.email,
        phone: form.phone,
        deptId: form.deptId,
        status: 'ENABLED',
        roleIds: [form.roleId!],
        remark: form.remark,
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row: UserItem) => {
  const next = row.statusUi
  try {
    await ElMessageBox.confirm(`确认${next === 'active' ? '启用' : '禁用'}用户 ${row.realName}？`, '提示', {
      type: 'warning',
    })
    await updateUser({ id: row.id, status: toApiStatus(next) })
    ElMessage.success('操作成功')
    await loadList()
  } catch {
    row.statusUi = next === 'active' ? 'inactive' : 'active'
  }
}

const handleResetPwd = async () => {
  try {
    await ElMessageBox.confirm('密码重置功能将在 seed-auth 阶段接入', '提示', { type: 'info' })
  } catch {}
}

const handleDelete = async (row: UserItem) => {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.realName}？`, '提示', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handleAddDept = () => {
  resetDeptForm()
  deptDialogTitle.value = '新增部门'
  deptDialogVisible.value = true
}

const handleEditDept = (data: DeptTreeNode) => {
  Object.assign(deptForm, { id: data.id, parentId: data.parentId, name: data.name })
  deptDialogTitle.value = '编辑部门'
  deptDialogVisible.value = true
}

const handleDeptSubmit = async () => {
  if (!deptFormRef.value) return
  try {
    await deptFormRef.value.validate()
  } catch {
    return
  }
  deptSubmitting.value = true
  try {
    if (deptForm.id) {
      await updateDept({ id: deptForm.id, parentId: deptForm.parentId, name: deptForm.name })
      ElMessage.success('部门已更新')
    } else {
      await createDept({ parentId: deptForm.parentId, name: deptForm.name })
      ElMessage.success('部门已创建')
    }
    deptDialogVisible.value = false
    await loadDeptTree()
  } finally {
    deptSubmitting.value = false
  }
}

const handleDeleteDept = async (data: DeptTreeNode) => {
  try {
    await ElMessageBox.confirm(`确认删除部门「${data.name}」？`, '提示', { type: 'warning' })
    await deleteDept(data.id)
    ElMessage.success('删除成功')
    if (selectedDeptId.value === data.id) {
      clearDeptFilter()
    }
    await loadDeptTree()
  } catch {}
}

const handleSyncDepts = async () => {
  syncingDepts.value = true
  try {
    const res = await syncDingTalkDepts()
    ElMessage.success(`部门同步完成：新增 ${res.created}，更新 ${res.updated}，跳过 ${res.skipped}`)
    await loadDeptTree()
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '部门同步失败')
  } finally {
    syncingDepts.value = false
  }
}

const handleSyncUsers = async () => {
  syncingUsers.value = true
  try {
    const res = await syncDingTalkUsers()
    ElMessage.success(`人员同步完成：新增 ${res.created}，更新 ${res.updated}，跳过 ${res.skipped}`)
    await loadList()
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '人员同步失败')
  } finally {
    syncingUsers.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadDeptTree()])
  await loadList()
})
</script>

<style scoped>
.user-manage {
  padding: 20px;
}
.layout {
  display: flex;
  gap: 16px;
  min-height: 560px;
}
.dept-panel {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
}
.dept-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.dept-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.dept-tree {
  flex: 1;
  overflow: auto;
}
.dept-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 4px;
}
.dept-node-actions {
  display: none;
}
.dept-node:hover .dept-node-actions {
  display: inline-flex;
}
.user-panel {
  flex: 1;
  min-width: 0;
}
</style>
