import { describe, expect, it } from 'vitest'
import { markdownToHtml } from './markdownHtml'

describe('markdownToHtml', () => {
  const sample = `## 赛事分析

**主队**近期状态出色，胜率 *65%*。

### 预测要点

- 看好主队不败
- 关注角球数
- 建议 **2-1** 比分

> 以上分析仅供参考

| 指标 | 主队 | 客队 |
| --- | --- | --- |
| 胜率 | 65% | 40% |
| 进球 | 2.1 | 1.3 |

详见 [官方数据](https://example.com/stats)`

  it('converts markdown structure to HTML for rich editor', () => {
    const html = markdownToHtml(sample)

    expect(html).toContain('<h2>赛事分析</h2>')
    expect(html).toContain('<h3>预测要点</h3>')
    expect(html).toContain('<strong>主队</strong>')
    expect(html).toContain('<em>65%</em>')
    expect(html).toContain('<ul>')
    expect(html).toContain('<li>看好主队不败</li>')
    expect(html).toContain('<blockquote>')
    expect(html).toContain('<table>')
    expect(html).toContain('<th>指标</th>')
    expect(html).toContain('<td>65%</td>')
    expect(html).toContain('href="https://example.com/stats"')
    expect(html).not.toContain('## ')
    expect(html).not.toContain('**主队**')
  })

  it('returns empty paragraph for blank input', () => {
    expect(markdownToHtml('')).toBe('<p></p>')
    expect(markdownToHtml('   ')).toBe('<p></p>')
  })

  it('passes through existing HTML', () => {
    const html = '<p>已有 <strong>HTML</strong></p>'
    expect(markdownToHtml(html)).toBe(html)
  })
})
