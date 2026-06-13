/**
 * 通用工具函数库
 */
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

/** 兼容 axios 直返与 { data } 包装两种响应形态 */
export function unwrapApiData<T>(response: T | { data?: T }): T {
  if (response !== null && typeof response === 'object' && 'data' in response) {
    return (response as { data?: T }).data as T
  }
  return response as T
}

/** 从分页响应中提取 list/records 与 total */
export function pickListPage<T>(page: { list?: T[]; records?: T[]; total?: number } | null | undefined) {
  const list = page?.list ?? page?.records ?? []
  return { list, total: page?.total ?? list.length }
}

/** 分页拉取全量数据（默认 500/页），用于导出 */
export async function fetchAllPaginated<T>(
  fetchPage: (pageNum: number, pageSize: number) => Promise<{ list?: T[]; records?: T[]; total?: number } | null | undefined>,
  pageSize = 500,
): Promise<T[]> {
  const first = pickListPage(await fetchPage(1, pageSize))
  let rows = [...first.list] as T[]
  if (first.total > pageSize) {
    const totalPages = Math.ceil(first.total / pageSize)
    for (let pageNum = 2; pageNum <= totalPages; pageNum += 1) {
      const page = pickListPage(await fetchPage(pageNum, pageSize))
      rows = rows.concat(page.list as T[])
    }
  }
  return rows
}

/** 报表行字段：优先 snake_case，兼容旧 camelCase API */
export function reportField(row: Record<string, unknown> | null | undefined, snake: string, camel?: string): unknown {
  if (!row) return undefined
  const v = row[snake]
  if (v != null && v !== '') return v
  const camelKey = camel ?? snake.replace(/_([a-z])/g, (_, c: string) => c.toUpperCase())
  const v2 = row[camelKey]
  if (v2 != null && v2 !== '') return v2
  return v ?? v2
}

/**
 * 导出表格数据为Excel文件（使用Blob）
 * @param data 表格数据数组
 * @param columns 列配置 [{key: 'name', label: '姓名'}]
 * @param fileName 文件名（不含扩展名）
 */
export function exportToExcel(
  data: any[],
  columns: { key: string; label: string }[],
  fileName: string = '导出数据'
) {
  if (!data || data.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  try {
    // 构建CSV内容
    const BOM = '\uFEFF' // UTF-8 BOM
    const headers = columns.map(col => col.label).join(',')
    const rows = data.map(row => {
      return columns.map(col => {
        const value = row[col.key]
        // 处理包含逗号或换行的值
        if (value === null || value === undefined) return ''
        const str = String(value)
        if (str.includes(',') || str.includes('\n') || str.includes('"')) {
          return `"${str.replace(/"/g, '""')}"`
        }
        return str
      }).join(',')
    })

    const csvContent = BOM + [headers, ...rows].join('\n')
    
    // 创建Blob并下载
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    
    link.setAttribute('href', url)
    link.setAttribute('download', `${fileName}_${formatDate(new Date())}.csv`)
    link.style.visibility = 'hidden'
    
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success(`导出成功，共 ${data.length} 条数据`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请重试')
  }
}

/**
 * 导出为JSON文件
 * @param data 数据
 * @param fileName 文件名
 */
export function exportToJSON(data: any, fileName: string = '导出数据') {
  try {
    const jsonStr = JSON.stringify(data, null, 2)
    const blob = new Blob([jsonStr], { type: 'application/json' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    
    link.setAttribute('href', url)
    link.setAttribute('download', `${fileName}_${formatDate(new Date())}.json`)
    link.style.visibility = 'hidden'
    
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请重试')
  }
}

/**
 * 格式化日期
 * @param date 日期对象
 * @param format 格式字符串（默认 YYYYMMDD_HHmmss）
 */
export function formatDate(date: Date, format: string = 'YYYYMMDD_HHmmss'): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/** 统一展示 yyyy-MM-dd HH:mm:ss */
export function formatDateTime(value: string | Date | null | undefined): string {
  if (value == null || value === '') return '-'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

/**
 * 统一错误处理
 * @param error 错误对象
 * @param defaultMessage 默认错误消息
 */
export function handleError(error: unknown, defaultMessage: string = '操作失败'): void {
  console.error('Error:', error)
  
  if (error instanceof Error) {
    ElMessage.error(error.message || defaultMessage)
  } else if (typeof error === 'string') {
    ElMessage.error(error)
  } else {
    ElMessage.error(defaultMessage)
  }
}

/**
 * 带重试的异步操作
 * @param fn 异步函数
 * @param retries 重试次数（默认3次）
 * @param delay 延迟时间（毫秒，默认1000ms）
 */
export async function retryAsync<T>(
  fn: () => Promise<T>,
  retries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: unknown
  
  for (let i = 0; i < retries; i++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error
      console.warn(`第 ${i + 1} 次尝试失败:`, error)
      
      if (i < retries - 1) {
        await new Promise(resolve => setTimeout(resolve, delay))
      }
    }
  }
  
  throw lastError
}

/**
 * 防抖函数
 * @param fn 要防抖的函数
 * @param delay 延迟时间（毫秒）
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null
  
  return function(this: any, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
      timer = null
    }, delay)
  }
}

/**
 * 节流函数
 * @param fn 要节流的函数
 * @param delay 延迟时间（毫秒）
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let lastTime = 0
  
  return function(this: any, ...args: Parameters<T>) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      fn.apply(this, args)
      lastTime = now
    }
  }
}

/**
 * 深拷贝
 * @param obj 要拷贝的对象
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj.getTime()) as any
  if (obj instanceof Array) return obj.map(item => deepClone(item)) as any
  if (obj instanceof Object) {
    const cloned = {} as T
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        cloned[key] = deepClone(obj[key])
      }
    }
    return cloned
  }
  return obj
}

/**
 * 金额格式化
 * @param amount 金额
 * @param decimals 小数位数（默认2）
 */
export function formatMoney(amount: number, decimals: number = 2): string {
  if (amount === null || amount === undefined) return '0.00'
  return Number(amount).toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 百分比格式化
 * @param value 值
 * @param decimals 小数位数（默认2）
 */
export function formatPercent(value: number, decimals: number = 2): string {
  if (value === null || value === undefined) return '0.00%'
  return `${Number(value).toFixed(decimals)}%`
}
