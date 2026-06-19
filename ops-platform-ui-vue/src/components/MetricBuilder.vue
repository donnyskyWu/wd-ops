<template>
  <div v-loading="schemaLoading" class="metric-builder">
    <el-alert v-if="schemaError" type="error" :title="schemaError" show-icon :closable="false" class="schema-error" />
    <div class="builder-layout">
      <div class="builder-main">
        <el-form label-width="96px" size="default">
          <el-form-item label="数据源">
            <el-select
              :model-value="config.dataSource"
              placeholder="选择预定义数据表"
              filterable
              style="width: 100%"
              @update:model-value="onDataSourceChange"
            >
              <el-option
                v-for="item in dataSources"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="计算方式">
            <el-select v-model="config.calcMethod" style="width: 100%" @change="emitFormula">
              <el-option
                v-for="m in METRIC_CALC_METHODS"
                :key="m.value"
                :label="m.label"
                :value="m.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="calcNeedsField" label="计算字段">
            <el-select
              v-model="config.calcField"
              placeholder="选择要聚合的字段"
              filterable
              clearable
              style="width: 100%"
              @change="emitFormula"
            >
              <el-option
                v-for="f in numericFields"
                :key="f.name"
                :label="`${f.label} (${f.name})`"
                :value="f.name"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="汇总字段">
            <el-select
              v-model="config.groupByFields"
              placeholder="可选，按字段分组"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              style="width: 100%"
              @change="emitFormula"
            >
              <el-option
                v-for="f in schemaFields"
                :key="f.name"
                :label="`${f.label} (${f.name})`"
                :value="f.name"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="关联表">
            <el-select
              v-model="config.joinTables"
              placeholder="可选，选择关联表"
              multiple
              filterable
              collapse-tags
              style="width: 100%"
              @change="emitFormula"
            >
              <el-option
                v-for="j in availableJoins"
                :key="j.table"
                :label="`${j.label} (${j.table})`"
                :value="j.table"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="查询条件">
            <div class="conditions-wrap">
              <div v-for="(cond, idx) in config.conditions" :key="idx" class="condition-row">
                <el-select
                  v-model="cond.field"
                  placeholder="字段"
                  filterable
                  style="width: 130px"
                  @change="onConditionFieldChange(cond)"
                >
                  <el-option
                    v-for="f in schemaFields"
                    :key="f.name"
                    :label="f.label"
                    :value="f.name"
                  />
                </el-select>
                <el-select v-model="cond.operator" placeholder="运算符" style="width: 100px" @change="emitFormula">
                  <el-option v-for="op in METRIC_FILTER_OPERATORS" :key="op.value" :label="op.label" :value="op.value" />
                </el-select>
                <el-checkbox v-model="cond.asParameter" class="param-toggle" @change="onParameterToggle(cond)">
                  参数
                </el-checkbox>
                <template v-if="cond.asParameter">
                  <DictSelect
                    v-model="cond.queryConditionType"
                    dict-type="dict_metadata_query_condition_type"
                    placeholder="条件类别"
                    style="width: 130px"
                    @change="emitFormula"
                  />
                </template>
                <template v-else-if="cond.operator !== 'IS NULL' && cond.operator !== 'IS NOT NULL'">
                  <MetricConditionInput
                    v-model="cond.value"
                    :query-condition-type="cond.queryConditionType || fieldMetaMap[cond.field]?.queryConditionType"
                    :dict-type="cond.dictType || fieldMetaMap[cond.field]?.dictType"
                    placeholder="值"
                    style="flex: 1"
                    @update:model-value="emitFormula"
                  />
                </template>
                <span v-else-if="cond.asParameter" class="param-hint">分析时输入</span>
                <el-button link type="danger" @click="removeCondition(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button size="small" @click="addCondition">+ 添加条件</el-button>
            </div>
          </el-form-item>
        </el-form>

        <div class="builder-actions">
          <el-button type="primary" @click="generateFormula">生成公式</el-button>
          <el-button :loading="previewing" @click="handlePreview">预览数据</el-button>
        </div>

        <el-form-item label="计算公式" label-width="96px" class="formula-block">
          <el-input
            :model-value="formula"
            type="textarea"
            :rows="4"
            spellcheck="false"
            placeholder="点击「生成公式」或手动编辑 SQL"
            @update:model-value="onFormulaInput"
          />
          <div class="form-tip">保存的公式仅含固定条件；勾选「参数」的条件写入 params_json，分析/预览时动态注入 :p_字段名 占位符</div>
        </el-form-item>
      </div>

      <div class="builder-side">
        <div class="side-title">字段列表</div>
        <div v-if="!config.dataSource" class="side-empty">请先选择数据源</div>
        <ul v-else class="field-list">
          <li
            v-for="f in schemaFields"
            :key="f.name"
            class="field-item"
            @click="pickField(f)"
          >
            <span class="field-name">{{ f.name }}</span>
            <span class="field-label">{{ f.label }}</span>
            <el-tag size="small" type="info">{{ f.type }}</el-tag>
          </li>
        </ul>
        <div class="side-tip">点击字段可填入「计算字段」或「查询条件」</div>
      </div>
    </div>

    <div v-if="previewRows.length > 0 || previewError" class="preview-panel">
      <div class="preview-title">预览结果（最多 20 行）</div>
      <el-alert v-if="previewError" type="error" :title="previewError" show-icon :closable="false" />
      <el-table v-else :data="previewRows" stripe size="small" max-height="220">
        <el-table-column
          v-for="col in previewColumns"
          :key="col"
          :prop="col"
          :label="col"
          min-width="100"
          show-overflow-tooltip
        />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import {
  METRIC_CALC_METHODS,
  METRIC_FILTER_OPERATORS,
  getMetricTableSchemas,
  buildMetricSqlFromConfig,
  buildRuntimeMetricSql,
  createEmptyMetricConfig,
  packMetricBuilderParams,
  unpackMetricBuilderParams,
  extractMetricParameters,
  resolveParamKey,
  type MetricBuilderConfig,
  type MetricFieldMeta,
  type MetricFilterCondition,
  type MetricFilterFieldMeta,
} from '@/constants/metricSchema'
import { previewMetric } from '@/api/metric'
import { fetchMetadataList, fetchMetadataFieldsByCode, toMetricFilterMeta } from '@/api/metadata'
import { useMetricSchemas } from '@/composables/useMetricSchemas'
import DictSelect from '@/components/DictSelect.vue'
import MetricConditionInput from '@/components/MetricConditionInput.vue'

