<template>

  <div class="layout-import-page">

    <el-page-header content="提取版式模板" @back="router.back()" />



    <el-alert

      type="info"

      show-icon

      :closable="false"

      class="disclaimer"

      title="导入保留完整视觉效果"

      description="导入时将保存原文的完整 HTML 样式用于模板预览（与公众号原文一致）。套用模板到内容时，仍只合并版式、保留您正文中的原有文字（ADR-020）。"

    />



    <el-tabs v-model="activeTab">

      <el-tab-pane label="公众号链接" name="url">

        <el-alert type="warning" show-icon :closable="false" title="链接抓取可能失败" description="若出现微信反爬验证，请改用手动粘贴 HTML 或上传浏览器保存的 .mhtml 文件。" />

        <el-form label-width="100px" class="import-form">

          <el-form-item label="文章 URL">

            <el-input v-model="urlForm.sourceUrl" placeholder="https://mp.weixin.qq.com/s/..." />

          </el-form-item>

          <el-form-item label="模板名称">

            <el-input v-model="urlForm.templateName" placeholder="可选" />

          </el-form-item>

          <el-form-item>

            <el-button type="primary" :loading="importing" @click="handleUrlImport">开始导入</el-button>

          </el-form-item>

        </el-form>

      </el-tab-pane>



      <el-tab-pane label="MHTML 上传" name="mhtml">

        <el-alert type="info" show-icon :closable="false" title="离线导入" description="在浏览器打开公众号文章后「另存为」MHTML，上传即可绕过微信反爬。" />

        <el-form label-width="100px" class="import-form">

          <el-form-item label="文件">

            <el-upload :auto-upload="false" :limit="1" accept=".mhtml" :on-change="handleMhtmlChange">

              <el-button>选择 .mhtml</el-button>

            </el-upload>

          </el-form-item>

          <el-form-item label="模板名称">

            <el-input v-model="mhtmlName" placeholder="可选" />

          </el-form-item>

          <el-form-item>

            <el-button type="primary" :loading="importing" @click="handleMhtmlImport">上传并解析</el-button>

          </el-form-item>

        </el-form>

      </el-tab-pane>



      <el-tab-pane label="Word 上传" name="docx">

        <el-alert type="info" show-icon :closable="false" title="保留 Word 样式预览" description="导入时会读取 Word 段落/字体样式并转为 inline HTML 预览（与链接导入一致）。表格、文本框、Word 艺术字等复杂元素仍可能无法完整还原，请在编辑器中微调槽位。" />

        <el-form label-width="100px" class="import-form">

          <el-form-item label="文件">

            <el-upload :auto-upload="false" :limit="1" accept=".docx" :on-change="handleFileChange">

              <el-button>选择 .docx</el-button>

            </el-upload>

          </el-form-item>

          <el-form-item label="模板名称">

            <el-input v-model="docxName" placeholder="可选" />

          </el-form-item>

          <el-form-item>

            <el-button type="primary" :loading="importing" @click="handleDocxImport">上传并解析</el-button>

          </el-form-item>

        </el-form>

      </el-tab-pane>



      <el-tab-pane label="粘贴 HTML" name="paste">

        <el-form label-width="100px" class="import-form">

          <el-form-item label="模板名称">

            <el-input v-model="pasteForm.templateName" />

          </el-form-item>

          <el-form-item label="HTML">

            <el-input v-model="pasteForm.html" type="textarea" :rows="12" placeholder="粘贴从浏览器复制的富文本 HTML" />

          </el-form-item>

          <el-form-item>

            <el-button type="primary" :loading="importing" @click="handlePasteImport">开始解析</el-button>

          </el-form-item>

        </el-form>

      </el-tab-pane>

    </el-tabs>



    <el-card v-if="jobPreview" shadow="never" class="job-preview">

      <template #header>
        <span v-if="jobPreview.suggestedName">导入预览 · {{ jobPreview.suggestedName }}</span>
        <span v-else>导入预览 · <DictLabel dict-type="dict_layout_import_job_status" :value="jobPreview.status" /></span>
      </template>

      <el-alert
        v-if="jobPreview.status === 'FAILED'"
        type="error"
        show-icon
        :closable="false"
        :title="jobPreview.errorMessage || '导入失败'"
        class="preview-alert"
      />

      <el-alert
        v-else-if="jobPreview.status === 'SUCCESS' && !hasFullPreview"
        type="warning"
        show-icon
        :closable="false"
        title="未能生成完整视觉预览"
        description="请尝试粘贴 HTML 或上传 .mhtml 文件后重试。"
        class="preview-alert"
      />

      <p v-if="jobPreview.extractionReport?.slotCount != null" class="meta">

        识别 {{ jobPreview.extractionReport.slotCount }} 个槽位

        <template v-if="jobPreview.extractionReport.inlineStyleCount">

          · {{ jobPreview.extractionReport.inlineStyleCount }} 处 inline 样式

        </template>

      </p>

      <ResourcePreviewPane
        v-if="hasFullPreview"
        :html="jobPreview.previewHtml"
        title="完整视觉效果（与原文一致）"
        class="full-preview"
      />

      <el-collapse v-if="jobPreview.previewLayoutSchema && jobPreview.status === 'SUCCESS'" class="advanced-panel">
        <el-collapse-item title="高级：验证结构还原（可选）" name="fidelity">
          <p class="advanced-hint">将示例正文套用模板槽位，检查 merge 输出是否与上方完整预览有差异。</p>
          <el-form label-width="100px" class="fidelity-form">
            <el-form-item label="样本正文">
              <el-input v-model="sampleBody" type="textarea" :rows="3" placeholder="用于验证套用还原度的示例段落" />
            </el-form-item>
            <el-form-item>
              <el-button :loading="validating" @click="runFidelityCheck">验证结构还原</el-button>
            </el-form-item>
          </el-form>
          <el-card v-if="fidelityPreview" shadow="never" class="fidelity-preview">
            <template #header>样本套用预览</template>
            <ul v-if="fidelityNotes.length" class="warnings">
              <li v-for="(n, i) in fidelityNotes" :key="i">{{ n }}</li>
            </ul>
            <div class="fidelity-html" v-html="fidelityPreview.layoutHtml" />
          </el-card>
        </el-collapse-item>
      </el-collapse>

      <el-button v-if="jobPreview.status === 'SUCCESS'" type="primary" class="confirm-btn" @click="confirmJobTemplate">

        确认创建模板

      </el-button>

    </el-card>

  </div>

