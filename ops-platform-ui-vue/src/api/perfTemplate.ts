/**
 * 考核模板 - API接口封装
 */
import { request } from '@/utils/request'
import type {
  PerfTemplateListItem,
  PerfTemplateDetail,
  PerfTemplateQuery,
  MetricOption,
  PageResult,
} from '@/types/perfTemplate'

export function getTemplateList(params: PerfTemplateQuery): Promise<PageResult<PerfTemplateListItem>> {
  return request.get({ url: '/oa/perf/template/list', params })
}

export function getTemplateDetail(id: number): Promise<PerfTemplateDetail> {
  return request.get({ url: `/oa/perf/template/${id}/items` })
}

export function createTemplate(data: PerfTemplateDetail): Promise<number> {
  return request.post({ url: '/oa/perf/template/create', data })
}

export function updateTemplate(data: PerfTemplateDetail): Promise<boolean> {
  return request.put({ url: '/oa/perf/template/update', data })
}

export function activateTemplate(id: number): Promise<boolean> {
  return request.post({ url: '/oa/perf/template/activate', data: { id } })
}

export function getMetricOptions(): Promise<MetricOption[]> {
  return request.get({ url: '/oa/perf/template/list', params: { pageSize: 1 } }).then(() => [])
}
