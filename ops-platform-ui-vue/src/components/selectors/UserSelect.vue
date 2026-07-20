<!--
  UserSelect - 系统用户选择器
  三大铁律 § 3.2 强制
  关联: ADR-049 D4 · GET /admin-api/system/user/simple-list（身份 SSOT = Football system_users）
  使用: <UserSelect v-model="form.userId" :role-code="form.roleCode" />
-->
<template>
  <el-select
    v-model="selectedValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :filterable="filterable"
    :multiple="multiple"
    :remote="remote"
    :remote-method="handleRemoteSearch"
    :loading="loading"
    style="width: 100%"
    @change="handleChange"
  >
    <el-option
      v-for="item in options"
      :key="item.id"
      :label="item.nickname"
      :value="item.id"
    >
      <span style="float: left">{{ item.nickname }}</span>
      <span style="float: right; color: #909399; font-size: 12px; margin-left: 12px">
        {{ item.deptName || '-' }} · {{ item.roleNames?.join('/') || '-' }}
      </span>
    </el-option>
    <template #empty>
      <el-empty v-if="!loading" description="未找到匹配的用户" :image-size="60" />
    </template>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSystemUserSimpleList, filterSystemUsers } from '@/api/football-user'
import { getIpGroupMembers, getIpGroupLeaderCandidateIds, IP_GROUP_LEADER_ROLE_CODE } from '@/api/ip-group'

interface UserVO {
  id: number
  username: string
  nickname: string
  phoneMasked?: string
  deptId?: number
  deptName?: string
  roleNames?: string[]
  status?: number
}

interface Props {
  modelValue?: number | number[] | undefined
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  filterable?: boolean
  multiple?: boolean
  remote?: boolean
  /** 限定到某部门 */
  deptId?: number
  /** 限定到某角色编码 */
  roleCode?: string
  /** 限定到某 IP 组成员 */
  ipGroupId?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  placeholder: '请选择用户',
  clearable: true,
  disabled: false,
  filterable: true,
  multiple: false,
  remote: true,
  deptId: undefined,
  roleCode: undefined,
  ipGroupId: undefined,
})

const emit = defineEmits<{
  'update:modelValue': [val: number | number[] | undefined]
  change: [val: number | number[] | undefined, item?: UserVO]
}>()

const selectedValue = ref<number | number[] | undefined>(props.modelValue)
const options = ref<UserVO[]>([])
const loading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

watch(() => props.modelValue, (val) => {
  selectedValue.value = val
  void ensureSelectedUser()
})
watch(() => props.ipGroupId, () => loadList(''))

const ensureSelectedUser = async () => {
  const val = props.modelValue
  if (val == null || (Array.isArray(val) && !val.length)) return
  const ids = (Array.isArray(val) ? val : [val]).map(Number)
  const missing = ids.filter((id) => !options.value.some((o) => o.id === id))
  if (!missing.length) return
  try {
    const users = await fetchSystemUserSimpleList()
    for (const id of missing) {
      const u = users.find((x) => Number(x.id) === id)
      if (!u || options.value.some((o) => o.id === Number(u.id))) continue
      options.value.push({
        id: Number(u.id),
        username: u.username || String(u.id),
        nickname: u.nickname,
        phoneMasked: u.mobile,
        deptId: u.deptId,
        deptName: u.deptName,
        roleNames: [],
        status: u.status,
      })
    }
  } catch {
    // keep raw id display if lookup fails
  }
}

const loadList = async (keyword: string) => {
  loading.value = true
  try {
    if (props.ipGroupId) {
      const members = await getIpGroupMembers(props.ipGroupId)
      const kw = keyword?.trim().toLowerCase()
      options.value = members
        .filter((item) => !kw || item.userName.toLowerCase().includes(kw))
        .map((item) => ({
          id: item.userId,
          username: String(item.userId),
          nickname: item.userName,
          phoneMasked: undefined,
          deptName: undefined,
          roleNames: item.positionText ? [item.positionText] : [],
        }))
      return
    }
    const users = await fetchSystemUserSimpleList()
    let filtered = filterSystemUsers(users, {
      keyword,
      deptId: props.deptId,
      enabledOnly: true,
    })
    // roleCode=ip_group_leader：仅展示持有 IP组长 角色的用户；其它 roleCode 暂无服务端列表接口，依赖后端校验
    if (props.roleCode === IP_GROUP_LEADER_ROLE_CODE) {
      try {
        const allowed = new Set((await getIpGroupLeaderCandidateIds()).map(Number))
        filtered = filtered.filter((u) => allowed.has(Number(u.id)))
      } catch (roleErr) {
        console.warn('[UserSelect] 加载 IP组长 候选人失败，回退全量列表（保存时仍由后端校验）:', roleErr)
      }
    }
    options.value = filtered
      .slice(0, 50)
      .map((u) => ({
        id: Number(u.id),
        username: u.username || String(u.id),
        nickname: u.nickname,
        phoneMasked: u.mobile,
        deptId: u.deptId,
        deptName: u.deptName,
        roleNames: [],
        status: u.status,
      }))
  } catch (e) {
    console.error('[UserSelect] 加载用户列表失败:', e)
    options.value = []
    ElMessage.error('用户列表加载失败，请稍后重试')
  } finally {
    loading.value = false
    await ensureSelectedUser()
  }
}

const handleRemoteSearch = (kw: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadList(kw), 200)
}

const handleChange = (val: number | number[] | undefined) => {
  selectedValue.value = val
  const item = Array.isArray(options.value)
    ? options.value.find((o) => o.id === val)
    : undefined
  emit('update:modelValue', val)
  emit('change', val, item)
}

onMounted(() => loadList(''))

defineExpose({ refresh: () => loadList('') })
</script>