const props = defineProps<{
  dataSource: string
  metricFormula: string
  paramsJson?: string
}>()

const emit = defineEmits<{
  (e: 'update:dataSource', v: string): void
  (e: 'update:metricFormula', v: string): void
  (e: 'update:paramsJson', v: string): void
}>()

const { loading: schemaLoading, error: schemaError, dataSources, ensureMetricSchemasLoaded } = useMetricSchemas()
const previewing = ref(false)
const previewRows = ref<Record<string, unknown>[]>([])
const previewColumns = ref<string[]>([])
const previewError = ref('')
const fieldMetaMap = ref<Record<string, MetricFilterFieldMeta>>({})

const config = reactive<MetricBuilderConfig>(createEmptyMetricConfig(props.dataSource || ''))

const formula = computed({
  get: () => props.metricFormula,
  set: (v: string) => emit('update:metricFormula', v),
})

const schema = computed(() => getMetricTableSchemas()[config.dataSource])
const schemaFields = computed(() => schema.value?.fields ?? [])
const availableJoins = computed(() => schema.value?.joins ?? [])
const calcNeedsField = computed(() =>
  METRIC_CALC_METHODS.find((m) => m.value === config.calcMethod)?.needsField ?? false,
)
const numericFields = computed(() =>
  schemaFields.value.filter((f) => f.type === 'number'),
)

function applyBuilderConfig(builder: MetricBuilderConfig) {
  config.dataSource = builder.dataSource
  config.calcMethod = builder.calcMethod || 'COUNT'
  config.calcField = builder.calcField || ''
  config.groupByFields = [...(builder.groupByFields || [])]
  config.joinTables = [...(builder.joinTables || [])]
  config.conditions = (builder.conditions || []).map((c) => ({ ...c }))
}

async function loadFieldMeta(dataSource: string) {
  fieldMetaMap.value = {}
  if (!dataSource) return
  try {
    const res = await fetchMetadataList({ status: 'ENABLED', pageNum: 1, pageSize: 200 })
    const entity = (res?.list ?? []).find((e) => e.physicalTable === dataSource)
    if (!entity) return
    const fields = await fetchMetadataFieldsByCode(entity.entityCode)
    const meta = toMetricFilterMeta(fields)
    fieldMetaMap.value = Object.fromEntries(meta.map((m) => [m.name, m]))
  } catch {
    /* 元数据不可用时仍可用文本输入 */
  }
}

function syncParamsJson() {
  emit('update:paramsJson', packMetricBuilderParams(config))
}

watch(
  () => props.dataSource,
  async (v) => {
    if (v && v !== config.dataSource) {
      config.dataSource = v
      await loadFieldMeta(v)
    }
  },
)

watch(
  () => props.paramsJson,
  (v) => {
    const unpacked = unpackMetricBuilderParams(v)
    if (unpacked?.dataSource) {
      applyBuilderConfig(unpacked)
    }
  },
  { immediate: true },
)

onMounted(async () => {
  try {
    await ensureMetricSchemasLoaded()
    if (config.dataSource) {
      await loadFieldMeta(config.dataSource)
    }
  } catch {
    /* schemaError 已赋值 */
  }
})

function resetBuilderState(dataSource: string) {
  Object.assign(config, createEmptyMetricConfig(dataSource))
  previewRows.value = []
  previewColumns.value = []
  previewError.value = ''
}

async function onDataSourceChange(v: string) {
  resetBuilderState(v)
  emit('update:dataSource', v)
  await loadFieldMeta(v)
  emitFormula()
}

function createCondition(): MetricFilterCondition {
  return { field: '', operator: '=', value: '', asParameter: false }
}

