<!--
  M10 - 采集日志
  依据: UX-M10 § 4 P-M10-003
-->
<template>
  <div class="collect-log-page">

    <TableSearch v-model="searchForm" @search="handleSearch" @reset="handleReset">
      <el-form-item label="任务">
        <el-select v-model="searchForm.taskId" placeholder="全部任务" clearable filterable>
          <el-option
            v-for="t in taskOptions"
            :key="t.id"
            :label="t.name"
            :value="t.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="searchForm.status" dict-type="dict_collect_status" placeholder="全部" clearable />
      </el-form-item>
      <el-form-item label="执行时间">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
    </TableSearch>

    <ContentWrap title="采集日志">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-class-name="collect-log-row"
        @row-click="handleViewDetail"
      >
        <template #empty>
          <el-empty description="暂无日志" />
        </template>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="taskName" label="任务名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startAt" label="开始时间" width="170" align="center" />
        <el-table-column prop="durationMs" label="耗时" width="100" align="right">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="采集记录" width="100" align="right" />
        <el-table-column prop="retryCount" label="重试次数" width="90" align="center" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="160">
          <template #default="{ row }">
            <el-button v-if="row.errorMessage" link type="danger" @click.stop="showError(row.errorMessage)">
              查看
            </el-button>
            <span v-else style="color: #67c23a">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleViewDetail(row)">查看详情</el-button>
            <el-button link type="primary" @click.stop="handleViewTask(row)">查看任务</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        @update:current-page="(val) => (pagination.pageNo = val)"
        @update:page-size="(val) => { pagination.pageSize = val; loadData() }"
        @change="loadData"
      />
    </ContentWrap>

    <el-drawer v-model="errorVisible" title="错误详情" size="500px">
      <pre style="white-space: pre-wrap; word-break: break-all; color: #f56c6c; line-height: 1.6">{{ errorText }}</pre>
    </el-drawer>

    <el-drawer v-model="detailVisible" title="采集日志详情" size="640px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="任务名">{{ detail.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ platformLabel(detail.platformType) }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ detail.accountName || detail.accountId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detail.status)">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.startAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.endAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ formatDuration(detail.durationMs) }}</el-descriptions-item>
          <el-descriptions-item label="采集记录">{{ detail.recordCount ?? 0 }} 条</el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ detail.retryCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.errorMessage" label="错误信息">
            <span style="color: #f56c6c">{{ detail.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="detail?.result">
          <el-divider content-position="left">采集结果</el-divider>
          <el-alert
            v-if="detail.result.summary"
            :title="detail.result.summary"
            :type="isMultiTypeResult ? (detail.status === 'PARTIAL' ? 'warning' : 'success') : 'success'"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          />

          <template v-if="isMultiTypeResult">
            <el-collapse v-model="activeTypePanels" style="margin-bottom: 16px">
              <el-collapse-item
                v-for="(typeResult, idx) in detail.result.typeResults"
                :key="`${typeResult.dataType}-${idx}`"
                :name="idx"
              >
                <template #title>
                  <span style="font-weight: 500">{{ dataTypeLabel(typeResult.dataType) }}</span>
                  <el-tag
                    :type="typeResult.success === false ? 'danger' : 'success'"
                    size="small"
                    style="margin-left: 8px"
                  >
                    {{ typeResult.success === false ? '失败' : '成功' }}
                  </el-tag>
                  <span v-if="typeResult.recordCount != null" style="margin-left: 8px; color: #909399; font-size: 12px">
                    {{ typeResult.recordCount }} 条
                  </span>
                </template>
                <el-alert
                  v-if="typeResult.errorMessage"
                  :title="typeResult.errorMessage"
                  type="error"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 12px"
                />
                <el-alert
                  v-else-if="typeResult.summary"
                  :title="typeResult.summary"
                  type="success"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 12px"
                />
                <el-descriptions :column="1" border size="small" style="margin-bottom: 12px">
                  <el-descriptions-item v-if="typeResult.targetTable" label="落库表">
                    {{ typeResult.targetTable }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="typeResult.targetHint" label="说明">
                    {{ typeResult.targetHint }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="typeResult.metrics?.followerCount != null" label="粉丝数">
                    {{ typeResult.metrics.followerCount }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="typeResult.metrics?.totalFriends != null" label="外部联系人">
                    {{ typeResult.metrics.totalFriends }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="typeResult.metrics?.statDate" label="统计日期">
                    {{ typeResult.metrics.statDate }}
                  </el-descriptions-item>
                </el-descriptions>
                <div v-if="sampleColumnsFor(typeResult.dataType).length && typeResult.samples?.length">
                  <div style="font-weight: 500; margin-bottom: 8px">样本数据（最多 10 条）</div>
                  <el-table :data="typeResult.samples" border stripe size="small" max-height="240">
                    <el-table-column
                      v-for="col in sampleColumnsFor(typeResult.dataType)"
                      :key="col.prop"
                      :prop="col.prop"
                      :label="col.label"
                      :min-width="col.minWidth"
                      show-overflow-tooltip
                    />
                  </el-table>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>

          <template v-else>
            <el-descriptions :column="1" border size="small" style="margin-bottom: 16px">
              <el-descriptions-item v-if="detail.result.dataType" label="数据类型">
                {{ dataTypeLabel(detail.result.dataType) }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.result.targetTable" label="落库表">
                {{ detail.result.targetTable }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.result.targetHint" label="说明">
                {{ detail.result.targetHint }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.result.metrics?.followerCount != null" label="粉丝数">
                {{ detail.result.metrics.followerCount }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.result.metrics?.totalFriends != null" label="外部联系人">
                {{ detail.result.metrics.totalFriends }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.result.metrics?.todayFriendInteractions != null" label="今日互动">
                {{ detail.result.metrics.todayFriendInteractions }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.result.metrics?.todayMessagesSent != null" label="今日消息">
                {{ detail.result.metrics.todayMessagesSent }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.result.metrics?.statDate" label="统计日期">
                {{ detail.result.metrics.statDate }}
              </el-descriptions-item>
            </el-descriptions>

            <div v-if="sampleColumnsFor(detail.result.dataType).length && detail.result.samples?.length">
              <div style="font-weight: 500; margin-bottom: 8px">样本数据（最多 10 条）</div>
              <el-table :data="detail.result.samples" border stripe size="small" max-height="320">
                <el-table-column
                  v-for="col in sampleColumnsFor(detail.result.dataType)"
                  :key="col.prop"
                  :prop="col.prop"
                  :label="col.label"
                  :min-width="col.minWidth"
                  show-overflow-tooltip
                />
              </el-table>
            </div>
          </template>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TableSearch from '@/components/TableSearch.vue'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictSelect from '@/components/DictSelect.vue'
import {
  getCollectLogPage,
  getCollectLogDetail,
  getCollectTaskPage,
  type CollectLogDetailVO,
  type CollectLogTypeResultVO,
} from '@/api/collect'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const taskOptions = ref<any[]>([])
const errorVisible = ref(false)
const errorText = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<CollectLogDetailVO | null>(null)
const activeTypePanels = ref<number[]>([])

const searchForm = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : undefined as number | undefined,
  status: undefined as string | undefined,
  dateRange: [] as string[],
  pageNo: 1,
  pageSize: 20,
})
const pagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const PLATFORM_LABEL: Record<string, string> = {
  WECHAT_OFFICIAL: '微信公众号',
  WECHAT_VIDEO: '微信视频号',
  DOUYIN: '抖音',
  KUAISHOU: '快手',
  XIAOHONGSHU: '小红书',
  BILIBILI: 'B站',
  WEWORK: '企业微信',
  WECHAT_PERSONAL: '个人微信',
}

const DATA_TYPE_LABEL: Record<string, string> = {
  ALL: '全量采集',
  MP_FOLLOWER_LIST: '公众号粉丝列表',
  MP_FOLLOWER_STATS: '公众号粉丝统计',
  MP_ARTICLE_LIST: '公众号图文列表',
  MP_ARTICLE_STATS: '公众号图文明细',
  MP_ARTICLE_CONTENT: '公众号图文内容',
  DOUYIN_FOLLOWER_LIST: '抖音粉丝列表',
  DOUYIN_VIDEO_LIST: '抖音作品列表',
  DOUYIN_VIDEO_STATS: '抖音作品明细',
  WECHAT_VIDEO_LIST: '视频号作品列表',
  WECHAT_VIDEO_STATS: '视频号作品明细',
  KUAISHOU_VIDEO_LIST: '快手作品列表',
  KUAISHOU_VIDEO_STATS: '快手作品明细',
  XIAOHONGSHU_NOTE_LIST: '小红书笔记列表',
  XIAOHONGSHU_NOTE_STATS: '小红书笔记明细',
  FOLLOWER_STATS: '粉丝统计',
  WECOM_DAILY_STATS: '企微日统计',
}

const statusText = (s: string) => ({ SUCCESS: '成功', FAILED: '失败', PARTIAL: '部分成功' }[s] || s)
const getStatusType = (s: string) => ({ SUCCESS: 'success', FAILED: 'danger', PARTIAL: 'warning' }[s] || '')
const platformLabel = (v?: string) => (v ? PLATFORM_LABEL[v] || v : '-')
const dataTypeLabel = (v?: string) => (v ? DATA_TYPE_LABEL[v] || v : '-')

const formatDuration = (ms?: number) => {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m${Math.floor((ms % 60000) / 1000)}s`
}

const isMultiTypeResult = computed(() => (detail.value?.result?.typeResults?.length ?? 0) > 0)

type SampleColumn = { prop: string; label: string; minWidth: number }

const sampleColumnsFor = (dataType?: string): SampleColumn[] => {
  if (dataType === 'MP_FOLLOWER_LIST') {
    return [
      { prop: 'openid', label: 'OpenID', minWidth: 160 },
      { prop: 'nickname', label: '昵称', minWidth: 120 },
      { prop: 'subscribedAt', label: '关注时间', minWidth: 160 },
    ]
  }
  if (dataType === 'MP_ARTICLE_LIST') {
    return [
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'readCount', label: '阅读数', minWidth: 90 },
      { prop: 'publishedAt', label: '发布时间', minWidth: 160 },
    ]
  }
  if (dataType === 'DOUYIN_FOLLOWER_LIST') {
    return [
      { prop: 'followerId', label: '粉丝ID', minWidth: 160 },
      { prop: 'nickname', label: '昵称', minWidth: 120 },
      { prop: 'followedAt', label: '关注时间', minWidth: 160 },
    ]
  }
  if (dataType === 'DOUYIN_VIDEO_LIST') {
    return [
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'videoId', label: '作品ID', minWidth: 140 },
      { prop: 'publishedAt', label: '发布时间', minWidth: 160 },
    ]
  }
  if (dataType === 'DOUYIN_VIDEO_STATS') {
    return [
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'playCount', label: '播放', minWidth: 90 },
      { prop: 'likeCount', label: '点赞', minWidth: 90 },
      { prop: 'commentCount', label: '评论', minWidth: 90 },
    ]
  }
  if (dataType === 'WECHAT_VIDEO_LIST' || dataType === 'KUAISHOU_VIDEO_LIST') {
    return [
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'videoId', label: '作品ID', minWidth: 140 },
      { prop: 'publishedAt', label: '发布时间', minWidth: 160 },
    ]
  }
  if (dataType === 'WECHAT_VIDEO_STATS' || dataType === 'KUAISHOU_VIDEO_STATS') {
    return [
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'playCount', label: '播放', minWidth: 90 },
      { prop: 'likeCount', label: '点赞', minWidth: 90 },
    ]
  }
  if (dataType === 'XIAOHONGSHU_NOTE_LIST') {
    return [
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'noteId', label: '笔记ID', minWidth: 140 },
      { prop: 'publishedAt', label: '发布时间', minWidth: 160 },
    ]
  }
  if (dataType === 'XIAOHONGSHU_NOTE_STATS') {
    return [
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'likeCount', label: '点赞', minWidth: 90 },
      { prop: 'commentCount', label: '评论', minWidth: 90 },
    ]
  }
  return []
}

const loadTasks = async () => {
  try {
    const res = await getCollectTaskPage({ pageNo: 1, pageSize: 200 }) as any
    taskOptions.value = res.list || res.data?.list || []
  } catch {
    taskOptions.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      taskId: searchForm.taskId,
      status: searchForm.status,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    }
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getCollectLogPage(params) as any
    tableData.value = res.list || res.data?.list || []
    pagination.total = res.total ?? res.data?.total ?? tableData.value.length
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNo = 1; loadData() }
const handleReset = () => {
  Object.assign(searchForm, { taskId: undefined, status: undefined, dateRange: [] })
  pagination.pageNo = 1
  loadData()
}

const showError = (text: string) => {
  errorText.value = text
  errorVisible.value = true
}

const handleViewTask = (row: any) => router.push(`/collect/task/${row.taskId}`)

const handleViewDetail = async (row: any) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  activeTypePanels.value = []
  try {
    const res = await getCollectLogDetail(row.id) as any
    detail.value = res.data ?? res
    const typeResults = detail.value?.result?.typeResults ?? []
    activeTypePanels.value = typeResults.map((_: CollectLogTypeResultVO, idx: number) => idx)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadTasks()
  loadData()
})
</script>

<style scoped>
.collect-log-page { padding: 20px; }
:deep(.collect-log-row) { cursor: pointer; }
</style>
