<!--
  M4 - 平台账号详情 / 编辑
  依据: UX-M4 § P-M4-009 / FR-M4-005
  路径: /account/platform/:id
-->
<template>
  <div class="pa-detail-page" v-loading="loading">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/platform-account' }">平台账号</el-breadcrumb-item>
      <el-breadcrumb-item>{{ detail?.accountName || '...' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <template v-if="detail">
      <el-card shadow="never">
        <div class="header">
          <div>
            <h2 style="margin: 0">
              <el-tag size="default">{{ platformLabel(detail.platformType || detail.platformName) }}</el-tag>
              {{ detail.accountName }}
              <el-tag :type="detail.status === 'NORMAL' ? 'success' : 'info'" size="default" style="margin-left: 8px">
                {{ detail.status === 'NORMAL' ? '正常' : detail.status === 'DISABLED' ? '停用' : detail.status }}
              </el-tag>
            </h2>
            <p class="meta">
              <span>IP 组：{{ detail.ipGroupName }}</span>
              <el-divider direction="vertical" />
              <span>粉丝：{{ detail.followerCount.toLocaleString() }}</span>
              <el-divider direction="vertical" />
              <span>总作品：{{ detail.workCount }}</span>
            </p>
          </div>
          <div>
            <el-button @click="router.back()">返回</el-button>
            <el-button type="primary" @click="editMode = !editMode">
              {{ editMode ? '取消编辑' : '编辑' }}
            </el-button>
          </div>
        </div>
      </el-card>

      <ContentWrap :title="editMode ? '编辑账号' : '账号信息'" style="margin-top: 16px">
        <el-form :model="form" label-width="120px" :disabled="!editMode" style="max-width: 900px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="平台">
                <el-input :model-value="platformLabel(detail.platformType || detail.platformName)" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="账号名" required>
                <el-input v-model="form.accountName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="账号 ID">
                <el-input v-model="form.platformAccountId" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属 IP 组" required>
                <IpGroupTreeSelect v-model="form.ipGroupId" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="归属人">
                <RealNameSelect v-model="form.realNameId" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机">
                <PhoneSelect v-model="form.phoneId" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态">
                <DictSelect v-model="form.status" dict-type="dict_account_status" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="登录方式">
                <el-select v-model="form.loginMethod" style="width: 100%">
                  <el-option label="手机号" value="phone" />
                  <el-option label="微信扫码" value="wechat" />
                  <el-option label="账号密码" value="password" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="2" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item v-if="editMode">
            <el-button @click="editMode = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
          </el-form-item>
        </el-form>
      </ContentWrap>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="12">
          <ContentWrap title="30 天粉丝趋势">
            <div ref="followerChartRef" style="width: 100%; height: 280px" />
          </ContentWrap>
        </el-col>
        <el-col :span="12">
          <ContentWrap title="内容产出(近 30 天)">
            <div ref="contentChartRef" style="width: 100%; height: 280px" />
          </ContentWrap>
        </el-col>
      </el-row>
    </template>
    <el-empty v-else-if="loadError" :description="loadError">
      <el-button type="primary" @click="loadDetail">重试</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import ContentWrap from '@/components/ContentWrap.vue'
import DictSelect from '@/components/DictSelect.vue'
import IpGroupTreeSelect from '@/components/selectors/IpGroupTreeSelect.vue'
import RealNameSelect from '@/components/selectors/RealNameSelect.vue'
import PhoneSelect from '@/components/selectors/PhoneSelect.vue'
import {
  getPlatformAccountDetail,
  updatePlatformAccount as updatePlatformAccountV2,
  getFollowerTrend,
  getContentTrend,
} from '@/api/account'
import { PLATFORM_LABEL, type PlatformType } from '@/utils/enum-alias'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const editMode = ref(false)
const submitting = ref(false)
const detail = ref<any>(null)
const form = reactive<any>({})
const followerChartRef = ref<HTMLDivElement | null>(null)
const contentChartRef = ref<HTMLDivElement | null>(null)
let followerChart: echarts.ECharts | null = null
let contentChart: echarts.ECharts | null = null

const initFollowerChart = (dates: string[] = [], vals: number[] = []) => {
  if (!followerChartRef.value) return
  const el = followerChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(() => initFollowerChart(dates, vals), 100)
    return
  }
  if (!followerChart) followerChart = echarts.init(el)
  const xs = dates.length > 0 ? dates : Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const data = vals.length > 0 ? vals : xs.map(() => 0)
  followerChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 16, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: xs, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data, itemStyle: { color: '#409eff' } }],
  })
}
const initContentChart = (dates: string[] = [], vals: number[] = []) => {
  if (!contentChartRef.value) return
  const el = contentChartRef.value
  if (el.getBoundingClientRect().width === 0) {
    setTimeout(() => initContentChart(dates, vals), 100)
    return
  }
  if (!contentChart) contentChart = echarts.init(el)
  const xs = dates.length > 0 ? dates : Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const data = vals.length > 0 ? vals : xs.map(() => 0)
  contentChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 16, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: xs, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data, itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] } }],
  })
}

const platformLabel = (platform: string) =>
  PLATFORM_LABEL[platform as PlatformType] || platform || '-'

const loadDetail = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const id = Number(route.params.id)
    const a: any = await getPlatformAccountDetail(id)
    detail.value = {
      ...a,
      platformName: a.platformType || a.platformName,
      platformAccountId: a.externalAccountId,
      ipGroupName: a.ipGroupName || (a.ipGroupId ? `IP组#${a.ipGroupId}` : '-'),
      followerCount: a.followerCount ?? 0,
      workCount: a.workCount ?? 0,
    }
    Object.assign(form, {
      accountName: a.accountName,
      platformAccountId: a.externalAccountId,
      ipGroupId: a.ipGroupId,
      realNameId: a.realnameId,
      phoneId: a.phoneId,
      status: a.status,
    })
    // 趋势图：尝试调 getFollowerTrend/getContentTrend
    try {
      const fts: any = await getFollowerTrend(id)
      const dates = (fts || []).map((p: any) => p.date || p.day || '')
      const vals = (fts || []).map((p: any) => p.followerCount || p.value || 0)
      initFollowerChart(dates, vals)
    } catch {
      initFollowerChart()
    }
    try {
      const cts: any = await getContentTrend(id)
      const dates = (cts || []).map((p: any) => p.date || p.day || '')
      const vals = (cts || []).map((p: any) => p.workCount || p.value || 0)
      initContentChart(dates, vals)
    } catch {
      initContentChart()
    }
  } catch (e: any) {
    detail.value = null
    loadError.value = e?.message?.includes('403') || e?.message?.includes('无权限')
      ? '无权限查看该账号，请联系管理员'
      : '账号详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
const submit = async () => {
  if (!detail.value?.id) return
  submitting.value = true
  try {
    await updatePlatformAccountV2({
      id: detail.value.id,
      accountName: form.accountName,
      ipGroupId: form.ipGroupId,
      realnameId: form.realNameId,
      phoneId: form.phoneId,
      status: form.status,
    })
    await loadDetail()
    editMode.value = false
    ElMessage.success('保存成功')
  } catch {
    // 错误已拦截
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.pa-detail-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-start; }
.meta { color: #909399; font-size: 13px; margin: 8px 0 0 0; }
</style>
