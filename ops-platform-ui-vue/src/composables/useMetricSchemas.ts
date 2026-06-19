/**
 * M6 指标/自定义查询：从 M8 元数据 API 加载表结构（替代硬编码 metricSchema）
 */
import { ref, shallowRef } from 'vue'
import {
  fetchMetadataList,
  fetchMetadataFieldsByCode,
  type MetadataEntityVO,
  type MetadataFieldVO,
} from '@/api/metadata'
import {
  METRIC_TABLE_JOINS,
  setMetricTableSchemas,
  type MetricTableSchema,
  type MetricFieldMeta,
} from '@/constants/metricSchema'

export interface MetricDataSourceOption {
  label: string
  value: string
}

const loading = ref(false)
const error = ref('')
const loaded = ref(false)
const dataSources = shallowRef<MetricDataSourceOption[]>([])

let loadPromise: Promise<void> | null = null

function inferMetricFieldType(field: MetadataFieldVO): MetricFieldMeta['type'] {
  const dt = (field.dataType || '').toUpperCase()
  const qct = field.queryConditionType
  if (qct === 'DATE' || qct === 'DATE_RANGE') {
    return dt.includes('TIME') || field.columnName.endsWith('_time') ? 'datetime' : 'date'
  }
  if (qct === 'NUMBER' || dt.includes('INT') || dt.includes('DECIMAL') || dt.includes('NUMERIC')
    || dt.includes('DOUBLE') || dt.includes('FLOAT') || dt.includes('TINYINT')) {
    return 'number'
  }
  if (dt.includes('DATE') && !dt.includes('TIME')) return 'date'
  if (dt.includes('TIME') || dt.includes('TIMESTAMP')) return 'datetime'
  return 'string'
}

function fieldsToMetricMeta(fields: MetadataFieldVO[]): MetricFieldMeta[] {
  return fields
    .slice()
    .sort((a, b) => a.sort - b.sort || a.id - b.id)
    .map((f) => ({
      name: f.columnName,
      label: f.fieldName,
      type: inferMetricFieldType(f),
    }))
}

function buildSchema(entity: MetadataEntityVO, fields: MetadataFieldVO[]): MetricTableSchema {
  const table = entity.physicalTable
  return {
    table,
    label: entity.entityName,
    alias: 't',
    fields: fieldsToMetricMeta(fields),
    joins: METRIC_TABLE_JOINS[table] ?? [],
  }
}

export async function ensureMetricSchemasLoaded(): Promise<void> {
  if (loaded.value) return
  if (loadPromise) return loadPromise

  loadPromise = (async () => {
    loading.value = true
    error.value = ''
    try {
      const res = await fetchMetadataList({ status: 'ENABLED', pageNum: 1, pageSize: 200 })
      const entities = res?.list ?? []
      const schemas: Record<string, MetricTableSchema> = {}
      const sources: MetricDataSourceOption[] = []

      await Promise.all(
        entities.map(async (entity) => {
          const fields = await fetchMetadataFieldsByCode(entity.entityCode)
          const schema = buildSchema(entity, fields)
          schemas[entity.physicalTable] = schema
          sources.push({
            label: `${entity.entityName} (${entity.physicalTable})`,
            value: entity.physicalTable,
          })
        }),
      )

      sources.sort((a, b) => a.value.localeCompare(b.value))
      setMetricTableSchemas(schemas)
      dataSources.value = sources
      loaded.value = true
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载元数据失败'
      throw e
    } finally {
      loading.value = false
      loadPromise = null
    }
  })()

  return loadPromise
}

export function useMetricSchemas() {
  return {
    loading,
    error,
    loaded,
    dataSources,
    ensureMetricSchemasLoaded,
  }
}
