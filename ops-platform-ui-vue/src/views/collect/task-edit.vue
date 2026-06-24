<!--
  M10 - 采集任务编辑
  依据: UX-M10 § 3 P-M10-002 / PRD-M10 FR-M10-001
  路径: /collect/task/:id  (id=0 表示新增)
-->
<template>
  <div class="task-edit-page">

    <el-card shadow="never" v-loading="loading">
      <template #header>
        <span>基本信息</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 800px">
        <el-form-item label="任务名" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="平台" prop="platformType">
          <DictSelect v-model="form.platformType" dict-type="dict_platform_type" placeholder="请选择平台" />
        </el-form-item>
        <el-form-item label="企微应用" prop="accountId" v-if="isWeworkCollect">
          <WeworkAccountSelect v-model="form.accountId" placeholder="请选择企微应用" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px">
            一任务对应一个企业微信应用，同步应用下全部员工日统计
          </div>
        </el-form-item>
        <el-form-item label="账号" prop="accountId" v-else>
          <AccountSelect v-model="form.accountId" :platform-type="form.platformType" placeholder="请选择账号" />
        </el-form-item>
        <el-form-item label="采集范围" v-if="collectScopeLabels.length">
          <div class="collect-scope">
            <el-tag v-for="label in collectScopeLabels" :key="label" size="small" type="info" style="margin: 2px 4px 2px 0">
              {{ label }}
            </el-tag>
          </div>
          <div style="color: #909399; font-size: 12px; margin-top: 4px">
            保存后每次执行将按顺序采集以上全部数据类型，无需分别建任务
          </div>
        </el-form-item>
        <el-form-item label="频率" prop="frequency">
          <DictSelect v-model="form.frequency" dict-type="dict_collect_frequency" placeholder="请选择频率" />
        </el-form-item>
        <el-form-item label="Cron 表达式" prop="cron">
          <el-input v-model="form.cron" placeholder="如: 0 0 0/1 * * ? (每小时)" />
          <div style="color: #909399; font-size: 12px; margin-top: 4px">
            <el-link type="primary" @click="showCronDoc = true">查看 Cron 语法</el-link>
          </div>
        </el-form-item>
        <el-form-item label="API 配置" prop="apiConfig">
          <el-input
            v-model="form.apiConfig"
            type="textarea"
            :rows="6"
            placeholder='JSON 格式,如: {"endpoint":"https://api.xxx.com/...","headers":{...}}'
          />
          <div style="color: #e6a23c; font-size: 12px; margin-top: 4px">
            ⚠️ 敏感凭证(API Key)将在保存时通过 AES-256 加密
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <DictSelect v-model="form.status" dict-type="dict_collect_status" placeholder="请选择状态" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 监控信息(只读) -->
    <el-card shadow="never" style="margin-top: 16px" v-if="isEdit">
      <template #header>
        <span>监控信息</span>
        <el-tag size="small" type="info" style="margin-left: 8px">只读</el-tag>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="最近执行">{{ monitor.lastRunAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下次执行">{{ monitor.nextRunAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="累计成功">{{ monitor.runCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="累计失败">{{ monitor.failCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ monitor.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后修改">{{ monitor.updatedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="showCronDoc" title="Cron 表达式语法" width="600px">
      <el-table :data="cronDocs" border>
        <el-table-column prop="field" label="字段" width="100" />
        <el-table-column prop="allow" label="允许值" width="160" />
        <el-table-column prop="wildcard" label="通配符" />
      </el-table>
      <p style="margin-top: 12px; color: #909399; font-size: 13px">
        示例: <code>0 0 0/1 * * ?</code> = 每小时整点
      </p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import DictSelect from '@/components/DictSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import WeworkAccountSelect from '@/components/selectors/WeworkAccountSelect.vue'
import { getCollectTaskDetail, createCollectTask, updateCollectTask } from '@/api/collect'

type PlatformDefaults = {
  source: string
  method: string
  scopeLabels: string[]
}

const PLATFORM_DEFAULTS: Record<string, PlatformDefaults> = {
  WECHAT_OFFICIAL: {
    source: 'WECHAT_MP_API',
    method: 'INTERNAL',
    scopeLabels: ['粉丝统计', '粉丝列表', '图文列表', '图文明细', '图文内容'],
  },
  WECHAT_VIDEO: {
    source: 'WECHAT_CHANNELS_API',
    method: 'INTERNAL',
    scopeLabels: ['粉丝统计', '作品列表', '作品明细'],
  },
  DOUYIN: {
    source: 'DOUYIN_OPEN_API',
    method: 'INTERNAL',
    scopeLabels: ['粉丝统计', '粉丝列表', '作品列表', '作品明细'],
  },
  KUAISHOU: {
    source: 'KUAISHOU_OPEN_API',
    method: 'INTERNAL',
    scopeLabels: ['粉丝统计', '作品列表', '作品明细'],
  },
  XIAOHONGSHU: {
    source: 'XIAOHONGSHU_OPEN_API',
    method: 'INTERNAL',
    scopeLabels: ['粉丝统计', '笔记列表', '笔记明细'],
  },
  BILIBILI: {
    source: 'BILIBILI_OPEN_API',
    method: 'INTERNAL',
    scopeLabels: ['粉丝统计'],
  },
  WEWORK: {
    source: 'WECOM_API',
    method: 'INTERNAL',
    scopeLabels: ['企微日统计'],
  },
  WECHAT_PERSONAL: {
    source: 'AOCHUANG_API',
    method: 'INTERNAL',
    scopeLabels: ['好友同步', '消息同步'],
  },
}

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => Number(route.params.id) > 0)
const loading = ref(false)
const submitting = ref(false)
const showCronDoc = ref(false)

const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  platformType: undefined as string | undefined,
  accountId: undefined as number | undefined,
  method: undefined as string | undefined,
  source: undefined as string | undefined,
  dataType: undefined as string | undefined,
  frequency: undefined as string | undefined,
  cron: '',
  apiConfig: '',
  status: 'PENDING',
})

const isWeworkCollect = computed(() => form.platformType === 'WEWORK')

const collectScopeLabels = computed(() => {
  if (!form.platformType) return []
  return PLATFORM_DEFAULTS[form.platformType]?.scopeLabels ?? []
})

const applyPlatformDefaults = (platformType?: string) => {
  if (!platformType) return
  const defaults = PLATFORM_DEFAULTS[platformType]
  if (!defaults) return
  form.source = defaults.source
  form.method = defaults.method
  form.dataType = undefined
}

watch(
  () => form.platformType,
  (val, oldVal) => {
    applyPlatformDefaults(val)
    if (!isEdit.value && oldVal !== undefined && val !== oldVal) {
      form.accountId = undefined
    }
  }
)

const rules: FormRules = {
  name: [{ required: true, message: '请输入任务名', trigger: 'blur' }],
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  frequency: [{ required: true, message: '请选择频率', trigger: 'change' }],
  cron: [
    { required: true, message: '请输入 Cron 表达式', trigger: 'blur' },
    { pattern: /^[\d\s\?\*\/\,\-A-Za-z]+$/, message: 'Cron 表达式格式不正确', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const monitor = reactive({
  lastRunAt: '',
  nextRunAt: '',
  runCount: 0,
  failCount: 0,
  createdAt: '',
  updatedAt: '',
})

const cronDocs = [
  { field: '秒', allow: '0-59', wildcard: ', - * /' },
  { field: '分', allow: '0-59', wildcard: ', - * /' },
  { field: '时', allow: '0-23', wildcard: ', - * /' },
  { field: '日', allow: '1-31', wildcard: ', - * / ? L W' },
  { field: '月', allow: '1-12', wildcard: ', - * /' },
  { field: '周', allow: '1-7', wildcard: ', - * / ? L #' },
]

const loadDetail = async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const data = await getCollectTaskDetail(id)
    Object.assign(form, data)
    Object.assign(monitor, data)
    applyPlatformDefaults(form.platformType)
    // AccountSelect clears v-model when platformType prop first binds; restore after child sync
    if (data.accountId != null) {
      await nextTick()
      form.accountId = data.accountId
    }
  } catch {
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请填写必填项')
    return
  }
  submitting.value = true
  try {
    const payload = {
      ...form,
      dataType: form.dataType || undefined,
    }
    if (isEdit.value) {
      await updateCollectTask(payload as any)
      ElMessage.success('保存成功')
    } else {
      await createCollectTask(payload as any)
      ElMessage.success('创建成功')
    }
    router.push('/collect/task')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.task-edit-page { padding: 20px; }
.collect-scope { line-height: 28px; }
</style>
