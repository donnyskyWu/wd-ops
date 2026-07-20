<template>
  <div class="account-detail-page">
    <el-page-header @back="router.back()" :title="'返回'" :content="`账号 #${accountId}`" />

    <el-tabs v-model="activeTab" class="detail-tabs" style="margin-top: 16px">
      <!-- 粉丝详情（聚合统计趋势） -->
      <el-tab-pane label="粉丝详情" name="followers">
        <ContentWrap>
          <el-table v-loading="followerLoading" :data="followerList" border stripe style="width: 100%">
            <el-table-column prop="statDate" label="日期" width="160">
              <template #default="{ row }">{{ formatDateTime(row.statDate) }}</template>
            </el-table-column>
            <el-table-column prop="accountName" label="账号" min-width="150" />
            <el-table-column prop="ipGroupName" label="所属IP组" width="140" />
            <el-table-column prop="followerCount" label="粉丝总数" width="120" align="right">
              <template #default="{ row }">{{ formatNumber(row.followerCount) }}</template>
            </el-table-column>
            <el-table-column prop="newFollower" label="新增" width="90" align="right">
              <template #default="{ row }">
                <span class="text-success">+{{ row.newFollower || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="unfollowCount" label="取消" width="90" align="right">
              <template #default="{ row }">
                <span class="text-danger">-{{ row.unfollowCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="netGrowth" label="净增" width="90" align="right">
              <template #default="{ row }">
                <span class="text-primary">+{{ row.netGrowth || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="growthRate" label="增长率" width="100" align="right">
              <template #default="{ row }">{{ ((row.growthRate || 0) * 100).toFixed(2) }}%</template>
            </el-table-column>
          </el-table>
        </ContentWrap>
      </el-tab-pane>

      <!-- 公众号粉丝列表（Football mp_user，仅 WECHAT_OFFICIAL） -->
      <el-tab-pane v-if="showWechatOfficial" label="粉丝列表" name="mp-followers" lazy>
        <ContentWrap title="粉丝列表">
          <el-table
            :data="mpFollowers"
            v-loading="mpFollowerLoading"
            border
            stripe
            empty-text="暂无粉丝数据，粉丝明细由 Football 公众号粉丝库（mp_user）维护"
          >
            <el-table-column label="头像" width="72" align="center">
              <template #default="{ row }">
                <FollowerAvatar :src="row.avatar" :nickname="row.nickname" :size="36" />
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="昵称" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.nickname || '-' }}</template>
            </el-table-column>
            <el-table-column prop="openid" label="OpenID" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span :title="row.openid">{{ truncateOpenid(row.openid) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="subscribedAt" label="关注时间" width="170" />
            <el-table-column prop="syncedAt" label="同步时间" width="170" />
          </el-table>
          <el-pagination
            v-if="mpFollowerPagination.total > 0"
            style="margin-top: 16px; justify-content: flex-end"
            :current-page="mpFollowerPagination.pageNo"
            :page-size="mpFollowerPagination.pageSize"
            :total="mpFollowerPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @update:current-page="(val) => { mpFollowerPagination.pageNo = val; loadMpFollowers() }"
            @update:page-size="(val) => { mpFollowerPagination.pageSize = val; mpFollowerPagination.pageNo = 1; loadMpFollowers() }"
          />
        </ContentWrap>
      </el-tab-pane>

      <!-- 作品详情 -->
      <el-tab-pane label="作品详情" name="contents">
        <ContentWrap>
          <el-table v-loading="contentLoading" :data="contentList" border stripe style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="contentType" label="类型" width="100" align="center">
              <template #default="{ row }">
                <DictLabel dict-type="dict_content_type" :value="row.contentType" />
              </template>
            </el-table-column>
            <el-table-column prop="publishTime" label="发布时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
            </el-table-column>
            <el-table-column prop="readCount" label="阅读量" width="110" align="right">
              <template #default="{ row }">{{ formatNumber(row.readCount) }}</template>
            </el-table-column>
            <el-table-column prop="likeCount" label="点赞" width="90" align="right" />
            <el-table-column prop="commentCount" label="评论" width="90" align="right" />
            <el-table-column prop="forwardCount" label="转发" width="90" align="right" />
            <el-table-column prop="isHit" label="爆款" width="80" align="center">
              <template #default="{ row }">
                <span v-if="row.isHit" class="viral-tag">🔥</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            :current-page="pagination.pageNo"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @update:current-page="(v) => (pagination.pageNo = v)"
            @update:page-size="(v) => { pagination.pageSize = v; loadContents() }"
            @change="loadContents"
          />
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
// P-GATE-UNMOCK-R S-R2-B：账号分析详情页（粉丝/作品 tab）
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAccountFollowerDetail, getAccountContentDetail } from '@/api/account-analysis'
import { getWechatMpFollowers, type MpFollowerVO } from '@/api/account'
import ContentWrap from '@/components/ContentWrap.vue'
import FollowerAvatar from '@/components/FollowerAvatar.vue'
import Pagination from '@/components/Pagination.vue'
import DictLabel from '@/components/DictLabel.vue'
import { formatDateTime } from '@/utils'

type DetailTab = 'followers' | 'contents' | 'mp-followers'
const VALID_TABS: DetailTab[] = ['followers', 'contents', 'mp-followers']

const route = useRoute()
const router = useRouter()

const accountId = Number(route.params.id)
const platformType = computed(() => (route.query.platform as string) || '')
const showWechatOfficial = computed(() => platformType.value === 'WECHAT_OFFICIAL')

const resolveInitialTab = (): DetailTab => {
  const requested = (route.query.tab as string) || 'followers'
  if (requested === 'mp-followers' && !showWechatOfficial.value) return 'followers'
  return VALID_TABS.includes(requested as DetailTab) ? (requested as DetailTab) : 'followers'
}

const activeTab = ref<DetailTab>(resolveInitialTab())

const followerLoading = ref(false)
const followerList = ref<any[]>([])

const contentLoading = ref(false)
const contentList = ref<any[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })

const mpFollowers = ref<MpFollowerVO[]>([])
const mpFollowerLoading = ref(false)
const mpFollowerPagination = reactive({ pageNo: 1, pageSize: 20, total: 0 })

const formatNumber = (n: any) => (n || 0).toLocaleString('zh-CN')

const truncateOpenid = (openid?: string) => {
  if (!openid) return '-'
  if (openid.length <= 16) return openid
  return `${openid.slice(0, 8)}…${openid.slice(-6)}`
}

const loadFollowers = async () => {
  followerLoading.value = true
  try {
    const list = await getAccountFollowerDetail({ accountId, startDate: undefined, endDate: undefined } as any)
    followerList.value = Array.isArray(list) ? list : []
  } catch (e) {
    ElMessage.error('粉丝详情加载失败：' + (e instanceof Error ? e.message : String(e)))
    followerList.value = []
  } finally {
    followerLoading.value = false
  }
}

const loadMpFollowers = async () => {
  if (!showWechatOfficial.value) {
    mpFollowers.value = []
    mpFollowerPagination.total = 0
    return
  }
  mpFollowerLoading.value = true
  try {
    const res = await getWechatMpFollowers(accountId, {
      pageNo: mpFollowerPagination.pageNo,
      pageSize: mpFollowerPagination.pageSize,
    })
    mpFollowers.value = res.list || []
    mpFollowerPagination.total = res.total ?? 0
  } catch (e: any) {
    mpFollowers.value = []
    mpFollowerPagination.total = 0
    const msg = e?.message || ''
    if (msg.includes('403') || msg.includes('无权限')) {
      ElMessage.warning('无权限查看粉丝列表，请联系管理员')
    } else if (msg) {
      ElMessage.error(msg)
    }
  } finally {
    mpFollowerLoading.value = false
  }
}

const loadContents = async () => {
  contentLoading.value = true
  try {
    const res: any = await getAccountContentDetail({ accountId, page: pagination.pageNo, size: pagination.pageSize })
    contentList.value = res?.list || []
    pagination.total = res?.total ?? 0
  } catch (e) {
    ElMessage.error('作品详情加载失败：' + (e instanceof Error ? e.message : String(e)))
    contentList.value = []
    pagination.total = 0
  } finally {
    contentLoading.value = false
  }
}

const loadActiveTab = (t: DetailTab) => {
  if (t === 'followers') loadFollowers()
  else if (t === 'mp-followers') loadMpFollowers()
  else loadContents()
}

watch(activeTab, loadActiveTab)

onMounted(() => loadActiveTab(activeTab.value))
</script>

<style scoped>
.account-detail-page { padding: 20px; }
.detail-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.viral-tag { color: #f56c6c; font-weight: 600; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
.text-primary { color: #409eff; }
</style>
