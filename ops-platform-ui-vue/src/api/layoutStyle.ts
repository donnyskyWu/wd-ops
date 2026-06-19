/**
 * M2 layout style library API (S-15)
 */
import { request } from '@/utils/request'

export interface LayoutStyleVO {
  id: number
  styleCode: string
  name: string
  category: string
  tags?: string
  htmlSnippet: string
  thumbnailFileId?: number
  sort?: number
  status: string
  updateTime?: string
}

export type LayoutStyleCategory = 'HEADING' | 'BODY' | 'IMAGE_TEXT' | 'GUIDE' | 'DIVIDER'

export function listEnabledLayoutStyles(category?: string, keyword?: string) {
  return request
    .get<LayoutStyleVO[]>({
      url: '/oa/layout-style/enabled-list',
      params: { category, keyword },
    })
    .then((res) => res as unknown as LayoutStyleVO[])
}

export function getLayoutStyle(id: number) {
  return request
    .get<LayoutStyleVO>({ url: `/oa/layout-style/${id}` })
    .then((res) => res as unknown as LayoutStyleVO)
}

export function listLayoutStyles(params: Record<string, unknown>) {
  return request
    .get<{ list: LayoutStyleVO[]; total: number }>({ url: '/oa/layout-style/list', params })
    .then((res) => res as unknown as { list: LayoutStyleVO[]; total: number })
}
