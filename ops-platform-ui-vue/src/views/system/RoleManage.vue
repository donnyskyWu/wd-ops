<!--
  M9 - 角色权限
  依据: FR-M9-002 角色权限
  4 区: 搜索/角色列表/CRUD/权限分配
-->
<template>
  <div class="role-manage">
    <ContentWrap title="角色权限" subtitle="角色与权限分配">
      <el-form :model="search" inline>
        <el-form-item label="角色名">
          <el-input v-model="search.keyword" placeholder="支持模糊搜索" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增角色
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="roleList" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="角色名称" min-width="160" />
        <el-table-column prop="code" label="角色编码" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="permissionCount" label="权限数" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <DictLabel dict-type="dict_status_enabled" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handlePermission(row)">权限分配</el-button>
            <el-button link type="danger" @click="handleDelete(row)" :disabled="row.code === 'OA_ADMIN'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="search.pageNo"
        v-model:page-size="search.pageSize"
        :total="total"
        :page-sizes="[10, 20]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadList"
        @size-change="handleSizeChange"
      />
    </ContentWrap>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="大写字母+下划线,创建后不可改" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配 -->
    <el-dialog v-model="permDialogVisible" :title="`权限分配 - ${currentRole?.name}`" width="560px" @close="resetPerm">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        title="权限说明"
        description="勾选该角色可访问的权限点，保存后立即生效"
      />
      <div v-loading="permLoading">
        <el-checkbox-group v-model="selectedPermIds">
          <div v-for="perm in allPermissions" :key="perm.id" style="margin-bottom: 8px">
            <el-checkbox :value="perm.id">
              {{ perm.name }}
              <el-tag size="small" type="info" style="margin-left: 8px">{{ perm.code }}</el-tag>
            </el-checkbox>
          </div>
        </el-checkbox-group>
        <el-empty v-if="!permLoading && allPermissions.length === 0" description="暂无权限点" />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm" :loading="permSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ContentWrap from '@/components/ContentWrap.vue'
import DictLabel from '@/components/DictLabel.vue'
import {
  fetchRoleList,
  createRole,
  updateRole,
  deleteRole,
  assignRolePermission,
  fetchRolePermissions,
  fetchPermissionList,
  type RoleVO,
  type PermissionVO,
} from '@/api/system-user'

interface RoleItem extends RoleVO {
  permissionCount: number
}

const loading = ref(false)
const submitting = ref(false)
const roleList = ref<RoleItem[]>([])
const total = ref(0)
const search = reactive({ keyword: '', pageNo: 1, pageSize: 20 })
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formRef = ref<FormInstance>()
const form = reactive<Partial<RoleVO>>({})

const permDialogVisible = ref(false)
const permLoading = ref(false)
const permSaving = ref(false)
const currentRole = ref<RoleItem | null>(null)
const allPermissions = ref<PermissionVO[]>([])
const selectedPermIds = ref<number[]>([])

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '须以大写字母开头，仅含大写字母、数字、下划线', trigger: 'blur' },
  ],
}

function mapRole(r: RoleVO): RoleItem {
  return {
    ...r,
    permissionCount: r.permissionIds?.length ?? 0,
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await fetchRoleList({
      name: search.keyword || undefined,
      pageNo: search.pageNo,
      pageSize: search.pageSize,
    })
    roleList.value = (res.list || []).map(mapRole)
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  search.pageNo = 1
  loadList()
}

const handleReset = () => {
  search.keyword = ''
  search.pageNo = 1
  loadList()
}

const handleSizeChange = () => {
  search.pageNo = 1
  loadList()
}

const resetForm = () => {
  Object.assign(form, { id: undefined, name: '', code: '', remark: '' })
  formRef.value?.clearValidate()
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

const handleEdit = (row: RoleItem) => {
  Object.assign(form, { id: row.id, name: row.name, code: row.code, remark: row.remark })
  dialogTitle.value = '编辑角色'
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
      await updateRole({ id: form.id, name: form.name, remark: form.remark })
      ElMessage.success('修改成功')
    } else {
      await createRole({ code: form.code!, name: form.name!, remark: form.remark })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: RoleItem) => {
  try {
    await ElMessageBox.confirm(`确认删除角色 ${row.name}？`, '提示', { type: 'warning' })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch {}
}

const handlePermission = async (row: RoleItem) => {
  currentRole.value = row
  permDialogVisible.value = true
  permLoading.value = true
  try {
    const [all, assigned] = await Promise.all([
      fetchPermissionList(),
      fetchRolePermissions(row.id),
    ])
    allPermissions.value = all || []
    selectedPermIds.value = (assigned || []).map((p) => p.id)
  } finally {
    permLoading.value = false
  }
}

const resetPerm = () => {
  allPermissions.value = []
  selectedPermIds.value = []
  currentRole.value = null
}

const savePerm = async () => {
  if (!currentRole.value) return
  permSaving.value = true
  try {
    await assignRolePermission({
      roleId: currentRole.value.id,
      permissionIds: selectedPermIds.value,
    })
    ElMessage.success(`已保存，共 ${selectedPermIds.value.length} 项权限`)
    permDialogVisible.value = false
    await loadList()
  } finally {
    permSaving.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.role-manage {
  padding: 20px;
}
</style>
