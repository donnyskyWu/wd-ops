<!--
  UserSelect - 系统用户选择器
  三大铁律 § 3.2 强制
  关联: PRD-M9 FR-M9-002 / API-M9 § 1 GET /admin-api/oa/system/user/list
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
import { request } from '@/utils/request'
import { getIpGroupMembers } from '@/api/ip-group'

interface UserVO {
  id: number
  username: string
  nickname: string
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

watch(() => props.modelValue, (val) => { selectedValue.value = val })
watch(() => props.ipGroupId, () => loadList(''))

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
          deptName: undefined,
          roleNames: item.positionText ? [item.positionText] : [],
        }))
      return
    }
    const res = await request.get<{ list: UserVO[] }>({
      url: '/oa/system/user/list',
      params: {
        nickname: keyword || undefined,
        deptId: props.deptId,
        roleCode: props.roleCode,
        status: 'ENABLED',
        pageSize: 50,
      },
    })
    const list = (res as any).list || (Array.isArray(res) ? (res as any) : [])
    options.value = list // P-GATE-UNMOCK S-A: 已去除 mock 兜底
  } catch (e) {
    console.error('[UserSelect] 加载用户列表失败:', e)
    options.value = []
    ElMessage.error('用户列表加载失败，请稍后重试')
  } finally {
    loading.value = false
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
