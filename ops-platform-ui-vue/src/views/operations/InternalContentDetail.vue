<!--
  M1 - 内部作品内容详情（列表抽屉面板 / 独立路由页）
  路径: /internal-content/:id
-->
<template>
  <div
    class="internal-content-detail-panel"
    :class="{ 'is-page': isPageMode }"
    v-loading="loading"
  >
    <template v-if="content">
      <div v-if="isPageMode" class="page-toolbar">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" @click="router.push(opsRouteTo({ name: 'InternalContent' }))">
          返回列表
        </el-button>
      </div>

      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 class="title">
              {{ content.title || '—' }}
              <el-tag v-if="content.isHit" type="danger" style="margin-left: 8px">爆款</el-tag>
            </h2>
            <p class="meta">
              <span>账号：{{ content.accountName || '—' }}</span>
              <el-divider direction="vertical" />
              <span>IP 组：{{ content.ipGroupName || '—' }}</span>
            </p>
            <p class="meta">
              <span>平台：<DictLabel dict-type="dict_platform_type" :value="content.platformType" /></span>
              <el-divider direction="vertical" />
              <span>类型：<DictLabel dict-type="dict_content_type" :value="content.contentType" /></span>
              <el-divider direction="vertical" />
              <span>发布：{{ formatDateTime(content.publishTime) }}</span>
            </p>
          </div>
        </div>
      </el-card>

      <el-row :gutter="16" class="stats-row">
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">阅读/播放</div>
            <div class="stat-value primary">{{ formatNumber(content.readCount) }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">点赞</div>
            <div class="stat-value success">{{ formatNumber(content.likeCount) }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">评论</div>
            <div class="stat-value warning">{{ formatNumber(content.commentCount) }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">转发</div>
            <div class="stat-value muted">{{ formatNumber(content.forwardCount) }}</div>
          </el-card>
        </el-col>
      </el-row>

      <ContentWrap title="作品信息" style="margin-top: 16px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">{{ content.title || '—' }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ content.accountName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="IP 组">{{ content.ipGroupName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <DictLabel dict-type="dict_content_type" :value="content.contentType" />
          </el-descriptions-item>
          <el-descriptions-item label="平台">
            <DictLabel dict-type="dict_platform_type" :value="content.platformType" />
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatDateTime(content.publishTime) }}</el-descriptions-item>
          <el-descriptions-item label="数据来源">{{ content.dataSource || '—' }}</el-descriptions-item>
          <el-descriptions-item label="爆款">
            <el-tag v-if="content.isHit" type="danger" size="small">是</el-tag>
            <span v-else>否</span>
          </el-descriptions-item>
          <el-descriptions-item label="内容摘要" :span="2">
            {{ summaryText }}
          </el-descriptions-item>
        </el-descriptions>
      </ContentWrap>

      <ContentWrap title="互动趋势">
        <template #extra>
          <div class="detail-trend-toolbar">
            <el-date-picker
              v-model="detailDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="handleDetailDateChange"
            />
            <el-radio-group v-model="detailQuickRange" @change="(val) => handleDetailQuickRange(String(val))">
              <el-radio-button label="7d">近 7 日</el-radio-button>
              <el-radio-button label="30d">近 30 日</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="trend-chart" />
      </ContentWrap>
    </template>

    <el-empty v-else-if="!loading" description="未找到该作品内容">
      <el-button v-if="isPageMode" type="primary" @click="router.push(opsRouteTo({ name: 'InternalContent' }))">
        返回列表
      </el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import ContentWrap from '@/components/ContentWrap.vue'
import DictLabel from '@/components/DictLabel.vue'
import { getContentAnalysisList, getContentTrend } from '@/api/works'
import type { ContentAnalysisVO } from '@/types/works'
import { formatDateTime } from '@/utils'
import { opsRouteTo } from '@/utils/ops-route'

const props = defineProps<{
  /** 抽屉面板模式：由列表页传入 */
  contentId?: number
  /** 列表行快照，避免重复分页检索 */
  initialContent?: ContentAnalysisVO | null
}>()

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const content = ref<ContentAnalysisVO | null>(null)
const trendChartRef = ref<HTMLElement>()
const detailDateRange = ref<string[]>(getDefaultWeekRange())
const detailQuickRange = ref<'7d' | '30d' | 'custom'>('7d')

const isPageMode = computed(() => props.contentId == null)

const effectiveContentId = computed(() => {
  if (props.contentId != null) return props.contentId
  const id = Number(route.params.id)
  return Number.isNaN(id) ? 0 : id
})

const summaryText = computed(() => {
  const row = content.value as (ContentAnalysisVO & { summary?: string; description?: string }) | null
  return row?.summary || row?.description || '（正文未单独采集，展示标题与互动指标）'
})

let trendChart: echarts.ECharts | null = null

function getDefaultWeekRange(): string[] {
  const end = dayjs().format('YYYY-MM-DD')
  const start = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
  return [start, end]
}

const formatNumber = (num: number) => (num ?? 0).toLocaleString('zh-CN')

async function findContentById(contentId: number): Promise<ContentAnalysisVO | null> {
  const pageSize = 100
  for (let page = 1; page <= 20; page += 1) {
    const res = await getContentAnalysisList({ page, size: pageSize })
    const found = res?.list?.find((item) => item.id === contentId)
    if (found) return found
    if ((res?.list?.length ?? 0) < pageSize) break
  }
  return null
}

const handleDetailQuickRange = (val: string) => {
  if (val === '7d') {
    detailDateRange.value = getDefaultWeekRange()
  } else if (val === '30d') {
    detailDateRange.value = [
      dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ]
  }
  if (content.value?.id) {
    renderTrendChart(content.value.id)
  }
}

const handleDetailDateChange = () => {
  detailQuickRange.value = 'custom'
  if (content.value?.id) {
    renderTrendChart(content.value.id)
  }
}

const renderTrendChart = async (contentId: number) => {
  await nextTick()
  if (!trendChartRef.value || trendChartRef.value.getBoundingClientRect().width === 0) {
    setTimeout(() => renderTrendChart(contentId), 100)
    return
  }
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }

  const [startDate, endDate] = detailDateRange.value?.length === 2
    ? detailDateRange.value
    : getDefaultWeekRange()

  try {
    const trendData = await getContentTrend({ contentId, startDate, endDate })
    const chart = echarts.init(trendChartRef.value)
    trendChart = chart
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['阅读量', '互动数'] },
      grid: { left: 48, right: 48, top: 40, bottom: 32 },
      xAxis: {
        type: 'category',
        data: trendData.map((d) => d.date),
      },
      yAxis: [
        { type: 'value', name: '阅读量', position: 'left' },
        { type: 'value', name: '互动数', position: 'right' },
      ],
      series: [
        {
          name: '阅读量',
          type: 'line',
          data: trendData.map((d) => d.readCount),
          smooth: true,
          lineStyle: { width: 3 },
        },
        {
          name: '互动数',
          type: 'bar',
          yAxisIndex: 1,
          data: trendData.map(
            (d) => (d.likeCount || 0) + (d.commentCount || 0) + (d.forwardCount || 0),
          ),
          itemStyle: { color: '#67C23A' },
        },
      ],
    })
  } catch (e) {
    ElMessage.error('趋势加载失败：' + (e instanceof Error ? e.message : String(e)))
  }
}

