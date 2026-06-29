<template>
  <el-dialog
    v-model="visible"
    title="一键排版"
    width="920px"
    append-to-body
    destroy-on-close
    class="wechat-quick-typeset-dialog"
    @open="onOpen"
  >
    <el-row :gutter="20" class="typeset-layout">
      <el-col :span="14">
        <div class="pane-label">排版预览</div>
        <ResourcePreviewPane :html="previewHtml" :loading="false" />
      </el-col>
      <el-col :span="10">
        <div class="pane-label">选择风格</div>
        <div class="style-list">
          <div
            v-for="style in WECHAT_QUICK_STYLES"
            :key="style.id"
            class="style-card"
            :class="{ active: selectedStyleId === style.id }"
            @click="selectedStyleId = style.id"
          >
            <div class="style-card-name">{{ style.name }}</div>
            <div class="style-card-desc">{{ style.description }}</div>
          </div>
        </div>

        <div class="pane-label theme-label">主题色</div>
        <div class="theme-colors">
          <button
            v-for="color in WECHAT_QUICK_THEME_COLORS"
            :key="color.id"
            type="button"
            class="theme-swatch"
            :class="{ active: selectedThemeId === color.id }"
            :title="color.name"
            @click="selectedThemeId = color.id"
          >
            <span class="swatch-dot" :style="{ background: color.primary }" />
            <span class="swatch-name">{{ color.name }}</span>
          </button>
        </div>
      </el-col>
    </el-row>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!canApply" @click="confirm">使用此排版</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import ResourcePreviewPane from './ResourcePreviewPane.vue'
import {
  WECHAT_QUICK_STYLES,
  WECHAT_QUICK_THEME_COLORS,
  mergeWechatQuickTypeset,
  type WechatQuickStyleId,
} from '@/utils/wechatQuickTypeset'

const props = defineProps<{
  modelValue: boolean
  title: string
  author?: string
  bodyHtml: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  apply: [html: string]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const selectedStyleId = ref<WechatQuickStyleId>('classic')
const selectedThemeId = ref(WECHAT_QUICK_THEME_COLORS[0].id)

const canApply = computed(
  () => !!(props.title?.trim() || props.bodyHtml?.trim()),
)

const previewHtml = computed(() =>
  mergeWechatQuickTypeset({
    title: props.title || '请输入标题',
    author: props.author,
    bodyHtml: props.bodyHtml,
    styleId: selectedStyleId.value,
    themeColorId: selectedThemeId.value,
  }),
)

function onOpen() {
  selectedStyleId.value = 'classic'
  selectedThemeId.value = WECHAT_QUICK_THEME_COLORS[0].id
}

function confirm() {
  if (!canApply.value) return
  emit('apply', previewHtml.value)
  visible.value = false
}

watch([() => props.title, () => props.bodyHtml], () => {
  // previewHtml is computed — no-op hook for future side effects
})
</script>

<style scoped>
.typeset-layout {
  min-height: 420px;
}

.pane-label {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}

.theme-label {
  margin-top: 18px;
}

.style-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.style-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.style-card:hover,
.style-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.style-card-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.style-card-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.theme-colors {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.theme-swatch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 20px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.theme-swatch:hover,
.theme-swatch.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.swatch-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
}

.swatch-name {
  white-space: nowrap;
}
</style>
