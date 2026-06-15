<template>

  <div class="rich-text-editor" :class="{ 'is-disabled': disabled, 'is-readonly': readonly }">

    <EditorToolbar

      v-if="editor && !readonly && !disabled"

      :editor="editor"

      :image-uploading="imageUploading"

      @pick-image="triggerImagePick"

      @set-image-width="setImageWidth"

    />

    <input

      ref="imageInputRef"

      type="file"

      class="rte-image-input"

      :accept="IMAGE_ACCEPT"

      @change="onImageFileChange"

    />



    <editor-content v-if="editor" :editor="editor" class="rte-content" />

  </div>

</template>



<script setup lang="ts">

import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

import { ElMessage } from 'element-plus'

import { EditorContent, useEditor } from '@tiptap/vue-3'

import StarterKit from '@tiptap/starter-kit'

import Placeholder from '@tiptap/extension-placeholder'

import TextAlign from '@tiptap/extension-text-align'

import Underline from '@tiptap/extension-underline'
import Color from '@tiptap/extension-color'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'

import { ResizableImage } from './ResizableImage'

import { StyledParagraph } from './StyledParagraph'

import { InlineStyleMark } from './InlineStyleMark'

import { WechatTextStyle } from './WechatTextStyle'

import { WechatParagraphStyle } from './WechatParagraphStyle'

import { WechatHighlight } from './WechatHighlight'

import { StyledSection } from './StyledSection'

import EditorToolbar from './EditorToolbar.vue'

import { IMAGE_ACCEPT, uploadContentImage, validateImageFile } from '@/api/file'

import { appendFileAuth, appendFileAuthToHtml, stripFileAuthFromHtml } from '@/utils/fileUrl'

import { ensureLayoutArticleHtml } from '@/utils/layoutSync'

import { normalizeWechatPasteHtml, sanitizeWechatExportHtml } from '@/utils/wechatHtml'



const props = withDefaults(

  defineProps<{

    modelValue?: string

    placeholder?: string

    disabled?: boolean

    readonly?: boolean

    minHeight?: string

  }>(),

  {

    modelValue: '',

    placeholder: '请输入内容…',

    disabled: false,

    readonly: false,

    minHeight: '240px',

  },

)



const emit = defineEmits<{

  'update:modelValue': [value: string]

}>()



const imageInputRef = ref<HTMLInputElement | null>(null)

const imageUploading = ref(false)

const suppressModelEcho = ref(false)

