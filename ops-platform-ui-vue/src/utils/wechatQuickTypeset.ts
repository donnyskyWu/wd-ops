/**
 * WeChat 公众号一键排版 — 3 套前端静态模板 + 主题色（Phase 1）
 * 输出 layout_html，遵循 section/span + inline style（WeChat 编辑器兼容子集）
 */
import { sanitizeWechatExportHtml } from '@/utils/wechatHtml'

export type WechatQuickStyleId = 'classic' | 'card' | 'sectioned'

export interface WechatQuickThemeColor {
  id: string
  name: string
  primary: string
  light: string
  dark: string
}

export interface WechatQuickStyleMeta {
  id: WechatQuickStyleId
  name: string
  description: string
}

export interface WechatQuickTypesetInput {
  title: string
  author?: string
  bodyHtml: string
  styleId: WechatQuickStyleId
  themeColorId: string
}

export const WECHAT_QUICK_STYLES: WechatQuickStyleMeta[] = [
  { id: 'classic', name: '经典导读', description: '标题区 + 作者行 + 正文，适合日常长文' },
  { id: 'card', name: '卡片极简', description: '顶部账号卡片 + 标题，清爽极简风' },
  { id: 'sectioned', name: '分节强调', description: '导语引用 + 彩色小节标题 + 正文块' },
]

export const WECHAT_QUICK_THEME_COLORS: WechatQuickThemeColor[] = [
  { id: 'wechat-green', name: '微信绿', primary: '#07c160', light: '#e8f8ef', dark: '#059148' },
  { id: 'brand-blue', name: '品牌蓝', primary: '#1890ff', light: '#e6f4ff', dark: '#096dd9' },
  { id: 'warm-orange', name: '活力橙', primary: '#fa8c16', light: '#fff7e6', dark: '#d46b08' },
  { id: 'elegant-purple', name: '优雅紫', primary: '#722ed1', light: '#f9f0ff', dark: '#531dab' },
  { id: 'classic-red', name: '经典红', primary: '#f5222d', light: '#fff1f0', dark: '#cf1322' },
  { id: 'ink-gray', name: '墨色', primary: '#434343', light: '#f5f5f5', dark: '#262626' },
]

const DEFAULT_THEME = WECHAT_QUICK_THEME_COLORS[0]

const BLOCK_TAG_RE =
  /(<(?:h[1-6]|p|blockquote|ul|ol|li|hr|figure|video|section|div)[^>]*>[\s\S]*?<\/(?:h[1-6]|p|blockquote|ul|ol|li|figure|video|section|div)>|<hr[^>]*\/?>|<(?:img|video)[^>]*\/?>)/gi

