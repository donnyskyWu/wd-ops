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
            <div class="kpi-change up">+{{ kpi.followerChangeRate }}% 较上月</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-label">月产出内容</div>
            <div class="kpi-value">{{ kpi.monthlyContent }}</div>
            <div class="kpi-change up">+{{ kpi.contentChangeRate }}% 较上月</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-label">月直播时长</div>
            <div class="kpi-value">{{ kpi.monthlyLiveHours }}h</div>
            <div class="kpi-change down">-{{ kpi.liveChangeRate }}% 较上月</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-label">月度 ROI</div>
            <div class="kpi-value" :class="{ negative: kpi.roi < 1 }">{{ kpi.roi.toFixed(2) }}</div>
            <div class="kpi-change up">+{{ kpi.roiChangeRate }}% 较上月</div>
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
            <el-table-column prop="anchorName" label="主播" width="140" />
            <el-table-column prop="platformName" label="平台" width="120" />
            <el-table-column prop="relType" label="关联类型" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.relType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="boundAt" label="关联时间" width="180" align="center" />
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
          </el-table>
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'

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

const formatNumber = (n: number) => n >= 10000 ? (n / 10000).toFixed(1) + '万' : n.toString()

const loadAuthor = async () => {
  loading.value = true
  const id = Number(route.params.id)
  // TODO: GET /admin-api/oa/author/{id}
  await new Promise((r) => setTimeout(r, 300))
  author.value = {
    id,
    nickname: '张三',
    avatar: '',
    authorType: '内容作者',
    status: 1,
    ipGroupName: 'A 大组',
    accountCount: 8,
    anchorCount: 3,
    joinedAt: '2025-03-15',
  }
  Object.assign(kpi, {
    totalFollowers: 1280000,
    followerChangeRate: 12.5,
    monthlyContent: 86,
    contentChangeRate: 8.3,
    monthlyLiveHours: 142,
    liveChangeRate: 3.2,
    roi: 1.65,
    roiChangeRate: 5.7,
  })
  anchorList.value = [
    { anchorName: '李四', platformName: '抖音', relType: '内容+直播', boundAt: '2025-04-01 10:00:00', remark: '重点培养对象' },
    { anchorName: '王五', platformName: '快手', relType: '内容', boundAt: '2025-05-15 14:00:00', remark: '' },
  ]
  loading.value = false
}

const initFollowerChart = () => {
  if (!followerChartRef.value) return
  followerChart = echarts.init(followerChartRef.value)
  followerChart.setOption({
    title: { text: '近 30 天粉丝变化', left: 'center' },
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 30, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: Array.from({ length: 30 }, (_, i) => `${i + 1}日`) },
    yAxis: { type: 'value', name: '粉丝数' },
    series: [{
      name: '粉丝数',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: Array.from({ length: 30 }, (_, i) => 1200000 + i * 3500 + Math.random() * 2000),
    }],
  })
}

const initContentChart = () => {
  if (!contentChartRef.value) return
  contentChart = echarts.init(contentChartRef.value)
  contentChart.setOption({
    title: { text: '近 30 天内容产出', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['视频', '文章', '直播'], top: 30 },
    grid: { left: 60, right: 30, top: 80, bottom: 30 },
    xAxis: { type: 'category', data: Array.from({ length: 30 }, (_, i) => `${i + 1}日`) },
    yAxis: { type: 'value', name: '数量' },
    series: [
      { name: '视频', type: 'bar', stack: 'c', data: Array.from({ length: 30 }, () => Math.floor(Math.random() * 5)) },
      { name: '文章', type: 'bar', stack: 'c', data: Array.from({ length: 30 }, () => Math.floor(Math.random() * 3)) },
      { name: '直播', type: 'bar', stack: 'c', data: Array.from({ length: 30 }, () => Math.floor(Math.random() * 2)) },
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