</template>



<script setup lang="ts">

import { reactive, ref, computed } from 'vue'

import { useRouter } from 'vue-router'

import { ElMessage } from 'element-plus'

import type { UploadFile } from 'element-plus'

import ResourcePreviewPane from '@/components/layout/ResourcePreviewPane.vue'
import DictLabel from '@/components/DictLabel.vue'

import {

  createLayoutTemplate,

  getLayoutImportJob,

  importLayoutDocx,

  importLayoutMhtml,

  importLayoutPastePreview,

  importLayoutUrl,

  validateExtractFidelity,

} from '@/api/layoutTemplate'

import type { LayoutImportJobVO, LayoutMergePreviewVO, LayoutSchema } from '@/types/layoutTemplate'



const router = useRouter()

const activeTab = ref('url')

const importing = ref(false)

const docxFile = ref<File | null>(null)

const mhtmlFile = ref<File | null>(null)

const docxName = ref('')

const mhtmlName = ref('')

const jobPreview = ref<(LayoutImportJobVO & { previewLayoutSchema?: LayoutSchema }) | null>(null)
const sampleBody = ref('示例标题\n\n这是第一段正文，用于验证模板槽位与段落映射。\n\n这是第二段正文。')
const validating = ref(false)
const fidelityPreview = ref<LayoutMergePreviewVO | null>(null)

const hasFullPreview = computed(() => Boolean(jobPreview.value?.previewHtml?.trim()))

const fidelityNotes = computed(() => {
  const report = fidelityPreview.value?.extractionReport as { fidelityNotes?: string[] } | undefined
  return report?.fidelityNotes || []
})



const urlForm = reactive({ sourceUrl: '', templateName: '' })

