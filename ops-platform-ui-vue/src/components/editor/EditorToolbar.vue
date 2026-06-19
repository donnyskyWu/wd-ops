<template>
  <div class="rte-toolbar">
    <!-- History -->
    <el-button-group>
      <el-tooltip content="撤销 (Ctrl+Z)" placement="top">
        <el-button size="small" :disabled="!editor.can().undo()" @click="editor.chain().focus().undo().run()">
          ↶
        </el-button>
      </el-tooltip>
      <el-tooltip content="重做 (Ctrl+Y)" placement="top">
        <el-button size="small" :disabled="!editor.can().redo()" @click="editor.chain().focus().redo().run()">
          ↷
        </el-button>
      </el-tooltip>
    </el-button-group>

    <span class="rte-sep" />

    <!-- Clear / format -->
    <el-button-group>
      <el-tooltip content="清除格式" placement="top">
        <el-button size="small" @click="editor.chain().focus().clearFormat().run()">清除</el-button>
      </el-tooltip>
      <el-tooltip content="格式刷 (P1)" placement="top">
        <el-button size="small" disabled title="格式刷 — P1 待实现">刷</el-button>
      </el-tooltip>
    </el-button-group>

    <span class="rte-sep" />

    <!-- Font size -->
    <el-select
      :model-value="currentFontSize"
      size="small"
      class="rte-select"
      placeholder="字号"
      @change="onFontSizeChange"
    >
      <el-option v-for="s in WECHAT_FONT_SIZES" :key="s" :label="s" :value="s" />
    </el-select>

    <!-- Inline styles -->
    <el-button-group>
      <el-button size="small" :type="editor.isActive('bold') ? 'primary' : 'default'" @click="editor.chain().focus().toggleBold().run()">
        <strong>B</strong>
      </el-button>
      <el-button size="small" :type="editor.isActive('italic') ? 'primary' : 'default'" @click="editor.chain().focus().toggleItalic().run()">
        <em>I</em>
      </el-button>
      <el-button size="small" :type="editor.isActive('underline') ? 'primary' : 'default'" @click="editor.chain().focus().toggleUnderline().run()">
        <span class="rte-u">U</span>
      </el-button>
      <el-button size="small" :type="editor.isActive('strike') ? 'primary' : 'default'" @click="editor.chain().focus().toggleStrike().run()">
        <span class="rte-s">S</span>
      </el-button>
    </el-button-group>

    <!-- Colors -->
    <el-popover placement="bottom" :width="220" trigger="click">
      <template #reference>
        <el-button size="small" class="rte-color-btn">
          <span>A</span>
          <span class="rte-color-bar" :style="{ background: currentTextColor || '#000' }" />
        </el-button>
      </template>
      <div class="rte-color-grid">
        <button type="button" class="rte-color-swatch rte-color-clear" title="清除颜色" @click="setTextColor(null)">×</button>
        <button
          v-for="c in WECHAT_BASIC_COLORS"
          :key="'fg-' + c"
          type="button"
          class="rte-color-swatch"
          :style="{ background: c }"
          :title="c"
          @click="setTextColor(c)"
        />
      </div>
    </el-popover>

    <el-popover placement="bottom" :width="220" trigger="click">
      <template #reference>
        <el-button size="small" class="rte-color-btn">
          <span>▮</span>
          <span class="rte-color-bar" :style="{ background: currentBgColor || '#fff' }" />
        </el-button>
      </template>
      <div class="rte-color-grid">
        <button type="button" class="rte-color-swatch rte-color-clear" title="清除背景" @click="setBgColor(null)">×</button>
        <button
          v-for="c in WECHAT_BASIC_COLORS"
          :key="'bg-' + c"
          type="button"
          class="rte-color-swatch"
          :style="{ background: c }"
          :title="c"
          @click="setBgColor(c)"
        />
      </div>
    </el-popover>

    <span class="rte-sep" />

    <!-- Alignment -->
    <el-button-group>
      <el-button
        size="small"
        :type="editor.isActive({ textAlign: 'left' }) ? 'primary' : 'default'"
        @click="editor.chain().focus().setTextAlign('left').run()"
      >
        左
      </el-button>
      <el-button
        size="small"
        :type="editor.isActive({ textAlign: 'center' }) ? 'primary' : 'default'"
        @click="editor.chain().focus().setTextAlign('center').run()"
      >
        中
      </el-button>
      <el-button
        size="small"
        :type="editor.isActive({ textAlign: 'right' }) ? 'primary' : 'default'"
        @click="editor.chain().focus().setTextAlign('right').run()"
      >
        右
      </el-button>
      <el-button
        size="small"
        :type="editor.isActive({ textAlign: 'justify' }) ? 'primary' : 'default'"
        @click="editor.chain().focus().setTextAlign('justify').run()"
      >
        两端
      </el-button>
    </el-button-group>

    <!-- Indent -->
    <el-button-group>
      <el-tooltip content="增加首行缩进" placement="top">
        <el-button size="small" @click="editor.chain().focus().increaseTextIndent().run()">缩进+</el-button>
      </el-tooltip>
      <el-tooltip content="减少首行缩进" placement="top">
        <el-button size="small" @click="editor.chain().focus().decreaseTextIndent().run()">缩进-</el-button>
      </el-tooltip>
    </el-button-group>

    <el-dropdown trigger="click" @command="onJustifyIndent">
      <el-button size="small">两端缩进 ▾</el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="v in WECHAT_JUSTIFY_INDENT" :key="v" :command="v">
            {{ v === 0 ? '0（默认）' : `${v}px` }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <span class="rte-sep" />

    <!-- Spacing -->
    <el-dropdown trigger="click" @command="onLineHeight">
      <el-button size="small">行距 ▾</el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="h in WECHAT_LINE_HEIGHTS" :key="h" :command="h">
            {{ h }}{{ h === '1.6' ? '（默认）' : '' }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-dropdown trigger="click" @command="onMarginTop">
      <el-button size="small">段前 ▾</el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="v in WECHAT_PARAGRAPH_SPACING" :key="'mt-' + v" :command="v">
            {{ v === '0' ? '0（默认）' : v + 'px' }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-dropdown trigger="click" @command="onMarginBottom">
      <el-button size="small">段后 ▾</el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="v in WECHAT_PARAGRAPH_SPACING" :key="'mb-' + v" :command="v">
            {{ v === '0' ? '0' : v + 'px' }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-dropdown trigger="click" @command="onLetterSpacing">
      <el-button size="small">字距 ▾</el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="v in WECHAT_LETTER_SPACING" :key="v" :command="v">
            {{ v === '0' ? '0（默认）' : v }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <span class="rte-sep" />

    <!-- Lists & blocks -->
    <el-button-group>
      <el-button size="small" :type="editor.isActive('bulletList') ? 'primary' : 'default'" @click="editor.chain().focus().toggleBulletList().run()">
        • 列表
      </el-button>
      <el-button size="small" :type="editor.isActive('orderedList') ? 'primary' : 'default'" @click="editor.chain().focus().toggleOrderedList().run()">
        1. 列表
      </el-button>
    </el-button-group>

    <el-button size="small" @click="insertSection">版块</el-button>

    <el-popover placement="bottom" :width="200" trigger="click">
      <template #reference>
        <el-button size="small">表格 ▾</el-button>
      </template>
      <div class="rte-table-picker">
        <p class="rte-table-hint">{{ tablePick.cols }}列 × {{ tablePick.rows }}行</p>
        <div class="rte-table-grid">
          <button
            v-for="cell in 40"
            :key="cell"
            type="button"
            class="rte-table-cell"
            :class="{ active: isTableCellActive(cell) }"
            @mouseenter="onTableHover(cell)"
            @click="insertTable"
          />
        </div>
      </div>
    </el-popover>

    <el-button-group>
      <el-button size="small" :type="editor.isActive('blockquote') ? 'primary' : 'default'" @click="editor.chain().focus().toggleBlockquote().run()">
        引用
      </el-button>
      <el-button size="small" @click="editor.chain().focus().setHorizontalRule().run()">分割线</el-button>
      <el-button size="small" :type="editor.isActive('codeBlock') ? 'primary' : 'default'" @click="editor.chain().focus().toggleCodeBlock().run()">
        代码
      </el-button>
    </el-button-group>

    <el-popover placement="bottom" :width="280" trigger="click">
      <template #reference>
        <el-button size="small">😀 表情</el-button>
      </template>
      <div class="rte-emoji-grid">
        <button v-for="e in WECHAT_EMOJIS" :key="e" type="button" class="rte-emoji-btn" @click="insertEmoji(e)">
          {{ e }}
        </button>
      </div>
    </el-popover>

    <span class="rte-sep" />

    <!-- Image -->
    <el-button-group v-if="editor.isActive('image')">
      <el-button size="small" @click="emit('setImageWidth', '25%')">25%</el-button>
      <el-button size="small" @click="emit('setImageWidth', '50%')">50%</el-button>
      <el-button size="small" @click="emit('setImageWidth', '75%')">75%</el-button>
      <el-button size="small" @click="emit('setImageWidth', '100%')">100%</el-button>
    </el-button-group>
    <el-button size="small" :loading="imageUploading" @click="emit('pickImage')">插入图片</el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import {
  DEFAULT_SECTION_STYLE,
  WECHAT_BASIC_COLORS,
  WECHAT_EMOJIS,
  WECHAT_FONT_SIZES,
  WECHAT_JUSTIFY_INDENT,
  WECHAT_LETTER_SPACING,
  WECHAT_LINE_HEIGHTS,
  WECHAT_PARAGRAPH_SPACING,
} from './wechatEditorConfig'

const props = defineProps<{
  editor: Editor
  imageUploading?: boolean
}>()

const emit = defineEmits<{
  pickImage: []
  setImageWidth: [width: string]
}>()

const tablePick = reactive({ rows: 0, cols: 0 })

const currentFontSize = computed(() => {
  const attrs = props.editor.getAttributes('textStyle')
  return (attrs.fontSize as string) || '17px'
})

const currentTextColor = computed(() => props.editor.getAttributes('textStyle').color as string | undefined)

const currentBgColor = computed(() => props.editor.getAttributes('highlight').color as string | undefined)

function onFontSizeChange(size: string) {
  props.editor.chain().focus().setFontSize(size).run()
}

function setTextColor(color: string | null) {
  if (color) {
    props.editor.chain().focus().setColor(color).run()
  } else {
    props.editor.chain().focus().unsetColor().run()
  }
}

function setBgColor(color: string | null) {
  if (color) {
    props.editor.chain().focus().setHighlight({ color }).run()
  } else {
    props.editor.chain().focus().unsetHighlight().run()
  }
}

function onLineHeight(h: string) {
  props.editor.chain().focus().setParagraphStyleProp('line-height', h).run()
}

function onMarginTop(v: string) {
  props.editor.chain().focus().setParagraphStyleProp('margin-top', v === '0' ? null : `${v}px`).run()
}

function onMarginBottom(v: string) {
  props.editor.chain().focus().setParagraphStyleProp('margin-bottom', v === '0' ? null : `${v}px`).run()
}

function onLetterSpacing(v: string) {
  if (v === '0') {
    props.editor.chain().focus().unsetLetterSpacing().run()
  } else {
    props.editor.chain().focus().setLetterSpacing(v).run()
  }
}

function onJustifyIndent(px: number) {
  props.editor.chain().focus().setJustifyIndent(px).run()
}

function insertSection() {
  props.editor
    .chain()
    .focus()
    .insertContent({
      type: 'styledSection',
      attrs: { style: DEFAULT_SECTION_STYLE },
      content: [{ type: 'paragraph', content: [{ type: 'text', text: '在此输入版块内容…' }] }],
    })
    .run()
}

function onTableHover(index: number) {
  tablePick.rows = Math.ceil(index / 10)
  tablePick.cols = ((index - 1) % 10) + 1
}

function isTableCellActive(index: number) {
  const row = Math.ceil(index / 10)
  const col = ((index - 1) % 10) + 1
  return row <= tablePick.rows && col <= tablePick.cols
}

function insertTable() {
  const rows = tablePick.rows || 3
  const cols = tablePick.cols || 3
  props.editor.chain().focus().insertTable({ rows, cols, withHeaderRow: true }).run()
  tablePick.rows = 0
  tablePick.cols = 0
}

function insertEmoji(emoji: string) {
  props.editor.chain().focus().insertContent(emoji).run()
}
</script>

<style scoped>
.rte-toolbar {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.rte-sep {
  width: 1px;
  height: 20px;
  background: var(--el-border-color);
  margin: 0 2px;
}

.rte-select {
  width: 72px;
}

.rte-u {
  text-decoration: underline;
}

.rte-s {
  text-decoration: line-through;
}

.rte-color-btn {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  line-height: 1;
  padding-top: 4px;
  padding-bottom: 2px;
}

.rte-color-bar {
  display: block;
  width: 14px;
  height: 3px;
  margin-top: 2px;
  border: 1px solid var(--el-border-color);
}

.rte-color-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
}

.rte-color-swatch {
  width: 18px;
  height: 18px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 2px;
  cursor: pointer;
  padding: 0;
}

.rte-color-clear {
  background: #fff;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.rte-table-picker {
  padding: 4px;
}

.rte-table-hint {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.rte-table-grid {
  display: grid;
  grid-template-columns: repeat(10, 16px);
  gap: 2px;
}

.rte-table-cell {
  width: 16px;
  height: 16px;
  border: 1px solid var(--el-border-color-lighter);
  background: #fff;
  padding: 0;
  cursor: pointer;
}

.rte-table-cell.active {
  background: var(--el-color-primary-light-5);
  border-color: var(--el-color-primary);
}

.rte-emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 2px;
  max-height: 200px;
  overflow-y: auto;
}

.rte-emoji-btn {
  border: none;
  background: transparent;
  font-size: 18px;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
}

.rte-emoji-btn:hover {
  background: var(--el-fill-color);
}
</style>
