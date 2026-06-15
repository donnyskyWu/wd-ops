import { ElMessageBox } from 'element-plus'

/** 平台账号保存时 1502 / 关联资源占用 */
export function isAccountBindingConflict(err: unknown): boolean {
  const msg = (err as { message?: string })?.message || ''
  return msg.includes('已被引用') || msg.includes('已被占用') || msg.includes('1502')
}

/** 展示占用明细并收集强制替换原因 */
export async function promptAccountForceReplace(
  err: unknown,
  onConfirm: (reason: string) => Promise<void>,
): Promise<boolean> {
  const detail = (err as { message?: string })?.message || '关联资源已被占用'
  try {
    const { value } = await ElMessageBox.prompt(
      `${detail}\n\n请填写强制替换原因（5-200字）`,
      '关联资源占用 · 强制替换',
      {
        confirmButtonText: '确认替换',
        cancelButtonText: '取消',
        inputPattern: /^.{5,200}$/,
        inputErrorMessage: '原因长度需 5-200 字',
      },
    )
    await onConfirm(value)
    return true
  } catch {
    return false
  }
}