const pasteForm = reactive({ templateName: '粘贴导入', html: '' })



function handleFileChange(file: UploadFile) {

  docxFile.value = file.raw || null

}

function handleMhtmlChange(file: UploadFile) {

  mhtmlFile.value = file.raw || null

}



async function loadImportJob(jobId: number) {
  jobPreview.value = await getLayoutImportJob(jobId)
  if (jobPreview.value?.status === 'FAILED') {
    ElMessage.error(jobPreview.value.errorMessage || '导入失败')
  }
}



async function runFidelityCheck() {
  if (!jobPreview.value?.previewLayoutSchema) return
  validating.value = true
  try {
    fidelityPreview.value = await validateExtractFidelity(
      jobPreview.value.previewLayoutSchema,
      sampleBody.value,
    )
  } finally {
    validating.value = false
  }
}

async function handleUrlImport() {

  if (!urlForm.sourceUrl?.trim()) {
    ElMessage.warning('请输入文章 URL')
    return
  }

  importing.value = true
  fidelityPreview.value = null

  try {

    const resp = await importLayoutUrl(urlForm)

    await loadImportJob(resp.jobId)

  } finally {

    importing.value = false

  }

}



async function handleMhtmlImport() {

  if (!mhtmlFile.value) {

    ElMessage.warning('请选择 .mhtml 文件')

    return

  }

  importing.value = true
  fidelityPreview.value = null

  try {

    const fd = new FormData()

    fd.append('file', mhtmlFile.value)

    if (mhtmlName.value) fd.append('templateName', mhtmlName.value)

    const resp = await importLayoutMhtml(fd)

    await loadImportJob(resp.jobId)

  } finally {

    importing.value = false

  }

}



async function handleDocxImport() {

  if (!docxFile.value) {

    ElMessage.warning('请选择 .docx 文件')

    return

  }

  importing.value = true
  fidelityPreview.value = null

  try {

    const fd = new FormData()

    fd.append('file', docxFile.value)

    if (docxName.value) fd.append('templateName', docxName.value)

    const resp = await importLayoutDocx(fd)

    await loadImportJob(resp.jobId)

  } finally {

    importing.value = false

  }

}



async function handlePasteImport() {

  if (!pasteForm.html?.trim()) {

    ElMessage.warning('请粘贴 HTML')

    return

  }

  importing.value = true

  fidelityPreview.value = null

  try {

    const resp = await importLayoutPastePreview(pasteForm)

    await loadImportJob(resp.jobId)

  } finally {

    importing.value = false

  }

}



async function confirmJobTemplate() {

  if (!jobPreview.value?.previewLayoutSchema) return

  const sourceType = jobPreview.value.sourceType || 'MANUAL'

  await createLayoutTemplate({

    templateName: jobPreview.value.suggestedName || '导入模板',

    layoutSchema: jobPreview.value.previewLayoutSchema,

    previewHtml: jobPreview.value.previewHtml,

    layoutHtml: jobPreview.value.previewHtml,

    styleCss: jobPreview.value.styleCss,

    sourceType,

    sourceUrl: jobPreview.value.sourceUrl,

    status: 'DRAFT'

  })

  ElMessage.success('模板已创建')

  router.push('/layout-template')

}

</script>



<style scoped>

.layout-import-page {

  padding: 16px;

}



.disclaimer {

  margin: 16px 0;

}



.import-form {

  margin-top: 16px;

  max-width: 720px;

}



.job-preview {

  margin-top: 24px;

}



.preview-alert {

  margin-bottom: 12px;

}



.full-preview {

  margin-bottom: 16px;

}



.advanced-panel {

  margin: 12px 0;

}

.advanced-hint {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}



.confirm-btn {

  margin-top: 16px;

}



.error {

  color: #f56c6c;

}



.meta {

  color: #909399;

  font-size: 13px;

}

.warnings {
  color: #e6a23c;
  font-size: 12px;
  margin: 8px 0;
  padding-left: 18px;
}

.fidelity-form {
  max-width: 720px;
}

.fidelity-preview {
  margin: 12px 0;
}

.fidelity-html {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  max-height: 280px;
  overflow-y: auto;
}

</style>
