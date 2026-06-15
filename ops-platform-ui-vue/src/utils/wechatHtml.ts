/**
 * WeChat Official Account editor HTML helpers.
 * WeChat mp editor uses UEditor chrome + ProseMirror body; normalize paste/export for TipTap.
 */
import { stripFileAuthFromUrl } from '@/utils/fileUrl'

const PROSEMIRROR_SEPARATOR_RE =
  /<img[^>]*class=["'][^"']*ProseMirror-separator[^"']*["'][^>]*>/gi
const PROSEMIRROR_TRAILING_BREAK_RE =
  /<br[^>]*class=["'][^"']*ProseMirror-trailingBreak[^"']*["'][^>]*\/?>/gi

/** Editor-only attributes that should not be stored or published. */
const EDITOR_ONLY_ATTR_RE =
  /\s(?:contenteditable|data-aistatus|data-imgqrcoded|translate|spellcheck)=["'][^"']*["']/gi

/** Unwrap WeChat leaf text spans while keeping styled wrappers. */
const LEAF_SPAN_RE = /<span[^>]*\bleaf(?:=["'][^"']*["'])?[^>]*>([\s\S]*?)<\/span>/gi

function sanitizeBaseHtml(html: string): string {
  if (!html) return ''
  let out = html.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
  out = out.replace(/\s(on\w+)\s*=/gi, ' data-blocked-event="$1"=')
  out = out.replace(/javascript:/gi, '')
  out = out.replace(/(<img[^>]+src=["'])([^"']+)(["'])/gi, (_m, pre, src, post) => {
    return `${pre}${stripFileAuthFromUrl(src)}${post}`
  })
  return out.trim()
}

function extractRichMediaBody(html: string): string {
  const richMedia = html.match(
    /class=["'][^"']*rich_media_content[^"']*["'][^>]*>([\s\S]*?)<\/div>\s*<\/div>\s*<\/div>/i,
  )
  if (richMedia?.[1]) {
    const prose = richMedia[1].match(
      /class=["'][^"']*ProseMirror[^"']*["'][^>]*>([\s\S]*?)<\/div>/i,
    )
    if (prose?.[1]) return prose[1]
    return richMedia[1]
  }
  return html
}

function stripEditorArtifacts(html: string): string {
  let out = html
  out = out.replace(PROSEMIRROR_SEPARATOR_RE, '')
  out = out.replace(PROSEMIRROR_TRAILING_BREAK_RE, '')
  out = out.replace(EDITOR_ONLY_ATTR_RE, '')
  let prev = ''
  while (prev !== out) {
    prev = out
    out = out.replace(LEAF_SPAN_RE, '$1')
  }
  return out
}

/** Normalize HTML pasted from WeChat mp editor (or other ProseMirror sources). */
export function normalizeWechatPasteHtml(html: string): string {
  if (!html?.trim()) return '<p></p>'
  let out = extractRichMediaBody(html)
  out = stripEditorArtifacts(out)
  // Keep <section> — parsed by StyledSection extension for layout blocks.
  return out.trim() || '<p></p>'
}

/** Prepare editor HTML for storage / WeChat publish pipeline. */
export function sanitizeWechatExportHtml(html: string): string {
  const cleaned = sanitizeBaseHtml(html || '')
  const normalized = stripEditorArtifacts(cleaned)
  if (!normalized) return ''
  return normalized.trim()
}