const MEDIA_TAG_RE = /<(img|video|figure|iframe|audio)\b/i

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function stripTags(html: string): string {
  return html
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

/** Keep blocks with text or embedded media (img-only paragraphs must not be dropped). */
function hasBlockContent(html: string): boolean {
  if (stripTags(html)) return true
  return MEDIA_TAG_RE.test(html)
}

export function resolveWechatQuickTheme(themeColorId: string): WechatQuickThemeColor {
  return WECHAT_QUICK_THEME_COLORS.find((c) => c.id === themeColorId) || DEFAULT_THEME
}

/** Strip outer layout-article wrapper; keep inner editor blocks. */
export function extractBodyInnerHtml(html: string): string {
  const cleaned = sanitizeWechatExportHtml(html || '')
  if (!cleaned) return ''
  const match = cleaned.match(/^<section[^>]*class=["'][^"']*layout-article[^"']*["'][^>]*>([\s\S]*)<\/section>$/i)
  return (match?.[1] || cleaned).trim()
}

/** Split body HTML into top-level block segments (preserves inline markup inside blocks). */
export function splitBodyBlocks(html: string): string[] {
  const inner = extractBodyInnerHtml(html)
  if (!inner) return []

  const segments: string[] = []
  let remaining = inner
  BLOCK_TAG_RE.lastIndex = 0

  while (remaining.trim()) {
    BLOCK_TAG_RE.lastIndex = 0
    const match = BLOCK_TAG_RE.exec(remaining)
    if (match) {
      if (match.index > 0) {
        const before = remaining.substring(0, match.index).trim()
        if (hasBlockContent(before)) segments.push(wrapOrPreserveBlock(before))
      }
      segments.push(match[0].trim())
      remaining = remaining.substring(match.index + match[0].length)
    } else {
      const tail = remaining.trim()
      if (hasBlockContent(tail)) segments.push(wrapOrPreserveBlock(tail))
      break
    }
  }
  return segments.filter(hasBlockContent)
}

/** Preserve media markup; wrap plain text runs as paragraphs. */
function wrapOrPreserveBlock(html: string): string {
  const trimmed = html.trim()
  if (!trimmed) return ''
  if (MEDIA_TAG_RE.test(trimmed) && !stripTags(trimmed)) return trimmed
  return wrapPlainTextAsParagraph(trimmed)
}

function wrapPlainTextAsParagraph(text: string): string {
  const plain = stripTags(text)
  if (!plain) return ''
  return `<p>${escapeHtml(plain)}</p>`
}

function authorInitial(author?: string): string {
  const name = (author || '作').trim()
  return escapeHtml(name.charAt(0) || '作')
}

function renderClassicBody(blocks: string[], theme: WechatQuickThemeColor): string {
  return blocks
    .map((block) => {
      const lower = block.toLowerCase()
      if (lower.startsWith('<h')) {
        return `<section style="margin:20px 0 12px;"><span style="font-size:18px;font-weight:bold;color:${theme.dark};line-height:1.4;">${stripInnerAndReescape(block)}</span></section>`
      }
      if (lower.startsWith('<blockquote')) {
        return `<section style="margin:16px 0;padding:12px 16px;background:${theme.light};border-left:4px solid ${theme.primary};"><span style="font-size:15px;color:#666;line-height:1.7;">${stripInnerAndReescape(block)}</span></section>`
      }
      if (MEDIA_TAG_RE.test(block)) {
        return `<section style="margin:16px 0;text-align:center;">${preserveMediaBlock(block)}</section>`
      }
      if (lower.startsWith('<ul') || lower.startsWith('<ol')) {
        return `<section style="margin:12px 0;font-size:16px;color:#333;line-height:1.75;">${block}</section>`
      }
      return `<section style="margin:0 0 16px;"><span style="font-size:16px;color:#333;line-height:1.75;">${stripInnerAndReescape(block)}</span></section>`
    })
    .join('')
}

function renderCardBody(blocks: string[], theme: WechatQuickThemeColor): string {
  return blocks
    .map((block) => {
      const lower = block.toLowerCase()
      if (MEDIA_TAG_RE.test(block)) {
        return `<section style="margin:16px 0;border-radius:6px;overflow:hidden;">${preserveMediaBlock(block)}</section>`
      }
      return `<section style="margin:0 0 14px;padding:0 4px;"><span style="font-size:16px;color:#444;line-height:1.8;">${stripInnerAndReescape(block)}</span></section>`
    })
    .join('')
}

function renderSectionedBody(blocks: string[], theme: WechatQuickThemeColor): string {
  let sectionIndex = 0
  return blocks
    .map((block, index) => {
      const lower = block.toLowerCase()
      if (index === 0 && (lower.startsWith('<p') || !lower.startsWith('<h'))) {
        return `<section style="margin:0 0 20px;padding:14px 16px;background:${theme.light};border-left:4px solid ${theme.primary};"><span style="font-size:15px;color:#555;line-height:1.7;font-style:italic;">${stripInnerAndReescape(block)}</span></section>`
      }
      if (lower.startsWith('<h')) {
        sectionIndex += 1
        return `<section style="margin:24px 0 10px;padding:6px 12px;background:${theme.primary};border-radius:4px;"><span style="font-size:16px;font-weight:bold;color:#ffffff;line-height:1.4;">${stripInnerAndReescape(block)}</span></section>`
      }
      if (lower.startsWith('<blockquote')) {
        return `<section style="margin:14px 0;padding:10px 14px;border:1px dashed ${theme.primary};background:#fafafa;"><span style="font-size:15px;color:#666;line-height:1.65;">${stripInnerAndReescape(block)}</span></section>`
      }
      if (MEDIA_TAG_RE.test(block)) {
        return `<section style="margin:16px 0;">${preserveMediaBlock(block)}</section>`
      }
      sectionIndex += 1
      const showMiniHeader = sectionIndex % 3 === 0
      const miniHeader = showMiniHeader
        ? `<section style="margin:18px 0 8px;"><span style="padding:2px 10px;font-size:13px;color:${theme.primary};border-bottom:2px solid ${theme.primary};">要点 ${Math.floor(sectionIndex / 3)}</span></section>`
        : ''
      return `${miniHeader}<section style="margin:0 0 14px;"><span style="font-size:16px;color:#333;line-height:1.75;">${stripInnerAndReescape(block)}</span></section>`
    })
    .join('')
}

/** Extract inner HTML from a block tag, preserving inline formatting where possible. */
function stripInnerAndReescape(block: string): string {
  const innerMatch = block.match(/^<([a-z0-9]+)[^>]*>([\s\S]*)<\/\1>$/i)
  if (innerMatch?.[2]) {
    const inner = innerMatch[2].trim()
    if (inner) return inner
    if (MEDIA_TAG_RE.test(block)) return innerMatch[0]
    return escapeHtml(stripTags(block))
  }
  if (MEDIA_TAG_RE.test(block)) return block
  return escapeHtml(stripTags(block))
}

/** Unwrap single wrapper (p/section/div) around media while keeping the tag itself intact. */
function preserveMediaBlock(block: string): string {
  const trimmed = block.trim()
  const wrapped = trimmed.match(/^<(p|section|div)[^>]*>([\s\S]*)<\/\1>$/i)
  if (wrapped?.[2] && MEDIA_TAG_RE.test(wrapped[2]) && !stripTags(wrapped[2])) {
    return wrapped[2].trim()
  }
  return trimmed
}

function renderClassicTemplate(input: WechatQuickTypesetInput, theme: WechatQuickThemeColor, blocks: string[]): string {
  const title = escapeHtml(input.title.trim() || '请输入标题')
  const author = escapeHtml((input.author || '作者').trim())
  const body = renderClassicBody(blocks.length ? blocks : [wrapPlainTextAsParagraph('在此输入正文…')], theme)
  return `<section class="layout-article"><section style="margin-bottom:24px;padding-bottom:16px;border-bottom:2px solid ${theme.primary};"><section style="margin-bottom:10px;"><span style="font-size:22px;font-weight:bold;color:#1a1a1a;line-height:1.45;">${title}</span></section><section><span style="font-size:14px;color:#888888;">${author}</span></section></section>${body}</section>`
}

function renderCardTemplate(input: WechatQuickTypesetInput, theme: WechatQuickThemeColor, blocks: string[]): string {
  const title = escapeHtml(input.title.trim() || '请输入标题')
  const author = escapeHtml((input.author || '公众号').trim())
  const initial = authorInitial(input.author)
  const body = renderCardBody(blocks.length ? blocks : [wrapPlainTextAsParagraph('在此输入正文…')], theme)
  return `<section class="layout-article"><section style="margin-bottom:20px;padding:16px;background:${theme.light};border-radius:8px;border:1px solid ${theme.primary};"><section style="text-align:center;"><section style="width:52px;height:52px;margin:0 auto 10px;border-radius:26px;background:${theme.primary};line-height:52px;text-align:center;"><span style="font-size:20px;color:#ffffff;font-weight:bold;">${initial}</span></section><section style="margin-bottom:4px;"><span style="font-size:15px;font-weight:bold;color:#333333;">${author}</span></section><section><span style="font-size:12px;color:#999999;">欢迎关注 · 分享有价值的内容</span></section></section></section><section style="margin-bottom:18px;"><span style="font-size:20px;font-weight:bold;color:#1a1a1a;line-height:1.45;">${title}</span></section>${body}</section>`
}

function renderSectionedTemplate(input: WechatQuickTypesetInput, theme: WechatQuickThemeColor, blocks: string[]): string {
  const title = escapeHtml(input.title.trim() || '请输入标题')
  const author = escapeHtml((input.author || '作者').trim())
  const body = renderSectionedBody(blocks.length ? blocks : [wrapPlainTextAsParagraph('在此输入正文…')], theme)
  return `<section class="layout-article"><section style="margin-bottom:20px;text-align:center;"><section style="padding:4px 16px;margin-bottom:12px;background:${theme.light};border-radius:20px;"><span style="font-size:12px;color:${theme.primary};letter-spacing:2px;">SPECIAL</span></section><section style="margin-bottom:8px;"><span style="font-size:22px;font-weight:bold;color:${theme.dark};line-height:1.45;">${title}</span></section><section><span style="font-size:13px;color:#999999;">${author}</span></section><section style="margin-top:16px;height:3px;background:${theme.primary};"></section></section>${body}</section>`
}

/** Merge title / author / body into selected WeChat layout template. */
export function mergeWechatQuickTypeset(input: WechatQuickTypesetInput): string {
  const theme = resolveWechatQuickTheme(input.themeColorId)
  const blocks = splitBodyBlocks(input.bodyHtml)

  switch (input.styleId) {
    case 'card':
      return sanitizeWechatExportHtml(renderCardTemplate(input, theme, blocks))
    case 'sectioned':
      return sanitizeWechatExportHtml(renderSectionedTemplate(input, theme, blocks))
    case 'classic':
    default:
      return sanitizeWechatExportHtml(renderClassicTemplate(input, theme, blocks))
  }
}