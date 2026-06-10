<!--
  M1 - 作者看板
  依据: UX-M1 § 4 P-M1-002 详情页 + FR-M1-002
  路径: /author/:id/dashboard
-->
<template>
  <div class="author-dashboard" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/author' }">作者管理</el-breadcrumb-item>
      <el-breadcrumb-item>{{ author?.nickname || '看板' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 头部信息卡 -->
    <el-card shadow="never" v-if="author">
      <div class="author-header">
        <el-avatar :size="64" :src="author.avatar" style="background: #409eff">
          {{ author.nickname?.charAt(0) }}
        </el-avatar>
        <div class="info">
          <h2>
            {{ author.nickname }}
            <el-tag v-if="author.authorType" size="small" effect="plain" style="margin-left: 8px">
              {{ author.authorType }}
            </el-tag>
            <el-tag v-if="author.status === 1" type="success" size="small" style="margin-left: 4px">在岗</el-tag>
            <el-tag v-else type="info" size="small" style="margin-left: 4px">离职</el-tag>
          </h2>
          <p class="meta">
            <span>所属 IP 组：{{ author.ipGroupName || '-' }}</span>
            <el-divider direction="vertical" />
            <span>负责账号：{{ author.accountCount || 0 }} 个</span>
            <el-divider direction="vertical" />
            <span>负责主播：{{ author.anchorCount || 0 }} 个</span>
            <el-divider direction="vertical" />
            <span>入职：{{ author.joinedAt || '-' }}</span>
          </p>
        </div>
        <div class="actions">
          <el-button @click="router.push('/author')">返回列表</el-button>
          <el-button type="primary" @click="router.push(`/author/${author.id}/edit`)">编辑作者</el-button>
        </div>
      </div>
    </el-card>

    <!-- KPI 4 联屏 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-label">总粉丝数</div>
            <div class="kpi-value">{{ formatNumber(kpi.totalFollowers) }}</div>
            <div class="kpi-change up">{{ formatPercent(kpi.followerChangeRate) }} 较上月</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-label">月产出内容</div>
            <div class="kpi-value">{{ formatNumber(kpi.monthlyContent) }}</div>
            <div class="kpi-change up">{{ formatPercent(kpi.contentChangeRate) }} 较上月</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-label">月直播时长</div>
            <div class="kpi-value">{{ formatNumber(kpi.monthlyLiveHours) }}h</div>
            <div class="kpi-change down">{{ formatPercent(-kpi.liveChangeRate) }} 较上月</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-label">月度 ROI</div>
            <div class="kpi-value" :class="{ negative: kpi.roi < 1 }">{{ formatNumber(kpi.roi) }}</div>
            <div class="kpi-change up">{{ formatPercent(kpi.roiChangeRate) }} 较上月</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 + Tab -->
    <el-tabs v-model="activeTab" style="margin-top: 16px">
      <el-tab-pane label="粉丝趋势" name="follower">
        <el-card shadow="never">
          <div ref="followerChartRef" style="width: 100%; height: 360px"></div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="内容产出" name="content">
        <el-card shadow="never">
          <div ref="contentChartRef" style="width: 100%; height: 360px"></div>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="运营→主播关联" name="anchor">
        <ContentWrap>
          <el-table :data="anchorList" border stripe>
            <el-table-column prop="userName" label="运营人员" width="140" />
            <el-table-column prop="deptName" label="部门" min-width="160" show-overflow-tooltip />
            <el-table-column prop="relTime" label="关联时间" width="180" align="center" />
          </el-table>
          <el-empty v-if="!anchorList.length" description="暂无关联运营" />
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import { getAuthorDashboard, getAuthorOpsList } from '@/api/author'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const author = ref<any>(null)
const activeTab = ref('follower')
const kpi = reactive({
  totalFollowers: 0,
  followerChangeRate: 0,
  monthlyContent: 0,
  contentChangeRate: 0,
  monthlyLiveHours: 0,
  liveChangeRate: 0,
  roi: 0,
  roiChangeRate: 0,
})
const anchorList = ref<any[]>([])
const followerChartRef = ref<HTMLDivElement>()
const contentChartRef = ref<HTMLDivElement>()
let followerChart: echarts.ECharts | null = null
let contentChart: echarts.ECharts | null = null

const safeNumber = (n: any): number => (typeof n === 'number' && !isNaN(n) ? n : 0)

const formatNumber = (n: any) => {
  const v = safeNumber(n)
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toString()
}

const formatPercent = (n: any) => {
  const v = safeNumber(n)
  return (v >= 0 ? '+' : '') + v.toFixed(1) + '%'
}

const loadAuthor = async () => {
  loading.value = true
  const id = Number(route.params.id)
  try {
    const [dash, opsList] = await Promise.all([
      getAuthorDashboard(id),
      getAuthorOpsList(id).catch(() => []),
    ])
    // P-GATE-UNMOCK: 后端 AuthorDashboardVO 实际字段是 followerCount/contentStats/liveStats/orderAttribution，
    // 与前端类型定义不一致。后续 P-GATE-UNMOCK-R 需要同步两端类型。当前阶段用安全 fallback。
    const d: any = dash || {}
    const contentStats: any = d.contentStats || {}
    const liveStats: any = d.liveStats || {}
    const orderAttribution: any = d.orderAttribution || {}
    author.value = {
      id,
      nickname: d.authorName || '-',
      avatar: '',
      authorType: '-',
      status: 1,
      ipGroupName: d.ipGroupName || '-',
      accountCount: 0,
      anchorCount: 0,
      joinedAt: '-',
      followerTrend: Array.isArray(d.followerTrend) ? d.followerTrend : [],
      contentStats: contentStats,
      liveStats: liveStats,
      orderAttribution: orderAttribution,
    }
    Object.assign(kpi, {
      totalFollowers: safeNumber(d.followerCount),
      followerChangeRate: 0,
      monthlyContent: safeNumber(contentStats.totalCount),
      contentChangeRate: 0,
      monthlyLiveHours: safeNumber(liveStats.totalHours),
      liveChangeRate: 0,
      roi: safeNumber(orderAttribution.roi),
      roiChangeRate: 0,
    })
    anchorList.value = Array.isArray(opsList) ? opsList : []
  } catch (e) {
    console.error('[AuthorDashboard] 加载作者详情失败:', e)
    author.value = null
    anchorList.value = []
    ElMessage.error('作者详情加载失败')
  } finally {
    loading.value = false
  }
}

const initFollowerChart = () => {
  if (!followerChartRef.value) return
  followerChart = echarts.init(followerChartRef.value)
  // S-R4 修复：用真实数据 (followerTrend) 替代 Math.random() 假数据
  // spec §4.2.3 §5: 作者数据自动从 oa_follower_daily 聚合
  const trend: any[] = Array.isArray(author.value?.followerTrend) ? author.value.followerTrend : []
  const xData = trend.map((p: any) => p.date?.substring(5) || '-')
  const yData = trend.map((p: any) => p.followerCount ?? 0)
  const noData = trend.length === 0
  followerChart.setOption({
    title: { text: noData ? '近 30 天粉丝变化（暂无数据）' : '近 30 天粉丝变化', left: 'center' },
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 30, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: noData ? ['暂无数据'] : xData },
    yAxis: { type: 'value', name: '粉丝数' },
    series: [{
      name: '粉丝数',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: noData ? [0] : yData,
    }],
  })
}

const initContentChart = () => {
  if (!contentChartRef.value) return
  contentChart = echarts.init(contentChartRef.value)
  // S-R4 修复：内容产出用真实 contentStats，无后端趋势时显示空状态
  const total = safeNumber(kpi.monthlyContent)
  const noData = total === 0
  contentChart.setOption({
    title: { text: noData ? '内容产出（暂无数据）' : '作者总内容产出', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['内容数', '爆款数'], top: 30 },
    grid: { left: 60, right: 30, top: 80, bottom: 30 },
    xAxis: { type: 'category', data: noData ? ['暂无数据'] : ['总内容', '爆款'] },
    yAxis: { type: 'value', name: '数量' },
    series: [
      { name: '内容数', type: 'bar', data: noData ? [0] : [total] },
      { name: '爆款数', type: 'bar', data: noData ? [0] : [safeNumber(kpi.contentStats?.hitCount)] },
    ],
  })
}

watch(activeTab, (v) => {
  nextTick(() => {
    if (v === 'follower') initFollowerChart()
    if (v === 'content') initContentChart()
  })
})

onMounted(() => {
  loadAuthor()
  nextTick(initFollowerChart)
})
</script>

<style scoped>
.author-dashboard { padding: 20px; }
.author-header { display: flex; align-items: center; gap: 16px; }
.author-header .info { flex: 1; }
.author-header h2 { margin: 0 0 8px 0; font-size: 20px; }
.author-header .meta { color: #909399; font-size: 13px; margin: 0; }
.author-header .actions { display: flex; gap: 8px; }
.kpi { padding: 8px 0; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.4; color: #303133; }
.kpi-value.negative { color: #f56c6c; }
.kpi-change { font-size: 12px; margin-top: 4px; }
.kpi-change.up { color: #67c23a; }
.kpi-change.down { color: #f56c6c; }
</style>
