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
    :placeholder="effectivePlaceholder"
    :clearable="clearable"
    :disabled="disabled"
    :multiple="multiple"
    :check-strictly="!multiple"
    :render-after-expand="false"
    node-key="id"
    style="width: 100%"
    @change="handleChange"
  >
    <template #default="scope">
      <span v-if="scope?.data" class="tree-node">
        <el-tag v-if="scope.data.groupType" size="small" :type="groupTypeTag(scope.data.groupType)" effect="plain" style="margin-right: 6px">
          {{ scope.data.groupType }}
        </el-tag>
        <span>{{ scope.data.groupName }}</span>
        <span v-if="scope.data.status === 0" style="color: #f56c6c; margin-left: 6px; font-size: 12px">已停用</span>
      </span>
    </template>
  </el-tree-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getIpGroupTree, getAccessibleIpGroupTree } from '@/api/ip-group'

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
  /** accessible=当前用户可访问 IP 组（成员∪组长；admin 全树）；默认 all=管理全树 */
  scope?: 'all' | 'accessible'
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  placeholder: '请选择 IP 组',
  clearable: true,
  disabled: false,
  multiple: false,
  loadOnMount: true,
  scope: 'all',
})

const emit = defineEmits<{
  'update:modelValue': [val: number | number[] | undefined]
  change: [val: number | number[] | undefined]
}>()

const selectedValue = ref<number | number[] | undefined>(props.modelValue)
const treeData = ref<TreeNode[]>([])
const treeProps = { children: 'children', label: 'groupName' }
const effectivePlaceholder = computed(() =>
  props.placeholder ?? (props.scope === 'accessible' ? '可选缩小范围' : '请选择 IP 组'),
)

watch(() => props.modelValue, (val) => { selectedValue.value = val })

const groupTypeTag = (t: string) => {
  const m: Record<string, string> = { 主题: 'success', 行业: 'warning', 通用: 'info' }
  return m[t] || 'info'
}

const loadTree = async () => {
  try {
    const loader = props.scope === 'accessible' ? getAccessibleIpGroupTree : getIpGroupTree
    const data = await loader()
    treeData.value = data && data.length ? data : []
  } catch (e) {
    console.error('[IpGroupTreeSelect] 加载 IP 组树失败:', e)
    treeData.value = []
    ElMessage.error('IP 组树加载失败，请稍后重试')
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
