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
    :remote="useRemoteSearch"
    :remote-method="useRemoteSearch ? handleRemoteSearch : undefined"
    :filter-method="useRemoteSearch ? undefined : filterLocalOptions"
    :loading="loading"
    style="width: 100%"
    @change="handleChange"
    @visible-change="handleVisibleChange"
  >
    <el-option
      v-for="item in options"
      :key="item.id"
      :label="formatUserLabel(item)"
      :value="item.id"
    >
      <span style="float: left">{{ formatUserLabel(item) }}</span>
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
import { ref, watch, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSystemUserSimpleList, filterSystemUsers, normalizeUserId } from '@/api/football-user'
import {
  getIpGroupMembers,
  getIpGroupLeaderCandidates,
  getIpGroupMemberCandidates,
  IP_GROUP_LEADER_ROLE_CODE,
} from '@/api/ip-group'

interface UserVO {
  id: string
  username: string
  nickname: string
  phoneMasked?: string
  deptId?: number
  deptName?: string
  roleNames?: string[]
  status?: number
}

interface Props {
  modelValue?: string | string[] | number | number[] | undefined
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
  /**
   * 租户内全部启用用户（OA /member-candidates，不受 Football simple-list 数据权限限制）。
   * 用于 IP 组「添加成员」等需选任意人的场景。
   */
  allTenantUsers?: boolean
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
  allTenantUsers: false,
})

const emit = defineEmits<{
  'update:modelValue': [val: string | string[] | number | number[] | undefined]
  change: [val: string | string[] | number | number[] | undefined, item?: UserVO]
}>()

const toModelValue = (val: string | string[] | number | number[] | undefined) => {
  if (val == null) return undefined
  if (Array.isArray(val)) return val.map((item) => normalizeUserId(item))
  return normalizeUserId(val)
}

const selectedValue = ref<string | string[] | undefined>(toModelValue(props.modelValue) as string | string[] | undefined)
const options = ref<UserVO[]>([])
const cachedOptions = ref<UserVO[]>([])
const loading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

/** 组长/成员候选人/IP 组成员等短列表走本地过滤，避免 remote 模式与雪花 id 精度问题 */
const useRemoteSearch = computed(() => {
  if (
    props.allTenantUsers
    || props.roleCode === IP_GROUP_LEADER_ROLE_CODE
    || props.ipGroupId != null
  ) {
    return false
  }
  return props.remote
})

const isGarbledNickname = (nickname?: string) =>
  !!nickname && (/^[?？]+$/.test(nickname) || /^[?？]+.+/.test(nickname))

const displayNickname = (item: Pick<UserVO, 'nickname' | 'username'>) => {
  if (isGarbledNickname(item.nickname)) {
    return item.username || item.nickname
  }
  return item.nickname
}

const formatUserLabel = (item: UserVO) => {
  const nickname = displayNickname(item)
  if (item.username && item.username !== nickname) {
    return `${nickname} (${item.username})`
  }
  return nickname
}

const matchesUserKeyword = (item: Pick<UserVO, 'nickname' | 'username'>, keyword: string) => {
  const kw = keyword.trim().toLowerCase()
  if (!kw) return true
  const nickname = (item.nickname || '').toLowerCase()
  const username = (item.username || '').toLowerCase()
  if (nickname.includes(kw) || username.includes(kw)) {
    return true
  }
  // 支持缩写检索：zw → zhangwu
  let cursor = 0
  for (const ch of username) {
    if (ch === kw[cursor]) {
      cursor += 1
    }
    if (cursor >= kw.length) {
      return true
    }
  }
  return false
}

const setOptions = (list: UserVO[]) => {
  cachedOptions.value = list
  options.value = list
}

watch(() => props.modelValue, (val) => {
  selectedValue.value = toModelValue(val) as string | string[] | undefined
  void ensureSelectedUser()
})
watch(() => props.ipGroupId, () => loadList(''))

