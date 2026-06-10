/**
 * 内容知识库 - API接口封装
 */

import { request } from '@/utils/request'
import type {
  KnowledgeVO,
  KnowledgeDetailVO,
  KnowledgeQuery,
  KnowledgeFormData,
  PageResult,
  LikeAction,
} from '@/types/knowledge'

// P-GATE-UNMOCK S-C: 已迁移到 @/utils/request，自动注入 dev token 和 tenantId
// P-GATE-UNMOCK-R S-R1 P0-2 (2026-06-09)：URL 前补 /oa/ 段（与后端 RequestMapping 对齐）

// ==================== 知识列表 ====================

/**
 * 获取知识列表（分页）
 */
export function getKnowledgeList(params: KnowledgeQuery): Promise<PageResult<KnowledgeVO>> {
  return request.get<PageResult<KnowledgeVO>>({ url: '/oa/knowledge/list', params }).then((res) => res as unknown as PageResult<KnowledgeVO>)
}

// ==================== 知识详情 ====================

/**
 * 获取知识详情
 */
export function getKnowledgeDetail(id: number): Promise<KnowledgeDetailVO> {
  return request.get<KnowledgeDetailVO>({ url: `/oa/knowledge/${id}` }).then((res) => res as unknown as KnowledgeDetailVO)
}

// ==================== 新增知识 ====================

/**
 * 新增知识
 */
export function createKnowledge(data: KnowledgeFormData): Promise<number> {
  return request.post<number>({ url: '/oa/knowledge/create', data }).then((res) => res as unknown as number)
}

// ==================== 编辑知识 ====================

/**
 * 编辑知识
 */
export function updateKnowledge(data: KnowledgeFormData): Promise<void> {
  return request.put<void>({ url: '/oa/knowledge/update', data }).then(() => undefined)
}

// ==================== 删除知识 ====================

/**
 * 删除知识
 */
export function deleteKnowledge(id: number): Promise<void> {
  return request.delete<void>({ url: '/oa/knowledge/delete', params: { id } }).then(() => undefined)
}

// ==================== 收藏/取消收藏 ====================

/**
 * 收藏/取消收藏
 */
export function toggleLike(data: LikeAction): Promise<void> {
  return request.post<void>({ url: '/oa/knowledge/like', data }).then(() => undefined)
}
