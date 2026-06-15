/**
 * Client-side layout schema ↔ HTML sync (mirrors LayoutSchemaHelper / LayoutJsonHelper).
 * ADR-021: best-effort bidirectional sync; complex inline styles may not round-trip perfectly.
 */
import type { LayoutBlock, LayoutDocument, LayoutSchema, LayoutSchemaBlock } from '@/types/layoutTemplate'
import { emptyLayoutDocument, emptyLayoutSchema } from '@/types/layoutTemplate'
import { stripFileAuthFromUrl } from '@/utils/fileUrl'

const PLACEHOLDER_HEADING = '标题样式'
const PLACEHOLDER_PARAGRAPH = '正文段落样式'
const PLACEHOLDER_QUOTE = '引用样式'
const PLACEHOLDER_LIST = '列表样式'
const PLACEHOLDER_IMAGE = '图片框'
const PLACEHOLDER_FIXED = '装饰区块'

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function escapeAttr(text: string): string {
  return escapeHtml(text)
}

function camelToKebab(key: string): string {
  return key.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase()
}

function inlineStyle(styles?: Record<string, string>): string {
  if (!styles || !Object.keys(styles).length) return ''
  const parts = Object.entries(styles).map(([k, v]) => `${camelToKebab(k)}:${escapeAttr(v)};`)
  return ` style="${parts.join('')}"`
}

function resolveStyles(globalStyles: Record<string, Record<string, string>>, styleRef?: string) {
  if (!styleRef) return {}
  return globalStyles[styleRef] || {}
}

function renderInlineChildren(children?: Array<{ text: string; bold?: boolean; italic?: boolean }>): string {
  if (!children?.length) return ''
  return children
    .map((child) => {
      let text = escapeHtml(child.text || '')
      if (child.bold) text = `<strong>${text}</strong>`
      if (child.italic) text = `<em>${text}</em>`
      return text
    })
    .join('')
}

function renderBlock(block: LayoutBlock, skeletonMode = false): string {
  const styleAttr = inlineStyle(block.styles)
  switch (block.type) {
    case 'heading': {
      const level = Math.min(6, Math.max(1, block.level || 2))
      const text = skeletonMode && !block.text ? PLACEHOLDER_HEADING : block.text || ''
      return `<h${level}${styleAttr}>${escapeHtml(text)}</h${level}>`
    }
    case 'paragraph': {
      const align = block.align || 'left'
      const style = styleAttr
        ? styleAttr.replace('style="', `style="text-align:${escapeAttr(align)};`)
        : ` style="text-align:${escapeAttr(align)}"`
      return `<p${style}>${renderInlineChildren(block.children)}</p>`
    }
    case 'image': {
      const src = escapeAttr(block.src || '')
      const width = escapeAttr(block.width || '100%')
      if (skeletonMode && !block.src) {
        return `<p${styleAttr}><span class="layout-placeholder">${PLACEHOLDER_IMAGE}</span></p>`
      }
      return `<p${styleAttr}><img src="${src}" style="width:${width};max-width:100%;height:auto;" alt=""/></p>`
    }
    case 'quote': {
      const text = skeletonMode && !block.text ? PLACEHOLDER_QUOTE : block.text || ''
      return `<blockquote${styleAttr}>${escapeHtml(text)}</blockquote>`
    }
    case 'divider':
      return `<hr${styleAttr}/>`
    case 'fixed':
      return `<div class="layout-fixed"${styleAttr}>${PLACEHOLDER_FIXED}</div>`
    case 'list': {
      const tag = block.ordered ? 'ol' : 'ul'
      const items = (block.items || []).map((item) => `<li>${escapeHtml(item)}</li>`).join('')
      return `<${tag}${styleAttr}>${items}</${tag}>`
    }
    default:
      return ''
  }
}

import { sanitizeWechatExportHtml } from '@/utils/wechatHtml'