const loadDetail = async (contentId: number) => {
  if (!contentId) {
    ElMessage.error('无效的内容 ID')
    content.value = null
    return
  }

  loading.value = true
  try {
    const row = props.initialContent?.id === contentId
      ? props.initialContent
      : await findContentById(contentId)
    if (!row) {
      content.value = null
      if (isPageMode.value) {
        ElMessage.warning('未找到该作品内容')
      }
      return
    }
    content.value = row
    detailDateRange.value = getDefaultWeekRange()
    detailQuickRange.value = '7d'
    await renderTrendChart(contentId)
  } catch (e) {
    console.error('[InternalContentDetail] 加载失败:', e)
    ElMessage.error('作品详情加载失败')
    content.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => [effectiveContentId.value, props.initialContent] as const,
  ([contentId]) => {
    if (contentId) loadDetail(contentId)
  },
  { immediate: true },
)

onUnmounted(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped lang="scss">
.internal-content-detail-panel {
  min-height: 120px;

  &.is-page {
    padding: 20px;
  }
}

.page-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 8px 0 0;
}

.stats-row {
  margin-top: 16px;
}

.stat-card {
  text-align: center;

  :deep(.el-card__body) {
    padding: 16px 12px;
  }
}

.stat-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;

  &.primary {
    color: var(--el-color-primary);
  }

  &.success {
    color: var(--el-color-success);
  }

  &.warning {
    color: var(--el-color-warning);
  }

  &.muted {
    color: var(--el-text-color-secondary);
  }
}

.detail-trend-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.trend-chart {
  height: 360px;
  width: 100%;
}
</style>
