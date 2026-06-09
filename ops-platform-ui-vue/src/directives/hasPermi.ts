/**
 * 按钮级权限控制指令 v-hasPermi
 * 
 * 用于控制页面操作按钮的显示/隐藏
 * - 无权限按钮不渲染（非禁用）
 * - 权限数据从 localStorage 获取并缓存
 */

import type { DirectiveBinding, Directive } from 'vue'

// 权限数据存储（从后端获取后设置）
let userPermissions: string[] = []


/**
 * 设置用户权限列表
 * 
 * @param permissions 权限标识数组
 * 
 * @example
 * // 登录成功后从后端获取权限
 * import { setUserPermissions } from '@/directives/hasPermi'
 * const response = await getUserPermissions()
 * setUserPermissions(response.data)
 */
export function setUserPermissions(permissions: string[]): void {
  userPermissions = permissions
  // 缓存到 localStorage
  localStorage.setItem('userPermissions', JSON.stringify(permissions))
}

/**
 * 获取用户权限列表
 */
function getUserPermissions(): string[] {
  if (userPermissions.length > 0) {
    return userPermissions
  }
  // 从 localStorage 读取
  const cached = localStorage.getItem('userPermissions')
  if (cached) {
    try {
      userPermissions = JSON.parse(cached)
    } catch {
      userPermissions = []
    }
  }
  return userPermissions
}

/**
 * 判断是否有权限
 * 
 * @param permission 权限标识
 * @returns boolean
 */
function hasPermission(permission: string | string[]): boolean {
  // 如果权限为空，认为有权限
  if (!permission) {
    return true
  }

  // 获取用户权限列表
  const perms = getUserPermissions()
  
  // 如果是数组，检查是否包含任一权限
  if (Array.isArray(permission)) {
    return permission.some(p => perms.includes(p))
  }
  
  // 如果是字符串，检查是否包含该权限
  return perms.includes(permission)
}

/**
 * hasPermi 指令
 * 
 * 使用方式：
 * - 单个权限：<el-button v-hasPermi="'oa:task:create'">新增</el-button>
 * - 多个权限（任一匹配）：<el-button v-hasPermi="['oa:task:create', 'oa:task:delete']">操作</el-button>
 * 
 * 注意：权限标识需要与后端保持一致
 */
export const hasPermi: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    
    // 空权限或有权限，显示按钮
    if (!value || hasPermission(value as string | string[])) {
      return
    }
    
    // 无权限，移除按钮
    el.parentNode?.removeChild(el)
  }
}

/**
 * hasNoPermi 指令（反向）
 * 
 * 使用方式：
 * - 无指定权限时才显示：<el-button v-hasNoPermi="'oa:admin'">仅普通用户可见</el-button>
 */
export const hasNoPermi: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    
    // 无权限或有不匹配的权限，显示按钮
    if (!value || !hasPermission(value as string | string[])) {
      return
    }
    
    // 有权限（所有权限都匹配），移除按钮
    el.parentNode?.removeChild(el)
  }
}

/**
 * hasAnyPermi 指令（显式名称）
 * 
 * 使用方式：
 * - 拥有任一权限时显示：<el-button v-hasAnyPermi="['oa:task:create', 'oa:task:edit']">操作</el-button>
 */
export const hasAnyPermi: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    
    if (!value || !Array.isArray(value)) {
      return
    }
    
    if (hasPermission(value)) {
      return
    }
    
    el.parentNode?.removeChild(el)
  }
}

/**
 * 注册所有权限指令
 */
export function registerPermissionDirectives(app: any): void {
  app.directive('hasPermi', hasPermi)
  app.directive('hasNoPermi', hasNoPermi)
  app.directive('hasAnyPermi', hasAnyPermi)
}