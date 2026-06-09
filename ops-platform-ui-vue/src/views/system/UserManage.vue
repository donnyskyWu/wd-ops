<!--
  M9 - 用户管理
  依据: FR-M9-001 用户维护
  4 区: 搜索/列表/CRUD 弹窗/角色分配
-->
<template>
  <div class="user-manage">
    <ContentWrap title="用户管理" subtitle="系统用户账号管理">
      <!-- 搜索 -->
      <el-form :model="search" inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="search.username" placeholder="支持模糊搜索" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="search.realName" placeholder="支持模糊搜索" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="search.roleId" clearable style="width: 160px">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" clearable style="width: 120px">
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
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="dept" label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="statusUi" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.statusUi"
              active-value="active"
              inactive-value="inactive"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastLogin" label="最后登录" width="160" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
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
    </ContentWrap>

    <!-- 新增/编辑 -->
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
        <el-form-item label="部门" prop="dept">
          <el-input v-model="form.remark" placeholder="备注/部门信息" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
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
const userList = ref<UserItem[]>([])
const total = ref(0)
const search = reactive({ username: '', realName: '', roleId: undefined as number | undefined, status: '', pageNo: 1, pageSize: 20 })
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref<FormInstance>()
const form = reactive<Partial<UserItem & { password?: string }>>({})

const roles = ref<RoleVO[]>([])

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 32, message: '长度 3-32', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
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
    phone: '',
    email: '',
    statusUi: 'active' as const,
    remark: '',
  })
  formRef.value?.clearValidate()
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
    await ElMessageBox.confirm(`确认${next === 'active' ? '启用' : '禁用'}用户 ${row.realName}？`, '提示', { type: 'warning' })
    await updateUser({ id: row.id, status: toApiStatus(next) })
    ElMessage.success('操作成功')
    await loadList()
  } catch {
    row.statusUi = next === 'active' ? 'inactive' : 'active'
  }
}

const handleResetPwd = async (_row: UserItem) => {
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

onMounted(async () => {
  await loadRoles()
  await loadList()
})
</script>

<style scoped>
.user-manage { padding: 20px; }
</style>
