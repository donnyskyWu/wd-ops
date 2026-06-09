/**
 * Excel导出工具
 * 
 * 用于将表格数据导出为Excel文件
 * - 支持大数据量二次确认
 * - 统一文件名格式
 * - 导出过程显示loading
 */

import * as XLSX from 'xlsx'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 导出Excel文件
 * 
 * @param data 表格数据数组
 * @param fileName 文件名（不含日期和扩展名）
 * @param options 导出选项
 * 
 * @example
 * // 基础使用
 * await exportToExcel(tableData.value, '任务管理')
 * 
 * // 带选项使用
 * await exportToExcel(tableData.value, '任务管理', {
 *   sheetName: '任务列表',
 *   confirmThreshold: 5000
 * })
 */
export async function exportToExcel<T = any>(
  data: T[],
  fileName: string,
  options: {
    sheetName?: string
    confirmThreshold?: number
    columns?: Array<{ key: string; label: string }>
  } = {}
): Promise<void> {
  const {
    sheetName = '数据',
    confirmThreshold = 10000,
    columns
  } = options

  // 数据量较大时二次确认
  if (data.length > confirmThreshold) {
    try {
      await ElMessageBox.confirm(
        `数据量较大（${data.length}条），导出可能需要几分钟，是否继续？`,
        '确认导出',
        {
          confirmButtonText: '继续导出',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      // 用户取消导出
      return
    }
  }

  try {
    let exportData = data

    // 如果指定了列映射，转换数据格式
    if (columns && columns.length > 0) {
      exportData = data.map((item: any) => {
        const mappedItem: any = {}
        columns.forEach(col => {
          mappedItem[col.label] = item[col.key]
        })
        return mappedItem
      })
    }

    // 创建工作表
    const ws = XLSX.utils.json_to_sheet(exportData)
    
    // 创建工作簿
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, sheetName)

    // 生成文件名（含日期）
    const dateStr = new Date().toISOString().split('T')[0]
    const fullFileName = `${fileName}_${dateStr}.xlsx`

    // 导出文件
    XLSX.writeFile(wb, fullFileName)

    ElMessage.success(`导出成功：${fullFileName}`)
  } catch (error) {
    console.error('Excel导出失败:', error)
    ElMessage.error('导出失败，请重试')
    throw error
  }
}

/**
 * 导出多Sheet Excel文件
 * 
 * @param sheets 多个Sheet数据
 * @param fileName 文件名
 * 
 * @example
 * await exportMultiSheetExcel([
 *   { name: '任务列表', data: taskData },
 *   { name: '完成统计', data: statsData }
 * ], '任务报表')
 */
export async function exportMultiSheetExcel(
  sheets: Array<{ name: string; data: any[] }>,
  fileName: string
): Promise<void> {
  try {
    // 创建工作簿
    const wb = XLSX.utils.book_new()

    // 添加所有Sheet
    sheets.forEach(sheet => {
      const ws = XLSX.utils.json_to_sheet(sheet.data)
      XLSX.utils.book_append_sheet(wb, ws, sheet.name)
    })

    // 生成文件名（含日期）
    const dateStr = new Date().toISOString().split('T')[0]
    const fullFileName = `${fileName}_${dateStr}.xlsx`

    // 导出文件
    XLSX.writeFile(wb, fullFileName)

    ElMessage.success(`导出成功：${fullFileName}`)
  } catch (error) {
    console.error('Excel导出失败:', error)
    ElMessage.error('导出失败，请重试')
    throw error
  }
}
