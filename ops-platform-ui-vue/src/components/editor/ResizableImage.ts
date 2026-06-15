import Image from '@tiptap/extension-image'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import ImageResizeNodeView from './ImageResizeNodeView.vue'

function parseWidthFromElement(element: HTMLElement): string | null {
  const attrWidth = element.getAttribute('width')
  if (attrWidth) return attrWidth.endsWith('%') || attrWidth.endsWith('px') ? attrWidth : `${attrWidth}px`

  const dataW = element.getAttribute('data-w')
  if (dataW) return dataW.endsWith('%') || dataW.endsWith('px') ? dataW : `${dataW}px`

  const style = element.getAttribute('style') || ''
  const widthMatch = style.match(/(?:^|;)\s*width\s*:\s*([^;]+)/i)
  if (widthMatch) return widthMatch[1].trim()

  const maxWidthMatch = style.match(/(?:^|;)\s*max-width\s*:\s*([^;]+)/i)
  if (maxWidthMatch) return maxWidthMatch[1].trim()

  return null
}

/** Width attr is SSOT; strip any stale width rule from parsed style before merge. */
function mergeImageStyle(width: string | null, existingStyle?: string | null): string {
  const withoutWidth = (existingStyle || '')
    .replace(/(?:^|;)\s*width\s*:\s*[^;]*/gi, '')
    .replace(/(?:^|;)\s*max-width\s*:\s*[^;]*/gi, '')
    .replace(/^\s*;+\s*|\s*;+\s*$/g, '')
    .trim()

  if (width) {
    const widthRule = `width:${width};max-width:100%;height:auto`
    return withoutWidth ? `${withoutWidth};${widthRule};` : `${widthRule};`
  }

  const fallback = 'max-width:100%;height:auto'
  return withoutWidth ? `${withoutWidth};${fallback};` : `${fallback};`
}

/** TipTap image node with inline width for WeChat-compatible HTML export. */
export const ResizableImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      width: {
        default: null,
        parseHTML: (element) => parseWidthFromElement(element as HTMLElement),
        renderHTML: () => ({}),
      },
      style: {
        default: null,
        parseHTML: (element) => element.getAttribute('style'),
        renderHTML: () => ({}),
      },
      class: {
        default: null,
        parseHTML: (element) => element.getAttribute('class'),
        renderHTML: (attributes) => {
          if (!attributes.class) return {}
          return { class: attributes.class }
        },
      },
      dataSrc: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-src'),
        renderHTML: (attributes) => {
          if (!attributes.dataSrc) return {}
          return { 'data-src': attributes.dataSrc }
        },
      },
      dataRatio: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-ratio'),
        renderHTML: (attributes) => {
          if (!attributes.dataRatio) return {}
          return { 'data-ratio': attributes.dataRatio }
        },
      },
      dataW: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-w'),
        renderHTML: (attributes) => {
          if (!attributes.dataW) return {}
          return { 'data-w': attributes.dataW }
        },
      },
      dataType: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-type'),
        renderHTML: (attributes) => {
          if (!attributes.dataType) return {}
          return { 'data-type': attributes.dataType }
        },
      },
    }
  },
  renderHTML({ HTMLAttributes }) {
    const width = HTMLAttributes.width as string | null
    const style = mergeImageStyle(width, HTMLAttributes.style as string | null)
    const { width: _w, style: _style, ...rest } = HTMLAttributes
    const attrs: Record<string, string> = { ...rest, style }
    if (width) {
      attrs['data-w'] = width
    }
    return ['img', attrs]
  },
  addNodeView() {
    return VueNodeViewRenderer(ImageResizeNodeView)
  },
})
