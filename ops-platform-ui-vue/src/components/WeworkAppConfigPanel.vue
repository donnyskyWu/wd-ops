<template>
  <el-card shadow="never" class="wework-app-config-panel">
    <template #header>
      <div class="card-header">
        <span>企业微信应用配置</span>
        <el-button type="primary" link @click="handleEdit">
          {{ weworkConfig ? '编辑配置' : '新增配置' }}
        </el-button>
      </div>
    </template>
    <el-descriptions v-if="weworkConfig" :column="2" border size="small">
      <el-descriptions-item label="账号名称">{{ weworkConfig.accountName }}</el-descriptions-item>
      <el-descriptions-item label="Corp ID">{{ weworkConfig.corpId }}</el-descriptions-item>
      <el-descriptions-item label="Agent ID">{{ weworkConfig.agentId }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="weworkConfig.status === 'ENABLED' ? 'success' : 'info'" size="small">
          {{ weworkConfig.status === 'ENABLED' ? '正常' : '停用' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="尚未配置企业微信应用，请点击右上角新增" :image-size="64" />
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
    <el-form :model="form" ref="formRef" :rules="rules" label-width="100px">
      <el-form-item label="账号名称" prop="accountName">
        <el-input v-model="form.accountName" maxlength="100" />
      </el-form-item>
      <el-form-item label="Corp ID" prop="corpId">
        <el-input v-model="form.corpId" maxlength="64" :disabled="!!form.id" />
      </el-form-item>
      <el-form-item label="Agent ID" prop="agentId">
        <el-input v-model="form.agentId" maxlength="64" :disabled="!!form.id" />
      </el-form-item>
      <el-form-item label="Secret" prop="secret">
        <el-input
          v-model="form.secret"
          type="password"
          show-password
          maxlength="128"
          :placeholder="form.id ? '留空则不修改' : '请输入 Secret'"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="正常" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getWeworkPage,
  createWework,
  updateWework,
  type WeworkVO,
} from '@/api/personal-account'

const emit = defineEmits<{
  'config-change': [config: WeworkVO | null]
}>()

const loading = ref(false)
const submitLoading = ref(false)
const weworkList = ref<WeworkVO[]>([])
const weworkConfig = computed(() => weworkList.value[0] ?? null)

const dialogVisible = ref(false)
const dialogTitle = ref('新增企微应用配置')
const formRef = ref<any>()
const form = reactive({
  id: undefined as number | undefined,
  accountName: '',
  corpId: '',
  agentId: '',
  secret: '',
  status: 'ENABLED',
})

const rules = {
  accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
  corpId: [{ required: true, message: '请输入 Corp ID', trigger: 'blur' }],
  agentId: [{ required: true, message: '请输入 Agent ID', trigger: 'blur' }],
  secret: [{
    validator: (_: unknown, val: string, cb: (e?: Error) => void) => {
      if (!form.id && !val) cb(new Error('请输入 Secret'))
      else cb()
    },
    trigger: 'blur',
  }],
}

const notifyConfigChange = () => {
  emit('config-change', weworkConfig.value)
}

const loadConfig = async () => {
  loading.value = true
  try {
    const res = await getWeworkPage({ pageNo: 1, pageSize: 1 })
    weworkList.value = res.list
    notifyConfigChange()
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  if (weworkConfig.value) {
    dialogTitle.value = '编辑企微应用配置'
    Object.assign(form, {
      id: weworkConfig.value.id,
      accountName: weworkConfig.value.accountName,
      corpId: weworkConfig.value.corpId,
      agentId: weworkConfig.value.agentId,
      secret: '',
      status: weworkConfig.value.status,
    })
  } else {
    dialogTitle.value = '新增企微应用配置'
    Object.assign(form, {
      id: undefined,
      accountName: '',
      corpId: '',
      agentId: '',
      secret: '',
      status: 'ENABLED',
    })
  }
  dialogVisible.value = true
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (form.id) {
      await updateWework({
        id: form.id,
        accountName: form.accountName,
        secret: form.secret || undefined,
        status: form.status,
      })
    } else {
      await createWework({
        accountName: form.accountName,
        corpId: form.corpId,
        agentId: form.agentId,
        secret: form.secret,
        status: form.status,
      })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadConfig()
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ weworkConfig, loadConfig })

onMounted(() => loadConfig())
</script>

<style scoped lang="scss">
.wework-app-config-panel {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
