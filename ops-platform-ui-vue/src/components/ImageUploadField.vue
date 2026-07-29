<template>
  <div class="image-upload-field">
    <el-image
      v-if="displayUrl"
      :src="displayUrl"
      fit="cover"
      class="upload-preview"
      :preview-src-list="[displayUrl]"
    />
    <el-upload
      :accept="IMAGE_ACCEPT"
      :show-file-list="false"
      :http-request="handleUpload"
    >
      <el-button :loading="uploading" size="small">
        {{ modelValue ? '重新上传' : '上传图片' }}
      </el-button>
    </el-upload>
    <el-button
      v-if="modelValue"
      link
      type="danger"
      size="small"
      @click="handleClear"
    >
      清除
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { appendFileAuth } from '@/utils/fileUrl'
import { IMAGE_ACCEPT, OA_FILE_VIEW_PREFIX, uploadContentImage, validateImageFile } from '@/api/file'

/** Build preview URL from storage key / infra url when API omits previewUrl. */
function viewUrlForKey(key: string): string {
  if (!key) return ''
  if (
    key.startsWith('/admin-api/')
    || key.startsWith('http://')
    || key.startsWith('https://')
    || key.startsWith('//')
  ) {
    return key
  }
  // 历史本地盘 key（tenantId/...）过渡预览；新上传为 infra url
  return OA_FILE_VIEW_PREFIX + encodeURIComponent(key)
}

const props = defineProps<{
  modelValue?: string
  previewUrl?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:previewUrl': [value: string]
}>()

const uploading = ref(false)
const localPreview = ref('')

// Parent reload (e.g. edit dialog after GET detail) must not keep a stale upload preview.
watch(
  () => [props.modelValue, props.previewUrl] as const,
  () => {
    localPreview.value = ''
  },
)

const displayUrl = computed(() => {
  const url = localPreview.value || props.previewUrl || viewUrlForKey(props.modelValue || '')
  return url ? appendFileAuth(url) : ''
})

const handleUpload = async (options: { file: File }) => {
  const err = validateImageFile(options.file)
  if (err) {
    ElMessage.error(err)
    return
  }
  uploading.value = true
  try {
    const uploaded = await uploadContentImage(options.file)
    emit('update:modelValue', uploaded.key)
    emit('update:previewUrl', uploaded.url)
    localPreview.value = uploaded.url
    ElMessage.success('上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const handleClear = () => {
  emit('update:modelValue', '')
  emit('update:previewUrl', '')
  localPreview.value = ''
}
</script>

<style scoped>
.image-upload-field {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.upload-preview {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}
</style>
