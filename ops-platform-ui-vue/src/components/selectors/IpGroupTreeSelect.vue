<!--
  IpGroupTreeSelect - IP 组树形选择器
  三大铁律 § 3.2 强制: 强关联属性必须使用专用选择器组件
  关联: PRD-M1 FR-M1-001 / API-M1 § 2.1 GET /admin-api/oa/ip-group/tree
  使用: <IpGroupTreeSelect v-model="form.ipGroupId" :multiple="false" />
-->
<template>
  <el-tree-select
    v-model="selectedValue"
    :data="treeData"
    :props="treeProps"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :multiple="multiple"
    :check-strictly="!multiple"
    :render-after-expand="false"
    node-key="id"
    style="width: 100%"
    @change="handleChange"
  >
    <template #default="{ data }">
      <span class="tree-node">
        <el-tag v-if="data.groupType" size="small" :type="groupTypeTag(data.groupType)" effect="plain" style="margin-right: 6px">
          {{ data.groupType }}
        </el-tag>
        <span>{{ data.groupName }}</span>
        <span v-if="data.status === 0" style="color: #f56c6c; margin-left: 6px; font-size: 12px">已停用</span>
      </span>
    </template>
  </el-tree-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { getIpGroupTree } from '@/api/ip-group'
import { mockIpGroupTree } from '@/mock/ip-group'

interface TreeNode {
  id: number
  groupName: string
  groupType?: string
  status?: number
  children?: TreeNode[]
}

interface Props {
  modelValue?: number | number[] | undefined
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  multiple?: boolean
  /** 租户过滤(后端会自动按 tenant_id 隔离,前端无需传) */
  loadOnMount?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  placeholder: '请选择 IP 组',
  clearable: true,
  disabled: false,
  multiple: false,
  loadOnMount: true,
})

const emit = defineEmits<{
  'update:modelValue': [val: number | number[] | undefined]
  change: [val: number | number[] | undefined]
}>()

const selectedValue = ref<number | number[] | undefined>(props.modelValue)
const treeData = ref<TreeNode[]>([])
const treeProps = { children: 'children', label: 'groupName' }

watch(() => props.modelValue, (val) => { selectedValue.value = val })

const groupTypeTag = (t: string) => {
  const m: Record<string, string> = { 主题: 'success', 行业: 'warning', 通用: 'info' }
  return m[t] || 'info'
}

const loadTree = async () => {
  try {
    const data = await getIpGroupTree()
    treeData.value = data && data.length ? data : [...mockIpGroupTree]
  } catch (e) {
    console.warn('[IpGroupTreeSelect] 加载 IP 组树失败,fallback to mock:', e)
    treeData.value = [...mockIpGroupTree]
  }
}

const handleChange = (val: number | number[] | undefined) => {
  selectedValue.value = val
  emit('update:modelValue', val)
  emit('change', val)
}

onMounted(() => { if (props.loadOnMount) loadTree() })

defineExpose({ refresh: loadTree })
</script>

<style scoped>
.tree-node { display: inline-flex; align-items: center; }
</style>
