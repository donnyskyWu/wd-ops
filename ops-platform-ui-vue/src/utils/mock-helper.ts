/**
 * 统一Mock数据加载工具
 * 
 * 用于标准化所有页面的Mock数据加载流程
 * - 统一延迟时间（300ms）
 * - 统一错误处理
 * - 支持分页参数
 */

/**
 * 加载Mock数据
 * 
 * @param mockFn Mock数据获取函数
 * @param params 查询参数（包含pageNo、pageSize等）
 * @returns Promise<{ list: T[], total: number }>
 * 
 * @example
 * // 在页面中使用
 * onMounted(async () => {
 *   const result = await loadMockData(mockGetTaskList, { pageNo: 1, pageSize: 10 })
 *   tableData.value = result.list
 *   pagination.total = result.total
 * })
 */
export function loadMockData<T>(
  mockFn: (params?: any) => { list: T[]; total: number },
  params: any = {}
): Promise<{ list: T[]; total: number }> {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      try {
        const result = mockFn(params)
        resolve(result)
      } catch (error) {
        console.error('Mock数据加载失败:', error)
        reject(error)
      }
    }, 300) // 统一300ms延迟，模拟网络请求
  })
}

/**
 * 加载单条Mock数据（用于详情页）
 * 
 * @param mockFn Mock数据获取函数
 * @param id 数据ID
 * @returns Promise<T>
 * 
 * @example
 * const detail = await loadMockDetail(mockGetTaskDetail, taskId)
 */
export function loadMockDetail<T>(
  mockFn: (id: any) => T,
  id: any
): Promise<T> {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      try {
        const result = mockFn(id)
        resolve(result)
      } catch (error) {
        console.error('Mock详情数据加载失败:', error)
        reject(error)
      }
    }, 300)
  })
}

/**
 * 执行Mock操作（用于新增、编辑、删除等操作）
 *
 * @param mockFn Mock操作函数
 * @param params 操作参数
 * @returns Promise<any>
 *
 * @example
 * await executeMockAction(mockCreateTask, formData)
 */
export function executeMockAction<T = any>(
  mockFn: (params?: any) => T,
  params: any = {}
): Promise<T> {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      try {
        const result = mockFn(params)
        resolve(result)
      } catch (error) {
        console.error('Mock操作执行失败:', error)
        reject(error)
      }
    }, 300)
  })
}

/**
 * Snowflake ID 生成器（前端 Mock 适配）
 *
 * 雪花算法 64-bit，但 JS Number 安全范围 53-bit。
 * Mock 场景下，我们只取 41-bit 时间戳 + 12-bit 序列号（共 53-bit），保证唯一性即可。
 *
 * 结构（53-bit Number）：
 * - 41 bit：毫秒时间戳（相对 epoch）
 * - 12 bit：序列号（单进程内单调递增，毫秒内重置）
 *
 * @returns 13 位以上的 Number ID（形如 1.9e18，远大于 53-bit 安全整数 9e15）
 */
const SNOWFLAKE_EPOCH = 1700000000000 // 2023-11-14
let snowflakeSeq = 0
let snowflakeLastMs = 0

export function snowflakeId(): number {
  let now = Date.now()
  if (now === snowflakeLastMs) {
    snowflakeSeq = (snowflakeSeq + 1) & 0xfff
    if (snowflakeSeq === 0) {
      // 序列号溢出，等待下一毫秒
      while (now <= snowflakeLastMs) now = Date.now()
    }
  } else {
    snowflakeSeq = 0
  }
  snowflakeLastMs = now
  // 41-bit timestamp + 12-bit sequence = 53 bit
  return ((now - SNOWFLAKE_EPOCH) << 12) | snowflakeSeq
}

/**
 * 批量生成 Snowflake ID
 * @param count 数量
 * @returns ID 数组
 */
export function snowflakeIds(count: number): number[] {
  return Array.from({ length: count }, () => snowflakeId())
}
