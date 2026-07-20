import MarkdownIt from 'markdown-it'
import { sanitizeLayoutHtml } from './layoutSync'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

const TABLE_SEPARATOR_RE = /^\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/

function isTableSeparator(line: string): boolean {
  return TABLE_SEPARATOR_RE.test(line.trim())
}

function parseTableRow(line: string): string[] {
  return line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((cell) => cell.trim())
}

function convertGfmTableToHtml(tableMd: string): string {
  const lines = tableMd.trim().split('\n').filter((line) => line.trim())
  if (lines.length < 2 || !isTableSeparator(lines[1])) return tableMd

  const header = parseTableRow(lines[0])
  const bodyLines = lines.slice(2)
  const renderCell = (cell: string) => md.renderInline(cell.trim())

  let html = '<table><thead><tr>'
  for (const cell of header) {
    html += `<th>${renderCell(cell)}</th>`
  }
  html += '</tr></thead><tbody>'
  for (const line of bodyLines) {
    const cells = parseTableRow(line)
    html += '<tr>'
    for (const cell of cells) {
      html += `<td>${renderCell(cell)}</td>`
    }
    html += '</tr>'
  }
  html += '</tbody></table>'
  return html
}

function splitMarkdownWithTables(markdown: string): Array<{ type: 'md' | 'table'; content: string }> {
  const lines = markdown.split('\n')
  const segments: Array<{ type: 'md' | 'table'; content: string }> = []
  let mdBuffer: string[] = []

  const flushMd = () => {
    if (mdBuffer.length) {
      segments.push({ type: 'md', content: mdBuffer.join('\n') })
      mdBuffer = []
    }
  }

  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    const next = lines[i + 1]
    if (line?.includes('|') && next && isTableSeparator(next)) {
      flushMd()
      const tableLines = [line, next]
      i += 2
      while (i < lines.length && lines[i].includes('|')) {
        tableLines.push(lines[i])
        i++
      }
      segments.push({ type: 'table', content: tableLines.join('\n') })
    } else {
      mdBuffer.push(line)
      i++
    }
  }
  flushMd()
  return segments
}

/** Convert AI markdown output to HTML for TipTap rich editor. */
export function markdownToHtml(markdown: string): string {
  if (!markdown?.trim()) return '<p></p>'
  if (/<[a-z][\s\S]*>/i.test(markdown)) return sanitizeLayoutHtml(markdown)

  const segments = splitMarkdownWithTables(markdown)
  let html = segments
    .map((seg) => (seg.type === 'table' ? convertGfmTableToHtml(seg.content) : md.render(seg.content)))
    .join('')

  // TipTap StarterKit headings: h2–h4 only
  html = html.replace(/<h1(\s|>)/gi, '<h2$1').replace(/<\/h1>/gi, '</h2>')

  return sanitizeLayoutHtml(html.trim() || '<p></p>')
}
