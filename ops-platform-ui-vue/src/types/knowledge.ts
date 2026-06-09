/**
 * 内容知识库 - TypeScript类型定义
 */

// ==================== 知识库核心类型 ====================

/**
 * 知识分类枚举
 */
export enum KnowledgeCategory {
  /** 案例库 */
  CASE = 'case',
  /** 模板库 */
  TEMPLATE = 'template',
  /** 行业资料 */
  INDUSTRY = 'industry',
  /** 运营经验 */
  EXPERIENCE = 'experience',
}

/**
 * 知识分类标签映射
 */
export const CATEGORY_LABELS: Record<KnowledgeCategory, string> = {
  [KnowledgeCategory.CASE]: '案例库',
  [KnowledgeCategory.TEMPLATE]: '模板库',
  [KnowledgeCategory.INDUSTRY]: '行业资料',
  [KnowledgeCategory.EXPERIENCE]: '运营经验',
}

/**
 * 知识列表项
 */
export interface KnowledgeVO {
  /** 知识ID */
  id: number
  /** 标题 */
  title: string
  /** 分类 */
  category: KnowledgeCategory
  /** 标签列表 */
  tags: string[]
  /** 是否公开 */
  isPublic: boolean
  /** 创建者姓名 */
  creatorName: string
  /** 阅读量 */
  viewCount: number
  /** 收藏数 */
  likeCount: number
  /** 创建时间 */
  createdAt: string
}

/**
 * 知识详情
 */
export interface KnowledgeDetailVO extends KnowledgeVO {
  /** 知识内容（富文本） */
  content: string
  /** 是否已收藏 */
  isLiked: boolean
  /** 更新时间 */
  updatedAt: string
}

/**
 * 知识查询参数
 */
export interface KnowledgeQuery {
  /** 页码 */
  pageNo: number
  /** 每页条数 */
  pageSize: number
  /** 关键词全文搜索 */
  keyword?: string
  /** 分类筛选 */
  category?: KnowledgeCategory
  /** 标签筛选（多选） */
  tags?: string[]
  /** 是否公开 */
  isPublic?: boolean
  /** 创建者ID */
  creatorUserId?: number
}

/**
 * 知识表单数据
 */
export interface KnowledgeFormData {
  /** 知识ID（编辑时必填） */
  id?: number
  /** 标题（必填，1-100字） */
  title: string
  /** 分类（必填） */
  category: KnowledgeCategory
  /** 内容（必填，富文本） */
  content: string
  /** 标签（可选，最多10个） */
  tags: string[]
  /** 是否公开（默认true） */
  isPublic: boolean
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  list: T[]
  total: number
}

/**
 * 收藏/取消收藏请求
 */
export interface LikeAction {
  /** 知识ID */
  id: number
  /** 操作类型 */
  action: 'like' | 'unlike'
}
