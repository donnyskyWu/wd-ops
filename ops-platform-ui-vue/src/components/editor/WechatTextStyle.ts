import { Extension } from '@tiptap/core'
import { TextStyle } from '@tiptap/extension-text-style'

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    wechatTextStyle: {
      setFontSize: (size: string) => ReturnType
      unsetFontSize: () => ReturnType
      setLetterSpacing: (spacing: string) => ReturnType
      unsetLetterSpacing: () => ReturnType
    }
  }
}

/** Inline font-size / letter-spacing via textStyle mark (WeChat-compatible span styles). */
export const WechatTextStyle = Extension.create({
  name: 'wechatTextStyle',
  addExtensions() {
    return [TextStyle]
  },
  addGlobalAttributes() {
    return [
      {
        types: ['textStyle'],
        attributes: {
          fontSize: {
            default: null,
            parseHTML: (element) => {
              const el = element as HTMLElement
              return el.style.fontSize || null
            },
            renderHTML: (attributes) => {
              if (!attributes.fontSize) return {}
              return { style: `font-size: ${attributes.fontSize}` }
            },
          },
          letterSpacing: {
            default: null,
            parseHTML: (element) => {
              const el = element as HTMLElement
              return el.style.letterSpacing || null
            },
            renderHTML: (attributes) => {
              if (!attributes.letterSpacing) return {}
              return { style: `letter-spacing: ${attributes.letterSpacing}` }
            },
          },
        },
      },
    ]
  },
  addCommands() {
    return {
      setFontSize:
        (size: string) =>
        ({ chain }) =>
          chain().setMark('textStyle', { fontSize: size }).run(),
      unsetFontSize:
        () =>
        ({ chain }) =>
          chain().setMark('textStyle', { fontSize: null }).removeEmptyTextStyle().run(),
      setLetterSpacing:
        (spacing: string) =>
        ({ chain }) =>
          chain().setMark('textStyle', { letterSpacing: spacing }).run(),
      unsetLetterSpacing:
        () =>
        ({ chain }) =>
          chain().setMark('textStyle', { letterSpacing: null }).removeEmptyTextStyle().run(),
    }
  },
})
