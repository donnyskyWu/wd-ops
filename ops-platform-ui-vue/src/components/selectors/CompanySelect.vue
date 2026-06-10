<!--

  CompanySelect - 公司选择器

  使用: <CompanySelect v-model="form.companyId" :include-quota="true" />

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

      :label="item.companyName"

      :value="item.id"

    >

      <span style="float: left">{{ item.companyName }}</span>

      <span style="float: right; color: #909399; font-size: 12px; margin-left: 12px">

        <template v-if="includeQuota">

          容量 {{ item.usedQuota }}/{{ item.totalQuota }}

        </template>

        <template v-else>

          {{ item.creditCode || item.legalPerson || '-' }}

        </template>

      </span>

    </el-option>

    <template #empty>

      <el-empty v-if="!loading" description="未找到匹配的公司" :image-size="60" />

    </template>

  </el-select>

</template>



<script setup lang="ts">

import { ref, watch, onMounted } from 'vue'

import { ElMessage } from 'element-plus'

import { request } from '@/utils/request'

import { debounceSearch } from './selector-utils'



interface CompanyVO {

  id: number

  companyName: string

  creditCode?: string

  legalPerson?: string

  usedQuota?: number

  totalQuota?: number

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

  includeQuota?: boolean

  activeOnly?: boolean

}



const props = withDefaults(defineProps<Props>(), {

  modelValue: undefined,

  placeholder: '请选择公司',

  clearable: true,

  disabled: false,

  filterable: true,

  multiple: false,

  remote: true,

  includeQuota: false,

  activeOnly: true,

})



const emit = defineEmits<{

  'update:modelValue': [val: number | number[] | undefined]

  change: [val: number | number[] | undefined, item?: CompanyVO]

}>()



const selectedValue = ref<number | number[] | undefined>(props.modelValue)

const options = ref<CompanyVO[]>([])

const loading = ref(false)



watch(() => props.modelValue, (val) => { selectedValue.value = val })



const loadList = async (keyword: string) => {

  loading.value = true

  try {

    const res = await request.get<{ list: any[] }>({

      url: '/oa/company/list',

      params: {

        companyName: keyword || undefined,

        status: props.activeOnly ? 'ENABLED' : undefined,

        pageSize: 50,

      },

    })

    const raw = (res as any).list || []

    const list = raw.map((item: any) => ({

      id: item.id,

      companyName: item.companyName,

      creditCode: item.creditCode,

      legalPerson: item.legalName,

      usedQuota: item.mpRegisteredCount ?? 0,

      totalQuota: item.mpCapacityStandard ?? 0,

      status: item.status === 'ENABLED' ? 1 : 0,

    }))

    options.value = list // P-GATE-UNMOCK S-A: 已去除 mock 兜底

  } catch (e) {

    console.error('[CompanySelect] 加载公司失败:', e)
    options.value = []
    ElMessage.error('公司列表加载失败，请稍后重试')

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


