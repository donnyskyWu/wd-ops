<template>
  <div class="workbench-todos" v-loading="loading">
    <el-page-header @back="router.back()" title="返回" content="全部待办" />

    <el-card shadow="never" style="margin-top: 16px">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="ALL" />
        <el-tab-pane
          v-for="tab in TODO_TABS"
          :key="tab.source"
          :label="`${tab.label}${tabCount(tab.source) ? ` (${tabCount(tab.source)})` : ''}`"
          :name="tab.source"
        />
      </el-tabs>

      <el-table :data="filteredTodos" stripe style="width: 100%" @row-click="handleTodoClick">
        <el-table-column prop="title" label="标题" min-width="280" show-overflow-tooltip />
        <el-table-column prop="source" label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ formatTodoSource(row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="navigateTo(resolveOpsUrl(row.actionUrl))">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredTodos.length === 0" description="暂无待办" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHomeTodos, type HomeTodoVO } from '@/api/dashboard'
import { formatDateTime } from '@/utils'
import { resolveOpsNavUrl } from '@/utils/ops-route'

const router = useRouter()
const loading = ref(false)
const todoList = ref<HomeTodoVO[]>([])
const activeTab = ref('ALL')

const TODO_TABS = [
  { source: 'IMPORT', label: '数据补录' },
  { source: 'SOP', label: 'SOP' },
  { source: 'PUBLISH', label: '内容审核' },
  { source: 'PERF', label: '绩效' },
  { source: 'INTEGRATION', label: '集成' },
] as const

const SOURCE_LABEL: Record<string, string> = {
  SOP: 'SOP',
  PUBLISH: '内容审核',
  PERF: '绩效',
  INTEGRATION: '集成',
  IMPORT: '数据补录',
}

function formatTodoSource(source: string) {
  return SOURCE_LABEL[source] || source
}

function tabCount(source: string) {
  return todoList.value.filter((t) => t.source === source).length
}

const filteredTodos = computed(() => {
  if (activeTab.value === 'ALL') {
    return todoList.value
  }
  return todoList.value.filter((t) => t.source === activeTab.value)
})

function handleTabChange() {
  // tab 切换仅过滤本地列表
}

async function navigateTo(route: string) {
  if (route) await router.push(route)
}

function handleTodoClick(row: HomeTodoVO) {
  navigateTo(resolveOpsNavUrl(row.actionUrl))
}

async function loadTodos() {
  loading.value = true
  try {
    todoList.value = await getHomeTodos({ limit: 50 })
  } finally {
    loading.value = false
  }
}

onMounted(loadTodos)
</script>

<style scoped lang="scss">
.workbench-todos {
  padding: 20px 24px;
}
</style>

