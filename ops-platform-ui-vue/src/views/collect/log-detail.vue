<!-- 采集日志详情面板（列表页抽屉内嵌） -->
<template>
  <div class="collect-log-detail-panel" v-loading="loading">
    <template v-if="detail">
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 class="title">
              {{ detail.taskName || '—' }}
              <el-tag :type="getStatusType(detail.status)" style="margin-left: 8px">
                {{ statusText(detail.status) }}
              </el-tag>
            </h2>
            <p class="meta">
              <span>平台：{{ platformLabel(detail.platformType) }}</span>
              <el-divider direction="vertical" />
              <span>账号：{{ detail.accountName || detail.accountId || '—' }}</span>
            </p>
            <p class="meta">
              <span>{{ detail.startAt || '—' }} ~ {{ detail.endAt || '—' }}</span>
              <el-divider direction="vertical" />
              <span>耗时：{{ formatDuration(detail.durationMs) }}</span>
              <el-divider direction="vertical" />
              <span>采集 {{ detail.recordCount ?? 0 }} 条</span>
              <el-divider direction="vertical" />
              <span>重试 {{ detail.retryCount ?? 0 }} 次</span>
            </p>
          </div>
        </div>
      </el-card>

      <el-alert
        v-if="detail.errorMessage"
        :title="detail.errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="error-alert"
      />

      <ContentWrap title="执行信息" style="margin-top: 16px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务名" :span="2">{{ detail.taskName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ platformLabel(detail.platformType) }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ detail.accountName || detail.accountId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detail.status)">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">{{ formatDuration(detail.durationMs) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.startAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.endAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="采集记录">{{ detail.recordCount ?? 0 }} 条</el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ detail.retryCount ?? 0 }}</el-descriptions-item>
        </el-descriptions>
      </ContentWrap>

      <ContentWrap v-if="detail.result" title="采集结果">
        <el-alert
          v-if="detail.result.summary"
          :title="detail.result.summary"
          :type="isMultiTypeResult ? (detail.status === 'PARTIAL' ? 'warning' : 'success') : 'success'"
          :closable="false"
          show-icon
          class="result-summary"
        />

        <template v-if="isMultiTypeResult">
          <el-collapse v-model="activeTypePanels" class="type-result-collapse">
            <el-collapse-item
              v-for="(typeResult, idx) in detail.result.typeResults"
              :key="`${typeResult.dataType}-${idx}`"
              :name="idx"
            >
              <template #title>
                <span class="type-title">{{ dataTypeLabel(typeResult.dataType) }}</span>
                <el-tag
                  :type="typeResult.success === false ? 'danger' : 'success'"
                  size="small"
                  class="type-tag"
                >
                  {{ typeResult.success === false ? '失败' : '成功' }}
                </el-tag>
                <span v-if="typeResult.recordCount != null" class="type-count">
                  {{ typeResult.recordCount }} 条
                </span>
              </template>

              <el-alert
                v-if="typeResult.errorMessage"
                :title="typeResult.errorMessage"
                type="error"
                :closable="false"
                show-icon
                class="type-alert"
              />
              <el-alert
                v-else-if="typeResult.summary"
                :title="typeResult.summary"
                type="success"
                :closable="false"
                show-icon
                class="type-alert"
              />

              <el-descriptions :column="2" border size="small" class="type-meta">
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

              <div v-if="sampleColumnsFor(typeResult.dataType).length && typeResult.samples?.length" class="sample-section">
                <div class="sample-title">样本数据（最多 10 条）</div>
                <el-table :data="typeResult.samples" border stripe size="small" max-height="280">
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
          <el-descriptions :column="2" border size="small" class="type-meta">
            <el-descriptions-item v-if="detail.result.dataType" label="数据类型">
              {{ dataTypeLabel(detail.result.dataType) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.result.targetTable" label="落库表">
              {{ detail.result.targetTable }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.result.targetHint" label="说明" :span="2">
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

          <div v-if="sampleColumnsFor(detail.result.dataType).length && detail.result.samples?.length" class="sample-section">
            <div class="sample-title">样本数据（最多 10 条）</div>
            <el-table :data="detail.result.samples" border stripe size="small" max-height="360">
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
      </ContentWrap>
    </template>
    <el-empty v-else-if="!loading" description="无法加载日志详情" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ContentWrap from '@/components/ContentWrap.vue'
import {
  getCollectLogDetail,
  type CollectLogDetailVO,
  type CollectLogTypeResultVO,
} from '@/api/collect'

const props = defineProps<{
  logId: number
}>()

const loading = ref(false)
const detail = ref<CollectLogDetailVO | null>(null)
const activeTypePanels = ref<number[]>([])

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
const getStatusType = (s: string) => ({ SUCCESS: 'success', FAILED: 'danger', PARTIAL: 'warning' }[s] || 'info')
const platformLabel = (v?: string) => (v ? PLATFORM_LABEL[v] || v : '—')
const dataTypeLabel = (v?: string) => (v ? DATA_TYPE_LABEL[v] || v : '—')

const formatDuration = (ms?: number) => {
  if (ms == null) return '—'
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

const loadDetail = async (logId: number) => {
  loading.value = true
  detail.value = null
  activeTypePanels.value = []
  try {
    const res = await getCollectLogDetail(logId) as any
    detail.value = res.data ?? res
    const typeResults = detail.value?.result?.typeResults ?? []
    activeTypePanels.value = typeResults.map((_: CollectLogTypeResultVO, idx: number) => idx)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => props.logId,
  (logId) => {
    if (logId) loadDetail(logId)
  },
  { immediate: true },
)
</script>

<style scoped lang="scss">
.collect-log-detail-panel {
  min-height: 120px;
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

.error-alert {
  margin-top: 16px;
}

.result-summary {
  margin-bottom: 16px;
}

.type-result-collapse {
  margin-top: 8px;
  border: none;

  :deep(.el-collapse-item__header) {
    font-size: 14px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-collapse-item__content) {
    padding: 12px 0 16px;
  }
}

.type-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.type-tag {
  margin-left: 8px;
}

.type-count {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.type-alert {
  margin-bottom: 12px;
}

.type-meta {
  margin-bottom: 12px;
}

.sample-section {
  margin-top: 4px;
}

.sample-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
  font-size: 13px;
}
</style>