function addCondition() {
  config.conditions.push(createCondition())
}

function removeCondition(idx: number) {
  config.conditions.splice(idx, 1)
  emitFormula()
}

function onConditionFieldChange(cond: MetricFilterCondition) {
  const meta = fieldMetaMap.value[cond.field]
  if (meta) {
    cond.queryConditionType = meta.queryConditionType
    cond.dictType = meta.dictType
  }
  emitFormula()
}

function onParameterToggle(cond: MetricFilterCondition) {
  if (cond.asParameter) {
    const meta = fieldMetaMap.value[cond.field]
    if (meta && !cond.queryConditionType) {
      cond.queryConditionType = meta.queryConditionType
      cond.dictType = meta.dictType
    }
    cond.value = ''
  }
  emitFormula()
}

function pickField(f: MetricFieldMeta) {
  if (f.type === 'number') {
    config.calcField = f.name
    if (config.calcMethod === 'COUNT') {
      config.calcMethod = 'SUM'
    }
  } else {
    const empty = config.conditions.find((c) => !c.field)
    const meta = fieldMetaMap.value[f.name]
    if (empty) {
      empty.field = f.name
      if (meta) {
        empty.queryConditionType = meta.queryConditionType
        empty.dictType = meta.dictType
      }
    } else {
      const cond = createCondition()
      cond.field = f.name
      if (meta) {
        cond.queryConditionType = meta.queryConditionType
        cond.dictType = meta.dictType
      }
      config.conditions.push(cond)
    }
  }
  emitFormula()
}

function generateFormula() {
  if (!config.dataSource) {
    ElMessage.warning('请先选择数据源')
    return
  }
  if (calcNeedsField.value && !config.calcField) {
    ElMessage.warning('请选择计算字段')
    return
  }
  const sql = buildMetricSqlFromConfig(config)
  emit('update:metricFormula', sql)
  syncParamsJson()
  ElMessage.success('公式已生成')
}

function emitFormula() {
  if (!config.dataSource) return
  const sql = buildMetricSqlFromConfig(config)
  if (sql) {
    emit('update:metricFormula', sql)
    syncParamsJson()
  }
}

function onFormulaInput(v: string) {
  emit('update:metricFormula', v)
}

function buildPreviewBindParams(): Record<string, string> {
  const params: Record<string, string> = {}
  for (const cond of extractMetricParameters(config)) {
    const pk = resolveParamKey(cond)
    if (cond.queryConditionType === 'DATE_RANGE') {
      if (cond.value) {
        const [start, end] = cond.value.split(',')
        if (start) params[`${pk}_start`] = start
        if (end) params[`${pk}_end`] = end
      }
    } else if (cond.value) {
      params[pk] = cond.value
    }
  }
  return params
}

async function handlePreview() {
  if (!config.dataSource) {
    ElMessage.warning('请先选择数据源')
    return
  }
  const bindParams = buildPreviewBindParams()
  const sql = buildRuntimeMetricSql(props.metricFormula?.trim() || '', packMetricBuilderParams(config), bindParams)
  if (!sql) {
    ElMessage.warning('请先生成或填写计算公式')
    return
  }
  previewing.value = true
  previewError.value = ''
  previewRows.value = []
  previewColumns.value = []
  try {
    const res: any = await previewMetric({ metricFormula: sql, bindParams })
    const data = res?.data ?? res
    const rows = data?.rows ?? []
    previewRows.value = Array.isArray(rows) ? rows : [rows]
    previewColumns.value = previewRows.value.length > 0 ? Object.keys(previewRows.value[0]) : []
    if (previewRows.value.length === 0) {
      ElMessage.info('查询成功，暂无数据')
    }
  } catch (e: any) {
    previewError.value = e?.message || '预览失败'
  } finally {
    previewing.value = false
  }
}
</script>

<style scoped>
.metric-builder { width: 100%; }
.schema-error { margin-bottom: 12px; }
.builder-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.builder-main { flex: 1; min-width: 0; }
.builder-side {
  width: 220px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px;
  background: var(--el-fill-color-blank);
  max-height: 420px;
  overflow-y: auto;
}
.side-title { font-weight: 600; font-size: 13px; margin-bottom: 8px; }
.side-empty { color: #909399; font-size: 12px; }
.field-list { list-style: none; margin: 0; padding: 0; }
.field-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  padding: 6px 4px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  cursor: pointer;
  font-size: 12px;
}
.field-item:hover { background: var(--el-fill-color-light); }
.field-name { font-family: monospace; color: var(--el-color-primary); }
.field-label { color: #606266; flex: 1; }
.side-tip { margin-top: 8px; font-size: 11px; color: #909399; }
.conditions-wrap { width: 100%; }
.condition-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.param-toggle { margin-right: 0; }
.param-hint { color: #909399; font-size: 12px; flex: 1; }
.builder-actions {
  display: flex;
  gap: 8px;
  margin: 0 0 12px 96px;
}
.formula-block { margin-top: 4px; }
.form-tip { margin-top: 4px; color: #909399; font-size: 12px; }
.preview-panel {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color);
}
.preview-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
</style>
