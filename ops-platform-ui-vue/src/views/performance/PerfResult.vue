<template>
  <div class="perf-result-page">

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="人员">
        <el-select v-model="searchForm.evaluateeId" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="张三" :value="1" />
          <el-option label="李四" :value="2" />
          <el-option label="王五" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="岗位">
        <el-select v-model="searchForm.position" placeholder="请选择" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="公众号运营" value="MP_OPS" />
          <el-option label="抖音运营" value="DOUYIN_OPS" />
        </el-select>
      </el-form-item>
      <el-form-item label="绩效等级">
        <DictSelect v-model="searchForm.grade" dict-type="dict_perf_grade" placeholder="请选择" />
      </el-form-item>
    </TableSearch>

    <div class="action-bar">
      <el-button @click="handleExport">
        <el-icon><Download /></el-icon>
        导出Excel
      </el-button>
      <span class="total-info">共 {{ total }} 条</span>
    </div>

    <el-table :data="resultList" v-loading="loading" stripe>
      <el-table-column prop="evaluateeName" label="人员" width="100" />
      <el-table-column prop="position" label="岗位" width="120" />
      <el-table-column prop="cycleDisplay" label="考核周期" width="140" />
      <el-table-column prop="totalScore" label="总分" width="100" align="center">
        <template #default="{ row }">
          <strong>{{ row.totalScore }}分</strong>
        </template>
      </el-table-column>
      <el-table-column prop="grade" label="等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getGradeType(row.grade)" effect="dark" size="small">
            {{ getGradeIcon(row.grade) }} {{ row.grade }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="evaluatorName" label="考核人" width="100" />
      <el-table-column prop="confirmedAt" label="确认时间" width="140" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" @click="handleTrend(row)">趋势</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="searchForm.pageNo"
      :page-size="searchForm.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @update:current-page="(val) => searchForm.pageNo = val"
      @update:page-size="(val) => { searchForm.pageSize = val; handleSearch() }"
      @current-change="handleSearch"
      @size-change="handleSearch"
    />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="绩效详情" width="700px">
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="被考核人">{{ currentDetail.evaluateeName }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ currentDetail.position }}</el-descriptions-item>
          <el-descriptions-item label="考核周期">{{ currentDetail.cycleDisplay }}</el-descriptions-item>
          <el-descriptions-item label="总分">
            <strong style="color: #409eff; font-size: 18px">{{ currentDetail.totalScore }}分</strong>
          </el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag :type="getGradeType(currentDetail.grade)" effect="dark">
              {{ getGradeIcon(currentDetail.grade) }} {{ currentDetail.grade }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="考核人">{{ currentDetail.evaluatorName }}</el-descriptions-item>
        </el-descriptions>

        <h3 style="margin: 24px 0 12px">指标明细</h3>
        <el-table :data="currentDetail.items" border size="small">
          <el-table-column prop="metricName" label="指标名称" min-width="140" />
          <el-table-column prop="weight" label="权重" width="80" align="center">
            <template #default="{ row }">{{ row.weight }}%</template>
          </el-table-column>
          <el-table-column prop="actualValue" label="实际值" width="100" align="center" />
          <el-table-column prop="score" label="得分" width="80" align="center" />
          <el-table-column prop="weightedScore" label="加权得分" width="100" align="center" />
          <el-table-column prop="grade" label="等级" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getGradeType(row.grade)" size="small">{{ row.grade }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- 趋势对话框 -->
    <el-dialog v-model="trendVisible" :title="`${trendUserName}的绩效趋势`" width="800px">
      <div class="trend-header">
        <span>员工: <strong>{{ trendUserName }}</strong></span>
        <span>岗位: <strong>{{ trendPosition }}</strong></span>
        <el-select v-model="trendPeriod" style="width: 150px">
          <el-option label="最近3个月" value="3" />
          <el-option label="最近6个月" value="6" />
          <el-option label="最近12个月" value="12" />
        </el-select>
      </div>

      <div ref="trendChartRef" style="height: 350px; margin-top: 20px"></div>

      <h3 style="margin: 24px 0 12px">历史考核记录</h3>
      <div class="history-list">
        <el-tag
          v-for="(item, index) in historyList"
          :key="index"
          :type="getGradeType(item.grade)"
          class="history-tag"
        >
          {{ item.cycle }}: {{ item.score }}分 {{ item.grade }}
        </el-tag>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  getPerfResultList,
  getPerfUserTrend,
  exportPerfResult,
} from '@/api/perfResult'

