<template>
  <el-drawer
    v-model="visible"
    :size="720"
    direction="rtl"
    :show-close="false"
    :close-on-click-modal="false"
    class="ai-content-drawer"
    append-to-body
    destroy-on-close
    @closed="handleClosed"
  >
    <template #header>
      <div class="drawer-header">
        <span class="drawer-title">🤖 AI 内容生成助手</span>
        <div class="drawer-header-right">
          <el-dropdown trigger="click" @command="handleModelChange">
            <div class="model-select">
              <span class="model-icon">{{ currentModel?.icon || '🔵' }}</span>
              <span class="model-name">{{ currentModel?.name || 'Qwen' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in modelOptions"
                  :key="item.id"
                  :command="item.id"
                  :class="{ 'is-active': item.id === selectedModel }"
                >
                  <span class="model-icon">{{ item.icon }}</span>
                  {{ item.name }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button link @click="handleCancel">✕</el-button>
        </div>
      </div>
    </template>

    <div class="drawer-body">
      <section class="info-section">
        <div class="section-title">信息参数</div>
        <div class="info-tags">
          <span class="info-tag"><span class="tag-label">赛事：</span>{{ context.matchName || '—' }}</span>
          <span class="info-tag"><span class="tag-label">作者：</span>{{ context.authorName || '—' }}</span>
          <span class="info-tag">
            <span class="tag-label">方案分析类型：</span>{{ schemeTypesDisplay }}
          </span>
        </div>
        <el-row :gutter="12">
          <el-col :span="16">
            <label class="field-label">历史战绩</label>
            <el-input
              v-model="params.historyRecord"
              placeholder="请输入该作者近10场历史战绩，如：7胜2平1负"
              maxlength="500"
            />
          </el-col>
          <el-col :span="8">
            <label class="field-label">主播风格</label>
            <DictSelect
              v-model="params.anchorStyle"
              dict-type="dict_anchor_style"
              placeholder="请选择"
              clearable
            />
          </el-col>
        </el-row>
        <div class="field-block">
          <label class="field-label">产品定义说明</label>
          <el-input
            v-model="params.productDescription"
            type="textarea"
            :rows="3"
            placeholder="请描述产品的定位、目标用户、核心卖点等"
            maxlength="1000"
            show-word-limit
          />
        </div>
      </section>

      <section v-if="showPrefPanel" class="pref-panel" :class="{ collapsed: prefCollapsed }">
        <div class="pref-header" @click="prefCollapsed = !prefCollapsed">
          <span>📋 历史对话偏好总结</span>
          <div class="pref-actions">
            <el-button link size="small" @click.stop="prefCollapsed = !prefCollapsed">
              {{ prefCollapsed ? '展开' : '收起' }}
            </el-button>
            <el-button link size="small" @click.stop="closePrefPanel">✕</el-button>
          </div>
        </div>
        <div v-show="!prefCollapsed" class="pref-body">
          <div v-if="dimensionTags.length" class="pref-dimensions">
            <span v-for="tag in dimensionTags" :key="tag.key" class="pref-dim-tag">
              {{ tag.label }}：{{ tag.value }}
            </span>
          </div>
          <el-input
            v-model="preferenceSummary"
            type="textarea"
            :rows="3"
            placeholder="可编辑偏好总结，将在本次对话中生效"
            @input="preferenceEdited = true"
          />
        </div>
      </section>

      <section ref="chatRef" class="chat-section">
        <div v-if="!conversationHistory.length && !generating" class="chat-empty">
          输入您的想法或要求，点击发送开始生成赛事方案
        </div>
        <div
          v-for="(msg, index) in conversationHistory"
          :key="index"
          class="chat-message"
          :class="msg.role"
        >
          <div class="chat-avatar" :class="msg.role">
            <el-icon v-if="msg.role === 'assistant'"><Cpu /></el-icon>
            <el-icon v-else><UserFilled /></el-icon>
          </div>
          <div class="chat-bubble">
            <div
              v-if="msg.role === 'assistant'"
              class="bubble-content markdown-body"
              v-html="renderMarkdown(msg.content)"
            />
            <div v-else class="bubble-content">{{ msg.content }}</div>
          </div>
        </div>
        <div v-if="generating" class="chat-message assistant typing">
          <div class="chat-avatar assistant">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="chat-bubble">
            <div class="bubble-content">
              <span class="typing-dots"><i /><i /><i /></span> 正在生成…
            </div>
          </div>
        </div>
      </section>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          placeholder="输入修改意见或生成要求，Enter 发送，Shift+Enter 换行"
          :disabled="generating"
          @keydown.enter="onEnterKey"
        />
        <div class="footer-actions">
          <el-button :disabled="generating || !inputMessage.trim()" type="primary" @click="handleSend">
            发送
          </el-button>
          <el-button :disabled="!latestAssistantContent" type="success" @click="handleAdopt('PAID')">
            写入付费内容
          </el-button>
          <el-button :disabled="!latestAssistantContent" type="success" plain @click="handleAdopt('FREE')">
            写入免费内容
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Cpu, UserFilled } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import DictSelect from '@/components/DictSelect.vue'
import {
  adoptAiContent,
  fetchAiContentModels,
  fetchAiConversationHistory,
  fetchAiPreferenceSummary,
  generateAiContent,
  generateAiPreferenceSummary,
  saveAiConversationHistory,
  updateAiPreferenceSummary,
  type AiContentModel,
} from '@/api/aiContent'
import { fetchAiModelList, type AiModelConfigVO } from '@/api/config'
import {
  buildPendingSnapshot,
  DEFAULT_AI_INPUT_MESSAGE,
  type PendingAiConversation,
  useAiContentSession,
} from '@/composables/useAiContentSession'
import { fetchDictData } from '@/api/dict'

export interface AiContentDrawerContext {
  matchName: string
  authorName: string
  schemeTypes: string[]
  contentId?: number
  authorId?: number
}

const props = defineProps<{
  modelValue: boolean
  context: AiContentDrawerContext
  /** 新建内容时由编辑面板传入的暂存对话 */
  pendingConversation?: PendingAiConversation | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  adopted: [payload: { content: string; target: 'PAID' | 'FREE'; isMock?: boolean }]
  cancelled: []
  pendingBuffered: [snapshot: PendingAiConversation | null]
}>()

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

const schemeTypeLabels = ref<Record<string, string>>({})

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const {
  sessionId,
  roundCount,
  conversationHistory,
  latestAssistantContent,
  preferenceSummary,
  preferenceDimensions,
  resetSession,
  resetPreference,
  appendUserMessage,
  appendAssistantMessage,
  buildContext,
  restoreConversation,
} = useAiContentSession()

const modelOptions = ref<AiContentModel[]>([])
const selectedModel = ref<number | null>(null)
const generating = ref(false)
const inputMessage = ref('')
const chatRef = ref<HTMLElement | null>(null)
const showPrefPanel = ref(false)
const prefCollapsed = ref(false)
const preferenceEdited = ref(false)
let prefAutoCollapseTimer: ReturnType<typeof setTimeout> | null = null
let skipPersistOnClose = false

const DIMENSION_LABELS: Record<string, string> = {
  language_style: '语言风格',
  prediction_direction: '预测方向',
  length_preference: '篇幅偏好',
  modification_tendency: '修改倾向',
}

const dimensionTags = computed(() => {
  return Object.entries(preferenceDimensions.value)
    .filter(([, dim]) => dim?.value && dim.value !== '未设定')
    .map(([key, dim]) => ({
      key,
      label: DIMENSION_LABELS[key] || key,
      value: dim.value,
    }))
})

const params = ref({
  historyRecord: '',
  anchorStyle: 'comprehensive',
  productDescription: '',
})

const currentModel = computed(() => modelOptions.value.find((m) => m.id === selectedModel.value))

const MODEL_TYPE_ICONS: Record<string, string> = {
  QWEN: '🔵',
  DEEPSEEK: '🟢',
  GLM: '🟣',
  MOONSHOT: '🟠',
  KIMI: '🟠',
}

function mapModelRow(row: AiModelConfigVO): AiContentModel {
  const type = (row.modelType || '').toUpperCase()
  return {
    id: row.id,
    name: row.modelName,
    icon: MODEL_TYPE_ICONS[type] || '🤖',
    status: row.connStatus === 'CONNECTED' ? 'available' : 'unavailable',
    description: row.remark || row.modelType,
    isDefault: row.isDefault,
  }
}

const schemeTypesDisplay = computed(() => {
  const types = props.context.schemeTypes || []
  if (!types.length) return '—'
  return types.map((st) => schemeTypeLabels.value[st] || st).join('、')
})

function renderMarkdown(content: string) {
  return md.render(content || '')
}

async function loadSchemeTypeLabels() {
  try {
    const res: any = await fetchDictData('dict_scheme_type')
    const map: Record<string, string> = {}
    for (const item of res?.list || []) {
      map[String(item.value)] = item.label
    }
    schemeTypeLabels.value = map
  } catch {
    schemeTypeLabels.value = {}
  }
}

async function loadModels() {
  try {
    const res = await fetchAiModelList({ status: 'ENABLED', pageNum: 1, pageSize: 100 })
    const rows = (res?.list || []).map(mapModelRow)
    if (rows.length) {
      modelOptions.value = rows
      const defaultModel = rows.find((m) => m.isDefault) || rows[0]
      selectedModel.value = defaultModel.id
      return
    }
    const fallback = await fetchAiContentModels()
    modelOptions.value = fallback?.models || []
    const defaultModel = modelOptions.value.find((m) => m.isDefault) || modelOptions.value[0]
    if (defaultModel) {
      selectedModel.value = defaultModel.id
    }
  } catch {
    modelOptions.value = []
    selectedModel.value = null
  }
}

async function loadPreference() {
  try {
    const res = await fetchAiPreferenceSummary(props.context.authorId)
    if (res?.summaryText) {
      preferenceSummary.value = res.summaryText
      preferenceDimensions.value = (res.dimensions || {}) as Record<string, { value: string }>
      showPrefPanel.value = true
      prefCollapsed.value = false
      preferenceEdited.value = false
      if (prefAutoCollapseTimer) clearTimeout(prefAutoCollapseTimer)
      prefAutoCollapseTimer = setTimeout(() => {
        prefCollapsed.value = true
      }, 5000)
    }
  } catch {
    // P0：无偏好时静默
  }
}

async function loadConversation() {
  // 新建内容尚无 contentId，不加载 author/global 历史，保持空对话
  if (!props.context.contentId) {
    return
  }
  try {
    const res = await fetchAiConversationHistory({
      contentId: props.context.contentId,
    })
    if (res?.conversationHistory?.length) {
      restoreConversation(res.conversationHistory, res.roundCount)
      scrollChatToBottom()
    }
  } catch {
    // P0：无历史对话时静默
  }
}

function buildPersistContext() {
  return buildContext({
    matchName: props.context.matchName,
    authorName: props.context.authorName,
    schemeTypes: [...props.context.schemeTypes],
    historyRecord: params.value.historyRecord,
    anchorStyle: params.value.anchorStyle,
    productDescription: params.value.productDescription,
  })
}

function buildPendingConversationSnapshot(): PendingAiConversation | null {
  return buildPendingSnapshot({
    sessionId: sessionId.value,
    conversationHistory: conversationHistory.value,
    roundCount: roundCount.value,
    preferenceSummary: preferenceSummary.value || undefined,
    preferenceDimensions: preferenceDimensions.value,
    persistContext: buildPersistContext(),
  })
}

function persistPreferenceFromConversation(contentId?: number) {
  if (!conversationHistory.value.length) {
    if (preferenceEdited.value && preferenceSummary.value.trim()) {
      updateAiPreferenceSummary({
        summaryText: preferenceSummary.value.trim(),
        dimensions: preferenceDimensions.value,
      }).catch(() => {})
    }
    if (!contentId) {
      emit('pendingBuffered', null)
    }
    return
  }
  // 已保存内容：立即持久化到 content:{id}
  if (contentId) {
    saveAiConversationHistory({
      sessionId: sessionId.value,
      conversationHistory: [...conversationHistory.value],
      authorId: props.context.authorId,
      contentId,
    }).catch(() => {})
    generateAiPreferenceSummary({
      sessionId: sessionId.value,
      conversationHistory: [...conversationHistory.value],
      context: buildPersistContext(),
      authorId: props.context.authorId,
      contentId,
      preferenceSummary: preferenceSummary.value || undefined,
    }).catch(() => {})
    emit('pendingBuffered', null)
    return
  }
  // 新建内容尚无 contentId：暂存到编辑面板，待保存后再写入后端
  emit('pendingBuffered', buildPendingConversationSnapshot())
}

function closePrefPanel() {
  showPrefPanel.value = false
  if (prefAutoCollapseTimer) {
    clearTimeout(prefAutoCollapseTimer)
    prefAutoCollapseTimer = null
  }
}

function scrollChatToBottom() {
  nextTick(() => {
    if (chatRef.value) {
      chatRef.value.scrollTop = chatRef.value.scrollHeight
    }
  })
}

async function handleSend() {
  const message = inputMessage.value.trim()
  if (!message || generating.value) return
  if (selectedModel.value == null) {
    ElMessage.warning('请先选择 AI 模型')
    return
  }
  if (roundCount.value >= 10) {
    ElMessage.warning('对话轮次已达上限（10/10），请采纳当前方案或关闭后重新开始')
    return
  }

  const userMessage = message
  const historyForApi = [...conversationHistory.value]
  appendUserMessage(userMessage)
  inputMessage.value = ''
  scrollChatToBottom()
  await nextTick()
  generating.value = true
  try {
    const res = await generateAiContent({
      sessionId: sessionId.value,
      modelId: selectedModel.value,
      message: userMessage,
      context: buildContext({
        matchName: props.context.matchName,
        authorName: props.context.authorName,
        schemeTypes: [...props.context.schemeTypes],
        historyRecord: params.value.historyRecord,
        anchorStyle: params.value.anchorStyle,
        productDescription: params.value.productDescription,
      }),
      preferenceSummary: preferenceSummary.value || '',
      conversationHistory: historyForApi,
      roundCount: roundCount.value + 1,
    })
    appendAssistantMessage(res.content || '')
    if (res.mock) {
      ElMessage.info(res.message || '占位生成完成')
    }
    scrollChatToBottom()
  } catch {
    inputMessage.value = userMessage
    ElMessage.error('AI 生成失败，请重试')
  } finally {
    generating.value = false
  }
}

function onEnterKey(event: Event) {
  const keyEvent = event as KeyboardEvent
  if (keyEvent.shiftKey) return
  keyEvent.preventDefault()
  handleSend()
}

async function handleAdopt(target: 'PAID' | 'FREE') {
  if (!latestAssistantContent.value) {
    ElMessage.warning('暂无可采纳的 AI 方案')
    return
  }
  try {
    await adoptAiContent({
      sessionId: sessionId.value,
      contentId: props.context.contentId,
      content: latestAssistantContent.value,
      modelId: selectedModel.value!,
      schemeTypes: [...props.context.schemeTypes],
      roundCount: roundCount.value,
    })
    skipPersistOnClose = true
    persistPreferenceFromConversation(props.context.contentId)
    emit('adopted', { content: latestAssistantContent.value, target })
    visible.value = false
    ElMessage.success(target === 'PAID' ? '已写入付费内容' : '已写入免费内容')
  } catch {
    ElMessage.error('采纳记录失败')
  }
}

function handleModelChange(modelId: number) {
  if (modelId === selectedModel.value) return
  selectedModel.value = modelId
  resetSession()
  conversationHistory.value = []
  latestAssistantContent.value = ''
  ElMessage.info('已切换模型，对话已重置')
}

function handleCancel() {
  skipPersistOnClose = true
  if (props.context.contentId) {
    // 编辑已有内容：取消仅更新偏好总结，不写入对话（与改前一致）
    if (conversationHistory.value.length) {
      generateAiPreferenceSummary({
        sessionId: sessionId.value,
        conversationHistory: [...conversationHistory.value],
        context: buildPersistContext(),
        authorId: props.context.authorId,
        preferenceSummary: preferenceSummary.value || undefined,
      }).catch(() => {})
    } else if (preferenceEdited.value && preferenceSummary.value.trim()) {
      updateAiPreferenceSummary({
        summaryText: preferenceSummary.value.trim(),
        dimensions: preferenceDimensions.value,
      }).catch(() => {})
    }
  } else {
    persistPreferenceFromConversation(undefined)
  }
  visible.value = false
  emit('cancelled')
}

function handleClosed() {
  if (!skipPersistOnClose) {
    persistPreferenceFromConversation(props.context.contentId)
  }
  skipPersistOnClose = false
  preferenceEdited.value = false
  closePrefPanel()
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    preferenceEdited.value = false
    skipPersistOnClose = false
    params.value = { historyRecord: '', anchorStyle: 'comprehensive', productDescription: '' }
    inputMessage.value = DEFAULT_AI_INPUT_MESSAGE

    const hasContentId = !!props.context.contentId
    const pending = props.pendingConversation

    if (hasContentId) {
      resetSession()
      resetPreference()
    } else if (pending?.conversationHistory?.length) {
      sessionId.value = pending.sessionId
      restoreConversation(pending.conversationHistory, pending.roundCount)
      if (pending.preferenceSummary) {
        preferenceSummary.value = pending.preferenceSummary
        preferenceDimensions.value = pending.preferenceDimensions || {}
        showPrefPanel.value = true
        prefCollapsed.value = false
      } else {
        resetPreference()
      }
    } else {
      resetSession()
      resetPreference()
    }

    const loaders: Promise<void>[] = [loadModels(), loadSchemeTypeLabels()]
    if (hasContentId) {
      loaders.push(loadPreference(), loadConversation())
    } else if (!pending?.conversationHistory?.length) {
      loaders.push(loadPreference())
    }
    await Promise.all(loaders)
    if (pending?.conversationHistory?.length) {
      scrollChatToBottom()
    }
  },
)
</script>

<style scoped lang="scss">
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.drawer-title {
  font-size: 18px;
  font-weight: 600;
}
.drawer-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.model-select {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
}
.model-icon {
  font-size: 16px;
}
.model-name {
  font-weight: 500;
}
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 180px);
  overflow: hidden;
}
.info-section {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 14px 16px;
  flex-shrink: 0;
}
.section-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}
.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.info-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #e6f7ff;
  border-radius: 4px;
  font-size: 12px;
  color: #1890ff;
}
.tag-label {
  color: #69b1ff;
}
.field-label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}
.field-block {
  margin-top: 10px;
}
.pref-panel {
  background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%);
  border: 1px solid #ffe58f;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}
