import { Extension } from '@tiptap/core'
import { mergeStyleProp, parsePx, parseStyleProp } from './editorStyleUtils'

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    wechatParagraphStyle: {
      setParagraphStyleProp: (prop: string, value: string | null) => ReturnType
      increaseTextIndent: () => ReturnType
      decreaseTextIndent: () => ReturnType
      setJustifyIndent: (px: number) => ReturnType
      clearFormat: () => ReturnType
    }
  }
}

function currentParagraphStyle(editor: { getAttributes: (name: string) => Record<string, unknown> }): string | null {
  const attrs = editor.getAttributes('paragraph')
  return (attrs.style as string | null) ?? null
}

/** Paragraph-level spacing / indent commands (WeChat rowspacing, lineheight, indent). */
export const WechatParagraphStyle = Extension.create({
  name: 'wechatParagraphStyle',
  addCommands() {
    return {
      setParagraphStyleProp:
        (prop: string, value: string | null) =>
        ({ chain, editor }) => {
          const style = mergeStyleProp(currentParagraphStyle(editor), prop, value)
          return chain().focus().updateAttributes('paragraph', { style: style || null }).run()
        },
      increaseTextIndent:
        () =>
        ({ chain, editor }) => {
          const current = parsePx(parseStyleProp(currentParagraphStyle(editor), 'text-indent'))
          const next = current + 32
          const style = mergeStyleProp(currentParagraphStyle(editor), 'text-indent', `${next}px`)
          return chain().focus().updateAttributes('paragraph', { style }).run()
        },
      decreaseTextIndent:
        () =>
        ({ chain, editor }) => {
          const current = parsePx(parseStyleProp(currentParagraphStyle(editor), 'text-indent'))
          const next = Math.max(0, current - 32)
          const style = mergeStyleProp(
            currentParagraphStyle(editor),
            'text-indent',
            next > 0 ? `${next}px` : null,
          )
          return chain().focus().updateAttributes('paragraph', { style: style || null }).run()
        },
      setJustifyIndent:
        (px: number) =>
        ({ chain, editor }) => {
          let style = currentParagraphStyle(editor)
          if (px <= 0) {
            style = mergeStyleProp(style, 'padding-left', null)
            style = mergeStyleProp(style, 'padding-right', null)
          } else {
            style = mergeStyleProp(style, 'padding-left', `${px}px`)
            style = mergeStyleProp(style, 'padding-right', `${px}px`)
          }
          return chain().focus().updateAttributes('paragraph', { style: style || null }).run()
        },
      clearFormat:
        () =>
        ({ chain }) =>
          chain()
            .focus()
            .unsetAllMarks()
            .updateAttributes('paragraph', { style: null })
            .run(),
    }
  },
})
