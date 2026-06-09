<!--
  内容创作/编辑
  依据: UX-M2 § 5.2 内容编辑
  关联: PRD-M2 § FR-M2-003, STATE-M2 内容状态机
-->
<template>
  <div class="content-edit-page">
    <el-breadcrumb separator="/" style="margin-bottom: 16px">
      <el-breadcrumb-item :to="{ path: '/content' }">内容管理</el-breadcrumb-item>
      <el-breadcrumb-item>{{ isEdit ? '编辑内容' : '创作内容' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card shadow="never">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        style="max-width: 900px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入内容标题" maxlength="100" show-word-limit />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="平台" prop="platformType">
              <DictSelect v-model="formData.platformType" dict-type="dict_platform_type" placeholder="请选择平台" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容类型" prop="contentType">
              <DictSelect v-model="formData.contentType" dict-type="dict_content_type" placeholder="请选择类型" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="发布账号" prop="accountId">
          <AccountSelect v-model="formData.accountId" :platform-type="formData.platformType" placeholder="请选择账号" />
        </el-form-item>

        <el-form-item label="封面">
          <el-input v-model="formData.cover" placeholder="封面图 URL" />
        </el-form-item>

        <el-form-item label="是否 AI 生成" prop="isAi">
          <el-switch v-model="formData.isAi" />
          <el-button
            v-if="formData.isAi"
            link
            type="primary"
            style="margin-left: 16px"
            @click="aiDialogVisible = true"
          >
            AI 辅助创作
          </el-button>
        </el-form-item>

        <el-form-item label="正文" prop="body">
          <el-input
            v-model="formData.body"
            type="textarea"
            :rows="12"
            placeholder="请输入正文"
            maxlength="10000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="formData.tagsText" placeholder="多个标签用逗号分隔" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave('DRAFT')">
            保存草稿
          </el-button>
          <el-button type="success" :loading="saving" @click="handleSave('PENDING_REVIEW')">
            提交审核
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- AI 辅助创作弹窗 -->
    <el-dialog v-model="aiDialogVisible" title="AI 辅助创作" width="640px">
      <el-form label-width="80px">
        <el-form-item label="模型">
          <DictSelect v-model="aiForm.model" dict-type="dict_ai_model" />
        </el-form-item>
        <el-form-item label="提示词">
          <el-input v-model="aiForm.prompt" type="textarea" :rows="4" placeholder="请描述你想创作的内容主题、风格、字数要求" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="aiGenerating" @click="handleAiGenerate">生成</el-button>
        </el-form-item>
        <el-form-item v-if="aiResult" label="生成结果">
          <el-input v-model="aiResult" type="textarea" :rows="8" readonly />
        </el-form-item>
        <el-form-item v-if="aiResult">
          <el-button type="success" @click="adoptAiResult">采纳</el-button>
          <el-button @click="aiResult = ''">丢弃</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import DictSelect from '@/components/DictSelect.vue'
import AccountSelect from '@/components/selectors/AccountSelect.vue'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref<FormInstance>()
const saving = ref(false)

const formData = reactive({
  title: '',
  platformType: undefined as string | undefined,
  contentType: undefined as string | undefined,
  accountId: undefined as number | undefined,
  cover: '',
  isAi: false,
  body: '',
  tagsText: '',
})
const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  platformType: [{ required: true, message: '请选择平台', trigger: 'change' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择发布账号', trigger: 'change' }],
  body: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

const aiDialogVisible = ref(false)
const aiForm = reactive({ model: 'GPT-4', prompt: '' })
const aiGenerating = ref(false)
const aiResult = ref('')

const handleSave = async (action: 'DRAFT' | 'PENDING_REVIEW') => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  // TODO: 接通 POST /admin-api/oa/content/save?action=DRAFT|PENDING_REVIEW
  await new Promise((r) => setTimeout(r, 600))
  saving.value = false
  ElMessage.success(action === 'DRAFT' ? '草稿已保存' : '已提交审核')
  router.push('/content')
}

const handleCancel = () => {
  router.push('/content')
}

const handleAiGenerate = async () => {
  if (!aiForm.prompt.trim()) {
    ElMessage.warning('请输入提示词')
    return
  }
  aiGenerating.value = true
  // TODO: 接通流式输出 /admin-api/oa/ai/generate
  await new Promise((r) => setTimeout(r, 1500))
  aiResult.value = `【AI 草稿 - ${aiForm.model}】\n基于提示词「${aiForm.prompt}」生成的初稿内容。\n（实际接入后会流式输出）`
  aiGenerating.value = false
}

const adoptAiResult = () => {
  formData.body = aiResult.value
  aiDialogVisible.value = false
  ElMessage.success('已采纳 AI 生成内容')
}

onMounted(() => {
  if (isEdit.value) {
    // TODO: 加载已有内容
  }
})
</script>

<style scoped>
.content-edit-page { padding: 20px; }
</style>
