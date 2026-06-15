/**
 * 统一 Axios 请求封装
 * 
 * 基于 Element Plus 和 Axios 封装，包含以下功能：
 * - 请求/响应拦截器
 * - 统一错误处理
 * - Mock 数据降级
 * - TypeScript 类型支持
 */

import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/admin-api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now(),
      }
    }
    
    // Dev Token + 租户（ADR-003 · GATE-S0）
    const envToken = import.meta.env.VITE_API_TOKEN
    const envTenantId = import.meta.env.VITE_TENANT_ID
    const token = envToken || localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const tenantId = envTenantId || localStorage.getItem('tenantId') || '1'
    config.headers['X-Tenant-Id'] = tenantId

    // FormData：须去掉 application/json，否则 transformRequest 会把 FormData 序列化成 JSON，
    // 后端 multipart 端点收到错误 body → Spring Security HTTP 403（空 body）
    if (config.data instanceof FormData) {
      // Axios 1.x: false removes Content-Type so the browser sets multipart boundary
      if (config.headers && typeof config.headers.setContentType === 'function') {
        config.headers.setContentType(false)
      } else if (config.headers) {
        delete config.headers['Content-Type']
        delete config.headers['content-type']
      }
      config.transformRequest = [(data) => data]
    }
    
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, msg, data } = response.data
    
    // 如果返回的不是标准格式，直接返回 data
    if (code === undefined) {
      return data
    }
    
    // 成功
    if (code === 0 || code === 200) {
      return data
    }
    
    // 未登录 / Token 无效
    if (code === 401) {
      ElMessage.error(msg || '登录已过期，请重新登录')
      localStorage.removeItem('token')
      if (!import.meta.env.VITE_API_TOKEN) {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(msg || '登录已过期'))
    }

    // 业务错误码 1500-1504 / 2006 / M1 1001-1402 / M2 2010-2040
    if ((code >= 1500 && code <= 1504) || code === 2006
        || (code >= 1001 && code <= 1008) || (code >= 1101 && code <= 1104)
        || code === 1201 || (code >= 1301 && code <= 1304)
        || (code >= 2010 && code <= 2040)) {
      ElMessage.error(msg || `业务错误(${code})`)
      return Promise.reject(new Error(msg || `业务错误(${code})`))
    }

    // 服务端业务失败（HTTP 200 但 code=500）
    if (code === 500) {
      ElMessage.error(msg || '服务器内部错误')
      return Promise.reject(new Error(msg || '服务器内部错误'))
    }
    
    // 无权限
    if (code === 403) {
      ElMessage.warning('无权限操作')
      return Promise.reject(new Error(msg || '无权限'))
    }
    
    // 其他错误
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    console.error('响应错误:', error)
    
    if (error.response) {
      switch (error.response.status) {
        case 400:
          ElMessage.error('请求参数错误')
          break
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.warning('无权限操作')
          break
        case 404:
          ElMessage.warning('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误，请联系管理员')
          break
        default:
          ElMessage.error('请求失败')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络后重试')
    } else if (error.message.includes('Network Error')) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    
    return Promise.reject(error)
  }
)

// 导出 request 实例
export default service

// 导出请求方法（方便直接调用）
export const request = {
  get<T = any>(config: AxiosRequestConfig): Promise<T> {
    const { url, ...rest } = config
    return service.get(url as string, rest)
  },
  post<T = any>(config: AxiosRequestConfig): Promise<T> {
    const { url, data, ...rest } = config
    return service.post(url as string, data, rest)
  },
  put<T = any>(config: AxiosRequestConfig): Promise<T> {
    const { url, data, ...rest } = config
    return service.put(url as string, data, rest)
  },
  delete<T = any>(config: AxiosRequestConfig): Promise<T> {
    const { url, ...rest } = config
    return service.delete(url as string, rest)
  },
}