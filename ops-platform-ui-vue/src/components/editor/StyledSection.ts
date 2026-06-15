import { Node } from '@tiptap/core'

/** WeChat layout section block — preserves styled `<section>` wrappers. */
export const StyledSection = Node.create({
  name: 'styledSection',
  group: 'block',
  content: 'block+',
  defining: true,
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
      class: {
        default: null,
        parseHTML: (element) => element.getAttribute('class'),
        renderHTML: (attributes) => {
          if (!attributes.class) return {}
          return { class: attributes.class }
        },
      },
    }
  },
  parseHTML() {
    return [{ tag: 'section' }, { tag: 'div[data-wechat-section]' }]
  },
  renderHTML({ HTMLAttributes }) {
    return ['section', HTMLAttributes, 0]
  },
})
