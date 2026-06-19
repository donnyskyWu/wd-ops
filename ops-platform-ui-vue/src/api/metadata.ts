/**
 * M8 元数据维护 API + 指标/自定义查询集成读口
 */
import { request } from '@/utils/request'

/** 查询条件类别 — 与 dict_metadata_query_condition_type 对齐 */
export type MetadataQueryConditionType =
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'DATE_RANGE'
  | 'DICT'
  | 'IP_GROUP_SELECT'
  | 'USER_SELECT'
  | 'PLATFORM_SELECT'
  | 'ACCOUNT_SELECT'
  | 'COMPETITION_SELECT'

export interface MetadataFieldVO {
  id: number
  entityId: number
  fieldCode: string
  fieldName: string
  columnName: string
  dataType: string
  queryConditionType: MetadataQueryConditionType
  dictType?: string
  selectorConfig?: Record<string, unknown>
  sort: number
}

export interface MetadataEntityVO {
  id: number
  entityCode: string
  entityName: string
  physicalTable: string
  status: string
  remark?: string
  updateTime?: string
  fields?: MetadataFieldVO[]
}

export interface UnmappedTableVO {
  tableName: string
  suggestedEntityCode: string
  suggestedEntityName: string
}

export interface MetadataListResult {
  list: MetadataEntityVO[]
  total: number
}

export function fetchMetadataList(params: Record<string, unknown>) {
  return request.get<MetadataListResult>({ url: '/oa/metadata/list', params })
}

export function fetchMetadataDetail(id: number) {
  return request.get<MetadataEntityVO>({ url: `/oa/metadata/${id}` })
}

/** 指标 & 自定义查询集成：按 entity_code 读取字段配置 */
export function fetchMetadataFieldsByCode(entityCode: string) {
  return request.get<MetadataFieldVO[]>({ url: `/oa/metadata/entity/${entityCode}/fields` })
}

export function fetchUnmappedTables() {
  return request.get<UnmappedTableVO[]>({ url: '/oa/metadata/unmapped-tables' })
}

export function createMetadataEntity(data: {
  entityCode: string
  entityName: string
  physicalTable: string
  status?: string
  remark?: string
}) {
  return request.post<number>({ url: '/oa/metadata/create', data })
}

export function updateMetadataEntity(data: {
  id: number
  entityName?: string
  status?: string
  remark?: string
}) {
  return request.put({ url: '/oa/metadata/update', data })
}

export function updateMetadataFields(entityId: number, fields: Partial<MetadataFieldVO>[]) {
  return request.put({
    url: `/oa/metadata/${entityId}/fields`,
    data: { fields },
  })
}

export function deleteMetadataEntity(id: number) {
  return request.delete({ url: `/oa/metadata/${id}` })
}

/** 将元数据字段转为指标构建器可用的 filter 元信息 */
export function toMetricFilterMeta(fields: MetadataFieldVO[]) {
  return fields
    .filter((f) => f.queryConditionType !== 'TEXT' || f.fieldCode !== 'title')
    .map((f) => ({
      name: f.columnName,
      label: f.fieldName,
      queryConditionType: f.queryConditionType,
      dictType: f.dictType,
      dataType: f.dataType,
    }))
}
