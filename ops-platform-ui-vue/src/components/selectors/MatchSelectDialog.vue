<!--
  MatchSelectDialog - 外部赛事选择弹窗
  关联: PRD-M2-S09 / API-M2-计划管理 §10 / ADR-016 BLK-M2-004
  使用: <MatchSelectDialog v-model:visible="visible" multiple @confirm="onConfirm" />
-->
<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择赛事"
    width="920px"
    destroy-on-close
    append-to-body
    @closed="handleClosed"
  >
    <el-form :inline="true" class="filter-form" @submit.prevent>
      <el-form-item label="日期">
        <el-date-picker
          v-model="filters.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="联赛">
        <el-select
          v-model="filters.leagueId"
          placeholder="全部联赛"
          clearable
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="item in leagueOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="球队">
        <el-input
          v-model="filters.teamKeyword"
          placeholder="搜索主队/客队"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="matchList" v-loading="loading" border stripe max-height="420px" empty-text="暂无赛事数据">
      <el-table-column prop="displayName" label="赛事名称" min-width="280" show-overflow-tooltip />
      <el-table-column prop="sClassName" label="联赛" width="140" show-overflow-tooltip />
      <el-table-column label="竞彩类型" width="100" align="center">
        <template #default="{ row }">
          {{ lotteryTypeLabel(row.lotteryType) }}
        </template>
      </el-table-column>
      <el-table-column prop="matchTime" label="比赛时间" width="160" />
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="isExcluded(row.scheduleId) || isPicked(row.scheduleId)"
            @click="handlePick(row)"
          >
            {{ isPicked(row.scheduleId) ? '已选' : '选择' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.pageNo"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @current-change="loadMatches"
      @size-change="handleSearch"
    />

    <div v-if="multiple && pickedList.length" class="picked-bar">
      <span class="picked-label">已选 {{ pickedList.length }} 项：</span>
      <el-tag
        v-for="item in pickedList"
        :key="item.scheduleId"
        closable
        size="small"
        class="picked-tag"
        @close="removePicked(item.scheduleId)"
      >
        {{ item.displayName }}
      </el-tag>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button v-if="multiple" type="primary" :disabled="!pickedList.length" @click="handleConfirm">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMatchLeagues,
  getMatchPage,
  lotteryTypeLabel,
  type MatchLeagueVO,
  type MatchVO,
} from '@/api/match'

interface Props {
  visible?: boolean
  multiple?: boolean
  /** 计划层已选 scheduleId，弹窗内不可重复选 */
  excludeIds?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  multiple: false,
  excludeIds: () => [],
})

const emit = defineEmits<{
  'update:visible': [val: boolean]
  select: [match: MatchVO]
  confirm: [matches: MatchVO[]]
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})

const loading = ref(false)
const matchList = ref<MatchVO[]>([])
const leagueOptions = ref<MatchLeagueVO[]>([])
const pickedList = ref<MatchVO[]>([])

const filters = reactive({
  date: new Date().toISOString().slice(0, 10),
  leagueId: undefined as string | undefined,
  teamKeyword: '',
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 20,
  total: 0,
})

const isExcluded = (scheduleId: string) => props.excludeIds.includes(scheduleId)

const isPicked = (scheduleId: string) => pickedList.value.some((item) => item.scheduleId === scheduleId)

const loadLeagues = async () => {
  try {
    leagueOptions.value = await getMatchLeagues()
  } catch (e) {
    console.error('[MatchSelectDialog] 联赛加载失败', e)
    leagueOptions.value = []
  }
}

const loadMatches = async () => {
  loading.value = true
  try {
    const res = await getMatchPage({
      date: filters.date,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      leagueId: filters.leagueId,
      teamKeyword: filters.teamKeyword || undefined,
    })
    matchList.value = res.list ?? []
    pagination.total = res.total ?? 0
  } catch (e) {
    console.error('[MatchSelectDialog] 赛事加载失败', e)
    matchList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNo = 1
  loadMatches()
}

const handleReset = () => {
  filters.date = new Date().toISOString().slice(0, 10)
  filters.leagueId = undefined
  filters.teamKeyword = ''
  handleSearch()
}

const handlePick = (row: MatchVO) => {
  if (isExcluded(row.scheduleId)) {
    ElMessage.warning('该赛事已在计划中')
    return
  }
  if (props.multiple) {
    if (!isPicked(row.scheduleId)) {
      pickedList.value.push(row)
    }
    return
  }
  emit('select', row)
  dialogVisible.value = false
}

const removePicked = (scheduleId: string) => {
  pickedList.value = pickedList.value.filter((item) => item.scheduleId !== scheduleId)
}

const handleConfirm = () => {
  if (!pickedList.value.length) {
    ElMessage.warning('请至少选择一项赛事')
    return
  }
  emit('confirm', [...pickedList.value])
  dialogVisible.value = false
}

const handleClosed = () => {
  pickedList.value = []
  pagination.pageNo = 1
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      loadLeagues()
      handleSearch()
    }
  },
)
</script>

<style scoped lang="scss">
.filter-form {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.picked-bar {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.picked-label {
  color: #606266;
  font-size: 13px;
}
.picked-tag {
  max-width: 320px;
}
</style>
