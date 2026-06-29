import { describe, expect, it } from 'vitest'
import {
  extractBodyInnerHtml,
  mergeWechatQuickTypeset,
  resolveWechatQuickTheme,
  splitBodyBlocks,
  WECHAT_QUICK_STYLES,
  WECHAT_QUICK_THEME_COLORS,
} from './wechatQuickTypeset'

function stripHtmlText(html: string): string {
  return html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

describe('wechatQuickTypeset', () => {
  const sampleBody = '<p>第一段正文。</p><p>第二段正文。</p>'

  it('exposes 3 styles and theme palette', () => {
    expect(WECHAT_QUICK_STYLES).toHaveLength(3)
    expect(WECHAT_QUICK_THEME_COLORS.length).toBeGreaterThanOrEqual(6)
  })

  it('strips layout-article wrapper when splitting body', () => {
    const wrapped = `<section class="layout-article">${sampleBody}</section>`
    expect(extractBodyInnerHtml(wrapped)).toBe(sampleBody)
    expect(splitBodyBlocks(wrapped)).toHaveLength(2)
  })

  it('merges title, author and body for classic style', () => {
    const html = mergeWechatQuickTypeset({
      title: '测试标题',
      author: '张三',
      bodyHtml: sampleBody,
      styleId: 'classic',
      themeColorId: 'wechat-green',
    })
    expect(html).toContain('layout-article')
    expect(html).toContain('测试标题')
    expect(html).toContain('张三')
    expect(html).toContain('第一段正文')
    expect(html).toContain('#07c160')
  })

  it('merges card style with account card area', () => {
    const html = mergeWechatQuickTypeset({
      title: '卡片标题',
      author: '运营号',
      bodyHtml: sampleBody,
      styleId: 'card',
      themeColorId: 'brand-blue',
    })
    expect(html).toContain('卡片标题')
    expect(html).toContain('运营号')
    expect(html).toContain('#1890ff')
    expect(html).toContain('欢迎关注')
  })

  it('merges sectioned style with lead quote on first paragraph', () => {
    const html = mergeWechatQuickTypeset({
      title: '分节标题',
      author: '李四',
      bodyHtml: sampleBody,
      styleId: 'sectioned',
      themeColorId: 'warm-orange',
    })
    expect(html).toContain('分节标题')
    expect(html).toContain('第一段正文')
    expect(html).toContain('#fa8c16')
  })

  it('preserves body plain text across merge (ADR-020)', () => {
    const bodyPlainBefore = stripHtmlText(sampleBody)
    const merged = mergeWechatQuickTypeset({
      title: '标题',
      author: '作者',
      bodyHtml: sampleBody,
      styleId: 'classic',
      themeColorId: 'wechat-green',
    })
    const bodyPlainAfter = stripHtmlText(extractBodyInnerHtml(merged))
    expect(bodyPlainAfter).toContain('第一段正文')
    expect(bodyPlainAfter).toContain('第二段正文')
    expect(bodyPlainAfter).toContain(bodyPlainBefore)
  })

  it('falls back to default theme for unknown color id', () => {
    const theme = resolveWechatQuickTheme('unknown-color')
    expect(theme.id).toBe('wechat-green')
  })

  const imgSrc = 'https://cdn.example.com/article-photo.jpg'
  const bodyWithImg = `<p>导语段落。</p><p><img src="${imgSrc}" style="width:100%;max-width:100%;height:auto;"></p><p>结尾段落。</p>`
  const standaloneImgBody = `<p>文字前。</p><img src="${imgSrc}" alt="配图"><p>文字后。</p>`
  const figureBody = `<p>正文。</p><figure><img src="${imgSrc}"><figcaption>图注</figcaption></figure>`

  it('splitBodyBlocks keeps img-only paragraphs', () => {
    const blocks = splitBodyBlocks(bodyWithImg)
    expect(blocks).toHaveLength(3)
    expect(blocks.some((b) => b.includes(imgSrc))).toBe(true)
  })

  it('preserves standalone img tags across merge', () => {
    const merged = mergeWechatQuickTypeset({
      title: '标题',
      author: '作者',
      bodyHtml: standaloneImgBody,
      styleId: 'classic',
      themeColorId: 'wechat-green',
    })
    expect(merged).toContain(imgSrc)
    expect(merged).toMatch(/<img[^>]+src=["']https:\/\/cdn\.example\.com\/article-photo\.jpg["']/)
  })

  it('preserves images in all quick typeset styles', () => {
    for (const styleId of ['classic', 'card', 'sectioned'] as const) {
      const merged = mergeWechatQuickTypeset({
        title: '标题',
        author: '作者',
        bodyHtml: bodyWithImg,
        styleId,
        themeColorId: 'wechat-green',
      })
      expect(merged, styleId).toContain(imgSrc)
      expect(merged, styleId).toContain('导语段落')
      expect(merged, styleId).toContain('结尾段落')
    }
  })

  it('preserves figure blocks with img', () => {
    const merged = mergeWechatQuickTypeset({
      title: '标题',
      author: '作者',
      bodyHtml: figureBody,
      styleId: 'classic',
      themeColorId: 'wechat-green',
    })
    expect(merged).toContain(imgSrc)
    expect(merged).toContain('图注')
  })

  it('preserves inline img inside text paragraphs', () => {
    const mixed = `<p>前文<img src="${imgSrc}" style="width:80%;">后文</p>`
    const merged = mergeWechatQuickTypeset({
      title: '标题',
      author: '作者',
      bodyHtml: mixed,
      styleId: 'classic',
      themeColorId: 'wechat-green',
    })
    expect(merged).toContain(imgSrc)
    expect(merged).toContain('前文')
    expect(merged).toContain('后文')
  })
})