const ensureSelectedUser = async () => {
  const val = props.modelValue
  if (val == null || (Array.isArray(val) && !val.length)) return
  const ids = (Array.isArray(val) ? val : [val]).map((item) => normalizeUserId(item))
  const missing = ids.filter((id) => !options.value.some((o) => o.id === id))
  if (!missing.length) return
  try {
    const users = await fetchSystemUserSimpleList()
    for (const id of missing) {
      const u = users.find((x) => normalizeUserId(x.id) === id)
      if (!u || options.value.some((o) => o.id === normalizeUserId(u.id))) continue
      options.value.push({
        id: normalizeUserId(u.id),
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
      setOptions(
        members
          .filter((item) => !kw || item.userName.toLowerCase().includes(kw))
          .map((item) => ({
            id: normalizeUserId(item.userId),
            username: String(item.userId),
            nickname: item.userName,
            phoneMasked: undefined,
            deptName: undefined,
            roleNames: item.positionText ? [item.positionText] : [],
          })),
      )
      return
    }
    if (props.roleCode === IP_GROUP_LEADER_ROLE_CODE) {
      const candidates = await getIpGroupLeaderCandidates()
      const kw = keyword?.trim().toLowerCase()
      setOptions(
        candidates
          .filter((u) => {
            if (!kw) return true
            return matchesUserKeyword(
              { nickname: u.nickname, username: u.username || String(u.id) },
              kw,
            )
          })
          .slice(0, 50)
          .map((u) => ({
            id: normalizeUserId(u.id),
            username: u.username || String(u.id),
            nickname: u.nickname,
            phoneMasked: undefined,
            deptName: undefined,
            roleNames: ['IP组长'],
          })),
      )
      return
    }
    if (props.allTenantUsers) {
      const candidates = await getIpGroupMemberCandidates()
      const kw = keyword?.trim().toLowerCase()
      setOptions(
        candidates
          .filter((u) => {
            if (!kw) return true
            return matchesUserKeyword(
              { nickname: u.nickname, username: u.username || String(u.id) },
              kw,
            )
          })
          .map((u) => ({
            id: normalizeUserId(u.id),
            username: u.username || String(u.id),
            nickname: u.nickname,
            phoneMasked: undefined,
            deptName: undefined,
            roleNames: [],
          })),
      )
      return
    }
    const users = await fetchSystemUserSimpleList()
    let filtered = filterSystemUsers(users, {
      keyword,
      deptId: props.deptId,
      enabledOnly: true,
    })
    setOptions(
      filtered.slice(0, 50).map((u) => ({
        id: normalizeUserId(u.id),
        username: u.username || String(u.id),
        nickname: u.nickname,
        phoneMasked: u.mobile,
        deptId: u.deptId,
        deptName: u.deptName,
        roleNames: [],
        status: u.status,
      })),
    )
  } catch (e) {
    console.error('[UserSelect] 加载用户列表失败:', e)
    setOptions([])
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

const filterLocalOptions = (query: string) => {
  const kw = query.trim().toLowerCase()
  if (!kw) {
    options.value = cachedOptions.value
    return
  }
  options.value = cachedOptions.value.filter((item) => matchesUserKeyword(item, kw))
}

const handleVisibleChange = (visible: boolean) => {
  if (!visible) return
  if (
    props.allTenantUsers
    || props.roleCode === IP_GROUP_LEADER_ROLE_CODE
    || props.ipGroupId != null
  ) {
    void loadList('')
  }
}

const handleChange = (val: string | string[] | number | number[] | undefined) => {
  const normalized = toModelValue(val) as string | string[] | undefined
  selectedValue.value = normalized
  const item = Array.isArray(normalized)
    ? undefined
    : options.value.find((o) => o.id === normalized)
      ?? cachedOptions.value.find((o) => o.id === normalized)
  emit('update:modelValue', normalized)
  emit('change', normalized, item)
}

onMounted(() => loadList(''))

defineExpose({ refresh: () => loadList('') })
</script>
