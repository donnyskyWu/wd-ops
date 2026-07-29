import { describe, expect, it } from 'vitest'
import { emptyLayoutDocument } from '@/types/layoutTemplate'
import {
  combineContentHtml,
  isEmptyContentHtml,
  resolvePaidContentHtml,
} from './layoutSync'

const STYLED_LAYOUT_HTML =
  '<section class="layout-article"><table style="width:100%;border-collapse:collapse"><tr><td style="padding:8px;color:#333">蓝鹰 VS 红狮队</td></tr></table></section>'

describe('resolvePaidContentHtml', () => {
  it('prefers layoutHtml over layoutJson skeleton', () => {
    const html = resolvePaidContentHtml({
      bodyFormat: 'LAYOUT',
      layoutHtml: STYLED_LAYOUT_HTML,
      layoutJson: {
        version: 1,
        blocks: [{ type: 'paragraph', align: 'left', children: [{ text: 'plain' }] }],
      },
    })
    expect(html).toContain('style=')
    expect(html).toContain('<table')
  })

  it('prefers paidBody HTML over layoutJson when layoutHtml is absent', () => {
    const html = resolvePaidContentHtml({
      bodyFormat: 'LAYOUT',
      paidBody: STYLED_LAYOUT_HTML,
      layoutJson: {
        version: 1,
        blocks: [{ type: 'paragraph', align: 'left', children: [{ text: 'plain' }] }],
      },
    })
    expect(html).toContain('style=')
    expect(html).toContain('蓝鹰 VS 红狮队')
  })

  it('prefers layoutHtml over editorHtml (ADR-021 SSOT)', () => {
    const degradedEditor =
      '<section class="layout-article"><p>plain from layoutJson render</p></section>'
    const html = resolvePaidContentHtml({
      bodyFormat: 'LAYOUT',
      layoutHtml: STYLED_LAYOUT_HTML,
      editorHtml: degradedEditor,
    })
    expect(html).toBe(STYLED_LAYOUT_HTML)
    expect(html).toMatch(/style=/)
  })

  it('uses hydrated editorHtml when layoutHtml is absent', () => {
    const html = resolvePaidContentHtml({
      bodyFormat: 'LAYOUT',
      editorHtml: STYLED_LAYOUT_HTML,
      layoutJson: emptyLayoutDocument(),
      paidBody: 'plain fallback',
    })
    expect(html).toBe(STYLED_LAYOUT_HTML)
  })
})

describe('combineContentHtml', () => {
  it('keeps styled paid body for readonly merged view', () => {
    const merged = combineContentHtml('<p></p>', STYLED_LAYOUT_HTML)
    expect(merged).toContain('layout-article')
    expect(merged).toMatch(/style=/)
    expect(merged).toContain('<table')
  })

  it('does not treat styled table html as empty', () => {
    expect(isEmptyContentHtml(STYLED_LAYOUT_HTML)).toBe(false)
  })
})
