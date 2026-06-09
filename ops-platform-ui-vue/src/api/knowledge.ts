/**
 * 内容知识库 - API接口封装
 */

import axios from 'axios'
import type {
  KnowledgeVO,
  KnowledgeDetailVO,
  KnowledgeQuery,
  KnowledgeFormData,
  PageResult,
  LikeAction,
} from '@/types/knowledge'

// 创建axios实例
const request = axios.create({
  baseURL: '/admin-api/oa',
  timeout: 10000,
})

// ==================== 知识列表 ====================

/**
 * 获取知识列表（分页）
 */
export function getKnowledgeList(params: KnowledgeQuery): Promise<PageResult<KnowledgeVO>> {
  return request.get('/knowledge/list', { params }).then(res => res.data)
}

// ==================== 知识详情 ====================

/**
 * 获取知识详情
 */
export function getKnowledgeDetail(id: number): Promise<KnowledgeDetailVO> {
  return request.get(`/knowledge/${id}`).then(res => res.data)
}

// ==================== 新增知识 ====================

/**
 * 新增知识
 */
export function createKnowledge(data: KnowledgeFormData): Promise<number> {
  return request.post('/knowledge/create', data).then(res => res.data)
}

// ==================== 编辑知识 ====================

/**
 * 编辑知识
 */
export function updateKnowledge(data: KnowledgeFormData): Promise<void> {
  return request.put('/knowledge/update', data).then(res => res.data)
}

// ==================== 删除知识 ====================

/**
 * 删除知识
 */
export function deleteKnowledge(id: number): Promise<void> {
  return request.delete('/knowledge/delete', { params: { id } }).then(res => res.data)
}

// ==================== 收藏/取消收藏 ====================

/**
 * 收藏/取消收藏
 */
export function toggleLike(data: LikeAction): Promise<void> {
  return request.post('/knowledge/like', data).then(res => res.data)
}
