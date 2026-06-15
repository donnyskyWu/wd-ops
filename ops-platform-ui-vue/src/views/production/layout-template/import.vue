<template>

  <div class="layout-import-page">

    <el-page-header content="公推模板导入向导" @back="router.back()" />



    <el-alert

      type="info"

      show-icon

      :closable="false"

      class="disclaimer"

      title="仅提取版式与样式"

      description="系统将仅提取版式与样式作为模板，不会保存原文正文。导入预览中的文字为样式示意，套用模板时将保留您内容中的原有文字。"

    />



    <el-tabs v-model="activeTab">

      <el-tab-pane label="公众号链接" name="url">

        <el-alert type="warning" show-icon :closable="false" title="链接抓取可能失败" description="失败时请改用手动粘贴 HTML 或 Word 导入。" />

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



      <el-tab-pane label="Word 上传" name="docx">

        <el-alert type="info" show-icon :closable="false" title="版式需人工微调" description="复杂 Word 版式无法 100% 还原，导入后请在编辑器中调整槽位。" />

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

            <el-button type="primary" :loading="importing" @click="handlePasteImport">导入为草稿</el-button>

          </el-form-item>

        </el-form>

      </el-tab-pane>

    </el-tabs>



    <el-card v-if="jobPreview" shadow="never" class="job-preview">

      <template #header>
        <span v-if="jobPreview.suggestedName">骨架预览 · {{ jobPreview.suggestedName }}</span>
        <span v-else>骨架预览 · <DictLabel dict-type="dict_layout_import_job_status" :value="jobPreview.status" /></span>
      </template>

      <p v-if="jobPreview.errorMessage" class="error">{{ jobPreview.errorMessage }}</p>

      <p v-if="jobPreview.extractionReport?.strippedCharCount" class="meta">

        已剥离 {{ jobPreview.extractionReport.strippedCharCount }} 字样本正文，识别 {{ jobPreview.extractionReport.slotCount }} 个槽位

      </p>

      <LayoutSchemaEditor v-if="jobPreview.previewLayoutSchema" v-model="jobPreview.previewLayoutSchema" />

      <el-button v-if="jobPreview.status === 'SUCCESS'" type="primary" class="confirm-btn" @click="confirmJobTemplate">

        确认创建模板

      </el-button>

    </el-card>

  </div>

</template>



<script setup lang="ts">

import { reactive, ref } from 'vue'

import { useRouter } from 'vue-router'

import { ElMessage } from 'element-plus'

import type { UploadFile } from 'element-plus'

import LayoutSchemaEditor from '@/components/layout/LayoutSchemaEditor.vue'
import DictLabel from '@/components/DictLabel.vue'

import {

  createLayoutTemplate,

  getLayoutImportJob,

  importLayoutDocx,

  importLayoutPaste,

  importLayoutUrl

} from '@/api/layoutTemplate'

import type { LayoutImportJobVO, LayoutSchema } from '@/types/layoutTemplate'



const router = useRouter()

const activeTab = ref('paste')

const importing = ref(false)

const docxFile = ref<File | null>(null)

const docxName = ref('')

const jobPreview = ref<(LayoutImportJobVO & { previewLayoutSchema?: LayoutSchema }) | null>(null)



const urlForm = reactive({ sourceUrl: '', templateName: '' })

const pasteForm = reactive({ templateName: '粘贴导入', html: '' })



function handleFileChange(file: UploadFile) {

  docxFile.value = file.raw || null

}



async function handleUrlImport() {

  importing.value = true

  try {

    const resp = await importLayoutUrl(urlForm)

    jobPreview.value = await getLayoutImportJob(resp.jobId)

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

  try {

    const fd = new FormData()

    fd.append('file', docxFile.value)

    if (docxName.value) fd.append('templateName', docxName.value)

    const resp = await importLayoutDocx(fd)

    jobPreview.value = await getLayoutImportJob(resp.jobId)

  } finally {

    importing.value = false

  }

}



async function handlePasteImport() {

  importing.value = true

  try {

    await importLayoutPaste(pasteForm)

    ElMessage.success('导入成功（仅版式骨架）')

    router.push('/layout-template')

  } finally {

    importing.value = false

  }

}



async function confirmJobTemplate() {

  if (!jobPreview.value?.previewLayoutSchema) return

  await createLayoutTemplate({

    templateName: jobPreview.value.suggestedName || '导入模板',

    layoutSchema: jobPreview.value.previewLayoutSchema,

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

</style>

