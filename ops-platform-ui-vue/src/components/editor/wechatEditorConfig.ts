/** WeChat mp editor toolbar presets (from 公众号.mhtml edui toolbar). */

export const WECHAT_FONT_SIZES = ['12px', '14px', '15px', '16px', '17px', '18px', '20px', '24px'] as const

export const WECHAT_LINE_HEIGHTS = ['1', '1.5', '1.6', '1.75', '2', '3', '4', '5'] as const

export const WECHAT_PARAGRAPH_SPACING = ['0', '8', '16', '24', '32', '40', '48'] as const

export const WECHAT_LETTER_SPACING = ['0', '0.5em', '1em', '2em'] as const

export const WECHAT_JUSTIFY_INDENT = [0, 8, 16, 32, 48] as const

/** Basic palette from WeChat forecolor/backcolor picker. */
export const WECHAT_BASIC_COLORS = [
  '#000000',
  '#888888',
  '#ffffff',
  '#ff0000',
  '#ff6827',
  '#ffda51',
  '#00d100',
  '#0080ff',
  '#ac39ff',
  '#ff2941',
  '#ffd7d5',
  '#ffdaa9',
  '#fffed5',
  '#73fcd6',
  '#a5c8ff',
  '#ffacd5',
  '#b2b2b2',
  '#3da742',
  '#3daad6',
  '#797baa',
] as const

export const WECHAT_EMOJIS = [
  '😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉', '😊',
  '😋', '😎', '😍', '😘', '🥰', '😗', '😙', '😚', '🙂', '🤗',
  '🤩', '🤔', '🤨', '😐', '😑', '😶', '🙄', '😏', '😣', '😥',
  '😮', '🤐', '😯', '😪', '😫', '🥱', '😴', '😌', '😛', '😜',
  '😝', '🤤', '😒', '😓', '😔', '😕', '🙃', '🤑', '😲', '🙁',
  '😖', '😞', '😟', '😤', '😢', '😭', '😦', '😧', '😨', '😩',
  '🤯', '😬', '😰', '😱', '🥵', '🥶', '😳', '🤪', '😵', '🥴',
  '👍', '👎', '👏', '🙌', '🤝', '🙏', '💪', '❤️', '💔', '💯',
  '✨', '🔥', '🎉', '🎊', '✅', '❌', '⭐', '🌟', '💡', '📌',
] as const

export const DEFAULT_SECTION_STYLE =
  'margin: 10px 0; padding: 16px; background: #f7f7f7; border-radius: 8px;'
