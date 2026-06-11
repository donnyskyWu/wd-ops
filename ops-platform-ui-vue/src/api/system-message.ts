/**
 * M9 消息管理 API
 */
import { request } from '@/utils/request'

export interface MessageVO {
  id: number
  title: string
  category: string
  channel: string
  receiver: string
  content: string
  status: string
  failReason?: string
  sendTime?: string
  readTime?: string
  read?: boolean
}

export interface MessagePageResult {
  list: MessageVO[]
  total: number
}

export function fetchMessageList(params: {
  title?: string
  receiver?: string
  status?: string
  category?: string
  pageNo?: number
  pageSize?: number
}) {
  return request.get<MessagePageResult>({ url: '/oa/system/message/list', params })
}

export function getMessage(id: number) {
  return request.get<MessageVO>({ url: '/oa/system/message/get', params: { id } })
}

export function fetchUnreadMessages(params?: { pageNo?: number; pageSize?: number }) {
  return request.get<MessagePageResult>({ url: '/oa/system/message/unread', params: { pageNo: 1, pageSize: 10, ...params } })
}

export function fetchUnreadMessageCount() {
  return request.get<number>({ url: '/oa/system/message/unread-count' })
}

export function markMessageRead(id: number) {
  return request.put<boolean>({ url: '/oa/system/message/read', params: { id } })
}

export function sendMessage(data: {
  title: string
  category: string
  channels: string[]
  receiver: string
  content: string
}) {
  return request.post<number>({ url: '/oa/system/message/send', data })
}

export function deleteMessage(id: number) {
  return request.delete<boolean>({ url: '/oa/system/message/delete', params: { id } })
}

export const MESSAGE_CATEGORY_LABEL: Record<string, string> = {
  ALERT: '预警通知',
  SYSTEM: '系统通知',
  BUSINESS: '业务通知',
}

export const MESSAGE_STATUS_LABEL: Record<string, string> = {
  PENDING: '待发送',
  SENT: '已发送',
  FAILED: '发送失败',
}

export const MESSAGE_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  SENT: 'success',
  FAILED: 'danger',
}
