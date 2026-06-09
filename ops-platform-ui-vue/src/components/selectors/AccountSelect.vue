<!--
  AccountSelect - 平台账号选择器
  关联: API-M4 GET /admin-api/oa/account/list
  使用: <AccountSelect v-model="form.accountId" :platform-type="form.platformType" />
-->
<template>
  <el-select
    v-model="selectedValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled || !platformType"
    :filterable="filterable"
    :multiple="multiple"
    :remote="remote"
    :remote-method="handleRemoteSearch"
    :loading="loading"
    :allow-create="false"
    style="width: 100%"
    @change="handleChange"
  >
    <el-option
      v-for="item in options"
      :key="item.id"
      :label="`${item.accountName} (${item.platformName})`"
      :value="item.id"
    >
      <span style="float: left">{{ item.accountName }}</span>
      <span style="float: right; color: #909399; font-size: 12px; margin-left: 12px">
        {{ item.externalAccountId || '-' }}
        <span v-if="item.ipGroupId" style="margin-left: 8px">IP#{{ item.ipGroupId }}</span>
      </span>
    </el-option>
    <template #empty>
      <el-empty
        v-if="!loading"
        :description="!platformType ? '请先选择平台' : '未找到匹配的账号'"
        :image-size="60"
      />
    </template>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { request } from '@/utils/request'
import { mockAccountList } from '@/mock/selectors'
import { applyExcludeBound, debounceSearch, platformLabel, useMockFallback } from './selector-utils'

interface AccountVO {
  id: number
  accountName: string
  externalAccountId?: string
  platformType: string
  platformName: string
  ipGroupId?: number
  status?: string
}

interface Props {
  modelValue?: number | number[] | undefined
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  filterable?: boolean
  multiple?: boolean
  remote?: boolean
  platformType?: string
  accountType?: string
  ipGroupId?: number
  companyId?: number
  activeOnly?: boolean
  excludeBound?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  placeholder: '请选择账号',
  clearable: true,
  disabled: false,
  filterable: true,
  multiple: false,
  remote: true,
  platformType: undefined,
  accountType: undefined,
  ipGroupId: undefined,
  companyId: undefined,
  activeOnly: true,
  excludeBound: false,
})

const emit = defineEmits<{
  'update:modelValue': [val: number | number[] | undefined]
  change: [val: number | number[] | undefined, item?: AccountVO]
}>()

const selectedValue = ref<number | number[] | undefined>(props.modelValue)
const options = ref<AccountVO[]>([])
const loading = ref(false)

watch(() => props.modelValue, (val) => { selectedValue.value = val })
watch([() => props.platformType, () => props.ipGroupId, () => props.companyId], () => {
  selectedValue.value = undefined
  emit('update:modelValue', undefined)
  loadList('')
})

const loadList = async (keyword: string) => {
  if (!props.platformType) {
    options.value = []
    return
  }
  loading.value = true
  try {
    const res = await request.get<{ list: any[] }>({
      url: '/oa/account/list',
      params: {
        accountName: keyword || undefined,
        platformType: props.platformType,
        accountType: props.accountType,
        companyId: props.companyId,
        status: props.activeOnly ? 'NORMAL' : undefined,
        pageSize: 50,
      },
    })
    const raw = (res as any).list || []
    let list: AccountVO[] = raw.map((item: any) => ({
      id: item.id,
      accountName: item.accountName,
      externalAccountId: item.externalAccountId,
      platformType: item.platformType,
      platformName: platformLabel(item.platformType),
      ipGroupId: item.ipGroupId,
      status: item.status,
    }))
    if (props.ipGroupId) {
      list = list.filter((a) => a.ipGroupId === props.ipGroupId)
    }
    list = useMockFallback(list, mockAccountList.filter((a) => a.platformType === props.platformType))
    options.value = list
  } catch (e) {
    console.warn('[AccountSelect] fallback to mock:', e)
    let list = mockAccountList.filter((a) => a.platformType === props.platformType)
    if (props.ipGroupId) list = list.filter((a) => a.ipGroupId === props.ipGroupId)
    options.value = import.meta.env.VITE_USE_MOCK === 'true' ? list : []
  } finally {
    loading.value = false
  }
}

const handleRemoteSearch = debounceSearch(loadList)

const handleChange = (val: number | number[] | undefined) => {
  selectedValue.value = val
  const item = !Array.isArray(val) ? options.value.find((o) => o.id === val) : undefined
  emit('update:modelValue', val)
  emit('change', val, item)
}

onMounted(() => { if (props.platformType) loadList('') })

defineExpose({ refresh: () => loadList('') })
</script>