const loading = ref(false)
const resultList = ref<any[]>([])
const total = ref(0)
const router = useRouter()

const searchForm = reactive({
  pageNo: 1,
  pageSize: 20,
  evaluateeId: undefined as number | undefined,
  position: undefined as string | undefined,
  grade: undefined as string | undefined,
})

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getPerfResultList({
      userId: searchForm.evaluateeId,
      grade: searchForm.grade,
      pageNum: searchForm.pageNo,
      pageSize: searchForm.pageSize,
    })
    resultList.value = (res.list || []).map((row: any) => ({
      id: row.id,
      evaluateeName: row.userName || row.evaluateeName || '',
      position: row.position || '',
      cycleDisplay: row.periodDisplay || (row.periodType || '') + ' ' + (row.periodStart || ''),
      totalScore: row.totalScore,
      grade: row.grade,
      evaluatorName: row.evaluatorName || '',
      confirmedAt: row.confirmedAt || row.createTime || '',
    }))
    total.value = res.total ?? 0
  } catch {
    resultList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNo = 1
  loadList()
}

const handleReset = () => {
  searchForm.evaluateeId = undefined
  searchForm.position = undefined
  searchForm.grade = undefined
  handleSearch()
}

const getGradeType = (grade: string) => {
  const map: Record<string, any> = {
    S: 'warning',
    A: 'success',
    B: '',
    C: 'warning',
    D: 'danger',
  }
  return map[grade] || ''
}

const getGradeIcon = (grade: string) => {
  const map: Record<string, string> = {
    S: '⭐',
    A: '🏅',
    B: '📊',
    C: '📉',
    D: '❌',
  }
  return map[grade] || ''
}

const handleExport = async () => {
  try {
    const ids = resultList.value.map((r: any) => r.id).filter(Boolean)
    const res: any = await exportPerfResult(ids)
    ElMessage.success(`导出任务已创建：${res?.jobId || res?.id || '已提交'}`)
  } catch {
    // 错误已拦截
  }
}

// 详情
const detailVisible = ref(false)
const currentDetail = ref<any>(null)

const handleDetail = (row: any) => {
  currentDetail.value = {
    ...row,
    items: [
      { metricName: '粉丝增长数', weight: 30, actualValue: 1250, score: 85, weightedScore: 25.5, grade: 'A' },
      { metricName: '内容发布完成率', weight: 25, actualValue: 95, score: 90, weightedScore: 22.5, grade: 'A' },
      { metricName: '转化率', weight: 20, actualValue: 3.2, score: 80, weightedScore: 16, grade: 'B' },
      { metricName: '内容质量评分', weight: 15, actualValue: 88, score: 88, weightedScore: 13.2, grade: 'A' },
      { metricName: '任务及时完成率', weight: 10, actualValue: 100, score: 100, weightedScore: 10, grade: 'S' },
    ],
  }
  detailVisible.value = true
}

// 趋势
const trendVisible = ref(false)
const trendUserName = ref('')
const trendPosition = ref('')
const trendPeriod = ref('6')
const trendChartRef = ref<HTMLElement>()
const historyList = ref<any[]>([])

const handleTrend = async (row: any) => {
  router.push(`/perf/result/${row.userId || row.evaluateeId || row.id}/trend`)
  trendPosition.value = row.position
  trendUserName.value = row.evaluateeName
  trendVisible.value = true
  historyList.value = []

  try {
    const res: any = await getPerfUserTrend(row.userId || row.evaluateeId || row.id)
    historyList.value = (res.points || res.historyList || []).map((p: any) => ({
      cycle: p.period || p.cycle,
      score: p.score ?? p.totalScore,
      grade: p.grade,
    }))
  } catch {
    historyList.value = []
  }

  nextTick(() => renderTrendChart())
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return

  const chart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: historyList.value.map(h => h.cycle).reverse(),
    },
    yAxis: {
      type: 'value',
      min: 50,
      max: 100,
      name: '分数',
    },
    series: [
      {
        name: '绩效分数',
        type: 'line',
        data: historyList.value.map(h => h.score).reverse(),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#409eff' },
        lineStyle: { width: 3 },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}分',
        },
      },
    ],
  }
  chart.setOption(option)
}

onMounted(() => loadList())
</script>

<style scoped>
.perf-result-page {
  padding: 20px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.total-info {
  color: #909399;
  font-size: 14px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.trend-header {
  display: flex;
  align-items: center;
  gap: 24px;
  font-size: 14px;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  margin: 0;
}
</style>
