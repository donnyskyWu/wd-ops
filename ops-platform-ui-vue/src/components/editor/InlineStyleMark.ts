import { Mark } from '@tiptap/core'

/** Preserve arbitrary inline styles on <span> (WeChat caption / emphasis wrappers). */
export const InlineStyleMark = Mark.create({
  name: 'inlineStyle',
  addAttributes() {
    return {
      style: {
        default: null,
        parseHTML: (element) => element.getAttribute('style'),
        renderHTML: (attributes) => {
          if (!attributes.style) return {}
          return { style: attributes.style }
        },
      },
    }
  },
  parseHTML() {
    return [{ tag: 'span[style]' }]
  },
  renderHTML({ HTMLAttributes }) {
    return ['span', HTMLAttributes, 0]
  },
})