/** Wrap editor HTML in layout-article section for WeChat publish pipeline. */
export function ensureLayoutArticleHtml(html: string): string {
  const cleaned = sanitizeWechatExportHtml(html || '')
  if (!cleaned) return '<section class="layout-article"><p></p></section>'
  const trimmed = cleaned.trim()
  if (/^<section[^>]*class=["'][^"']*layout-article/i.test(trimmed)) {
    return trimmed
  }
  return `<section class="layout-article">${trimmed}</section>`
}

function parseImageWidthFromSegment(seg: string): string {
  const widthAttr = seg.match(/\bwidth=["']([^"']+)["']/i)?.[1]
  if (widthAttr) return widthAttr

  const dataW = seg.match(/\bdata-w=["']([^"']+)["']/i)?.[1]
  if (dataW) return dataW

  const styleAttr = seg.match(/\bstyle=["']([^"']*)["']/i)?.[1] || ''
  const widthStyle = styleAttr.match(/(?:^|;)\s*width\s*:\s*([^;]+)/i)?.[1]
  if (widthStyle) return widthStyle.trim()

  const maxWidthStyle = styleAttr.match(/(?:^|;)\s*max-width\s*:\s*([^;]+)/i)?.[1]
  if (maxWidthStyle) return maxWidthStyle.trim()

  return '100%'
}

/** Merge width into img inline style (mirrors ResizableImage.mergeImageStyle). */
function mergeImageInlineStyle(width: string, existingStyle?: string): string {
  const withoutWidth = (existingStyle || '')
    .replace(/(?:^|;)\s*width\s*:\s*[^;]*/gi, '')
    .replace(/(?:^|;)\s*max-width\s*:\s*[^;]*/gi, '')
    .replace(/^\s*;+\s*|\s*;+\s*$/g, '')
    .trim()

  const widthRule = `width:${width};max-width:100%;height:auto`
  return withoutWidth ? `${withoutWidth};${widthRule};` : `${widthRule};`
}

/**
 * Ensure each img has inline width from data-w / width attr / existing style.
 * Edit mode (TipTap NodeView) respects data-w even without style.width; LayoutViewer must too.
 */
export function ensureImageWidthStyles(html: string): string {
  if (!html) return html
  return html.replace(/<img\b[^>]*\/?>/gi, (tag) => {
    const width = parseImageWidthFromSegment(tag)
    const styleMatch = tag.match(/\bstyle=(["'])([\s\S]*?)\1/i)
    const merged = mergeImageInlineStyle(width, styleMatch?.[2])
    if (styleMatch) {
      return tag.replace(styleMatch[0], `style="${merged}"`)
    }
    return tag.replace(/^<img\b/i, `<img style="${merged}"`)
  })
}

export function sanitizeLayoutHtml(html: string): string {
  if (!html) return ''
  let out = html.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
  out = out.replace(/\s(on\w+)\s*=/gi, ' data-blocked-event="$1"=')
  out = out.replace(/javascript:/gi, '')
  out = out.replace(/(<img[^>]+src=["'])([^"']+)(["'])/gi, (_m, pre, src, post) => {
    return `${pre}${stripFileAuthFromUrl(src)}${post}`
  })
  return out.trim()
}

export function renderLayoutHtml(doc: LayoutDocument, skeletonMode = false): string {
  const blocks = doc.blocks || []
  const inner = blocks.map((b) => renderBlock(b, skeletonMode)).join('')
  return sanitizeLayoutHtml(`<section class="layout-article">${inner}</section>`)
}

function paragraphBlock(text: string): LayoutBlock {
  return {
    type: 'paragraph',
    align: 'left',
    children: [{ text: text || '', bold: false, italic: false }],
  }
}

function stripTags(html: string): string {
  return html
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function splitHtmlSegments(html: string): string[] {
  const segments: string[] = []
  let remaining = html
  const blockTag = /(<(h[1-6]|p|blockquote|hr|img|ul|ol)[^>]*>.*?<\/\2>|<\/?(hr|img)[^>]*\/?>)/gis
  while (remaining.trim()) {
    const match = blockTag.exec(remaining)
    if (match) {
      if (match.index > 0) {
        const before = remaining.substring(0, match.index).trim()
        if (stripTags(before)) segments.push(before)
      }
      segments.push(match[0])
      remaining = remaining.substring(match.index + match[0].length)
      blockTag.lastIndex = 0
    } else {
      if (stripTags(remaining)) segments.push(remaining.trim())
      break
    }
  }
  return segments
}

function segmentToBlock(seg: string): LayoutBlock | null {
  if (!seg.trim()) return null
  const lower = seg.toLowerCase()
  if (lower.startsWith('<hr')) return { type: 'divider' }
  const hMatch = seg.match(/<h([1-6])[^>]*>[\s\S]*<\/h\1>/i)
  if (hMatch) {
    return { type: 'heading', level: parseInt(hMatch[1], 10), text: stripTags(seg) }
  }
  if (lower.includes('<blockquote')) {
    return { type: 'quote', text: stripTags(seg) }
  }
  if (lower.includes('<img')) {
    const srcMatch = seg.match(/src=["']([^"']+)["']/i)
    return {
      type: 'image',
      src: stripFileAuthFromUrl(srcMatch?.[1] || ''),
      width: parseImageWidthFromSegment(seg),
    }
  }
  if (lower.includes('<ul') || lower.includes('<ol')) {
    const items = [...seg.matchAll(/<li[^>]*>([\s\S]*?)<\/li>/gi)].map((m) => stripTags(m[1]))
    return { type: 'list', ordered: lower.includes('<ol'), items }
  }
  return paragraphBlock(stripTags(seg))
}

export function parseHtmlToLayoutDocument(html: string): LayoutDocument {
  const cleaned = sanitizeLayoutHtml(html)
  if (!cleaned) return emptyLayoutDocument()
  const inner = cleaned.replace(/^<section[^>]*>/i, '').replace(/<\/section>$/i, '')
  const segments = splitHtmlSegments(inner)
  const blocks: LayoutBlock[] = []
  for (const seg of segments) {
    const block = segmentToBlock(seg)
    if (block) blocks.push(block)
  }
  if (!blocks.length) blocks.push(paragraphBlock(stripTags(cleaned)))
  return { version: 1, blocks }
}

function slotBlock(
  slotKind: string,
  styleRef: string,
  ordered: boolean,
  repeat: boolean
): LayoutSchemaBlock {
  const slot: LayoutSchemaBlock = { type: 'slot', slotKind: slotKind as LayoutSchemaBlock['slotKind'], styleRef }
  if (slotKind === 'list') slot.ordered = ordered
  if (repeat) slot.repeat = true
  return slot
}

export function extractLayoutSchemaFromDocument(doc: LayoutDocument, base?: LayoutSchema): LayoutSchema {
  const schema = base ? JSON.parse(JSON.stringify(base)) : emptyLayoutSchema()
  const schemaBlocks: LayoutSchemaBlock[] = []
  let hasParagraphRepeat = false
  for (const block of doc.blocks || []) {
    let schemaBlock: LayoutSchemaBlock | null = null
    switch (block.type) {
      case 'heading':
        schemaBlock = {
          type: 'heading',
          level: block.level || 2,
          styleRef: `heading${block.level || 2}`,
          slotKind: 'heading',
        }
        break
      case 'quote':
        schemaBlock = slotBlock('quote', 'quote', false, false)
        break
      case 'divider':
        schemaBlock = { type: 'divider', styleRef: 'divider' }
        break
      case 'image':
        schemaBlock = { type: 'frame', slotKind: 'image', styleRef: 'image', optional: true }
        break
      case 'list':
        schemaBlock = slotBlock('list', 'list', true, false)
        break
      case 'paragraph':
        if (!hasParagraphRepeat) {
          hasParagraphRepeat = true
          schemaBlock = slotBlock('paragraph', 'paragraph', true, true)
        }
        break
    }
    if (schemaBlock) schemaBlocks.push(schemaBlock)
  }
  if (!schemaBlocks.length) schemaBlocks.push(slotBlock('paragraph', 'paragraph', true, true))
  schema.blocks = schemaBlocks
  return schema
}

export function htmlToLayoutSchema(html: string, existing?: LayoutSchema): LayoutSchema {
  const doc = parseHtmlToLayoutDocument(html)
  return extractLayoutSchemaFromDocument(doc, existing)
}

function appendSlotPreview(
  out: LayoutBlock[],
  block: LayoutSchemaBlock,
  styles: Record<string, string>
) {
  const slotKind = block.slotKind || 'paragraph'
  const repeat = block.repeat || false
  const count = repeat ? 2 : 1
  for (let i = 0; i < count; i++) {
    switch (slotKind) {
      case 'heading':
        out.push({ type: 'heading', level: block.level || 3, text: PLACEHOLDER_HEADING, styles })
        break
      case 'quote':
        out.push({ type: 'quote', text: PLACEHOLDER_QUOTE, styles })
        break
      case 'list':
        out.push({
          type: 'list',
          ordered: block.ordered || false,
          items: [PLACEHOLDER_LIST, PLACEHOLDER_LIST],
          styles,
        })
        break
      case 'paragraph':
        out.push({
          type: 'paragraph',
          align: block.align || 'left',
          styles,
          children: [{ text: PLACEHOLDER_PARAGRAPH, bold: false, italic: false }],
        })
        break
    }
  }
}

function appendPreviewBlock(
  out: LayoutBlock[],
  block: LayoutSchemaBlock,
  globalStyles: Record<string, Record<string, string>>
) {
  const styleRef = block.styleRef || 'paragraph'
  const styles = resolveStyles(globalStyles, styleRef)
  switch (block.type) {
    case 'heading':
      out.push({ type: 'heading', level: block.level || 2, text: PLACEHOLDER_HEADING, styles })
      break
    case 'slot':
      appendSlotPreview(out, block, styles)
      break
    case 'divider':
      out.push({ type: 'divider', styles })
      break
    case 'frame':
      out.push({ type: 'image', src: '', width: '100%', styles })
      break
    case 'fixed':
      out.push({ type: 'quote', text: PLACEHOLDER_FIXED, styles })
      break
    case 'section':
      for (const child of block.children || []) {
        appendPreviewBlock(out, child, globalStyles)
      }
      break
  }
}

export function schemaToPreviewDocument(schema: LayoutSchema): LayoutDocument {
  const out: LayoutBlock[] = []
  const globalStyles = schema.globalStyles || {}
  for (const block of schema.blocks || []) {
    appendPreviewBlock(out, block, globalStyles)
  }
  return { version: 2, blocks: out }
}

export function schemaToPreviewHtml(schema: LayoutSchema): string {
  const doc = schemaToPreviewDocument(schema)
  return renderLayoutHtml(doc, true)
}

/** Plain text extract for search / AI (strips tags, preserves paragraph breaks). */
export function extractPlainText(html: string): string {
  if (!html) return ''
  const cleaned = sanitizeLayoutHtml(html)
  const inner = cleaned.replace(/^<section[^>]*>/i, '').replace(/<\/section>$/i, '')
  const segments = splitHtmlSegments(inner)
  if (!segments.length) return stripTags(cleaned)
  return segments.map((seg) => stripTags(seg)).filter(Boolean).join('\n\n')
}

/** Wrap plain text as simple HTML paragraphs for rich editor bootstrap. */
export function plainTextToHtml(text: string): string {
  if (!text?.trim()) return '<p></p>'
  if (/<[a-z][\s\S]*>/i.test(text)) return sanitizeLayoutHtml(text)
  const paragraphs = text.split(/\n\n+/).map((p) => p.trim()).filter(Boolean)
  if (!paragraphs.length) return '<p></p>'
  return paragraphs.map((p) => `<p>${escapeHtml(p.replace(/\n/g, ' '))}</p>`).join('')
}
