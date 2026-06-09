<!--
  SimCardSelect - 手机卡选择器
  使用: <SimCardSelect v-model="form.simCardId" :phone-id="form.phoneId" />
-->
<template>
  <el-select
    v-model="selectedValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled || !phoneId"
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
      :label="item.simNoMask"
      :value="item.id"
    >
      <span style="float: left">{{ item.simNoMask }}</span>
      <span style="float: right; color: #909399; font-size: 12px; margin-left: 12px">
        {{ item.carrier }}
        <span v-if="item.totalLinkedAccounts" style="margin-left: 6px; color: #e6a23c">已绑定</span>
      </span>
    </el-option>
    <template #empty>
      <el-empty v-if="!loading" :description="!phoneId ? '请先选择手机' : '未找到匹配的 SIM 卡'" :image-size="60" />
    </template>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { request } from '@/utils/request'
import { mockSimCardList } from '@/mock/selectors'
import { applyExcludeBound, debounceSearch, operatorLabel, useMockFallback } from './selector-utils'

interface SimCardVO {
  id: number
  simNoMask: string
  carrier: string
  phoneId?: number
  totalLinkedAccounts?: number
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
  phoneId?: number
  activeOnly?: boolean
  excludeBound?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  placeholder: '请选择 SIM 卡',
  clearable: true,
  disabled: false,
  filterable: true,
  multiple: false,
  remote: true,
  phoneId: undefined,
  activeOnly: true,
  excludeBound: false,
})

const emit = defineEmits<{
  'update:modelValue': [val: number | number[] | undefined]
  change: [val: number | number[] | undefined, item?: SimCardVO]
}>()

const selectedValue = ref<number | number[] | undefined>(props.modelValue)
const options = ref<SimCardVO[]>([])
const loading = ref(false)

watch(() => props.modelValue, (val) => { selectedValue.value = val })
watch(() => props.phoneId, (val, oldVal) => {
  if (oldVal !== undefined && val !== oldVal) {
    selectedValue.value = undefined
    emit('update:modelValue', undefined)
  }
  if (val) loadList('')
  else options.value = []
})

const loadList = async (keyword: string) => {
  if (!props.phoneId) {
    options.value = []
    return
  }
  loading.value = true
  try {
    const res = await request.get<{ list: any[] }>({
      url: '/oa/sim-card/list',
      params: {
        iccid: keyword || undefined,
        phoneId: props.phoneId,
        status: props.activeOnly ? 'ENABLED' : undefined,
        pageSize: 50,
      },
    })
    const raw = (res as any).list || []
    let list: SimCardVO[] = raw.map((item: any) => ({
      id: item.id,
      simNoMask: item.iccidMasked || item.phoneNumberMasked,
      carrier: operatorLabel(item.operator),
      phoneId: item.phoneId,
      totalLinkedAccounts: item.totalLinkedAccounts ?? item.accountBoundCount ?? 0,
      status: item.status === 'ENABLED' ? 1 : 0,
    }))
    list = useMockFallback(list, mockSimCardList.filter((s) => s.phoneId === props.phoneId))
    options.value = applyExcludeBound(list, props.excludeBound, props.modelValue)
  } catch (e) {
    console.warn('[SimCardSelect] fallback to mock:', e)
    options.value = import.meta.env.VITE_USE_MOCK === 'true'
      ? mockSimCardList.filter((s) => s.phoneId === props.phoneId)
      : []
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

onMounted(() => { if (props.phoneId) loadList('') })

defineExpose({ refresh: () => loadList('') })
</script>
