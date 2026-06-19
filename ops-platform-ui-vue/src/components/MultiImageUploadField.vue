<template>
  <div class="multi-image-upload-field">
    <div v-if="previewItems.length" class="image-grid">
      <div v-for="(item, index) in previewItems" :key="item.key" class="image-item">
        <el-image
          :src="item.url"
          fit="cover"
          class="upload-preview"
          :preview-src-list="previewItems.map((p) => p.url)"
          :initial-index="index"
        />
        <el-button link type="danger" size="small" @click="removeAt(index)">删除</el-button>
      </div>
    </div>
    <el-upload
      v-if="modelValue.length < maxCount"
      :accept="IMAGE_ACCEPT"
      :show-file-list="false"
      :http-request="handleUpload"
    >
      <el-button :loading="uploading" size="small">
        <el-icon><Plus /></el-icon>
        上传图片
      </el-button>
    </el-upload>
    <span v-if="modelValue.length >= maxCount" class="limit-tip">最多 {{ maxCount }} 张</span>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { appendFileAuth } from '@/utils/fileUrl'
import { IMAGE_ACCEPT, uploadContentImage, validateImageFile } from '@/api/file'

const OA_FILE_VIEW_PREFIX = '/admin-api/oa/file/view?key='

/** Build preview URL from storage key when API omits businessLicenseUrls (reload path). */
function viewUrlForKey(key: string): string {
  if (!key) return ''
  if (key.startsWith('/admin-api/') || key.startsWith('http://') || key.startsWith('https://')) {
    return key
  }
  return OA_FILE_VIEW_PREFIX + key
}

const props = withDefaults(defineProps<{
  modelValue?: string[]
  previewUrls?: string[]
  maxCount?: number
}>(), {
  modelValue: () => [],
  previewUrls: () => [],
  maxCount: 10,
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  'update:previewUrls': [value: string[]]
}>()

const uploading = ref(false)

const previewItems = computed(() =>
  props.modelValue.map((key, index) => ({
    key,
    url: appendFileAuth(props.previewUrls[index] || viewUrlForKey(key)),
  })).filter((item) => item.key && item.url),
)

const removeAt = (index: number) => {
  const keys = [...props.modelValue]
  const urls = [...props.previewUrls]
  keys.splice(index, 1)
  urls.splice(index, 1)
  emit('update:modelValue', keys)
  emit('update:previewUrls', urls)
}

const handleUpload = async (options: { file: File }) => {
  const err = validateImageFile(options.file)
  if (err) {
    ElMessage.error(err)
    return
  }
  if (props.modelValue.length >= props.maxCount) {
    ElMessage.warning(`最多上传 ${props.maxCount} 张图片`)
    return
  }
  uploading.value = true
  try {
    const uploaded = await uploadContentImage(options.file)
    emit('update:modelValue', [...props.modelValue, uploaded.key])
    emit('update:previewUrls', [...props.previewUrls, uploaded.url])
    ElMessage.success('上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.multi-image-upload-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.upload-preview {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.limit-tip {
  color: #909399;
  font-size: 12px;
}
</style>
