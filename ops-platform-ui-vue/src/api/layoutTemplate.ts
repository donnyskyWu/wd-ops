/**

 * 公推模板库 API

 */

import { request } from '@/utils/request'

import type { PageResult } from '@/types/knowledge'

import type {

  LayoutImportJobVO,

  LayoutMergePreviewVO,

  LayoutTemplateDetailVO,

  LayoutTemplateForm,

  LayoutTemplateSelectVO,

  LayoutTemplateVO

} from '@/types/layoutTemplate'



export function listLayoutTemplates(params: Record<string, unknown>) {

  return request

    .get<PageResult<LayoutTemplateVO>>({ url: '/oa/layout-template/list', params })

    .then((res) => res as unknown as PageResult<LayoutTemplateVO>)

}



export function getLayoutTemplate(id: number) {

  return request

    .get<LayoutTemplateDetailVO>({ url: `/oa/layout-template/${id}` })

    .then((res) => res as unknown as LayoutTemplateDetailVO)

}



export function listLayoutTemplateSelect(contentType: string, documentType?: string) {

  return request

    .get<LayoutTemplateSelectVO[]>({

      url: '/oa/layout-template/select-list',

      params: { contentType, documentType }

    })

    .then((res) => res as unknown as LayoutTemplateSelectVO[])

}



export function createLayoutTemplate(data: LayoutTemplateForm) {

  return request.post<number>({ url: '/oa/layout-template/create', data }).then((res) => res as unknown as number)

}



export function updateLayoutTemplate(data: LayoutTemplateForm & { id: number }) {

  return request.put<void>({ url: '/oa/layout-template/update', data }).then(() => undefined)

}



export function deleteLayoutTemplate(id: number) {

  return request.delete<void>({ url: `/oa/layout-template/${id}` }).then(() => undefined)

}



export function copyLayoutTemplate(id: number) {

  return request.post<number>({ url: `/oa/layout-template/${id}/copy` }).then((res) => res as unknown as number)

}



export function publishLayoutTemplate(id: number) {

  return request.post<void>({ url: `/oa/layout-template/${id}/publish` }).then(() => undefined)

}



export function disableLayoutTemplate(id: number) {

  return request.post<void>({ url: `/oa/layout-template/${id}/disable` }).then(() => undefined)

}



export function enableLayoutTemplate(id: number) {

  return request.post<void>({ url: `/oa/layout-template/${id}/enable` }).then(() => undefined)

}



export function importLayoutUrl(data: { sourceUrl: string; templateName?: string; documentType?: string }) {

  return request

    .post<{ jobId: number; status: string }>({ url: '/oa/layout-template/import-url', data })

    .then((res) => res as unknown as { jobId: number; status: string })

}



export function importLayoutPaste(data: { templateName: string; html: string; documentType?: string }) {

  return request

    .post<LayoutTemplateDetailVO>({ url: '/oa/layout-template/import-paste', data })

    .then((res) => res as unknown as LayoutTemplateDetailVO)

}



export function importLayoutDocx(formData: FormData) {

  return request

    .post<{ jobId: number; status: string }>({

      url: '/oa/layout-template/import-docx',

      data: formData,

    })

    .then((res) => res as unknown as { jobId: number; status: string })

}



export function getLayoutImportJob(jobId: number) {

  return request

    .get<LayoutImportJobVO>({ url: `/oa/layout-template/import-job/${jobId}` })

    .then((res) => res as unknown as LayoutImportJobVO)

}



export function previewTemplateMerge(templateId: number, body: string, existingLayoutJson?: unknown) {

  return request

    .post<LayoutMergePreviewVO>({

      url: `/oa/layout-template/${templateId}/preview-merge`,

      data: { body, existingLayoutJson }

    })

    .then((res) => res as unknown as LayoutMergePreviewVO)

}



export function applyLayoutTemplate(contentId: number, layoutTemplateId: number, overwrite = false) {

  return request

    .post<Record<string, unknown>>({

      url: `/oa/content/${contentId}/apply-layout-template`,

      data: { layoutTemplateId, overwrite }

    })

    .then((res) => res as unknown as Record<string, unknown>)

}



export function previewApplyLayoutTemplate(contentId: number, layoutTemplateId: number) {

  return request

    .post<LayoutMergePreviewVO>({

      url: `/oa/content/${contentId}/apply-layout-template/preview`,

      data: { layoutTemplateId, overwrite: true }

    })

    .then((res) => res as unknown as LayoutMergePreviewVO)

}

