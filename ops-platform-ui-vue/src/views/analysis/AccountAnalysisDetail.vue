<template>
  <div class="account-detail-page">
    <el-page-header @back="router.back()" :title="'返回'" :content="`账号 #${accountId}`" />

    <el-tabs v-model="activeTab" class="detail-tabs" style="margin-top: 16px">
      <!-- 粉丝详情 -->
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
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAccountFollowerDetail, getAccountContentDetail } from '@/api/account-analysis'
import ContentWrap from '@/components/ContentWrap.vue'
import Pagination from '@/components/Pagination.vue'
import DictLabel from '@/components/DictLabel.vue'
import { formatDateTime } from '@/utils'

const route = useRoute()
const router = useRouter()

const accountId = Number(route.params.id)
const activeTab = ref<'followers' | 'contents'>(((route.query.tab as string) || 'followers') as any)

const followerLoading = ref(false)
const followerList = ref<any[]>([])

const contentLoading = ref(false)
const contentList = ref<any[]>([])
const pagination = reactive({ pageNo: 1, pageSize: 10, total: 0 })

const formatNumber = (n: any) => (n || 0).toLocaleString('zh-CN')

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

watch(activeTab, (t) => {
  if (t === 'followers') loadFollowers()
  else loadContents()
})

onMounted(() => {
  if (activeTab.value === 'followers') loadFollowers()
  else loadContents()
})
</script>

<style scoped>
.account-detail-page { padding: 20px; }
.detail-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.viral-tag { color: #f56c6c; font-weight: 600; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
.text-primary { color: #409eff; }
</style>
