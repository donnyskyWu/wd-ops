<!--
  M1 - 内部作品内容详情
  路径: /internal-content/:id
-->
<template>
  <div class="internal-content-detail" v-loading="loading">
    <div class="detail-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="detail-title">{{ content?.title || '作品详情' }}</span>
        </template>
      </el-page-header>
    </div>

    <el-card v-if="content" shadow="never" class="detail-card">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
        <el-descriptions-item label="标题" :span="2">{{ content.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ content.accountName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP组">{{ content.ipGroupName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <DictLabel dict-type="dict_content_type" :value="content.contentType" />
        </el-descriptions-item>
        <el-descriptions-item label="平台">
          <DictLabel dict-type="dict_platform_type" :value="content.platformType" />
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatDateTime(content.publishTime) }}</el-descriptions-item>
        <el-descriptions-item label="阅读量">{{ formatNumber(content.readCount) }}</el-descriptions-item>
        <el-descriptions-item label="点赞">{{ formatNumber(content.likeCount) }}</el-descriptions-item>
        <el-descriptions-item label="评论">{{ formatNumber(content.commentCount) }}</el-descriptions-item>
        <el-descriptions-item label="转发">{{ formatNumber(content.forwardCount) }}</el-descriptions-item>
        <el-descriptions-item label="爆款">{{ content.isHit ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="数据来源">{{ content.dataSource || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">
          {{ (content as any).summary || (content as any).description || '（正文未单独采集，展示标题与互动指标）' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">互动趋势</el-divider>
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
      <div ref="trendChartRef" style="height: 400px;"></div>
    </el-card>

    <el-empty v-else-if="!loading" description="未找到该作品内容">
      <el-button type="primary" @click="router.push('/internal-content')">返回列表</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getContentAnalysisList, getContentTrend } from '@/api/works'
import type { ContentAnalysisVO } from '@/types/works'
import DictLabel from '@/components/DictLabel.vue'
import { formatDateTime } from '@/utils'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const content = ref<ContentAnalysisVO | null>(null)
const trendChartRef = ref<HTMLElement>()
const detailDateRange = ref<string[]>(getDefaultWeekRange())
const detailQuickRange = ref<'7d' | '30d' | 'custom'>('7d')

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
      xAxis: {
        type: 'category',
        data: trendData.map(d => d.date),
      },
      yAxis: [
        { type: 'value', name: '阅读量', position: 'left' },
        { type: 'value', name: '互动数', position: 'right' },
      ],
      series: [
        {
          name: '阅读量',
          type: 'line',
          data: trendData.map(d => d.readCount),
          smooth: true,
          lineStyle: { width: 3 },
        },
        {
          name: '互动数',
          type: 'bar',
          yAxisIndex: 1,
          data: trendData.map(d => (d.likeCount || 0) + (d.commentCount || 0) + (d.forwardCount || 0)),
          itemStyle: { color: '#67C23A' },
        },
      ],
    })
  } catch (e) {
    ElMessage.error('趋势加载失败：' + (e instanceof Error ? e.message : String(e)))
  }
}

const loadDetail = async () => {
  const contentId = Number(route.params.id)
  if (!contentId || Number.isNaN(contentId)) {
    ElMessage.error('无效的内容 ID')
    return
  }

  loading.value = true
  try {
    const row = await findContentById(contentId)
    if (!row) {
      content.value = null
      ElMessage.warning('未找到该作品内容')
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

onMounted(loadDetail)

onUnmounted(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped lang="scss">
.internal-content-detail {
  padding: 20px;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
}

.detail-card {
  border-radius: 12px;
}

.detail-trend-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
