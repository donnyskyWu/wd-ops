<!--

  PhoneSelect - 手机号选择器

  使用: <PhoneSelect v-model="form.phoneId" :real-name-id="form.realnameId" />

  realNameId 可选：传入时按实名人筛选，未传时加载全部手机

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

      :label="item.label"

      :value="item.id"

    >

      <span style="float: left">{{ item.label }}</span>

      <span style="float: right; color: #909399; font-size: 12px; margin-left: 12px">

        {{ item.subLabel }}

        <span v-if="item.accountBoundCount" style="margin-left: 6px; color: #e6a23c">已绑定</span>

      </span>

    </el-option>

    <template #empty>

      <el-empty v-if="!loading" description="未找到匹配的手机" :image-size="60" />

    </template>

  </el-select>

</template>



<script setup lang="ts">

import { ref, watch, onMounted } from 'vue'

import { ElMessage } from 'element-plus'

import { request } from '@/utils/request'

import { applyExcludeBound, debounceSearch } from './selector-utils'



interface PhoneVO {

  id: number

  label: string

  subLabel: string

  realNameId?: number

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

  realNameId?: number

  activeOnly?: boolean

  excludeBound?: boolean

}



const props = withDefaults(defineProps<Props>(), {

  modelValue: undefined,

  placeholder: '请选择关联手机',

  clearable: true,

  disabled: false,

  filterable: true,

  multiple: false,

  remote: true,

  realNameId: undefined,

  activeOnly: true,

  excludeBound: false,

})



const emit = defineEmits<{

  'update:modelValue': [val: number | number[] | undefined]

  change: [val: number | number[] | undefined, item?: PhoneVO]

}>()



const selectedValue = ref<number | number[] | undefined>(props.modelValue)

const options = ref<PhoneVO[]>([])

const loading = ref(false)



watch(() => props.modelValue, (val) => { selectedValue.value = val })

watch(() => props.realNameId, (val, oldVal) => {

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

      url: '/oa/phone/list',

      params: {

        phoneNumber: keyword || undefined,

        realnameId: props.realNameId,

        status: props.activeOnly ? 'ENABLED' : undefined,

        pageSize: 50,

      },

    })

    const raw = (res as any).list || []

    let list: PhoneVO[] = raw.map((item: any) => {

      const code = item.phoneCode ? `[${item.phoneCode}] ` : ''

      const mask = item.phoneNumberMasked || '未知号码'

      const model = item.phoneModel || '-'

      return {

        id: item.id,

        label: `${code}${mask}`,

        subLabel: model,

        realNameId: item.realnameId,

        accountBoundCount: item.accountBoundCount ?? 0,

        status: item.status === 'ENABLED' ? 1 : 0,

      }

    })

    options.value = applyExcludeBound(list, props.excludeBound, props.modelValue)

  } catch (e) {

    console.error('[PhoneSelect] 加载手机号失败:', e)
    options.value = []
    ElMessage.error('手机号列表加载失败，请稍后重试')

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



onMounted(() => { loadList('') })



defineExpose({ refresh: () => loadList('') })

</script>