/** Compare editor HTML with v-model without layout-article wrapper / auth token drift. */
function normalizeHtmlForSync(html: string): string {
  let normalized = sanitizeWechatExportHtml(stripFileAuthFromHtml(html || ''))
  normalized = normalized
    .replace(/^<section[^>]*class=["'][^"']*layout-article[^"']*["'][^>]*>/i, '')
    .replace(/<\/section>\s*$/i, '')
    .trim()
  return normalized
}



const editor = useEditor({

  content: appendFileAuthToHtml(props.modelValue || '<p></p>'),

  editable: !props.disabled && !props.readonly,

  extensions: [

    StarterKit.configure({

      heading: { levels: [2, 3, 4] },

      paragraph: false,

    }),

    StyledParagraph,

    StyledSection,

    Placeholder.configure({ placeholder: props.placeholder }),

    TextAlign.configure({ types: ['heading', 'paragraph', 'styledSection'] }),

    Underline,

    WechatTextStyle,

    Color,

    WechatHighlight.configure({ multicolor: true }),

    WechatParagraphStyle,

    InlineStyleMark,

    Table.configure({ resizable: true }),

    TableRow,

    TableHeader,

    TableCell,

    ResizableImage.configure({ inline: false, allowBase64: false }),

  ],

  editorProps: {

    transformPastedHTML(html) {

      return normalizeWechatPasteHtml(html)

    },

  },

  onUpdate: ({ editor: ed }) => {
    if (suppressModelEcho.value) return
    emit('update:modelValue', ensureLayoutArticleHtml(sanitizeWechatExportHtml(ed.getHTML())))
  },

})



function setImageWidth(width: string) {

  const normalizedWidth = width.includes('%') || width.includes('px') ? width : `${width}px`
  const style = `width:${normalizedWidth};max-width:100%;height:auto;`
  editor.value?.chain().focus().updateAttributes('image', { width: normalizedWidth, style, dataW: normalizedWidth }).run()

}



function triggerImagePick() {

  imageInputRef.value?.click()

}



async function onImageFileChange(event: Event) {

  const input = event.target as HTMLInputElement

  const file = input.files?.[0]

  input.value = ''

  if (!file || !editor.value) return



  const validationError = validateImageFile(file)

  if (validationError) {

    ElMessage.warning(validationError)

    return

  }



  imageUploading.value = true

  try {

    const uploaded = await uploadContentImage(file)

    const displayUrl = appendFileAuth(uploaded.url)

    editor.value.chain().focus().setImage({ src: displayUrl, alt: uploaded.name }).updateAttributes('image', { width: '100%', style: 'width:100%;max-width:100%;height:auto;', dataW: '100%' }).run()

  } catch (err: unknown) {

    const message = err instanceof Error ? err.message : '图片上传失败'

    ElMessage.error(message)

  } finally {

    imageUploading.value = false

  }

}



watch(
  () => props.modelValue,
  (val) => {
    if (!editor.value) return
    const next = appendFileAuthToHtml(val || '<p></p>')
    const current = editor.value.getHTML()
    if (normalizeHtmlForSync(current) === normalizeHtmlForSync(next)) return
    suppressModelEcho.value = true
    editor.value.commands.setContent(next, { emitUpdate: false })
    nextTick(() => {
      suppressModelEcho.value = false
    })
  },
)



watch(

  () => [props.disabled, props.readonly] as const,

  ([disabled, readonly]) => {

    editor.value?.setEditable(!disabled && !readonly)

  },

)



onBeforeUnmount(() => {

  editor.value?.destroy()

})

</script>



<style scoped>

.rich-text-editor {

  border: 1px solid var(--el-border-color);

  border-radius: 8px;

  overflow: hidden;

  background: #fff;

}



.rte-image-input {

  display: none;

}



.rte-content :deep(.ProseMirror) {

  min-height: v-bind(minHeight);

  padding: 12px 16px;

  outline: none;

  line-height: 1.75;

  font-size: 15px;

}



.rte-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {

  color: var(--el-text-color-placeholder);

  content: attr(data-placeholder);

  float: left;

  height: 0;

  pointer-events: none;

}



.rte-content :deep(.ProseMirror h2) {

  font-size: 1.35em;

  font-weight: 600;

  margin: 0.75em 0 0.35em;

}



.rte-content :deep(.ProseMirror h3) {

  font-size: 1.15em;

  font-weight: 600;

  margin: 0.65em 0 0.3em;

}



.rte-content :deep(.ProseMirror blockquote) {

  border-left: 4px solid var(--el-border-color);

  margin: 12px 0;

  padding: 8px 16px;

  color: var(--el-text-color-secondary);

  background: var(--el-fill-color-light);

}



.rte-content :deep(.ProseMirror hr) {

  border: none;

  border-top: 1px solid var(--el-border-color);

  margin: 16px 0;

}



.rte-content :deep(.ProseMirror pre) {

  background: #2d2d2d;

  color: #f8f8f2;

  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;

  font-size: 13px;

  padding: 12px 16px;

  border-radius: 6px;

  margin: 12px 0;

  overflow-x: auto;

}



.rte-content :deep(.ProseMirror pre code) {

  background: none;

  color: inherit;

  padding: 0;

}



.rte-content :deep(.ProseMirror table) {

  border-collapse: collapse;

  width: 100%;

  margin: 12px 0;

}



.rte-content :deep(.ProseMirror th),

.rte-content :deep(.ProseMirror td) {

  border: 1px solid var(--el-border-color);

  padding: 6px 10px;

  min-width: 60px;

}



.rte-content :deep(.ProseMirror th) {

  background: var(--el-fill-color-light);

  font-weight: 600;

}



.rte-content :deep(.ProseMirror section) {

  margin: 10px 0;

}



.rte-content :deep(.ProseMirror img) {

  display: block;

  max-width: 100%;

  height: auto;

  margin: 12px 0;

  border-radius: 4px;

}



.rich-text-editor.is-disabled .rte-content,

.rich-text-editor.is-readonly .rte-content {

  background: var(--el-fill-color-lighter);

}

</style>


