import Highlight from '@tiptap/extension-highlight'

/** Background highlight as inline span (WeChat backcolor compatibility). */
export const WechatHighlight = Highlight.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      color: {
        default: null,
        parseHTML: (element) =>
          (element as HTMLElement).style.backgroundColor || element.getAttribute('data-color'),
        renderHTML: (attributes) => {
          if (!attributes.color) return {}
          return { style: `background-color: ${attributes.color}` }
        },
      },
    }
  },
  parseHTML() {
    return [
      { tag: 'mark' },
      {
        tag: 'span',
        getAttrs: (element) => {
          const el = element as HTMLElement
          const bg = el.style.backgroundColor
          return bg ? { color: bg } : false
        },
      },
    ]
  },
  renderHTML({ HTMLAttributes }) {
    return ['span', HTMLAttributes, 0]
  },
})