.pref-panel.collapsed .pref-body {
  display: none;
}
.pref-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #874d00;
  cursor: pointer;
}
.pref-body {
  padding: 0 14px 14px;
}
.pref-dimensions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.pref-dim-tag {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 4px;
  font-size: 11px;
  color: #874d00;
}
.chat-section {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chat-empty {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 40px 20px;
}
.chat-message {
  display: flex;
  gap: 10px;
  max-width: 85%;
  animation: chat-msg-in 0.3s ease-out;
}
.chat-message.assistant {
  align-self: flex-start;
}
.chat-message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.chat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}
.chat-avatar.assistant {
  background: #e6f7ff;
  color: var(--el-color-primary);
}
.chat-avatar.user {
  background: var(--el-color-primary);
  color: #fff;
}
.chat-bubble {
  min-width: 0;
}
.chat-message.assistant .bubble-content {
  background: #f5f5f5;
  color: var(--el-text-color-primary);
  border-radius: 12px 12px 12px 4px;
}
.chat-message.user .bubble-content {
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 12px 12px 4px 12px;
}
.bubble-content {
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}
@keyframes chat-msg-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 8px 0;
}
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ddd;
  padding: 6px 8px;
}
.typing-dots {
  display: inline-flex;
  gap: 4px;
  vertical-align: middle;
}
.typing-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  display: inline-block;
  animation: typing 1.2s infinite ease-in-out;
}
.typing-dots i:nth-child(2) { animation-delay: 0.2s; }
.typing-dots i:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}
.drawer-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
:deep(.el-dropdown-menu__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
