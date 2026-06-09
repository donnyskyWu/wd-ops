<!--
  M10 - 采集任务编辑
  依据: UX-M10 § 3 P-M10-002 / PRD-M10 FR-M10-001
  路径: /collect/task/:id  (id=0 表示新增)
-->
<template>
  <div class="task-edit-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/collect/task' }">采集任务</el-breadcrumb-item>
      <el-breadcrumb-item>{{ isEdit ? '编辑任务' : '新增任务' }}</el-breadcrumb-item>
    </el-breadcrumb>

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
        <el-form-item label="账号" prop="accountId">
          <AccountSelect v-model="form.accountId" :platform-type="form.platformType" placeholder="请选择账号" />
        </el-form-item>
        <el-form-item label="采集方式" prop="method">
          <DictSelect v-model="form.method" dict-type="dict_collect_method" placeholder="请选择方式" />
        </el-form-item>
        <el-form-item label="数据来源" prop="source">
          <DictSelect v-model="form.source" dict-type="dict_collect_source" placeholder="请选择来源" />
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
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import DictSelect from '@/components/DictSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'
import { getCollectTaskDetail, createCollectTask, updateCollectTask } from '@/api/collect'
import { mockCollectTasks } from '@/mock/collect'

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
  frequency: undefined as string | undefined,
  cron: '',
  apiConfig: '',
  status: 'ENABLED',
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入任务名', trigger: 'blur' }],
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账号', trigger: 'change' }],
  method: [{ required: true, message: '请选择采集方式', trigger: 'change' }],
  source: [{ required: true, message: '请选择数据来源', trigger: 'change' }],
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
  } catch {
    // mock 兜底
    const m = mockCollectTasks.find((t) => t.id === id)
    if (m) {
      Object.assign(form, m)
      Object.assign(monitor, m)
    }
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
    if (isEdit.value) {
      await updateCollectTask(form as any)
      ElMessage.success('保存成功')
    } else {
      await createCollectTask(form as any)
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
</style>
