<template>
  <div class="efficiency-page">
    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="统计周期">
        <DictSelect v-model="searchForm.period" dict-type="dict_time_dimension" style="width: 120px" />
      </el-form-item>
      <el-form-item label="统计日期">
        <el-date-picker v-model="searchForm.statDate" type="month" placeholder="选择月份" style="width: 160px" />
      </el-form-item>
      <el-form-item label="岗位">
        <DictSelect v-model="searchForm.position" dict-type="dict_position_type" placeholder="全部岗位" clearable style="width: 160px" />
      </el-form-item>
    </TableSearch>

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <span>共 <strong>{{ totalCount }}</strong> 条记录</span>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon> 导出
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="main-tabs">
        <!-- 公众号运营数据 -->
        <el-tab-pane label="公众号运营数据统计" name="wechat">
          <el-table :data="wechatList" v-loading="loading" border stripe row-key="rank">
            <el-table-column type="expand" width="50">
              <template #default="{ row }">
                <div class="expand-content">
                  <el-row :gutter="16">
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#409eff"><Document /></el-icon> 任务完成情况</div>
                          <div class="expand-row"><span>已完成任务: <b>{{ row.taskCompleted }}</b></span><span>完成率: <b>{{ row.taskRate }}%</b></span></div>
                          <el-progress :percentage="row.taskRate" :stroke-width="6" />
                          <div class="expand-desc">原创文章{{ row.originalCount }}篇 | 审核通过{{ row.approvedCount }}篇 | 退回{{ row.rejectedCount }}篇</div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#e6a23c"><Coin /></el-icon> 财务指标</div>
                          <div class="expand-row"><span>投入成本: <b>¥{{ row.inputCost.toLocaleString() }}</b></span><span>产出价值: <b style="color: #67c23a">¥{{ row.outputValue.toLocaleString() }}</b></span></div>
                          <div class="expand-row"><span>ROI: <b style="color: #67c23a">{{ row.roi }}%</b></span><span>人效: <b :class="getEfficiencyClass(row.efficiency)">{{ row.efficiency }}</b></span></div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#409eff"><Document /></el-icon> 内容产出</div>
                          <div class="expand-row"><span>发布作品: <b>{{ row.publishCount }}篇</b></span><span>平均阅读: <b style="color: #409eff">{{ row.avgReading.toLocaleString() }}</b></span></div>
                          <div class="expand-row"><span>爆款率: <b>{{ row.hotRate }}%</b></span><span>互动率: <b>{{ row.engagement }}%</b></span></div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#f56c6c"><TrendCharts /></el-icon> 时间趋势</div>
                          <div class="expand-desc">近30天人效趋势</div>
                          <div class="mini-chart" :ref="el => setWechatChartRef(el, row.rank)"></div>
                        </div>
                      </el-card>
                    </el-col>
                  </el-row>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="rank" label="排名" width="80" align="center">
              <template #default="{ row }">
                <span :class="['rank-badge', getRankClass(row.rank)]">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="period" label="统计周期" width="100" />
            <el-table-column prop="ipName" label="IP（主播）" width="120" />
            <el-table-column prop="accountName" label="公众号名称" width="120" />
            <el-table-column prop="handler" label="经办人" width="80" />
            <el-table-column prop="totalFollowers" label="粉丝总数" width="100" align="right">
              <template #default="{ row }">{{ row.totalFollowers.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="newFollowers" label="新增关注" width="100" align="right">
              <template #default="{ row }">{{ row.newFollowers.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="unfollowCount" label="取关数" width="80" align="right" />
            <el-table-column prop="netGrowth" label="净增数" width="100" align="right">
              <template #default="{ row }">
                <span :class="row.netGrowth >= 0 ? 'text-success' : 'text-danger'">+{{ row.netGrowth.toLocaleString() }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="platformNetGrowth" label="平台净增" width="100" align="right" />
            <el-table-column prop="platformFollowers" label="平台粉丝" width="100" align="right" />
            <el-table-column prop="platformNewFollowers" label="平台新增" width="100" align="right" />
            <el-table-column prop="conversionRate" label="转化率" width="100" align="right">
              <template #default="{ row }">{{ row.conversionRate }}%</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 短视频运营数据 -->
        <el-tab-pane label="短视频运营数据统计" name="video">
          <el-table :data="videoList" v-loading="loading" border stripe row-key="rank">
            <el-table-column type="expand" width="50">
              <template #default="{ row }">
                <div class="expand-content">
                  <el-row :gutter="16">
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#409eff"><VideoCamera /></el-icon> 视频产出</div>
                          <div class="expand-row"><span>发布视频: <b>{{ row.videoCount }}个</b></span><span>平均播放: <b style="color: #409eff">{{ row.avgViews.toLocaleString() }}</b></span></div>
                          <div class="expand-row"><span>爆款率: <b>{{ row.hotRate }}%</b></span><span>完播率: <b>{{ row.completionRate }}%</b></span></div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#e6a23c"><Star /></el-icon> 互动指标</div>
                          <div class="expand-row"><span>总点赞: <b>{{ row.likes.toLocaleString() }}</b></span><span>总评论: <b>{{ row.comments.toLocaleString() }}</b></span></div>
                          <div class="expand-row"><span>总分享: <b>{{ row.shares.toLocaleString() }}</b></span><span>互动率: <b>{{ row.engagement }}%</b></span></div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#67c23a"><User /></el-icon> 粉丝增长</div>
                          <div class="expand-row"><span>新增粉丝: <b>{{ row.newFollowers.toLocaleString() }}</b></span><span>转化率: <b>{{ row.conversionRate }}%</b></span></div>
                          <div class="expand-row"><span>粉丝ROI: <b style="color: #67c23a">{{ row.fansRoi }}%</b></span></div>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="6">
                      <el-card shadow="hover" body-style="padding: 16px">
                        <div class="expand-card">
                          <div class="expand-title"><el-icon color="#f56c6c"><TrendCharts /></el-icon> 时间趋势</div>
                          <div class="expand-desc">近30天视频趋势</div>
                          <div class="mini-chart" :ref="el => setVideoChartRef(el, row.rank)"></div>
                        </div>
                      </el-card>
                    </el-col>
                  </el-row>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="rank" label="排名" width="80" align="center">
              <template #default="{ row }">
                <span :class="['rank-badge', getRankClass(row.rank)]">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="ipName" label="IP（主播）" min-width="120" />
            <el-table-column prop="platform" label="平台" width="100">
              <template #default="{ row }"><el-tag>{{ row.platform }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="handler" label="经办人" width="80" />
            <el-table-column prop="followers" label="粉丝数" width="120" align="right">
              <template #default="{ row }">{{ row.followers.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="newFollowers" label="新增粉丝" width="100" align="right">
              <template #default="{ row }"><span :class="row.newFollowers >= 0 ? 'text-success' : 'text-danger'">+{{ row.newFollowers.toLocaleString() }}</span></template>
            </el-table-column>
            <el-table-column prop="views" label="总播放" width="120" align="right">
              <template #default="{ row }">{{ row.views.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="likes" label="点赞数" width="100" align="right">
              <template #default="{ row }">{{ row.likes.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="engagement" label="互动率" width="80" align="right">
              <template #default="{ row }">{{ row.engagement }}%</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 个人管理账号的粉丝情况 -->
        <el-tab-pane label="个人管理账号的粉丝情况" name="fans">
          <el-table :data="fansList" v-loading="loading" border stripe>
            <el-table-column prop="handler" label="经办人" width="100" />
            <el-table-column prop="accountCount" label="管理账号数" width="120" align="right" />
            <el-table-column prop="totalFollowers" label="粉丝总数" width="120" align="right">
              <template #default="{ row }">{{ row.totalFollowers.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="avgFollowers" label="账号均粉丝" width="120" align="right">
              <template #default="{ row }">{{ row.avgFollowers.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="growthRate" label="增长率" width="100" align="right">
              <template #default="{ row }">
                <span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">+{{ row.growthRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="activeRate" label="活跃率" width="100" align="right">
              <template #default="{ row }">{{ row.activeRate }}%</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 个人管理的会员情况 -->
        <el-tab-pane label="个人管理的会员情况" name="vip">
          <el-table :data="vipList" v-loading="loading" border stripe>
            <el-table-column prop="rank" label="排名" width="80" align="center">
              <template #default="{ row }">
                <span :class="['rank-badge', getRankClass(row.rank)]">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="handler" label="经办人" width="100" />
            <el-table-column prop="vipCount" label="会员数" width="100" align="right">
              <template #default="{ row }">{{ row.vipCount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="newVipCount" label="新增会员" width="100" align="right">
              <template #default="{ row }"><span class="text-success">+{{ row.newVipCount.toLocaleString() }}</span></template>
            </el-table-column>
            <el-table-column prop="vipRetention" label="会员留存" width="100" align="right">
              <template #default="{ row }">{{ row.vipRetention }}%</template>
            </el-table-column>
            <el-table-column prop="vipRevenue" label="会员收入" width="120" align="right">
              <template #default="{ row }">¥{{ row.vipRevenue.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="avgConsumption" label="人均消费" width="100" align="right">
              <template #default="{ row }">¥{{ row.avgConsumption.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="vipRate" label="会员占比" width="100" align="right">
              <template #default="{ row }">{{ row.vipRate }}%</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import TableSearch from '@/components/TableSearch.vue'
import DictSelect from '@/components/DictSelect.vue'
import { exportToExcel } from '@/utils'
import { Download, Document, Coin, TrendCharts, VideoCamera, Star, User } from '@element-plus/icons-vue'

const activeTab = ref('wechat')
const loading = ref(false)
const totalCount = ref(25)
const searchForm = reactive({
  period: 'monthly',
  statDate: '2026-05',
  position: undefined as string | undefined,
  keyword: undefined as string | undefined,
})
const wechatChartRefs = reactive<Record<number, HTMLElement | null>>({})
const videoChartRefs = reactive<Record<number, HTMLElement | null>>({})
const wechatCharts: Record<number, echarts.ECharts> = {}
const videoCharts: Record<number, echarts.ECharts> = {}

const setWechatChartRef = (el: any, rank: number) => {
  if (el) wechatChartRefs[rank] = el as HTMLElement
}
const setVideoChartRef = (el: any, rank: number) => {
  if (el) videoChartRefs[rank] = el as HTMLElement
}

// 公众号运营数据
const wechatList = ref([
  { rank: 1, period: '2026-05', ipName: '体育解说A', accountName: 'XX体育', handler: '张三', totalFollowers: 15230, newFollowers: 520, unfollowCount: 80, netGrowth: 440, platformNetGrowth: 380, platformFollowers: 8500, platformNewFollowers: 380, conversionRate: 73.1, taskCompleted: 24, taskRate: 85.7, originalCount: 12, approvedCount: 11, rejectedCount: 1, inputCost: 3200, outputValue: 8500, roi: 265.6, efficiency: 'A+', publishCount: 18, avgReading: 5420, hotRate: 16.7, engagement: 4.2 },
  { rank: 2, period: '2026-05', ipName: '财经观察B', accountName: '财经洞察', handler: '李四', totalFollowers: 12800, newFollowers: 480, unfollowCount: 100, netGrowth: 380, platformNetGrowth: 320, platformFollowers: 7200, platformNewFollowers: 320, conversionRate: 66.7, taskCompleted: 22, taskRate: 78.6, originalCount: 10, approvedCount: 9, rejectedCount: 1, inputCost: 2800, outputValue: 7200, roi: 257.1, efficiency: 'A', publishCount: 15, avgReading: 4800, hotRate: 13.3, engagement: 3.8 },
  { rank: 3, period: '2026-05', ipName: '美食探店C', accountName: '吃货联盟', handler: '王五', totalFollowers: 9800, newFollowers: 380, unfollowCount: 60, netGrowth: 320, platformNetGrowth: 280, platformFollowers: 5600, platformNewFollowers: 280, conversionRate: 73.7, taskCompleted: 20, taskRate: 71.4, originalCount: 8, approvedCount: 7, rejectedCount: 1, inputCost: 2500, outputValue: 5600, roi: 224.0, efficiency: 'B+', publishCount: 12, avgReading: 4200, hotRate: 8.3, engagement: 3.2 },
])

// 短视频运营数据
const videoList = ref([
  { rank: 1, ipName: '科技小达人', platform: '抖音', handler: '赵六', followers: 5200000, newFollowers: 52000, views: 45600000, likes: 3200000, engagement: 7.0, videoCount: 28, avgViews: 180000, hotRate: 17.9, completionRate: 45.2, comments: 456000, shares: 234000, conversionRate: 1.1, fansRoi: 890.0 },
  { rank: 2, ipName: '美食探索家', platform: '抖音', handler: '钱七', followers: 3800000, newFollowers: 38000, views: 32000000, likes: 2200000, engagement: 6.9, videoCount: 22, avgViews: 150000, hotRate: 13.6, completionRate: 42.8, comments: 320000, shares: 180000, conversionRate: 1.2, fansRoi: 820.0 },
  { rank: 3, ipName: '旅行摄影师', platform: '视频号', handler: '孙八', followers: 1200000, newFollowers: 18000, views: 8500000, likes: 580000, engagement: 6.8, videoCount: 15, avgViews: 120000, hotRate: 6.7, completionRate: 38.5, comments: 85000, shares: 42000, conversionRate: 2.1, fansRoi: 680.0 },
])

// 粉丝情况
const fansList = ref([
  { handler: '张三', accountCount: 8, totalFollowers: 125000, avgFollowers: 15625, growthRate: 5.2, activeRate: 68.5 },
  { handler: '李四', accountCount: 6, totalFollowers: 98000, avgFollowers: 16333, growthRate: 4.8, activeRate: 72.3 },
  { handler: '王五', accountCount: 5, totalFollowers: 72000, avgFollowers: 14400, growthRate: 3.2, activeRate: 65.8 },
])

// 会员情况
const vipList = ref([
  { rank: 1, handler: '张三', vipCount: 1250, newVipCount: 85, vipRetention: 92.5, vipRevenue: 85000, avgConsumption: 68, vipRate: 1.0 },
  { rank: 2, handler: '李四', vipCount: 980, newVipCount: 62, vipRetention: 91.2, vipRevenue: 68000, avgConsumption: 69, vipRate: 1.0 },
  { rank: 3, handler: '王五', vipCount: 720, newVipCount: 48, vipRetention: 89.8, vipRevenue: 52000, avgConsumption: 72, vipRate: 1.0 },
])

const getRankClass = (rank: number) => rank <= 3 ? 'rank-top' : ''
const getEfficiencyClass = (eff: string) => eff.startsWith('A') ? 'text-success' : eff.startsWith('B') ? '' : 'text-warning'

const handleSearch = () => { console.log('搜索') }
const handleReset = () => { searchForm.position = undefined; searchForm.keyword = undefined }
const handleExport = () => {
  const columns = [
    { key: 'rank', label: '排名' },
    { key: 'period', label: '统计周期' },
    { key: 'accountName', label: '账号' },
    { key: 'position', label: '岗位' },
    { key: 'ipGroupName', label: 'IP组' },
    { key: 'score', label: '效率分' },
  ]
  const dataSource = activeTab.value === 'wechat' ? wechatList.value
    : activeTab.value === 'video' ? videoList.value
    : fansList.value
  const tabName = activeTab.value === 'wechat' ? '公众号' : activeTab.value === 'video' ? '短视频' : '粉丝'
  exportToExcel(dataSource, columns, `运营效率_${tabName}`)
}

// 渲染迷你趋势图（使用已保存的 ref）
const renderMiniChart = (el: HTMLElement, data: number[], chartStore: Record<number, echarts.ECharts>, rank: number) => {
  if (!el || el.clientWidth === 0) {
    setTimeout(() => renderMiniChart(el, data, chartStore, rank), 100)
    return
  }
  if (chartStore[rank]) chartStore[rank].dispose()
  const chart = echarts.init(el)
  chartStore[rank] = chart
  chart.setOption({
    grid: { left: 0, right: 0, top: 0, bottom: 0 },
    xAxis: { type: 'category', show: false, data: Array.from({ length: 10 }, (_, i) => i) },
    yAxis: { type: 'value', show: false },
    series: [{ type: 'bar', data: data.map(v => Math.max(v, 10)), itemStyle: { color: '#67c23a', borderRadius: [2, 2, 0, 0] }, barWidth: '60%' }],
  })
}

onMounted(async () => {
  await nextTick()
  // 渲染公众号迷你图
  wechatList.value.forEach((row) => {
    const el = wechatChartRefs[row.rank]
    const data = Array.from({ length: 10 }, () => Math.round(Math.random() * 60 + 30))
    if (el) renderMiniChart(el, data, wechatCharts, row.rank)
  })
  // 渲染视频迷你图
  videoList.value.forEach((row) => {
    const el = videoChartRefs[row.rank]
    const data = Array.from({ length: 10 }, () => Math.round(Math.random() * 60 + 30))
    if (el) renderMiniChart(el, data, videoCharts, row.rank)
  })
})
</script>

<style scoped lang="scss">
.efficiency-page { padding: 20px; }
.main-card { margin-top: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.main-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.rank-badge { display: inline-block; width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 50%; color: #fff; font-size: 12px; font-weight: 600; }
.rank-top { background: #f56c6c; }
.rank-badge:not(.rank-top) { background: #e6a23c; }
.text-success { color: #67c23a; font-weight: 600; }
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.expand-content { padding: 16px 24px; background: #f5f7fa; }
.expand-card .expand-title { display: flex; align-items: center; font-size: 13px; color: #909399; margin-bottom: 8px; gap: 4px; }
.expand-card .expand-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; }
.expand-card .expand-row b { color: #303133; }
.expand-card .expand-desc { font-size: 12px; color: #909399; margin-top: 4px; }
.mini-chart { height: 40px; margin-top: 8px; }
</style>
