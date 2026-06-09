<!--
  RealNameSelect - 实名人选择器
  使用: <RealNameSelect v-model="form.realnameId" :company-id="form.companyId" />
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
    :allow-create="false"
    style="width: 100%"
    @change="handleChange"
  >
    <el-option
      v-for="item in options"
      :key="item.id"
      :label="`${item.realName} (${item.idCardMask})`"
      :value="item.id"
    >
      <span style="float: left">{{ item.realName }}</span>
      <span style="float: right; color: #909399; font-size: 12px; margin-left: 12px">
        {{ item.idCardMask }} · {{ item.companyName || '未关联公司' }}
        <span v-if="item.accountBoundCount" style="margin-left: 6px; color: #e6a23c">已绑定</span>
      </span>
    </el-option>
    <template #empty>
      <el-empty v-if="!loading" description="未找到匹配的实名人" :image-size="60" />
    </template>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { request } from '@/utils/request'
import { mockRealNameList } from '@/mock/selectors'
import { applyExcludeBound, debounceSearch, useMockFallback } from './selector-utils'

interface RealNameVO {
  id: number
  realName: string
  idCardMask: string
  companyId?: number
  companyName?: string
  accountBoundCount?: number
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
  companyId?: number
  activeOnly?: boolean
  excludeBound?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  placeholder: '请选择实名人',
  clearable: true,
  disabled: false,
  filterable: true,
  multiple: false,
  remote: true,
  companyId: undefined,
  activeOnly: true,
  excludeBound: false,
})

const emit = defineEmits<{
  'update:modelValue': [val: number | number[] | undefined]
  change: [val: number | number[] | undefined, item?: RealNameVO]
}>()

const selectedValue = ref<number | number[] | undefined>(props.modelValue)
const options = ref<RealNameVO[]>([])
const loading = ref(false)

watch(() => props.modelValue, (val) => { selectedValue.value = val })
watch(() => props.companyId, (val, oldVal) => {
  if (oldVal !== undefined && val !== oldVal) {
    selectedValue.value = undefined
    emit('update:modelValue', undefined)
  }
  loadList('')
})

const loadList = async (keyword: string) => {
  loading.value = true
  try {
    const res = await request.get<{ list: any[] }>({
      url: '/oa/realname/list',
      params: {
        realName: keyword || undefined,
        companyId: props.companyId,
        status: props.activeOnly ? 'ENABLED' : undefined,
        pageSize: 50,
      },
    })
    const raw = (res as any).list || []
    let list: RealNameVO[] = raw.map((item: any) => ({
      id: item.id,
      realName: item.realName,
      idCardMask: item.idCardMasked,
      companyId: item.companyId,
      companyName: item.companyName,
      accountBoundCount: item.accountBoundCount ?? 0,
      status: item.status === 'ENABLED' ? 1 : 0,
    }))
    list = useMockFallback(list, props.companyId
      ? mockRealNameList.filter((r) => r.companyId === props.companyId)
      : mockRealNameList)
    options.value = applyExcludeBound(list, props.excludeBound, props.modelValue)
  } catch (e) {
    console.warn('[RealNameSelect] fallback to mock:', e)
    const mock = props.companyId
      ? mockRealNameList.filter((r) => r.companyId === props.companyId)
      : mockRealNameList
    options.value = import.meta.env.VITE_USE_MOCK === 'true' ? mock : []
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

onMounted(() => loadList(''))

defineExpose({ refresh: () => loadList('') })
</script>
