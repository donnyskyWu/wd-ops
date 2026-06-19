<template>
  <el-dialog v-model="visible" title="排版对比" width="720px" append-to-body>
    <el-row :gutter="16">
      <el-col :span="12">
        <div class="compare-label">排版前</div>
        <div class="compare-pane" v-html="beforeHtml" />
      </el-col>
      <el-col :span="12">
        <div class="compare-label">排版后</div>
        <div class="compare-pane" v-html="afterHtml" />
      </el-col>
    </el-row>
    <el-alert
      v-if="textPreserved"
      type="success"
      :closable="false"
      show-icon
      title="文字内容已保留（ADR-020）"
      class="preserve-alert"
    />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirm">应用排版</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: boolean
  beforeHtml: string
  afterHtml: string
  plainTextBefore?: string
  plainTextAfter?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const textPreserved = computed(
  () =>
    props.plainTextBefore != null &&
    props.plainTextAfter != null &&
    props.plainTextBefore === props.plainTextAfter,
)

function confirm() {
  emit('confirm')
  visible.value = false
}
</script>

<style scoped>
.compare-label {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.compare-pane {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 12px;
  min-height: 200px;
  max-height: 360px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.75;
}

.preserve-alert {
  margin-top: 12px;
}
</style>
