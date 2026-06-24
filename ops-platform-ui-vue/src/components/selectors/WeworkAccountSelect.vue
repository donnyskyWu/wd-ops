<!--

  WeworkAccountSelect - 企业微信应用选择器

  关联: API-M4 GET /admin-api/oa/internal/wework/list

  使用: <WeworkAccountSelect v-model="form.accountId" />

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

      :label="`${item.accountName} (${item.corpId})`"

      :value="item.id"

    >

      <span style="float: left">{{ item.accountName }}</span>

      <span style="float: right; color: #909399; font-size: 12px; margin-left: 12px">

        Agent {{ item.agentId || '-' }}

        <span v-if="item.connStatus" style="margin-left: 8px">{{ item.connStatus }}</span>

      </span>

    </el-option>

    <template #empty>

      <el-empty v-if="!loading" description="未找到匹配的企微应用" :image-size="60" />

    </template>

  </el-select>

</template>



<script setup lang="ts">

import { ref, watch, onMounted } from 'vue'

import { ElMessage } from 'element-plus'

import { getWeworkPage, type WeworkVO } from '@/api/personal-account'

import { debounceSearch } from './selector-utils'



interface Props {

  modelValue?: number | number[] | undefined

  placeholder?: string

  clearable?: boolean

  disabled?: boolean

  filterable?: boolean

  multiple?: boolean

  remote?: boolean

  activeOnly?: boolean

}



const props = withDefaults(defineProps<Props>(), {

  modelValue: undefined,

  placeholder: '请选择企微应用',

  clearable: true,

  disabled: false,

  filterable: true,

  multiple: false,

  remote: true,

  activeOnly: true,

})



const emit = defineEmits<{

  'update:modelValue': [val: number | number[] | undefined]

  change: [val: number | number[] | undefined, item?: WeworkVO]

}>()



const selectedValue = ref<number | number[] | undefined>(props.modelValue)

const options = ref<WeworkVO[]>([])

const loading = ref(false)



watch(() => props.modelValue, (val) => { selectedValue.value = val })



const loadList = async (keyword: string) => {

  loading.value = true

  try {

    const res = await getWeworkPage({

      accountName: keyword || undefined,

      status: props.activeOnly ? 'ENABLED' : undefined,

      pageNo: 1,

      pageSize: 50,

    })

    options.value = res.list || []

  } catch (e) {

    console.error('[WeworkAccountSelect] 加载企微应用失败:', e)

    options.value = []

    ElMessage.error('企微应用列表加载失败，请稍后重试')

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
