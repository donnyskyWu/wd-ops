/**
 * 统一HTTP错误处理工具
 * 
 * 用于标准化所有API请求的错误处理
 * - 统一HTTP状态码处理（401/403/404/500）
 * - 网络超时处理
 * - 错误信息统一显示
 */

import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * HTTP状态码对应的错误处理
 */
const HTTP_ERROR_MESSAGES: Record<number, string> = {
  400: '请求参数错误',
  401: '登录已过期，请重新登录',
  403: '无权限操作',
  404: '资源不存在',
  409: '操作冲突，请稍后重试',
  422: '数据验证失败',
  429: '操作过于频繁，请稍后重试',
  500: '系统异常，请联系管理员',
  502: '服务暂时不可用',
  503: '服务暂时不可用',
  504: '请求超时'
}

/**
 * 处理HTTP错误响应
 * 
 * @param error 错误对象
 * @param options 配置选项
 * 
 * @example
 * // 在API拦截器中使用
 * .catch((error) => handleHttpError(error))
 */
export function handleHttpError(
  error: any,
  options: {
    showMessage?: boolean
    on401?: () => void
    on403?: () => void
    on404?: () => void
    on500?: () => void
    onTimeout?: () => void
  } = {}
): void {
  const {
    showMessage = true,
    on401,
    on403,
    on404,
    on500,
    onTimeout
  } = options

  // 获取状态码
  const status = error?.response?.status || error?.status || 0
  const responseData = error?.response?.data
  const errorMsg = responseData?.msg || error?.message || ''

  // 根据状态码处理
  switch (status) {
    case 401:
      // 登录过期，跳转登录页
      if (showMessage) {
        ElMessage.error(HTTP_ERROR_MESSAGES[401] || '登录已过期，请重新登录')
      }
      // 清除用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 跳转登录页
      router.push('/login')
      on401?.()
      break

    case 403:
      if (showMessage) {
        ElMessage.warning(HTTP_ERROR_MESSAGES[403] || '无权限操作')
      }
      on403?.()
      break

    case 404:
      if (showMessage) {
        ElMessage.warning(errorMsg || HTTP_ERROR_MESSAGES[404] || '资源不存在')
      }
      on404?.()
      break

    case 429:
      if (showMessage) {
        ElMessage.warning(HTTP_ERROR_MESSAGES[429] || '操作过于频繁，请稍后重试')
      }
      break

    case 500:
    case 502:
    case 503:
      if (showMessage) {
        ElMessage.error(errorMsg || HTTP_ERROR_MESSAGES[status] || '系统异常，请联系管理员')
      }
      on500?.()
      break

    default:
      // 超时错误
      if (error?.code === 'ECONNABORTED' || errorMsg?.includes('timeout')) {
        if (showMessage) {
          ElMessage.error('请求超时，请检查网络后重试')
        }
        onTimeout?.()
      } else if (!error?.response) {
        // 网络错误
        if (showMessage) {
          ElMessage.error('网络连接失败，请检查网络')
        }
      } else {
        // 其他错误
        if (showMessage && errorMsg) {
          ElMessage.error(errorMsg)
        }
      }
  }
}

/**
 * 创建错误处理拦截器配置
 * 
 * @param options 配置选项
 * @returns Axios拦截器配置对象
 * 
 * @example
 * // 在API模块中使用
 * import { createErrorHandler } from '@/utils/error-handler'
 * 
 * service.interceptors.response.use(
 *   (response) => response.data,
 *   (error) => createErrorHandler(error)
 * )
 */
export function createErrorHandler(options: {
  showMessage?: boolean
  on401?: () => void
  on403?: () => void
  on404?: () => void
  on500?: () => void
  onTimeout?: () => void
} = {}) {
  return (error: any) => {
    handleHttpError(error, options)
    return Promise.reject(error)
  }
}

/**
 * 业务错误码处理
 * 
 * @param code 错误码
 * @param msg 错误消息
 * @param options 配置选项
 * 
 * @example
 * handleBusinessError(1001, '内容不存在')
 */
export function handleBusinessError(
  code: number,
  msg: string,
  options: {
    showMessage?: boolean
  } = {}
): void {
  const { showMessage = true } = options

  if (showMessage && msg) {
    ElMessage.error(msg)
  }
}

/**
 * 业务成功提示
 * 
 * @param message 成功消息
 */
export function showSuccess(message: string = '操作成功'): void {
  ElMessage.success(message)
}

/**
 * 业务警告提示
 * 
 * @param message 警告消息
 */
export function showWarning(message: string): void {
  ElMessage.warning(message)
}

/**
 * 业务信息提示
 * 
 * @param message 信息消息
 */
export function showInfo(message: string): void {
  ElMessage.info(message)
}

/**
 * 分页越界自动回退
 * 
 * @param currentPage 当前页码
 * @param total 总记录数
 * @param pageSize 每页条数
 * @param callback 回退回调
 * 
 * @example
 * handlePageOverflow(currentPage, total, pageSize, (newPage) => {
 *   currentPage.value = newPage
 *   loadData()
 * })
 */
export function handlePageOverflow(
  currentPage: number,
  total: number,
  pageSize: number,
  callback: (newPage: number) => void
): void {
  const totalPages = Math.ceil(total / pageSize)
  if (currentPage > totalPages && totalPages > 0) {
    callback(totalPages)
  }
}