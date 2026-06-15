import Paragraph from '@tiptap/extension-paragraph'

/** Paragraph node that preserves inline style (WeChat body text uses styled <p>). */
export const StyledParagraph = Paragraph.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
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
})
