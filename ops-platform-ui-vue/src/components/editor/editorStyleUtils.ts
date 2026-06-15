/** Merge a single CSS property into an inline style string. */
export function mergeStyleProp(style: string | null | undefined, prop: string, value: string | null): string {
  const rules = new Map<string, string>()
  for (const part of (style || '').split(';')) {
    const trimmed = part.trim()
    if (!trimmed) continue
    const colon = trimmed.indexOf(':')
    if (colon <= 0) continue
    rules.set(trimmed.slice(0, colon).trim().toLowerCase(), trimmed.slice(colon + 1).trim())
  }
  if (value) {
    rules.set(prop.toLowerCase(), value)
  } else {
    rules.delete(prop.toLowerCase())
  }
  return Array.from(rules.entries())
    .map(([k, v]) => `${k}:${v}`)
    .join(';')
}

export function parseStyleProp(style: string | null | undefined, prop: string): string | null {
  if (!style) return null
  const re = new RegExp(`(?:^|;)\\s*${prop}\\s*:\\s*([^;]+)`, 'i')
  const match = style.match(re)
  return match?.[1]?.trim() ?? null
}

export function parsePx(value: string | null): number {
  if (!value) return 0
  const num = parseFloat(value)
  return Number.isFinite(num) ? num : 0
}
