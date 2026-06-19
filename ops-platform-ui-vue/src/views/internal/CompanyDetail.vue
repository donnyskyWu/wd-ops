<!--
  M4 - 公司详情
  依据: UX-M4 § 3.1 P-M4-002 / FR-M4-001
  路径: /account/company/:id
  4 区: 基本信息 / 公众号容量 / 扩容记录 / 关联账号
-->
<template>
  <div class="company-detail-page" v-loading="loading">

    <template v-if="detail">
      <!-- 顶部操作 -->
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 style="margin: 0">
              {{ detail.companyName }}
              <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="default" style="margin-left: 8px">
                {{ detail.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </h2>
            <p class="meta">
              <span>信用代码：{{ detail.creditCode }}</span>
              <el-divider direction="vertical" />
              <span>法人：{{ detail.legalPerson || '-' }}</span>
              <el-divider direction="vertical" />
              <span>创建于：{{ detail.createdAt }}</span>
            </p>
          </div>
          <div>
            <el-button @click="router.back()">返回</el-button>
            <el-button type="primary" @click="handleEdit">编辑</el-button>
            <el-button @click="handleExpand">扩容</el-button>
          </div>
        </div>
      </el-card>

      <el-tabs v-model="activeTab" style="margin-top: 16px">
        <!-- Tab 1: 基本信息 -->
        <el-tab-pane label="基本信息" name="basic" :lazy="false">
          <ContentWrap>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="公司名">{{ detail.companyName }}</el-descriptions-item>
              <el-descriptions-item label="信用代码">{{ detail.creditCode }}</el-descriptions-item>
              <el-descriptions-item label="行业">
                <el-tag>{{ detail.industry || '-' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="法人">{{ detail.legalPerson || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ detail.updatedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="营业执照" :span="2">
                <div v-if="licensePreviewUrls.length" class="license-grid">
                  <el-image
                    v-for="(url, index) in licensePreviewUrls"
                    :key="url"
                    :src="url"
                    fit="cover"
                    class="license-preview"
                    :preview-src-list="licensePreviewUrls"
                    :initial-index="index"
                  />
                </div>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </ContentWrap>
        </el-tab-pane>

        <!-- Tab 2: 公众号容量 -->
        <el-tab-pane label="公众号容量" name="quota" :lazy="false">
          <ContentWrap>
            <el-row :gutter="16">
              <el-col :span="6">
                <el-card shadow="hover">
                  <div class="quota-card">
                    <div class="quota-label">总容量</div>
                    <div class="quota-value primary">{{ detail.totalQuota }}</div>
                    <div class="quota-unit">个公众号</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="hover">
                  <div class="quota-card">
                    <div class="quota-label">已注册</div>
                    <div class="quota-value success">{{ detail.usedQuota }}</div>
                    <div class="quota-unit">个公众号</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="hover">
                  <div class="quota-card">
                    <div class="quota-label">剩余</div>
                    <div class="quota-value" :class="{ warning: (detail.totalQuota - detail.usedQuota) < 5 }">
                      {{ detail.totalQuota - detail.usedQuota }}
                    </div>
                    <div class="quota-unit">个公众号</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="hover">
                  <div class="quota-card">
                    <div class="quota-label">使用率</div>
                    <el-progress
                      type="circle"
                      :percentage="Math.round((detail.usedQuota / detail.totalQuota) * 100)"
                      :status="(detail.usedQuota / detail.totalQuota) > 0.8 ? 'exception' : 'success'"
                    />
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-alert
              v-if="(detail.totalQuota - detail.usedQuota) < 5"
              style="margin-top: 16px"
              type="warning"
              :closable="false"
              show-icon
              title="容量预警"
              :description="'剩余容量不足 5 个,建议尽快扩容(点击右上角「扩容」按钮)'"
            />
          </ContentWrap>
        </el-tab-pane>

        <!-- Tab 3: 扩容记录 -->
        <el-tab-pane label="扩容记录" name="expansion" :lazy="false">
          <ContentWrap>
            <el-timeline>
              <el-timeline-item
                v-for="(r, i) in expansionHistory"
                :key="i"
                :timestamp="r.time"
                :type="r.type"
                :hollow="i !== 0"
                placement="top"
              >
                <el-card shadow="never">
                  <h4 style="margin: 0 0 8px 0">
                    {{ r.action }}
                    <el-tag size="small" :type="r.type">{{ r.delta }}</el-tag>
                  </h4>
                  <p style="margin: 0; color: #909399; font-size: 13px">操作人：{{ r.operator }} ｜ 原因：{{ r.reason }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </ContentWrap>
        </el-tab-pane>

        <!-- Tab 4: 关联账号 -->
        <el-tab-pane label="关联账号" name="accounts" :lazy="false">
          <ContentWrap>
            <div class="toolbar">
              <span class="title">共 {{ accounts.length }} 个账号</span>
            </div>
            <el-table :data="accounts" border stripe>
              <el-table-column prop="accountName" label="账号名" min-width="160" show-overflow-tooltip />
              <el-table-column prop="platformName" label="平台" width="100" align="center">
                <template #default="{ row }">
                  <el-tag>{{ row.platformName }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="ipGroupName" label="所属 IP 组" min-width="120" />
              <el-table-column prop="followerCount" label="粉丝数" width="120" align="right" />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '在用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
            </el-table>
          </ContentWrap>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 扩容弹窗 -->
    <el-dialog v-model="expandDialogVisible" title="申请扩容" width="500px">
      <el-form :model="expandForm" label-width="100px">
        <el-form-item label="当前容量">
          <el-input :model-value="detail?.totalQuota" disabled />
        </el-form-item>
        <el-form-item label="扩容数量" required>
          <el-input-number v-model="expandForm.delta" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="扩容原因" required>
          <el-input v-model="expandForm.reason" type="textarea" :rows="3" placeholder="如：双十一活动储备" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="expandDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="expandSubmitting" @click="submitExpand">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ContentWrap from '@/components/ContentWrap.vue'
import { appendFileAuth } from '@/utils/fileUrl'
import {
  getCompany,
  expandCompany,
} from '@/api/company'
import { getPlatformAccountList } from '@/api/account'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('basic')
const detail = ref<any>(null)
const expansionHistory = ref<any[]>([])
const accounts = ref<any[]>([])

const expandDialogVisible = ref(false)
const expandSubmitting = ref(false)
const expandForm = reactive({ delta: 5, reason: '' })

const OA_FILE_VIEW_PREFIX = '/admin-api/oa/file/view?key='

function viewUrlForKey(key?: string): string {
  if (!key) return ''
  if (key.startsWith('/admin-api/') || key.startsWith('http://') || key.startsWith('https://')) {
    return key
  }
  return OA_FILE_VIEW_PREFIX + key
}

const imagePreview = (url?: string, key?: string) => {
  const resolved = url || viewUrlForKey(key)
  return resolved ? appendFileAuth(resolved) : ''
}

const licensePreviewUrls = computed(() => {
  const d = detail.value
  if (!d) return [] as string[]
  const urls = (d.businessLicenseUrls || []) as string[]
  const keys = (d.businessLicenseKeys || []) as string[]
  const maxLen = Math.max(urls.length, keys.length)
  const result: string[] = []
  for (let i = 0; i < maxLen; i += 1) {
    const preview = imagePreview(urls[i], keys[i])
    if (preview) result.push(preview)
  }
  return result
})

const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const found = await getCompany(id)
    detail.value = {
      ...found,
      creditCode: found.creditCode || '',
      industry: found.industry || '',
      legalPerson: found.legalName || '',
      totalQuota: found.mpCapacityStandard ?? 0,
      usedQuota: found.mpRegisteredCount ?? 0,
      status: found.status === 'ENABLED' ? 1 : 0,
      createdAt: found.createTime || '',
      updatedAt: found.createTime || '',
      businessLicenseKeys: found.businessLicenseKeys || [],
      businessLicenseUrls: found.businessLicenseUrls || [],
    }
    // 关联账号通过 /account/list 过滤
    try {
      const accRes: any = await getPlatformAccountList({ companyId: id, pageNo: 1, pageSize: 50 })
      accounts.value = (accRes.list || []).map((a: any) => ({
        id: a.id,
        accountName: a.accountName,
        platformName: a.platformName || a.platform,
        ipGroupName: a.ipGroupName || '',
        followerCount: a.followerCount || 0,
        status: a.status === 'ENABLED' ? 1 : 0,
        createdAt: a.createTime || '',
      }))
    } catch {
      accounts.value = []
    }
    // 扩容历史后端暂未提供独立端点，留空
    expansionHistory.value = []
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  ElMessage.info('编辑公司: 实际应跳转到 /company 编辑模式或弹窗')
}
const handleExpand = () => {
  expandForm.delta = 5
  expandForm.reason = ''
  expandDialogVisible.value = true
}
const submitExpand = async () => {
  if (!detail.value) return
  if (!expandForm.reason.trim()) {
    ElMessage.warning('请填写扩容原因')
    return
  }
  expandSubmitting.value = true
  try {
    const newCapacity = (detail.value.totalQuota || 0) + expandForm.delta
    await expandCompany(detail.value.id, { newCapacity, reason: expandForm.reason })
    detail.value.totalQuota = newCapacity
    expansionHistory.value.unshift({
      time: new Date().toLocaleString('zh-CN'),
      action: '扩容',
      delta: `+${expandForm.delta}`,
      type: 'success',
      operator: '当前用户',
      reason: expandForm.reason,
    })
    ElMessage.success(`已扩容 +${expandForm.delta} 个,共 ${detail.value.totalQuota} 个`)
    expandDialogVisible.value = false
  } catch {
    // 错误已拦截
  } finally {
    expandSubmitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.company-detail-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-start; }
.meta { color: #909399; font-size: 13px; margin: 8px 0 0 0; }
.quota-card { text-align: center; padding: 12px 0; }
.quota-label { color: #909399; font-size: 13px; }
.quota-value { font-size: 32px; font-weight: 600; line-height: 1.4; }
.quota-value.primary { color: #409eff; }
.quota-value.success { color: #67c23a; }
.quota-value.warning { color: #e6a23c; }
.quota-unit { color: #909399; font-size: 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.toolbar .title { font-weight: 600; }
.license-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.license-preview { width: 160px; height: 100px; border-radius: 4px; border: 1px solid #dcdfe6; }
</style>
