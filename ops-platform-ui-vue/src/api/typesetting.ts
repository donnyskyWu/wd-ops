/**

 * M2 typesetting API (S-17+)

 */

import { request } from '@/utils/request'



export interface TypesettingRuleVO {

  id: number

  ruleCode: string

  name: string

  description?: string

  ruleConfig?: Record<string, unknown> & {

    type?: string

    linkedTemplateId?: number

  }

  sort?: number

  status: string

}



export interface ContentTypesetResult {

  html: string

  plainTextBefore: string

  plainTextAfter: string

  rulesApplied: number

  mode?: 'TEMPLATE' | 'AUTO'

  templateId?: number

  layoutJson?: unknown

  overflowSegmentCount?: number

}



export interface TypesetOptions {

  html: string

  body?: string

  mode?: 'TEMPLATE' | 'AUTO'

  templateId?: number

  ruleCodes?: string[]

  paramOverrides?: Record<string, Record<string, string>>

}



export function listEnabledTypesettingRules() {

  return request

    .get<TypesettingRuleVO[]>({ url: '/oa/typesetting-rule/enabled-list' })

    .then((res) => res as unknown as TypesettingRuleVO[])

}



export function typesetContent(options: TypesetOptions) {

  return request

    .post<ContentTypesetResult>({

      url: '/oa/content/typeset',

      data: options,

    })

    .then((res) => res as unknown as ContentTypesetResult)

}



export function typesetContentById(contentId: number, options: TypesetOptions) {

  return request

    .post<ContentTypesetResult>({

      url: `/oa/content/${contentId}/typeset`,

      data: options,

    })

    .then((res) => res as unknown as ContentTypesetResult)

}



export function getLinkedTemplateId(rule: TypesettingRuleVO): number | undefined {

  const id = rule.ruleConfig?.linkedTemplateId

  return typeof id === 'number' ? id : undefined

}



export function isSmartRule(rule: TypesettingRuleVO): boolean {

  return rule.ruleConfig?.type === 'SMART_OPTIMIZE'

}



export function isTemplateLinkRule(rule: TypesettingRuleVO): boolean {

  return rule.ruleConfig?.type === 'TEMPLATE_LINK'

}


